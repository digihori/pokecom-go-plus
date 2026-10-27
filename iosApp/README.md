# iOS Player host — PC-1245 / PC-125x / PC-126x / PC-1350 / PC-140x / PC-1450 / PC-1360 / PC-1460 / PC-1470U / PC-1360K

## 利用者向けの入口と記録の読み方

現在の導入・操作・制限は[同梱iPhoneヘルプ](../docs/guides/PLAYER_HELP_IOS.md)を参照してください。
本書は開発者向けのビルド手順と、日付別の実装・受入れ記録です。2026-10-12／17時点の
PC-1245単体の説明を、現在の対応機種全体の制限として読み替えないでください。
全体受入れは[2026-10-21の記録](../docs/development/IOS_MACHINE_SUPPORT_STEP8.md)へまとめています。

iPhone専用・最低iOS 16。SwiftUI/UIKit hostからPgpPlayer static frameworkを使用する。
共有Session接続、実行制御、スキン/LCD/タッチ入力、表示切替、ROM管理に加え、
ホーム・階層メニュー・BASIC/DMPロード/セーブと静的同梱の情報画面を接続した。
通常起動はホーム。登録済みROMは停止状態で復元し、初回プレイ入場でCPUを自動実行する（ROM管理を開いた後は停止意図を保つ）。
同Sessionのホーム往復は実行意図を保ち、通常背景復帰は停止する。
合成表示はDEBUGのSimulator probe専用。共有PCMの音声出力・CPU速度5段階・設定保存を接続。GameController・Physical入力と全体コントローラ設定を接続。実周辺機器の受入れは未実施。

最新の外部入力・検証区分は[手順4](../docs/development/IOS_PLAYER_STEP4.md)。音声・速度は [手順3](../docs/development/IOS_PLAYER_STEP3.md)、ホーム・入出力は [手順2](../docs/development/IOS_PLAYER_STEP2.md) を参照。
以下に残る2026-10-12の暫定導線・件数・配布情報は当時の段階別記録。

## ホーム表示・画面方向の修正（2026-10-21）

ホームは機種名順で、初期表示はタイル。タイル／リストを切り替えられ、形式は保存する。
機種画面だけ横固定とし、ホーム・設定・ROM管理・入出力・情報画面は縦／横に対応する。
ホームの3点メニューはSwiftUIの標準Menuで表示し、ROM管理から戻った際のアイコン消失を修正・回帰確認済み。
[実装・検証記録](../docs/development/IOS_HOME_PRESENTATION_FIX.md)を参照。
ユーザーから全対応機種の動作・音声再生と、修正後のホーム画面を一通り確認した旨を受領し、今回の対応を完了とした。音声の途切れ感の調整は後続とする。

## 全機種の受入れ状況（2026-10-21）

対応14モデル・ROM保存10シリーズ。対応表、全体回帰の結果、実ROM・実機の未確認項目は
[機種対応 手順8](../docs/development/IOS_MACHINE_SUPPORT_STEP8.md)を最新記録とする。
全機種のiOS接続を実装済みだが、今回の自動検証は独自の合成ROMを用いる。
ユーザーの動作確認とは別に、自動検証の範囲を記録する。端末・OS・ROM版・周辺機器別の
詳細と、最低iOS 16・署名付き配布の受入れは別途管理する。2026-10-12のPC-1245実ROM起動・計算の過去記録は末尾に保持する。

## バンク機種の追加（2026-10-21）

[手順6](../docs/development/IOS_MACHINE_SUPPORT_STEP6.md)でPC-1360・PC-1460・PC-1470Uを追加。
Studioが作成する内部8 KiB＋16 KiB×8／4／8バンクのpgromを検証・保存する。
25桁×4行／24桁×1行／24桁×2行LCD、S2 BASIC/DMP、固有キーとPhysical入力を接続する。
右上RUN/PROと動作モードメニューは、引き続きPC-1245／1251系／1261系だけに表示する。
[手順7](../docs/development/IOS_MACHINE_SUPPORT_STEP7.md)でPC-1360Kと漢字ROMの検証・固有キー・平／片／反転S表示を追加。ユーザーは対応機種一通りを確認済み。漢字変換手順・ROM版ごとの詳細は未記録。

## PC-1401系・PC-1450の追加（2026-10-21）

[手順5](../docs/development/IOS_MACHINE_SUPPORT_STEP5.md)でPC-1401／1402とPC-1450を追加。
16桁×1行LCD、関数キーと日本語入力キー、S1 BASIC/DMP、64 KiB単体ROM／pgromを接続する。
PC-1401／1402はシリーズ内ROMを共用し、モデル別標準RAMでSessionを生成する。
本体CAL／BASICキーを既存Coreへ配送し、LCDシンボルはROMの表示に従う。
右上トグルと動作モードメニューはPC-1245／1251系／1261系だけに表示する。

## PC-1350の追加（2026-10-21）

[手順4](../docs/development/IOS_MACHINE_SUPPORT_STEP4.md)でPC-1350を追加。25桁×4行LCD、62タッチ領域、
S1 BASIC/DMP、64 KiB単体ROM／pgromを共有定義へ接続する。
MODEキーでROM側のモードを操作し、直接のRUN/PROボタンや動作モードメニューは表示しない。
左右SHIFTの領域IDと保持所有権、固有Physicalキー、機種別の割当可否にも対応する。

## LCDモード位置・右上トグルの修正（2026-10-21）

[修正記録](../docs/development/IOS_MODE_INDICATORS_FIX.md)：PC-1245／1251系のLCDモードを左からRUN/PRO/RSVの
固定位置へ統一（PC-1245はRUN/PROのみ）。右上トグルはPC-1245／1251系／1261系に限定し、RUN/PROのみを切り替える。RSVはメニューから選ぶ。
以下の段階別記録にある旧トグルの操作記述より、この修正と現在の同梱ヘルプを優先する。

## PC-1261系の追加（2026-10-21）

[手順3：PC-1260／1261](../docs/development/IOS_MACHINE_SUPPORT_STEP3.md)で2モデルを追加。
24桁×2行LCD、RUN/PRO/RSV、S1 BASICとDMPを既存共有定義へ接続した。
ホームのPC-1261系カードと共通ROM保存先`pc-1261.json`を使い、単体ROMは64 KiBのみ受け付ける。
選択・実行モデルの独立保存、プレイ時の切替確認は手順2と同じ。

## PC-1251系の追加（2026-10-21）

[手順2：PC-1250／1251／1255](../docs/development/IOS_MACHINE_SUPPORT_STEP2.md)で3モデルを追加。
ホームのシリーズカードでモデルを選択し、ROMをシリーズ共用する。選択変更と起動中Sessionは独立し、
「プレイ」の確認後に新しい標準RAM構成へ切り替える。PC-125xはRUN/PRO/RSVに対応する。
以下の2026-10-17以前のPC-1245限定という記述と検証件数は当時の記録。

## 機種追加の共通基盤（2026-10-21）

[手順1：複数機種の共通基盤](../docs/development/IOS_MACHINE_SUPPORT_STEP1.md)として、
iOS向け機種カタログ、機種別ROM保存、機種情報に基づく表示・モード・プログラム解析と
Session切替の入口を追加した。手順1の時点ではPC-1245のみを公開し、既存保存ROMとの互換性を維持した。
PC-1251系の公開とシリーズ共通ROMは上記の手順2で追加した。

## 現在の対応と検証（2026-10-17）

手順1〜4は実装と合成入力・Simulatorで可能な検証まで完了。第5段階の実機受入れは未完了。
PC-1245のRUN/PROのみを扱い、RSV・他機種・iPadは含めない。

| 段階 | 対応・記録 |
| --- | --- |
| 1 | [標準Files picker、コピー読込み、取得取消・失敗時の保護](../docs/development/IOS_PICKER_STEP1.md) |
| 2 | [ホーム・階層メニュー、BASIC/DMP、静的情報本文](../docs/development/IOS_PLAYER_STEP2.md) |
| 3 | [共有PCM、音声出力、速度5段階、設定保存](../docs/development/IOS_PLAYER_STEP3.md) |
| 4 | [GameController、Physical入力、全体コントローラ設定](../docs/development/IOS_PLAYER_STEP4.md) |

最新の確認: 全体build・Core desktop 436件、Android unit 43件、共有Player desktop/Android各60件、
共有Player iOS Native 70件、hostInput desktop/Android/iOS Native各12件、Swift33件、UI6件が成功。
CoreのUP-TO-DATE、Swift/UIの分割再実行や途中の失敗は[手順4の検証表](../docs/development/IOS_PLAYER_STEP4.md#検証)に区別する。
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
  案内・ライセンスはビルド時同梱。利用者向け操作は[同梱ヘルプ](../docs/guides/PLAYER_HELP_IOS.md)を参照。
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
ロード・セーブとROM登録のFiles pickerは開始フォルダを強制せず、iOSの前回の場所の復元に任せる。選択画面はimport／export URLを持つ
単一のpresentationとして管理し、キャンセルしても元のプログラム画面を維持する。
保存picker完了はクラウド同期完了を意味しない。Google Driveでのファイル選択と前回位置の復元は実機でユーザーが確認済み。
その他のproviderや環境別の動作は別途確認する。

## 残る受入れ

今回のユーザー確認は完了した。次は以下の環境別・機能別の詳細確認と、音声の途切れ感の調整を別工程として管理する。実機確認時にDeveloper登録・署名環境を改めて確認する。

- iPhone実機、最低iOS 16、現行OSを分けた起動・両横向き・ノッチ/ホームインジケータ・文字サイズの確認。
- 端末内/iCloud Drive/Google Driveで選択・未取得待ち・取消・取得失敗・背景復帰・保存後の再ロード。
- 実ROMのBEEP/ゲーム音と聴感、長時間再生、消音、他アプリ音声、割込み、出力先変更・ヘッドホン切断。
- 実パッド/Physicalキーボード、接続方式・配列差、複数機器選択、単独SHIFT/同時押し/repeat/短押し/切断と復帰。
- ROM版ごとのBASIC容量・漢字変換の詳細と、実画面でのLCDドットの見え方。PC-1250のMEM表示、PC-1402のROM版依存を継続管理する。

## CIとローカル検証

iOSの通常CIはアプリのビルドとSwift単体テストを実行する。現行の単体テストは、ライブ出力1件を除く49件。
共有ロジックは既存のCoreテストでも検証する。Native専用の3モジュールのテストは
GitHub Actionsの `iOS Native tests (manual)` から手動実行する。
Xcode 26.6・iOS 26.5を固定し、UI操作テストと実機向けRelease Framework生成は通常CIに含めない。
同じ単体テストをローカルで実行するには `sh iosApp/test-simulator.sh --unit` を使う。
CIと`--unit`では、実デバイス出力を使う`testLiveSimulatorAudioNodeConsumesSyntheticPCM`のみを
実行対象から外す。2026-10-21のCIでは、このテストのCoreAudio初期化がRPC timeoutでプロセスを終了させた。
実際のAVAudioEngineによるオフライン描画の全5速度、PCM変換、queue、割込み・再開のテストは継続する。
ライブ出力テストは`sh iosApp/test-simulator.sh`のローカル全体検証で実行する。
修正後の`--unit`を使い捨てSimulatorで実行し、手順1の変更を含むSwift単体35件が成功した。
全5速度のオフライン音声テストの成功と、ライブ出力テストが実行されないことも確認した。
結果は`iosApp/build/CI-TestResults-20261021-075906.xcresult`（Git対象外）。GitHub CIでの再実行は未確認。
専用scheme `PokecomGOPlayerUnitTests`はUIテストのターゲットを含めず、Filesの事前起動も行わない。
全UIテストを含むローカル検証は `sh iosApp/test-simulator.sh`。
ローカルでは両モードとも使い捨てiPhone Simulatorを作成し、終了時にその端末だけを削除する。
GitHub Actionsの単体テストモードだけは、ユーザーのROMが存在しないCI環境の既存端末を使い、
作成・削除を行わない。Nativeツールチェーンは `~/.konan` をXcode・Kotlin版に応じたキーでキャッシュする。
初期段階の単体35件・UI6件は当時の記録。現行の単体49件、全機種のUI回帰、ホーム修正のiPhone 17 Pro／SEでの確認は上記の手順8・ホーム修正記録を参照する。FilesのUIはローカルSimulatorで確認し、通常CIの対象には含めない。
`PGP_IOS_SIMULATOR_RUNTIME`でRuntimeの完全な識別子を指定でき、未導入なら別版へ黙って切り替えず失敗する。
指定なしのローカル実行では、利用可能な最も新しいiOS RuntimeのiPhoneを選ぶ。
実機・古いiOSの保存動作まで検証したことにはならない。
Files pickerのCancelはiOS 18.5でもボタンとして公開されない場合がある。UIテストはOS番号で分岐せず、
押せるボタンを優先し、必要ならナビゲーションバーから戻ってキャンセルする。
初回起動完了を待ってテストする。全UIモードではFilesを一度起動し、失敗時にFiles関連のシステムログもbuild配下へ残す。
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

`<device-uuid>` は一覧に出たUUIDへ置き換える。ROM選択画面で「ブラウズ」→「このiPhone内」→Playerのフォルダを選ぶ。
コピーだけでは登録にならないので、アプリの「ROMを選択」から選ぶ。
通常はFilesアプリの「このiPhone内 → Pokecom GO Player」からも見える。
Simulatorへのドラッグ&ドロップはファイル種類/OSによって扱いが異なるため、
任意のROMが保存されることを前提にせず、上記Documentsコピーを確実な手順とする。
アプリをアンインストール/仮想端末を消去すると登録ROMも失われる。元ROMは別途保管する。

## 将来の配布

2026-10-25のiOS初回正式配布準備では、公開済みAndroid版`0.7.0-alpha.1`の数字部分に合わせ、
Version `0.7.0`とする。アップロード済みBuildは`2`で、今回の操作改善を再提出するときは
未使用のBuild番号（前回が`2`なら`3`）へ増やして新しいArchiveを作成する。App StoreのVersionは数字とピリオドのみのため、`-alpha.1`は含めない。
Bundle IDは`com.digihori.pgp.player.ios`。Teamは利用者の署名環境で指定する。
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

## iOS公開用プライバシーポリシー（2026-10-25）

[日本語原稿](../docs/guides/PLAYER_PRIVACY_POLICY_IOS.md)と[英語原稿](../docs/guides/PLAYER_PRIVACY_POLICY_IOS.en.md)、
公開用HTML `docs/player/privacy-ios-ja.html`／`privacy-ios-en.html`を用意した。
Android版とは別のポリシーとし、アプリ内の日本語同梱本文も公開用の原稿へ更新する。
アプリ内の本文は静的同梱のため、アップロード前に新しくArchiveを作成する。

GitHub Pagesの公開先は[公開ページのREADME](../docs/player/README.md#ios-privacy-policy)を参照。
2026-10-25にmainへ反映し、Pagesの公開成功と日英HTMLの一致を確認済み。

## iOS操作改善（2026-10-25）

ファイル選択の前回位置、キー周囲のタッチ判定・下端と左右端の入力余白、
PC-1360Kの変換／PC-1470Uの＝、ホーム位置の復元をユーザーが実機で確認済み。
キー判定の単体テストと、リスト／タイルで各2回の機種画面往復のUIテストも成功した。
Releaseビルドでは`DWARF with dSYM File`を指定し、配布Archiveのシンボルを生成する。

### ホームのスクロール位置

機種画面から戻る際は、リスト／タイル別に前回上端に見えていた機種を再表示する。
横固定の機種画面から縦画面へ戻る回転後も、同じ機種の位置を復元する。
位置は起動中の一時状態で、アプリ再起動後の復元やピクセル単位のオフセット保存は行わない。
