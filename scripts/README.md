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
