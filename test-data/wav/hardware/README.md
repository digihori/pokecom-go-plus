# 実機往復テスト：正常データ6ケース

**実施結果：2026-10-31、6ケースすべてユーザー確認OK。** [結果記録](RESULTS_2026-10-31.md)を参照。

2026-10-31。対象はPC-1245（OLD）、PC-1401（S1）、PC-1360K（S2）。
**各機種でBASIC 1本とBinary 1本を往復し、表示・内容・チェックサム境界をまとめて確認する。**
意図的なchecksum破損、保護付きtype、文字種の網羅は今回の実機セットに含めない。

## 実施する6ケース

数値は転送bodyのlogical byte数。BASICテキストの文字数やWAV全体の大きさではない。

| 順番 | 機種 | ケース | 長さ | 確認内容 |
|---|---|---|---|---|
| 1 | PC-1245 | `old-basic` | 169 | RUNで`OLD OK`。8 byte SUM、80 byteリセットを2回越え、末尾端数も確認 |
| 2 | PC-1245 | `old-binary` | 168 | metadata・配置、80 byteリセット2回、最後の完全8 byteのSUM |
| 3 | PC-1401 | `s1-basic` | 241 | RUNで`S1 OK`。非最終120 byte chunk×2と、端数1 byteを含むtail |
| 4 | PC-1401 | `s1-binary` | 240 | metadata・配置、非最終120 byteと、最終120 byteを含むtail |
| 5 | PC-1360K | `s2-basic` | 241 | RUNで`S2 OK`。S2 type 27と、S1同様のchunk／端数tail |
| 6 | PC-1360K | `s2-binary` | 240 | S2で共用するBinary 67と、最終120 byteのtail |

まず各機種のBASICを1回通すと、音量・接続と実行まで同時に確認できる。
その後Binaryへ進む。169／168、241／240の組合せで、別々の長さのWAVを何本も試す負担を減らす。
最後の完全120 byteと端数はBASIC／Binaryで分担して確認するため、各形式の全長境界を網羅する試験ではない。
残る長さ・保護付き形式・破損検出は自動テストで補い、実機で問題が出た場所だけ追加ケースを作る。

BASICはPRINT＋END＋短いREM行で構成する。RUNでは各形式の`OK`を表示する。
ENDより後ろのREMも保存され、長さ・checksum確認に使う。各行を短くしてPC-1401の長い行の制約を避ける。
Binaryは00／0F／F0／FFと非対称パターンを混ぜた照合用データで、CALL／実行しない。

## Binary配置と転送名

| 機種 | 開始 | 終了（含む） | BASIC転送名 | Binary転送名 |
|---|---|---|---|---|
| PC-1245 | `C500`（50432） | `C5A7`（50599） | W1245A | W1245B |
| PC-1401 | `4400`（17408） | `44EF`（17647） | W1401A | W1401B |
| PC-1360K | `C000`（49152） | `C0EF`（49391） | W1360A | W1360B |

開始位置はPGPの機種別サンプル配置に合わせる。既存データがある場合は保存し、指定範囲を空ける。
Binaryの保存もこの開始・終了を指定する。長さを終了アドレスとして入力しない。
キー操作と機械語の読込・保存コマンド構文は各機種のマニュアルに従う。

## 各ケースの手順

1. `generated/<ケース>/send.wav`を等速再生し、実機で読込む。接続・再生音量を記録する。
   leaderや前後を編集しない。
2. BASICはRUNで`OLD OK`／`S1 OK`／`S2 OK`を確認し、LISTでREM行も保持されていることを見る。
   Binaryは実行せず、読込成功と指定範囲を確認する。
3. 実機から同じデータをCSAVE録音する。転送名を上表に合わせ、Binaryは開始・終了も合わせる。
4. まず48000Hz・16-bit PCMで録音する。mono、または同相信号のstereoを使い、leader全体と末尾の余裕を含める。
   原本を`recordings/old-basic-48k.wav`等へ保存する。
5. StudioのWAV ToolでDecodeし、信号／checksum診断、プレビュー、プログラム保存可否を確認する。
6. 下記ツールで**全body byteとBinary配置**を比較し、`results.csv`へ記入する。

44.1kHzも実録音で確認する場合は、各系統のBASICだけ追加録音する。6ケース全部を録り直す必要はない。
速度ずれの負荷試験や音量条件の総当たりは行わず、まず通常条件で内容が往復することを優先する。
実機の結果はすべて未実施から始め、表示だけで全byte一致としない。

## 同梱データ

- `cases.csv`：6ケースのtype、長さ、配置、WAVの場所。
- `results.csv`：機種・ROM版・録音Hz・接続・音量・往復結果の記入用。初期値は未実施。
- 各ケースの`send.wav`：正常な8000Hz・8-bit mono WAV。
- `source.bas`／`source.hex`：Studioで再Encodeする入力。
- `expected-return.bas`／`expected-return.dmp`：Decode後の比較用テキスト。
- `expected-body.bin`：全body byteの照合基準。
- `expected-basic-image.bin`：BASICを対象機種でコンパイルしたFF外枠付きimage。
- `expected-transfer.raw`：type、name、metadata、SUM込みraw。問題発生時の調査用。
- `case.json`：転送名、長さ、type、配置、SHA-256。

元データは独自作成。ROM・ROM由来データ・実機録音を含まない。
生成器はStudioのEncode／Decodeで全件を自己往復し、BASICのbodyを対象機種のコンパイル結果とも照合する。
準備データの自動検証に加え、今回の6ケースはユーザーによる実機往復の受入れを完了した。

## 再生成・録音照合

JDK 21、リポジトリ直下で実行する。ROM不要。

```sh
./gradlew :desktopApp:generateWavHardwareFixtures
```

出力先は`generated/`。既存記録を上書きしないため、空のフォルダに限る。
再生成は`-PwavFixtureOutput=test-data/wav/hardware/generated/recheck`等で新しい出力先を指定する。

```sh
./gradlew :desktopApp:verifyWavHardwareRecording \
  -PwavCase=test-data/wav/hardware/generated/old-basic \
  -PwavRecording=test-data/wav/hardware/recordings/old-basic-48k.wav
```

ケースと録音のパスを置き換える。JSONでbody／配置／name／typeの一致、信号・SUM診断を表示する。
body不一致時は最初の異なるoffsetも表示し、Decode失敗・内容不一致は非ゼロ終了する。

`contentPass=true`が内容の合格条件。name／typeは実機の保存操作で変わる可能性があるため、別の`metadataPass`へ表示する。
name／typeだけ違う場合は成功終了するが、保存操作と共に差を記録する。
WAV hashや全PCM sampleの一致は往復の合否条件ではない。BASICテキストの整形差もbodyで判定する。

`generated/`、`recordings/`、`results/`、ZIPはGit管理外。録音や実施記録は自動commitしない。
