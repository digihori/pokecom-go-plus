# Roadmap

このロードマップは方向性を示すものであり、リリース日を保証するものではない。
各段階は小さな動作確認とテストを完了してから次へ進む。

## Technical Preview 0.1公開準備

PC-1245／1251ファミリーの基本機能を試せる最初の公開版を目指す。
詳細な公開条件と確認状況は[Technical Preview公開チェックリスト](PUBLIC_RELEASE_CHECKLIST.md)で管理する。

- [x] PC-1245／1251ファミリーの基本実行環境を実装する
- [x] macOS／Windows／LinuxのCIを用意する
- [ ] Windows／Linuxで手動スモークテストを実施する
- [x] 新規clone環境でREADMEのビルド手順を検証する
- [x] コード、資産、依存関係の権利と由来を確認する
- [x] Git履歴にROM、秘密情報、非公開データがないことを確認する
- [x] Technical PreviewのRelease Notesを作成する
- [ ] リポジトリをPublicへ変更する

## Technical Preview 0.2

PC-1350／1360、ROM Library、新しいStudio UI、Project／Debugger改善、Assembly Workspace初版を含む
早期評価版。公開条件と結果は[0.2 release checklist](RELEASE_CHECKLIST_0.2.0-alpha.1.md)で管理する。

## Phase 0: プロジェクト基盤

- [x] 構想書とアーキテクチャ文書を作成する
- [x] KMP CoreとCompose Desktopのビルド骨格を作成する
- [x] ライセンス、リポジトリ規約、CIを設定する
- [x] Emulator Coreの公開APIと責務を定義する
- [x] 最初の対象機種をPC-1245に決定し、初期機種仕様を整理する
- [x] Pokecom GOから採取するGolden Test Dataの形式を決める
- [ ] Pokecom GOおよび過去資産の由来を確認する

## Phase 1: Headless PC-1245

- [x] CPU状態モデルを定義する
- [x] SC61860の最小命令実装を移植する
- [x] PC-1245のメモリマップを実装する
- [x] ROMを`ByteArray`として読み込む
- [x] ROMをコンポーネント化し、PC-1245 Flat 32/64KiB Importerを実装する
- [x] PC-1245の物理ROM別8KiB＋16KiB Importerを実装する
- [x] `.pgrom` v1のmanifest検証とDesktop ZIP入出力を実装する
- [x] `reset`、`step`、`runCycles`を実装する
- [x] CPU、RAM Snapshotを実装する
- [x] LCD Snapshotを実装する
- [x] PC-1245キーマトリクスと公開キー入力APIを実装する
- [x] SC61860 I/O命令をPC-1245キーマトリクスへ接続する
- [x] SC61860の絶対分岐・呼出し・内部RAMスタック命令を移植する
- [x] SC61860の相対分岐命令を移植する
- [x] SC61860の基本レジスタ・zero/carry命令を移植する
- [x] SC61860の8bit即値演算・比較命令を移植する
- [x] SC61860のA-P間8bit演算・比較命令を移植する
- [x] SC61860の内部RAMポインタ・DPロード命令を移植する
- [x] SC61860の単byteメモリ転送命令を移植する
- [x] SC61860のX/Yインデックス・メモリアクセス命令を移植する
- [x] SC61860のAレジスタcarry経由シフト命令を移植する
- [x] SC61860の16bit加減算命令を移植する
- [x] SC61860の内部RAMブロック転送・交換命令を移植する
- [x] SC61860のDPブロック転送・交換命令を移植する
- [x] SC61860のブロック塗りつぶし命令を移植する
- [x] SC61860のpacked BCD加減算命令を移植する
- [x] SC61860の複数byteニブルシフト命令を移植する
- [x] SC61860のLOOP命令を移植する
- [x] SC61860のCASEテーブル分岐命令を移植する
- [x] SC61860のレジスタ・スタック補助命令とaliasを移植する
- [x] SC61860のDP即値論理・テスト命令を移植する
- [x] SC61860のF・Control出力ポート命令とI/O通知境界を移植する
- [x] SC61860のTEST命令をdivider latchとPC-1245 BREAKへ接続する
- [x] SC61860のMVWP内部サブルーチン型ブロック転送命令を移植する
- [x] SC61860のCUP・CDN X入力カウント命令を移植する
- [x] SC61860のWAIT命令を決定論的なサイクル消費として実装する
- [x] PC-1245 I/Oを集約し、OUTCをLCD有効状態へ接続する
- [x] PC-1245の論理Buzzerとプラットフォーム非依存Audio Snapshotを実装する
- [x] ローカル実ROMによるPC-1245ヘッドレス起動スモークテストを追加する
- [x] SC61860 dividerを命令サイクルから決定論的に生成する
- [x] Golden Test Data v1のPGP側JSON再生・比較基盤を実装する
- [x] 実ROMからPGP側Golden Snapshotを採取するエクスポーターを実装する
- [x] Pokecom GO既存Save State JSONのGolden Expectation Importerを実装する
- [x] Pokecom GO SharedPreferences XMLから`PREF_SC`を抽出するブリッジを実装する
- [x] Pokecom GOの固定cycle期待結果と明示的なDesktop integration testで比較する

## Phase 2: macOS最小Pokecom GO Studio

- [x] 実時間をサイクル予算へ変換する共通Plannerを実装する
- [x] PC-1245従来形式ROM選択とSession生成
- [x] Run / Pause / Reset / Step
- [x] PC-1245 LCD Snapshot表示
- [x] 最低限の画面キー・物理キー入力
- [x] CPUレジスタ表示
- [x] Audio SnapshotをDesktop音声出力へ接続

## Phase 3: BASICクロス開発

- [x] PC-1245 BASIC方言と文字コードを定義する（共通テキストescape parserは実装済み）
- [x] BASIC Tokenizer / Detokenizerを実装する
- [x] `.BAS`の読み込みと保存
- [x] BASICプログラムのRAMへの展開と抽出
- [x] 複数ソースを束ねるバージョン付きプロジェクト定義を実装する
- [x] BASIC／マシン語／混在テンプレートから最小プロジェクトを作成する
- [x] 外部エディタによるソースファイルの変更を検知する
- [x] BASIC、`.dmp`、Raw Binaryをプロジェクト単位でBuild & Loadする
- [x] プロジェクトはBuild & Loadまでを担当し、実行操作をポケコン側へ委ねる
- [x] RUNモードでENTER確定した直接入力をセッション内コマンド履歴として再入力できるようにする
- [x] 親フォルダの下へプロジェクト名のフォルダと`src/`、`build/`を生成する
- [x] 新規プロジェクトの既定フォルダ名が既存フォルダと重複する場合は通し番号で回避する
  - [x] `New-PGP-Project`が存在する場合は`New-PGP-Project-2`、`-3`の順に最初の空き名を選ぶ
  - [x] 番号決定後にも既存ファイルやフォルダを上書きしないことを検証する
  - [x] 番号付きフォルダを作成した場合は`pgp-project.json`の`name`にも同じ通し番号を付ける
- [x] 新規プロジェクトのAssembly初期ソースを実用的な構文サンプルにする
  - [x] 編集・削除可能なサンプルであることを先頭コメントで明示する
  - [x] `ORG`、ラベル、即値命令、アドレス参照、相対分岐、`DB`を含むAssembler文書の例を使用する
- [x] Tracked／Untrackedを表示する軽量なプロジェクトツリーを実装する
- [x] ファイル構成変更をツリーへ自動反映し、Manifestは明示操作時だけ更新する
- [x] 明示的なUpdate Projectで追加・削除されたソースをManifestへ同期する
- [ ] プロジェクト対象を単一`machineId`固定からソース種別に応じた互換範囲へ変更する
  - [ ] BASICソースだけのプロジェクトは全対応機種を対象とし、Manifestで特定機種へ限定しない
  - [ ] BASICはBuild & Load時に現在の実行機種の方言とメモリ配置へ変換する
  - [ ] PC-1245／1250／1251／1255はBASICとマシン語が混在するプロジェクトでも同一互換グループとして扱う
  - [ ] マシン語を含む他機種のプロジェクトは、確認済みの互換グループまたは単一機種を明示する
  - [ ] 既存formatVersion 1の`machineId`指定プロジェクトを後方互換で読み込み、新しい対象指定へ移行できるようにする
  - [ ] 機種非限定プロジェクトを開いた際はROMを自動切替せず、現在の実行機種を維持する
- [ ] BuildとUpdate Emulatorを分離し、Source／Build／Emulator状態を個別表示する
- [ ] エラーから外部エディタのファイル・行を開く

## Phase 4: Pokecom GO Studio対応拡大

- [ ] Windowsでのビルド・配布
- [ ] Linuxでのビルド・配布
- [ ] ROM Import Wizardを実装する（入力slot一覧、inline検証、Pokecom GO形式から`.pgrom`への変換）
- [ ] バンク機の物理ROMをBank 0..Nの一覧で割り当て、一括選択できるようにする
- [x] 前回使用したROMと機種の保存・自動読込み
- [x] ROMイメージの読込み成功後は、ROM Libraryからの復元、登録、機種切替のいずれでも自動的にRUNを開始する
  - [x] ROM検証またはSession生成に失敗した場合はRUNせず、エラーを表示する
- [ ] 将来のROM管理画面から機種／ROM setを切り替えた場合も同じ自動RUN規則を適用する
- [ ] Studio／Playerで共有する機種別ROMライブラリの設計と永続化を実装する
  - [x] Studioで読み込んだ実機ROMイメージ本体を、元ファイルへの参照ではなくアプリ専用領域へ取り込んで保持する
  - [x] Studioで機種ごとに最後に取り込んだROM setを保持し、別機種へ切り替えた後も再読込みなしで復帰できるようにする
  - [x] PC-1250／1251／1255はPC-1251系ROMを一組だけ保持し、同じROMから各モデルのSessionを生成する
  - [x] 内部ROM、外部ROM、bank ROM等の複数componentを一つのROM setとして保存し、manifest、size、hashを検証する
  - [x] StudioのROMはユーザーが再取込みで置換するか、管理画面から明示的に廃棄するまで保持する
  - [x] Studioに保存済みROMの一覧、使用機種、取込元、検証状態、置換、削除を扱うROM管理画面を用意する
  - [x] Studioの初回取込み後は通常起動でアプリ内コピーを使用する
  - [ ] Playerでも同じROMライブラリモデルを実装する
  - [ ] ROMデータをプロジェクト、ログ、バックアップ、同期対象へ意図せず含めない保存方針を定義する
- [ ] Studio設定と最近使ったプログラムファイル
- [x] StudioへFile／Project／Program／Emulator／Debugメニューバーを追加する
- [x] メイン画面をLCD、機種別キーボード、Run／Pause／Reset／Step、動作モード中心に整理する
- [x] 機種選択ボタン群をコンボボックスへ置き換える
- [x] 実行状態、cycle、project状態、直近の操作通知をメイン画面下部へ表示する
- [x] RUN／PRO／RSV切替はPC-1245／1251／1261系等の対応機種だけメイン画面へボタン表示する
  - [x] 非対応機種ではRUN／PRO／RSVボタンをすべて非表示にする
  - [x] 動作モード切替は頻繁な実機操作としてボタンに限定し、Emulatorメニューから削除する
- [ ] Desktop物理キーボード入力を「文字／論理入力」と「物理キー入力」で切り替えられるようにする
  - 文字／論理入力では現在どおりホストの文字を機種別キー列へ変換し、ホストShift単体は送信しない
  - 物理キー入力では文字変換を介さず、PC側の各キーの物理的な押下／解放を対応するポケコンキーへ送る
  - SHIFT単体入力はその一例として扱い、英数字、記号、方向、機能キーを含む機種別の物理割当を定義する
  - 同時押し、key repeat、キーを押したままの状態、およびfocus喪失時の解放を正しく扱う
  - プログラム実行中の自動切替とユーザー設定による明示切替のどちらが適切か、誤判定と操作性を比較して決める
- [ ] マシン語プログラム実行中のBRKキー入力を実機と照合し、確実に検出できるようにする
  - [ ] PC物理キーボードとソフトウェアキーボードの双方で、押下／保持／解放とキーマトリクス反映を確認する
  - [ ] 高速実行中に短い押下を取りこぼしていないか、Runnerのtick単位とキー保持時間を検証する
  - [ ] 機種別のBRK走査方法および割込み・停止条件をROM処理とマシン語直接処理の双方で確認する
- [ ] RUNモードのコマンド履歴をプロジェクトの有無にかかわらず通常のROM利用時も常時有効にする
  - PC側キーボードからENTERで確定した入力を記録し、通常セッションでもAlt+↑／Alt+↓で再入力できることを回帰テストする
- [ ] ゲームパッド入力に対応する（ゲームパッドの各操作にポケコンキーを割り当て、ゲームごとに設定できるようにする）
- [ ] パッケージ生成とリリース自動化（3環境の成果物生成は確認済み、tag release検証待ち）

## Phase 5: 開発支援機能

Debuggerの現在仕様とUI再構成方針は[DEBUGGER.md](DEBUGGER.md)を参照する。

- [x] PGP Memory Dump (`.dmp`)形式と共通パーサー
- [x] `.dmp`のメモリ配置とDesktop入力
- [x] `.dmp`のDesktop出力
- [ ] Intel HEX入出力
- [ ] Raw Binary入出力（ロード開始アドレス指定）
- [ ] Debugger Core
- [x] Debuggerの停止理由とStep後のCPU状態差分を扱う基盤
- [x] 複数の実行ブレークポイントと継続実行
- [x] Disassembly行を指定した1回限りのRun to Cursor
- [x] 指定アドレスをHEX＋ASCIIで確認する読み取り専用メモリビュー
- [x] Machine Definitionに基づく機種別メモリマップとMemory Viewへの移動
- [x] 有効時だけ記録する固定長256命令トレース
- [x] 指定範囲の値を比較し、変更した命令で停止するメモリ変更ウォッチ
- [x] Assembler／Disassembler共通のSC61860命令定義と1命令Decoder
- [x] 現在PCから先を表示する読み取り専用Disassemblyビュー
- [x] 指定範囲をアセンブラ向けテキストへ保存するDisassembler
- [x] ラベル、ORG、DBに対応したSC61860 AssemblerとプロジェクトBuild連携
- [ ] SC61860 Assemblerの言語仕様を拡張する（最終構文と互換方針は別途策定）
  - [ ] 即値、アドレス、`ORG`、`DB`等でラベルと数値を組み合わせた式を評価する（例：`LII 0x12+0x01`）
  - [ ] 演算子、優先順位、括弧、範囲検査、オーバーフローの仕様を定義する
  - [ ] 定数定義とラベル参照の仕様を定義する
  - [ ] マクロ定義、引数、展開、ローカルラベル、再帰制限と診断の仕様を定義する
  - [ ] Include、条件Assembly、文字列／データ定義などの採用範囲を検討する
  - [ ] マクロ展開後も元ファイルと行番号へ対応できる診断・Listing情報を保持する
- [x] 外部エディタでの反復開発に対応するAssembly Workspaceを実装する
  - [x] プロジェクトのAssembly入力と生成物を分けたツリーを専用ウィンドウに表示する
  - [x] 外部エディタによる変更を検出し、最後に成功したAssemblyより新しい入力へ`*`を表示する
  - [x] `Assemble`は保存先を毎回要求せずメモリ上の成果物とプレビューを更新する
  - [x] Assembly失敗時は前回成功した成果物を保持し、ファイル・行番号・ソース行を含む診断を表示する
  - [x] Memoryセグメント、Disassembly、Listing、Symbol Mapをプレビューする
  - [x] `Build`成功時だけ`build/`へ`program.dmp`、Listing、Mapを生成し、生成物はプロジェクト入力から除外する
  - [x] `Build & Load`は同じビルド処理を使用し、全入力の成功後にだけ成果物をエミュレータへ一括ロードする
  - [x] Source変更、Assembly済み、Build済み、EmulatorへLoad済みの状態を個別に表示する
  - [x] 診断から設定済みの外部エディタで該当ファイル・行を開けるようにする
  - [x] 現在の単発変換は補助機能`Quick Assemble`として残す
- [ ] Assembly Workspaceの外部エディタ操作とSources表示を改善する
  - [ ] Studio内にExternal Editor設定を追加し、実行ファイル、引数テンプレート、行指定を保存する
  - [ ] macOS／Windows／Linuxで代表的なエディタを自動検出し、手動選択とTest起動も提供する
  - [ ] `PGP_EDITOR`はアプリ設定を上書きする上級者向け手段として維持する
  - [ ] 起動できない場合はOS別の環境変数設定だけでなくExternal Editor設定画面へ誘導する
  - [x] SourcesのOpen操作を行末の右寄せからファイル名の直後へ移動する
  - [x] Open操作をテキストリンク風表示ではなく、ボタンと明確に分かる外観にする
- [ ] Pokecom GO Studioを外部AIから操作できるAI／MCP連携を実装する
  - [ ] [AI連携構想](AI_INTEGRATION.md)に従い、Studio UIとAIから共用するApplication Service境界を定義する
  - [ ] Capability、Machine、ROM登録状態、Project、Diagnosticsを返すversion付きschemaを定義する
  - [ ] CPU、停止理由、bank、Disassembly、Memory Watch、Breakpoint、Trace、Symbol／Source MapをまとめたDebug Contextを定義する
  - [ ] localhostだけでlistenし、起動ごとの短期トークンで接続する読み取り専用MCPサーバーを実装する
  - [ ] BASICプロジェクト作成、複数機種Build検証、Diagnostics解析を行うAI E2Eシナリオを追加する
  - [ ] `.dmp`／Memoryの静的解析、仮ラベル、制御フロー、Assembly Workspaceプレビューを行うAI E2Eシナリオを追加する
  - [ ] Breakpoint、Watch、Pause、Stepをツールごとの権限とStudio側承認付きで公開する
  - [ ] ソース変更は全文上書きせず差分を提示し、承認後にだけ適用する
  - [ ] Build成果物のLoad、Continue、Memory書込みは個別承認と操作履歴を必須にする
  - [ ] AIへ送るソース、Memory、Trace、ROM由来Disassemblyの範囲をユーザーが確認できるようにする
  - [ ] ROM全体、APIキー、プロジェクト外ファイルをAI Contextへ含めない回帰テストを追加する
  - [ ] 外部MCP運用の評価後に、クラウド／ローカルモデルを切り替える内蔵AI Providerの要否を判断する
- [ ] PC-1360K解析支援（1360とのROM差分、未知アドレスアクセス停止、Read／Write・バンク履歴、メモリ差分）
  - [x] 内容が変化したアドレス・変更前後の値・実行命令PCの記録
  - [x] CPU論理アドレスでのRead／Writeアクセスイベントと、同値Writeを含む停止
  - [x] バンク切替イベントと物理ROM位置を含む履歴
- [ ] PC-1360K実機調査用のチェックポイント出力と漢字ROMアクセス検証プログラム作成支援
  - [x] CPU状態・指定メモリ・命令トレースを含むJSONチェックポイント出力
  - [ ] 実機側チェックポイント取得手順と比較ツール
- [ ] 5×7 Character Editor
- [ ] WAV Encoder / Decoder

## Phase 6: 機種追加

機種追加の順序は、仕様資料、Golden Test Data、利用目的を確認して決める。

- [ ] 次期S1機種としてPC-1350を実装する（25文字×4行LCD、非連続VRAM、S1 BASIC）
  - [x] Display SnapshotとStudio LCD描画を複数文字行へ対応
  - [x] 内部ROM・外部ROM・RAMの基本メモリマップを実装
  - [x] 20ブロックに分かれたLCD VRAMの論理配置を実装
  - [x] 600バイトのVRAMを25文字×4行（150×32ドット）の表示平面へ変換
  - [x] 12グループのキーマトリクスとCPU／Memory Bus／LCD／BuzzerのMachine接続
  - [x] Machine CatalogとEmulatorFactoryへ登録しHeadless Sessionとして公開
  - [x] PC-1350固有キーをCoreへ追加し、専用ソフトウェアキーボードを実装
  - [x] S1 BASICのプログラム領域・開始／終了ポインタと配置処理を実装
  - [x] BASICテキストをS1中間コードへ変換してLoad BASICへ接続
  - [x] PCWAV互換のS1半角カナ（FE＋A1..DF）をTokenizerへ実装
  - [x] PC-1350のShift記号を物理キー入力とソフトウェアキー凡例へ反映
- [x] 次期S2／バンク機種としてPC-1360の起動基盤を実装する（16KiB×8 ROMバンク、LCD、キー入力）
- [x] PC-1360のS2 BASIC Tokenizer／DetokenizerとプログラムRAM配置を実装する
- [x] PC-1360のバンクイベントをDebuggerの物理ROM履歴へ接続する

- [x] 2機種目をPC-1251に決定し、ROM構成・メモリマップ・24桁LCDモデルを追加する
- [x] PC-1251をHeadless Emulator Sessionとして公開する
- [x] PC-1251の括弧入力と画面キーボードを機種別定義に分離する
- [x] PC-1251のRUN／PRO／RSVモード入力と表示を実装する
- [x] PC-1251の外部ROM・RAM・LCDミラーを参照実装と照合する
- [x] ROM経由BASIC入力の文字キー列をPC-1245／PC-1251で分離する
- [x] PC-1251のLCDシンボルを`0xf83c`／`0xf83d`とモードスイッチ状態へ限定する
- [x] ローカル実ROMによるPC-1251起動・LCD・3位置モードのスモークテストを追加する
- [x] PC-1251実ROMで共有キーマトリクスからLCD更新までの入力経路を確認する
- [x] PC-1251実ROMのPROモードで括弧を含むBASIC行を入力・再抽出する
- [x] StudioのROM・BASICガイドとエラー表示からPC-1245固定表記を除く
- [x] 履歴がない初回起動時のローカルROM自動検出をPC-1245／PC-1251へ対応する
- [ ] PC-1251のユーザー登録可能な予約語ショートカットを再現する（優先度低）
- [x] Machine DefinitionをCatalogへ集約し、FactoryとStudioの機種ID分岐を整理する
- [ ] 機種固有BASIC方言への対応
- [x] PC-1250/1251/1255を共通ファミリーとユーザーメモリ容量差で定義する
- [x] PC-1250/1251/1255で実機RAM容量とPC-1255相当の拡張RAMをCore設定で切り替え可能にする
- [x] StudioからPC-1250/1251/1255の実機RAM／拡張RAM設定を選択できるようにする
- [ ] PC-1245／1250の`0xb000..0xbfff`周辺が実機でどのようにミラーされるか資料または実機テストで再確認する
- [ ] PC-1260/1261/1262を共通ファミリーとユーザーメモリ容量差で定義する
- [ ] PC-1401/1402のユーザーメモリ容量差をデータ定義で表現する
- [ ] PC-1360、PC-1460、PC-1470Uのバンク切替を機種別に実装する
- [ ] PC-1360KをMachine Catalogへ登録する（詳細不明のため実装は保留）

## Phase 7: Pokecom GO Player

- [ ] Android Playerアプリ
- [ ] iOS Playerアプリ
- [ ] Studioとのファイル受け渡し
- [ ] `.pgrom`選択と前回ROMの自動復元
- [ ] 初回取込み後のROMをPlayerのアプリ専用領域へ保存し、機種切替後も機種別ROMライブラリから再利用する
- [ ] Playerから保存済みROMの置換と明示削除を行えるようにする
- [ ] LCDと機種別ソフトウェアキーボード
- [ ] RUN／PRO／RSV、Reset、Pause
- [ ] Android AudioTrack／iOS音声APIへのPCM接続
- [ ] セーブステートとモバイルライフサイクル
- [ ] ゲームパッドのキー割当

Pokecom GO Playerはエミュレータの実行に特化し、Pokecom GO Studioが提供するデバッガ、
アセンブラ、ディスアセンブラ、メモリ編集等の開発機能は実装しない。機能範囲は既存の
Pokecom GOを基準とする。
