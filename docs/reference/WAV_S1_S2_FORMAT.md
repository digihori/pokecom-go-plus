# S1／S2 WAV参照動作仕様


## 2026-10-22追記：pcwav 0.5.2の実機による訂正

pcwav `156bb6a2ed09df3ac03c6e7ccc42e96c87cc406d` の追加実機検証により、
S1 Binaryも最後の1〜120 raw bytesには独立chunk checksumを付けず、
最後のchunk全体と最初の`FF`をtail checksumへ含める規則と確認された。
従来の§5.1の「120の倍数なら最後にもchunk checksumを付け、tailはFFのみ」は旧想定である。
4112 bytesのpartial-tail確認自体は引き続き有効。今回更新した単独pcwavは訂正済み。
Studio/Core内蔵Codecも同規則へ追従。固定raw／PCM期待値、境界・破損テストと
StudioのWAV変換経路で検証する。今回新たに実機録音を取得したわけではない。
上流記録：`docs/BUGFIX_0.5.2.md`、`docs/HARDWARE_TEST_0.5.1.md`。
旧版との差は履歴として記録し、§5.1は現在の規則へ更新した。

## 1. 文書の位置付け

この文書は、PGPのS1／S2 WAV Codecの参照仕様と、同一作者の`pcwav`を読み取り専用で調査した結果を
記録する。現在の操作・PGPの対応範囲は[操作ガイド](../guides/WAV_TOOL_GUIDE.md)を参照する。参照commitで観測した動作と、2026-10-12に作者から提供された仕様・動作実績を区別して記録する。
作者確認によりS2 typeの意味、末尾checksumの採用、S2 BinaryのS1共用をPGP実装方針として確定した。
2026-10-20にS1 Binaryの実機確認済み末尾checksumと120-byte境界の差を追記し、
段階1（仕様・検証データ整理）を実施した。その後、段階2の共通信号と段階3のS1 BinaryをCore／Studioへ追加。
2026-10-21に段階4のS1 BASICを追加。同日、段階5のS2 BASIC（type 27／37）も追加。現在の範囲と実機未確認事項は[進捗](../development/WAV_S1_S2_PROGRESS.md)を参照。

- Reference repository: `pcwav`
- Reference commit: `14e1ef53596f0d8791b6d14965061f63846d11a7`
- Primary reference files:
  - `src/PCWAV/WavWriter.pm`
  - `src/PCWAV/RawDecode.pm`
  - `src/PCWAV/Common.pm`
  - `src/PCWAV/Format/S1.pm`
  - `src/PCWAV/Binary/S1Encode.pm`
  - `src/PCWAV/Binary/S1Decode.pm`
  - `src/PCWAV/Basic/S1Encode.pm`
  - `src/PCWAV/Basic/S1Decode.pm`
  - `src/PCWAV/Basic/S2Encode.pm`
  - `src/PCWAV/Basic/S2Decode.pm`
  - `src/PCWAV/TextCodec.pm`
  - `src/encode_main.pl`
  - `src/decode_main.pl`

## 2. 対応状況

| 形式 | Encode | Decode | 参照実装での扱い |
|---|---:|---:|---|
| S1 Binary | あり | あり | 開始addressと長さを持つ単一連続body |
| S1 BASIC | あり | あり | type `07` |
| S2 BASIC | あり | あり | `27`: BASIC、`37`: password付きBASIC（作者確認） |
| S2 Binary | なし | なし | 参照commitに専用実装なし。作者確認によりPGPではS1 Binaryを共用 |

この表は参照pcwavの実装状況を示し、PGPで実装済みという意味ではない。
2026-10-12の作者指定により、PGPのS2 Binaryは§5のS1 Binaryと同じ方式（type `67`、
header/metadata、checksum、body、終端、共通信号）を共用する。これはS1からの独自類推ではない。
単一連続領域制約も共通とする。作者提供情報を採用し、fixture未取得を実装開始の阻害条件にはしない。

## 3. S1／S2共通信号

参照EncoderはS1 Binary、S1 BASIC、S2 BASICに同じ物理信号を使用する。

- WAV: 8000 Hz、8-bit unsigned、mono PCM
- 1信号単位: 16 samples、2 ms
- `W1`: `FF 00`を8回（4 kHz矩形波）
- `W0`: `FF FF 00 00`を4回（2 kHz矩形波）
- leader: `W1`を`0x800`回、32,768 samples、4.096秒
- 1 byte: 16信号単位、256 samples、32 ms

byte framingは次の順である。data bitは各nibble内でLSB firstとする。

```text
W0
bit 0, bit 1, bit 2, bit 3
W1
W0
bit 4, bit 5, bit 6, bit 7
W1 W1 W1 W1 W1
```

2026-10-31の提供実録音では、ヘッダは16-unit、データ部はhigh nibble後のW1を2単位に短縮した
13-unitの送出を確認した。末尾SUM後の停止区間も一定の5単位とは限らない。
PGP Decodeはbyteごとの実際のW1→W0同期位置を近傍探索し、high nibble後の2〜5単位の停止を受理する。
各start／stop、carrier、payloadの長さ・終端・SUMの検査は維持する。
録音末尾の搬送波減衰・DC settlingを、新しいデータbyteとして出力しない。
Encoderの16-unit／8000Hz出力は変更しない。

これはOLDの19-unit framingと異なり、low nibble後の同期`W1`が1個である。参照Decode CLIは信号Decode前に
既定で先頭1000 msを除外するが、RawDecode library自体は固定skipを要求しない。PGPでは固定skipを形式仕様に
せずleader／同期検出として実装する。

S1／S2 BASICのCLI Encoderは論理payload末尾`FF FF checksum`の後へ、物理stream trailerとしてさらに`FF`を
1 byte追加する。この追加byteはBASIC payloadの一部として解釈しない。

### PGPの入力レート（2026-10-31）

S1／S2の信号Decodeは8000／16000／24000／32000／44100／48000Hzを受理する。
44100／48000Hzは低域フィルタ付きで16000Hzへ再標本化して解析する。
rawへの対応、同期・診断位置は元入力sample座標へ戻し、元PCMを保持する。
Encodeの8000Hz出力、転送byte列・checksum対象は変えない。
速度補正追加・実録音耐性の保証は今回の範囲に含めない。

## 4. S1 checksum

共通S1 checksumは各byteをlow nibble、high nibbleの順で加算し、low nibble加算直後にend-around carryを
適用する。格納時には計算結果をnibble swapする。ただしBASIC末尾checksumは別処理であり、同じ計算だと
仮定してはならない。

参照S1 Binary Decoderはname／metadataのchecksumを検査するが、末尾checksumは検査していない。
BASIC Decoderにもchunk／tail checksumの比較を省略する経路がある。
PGP Decoderは参照実装の省略・不整合を引き継がず、各checksumを検証し、不一致位置をdiagnosticとして返す。
BASIC末尾の処理は、logical byteのhigh nibbleを加算した直後だけend-around carryを行い、
low nibble加算後はmod 256とする。参照ソース内の「単純mod 256・キャリー補正なし」という古い説明は採用しない。
これはswap済みrawに共通S1 checksumを適用する式と一致する。どのbyte列を渡すかを明確に分ける。

## 5. S1 Binary

### 5.1 payload

```text
67
name block (8 bytes)
name checksum (1 byte)
metadata (8 bytes)
metadata checksum (1 byte)
body
FF FF body-tail-checksum
```

name blockは`00 + 逆順filename 6文字 + 00 padding + F5`で合計8 bytesである。filenameは大文字化し、
英数字とspace以外を除外して最大6文字とする。

Encoderが出力するmetadataは次の8 bytesである。

```text
00 00 00 00 startHi startLo lengthMinus1Hi lengthMinus1Lo
```

Decodeでは先頭4 bytesを0に制限しない。PC-1360KのCSAVE M実録音では`EF C0 00 00`を確認し、
metadata checksum、開始C000、長さ240、bodyとtail checksumがすべて一致した。
先頭4 bytesは意味を推測して配置へ適用せず、原値を`copyReservedMetadata()`とrawで保持し、Studioのsummaryにも表示する。
残り4 bytesによる開始・長さ、全checksum、終端、余剰dataの検証は維持する。
DMP／HEXには転送headerを持たないため、再Encodeの先頭4 bytesは既定の0となり、元headerの再現は保証しない。

従ってS1 Binaryは、開始addressから`lengthMinus1 + 1` bytesの単一連続領域だけを表す。複数領域やgapを
記録する構造はない。PGPの`.dmp`／Intel HEX入力に複数領域またはgapがある場合は拒否し、暗黙に結合または
zero-fillしない。

bodyはlogical byteをnibble swapして格納する。最後以外の120 data bytesごとに、
swap後のdataを対象にしたchunk checksumを挿入する。
最後のchunkは1〜120 bytesで、ちょうど120 bytesでも個別chunk checksumを付けない。

末尾は`FF FF tailChecksum`。tail checksumは **最後のchunkのraw bytes＋最初の`FF`** を
対象に共通S1 checksumを計算し、nibble swapして格納する。2個目の`FF`は加算しない。
body長が120の倍数でも、最後の120 bytesをtail計算へ含める。

2026-10-18のPC-1360K実機録音32レコード（各4112 bytes）でpartial-tailを確認。
2026-10-22のpcwav側追加実機検証で120／240 bytesの最終full chunk規則を訂正した。
pcwav 0.5.2は訂正済み。旧pcwavのFF-only tailや最終full chunkへの余計なchecksumは
黙って受け入れず、不一致／終端不正として診断する。

| body長 | 区切りchecksum付きchunk数 | tail checksumの対象（swap後） |
| --- | ---: | --- |
| 1 | 0 | 最後の1 byte＋`FF` |
| 119 | 0 | 最後の119 bytes＋`FF` |
| 120 | 0 | 最後の120 bytes＋`FF` |
| 121 | 1 | 最後の1 byte＋`FF` |
| 240 | 1 | 最後の120 bytes＋`FF` |

旧参照Decoderの`remaining > 120`条件自体は実機のchunk境界と一致するが、
末尾SUM検証の欠落と65536 bytesの長さが0になる問題があった。pcwav 0.5.2はこれらも修正済み。
PGPはmetadataを`1..65536`として扱い、`start + length <= 65536`を別途検証する。
最大長の境界テストは合成検証で、実機確認と区別する。

### 5.2 Decode結果

参照Decoderはtype `67`を探索し、filename、開始address、宣言長、body、footer検出結果を返す。bodyは宣言長で
切り出すため連続領域である。PGPではheader／metadata／body／footer checksum、16-bit address範囲、truncation、
余剰raw dataを構造化diagnosticとして検証する。

## 6. S1 BASIC

payload headerは次の10 bytesで始まる。

```text
07 + reversed filename (7 bytes) + F5 + header checksum
```

2026-10-21追記：利用者指定の保護付きS1 BASICはtype `17`として生成・識別・保持し、
同じfilename／body／checksum構造を使用する。通常形式の固定参照vectorはtype `07`のままとする。

filenameはprintable ASCII最大7文字で、末尾を`00` paddingしてから7 bytes全体をreverseする。header checksumは
逆順filename 7 bytesと`F5`を対象とし、type `07`を含めない。

logical bodyは行ごとに次の構造を持つ。

```text
lineNumberHi lineNumberLo lineLength statementBytes 0D
```

- line number: `1..65279`
- lineLength: statement bytesと末尾`0D`の合計、最大255
- keyword: 1-byte token
- ASCII: `20..7E`
- 半角カナ: `FE` + Shift-JIS half-width kana byte (`A1..DF`)
- quote内と`REM`後: token化せずtextとして扱う
- source中のspace／tab: quote／`REM`外では原則除去する

bodyはlogical byteをnibble swapし、120-byte chunkに分割する。最後以外のfull chunkにはswap後dataを対象にした
checksumを挿入し、最後のchunkには長さが120 bytesでもchunk checksumを付けない。末尾は
`FF FF tailChecksum`である。
tail checksumは最後のchunkのswap前logical bytesと最初の`FF`を対象に、
high nibble加算→carry補正→low nibble加算→mod 256の順で計算し、nibble swapして格納する。
最後のchunkは長さが120 bytesでもtail計算へ含める。Binaryも最後の1〜120 bytesをtail計算へ含める規則である。

## 7. S2 BASIC

S2 BASICはS1 BASICと同じheader／120-byte chunk／末尾構造およびS1物理信号を再利用する。相違点は次の通り。

- type `27`: 通常BASIC、type `37`: password付きBASIC（2026-10-12作者確認）。通常BASICをdefaultとする
- keywordは`FE tokenByte`の2-byte表現
- `GOTO`、`GOSUB`、`THEN`等の数値行参照は`1F lineHi lineLo`
- `ON ... GOTO/GOSUB`はcomma区切りの行番号列を同形式へ変換する
- ASCII、半角カナ、2-byte Shift-JISを含むCP932 textを扱う
- line構造とline number範囲はS1 BASICと同じ

参照EncoderにはS2 tail checksumを「現状はS1と同じ暫定」とするcommentがあるが、
2026-10-12に作者からpcwavの動作実績と同方式の採用方針が提供された。
PGPは§6のpcwav BASIC tail checksum方式を正式な実装方針として採用し、commentだけを理由に実装を保留しない。
参照S2 Decoderはchunk/tail checksumを比較せず読み飛ばす経路があるため、PGPではEncoder方式で検証し、
不一致をdiagnosticとして返す。参照一致試験と、今後のPGP実機往復確認は別に記録する。

password付きtypeの意味の確定は、password文字列の入力/保存方法の設計まで決定するものではない。
上記固定commitのS2 Encoderを照合した結果、type `27`と`37`は同じソースから
同じbodyを作り、headerの先頭typeだけが変わる。独立したpassword文字列引数やpassword保存領域を追加する処理はない。
PGPでもtype `37`の識別・保持と、password文字列を新規設定するUIを分ける。
文字列設定の仕様が未定であることを、共通信号・通常BASICの実装開始の阻害条件にしない。

## 8. 検証に追加するfixture

この節は記録時点の内容です。[S1／S2 WAVのfixture・実装順履歴](../development/WAV_S1_S2_HISTORY.md)へ分離しました。

## 9. 段階1の合成検証データ（2026-10-20）

この節は記録時点の内容です。[S1／S2 WAVのfixture・実装順履歴](../development/WAV_S1_S2_HISTORY.md)へ分離しました。

## 10. 実装順序と今回の区切り

この節は記録時点の内容です。[S1／S2 WAVのfixture・実装順履歴](../development/WAV_S1_S2_HISTORY.md)へ分離しました。

## EncodeとDecodeの許容方針

- Encode：既知の標準形を安定して生成する。既定の予約値・16-unit信号・8000Hz出力を維持する。
- Decode：Encoderの既定値を必須条件にせず、実機が出す未解釈の値や確認済みのframe差を保持して受理する。
- checksum不一致、宣言長不足、16-bit範囲外、欠落終端等は正常なプログラムとして出力しない。
- 未知値の意味を推測して実行・配置を変更しない。許容の根拠と回帰例を記録する。
