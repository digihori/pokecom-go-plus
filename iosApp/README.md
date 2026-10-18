# iOS Player host — PC-1245

iPhone専用・最低iOS 16。SwiftUI/UIKit hostからPgpPlayer static frameworkを使用する。
共有Session接続、実行制御、スキン/LCD/タッチ入力、表示切替、ROM管理に加え、
ホーム・階層メニュー・BASIC/DMPロード/セーブと静的同梱の情報画面を接続した。
通常起動はホーム。登録済みROMは停止状態で復元し、初回プレイ入場でCPUを自動実行する。
同Sessionのホーム往復は実行意図を保ち、通常背景復帰は停止する。
合成表示はDEBUGのSimulator probe専用。共有PCMの音声出力・CPU速度5段階・設定保存を接続。GameController・Physical入力と全体コントローラ設定を接続。実周辺機器の受入れは未実施。

最新の外部入力・検証区分は[手順4](../docs/IOS_PLAYER_STEP4.md)。音声・速度は [手順3](../docs/IOS_PLAYER_STEP3.md)、ホーム・入出力は [手順2](../docs/IOS_PLAYER_STEP2.md) を参照。
以下に残る2026-10-12の暫定導線・件数・配布情報は当時の段階別記録。

## 現在の対応と検証（2026-10-17）

手順1〜4は実装と合成入力・Simulatorで可能な検証まで完了。第5段階の実機受入れは未完了。
PC-1245のRUN/PROのみを扱い、RSV・他機種・iPadは含めない。

| 段階 | 対応・記録 |
| --- | --- |
| 1 | [標準Files picker、コピー読込み、取得取消・失敗時の保護](../docs/IOS_PICKER_STEP1.md) |
| 2 | [ホーム・階層メニュー、BASIC/DMP、静的情報本文](../docs/IOS_PLAYER_STEP2.md) |
| 3 | [共有PCM、音声出力、速度5段階、設定保存](../docs/IOS_PLAYER_STEP3.md) |
| 4 | [GameController、Physical入力、全体コントローラ設定](../docs/IOS_PLAYER_STEP4.md) |

最新の確認: 全体build・Core desktop 436件、Android unit 43件、共有Player desktop/Android各60件、
共有Player iOS Native 70件、hostInput desktop/Android/iOS Native各12件、Swift33件、UI6件が成功。
CoreのUP-TO-DATE、Swift/UIの分割再実行や途中の失敗は[手順4の検証表](../docs/IOS_PLAYER_STEP4.md#検証)に区別する。
署名なしiPhone向けビルドは成功したが、実機実行やiOS 16 runtime検証を意味しない。

以下の操作案内は現在の実装を示す。過去の提案・環境・件数は各段階の記録として読む。

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

ビルド先の機種名は `xcrun simctl list devices available` にある端末へ変更できる。
host/UIテストは、既存端末のROM Libraryや設定に触れない使い捨てSimulatorのスクリプトを使う。
合成probeを通常利用中のSimulatorへインストールしない。

```sh
env JAVA_HOME="$(/usr/libexec/java_home -v 21)" sh iosApp/test-simulator.sh
```

共有側は次のコマンドで検証できる。

```sh
env JAVA_HOME="$(/usr/libexec/java_home -v 21)" \
  ./gradlew --gradle-user-home .gradle-local :playerShared:iosSimulatorArm64Test
```

## 所有境界と操作

- Swift MainActorがSession操作を直列化し、CADisplayLinkの単調時刻を共有Runnerへ渡す。
  音声workerにはSwift所有のPCMコピーだけを渡し、CoreはOS・UI・時刻・threadを所有しない。
- 起動時はホーム。初回/新Sessionのプレイ入場はCPU自動実行。
  同Sessionのホーム往復は実行意図を保つ。明示Pause後は停止を維持する。
  通常の背景復帰はPAUSEDで入力を解放する。ResetはRUNモードへ戻してCPUを自動実行する。
- ホームは機種選択と「設定」「ヘルプ・情報」。設定順はROM管理→CPU速度→サウンド→コントローラ設定。
  案内・ライセンスはビルド時同梱。利用者向け操作は[同梱ヘルプ](../docs/PLAYER_HELP_IOS.md)を参照。
- PlayerタイトルバーはPC-1245・CPU実行状態・RUN/PROボタン・3点メニュー。
  状態表示をタップしてRun/Pause。「エミュレータ操作」にもRun/Pause/Resetを残す。
  「プログラム」はロード/セーブ、「動作モード」はRUN/PRO、「表示」は通常/Controller Display。
- BASIC/DMPロード成功でCPUを再開し、BASICのRUNコマンドは自動入力しない。
  取消・検証失敗はメモリを変えず停止を維持。セーブ終了後は開始前の実行意図へ戻すが、背景移行で再開意図を取り消す。
- サウンドは消音モードに従い、他アプリ音声と共存する。背景再生なし。
  速度は0.8/0.9/1.0/1.1/1.2倍で音高・テンポも追従する。初期値は1.0倍・サウンドON。
- ゲームパッドは1台、10入力を1セットで割当・自動保存。初期OFF/未割当。
  Physicalキーボードは文字変換・IMEを使わず、共有の保持/解放/短押し処理へ接続する。
- タッチ・パッド・Physicalキーは同じ共有所有権を使用する。メニュー、モーダル、フォーカス喪失、
  背景、切断等で解放し、保持中の入力を復帰時に再送しない。
- Controller Displayはスキン上部を切り出し、タッチキーとaccessibility要素を無効化する。
  外部入力は利用可能。表示切替前に全入力を解放し、Resetでも表示選択を維持する。
- 52タッチキーは指ごとのtokenを持ち、範囲外へ移動したら解放する。
  スキン/LCD/キーの配置は共有SkinLayoutEngineから算出し、safe areaはrootで一度だけ適用する。
  iOSのLCDドット拡大は実機の見え方を踏まえる残項目で、Androidの88%へ変更していない。

## ROM登録と保存

ホームの「ROMを登録」、または「設定」→「ROM管理」から標準Files pickerを開く。
PC-1245の `.pgrom`（ZIP store/deflate）または32/64 KiBのlegacy ROMを選ぶ。
読込みはコピーURLとsecurity scopeを使い、専用I/O queueで行う。取得取消・検証失敗では旧Session/保存ROMを保護する。

検証後、正規packageとmetadataをアプリ内Libraryへatomic保存し、成功後だけ新Sessionを公開する。
以後の復元は元URLに依存しない。Libraryはバックアップ対象外。RAM/CPUのセーブステートは保存しない。
登録・置換後もホームではCPUを実行せず、プレイ入場で自動実行する。
置換・削除は確認を表示し、取消・失敗では既存Session/保存ROMを保持して停止する。

基本対象は「このiPhone内」とiCloud Drive。Google Driveは利用者がDriveアプリを設定し、Filesで有効にした場合の追加対象。
独自ログイン・クラウド同期・Google SDKは追加しない。Player自身の通信とproviderの取得/同期通信を区別する。
ロード・セーブのFiles pickerはDocumentsから開く。選択画面はimport／export URLを持つ
単一のpresentationとして管理し、キャンセルしても元のプログラム画面を維持する。
保存picker完了はクラウド同期完了を意味しない。実providerの検証は未実施。

## 残る受入れ

現在実機を用意していないため、以下を未確認として残す。機材準備時にDeveloper登録・署名環境を改めて確認する。

- iPhone実機、最低iOS 16、現行OSを分けた起動・両横向き・ノッチ/ホームインジケータ・文字サイズの確認。
- 端末内/iCloud Drive/Google Driveで選択・未取得待ち・取消・取得失敗・背景復帰・保存後の再ロード。
- 実ROMのBEEP/ゲーム音と聴感、長時間再生、消音、他アプリ音声、割込み、出力先変更・ヘッドホン切断。
- 実パッド/Physicalキーボード、接続方式・配列差、複数機器選択、単独SHIFT/同時押し/repeat/短押し/切断と復帰。
- 実ROMでのPC-1245全体受入れと、LCDドットの見え方。iOSの他機種追加は別工程。

CIと同じ検証は `sh iosApp/test-simulator.sh`。使い捨てiPhone Simulatorを作成し、終了時にその端末だけを削除する。
CIはXcode 26.6とiOS 26.5 Runtimeを明示して全UIテストを実行する。
`PGP_IOS_SIMULATOR_RUNTIME`でRuntimeの完全な識別子を指定でき、未導入なら別版へ黙って切り替えず失敗する。
指定なしのローカル実行では、利用可能な最も新しいiOS RuntimeのiPhoneを選ぶ。
実機・古いiOSの保存動作まで検証したことにはならない。
Files pickerのCancelはiOS 18.5でもボタンとして公開されない場合がある。UIテストはOS番号で分岐せず、
押せるボタンを優先し、必要ならナビゲーションバーから戻ってキャンセルする。
結果bundleはbuild配下へ残す。署名付きArchive・TestFlight/App Store配布は別途の指示と検証が必要。

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

プロジェクトの設定値はVersion 0.4.0 / Build 1、Bundle ID `com.digihori.pgp.player.ios`、Team空欄。
これはDeveloper登録や証明書の現状を示すものではない。実機・配布の準備時に環境を再確認し、
Team/配布用Bundle ID/Version/Build番号を別途確定する。
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

## 段階別の過去記録

2026-10-12の確認は以下に保持する。当時の自動実行導線や件数を現在の仕様・最新検証と混同しない。

### 2026-10-12の確認結果

- Xcode 26.6 / iOS Simulator 26.5。SE3・iPhone 12で表示/入力/回転を確認。
- Swift host 9件、UI 2件、共有Native Player 23件を通過。
- JDK 21による全体Gradle buildとCore checks成功。Core 408、Studio 195（1件skip）、
  shared desktop Player 20、Android 9。署名なしgeneric iPhone arm64ビルドも成功。
- iPhone 12で実ROMのpicker登録、CPU自動実行、タッチ計算 `7+3=10`、
  プロセス再起動後のROM復元・自動実行を確認。ユーザーもXcodeから起動・動作確認済み。
- 両横向き、portrait制限、Run/Pause/Reset、背景復帰PAUSED、通常/Controller Displayを確認。
- 最大Dynamic Typeでも表示を確認後、Simulator文字サイズを元のlargeへ戻した。
- 実機・iOS 16 runtime・署名付きArchive・TestFlight/App Store配布は未検証。
