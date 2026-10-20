# 残作業と優先順位の検討用一覧

2026-10-13時点。ROADMAP、関連設計・操作文書、公開記録と一部の現行実装を照合して整理した。
優先度は見直し用の提案であり、実装着手や開発順序の確定を意味しない。
未チェックの親項目をそのまま未実装とは扱わない。直近CIの再取得や実機再検証は今回行っていない。

## 追加構想の管理（2026-10-20）

海外への公開・発信、多言語ドキュメント、ROM吸い出し支援、プラットフォーム拡張、
AI連携（Codex／Claude Codeによる解析・制作支援）、階層的な設計ドキュメントを追加構想として記録した。
[構想の詳細](PGP_CONCEPT.md#16-追加構想海外への展開と開発利用基盤の整備)と
[ロードマップの一覧](ROADMAP.md#追加構想の整理2026-10-20)を参照する。
既存の個別開発タスクと区別し、優先順位・実施範囲は今後決める。過去の一覧全体を最新状態へ再監査した記録ではない。

## ユーザーによる優先順位見直し

2026-10-13、アーキテクチャへの影響を重視し、外部Assembler基盤、Studio日本語／英語対応、
Projectの機種互換範囲・メモリ方針を優先候補とする。外部エディタではなく外部Assembler。
SC61初版は一区切りとし、次段階は内蔵／SC61／yasm.pl／その他（汎用）の構成を検討する。
空き領域RAMモードの実装は後続検討のまま。MSI更新インストールは次の修正タイミングで対応する。
以下のA〜Dは分類のために保持し、この見直し前の提案を確定した開発順とは扱わない。

## 機種追加前に片付ける今回の４項目（2026-10-13ユーザー指定）

1. Windows MSI更新インストール対応。
2. Windows音声ノイズ対策。
3. BRK入力の確認・必要な修正。
4. iOS CI対応。

2026-10-13ユーザー確認により、上記４項目の今回分は一旦完了とする。
Windows音声の改善、PC-1245／1360ゲームでのBRK認識はユーザー確認済み。
MSIの実測受入れは次回テクニカルプレビュー時、iOS hosted CIはpush後に確認する。
未記録の詳細検証は後続として保持し、今回の区切りを妨げない。commit／pushは別指示。

外部Assembler、Windows Projectツリー、外部エディタはユーザー確認により今回分を完了とする。
他ツールadapter・エディタ検出等の拡張は別の後続候補であり、今回の受入れを妨げない。
Playerの仮文言整理は2026-10-13ユーザーの追加指示で先延ばし。Quick Assemble等は今回の４項目には含めない。

実装・自動検証・Windows受入れの詳細は [今回の確認記録](PLATFORM_FIXES_2026-10.md) を参照。
MSIは公開0.4のUpgradeCodeを固定しpackageVersionを1.3.1へ更新。Windows／Linux動作とWindows更新インストールを2026-10-14にユーザー確認済み。
2026-10-16のWindows fixtureタイムアウトとiOS初回RunのUIテスト対策は、[CI安定化記録](PLATFORM_FIXES_2026-10.md#2026-10-16-player音声速度追加後のci安定化)を参照。
Technical Preview 0.5の公開作業は[チェックリスト](RELEASE_CHECKLIST_0.5.0-alpha.1.md)へ記録する。

## A: 先に確認・解消したいもの

| ID | 項目 | 残作業・現在の状態 |
|---|---|---|
| A1 | Windows音声のクリックノイズ | 空／無音PCMでラインを終了する修正済み。2026-10-13ユーザーからWindowsで改善報告あり。通常再生・自然終了・BREAK/Pause/Reset/アプリ終了の個別結果は未記録 |
| A2 | マシン語実行中のBRK入力 | 保持中にKEY ONが消える問題を修正。全機種合成TEST 8とRunner所有者テストを追加。ユーザーがPC-1245／1360のゲームでBRK認識を確認。入力別・速度別・実機照合の個別結果は未記録 |
| A3 | SC61のWindows実機連携 | ユーザー確認により今回分を完了。SC61初版は一区切り。将来のadapter拡張は別項目 |
| A4 | iOS CI | 今回分は一旦完了。共有Native・framework・署名なしiPhone Simulatorのローカル検証成功。ユーザー方針によりhosted CIはpush後に確認 |
| A5 | 現行Windows/Linux手動スモーク | 過去の配布・起動確認と、最新変更の操作確認を分ける。音声、ファイル入出力、ゲームパッド等を確認 |

## B: 次の機能開発の候補

| ID | 項目 | 残作業・現在の状態 |
|---|---|---|
| B1 | PC-1245 iOS品質向上 | [後続案](IOS_PLAYER_PLAN.md)を2026-10-16に記録。標準Files経由のiCloud／Google Drive、bas/dmpロード・セーブ、サウンド・CPU速度、ホーム整理・Physical入力。[手順1](IOS_PICKER_STEP1.md)のSimulator範囲を完了し、[手順2](IOS_PLAYER_STEP2.md)でホーム・階層メニュー・静的案内・共有BASIC/DMPを接続。[手順3](IOS_PLAYER_STEP3.md)でPCM出力・速度5段階・設定保存を接続。消音に従い他アプリ音声と共存する方針をユーザーが選択。実機/実provider・実ROM音声・聴感は未確認。[手順4](IOS_PLAYER_STEP4.md)でGameController・Physical入力・全体コントローラ設定を接続。実周辺機器を含む受入れは第5段階 |
| B2 | iOSゲームパッド | [手順4](IOS_PLAYER_STEP4.md)で共有入力・GameController・選択/割当/保存/解放を接続し合成検証。実パッド・接続方式ごとの受入れは未確認 |
| B3 | Android Playerの機器別追加検証 | PC-1401は2026-10-15に予定した6段階を完了し、ユーザーが今回の区切りを承認。[検証区分](ANDROID_PC1401_PROGRESS.md)を保持。PC-1350も2026-10-16にユーザーが一通りの動作とLCD改善を確認し、今回の対応を一区切りとした。[段階別検証と機器別の未確認事項](ANDROID_PC1350_PROGRESS.md)を保持。両機種のiOSは別検討 |
| B4 | Desktop物理キー入力モード | 現在の文字/論理入力と、押下/解放を直接送る入力を切替。SHIFT、同時押し、repeat、focus喪失を扱う。A2との関連を確認 |
| B5 | Quick Assemble結果表示 | DMP保存に加え、機械語・Listing・Symbol Map・診断を確認する画面 |
| B6 | プロジェクト機種互換範囲 | BASIC単独の機種非限定化、現在機種への変換、混在プロジェクトの互換グループ、旧Manifest互換 |
| B7 | 外部エディタ設定の仕上げ | ユーザー指定により現在の対応を一旦完了とする。代表的エディタの検出・Test起動等は将来の拡張候補 |
| B8 | Studio日本語/英語UI | 2026-10-14ユーザーによる一通りの確認を受領し今回分を完了。対象画面・共有UI・エラー表示、OS追従と設定の言語上書き（再起動反映）を実装。36 tests、日英870 key、保存互換性、全体build／Core tests成功。確認OS・個別操作の内訳は未指定。Windows／Linux個別結果・OS設定変更試験・独立描画の欠けは後続確認（[最終検証](STUDIO_LOCALIZATION_VERIFICATION.md)、[棚卸し](STUDIO_LOCALIZATION_INVENTORY.md)、[進捗](STUDIO_LOCALIZATION_PLAN.md)） |

## M: Studio/Coreの機種対応（優先順位を独立して検討）

現在のCore/Studio対応機種はPC-1245、1250、1251、1255、1260、1261、1350、1360、1401、1402、1450、1460、1470U。
対応済みは全ROM版・全周辺機能の実機一致を保証する意味ではない。
Android/iOS Playerへの機種追加（B3）とは別の作業として扱う。

| ID | 項目 | 残作業・現在の状態 |
|---|---|---|
| M1 | PC-1260/1262 | PC-1261実装を基準に容量・ROM/BASIC差分を確認し、共通ファミリー、Studio選択、ROM Library、ロード検査を対応。その時点でExpanded RAM切替を検討（D1と同じ作業） |
| M2 | PC-1402 | PC-1401を基準にRAM容量差とROM互換性を確認し、機種定義・Studio選択・ロード検査へ接続 |
| M3 | PC-1450 | Core/Studio実装・合成／ローカルROM検証済み。実機／Windowsユーザー操作は別。詳細はmachines/PC-1450.md |
| M4 | PC-1460 | Core/Studioの4バンク・24桁LCD・S1実装済み。合成／ローカルROMと実機確認を区別。詳細はmachines/PC-1460.md |
| M5 | PC-1470U | Core/Studioの8バンク・24桁×2行・専用S2/キー実装済み。↕︎キーの入力は対応済み、SHIFT TITLE機能固有の条件は未確認。実機照合は別。詳細はmachines/PC-1470U.md |
| M6 | PC-1360K | 漢字ROMの配置・選択、文字コード、表示仕様を実機調査。仕様未確定のため機種実装は保留。C3の解析支援と関連 |
| M7 | 既存機種の互換性確認 | PC-1245/1250のRAMミラー、機種固有BASIC/文字コード、ROM版差、BRK等を確認。既存PC-1360のバンク実装を新規作業に数えない |

2026-10-13の追加依頼でM3→M4→M5の順に実装。M1/M2/M6は今回の対象外。
空き領域RAMモード（D2）は機種追加とは独立した検討項目。

## C: 開発支援・解析の拡張

2026-10-15ユーザー方針: デバッグ強化は0.5で一旦終了。残りは[記録](DEBUGGER_FOLLOW_UP.md)として保持し、対象指定後に再開する。

| ID | 項目 | 残作業・現在の状態 |
|---|---|---|
| C1 | Debuggerとソースの連携 | Assembly Listing、Symbol/Source Map、ソース行表示を接続 |
| C2 | Debugger操作・表示 | 固定操作・停止情報・配置（[ステップ1](DEBUGGER_STEP1_VERIFICATION.md)）、値変更強調／比較基準管理（[ステップ2](DEBUGGER_STEP2_VERIFICATION.md)）、読み書き／bank履歴の表示・絞込み・移動（[ステップ3](DEBUGGER_STEP3_VERIFICATION.md)）、登録一覧／編集／有効切替・複数監視（[ステップ4](DEBUGGER_STEP4_VERIFICATION.md)）は実装済み。条件付きBreakpoint、設定永続化、Memory検索／編集、Trace/Access export等は後続 |
| C3 | 実機Checkpoint比較 | 実機側採取手順、差分ツール、PC-1360Kの漢字ROM/work RAM観測。機種登録は仕様不明で保留 |
| C4 | AI/MCP相互運用 | Claude Code等の接続・認証・設定案内、schema整理、BASIC/Assembly解析E2E。読み取り専用サーバーは実装済み |
| C5 | AI/MCP操作権限 | Breakpoint/Watch/Pause/Step、ソース差分適用、Load/Continue/Memory書込み、承認・履歴・送信範囲検証。内蔵AI Providerは外部運用評価後 |
| C6 | Intel HEXとRaw Binary導線 | HEX入出力、開始アドレス付き一度限りのBinary import、DMP/HEX中心の新規Project、旧Manifest互換 |
| C7 | Character Editor | Undo/Redo、複数文字、ファイル入出力、Emulator preview、機種別UDC |
| C8 | キー操作の記録・再生 | 開始状態、cycle基準、入力所有権、中断・ループ、保存形式を先に設計。任意状態復元にはSave Stateが必要 |
| C9 | 内蔵Assemblerの追加構文 | マクロ、Include、条件Assembly、展開後の元行対応。外部連携を先に進める既存方針を維持 |
| C10 | 外部Assemblerの他ツール対応 | 現在の成果物判定・LST解析はSC61向け。YASM等は実需要に応じadapter/出力形式を検討 |

## D: 後続・調査・環境準備が必要なもの

| ID | 項目 | 残作業・現在の状態 |
|---|---|---|
| D1 | PC-1260/1262 | ROM/RAM/BASIC差分を確定し共通ファミリー化。その時点で実機RAM/Expanded RAM容量切替を検討 |
| D2 | 空き領域をRAMとみなすモード | D1の容量切替と別項目。機種別に対象を確認しROM/LCD/制御領域を守る。ロードとCPU処理を一致させる。次回以降 |
| D3 | その他機種・仕様差 | PC-1402用ROMのBASIC実行照合、PC-1460/1470Uの実機／最大容量照合、機種固有BASIC、PC-1245/1250ミラー実機照合。PC-1360バンクは実装済み |
| D4 | S1/S2 WAV | 信号・BASIC/Binary codec、checksum、実機往復。Playerを優先して後回し、当面pcwavを案内 |
| D5 | WAV Analyzer追加 | 全信号形式、候補比較、ON/OFF/W0/W1/leader/byte境界overlay、原channel、段階間選択、個別export、実録音fixture。OLD波形/zoom/pan/cursor等は実装済み |
| D6 | Player音声・速度・Save State | Android音声出力／速度5段階を実装。ユーザーから再生成功報告あり（環境・機種未特定）。[検証区分と実機の残作業](PLAYER_AUDIO_SPEED_PROGRESS.md)。iOS接続は[手順3](IOS_PLAYER_STEP3.md)で実装・合成検証。iOS実機音声、無制限、完全状態の保存復元は後続。現在のCheckpoint出力とは別 |
| D7 | Player後続UI・入力 | パッド接続時の表示自動切替、追加機種のRUN/PRO/RSV表示。AndroidのPhysical外付けキーボードは今回実装済み |
| D8 | iOS実機・最低OS・配布 | Developer登録/署名、端末操作、Files共有受取り、実パッド、iOS 16確認。Simulator完了と別。iPadは現在対象外 |
| D9 | ROM管理の拡張 | Import Wizard、bank slot一括割当、機種拡大時のPlayer Library、ROMをバックアップ/同期へ含めない方針 |
| D10 | 設定・公開運用 | 最近使ったプログラム、次期Previewの差分・版数・配布物検証。PlayerのStudio説明と情報画面は静的同梱へ整理済み。プライバシー原文案の公開方針・適用開始は別途判断 |
| D11 | 低優先度機能 | PC-1251のユーザー登録予約語ショートカット、その他実需要を確認してから着手するもの |

## ロードマップの記録整理

新規実装として数えず、状態・文言を整理したい項目:

- Public化、3 OS配布、tagによる公開自動化は公開チェックリストで完了。現行版の手動確認だけを残作業とする。
- ゲームパッドはStudio/Android/PC-1245 iOSで実装済み。iOSの合成検証と、実周辺機器・OS/device別受入れ・表示自動切替の未対応を区別する。
- 外部エディタ設定保存と診断行移動は実装済み。Phase 3/5の未チェック項目と重複を整理する。
- Assembly WorkspaceでSource/Assembly/Build/Load状態は表示済み。Project全体のBuild/Update分離は追加の差分を定義する。
- PC-1350の実装、資産由来確認は完了記録と未チェックの親項目を照合する。
- 外部Assembler調査欄の「INCLUDE未確認」「Build連携は後続」等の段階別記述を現在の状態と区別する。
- Debugger Core、AI schema、ROM Library等の大きな親項目は、完了した基盤と残る具体的機能へ分解する。
- GAMEPAD_DESIGNは手順4の現在状態を追記し、2026-10-11の「iOS未実装」等を当時の記録として区別した。実周辺機器の受入れ記録を後続で追加する。
- WAV Analyzerの波形表示等の未チェック項目は、OLDで実装済みの範囲と全形式への拡張を分ける。

参照: [ROADMAP](ROADMAP.md)、[外部Assembler](EXTERNAL_ASSEMBLER.md)、[iOS計画](IOS_PLAYER_PLAN.md)、
[Debugger](DEBUGGER.md)、[ゲームパッド](GAMEPAD_DESIGN.md)、[WAV](WAV_OPERATIONS.md)、
[公開確認](PUBLIC_RELEASE_CHECKLIST.md)、[0.4公開記録](RELEASE_CHECKLIST_0.4.0-alpha.1.md)。


### 2026-10-14 の確認と残課題

- Studio LCD比率は1460だけ3:4、他機種は1:1。ユーザー確認済み。
- 関数panelは6列×3行。1401/1450/1460の通常「↕︎」／SHIFT STAT、1470Uの通常「↕︎」／SHIFT TITLE刻印は一旦OK。
- 保留: Unicode矢印の絵文字表示（フォント選択・fallback）、1470UのSHIFT TITLE機能固有の操作条件・結果。両者は別課題。
- 全体buildとCore tests成功。ROM、作成中skin、Player新機種選択、仮文言整理は今回のcommit対象外。


### 2026-10-14 126x / 140xシリーズ整理

- 125xと同じ考え方で、機能はシリーズ共通、機種差はユーザーメモリ容量。外部ROM版はユーザー提供イメージを使用し、既知hashで制限しない。
- Core/Studioの追加IDは1260／1402。1262は1261の価格改定版なので選択ラインナップから除外。
- 容量別BASIC既定開始位置・RAM境界を接続し、ROM Libraryは各シリーズで共用。既存登録パスを維持。
- RAMミラーの追加は保留（実機でミラーしていない可能性あり）。LCD既存ミラーは維持。
- ローカル1261 ROMで1260/1261を確認。1402の実ROM検証はROM未提供で未実施。Player新機種／Project互換方針変更／メモリーカード拡張は別。

- ROM登録画面はPC-1261 family／PC-1401 familyへ集約。実行機種選択は個別。
- ユーザー確認: 1260/1261のMEM値は異なる。1402 ROMでは1401選択時に1401容量となる。全ROM版互換や1402のBASIC実行の確認とは区別する。

- 1470U再確認: ↕物理キーのbit2接続・SHIFT消費と、SHIFT＋「=」のSTAT ON/OFFをローカルROMで確認。TITLE機能固有の結果は未確認で残す。

- ユーザー指示: TITLE機能は使用経験がなく取説の記載も見つからないため、詳細確認はペンディング。追加調査は行わず、確認済みの↕入力・STAT切替を維持する。
