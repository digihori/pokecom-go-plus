# pcwav 配布用スクリプト

BASICテキスト／生バイナリーとポケコンのカセットWAVを変換する、単独実行用のPerlスクリプトです。
Studioからは呼び出しません。Perl 5の実行環境を各自で用意してください。
追加のCPANモジュール、pcwavソースの取得・ビルドは不要です。

| ファイル | 用途 |
| --- | --- |
| `pcwav-encode.pl` | BASICテキスト／生バイナリーからWAVを作成 |
| `pcwav-decode.pl` | WAVからBASICテキスト／生バイナリーを復元 |

## 使用例

以下はリポジトリ直下で実行します。出力先に既存ファイルがあると上書きされるため、別名を指定してください。

```sh
perl tools/pcwav/pcwav-encode.pl s1basic tools/pc1360k-rom-dump/transfer-test.bas transfer-test.wav
perl tools/pcwav/pcwav-decode.pl s1basic transfer-test.wav transfer-test-decoded.bas
```

実機から録音したWAVを復元するときは、2番目のコマンドの入力を録音ファイルに置き換えます。
BASICはUTF-8テキストです。整形によって空白等が変わる場合があります。

### 対応モード

| モード | 内容 |
| --- | --- |
| `oldbasic` | OLD BASIC |
| `s1basic` | S1 BASIC |
| `s2basic` | S2 BASIC |
| `oldbin` | OLDバイナリー |
| `s1bin` | S1バイナリー |
| `raw` | WAVの生バイト列復元（decodeのみ。プログラム本体だけの抽出ではない） |

BASIC encodeは末尾に転送名を省略可能な引数として指定できます。
バイナリーencodeは転送名、開始アドレスの順です。アドレスを省略すると`0xC000`になるため、
実際のロードアドレスを明示することを推奨します。

```sh
perl tools/pcwav/pcwav-encode.pl s1bin program.bin program.wav PROGRAM 0x9000
perl tools/pcwav/pcwav-decode.pl s1bin recorded.wav recovered.bin
```

バイナリー復元出力にはアドレス情報を保持しません。開始アドレスを確認・記録し、必要なら
[メモリー変換ツール](../memory/README.md)でDMPへ変換してください。
DMPを直接入力することはできません。

引数なしで実行すると使用方法を表示して非ゼロで終了します（`--help`方式ではありません）。

```sh
perl tools/pcwav/pcwav-encode.pl
perl tools/pcwav/pcwav-decode.pl
```

[PC-1360K漢字ROM吸い出し](../pc1360k-rom-dump/README.md)では、BASICの転送と事前確認に使います。
本番の漢字ROM録音の復元には専用の`decode.py`を使います。
通常ROMを吸い出す実機プログラムは、このpcwavスクリプトには含まれません。

## 取得元と更新

- 取得元：[digihori/pcwav](https://github.com/digihori/pcwav)
- 元コミット：`14e1ef53596f0d8791b6d14965061f63846d11a7`
- スクリプト内のバージョン：`0.5`
- 同梱日：2026-10-22
- 元の`build.pl`で生成した2本に、下記の同梱版修正を加えています。

修正はpcwav側で行い、確認済みのコミットから生成し直して取り込んでください。
このリポジトリの通常ビルドではpcwavの取得・生成を行いません。
更新時は元コミットと下記SHA-256も更新します。

```text
999792ead387dade704dc9f95e9115b2bec687cdcb7427a1721d862ccaa4fd5d  pcwav-encode.pl
2e00085d214e1cede00820fe05ecebb5588dd01a76b500da9d591b7648d3aa41  pcwav-decode.pl
```

### 同梱版の修正

元コミットでは開始アドレスやdecodeのスキップ数値オプションから呼ぶ
`PCWAV::Common::parse_num`が未定義でした。両スクリプトのCommonモジュールに、
10進数／`0x`付き16進数を読む関数だけを追加しています。数値以外と負数は拒否します。
再生成時はこの修正も再適用するか、上流で修正済みか確認してください。
BASICのdecode出力は`10:PRINT ...`のように行番号の後にコロンを付けます。

元コミットにはLICENSEファイルや明示的なライセンス表記がありません。
ここでは独自にライセンスを付与せず、PGPのMITライセンスが自動適用されるとは扱いません。

## 検証

```sh
perl -c tools/pcwav/pcwav-encode.pl
perl -c tools/pcwav/pcwav-decode.pl
python3 tools/pcwav/test_roundtrip.py
```

往復テストはPython 3.10以降とPerlを使い、一時フォルダー内の合成BASIC／バイト列だけを扱います。
この同梱版の実機確認状況は、各機種の手順・記録を参照してください。
