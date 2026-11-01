# AIからProject・Sourceを新規作成する

更新日：2026-11-02。PC-1245／PC-1251のテキストProjectを作成し、Build・Load・上限付きの起動／入力・LCD確認へ進めます。
Studio側の承認、利用者が用意するROMとRUNモード、各操作のSession／revisionの照合が必要です。

## 新しいProjectを作る

1. Studioで開いているProjectを閉じ、内蔵assemblerを選びます。作成とBuildにはROMは不要です。
2. MCPを接続したAIへ要求を伝え、機種知識・作例を取得してから、
   `pgp_propose_project_creation`でProject名とBASIC／Assembly本文を提案するよう依頼します。
3. Studioの確認画面で提案した対象機種、ファイル名、本文を確認します。
4. 「保存先の親フォルダーを選ぶ…」で保存先を選びます。この操作だけでは書き込みません。
   画面に表示する`pgp-generated-<識別子>`フォルダーを新しく作ります。
5. 「承認」でProjectとSourceを作成して開きます。「却下」なら作成しません。
6. AIは`pgp_get_operation`のAPPLIEDを確認し、開いたProjectのmetadataと
   `pgp_get_source`を再取得します。そこで得るprojectRevisionでBuildを提案し、Studioで承認します。

AIは保存先パスや任意のmanifestを指定できません。Source IDからStudioが
`src/<ID>.bas`または`src/<ID>.asm`を決め、manifestへ登録します。
既存フォルダー・ファイルへの上書きは行わず、作成先が既にある場合は失敗します。
承認前に別のProjectを開いた場合や処理中の場合も作成しません。

作成時にmachineId（pc-1245／pc-1251）を指定し、実行中の機種・ROM・RAM・PCは変更しません。
Projectを開いた状態では新規作成を拒否するため、既存Projectへ追加したい場合は次の操作を使います。
Build前にはmetadataの`buildMachineId`も確認します。既存のBuildは実行中Sessionの機種を使うため、
別機種を開いている場合は利用者がProjectと同じ機種へ切り替えるか、Sessionなしの状態でBuildしてください。

## Sourceを追加する

AIが現在の登録済みSourceを`pgp_get_source`で読み、そのprojectRevisionと新しいSource本文を
`pgp_propose_source_creation`へ渡します。Studioでファイル名と全文を確認して承認すると、
現在のProjectの`src`へ新規ファイルを作り、manifestへ登録します。

同じID、既存の登録パス、同名の未登録ファイル、リンク先への追加を拒否します。
提案後に既存Sourceやmanifestが編集された場合は失敗するので、再取得して新しく提案してください。
追加後もprojectRevisionが変わるため、古い値をBuildや差分編集へ使わないでください。
既存Sourceの修正には従来の`pgp_propose_source_patch`を使います。

## AIへの依頼例

> PC-1245向けに、画面へHELLOを表示して終了するBASICプログラムを作ってください。
> PGPの機種知識・作例を確認し、新規Project作成を提案してください。
> Studio承認後にoperationとSourceを再取得し、最新revisionでBuildを提案して診断を確認してください。
> 今回はLoad・起動・実行確認は不要です。Build成功と実行成功を区別してください。

初版の入力は1〜8個のBASIC／Assembly、本文合計32 KiB（UTF-8）以内。
Source IDは英小文字で始まる英小文字・数字・`_`・`-`の32文字以内で、予約名は使えません。
追加は1個ずつ。PC-1245／PC-1251・内蔵assemblerのテキストProjectが対象です。
構文不正や重複配置等は作成時に意味検証せず、Build診断で確認・修正します。
たとえばBASICを複数追加できても、現行Buildは複数BASICを拒否します。

## Buildの後

1. AIが成功したBuildの`pgp_get_build_artifact`を取得します。
2. 適切なROMを使うProjectと同じ機種を停止し、現在のSession ID／revisionとprojectRevisionで`pgp_propose_load`を提案します。
3. Studioで書込み範囲を確認して承認します。Load成功後も停止したままです。
4. RUNモードで、BASICが通常のコマンド入力待ちであることをLCDで確認します。
   エミュレーターの「停止中」とBASICプログラムの「終了」は別です。INPUT待ちの途中にRUNを打ち直さないでください。
5. 新しいSession revisionで`pgp_propose_bounded_execution`のrunを提案し、キー列と上限をStudioで承認します。
6. `pgp_get_bounded_execution`を取得し、RUNNINGなら停止結果を待ちます。
7. 結果のobservedRevisionとSession IDで`pgp_get_display`を取得し、LCDを確認します。
8. 次の数値入力も新しいrevisionのkeys要求として提案・承認し、同じように停止後のLCDを確認します。

生成結果の正しさはBuildや上限停止だけでは保証できません。
[共通課題G01](../development/AI_MCP_ACCEPTANCE.md#6-g01要求からの新規生成)の入力列・表示・終了後の応答を根拠にします。

## 上限付き実行と入力

| action | 入力するキー列 |
| --- | --- |
| run | CLEAR → R → U → N → ENTER |
| call | CLEAR → C → A → L → L → SPACE → RAMアドレスの10進数字 → ENTER |
| keys | 明示したキー列。数値1とENTERなら`["NUM_1","ENTER"]` |
| continue | キーなしで現在PCから再開 |

callプリセットのcallAddressは`0xC000..0xC7FF`の整数。たとえば`0xC500`（10進50432）は
BASICへ`CALL 50432`として入力します。直接PCを変更せず、呼出し環境はBASICに準備させます。
従来の`pgp_propose_continue`は無制限の再開なので、入力・生成検証では上限付きのcontinueを使います。

両方の上限が必須です。maxInstructionsは1〜1000000、maxCyclesは1〜5000000。
エミュレートした命令数・サイクル数で制限し、ホストの経過時間の制限ではありません。
サイクル数は命令終了時に確認するため、最後の1命令分だけ超える場合があります。
初期の検証例は200000命令／500000サイクルです。用途と観測結果に合わせて設定します。

キー入力例（Session・revisionは実際の取得値へ置換）：

```json
{
  "sessionId": "<現在のSession ID>",
  "sessionRevision": 0,
  "projectRevision": "<pgp_get_sourceのprojectRevision>",
  "action": "keys",
  "keys": ["NUM_1", "ENTER"],
  "maxInstructions": 200000,
  "maxCycles": 500000
}
```

SUCCEEDEDは要求が停止結果まで到達した意味で、プログラムの正常終了ではありません。
inputCompleteもキー列の送信完了で、ROMによる処理完了や入力待ちの証明ではありません。
Breakpoint／Watch／Faultや上限で途中停止した場合、未送信キーを破棄し押下中の自動キーを解放します。
途中までの副作用はあり得るため、入力を自動再送せず、LCD・Debug Contextを確認して次を提案してください。

手動Pause、モード切替、Reset、手動入力、Project／Session切替、サーバー停止でも実行を中断します。
上限付き実行中の手動Loadは、上限のない実行へ戻さず停止します。
projectStillCurrentはSourceファイルの一致確認であり、実行中RAMとの同一性を証明する値ではありません。

## LCDで結果を確認する

`pgp_get_display`は停止中のSession IDとsessionRevisionを指定する読み取りツールです。
PNG画像、見えているドットの行配列、表示有効フラグ、シンボルを返し、実行や自動Pauseはしません。
文字のOCR結果は提供しません。画像の観察とAPIが返した数値・ピクセルを区別して記録します。
手動Step等でrevisionが変わった場合は取得し直し、別時点のLCDを前の操作結果と混同しません。

G01の[同梱作例](../../test-data/basic/ai/pc1245-numeric-labels/README.md)は
`pgp_get_example(machineId="pc-1245", exampleId="pc1245-numeric-labels")`からも取得できます。
ラベルをINPUTのプロンプトにして表示と次の数値入力待ちをまとめており、カーソルも表示されます。
以下は検証用Sessionから実際に取得したLCDです。実機・任意ROM版の保証ではありません。

| 入力 | 取得したLCD |
| --- | --- |
| RUN | ![最初の入力待ち](../images/ai-generation/run.png) |
| 1＋ENTER | ![ONEと入力カーソル](../images/ai-generation/one.png) |
| 2＋ENTER | ![TWOと入力カーソル](../images/ai-generation/two.png) |
| 3＋ENTER | ![OTHERと入力カーソル](../images/ai-generation/other.png) |
| 0＋ENTER | ![通常のBASICプロンプト](../images/ai-generation/zero.png) |
| 終了後のCLEAR＋1＋ENTER | ![通常の数値1の応答](../images/ai-generation/after-end.png) |

終了後にCLEAR→1→ENTERを入力すると、ONEではなく通常の数値1の応答になることも確認しました。
これは利用した環境での観測結果で、上限付き実行API自身がBASICの終了を検出する機能ではありません。

API・エラーは[AI連携仕様](../reference/AI_INTEGRATION.md)、接続は[接続ガイド](AI_CONNECTION_GUIDE.md)、
今回の確認範囲は[検証記録](../development/AI_PROGRAM_GENERATION_VERIFICATION.md)を参照。

## PC-1251の追加範囲

Project／Source生成、上限付きRUN／CALL・キー入力、停止中LCD取得を提供する。
PC-1251のLCDは120列×7行（24文字）。CALLのschema範囲は0xA000〜0xC7FFだが、
実際に許可される入口は現在のSessionのRAM構成で決まる。
HARDWARE構成のPC-1251は0xB800より下を使えない。RAMミラーへのCALL指定も拒否する。
PC-1245では引き続き0xC000〜0xC7FFのみ。

新規Project作成でROM・RAM設定・現在PCは変わらない。
既存ProjectとSessionの機種が一致しないと上限実行を提案できない。
停止上限・入力完了・表示取得の意味は両機種共通。
PC-1251の数値入力作例は`pgp_get_example(machineId="pc-1251", exampleId="pc1251-numeric-labels")`から取得できます。
1つのローカルROMで通常起動→生成→Build→Load→RUN→数値入力→LCDを確認しました。
上記G01の画像はPC-1245のもので、PC-1251の画像・CALL観測は
[機種展開記録](../development/AI_MACHINE_ROLLOUT_VERIFICATION.md)を参照してください。
長いCALLキー列はPC-1251では50万サイクルで途中停止した観測があります。
inputComplete=trueでも実行成功とは限らないため、RAM・停止理由・表示を照合します。
実機・任意ROM・外部AIの全工程受入れは未検証です。
