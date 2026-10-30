# 開発用スクリプト

PGPのソースコード生成・保守に使うスクリプトです。Studio利用者が実行する必要はありません。
単独利用する補助ツールは[tools/](../tools/README.md)を参照してください。

## ci-ios-changes.py

GitHub CIで、iOSの入力が変わった場合だけiOSジョブを実行するための判定です。
2026-10-31に追加しました。Desktop／Androidのジョブは従来どおり実行します。

| 変更内容 | iOS CI |
| --- | --- |
| `iosApp/` | 実行 |
| `core/`・`hostInput/`・`playerShared/` | 実行（共通モジュール全体を保守的に対象とする） |
| Gradleスクリプト・設定・wrapper・`buildSrc/`・`build-logic/` | 実行（別モジュールのGradleスクリプトも構成処理へ影響し得る） |
| `.github/`・この判定スクリプト・対応テスト | 実行 |
| `LICENSE`・`THIRD_PARTY_NOTICES.md`・`docs/licenses/` | 実行 |
| iOS同梱の `PLAYER_HELP_IOS.md`・`PLAYER_PRIVACY_POLICY_IOS.md`・`PLAYER_STUDIO_GUIDE.md` | 実行 |
| 上記以外の文書・Desktop／Android／Controller UIソースだけ | 省略 |

同梱文書の対象は`iosApp/build-shared.sh`と対応させています。共有モジュール内のプラットフォーム別ソースも
今回は一括で対象に含めます。入力や同梱リソースを追加した場合は判定とテストを更新してください。

`Detect iOS changes`ジョブで履歴を取得し、PRはmerge-baseからheadまで、mainへのpushは
イベントのbeforeからafterまでを比較します。直近1コミットだけの比較やGitHubのパスフィルターの
ファイル数上限に依存せず、削除・移動前後のパス、空白・改行・日本語を含む名前を扱います。
差分の取得・判定に失敗した場合はiOSを実行します。判定ジョブ自体の失敗でもiOSを実行し、
ワークフローがキャンセルされた場合は起動しません。

ワークフロー全体のパスフィルターではなく、従来の`Build and test (iOS unit tests)`ジョブを
条件付きでスキップします。既存のチェック名を維持します。省略されたジョブはテスト成功の実績として扱わず、
チェックの状態で区別してください。全CIを明示的に実行したい場合は、Actionsの`CI`を`Run workflow`から起動できます。

判定ロジックのローカル検証：

```sh
python3 -m unittest discover -s scripts/tests -p test_ci_ios_changes.py
```

Simulator起動とコンパイルの並行化は既存の`codex/ios-ci-speed`の変更です。この判定は
それとは別に不要なiOS実行を省くもので、必要なiOSビルド1回の所要時間を短縮する変更ではありません。

## generate-s2-cp932.py

PC-1360KのBASIC文字処理で使う、S2用CP932文字変換表を生成します。
Pythonの標準ライブラリーからCore用の`S2JapaneseText.kt`を生成するもので、
実機のROM吸い出しやROMデータの生成は行いません。Python 3が必要です。

リポジトリ直下で、生成済みソースを変更せずに照合する場合：

```sh
python3 scripts/generate-s2-cp932.py --verify
```

再生成する場合：

```sh
python3 scripts/generate-s2-cp932.py
```

再生成は`core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1360/S2JapaneseText.kt`を
上書きします。変更差分を確認してください。通常のStudioビルドでは実行しません。

## Windows向け開発補助

以下は`docs/`から移動した開発用スクリプトです。リポジトリ直下から実行します。
Studio利用者が実行する必要はありません。

| ファイル | 用途 |
| --- | --- |
| [sync-from-mac.cmd](sync-from-mac.cmd) | Mac側のソースをWindowsへ同期する起動用ラッパー |
| [sync-from-mac.ps1](sync-from-mac.ps1) | ソース同期の本体。対象・除外・失敗時の扱いは[ビルド手順](../docs/reference/BUILDING.md)を参照 |
| [test-sync-from-mac.ps1](test-sync-from-mac.ps1) | 一時フォルダー内の合成データで同期処理を検証 |
| [inspect-msi.ps1](inspect-msi.ps1) | Windows InstallerのCOM APIでMSIのプロパティを検査 |

同期の実行前に`PGP_SYNC_SOURCE`・`PGP_SYNC_DESTINATION`等の設定と対象パスを確認し、
`sync-from-mac.cmd /preview`で内容を確認してください。MSI検査はWindows上で実行します。

## set-dmg-volume-icon.sh

macOSの`packageDmg`タスクから自動実行し、マウントしたディスクのJava標準アイコンを
StudioのICNSへ変更します。アプリ本体のアイコンとは別の設定です。
Composeがjpackageの作業用resource directoryを再生成するため、内部パスへファイルを
差し込まず、生成DMGの一時的な書込み用コピーへ`.VolumeIcon.icns`とカスタムアイコン属性を設定します。
再圧縮・検証が成功してから元の生成DMGを置換します。ユーザーがマウント中のディスクは操作しません。

macOS、`hdiutil`、Xcode Command Line Toolsの`SetFile`が必要です。

```sh
sh scripts/set-dmg-volume-icon.sh path/to/generated.dmg desktopApp/icons/pokecom-go-dmg.icns
```

タスクはスクリプトとアイコンを入力として追跡し、CIでも同じ処理を行います。

## macOSの公証

`notarize-macos.py`は署名済みapp／DMGを公証し、Accepted後にstaple・検証を行います。
`python3 -m unittest discover -s scripts/tests -p test_notarize_macos.py`で、拒否・処理中・タイムアウト時に
stapleへ進まないことを確認できます。認証・CI設定は[署名手順](../docs/reference/MACOS_SIGNING.md)を参照してください。

## 配布版Javaランタイムの検証

`./gradlew :desktopApp:verifyDistributionRuntime`は配布アプリを生成し、実際の同梱ランタイムの
`release`ファイルを調べます。MCPサーバー用の`jdk.httpserver`と、LWJGL用の`jdk.unsupported`が
不足していれば失敗します。全OSのRelease CIで、公証や資産収集より前に実行します。
開発JDK上の単体テストだけでは検出できない、縮小ランタイムのモジュール欠落を検出するための検査です。
