# Machine Code File Formats

PGPはマシン語のアドレス付きファイル形式として、次の2形式を正式に扱う。

- PGP Memory Dump (`.dmp`、Studio／Projectの標準形式)
- Intel HEX (`.hex`／`.ihx`、現在はStudio WAV Toolの入出力で対応)

Raw Binary (`.bin`)はアドレス情報を持たないため、通常のStudio sourceまたはbuild成果物にはしない。
互換import時に限って開始addressを一度指定し、直ちにアドレス付きmemory imageへ変換する。Projectへ保存する
場合は`.dmp`へ変換する。Intel HEXはWAV Toolの交換形式として扱う。各画面が`.bin`用の開始address入力欄を
個別に持つ設計にはしない。

既存Project manifestの`raw-binary` sourceは移行期間の後方互換入力として読み込める状態を維持するが、
新規Project作成や通常の追加操作では生成しない。読込み後は`.dmp`への変換を案内する。

## 拡張子とファイル内容

`.hex`という拡張子だけではIntel HEX形式とは判定できません。
`6100 34 ; PUSH`のようなアドレス付きダンプはDMPパーサーで読める内容であり、
Studioのマシン語ロードは拡張子が`.hex`でもその内容を受理します。
一方、`:049000000001020366`のようなレコードで構成するIntel HEXは別形式です。
本書のIntel HEX対応範囲はファイル内容についての説明です。
Android Playerの拡張子の受理条件は[Player入出力](../guides/PLAYER_PROGRAM_IO.md)を参照してください。

## 共通の扱い

- Studio内部では、入力形式にかかわらずアドレス付きデータ領域の集合へ変換して扱う。
- Assemblerの標準成果物は`.dmp`とする。
- Debugger、Emulator load、Project build、AI解析、WAV Toolは同じmemory image表現を受け渡す。
- Studioからの標準保存形式は`.dmp`、外部交換・厳密なchecksum用途はIntel HEXとする。
- Raw Binaryの開始address指定はimport境界だけに置き、変換後の各Toolでは再入力させない。
- 複数領域や非連続領域を保持できるのは`.dmp`とIntel HEXであり、利用先が単一連続領域を要求する場合は
  暗黙にgapを埋めず、明示的なエラーにする。

## PGP Memory Dump (`.dmp`)

`.dmp`は、人が読み書きしやすいアドレス付き16進ダンプ形式である。
文字コードはUTF-8とし、改行はCRLF、LF、CRを受理する。

```text
; address followed by separate bytes
9000 00 01 02 03

# ':' and contiguous data are also accepted
9100:1112
A000 : FF EEFF
9200 20 21 22 23:7A
```

### 規則

- 各データ行は、4桁の16進アドレスと1バイト以上のデータで構成する。
- アドレスとデータは、空白またはコロン (`:`) で区切る。
- データは2桁ごとに1バイトと解釈する。
- データの空白区切り、連続表記、および両者の混在を許可する。
- 行ごとにデータ長が異なってよい。
- 行の順序は自由であり、離れた複数のアドレス範囲を記述できる。
- 空行と、`;`または`#`以降のコメントを許可する。
- 行末に`:XX`形式の2桁16進チェックサムがある場合は受理する。v1では値を検算せず読み飛ばす。
- 英字の大文字と小文字を区別しない。
- 同じアドレスを複数行から指定した場合は、値が同一でもエラーとする。
- データが`0xFFFF`を越える場合はエラーとする。
- v1ではチェックサムの計算方式を定義しない。既存ダンプの値は保持・検算せず、厳密な検証が必要な用途にはIntel HEXを使用する。

次の例は、`0x9002`と`0x9003`が重複するため無効である。

```text
9000 00010203
9002 1011
```

パーサーはファイルをアドレス付きデータ領域の集合へ変換する。メモリへの書き込み可否は、
対象機種とMemory Profileのポリシーが別途判定する。

## Intel HEX (`.hex`)

Intel HEXは外部Toolや実機関連Toolとの交換形式とする。現在の実装はStudio WAV Toolの
`DesktopWavBinaryFiles`であり、`.hex`／`.ihx`の読込みとDecode結果の保存に対応する。
Projectの通常sourceやPlayerロードまで対応しているという意味ではない。

- record長とchecksumを検証し、data record（00）を16-bitのアドレス付きデータへ変換する。
- EOF record（01）は必須。EOF以後のrecordは拒否する。
- 拡張segment／linear address record（02／04）はbaseが0の場合だけ受理する。
- その他のrecord、16-bit範囲外のdata、非0の拡張baseは拒否する。
- WAV Binaryが要求する単一連続領域の検証は変換後に行い、gapを暗黙に埋めない。
- 出力は16バイト単位のdata recordとEOF recordで構成する。

実装経緯と実機未確認事項は[WAV対応記録](../development/WAV_S1_S2_PROGRESS.md)、
操作は[WAV Toolガイド](../guides/WAV_TOOL_GUIDE.md)を参照する。

## Raw Binary (`.bin`) import

Raw Binary importは、入力byte列とユーザーが一度指定した開始addressから単一の連続領域を生成する変換操作である。
空ファイル、16-bit address空間を越える入力は拒否する。変換結果をProjectへ追加するときは`.dmp`として保存し、
元の`.bin`と開始addressをProject sourceの組として保持しない。

## 旧Pokecom GOからの移行と単独変換ツール

旧Pokecom GOのマシン語保存ファイルは開始アドレスを含まない生バイナリーです。
PGPでの違いと移行の入口は[はじめに](../guides/GETTING_STARTED.md#最初に旧pokecom-goから移行するマシン語ファイル)、
実際の変換は[変換ツール](../../tools/memory/README.md)を参照してください。
単独ツールは生バイナリーからDMP／Intel HEX、DMPから生バイナリー／Intel HEXへの変換を提供します。
形式変換後の機種別ロード範囲の検証と、プログラムの実行開始は利用側で行います。
