# WAV入力レート・checksum点検

2026-10-31。ユーザー指定範囲はOLD／S1／S2の44100／48000Hz入力、8000Hz出力の維持、
checksum計算の重点確認。速度ずれ補正・ノイズ耐性アルゴリズムの追加は対象外。

## 入出力仕様

| 経路 | sample rate |
|---|---|
| OLD Decode | 4000〜32000Hz、44100Hz、48000Hz |
| S1／S2 Decode | 8000／16000／24000／32000／44100／48000Hz |
| 全形式Encode | 8000Hz（変更なし） |

高レート入力は8／16-bit PCM、mono／stereoの既存正規化後に信号処理する。
内部16000Hz化は6500Hz cutoff、半径32入力sampleのHann窓付きsinc filterを使用し、
位相ごとの係数を再利用する。高レート経路では差分判定をcarrier 2周期分で平均化し、
矩形波のsample量子化による一時的な差分が誤同期にならないようにする。
既存レート経路にはこの平均化を追加しない。データ型・転送checksumには触れない。
元WAV／PCMを保持し、内部解析のbyte対応範囲・同期・診断を元sample座標へ戻す。
範囲は開始切捨て・終了切上げで表し、再標本化により位置は近似となる。
フィルタの近傍sampleも判断に寄与するため、表示範囲を独立した厳密frame境界と解釈しない。

## checksumの計算対象

`H(x)=上位nibble`、`L(x)=下位nibble`とする。各byteについて最初のnibble加算でのみ
255超過時に1を加えてmod 256へ戻し、次のnibble加算はmod 256のみ。格納時はnibble swapする。

| 区間 | 入力byte・順序 | 蓄積と区切り |
|---|---|---|
| OLD name | raw name block 8 bytes、H→L | typeを含めず独立計算 |
| OLD Binary metadata | raw metadata 8 bytes、H→L | name／bodyから独立 |
| OLD body | logical H→L＝swap済みraw L→H | 8 data bytesごとに累積値を挿入、80 data bytesごとに0へ戻す。最後の端数にSUMなし |
| S1／S2 name | raw name block 8 bytes、L→H | typeを含めず独立計算 |
| 共通Binary metadata | raw metadata 8 bytes、L→H | name／bodyから独立 |
| S1／S2 bodyの非最終chunk | swap済みraw 120 bytes、L→H | chunkごとに0から計算 |
| S1／S2 tail | 最後の1〜120 raw bytes＋最初のFF、L→H | 2個目のFF、checksum自身、物理BASIC trailerは含めない |

BASIC tailをlogical H→Lで計算する参照実装と、PGPのraw L→Hは等価。
最後がちょうど120 bytesでも独立chunk SUMを付けずtail対象へ含める。
S1の07／17、S2の27／37は同じ算術・区切り。Binary 67はS1／S2で共用する。

## 点検方法と結果

- OLDの全256累積状態×全256入力byteを、独立した商・剰余による演算と照合する。
- OLDは1／7／8／9／79／80／81／88／159／160／161／240 bytesを確認する。
  00／0F／F0／FF反復と非対称パターンを用い、name／metadata／累積bodyを独立照合する。
- S1／S2 Binaryは1／119／120／121／239／240／241／359／360／361／65536 bytesを確認する。
- BASIC 07／17／27／37は4／119／120／121／239／240／241／258 bytesを確認する。
- checksum格納byteの全8bitをそれぞれ反転し、エラーコード、位置、expected／actualを検証する。
  OLD診断の値はswapを戻した累積値、S1／S2診断の値は格納raw値であり、既存の意味を維持する。
- 既存の独立固定vector 28件と製品codecの照合を回帰確認する。
  `generate.py --verify`は28件成功。pcwav 0.5旧Binary tailは比較・拒否用であり、現行期待値と混同しない。
- 同梱pcwav 0.5.2のchecksum／tail処理を読み取り比較し、独立ツールの合成往復テストも実行する。
  ソースの古い「BASIC tailはcarryなし」というコメントより、実行されるcarry処理と現行仕様を優先する。

現在のchecksum実装に計算・挿入位置の不一致は見つからず、製品のchecksumロジックは変更していない。
これは独立期待値・参照実装との一致であり、全機種の実機互換を新たに保証するものではない。

## 信号検証で見つかった修正

- S1／S2：正弦波のゼロ交差1 sampleを無音扱いしてleader累積をリセットしていた。
  1周期内の有意sampleでcarrierの存在を確認する。
- OLD：carrierが無音へ移るedgeを次のnibble開始と誤認していた。
  start unitの中央に信号があることを確認する。
- S1／S2：録音末尾の短いフィルタ過渡成分を新しい破損frameとして扱わない。
  carrierが続く切詰めframeは引き続き診断する。

高レートの独立2／4kHz正弦波、位相差、全振幅矩形波、00〜FF、無音・切詰め・位置座標をテストする。
OLDの差分overlayは元PCMの観測として維持し、高レートでは内部の判断値と区別する案内を表示する。
StudioではOLD／S1／S2のBASIC通常／保護付き、Binary、自動判別、16-bit mono／stereo、
checksum不正時にPCM／rawを残してプログラム保存を拒否する経路を検証する。
実機録音・実機往復とGUI手動操作は今回未実施。

## 自動検証と実機用セット

JDK 21で`build :core:desktopTest`成功。Core desktop 509件、Studio 473件（既存skip 1）で失敗なし。
`generate.py --verify`は固定28件成功、同梱pcwavの合成往復2テストも成功。
`git diff --check`と更新文書のローカルリンク確認も成功。

実機往復は[PC-1245／PC-1401／PC-1360K用正常データ](../../test-data/wav/hardware/README.md)で実施する。
ユーザー指定により、実機セットは正常データ6ケース（各機種のBASIC／Binary）に集約し、SUMを意図的に壊したWAVは含めない。

実機用セットは正常6件を生成し、全件のStudio自己往復・機種別BASIC body一致、
WAVの8000Hz／8-bit mono、body／raw／WAVのSHA-256、CSV列数を確認した。
録音照合CLIも生成済みOLD BASICのWAVで`contentPass=true`／`metadataPass=true`を確認した。
生成・照合タスク追加後もJDK 21で`build :core:desktopTest`成功。
実機での実施結果はまだなく、結果表は全件「未実施」のままにしている。

## 2026-10-31：実録音の短縮frame・末尾処理の修正

ユーザー提供のS1 44100Hz録音では6 raw bytesで同期を失い、S2も44100／48000Hzで失敗した。
固定16-unit／byteで進むPGP Decoderが、実機のデータ部13-unit間隔へ追従できていなかった。
OLDはnibbleごとに同期を取り直すため、同条件の録音でも読めていた。

S1／S2共通Decoderで以下を変更した。

- byte開始の近傍（1/4 unit）でW1→W0を探索し、微小なclock誤差をbyte間で累積させない。
- high nibble後の停止が2〜5 unitのframeを受理する。start／stopとcarrierの検査を残す。
- 末尾SUM直後の停止長の違い、carrierの減衰、DC settlingをデータと誤認しない。
- 実際の破損・切詰め・後続leaderへの再同期の診断は回帰テストで維持する。
- Encodeのbyte列、16-unit出力、checksum算術・検査を変更しない。

提供録音の全body照合結果：

| 系統・機種 | 録音Hz | body長 | 全byte／checksum／プログラム保存 |
|---|---|---|---|
| OLD／PC-1245 | 44100／48000 | 169 | 一致・成功 |
| S1／PC-1401 | 44100／48000 | 241 | 一致・成功 |
| S2／PC-1360K | 16000／44100／48000 | 241 | 一致・成功 |

いずれも録音からのローカルDecode照合。新たな実機キー操作や再録音をこちらで行ったという意味ではない。
提供録音は転送名が空で、fixture指定名とのmetadata差として記録し、body不一致とは扱わない。
私有の録音ファイルはコピー・commitしない。

`WavHardwareRecordingTest`は`PGP_WAV_RECORDING_DIR`指定時だけローカル録音を検証する。
通常CIではskipする。公開の合成テストは16→13-unit切替、末尾15-unit、DC減衰、0.2%のclock差を
16000／44100／48000Hzで再現し、同じ問題を録音なしで回帰確認する。

```sh
PGP_WAV_RECORDING_DIR=/path/to/recordings ./gradlew :desktopApp:test --tests '*WavHardwareRecordingTest'
```

修正後、録音指定付きでJDK 21の`build :core:desktopTest`成功。
Core desktop 510件、Studio 474件（既存skip 1）で失敗なし。
同期喪失・切詰め・後続leader復帰を含む既存テストと、新しい短縮frame合成テストが成功した。
提供録音7本も全body一致・信号／SUM診断なし・プログラム保存可能を確認した。

`:desktopApp:createDistributable`も成功し、ローカルの修正版Studioアプリを生成した。GUIの再起動・手動操作はユーザー側で実施する。

## 2026-10-31：PC-1360K CSAVE Mのmetadata

共通Binary 67のDecoderが、metadata先頭4 bytesをEncoder既定の`00 00 00 00`に固定していた。
提供された48000Hz録音はname／metadataのSUMを通過した後、このゼロ必須条件で拒否されていた。
実録音の先頭4 bytesは`EF C0 00 00`。意味は推測せずopaqueな値として保持する。
Decoderの制約を撤廃し、`copyReservedMetadata()`で不変コピーを返し、Studio summaryで原値を表示する。
Encoderは0の既定出力を維持する。S2専用Binary codecを作る変更ではない。

修正後、263 raw bytes、240 logical bytes、C000..C0EFの全body／開始・終了一致、
全checksum正常、DMP／HEX保存可能を確認した。転送名は空のためfixture名との差として別扱い。
録音は私有データのまま保持し、repositoryへコピーしない。

合成回帰では独立固定header SUM（00 00 00 00→71、00 00 00 60→D1、12 34 56 78→B3）を用い、
非ゼロmetadataの受理・copy所有・アドレス／body保持と、metadata破損時のSUM拒否を検証する。
検証bodyは9000から120 zero bytes、末尾SUMは固定E1。実機録音照合は環境変数指定時だけ実施する。

今後の許容判断も「Encodeは既知の標準形、Decodeは確認済み実機差を保持して受理」を基準にする。
未知値を既定値へ強制せず、SUM・宣言長・アドレス・終端など内容の完全性は別に検査する。

検証結果：録音指定付き`build :core:desktopTest :desktopApp:createDistributable`成功。
Core desktop 511件、Studio 475件（既存skip 1）で失敗なし。
追加のローカル照合でも、PC-1401／PC-1360KのBinary各44100／48000Hz、計4本が
240 bytes・開始／終了アドレス・全SUM一致で成功した。
PC-1401の先頭metadataは00 00 00 00、PC-1360KはEF C0 00 00だった。
既存BASIC録音7本も一致を維持した。修正版のローカルStudioアプリを更新済み。

## 2026-10-31：実機受入れ完了

ユーザーから「用意したテストケースはすべて確認してOKでした」と報告を受領。
PC-1245／PC-1401／PC-1360KのBASIC／Binary、正常6ケースの往復確認を完了とする。
[実機結果表](../../test-data/wav/hardware/RESULTS_2026-10-31.md)へ、対象・配置・受入れ結果と
自動照合の根拠、未記録条件を集約した。6ケース外の機能や全機種への保証には拡張しない。
