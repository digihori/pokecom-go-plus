# Pokecom GO Plus

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

Pokecom GO Plus（PGP）は、レトロなポケットコンピュータ向けプログラムを
現代のPC上で作成・実行・解析し、実機への転送まで支援することを目指す
マルチプラットフォーム開発環境です。

> [!NOTE]
> 「Pokecom GO Plus」と「PGP」は開発コードネームです。正式名称は未定です。

## 現在の状態

プロジェクトは初期実装段階です。最初の対象機種であるPC-1245について、
エミュレーターCoreとCompose Desktopアプリを実装しています。macOSでは実ROMの起動、
物理キーボード入力、BASICプログラム実行、LCD表示まで動作確認済みです。
WindowsとLinuxはCIでビルドと自動テストを行い、実機操作は今後確認します。

## プロジェクトの位置付け

- PGPは既存Androidアプリ「Pokecom GO」の新バージョンではありません。
- Pokecom GOはAndroidアプリとして独立して維持されます。
- PGPはPokecom GOからCPU、機種定義、BASIC変換等の技術を分析・再設計して利用します。
- SHARP Brain / Brainuxは現在のPGP対象プラットフォームではありません。
- ROMイメージは本リポジトリおよび配布物に含めません。

## 対象プラットフォーム

初期開発はDesktop Firstです。

- macOS
- Windows
- Linux

Core成熟後にAndroidおよびiOSへの展開を検討します。

## リポジトリ構成

```text
pokecom-go-plus/
├── core/          Kotlin Multiplatformの共有Core
├── desktopApp/    Compose Desktopアプリケーション
├── docs/          構想・設計・移植記録・ロードマップ
├── local-data/    ローカルROM等（Git管理外）
└── test-data/     再配布可能なテストデータ
```

開発用の実ROMは`local-data/roms/<machine-id>/`へ配置する。現在のPC-1245用ファイルは
`local-data/roms/pc-1245/pc1245mem.bin`を使用する。`local-data/`全体はGit管理外であり、
ROMイメージをコミットしない。

## 開発環境

- JDK 21
- Gradle Wrapper（同梱）
- Kotlin 2.4.20
- Compose Multiplatform 1.12.1

## ビルドとテスト

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

## ドキュメント

- [プロジェクト構想](docs/PGP_CONCEPT.md)
- [アーキテクチャ](docs/ARCHITECTURE.md)
- [Emulator Core公開API](docs/CORE_API.md)
- [SC61860機種世代](docs/MACHINE_FAMILIES.md)
- [PC-1245機種定義](docs/machines/PC-1245.md)
- [PGP ROMパッケージ形式](docs/ROM_PACKAGE.md)
- [Golden Test Data形式](docs/GOLDEN_TEST_DATA.md)
- [ロードマップ](docs/ROADMAP.md)
- [移植記録](docs/PORTING_NOTES.md)

## 参照プロジェクト

開発時には、兄弟ディレクトリにある以下のリポジトリを読み取り専用の参照実装として
使用します。PGPのビルドはこれらに依存しません。

- `../pokecom` — Pokecom GO
- `../pcwav` — ポケコン向けWAV変換実装

## ライセンス

本プロジェクトは[MIT License](LICENSE)の下で公開します。

Pokecom GO等から移植するコード・仕様・画像については、由来と権利を確認し、
[PORTING_NOTES.md](docs/PORTING_NOTES.md)へ記録します。
