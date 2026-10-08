# S1／S2 WAV参照動作仕様

## 1. 文書の位置付け

この文書は、将来PGPへS1／S2 WAV Codecを実装するため、同一作者の`pcwav`を読み取り専用で調査した結果を
記録する。記載内容は参照commit時点の実装動作であり、実機互換性が未確認の項目を確定仕様として扱わない。

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
| S2 BASIC | あり | あり | type `27`または`37` |
| S2 Binary | なし | なし | `pcwav`だけでは仕様を確定できない |

S2 BinaryをPGPへ追加する場合は、別の一次資料または実機WAV fixtureが必要である。それまでは未対応とし、
S1からの類推だけでtype、header、checksumを実装しない。入力memory imageについては安全側の共通制約として
単一連続領域だけを候補にする。

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

参照Decoderにはheader、chunk、tail checksumを実際に比較せず、位置だけ読み飛ばす経路がある。PGP Decoderは
Encode側の生成動作をそのまま「検証済み仕様」とせず、fixtureで期待値を確定してから各checksumを検証し、
不一致位置をdiagnosticとして返す。

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

bodyはlogical byteをnibble swapして格納する。120 data bytesごとに、nibble swap後のdataを対象にした
chunk checksumを挿入し、最後のpartial chunkにはchunk checksumを付けない。末尾は`FF FF`と、`FF`に対する
S1 checksumをnibble swapしたbyteで閉じる。

### 5.2 Decode結果

参照Decoderはtype `67`を探索し、filename、開始address、宣言長、body、footer検出結果を返す。bodyは宣言長で
切り出すため連続領域である。PGPではheader／metadata／body／footer checksum、16-bit address範囲、truncation、
余剰raw dataを構造化diagnosticとして検証する。

## 6. S1 BASIC

payload headerは次の10 bytesで始まる。

```text
07 + reversed filename (7 bytes) + F5 + header checksum
```

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
tail checksumは最後のchunkのswap前logical bytesと`FF`を対象にhigh nibble、low nibble順でend-around carryを
適用し、結果をnibble swapして格納する。

## 7. S2 BASIC

S2 BASICはS1 BASICと同じheader／120-byte chunk／末尾構造およびS1物理信号を再利用する。相違点は次の通り。

- typeは`27`または`37`。参照実装だけでは両者の意味を確定しない
- keywordは`FE tokenByte`の2-byte表現
- `GOTO`、`GOSUB`、`THEN`等の数値行参照は`1F lineHi lineLo`
- `ON ... GOTO/GOSUB`はcomma区切りの行番号列を同形式へ変換する
- ASCII、半角カナ、2-byte Shift-JISを含むCP932 textを扱う
- line構造とline number範囲はS1 BASICと同じ

参照EncoderのS2 tail checksumには「現状はS1と同じ暫定」と明記されている。従って、PGPでは実機fixtureを
得るまでS2 tail checksumを確定仕様にせず、暫定実装であることをdiagnosticまたは機能表示上で明示する。
参照S2 Decoderもchunk／tail checksumを検証せず読み飛ばすため、Decode成功だけを互換性の根拠にしない。

## 8. PGP実装前に必要なfixture

- S1 Binary: body長`1`、`119`、`120`、`121` bytes、開始address境界、実機保存WAV
- S1 BASIC: bodyが119／120／121 bytesとなるsource、半角カナ、末尾checksum、物理trailer `FF`
- S2 BASIC: type `27`／`37`、行参照、`ON ... GOTO/GOSUB`、半角カナ／漢字、末尾checksum、物理trailer
- 各形式: PGP Encoderと実機、`pcwav`のraw byte列およびPCM比較
- S2 Binary: type、metadata、checksum、signal framingを確認できる一次資料または実機WAV
