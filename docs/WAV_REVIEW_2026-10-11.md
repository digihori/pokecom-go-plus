# WAV機能の現状と段階案（方針確認用）

調査日: 2026-10-11。PGP開始点: `main / cf849bc`。実装変更は行っていない。
参照pcwav HEADは指定commit `14e1ef53596f0d8791b6d14965061f63846d11a7`と一致。
本稿の段階案は提案であり、ROADMAPの採用済み計画や実機仕様の確定を意味しない。

2026-10-12追記: 統合Tool、EncodeのProjectツリー/既存ファイル両経路、ROM CSAVE/CLOAD非連動を
ユーザーが指定した。以後の操作方針は[WAV_OPERATIONS.md](WAV_OPERATIONS.md)を参照し、
本稿のProject導線を後続候補とした提案より、この確認済み方針を優先する。

2026-10-12作者確認による更新: S2 `27`はBASIC、`37`はpassword付きBASIC。
末尾checksumは動作実績のあるpcwav方式を採用し、S2 BinaryはS1 Binaryと同じ方式を共用する。
以下の未確定/資料待ち/未対応提案は調査時点の記録であり、この作者確認と更新済み
[WAV_S1_S2_FORMAT.md](WAV_S1_S2_FORMAT.md)を優先する。PGPの実装完了・実機検証済みを意味しない。

## 1. 結論

最初はOLD Toolの「変換→確認→保存」と途中結果の保持を整え、OLD Raw Decodeを提供する。
次にOLD Analyzerの最小版を作り、信号の観測情報と耐性評価を追加する。
その基盤の上でS1共通信号→S1 Binary→S1 BASICを進める。S2は未確定仕様を解決する別の段階にする。
Intel HEXは並行可能な共通交換機能。実時間音声I/OとCLOAD/CSAVE連携は別案件として方針確認する。

## 2. 実装済み（コード確認）

| 機能 | 現在のコードと実際の範囲 |
|---|---|
| OLD payload | `core/.../wav/OldPayloadEncoder.kt` / `OldPayloadDecoder.kt`: BASIC 02/12、Binary 62。filename、metadata、body checksum、終端、宣言長、16-bit addressを検証 |
| checksum | `OldTransferChecksum.kt`: nibble swap、8-byte checksum挿入、80-data-byte累積reset、end-around carry |
| signal | `OldSignalEncoder.kt`: 8 kHz、19-unit framing、2.048秒leader、任意raw列のEncode。`OldSignalDecoder.kt`: raw byteごとのPCM範囲とlow/high nibble同期位置 |
| WAV container | `PcmWavReader.kt`: PCM、8/16-bit、mono/stereo、未知chunk、奇数長pad処理、RIFF外余剰warning。`PcmNormalizer`: signed 16-bit相当、stereo左右平均。Writerの出力は8-bit mono |
| 統合API | `OldWavCodec.kt`: payload→signal→WAVと逆経路。成功時は中間結果、各段階のdiagnosticを保持 |
| Studio | `desktopApp/.../Main.kt`: Toolsの専用window、filename、12 type checkbox、BASIC source→WAV、単一連続領域.dmp→WAV、WAV→.bas/.dmp。Project/ROM/Session不要 |
| BASIC adapter | `desktopApp/.../basic/DesktopOldBasicTransferAdapter.kt`: FF ... FF ↔ ... F0。外枠・終端を確認。caller側でtokenize/detokenize |
| 解析用model | `WavCodecModel.kt`: WAV/PCM/raw/logical/textの座標、range、mapping、severity/stage/code/range/details付きdiagnostic。不変コピー所有 |

Binaryのreserved metadataはDecode結果に保存し、既定値との差はwarning。未知filename byteは`?`とwarning。
BASIC 12の対応はtypeの識別・生成であり、password文字列を入力・抽出するAPIはない。
Decodeした.basだけではtype 12やreserved metadata等を再現できない。UIもpasswordの実値を扱わない。
CoreにファイルI/O、UI、デバイス、時刻APIやスレッド所有を追加する必要はない。

## 3. 文書との差・現在の不足

1. `WAV_OLD_FORMAT.md` §3.1は12を「初期UIでは生成しない」としているが、現在のUIには生成checkboxがある。§7の記述とコードが新しい。
2. 統合APIの中間結果保持は**成功時のみ**。`CodecResult.Failure`はdiagnosticだけで、payload失敗時のwav/pcm/rawを返さない。文書の「中間結果保持」を全失敗経路まで保証するものと読んではいけない。
3. payload Decoderも最初の致命的errorで停止し、途中logical bytesや構造を返さない。filename/reservedのwarningが後続body errorで失われる経路もある。
4. Core診断には位置とchecksum expected/actualがあるが、UIの`displayText()`はstageとmessageだけ。severity/code/range/detailsを表示しない。
5. Encode成功時はdiagnosticを表示せず、Decode warningは保存成功後にだけ表示。保存をcancelすると新しい結果のsummaryが残らない。変換結果を確認する独立操作・保持modelはない。
6. 入力/保存先のfull path、変換前後の長さ、Binary end address、WAV metadata、previewを並べる画面はない。statusは選択・scroll可能だが、一つの文字列に上書きされる。
7. filename既定値はPROGRAM。空欄の場合だけ入力ファイル名へfallbackする。正規化結果は返るが、切詰め/文字除外をwarningとして通知しない。
8. Readerは1〜32000 Hzを受理し、Signal Decoderは4000〜32000 Hzを受理する。受理範囲と復号保証範囲は別。44.1/48 kHz録音は現状拒否される。
9. 信号Decodeは固定差分threshold 5120、整数sampleRate/500の間隔、同期条件を使う。適応threshold、速度追従、信号unit/edge/levelのtrace、複数streamの分離はない。rawのPCM範囲は復号に使った区間であり、19-unit全frameの厳密な境界保証ではない。
10. Raw Decode UI、Analyzer、S1/S2 WAV codec、Intel HEXは未実装。S1/S2文書の対応表はpcwav側の対応表であり、PGPの実装表ではない。
11. adapter単体は外枠だけを検証する。文書の「既存Tokenizer/Detokenizerで検証」はStudioの全経路として成立している。

## 4. 文書にある未実装の構想

- ROADMAP / OLD仕様: Raw Decodeのhex表示・保存、WAV metadata・同期位置、payload解釈と独立した信号結果。
- ROADMAP / OLD仕様: 波形、zoom/pan/cursor/範囲、min/max envelope、sample/edge、level/W0/W1/leader/sync/byte overlay、元channel比較、双方向選択、構造/文字、候補比較、個別exportを備えるAnalyzer。
- ROADMAP / S1/S2仕様: S1共通信号、Binary 67、BASIC 07、120-byte chunk、半角カナ、BASIC物理trailer。S2 BASIC 27/37、FE token、1F行参照、CP932。
- ROADMAP / MACHINE_CODE_FORMATS / PORTING_NOTES: Intel HEXによるaddress付き交換。単一連続領域制約はWAV側で引き続き適用。
- ROADMAP: 実機録音fixtureによる許容範囲の確定。
- OLD仕様: マイク/スピーカー実時間I/Oは将来platform adapterとして言及されるが、初期対象外。具体的なUI・作業計画はない。
- PORTING_NOTES / ARCHITECTURE: 参照Pokecom GOのROM内CLOAD/CSAVE hookは初期Coreへ移植しない。実時間のエミュレータ連携は採用済みWAV作業とみなさない。

## 5. 未確定仕様と追加資料

| 論点 | 必要な根拠／判断 |
|---|---|
| S2 type 27/37 | 各typeを出す機種・モードの一次資料と実機保存WAV。名称や選択既定値を類推しない |
| S2 tail checksum | pcwav S2Encode自身が暫定と記載。複数body長・内容の実機fixtureで計算を照合。Decodeが通るだけでは確定しない |
| S2 Binary | pcwavに実装なし。type/header/checksum/framingの一次資料か実機fixtureが揃うまで未対応 |
| S1 checksum/trailer | header/chunk/tailの計算対象と120境界、論理終端と物理FFを独立確認。参照Decodeはchecksumを比較しない経路がある |
| OLD BASIC 12 | type対応と実機password保護の意味・保存情報を区別。実機source、設定、WAVで確認 |
| filename/reserved | 非標準値の実機での意味、.bas/.dmp export時の情報損失を許容するか、付随metadata保存が必要か |
| 録音許容範囲 | rate/振幅/DC/noise/速度差/stereo位相等の検証条件を固定。sample rate受理だけで実機互換と表示しない |

既存S1 BASIC tokenizer/detokenizer（PC-1350、PC-1261、PC-1401）には行構造と機種別語彙がある。
PC-1350ではFE+A1..DF半角カナが実装済み。PC-1401はallowKana=falseなど機種差がある。
FF外枠を持つエミュレータimageからWAV bodyへ専用adapterが必要で、全S1をPC-1350語彙に統一しない。
S2のFE token、1F参照、CP932 codecは既存S1処理をそのまま再利用できない。共通source model、診断、行処理の再利用可否を個別評価する。
CP932変換をcommon化する場合もJVM Charset APIをcommonMainへ持ち込まない。

## 6. 新しく提案する候補（未採用）

- Studioの変換結果snapshot: 入力情報、生成物、metadata、diagnostic、保存状態を一つの不変結果として保持。入力変更後の旧結果を明示する。
- 保存cancel/保存失敗後の再保存、filename正規化preview、失敗位置/expected/actualの表形式表示。
- stageごとの解析結果model: container成功、signal成功、payload検証成功を別状態で保持。errorを正常Programへ変換しない。
- metadata付随export: type/reserved等を残す解析report（JSON等）の要否を確認。正常Programの標準形式変更はしない。
- 重い処理のhost側background実行・取消・長い入力の描画上限/virtualization。Coreはスレッドを作らない。
- 適応threshold、DC補正、速度追従、44.1/48 kHz向けresampling。まず合成劣化試験で必要性を確認し、選択可能な解析設定として比較する。
- Project/アセンブル成果物からaddress付きimageを渡す導線。手動.dmp交換は既に可能であり、自動連携はWAV基盤後の別提案。アセンブラ自体の残課題は含めない。

## 7. 優先順位・依存関係・完了条件

| 段階 | ユーザー向け改善 | 内部的な正確性・依存 | 完了条件 |
|---|---|---|---|
| A: OLD Toolの結果確認（最優先） | 入力→変換→preview→保存。type/name/password flag/address/長さ/保存先、warning/errorを常時確認 | 結果snapshot、構造化診断表示。codecの挙動は維持 | BASIC 02/12、Binaryで成功/失敗/cancel/再保存をGUI確認。位置・checksum値を表示。保存前に確認可能 |
| B: OLD Raw Decode | metadata、offset付きhex、同期位置、raw保存。payload失敗後も観察可能 | Aの結果保持。WAV→PCM→signalをpayloadから独立実行。失敗段階以前の結果を保持 | checksum破損/未知typeでもrawを表示・export。無信号でもmetadata/PCMを保持。未検証表示と直接ロード禁止 |
| C: Analyzer最小版（OLD） | 波形＋raw、zoom/pan/cursor/範囲、byte選択→PCM強調、diagnostic移動 | B、min/max envelope、sample表示、既存mapping。まず表示対象はOLDのみ | 長いWAVでもpulseを消さない。PCM/rawの相互選択、途中失敗を保持。候補解釈は任意操作で、正常判定を自動断定しない |
| D: Analyzer観測拡張＋耐性評価 | 元channel/mono比較、level/edge/W0/W1/leader/sync/byte overlay、構造/文字、個別export | C。実際のDecoderが使う判断trace、header/checksum/terminator mapping、部分payload結果。推測overlayを実測と混ぜない | sample→signal→raw→logical→文字の対応、checksum根拠を確認。劣化fixtureの成功/失敗条件を固定 |
| E1: S1共通信号 | Raw DecodeでOLD/S1 signal選択・結果比較 | B以降で可能。16-unit framing/4.096秒leaderを独立実装し実証。S2 payload対応とは別 | raw/PCM固定vector、同期・欠損試験、物理trailer位置。OLDとS1の対応範囲を表示 |
| E2: S1 Binary 67 | .dmp入出力、name/address/body preview | E1、S1 checksum、120-byte境界、footer、連続領域制約 | 1/119/120/121に加え複数chunk・address端、破損checksum/欠損/余剰を検証。参照一致と実機確認を別表示 |
| E3: S1 BASIC 07 | 機種語彙選択、半角カナ、source保存 | E1、E2の共通部、既存tokenizerとbody adapter。BinaryとBASICの最終full chunk規則を混同しない | 119/120/121および240境界、quote/REM/kana、tail/物理FF、機種差を確認 |
| F: S2 BASIC | type別候補、FE token/1F参照/CP932 | E1/E3、typeとtail根拠、S2 program codec。資料不足なら解析・暫定表示まで | 27/37、ON行参照、kana/漢字、各checksum/終端をfixture確認してから互換対応完了 |
| 独立: Intel HEX | address付き交換 | 共通memory image parser/writer。AやBの必須依存にしない | checksum、address範囲、重複/複数領域を検証し、WAVではgapを拒否 |
| 後続: 複数stream/実機耐性 | stream一覧・選択、録音対応範囲拡大 | Dの評価基盤。stream単位の座標とdiagnostic、leader再探索 | 二連結stream、noise/欠損/誤検出の分離試験。誤った正常Program判定を抑制 |

依存の主線はA→B→C→D。S1はBから着手でき、Analyzer完成を待つ必要はない。
fixture収集と権利確認は全段階で進める。Dで観測可能にしてから耐性アルゴリズムを変更する順を推奨する。
S2 Binary、音声再生/録音、CLOAD/CSAVE連携はこの表の実装対象に自動追加しない。

## 8. 検証の現状と不足

既存wavテスト9ファイルでは固定payload/PCM vector、checksum carry/reset、境界長、型の不変性、
未知chunk/pad、8-bit mono/16-bit stereo、破損/余剰、WAV往復を確認する。
Signalの耐性試験は8 kHz合成、16 kHz sample複製、先頭100 ms無音＋振幅1/2、末尾frame欠損が中心。
4000〜32000 Hz全域、一般的な非整数周期rate、速度変動、DC offset、noise、位相反転stereo、複数streamの保証はない。

検証結果は次の三種類に分離する。

1. 合成自己往復／劣化試験: 再現性のある生成条件と期待byte・位置・失敗段階を固定。
2. pcwav参照一致: raw/PCM/containerを独立照合。独自に固定したvectorと相互Encode/Decodeを使い、自己往復だけに依存しない。
3. 実機互換: 対象機種、保存/読込方向、録音経路、rate、レベル等を記録。参照一致では代替しない。

必要fixture: OLD 02/12/62の機種別実機保存と実機読込、name/reserved異常値、checksum境界、
S1/S2文書§8の境界と文字・type・trailer。録音は権利者/公開許可/由来を確認し、ROMやROM由来検証dataはcommitしない。
公開不能fixtureはローカル確認の証跡と合成の公開testを分離する。

今回の基本チェック: 指定JDK 21で `build :core:desktopTest` 成功（573 tasks、16 executed、557 up-to-date）。
既存テストの多くはup-to-dateであり、全件を新規再実行したという報告ではない。GUI操作検証は今回は未実施。

## 9. 方針確認事項

次の実装範囲としてA+B（OLDの結果確認＋Raw Decode）を推奨する。
AnalyzerはCの最小版から別段階で進める。S1はその後、またはB完了後に独立着手する。
S2は実機仕様の確定を完了条件とし、資料が揃うまで通常の互換Encode機能として公開しない。
Intel HEX、Project導線、実時間I/Oはそれぞれ必要性と範囲を改めて決める。

## 10. 主な根拠

- PGP: `AGENTS.md`、`docs/ROADMAP.md` WAV項、`docs/WAV_OLD_FORMAT.md`、`docs/WAV_S1_S2_FORMAT.md`、`docs/PORTING_NOTES.md` OLD/S1/S2項、`docs/ARCHITECTURE.md` §19.2。
- PGP: `core/src/commonMain/kotlin/com/digihori/pgp/core/wav/`、対応commonTest、Desktop `Main.kt`の467〜571行および3839〜3943行、`basic/DesktopOldBasicTransferAdapter.kt`とtest、各S1 tokenizer。
- pcwav指定commit: `src/PCWAV/RawDecode.pm`、`WavWriter.pm`、`Basic/S1Encode.pm`、`Basic/S2Encode.pm`（141行の暫定tail checksum記録）。外部repositoryは読み取り専用。
