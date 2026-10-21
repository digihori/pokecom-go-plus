# PC-1245 iOS Player 調査・設計案

文書区分：調査・計画・実装／検証の記録（2026-10-22整理）。
過去の時制・件数・未確認事項を保持しています。現在の操作は[導入ガイド](../guides/GETTING_STARTED.md)、
[Androidヘルプ](../guides/PLAYER_HELP.md)、[iPhoneヘルプ](../guides/PLAYER_HELP_IOS.md)または対応する機能ガイドを参照してください。

## 現在の状態（2026-10-21）

iOSの14モデル・10 ROMシリーズ対応と、ホームの名前順・タイル／リスト・機種画面のみ横固定・ROM管理後のメニュー表示を実装し、ユーザーが一通り確認した。今回の対応は完了。音声の途切れ感は後続課題。最新の対応表と検証区分は[機種対応 手順8](IOS_MACHINE_SUPPORT_STEP8.md)と[ホーム修正記録](IOS_HOME_PRESENTATION_FIX.md)を参照する。

以下はPC-1245単体で進めていた時点の設計・段階別履歴であり、現在の機種追加予定や未着手項目を示すものではない。

## 2026-10-17時点の状態

PC-1245・iPhoneに限定した品質向上の手順1〜4を実装し、合成データ・Simulatorで可能な検証を完了。
現在の操作・検証一覧・残る受入れは[iosApp README](../../iosApp/README.md)を参照する。
実機、実provider、実ROMの音声/入力、聴感、最低OSでの実行は未確認で、第5段階の実機受入れは未完了。
PC-1401/PC-1350等のiOS追加は別工程。packageVersionは1.3.1を維持する。

- [手順1: ファイルピッカー](IOS_PICKER_STEP1.md)
- [手順2: ホーム・階層メニュー・BASIC/DMP](IOS_PLAYER_STEP2.md)
- [手順3: サウンド・CPU速度](IOS_PLAYER_STEP3.md)
- [手順4: ゲームパッド・Physical入力](IOS_PLAYER_STEP4.md)

PC-1245はユーザー選択により現行Androidと同じRUN/PROのみで、RSVは表示しない。
音声は「消音モードに従い、他アプリ音声と共存」（ambient）に確定。背景再生は追加しない。
以下は段階ごとの記録。各段階の「後続」「未決」「未着手」はその時点の状態であり、上記の現在状態を優先する。

## 今回の品質向上・手順4（2026-10-17）

ユーザーの第4段階の依頼によりGameController/Physical入力とホームの全体コントローラ設定を接続。
共有所有権・割当・単一機器選択と、Androidから共有化したPhysicalキー保持を再利用する。
[実装と合成/Simulator・実機の検証区分](IOS_PLAYER_STEP4.md)を参照。
実周辺機器・実ROMと包括的な実機受入れは第5段階として残す。

## 今回の品質向上・手順3（2026-10-17）

ユーザーが第3段階を依頼し、共有PCM・速度設定をSwiftへ接続。
音声方針はユーザー選択により「消音モードに従う・他アプリ音声と共存」（ambient）に確定。
[手順3の実装と検証区分](IOS_PLAYER_STEP3.md)を参照。
実ROMのBEEP・聴感・実機割込み/経路・最低OSは未検証。外部入力は第4段階。
以下の各段階と未着手時点の提案は、その時点の履歴として保持する。

## 今回の品質向上・手順2（2026-10-17）

今回ユーザーが第2段階を依頼。ホーム・階層メニューとBASIC/DMP入出力を接続する。
[現在の仕様・検証記録](IOS_PLAYER_STEP2.md)を参照。
PC-1245は現行AndroidどおりRUN/PROのみとする方針をユーザーが選択し、RSVは表示しない。
手順2終了時点では音声/速度・外部入力・実機受入れを後続とした。手順3/4の実装結果は上記を参照。

## 今回の品質向上・手順1（2026-10-16）

今回のユーザー依頼ではPC-1245に限定し、ファイルピッカー → ホーム・階層メニューと
BASIC/DMP → 音声・速度 → 外部入力 → 受入れの順に、一段階ずつ進める。
以下の「後続案」と2026-10-12の承認済み範囲は履歴として保持する。
ユーザーは現在実機を用意していないため、手順1はSimulatorで可能な範囲を確認し、
実機provider検証は未実施として残す。実機・署名の準備を後続全体の着手条件にはしない。

- 標準pickerは `forOpeningContentTypes: [.data], asCopy: true` を維持。
  独自拡張子を狭いUTType登録で除外せず、選択後にROM内容・機種を検証する。
  bas/dmpがROM pickerで選べても、プログラムとして適用する機能ではない。
- security scopeは専用I/O queueで読込み中だけ保持し、成功・失敗の両方で終了する。
  読込みは32MiB上限。検証・新Session生成はMainActor、atomic保存はI/O queue、
  保存成功後のSession公開はMainActorという既存境界を維持する。
- 選択後の読込み待機に取消を追加。取消で世代を無効化し、後着の成功・失敗を適用しない。
  待機中もMainActorは背景往復・取消を処理でき、取消・失敗では既存Sessionと保存ROMを保持して停止する。
  既に開始したFileHandle/provider処理自体の強制中断は保証しない。
  直列I/O queueのため、再取込みの読込みは先行処理の終了後になる。
- pickerがコピーを返す前のprovider内取得待ちは、アプリ内の読込み取消とは別。
  その待機・取消・認証・ネットワーク復旧は実providerでの確認が必要。
- ROMは取り込んだ内容をアプリ内Libraryへ保存し、復元は元URLへ依存しない。
  将来のBASIC/DMP保存でpickerが成功してもクラウド同期完了とは表示しない。
  Player独自の通信・ログイン・同期やGoogle SDKは追加しない。
  Files/providerの取得・同期通信は利用者側providerの処理として説明する。

検証結果と残る実機項目は [手順1検証記録](IOS_PICKER_STEP1.md) に分ける。
ホーム、BASIC/DMPの内容適用・セーブ、音声、入力機器の実装は本手順に含めない。

## 過去の提案: Android相当の品質へ揃える後続案（2026-10-16）

**2026-10-16時点: 調査・提案のみ。** 後に依頼された手順1〜4の実装・決定は上記を参照。
以下は従来の承認済み範囲とは別の将来案であり、実装着手の承認を意味しない。
まずPC-1245の品質を揃え、PC-1401／PC-1350等の機種追加は別工程とする案。

### 対応範囲

- ホームとPlayerを分離し、設定・ヘルプをホーム、本体操作・プログラム操作をPlayerへ配置。
  初回入場の自動実行、明示Pauseの維持、通常背景復帰の停止をAndroidの方針へ揃える。
- 状態表示のRun／Pauseトグル、独立RUN／PRO、メニューのReset・動作モードを整理。
- 既存ROM登録／置換／削除／復元を再利用し、新ホームと失敗・取消・再起動時の動作を検証。
- BASIC／DMPは既存共有Playerの検証・配置・保存処理をSwiftへ接続。
  ロード成功後はCPU再開（BASICのRUNコマンドは自動入力しない）。
  セーブは開始前の実行意図へ戻し、明示Pauseは維持。他アプリへの共有機能は追加しない案。
- サウンド出力、共通ON／OFF、CPU速度0.8／0.9／1.0／1.1／1.2倍と設定保存。
- GameControllerを共有入力所有権・単一パッド選択・割当・解放規則へ接続。
- 外部キーボードはPhysical入力を基本とし、単独SHIFT、モーダル・フォーカス・背景・切断を検証。
- Playerとは／Studioについて／プライバシー／バージョン・ライセンスの仮表示を置換。
  本文はビルド時同梱を基本とし、iOS固有の保存・クラウド利用について調整。
- 両横向き、ノッチ・ホームインジケータ、文字サイズ、複数指、最低対象OSと実機を検証。

### ファイルピッカーの方針案

iOS標準UIDocumentPickerViewControllerから、端末内・iCloud Drive・対応する他社File Providerへ
アクセスできる。iCloudを必須とはしない。「このiPhone内」とiCloud Driveを基本検証対象、
Google Driveを追加の実機検証対象とする案。
Google Driveは利用者がDriveアプリを設定し、「ファイル」アプリの場所として有効にする必要がある。
表示・認証・取得可否はアカウント、ネットワーク、プロバイダーの状態にも依存する。
専用Google SDK、Player独自のログイン、独自クラウド同期は追加しない案。

現行ROMピッカーは既に標準APIのコピー読込みを使用し、security scopeの開始・終了も実装済み。
これを再利用し、次の品質を補う。

- 未取得クラウドファイルの待機・失敗・取消・復帰で画面を固めず、旧Sessionを保護する。
- pgrom／bas／dmpの選択可否と内容検証、保存先選択・再ロードを実際のproviderで確認。
- ROMは検証後にアプリ内Libraryへ保存し、以後は元のクラウドURLへ依存しない。
- オフラインの取得済み／未取得を区別し、保存完了とクラウド同期完了を同一視しない。
- プライバシー説明ではPlayer自身の通信と、利用者が選択したクラウドproviderの通信を区別する。

### サウンドの方針案・未決事項

共有RunnerはPCMを生成するが、現行ApplePlayerFacade.tickは実行cycle数だけを取り出し、
PCMをSwiftへ公開していない。FacadeからPCM・速度を渡し、Swift側の
AVAudioEngine／AVAudioPlayerNodeへ接続する案。
無音時間の保持、蓄積・音切れ・停止後の鳴り残りを検証し、Pause・Reset・ホーム・背景で停止・破棄。
音声割込み、出力先変更、ヘッドホン切断、復帰を扱う。CPU倍率に音高・テンポも追従させる。

**未決:** 消音モードに従うか、他アプリ音声との共存方針。
消音モードに従い、その旨を設定に表示する案を提示した段階であり、ユーザー決定ではない。
背景再生は追加しない案。実機の聴感確認とSimulator／PCM経路確認を分ける。

### 着手時の推奨順序（未着手）

1. 標準ピッカーの実機調査：端末内・iCloud Drive・Google Drive、取得待ち・取消。
2. BASIC／DMPのロード・セーブ・再ロード。
3. サウンドとCPU速度：合成PCMから実ROMのBEEP、実機確認へ。
4. ホーム・階層メニュー・案内画面。
5. ゲームパッドとPhysical外部キーボード。
6. 実機受入れと文書整理。署名・配布は別途の指示とする。

共有CoreへOS／UI／filesystem／時刻／thread APIを追加せず、型付きエラーを維持する。
Android回帰、共有Native、Swift host／UI、自動検証と実機の区分を記録する。

出典:
- [Apple: ファイルアプリで他社製のクラウドアプリを使う](https://support.apple.com/ja-jp/102238)
- [Apple: Providing access to directories](https://developer.apple.com/documentation/uikit/providing-access-to-directories)
- [Apple: UIDocumentPickerViewController](https://developer.apple.com/documentation/uikit/uidocumentpickerviewcontroller)
- [Apple: AVAudioPlayerNode](https://developer.apple.com/documentation/avfaudio/avaudioplayernode)
- [Apple: AVAudioSession](https://developer.apple.com/documentation/avfaudio/avaudiosession)

## 2026-10-12の調査・実装記録

2026-10-12。調査時の記録と承認済み設計。実装項目1〜6と8のROM管理を完了。7は暫定メニュー/情報導線。

実装と操作は [iosApp README](../../iosApp/README.md)、検証・出典は
[PORTING_NOTES](PORTING_NOTES.md) の「iOS host and shared bridge — implementation items 1–4」を参照。
実ROMの登録・保存・復元・置換・削除からCPU/LCD/キー入力まで接続済み。
ユーザーの追加指示により、7〜8は実ROM動作を優先し、その他のメニュー処理は暫定。
bas/dmp入出力、ゲームパッドと階層メニューの完成は次の段階。

### 当時の承認済み目標

ユーザーはSwiftUI/UIKit host、iPhone専用、最低iOS 16、iPad対象外を承認した。
実機を所有しておらずDeveloper登録は後日のため、今回の完了目標はSimulatorによる構造確認。
Team設定・実機・配布検証は今回の完了条件に含めない。将来の端末候補はSE3またはiPhone 12。
現状環境と調査結果は以下に調査時点の記録として残す。

### 実装順序と現在の状態

2026-10-12時点。ユーザーは1〜4、5〜6、7〜8の順に実装を承認し、
7〜8では実ROM起動を優先して他のメニューは暫定とした。現在はiPhone開発を一区切りとする。
番号は以下の作業順序を指す。段階別の履歴はPORTING_NOTES、操作手順はiosApp/READMEを参照。

| 番号 | 作業内容 | 状態/完了条件 |
|---|---|---|
| 1 | 設計文書とSimulator完了条件 | 完了。本書に境界/対象/検証条件を記録 |
| 2 | iOS host/Xcode project | 完了。iPhone専用/iOS 16以上/両横向き、Simulator build |
| 3 | Kotlin framework/Swift Facade | 完了。Session/snapshot/入力/実行制御を接続 |
| 4 | Runner/lifecycle | 完了。frame駆動、Run/Pause/Reset、非active停止・解放、通常復帰PAUSED |
| 5 | PC-1245スキン/LCD/52キー | 完了。共有座標・タッチ所有・表示をSimulator確認 |
| 6 | 通常/Controller Display・safe area | 完了。両横向き、表示切替、ノッチ/文字サイズ確認 |
| 7 | 階層メニュー/情報 | 一部。機種/ROM・情報・上部操作は暫定。全階層の整理は後続 |
| 8 | ROM管理 | 完了。登録/保存/復元/置換/削除/失敗保持、実ROM起動・計算・再起動復元 |
| 9 | bas/dmpロード・成功後再開 | 未着手。Android BASICロード成功後もCPU自動再開へ揃える |
| 10 | BASIC/RAM範囲保存・共有 | 未着手。Files/共有sheetと内容round trip検証 |
| 11 | ゲームパッド | 未着手。共有入力とiOS GameController、接続/切断/割当検証 |
| 12 | 最終検証/文書更新 | 今回分は完了。9〜11と7完成後に最終検証を追加 |

次回は9・10のプログラム入出力を中心に、7のメニュー整理を進める。
実機/iOS 16 runtime/署名・配布はSimulator完了とは別の未完了項目。

1〜4は独自の合成プログラムと仮画面をSimulatorで使用する。ROMは不要。
共有NativeテストとSwift hostテストで接続/状態所有を確認し、UIテストで実フレーム駆動、
Run/Pause/Reset、両横向きと背景復帰PAUSEDを確認する。
最低OS設定が16でも、runtime 26.5のみでの成功をiOS 16動作確認とは扱わない。

### 現状と検証

- 開始時はmain、HEAD `36ddcc93ba8e44cdbdef091e9aef2816d8f3b4ee`、作業ツリーはクリーン。
- AGENTS.md、ROADMAP Phase 7、PORTING_NOTES、ARCHITECTURE、PLAYER_PROGRAM_IO、BASIC_TEXT_FORMAT、MACHINE_CODE_FORMATS、0.4 release notes、共有Player/テスト、Android host/テスト/manifest/skin、Gradle、hostInput/controllerUiを確認。
- core/playerShared/hostInputはiosArm64・iosSimulatorArm64対応（Kotlin 2.4.20）。Apple klibと共有テストはあるが、iOS host、Xcode project、framework公開設定、Swift Facade、Team、Bundle ID、最低OS設定はない。
- Player UIはAndroid PlayerApp.ktにある。playerSharedはUI依存なし。controllerUiはCompose設定画面だがAndroid/Desktopのみ。Compose plugin 1.12.1導入済みでも、iOS共有UI構成はない。
- Android ROM archive/hash/storageはJava API依存。CoreのRomPackageValidator、RomSet、PC-1245 legacy importerは再利用可能。
- Xcode 26.6 (17F113)、選択先 `/Applications/Xcode.app/Contents/Developer`、iOS/Simulator SDK 26.5、hostはarm64。
- 使用可能runtimeはiOS 26.5 (23F77)。iPhone 17/Pro/Pro Max/17e/Air、iPad Pro M5/Air M4/mini A17 Pro/A16のSimulator登録あり（確認時Shutdown）。
- devicectlの接続端末なし、有効codesigning identity 0件。Apple account/Teamの有無や将来の実機検証不可を断定する情報ではない。
- 指定JDK 21で `build :core:desktopTest` 成功（573 tasks、16実行、557 up-to-date）。Apple共有compile/Simulator testsはup-to-date。iOSアプリ起動・追加機能・実機検証は未実施。
- Gradle socket/Simulator serviceはsandboxで制限されたため、許可後にsandbox外で確認した。

文書の将来構想・過去の未検証記録を今回の追加依頼と扱わない。最新ユーザー依頼を優先する。過去PORTING_NOTESのDMP BASIC保護、ロード形式選択/確認、Reset PAUSEDは後続記録と現行実装で更新済み。0.4のBASICロードPAUSEDは公開時仕様として保持し、次の修正で新仕様に変更する。

### 推奨構成

**SwiftUI画面・階層メニュー + UIKit描画/タッチ/picker bridge + KMP共有ロジック**を第一案とする。Android動作を基準とし、Android画面全体のCompose共通化は今回の前提にしない。UI共有必須ならCompose iOS構成とPlayerApp抽出を別途設計する。

| 層 | 再利用・追加する責務 |
|---|---|
| Core | CPU、ROMモデル/検証、LCD snapshot、キー/モード、BASIC codec、DMP parser/writer、全RAM検証・適用 |
| playerShared/commonMain | PlayerSession/Runner、画面状態、機種選択、skin定義/座標変換、symbol formatter、program I/O、成功後の再開判断 |
| hostInput/commonMain | HostKeyState、単一pad選択、設定/割当、抑止・再押下規則 |
| iOS用Kotlin bridge | 小さなSwift Facade/DTO、byte変換、framework公開境界 |
| iOS host | UI/asset、CADisplayLink/時刻、lifecycle、orientation/safe area、Files picker/共有、ROM storage/ZIP/SHA、GameController、設定保存 |

playerSharedから単一static frameworkを作り、必要なCore/hostInput依存を明示exportする案。Swiftは小さなFacadeを経由しCPU内部へ直接依存しない。Xcode build phaseでGradle direct integrationを使用し、JDK 21と.gradle-localを指定する。frameworkリンクとSwift API可用性は第1段階で検証する。Coreはthreadを作成・所有せずcommonMainへOS/UI/file/time/device APIを追加しない。

最低OS設定は**iOS 16以上**、iPhone専用・iPad対象外で確定した。iOS 16そのものの動作は未検証。将来iPadを追加する場合はwindow/orientation条件を別途検討し、deprecatedのUIRequiresFullScreenだけで横向きを保証しない。開発用Bundle IDは `com.digihori.pgp.player.ios`、配布用確定とTeam設定はDeveloper登録後に行う。

### 操作案

#### ROM

- 未登録画面から取込み。機種/ROMはPC-1245のみ表示。
- pgrom / legacy 32・64KiBをpickerでコピー取込み。外部URL/bookmarkに恒久依存しない。
- bounded read → archive/manifest/hash検証 → RomSet → 新Session生成 → 正規package保存 → Session公開。ZIPライブラリは第2段階で選定、entry数/展開容量/重複/path traversal制限を設ける。
- Application Support内の機種別library、ROMはバックアップ対象外。temporary file/置換/rollbackで保存成功前は旧Session/RAM/選択を保持。rollback失敗は明示する。
- 起動時はROMから新Sessionを復元・自動実行。RAM/CPU save stateは復元しない。
- 置換/Session切替はRAM消失確認。削除確認、使用中ならSession破棄・未登録画面、別機種へ自動切替しない。
- 登録/復元/置換/切替/Reset成功はRUNモード・CPU自動実行、表示選択を維持。

#### 表示・入力・lifecycle

- Androidと同じ階層（実行、プログラム、動作モード、表示、機種/ROM、情報、ゲームパッド）。画面内パネルとバージョン表示、情報は既存placeholder。
- 通常/Controller Displayは手動切替。host assetは1206×616。LCD (70,104,760,70)、panel (55,60,787,158)、上方symbol配置、80/20ドットを共有定義に従って描画。
- symbolは矩形へ計測して収め、Dynamic Typeで欠けない描画。メニューは文字サイズに追従。RUN/PROとRUNNING/PAUSED/FAULTEDを別表示。1245にはRSVを出さない。
- landscapeLeft/Rightのみ。回転時Session/RAM維持・キー解放・safe area再計算。safe areaを一度だけ適用し、skin/LCD/touchに同じSkinPlacement。メニュー/状態/アドレス入力もnotch/home indicatorを避ける。
- 複数touchのpress/hold/release/cancelをHostKeyStateへ渡す。指が外れる、回転、非active、モーダルで解放。
- 初期はmain threadでRunner/Reset/apply/キー操作を直列化。CADisplayLink単調timestampをnanosecondへ変換。I/Oは別queueへcopy/snapshotを渡す。負荷測定後に必要ならhost-owned専用queueを検討。
- inactive/backgroundでframe停止・pause・入力解放。通常復帰PAUSED。
- ROM/Reset/load成功の自動開始要求はactiveの最初のframeで消費。未消費要求のみ背景を越えて保持。picker復帰だけでは再開しない。
- 非同期結果にはSession identity/操作generationを付け、置換・削除後の古い結果を適用しない。終了後の未完了操作は復元しない。sceneごとの重複Session生成を避ける。

#### bas/dmp

- ロード形式選択なし、拡張子判定、2MiB制限、厳密UTF-8。providerが独自拡張子を分類しない場合も選択可能にし、選択後検証。
- pause/解放 → copied bytesを共有parser → 所有threadでSession再確認 → 全体検証/apply。上書き確認なし。RAM限定、C000/BASIC重複を許可、失敗は途中変更なし。
- **bas/dmp成功はどちらもCPU再開、BASIC RUN文字列は入力しない。失敗/取消はPAUSED。**現在の共有applyは停止中操作であり時刻を持たない。成功を共通の再開判断へ渡し、hostがactive/時刻を処理する。AndroidのDMP限定分岐も置換。Core loaderへ自動実行を追加しない。
- 保存はBASIC全体、または開始～終了両端含むRAMダンプを選択（ロード形式選択とは別操作）。snapshotから固有temporary fileを作りexport picker(asCopy) / UIActivityViewControllerへ。iPadならpopover anchor指定。共有内容を後の操作で上書きしない。保存/共有後PAUSED。
- security scoped URLはhostで必要期間だけ使用。共有先受取り完了は実機確認。

### 実装順序・完了条件

| 段階 | 範囲 | 完了条件 |
|---|---|---|
| 1 | iOS host/Xcode、framework/Facade、synthetic表示、Runner/Run/Pause/Reset、両横向き/safe area、基本touch/lifecycle | 署名なしSimulator build/起動。SwiftからSession/snapshot/tick/入力。回転/非active解放、通常復帰PAUSED。既存build維持 |
| 2 | ROM管理/実ROM縦切り、LCD/52キー、Controller Display、階層/情報 | synthetic archive破損/rollback/削除検証。Simulator再起動復元/自動実行、両表示/両横向き。実機ROM起動は別判定 |
| 3 | picker/save/share、両形式成功再開・Android修正 | round trip/C000、不正UTF-8/容量/保護/aliasの無変更、取消/stale/背景待機。Android回帰。Files/共有先実機確認は別判定 |
| 4 | GameController adapter/選択/割当/永続化/設定画面 | 十字/物理位置face/L1/R1、単一pad、touch併用、切断/背景/モーダル解放、release後再押下。実pad検証。接続時表示自動切替なし |
| 5 | 端末受入れ・操作文書 | Simulator/実機項目を分けて記録、build/Core checks成功。commit/push/公開は別指示 |

**調査時の初回提案（現在は実装済み）。**Kotlin/Native→Swift接続と描画・入力・回転・停止復帰を検証してから永続化を進める。ROMなしでもsyntheticで開始可能。

Simulator完了: notchありiPhoneと別サイズ、左右回転、文字サイズ、通常/Controller、メニュー/入力、Runner/fault/lifecycle、ROM library失敗系、bas/dmp round trip、local Files picker/取消/share sheet。最低OS保証には最低OS runtime/端末を追加する（現環境は26.5のみ）。

実機完了: Team/Bundle IDで署名・install、ユーザーROM起動、両横向きnotch/home indicator、複数指/長押し/cancel、lock/背景/復帰/Reset、Files実利用provider、再ロード/save/share受取り内容、実pad接続/切断/割当。iPadを対象とするならwindow/sceneも検証。未実施を明記しSimulator成功を実機完了としない。

### 後続で確認する事項

実機端末/OS、署名Team、配布用Bundle IDはDeveloper登録後に確認する。
今回の構造確認はTeam空欄・署名なしのSimulatorで進める。private key/証明書を文書/Gitへ追加しない。

ゲームパッドはhostInputの共有基盤を再利用、iOS GameControllerの対応profile/available buttonsをhostで確認。controllerUiをそのままiOS利用せずSwiftUI設定画面を用意する案。外付けキーボードは後続: UIKit press/key commandまたはGCKeyboardから論理キー/共有文字入力へ。IME、記号配列、Command shortcut、モーダル抑止、key-up/focus lossを別設計。

他機種、WAV、Project、debugger、audio、save stateは今回対象外。Linux PC-1251音階確認をPC-1245速度不具合へ読み替えず、timing値を変更しない。

### 出典

第一資料は上記リポジトリ文書/現行実装。外部プロジェクトのコードコピーなし。ROM/ROM由来fixture追加なし。

- [KMP direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html): frameworkとXcode build phase。
- [UIDocumentPickerViewController](https://developer.apple.com/documentation/uikit/uidocumentpickerviewcontroller): import/exportとsecurity scope。
- [safeAreaInsets](https://developer.apple.com/documentation/uikit/uiview/safeareainsets): host安全領域。
- [UIRequiresFullScreen](https://developer.apple.com/documentation/bundleresources/information-property-list/uirequiresfullscreen) / [TN3192](https://developer.apple.com/documentation/technotes/tn3192-migrating-your-app-from-the-deprecated-uirequiresfullscreen-key): iPad orientation/windowの検討。
- [GameController extendedGamepad](https://developer.apple.com/documentation/gamecontroller/gccontroller/extendedgamepad): 対応profileから論理buttonへ変換。
- [CADisplayLink](https://developer.apple.com/documentation/quartzcore/cadisplaylink): 単調frame timestampとinvalidate。
- [SwiftUI scenePhase](https://developer.apple.com/documentation/swiftui/scenephase): active/inactive/backgroundのhost制御。
- [UIDevice orientationDidChangeNotification](https://developer.apple.com/documentation/uikit/uidevice/orientationdidchangenotification): 画面サイズが変わらない180度反転でも入力解放。
- [Native binaries/framework export](https://kotlinlang.org/docs/multiplatform/multiplatform-build-native-binaries.html): Core/hostInputのframework明示export。
