# バイナリー／DMP／Intel HEX変換ツール

旧Pokecom GO等で使っていた生バイナリーを、Studioで読めるアドレス付きPGP Memory Dump
（`.dmp`）またはIntel HEX（`.hex`／`.ihx`）へ変換します。DMPから生バイナリーへの逆変換、
DMPからIntel HEXへの変換、DMP情報表示にも対応します。
Python 3.10以降の標準ライブラリーだけで動きます。Studioの起動やインストールは不要です。
このフォルダーの`pgp_memory.py`だけをコピーしても使えます。

## 配布ファイルと単独実行

配布するファイルは`pgp_memory.py`と、この`README.md`です。ZIPへのまとめやインストーラーは不要です。
実行時に必要なのは`pgp_memory.py`だけで、テスト用ファイルは利用者へ渡す必要がありません。
GitHub Releaseでは説明書を`pgp_memory_README.md`として添付します。ZIPへまとめる必要はありません。
ライセンスはプロジェクトの[MIT License](https://github.com/digihori/pokecom-go-plus/blob/main/LICENSE)です。

任意のフォルダーへ2ファイルを保存し、ターミナルでそのフォルダーへ移動して実行します。
元のバイナリーファイルが同じフォルダーにある場合の例です。

```sh
python3 pgp_memory.py bin-to-dmp old.bin --start 0x9000 -o program.dmp
python3 pgp_memory.py bin-to-hex old.bin --start 0x9000 -o program.hex
python3 pgp_memory.py info program.dmp
```

Windowsでは`python3`を`py -3`に読み替えられます。
`0x9000`は例なので、必ず元のプログラムのロード開始アドレスに置き換えてください。

以下はリポジトリ直下で実行する例です。Windowsで`python3`が使えない場合は`py -3`などに
読み替えてください。空白を含むファイル名は引用符で囲みます。

## 旧Pokecom GOからの移行

旧版のマシン語保存は、開始アドレスから終了アドレスまでの生バイト列です。
ヘッダーやアドレス情報は含まれません。入力は拡張子にかかわらず生バイナリーとして扱い、
元のロード開始アドレスを`--start`で指定します。拡張子の変更だけではDMPやHEXになりません。
BASICファイル・ROMセット・状態保存ファイルの変換には使いません。

StudioのProgram／ProjectやAndroid Playerへのロード用にはDMPを選びます。
Intel HEXはStudio WAV Toolや外部ツールとの交換用で、通常のProject／Player直接ロードは対象外です。

### `.hex`という拡張子と内容の違い

`.hex`には、Intel HEX形式のほか、`6100 34 ; PUSH`のようなDMP互換の
アドレス付きテキストが使われている場合もあります。後者はStudioでそのままロードできます。
`info`も拡張子にかかわらずDMPとして解析するため、この種の`.hex`を検証できます。
生バイナリー変換コマンドにテキストダンプを入力するとテキスト自体をバイト列として変換してしまうため、
ファイルの中身を確認してコマンドを選んでください。
コロンで始まるレコードのIntel HEX入力は、本ツールでは解析しません。

## 生バイナリー → DMP

```sh
python3 tools/memory/pgp_memory.py bin-to-dmp old.bin --start 0x9000 -o program.dmp
```

開始アドレスは元のプログラムのロードアドレスを指定します。ファイル名や内容からは推定しません。
数値引数は`0x9000`のような16進数か、`36864`のような10進数です。`9000`は10進数です。
開始アドレスに続けてファイルの全バイトを配置し、16バイトごとにUTF-8／LFのDMPを書き出します。
空ファイル、負のアドレス、`0xFFFF`を超えるデータは拒否します。
ヘッダー付きファイル、圧縮ファイル、エミュレーターの状態保存ファイルの解析は行いません。

出力例：

```text
9000 00 01 02 03
```

## 生バイナリー → Intel HEX

```sh
python3 tools/memory/pgp_memory.py bin-to-hex old.bin --start 0x9000 -o program.hex
```

開始アドレスと拒否条件はDMP変換と同じです。16バイト以下のデータレコード（00）と
EOFレコード（01）をUTF-8／LFで出力し、各レコードのチェックサムを計算します。
拡張アドレスや実行開始アドレスのレコードは付けません。データのロード先と実行開始位置は別です。
拡張子`.ihx`を使う場合も出力内容は同じです。

入力バイト`00 01 02 03`、開始アドレス`0x9000`の出力例：

```text
:049000000001020366
:00000001FF
```

## DMP → Intel HEX

```sh
python3 tools/memory/pgp_memory.py dmp-to-hex program.dmp -o program.hex
```

DMP内のアドレスを保持し、非連続領域は別のデータレコードとして出力します。穴は埋めません。
Studio WAV ToolでBinary WAVへ変換する場合は単一連続領域が必要です。
本ツールのIntel HEX対応は出力のみで、HEX入力の解析・逆変換は行いません。

## DMPの情報表示・検証

```sh
python3 tools/memory/pgp_memory.py info program.dmp
```

実データのバイト数、連続するアドレス範囲と各サイズ、範囲間の空きバイト数を表示します。
隣接行は同じ連続領域として集計します。形式不正、空データ、アドレス重複（値が同じ場合も）、
アドレス範囲超過を検出します。検証だけならこのコマンドを使えます。
機種別のRAM書込み可否や、プログラムが正常動作するかは判定しません。

入力は[PGP DMP仕様](../../docs/reference/MACHINE_CODE_FORMATS.md)に従い、コメント、コロン区切り、
連続16進表記、順不同の行、UTF-8 BOM、LF／CRLF／CR改行を受け付けます。
任意の行末チェックサム`:XX`は書式だけを確認して読み飛ばし、値の検算はしません。
この`info`コマンドはDMP専用で、Intel HEX入力には対応しません。

## DMP → 生バイナリー

```sh
python3 tools/memory/pgp_memory.py dmp-to-bin program.dmp -o program.bin
```

最小アドレスから最大アドレスまでを出力します。開始アドレスはバイナリーに保存されないため、
再ロード時のために別途控えてください。非連続領域は既定でエラーになります。
必要な場合に限り、穴を埋めるバイト値を明示指定できます。

```sh
python3 tools/memory/pgp_memory.py dmp-to-bin sparse.dmp --fill 0xFF -o filled.bin
```

`--fill`は0〜255です。先頭より前や末尾より後への追加、ROMバンクの連結は行いません。

## 保存とエラー

出力は新規ファイルとして作成します。既存ファイルを上書きする場合だけ`--force`を指定します。
入力自身への出力は`--force`でも拒否します。入力の検証を終えてから出力を開きますが、
ディスク容量不足等の書込み失敗時は不完全な出力が残る場合があります。
親フォルダーは事前に作成してください。

成功は終了コード0、入力・変換・入出力エラーは1、コマンド引数のエラーは2です。
エラーは標準エラー出力へ表示します。

```sh
python3 tools/memory/pgp_memory.py --help
python3 tools/memory/pgp_memory.py bin-to-dmp --help
```

## ROM吸い出しとの関係

本ツールは取得済みの生バイナリーを変換するものです。通常ROMやPC-1360K漢字ROMの
実機からの吸い出し処理は含みません。ROMイメージはStudioのROMセット作成で扱い、
変換したDMPをRAMロード用のプログラムとして使うことと区別してください。
全体の対応範囲は[ツール一覧](../README.md)を参照してください。

## 開発時の検証

```sh
python3 -m unittest discover -s tools/memory -p 'test_*.py'
```

ROMを含まない合成データで、往復変換、DMPの受理形式・拒否条件、CLIの保存動作を確認します。

## ライセンス全文

単独で配布・再配布する際も、以下の著作権表示と許諾文を保持してください。

```text
MIT License

Copyright (c) 2026 Y Horiuchi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
