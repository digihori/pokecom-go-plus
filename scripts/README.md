# 開発用スクリプト

PGPのソースコード生成・保守に使うスクリプトです。Studio利用者が実行する必要はありません。
単独利用する補助ツールは[tools/](../tools/README.md)を参照してください。

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
