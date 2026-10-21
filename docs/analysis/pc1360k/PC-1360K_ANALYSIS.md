# PC-1360K initial local observation (2026-10-17)

> 本ファイルは解析当時の経過記録です。「未取得」「未実装」等は各時点の状態を示します。
> 現在の到達点・確認済み事項・未解明の点は、[PC-1360K 漢字ROM解析とStudio対応](../../reference/machines/PC-1360K.md)を参照してください。

## Reproduction

The opt-in `Pc1360kLocalAnalysisTest` experiment runs only when
`PGP_ANALYZE_PC1360K=1`. It reads local firmware and saves traces and LCD dot
snapshots under the ignored `local-data/analysis/pc-1360k/` directory.

Cold boot initially waits at `RAM CARD S1 CLEAR O.K.?`. The experiment answers
Y, ENTER, CLEAR, then types `WIDTH,2`, ENTER, waits for display processing,
presses KANJI, and finally A. Insufficient waiting after WIDTH can miss KANJI.
The current experiment uses a one-million-cycle wait after ENTER.

Observed state:

| Stage | 0x303C | CharMax (0xFD07) | WIDTH (0xFD08) |
| --- | --- | --- | --- |
| Initialized | 0x08 | 24 | 4 |
| WIDTH,2 | 0x08 | 18 | 2 |
| KANJI | 0xA8 | 18 | 2 |
| A | 0xA8 | 18 | 2 |

LCD dot snapshots reproduce the user's reported bunsetsu indicator after KANJI
and striped glyph after A. This is a fresh local session, not a capture of the
user's running Studio session. It does not validate missing Kanji-ROM hardware.

## 0x3600 and F-port

No 0x3600 writes occurred in the boot, initialization, WIDTH or KANJI stages.
A produced two writes in this run:

- Normal ROM bank 1, PC 0x509C: write 0 to 0x3600.
- Normal ROM bank 1, PC 0x5118: write 1 to 0x3600.

This particular sequence is 0 then 1, rather than the user's recollection of
1 then 0. Both writes must be watched, including the first same-value write.
It does not determine the signal controlled or establish a chip-selection rule.

F output changed 0 -> 1 -> 5 -> 1 during cold boot. There were four OUTF
instructions during boot, three more during initialization, and none during
WIDTH, KANJI or A in the recorded run. F remained 1 during the two selector
writes. Thus absence of a nearby OUTF does not mean F was never configured.

## Read-path limitation requiring investigation

After the second selector write, a loop around bank 1 PC 0x514D increments X,
executes opcode 0x56 at 0x514E, then stores A through incremented Y at 0x514F.
The current CPU implementation of opcode 0x56 reads the byte at the next PC
without consuming it. The observed loop therefore reads the next opcode (0x26)
and repeats that value into the 0xFEB0 buffer, instead of observing reads at the
successive X/DP addresses.

A similar opcode occurs in the preceding table-lookup path. The existing
reference emulator and PGP tests encode this next-PC behavior. These facts do
not establish the real hardware behavior of the instruction in this context.
Verify opcode semantics and external bus behavior before using computed DP
addresses to infer the Kanji-ROM window or treating the stripes as caused only
by missing ROM contents. No CPU instruction or Kanji mapping is changed by this
experiment.

## Next questions

1. What addresses and bus signals should opcode 0x56 use on this CPU variant?
2. Does 0x3600 select address interpretation, chip enable, or a higher address bit?
3. How do CED, AD16, AD17 and the initialized F output interact on the schematic?
4. Once the read path is verified, compare A with other characters and observe
   the actual character-code-to-address transformation.

## Follow-up: READ semantics and row-table candidates

Further inspection of Bernard Saretz's *Langage Machine SC61860* (1986),
printed pp. 161-162 (PDF pages 83-84), supports `(PC+1) -> A` for READ/0x56
and `(PC+1) -> (P)` for READM/0x54. Its example reads the next LIB opcode while
still executing LIB afterward. Thus PGP's common next-PC implementation is
consistent with this published example. This is third-party documentation of
an undocumented instruction, not an authoritative statement about every CPU
revision or hardware bus cycle. No common CPU semantics were changed.

Reference: <https://pockemul.com/Documents/lm61860.pdf>. A local copy is kept in
the ignored analysis references directory; the book is not bundled with PGP.

Three independent cold-boot reproductions produced:

| Input mode / key | Mode (303C) | X at bank 1 PC 509C | DP at first READ, PC 50DD |
| --- | --- | --- | --- |
| Hiragana / A | A8 | 2422 | 0156 |
| Hiragana / I | A8 | 2424 | 0156 |
| Katakana / A | C8 | 2522 | 01C8 |

The codes correspond to JIS positions for the reported syllables (a, i, A).
The first computed DP agrees with `(row - 0x21) * 0x72` for these observed rows.
This is a row-table addressing hypothesis supported by the traces, not a
verified physical address mapping or a claim about every JIS row.

For each run the selector writes remained 0 at 509C, then 1 at 5118, with no
OUTF during WIDTH/Kanji/character entry. The first table-read phase occurs
between those writes. Subsequent font-copy addresses depend on already invalid
read data and must not be treated as recovered glyph addresses.

A consonant-only B experiment did not reach the glyph-read path; that is
consistent with pending Romaji input, rather than evidence of a different
Kanji-ROM access method. The experiment no longer requires every arbitrary
key to generate a glyph.

A [hardware comparison probe](README.md) is prepared and
checked on the emulator. It compares next-PC READ results with two distinct
DP values under selector 0 and 1. Hardware results are still needed to resolve
the observed conflict between generic documented READ behavior and the glyph
routine's address calculations. The probe is not executed on a physical device
by PGP or this analysis.

## Follow-up: confirmed firmware arithmetic and selector data dependency

The local experiment now stops the unmodified normal-ROM routine immediately
before its first READ. This isolates calculations that do not yet depend on
missing Kanji data. It checks 188 input cases: rows 21..7E at column 22, and
columns 21..7E at row 24 (one repeated pair). Every case reached bank 1 PC 50DD.
The formula reproducing the computed DP is:

```text
r = row - 0x21
if r >= 0x0F: r -= 7
c = column - 0x20
firstTableDP = r * 0x72 + (c >> 4) * 0x13
```

This establishes the arithmetic of the supplied normal ROM, not a physical
Kanji-ROM address map. Some swept row values represent gaps/undefined JIS rows;
the arithmetic result does not establish that those characters exist.
The 0x72 stride is six 0x13-byte blocks. Each block is consumed as a three-byte
header followed by sixteen one-byte descriptors. The low nibble of c determines
the descriptor within the block. Prior nonzero descriptor low-five-bit lengths
are incremented and accumulated to find a character's data position.

Crucially, the second selector write is not a hardcoded 1. The routine stores
the first header byte in internal K and computes:

```text
second3600Value = (firstHeaderByte & 0x04) >> 2
```

A test-only controlled experiment injects header bytes 00..07 after READ and
stops after the second selector write. All eight values produce the expected
selector and base-address arithmetic, without changing production CPU or bus
behavior. Header bits 0..1 contribute at bit positions 14..15 of X; the next
two header bytes provide a big-endian word that is added to X. These statements
describe the routine's arithmetic and do not assign a meaning to the board's
AD16/AD17 or chip-select signals.

In the no-ROM baseline, first READ returns the next opcode 34. Its bit2 is 1,
so the observed subsequent write of 1 is a consequence of invalid substitute
data. The observed 0->1 sequence must not be used as proof that real hardware
always selects that value or that it switches between the two physical chips.
The calculation of the table DP and the initial write of 0 precede this first
invalid value and remain useful independent observations.

The prepared probe is now also available as `read-path-probe.bas`, a generated
75-byte DATA/POKE loader. The local ROM successfully executes its FOR/READ/POKE,
CALL, RTN, post-CALL marker and PRINT. Synthetic tests additionally check saved
A/B/P and stack balance. Physical hardware execution remains outstanding.

## Local firmware comparison

Byte comparison of the user's existing local files confirms their earlier
recollection: both internal-ROM images are 8192 bytes and are identical.
The number of differing bytes per 16 KiB normal-ROM bank is:

| Bank | Differing bytes |
| --- | ---: |
| 0 | 34 |
| 1 | 316 |
| 2 | 1530 |
| 3 | 14322 |
| 4 | 9 |
| 5 | 0 |
| 6 | 0 |
| 7 | 5311 |

In particular, bank 1 logical 5013..527D (the currently examined glyph routine)
is identical between these two local sets, including opcodes 54 and 56 at the
read sites. This does not establish firmware identity for other units or CPU
hardware identity. It is additional reason to avoid an unsupported K-only
opcode rewrite: generic documentation and both firmware sets share the current
code pattern, while physical bus/read selection remains unverified.

## First hardware probe: user-reported result

The user returned `0,1,55,94` (decimal) from the first prepared probe:

| Selector | Probe DP | Next opcode | Hardware report (hex) | Current Studio (hex) |
| --- | --- | --- | --- | --- |
| 0 | C100 | 03 | 00 | 03 |
| 0 | C101 | 12 | 01 | 12 |
| 1 | C100 | 03 | 37 | 03 |
| 1 | C101 | 12 | 5E | 12 |

This contradicts the current emulator's next-opcode result in these tested
conditions and establishes a selector-dependent result. It does not yet prove
DP addressing because DP and next opcode were changed together. It also does
not identify a chip, total address width, or all required control conditions.
The earlier notes stating that hardware execution is outstanding describe the
state before this report; only the second experiment remains pending.

A [second fixed-site experiment](READ_MATRIX.md) now separates
DP and next-opcode/instruction-site variations, with an ordinary RAM PEEK
control. Its 123-byte BASIC loader passes synthetic tests and the local-ROM
BASIC CALL/return test. Production opcode and memory mapping remain unchanged
pending the hardware comparison.

## Second hardware result: DP-dependent READ established in tested setup

The user reported all three lines from the second experiment:

```text
0: 0,0,1,1
1: 55,55,94,94
R: 165,90
```

This matches the stated prediction. At a fixed READ instruction address, DP
C100 versus C101 changes the result; at fixed DP, the two READ sites with
different following instructions agree. Selector 0 and 1 produce distinct
sources, while ordinary BASIC PEEK still reads the A5/5A RAM sentinels.
Within these tested conditions, the hardware READ operation is therefore
DP-dependent and separate from ordinary RAM reads. It is not the generic
next-opcode behavior currently emulated. It remains unknown which physical
ROM/bank is selected and whether additional F-port or address conditions apply.

The glyph routine also uses READM/54 to internal RAM. A
[READ/READM comparison](READM_COMPARISON.md) is now prepared
and locally verified, including restoration of internal byte 20 and A/B/P and
execution through the normal BASIC caller. Its hardware result is pending.
No speculative ROM contents or physical address mapping have been implemented.

## READM hardware result and first table-data request

The user returned the READ/READM comparison:

```text
0: 0,0,1,1
1: 55,55,94,94
R: 165,90
```

Thus READ/56 and READM/54 agree for both tested DP addresses and selector
values, and ordinary BASIC RAM PEEK remains distinct. This supports a shared
machine-specific ROM-read operation for both instructions in this setup.
It does not determine the total ROM capacity exposed, chip identity, address
window boundaries or additional control requirements.

The next prepared experiment reads only selector-0 DP 0156..015B, the six bytes
needed for the 2422 table entry in the normal-ROM arithmetic. The
[small Hiragana-table reader](HIRAGANA_TABLE.md) is an
83-byte routine with a BASIC loader, locally verified through CALL and return.
Its hardware bytes are not yet available; no font pointer or placeholder table
contents have been accepted as recovered data.

## Hardware table bytes and first glyph candidate

User report: T0=0,35,90 and T1=0,29,29, for selector 0 at 0156..015B.
Using the observed normal-ROM arithmetic gives header 00, base word 235A,
preceding descriptor advances 0 and 30, hence glyph candidate 2378 with
selector 0. The current descriptor is 1D; flags 40/80 are clear. The routine
copies 15 source bytes twice, adding a zero pad after each half, so the
candidate read spans 2378..2395 (30 bytes).

This establishes the next concrete address to check from actual reported
table values, not a recovered glyph. It also shows why selector 0 must not
be hardcoded as a metadata-only bank and selector 1 as a glyph-only bank.
A [30-byte glyph reader](HIRAGANA_GLYPH.md) is now prepared,
including DATA/POKE loading and eight PRINT/ENTER steps, all verified with the
local ROM. Its physical hardware bytes and rendered glyph are still pending.
Recovered short read segments are stored only in the ignored local analysis
folder, without assigning them to either physical SC6976 chip.

## First glyph recovered and visually checked

The user returned all 30 values from the targeted selector-0 read at
2378..2395. Row 4 contained a duplicate comma and was interpreted as the four
values 28,34,33,48, preserving the requested total of 30 bytes. This
transcription assumption is retained in the local provenance metadata.

Rendering the first 15 bytes as columns of the upper 8-pixel page and the next
15 bytes as the lower page, bit0 at the top, produces a recognizable Hiragana
A (the intended character). No orientation adjustment was necessary.

This closes the first end-to-end check: actual table bytes imply selector 0
and source 2378, and the 30 bytes there decode to the expected character in
the layout inferred from the unmodified normal-ROM routine. It establishes a
working read and address-computation path for this character in the measured
setup, not a complete physical ROM map or capacity.

Raw recovered glyph bytes, provenance JSON, accumulated known read segments
and rendered PNGs are kept only under ignored `local-data/analysis/pc-1360k/`.
Firmware-derived binary/image assets are not added to the repository.
The small glyph experiment is now completed on hardware. Future work should
measure selector-1 glyphs and additional address ranges/control conditions
before assigning read selections to physical chips or extracting a full image.

## WIDTH comparison and shared display boundary (2026-10-18)

The next planned standard-ROM analysis is documented in
[PC-1360 WIDTH analysis](PC-1360_WIDTH_ANALYSIS.md). The supplied PC-1360 and
PC-1360K normal-ROM sets render the same WIDTH=2 A from normal-ROM bank 1
4723..4732, using IXL/IYS rather than READ/READM, with no writes to 3600 or FEB0.
The K Kanji path does use FEB0 and then joins the same LCD copy loop.
The earlier suggestion that FEB0 might be a universal WIDTH=2 glyph buffer
must therefore be narrowed to the observed Kanji acquisition path. In these
four A-entry windows, the 2 KiB work area received no CPU writes; conversion
and actual Kanji-read content remain outside this comparison.

## 2026-10-18: 次の実機確認 — READの上位アドレス比較

[実行手順と結果の記録形式](READ_ADDRESS_RANGE.md)を追加。
同じREAD命令位置で、0157/4157/8157/C157および237D/637D/A37D/E37Dを
selector 0/1それぞれで比較する。実機結果は未取得。
通常ROMバンクとF出力は変更せず、上位アドレスの影響だけをまず調べる。
現行Studio上でDATA/POKE、CALL/RTN、4行のPRINTと終了まで検証し、全体ビルド成功。

### 上位アドレス比較の実機結果を受領

0T=35,128,245,16、0G=241,67,240,15、1T=14,240,68,177、
1G=65,22,88,86。過去のselector 0の0157・237Dの実測とも一致。
各下位アドレスの上位2ビット4通りで値が異なり、全8アドレスでselector間も
異なる。下位14ビットだけに折り返すモデルは今回の結果と一致しない。
16ビットDP＋selectorによる読出しを作業仮説とし、次は連続データの取得と
再読一致を確認する。全容量・物理チップ対応・Fポート条件は未確定。
詳細は[上位アドレス比較](READ_ADDRESS_RANGE.md)。

## 2026-10-18: 連続8バイトと再読一致の実機確認を準備

[境界と再読の確認手順](READ_BOUNDARY_REPEAT.md)を追加。
DP 3FFC..4003をselector 0/1で読み、同じ手順を繰り返す。
1回目の16バイトと、2回目との差の個数を表示する。実機結果は未取得。
通常ROMバンク・F出力は変更しない。StudioでBASICから終了まで検証済み。

### 境界・再読確認の実機結果を受領

3FFC..3FFFはselector 0/1とも00 00 00 00。4000..4003は
selector 0が80 80 80 80、selector 1がBF 02 FD 86。
不一致数D=0で、今回の全16バイトは2回の読出しで一致した。
この限定範囲の再現性を確認。ゼロの意味や物理的なバンク境界は未確定。
[結果と解釈](READ_BOUNDARY_REPEAT.md)を記録した。

## 2026-10-18: ファイル回収用の256バイトリーダーを準備

[ブロック回収手順](READ_BLOCK.md)を追加。
初回はselector 0、DP 0100..01FFをD000..D0FFへ回収し、2回目の読出しと比較。
SUMと不一致数Dを表示し、CSAVE Mで出力バッファを保存する。
ローカルROMでBASIC経由の動作、可変パターン・1バイト破損検出を検証。
実機でのブロック取得・録音・デコードは未確認。

### 初回256バイトを実ファイルとして取得・照合

selector 0、DP 0100..01FFの256バイトを受領。SUM=7380、D=0。
ファイルサイズ・合計値と、既知の一覧表0156..015Bの6バイトがすべて一致。
回収ファイルと由来情報はlocal-dataに保存。次は同じプログラムで
2300..23FFを回収し、既知の「あ」の30バイトとの一致を確認する。
[手順と結果](READ_BLOCK.md)。

### 第2ブロックも既知の字形と一致

selector 0、2300..23FFの256バイトを受領。SUM=8079、D=0、
ファイル合計と一致。「あ」の2378..2395の30バイトも完全一致。
初期の手入力値の二重カンマ解釈も今回のバイナリと整合。
次の小規模確認はselector 1の0100..01FF。既知値0157=14と照合可能。

### selector 1のブロック回収も照合成功

0100..01FF、256バイト。SUM=30624、D=0、ファイル合計と一致。
既知の0157=14も一致。同範囲のselector 0とは256/256バイトが異なる。
両selectorで小規模回収を確認したため、次は回収の効率化を準備する。
全容量・S/T対応・Fポート条件は引き続き未確定。

## 2026-10-18: 4KiBの機械語一括回収を準備

[4KiB版の実機手順](READ_FAST_4K.md)を追加。
1回のCALLで4096バイトの保存・再読比較を行う。24ビットSUMと16ビットDを使用し、
CPU内部00..0CとP/Q・スタックを保存・復元する。
境界・桁上がり・1バイト/全4096バイトの不一致検出を自動テストで確認。
ローカルROMのBASIC経由実行と、取得済み256バイトを使う検証も成功。
初回はselector 0の0000..0FFFを回収し、既知の0100..01FFと全バイト照合する。
実機での4KiB版回収は未確認。CPUの製品READ実装やROMマッピングは変更していない。

### 初回4KiBの実機回収を照合成功

selector 0、0000..0FFFの4096バイトを受領。SUM=255966、D=0。
ファイル合計と一致し、0100..01FFの既存256バイトも完全一致。
高速版の連続読出しが従来の明示DP方式と整合した。
次は2000..2FFFを回収し、既存の2300..23FFと照合する。

### 第2の4KiBも取得済み字形と整合

selector 0、2000..2FFF、4096バイト。SUM=131607、D=0。
ファイル合計、既存の2300..23FFの256バイトと「あ」の30バイトが一致。
次はselector 1の0000..0FFFを回収し、既存256バイトと照合する。

### selector 1の高速版4KiB回収も照合成功

0000..0FFF、4096バイト。SUM=449062、D=0、ファイル合計と一致。
既存のselector 1の0100..01FFの256バイトとも一致。
両selectorで高速版を確認。取得済み計12KiB、次はselector 0の1000..1FFF。

## 2026-10-18: ユーザー指示により全域の自動回収へ変更

個別の4KiB回収を繰り返す依頼をやめ、[pc1360k-kanji-dump-01.basの一括手順](READ_ALL.md)を追加。
既存の機械語を使い、selector 0/1各64KiBを32回のCSAVE Mで連続保存する。
再読不一致なら保存前に停止。各保存に識別ヘッダーを付け、一括WAVから分割して
selectorごとに復元する補助スクリプトも作成した。
BASICの32周回・不一致停止は保存を代替するローカルROM試験で検証。
S1抽出は独立したpcwav encoder、raw decoderと人工データで照合済み。
実機のCSAVE M連続動作と全域データは録音受領後に確認する。
対象は指定方法が分かっている論理128KiB。物理全容量と残る選択条件は未確定。

## 2026-10-18: 一括録音から論理128KiBの回収を完了

全32ブロック、欠番なし、全D=0。各SUMとS1各部チェックサムが一致。
selector 0/1それぞれ65536バイトへ結合した。以前取得した全既知値と
3つの4KiBファイルも一致した。録音とイメージはlocal-dataのみに保存。

S1末尾は端数rawデータ＋最初のFFのチェックサムで、参照encoderのFF単独方式と
異なることを全32件で確認。抽出スクリプトを実機方式へ修正し、
旧方式は明示互換オプションへ分離した。参照pcwav自体は変更していない。
実機の長時間連続保存と、取得データが終端までそろうことを確認できた。
S/Tの物理割当や追加領域の選択条件は未確定。
詳細は[一括回収結果](READ_ALL.md)。

## 2026-10-18: 取得した論理ROMをStudioへ統合

pgrom v1のpc-1360kへkanji-selector-0/1（role=kanji、各65536バイト）を
追加できるようにした。隠し機種のROMセット作成画面に2つの選択欄を追加。
両方なしは従来の解析用状態として許可し、片方だけ・サイズ違い・通常1360への
追加は拒否する。ローカルのpc-1360k.pgromも作成・再読込した。

CPUのREAD/READMデータ取得をバスへ委譲し、漢字ROMを持つK機種だけ
3600=0/1とDPで読む。命令フェッチ・通常メモリ・3400とは独立。
トレースにはDPでの読出し値を記録する。未確認の選択値はFFとし、
Fポートによる制御は仮定しない。通常機種とROMなし解析の既存動作は維持。

未変更のローカルROMでWIDTH,2→漢字→Aを実行し、「あ」のLCD表示を確認。
FEB0..FECFは実機から取得した30バイトに通常ROMが入れる2つのゼロを
加えた内容と完全一致。変換キー後の動作も記録したが、変換候補の正否は未確認。
表示画像・トレースはlocal-data/analysis/pc-1360k/studio-kanji以下。

## 2026-10-18: 変換処理で使われる選択値2・3の一括回収を準備

取得済みROMによる「あ」表示の後、変換キーで3600への2・3の書込みを観測。
現行Studioは未確認の選択値にFFを返すため、これを実機の値とは扱わない。
ユーザーの指示で、物理対応の確定を待たずに2・3の全範囲を回収する方針へ。
[pc1360k-kanji-dump-23.basと手順](READ_ALL_23.md)を作成した。
既存版からは行60をFOR K=&20 TO &3Fへ変更するだけ。
抽出ツールは--selectors 2 3で番号32..63を検証・結合できる。
BASICの32周回と不一致停止、抽出・結合・欠落検出を検証済み。
実機データの取得と2・3のStudioへの組込みは未実施。

## 2026-10-18: 選択値2・3の実機データを回収

一括録音から32ブロックを抽出。欠番なし、全D=0、全S1チェックサムと
パケットの合計値が一致。各65536バイトへ結合した。
selector 2の合計7107753、selector 3の合計6170701。
2/3相互では64831バイトが異なり、0/1との比較でもすべて異なる内容。
これで0〜3の論理256KiBを取得。全64ブロックをまとめて再検証済み。
物理S/T対応とゲートアレイのビット割当は未確定。
[回収結果と比較表](READ_ALL_23.md)。
Studioへの2・3の統合は未実施。

## 2026-10-18: 選択値2・3をStudioへ統合、変換動作を確認

漢字ROMを最大4つの64KiBコンポーネントへ拡張。3600の値0..3で
READ/READMのDP読み出し先を選択する。0/1のみの既存pgromとROMなしの
解析も互換性を維持し、非連続・3つだけ等の不完全な組合せは拒否する。
ROMセット作成画面を4欄へ拡張し、ローカルpgromも13コンポーネントへ更新。

未変更の実ROMを用いるローカルテストで以下を確認した。

- WIDTH,2→漢字→Aで「あ」。従来の実機字形と一致。
- 変換キーで「亜」が表示され、FIXで確定。
- KANJI入力で「かんじ」、変換で「感じ」、もう一度変換で「幹事」、FIXで確定。
- 変換処理がselector 2/3を読み、候補の字形表示へ進む。

画面・トレースはlocal-data/analysis/pc-1360k/studio-kanji-fourに保存。
これは上記入力経路の動作確認であり、全辞書・全操作を検証したという意味ではない。
物理チップ割当や未確認のFポート条件を仮定する変更は行っていない。
