# Roadmap

このロードマップは方向性を示すものであり、リリース日を保証するものではない。
各段階は小さな動作確認とテストを完了してから次へ進む。

## 当面の開発優先順位（2026-10-21）

iOSの実装を先に進め、AIによるマシン語生成は進め方の記録に留める。
Studioは、ユーザーの認識としてデバッグ機能の充実を除き概ね一区切りとする。
この方針は既存の未完了項目を一括で完了扱いにするものではない。
AI生成の再開時は1機種・1アセンブラの`HELLO WORLD`から始め、Studioが持つ機種仕様の取得と
手動Build・Load・実行を確認し、必要なAPIを段階追加する。
[進め方の記録](AI_INTEGRATION.md#11-マシン語生成の進め方と当面の優先順位2026-10-21)を参照。

## 追加構想の整理（2026-10-20）

[構想書の追加構想](PGP_CONCEPT.md#16-追加構想海外への展開と開発利用基盤の整備)を次のカテゴリで管理する。
以下は検討・整備の候補であり、記載順は開発優先順位ではない。既存の機能開発・不具合修正とは
粒度を分け、方針を具体化してから個別タスクへ落とし込む。

- [ ] 公開・コミュニティ：海外のPokecom GO利用者への発信方針、英語紹介、デモ、報告窓口を整える。
- [ ] 多言語ドキュメント：英語`README.md`／日本語`README.ja.md`の入口を作り、詳細文書の翻訳・更新方針を決める。
- [ ] ROM吸い出し支援：内部ROMを含む仕組み・機種別手順・検証方法を整え、ツールとセットで提供する。
- [ ] プラットフォーム拡張：基本3環境を軸にARM Linux／BSD等の実現性と、移植・配布を追加する仕組みを調べる。ChromeOSは下記の後続項目と連携する。
- [ ] AI連携：Codexに加えてClaude Codeの接続・検証を整え、既存マシン語の解析と要求仕様からのマシン語生成を段階的に実現する。
- [ ] 設計ドキュメント：全体アーキテクチャ→モジュールの責務・機能→個別機能の詳細という階層で、現行コードの設計を解説する。

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

## Technical Preview 0.3

Studioのレイアウトと操作性、5×7 Character Editor、OLD WAV試作、ビルド案内を改善し、Android版
Pokecom GO Playerの最小プロトタイプを初めて同梱する早期評価版。公開条件と結果は
[0.3 release checklist](RELEASE_CHECKLIST_0.3.0-alpha.1.md)で管理する。

## Technical Preview 0.4（2026-10-12公開済み）

PC-1245 Android Playerの今回の区切り、PC-1261／1401のStudio/Core対応、ゲームパッド、
SC61860 Assembler強化、Studio統合WAV Toolと波形解析を含む。
公開条件と結果は[0.4 release checklist](RELEASE_CHECKLIST_0.4.0-alpha.1.md)で管理する。

## ChromeOS対応（将来対応・2026-10-20記録）

ユーザーから、Chromebook上でLinux版Studioが動作したとの報告を受領。
ChromeOSではAndroid版Playerも動作するため、同一端末でStudioによる開発とPlayerによる実行を行う利用を想定する。
動作報告は正式なChromeOS対応・網羅的な検証完了を意味しない。今回実装には着手せず、後日対応する。

- [ ] Chromebook上のStudio（Linux版）とPlayer（Android版）の動作条件・対応範囲を確認する。
- [ ] Studioで発生した可能性があるパーミッション関連エラーを再現し、ログと操作手順から原因を調べる。
  - エラー内容は未採取。通常のLinuxとのファイルパスの違いが原因という見方は、現時点では仮説。
  - Linux環境・Android環境・ChromeOS間のファイル参照先、共有フォルダ、アクセス権限の関係を確認する。
- [ ] 同一Chromebook上のStudioからPlayerへ、BASICソースや実行用ファイルを転送する機能を検討する。
  - 転送対象・受け渡し方式・Playerでの受取／ロード操作は未決定。既存のBASIC／DMPロードを出発点として調査する。
  - Playerは実行に特化する方針を維持する。ROM転送やセーブステート共有は、この要望の対象に含めない。
- [ ] 実装・自動検証・ChromeOS実機確認を区別して記録し、利用手順を整備する。

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
- [ ] Windowsでサウンド使用プログラムの終了後にクリックノイズが鳴り続ける問題を解消する
  - [ ] オーディオラインの終了条件と無音バッファ処理を見直す
  - [ ] 通常再生を崩さず、プログラム終了、BREAK、Pause、Reset、アプリ終了を実機で確認する
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
- [x] Studio UIをシステム言語設定に応じてローカライズする（2026-10-14ユーザー受入れ済み）
  - 2026-10-14: Studio対象画面の日英文言移行・再起動反映の言語設定・自動検証を完了。ユーザーによる一通りの確認を受領。OS／個別操作の内訳は未指定で、OS設定変更試験・Windows／Linux個別結果・独立画像の描画欠けは後続確認として記録。[最終検証](STUDIO_LOCALIZATION_VERIFICATION.md)。[進捗](STUDIO_LOCALIZATION_PLAN.md)、[棚卸し・用語表](STUDIO_LOCALIZATION_INVENTORY.md)。
  - [x] StudioのUI文言をリソースへ分離し、起動時に日本語／英語を選択する（実行中切替UIは今回対象外）
  - [x] 初期言語はDesktopでOS由来の表示Localeから選び、未対応言語では英語へフォールバックする（実OS設定変更試験は未実施）
  - [x] AI / MCP Server、Debugger、Project、ROM管理を含む説明、操作、状態、エラー文言を日英へ移行する
  - [x] MCP tool名、JSON field、schema ID、設定key、URL、file path等の外部仕様上の識別子を維持する
  - [ ] 本番Mac／Windows／Linuxの実表示・操作とOS言語設定変更試験を確認する
  - [x] ユーザー追加指示により、設定でOSの設定に従う／日本語／Englishを保存し、アプリ再起動後に反映する（実行中切替は対象外）
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
- [x] RUNモードのコマンド履歴をプロジェクトの有無にかかわらず通常のROM利用時も常時有効にする
  - PC側キーボードからENTERで確定した入力を記録し、通常セッションでもAlt+↑／Alt+↓で再入力できることを回帰テストする
- [ ] ゲームパッド入力に対応する（ゲームパッドの各操作にポケコンキーを割り当て、ゲームごとに設定できるようにする）
- [ ] パッケージ生成とリリース自動化（3環境の成果物生成は確認済み、tag release検証待ち）
  - [ ] 次の修正タイミングでWindows MSIの更新インストール対応を整える（2026-10-13ユーザー指定）
    - [x] 公開0.4のUpgradeCodeを固定し、修正版のpackageVersionを1.3.1へ更新。配布更新ごとに増加する運用を定義。
    - [ ] 旧版からの更新、スタートメニュー登録、設定・ROM Library保持のWindows受入れ（2026-10-13ユーザー方針: 次回テクニカルプレビュー時に実施）。
    - [今回の修正・検証記録](PLATFORM_FIXES_2026-10.md)にMSI／音声／BRK／iOS CIを記録。

2026-10-13ユーザー確認: 機種追加前のMSI／Windows音声／BRK／iOS CIの今回分は一旦完了。
2026-10-14にWindows／Linux動作とWindows更新インストールをユーザー確認。
AI読み取り登録APIまでの5環境CI・配布物生成は成功。0.5公開記録は[チェックリスト](RELEASE_CHECKLIST_0.5.0-alpha.1.md)を参照。
詳細検証の未完了チェックは後続確認として保持する。

### ゲームデモ用のキー操作記録・再生（後続）

- [ ] Studioでキー操作を記録し、保存した操作列をゲームのデモとして再生する
- [ ] 記録開始／停止、再生／中断、ループ再生を設計する。Playerでの再生とProjectへの同梱は後続検討とする

初版候補は、Coreへ届いた論理キーのpress/releaseと記録開始からのエミュレーションサイクルを保存する方式。
PCキーボード・ソフトキー・ゲームパッドを共通化し、押し続ける時間や複数キーの同時押しも保持する。
既存の文字列入力・コマンド履歴の再入力とは用途を分ける。

実装前に確認する課題:

- **再生開始状態**: 同じキー列でもRAM、CPU、モード、乱数・timer状態が違えば結果が変わる。
  初版はreset→同じprogramをload→一定cycle実行などの開始手順を定義する。
  任意の瞬間から完全再現するにはCPU・RAMだけでなくdivider、入力latch、機種固有状態等も復元できる
  セーブステートが必要。既存のdebug snapshot保存だけで完全復元できるとは扱わない。
- **タイミング**: 壁時計や画面frameでなく実行cycleを基準にし、Pauseや速度変更でも操作間隔を保つ。
  入力適用は同じ命令境界にそろえ、現在のrunCycles budget超過分を含めた境界処理を検証する。
- **入力所有権**: HostKeyStateの統合後に実際にCoreへ届いた変化を記録し、キーrepeatや複数入力元の
  重複を記録しない。既存KeyInputQueueは一定保持時間の単一キー列なので、そのままでは任意の保持・
  同時押しを再現できない。mode操作、自動入力、Resetやprogram loadは記録範囲を決める。
- **中断とループ**: 再生中の実入力による中断方針を決め、中断・focus喪失・機種切替・終了時は
  再生が所有するキーを解放する。ループごとに開始状態を復元し、前周の状態を引き継がない。
- **互換性と保存**: version、machine ID、ROM／program識別用hash、RAM設定、開始条件を記録し、
  不一致は再生前に検出する。ROM本体を操作ファイルへ同梱しない。更新でCPU挙動が変わる場合も考慮する。
  途中からのseekは後回しとし、最初からの再生または復元可能なcheckpointが必要とする。
- **責務と検証**: 記録・保存・UI・deviceはHost側、必要なcycle駆動schedulerは純粋な共通処理へ置く。
  CoreにOS時刻・ファイルAPI・thread所有を追加しない。自作programで長押し、同時押し、Pause、
  速度変更、中断時の解放、繰返し再生とStudio／Player間の再現性を検証する。

## Phase 5: 開発支援機能

Debuggerの現在仕様とUI再構成方針は[DEBUGGER.md](DEBUGGER.md)を参照する。

- [x] PGP Memory Dump (`.dmp`)形式と共通パーサー
- [x] `.dmp`のメモリ配置とDesktop入力
- [x] `.dmp`のDesktop出力
- [ ] Intel HEX入出力
- [ ] Raw Binaryを開始address付きで一度だけimportし、`.dmp`へ変換する共通導線
- [ ] 新規Project sourceを`.dmp`／Intel HEXへ統一し、既存`raw-binary` manifestを後方互換入力にする
- [ ] Debugger Core
- [x] Debuggerの停止理由とStep後のCPU状態差分を扱う基盤
- [x] Debugger内のRun／Pause／Step、固定の停止情報、停止原因への移動、広幅の左右配置／縮小時タブ（日英。検証範囲は[ステップ1記録](DEBUGGER_STEP1_VERIFICATION.md)）
- [x] Step前後／前回停止時のレジスタ・表示メモリの変更強調、比較基準の表示、Reset／ページ／Session／物理ROM切替時の基準管理（日英。[ステップ2記録](DEBUGGER_STEP2_VERIFICATION.md)）
- [x] 読み書き／bank履歴タブ、表示の絞込み、保持状態・上限表示、現在の命令／メモリ表示への移動（日英。MCP出力・記録は維持。[ステップ3記録](DEBUGGER_STEP3_VERIFICATION.md)）
- [x] AI向け登録一覧の読み取り専用MCP tool（既存Debug Context version 1を維持）
- [x] 登録一覧・追加／編集／削除・有効切替、複数の値変更／読み書き監視（日英。[ステップ4記録](DEBUGGER_STEP4_VERIFICATION.md)）
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
- [x] 設定で内蔵／外部SC61860 Assemblerを選択できるようにする（SC61初版は2026-10-13に一区切り。詳細なWindows検証と他ツール対応は後続）
  - [x] macOS版SC61 Rev 0.04aの正常／異常終了、生成物、相対INCBIN、空白・日本語パスを調査する（[調査記録](SC61_INTEGRATION.md)）。相対INCLUDEも後続で確認済み。
  - 既存Assemblerごとのニーモニック、条件Assembly、INCBIN等の方言を、そのツールで処理できるようにする。
  - 2026-10-12方針: 開発者が普段使うAssemblerを選べる外部連携を、内蔵Assemblerの追加の方言対応より先に進める。Assembler選択はStudio全体の共通設定とし、プロジェクトごとの切替は設けない。
  - [x] Studio共通の設定保存と外部起動の接続テスト、ログ表示、タイムアウト・キャンセルを実装する（第2段階。Build連携も後続段階で実装済み）
  - [x] SC61の成功ログと隔離した新規DMPを取り込み、末尾DOS EOFを許容して機種別配置検査と範囲表示を行う（第3段階。現行では機種別配置・BASIC衝突はLoad時に検査）
  - [x] Studio共通の内蔵／外部Assembler選択、実行ファイル、引数テンプレートを定義し、作業ディレクトリはソース／プロジェクト位置から解決できるようにする
  - [x] Workspace、Build／Build & Load、Quick Assembleで共通のAssembler選択処理を使用する
  - [x] 外部コマンドの終了コード・標準出力・標準エラーを取り込み、失敗・タイムアウト・キャンセルを扱う
  - [x] SC61のaddress付きDMPを初期対応として取り込む。Buildは全範囲を保存し、同一アドレス重複を検査する。機種別配置・物理ミラー・BASIC衝突はLoad時に検査する（Raw Binaryは後続ツール対応時に検討）
  - [x] 今回の実行で生成された成果物だけを取り込み、失敗時に前回の出力を誤ってLoadしないようにする
  - [x] SC61のLST原文表示・保存、Symbol Map、行番号付き診断を取り込む。INCLUDEのファイル名はLSTに出ないため、行番号と内容で一意に特定できる場合のみ行移動する。未提供時もBuild／Loadは利用可能。
  - [x] macOSで設定、Workspace／Build & Load、Quick Assemble、エラー保持・診断行移動、内蔵切替を実画面確認し、[利用手順](EXTERNAL_ASSEMBLER.md)を整備する
  - [x] Windows版SC61 Rev 0.04aをWine 11.0の一時環境で確認する（Windows上のStudio確認とは別）
  - [x] Windowsでの外部Assembler対応をユーザー確認により今回分完了とする（2026-10-13。[詳細項目](EXTERNAL_ASSEMBLER.md)の個別記録とは区別）
  - [x] Windowsユーザー確認でSC61接続テストとBuild成功を確認する（2026-10-13）。「プログラム登録」成功も報告された。スタートメニューの登録先詳細は確認待ち。その後、ユーザー確認により外部AssemblerとWindows Projectツリーは今回分完了。
  - 内蔵Assemblerは維持する。初期対応はSC61のDMP出力。Windowsの詳細検証と他ツールのadapterは後続。
- [x] 外部Assembler基盤の次段階として「内蔵／SC61／yasm.pl／その他（汎用DMP）」の選択構成を実装する
  - プロセス起動・ログ・タイムアウト・キャンセルを共有し、成功判定と成果物取り込みを種類ごとに分ける。選択はStudio全体の設定を維持する。
  - 2026-10-20: SC61旧設定互換、YASMの非整列DMP、汎用DMPと設定可能な成功終了コードを接続。[設定・検証範囲](EXTERNAL_ASSEMBLER.md)。
  - 同日、使用対象選択とツール別設定タブ・個別保存、メイン画面表示、長いログの固定枠を追加。ユーザーから問題なしとの動作確認報告を受領し、今回分を一区切りとした。
  - yasm.pl 1.02をローカル実行で検証。汎用出力はアドレス付きDMPに限定し、Intel HEX／raw binary・他形式のListing／Symbol Map・OS別実機確認は後続。
  - 優先見直し対象は、外部Assembler基盤、Studio日本語／英語対応、Projectの機種互換範囲・メモリ方針。空き領域RAMモードは引き続き後続検討。
- [ ] SC61860 Assemblerの言語仕様を拡張する（最終構文と互換方針は別途策定）
  - [x] 即値、アドレス、`ORG`、`DB`等でラベルと数値を組み合わせた式を評価する（例：`LII 0x12+0x01`）
  - [x] 演算子、優先順位、括弧、範囲検査、オーバーフローの仕様を定義する
  - [x] 不変EQU定数定義とラベル参照の仕様を定義する
  - [ ] マクロ定義、引数、展開、ローカルラベル、再帰制限と診断の仕様を定義する
  - [x] DW、文字列DB、DS、ALIGN、現在位置`.`、`;`／`#`コメントを実装する
  - [ ] Include、条件Assemblyの採用範囲を検討する
  - [ ] マクロ展開後も元ファイルと行番号へ対応できる診断・Listing情報を保持する
- [x] 外部エディタでの反復開発に対応するAssembly Workspaceを実装する
  - [x] プロジェクトのAssembly入力と生成物を分けたツリーを専用ウィンドウに表示する
  - [x] 外部エディタによる変更を検出し、最後に成功したAssemblyより新しい入力へ`*`を表示する
  - [x] `Assemble`は保存先を毎回要求せずメモリ上の成果物とプレビューを更新する
  - [x] Assembly失敗時は前回成功した成果物を保持し、ファイル・行番号・ソース行を含む診断を表示する
  - [x] Memoryセグメント、Disassembly、Listing、Symbol Mapを縦・横スクロール付き結果欄に表示する
  - [x] 単一Assemblyソースの出力名をソース名に揃える（複数Assemblyソースはprogram.*）
  - [x] 機種別配置、ROM／未割当／ミラー重複、BASIC衝突を事前検証する
  - [x] `Build`成功時だけ`build/`へDMP、Listing、Mapを生成し、生成物はプロジェクト入力から除外する
  - [x] `Build & Load`は同じビルド処理を使用し、全入力の成功後にだけ成果物をエミュレータへ一括ロードする
  - [x] Source変更、Assembly済み、Build済み、EmulatorへLoad済みの状態を個別に表示する
  - [x] 診断から設定済みの外部エディタで該当ファイル・行を開けるようにする
  - [x] 現在の単発変換は補助機能`Quick Assemble`として残す
- [ ] Quick Assembleでアセンブル結果を確認できるようにする（2026-10-11ユーザー指摘）
  - 現状はDMP保存が中心で、生成された機械語・Listing・Symbol Mapを画面で確認できない。
  - Assembly Workspaceと同様のスクロール付き結果表示を検討し、成功／失敗と保存先も明確にする。
  - 単発変換の操作性を維持しつつ、結果表示とファイル保存の流れを設計する。仕様・実装は未着手。
- [x] Assembly Workspaceの外部エディタ操作とSources表示を改善する（2026-10-13ユーザー指定で現在の対応を一旦完了。未実装の検出等は後続候補）
  - [ ] Studio内にExternal Editor設定を追加し、実行ファイル、引数テンプレート、行指定を保存する
  - [ ] macOS／Windows／Linuxで代表的なエディタを自動検出し、手動選択とTest起動も提供する
  - [ ] `PGP_EDITOR`はアプリ設定を上書きする上級者向け手段として維持する
  - [ ] 起動できない場合はOS別の環境変数設定だけでなくExternal Editor設定画面へ誘導する
  - [x] SourcesのOpen操作を行末の右寄せからファイル名の直後へ移動する
  - [x] Open操作をテキストリンク風表示ではなく、ボタンと明確に分かる外観にする
- [ ] Pokecom GO Studioを外部AIから操作できるAI／MCP連携を実装する
  - [x] [AI連携構想](AI_INTEGRATION.md)に従い、Studio UIとAIから共用するApplication Service境界を定義する
  - [x] Studio Debuggerから読み取り専用Debug Context JSONをプレビュー／保存する最小UIを実装する
  - [ ] Capability、Machine、ROM登録状態、Project、Diagnosticsを返すversion付きschemaを定義する
  - [ ] CPU、停止理由、bank、Disassembly、Memory Watch、Breakpoint、Trace、Symbol／Source MapをまとめたDebug Contextを定義する
  - [x] localhostだけでlistenし、起動ごとの短期トークンで接続する読み取り専用MCPサーバーを実装する
  - [ ] Codex以外のMCPクライアントとの相互運用性を整備する
    - [ ] Claude CodeからStreamable HTTP接続、認証、tool呼び出しをE2E確認する
    - [ ] AI / MCP Server画面へClaude Code用設定のコピーと接続手順を追加する
    - [ ] `http_headers_helper`を持たないクライアントでも、短期tokenを設定へ平文保存せず更新できる方式を検討する
    - [ ] クライアント固有設定と、MCP共通のtool／schema／security境界を分離して文書化する
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
- [x] 5×7 Character Editor最小試作
  - [x] サイズ、packing方向、bit方向を差し替え可能なDotPattern formatを定義する
  - [x] byte列との相互変換とBASIC `DATA`／Assembler `DB`出力をCoreへ実装する
  - [x] Studioの`Tools`から独立windowを開き、単一文字をクリック／ドラッグ編集する
  - [x] Clear、Invert、数値入力のApply、出力Copyを実装する
  - [ ] Undo／Redo、複数文字、ファイル入出力、Emulator preview、機種固有UDC形式
- [ ] WAV Encoder / Decoder
  - [x] `pcwav`参照commitからOLD系payload、checksum、PCM/WAV仕様を文書化する
  - [x] `pcwav`参照commitからS1／S2のpayload、checksum、PCM動作と未対応範囲を文書化する
  - [x] raw／logical／PCMの不変データ型、範囲mapping、構造化diagnostic、codec resultを定義する
  - [x] OLDのnibble swap、logical checksum、8-byte挿入、80-data-byte累積resetを実装する
  - [x] OLD BASIC `02`／`12`とBinary `62`のraw payload Encoderを実装する
  - [x] OLD raw payload Decoderとchecksum／終端／宣言長／余剰data診断を実装する
  - [x] OLDの2.048秒leader、W0／W1、19-unit byte framingをPCM Signal Encoderへ実装する
  - [x] PCM WAV Writer／Readerと8／16-bit・mono／stereo正規化をUI非依存で実装する
  - [x] OLD PCM Signal Decoderとraw byte位置／nibble同期位置の解析情報をUI非依存で実装する
  - [x] OLD系の転送・信号・WAV container codecをUI非依存の統合APIで接続する
  - [x] OLD BASIC / Binaryの固定vectorと境界長testを追加する
  - [x] Studioの「Tools」からOLD WAV Encode / Decodeを開ける最小UIを実装する
  - [x] OLD WAV Toolの初期UI確認で判明した問題を修正する
    - [x] Toolから開いたfile dialogを閉じた後、メイン画面ではなくWAV Tool windowへfocusを戻す
    - [x] Emulator BASICの外枠`FF ... FF`をOLD転送bodyの`... F0`へ変換するadapterを追加し、Encodeエラーを解消する
    - [x] OLD転送bodyの`... F0`をEmulator BASICの`FF ... FF`へ戻すadapterを追加し、Decode後のsource保存を成立させる
    - [x] Binaryの開始address欄と`.bin`直接入出力を廃止し、単一連続領域の`.dmp`入出力へ置き換える
    - [ ] Intel HEXをaddress付き交換形式として追加する
  - [x] Studio専用の統合WAV Toolへ切り替え、ProjectツリーのBASIC／`.dmp`指定と既存ファイル指定を共通操作へ接続する
  - [x] OLD変換結果を確認してから保存し、保存取消／失敗後も再保存できるようにする
  - [x] 診断のseverity／stage／位置／checksum期待値・実測値を表示する
  - [x] OLD Raw Decodeのhex表示・raw保存、metadata／同期位置表示、payload失敗時のPCM／raw保持を実装する
  - [x] Analyzer最小版としてmono波形、min/max envelope、sample表示、zoom／pan／cursor／範囲指定、raw行との移動、nibble sync位置を表示する
  - 運用方針: Player非搭載、ROM CSAVE/CLOAD非連動、Decodeからのエミュレータ直接ロード対象外。
    操作と残る拡張は[WAV_OPERATIONS.md](WAV_OPERATIONS.md)参照。以下のRaw Decode／Analyzerの全形式・全overlay対応は未完了。
  - [ ] WAVを転送形式として解釈せずraw byte列へ復号するRaw Decode解析ツールを追加する
    - [x] raw byte列のhex表示とファイル保存に対応する（現在OLD信号のみ）
    - [x] sample rate、channel、bit depth、検出byte数、同期位置などの解析情報を表示する
    - [ ] OLD／S1／S2 payloadとして解釈できないWAVでも信号Decode結果を確認できるようにする
  - [ ] Raw Decode結果をOLD／S1／S2の候補形式で比較解析するWAV Analyzerを追加する
    - [ ] 上段にPCM波形を表示し、sample／時間軸のzoom、pan、cursor、範囲選択に対応する
    - [ ] 縮小時は区間内min／max envelope、拡大時は個々のsampleとedgeを表示して長いWAVでも形を失わない
    - [ ] 波形へthreshold後のON／OFF level、W0／W1、leader、sync、byte境界をoverlay表示する
    - [ ] channel原波形とmono正規化後波形を切り替え、DC offset、振幅、thresholdを確認できるようにする
    - [ ] 下段左にoffset付きraw byte列、下段右に選択した形式の変換後byte列、構造、文字表現を表示する
    - [ ] 波形範囲、signal unit、raw byte、logical byte、文字位置を相互選択・強調表示する
    - [ ] type、header、filename、address、body、terminator、checksumを領域別に可視化する
    - [ ] checksum一致／不一致、同期喪失、未定義byte、途中終端、余剰dataを位置付きで表示する
    - [ ] OLD／S1／S2を自動断定せず、候補ごとの解析結果と信頼度を比較できるようにする
    - [ ] payload解釈に失敗しても波形、ON／OFF、W0／W1、raw byteまでの成功結果を残す
    - [ ] sample rate、channel、bit depth、duration、peak、DC offset、検出edge／byte数をsummary表示する
    - [ ] raw／変換後byte列、文字変換結果、解析diagnosticを個別にexportできるようにする
  - [x] S1 WAV Codecを実装する（段階3・4、実機往復は別途未確認）
    - [x] 2026-10-20: 実機確認済みの末尾checksumへ仕様を修正し、28件の合成vectorを整理する。[進捗](WAV_S1_S2_PROGRESS.md)。後続の段階3〜5でpayloadも実装。
    - 2026-10-12方針: Playerの作業を優先し、S1/S2対応は後回し。WAV Tool冒頭でpcwavの利用を案内する。
    - [x] S1共通信号と4.096秒leader、16-unit byte framingを実装する（段階2：8／16／24／32kHz Decode、合成vector・破損診断テスト）
    - [x] S1 Binary `67`を単一連続領域の`.dmp`／Intel HEX入出力として実装する（段階3：合成検証完了、実機往復未確認）
    - [x] S1 BASIC `07`、120-byte chunk、半角カナ、物理trailer `FF`を実装する（2026-10-21：段階4、機種別adapter／Studio接続、合成検証済み・実機往復未確認）
  - [x] S2 BASIC WAV Codecを実装する（2026-10-21：段階5、合成検証済み・実機往復未確認）
    - [x] type `27`（BASIC）／`37`（password付きBASIC）、`FE token`、`1F line reference`、CP932 textを実装する
    - [x] 作者確認済みのpcwav BASIC tail checksum方式を採用し、固定vector一致を検証する
    - [ ] 新PGP codecの実機往復を確認する（[確認表](WAV_TOOL_GUIDE.md#実機往復の確認記録)）
  - [x] S2 Binaryは作者確認に基づきS1 Binary `67`と同じcodecを共用して対応する（実機往復未確認）
  - [x] 2026-10-21：段階6の全形式UI・診断・操作ガイド・OLD回帰を仕上げる（[ガイド](WAV_TOOL_GUIDE.md)）
  - [ ] 実機録音fixtureでDecode許容範囲を確定する

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
- [x] PC-1260/1261を共通ファミリーと容量差で定義しCore/Studioへ接続する
  - PC-1262は価格改定版なので独立ラインナップから除外し、1261として扱う。
  - 外部ROM版は機種差にしない。RAMミラーは実機仕様未確定のためユーザー指示で保留。
  - Expanded RAMや1350/1460等のメモリーカード拡張は、今回のシリーズ対応後に別途整理する。
- [ ] RAM容量切り替えとは別に、機種ごとの空き領域をRAMとして利用するモードを検討する（次回以降）
  - ROM・LCD・制御領域を拡張対象にせず、ロード検査とCPUのメモリ処理で同じ設定を使用する。既存BASICとの衝突検査は維持する。
- [x] PC-1401/1402のユーザーメモリ容量差をデータ定義で表現しCore/Studioへ接続する
  - [ ] 1402用ユーザーROMで容量認識・NEW／BASIC実行を確認する
  - RAMミラーはユーザー指示で保留。ROM版を既知hashに限定しない。
- [x] PC-1360、PC-1460、PC-1470Uのバンク切替を機種別に実装する（1460/1470Uは2026-10-13）
- [x] PC-1450のCore/Studio、16桁LCD、キー走査、専用S1 BASICとROM入出力を実装する
- [x] PC-1460のCore/Studio、4バンク、24桁LCDと物理3:4 dot pitch、S1 BASICを実装する
- [x] PC-1470UのCore/Studio、8バンク、24桁×2行LCD、専用キー／S2 BASICを実装する
  - [x] ↕︎キーの走査位置をローカルROMで確認し入力を接続する
  - [ ] SHIFT TITLE機能固有の操作条件・結果を確認する（ユーザー指示で保留。取説の記載も未発見）
- [x] Studio LCD dot比率を1460のみ3:4、他機種1:1に統一する（ユーザー確認済み）
- [ ] 関数キーのUnicode「↕︎」が絵文字表示になる問題を解決する（現状はユーザー了承で保留）
  - 合成／ローカルROM確認と実機・Windowsユーザー操作を区別。Player機種追加・RAM容量選択は別。
  - 詳細と未確定事項はdocs/machines/PC-1450.md、PC-1460.md、PC-1470U.md。
- [ ] PC-1360KをMachine Catalogへ登録する（詳細不明のため実装は保留）

## Phase 7: Pokecom GO Player

2026-10-19: PC-1251／PC-1261はユーザーから実機テスト完了報告を受領し、今回の対応を一区切りとした。
2026-10-20: PC-1360／PC-1360KをAndroidホーム・ROM管理へ接続。1350／1360／1360KのLCD再配置・Kの反転Sを含む今回の範囲について、ユーザーから実機確認「概ねOK」の報告を受領。個別機器・機能の網羅結果は未採取。
ホームは名前順のタイル／リスト切替とし、選択中の機種は名前の先頭に`*`を表示する。
PC-1450 → PC-1460 → PC-1470Uの順にCore／Studio差分を確認し、ユーザー提供の機種別LCD画像に従ってAndroidへ接続。自動検証・エミュレータ確認を実施済み。2026-10-20にユーザーから一通りの実機動作で問題なしとの報告を受領し、今回のAndroid機種追加を完了とした。ホームの表示・スクロール・回転とRedmi Pad 2（Android 16）の機種画面固定・遷移表示の改善も受入れ済み。個別機能・周辺機器の網羅試験とは区別する。
記録：[PC-1450](ANDROID_PC1450_PROGRESS.md)／[PC-1460](ANDROID_PC1460_PROGRESS.md)／[PC-1470U](ANDROID_PC1470U_PROGRESS.md)。
2026-10-20: 1250／1251／1255、1260／1261、1401／1402のシリーズ内機種切替をホームのプルダウン・共通ROM管理・標準RAMで接続。自動検証・隔離エミュレータ確認済み、今回の実機受入れは未実施。[対応記録](ANDROID_SERIES_PROGRESS.md)。その後、ユーザーよりPC-1250のMEM表示（期待1486に対し3486）を一旦許容し、Android Player全体を一区切りとする意向を受領。容量認識の原因切り分けは保留し、7モデル全機能の実機確認完了とは区別する。RAM拡張・セーブステート・iOS機能拡張・iOS CI高速化も今回の対象外。
最新の検証範囲は[PC-1360対応記録](ANDROID_PC1360_PROGRESS.md)／[PC-1360K対応記録](ANDROID_PC1360K_PROGRESS.md)を参照。
以下の機種範囲や未実装項目には、それ以前の段階の記録を含む。

2026-10-12にPC-1245 Androidの今回の範囲を完了し、ユーザーが動作確認済み。
2026-10-15にホーム・メニュー・情報画面の整理、Physical外部キーボード、PC-1401 Androidの予定範囲を完了し、ユーザーが今回の区切りを承認した。
[検証区分と未確認事項](ANDROID_PC1401_PROGRESS.md)を保持する。PC-1350も2026-10-16にユーザーが一通りの動作とLCD改善を確認し、今回の対応を一区切りとした。[段階別検証と機器別の未確認事項](ANDROID_PC1350_PROGRESS.md)を保持する。PC-1245 iOSの残作業は後続。1401／1350のiOSは今回に含めない。
iOS PC-1245の音声・速度設定は[手順3](IOS_PLAYER_STEP3.md)で合成/Simulator接続を検証し、実機・実ROM音声の受入れを残す。
WAV、Project、Debugger、セーブステート、ゲームパッド接続時の表示自動切替は後続。
公開の判断・version更新・配布物検証は開発完了とは別に行う。操作は[Playerガイド](PLAYER_PROGRAM_IO.md)を参照。

- [x] Android PlayerアプリのPC-1245対応（2026-10-12、ユーザー動作確認済み）
  - [x] Android applicationモジュール、CoreのAndroid target、最小Compose起動画面とdebug APK生成を追加する
  - [x] Android／iOSで共有するPlayer画面状態、最小Runner、スキン座標モデルと配置計算をKMPモジュールへ分離する
  - [x] ROM未登録画面から始まるPlayer画面状態を実装する
  - [x] SAFで`.pgrom`／PC-1245 legacy ROMを選択し、検証後にアプリ専用領域へ正規化保存する
  - [x] 保存済みROMからPlayer共有層の単一セッションを生成し、Android画面状態へ接続する
  - [x] ROMの取込みまたは保存済みROMの復元からSession生成に成功したら、自動的にRUNを開始する
  - [x] ROMが動作中であることを常時確認できる実行状態インジケーターを追加する
    - [x] `RUNNING`／`PAUSED`へ連動し、PC表示やグリーン点灯などを候補に、色だけに依存しない表示とする
  - [x] 自作PC-1245スキンを設計座標へ登録し、LCDを同一変換で重ねてAndroid画面へ表示する
  - [x] PC-1245スキンの全52キーへ共有設計座標のタッチ領域を定義し、押下／保持／解放を単一セッションへ接続する
  - [x] スキンキーの押下中ハイライトと解放時フェードアニメーションを追加する
  - [x] 実行画面をスクロールしないスキン固定表示とし、実行・表示・動作モード操作をオーバーフローメニューへ集約する
  - [x] ステータスバーを隠し、ジェスチャーナビでは全面表示、旧3ボタンナビでは操作領域を避けてスキンを最大化する
- [x] Android PlayerアプリのPC-1401対応（2026-10-15、今回の予定範囲を完了）
  - [x] 機種選択、ROM登録・復元・置換・削除、Session切替
  - [x] スキン・LCDシンボル・76キー、CAL／BASICによるモード切替、実行・停止・Reset
  - [x] S1 BASIC／DMPのロード・セーブ、CPU再開とキャンセル・明示PAUSE時の復帰
  - [x] 共有ゲームパッド割当・入力所有権・背景／切断時の解放（実機基本動作ユーザー確認済み）
  - [x] Android 9・3ボタン実機のステータスバー余白を修正し、メニュー・背景復帰を確認
  - 機器・接続方式の網羅検証と残る実機確認は[対応記録](ANDROID_PC1401_PROGRESS.md)に分けて記録
- [ ] iOS Playerアプリ
  - PC-1245品質向上の今回の手順1: [標準ピッカーのSimulator検証と実機の残項目](IOS_PICKER_STEP1.md)。後続のホーム・入出力・音声・入力とは分ける。
  - [x] iPhone専用・最低iOS 16のSwiftUI/UIKit host、Apple framework/Facade、Simulator合成プログラムによる接続確認
  - [x] CADisplayLinkによる共有Runner駆動、Run/Pause/Reset、非active停止・入力解放・通常復帰PAUSED
  - [x] PC-1245スキン/LCDシンボル/RUN・PRO表示/52タッチキー、通常・Controller Display、両横向きsafe area（Simulator確認）
  - [x] PC-1245 ROM登録・atomic保存・起動時復元・置換・削除・成功後CPU自動実行（Simulator実ROM/計算入力確認）
  - [x] ホーム/階層メニュー、静的情報本文、共有BASIC/DMPロード・セーブ接続（[手順2](IOS_PLAYER_STEP2.md)、Simulator/合成検証と実機未確認を区別）
  - [x] PC-1245のPCM出力・サウンド/CPU速度設定（[手順3](IOS_PLAYER_STEP3.md)、合成・Simulator検証。実機・実ROM音声・聴感は未確認）
  - [x] PC-1245 GameController・Physical外部キーボード、全体コントローラ設定（[手順4](IOS_PLAYER_STEP4.md)。合成/Simulatorと実周辺機器を区別）
  - [ ] PC-1245 iPhone実機の包括的受入れ、実ROM・実周辺機器・最低OSの検証（第5段階）
- [x] Studioとのbas/dmpファイル受け渡し（PC-1245 Android）
  - [x] 共有変換・停止中Session検証／適用・保存データ生成とAndroidのファイル選択／保存を接続（他アプリへの共有機能は削除）
- [x] `.pgrom`選択と前回ROMの自動復元
- [x] 初回取込み後のROMをPlayerのアプリ専用領域へ保存し、機種切替後も機種別ROMライブラリから再利用する
- [x] Playerから保存済みROMの置換と明示削除を行えるようにする（Android、synthetic storage test／ユーザー動作確認済み）
- [x] LCDと機種別ソフトウェアキーボード
  - [x] CoreのDisplay SnapshotをPlayer共有LCDフレームへ変換し、Android Composeで可変サイズ描画する
  - [x] Machine Definitionに基づくPC-1245ソフトウェアキーボードを表示し、押下／解放を単一セッションへ接続する
- [ ] 実機スキンの表示部を画面へ拡大するController Displayモード
  - [x] 初期Androidプロトではゲームパッド接続に依存しない手動切替を用意し、LCD周辺のスキン切出し表示とソフトウェアキーボード非表示だけを薄く実装する
  - [x] スキン定義に全体表示領域、表示部切出し領域、LCD描画領域を分離して保持し、端末サイズに応じて同じ座標変換で拡大する
  - [x] 表示モードの状態とレイアウト計算をAndroid／iOSで共有可能なPlayer層へ置き、ゲームパッドAPIと画面描画はCoreへ持ち込まない
  - [ ] ゲームパッド対応時に接続検出から拡大表示を提案または自動選択し、切断時は通常表示へ安全に復帰する
- [x] RUN／PRO／RSV、Reset、Pause
  - [x] Androidの画面フレームで共有Runnerを駆動し、Run／Pause／ResetとLCD更新を接続する
  - [x] 機種定義の対応範囲に従ってRUN／PRO／RSV動作モードを切り替える
- [x] Android AudioTrackへのPCM接続とサウンドON／OFF（実装・自動テスト、ユーザー再生成功報告あり。環境・機種は未特定、機種別の実機確認は後続）
- [x] Android CPU速度の固定5段階と音声再生レートの連動（無制限は未実装）
  - [音声・速度対応記録](PLAYER_AUDIO_SPEED_PROGRESS.md)に実装・自動検証・エミュレータ・実機を分離
- [x] iOS音声APIへのPCM接続・速度設定UI（[手順3](IOS_PLAYER_STEP3.md)。実機と実ROM BEEPの受入れは別）
- [ ] セーブステートとモバイルライフサイクル
- [x] ゲームパッドのキー割当（Studio／Android／PC-1245 iOS、単一設定。iOSは[合成検証](IOS_PLAYER_STEP4.md)。接続による表示自動切替は後続）
- [x] Android Physical外部キーボード入力、フォーカス復帰、SHIFT単独、機種別の最小保持・解放間隔
- [ ] PlayerモバイルUIの仕上げ
  - [x] 実行画面を横向き専用にし、縦持ちには追従せず、左右180度反転した横向きには追従する（Androidは`sensorLandscape`相当）
  - [x] 3点メニューを薄いグレーの円形背景と十分なアイコン／タッチサイズで表示し、スキン上でも視認・操作しやすくする
  - [x] 16桁ドット表示に加えてSHIFT、DEF等の機種別LCDステータスをスキン内へ合成する
  - [x] PC-1245のRUN／PROをスキン内に明示する（Android、ユーザー動作確認済み）
  - [x] PC-1251／1261系のRUN／PRO／RSVは実機のハードスイッチ状態であっても、PlayerではLCDステータスとして明示する
  - [x] ホームの設定／ヘルプ・情報へ導線を整理し、Playerとは／Studioについて／バージョン・ライセンス／プライバシー原文案を静的同梱する（原文案の公開方針確定は別）
  - [x] 3点メニューからbas/dmpプログラムをロード／セーブできるようにする（Android、ユーザー動作確認済み。Playerの実行用入出力とし、Project機能とは分離する）
    - [x] bas/dmp拡張子の自動判定、確認なしのRAMロードとAndroidのファイル保存を実装する（docs/PLAYER_PROGRAM_IO.md、PC-1245 iOSはIOS_PLAYER_STEP2.mdで接続・合成検証済み）
  - [x] ホームの画面ボタンで機種選択、設定配下のROM管理、機種別Library、Session再生成を実装する（選択対象はPC-1245／PC-1401）

Pokecom GO Playerはエミュレータの実行に特化し、Pokecom GO Studioが提供するデバッガ、
アセンブラ、ディスアセンブラ、メモリ編集等の開発機能は実装しない。機能範囲は既存の
Pokecom GOを基準とする。

2026-10-14: Technical Preview 0.5を公開。[公開記録](RELEASE_CHECKLIST_0.5.0-alpha.1.md)。

2026-10-15: ユーザー方針によりデバッグ強化は0.5で一旦終了。未実装候補と依存条件は[残作業記録](DEBUGGER_FOLLOW_UP.md)へ整理。実装着手は別の指示後。
