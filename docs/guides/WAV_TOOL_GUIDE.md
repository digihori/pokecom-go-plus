# WAV Tool 操作ガイド

更新日：2026-10-21。Studio専用のファイル変換・解析機能。

## 対応形式

| 選択形式 | BASIC | Binary | BASIC対象機種 |
| --- | --- | --- | --- |
| OLD | 02／12 | 62 | PC-1245、PC-125x系 |
| S1 | 07／17 | 67 | PC-1260／1261／1350／1401／1402／1450／1460 |
| S2 | 27／37 | S1と同じ67 | PC-1360／1360K／1470U |

BASIC入力・保存はUTF-8の`.bas`。Binary入力は単一連続領域の`.dmp`またはIntel HEX（`.hex`／`.ihx`）、
保存は`.dmp`／Intel HEX。gapの埋め込みや複数の独立領域の結合はしない。
Intel HEXは00／01とbaseが0の02／04に対応し、それ以外は診断する。
WAV変換は個別機種の制約をかけず、S1はカナ対応の共通語彙、S2はCP932日本語を含む共通語彙を使用する。ファイル変換対応と実機の表示能力は別に確認する。

## WAVを作成する

1. `Tools` → `WAV Tool` → `WAVを作成…`で保存済みファイルを選ぶ。Projectツリーからの起動も可能。
2. Projectツリー起動ではプロジェクトの機種と拡張子から初期選択する（例：PC-1251の.dmpはOLD・マシン語）。
   対象機種の選択UIは設けず、プロジェクト情報は初期形式の選択にだけ使用する。形式は利用者が後から変更できる。
   S2機種のBinaryは共通方式のS1・マシン語を初期選択する。
3. エンコード形式を明示選択する。OLD／S1は「BASICパスワードなし／BASICパスワードあり／マシン語」、
   S2は「BASICパスワードなし／BASICパスワードあり」。拡張子で形式を自動決定しない。
   BASICは`.bas`、マシン語は`.dmp`／`.hex`／`.ihx`が必要。不一致の場合は入力の変更を案内する。
4. 転送名を入力する。S1／S2 BASICの転送名は先頭7文字を使い、短縮した場合はレポートへ記録する（転送名の文字は印字可能ASCII）。保護付きはOLD 12／S1 17／S2 37を保持する。
   パスワード文字列の新規設定とは別。type番号のcheckboxや冒頭の形式説明・互換性注記は表示しない。
5. `変換`を実行し、内容・raw・波形・診断を確認して`WAVを保存…`を押す。

Encodeは8000Hz／8-bit mono。生成時のプログラム内容はpreviewであり、Decode用のプログラム保存は出さない。
設定を変更した場合は再変換する。未保存のWAV／プログラムを置換・終了する前に確認画面が出る。

## WAVをDecodeする

1. `WAVを開く…`から入力を選ぶ。Decodeには形式・機種指定は不要。
2. `解析／再解析`でOLDとS1/S2共通信号を解析し、ヘッダーとchecksum・長さ・終端から形式を確定する。
3. 検出形式を結果に表示する。保護付きBASICも識別する。Binary 67はS1/S2共通形式として表示し、機種推定をしない。
4. 信号・payload・textが検証できれば`.bas`／`.dmp`／Intel HEXとして保存できる。
   S1のカナなどを個別実機の制約で拒否しない。
5. 破損などで形式を確定できない場合や複数候補が検証を通過する場合は、断定せずプログラム保存を出さない。
   候補ごとの診断・raw・PCMを内部結果に保持し、画面には有力候補、レポートには各候補の診断を残す。
   生バイト列とレポートは引き続き保存可能。

`.bas`や`.dmp`には転送type・filename等の一部metadataが残らない。元WAV／raw／reportも必要に応じて保存する。
S1／S2 BASICは共通の形式仕様で再コンパイルし、元のbyteと一致する場合だけプログラム保存を許す。
OLDも含め、信号破損警告が残る場合は検証済みpreviewがあってもプログラム保存を出さない。

## 波形と診断

- rawは256-byte単位。行を選ぶと対応する波形へ移動する。波形側のbyte選択でrawへ戻れる。
- LOW／HIGH同期marker、cursor、zoom、横移動、範囲、振幅ガイドを使用できる。
- OLDの差分しきい値表示はOLD専用。44100／48000Hzでは元PCMの差分であり、再標本化・平滑化した復号器内部の判断値ではない。S1／S2では波形と16-unit同期位置を表示する。
- 診断のPCM／RAW範囲を選ぶと、その位置へ移動する。末尾や空の範囲も表示範囲内へ収める。
- 内容previewは16000文字まで。保存可能なプログラムには全文を含む。

| 診断・症状 | 確認すること |
| --- | --- |
| `UNSUPPORTED_FORMAT` | Encode形式、入力データ、PCM形式・sample rateを確認する |
| `SYNCHRONIZATION_LOST`／`TRUNCATED_INPUT` | leader・信号欠落・切詰めを波形で確認する。復元済みrawは保存可能 |
| `CHECKSUM_MISMATCH` | 表示位置、expected／actualを確認する。S1 Binaryの旧pcwav partial tailを黙って受け入れない |
| `INVALID_HEADER`／`MISSING_TERMINATOR` | filename、宣言長・address、行長・行末・終端を確認する |
| `TRAILING_DATA` | 複数streamや余剰byteがないか確認する。複数stream一括抽出は未対応 |
| BASICのプログラム保存が出ない | 形式に合わないtoken、text再コンパイル不一致、信号破損、診断を確認する |

入力はOLDで4000〜32000Hzと44100／48000Hz、S1／S2で8000／16000／24000／32000／44100／48000Hzに対応する。
PCM WAVは8-bit unsigned／16-bit signed、mono／stereoを受理する。
44100／48000Hzは信号Decodeのために低域フィルタ付きで内部16000Hzへ再標本化する。
波形・PCMの表示、raw byteへの対応位置、同期位置・診断は元の入力sample座標で保持する。
入力原本と転送raw byte、payloadのchecksum規則は変更しない。Encode出力は全形式8000Hzを維持する。

速度ずれ補正の追加は今回の対象外。ノイズへの許容範囲、実録音の条件は未確定で、
高sample rateの受理が実機互換性や任意の録音品質を保証するものではない。録音ファイルは原本を残して解析する。
チェックサムの計算対象・境界・今回の検証は[WAV入力・checksum検証記録](../development/WAV_INPUT_CHECKSUM_AUDIT.md)を参照。
Player、音声デバイスI/O、ROMのCSAVE/CLOAD hook、Decodeからの直接ロードは今回の対象外。

2026-10-21に一通りの動作確認を終え、今回分は一区切りとした。
[文字変換確認用BASIC](../../test-data/wav/samples/README.md)と[進捗・残課題](../development/WAV_S1_S2_PROGRESS.md)も参照。

## 実機往復の確認記録

PC-1245（OLD）／PC-1401（S1）／PC-1360K（S2）向けに、
[正常データのテストケースと生成・照合手順](../../test-data/wav/hardware/README.md)を用意した。
BASIC／Binary各1本、計6ケースで8／80／120-byte境界と末尾をまとめて確認する。実施前のケースを確認済みにしない。


2026-10-31、ユーザーから正常データ6ケースすべてOKの報告を受領した。
[実機受入れ結果](../../test-data/wav/hardware/RESULTS_2026-10-31.md)に記録した。

| 形式 | 機種 | 確認データ | PGP→実機 | 実機録音→PGP |
| --- | --- | --- | --- | --- |
| OLD BASIC 02 | PC-1245 | 169 bytes | OK | OK |
| OLD Binary 62 | PC-1245 | 168 bytes、C500..C5A7 | OK | OK |
| S1 BASIC 07 | PC-1401 | 241 bytes | OK | OK |
| 共通Binary 67 | PC-1401 | 240 bytes、4400..44EF | OK | OK |
| S2 BASIC 27 | PC-1360K | 241 bytes | OK | OK |
| 共通Binary 67 | PC-1360K | 240 bytes、C000..C0EF | OK | OK |

録音は44100／48000Hzの16-bit mono PCM。S2 BASICは16000Hzの提供録音も照合した。
全byte照合の実施範囲・metadata差・未記録の条件は結果記録を参照。
他の長さ、他機種、保護付き形式、日本語等の追加ケースは今回の受入れに含めない。

実施時は機種・ROM版、source、type、address／長さ、接続機器、録音形式・sample rate、
再現手順、結果・診断を記録する。元のBASIC imageまたはBinaryのaddressと全byteを比較する。
ROMや個人データを検証fixtureとして追加しない。

[進捗・検証記録](../development/WAV_S1_S2_PROGRESS.md)／[詳細方針](../development/WAV_OPERATIONS.md)／[S1・S2仕様](../reference/WAV_S1_S2_FORMAT.md)

### 2026-10-22：S1／S2 Binaryの120-byte境界修正

内蔵WAV変換はpcwav 0.5.2の実機確認済み規則へ追従しました。
最後の1〜120 bytesには個別SUMを付けず、末尾SUMに含めます。
旧版で作った120の倍数のBinary WAVが終端不正になる場合は、原本のDMP／HEX等から
WAVを再生成してください。誤SUMを無視する互換処理はありません。

## 実録音の短い停止区間（2026-10-31修正）

S1／S2実機がデータを13-unit間隔で送る場合もDecodeできるよう、byteごとに同期位置を取り直す。
従来の固定16-unitでは、44.1kHzや48kHzの録音で数byteの後に同期を失うことがあった。
末尾の搬送波の減衰や直流過渡成分も、未完のデータbyteと区別する。
転送checksumの検査と8000Hz Encodeは変更しない。
提供録音の照合は[検証記録](../development/WAV_INPUT_CHECKSUM_AUDIT.md)を参照。

## S1／S2共通Binaryのmetadata（2026-10-31）

S2機種のBinaryもtype 67のS1／S2共通BinaryとしてDecodeする。
実機テストの`s2-binary`は対象機種を示すケース名で、別の転送形式ではない。
PC-1360KのCSAVE Mではmetadata先頭4 bytesに0以外の値が入るため、Decodeのゼロ必須チェックを撤廃した。
原値を保持しsummaryへ表示する。全checksum・開始・長さ・終端の検査と、Encodeの既定出力は維持する。
