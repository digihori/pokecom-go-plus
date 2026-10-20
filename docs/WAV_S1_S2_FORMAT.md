# S1／S2 WAV参照動作仕様

## 1. 文書の位置付け

この文書は、将来PGPへS1／S2 WAV Codecを実装するため、同一作者の`pcwav`を読み取り専用で調査した結果を
記録する。参照commitで観測した動作と、2026-10-12に作者から提供された仕様・動作実績を区別して記録する。
作者確認によりS2 typeの意味、末尾checksumの採用、S2 BinaryのS1共用をPGP実装方針として確定した。
2026-10-20にS1 Binaryの実機確認済み末尾checksumと120-byte境界の差を追記し、
段階1（仕様・検証データ整理）を実施した。その後、段階2の共通信号と段階3のS1 BinaryをCore／Studioへ追加。
2026-10-21に段階4のS1 BASICを追加。同日、段階5のS2 BASIC（type 27／37）も追加。現在の範囲と実機未確認事項は[進捗](WAV_S1_S2_PROGRESS.md)を参照。

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

これはOLDの19-unit framingと異なり、low nibble後の同期`W1`が1個である。参照Decode CLIは信号Decode前に
既定で先頭1000 msを除外するが、RawDecode library自体は固定skipを要求しない。PGPでは固定skipを形式仕様に
せずleader／同期検出として実装する。

S1／S2 BASICのCLI Encoderは論理payload末尾`FF FF checksum`の後へ、物理stream trailerとしてさらに`FF`を
1 byte追加する。この追加byteはBASIC payloadの一部として解釈しない。

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

metadataは次の8 bytesである。

```text
00 00 00 00 startHi startLo lengthMinus1Hi lengthMinus1Lo
```

従ってS1 Binaryは、開始addressから`lengthMinus1 + 1` bytesの単一連続領域だけを表す。複数領域やgapを
記録する構造はない。PGPの`.dmp`／Intel HEX入力に複数領域またはgapがある場合は拒否し、暗黙に結合または
zero-fillしない。

bodyはlogical byteをnibble swapして格納する。120 data bytesごとに、swap後のdataを対象にした
chunk checksumを挿入する。最後がちょうど120 bytesの場合もそのchunk checksumを付ける。
最後のpartial chunk（1〜119 bytes）にはchunk checksumを付けない。

末尾は`FF FF tailChecksum`。採用するtail checksumは、**最後のpartial raw bytes＋最初の`FF`**を
対象に共通S1 checksumを計算し、nibble swapして格納する。2個目の`FF`は加算しない。
body長が120の倍数ならpartialは空で、`FF`だけを対象とする。

2026-10-18のPC-1360K実機録音32レコード（各4112 bytes）と、その後の配布デコーダーで確認した方式を採用する。
参照pcwav Encoderは長さによらず`FF`だけからtailを計算するため、partialがある入力では差が出る。
PGPはこの旧方式へ合わせず、checksum不一致として診断する。旧方式を黙って受け入れる互換fallbackは追加しない。
Binaryの他の長さ境界へこの構造を適用することと、それぞれを実機で確認したことは区別する。

| body長 | 区切りchecksum付きchunk数 | tail checksumの対象（swap後） |
| --- | ---: | --- |
| 1 | 0 | 最後の1 byte＋`FF` |
| 119 | 0 | 最後の119 bytes＋`FF` |
| 120 | 1 | `FF`のみ |
| 121 | 1 | 最後の1 byte＋`FF` |
| 240 | 2 | `FF`のみ |

参照S1 Binary Decoderは`remaining > 120`でchunkを処理するため、Encoderが出す最後のfull chunkの
checksumと整合しない。また、宣言長の`+1`後に`& 0xFFFF`を行い65536が0になる。
PGPはmetadataを`1..65536`として扱い、`start + length <= 65536`を別途検証する。
これらの境界動作も参照Decoderの成功だけでは判定しない。

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
最後のchunkは長さが120 bytesでもtail計算へ含める。Binaryの「120の倍数ならpartialは空」と混同しない。

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

- S1 Binary: body長`1`、`119`、`120`、`121` bytes、開始address境界、実機保存WAV
- S1 BASIC: bodyが119／120／121 bytesとなるsource、半角カナ、末尾checksum、物理trailer `FF`
- S2 BASIC: type `27`／`37`、行参照、`ON ... GOTO/GOSUB`、半角カナ／漢字、末尾checksum、物理trailer
- 各形式: PGP Encoderと実機、`pcwav`のraw byte列およびPCM比較
- S2 Binary: S1共用codecの境界・checksum試験に加え、S2機種での実機往復WAV


## 9. 段階1の合成検証データ（2026-10-20）

[test-data/wav/s1-s2](../test-data/wav/s1-s2/README.md)に28件の固定vectorを用意した。
logical byte列から独立に組み立てた採用期待値と、固定commitのpcwavが出す参照値を分けて保存する。
PGPの製品codecを使って期待値を生成していない。参照値の再採取だけに外部pcwav／Perlが必要であり、
通常の検証と将来のCoreテストには固定JSONを使う。

- Binary: 1／119／120／121／239／240／241 bytes、終端やtypeと同じ値を含むbody、開始`0000`／`4000`／`FFFF`。
- S1／S2 BASIC: logical body 119／120／121／239／240／241 bytes、quote／REM、カナ、S2漢字。
- S2: `ON ... GOTO`の行番号列、`THEN`の行参照、同じbodyでtype `27`／`37`を比較。
- 共通信号: `00..FF`全256 bytes、leader込みPCMのsample数とSHA-256。
- 各payload: raw全byteと、BASICの物理trailerを含めたPCMのsample数・SHA-256。

Binaryの参照値は旧tailを含むため、差分を認識する対照データであり、無条件の成功期待値ではない。
実機ROM・録音由来のbyte列は追加していない。

### 後続で追加する失敗系・最大値の検証

製品codec追加時、上記の固定正常vectorへ決定論的な破損を加えて失敗段階を検証する。

| 対象 | 条件 | 期待する扱い |
| --- | --- | --- |
| name／metadata／chunk／tail | checksumまたは対象dataを1 bit反転 | 位置付き`CHECKSUM_MISMATCH`、通常Program出力不可 |
| header／body／終端 | 切詰め、`FF`欠落、余剰raw | 切詰め・終端・余剰の診断。PCM/rawは保持 |
| Binary範囲 | 空body、先頭FFFF＋2 bytes、先頭0000＋65536 bytes | 前2つは拒否。最後はmetadataのFFFFを65536として扱う |
| Binary入力 | 複数領域・gap | 連結・zero-fillせず拒否 |
| BASIC行 | 不正長・行末0D欠落・行番号範囲・未知byte | 転送構造と文字変換の診断を分ける |
| 信号 | 無音、振幅差、別sample rate、先頭ずれ、途中欠落 | 同期検出・診断とraw位置対応を検証 |

## 10. 実装順序と今回の区切り

1. 仕様と合成検証データ整理：完了。
2. S1/S2共通信号のEncode／Decode：完了。OLDの19-unit処理を変更せず、S1の16-unit処理を追加する。
3. s1bin：payloadとStudio接続・合成検証は完了。PGP Encoderでの実機往復は未確認。
4. s1basic：機種別BASICの転送body adapterとStudio接続・合成検証が完了。実機往復は未確認。
5. s2basic：機種別語彙、行参照、日本語、typeのStudio接続・合成検証が完了。実機往復は未確認。
6. UI・診断・操作ガイド・OLD回帰の仕上げ。

WAVはStudio専用とし、Player、音声デバイスI/O、ROM CSAVE/CLOAD hook、Decodeからの直接ロードは追加しない。
既存のBASIC Codecを再利用するが、S2のPC-1360系／PC-1470U系などの語彙を一律に扱わない。
