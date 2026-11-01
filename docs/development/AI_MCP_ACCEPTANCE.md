# AI / MCP共通受入れ課題

更新日：2026-11-01。拡充計画の第1段階として、現行実装の棚卸しと検証手順を定義する。
本書の作成は実クライアントの受入れ完了を意味しない。
[拡充計画](AI_MCP_EXPANSION_PLAN.md)／[現行API仕様](../reference/AI_INTEGRATION.md)

## 1. 現行15ツールと不足機能

| ツール | 用途 | Studio承認 |
| --- | --- | --- |
| `pgp_get_capabilities` | 利用可能な機能と制限の取得 | 不要 |
| `pgp_get_machine_spec` | 機種仕様・命令一覧の取得 | 不要 |
| `pgp_get_development_context` | Project、診断、Assembly、Debug情報の取得 | 不要 |
| `pgp_get_debug_context` | 停止中のCPU、Memory、逆アセンブル、履歴の取得 | 不要 |
| `pgp_get_debug_registrations` | Breakpoint・Watch登録の取得 | 不要 |
| `pgp_get_source` | 登録済みSourceと内容revisionの取得 | 不要 |
| `pgp_propose_source_patch` | 登録済みSourceへの差分提案 | 必要 |
| `pgp_build_project` | 内蔵assemblerによるBuild提案 | 必要 |
| `pgp_get_operation` | Source・Build操作結果の取得 | 不要 |
| `pgp_propose_debug_action` | Breakpoint・Watch・Trace・Pause・Stepの提案 | 必要 |
| `pgp_get_debug_operation` | Debug操作結果の取得 | 不要 |
| `pgp_get_build_artifact` | 成功したメモリ内成果物の識別情報取得 | 不要 |
| `pgp_propose_load` | 成果物を停止中SessionへLoadする提案 | 必要 |
| `pgp_propose_continue` | 現在PCからの実行再開提案 | 必要 |
| `pgp_get_execution_operation` | Load・Continue操作結果の取得 | 不要 |

操作可能な状態、引数、上限は現行API仕様と接続先のcapabilitiesで確認する。
読み取りでもDebug情報は停止が必要な場合がある。取得のために自動Pauseしない。

| 不足機能 | 影響する課題 | 現段階の扱い |
| --- | --- | --- |
| 汎用接続設定・短期認証のクライアント間差の解消 | 接続 | 実接続で差を確認してから方式を確定 |
| 機種知識の検索・詳細取得、作例提供 | 全課題 | 仕様と情報の不足を記録。外部資料の利用を記録 |
| Project・Source新規作成 | 生成 | 現行機能の確認時は手動で作業用Projectを用意 |
| 利用者が選んだファイルのMCP経由取り込み | 解析 | 現行機能の確認時は手動で登録・Load |
| RUN／CALL・キー操作 | 実行観測・生成 | 利用者が操作。Continueと混同しない |
| LCD画像・表示状態取得 | 生成 | 利用者が目視。AIによる自動確認済みと扱わない |
| 命令数・サイクル数を制限した実行 | 実行観測・生成 | Breakpointと明示的Pauseを使い、無制限実行の完了待ちはしない |

## 2. 共通準備と記録方法

対象はPC-1245、Studio内蔵assembler。ROMは利用者が登録し、ROM本体は記録・共有しない。
サンプルは作業用フォルダーへコピーし、既存プログラムのない検証用Sessionを使う。
各課題を別チャットで開始し、指定した入力以外の正解資料をAIへ先に与えない。

結果はPASS、FAIL、BLOCKED、NOT_RUNで記録する。未実装機能やアカウント不足はBLOCKEDとし、
何を手動で補ったかを明記する。手動補助で確認できた部分と、MCPだけで完了した部分を区別する。

記録テンプレート：

```text
実施日 / 実施者:
Studio版・commit / OS:
AIクライアント名・版 / モデル:
接続方式 / 利用可能ツール:
機種 / RAM構成 / ROM識別hash:
課題ID / 結果:
入力・作業用Project:
ツール呼出しとoperation結果の概要:
確認できた結果・根拠:
手動補助 / 外部資料・リポジトリ直接参照:
失敗・不足機能 / 次の対応:
```

認証トークン、ヘッダー、個人の絶対パスは記録しない。
回答文の一致ではなく、取得根拠と観測結果で判定する。

## 3. C01：現行機能の接続・承認往復

入力Projectは[PC-1245 HELLO WORLD](../../test-data/assembly/pc1245-hello/README.md)の作業用コピー。
検証対象クライアントを接続し、次を依頼する。

> PGPの利用可能な機能、機種仕様、Project情報、登録済みSourceを取得してください。
> Assemblyのコメントを1行追加する変更を提案してください。承認後にSourceを再取得し、
> 新しいrevisionでBuildを提案し、完了結果と診断を確認してください。Loadや実行は不要です。

確認手順：

1. ツール一覧とcapabilitiesを取得し、PC-1245・内蔵assembler・正しいProjectであることを確認する。
2. SourceとprojectRevisionを取得し、意味を変えないコメント差分を提案する。
3. 承認前にファイルが変わっていないことを確認し、Studioで却下する。却下結果と無変更を確認する。
4. 改めて提案してStudioで承認し、operation完了と再取得したSourceへの反映を確認する。
5. 再取得したrevisionでBuildを提案し、Studio承認後にoperationとdiagnosticsを確認する。

PASS条件：認証、一覧取得、構造化応答の解釈、却下、承認、結果取得、Build成功が成立する。
待機中の操作を成功と誤認せず、取得済みrevisionを適切に更新する。
自動テストによる承認境界・revision拒否の確認とは別に、クライアント実接続の結果を残す。

## 4. A01：BASICソース解析

入力は[HELLO WORLD Project](../../test-data/assembly/pc1245-hello/pgp-project.json)と
登録済みの`src/main.bas`。AIには次だけを依頼する。

> BASICソースを取得して、各行の役割、入出力、機械語との関係を説明してください。
> BASICだけから分かる事実と、追加調査が必要な点を分けてください。

正解照合用：`10 CALL 50432`は`0xC500`の機械語を呼び出し、復帰後に`20 END`で終了する。
明示的なBASIC変数やPRINTはない。表示内容・破壊レジスタ・安全な復帰はBASIC本文だけでは確定しない。

PASS条件：機種とSourceを取得し、10進50432と`0xC500`の対応を説明する。
未取得のAssembly動作を推測で断定しない。追加Sourceを取得した場合は説明の根拠を明記する。
実行は不要。この課題は最小ソースを対象とし、一般的なBASIC解析能力全体の保証とはしない。

## 5. A02：ソースなしの機械語解析

準備担当者は[Debugger fixture](../../test-data/assembly/pc1245-debugger-acceptance/pgp-project.json)を
内蔵assemblerでBuildし、生成したDMPの内容を検証用Sessionへ手動Loadする。
BuildしたソースProjectは閉じ、登録SourceやListingに依存せず解析する。fixtureのソースは評価担当者の照合用とする。
AIへの入力は機種PC-1245、入口`0xC000`、対象`0xC000..0xC02F`、観測先`0xC100`のみ。

> 指定範囲を逆アセンブルして処理を説明してください。必要なら承認付きのBreakpoint・Trace・Stepで
> 実行を観測し、書込み順と呼出し・復帰を確認してください。解析結果は事実・推定・未確認に分けてください。

正解照合用：`0xC100`に`0x41`を書き、`0xC020`のサブルーチンが同じ位置へ`0x42`を書く。
サブルーチンはRTNし、呼出し側でAに9を設定して入口へジャンプする。全体はBASICへ戻らないループ。
最終Memory値だけでは途中の`0x41`書込みを証明できない。

PASS条件：逆アセンブルから処理を説明し、実行観測を行った場合は停止位置・Trace等の根拠を示す。
観測していないことを実測と表現せず、単なる無限ループを正常終了と扱わない。
実行開始は利用者が対象機種の構文を確認してCALLし、Breakpoint等で停止させる。
起動をMCPで自動化できない点は手動補助として記録する。ROM内の固定アドレスへPCを飛ばして起動しない。

## 6. G01：要求からの新規生成

空の作業領域から開始し、次を依頼する。言語はPC-1245対応BASICを初期候補とし、Assemblyが必要な場合は理由を説明する。

> PC-1245向けに、数値1を入力してENTERを押すとONE、2ならTWO、それ以外ならOTHERを表示する
> プログラムを新規作成してください。表示後は次の入力を待ち、数値0で終了してください。
> 必要な仕様をPGPから調べ、Buildして実行結果を確認し、問題があれば修正してください。

入力列と期待結果：1→ONE、2→TWO、3→OTHER、0→通常のBASIC入力待ちへ復帰。
キーを1回押しただけで切り替わる仕様ではなく、数値入力とENTERを使う課題とする。
非数値入力の処理は初期課題の対象外。

PASS条件：新規Project・Source作成、対象方言でのBuild、Load、起動、入力列の表示、終了を確認する。
Build失敗時は診断を取得して修正し、再Buildする。目視だけの場合は利用者による確認と記録する。
仕様取得不能、Project作成不能、キー操作不能、LCD取得不能は該当段階をBLOCKEDとして残す。
現行機能の部分確認では手動でProject作成・入力・表示確認を補ってよいが、MCPでの全工程完了とは扱わない。

## 7. 第1段階の結果と次の作業

2026-11-01：ソース実装と既存fixtureを確認し、15ツールの棚卸し、C01・A01・A02・G01の
入力・期待結果・判定方法・記録方法を定義した。実クライアント受入れは未実施。

C01の実施結果とクライアントの実行環境による差は
[接続検証記録](AI_MCP_CONNECTION_VERIFICATION.md)に記録する。
その結果から汎用設定生成とstdioブリッジの必要範囲を確定する。

2026-11-01追記：解析用取り込みAPIを追加した。[検証結果](AI_PROGRAM_ANALYSIS_VERIFICATION.md)で
A01とA02の最小入力の静的取得・解釈を確認。A02のTraceによる動的確認、全fixture、
他AIでの新API受入れは未完了。前述の手動Project登録・Loadは実行観測時の手順として残す。

2026-11-01追記：A02最小18 byteの対象RAM一致、通常CALL、Breakpoint、9命令Step／Trace、
途中2地点のMemory取得を確認。Copilot＋VS Code「ローカル」でもBASIC／DMP／停止中Contextの
取得と解釈を確認した。既存チャットで取得範囲を指定した誘導付き検証であり、
第2節に定めた新規チャットでの独立した全課題PASSとは区別する。
停止理由JSONと解析metadataの誤解を招く表現を修正し、再取得も確認。
詳細と残る範囲は[プログラム解析検証記録](AI_PROGRAM_ANALYSIS_VERIFICATION.md)を参照。

2026-11-01追記：G01の新規作成部分を進めるため、承認付きProject／Source作成提案を追加。
初版はPC-1245・内蔵assembler・BASIC／Assembly。利用者が保存先をStudioで選ぶ。
最小HELLO入力で作成・追加・再取得・Build成功を確認したが、G01の入力列・表示・終了は未検証。
起動・キー操作・LCD取得・実行上限も未追加のため、G01全工程PASSとは扱わない。
[生成検証記録](AI_PROGRAM_GENERATION_VERIFICATION.md)を参照。

2026-11-01追実装：上限付きRUN／CALL／キー入力とLCD取得を追加。
CodexのstdioプローブとStudio承認で、新規作成からG01の1／2／3／0、表示、END後の通常応答まで確認。
この環境でのG01機能要件は確認済み。Copilotはmetadata→LCD取得のみ確認し、全工程は未実施。
共通課題の新規チャット・他AI独立受入れまで一括してPASSとは扱わない。
作例と画像・停止根拠は[生成検証記録](AI_PROGRAM_GENERATION_VERIFICATION.md)に記録した。
