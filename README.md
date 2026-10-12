# Pokecom GO Plus

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

> **開発版（Technical Preview 0.3以降の変更を含む）** — PC-1245 Android Playerは2026-10-12に今回の対応・動作確認を完了しました。次の公開版は未確定です。

Pokecom GO Plus（PGP）は、レトロなポケットコンピュータ向けプログラムを
現代のPC上で作成・実行・解析し、実機への転送まで支援することを目指す
マルチプラットフォーム開発環境です。

将来はStudioのProject、Assembler、Debuggerをlocalhost MCP経由でAIから利用できるようにし、
BASIC作成、既存マシン語の解析、実行中デバッグを安全な承認付き操作で支援することを目指します。

PGPは次の2製品と共有Emulator Coreで構成します。

- **Pokecom GO Studio** — macOS／Windows／Linux向けのポケコン開発環境
- **Pokecom GO Player** — Android／iOS向けの実行専用エミュレータ
- **PGP Emulator Core** — StudioとPlayerが共有するKotlin Multiplatform Core

「Pokecom GO Plus」と「PGP」は、このプロジェクト全体を表す開発コードネームとして使用します。

## 現在の状態

プロジェクトは初期実装段階です。Pokecom GO StudioではPC-1245、PC-1250／1251／1255ファミリー、
PC-1350を実装し、PC-1360は実ROM起動、16KiB×8 ROMバンク、LCD、キー入力、S2 BASICに対応しています。
macOSでは実ROMの起動、物理／画面キーボード入力、BASICと
マシン語の入出力、LCD表示、サウンド出力まで動作確認済みです。WindowsではROM起動、キー入力、BASICと
サウンド、Linux x86_64ではROM起動、基本操作、サウンド、tar.gz／DEB配布物を手動確認しています。
Pokecom GO PlayerのPC-1245 Android版は、LCD／スキン入力、bas／dmp入出力、ROM管理、横向き・ノッチ対応、ゲームパッド割当に対応し、2026-10-12にユーザー動作確認済みです。iOSとAndroidの1401／1350は後続です。

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
[SC61860アセンブラガイド](docs/SC61860_ASSEMBLY.md)を参照してください。

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

実行機能へ特化したPokecom GO PlayerはPC-1245 Android版の今回の開発範囲を完了しています。
iOSは後続対象です。Playerにはデバッガ、アセンブラ、逆アセンブラなどの開発機能を搭載しません。

## リポジトリ構成

```text
pokecom-go-plus/
├── core/          Kotlin Multiplatformの共有Core
├── hostInput/     ゲームパッド割当・入力源別の押下管理
├── controllerUi/  Studio／Android共通のコントローラー設定画面
├── playerShared/  Android／iOS Playerで共有する実行・表示モデル
├── desktopApp/    Pokecom GO Studio（現在のCompose Desktop実装）
├── androidApp/    Pokecom GO Player（PC-1245 Android）
├── docs/          構想・設計・移植記録・ロードマップ
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
[ソースからのビルド](docs/BUILDING.md)を正本とする。以下は日常的に使う起動・テスト手順の要約である。

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

### PC-1245 Android Player

Android SDK 36とJDK 21を設定した環境でdebug APKを生成する。

```bash
./gradlew :androidApp:assembleDebug
```

生成先は`androidApp/build/outputs/apk/debug/androidApp-debug.apk`。
`.pgrom`／PC-1245 legacy ROMを取り込み、機種別のアプリ専用領域へ保存する。
取込み・復元・Reset後はCPUを自動実行する。ROMは同梱しない。

- スキン上の52キー、16桁LCDとSHIFT／DEF等、RUN／PRO、常時実行状態表示
- 両方向の横向き、ノッチ・ナビゲーション領域回避、手動Controller Display
- 画面内の階層メニュー、ROM取込み・置換・削除、ゲームパッド割当
- `.bas`／`.dmp`の拡張子自動判定ロード、BASIC全体／指定RAM範囲の保存・共有
- dmpロード成功後はCPUを自動再開。BASICロード、失敗・キャンセル後はPAUSED
- バージョン情報、プライバシーポリシー／Studio案内の仮表示

現在の選択対象は1245のみ。Project、Assembler、Debugger、WAV、音声出力、セーブステートは搭載しない。
操作と制限は[Android Playerガイド](docs/PLAYER_PROGRAM_IO.md)を参照。
今回の動作はユーザー確認済みだが、次の公開versionと配布物検証は別途決定する。

### BASICテキストの読み込み

ROM起動後にUTF-8の`.bas`ファイルを指定して読み込める。通常はTokenizerで直接中間コードへ変換するため、
実機の1行入力長を超える行やエスケープ表記した特殊文字も扱える。ROM自身の編集処理を通して入力する方式も、
手軽な互換入力として利用できる。

文字コード、予約語、特殊文字の割り当てには機種差がある。対応する表記と現在の制約は
`docs/BASIC_TEXT_FORMAT.md`を参照する。

### WAV変換と解析（Studio専用）

`Tools` → `WAV Tool`でOLD BASIC（02/12）とBinary（62）を変換できる。
Encodeは既存の`.bas`/単一連続領域の`.dmp`、またはProjectツリーの選択項目から`Create WAV…`で指定する。
`Convert`後に結果を確認し、`Save WAV…`で保存する。録音WAVは`Open WAV…`→`Analyze / reanalyze`で解析し、
検証済みのBASIC/`.dmp`、raw byte列、解析reportを個別に保存できる。
payload検証に失敗しても取得できたPCM/rawを確認でき、波形の目盛・zoom・水平移動・差分threshold表示を備える。

S1/S2は未実装のためpcwavを使用する。Playerには搭載せず、ROMのCSAVE/CLOADやDecodeからの
エミュレータ直接ロードとも連動しない。操作・対応範囲は[WAV操作方針](docs/WAV_OPERATIONS.md)参照。

### ROMセットの作成

`File` → `Create ROM Set…`で専用ウィンドウを開き、対象機種、内部ROM、外部ROM、
保存先のフォルダとファイル名をそれぞれ指定して`Create`を押す。
内部ROMは全対応機種で8 KiB。外部ROMはPC-1245／1250／1251／1255が16 KiB、
PC-1261／1350／1401が32 KiB、PC-1360が128 KiB（bank 0〜7の順に連結したファイル）。
必要な項目とROMサイズを確認後、manifestとSHA-256を記録した`.pgrom` v1パッケージを生成する。

作成後の`Register in ROM Library`で登録・起動できる。登録は後から`Manage ROMs…`でも行える。
元のROMと生成物はリポジトリへ追加せず、`local-data/`などGit管理外の場所に保存する。
詳しい操作は[導入ガイドのROMセット作成手順](docs/GETTING_STARTED.md#43-romダンプからpgromを作る)を参照。

既存のPokecom GO用32/64KiBイメージは、ROM Libraryから互換入力として直接登録できる。

### ビルドと自動テスト

```bash
./gradlew build
```

初回ビルドではGradle、Kotlin/Native、Compose等をダウンロードするため、回線環境によっては
数分かかり、数百MB以上のディスク領域を使用する。2回目以降はローカルキャッシュを再利用する。

### Technical Preview配布物

Technical Previewではソースコード、macOS／Windows／Linux向けStudio配布物、およびAndroid向けPlayer
プレビューAPKをGitHub Releasesへ掲載する予定である。StudioはDeveloper ID等による正式署名やmacOS公証を
行わない。Player APKはAndroidのデバッグ署名を使用し、ストア配布用の正式署名版ではない。
OSのセキュリティ警告が表示される可能性がある。ROMイメージは配布物にも含めない。

配布物にはPGPの`LICENSE`と`THIRD_PARTY_NOTICES.md`をアプリresourceとして含め、
Releaseには各ファイルのSHA-256チェックサムを掲載する。

`.github/workflows/release.yml`をGitHub Actionsから手動実行すると、Studioを3環境、PlayerをAndroid環境で
テストして配布物を生成し、14日間保持されるworkflow artifactとして取得できる。`v0.3.0-alpha.1` tagをpushした場合は、
同じ成果物を使ってprereleaseのGitHub Releaseを自動作成する。公開候補の検証が完了するまではtagを作成しない。

GitHubへpushした場合とPull Requestを更新した場合は、GitHub ActionsがmacOS、Windows、
Linux上でCoreのDesktopテストとDesktopアプリのビルド・テストを実行し、別のAndroid環境で
Playerの共有層テスト、Android固有テスト、APK生成を行う。CIにはROMを渡さず、再配布可能な
合成データだけを使用する。

ローカルでのDMG／MSI／DEB生成方法は[ソースからのビルド](docs/BUILDING.md)を参照する。

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
- サウンド出力は初期実装で、プログラムによっては音の途切れや波形差が発生する可能性がある。
  Windowsでは、サウンドを使用したプログラムの終了後もクリックノイズが鳴り続けることがある。
  オーディオラインの終了判定と無音バッファ処理は今後の改善項目である。
- PC-1245／1250の`0xb000..0xbfff`周辺のミラー仕様は再確認が必要である。
- PlayerはPC-1245 Androidに対応する。iOS、1401／1350のAndroidスキン、音声出力、セーブステートは後続。プライバシーポリシーとStudio案内は仮表示である。

公開版に向けた確認状況は[Technical Preview公開チェックリスト](docs/PUBLIC_RELEASE_CHECKLIST.md)を参照する。

## フィードバック

Technical Previewの不具合報告と機能要望はGitHub Issuesで受け付ける。
IssueにはROMイメージ、秘密情報、再配布できないデータを添付しないこと。

## ドキュメント

- [はじめに](docs/GETTING_STARTED.md)
- [ソースからのビルド](docs/BUILDING.md)
- [プロジェクト構想](docs/PGP_CONCEPT.md)
- [アーキテクチャ](docs/ARCHITECTURE.md)
- [Emulator Core公開API](docs/CORE_API.md)
- [Debugger](docs/DEBUGGER.md)
- [AI連携構想](docs/AI_INTEGRATION.md)
- [SC61860機種世代](docs/MACHINE_FAMILIES.md)
- [PC-1245機種定義](docs/machines/PC-1245.md)
- [PC-1350次期機種定義](docs/machines/PC-1350.md)
- [PC-1360次期機種定義](docs/machines/PC-1360.md)
- [PGP ROMパッケージ形式](docs/ROM_PACKAGE.md)
- [Golden Test Data形式](docs/GOLDEN_TEST_DATA.md)
- [外部エディタ連携とプロジェクト形式](docs/PROJECTS.md)
- [SC61860 Assembler／Disassembler](docs/SC61860_ASSEMBLY.md)
- [WAV操作方針と実装範囲](docs/WAV_OPERATIONS.md)
- [OLD WAV形式](docs/WAV_OLD_FORMAT.md)
- [S1／S2 WAV仕様と作者確認](docs/WAV_S1_S2_FORMAT.md)
- [ロードマップ](docs/ROADMAP.md)
- [Technical Preview公開チェックリスト](docs/PUBLIC_RELEASE_CHECKLIST.md)
- [Technical Preview 0.3 Release Notes](docs/RELEASE_NOTES_0.3.0-alpha.1.md)
- [Technical Preview 0.2 Release Notes](docs/RELEASE_NOTES_0.2.0-alpha.1.md)
- [Technical Preview 0.1 Release Notes](docs/RELEASE_NOTES_0.1.0-alpha.1.md)
- [移植記録](docs/PORTING_NOTES.md)
- [第三者ソフトウェア通知](THIRD_PARTY_NOTICES.md)

## ライセンス

本プロジェクトは[MIT License](LICENSE)の下で公開します。

Pokecom GO等から移植するコード・仕様・画像については、由来と権利を確認し、
[PORTING_NOTES.md](docs/PORTING_NOTES.md)へ記録します。
