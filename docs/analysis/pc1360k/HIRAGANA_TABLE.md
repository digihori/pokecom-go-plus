# PC-1360K first six Hiragana-table bytes

> この文書は個別実験の経過記録です。「未取得」「未実装」等は各記述時点の状態を示します。
> 現在のまとめは[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)、
> 吸い出し用BASICの使い方は[配布用手順書](../../../tools/pc1360k-rom-dump/README.md)を参照してください。

The user reported the READ/READM comparison exactly matching the DP-read
prediction. At C100/C101 under the same setup, opcodes 56 and 54 agree and
ordinary RAM PEEK remains A5/5A. READM's behavior is no longer merely a
prediction in these tested conditions. Physical bank count, AD16/AD17 and
all F-port conditions remain unverified.

## Small actual-data read

Run `read-hiragana-table.bas` as a new BASIC program on the same real machine
and return its two lines, T0 and T1. This program does not need prior WIDTH or
Kanji key operations; it explicitly writes selector 0, uses READ at DP
0156..015B, then leaves selector 1. If it produces unexpected data, retain the
exact values and setup instead of assuming the mapping is universal.

The addresses are the candidate table region computed by the normal-ROM routine
for input 2422 (Hiragana A). This is an 83-byte machine-code routine loaded by
DATA/POKE. Entry C000, code C000..C052, results C200..C205, and the BASIC
post-CALL completion marker at C206. Requires RAM C000..C206 available. A/B/P
and stack balance are preserved. F output and normal-ROM bank are unchanged
by the machine-code routine.

| Output | Selector | Requested DP addresses | Intended interpretation |
| --- | --- | --- | --- |
| T0 | 0 | 0156,0157,0158 | header and big-endian base word |
| T1 | 0 | 0159,015A,015B | descriptors 0,1,2; descriptor 2 corresponds to 2422 |

The meaning of these bytes is inferred from unmodified normal-ROM arithmetic;
it is not yet confirmed using actual table contents. No specific hardware byte
values are predicted. Current Studio's generic next-PC implementation returns
3,3,3 on both lines, which is only an emulator baseline.

The normal-ROM routine would compute, for header h, base bytes hi/lo and
preceding descriptors d0/d1:

```text
selector = (h & 4) >> 2
base = (((h & 3) << 14) + (hi << 8) + lo) & 0xFFFF
advance(d) = 0 if (d & 0x1F) == 0 else (d & 0x1F) + 1
candidateGlyphAddress = (base + advance(d0) + advance(d1)) & 0xFFFF
```

Descriptor 2 then controls the copy/decode path for the requested character.
Actual bytes are needed before preparing a targeted glyph-data read. This
program reads only six bytes; it is not a whole-ROM dump and does not imply
that either chip has been fully mapped.

## Validation

Synthetic tests check the emulator baseline, restoration of saved registers,
stack balance and final selector 1. The opt-in real-ROM experiment with
`PGP_PC1360K_TABLE=1` checks the complete BASIC DATA/POKE loader, CALL, return and
completion marker. These pass; hardware table data are pending.

## Hardware data received

The user returned T0=0,35,90 and T1=0,29,29. Applying the formula gives selector
0 and glyph candidate 2378, with a 30-byte raw copy path. Continue with the
[targeted glyph reader](HIRAGANA_GLYPH.md).
