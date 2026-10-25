# Pokecom GO Plus

[English](README.md) | [日本語](README.ja.md)

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

> **[Technical Preview 0.7 (alpha 1)](https://github.com/digihori/pokecom-go-plus/releases/tag/v0.7.0-alpha.1)** — Android Player向けのROM作成、デバッグ・WAVツール・Studio承認付きAI連携を整備した早期評価版です。

Pokecom GO Plus（PGP）は、レトロなポケットコンピュータ向けプログラムを
現代のPC上で作成・実行・解析し、実機への転送まで支援することを目指す
マルチプラットフォーム開発環境です。

開発中のStudioでは、localhost MCP経由で外部AIから機種仕様・開発状態を取得し、
ソース差分・Build・Load・実行操作をStudio承認付きで行えます。接続方法と制限は[AI接続ガイド](docs/guides/AI_CONNECTION_GUIDE.md)を参照してください。

PGPは次の2製品と共有Emulator Coreで構成します。

- **Pokecom GO Studio** — macOS／Windows／Linux向けのポケコン開発環境
- **Pokecom GO Player** — Android／iOS向けの実行専用エミュレータ
- **PGP Emulator Core** — StudioとPlayerが共有するKotlin Multiplatform Core

「Pokecom GO Plus」と「PGP」は、このプロジェクト全体を表す開発コードネームとして使用します。

Android Playerの初回設定は[日本語](https://digihori.github.io/pokecom-go-plus/player/ja.html)／[English](https://digihori.github.io/pokecom-go-plus/player/en.html)の公開用ガイドを用意しています（[閲覧・公開方法](docs/player/README.md)）。

使い方は[ドキュメント総合目次](docs/README.md)、初回起動は[はじめに](docs/guides/GETTING_STARTED.md)を参照してください。

Android Playerは[Google Play](https://play.google.com/store/apps/details?id=com.digihori.pgp.player)で公開しています。ROMは同梱していません。

## 現在のリリース

Android Playerに合わせてStudio **0.7.0-alpha.1**を公開しました。
[リリースノート](docs/releases/notes/RELEASE_NOTES_0.7.0-alpha.1.md)と
[チェックリスト](docs/releases/checklists/RELEASE_CHECKLIST_0.7.0-alpha.1.md)を参照してください。

## 現在の状態

開発版のStudioとAndroid／iPhone版Playerは、次の14モデルに対応しています。
PC-1360Kは漢字ROMを登録すると、かな入力・漢字変換も利用できます。

| 機種・シリーズ | Android Player | iPhone Player |
| --- | --- | --- |
| PC-1245 | 対応 | 対応 |
| PC-1250・PC-1251・PC-1255 | 対応 | 対応 |
| PC-1260・PC-1261 | 対応 | 対応 |
| PC-1350 | 対応 | 対応 |
| PC-1401・PC-1402 | 対応 | 対応 |
| PC-1450 | 対応 | 対応 |
| PC-1360・PC-1360K | 対応 | 対応 |
| PC-1460 | 対応 | 対応 |
| PC-1470U | 対応 | 対応 |

Android Playerは2026-10-20に全体を一区切りとし、iPhone版も2026-10-21に全対応機種の動作・音声再生とホーム画面の修正を確認しました。
機種対応と環境別の詳細検証は区別し、[Androidの受入れ記録](docs/development/ANDROID_SERIES_PROGRESS.md)と
[iPhoneの全体受入れ記録](docs/development/IOS_MACHINE_SUPPORT_STEP8.md)に未確認事項を残しています。
iPhone版はiPhone専用・最低iOS 16で、署名付き配布は別工程です。音声の途切れ感は今後の調整項目です。
現在の操作は[Androidヘルプ](docs/guides/PLAYER_HELP.md)・[iPhoneヘルプ](docs/guides/PLAYER_HELP_IOS.md)を参照してください。

StudioはmacOSで実ROM起動、物理／画面キー、BASIC・マシン語入出力、LCD、音声を確認しています。
WindowsではROM起動・キー・BASIC・音声、Linux x86_64ではROM起動・基本操作・音声・tar.gz／DEBを手動確認しています。
公開済み配布版の対応範囲は[各版のリリースノート](docs/README.md#リリース情報)を参照してください。

現在利用できる主な機能は次のとおりです。

- 実ROMおよび`.pgrom`パッケージからのエミュレーター起動
- RUN／PRO／RSVモード、物理キーボード、機種別画面キーボード
- BASICテキストのROM経由入力とTokenizerによる直接読み書き
- `.dmp`形式によるマシン語データの読み書き
- 外部エディタ向け複数ソースプロジェクトの作成、変更検知、Build & Load
- LCD表示、CPU状態表示、サウンド出力
- 実機RAM容量と拡張RAMモードの切り替え
- File／Project／Program／Emulator／Debugに分けたメニューバー
- 機種別ROM Library、メイン画面のProjectツリー、独立Debuggerウィンドウ

## 画面構成

メインウィンドウはLCD、機種別ソフトウェアキーボード、Run／Pause／Reset／Step、対応機種の
RUN／PRO／RSV操作に集中する。右上の機種プルダウンでは、ROM Libraryへ登録済みの機種だけを切り替えられる。

- `File`: ROMの登録・管理とROM set作成
- `Project`: 新規作成、Open、Assembly Workspace表示、Build & Load
- `Program`: BASIC／マシン語の単発入出力とQuick Assemble
- `Emulator`: 実行制御とRAM profile
- `Debug`: DebuggerとCheckpoint

Assembly WorkspaceのAssembleは結果確認、Buildはファイル生成、Build & Loadは生成後の
エミュレータへのロードを行います。式・EQU・データ定義、機種別配置、出力名と制約は
[SC61860アセンブラガイド](docs/guides/SC61860_ASSEMBLY.md)を参照してください。

プロジェクトツリーはメイン画面左側、CPU、Disassembly、Memory、TraceはDebuggerウィンドウへ
分離している。機種別ソフトウェアキーボードは実機のキー配列、SHIFT表記、予約語表記を反映する。

## プロジェクトの位置付け

- PGPは既存Androidアプリ「Pokecom GO」の新バージョンではありません。
- Pokecom GOはAndroidアプリとして独立して維持されます。
- PGPはPokecom GOからCPU、機種定義、BASIC変換等の技術を分析・再設計して利用します。
- ROMイメージは本リポジトリおよび配布物に含めません。

## 対象プラットフォーム

初期開発はPokecom GO Studioを優先します。

- macOS
- Windows
- Linux

実行機能へ特化したPokecom GO Playerは、Android／iPhoneとも上記14モデルに対応しています。
機種対応の完了と、端末・OS・ROM版別の確認、署名付き配布は区別して管理します。
Playerにはデバッガ、アセンブラ、逆アセンブラなどの開発機能を搭載しません。

## リポジトリ構成

```text
pokecom-go-plus/
├── core/          Kotlin Multiplatformの共有Core
├── hostInput/     ゲームパッド割当・入力源別の押下管理
├── controllerUi/  Studio／Android共通のコントローラー設定画面
├── playerShared/  Android／iOS Playerで共有する実行・表示モデル
├── desktopApp/    Pokecom GO Studio（現在のCompose Desktop実装）
├── androidApp/    Pokecom GO Player（14モデル対応Android）
├── iosApp/        Pokecom GO Player（複数機種対応iPhone host）
├── docs/          利用ガイド・技術資料・開発記録・リリース資料
├── local-data/    ローカルROM等（Git管理外）
└── test-data/     再配布可能なテストデータ
```

開発用の実ROMは`local-data/roms/<machine-id>/`へ配置する。従来形式を使う場合の既定位置は、
PC-1245が`local-data/roms/pc-1245/pc1245mem.bin`、PC-1251ファミリーが
`local-data/roms/pc-1251/pc1251mem.bin`である。`local-data/`全体はGit管理外であり、
ROMイメージをコミットしない。

## 開発環境

- JDK 21
- Android SDK 36（Android Playerをビルドする場合）
- Gradle Wrapper（同梱）
- Kotlin 2.4.20
- Compose Multiplatform 1.12.1

通常のビルドと実行にはJDK 21を使用する。Android Studio付属JBRはJava 21の場合のみ使用でき、Java 25は使用しない。
DMG／MSI／DEBを生成する場合は、`jpackage`を含む完全なJDK 21が必要であり、
現在のAndroid Studio付属JBRだけではパッケージを生成できない。

## ビルドとテスト

公開ソースの取得、JDK設定、OS別ビルド、配布パッケージ生成、生成物の場所、典型的なエラーは
[ソースからのビルド](docs/reference/BUILDING.md)を正本とする。以下は日常的に使う起動・テスト手順の要約である。

### 起動前の準備

PGPはROMイメージを同梱しない。利用者自身が正当に入手した、対象機種のROMイメージを用意する必要がある。
ROMは起動後に`File` → `Register ROM…`または`Manage ROMs…`から登録できる。上記の既定位置へ置けば
初回起動時に自動検出され、検証済み`.pgrom`としてアプリ専用領域へ取り込まれる。

ソースからのビルドにはJDK 21を使用する。Gradle本体を別途インストールする必要はない。

### macOSでの起動

Finderで`run-pgp.command`をダブルクリックするか、ターミナルで次を実行する。
インストール済みJDK 21とローカルGradleキャッシュはスクリプトが自動設定する。JDK 21が見つからない場合は起動を中止する。
前回使用した機種のROMがLibraryにある場合は、アプリ内コピーから自動的に復元する。履歴がない場合は、既定位置の
`local-data/roms/pc-1245/pc1245mem.bin`、続いて
`local-data/roms/pc-1251/pc1251mem.bin`を探索し、最初に見つかったROMをロードして実行する。

```bash
./run-pgp.command
```

初回はmacOSのセキュリティ確認が表示されることがある。その場合はFinderでファイルを右クリックし、
「開く」を選択する。

### Windows／Linuxでの起動

Windowsでは次を実行する。

```powershell
.\gradlew.bat :desktopApp:run
```

Linuxでは次を実行する。

```bash
./gradlew :desktopApp:run
```

Windows／LinuxともCIによるビルドと自動テストは実施しているが、画面操作、音声、ファイル選択を含む
手動スモークテストは公開前の確認項目である。

### Android Player（14モデル対応）

Android SDK 36とJDK 21を設定した環境でdebug APKを生成する。

```bash
./gradlew :androidApp:assembleDebug
```

生成先は`androidApp/build/outputs/apk/debug/androidApp-debug.apk`。
`.pgrom`／選択機種のlegacy ROMを取り込み、機種別のアプリ専用領域へ保存する。
対応機種は上記の14モデルです。シリーズ内モデルの選択とROM共用は[Androidヘルプ](docs/guides/PLAYER_HELP.md)を参照。
起動時はホームへROMを復元し、初回プレイとReset後はCPUを自動実行する。ROMは同梱しない。

- 機種別のスキン・キー・LCD・シンボル、対応機種のモード操作、常時実行状態表示
- 両方向の横向き、ノッチ・ナビゲーション領域回避、手動Controller Display
- 画面内の階層メニュー、ROM取込み・置換・削除、ゲームパッド割当
- `.bas`／`.dmp`の拡張子自動判定ロード、BASIC全体のセーブ（AndroidのDMPはロード専用）
- BASIC／dmpロード成功後はCPUを自動再開。失敗・キャンセル後はPAUSED
- ホームの設定／ヘルプ・情報、Playerとは／Studioについて／バージョン・ライセンス、Androidプライバシーポリシーの同梱表示
- 外部キーボードのPhysical入力、左上状態表示のRun/Pause切替

Project、Assembler、Debugger、WAV、セーブステートは搭載しない。AndroidにはサウンドON／OFFとCPU速度5段階を追加。実機の音質などの未確認事項は[音声・速度対応記録](docs/development/PLAYER_AUDIO_SPEED_PROGRESS.md)を参照。
操作と制限は[Android Playerガイド](docs/guides/PLAYER_PROGRAM_IO.md)を参照。
現行の受入れ状況は[Android全体の記録](docs/development/ANDROID_SERIES_PROGRESS.md)、公開済みの版は[リリース情報](docs/README.md#リリース情報)を参照。

### BASICテキストの読み込み

ROM起動後にUTF-8の`.bas`ファイルを指定して読み込める。通常はTokenizerで直接中間コードへ変換するため、
実機の1行入力長を超える行やエスケープ表記した特殊文字も扱える。ROM自身の編集処理を通して入力する方式も、
手軽な互換入力として利用できる。

文字コード、予約語、特殊文字の割り当てには機種差がある。対応する表記と現在の制約は
`docs/reference/BASIC_TEXT_FORMAT.md`を参照する。

### WAV変換と解析（Studio専用）

[操作ガイド・診断・実機確認表](docs/guides/WAV_TOOL_GUIDE.md)。

`Tools` → `WAV Tool`でOLD BASIC（02/12）、OLD Binary（62）、S1 BASIC（07/17）、S2 BASIC（27/37）、S1/S2 Binary（67）を変換できる。
Encodeは既存の`.bas`／単一連続領域の`.dmp`・Intel HEX（`.hex`／`.ihx`）、またはProjectツリーの選択項目から`Create WAV…`で指定する。
`Convert`後に結果を確認し、`Save WAV…`で保存する。録音WAVは`Open WAV…`→`Analyze / reanalyze`で解析し、
検証済みのBASIC/`.dmp`、raw byte列、解析reportを個別に保存できる。
payload検証に失敗しても取得できたPCM/rawを確認でき、波形の目盛・zoom・水平移動・差分threshold表示を備える。

S1/S2のバイナリーに対応し、検証済みDecode結果は`.dmp`／Intel HEXとして保存できる。
WAVのエンコード形式はBASICパスワードなし／あり・マシン語から明示選択する（S2はBASICのみ）。拡張子は選択形式との適合確認に使用する。

S1/S2 BASICは転送名は印字可能ASCIIを使用する。7文字を超える場合は先頭7文字へ短縮し、レポートに記録する。対象機種の選択UIは設けず、Projectの機種とファイルから形式・BASIC／マシン語を初期選択する。Project情報は初期選択にだけ使用し、Decode結果は`.bas`として保存できる。
S2はCP932日本語・行参照・形式共通語彙に対応。通常はtype 27を使い、保護付き選択は転送type 37を指定する（パスワード文字列の新規設定は行わない）。新codecは合成テスト済みで、PGPでの実機往復は未確認。
Playerには搭載せず、ROMのCSAVE/CLOADやDecodeからの
エミュレータ直接ロードとも連動しない。操作・対応範囲は[WAV操作方針](docs/development/WAV_OPERATIONS.md)参照。

### ROMセットの作成

`File` → `Create ROM Set…`で専用ウィンドウを開き、対象機種、内部ROM、外部ROM、
保存先のフォルダとファイル名をそれぞれ指定して`Create`を押す。
内部ROMは全対応機種で8 KiB。外部ROMはPC-1245／1250／1251／1255が16 KiB、
PC-1261／1350／1401が32 KiB、PC-1360／1360Kが128 KiB（bank 0〜7の順に連結したファイル）。
必要な項目とROMサイズを確認後、manifestとSHA-256を記録した`.pgrom` v1パッケージを生成する。

作成後の`Register in ROM Library`で登録・起動できる。登録は後から`Manage ROMs…`でも行える。
元のROMと生成物はリポジトリへ追加せず、`local-data/`などGit管理外の場所に保存する。
詳しい操作は[導入ガイドのROMセット作成手順](docs/guides/GETTING_STARTED.md#43-romダンプからpgromを作る)を参照。

既存のPokecom GO用32/64KiBイメージは、ROM Libraryから互換入力として直接登録できる。

### ビルドと自動テスト

```bash
./gradlew build
```

初回ビルドではGradle、Kotlin/Native、Compose等をダウンロードするため、回線環境によっては
数分かかり、数百MB以上のディスク領域を使用する。2回目以降はローカルキャッシュを再利用する。

### 正式版1.0.0の準備

Studio／Androidの正式版1.0.0を準備しています。現在公開中の最新版は冒頭の0.7 alpha 1です。
macOSはDeveloper ID署名・公証を導入中、WindowsはOSS無料署名を検討し、難しければ当面未署名で配布します。
AndroidはGoogle Playで更新し、debug署名APKはCI成果物のみにします。iOSのリリース準備は別途進めます。
[準備状況](docs/development/planning/FORMAL_RELEASE_TASKS.md)を参照してください。

### Technical Preview配布物

公開済みのTechnical Preview 0.7ではソースコード、macOS／Windows／Linux向けStudio配布物、およびAndroid向けPlayer
プレビューAPKをGitHub Releasesへ掲載した。この版のStudioはDeveloper ID等による正式署名やmacOS公証を
行わない。Player APKはAndroidのデバッグ署名を使用し、ストア配布用の正式署名版ではない。
OSのセキュリティ警告が表示される可能性がある。ROMイメージは配布物にも含めない。

配布物にはPGPの`LICENSE`と`THIRD_PARTY_NOTICES.md`をアプリresourceとして含め、
Releaseには各ファイルのSHA-256チェックサムを掲載する。

`.github/workflows/release.yml`をGitHub Actionsから手動実行すると、Studioを3環境、PlayerをAndroid環境で
テストして配布物を生成し、14日間保持されるworkflow artifactとして取得できる。`v0.7.0-alpha.1` tagをpushした場合は、
Studio成果物を使ってprereleaseのGitHub Releaseを自動作成する。Android debug APKは今後のReleaseには添付せず、CI成果物としてのみ保持する。公開候補の検証が完了するまではtagを作成しない。

GitHubへpushした場合とPull Requestを更新した場合は、GitHub ActionsがmacOS、Windows、
Linux上でCoreのDesktopテストとDesktopアプリのビルド・テストを実行し、別のAndroid環境で
Playerの共有層テスト、Android固有テスト、APK生成を行う。CIにはROMを渡さず、再配布可能な
合成データだけを使用する。
iOSはアプリビルド・Swift単体テストを通常CIで実行する。Native専用テストは手動フロー、UI操作はローカルで別途検証する。

ローカルでのDMG／MSI／DEB生成方法は[ソースからのビルド](docs/reference/BUILDING.md)を参照する。

Coreの共通テストだけを実行する場合：

```bash
./gradlew :core:desktopTest
```

PC-1245の実ROMを使うDesktopスモークテストは、既定の`local-data`配置を自動検出する。
別の場所に置く場合は`PGP_PC1245_ROM`へ絶対パスまたはリポジトリ相対パスを指定する。

```bash
PGP_PC1245_ROM=/path/to/pc1245mem.bin \
  ./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.RealPc1245RomSmokeTest
```

ROMが見つからない環境ではこのテストだけをスキップし、ROMイメージをCIやGitへ持ち込まない。

PC-1251の実ROMスモークテストも既定の`local-data`配置を自動検出する。別の場所に置く場合は
`PGP_PC1251_ROM`を指定する。

```bash
PGP_PC1251_ROM=/path/to/pc1251mem.bin \
  ./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.RealPc1251RomSmokeTest
```

再配布可能なGoldenケースのJSON再生テスト：

```bash
./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.GoldenScenarioRunnerTest
```

実ROMからPGP側Golden Snapshotを`local-data/golden/`へ明示的に採取する場合：

```bash
PGP_EXPORT_GOLDEN=1 \
PGP_PC1245_ROM=/path/to/pc1245mem.bin \
PGP_GOLDEN_PRODUCER_REVISION=<git-revision> \
  ./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.GoldenSnapshotExporterTest.exportsLocallySuppliedRomOnlyWhenExplicitlyRequested
```

出力はROM SHA-256を含み、生成直後に同じRunnerで再生検証される。実ROMと出力JSONは
`local-data/`配下に留まり、Git管理対象にはならない。

Desktopアプリを起動する場合：

```bash
./gradlew :desktopApp:run
```

## 既知の制限

- ROMイメージは同梱しない。利用には対象機種のROMが必要である。
- Apple Silicon MacのVMware上にあるWindows ARM64で、x64版をエミュレーション実行した環境では、
  SkikoのDirect3D／OpenGL描画で一部ボタンのhover表示が壊れた。互換性を優先してWindows版は
  当面Software描画を既定とする。ネイティブWindows x64で再確認後に既定値を再評価する。
  描画方式の比較には`PGP_RENDER_API=DIRECT3D`または`OPENGL`を指定できる。
- Linux x86_64では展開型tar.gzの起動、音声再生と、DEBのインストール・起動・アンインストールを確認済みである。
  ARM64 Linuxの配布物はまだ用意していない。DEBのメニュー項目は現在「その他」に分類される。
- ROM Importerの操作ガイドと、バンクROMを持つ機種の一括選択UIは暫定実装である。
- ROM経由のBASIC入力には実機同様の1行入力長制限がある。長い行にはTokenizer方式を使用する。
- サウンドには実機との波形差が残る可能性がある。Studio／Androidは現状を正式版の基準とし、iOSの音声改善は別途進める。
- 開発版では[1245／125x／126xのRAM仕様](docs/reference/machines/RAM_1245_1261.md)に沿ってメモリ容量・ミラー処理を整理した。MEM表示の期待値との不一致は保留中。公開済み0.7の動作と区別する。
- PlayerはAndroid／iPhoneとも上記14モデルに対応する。機種・端末・ROM版別の確認範囲は[Android受入れ](docs/development/ANDROID_SERIES_PROGRESS.md)・[iOS受入れ](docs/development/IOS_MACHINE_SUPPORT_STEP8.md)を参照。セーブステートは未実装。情報画面は静的同梱。Androidのプライバシーポリシーは適用済み。iPhone用は[日本語原稿](docs/guides/PLAYER_PRIVACY_POLICY_IOS.md)・[英語原稿](docs/guides/PLAYER_PRIVACY_POLICY_IOS.en.md)を用意し、[公開用HTML](docs/player/README.md#ios-privacy-policy)は2026-10-25にPages公開・本文一致を確認済み。

公開済み各版の説明と過去の確認記録は[ドキュメント目次](docs/README.md#リリース情報)を参照する。

## フィードバック

不具合報告と機能要望はGitHub Issuesで受け付ける。
IssueにはROMイメージ、秘密情報、再配布できないデータを添付しないこと。

## ドキュメント

[ドキュメント総合目次](docs/README.md)から、操作ガイド・技術資料・リリース情報・開発記録へ進めます。

- [Studioを始める・ROMを登録する](docs/guides/GETTING_STARTED.md)
- [Project・Build & Load](docs/guides/PROJECTS.md)／[Assembler](docs/guides/SC61860_ASSEMBLY.md)／[Debugger](docs/guides/DEBUGGER.md)
- [WAV Tool](docs/guides/WAV_TOOL_GUIDE.md)／[AI接続](docs/guides/AI_CONNECTION_GUIDE.md)
- [Android Player](docs/guides/PLAYER_HELP.md)／[iPhone Player](docs/guides/PLAYER_HELP_IOS.md)
- [単独で使える補助ツール](tools/README.md)
- [ソースからのビルド](docs/reference/BUILDING.md)
- [リリース情報](docs/README.md#リリース情報)／[開発記録・初期設計](docs/README.md#開発記録初期設計)

## ライセンス

本プロジェクトは[MIT License](LICENSE)の下で公開します。

Pokecom GO等から移植するコード・仕様・画像については、由来と権利を確認し、
[PORTING_NOTES.md](docs/development/PORTING_NOTES.md)へ記録します。

WAVのBASIC変換は個別機種で制限せず、形式単位の上位仕様を使用する。Decodeは信号とヘッダーから自動判別し、形式・機種指定は不要。
