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
場合だけGitHub prereleaseを公開する。Playerのdebug APKはストア配布用の正式署名版ではない。

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
