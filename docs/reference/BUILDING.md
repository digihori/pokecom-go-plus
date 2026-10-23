# ソースからのビルド

この文書は、公開リポジトリを取得してPokecom GO Studioをビルド、実行、テストする手順をまとめる。
ROMイメージはビルドや自動テストには不要である。エミュレーターとして使用する場合だけ、利用者自身が
正当に取得した対応機種のROMを用意する。ROMをリポジトリへ追加したり、Issueへ添付したりしないこと。

## 1. 必要な環境

Desktop版の基本的なビルドには次が必要である。

- Git
- 64-bit OS（macOS、Windows、Linux）
- JDK 21
- インターネット接続（初回のGradleおよび依存ライブラリ取得に使用）

Gradle Wrapperを同梱しているため、Gradleを別途インストールする必要はない。
Android Playerもビルドする場合はAndroid SDK 36が必要である。

DMG、MSI、DEBを作る場合は、`jpackage`を含む完全なJDK 21を使用する。Android Studio付属JBRは
通常のコンパイルには使用できるが、配布パッケージ作成に必要なツールを含まない場合がある。

### JDKの確認

```text
java -version
jpackage --version
```

`java -version`が21以外を示す場合は、JDK 21を`JAVA_HOME`へ設定する。

macOSの例：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Windows PowerShellの例：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Linuxの例：

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
```

インストール先はJDKディストリビューションやOSによって異なる。上記は一般例であり、個人の環境に
合わせて変更する。

## 2. リポジトリを取得する

```bash
git clone https://github.com/digihori/pokecom-go-plus.git
cd pokecom-go-plus
```

Windows PowerShellでも同じ`git clone`と`cd`を使用できる。

## 3. Desktop版を実行する

macOS／Linux：

```bash
./gradlew :desktopApp:run
```

Windows PowerShell：

```powershell
.\gradlew.bat :desktopApp:run
```

macOSでは`run-pgp.command`をFinderから開くこともできる。このスクリプトは利用可能なJDK 21を選び、
リポジトリ内の開発用Gradleキャッシュを使ってDesktop版を起動する。

```bash
./run-pgp.command
```

起動後は`File` → `Register ROM…`または`Manage ROMs…`からROMを登録する。

## 4. テストとビルド

### Macで編集したソースをWindowsへコピーして確認する

MacのリポジトリルートをSMB共有し、Windowsの`P:`へ割り当てた場合は、
共有フォルダの`scripts/sync-from-mac.cmd`を使用できる。既定のコピー先は
`C:\work\pokecom-go-plus`。未コミットのソースもコピーするため、途中のpush/pullは不要。
WindowsにはJDK 21が必要で、GitやIDEはこのコピー方式での実行には必須ではない。

PowerShellからの例:

```powershell
# コピー予定だけを確認（書込みなし）
& 'P:\scripts\sync-from-mac.cmd' /preview

# コピーのみ
& 'P:\scripts\sync-from-mac.cmd'

# コピー成功後、WindowsローカルからStudioを起動
& 'P:\scripts\sync-from-mac.cmd' /run
```

Studioの確認用プロセスを閉じてから再実行する。通常の差分コピーで、Windows側の
`.gradle`／`.gradle-local`、各`build`、`local.properties`、`local-data`、ROM package、
署名情報などはコピー対象から除外する。Macのソースは変更しない。
コピー失敗時はStudioを起動せず、Robocopyの終了コード0〜7は正常終了として扱う。
Windows標準PowerShellの補助スクリプトを使用する。実行ポリシー指定は起動するプロセスだけに適用し、
Windows全体やユーザーの永続設定は変更しない。
コピー先のソースはMac側で上書きされるため、Windowsでソースを編集しない。

コピー成功時にWindows側の`.pgp-sync/files.json`へ同期したファイルの一覧を保存する。
次回は前回一覧にあるファイルのうち、Mac側で削除・移動されたものだけをWindows側で削除する。
`/preview`ではコピー予定と削除予定を表示するだけで、ファイルや一覧を変更しない。
初回は既存のWindows側ファイルを削除しないため、旧スクリプト時代の削除済みソースは必要に応じて手動整理する。
一覧を削除すると再び初回扱いになり、そこから先の同期分だけが削除対象になる。
Windows固有ファイル、保護対象、空になったフォルダは残す。共有断・コピー失敗・コピー中のファイル構成変更時は、
削除を行わず一覧も更新しない。壊れた一覧や別のコピー元の一覧では停止し、リンク／junction経由の書込み・削除も拒否する。
コピー先のGit情報は同期しないので、
このフォルダからcommit/pushせず、Git操作はMacの元リポジトリで行う。

共有ドライブやコピー先を変更する場合は、実行前に次の環境変数を指定する。

```powershell
$env:PGP_SYNC_SOURCE = 'P:\'
$env:PGP_SYNC_DESTINATION = 'C:\work\pokecom-go-plus'
```

削除判定の回帰テストは`scripts/test-sync-from-mac.ps1`。一時フォルダとコピー処理の代替関数で、
初回保持、削除・移動、日本語名、preview、保護対象、コピー失敗、並行変更、不正一覧、共有不在を検証する。
実際のSMB／Robocopyの動作確認とは区別する。

### テストコマンド

Desktopと共通Coreの主要テスト：

```bash
./gradlew :core:desktopTest :desktopApp:test
```

Windows PowerShell：

```powershell
.\gradlew.bat :core:desktopTest :desktopApp:test
```

リポジトリ全体のビルド：

```bash
./gradlew build
```

`build`はCore、Desktop、Android、利用可能なKotlin Multiplatformターゲットを対象にする。環境によっては
Android SDK等も必要になる。Desktop版だけを確認したい場合は、前述のDesktop／Coreテストと次の
展開型アプリ生成を使用する。

```bash
./gradlew :desktopApp:createDistributable
```

主な生成物：

```text
desktopApp/build/compose/binaries/main/app/
```

## 5. OS別の配布パッケージ

配布パッケージは対象OS上で作成する。異なるOS向けのパッケージをクロスビルドしない。

### macOS

必要な追加ツールは通常macOSに含まれている。

```bash
./gradlew :core:desktopTest :desktopApp:test \
  :desktopApp:createDistributable :desktopApp:packageDmg
```

生成先：

```text
desktopApp/build/compose/binaries/main/app/
desktopApp/build/compose/binaries/main/dmg/
```

### Windows

MSIの作成にはJDK 21の`jpackage`に加え、WiX Toolset 3系が必要である。WiXの実行ファイルを`PATH`から
利用できる状態にする。

```powershell
.\gradlew.bat :core:desktopTest :desktopApp:test `
  :desktopApp:createDistributable :desktopApp:packageMsi
```

生成先：

```text
desktopApp/build/compose/binaries/main/app/
desktopApp/build/compose/binaries/main/msi/
```

### Linux

DEB作成には`fakeroot`等のDebianパッケージツールが必要である。Debian／Ubuntu系の例：

```bash
sudo apt-get update
sudo apt-get install --yes fakeroot
./gradlew :core:desktopTest :desktopApp:test \
  :desktopApp:createDistributable :desktopApp:packageDeb
```

生成先：

```text
desktopApp/build/compose/binaries/main/app/
desktopApp/build/compose/binaries/main/deb/
```

生成されるDesktop配布物は未署名であり、macOS公証も行っていない。OSのセキュリティ警告が表示される
可能性がある。

## 6. Android Player

現在のPlayer対応機種はPC-1245。ROM登録・bas/dmp入出力・保存／共有・実行操作は
[Android Playerガイド](../guides/PLAYER_PROGRAM_IO.md)を参照。iOS hostと1401／1350のAndroidスキンは後続。

Android SDK 36を設定した環境でdebug APKを生成する。

macOS／Linux：

```bash
./gradlew :androidApp:assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

生成先：

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## 7. GitHub Actions

pushおよびPull Requestでは`.github/workflows/ci.yml`がDesktopの3 OSとAndroid Playerの自動テストを
実行する。`.github/workflows/release.yml`を手動実行すると、macOS、Windows、LinuxのStudioパッケージと
Android Playerのdebug APKを生成し、workflow artifactとして保存する。検証済みのversion tagをpushした
場合だけStudio配布物をGitHub prereleaseへ公開する。Androidのdebug APKはCI成果物としてのみ保持し、Releaseには添付しない。
Playerのdebug APKはストア配布用の正式署名版ではない。1.0.0の正式Release設定は候補版準備時に更新する。
macOSの署名・公証は[ローカル手順](MACOS_SIGNING.md)を参照。通常のCIビルドでの署名・公証はまだ設定していない。

## 8. よくある問題

### Gradle JVMとしてJava 25が選ばれる

このプロジェクトはJDK 21を基準とする。Android StudioのGradle JVMまたは`JAVA_HOME`をJDK 21へ変更する。
GradleやAndroid Gradle Plugin自体を、JVM 25を使うためだけに更新する必要はない。

### `jpackage`が見つからない

完全なJDK 21をインストールし、`JAVA_HOME`と`PATH`を確認する。JREや一部のIDE付属runtimeには
`jpackage`が含まれない。

### Android SDKが見つからない

Desktopだけを試す場合は`:core:desktopTest`、`:desktopApp:test`、`:desktopApp:run`を個別に実行する。
リポジトリ全体またはAndroid Playerをビルドする場合はAndroid SDK 36を設定する。

### 初回ビルドに時間がかかる

初回はGradle、Kotlin/Native、Compose等を取得するため、数分と数百MB以上のディスク領域を使う場合がある。
2回目以降はGradleキャッシュが再利用される。

### ROMがなくてテストできない

通常のビルドと自動テストはROMなしで実行できる。実ROMを使う任意の統合テストとStudio上の
エミュレーター実行だけがROMを必要とする。

## 9. iOS Player host（構造確認）

iPhone専用・最低iOS 16のSwiftUI/UIKit hostを `iosApp` に追加した。
macOS Apple Silicon + Xcode + JDK 21で `iosApp/PokecomGOPlayer.xcodeproj` を開き、
共有scheme `PokecomGOPlayer` とiPhone Simulatorを選択する。Developer登録・Team設定は不要。
Xcode build phaseからPlayer共有frameworkをビルドする。Gradleの全体buildだけではiOS appの
Swiftコンパイル/UIテストは実行しないため、Xcodeで別に検証する。
コマンド・操作・所有境界は [iosApp README](../../iosApp/README.md) を参照。
通常起動はROM未登録画面。Files pickerからPC-1245 `.pgrom` /32・64 KiB legacy ROMを登録できる。
保存ROMは起動時に復元してCPUを自動実行。スキン/LCD/52キー/Controller Displayを接続済み。
合成表示はDEBUGの `--structural-probe` テスト引数でのみ使用する。
機種/ROM・情報メニューは暫定、階層メニューの完成とbas/dmp入出力・ゲームパッドは後続。

## MSI更新とiOS CIの検証

MSIの固定UpgradeCode、numeric packageVersion運用、Windows更新手順と音声／BRKの受入れ項目は
[機種追加前の修正と受入れ確認](../development/PLATFORM_FIXES_2026-10.md)を参照。
iOS CIは共有Native・framework・署名なしiPhone Simulatorを対象とし、実機署名・配布は別途。
