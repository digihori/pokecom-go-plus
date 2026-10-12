# PC-1245 iOS Player 調査・設計案

2026-10-12。調査時の記録と承認済み設計。実装項目1〜6と8のROM管理を完了。7は暫定メニュー/情報導線。

実装と操作は [iosApp README](../iosApp/README.md)、検証・出典は
[PORTING_NOTES](PORTING_NOTES.md) の「iOS host and shared bridge — implementation items 1–4」を参照。
実ROMの登録・保存・復元・置換・削除からCPU/LCD/キー入力まで接続済み。
ユーザーの追加指示により、7〜8は実ROM動作を優先し、その他のメニュー処理は暫定。
bas/dmp入出力、ゲームパッドと階層メニューの完成は次の段階。

## 承認済みの今回の目標

ユーザーはSwiftUI/UIKit host、iPhone専用、最低iOS 16、iPad対象外を承認した。
実機を所有しておらずDeveloper登録は後日のため、今回の完了目標はSimulatorによる構造確認。
Team設定・実機・配布検証は今回の完了条件に含めない。将来の端末候補はSE3またはiPhone 12。
現状環境と調査結果は以下に調査時点の記録として残す。

## 実装順序と現在の状態

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

## 現状と検証

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

## 推奨構成

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

## 操作案

### ROM

- 未登録画面から取込み。機種/ROMはPC-1245のみ表示。
- pgrom / legacy 32・64KiBをpickerでコピー取込み。外部URL/bookmarkに恒久依存しない。
- bounded read → archive/manifest/hash検証 → RomSet → 新Session生成 → 正規package保存 → Session公開。ZIPライブラリは第2段階で選定、entry数/展開容量/重複/path traversal制限を設ける。
- Application Support内の機種別library、ROMはバックアップ対象外。temporary file/置換/rollbackで保存成功前は旧Session/RAM/選択を保持。rollback失敗は明示する。
- 起動時はROMから新Sessionを復元・自動実行。RAM/CPU save stateは復元しない。
- 置換/Session切替はRAM消失確認。削除確認、使用中ならSession破棄・未登録画面、別機種へ自動切替しない。
- 登録/復元/置換/切替/Reset成功はRUNモード・CPU自動実行、表示選択を維持。

### 表示・入力・lifecycle

- Androidと同じ階層（実行、プログラム、動作モード、表示、機種/ROM、情報、ゲームパッド）。画面内パネルとバージョン表示、情報は既存placeholder。
- 通常/Controller Displayは手動切替。host assetは1206×616。LCD (70,104,760,70)、panel (55,60,787,158)、上方symbol配置、80/20ドットを共有定義に従って描画。
- symbolは矩形へ計測して収め、Dynamic Typeで欠けない描画。メニューは文字サイズに追従。RUN/PROとRUNNING/PAUSED/FAULTEDを別表示。1245にはRSVを出さない。
- landscapeLeft/Rightのみ。回転時Session/RAM維持・キー解放・safe area再計算。safe areaを一度だけ適用し、skin/LCD/touchに同じSkinPlacement。メニュー/状態/アドレス入力もnotch/home indicatorを避ける。
- 複数touchのpress/hold/release/cancelをHostKeyStateへ渡す。指が外れる、回転、非active、モーダルで解放。
- 初期はmain threadでRunner/Reset/apply/キー操作を直列化。CADisplayLink単調timestampをnanosecondへ変換。I/Oは別queueへcopy/snapshotを渡す。負荷測定後に必要ならhost-owned専用queueを検討。
- inactive/backgroundでframe停止・pause・入力解放。通常復帰PAUSED。
- ROM/Reset/load成功の自動開始要求はactiveの最初のframeで消費。未消費要求のみ背景を越えて保持。picker復帰だけでは再開しない。
- 非同期結果にはSession identity/操作generationを付け、置換・削除後の古い結果を適用しない。終了後の未完了操作は復元しない。sceneごとの重複Session生成を避ける。

### bas/dmp

- ロード形式選択なし、拡張子判定、2MiB制限、厳密UTF-8。providerが独自拡張子を分類しない場合も選択可能にし、選択後検証。
- pause/解放 → copied bytesを共有parser → 所有threadでSession再確認 → 全体検証/apply。上書き確認なし。RAM限定、C000/BASIC重複を許可、失敗は途中変更なし。
- **bas/dmp成功はどちらもCPU再開、BASIC RUN文字列は入力しない。失敗/取消はPAUSED。**現在の共有applyは停止中操作であり時刻を持たない。成功を共通の再開判断へ渡し、hostがactive/時刻を処理する。AndroidのDMP限定分岐も置換。Core loaderへ自動実行を追加しない。
- 保存はBASIC全体、または開始～終了両端含むRAMダンプを選択（ロード形式選択とは別操作）。snapshotから固有temporary fileを作りexport picker(asCopy) / UIActivityViewControllerへ。iPadならpopover anchor指定。共有内容を後の操作で上書きしない。保存/共有後PAUSED。
- security scoped URLはhostで必要期間だけ使用。共有先受取り完了は実機確認。

## 実装順序・完了条件

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

## 後続で確認する事項

実機端末/OS、署名Team、配布用Bundle IDはDeveloper登録後に確認する。
今回の構造確認はTeam空欄・署名なしのSimulatorで進める。private key/証明書を文書/Gitへ追加しない。

ゲームパッドはhostInputの共有基盤を再利用、iOS GameControllerの対応profile/available buttonsをhostで確認。controllerUiをそのままiOS利用せずSwiftUI設定画面を用意する案。外付けキーボードは後続: UIKit press/key commandまたはGCKeyboardから論理キー/共有文字入力へ。IME、記号配列、Command shortcut、モーダル抑止、key-up/focus lossを別設計。

他機種、WAV、Project、debugger、audio、save stateは今回対象外。Linux PC-1251音階確認をPC-1245速度不具合へ読み替えず、timing値を変更しない。

## 出典

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
