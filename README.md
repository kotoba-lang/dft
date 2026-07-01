# kotoba-lang/dft

Zero-dep portable `.cljc` — restored from the legacy `kami-engine/kami-dft`
Rust crate (deleted in kotoba-lang/kami-engine PR #82 "Remove Rust workspace
from kami-engine") as part of the **clj-wgsl migration** (ADR-2607010930,
`com-junkawasaki/root`).

**DFT = Design-For-Test**, not Discrete Fourier Transform. Scan chain
insertion, BIST generation, ATPG pattern generation, and JTAG/BSDL support.

| Namespace | Restored from | Purpose |
|---|---|---|
| `dft.scan` | `scan` | Scan chain insertion (round-robin flip-flop distribution) |
| `dft.bist` | `bist` | Memory BIST (MBIST) + logic BIST (LBIST) generation |
| `dft.atpg` | `atpg` | Automatic test pattern generation (stuck-at/transition/bridging faults) |
| `dft.jtag` | `jtag` | IEEE 1149.1 boundary scan + BSDL file generation |

Depends on `kotoba-lang/engineer` for shared contracts (constraint/DRC/etc).

## Status

Restored — all 4 modules ported from the original 695-line Rust source
(`lib.rs` + `scan.rs` + `bist.rs` + `atpg.rs` + `jtag.rs`), with all 12
original Rust unit tests mirrored 1:1 in `test/dft_test.cljc` (+1 smoke
test). Pure data + pure functions throughout; `dft.atpg`'s xorshift64 PRNG
uses unsigned 64-bit shift/mod (JVM `Long/remainderUnsigned` — a CLJS arm
can be added if a browser consumer needs ATPG).

## Develop

```bash
clojure -M:test
```
