# OLD系 WAV Encoder / Decoder 仕様

## 1. 文書の位置付け

この文書は、Pokecom GO Studioへ最初に実装するOLD系WAV Encoder / Decoderの仕様を定める。
対象は、OLD BASICとOLD Binaryのファイル転送データ、およびそれを格納するPCM WAVである。
PGPの機種分類ではPC-1245、PC-1250、PC-1251、PC-1255がOLD系に該当する。ただし、全機種の
実機WAV互換性をこの文書だけで保証するものではない。
S1／S2の参照動作と未確定事項は[WAV_S1_S2_FORMAT.md](WAV_S1_S2_FORMAT.md)へ分離する。

仕様抽出には、同一作者による`pcwav`を読み取り専用の参照実装として使用した。

- Reference repository: `pcwav`
- Reference commit: `14e1ef53596f0d8791b6d14965061f63846d11a7`
- Primary reference files:
  - `src/PCWAV/Format/Old.pm`
  - `src/PCWAV/Common.pm`
  - `src/PCWAV/WavWriter.pm`
  - `src/PCWAV/WavReader.pm`
  - `src/PCWAV/PcmNormalize.pm`
  - `src/PCWAV/RawDecode.pm`
  - `src/PCWAV/Basic/OldEncode.pm`
  - `src/PCWAV/Basic/OldDecode.pm`
  - `src/PCWAV/Binary/OldEncode.pm`
  - `src/PCWAV/Binary/OldDecode.pm`

特にbody checksumは、参照履歴のcommit `afb2bb9`（checksum計算ロジックの修正）後の実装を採用する。
それ以前の非累積処理やraw nibble順での計算は採用しない。

この段階ではS1、S2、実時間録音、マイク／スピーカー制御を対象外とする。`pcwav`のコードを
PGPへ直接取り込まず、以下に確定した振る舞いをKotlinで再実装する。

## 2. レイヤーと用語

WAV変換はエミュレーターSessionとは独立したツール機能とする。

```text
BASIC text / Addressed machine-code image
            ↕
    OLD program codec
            ↕ logical bytes
    OLD transfer codec
            ↕ raw transfer bytes
      signal codec
            ↕ PCM samples
      WAV container
```

- **logical byte**: BASIC中間コードまたはバイナリ本体が保持する本来のbyte値
- **raw byte**: OLD転送stream上に現れるbyte値
- **nibble swap**: `AB`を`BA`へ変換する操作
- **data chunk**: bodyのlogical byte 8個に対応する単位
- **checksum window**: checksumを累積する最大80 data bytesの範囲

ファイル選択、画面表示、進捗表示はStudio層の責務とし、形式変換には含めない。

## 3. OLD raw payload

### 3.1 種別

| 種別 | raw type | PGP初期対応 |
|---|---:|---|
| BASIC | `02` | Encode / Decode |
| password付きBASIC | `12` | Decode。Encode APIは値を表現できるようにするが、初期UIでは生成しない |
| Binary | `62` | Encode / Decode |

type byteは単独で配置し、ニブルスワップもchecksum付加も行わない。

### 3.2 ファイル名block

ファイル名は次の順で正規化する。

1. 大文字へ変換する。
2. 最後の拡張子を除く。
3. `A..Z`と`0..9`以外を除く。
4. 先頭7文字へ切り詰める。
5. 文字順を反転して文字コード化する。
6. 7 bytesになるまで先頭を`00`で埋める。

文字コードは数字`0..9`が`40..49`、英字`A..Z`が`51..6A`である。7-byte filenameの後に
terminator `F5`を置き、合計8 bytesをname blockとする。このblock自体はニブルスワップしない。

```text
filename[7] | F5 | nibswap(checksum(name block))
```

例として`PC1245.BAS`は、raw上で次のようになる。

```text
00 45 44 42 41 53 60 F5 E3
```

### 3.3 BASIC payload

```text
02または12
name block[8]
name checksum[1]
body data/checksum stream
```

BASIC logical bodyは行を連結し、最後にlogical terminator `F0`を1 byte置く。各行の詳細な
中間コードは[BASIC_TEXT_FORMAT.md](BASIC_TEXT_FORMAT.md)に従う。転送層はBASIC tokenを解釈せず、
logical body全体を3.5節の規則で変換する。

### 3.4 Binary payload

```text
62
name block[8]
name checksum[1]
metadata[8]
metadata checksum[1]
body data/checksum stream
```

metadataはニブルスワップせず、次の8 bytesをrawへそのまま置く。

| offset | length | 内容 |
|---:|---:|---|
| 0 | 4 | reserved: `00 00 00 60` |
| 4 | 2 | start address、big-endian |
| 6 | 2 | end offset、big-endian |

`end offset = body length - 1`、`end address = start address + end offset`とする。metadata checksumは、
metadata 8 bytesに3.6節のOLD checksumを適用してからニブルスワップした1 byteである。

### 3.5 bodyのニブルスワップとchecksum配置

bodyでは、各logical byteをニブルスワップしてraw data byteとする。raw data 8 bytesごとに
checksumを1 byte追加する。最後が1..7 data bytesの場合、その端数にはchecksumを付けない。

```text
raw-data[8] | checksum | raw-data[8] | checksum | ... | raw-data[0..7]
```

checksumは8-byte chunkごとに初期化しない。同じ80-data-byte window内では累積し、10 chunks、
すなわち80 data bytesの先頭で0へ戻す。この規則は参照実装ではBASICとBinaryの両方へ適用される。

```text
chunk 0 checksum = sum(data  0.. 7)
chunk 1 checksum = sum(data  0..15)
...
chunk 9 checksum = sum(data  0..79)
chunk 10 checksum = sum(data 80..87)  // reset
```

### 3.6 OLD checksum

checksumの内部状態はlogicalなニブル順で更新する。各logical byteについて上位ニブル、下位ニブルの順に
加算する。上位ニブル加算直後だけend-around carryを適用し、下位ニブル加算は8 bitへ丸める。

```text
sum = 0
for byte in logicalBytes:
    sum += highNibble(byte)
    if sum > FF:
        sum = (sum + 1) & FF
    sum = (sum + lowNibble(byte)) & FF
```

body実装がraw byteを処理する場合、logical high nibbleはraw low nibble、logical low nibbleはraw high
nibbleに対応する。したがって「rawをそのまま通常の上位→下位順で加算」してはいけない。

保存するchecksum byteは、得られたlogical checksumをさらにニブルスワップした値である。name、
metadata、bodyのいずれも、rawに置くchecksum byteの見え方はこの規則に従う。

### 3.7 確定テストベクトル

参照commitから抽出した固定vectorをPGPのunit testへ移す。

logical body `00 01 ... 13`、filename `PC1245.BAS`のBASIC payload:

```text
02 00 45 44 42 41 53 60 F5 E3
00 10 20 30 40 50 60 70 C1
80 90 A0 B0 C0 D0 E0 F0 87
01 11 21 31
```

同じbody、filename `TEST.BIN`、start address `C000`、end offset `0013`のBinary payload:

```text
62 00 00 00 64 63 55 64 F5 B3
00 00 00 60 C0 00 00 13 61
00 10 20 30 40 50 60 70 C1
80 90 A0 B0 C0 D0 E0 F0 87
01 11 21 31
```

少なくとも、body長`0`、`1`、`7`、`8`、`9`、`79`、`80`、`81`、`88` bytes、およびchecksum
carryが発生する入力を境界testに含める。Binary encoderは空bodyを拒否する。

PGP testでは、参照実装から抽出したBASIC／Binary payloadを固定vectorとしてEncode／Decodeの両方向で確認する。
さらに上記境界のpayload単体testに加え、`1`〜`88` bytesの有効なBASIC／Binary境界値をWAVまでEncodeし、
統合APIでDecodeする。各境界ではraw byte数、PCM sample数、WAV byte数も仕様の計算式と一致させる。

## 4. raw byteからPCMへの変換

### 4.1 信号単位

Encoderが出力するWAVは8000 Hz、8-bit unsigned、mono PCMである。1信号単位は16 samplesで、
次の矩形波を使用する。

```text
W1 = FF 00 を8回       // 16 samples、2 ms、4 kHz矩形波を8周期
W0 = FF FF 00 00 を4回 // 16 samples、2 ms、2 kHz矩形波を4周期
```

byte framing上はどちらも2 msの1信号単位として扱う。

### 4.2 リーダー

payloadの前に`W1`を`0x400`回置く。8000 Hzでは16,384 samples、2.048秒である。
参照Encoderはpayload後へ明示的なtrailerや無音を追加しない。

### 4.3 1 raw byteのframing

raw byteのbitは各nibble内でLSB firstに送る。

```text
W0
bit 0, bit 1, bit 2, bit 3
W1 W1 W1 W1
W0
bit 4, bit 5, bit 6, bit 7
W1 W1 W1 W1 W1
```

bit値1を`W1`、0を`W0`へ変換する。1 byteは19信号単位、304 samples、38 msである。
従ってEncoderのPCM sample数は次式で固定できる。

```text
16384 + rawPayloadSize * 304
```

## 5. WAV containerとDecode入力

### 5.1 Encoder出力

Encoderは標準的なRIFF/WAVEを出力する。

- format: PCM (`audioFormat = 1`)
- channels: 1
- sample rate: 8000 Hz
- bits per sample: 8
- block align: 1
- byte rate: 8000
- `fmt ` chunk length: 16
- `data` chunkに4節のPCMを格納する

### 5.2 Decoder受理範囲

初期Decoderは参照実装と同じ範囲を受理する。

- RIFF/WAVE、PCMのみ
- monoまたはstereo
- 8-bit unsignedまたは16-bit signed little-endian
- sample rateは32,000 Hz以下
- 未知chunkは読み飛ばし、奇数長chunkのpad byteも読み飛ばす
- stereoは左右sampleの平均でmono化する
- 8-bit入力は`(unsigned - 128) * 256`でsigned 16-bit相当へ正規化する

互換性のため、参照Encoderと同様に末尾の`data` chunkが奇数長の場合はpad byteのないWAVも受理する。
途中にある奇数長chunkでは、後続chunkとの境界を保つpad byteを必要とする。

32,000 Hz以下という上限だけでは、極端に低いsample rateでの復号可能性を保証しない。対応sample rateの
下限と実用範囲は実装testで確定する。

### 5.3 信号Decode

参照Decoderは、sample rateに応じた差分tap、固定threshold、2 ms単位のsamplingでエッジからlevelを作り、
nibble間の同期patternを検出してraw byteを復元する。PGP実装ではこの処理をWAV parserから分離する。

初期実装の成功条件は次の順とする。

1. PGP Encoder出力をPGP Decoderが復号できる。
2. `pcwav` Encoder出力とのraw payloadおよびPCM一致を確認できる。
3. 提供可能な実機録音fixtureを追加し、振幅差、先頭無音、軽微な速度差に対する許容範囲を固定する。

参照CLIはOLD Decode時に既定で先頭1000 msを除外してから信号Decodeする。一方、libraryのRawDecode自体は
先頭除外を要求しない。PGPでは固定の1000 ms除外をformat仕様にせず、leader探索として扱う。

PGPの信号Decoderは4,000〜32,000 Hzの正規化済みmono signed 16-bit相当sample列を受け取る。先頭の無音は
最初の有意な変化まで読み飛ばし、leaderを確認してから完全なbyteだけを確定する。復号結果にはraw byteごとの
入力sample範囲とlow／high nibbleの同期位置を保持し、末尾の不完全なframeは完成済みbyteを失わずwarningにする。
これは将来のRaw Decode／WAV Analyzerで波形とraw byteを対応付けるための情報であり、UI状態は含まない。

## 6. Decode時の検証と終端

Decoderは最低限、次を検証して失敗理由を返す。

- payload最小長
- 既知type (`02`、`12`、`62`)
- filename terminator `F5`
- filename checksum
- Binary metadata長とchecksum
- bodyの各checksum
- BASIC logical terminator `F0`
- Binaryの`endOffset + 1`と復号body長の整合

参照実装のgeneric unwrapは、入力raw streamの残りをすべてbodyとして解釈する。またBinary Decodeは
`endOffset + 1`を報告するが、復号bodyをその長さへ切り詰めない。PGPではtype固有の終端を先に確定し、
後続noiseや別streamをbodyへ混入させない。余剰raw bytesを黙って捨てず、消費byte数とwarningを返す。

## 7. PGP実装境界

初期実装では、単一の巨大な`WavCodec`を作らず、少なくとも次の責務を分ける。

| 責務 | 想定する配置 | Desktop／UI依存 |
|---|---|---|
| OLD BASIC/Binary logical image | Coreまたは既存Program model | なし |
| OLD payload wrap/unwrap | commonなTooling logic | なし |
| checksum、nibble swap | commonなTooling logic | なし |
| signal Encode/Decode | commonなTooling logic | なし |
| RIFF/WAVE read/write | commonなTooling logic | なし |
| ファイルpicker、保存確認、メニュー、進捗 | Studio Desktop | あり |
| マイク／スピーカー実時間I/O | 将来のplatform adapter | あり |

変換APIはbyte列、sample列、診断結果を受け渡し、パス、Compose state、エミュレーターSessionを引数にしない。
`OldWavCodec`はこれらの独立したcodecを順番に呼ぶ薄いfacadeとし、BASIC／パスワード付きBASIC／Binaryの
Encodeと、WAVから形式判定済みOLD payloadまでのDecodeを提供する。結果には各中間表現を残し、成功前に
発生したwarningと失敗段階のerrorをまとめて返す。これによりStudioは同じAPIから最終結果を利用しつつ、
必要に応じてraw byte、PCM mapping、同期位置を検査できる。

Studioではメイン画面の「ツール」からWAV Encoder / Decoderを開き、Projectを開かなくても利用できる形を
初期案とする。AssemblerやCharacter Editorと同じToolsカテゴリに置くが、実装moduleを共有することは
意味しない。

初期Studio UIは`Tools > OLD WAV Encoder / Decoder`から独立windowを開く。OLD BASIC sourceはPC-1245系の
tokenizerでlogical byte列へ変換し、通常BASIC (`02`) またはpassword付きBASIC (`12`) としてWAVへ保存する。
Binary EncodeはPGP Memory Dump (`.dmp`)からアドレス付きmemory imageを読み、単一の連続領域を
Binary (`62`) として保存する。OLD Binaryは開始addressと連続bodyを1組しか保持できないため、複数領域またはgapを
含む入力は暗黙に結合・補完せずエラーにする。DecodeしたBinaryは`.dmp`を標準出力とし、Intel HEX実装後は交換用
出力として選択可能にする。Raw Binary (`.bin`)と開始address欄をWAV Tool固有の通常UIには置かない。
filename、検出したaddress範囲、診断結果を表示し、Project、ROM、Emulator Sessionを要求しない。

初期UI確認で判明した、file dialogを閉じるとメインwindowへfocusが移る問題は、dialogのowner／focus復帰先を
WAV Tool windowにして解消した。既存BASIC sourceのtokenize結果をそのままOLD転送bodyへ渡してEncodeを拒否する
問題は、次の双方向adapterをStudio境界に追加して解消した。

後者は単純な`F0`追記ではない。PC-1245 Emulator／Project用のtokenized programは、program全体を
`FF ... FF`で囲み、各行を`00`で終える。一方、OLD WAV転送bodyは先頭`FF`を持たず、各行の`00`に続いて
program全体を`F0`で終える。従ってStudio adapterで次の双方向変換を行い、各入力形式をそれぞれの既存
Tokenizer／Detokenizerで検証してから変換する。

```text
Emulator / Project: FF [line ... 00] ... FF
OLD transfer body:    [line ... 00] ... F0
```

Encode時は先頭`FF`を除き、末尾`FF`を`F0`へ置換する。Decode時は末尾`F0`を除き、全体を`FF ... FF`で囲んで
から既存Detokenizerへ渡す。既に転送bodyであるbyte列へ重ねて変換したり、`F0`を二重付与したりしない。

### 7.1 Raw Decode解析ツール

通常のWAV Decoderとは別に、WAVから復元したraw byte列をpayloadとして解釈せず確認できるRaw Decodeを
Studioの解析ツールとして提供する。用途は、形式が不明なWAV、壊れたWAV、未対応機種のWAVについて、
信号層まで復号できているかを調べることである。

Raw DecodeはWAV reader、PCM normalize、signal decoderまでを使用し、OLD BASIC／Binaryのtype、filename、
checksumを前提にしない。結果として少なくともraw byte列のhex表示と保存、WAV入力形式、検出byte数、
同期位置を返す。将来signal decoderがconfidenceや同期喪失位置を保持できるようになった場合は、それらも
解析結果へ追加する。

これは通常Decodeの失敗を迂回して「正常なプログラム」として保存する機能ではない。payload validationを
通過していないことをUI上で明示し、ProgramまたはEmulator Sessionへ直接ロードしない。

### 7.2 将来のWAV Analyzer

Raw Decodeの上位ツールとして、raw streamと形式別の解釈結果を並べて調査できるWAV Analyzerを将来追加する。
基本画面は、上段にPCM波形、下段左にoffset付きraw byte列、下段右にOLD／S1／S2から選択した形式の構造、
変換後byte列、文字表現を置く。各表示は同じ入力範囲を表すsource mappingで結び、一方のsample、signal unit、
byte、構造、文字を選ぶと対応範囲を他方でも強調する。

```text
PCM waveform / derived signal level
time, samples, edges, ON/OFF, W0/W1, leader, sync, byte boundaries
                              ⇅
Raw stream                         Candidate interpretation
offset  hex bytes                  OLD / S1 / S2
0000    02 00 45 ...       <───>   type, filename, checksum
000A    00 10 20 ...       <───>   logical bytes, BASIC text
```

波形は音声振幅だけでなく、Decoderがthreshold処理したON／OFF levelを判別できることを必須とする。さらに
W0／W1判定、leader、nibble／byte同期点を別overlayとして表示し、単なる音声viewerではなくDecode判断の根拠を
追跡できるようにする。zoom out時はpixel区間内のmin／max envelopeを描き、短いpulseを平均化で消さない。
zoom in時は個々のsample、edge、sample index／時刻を確認できるようにする。stereo等の入力はchannel原波形と
mono正規化後波形を切り替え、振幅、DC offset、thresholdも表示する。

Analyzerは、type、header、filename、address、body、terminator、checksumを領域として表示し、checksum不一致、
同期喪失、未定義byte、途中終端、余剰dataをraw offset付きdiagnosticとして示す。形式は自動で一つに断定せず、
複数候補の解析結果と根拠を比較できるようにする。raw byte列、変換後byte列、文字変換結果、diagnosticは
それぞれ個別にexport可能とする。

payload解釈が失敗しても、それ以前に成功したWAV metadata、PCM波形、ON／OFF level、W0／W1、同期点、raw byteを
破棄しない。summaryにはsample rate、channel、bit depth、duration、peak、DC offset、検出edge数、検出byte数を
表示し、diagnostic選択時には対応する波形区間とraw／logical範囲へ移動する。

このため、各Transfer Decoderは最終的なProgramだけでなく、消費したraw範囲、生成したlogical範囲、意味、
検証結果を保持する中間解析modelを返せる設計にする。WAV Analyzer UIとsource mapping modelはTooling／Studio
側へ置き、エミュレーターCoreへ解析画面固有の状態を持ち込まない。

## 8. 参照実装から意図的に持ち込まないもの

- Perlのfile I/OとCLI引数処理
- 固定1000 ms skipというUI既定値
- privateな非累積checksum helper
- raw stream終端を入力末尾へ依存させる処理
- Decode失敗を文字列`die`だけで返す設計
- BASIC tokenizerとWAV転送層の密結合

PGPではエラー位置、期待値、実測値、処理段階を構造化したdiagnosticとして返す。

## 9. 未確定事項と互換性確認

次は`pcwav`だけでは実機互換性を確定できないため、初期UI確認後の検証項目とする。

- password付きBASIC (`12`) の実機での生成・読込み条件
- 実機録音で許容すべきsample rate、振幅、DC offset、速度変動、先頭／末尾noise
- 複数streamを1つのWAVへ含める場合の探索と分離
- filenameが空、7文字超、非英数字を含む場合のUI表示とwarning
- Binary metadataのreserved `00 00 00 60`を固定値として拒否するか、warning付きで保持するか
- BASIC終端後またはBinary宣言長後に存在するraw byteの扱い

この調査が必要になった場合だけ、実機fixtureまたは追加資料を参照する。Pokecom GOおよび
PC-1251 Emulatorを先回りして参照しない。
