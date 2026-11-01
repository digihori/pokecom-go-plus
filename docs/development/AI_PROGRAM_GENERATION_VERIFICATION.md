# Project・Source新規作成提案の初版検証

実施日：2026-11-01。[拡充計画](AI_MCP_EXPANSION_PLAN.md)第5段階。初版の作成・Buildと、その後の起動・入力・LCD確認を記録する。
操作手順は[新規生成ガイド](../guides/AI_PROGRAM_GENERATION.md)、APIは[AI連携仕様](../reference/AI_INTEGRATION.md#23-projectsourceの新規作成提案2026-11-01)を参照。

## 作成・Build初版の実装範囲

- PC-1245のBASIC／Assembly本文から、新しいProjectを提案・ローカル保存先選択・Studio承認後に作成して開く。
- 開いているProjectへ、最新のcontent revisionを指定して新しいSourceを提案・承認後に作成・登録する。
- 任意パス・任意manifest・上書き・削除・リモート承認を追加しない。
- 既存の操作結果取得・Source再取得・差分・Build・診断・承認付きLoadへの接続を維持。
- 同一ProjectへのSource追加で、エミュレーター側BASICの変更追跡を保持する。
- MCPは21ツール。設定コピー・設定例・ツールの変更annotationを更新。
- 構造化応答を使わないクライアント向けに、共通成功応答のtextへ実データも含める。

これは自動的に実行可能なコードを保証する生成器や、アプリ内AIを追加するものではない。
外部AIが要求から本文を作り、Studioは利用者の承認後にそのファイルを作成する。

## 作成・Build初版の自動検証

JDK 21で`./gradlew build :core:desktopTest :desktopApp:verifyDistributionRuntime`を実施。
成功。Desktop 505件、失敗0件、skip 3件。`git diff --check`成功。
追加テストは以下を確認する。

- 提案・保存先選択だけではファイルを作らず、却下でも無変更、保存先なしの承認は拒否。
- 確認したBASIC／Assembly本文とmanifestの一致、作成後の通常loader／Buildとの互換性。
- Source追加前のmanifest不変、承認後の登録、以前のrevisionによるBuild拒否、再取得後のBuild成功。
- 提案後の既存Source編集、未登録ファイルの出現、既存ID・パス、別Project、保存先の差し替えを拒否。
- Sourceディレクトリのsymlinkを拒否。最終revision検査失敗時に新規Sourceと一時ファイルだけを後始末。
- 型・未知キー・MCPパス指定・MCP承認指定・不正ID・重複ID・UTF-8サイズ超過・制御文字・他機種を拒否。
- operation IDをtext側でも取得できること、既存提案と共通のbusyキュー。
- 日英・幅420／800で、保存先選択前の承認無効、選択と承認が別操作、ファイル名・全文の表示。
- 公開21ツールと既存stdio／HTTPの回帰。

## 配布アプリ・stdioの実画面検証

macOS 26.6.2 / Apple Siliconの生成済み配布アプリで実施。
Codexが、リポジトリ外の空作業ディレクトリから同梱stdioブリッジを起動して検証用提案を送信し、
Studioの実画面を操作して承認・却下した。CodexネイティブMCP接続やCopilotの新API受入れとは区別する。
保存先は検証用の一時フォルダーで、MCPの引数にそのパスは渡していない。

| 操作 | 確認結果 |
| --- | --- |
| Project未選択で新規BASIC Project提案 | PENDING_APPROVAL、画面にファイル名と本文。保存先未選択で承認ボタン無効 |
| 却下・operation取得 | REJECTED、保存先にファイルなし |
| 再提案→親フォルダー選択 | 表示した新設先を確認、選択だけではファイルなし |
| Studioで承認 | APPLIED、新しいProjectが開き、metadataはNOT_BUILT・Source 1個 |
| BASIC Source再取得→Assembly追加提案 | 取得したprojectRevisionでPENDING_APPROVAL |
| Studioで追加承認 | APPLIED、追加Sourceの本文を再取得。projectRevisionが変更 |
| 新revisionでBuild提案→Studio承認 | SUCCEEDED、診断0件、BUILT_NOT_LOADED、Source 2個 |

検証した本文はBASIC `10 PRINT "HELLO"`／`20 END`と、Assembly `ORG 0xC500`／`RTN`。
Buildまでの検証で、HELLOの表示や実行時の復帰を確認したとは扱わない。
開始前にSessionを一時停止し、Project作成前後・Source追加後・Build後で同じSession ID／revisionを確認。
生成したプログラムのLoad・RUN／CALL・Stepは実施していない。
検証終了後にStudioを終了し、認証ファイルが削除されることを確認した。

## 上限付き起動・入力・LCD取得の追実装

- `pgp_propose_bounded_execution`：PC-1245のRUN／CALL／明示キー列／キーなし再開を、命令数とサイクル数の上限付きで提案・Studio承認後に実行。
- `pgp_get_bounded_execution`：承認待ち・実行中・停止結果、送信完了、実測命令数／サイクル数、停止位置・revisionを取得。
- `pgp_get_display`：停止中のPC-1245 LCDを、Session／revision付きピクセル・シンボル・PNGとして取得。OCRや実行はしない。
- 公開ツールは24件。従来の無制限Continueは維持し、上限付き再開と明確に区別。
- 停止時は未送信キーを破棄し自動キーを解放。手動操作・Project／Session切替・サーバー停止で中断する。
- Debug Context・画面・履歴へexecutionLimit停止理由を追加。既存のBreakpoint／Watch／Faultを尊重。
- G01の自作BASICを`pc1245-numeric-labels`として同梱し、知識カタログをpc1245-v2へ更新。

追加自動検証は以下を確認した。

- トレース無効・ホスト時刻が進まない条件でも、指定した命令数で正確に停止。
- 1サイクル上限で1命令の終了後に停止し、未完了入力を破棄。次の実行へ持ち越さない。
- キー列送信完了、Breakpoint、Watch、Fault、手動Pause・Reset・モード・ホスト入力による中断。
- 手動Loadで上限のない実行へ戻らないこと。
- 承認前・却下の無実行、古いrevision・Source変更・Project切替の拒否／中断、サーバー停止相当のcancel。
- 後の手動Stepが以前の停止結果を上書きしないこと、JSONのexecutionLimit出力。
- RUN／CALLの正確なキー列、未知キー・余計な引数・上限外・文字列化した数値を拒否。
- LCD読み取りの無実行・revision照合・実行中拒否、PNGのデコード、ドット配列とLCDメモリの一致、表示無効時の可視ピクセルが空であること。
- 日英・幅420／800の承認画面で、キー列と両上限を確認できること。
- 新しい同梱作例の本文・manifest一致とBASICコンパイル、出典・観測限界の取得。

## G01の配布アプリ・stdio実行検証

利用者登録済みROM、PC-1245・RUNモードの検証用Sessionで実施。
既存Projectをコピーする代わりに、Codexが要求から本文を作り、新規Project作成提案から開始した。
保存先選択と各承認はStudioの画面操作で行い、そのほかの提案・読取りは同梱stdioブリッジ経由。
手動でRUNや数値キーを入力したり、PCを強制移動したりしていない。
CodexネイティブMCPや、別チャットで正解情報を伏せた他AI受入れとは区別する。

1. BASIC本文11行で新規作成を提案し、Studioで親フォルダーを選んで承認。作成後のSourceを再取得。
2. Buildを提案・承認し成功。BASICは95 byte。成果物IDと最新revisionを使いLoadを提案・承認。
3. 上限200000命令／500000サイクルのrunを提案・承認し、停止後LCDで`?`を確認。
4. NUM_1→ENTER、NUM_2→ENTER、NUM_3→ENTER、NUM_0→ENTERを別々に提案・承認。
   各回、停止結果と同じSession／revisionのLCDを取得し、ONE／TWO／OTHER／通常の`>`を画像で確認。
5. 終了後にCLEAR→NUM_1→ENTERを提案・承認すると、ONEではなく通常の数値1を表示した。

| 操作 | 実測命令数 | 実測サイクル数 | LCDで観測した状態 |
| --- | --- | --- | --- |
| RUN | 126993 | 500002 | `?` |
| 1＋ENTER | 127104 | 500000 | ONEと入力カーソル |
| 2＋ENTER | 126760 | 500000 | TWOと入力カーソル |
| 3＋ENTER | 126862 | 500000 | OTHERと入力カーソル |
| 0＋ENTER | 127245 | 500000 | 通常の`>` |
| 終了後のCLEAR＋1＋ENTER | 127307 | 500000 | 通常の数値1 |

すべてcycleLimitで停止、inputComplete=true、projectStillCurrent=true。
programFinishedはAPI仕様どおりnotDeterminedであり、上限停止を終了検出と解釈していない。
表の表示はAPIの文字OCRではなく、返却されたPNGの観察結果。
Sourceは[再現用fixture](../../test-data/basic/ai/pc1245-numeric-labels/README.md)、
取得画像は[操作ガイド](../guides/AI_PROGRAM_GENERATION.md#lcdで結果を確認する)に掲載。
入力・表示・終了後の応答というG01の機能要件はこの環境で確認した。

### 入力途中での上限停止

NUM_7→ENTERを上限1サイクルで提案・承認すると、1命令・4サイクルで停止しinputComplete=false。
次のキーなしcontinueを500000サイクルで実行した結果は500004サイクル・inputComplete=trueで、
LCDのdisplayRevisionと表示は元の数値1のままだった。残りENTERの自動再送や、暗黙の入力再開はなかった。
この観測は、任意の途中入力が副作用を持たない保証ではない。

### CALLプリセットの実接続確認

別の新規ProjectにBASIC `10 END`と、以下の自作Assemblyを作成・Build・Loadした。

```text
ORG 0xC500
LIDP 0xC100
LIA 0x5A
STD
RTN
```

callAddress=`0xC500`（10進50432）を指定したcallプリセットの承認画面で、
CLEAR→C→A→L→L→SPACE→NUM_5→NUM_0→NUM_4→NUM_3→NUM_2→ENTERを確認・承認。
125888命令・500001サイクル、cycleLimit、inputComplete=trueで停止した。
停止前に`0xC100=0x00`、停止後に`0x5A`を確認し、LCDは通常の`>`へ戻った。
Debug Contextも`stop.kind=executionLimit`、`limit=cycleLimit`を返し、operationのrevisionと一致した。
この小さなルーチンでの確認を、任意コードのCALL規約や全レジスタ保存保証へ一般化しない。

### 配布resourceとCopilotのLCD読取り

最終配布アプリを再起動し、Project未選択・リポジトリ外の空作業ディレクトリから
`pgp_get_example(pc-1245, pc1245-numeric-labels)`を取得。
catalogVersion=pc1245-v2、manifestとBASICの2ファイル、検証した本文との一致を確認した。

VS Codeの「ローカル」実行環境、表示モデルGPT-6 Lunaでstdio設定を一時復元。
Copilotがmetadataの新しいSession ID／revisionで`pgp_get_display`を呼び出した。
実行詳細にPNGの添付画像が現れ、Copilotはenabled=true、80×7 pixels、`>`状の表示、
DE／G／RUNシンボル、pausedとBASIC終了の違いを説明した。
旧Sessionを再利用せず、Source・ファイル直接参照・変更・実行・入力は行わなかった。
新しい読取りAPIの実接続確認で、Copilotによる新規作成から全工程の独立受入れではない。

終了時はStudioを終了して認証ファイル削除を確認し、検証用VS Code設定を空のserversへ戻した。

## 追実装の最終チェック

JDK 21で`./gradlew build :core:desktopTest :desktopApp:verifyDistributionRuntime`成功。
Desktop 520件、失敗0件、skip 3件。`git diff --check`成功。
実画面検証と自動テスト、エミュレーターと実機の確認を区別して記録した。

## 残る検証

- Copilot等で新規作成からの独立受入れ、CodexネイティブMCP、他OS。Copilotの新APIはLCD読取りのみ確認。
- 他機種、外部assembler、バイナリSourceの新規作成提案。

初版の作成・Buildに加え、G01の機能経路をこの検証環境で確認した。
新規チャット・他AI・他ROM・実機までを一括してPASSとするものではない。
