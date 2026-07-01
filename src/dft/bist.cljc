(ns dft.bist
  "Built-In Self-Test — memory BIST (MBIST) and logic BIST (LBIST)
  generation. Restored from kami-dft's `bist` module (kami-engine/kami-dft/
  src/bist.rs, deleted PR #82).")

(def bist-types #{:memory-bist :logic-bist})
(def march-algorithms #{:march-c :march-c-minus :march-b :march-a :checkerboard})

(defn march-elements
  "Number of read/write operations per address for `algorithm`."
  [algorithm]
  (case algorithm
    :march-c 10
    :march-c-minus 10
    :march-b 17
    :march-a 15
    :checkerboard 4))

(defn mbist-config
  [{:keys [memory-name algorithm data-width addr-width]}]
  {:memory-name memory-name :algorithm algorithm
   :data-width data-width :addr-width addr-width})

(defn create-mbist
  "Create a memory BIST controller: computes state-machine complexity and
  test time from the march algorithm and memory dimensions."
  [config]
  (let [num-addresses (bit-shift-left 1 (:addr-width config))
        march-ops (march-elements (:algorithm config))
        test-time-cycles (+ (* num-addresses march-ops) 10)
        state-count (+ (march-elements (:algorithm config)) 3)]
    {:config config :state-count state-count :test-time-cycles test-time-cycles}))

(defn lbist-config
  [{:keys [seed polynomial scan-chain-count]}]
  {:seed seed :polynomial polynomial :scan-chain-count scan-chain-count})

(defn create-lbist
  "Create a logic BIST controller: 1024 patterns per scan chain (standard
  LBIST depth)."
  [config]
  {:config config :test-cycle-count (* 1024 (:scan-chain-count config))})
