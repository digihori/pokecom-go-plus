# iOS Player host — PC-1245表示・入力の構造確認

iPhone専用・最低iOS 16。SwiftUI/UIKit hostからPgpPlayer static frameworkを使用する。
共有Session接続、実行制御、スキン/LCD/タッチ入力、表示切替、ROM管理を実装済み。
実装順序と各項目の状態は [iOS実装計画](../docs/IOS_PLAYER_PLAN.md#実装順序と現在の状態) を参照。
通常起動はROM未登録画面、登録済みなら保存ROMからSessionを復元してCPUを自動実行する。
合成表示はDEBUGの `--structural-probe` 起動引数を付けた構造テスト専用。
bas/dmp入出力とゲームパッド、完全な階層メニューは後続。

## ビルドと実行

macOS Apple Silicon、Xcode/iOS Simulator、JDK 21、および既存Gradle環境が必要。
`PokecomGOPlayer.xcodeproj`の共有scheme `PokecomGOPlayer`を開き、iPhone Simulatorを選ぶ。
Apple Developer登録/Team/実機署名は不要。Intel Simulatorは現KMP構成の対象外。

Xcode build phaseが `build-shared.sh` を通じて
`:playerShared:embedAndSignAppleFrameworkForXcode` を実行する。
JAVA_HOME未指定ならmacOSの `/usr/libexec/java_home -v 21` でJDK 21を選ぶ。
Android Studio同梱Java 25を使わない。User Script SandboxingはこのGradle統合に必要なため無効。

```sh
env JAVA_HOME="$(/usr/libexec/java_home -v 21)" \
  xcodebuild -project iosApp/PokecomGOPlayer.xcodeproj -scheme PokecomGOPlayer \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath iosApp/build/DerivedData CODE_SIGNING_ALLOWED=NO build
```

同じコマンドの最後を `test -parallel-testing-enabled NO` にするとhost/UIテストを実行する。
機種名は `xcrun simctl list devices available` にある端末へ変更できる。
共有側は次のコマンドで検証できる。

```sh
env JAVA_HOME="$(/usr/libexec/java_home -v 21)" \
  ./gradlew --gradle-user-home .gradle-local :playerShared:iosSimulatorArm64Test
```

## 所有境界と操作

- Swift MainActorがSessionの全操作を直列化し、CADisplayLinkの単調時刻をRunnerへ渡す。
- SwiftUI scenePhaseがactive以外になったらframe停止、pause、全入力解放。
- 通常active復帰はPAUSED。ResetはRUNモードに戻し、次のactive frameで自動実行。
- 未消費の自動開始要求は非activeを越えて保持。明示Pauseで取消。
- 52キーはUIKitの複数touchを指ごとのtokenで共有HostKeyStateへ接続。押下ハイライトと解放フェードあり。
  指が元のキー外へ移動したら解放し、戻っても新しいtouch-downまで押下しない。
- 通常/Controller Displayは右端の矢印アイコンで手動切替。Controller Displayはスキン上部を切り出し、
  画面キーとそのaccessibility要素を無効化。切替前に全入力を解放し、Resetでも表示選択を維持する。
- スキン/LCD/キー/accessibility矩形は共有SkinLayoutEngineの同じ配置から算出。
  SwiftUI rootでsafe areaを一度だけ適用するため、native側は二重にinsetを差し引かない。
- LCDはコピーされた共有snapshot、80/20ドットと共有パネル/ステータス座標。DEGRADは連続segment、
  SHIFTは右揃え、RUN/PROはCPU実行状態と別表示。固定設計矩形へ字体を計測して収める。
- CoreはUI/OS/時刻/threadを所有しない。上部の直接操作欄は階層メニュー実装前の暫定導線。

有料Developer登録/実機検証/iOS 16 runtime検証は未完了であり、今回Simulator完了と分ける。
CI/リリースworkflowにはiOS app配布を追加していない。

## 現在の確認結果（2026-10-12）

- Xcode 26.6 / iOS Simulator 26.5。SE3・iPhone 12で表示/入力/回転を確認。
- Swift host 9件、UI 2件、共有Native Player 23件を通過。
- JDK 21による全体Gradle buildとCore checks成功。Core 408、Studio 195（1件skip）、
  shared desktop Player 20、Android 9。署名なしgeneric iPhone arm64ビルドも成功。
- iPhone 12で実ROMのpicker登録、CPU自動実行、タッチ計算 `7+3=10`、
  プロセス再起動後のROM復元・自動実行を確認。ユーザーもXcodeから起動・動作確認済み。
- 両横向き、portrait制限、Run/Pause/Reset、背景復帰PAUSED、通常/Controller Displayを確認。
- 最大Dynamic Typeでも表示を確認後、Simulator文字サイズを元のlargeへ戻した。
- 実機・iOS 16 runtime・署名付きArchive・TestFlight/App Store配布は未検証。

## ROM登録と保存

未登録画面の「ROMを選択」、または上部メニューの「機種／ROM」からFiles pickerを開く。
PC-1245の `.pgrom`（ZIP store/deflate）または32/64 KiB legacy ROMを選ぶ。
pickerはアプリのDocumentsから開始する。Files providerも利用可能。Simulatorへ検証ROMを置く場合は、
`xcrun simctl get_app_container <device-id> com.digihori.pgp.player.ios data` のDocumentsへコピーする。
ROMはリポジトリの追跡対象へ置かない。

全体検証後、内部/外部ROMとSHA-256を持つ正規packageへ変換し、Application Support/rom-libraryへ
metadataとまとめてatomic保存する。このlibraryはバックアップ対象外。保存成功後に新Sessionを公開する。
登録/置換/復元成功はCPUの自動実行であり、BASICのRUNコマンドは入力しない。
置換と削除には確認を表示する。取消/読込み・保存失敗では旧Sessionと保存ROMを保持しPAUSEDになる。
通常の背景復帰もPAUSED。再起動復元はROMのみで、RAM/CPUセーブステートではない。

ROM管理の開閉・picker・保存中は入力を解放し、保存中の重複操作を抑止する。
情報にはバージョンとプライバシーポリシー/Studioのプレースホルダーを表示する。
階層メニューの完成、プログラム入出力、ゲームパッドは未実装。

## Xcodeの基本操作と成果物

1. リポジトリのフォルダではなく `iosApp/PokecomGOPlayer.xcodeproj` を開く。
2. 上部schemeは **PokecomGOPlayer**、実行対象はiPhone Simulatorを選択する。
3. **⌘B** はビルド、**⌘R** はビルドして起動、**⌘U** はテスト。

今回作成した `PGP iPhone SE3` / `PGP iPhone 12` は開発Mac内の仮想端末の名前であり、
アプリの必須設定ではない。各端末でアプリとROM保存領域は別になる。
端末が一覧に出ない場合は **Manage Run Destinations… → Simulators** から選び、
Show run destinationをAlways（チェック式の場合は有効）にする。

Xcodeの通常ビルド成果物は概ね次の場所（英数字部分は環境ごとに異なる）。

```text
~/Library/Developer/Xcode/DerivedData/PokecomGOPlayer-<id>/Build/Products/Debug-iphonesimulator/PokecomGOPlayer.app
```

**Product → Show Build Folder in Finder** からBuildフォルダを開き、
Products/Debug-iphonesimulatorへ進む。Productsが左側に表示されない構成でも利用できる。
上記コマンドビルドの成果物は `iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/`。
シミュレータ用.appは実機へ配布できない。PgpPlayer.frameworkは静的リンク用の共有部品であり、
利用者へ別途配布するアプリではない。

## SimulatorへのROMコピー

アプリを一度インストールしてから、ターミナルで端末一覧を確認する。

```sh
xcrun simctl list devices available
```

対象端末のUUIDを使ってDocumentsをFinderで開き、手元のROMをコピーする。

```sh
open "$(xcrun simctl get_app_container <device-uuid> com.digihori.pgp.player.ios data)/Documents"
```

`<device-uuid>` は一覧に出たUUIDへ置き換える。ROM選択画面はこのDocumentsから始まる。
コピーだけでは登録にならないので、アプリの「ROMを選択」から選ぶ。
通常はFilesアプリの「このiPhone内 → Pokecom GO Player」からも見える。
Simulatorへのドラッグ&ドロップはファイル種類/OSによって扱いが異なるため、
任意のROMが保存されることを前提にせず、上記Documentsコピーを確実な手順とする。
アプリをアンインストール/仮想端末を消去すると登録ROMも失われる。元ROMは別途保管する。

## 将来の配布

現在はVersion 0.4.0 / Build 1、Bundle ID `com.digihori.pgp.player.ios`、Team未設定。
Developer登録後にTeam/配布用Bundle ID/Version/Build番号を確定する。
実行対象をAny iOS Device (arm64)にして **Product → Archive**、Organizerの
**Distribute App** からApp Store Connectへ送信し、TestFlight/App Store配布を進める。

| 成果物 | 用途 |
|---|---|
| .app | ビルドしたアプリ本体（Simulator用と実機用は別） |
| .xcarchive | アプリとデバッグ情報を保管するArchive |
| .ipa | Archiveから書き出す署名付きiPhone配布パッケージ |

Xcodeから直接アップロードする場合は手元へのIPA書き出しは必須ではない。
IPAをGitHubへ置くだけでは任意のiPhoneへインストールできず、署名/配布経路が必要。
ROMは配布成果物へ同梱しない。現在Archive/配布は未検証で、CI配布も未導入。
参照: [Appleの配布手順](https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases)。

## 次回の作業

階層メニューを完成し、bas/dmpロード、保存/共有、ゲームパッドを順に接続する。
BASICロード成功後のCPU自動再開（BASIC RUN入力ではない）はAndroid側も合わせる。
実装計画の9〜12と未完了の7を参照。iPhone開発はこの状態で一区切りとする。
