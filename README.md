# Pokecom GO Plus

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

> **Technical Preview（開発中）** — 現在は公開版に向けて仕様と実装を安定化している段階です。

Pokecom GO Plus（PGP）は、レトロなポケットコンピュータ向けプログラムを
現代のPC上で作成・実行・解析し、実機への転送まで支援することを目指す
マルチプラットフォーム開発環境です。

PGPは次の2製品と共有Emulator Coreで構成します。

- **Pokecom GO Studio** — macOS／Windows／Linux向けのポケコン開発環境
- **Pokecom GO Player** — Android／iOS向けの実行専用エミュレータ
- **PGP Emulator Core** — StudioとPlayerが共有するKotlin Multiplatform Core

「Pokecom GO Plus」と「PGP」は、このプロジェクト全体を表す開発コードネームとして使用します。

## 現在の状態

プロジェクトは初期実装段階です。Pokecom GO Studioの最初の対象機種としてPC-1245と
PC-1250／1251／1255ファミリーを実装しています。macOSでは実ROMの起動、物理／画面キーボード入力、BASICと
マシン語の入出力、LCD表示、サウンド出力まで動作確認済みです。
WindowsとLinuxはCIでビルドと自動テストを行い、実機操作は今後確認します。
Pokecom GO PlayerはCoreとStudioが安定した後に開発します。

現在利用できる主な機能は次のとおりです。

- 実ROMおよび`.pgrom`パッケージからのエミュレーター起動
- RUN／PRO／RSVモード、物理キーボード、機種別画面キーボード
- BASICテキストのROM経由入力とTokenizerによる直接読み書き
- `.dmp`形式によるマシン語データの読み書き
- LCD表示、CPU状態表示、サウンド出力
- 実機RAM容量と拡張RAMモードの切り替え

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

Core成熟後に、実行機能へ特化したPokecom GO PlayerをAndroidおよびiOSへ展開します。
Playerにはデバッガ、アセンブラ、逆アセンブラなどの開発機能を搭載しません。

## リポジトリ構成

```text
pokecom-go-plus/
├── core/          Kotlin Multiplatformの共有Core
├── desktopApp/    Pokecom GO Studio（現在のCompose Desktop実装）
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
- Gradle Wrapper（同梱）
- Kotlin 2.4.20
- Compose Multiplatform 1.12.1

## ビルドとテスト

### 起動前の準備

PGPはROMイメージを同梱しない。利用者自身が正当に入手した、対象機種のROMイメージを用意する必要がある。
ROMは起動後に`Select ROM`から選択できるほか、上記の既定位置へ置けば初回起動時に自動検出される。

ソースからのビルドにはJDK 21を使用する。Gradle本体を別途インストールする必要はない。

### macOSでの起動

Finderで`run-pgp.command`をダブルクリックするか、ターミナルで次を実行する。
Android Studio付属のJDK 21とローカルGradleキャッシュはスクリプトが自動設定する。
前回選択したROMがある場合は、その機種とファイルを自動的に復元する。履歴がない場合は、既定位置の
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

両環境ともCIによるビルドと自動テストは実施しているが、画面操作、音声、ファイル選択を含む
手動スモークテストは公開前の確認項目である。

### BASICテキストの読み込み

ROM起動後にUTF-8の`.bas`ファイルを指定して読み込める。通常はTokenizerで直接中間コードへ変換するため、
実機の1行入力長を超える行やエスケープ表記した特殊文字も扱える。ROM自身の編集処理を通して入力する方式も、
手軽な互換入力として利用できる。

文字コード、予約語、特殊文字の割り当てには機種差がある。対応する表記と現在の制約は
`docs/BASIC_TEXT_FORMAT.md`を参照する。

### ROMセットの作成

`Create ROM Set`を選び、対象機種と、吸い出した内部ROM・外部ROMを指定する。
現在対応しているPC-1245／1251ファミリーでは8KiBの内部ROMと16KiBの外部ROMを使用する。
保存先を選ぶと、サイズとSHA-256を記録した`.pgrom` v1パッケージを生成する。
生成したファイルは`Select ROM`から直接読み込める。元のROMと生成物はリポジトリへ追加せず、
`local-data/`などGit管理外の場所に保存する。

既存のPokecom GO用32/64KiBイメージは、互換入力として`Select ROM`から直接起動できる。

### ビルドと自動テスト

```bash
./gradlew build
```

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
- macOS以外はCIでのビルド・テストのみで、手動操作確認が完了していない。
- ROM Importerの操作ガイドと、バンクROMを持つ機種の一括選択UIは暫定実装である。
- ROM経由のBASIC入力には実機同様の1行入力長制限がある。長い行にはTokenizer方式を使用する。
- サウンド出力は初期実装で、プログラムによっては音の途切れや波形差が発生する可能性がある。
- PC-1245／1250の`0xb000..0xbfff`周辺のミラー仕様は再確認が必要である。
- Pokecom GO Player（Android／iOS）は未実装である。

公開版に向けた確認状況は[Technical Preview公開チェックリスト](docs/PUBLIC_RELEASE_CHECKLIST.md)を参照する。

## フィードバック

Technical Preview公開後の不具合報告と機能要望はGitHub Issuesで受け付ける予定である。
IssueにはROMイメージ、秘密情報、再配布できないデータを添付しないこと。

## ドキュメント

- [プロジェクト構想](docs/PGP_CONCEPT.md)
- [アーキテクチャ](docs/ARCHITECTURE.md)
- [Emulator Core公開API](docs/CORE_API.md)
- [SC61860機種世代](docs/MACHINE_FAMILIES.md)
- [PC-1245機種定義](docs/machines/PC-1245.md)
- [PGP ROMパッケージ形式](docs/ROM_PACKAGE.md)
- [Golden Test Data形式](docs/GOLDEN_TEST_DATA.md)
- [ロードマップ](docs/ROADMAP.md)
- [Technical Preview公開チェックリスト](docs/PUBLIC_RELEASE_CHECKLIST.md)
- [移植記録](docs/PORTING_NOTES.md)

## ライセンス

本プロジェクトは[MIT License](LICENSE)の下で公開します。

Pokecom GO等から移植するコード・仕様・画像については、由来と権利を確認し、
[PORTING_NOTES.md](docs/PORTING_NOTES.md)へ記録します。
