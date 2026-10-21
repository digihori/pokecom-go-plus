# PC-1360K READ-path verification probe

> この文書は個別実験の経過記録です。「未取得」「未実装」等は各記述時点の状態を示します。
> 現在のまとめは[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)、
> 吸い出し用BASICの使い方は[配布用手順書](../../../tools/pc1360k-rom-dump/README.md)を参照してください。

This is a prepared hardware experiment, not a confirmed Kanji-ROM dumper.
It distinguishes the documented next-PC read from behavior that depends on DP
or the selector. It has been assembled and checked on the current emulator;
it has not been run on hardware.

## BASIC loader

`read-path-probe.bas` is a complete DATA/POKE loader for the 75-byte routine.
It initializes the code, calls it and prints the four results separated by
commas. This path has been run through the local PC-1360K ROM, including BASIC
FOR/READ/POKE, CALL, RTN and the following PRINT. A marker at C204 (decimal
49668) becomes 165 after CALL returns. The hardware result is still unknown.

Load it as a new BASIC program, using the normal BASIC transfer method, then
RUN on the real machine with RAM C000..C204 available. The current Studio
baseline is `3,18,3,18` (the display can add numeric-format decimal points).
Record the real four values in their displayed order. This first check does
not change F output and does not prove a physical ROM mapping even if it matches.

## Files and placement

- Source: `read-path-probe.asm` in this directory.
- Entry: `0xC000` (decimal 49152).
- Test data: `0xC100..0xC101` (A5, 5A), initialized by the routine.
- Results: `0xC200..0xC203` (decimal 49664..49667).
- The routine writes `0x3600` first to 0, then 1, and leaves it at 1.
- It preserves A, B and P and balances the CPU stack. Other CPU temporaries and
  flags are not preserved. It does not issue OUTF or change `0x3400`.

Use only when RAM `0xC000..0xC204` is available for the experiment and code at
`0xC000` can be loaded on the particular machine/RAM-card configuration.
The profile's writable map does not establish that this RAM exists on hardware.

Assemble with Studio's built-in assembler for the 1360K profile and load at
`0xC000`. On the machine, run `CALL &C000`. Inspect the four results with
`PEEK &C200` through `PEEK &C203` (decimal results are also listed below).
Transfer to hardware uses the user's existing machine-code loading method;
no physical device is connected to the automated test.

## Results

| Result address | 0x3600 | DP for READ | Next instruction | Current Studio result |
| --- | --- | --- | --- | --- |
| C200 | 0 | C100 | LIB (03) | 03 (3) |
| C201 | 0 | C101 | LIP (12) | 12 (18) |
| C202 | 1 | C100 | LIB (03) | 03 (3) |
| C203 | 1 | C101 | LIP (12) | 12 (18) |

If hardware returns 03/12/03/12, that supports the documented behavior under
these particular test conditions. It does not establish all ROM read modes.
Differences identify a missing condition to investigate; they do not by
themselves establish DP addressing or identify a physical ROM chip.

For a second comparison, change the two test-data immediates A5/5A to 3C/C3
and rerun. Whether the results follow RAM contents, next opcode, or neither
helps distinguish read sources. Existing F-port output is deliberately left
unchanged in this first probe; a negative result cannot rule out a requirement
for a different F output or a different memory window.

## Automated baseline / local experiment

With JDK 21 configured, run:

```sh
./gradlew --gradle-user-home .gradle-local :core:desktopTest --tests '*Pc1360kLocalAnalysisTest' --rerun
```

The baseline test assembles this source and checks the four results, saved
registers, and stack balance using synthetic firmware, stopping before RTN.
The opt-in local-ROM experiment separately executes the BASIC loader and verifies
its CALL returns and the post-CALL marker is written. No physical device is used.

For the separate local-ROM experiment, set `PGP_ANALYZE_PC1360K=1`. This also
writes the assembled probe binary under ignored
`local-data/analysis/pc-1360k/read-path-probe.bin`, with load address C000.
The local-ROM run initializes RAM, enters WIDTH,2, selects Kanji and presses A.
`PGP_PC1360K_ANALYSIS_KEY=I` selects a different input key, and
`PGP_PC1360K_ANALYSIS_KATA=1` selects Katakana before that key.
A consonant alone may wait for the next Romaji letter without accessing a glyph.

## Hardware follow-up

The user has now reported `0,1,55,94` from the original probe, which differs
from Studio. See [second experiment](READ_MATRIX.md) for the fixed-site follow-up
program `read-path-matrix.bas` and its three output lines. Earlier hardware
pending statements in this document describe the initial preparation stage.
