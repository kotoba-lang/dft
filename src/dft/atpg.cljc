(ns dft.atpg
  "Automatic Test Pattern Generation — fault modeling and pattern generation
  for stuck-at and transition faults. Restored from kami-dft's `atpg`
  module (kami-engine/kami-dft/src/atpg.rs, deleted PR #82).

  The original Rust used a `u64` xorshift PRNG for deterministic-but-varied
  pattern fill; ported here with unsigned right-shift (`unsigned-bit-shift-
  right`) so the 64-bit bit pattern matches the Rust `>>` on `u64` (logical,
  not arithmetic). No test depends on exact PRNG output values — only on
  aggregate detection behavior, which is deterministic per fault type.")

(def fault-types
  #{:stuck-at-0 :stuck-at-1 :transition-slow :transition-fast
    :bridging-and :bridging-or})

(defn- xorshift64
  "Simple xorshift64 PRNG step, matching the original Rust `u64` bit pattern."
  [state]
  (let [state (bit-xor state (bit-shift-left state 13))
        state (bit-xor state (unsigned-bit-shift-right state 7))
        state (bit-xor state (bit-shift-left state 17))]
    state))

(defn- umod
  "`state mod n` treating `state` as an unsigned 64-bit long (matches Rust's
  unsigned `%`). JVM-only; a CLJS arm can be added if a browser consumer
  needs ATPG (unsigned 64-bit mod isn't representable in JS doubles without
  BigInt)."
  [state n]
  #?(:clj (Long/remainderUnsigned state n)
     :cljs (mod state n)))

(defn generate-patterns
  "Generate test patterns for `faults` (a seq of `{:net-name :fault-type
  :detected}` maps) against a design with `gate-count` gates. Uses a
  simplified D-algorithm seed (drive the activating value on the faulted
  net) + xorshift64 pseudo-random fill for other inputs, with a
  fault-type-dependent simulated detection rate (stuck-at: always detected;
  transition: 80%; bridging: 60%). Returns `{:patterns :fault-coverage
  :detected-faults :total-faults :aborted-faults}` plus the faults with
  `:detected` updated."
  [faults gate-count]
  (let [total-faults (count faults)
        gate-shift (min gate-count 16)
        fill-mod (bit-shift-left 1 gate-shift)]
    (loop [remaining faults
           rng-state (unchecked-long 0xCAFEBABE12345678N)
           patterns []
           updated-faults []
           detected 0
           aborted 0]
      (if (empty? remaining)
        {:patterns patterns
         :fault-coverage (if (pos? total-faults) (/ (double detected) total-faults) 0.0)
         :detected-faults detected
         :total-faults total-faults
         :aborted-faults aborted
         :faults updated-faults}
        (let [fault (first remaining)
              fault-type (:fault-type fault)
              [activating-value rng-state]
              (case fault-type
                :stuck-at-0 [1 rng-state]
                :stuck-at-1 [0 rng-state]
                (:transition-slow :transition-fast :bridging-and :bridging-or)
                (let [s (xorshift64 rng-state)] [(bit-and s 1) s]))
              rng-state (xorshift64 rng-state)
              random-input (umod rng-state fill-mod)
              pattern {:inputs [[(:net-name fault) activating-value]
                                ["random_fill" random-input]]
                       :expected [["out" activating-value]]}
              [is-detected rng-state]
              (case fault-type
                (:stuck-at-0 :stuck-at-1) [true rng-state]
                (:transition-slow :transition-fast)
                (let [s (xorshift64 rng-state)] [(< (umod s 100) 80) s])
                (:bridging-and :bridging-or)
                (let [s (xorshift64 rng-state)] [(< (umod s 100) 60) s]))]
          (recur (rest remaining)
                 rng-state
                 (conj patterns pattern)
                 (conj updated-faults (assoc fault :detected is-detected))
                 (if is-detected (inc detected) detected)
                 (if is-detected aborted (inc aborted))))))))
