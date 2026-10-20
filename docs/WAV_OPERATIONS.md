# WAV操作方針と現在の実装

日常の手順・対応表・診断と実機確認は[操作ガイド](WAV_TOOL_GUIDE.md)を参照。

2026-10-11作成、2026-10-12更新。統合Tool、Encodeの二つの入力経路、ROM CSAVE/CLOAD非連動、Decodeからの直接ロード対象外はユーザー確認済み。
基本操作は実装開始時に採用済み。本文は全体方針を示し、現在の実装範囲と後続事項は§10で区別する。
実装状況と根拠は[調査結果](WAV_REVIEW_2026-10-11.md)を参照する。

## S1／S2対応の再開（2026-10-20）

ユーザー指示によりs1bin／s1basic／s2basic対応を順番に進める。
[進捗記録](WAV_S1_S2_PROGRESS.md)の段階1で、仕様修正と28件の合成vector整理を完了した。
段階2の共通信号、段階3のs1bin、段階4のs1basicに続き、段階5のs2basicとStudio接続・合成検証まで完了した。段階6のUI・診断・操作ガイド・OLD回帰も実施。実機往復は別途継続する。
S1／S2 binaryのWAV作成、Decode、`.dmp`／Intel HEX保存を利用できる。実機往復は未確認。
S1／S2 BASICは対象機種を選択して`.bas`入出力に対応。本文の2026-10-12の
後回し方針は当時の経緯として保持する。既存の非連動・Studio専用方針は継続する。

## 1. 入口と作業単位

WAV ToolはStudio専用機能とする。Encode/Decode/Raw Decode/Analyzer、Projectツリーからの
起動導線をPlayerには搭載しない。共通codecの配置と製品UIの搭載範囲は分けて扱い、
Coreのcodecが存在することをPlayer機能追加の根拠にしない。

2026-10-12方針: Playerの作業を優先してS1/S2実装は後回しにする。Tool冒頭に
「S1/S2対応は開発中です。S1/S2のWAV変換にはpcwavツールを使用してください。」と表示する。
作者確認済みのS1/S2仕様は維持する。

StudioのTools → WAV Toolから専用windowを開く。Projectやエミュレータを起動していなくても使える。
入口は「WAVを作成」「WAVを開く」の二つとする。Raw DecodeとAnalyzerは、開いたWAVの結果を見る表示として扱う。
表示を切り替える際にファイルを開き直したり、同じ条件で再Decodeしたりしない。

一つのwindowで一つの入力を扱う。結果は入力と実行時の設定に結び付けて保持する。
将来の複数streamは、その入力内のstream一覧から選択する操作とする。複数ファイルの一括変換は別候補。

## 2. WAVを作成する流れ

1. プロジェクトツリーで入力を指定して「WAVを作成…」を選ぶ、またはToolの「既存ファイルを開く…」でBASIC source/address付きmemory imageを指定する。
2. 転送形式OLD/S1/S2を選ぶ。OLD／S1／S2のBASIC・Binaryに対応。S1／S2ではProject由来の機種情報から機種別token語彙を使用する。
3. ポケコン側filename、形式、必要な設定を確認する。入力ファイル名をfilenameの初期候補とし、実際に送る正規化名を表示する。
4. BASIC sourceまたはBinary address範囲・長さを確認して「変換」を押す。
5. 結果を確認する。BASIC/転送type、filename、address、logical/raw長、WAV形式・時間、warning/errorを表示する。
6. 「WAVを保存…」で保存先を選ぶ。保存成功後はfull pathと保存状態を表示する。cancel/失敗しても結果を保持し、再保存できる。

OLD／S1／S2と、BASICパスワードなし／あり・マシン語を明示選択する（S2はBASICのみ）。
冒頭のtype一覧・互換性注記、ファイル名下のtype checkboxは表示しない。
入力拡張子で種別を自動決定せず、選択した種別と入力が一致するかだけ確認する。
保護付き形式はOLD 12／S1 17／S2 37として保持する。password文字列の新規設定は別の機能である。
Binaryの複数領域/gapは変換前に説明して拒否し、addressを書き直したりzero-fillしたりしない。

生成WAVも「波形」「raw」「内容」の表示で検査できるようにする。
通常の作成操作にはrawの手編集や任意raw→WAVを含めない。

## 3. WAVを開く流れ

1. 「WAVを開く…」で入力を選ぶ。
2. containerを読み、sample rate/channel/bit depth/時間を表示する。ここまでの結果は信号Decodeが失敗しても残す。
3. 対応する信号方式で解析する。初期はOLDのみ。将来は「候補を探索」またはOLD/S1共通信号の指定を選べる。
4. raw byte列を復元し、検出byte数、同期位置、信号のwarning/errorを表示する。
5. 対応するpayload候補を検証し、type/filename/address/長さ/checksum/終端の根拠を表示する。
6. 選択した形式でsourceへ変換する。現在のOLDは共通token処理。将来のS1/S2ではtypeだけで機種や語彙を確定しない。Binaryはaddress付きimageとして確認する。
7. 出力を選んで保存する。BASIC source、memory image、raw bytes、解析reportは別の保存操作とする。

初期の「解析」は標準設定で実行し、将来の詳細設定変更時は「再解析」を明示的に行う。
自動探索は候補と検証結果を提示し、実機形式や互換性を断定しない。
複数候補が成立する場合はユーザーが選ぶ。無理に一つの正常Programへ絞らない。

## 4. 結果を見る画面

上部に入力・解析条件・summary、下部に結果を置く。診断一覧はどの表示からも確認できる。

| 表示 | 主な内容と操作 |
|---|---|
| 内容 | 候補一覧、header/name/address/body/checksum/終端、BASIC sourceまたはBinary内容、各検証状態 |
| raw | offset付きhexとbyte数。rawを保存。常にpayload検証状態を併記。同期sampleの詳細一覧は表示しない |
| 波形 | PCM、zoom/pan/cursor/範囲選択、元channel/mono、信号判断overlay。選択範囲をrawや内容へ連動 |
| 診断 | severity、失敗段階、位置、期待値/実測値、説明。項目を選ぶと対応する結果・波形位置へ移動 |

Analyzerはこの波形と内容の連動表示を段階的に充実させる。最小版でもrawとPCMの位置対応を優先する。
表示に使う部分結果と、通常保存に使える検証済み結果を区別する。

## 5. 成功・途中成功・失敗と保存

「WAVを読めた」「rawを復元した」「payloadを検証した」「BASIC文字変換ができた」を別々に表示する。
一つの緑色の成功表示で全工程を代表させない。参照互換/実機確認済みの情報も別に表示する。

| 到達状態 | 使用可能な操作 |
|---|---|
| container失敗 | 診断確認・report保存 |
| PCMまで成功、signal失敗 | metadata/波形/診断確認・report保存 |
| rawまで成功、payload失敗 | raw表示・保存、波形解析、別候補/設定で再解析 |
| payload検証成功、BASIC文字変換失敗 | raw/logical解析export・診断確認、語彙選択を変更して再変換 |
| Program変換成功 | 上記に加え.bas/.dmp保存（Intel HEX対応後は選択可能） |

raw・途中logical bytesは「解析用・未検証」を明示してexportする。通常Program保存や直接ロードへ流さない。
warningがある場合は確認できる状態で保存を許可し、内容に関するerrorがある場合は通常Program保存を無効にする。
警告のたびに追加確認dialogを重ねず、保存前の結果画面で判断できるようにする。
.bas/.dmpではtypeやreserved等が失われる場合、その点を保存説明へ表示し、必要ならreportを個別保存する。

## 6. 入力変更・取消・再実行

- 表示切替や保存は再変換を行わない。出力は実行済み結果から取得する。
- 機種/形式/filename/解析設定を変更すると「設定変更あり・再実行が必要」と表示する。旧結果は旧設定付きで残し、新設定の結果に見せない。
- 再実行後は新しい実行結果に置き換える。失敗した場合もその実行の部分結果・診断を表示する。
- 処理取消では直前の完了結果を保持し、取消の処理条件から生成物を保存しない。
- 新しい入力を開く際、未保存の生成WAV/Program結果がある場合のみ破棄確認を行う。閲覧だけの結果には確認を重ねない。
- 保存cancelは結果を変更しない。保存失敗は変換失敗と別の診断として扱う。
- 重い処理はStudio側で実行・取消を管理する。Coreはスレッドを作成・所有しない。

## 7. Encodeの二つの入力経路

両経路をEncodeの基本操作として用意する。Project連携を後続だけの候補にはしない。

| 経路 | 操作案 |
|---|---|
| プロジェクトツリーから指定 | 対象項目の「WAVを作成…」で統合Toolの作成画面を開き、入力を設定する。Projectの機種を初期候補にし、入力の由来と機種・形式を確認する |
| 既存ファイルを指定 | Tools → WAV Tool → WAVを作成 →「既存ファイルを開く…」。Project外のファイルも使え、対象機種・形式を指定する |

どちらも同じ設定→変換→結果確認→保存へ接続する。入力の由来（Project名/項目、ファイルpath）を表示する。
Projectから指定しただけで変換・保存・ロードを実行しない。外部ファイルを自動的にProjectへ取り込まない。
Toolが既に開いている場合は同じwindowへ入力を渡し、未保存結果の扱いは§6に従う。
初期の入力対象はBASIC sourceとaddress付きmemory image。アセンブリsourceを直接指定する場合の
成果物選択・build要否、未保存エディタ内容と保存済みファイルの扱いは、対象項目の詳細設計で決める。
アセンブラの機能改善はこの操作方針に含めない。

ユーザー方針により、WAVをDecodeしながら、またはDecode結果からエミュレータへ直接ロードする経路は対象外とする。
Decodeは結果確認とファイル保存までで完結させる。エミュレータへロードする場合は、
保存したBASIC source/memory imageを既存の別操作で指定する。
Decode結果の「Projectへ追加」は未採用の別候補であり、直接ロードの代替導線として自動追加しない。

## 8. 実時間音声の位置付け

WAVファイルの作成/Decodeとスピーカー再生/マイク録音は別機能とする。
将来、再生は作成済みWAV結果から、録音は確定した録音結果から「WAVを開く」流れへ接続できる。
録音start/stop、device選択、level、再生停止などの操作は、音声I/Oを採用する時点で別途決める。
ユーザー方針によりエミュレータROMのCSAVE/CLOADとは連動しない。ROM hookの追加や
CSAVE/CLOAD操作によるTool起動・自動変換・転送は本機能の対象外とする。

## 9. 確認済み方針と残る設計

1. 専用WAV Toolへまとめ、入口は「作成」「開く」、Raw Decode/Analyzerは結果表示として統合する。
2. すべて「入力→設定→変換/解析→結果確認→保存」を基本にする。
3. Encodeはプロジェクトツリーからの指定と既存ファイル指定の両経路を用意する。
4. エミュレータROMのCSAVE/CLOADとは連動しない。
5. WAV Decodeからエミュレータへの直接ロードは対象外。結果確認・保存と、既存のロード操作を分ける。
6. WAV機能はStudio専用とし、Playerには搭載しない。

1・3・4・5・6は2026-10-12にユーザー確認済み。2と本文の操作詳細を基に、画面配置、
Projectツリーでの対象項目と起動操作、未保存sourceの扱い、各段階の実装範囲を具体化する。
音声I/Oは今回の確認によって採用されたものではない。

## 10. 最初の実装（2026-10-12）

Studioの統合WAV Toolに、Projectツリーの選択項目からの「Create WAV…」と、Tool内の
「Create WAV…」「Open WAV…」を実装した。初期対象はOLD 02/12/62、入力は保存済み`.bas`/`.dmp`。
Project機種に対応する形式を初期値とし、非OLD形式は暗黙変換せず形式変更を求める。アセンブリsourceの直接Encodeは行わない。

変換と保存を分離し、未保存結果の置換・Tool終了確認、設定変更表示、host側background処理と取消を実装。
DecodeはPCM/raw/payloadの成功結果を保持し、payloadが失敗してもraw exportと波形を利用できる。
内容/raw/波形は同じ結果を共有する。診断はseverity/stage/code/range/detailsを表示し、PCM/raw位置へ移動できる。

Analyzer最小版はmono正規化波形、min/max envelope、拡大sample、zoom/pan、cursor、
long pressによる範囲終点、nibble sync位置、raw行→波形cursorとcursor→rawページの移動を提供する。
rawは256-byteページ、内容previewは16000文字まで（保存は全文）。
Raw欄はoffset付きhex byte列に絞り、byteごとのLOW/HIGH同期sample一覧は表示しない。
同期情報は内部に保持し、波形markerと位置対応に使用する。
詳細なsignal trace/ON-OFF/W0-W1/leader overlay、元channel比較、構造/文字の完全mapping、
部分logical解析、候補比較、複数streamは後続。
2026-10-20追加：S1/S2 binaryと16-bitのIntel HEX入出力は段階3で対応した。

初期操作: 入力指定→形式/転送名設定→ConvertまたはAnalyze→結果確認→Save WAV/Save program。
rawと解析reportは別のexport。Decodeにロード操作はなく、PlayerのUIも変更しない。

形式選択UIは機種4個のボタンからOLD/S1/S2へ変更。Project機種のbasicDialectを初期値に使い、
OLD／S1／S2ともBASICとバイナリーに対応する。`.dmp`／`.hex`／`.ihx`入力とWAV解析に対応。
S1／S2の`.bas`ではProject由来の機種情報を使用し、転送名を印字可能ASCIIの7文字以内にする。Project指定はその機種を保持する。
S2 BASICは通常type 27を使用し、必要な場合のみ保護付き形式のtype 37を選ぶ。パスワード文字列の設定ではない。既存ファイル指定は選択中の形式と対象機種を維持する。
Decode成功後は「プログラムを保存」で`.dmp`、「Intel HEXを保存」で`.hex`を保存できる。
S1／S2 BASICのDecode成功時は「プログラムを保存」で`.bas`を保存できる。文字変換のbyte一致も確認する。
信号・payloadが破損している場合やテキストが全byteを保持できない場合はrawとreportを保存して診断を確認する。

### 波形ビューの表示と移動

2026-10-12ユーザー操作確認: BASIC Encode→実機ロード→実機セーブ録音→WAV Decodeを確認済み。
Encodeは既存ファイル指定とProjectツリー指定の両経路で動作。機種・録音条件の詳細は未記録。

生成したWAV結果はsample hold（水平保持＋垂直edge）、開いたWAVはsample間を結ぶ表示を初期値にする。
録音の由来や波形種別は自動断定せず、表示方法はcheckboxで切り替えられる。平滑化・再サンプリングは行わない。
縮小時はどちらもmin/max envelope。0基準線、signed 16-bit相当の振幅目盛、ms/s時間目盛、
全体min/max/DC値、cursor sample値を表示する。拡大時の0交差は緑のmarkerで示す。

振幅の補助threshold線は表示用の任意値であり、Decoderの判定値とは区別する。
「OLD decoder difference / threshold」で実際の`x[n] − x[n−tap]`と固定threshold±5120を確認できる。
PCMは青で常時残し、差分は橙のoverlayとする。振幅guideは紫。周期がtapと一致する区間では差分が0となることを説明する。
0交差だけでDecodeが成立すると表示しない。差分値は16-bit振幅範囲を超えるため±65536の軸を使う。
差分の開始位置・tap・thresholdはCoreのinspection APIから取得し、Decoderと共通の差分計算を使う。
色はPCM（青）、差分/差分threshold（橙）、振幅guide（紫）、0交差（緑）、cursor（赤い縦線）、
nibble同期点（上端の紫marker）。header/body/checksumによる色分けではない。

水平scrollbarは全sample列に対する表示範囲を表す。zoom後にthumbをdragして左右へ移動する。
波形上ではトラックパッドの横scrollとShift+wheelでpanし、通常の縦scrollはwindowの上下移動に使う。
zoomは表示中心を保持し、Fitで全体表示へ戻る。
Zoom/左右移動/Fitのボタンは波形と水平scrollbarの下へ配置する。
