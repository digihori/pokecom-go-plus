# Pokecom GO Plus

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

> **Technical Preview 0.2（開発中）** — 早期評価版です。仕様と実装は今後も変更される可能性があります。

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
Pokecom GO PlayerはCoreとStudioが安定した後に開発します。

現在利用できる主な機能は次のとおりです。

- 実ROMおよび`.pgrom`パッケージからのエミュレーター起動
- RUN／PRO／RSVモード、物理キーボード、機種別画面キーボード
- BASICテキストのROM経由入力とTokenizerによる直接読み書き
- `.dmp`形式によるマシン語データの読み書き
- 外部エディタ向け複数ソースプロジェクトの作成、変更検知、Build & Load
- LCD表示、CPU状態表示、サウンド出力
- 実機RAM容量と拡張RAMモードの切り替え
- File／Project／Program／Emulator／Debugに分けたメニューバー
- 機種別ROM Library、専用Project Filesウィンドウ、独立Debuggerウィンドウ

## 画面構成

メインウィンドウはLCD、機種別ソフトウェアキーボード、Run／Pause／Reset／Step、対応機種の
RUN／PRO／RSV操作に集中する。右上の機種プルダウンでは、ROM Libraryへ登録済みの機種だけを切り替えられる。

- `File`: ROMの登録・管理とROM set作成
- `Project`: 新規作成、Open、Project Files／Assembly Workspace表示、Build & Load
- `Program`: BASIC／マシン語の単発入出力とQuick Assemble
- `Emulator`: 実行制御とRAM profile
- `Debug`: DebuggerとCheckpoint

プロジェクトツリーは専用のProject Filesウィンドウ、CPU、Disassembly、Memory、TraceはDebuggerウィンドウへ
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

実行機能へ特化したPokecom GO PlayerはAndroid最小プロトタイプの開発を開始しています。
iOSは後続対象です。Playerにはデバッガ、アセンブラ、逆アセンブラなどの開発機能を搭載しません。

## リポジトリ構成

```text
pokecom-go-plus/
├── core/          Kotlin Multiplatformの共有Core
├── playerShared/  Android／iOS Playerで共有する実行・表示モデル
├── desktopApp/    Pokecom GO Studio（現在のCompose Desktop実装）
├── androidApp/    Pokecom GO Player（Android最小プロトタイプ）
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

通常のビルドと実行にはJDK 21、またはAndroid Studio付属JBRを使用できる。
DMG／MSI／DEBを生成する場合は、`jpackage`を含む完全なJDK 21が必要であり、
現在のAndroid Studio付属JBRだけではパッケージを生成できない。

## ビルドとテスト

### 起動前の準備

PGPはROMイメージを同梱しない。利用者自身が正当に入手した、対象機種のROMイメージを用意する必要がある。
ROMは起動後に`File` → `Register ROM…`または`Manage ROMs…`から登録できる。上記の既定位置へ置けば
初回起動時に自動検出され、検証済み`.pgrom`としてアプリ専用領域へ取り込まれる。

ソースからのビルドにはJDK 21を使用する。Gradle本体を別途インストールする必要はない。

### macOSでの起動

Finderで`run-pgp.command`をダブルクリックするか、ターミナルで次を実行する。
Android Studio付属のJDK 21とローカルGradleキャッシュはスクリプトが自動設定する。
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

### Android Playerプロトタイプ

Android SDK 36を設定した環境で、debug APKを生成する。

```bash
./gradlew :androidApp:assembleDebug
```

生成物は`androidApp/build/outputs/apk/debug/androidApp-debug.apk`へ出力される。現在はAndroidアプリの
起動、SAFによる`.pgrom`／PC-1245 legacy ROMの取込み、検証、アプリ専用領域への保存と再起動時の
再検証、単一エミュレータセッションの生成、可変サイズLCD描画、PC-1245ソフトウェアキーボードまで
実装している。Androidの画面フレームに同期したRun／Pause／Reset、自作PC-1245スキンへのLCD重ね表示、
およびスキン画像上の全52キーからの直接入力も利用できる。
`Enlarge display`でLCD周辺だけを切り出したController Displayへ切り替え、`Show full device`で
通常の実機スキン表示へ戻せる。
動作モードは機種定義に従って表示し、PC-1245ではRUN／PROを切り替えられる。RSV対応機種を追加した場合は
同じPlayer UIへRSVも表示される。
実行画面はスクロールせず、端末の利用可能領域へスキンを縦横比維持で固定表示する。Run／Pause／Reset、
表示部拡大、動作モードは右上のオーバーフローメニューから操作する。

### BASICテキストの読み込み

ROM起動後にUTF-8の`.bas`ファイルを指定して読み込める。通常はTokenizerで直接中間コードへ変換するため、
実機の1行入力長を超える行やエスケープ表記した特殊文字も扱える。ROM自身の編集処理を通して入力する方式も、
手軽な互換入力として利用できる。

文字コード、予約語、特殊文字の割り当てには機種差がある。対応する表記と現在の制約は
`docs/BASIC_TEXT_FORMAT.md`を参照する。

### ROMセットの作成

`File` → `Create ROM Set…`を選び、対象機種と、吸い出した内部ROM・外部ROMを指定する。
現在対応しているPC-1245／1251ファミリーでは8KiBの内部ROMと16KiBの外部ROMを使用する。
保存先を選ぶと、サイズとSHA-256を記録した`.pgrom` v1パッケージを生成する。
生成したファイルはROM Libraryの`Register…`から登録できる。元のROMと生成物はリポジトリへ追加せず、
`local-data/`などGit管理外の場所に保存する。

既存のPokecom GO用32/64KiBイメージは、ROM Libraryから互換入力として直接登録できる。

### ビルドと自動テスト

```bash
./gradlew build
```

初回ビルドではGradle、Kotlin/Native、Compose等をダウンロードするため、回線環境によっては
数分かかり、数百MB以上のディスク領域を使用する。2回目以降はローカルキャッシュを再利用する。

### Technical Preview配布物

Technical Previewではソースコードに加え、macOS、Windows、Linux向けの実行配布物を
GitHub Releasesへ掲載する予定である。これらはDeveloper ID等による正式署名やmacOS公証を行わないため、
OSのセキュリティ警告が表示される可能性がある。ROMイメージは配布物にも含めない。

配布物にはPGPの`LICENSE`と`THIRD_PARTY_NOTICES.md`をアプリresourceとして含め、
Releaseには各ファイルのSHA-256チェックサムを掲載する。

`.github/workflows/release.yml`をGitHub Actionsから手動実行すると、3環境でテスト後に配布物を生成し、
14日間保持されるworkflow artifactとして取得できる。`v0.2.0-alpha.1` tagをpushした場合は、
同じ成果物を使ってprereleaseのGitHub Releaseを自動作成する。公開候補の検証が完了するまではtagを作成しない。

GitHubへpushした場合とPull Requestを更新した場合は、GitHub ActionsがmacOS、Windows、
Linux上でCoreのDesktopテストとDesktopアプリのビルド・テストを実行する。CIにはROMを
渡さず、再配布可能な合成データだけを使用する。

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
  Windowsでは発音中のクリックノイズに加え、BASICをBREAKした後もアプリ終了までクリックノイズが
  続く場合がある。Java Soundの出力ラインと無音バッファの停止条件を再設計する必要がある。
- PC-1245／1250の`0xb000..0xbfff`周辺のミラー仕様は再確認が必要である。
- Pokecom GO PlayerはAndroid最小プロトタイプを実装中であり、iOS版と実行画面は未実装である。

公開版に向けた確認状況は[Technical Preview公開チェックリスト](docs/PUBLIC_RELEASE_CHECKLIST.md)を参照する。

## フィードバック

Technical Previewの不具合報告と機能要望はGitHub Issuesで受け付ける。
IssueにはROMイメージ、秘密情報、再配布できないデータを添付しないこと。

## ドキュメント

- [はじめに](docs/GETTING_STARTED.md)
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
- [ロードマップ](docs/ROADMAP.md)
- [Technical Preview公開チェックリスト](docs/PUBLIC_RELEASE_CHECKLIST.md)
- [Technical Preview 0.2 Release Notes](docs/RELEASE_NOTES_0.2.0-alpha.1.md)
- [Technical Preview 0.1 Release Notes](docs/RELEASE_NOTES_0.1.0-alpha.1.md)
- [移植記録](docs/PORTING_NOTES.md)
- [第三者ソフトウェア通知](THIRD_PARTY_NOTICES.md)

## ライセンス

本プロジェクトは[MIT License](LICENSE)の下で公開します。

Pokecom GO等から移植するコード・仕様・画像については、由来と権利を確認し、
[PORTING_NOTES.md](docs/PORTING_NOTES.md)へ記録します。
