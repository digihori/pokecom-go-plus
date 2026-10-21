# PC-1360K READ versus READM comparison

> この文書は個別実験の経過記録です。「未取得」「未実装」等は各記述時点の状態を示します。
> 現在のまとめは[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)、
> 吸い出し用BASICの使い方は[配布用手順書](../../../tools/pc1360k-rom-dump/README.md)を参照してください。

The second fixed-site hardware experiment returned exactly the DP-read
prediction, as reported by the user:

```text
0: 0,0,1,1
1: 55,55,94,94
R: 165,90
```

At each selector value, the same READ site changed result with DP, while the
same DP gave identical results at two sites with different following opcodes.
Ordinary BASIC RAM reads still returned the A5/5A sentinels. Thus, under the
reported setup, READ is DP-dependent, selector-dependent, and distinct from
ordinary RAM reading. This does not identify the physical chips or guarantee
behavior under every F-port value, machine revision or memory window.

## Compare the other glyph-read instruction

Run `read-path-readm.bas` as a new BASIC program on the same real machine and
return its three display lines. The first item in each pair is READ/56, and the
second is READM/54. READM writes internal RAM at P, which is then copied to A
for the result. The overwritten internal RAM byte at 20 and A/B/P are saved
and restored; the CPU stack balances. The routine does not change F output or
normal-ROM bank and leaves 3600 at 1, matching the previous experiments.

The program uses RAM C000..C208, like the fixed-site experiment. The generated
BASIC loads the routine, CALLs it and prints results. The bare binary is also
available under the ignored local analysis output directory at load address
C000. It is a comparison probe, not a ROM dumper.

| Display | Result order |
| --- | --- |
| 0: | selector 0: C100/READ, C100/READM, C101/READ, C101/READM |
| 1: | selector 1: C100/READ, C100/READM, C101/READ, C101/READM |
| R: | ordinary BASIC RAM PEEK at C100,C101 |

If READM shares the hardware READ source, the predicted output is again:

```text
0: 0,0,1,1
1: 55,55,94,94
R: 165,90
```

This output is a prediction for READM; it is not yet hardware-verified.
Current Studio instead returns 3,89,3,89 for each selector row: 56 reads the next
LIB opcode 03 and 54 reads the next LDM opcode 59. The ordinary RAM check is
165,90. This baseline and full BASIC loader/CALL/return pass automated tests.
No production CPU/mapping behavior is changed by creating these experiments.

Once both read instructions are verified, model their reads as an explicit
machine-specific ROM-read operation, separate from instruction fetch and
ordinary DP RAM reads. Generic CPUs can retain their existing behavior until
other models are measured. ROM contents, physical bank count and control
conditions should remain explicit unknowns; the two tested selector values
alone do not establish a 256 KiB mapping.

## Hardware result received

The user returned 0:0,0,1,1 / 1:55,55,94,94 / R:165,90, matching the prediction.
The next experiment is the [six-byte Hiragana-table reader](HIRAGANA_TABLE.md).
