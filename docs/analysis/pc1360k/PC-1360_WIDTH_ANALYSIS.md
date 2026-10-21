# PC-1360 WIDTH alphabet path (2026-10-18)

> 本ファイルは解析当時の経過記録です。「未取得」「未実装」等は各時点の状態を示します。
> 現在の到達点・確認済み事項・未解明の点は、[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)を参照してください。

## Scope

Opt-in local experiment `Pc1360WidthLocalAnalysisTest`, enabled with
`PGP_ANALYZE_PC1360_WIDTH=1`, uses the user's existing local PC-1360 and
PC-1360K internal/normal-ROM files. No Kanji ROM is loaded and production
CPU/bus behavior is not changed. All trace, font and image files are saved
under ignored `local-data/analysis/pc-1360-width/`.

The experiment initializes RAM through Y/ENTER/CLEAR, then compares normal
A input with direct writes of CharMax=18 (FD07) and WIDTH=2 (FD08). It also
compares the K normal-alphabet path and K Kanji mode, selecting Kanji before
recording the final A press. This models the direct-register entry described
by the user, not proof of KS8/jumper behavior or WIDTH-command availability.

## Findings

| Case | Font source | LCD write site, bank 1 | FEB0..FECF writes | 3600 writes | READ/READM |
| --- | --- | --- | ---: | ---: | ---: |
| 1360 normal A | normal-ROM bank 1, 4230..4234 | 61E9 (+61EF blank column) | 0 | 0 | 0 |
| 1360 WIDTH=2 A | normal-ROM bank 1, 4723..4732 | 51E3 | 0 | 0 | 0 |
| 1360K WIDTH=2 A | same normal-ROM range and bytes | 51E3 | 0 | 0 | 0 |
| 1360K Kanji-mode A, missing Kanji ROM | substitute data, via FEB0..FECF | 51E3 | 32 | 2 | 38 |

The logged LCD write counts (including cursor-related drawing) were 12, 32,
32 and 48 respectively. ASCII font bytes were checked against the first
5/16 bytes actually written to LCD memory; this confirms the source, not just
a visually similar display. The wide A font bytes also match between the two
supplied normal-ROM sets.

Normal A uses a 5-column x 8-row cell bitmap. WIDTH=2 A uses a separately stored
8-column x 16-row cell bitmap, not a runtime vertical enlargement of the same
five bytes. For ASCII A (41), the wide pointer calculation is consistent with
4513 + (41 - 20) * 10 = 4723 (hex), visible in the routine around 5242..525D.
The common raw transfer reads via IXL/24 at 51D8 and writes via IYS/26 at 51E3.

In the K Kanji case, the routine fills the FEB0 buffer, then sets X to that
buffer and uses the same 51D8/51E3 read/copy loop. The byte values in this case
are invalid emulator substitutes because Kanji READ semantics/content are not
yet implemented. They establish the observed path through the current code,
not correct dictionary/character contents on hardware.

## Correction of the earlier shared-buffer hypothesis

FEB0 is not used by the measured WIDTH=2 ASCII A path. A more accurate observed
flow is:

```text
normal-ROM wide alphabet font -----------+--> shared LCD copy loop --> LCD
                                        |
Kanji read --> FEB0 temporary buffer ----+
```

Thus the shared part is the wide display/copy path, while FEB0 is needed by the
observed Kanji glyph acquisition path. Other characters, effects or conversion
operations may use additional buffers and have not been exhaustively checked.

No CPU writes to 2000..27FF occurred during any of the four measured A-entry
windows; before/after snapshots of that range also match. This does not prove
that the 2 KiB area is unused: Kanji-mode setup precedes the recording, real
Kanji data are missing, and conversion has not yet been tested.

## Validation and limits

Assertions check actual LCD activity, normal-ROM font bytes matching LCD
writes, absence of special reads/selector/FEB0 activity in the three ASCII
cases, and presence of special reads/selector/FEB0 activity in the K Kanji case.
This is a headless local-ROM observation. The new facts have not separately
been measured on physical hardware. The user's earlier hardware observations
support direct WIDTH-mode entry, but do not independently verify every address
listed here.

The first two analysis stages now have a concrete boundary between standard
alphabet rendering and Kanji glyph acquisition. Next, verify remaining
READ/READM control/address conditions on hardware before full-ROM extraction.
KS8's physical connection and the hidden WIDTH-command gate remain separate
follow-up items rather than assumptions in this experiment.
