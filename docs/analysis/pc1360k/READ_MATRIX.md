# PC-1360K second READ experiment

> この文書は個別実験の経過記録です。「未取得」「未実装」等は各記述時点の状態を示します。
> 現在のまとめは[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)、
> 吸い出し用BASICの使い方は[配布用手順書](../../../tools/pc1360k-rom-dump/README.md)を参照してください。

The user reported these decimal results from the first hardware probe:
`0,1,55,94`. They correspond to hex `00,01,37,5E`, unlike the current Studio
baseline `03,12,03,12`. They are a user-reported hardware observation, not an
automated device capture; F-port state and the complete setup were not captured.

The selector is therefore relevant to the read result under the tested
conditions. The first experiment varied both DP and the following instruction,
so it does not by itself establish which address READ uses. No production CPU
or mapping change is made solely on this four-byte observation.

## Run the second BASIC program

Load `read-path-matrix.bas` as a new BASIC program and RUN, using the same
machine/setup as the first experiment. Its 123-byte routine is assembled from
`read-path-matrix.asm`; the generated BASIC loads it using DATA/POKE.
Requires available RAM C000..C208. Test data are at C100/C101, results at
C200..C207, and the post-CALL completion marker at C208. The selector is left
at 1. F output and normal-ROM bank are not changed by the machine-code routine.
A/B/P and stack balance are preserved.

Each group uses two fixed READ subroutine addresses. Within the same READ site,
only DP changes. Each DP is also tested at both sites, with LIB (03) or LIP (12)
as the next instruction. This avoids conflating the two changes as in probe 1.

| Display | Result order | Current Studio |
| --- | --- | --- |
| 0: | selector 0: C100/LIB, C100/LIP, C101/LIB, C101/LIP | 3,18,3,18 |
| 1: | selector 1: C100/LIB, C100/LIP, C101/LIB, C101/LIP | 3,18,3,18 |
| R: | ordinary BASIC PEEK of C100,C101 | 165,90 |

If READ follows DP and the first hardware bytes recur independently of the new
READ instruction sites, the predicted result is:

```text
0: 0,0,1,1
1: 55,55,94,94
R: 165,90
```

This is a prediction to test, not an observed hardware result. Return all three
display lines even if they differ. Changed results can reveal an address,
instruction-location, state or transfer dependency and must be investigated
before choosing a mapping. RAM sentinels help identify a damaged or incomplete
load. The routine alone does not identify the physical Kanji ROM chip or the
AD16/AD17 relationship.

## Validation

The synthetic test checks that each fixed READ site is actually called twice
per selector with DP C100 and C101, verifies the documented Studio results and
A/B/P/stack restoration, and checks the sentinels remain A5/5A.
The opt-in local-ROM run with `PGP_PC1360K_MATRIX=1` separately runs the complete
DATA/POKE loader through BASIC and verifies post-CALL completion. Both pass.
Hardware verification is pending.

## Hardware result received

The user returned exactly the three predicted lines shown above. This supports
DP-dependent selector-specific reads under the tested setup, distinct from
ordinary RAM. The next step is the [READM comparison](READM_COMPARISON.md),
which tests opcode 54 in addition to the now-observed opcode 56 behavior.
