# First Hiragana A glyph candidate

> この文書は個別実験の経過記録です。「未取得」「未実装」等は各記述時点の状態を示します。
> 現在のまとめは[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)、
> 吸い出し用BASICの使い方は[配布用手順書](../../../tools/pc1360k-rom-dump/README.md)を参照してください。

The user returned the six table bytes in decimal:

```text
T0: 0,35,90
T1: 0,29,29
```

These are the user-reported READ results for selector 0, DP 0156..015B.
The first header byte is 00, base word is 235A, and the preceding two
descriptors are 00 and 1D. Applying the unmodified normal-ROM arithmetic:

```text
next selector = (00 & 04) >> 2 = 0
base = (00 & 03) * 4000 + 235A = 235A
preceding advances = 0 + (1D + 1) = 001E
candidate glyph start = 235A + 001E = 2378
current descriptor = 1D
```

The descriptor's compression flags 40/80 are clear. The ROM copy path reads
fifteen bytes twice and inserts a zero padding byte after each group, yielding
30 ROM bytes and a 32-byte working buffer. This is consistent with a 15-column,
two-page bitmap. It remains a glyph candidate until its actual bytes can be
rendered and checked. The selector remains 0 for this glyph; a fixed table-0 /
font-1 rule would be incorrect.

## Read the 30 bytes

Load `read-hiragana-glyph.bas` as a new BASIC program on the same hardware/setup
and RUN. It reads selector 0, DP 2378..2395 (30 bytes), then restores selector 1.
Record display lines 0 through 7, pressing ENTER after each PRINT to continue.
Lines 0..6 each contain four decimal bytes; line 7 contains the final two.
The ROM's PRINT statement itself waits for ENTER; no numeric INPUT is needed.

```text
0: b0,b1,b2,b3
1: b4,b5,b6,b7
2: b8,b9,b10,b11
3: b12,b13,b14,b15
4: b16,b17,b18,b19
5: b20,b21,b22,b23
6: b24,b25,b26,b27
7: b28,b29
```

The file contains a DATA/POKE loader for a 323-byte unrolled routine.
Requires RAM C000..C21F available: code C000..C142, output C200..C21D,
post-CALL marker C21E and final display-completion marker C21F. It preserves
A/B/P and balances the CPU stack, without changing F output or normal-ROM bank.
No Kanji-mode keyboard setup is required by the routine itself, but use the
same hardware state as the earlier successful read experiments.

## Verification and next interpretation

Synthetic tests check all 30 current-emulator bytes (03 because the generic
READ implementation reads the next LIB), saved registers and stack balance.
The opt-in local-ROM experiment (`PGP_PC1360K_GLYPH=1`) tests the entire BASIC
loader, CALL, all eight PRINT continuations and normal completion. It passes.
The hardware data remain pending.

After receiving all 30 bytes, render the first fifteen bytes as columns of
an upper 8-pixel page and the next fifteen as columns of the lower page,
starting with bit0 at the top of each page, consistent with PGP's current LCD
bit order. A rendered match to the expected character will test both the
pointer arithmetic and copy-layout interpretation. Retain raw bytes separately
from any orientation or display interpretation. This single character does
not establish the full ROM mapping or chip capacity.

## Hardware glyph received

The user returned all 30 bytes. A duplicate comma in output row 4 was treated
as an extra separator, so that row contributes four values (28,34,33,48).
Rendering the two 15-column pages with bit0 at the top produces the expected
Hiragana A. Thus the table-derived source 2378 and selector 0 are verified for
this character in the measured setup. Raw bytes and rendered images remain in
the ignored local analysis directory, not in this document or source fixtures.
