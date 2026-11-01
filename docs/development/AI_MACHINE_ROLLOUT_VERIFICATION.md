# AI機種展開・配布検証

実施日：2026-11-02。第6段階の最初の作業単位。

## 今回の対象

PC-1251の機種知識と、Project／Source生成・上限実行・LCD取得を提供する。
生成・実行の全機種対応や第6段階全体の完了を意味しない。

| 機能 | PC-1245 | PC-1251 |
| --- | --- | --- |
| 機種知識の検索・取得 | 対応 | 今回追加 |
| 共通assembler・CPUメタデータ | 対応 | 今回追加 |
| 同梱作例の取得 | HELLO／numeric-labels | numeric-labels |
| 解析用ファイルの選択・公開 | 対応 | 未対応 |
| Project／Source新規作成提案 | 対応 | 実装・自動検証済み |
| 上限付きRUN／CALL・キー入力 | 対応 | 自動検証＋下記の実ROM観測 |
| 停止中LCDピクセル・PNG取得 | 対応 | 120列・自動検証済み |

PC-1251の情報源・適用条件は[同梱解説](../reference/knowledge/PC1251_SCOPE.md)。
知識の対象機種はmachineIdsに記録し、検索・本文・作例取得のいずれでも照合する。
PC-1245作例やメモリ情報をPC-1251指定で取得するとNOT_FOUNDとなる。
PC-1250／PC-1255はまだ知識APIの対象に含めない。

## 自動検証

JDK 21で以下を実行し、成功した。

```sh
./gradlew --gradle-user-home .gradle-local build :core:desktopTest :desktopApp:verifyDistributionRuntime
```

デスクトップ521件、failure 0、error 0、skip 3。
知識APIはSessionへのアクセスを禁止したテストで呼出し、PC-1251の検索、
BASIC定義、カタログ既定の開始位置0xB800、LCD24文字、directDrawing=null、
共通命令、未対応操作、他機種ID・作例の拒否を確認した。
PC-1245の既存知識・作例テストも成功した。

macOSのcreateDistributableで生成したアプリ内JARを検査し、
PC1251_SCOPE.mdが元ファイルと一致すること、既存numeric-labels作例が残ることを確認。
同梱Java runtimeのjdk.unsupported／jdk.httpserverも検証した。
これは配布物の同梱確認であり、クリーン環境へのインストール・実クライアント受入れではない。
ROM画像・生成済みROMデータは追加していない。

## 次の作業単位

1. PC-1251の解析取り込みと同梱作例を整備する。生成・上限実行・LCDの実装追加は下記のとおり。
2. PC-1251専用作例を用意し、通常のROM起動から表示・入力までを検証する。
3. Copilot等の新規チャットで、公開手順だけを使う生成課題の全工程受入れを実施する。
4. 他OSの配布物と、開発環境を持たないインストール環境で接続・解析・生成を確認する。

初回の知識追加時点ではPC-1251の実ROM・実機・外部AI検証は実施していなかった。
後日の実ROM観測は下記へ追記する。
PC-1245の実績をPC-1251の動作保証へ移さない。

## 生成・実行対応の追加検証

2026-11-02：PC-1251のProject作成・Source追加・Build、承認付き上限実行と
停止中LCD取得を追加。24ツールを維持して既存ツールの対象機種を拡張した。

独自の最小テスト用ROM（SC61860命令列）で次を確認した。
著作権のあるファームウェアを追加・使用した検証ではない。

- Projectの提案ではファイルを作らず、保存先選択・承認後にPC-1251のmanifestを作る。
- Source追加後も機種が変わらず、Buildとmanifest再読込みが成功する。
- 上限実行は承認前にPCを変更せず、承認後に指定命令数で停止し、結果にpc-1251を記録する。
- LCD RAMへの書込みを120列×7行のピクセルと幅882pxのPNGへ反映する。
- PC-1245 ProjectとPC-1251 Sessionの組合せを拒否する。
- PC-1251 HARDWARE設定では0xA000とRAMミラー0xB000へのCALLを拒否し、0xB800を受け付ける。
- 既存のPC-1245承認・古いrevision拒否・入力破棄・停止結果保持のテストも成功する。

これは入力スケジューラーとサービス境界の検証であり、PC-1251実ROMが
RUN／CALLを受理することや、G01の期待表示を得られることの検証ではない。
実装直後の次の課題はPC-1251専用作例と通常のROM起動からの確認であり、
その後の実施結果を下記へ追記する。

追実装後の最終全体ビルド・Coreテスト・配布runtime検証は成功。
デスクトップ524件、failure 0、error 0、skip 3。

## PC-1251実ROMの配布アプリ検証（2026-11-02）

macOSの開発作業版createDistributable、同梱stdioブリッジを使用。
ローカル登録済みの1つのPC-1251 ROMを通常起動し、BASICの「>」をLCD PNGで確認した。
ROM本体・ハッシュ・個人用パスは同梱しない。実機やクリーンインストールの検証ではない。
MCP呼出しはリポジトリ外の空の作業フォルダーから行い、承認はStudio画面で実施した。
この実施はCodexからのstdioプローブとUI操作であり、CodexネイティブMCPや
他クライアントの独立した全工程受入れとは区別する。

### BASIC生成・入力・LCD

新規ProjectをmachineId=pc-1251で提案し、Studioで保存先選択・承認。
[同梱作例](../../test-data/basic/ai/pc1251-numeric-labels/README.md)のBASIC本文をBuildし、
本文95 byteを0xA030..0xA08E、ポインター4 byteを0xC6E1..0xC6E4へLoadした。
現在の開始ポインターを使った実測値であり、任意RAM構成の固定ロード位置ではない。
PC強制移動・手動RUN・手動の数値入力は使用していない。
各RUN／キー要求の上限は200000命令／500000サイクル。

| 入力 | 実行命令数 | サイクル数 | LCD（MCP PNG） |
| --- | ---: | ---: | --- |
| RUN | 128056 | 500002 | ![入力待ち](../images/ai-generation/pc1251/run.png) |
| 1 ENTER | 127338 | 500001 | ![ONE](../images/ai-generation/pc1251/one.png) |
| 2 ENTER | 126876 | 500010 | ![TWO](../images/ai-generation/pc1251/two.png) |
| 3 ENTER | 126978 | 500001 | ![OTHER](../images/ai-generation/pc1251/other.png) |
| 0 ENTER | 127517 | 500002 | ![通常プロンプト](../images/ai-generation/pc1251/zero.png) |
| END後のCLEAR,1,ENTER | 127933 | 500000 | ![通常の数値応答](../images/ai-generation/pc1251/after-end.png) |

各要求はSUCCEEDED／cycleLimit／inputComplete=true／projectStillCurrent=true。
停止結果のobservedRevisionとLCDのsessionRevisionを一致させて取得した。
SUCCEEDEDやcycleLimitをBASIC終了とは扱わず、0での「>」とその後の数値応答で照合した。
文字はPNGを目視した記録であり、APIがOCR結果を返したものではない。

### CALLと停止時点の観測

同じProjectへ次のルーチンをSource追加提案し、承認→Build→Loadした。
機械語は0xC500..0xC506の7 byte、BASIC・ポインター込みの総書込みは106 byte。

```asm
ORG 0xC500
LIDP 0xC100
LIA 0x5A
STD
RTN
```

CALLプリセットはCLEAR,C,A,L,L,SPACE,5,0,4,3,2,ENTER。
500000サイクルでは128705命令／500003サイクル、inputComplete=falseで
![途中のCALL入力](../images/ai-generation/pc1251/call.png)となり、RAM[0xC100]は00だった。
未送信キーを勝手に継続せず、表示を確認してからCLEARで始める新規要求を提案した。

400000命令／1000000サイクルの要求では256423命令／1000005サイクルで
inputComplete=true、![入力完了後のプロンプト](../images/ai-generation/pc1251/call-complete.png)。
ただしRAM[0xC100]は00で、これだけではCALL成功と判定しなかった。
この要求で値が観測できなかった原因は未特定。ファームウェアによる消去とは断定しない。

続いて0xC100のWRITE Watchを承認付きで登録し、通常プロンプトから新規CALLを実施。
165684命令／645060サイクルでwatch停止し、次を照合した。

- STDの実行アドレス0xC505、書込み先0xC100、書込み値0x5A。
- 停止PC=0xC506、RAM[0xC100]=0x5A。
- キー列はENTERの送信期間内で停止したためinputComplete=false。
  キー列完了と、プログラムが入力を受理・実行したことは別の観測である。
- 1命令StepでRTNを実行し、programCounter=0x51CCのROM内へ戻った。
- 検証用Watchを削除し、空キーの上限付きContinueを実施。
  128211命令／500000サイクルで停止、RAMの0x5Aを保持し、
  ![CALL後の通常プロンプト](../images/ai-generation/pc1251/after-call.png)を確認。

CALL／STD／RTN経路は観測できたが、任意の開始タイミング・上限での入力成功を保証しない。
値を保持できない原因の特定、再現性、複数ROM・RAM構成・実機は後続。
最終的にStudioを終了し、一時認証ファイルの削除を確認した。

### 同梱作例

実際に確認したBASIC本文をpc1251-numeric-labelsとしてmanifestとともに同梱。
知識APIのcatalogVersionはpc1251-v3。PC-1245版との誤取得を拒否する。
CALL用ルーチンは検証記録だけに置き、BASIC作例には含めない。

作例追加後の全体ビルド・Coreテスト・配布runtime検証は成功。
デスクトップ525件、failure 0、error 0、skip 3。
配布アプリ内の作例BASICと解説が元ファイルと一致することも確認した。
