# WAV Toolの実装・確認履歴

2026-10-22に現行ガイドから分離した計画・検証履歴です。
各節の「現在」「未実装」「後続」は記録時点を表し、現在の対応範囲を示しません。
現行の説明は[WAV_OPERATIONS.md](WAV_OPERATIONS.md)を参照してください。
元の本文を保持し、当時の未確認事項を後から完了扱いにはしていません。

## S1／S2対応の再開（2026-10-20）

ユーザー指示によりs1bin／s1basic／s2basic対応を順番に進める。
[進捗記録](WAV_S1_S2_PROGRESS.md)の段階1で、仕様修正と28件の合成vector整理を完了した。
段階2の共通信号、段階3のs1bin、段階4のs1basicに続き、段階5のs2basicとStudio接続・合成検証まで完了した。段階6のUI・診断・操作ガイド・OLD回帰も実施。実機往復は別途継続する。
S1／S2 binaryのWAV作成、Decode、`.dmp`／Intel HEX保存を利用できる。実機往復は未確認。
S1／S2 BASICは対象機種を選択して`.bas`入出力に対応。本文の2026-10-12の
後回し方針は当時の経緯として保持する。既存の非連動・Studio専用方針は継続する。

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
