# Pokecom GO Plus

Pokecom GO Plus（PGP）は、レトロなポケットコンピュータ向けプログラムを
現代のPC上で作成・実行・解析し、実機への転送まで支援することを目指す
マルチプラットフォーム開発環境です。

> [!NOTE]
> 「Pokecom GO Plus」と「PGP」は開発コードネームです。正式名称は未定です。

## 現在の状態

プロジェクトは初期設計・基盤構築段階です。最初の対象機種をPC-1245とし、
Pokecom GOの実績ある動作を参照しながら、ヘッドレスなエミュレーターCoreを
Kotlin Multiplatformで構築します。

現時点では、エミュレーター本体やROM実行機能はまだ実装されていません。

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
└── test-data/     再配布可能なテストデータ
```

## 開発環境

- JDK 21
- Gradle Wrapper（同梱）
- Kotlin 2.4.20
- Compose Multiplatform 1.12.1

## ビルドとテスト

```bash
./gradlew build
```

Coreの共通テストだけを実行する場合：

```bash
./gradlew :core:desktopTest
```

Desktopアプリを起動する場合：

```bash
./gradlew :desktopApp:run
```

## ドキュメント

- [プロジェクト構想](docs/PGP_CONCEPT.md)
- [アーキテクチャ](docs/ARCHITECTURE.md)
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
