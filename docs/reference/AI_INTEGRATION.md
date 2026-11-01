# Pokecom GO Studio AI Integration

**ステータス:** 作業順8〜12に加え、同梱stdioブリッジ、PC-1245知識APIと解析用取り込みを実装。Project／Source作成提案を追加。上限付き実行・LCD取得を追加。公開MCP toolは24件
**更新日:** 2026-11-01

今後の拡充方針は[MCP対応AIクライアントへのAI連携拡充計画](../development/AI_MCP_EXPANSION_PLAN.md)
（2026-11-01）を参照。Codexを主な開発・確認環境とし、Copilot等で相互運用を検証する。
以下は現行APIの仕様であり、計画中の機能とは区別する。

## 現在の到達点（2026-10-22）

機種仕様・Source・開発／Debug Context取得から、Source差分提案、Build、Load、Continue、
PauseまでをStudio承認付きで接続した。Source差分／BuildとLoad／Continue／Pauseは実接続で
成功を確認し、PC-1245のHELLO WORLD表示も確認した。
登録編集・Trace・1命令Stepの詳細は合成／HTTPテストで検証済みで、実ROMでの全操作受入れとは区別する。

| 項目 | 現在の範囲 |
| --- | --- |
| 機種仕様・開発状態 | 内蔵assembler向けの範囲限定読み取り |
| Source変更 | 登録済みUTF-8 BASIC / Assemblyの差分をStudioで承認 |
| Build / Load | それぞれ承認。Loadは成功したメモリ内成果物を使い、停止を維持 |
| 実行操作 | Pause・1命令Step・現在PCからのContinueをそれぞれ承認 |
| デバッグ登録 | Breakpoint / Watch・Trace設定を承認付きで変更 |
| アドレス案内 | 原則16進。実行入力は対象機種で使える構文を使用 |

新規Project作成、キー入力、LCD画像取得、直接Memory書込み、外部assemblerのMCP Build、
自動の実行上限は今回の対象外。ContinueはBASICのRUN / CALLを入力しない。
次の機能追加は上記の拡充計画に沿って具体化し、実利用で得られた不具合や操作性の改善も反映する。
接続手順は[AI接続ガイド](../guides/AI_CONNECTION_GUIDE.md)、初期構想・実装順・検証結果は[履歴](../development/AI_INTEGRATION_HISTORY.md)を参照。
以下は現行APIの仕様・制約を中心に記載し、段階名と日付は由来の識別に残す。

## 1. 目的

Pokecom GO Studioを、AIがポケコンプログラムの作成、解析、デバッグを支援できる開発環境にする。
Studioへ特定のチャットUIやAIモデルを埋め込むことから始めるのではなく、Studioが保持するプロジェクト、
Assembler、Debugger、実行中Sessionを、安全な構造化ツールとして外部AIへ公開する。

AIはSC61860や機種固有仕様を記憶だけで推測せず、Studioが返すMachine Definition、Memory Map、
Disassembly、Symbol、Trace、Snapshotを根拠として解析する。

## 2. 想定ユースケース

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 3. 接続モデル

初期実装は、Studioがlocalhost限定のMCPサーバーとして動作し、Codex等のMCP対応AIクライアントから
接続する方式を優先する。

```text
AI Client
├─ AIサービスへの認証
├─ モデル選択
└─ 会話と承認UI
        ↓ localhost MCP
Pokecom GO Studio
├─ Project／Build／Diagnostics
├─ Assembly／Disassembly／Symbols
├─ Debug Snapshot／Trace／Memory
└─ Emulator Commands
```

この構成ではStudioがクラウドAIの資格情報を保持しない。将来、内蔵AIを追加する場合もProvider境界を設け、
OpenAI等のクラウドAPI、ローカルモデル、外部MCPクライアントを切り替え可能にする。

## 4. MCPツール候補

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 5. 安全性と承認

初期段階では読み取り専用ツールだけを提供する。状態変更ツールを追加する際は、次を原則とする。

- localhost以外ではlistenしない
- 起動ごとの短期接続トークンを使用する
- 利用可能なツールと対象プロジェクトを接続時に明示する
- ソース変更は全文上書きでなく差分を提示する
- Memory書込み、Build成果物のLoad、実行再開はStudioで承認する
- ROM登録、ROM削除、任意ファイルアクセスはAIへ公開しない
- すべての状態変更操作をユーザーが確認できる履歴へ記録する

AIが提案したコードと操作は信頼済み入力とみなさず、Assembler、Manifest、Memory規則、アドレス範囲を
Studio側で必ず再検証する。

## 6. データ送信方針

AIへ渡す情報はタスクに必要な最小範囲とする。実機ROM全体は公開せず、必要なアドレス範囲のbyte列または
Disassemblyだけを提供する。送信候補にはソース、Memory、Trace、ROM由来Disassemblyが含まれ得るため、
外部AIへ渡すデータのプレビューと許可範囲をユーザーが確認できるようにする。

APIキー、ROM、プロジェクト外ファイル、個人情報をDiagnostics、操作履歴、AIプロンプトへ自動的に含めない。

## 7. 実装順

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 8. 非目標

- AIへROM全体を無条件に送信すること
- AIが無確認でソース、Memory、ROMを変更すること
- PGP Coreを特定AIベンダーのSDKへ依存させること
- AIの推測を実機仕様や解析結果として確定すること
- Playerへ開発・デバッグ用AI操作を搭載すること

## 9. 読み取り専用Debug Context v1

Studio Desktopには、UIと将来のMCP Adapterから共用する`DebugContextService`境界を置く。
初期実装は停止中またはFault状態のSessionだけを対象とし、取得のために自動Pauseしない。

`pgp-debug-context` schema version 1は、Session ID／revision、CPU、構造化された停止理由、現在の
ROM component／bank／offset、明示指定されたMemory、Disassembly、Memory Access History、Bank History、
Instruction Traceを保持する。Memoryは最大8範囲、合計4096byteとし、ROMおよびROM mirrorのbyte取得を
拒否する。Disassembly、履歴、Traceにも件数上限を設ける。

Memory Access観測は実行負荷を伴うため明示的に有効化し、Debug Contextの読み取り要求自体は観測設定や
Session状態を変更しない。ROM由来情報は物理位置と範囲制限されたDisassemblyに限定する。

Studio DebuggerとDebugメニューの`Export AI Debug Context`から、Memory範囲、Disassembly件数、各履歴件数を
指定し、JSONをプレビューしてから保存できる。Debuggerを開いている間だけMemory Access観測を有効化する。
Context取得時にSessionが実行中の場合は、自動Pauseせずユーザーへ停止を求める。

## 10. localhost MCP初版

Studioの`Debug` → `AI / MCP Server`から、`127.0.0.1`だけにbindするStreamable HTTPサーバーを
起動できる。既定endpointは`http://127.0.0.1:8765/mcp`で、portは起動前に変更できる。
Studio起動ごとに256-bitの短期Bearer tokenを生成し、認証されていない要求を拒否する。token、tool引数、
Debug Context本体は通常ログへ出力せず、管理画面には直近要求のmethod、tool名、成否だけを表示する。


### HTTP接続の制限（1.0.0）

接続対象はローカルのネイティブAIクライアントです。`Origin`ヘッダーがある要求は、
`null`やlocalhost由来も含めてHTTP 403で拒否します。ブラウザーからの直接接続とCORSはサポートしません。
`Host`は実際の待受ポートを含む`127.0.0.1:<port>`または`localhost:<port>`との完全一致が必要です。
任意のホスト名をDNS解決して許可することはありません。これらの検査はMCPとルートURLの両方に適用します。
MCPでは引き続きBearer認証が必要で、Originのない認証済みPOSTだけを受け付けます。
プリフライトを許可する応答は返しません。通常のクライアントは表示されたendpointと認証ヘルパーを使用してください。

この方針は[MCPのHTTP transportのセキュリティ要件](https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/docs/specification/2025-06-18/basic/transports.mdx)に沿って、
接続元の検証、ループバック限定の待受け、認証を組み合わせます。ソース差分・Build・Load・実行のローカル承認は引き続き必要です。

基本の読み取り専用toolは次の五つである。Source / Buildは第14節、デバッグ操作は第15節、Load / Continueは第16節を参照。

- `pgp_get_capabilities`
- `pgp_get_debug_context`
- `pgp_get_debug_registrations`
- `pgp_get_machine_spec`
- `pgp_get_development_context`

いずれも`readOnlyHint = true`、`destructiveHint = false`、`openWorldHint = false`とする。HTTP handlerは
Emulator Sessionへ直接触れず、StudioのApplication thread上で共通`DebugContextService`を呼び出す。
要求bodyは64KiBまでとし、Context側のMemory、Disassembly、履歴上限も重ねて検証する。

サーバー起動時、Studioは短期tokenをプロジェクト外のOS別アプリデータ領域へ保存する。保存先directoryと
credential fileは現在のOSユーザーだけがアクセスできる権限に制限し、安全な権限を設定・検証できなければ
サーバー起動を失敗させる。終了時には、自分が書いたtokenと一致する場合だけcredential fileを削除する。

Codexから接続する場合は、管理画面の`Copy Codex config`で取得した次の形式の内容を`config.toml`へ一度追加する。
`http_headers_helper`は各request時に現在の短期tokenを読み取るため、Studio再起動後も環境変数の再設定は不要である。

```toml
[mcp_servers.pokecom_go_studio]
url = "http://127.0.0.1:8765/mcp"
http_headers_helper = "/bin/cat '/Users/example/Library/Application Support/PokecomGOStudio/mcp-headers.json'"
enabled_tools = ["pgp_propose_bounded_execution", "pgp_get_bounded_execution", "pgp_get_display", "pgp_propose_project_creation", "pgp_propose_source_creation", "pgp_get_analysis_context", "pgp_search_knowledge", "pgp_get_knowledge", "pgp_get_example", "pgp_get_capabilities", "pgp_get_debug_context", "pgp_get_debug_registrations", "pgp_get_machine_spec", "pgp_get_development_context", "pgp_get_source", "pgp_propose_source_patch", "pgp_build_project", "pgp_get_operation", "pgp_propose_debug_action", "pgp_get_debug_operation", "pgp_get_build_artifact", "pgp_propose_load", "pgp_propose_continue", "pgp_get_execution_operation"]
default_tools_approval_mode = "auto"
```

短期tokenはStudioを再起動すると変わる。credential fileにはHTTP Authorization headerだけをJSONで保持し、
通常ログ、Debug Context、プロジェクト内ファイルには含めない。保存先はmacOSでは
`~/Library/Application Support/PokecomGOStudio/`、Windowsでは`%LOCALAPPDATA%\\PokecomGOStudio\\`、
Linuxでは`$XDG_RUNTIME_DIR/pokecom-go-studio/`（未設定時は`~/.local/state/pokecom-go-studio/`）とする。

## 登録状態の読み取り（2026-10-14）

`pgp_get_debug_registrations`は引数なし（`{}`）で取得する。
新toolの追加だけで、既存tool名・MCPプロトコル・Debug Context version 1の応答は変更しない。
既存のCodex設定がenabled_toolsで制限している場合、新toolを追加するかStudioから設定を再コピーする。

応答のstructuredContentはformat `pgp-debug-registrations`、schemaVersion 1。
sessionにはid・revision・machineId・runStateを返す。通常のaddressScopeは`logicalAllBanks`、
bank指定Breakpointがある場合は`logicalWithOptionalRomBank`を返す（2026-10-30追加、[第19節](#19-bank指定breakpoint2026-10-30)）。
breakpointsはid・address・enabled、watchesはid・startAddress・endAddressInclusive・kind・enabled。
kindは`change`／`read`／`write`／`readWrite`。アドレス・ID・revisionはJSON数値、enabledはboolean。
無効登録も含み、空一覧は空配列。ブレークポイントはアドレス順、監視は登録順。
IDはSession内だけで有効。Resetで登録とIDを維持し、新Sessionへ引き継がない。
登録変更はSession revisionを更新する。別々のContext取得との照合にはSession IDとrevisionの両方を使う。
停止理由が登録IDを持たないため、後から編集された現在登録との一致を停止当時の登録だと断定しない。

停止中／Fault時に取得可能。実行中はSESSION_RUNNINGで、自動停止しない。
SessionなしはNO_ACTIVE_SESSION、引数ありはINVALID_ARGUMENTS。
既存のApplication dispatcher・認証・localhost制限を共用し、読み取りでCPU、履歴、Trace、登録、revisionを変更しない。
ROMバイト・監視基準値・任意ファイル・認証情報は返さない。登録の変更APIは追加しない。

### 読み取り登録APIの検証

当時の自動検証・実接続の区分は[検証履歴](../development/AI_INTEGRATION_HISTORY.md)を参照。

## 11. マシン語生成の進め方と当面の優先順位（2026-10-21）

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 2026-10-21: 条件付きBreakpointとの関係

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 12. 読み取りAPIの拡充（2026-10-21、作業順8）

デバッグ強化1〜4の実装後、AI連携強化へ移行した。以下は実装済みの読み取り専用API。
新しいCommon Application Serviceは既存のUIモデルとCoreの定義を読む。
HTTP handlerからSessionへ直接触れず、既存のApplication dispatcher、Bearer認証、localhost制限を共用する。
既存3ツールの名前・引数・応答schemaを維持し、新しい2ツールを追加した。

### `pgp_get_machine_spec`

引数は任意の`machineId`だけ。省略時は現在のSession機種、指定時はMachineCatalogの対応機種を返す。
Sessionなしでも、機種IDを指定すればROMを読み込まずに問い合わせられる。
引数の機種候補はtools/listのschemaにも掲載する。

```json
{"machineId":"pc-1245"}
```

応答は`format: pgp-machine-spec`、`schemaVersion: 1`。
機種・世代・BASIC方言・文字表示の列数／行数、キーボードlayout IDと入力保持／解放cycle、
動作モード、基準cycles/second、Memory RegionとROM bank窓、推奨Assembly開始アドレス等を返す。
SC61860命令のopcode・長さ・operand encoding・control flow・encodableは、
Assembler／Disassemblerと同じ`Sc61860InstructionSet`から列挙する。

`memoryLayout.scope`が`activeSession`なら現在のRAM構成、`catalogDefaults`なら機種の既定値である。
Memory RegionはDebuggerと同じ説明用定義であり、全アドレスの書込み許可表ではない。
実ROMのルーチン呼出規約、LCDのpixel配置・符号化、キー読取りABI、実機での独立したタイミング検証は
このAPIで補完・推定せず、`notProvided`に明示する。ROMデータは返さない。

### `pgp_get_development_context`

全応答は`format: pgp-development-context`、`schemaVersion: 1`。`section`は省略時`metadata`。
必要なページだけ取得し、全ソース・全履歴を暗黙に送信しない。

| section | 主な内容 | 追加引数 |
|---|---|---|
| `metadata` | 現在Project、機種、StudioのBuild状態、Source一覧、変更検出状態、選択Assemblerの種類・timeout・Listing対応、現在ROMの識別hash | なし |
| `diagnostics` | 最後のBuild／Assemblyの診断。型付きcode、source ID、利用可能な行／アドレス。内部Assemblerのメッセージはパスを除去・長さを制限する | なし |
| `assembly` | 指定Sourceの最後の成功Assembly PreviewのSymbol・Listing位置、鮮度、別途確認したLoad済み実行位置 | `sourceId`必須 |
| `debug` | 条件とヒット回数を含むBreakpoint、Watch、Trace設定、Step Over／Out状態 | なし |

```json
{"section":"metadata","offset":0,"limit":32}
```

```json
{"section":"assembly","sourceId":"main","offset":0,"limit":32}
```

```json
{"section":"debug"}
```

`offset`は0〜1000000、`limit`は1〜128（既定32）。metadata／diagnosticsは`total`と`nextOffset`、
assemblyはSymbol／Listing別、debugはBreakpoint／Watch別の件数と次offsetを返す。
`sourceId`は開いているProjectの宣言済みID（128文字以内）のみで、パスや任意ファイルを引数に取らない。
assembly以外へのsourceId、未知引数、null、文字列での整数、範囲外の値を拒否する。
データ本体は最大128 KiB。超過時は`RESPONSE_TOO_LARGE`を返し、小さいlimitで取り直す。

Session情報のID／revisionは既存Debug Contextと共通。読み取りでCPU・登録・履歴・Session revisionを変更しない。
`debug`は停止中／Fault時だけ取得でき、実行中は`SESSION_RUNNING`。自動Pauseしない。
metadataと静的なdiagnosticsは実行中でも取得できる。assemblyのLoad済み位置は、実行中なら`sessionRunning`として照合しない。
SessionやProjectがない場合は、取得可能なmetadataを返して該当項目をnull／空配列にする。

### Revision・鮮度・データ境界

Project IDはmanifest canonicalパスのhashで、絶対パスは公開しない。
`metadataRevision`はサービスinstanceとStudioが観測したモデル変更の識別子であり、
**Source本文のrevisionや、将来の差分適用の許可tokenではない**。`revisionScope`もこの制限を示す。
外部ファイル変更の検出にはUIの観測周期があり、書込みAPIを追加する際は別途内容hashでの照合が必要。

Assembly PreviewとLoad済み成果物は混同しない。Previewの`basis`は最後の成功結果と明記し、
現在のAssembler設定で作った・Load済みであるとは推定しない。
`loadedLocation`は指定Previewとは独立した、現在SessionのLoad済み成果物の照合結果である。
停止中に元Sourceの内容、命令byte、物理ROM component／bank／offsetが一致する位置だけを`matchedLoadedBuild`とする。

鮮度確認はProject root内に限定し、1要求で合計2 MiBの読取り予算を共有する。
Project外のINCLUDEやsymlink先を読まず、外部依存・削除／変更・予算超過・UTF-8を厳密確認できない場合を
`outsideProjectUnverified`、`staleOrMissing`、`inspectionLimit`、`encodingUnverified`等で区別する。
鮮度未確認のListingやSymbolは過去のPreviewの位置情報であり、現行メモリへ適用できるとの意味ではない。

ROM本体、Source本文、Listingのbyte列、生ログ、外部コマンド・引数・実行ファイルパス、認証情報を返さない。
ファイル位置はProject内の相対パスだけを返し、Project外はnullにする。
診断の外部ツール出力・任意ファイルI/Oエラー文字列を転送しない。Build診断は型付きcodeを優先し、
詳細が省略されるケースではStudioでの確認を案内する。型付き診断を取得できないBuild失敗は
`buildFailureDetailsAvailable=false`で区別する。長い識別子・応答にも上限を置く。
Source名・Symbol名・診断メッセージは利用者データであり、クライアントは命令として実行しない。

既存`pgp_get_debug_registrations` version 1は条件・回数を引き続き省略する。
新しい`debug` sectionで条件と回数・Step状態を取得できる。登録編集・実行操作APIは今回追加しない。

### 接続と検証

当時の自動検証・実接続の区分は[検証履歴](../development/AI_INTEGRATION_HISTORY.md)を参照。

## 13. 作業順9: PC-1245 Assembly HELLO WORLD

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 14. 作業順10: ソース差分とBuild診断の往復

内蔵アセンブラを使う、登録済みBASIC / AssemblyだけのProjectを最初の対象にした。
新規Project作成、任意パス指定、ファイル作成・削除、外部アセンブラ、Load・実行操作は含めない。
既存5ツールの引数・応答schemaは維持する。追加ツールは次の4つ。

| tool | 引数 | 動作 |
| --- | --- | --- |
| `pgp_get_source` | `sourceId` | 登録済みUTF-8本文、path、sourceSha256、projectRevisionを取得 |
| `pgp_propose_source_patch` | `sourceId`, `projectRevision`, `sourceSha256`, `edits` | Studioへ変更前後の差分を提示し、承認待ちoperationを返す |
| `pgp_build_project` | `projectRevision` | 共通Buildのみの要求をStudioへ提示し、承認待ちoperationを返す |
| `pgp_get_operation` | `operationId` | 承認待ち・却下・適用・実行中・成功・失敗の状態を取得 |

`edits` は `[{"oldText":"LIA 1","newText":"LIA 2"}]` のような完全一致の置換。
各oldTextは空でなく、順に適用される時点で一意に一致する必要がある。newTextは空も可。
不明なフィールド、複数一致、未一致、無変更、NUL、不正UTF-8、上限超過を拒否する。
登録ソース本文は1ファイル64 KiB、Projectは64ファイル・計2 MiB、1提案16置換、置換前後合計32 Ki文字まで。
外部ファイルを含むリンク、同じ物理pathへの重複登録、対応OS上のhard link、build出力先のlinkや入力衝突も拒否する。

`projectRevision` は旧metadataRevisionとは異なり、Projectを開いた世代、manifest、登録された全ソースの内容、
内蔵assembler設定、対象メモリ構成のSHA-256に基づく。取得後の変更やProject切替は拒否する。
manifestの内容がStudioで開いた定義と変わった場合は、Projectを開き直す必要がある。
承認直前にもrevisionを確認し、一時ファイルからatomic renameで一括置換する。atomic move非対応のFSでは失敗させる。
通常の外部編集との競合を検出するが、別プロセスによるファイルシステムの敵対的な同時変更をOS全体でロックするものではない。

MCPから承認・適用を確定するtoolは設けない。提案が来るとStudioのAI / MCP画面が開き、
Project名・登録path・変更前後を見て「承認」または「却下」を選べる。Buildも明示承認する。
これは外部MCPクライアントのtool承認とは別の、Studio側の確認である。
状態変更履歴は直近32件をStudioの同画面に保持する（メモリ内、永続化しない）。
server停止時の未承認要求は却下する。承認後に始まったBuildは通常のStudio処理として完了する。

`pgp_propose_source_patch` と `pgp_build_project` は `readOnlyHint=false`。
両者は承認待ち要求を作成するだけで、Studioの承認前には書込みやBuildを行わない。
source / operation取得はreadOnly。全4ツールとも `openWorldHint=false`、任意コマンドやROM本体を返さない。
ソース本文を返すAPIは明示的な `pgp_get_source` だけであり、既存metadata APIへ本文を追加しない。

### 操作の流れ

1. metadataでProjectとassemblerを確認し、`pgp_get_source` で対象本文とrevisionを得る。
2. 変更前後を `pgp_propose_source_patch` で提案する。Studioでユーザーが確認・承認する。
3. operationを確認し、APPLIEDなら本文を再取得する。以前のrevisionを使い回さない。
4. 新しいrevisionで `pgp_build_project` を要求し、Studioで承認する。
5. operation完了後、`pgp_get_development_context({"section":"diagnostics"})` を取得する。
6. 失敗なら行・code・messageを基に修正案を再提案する。成功時はBUILT_NOT_LOADED。

BuildはStudioの「出力をBuild」と同じ `buildProjectOutputsWork` → 共通compiler / builder / output writerを使う。
Build中の入力変更はoperation成功として承認しない。失敗diagnosticsは既存の構造化APIで取得する。
Load・CPU実行・停止・RAM変更・デバッグ登録の変更は自動では行わない。
新ツールを使うには最新Studioへ更新し、MCP画面の「Codex設定をコピー」で9ツールの設定を反映する。
実行中の旧Studioや既存クライアントの接続設定は、この実装作業では自動更新していない。

### 検証

当時の自動検証・実接続の区分は[検証履歴](../development/AI_INTEGRATION_HISTORY.md)を参照。

## 15. 作業順11: Studio承認付きデバッグ操作

2026-10-22: Breakpoint / Watchの登録・更新・削除、命令Traceの有効化・容量変更、Pause、1命令Stepを追加。
既存9ツールの名前・引数・応答は変更しない。Load、Continue、Reset、直接Memory書込み、Step Over/Out、
Trace履歴全消去、ROM操作は今回の追加toolに含めない。

### 追加ツール

- `pgp_propose_debug_action`: Sessionを指定し、Studio上の確認待ち要求を作成する。
- `pgp_get_debug_operation`: operationIdから承認待ち・却下・成功・失敗、Session ID、要求時と完了時のrevision、
  登録ID、変更前後の概要を取得する。直近32件をメモリ内で保持し、再起動で消去する。

提案toolはreadOnly=false、destructiveHint=true、openWorld=false。取得toolはreadOnly=true。
MCPに承認toolは設けず、Studioで対象Session・revision・変更前後を見て承認または却下する。
server停止時は未承認要求を却下する。Studioの同画面に操作履歴と変更概要を表示する。

### 引数と状態条件

`pgp_propose_debug_action` は `sessionId`, `action`, `parameters` を必須とする。
Pause以外は整数の `sessionRevision` も必須。未知のキー、文字列化した数値・boolean、未定義actionを拒否する。

| action | parameters | 対象状態 |
| --- | --- | --- |
| `save_breakpoint` | address, enabled、任意のid / condition / hitCondition | 停止中・Fault |
| `remove_breakpoint` | id | 停止中・Fault |
| `save_watch` | startAddress, endAddress, kind, enabled、任意のid | 停止中・Fault |
| `remove_watch` | id | 停止中・Fault |
| `trace` | enabled, capacity | 停止中・Fault |
| `pause` | 空object | 実行中 |
| `step` | 空object | 一時停止中 |

idなしは新規登録、idありは同Sessionの既存登録の更新。Breakpointのcondition / hitConditionを省略すると
更新時は既存値を保持し、新規は空条件になる。空文字列を指定すると条件を解除する。
条件式・回数条件は既存の制限付きDSLだけを使用し、スクリプトは実行しない。
Watch kindは `change`, `read`, `write`, `readWrite`。
アドレス0..65535、Breakpoint 256件、Watch 64件、Change Watchの4096 byte制限は既存登録処理と共用する。
Trace容量は16..65536。容量を減らすと古い履歴が削られる場合があることを確認画面へ表示する。

停止中は提案時・承認直前の両方でSession ID、同じrunner、revision、同じProjectを検証する。
別のStep、入力、登録変更、Load、Reset、Run等が入れば古い要求を拒否する。
現在のSession ID / revisionは `pgp_get_development_context` のmetadata / debug、またはDebug Contextから取得する。
条件やhitCountの確認には、旧registrations v1ではなくdevelopment contextのdebug節を使う。

実行中のrevisionは命令ごとに進むため、Pauseだけは `sessionRevision` を渡さず、現在のSessionを対象とする。
提案後に同Sessionの実行が進むことを許し、承認時の位置で停止する。要求時のPCを停止位置として保証しない。
同じrunner / Session ID / Projectに加え、Reset・Load試行・動作モード変更の世代も承認時に確認する。
途中でこれらが変わった要求は拒否する。既に停止した場合も自動再開せず失敗を返す。

### 共通処理とStepの影響

Breakpoint / WatchはUIと同じrunner登録メソッドを使用し、Traceも既存設定を使用する。
登録・Traceの反映後はデバッガー表示と編集フォームを更新する。
Pause / StepはMainの既存 `pauseEmulator` / `stepEmulator` を共用し、CPU表示・サイクル数・LCD・音声・停止理由を更新する。
Pauseは既存UI同様、ホストキーと入力キューも解放する。
Stepは命令を1つ実行するため、RAM / I/O書込みやFaultが起こり得ることを承認画面に明示する。
1命令の処理後も連続実行へ移らない。完了後は新しいDebug ContextでCPUと停止理由を確認する。
既にFaultのSessionに対するStepは拒否する。operationのSUCCEEDEDは要求処理の完了であり、命令実行結果がFaultでない保証ではない。

例（値は取得した最新Sessionに置き換える）:

```json
{
  "sessionId": "<current-session-id>",
  "sessionRevision": 42,
  "action": "save_breakpoint",
  "parameters": { "address": 50432, "enabled": true, "condition": "A == 0x41", "hitCondition": ">= 2" }
}
```

Studioで承認後、`pgp_get_debug_operation({"operationId":"<returned-id>"})` と最新debug節を取得する。
Stepや別の登録変更には更新後のrevisionを使い、前のrevisionを使い回さない。
Pauseの例は `{ "sessionId":"<current-session-id>", "action":"pause", "parameters":{} }`。

### 検証と接続更新

当時の自動検証・実接続の区分は[検証履歴](../development/AI_INTEGRATION_HISTORY.md)を参照。

## 16. 作業順12: Studio承認付きLoad / Continue（2026-10-22）

追加4ツールにより公開toolは合計15。既存11ツールの名前・schemaは維持する。

| Tool | 引数 | 動作 |
| --- | --- | --- |
| `pgp_get_build_artifact` | 空object | 成功したBuildのartifactId、projectRevision、サイズを取得 |
| `pgp_propose_load` | sessionId, sessionRevision, projectRevision, artifactId | 停止中SessionへのLoadを提案 |
| `pgp_propose_continue` | sessionId, sessionRevision, projectRevision | 現在PCからの実行再開を提案 |
| `pgp_get_execution_operation` | operationId | 承認待ち・成功・拒否・失敗と対象を取得 |

Loadは内蔵assemblerを使う登録済みUTF-8 BASIC / Assembly Projectが対象。
成功したBuildのメモリ内成果物を保持し、ディスク上の変更されたdmpは読み込まない。
Studio再起動後は再Buildが必要。新しいBuildを開始すると以前の成果物は失効し、
Source・manifest・assembler設定等の内容が変わった成果物も拒否する。
metadataのavailableは現在利用可能なBuildを示し、Load済み状態の証明ではない。

提案時と承認直前に同じProject・runner・Session ID・revision・内容revisionを確認する。
Reset・Load試行・動作モード変更の世代も確認し、古い要求を拒否する。
実行中・Fault・処理中のSessionは対象外。承認はStudio画面のみで行い、履歴は最大32件。
Load前に共通ProgramImageLoaderで全書込みを計画し、BASIC管理ポインタや旧本文の消去を含む
実際の書込み範囲と合計byte数を承認画面へ表示する（最大128範囲）。
既存のProject loaderとUI更新処理を共用し、Load後も停止を維持する。

Continueは現在PCから既存UIと同じ実行を開始する。新しいBuildは必須ではないが、
対応Projectと最新の内容revisionは必要。BASICのRUN / CALL入力や開始アドレスへのジャンプは行わない。
Breakpoint / Watch・Fault・手動Pauseまで実行し、自動の命令数上限は設けない。
operationのSUCCEEDEDは実行開始を示し、プログラム終了を意味しない。
LoadとContinueは別々の承認が必要で、Load後は最新sessionRevisionを取り直す。

手順はBuild承認→artifact取得→最新Session取得→Load提案→Studio承認→operationとDebug Context確認。
必要な停止条件を設定した後、最新Session / 内容revisionでContinueを提案して承認する。
既存設定でenabled_toolsを制限している場合は、最新版Studioを再起動し、MCP画面の
「Codex設定をコピー」で追加4ツールを反映する。

合成Sessionのテストで承認前の無変更、却下・古いrevision・Source変更・再Build・Session交換の拒否、
BASIC書込み計画、ROM範囲拒否、ディスク成果物改変の非使用、停止Load、現在PCからの再開と
Breakpoint停止・CALL復帰を確認。認証付きHTTPとAWT経由の提案・ローカル承認・結果取得も検証する。
日英・広幅／狭幅のLoad / Continue承認画面を隔離描画する。
本番の実ROM SessionへのLoad / Continueとクライアント設定変更は、この実装作業では行っていない。

2026-10-22検証結果：JDK 21で `./gradlew build :core:desktopTest` 成功。
Desktop 406件、失敗0、既存skip 1件。承認画面の日英・広幅／狭幅の隔離描画と
`git diff --check` も成功。この自動検証時点では本番接続での承認往復は未実施（下記追試で確認）。

2026-10-22 実接続追試：サーバーの15ツール公開と成功Buildの利用可能状態を確認。
認証付きMCPでLoad提案→Studio承認→SUCCEEDED、停止維持とrevision更新を確認した。
BASIC本文15 byte・Assembly 87 byte・文字データ60 byte・管理ポインタ4 byteを取得し、
計画どおり合計166 byteの書込み結果を確認。現在PCは0x1F8Fのままで、
Continue提案→Studio承認→SUCCEEDEDと同Sessionのrunning・revision進行を確認した。
この追試は現在PCからの再開であり、BASIC RUN / CALLやHELLO WORLD表示の再実行は行っていない。
クライアントの公開ツール一覧は旧11件のままだったため、追加toolは既存認証ヘルパーを
使って同じMCP endpointへ直接接続した。検証後のPause operationもSUCCEEDEDとなり、同Sessionのpausedを取得して終了した。

## 17. AIからのアドレス案内の既定表記

MCP initializeのinstructionsで、メモリアドレス・PC・開始位置・範囲は原則16進表記を
案内する（16 bitは `0xC500` のように大文字4桁）。実行するソースやキー入力は対象言語・機種の
対応構文を使い、BASICが `0x` や `&H` に対応すると推測しない。10進入力が必要な場合も
「`0xC500`（10進50432、BASIC入力は `CALL 50432`）」のように16進を先に示す。
MCPのJSONアドレス引数は従来どおりintegerで、schemaは変更しない。
AIへの案内方針であり応答書式の強制機構ではない。反映には最新版StudioとMCPの再接続が必要。

## 18. CI追試とOS間パス照合の修正（2026-10-22）

この節は記録時点の内容です。[AI連携の初期構想・実装順・検証履歴](../development/AI_INTEGRATION_HISTORY.md)へ分離しました。

## 19. bank指定Breakpoint（2026-10-30）

`pgp_propose_debug_action`の`save_breakpoint`に任意の`bank`（整数またはnull）を追加しました。
整数は機種のROM切替窓の正規化されたbank番号、nullはbank指定解除、省略は編集時の指定保持です。
新規登録で省略した場合は従来どおりbank指定なしです。Studioでの承認前に登録を変更しません。
切替窓外・範囲外bank・指定が重複する登録は拒否します。取得したSession ID／revisionを使う規則は維持します。

`pgp_get_debug_registrations`および`pgp_get_development_context`のdebug sectionは、
bank指定登録に任意の整数項目`bank`を返します。bankがない登録の形状は従来どおりです。
addressScopeは指定あり登録があれば`logicalWithOptionalRomBank`、なければ`logicalAllBanks`です。
応答schemaVersion 1への任意項目の追加として扱い、既存tool名・必須入力・Debug Contextの形式を維持します。
クライアント側でbankを無視して全bankの停止と解釈しないでください。

設定保存はbankを含む場合v2、含まない場合v1です。両方の読み込みをサポートします。
詳細は[Debuggerのbank指定仕様](../guides/DEBUGGER.md#24-bank指定breakpoint2026-10-30)を参照してください。
MCP adapterとApplication Serviceを通す自動検証で、提案・承認・取得、型エラー・範囲外の拒否、
省略による保持・nullによる解除を確認しました。外部AIクライアントでの実接続受入れは未実施です。

## 20. 同梱stdioブリッジ（2026-11-01）

開発版は`PokecomGOStudio --mcp-stdio [--port <1..65535>]`でstdioブリッジを起動できる。
既定portは8765。GUI、設定読込み、ROM、Sessionの初期化に入る前に分岐する。
既存のlocalhost HTTPサーバーと15ツールを共用し、承認・revision照合の意味を変更しない。
Studioのサーバーは利用者が別途起動する。ブリッジ自身は待受けない。

stdin/stdoutはUTF-8の改行区切りJSON-RPC。通知には応答しない。
要求は64 KiB、応答は1 MiBまで。過大な入力は残りを読み捨て、次の行から処理を続ける。
接続先は`127.0.0.1`の指定portに固定し、プロキシ・HTTPリダイレクトを使わない。
接続timeoutは5秒、応答timeoutは30秒。自動再送・承認・実行操作は追加しない。

既存と同じOS別保存先から、要求ごとに一時Authorizationを読み直す。
通常ファイル、所有者限定のPOSIX権限またはWindows ACL、サイズ、ヘッダー形式を検証する。
認証情報、任意の例外メッセージ、ローカルパスをstdoutやエラー応答へ出力しない。
HTTP失敗、接続不能、認証読込み失敗は要求ID付きのJSON-RPC error `-32050`を返す。
失敗後の状態変更要求は、呼出し側がoperation等を照合してから再提案する。

配布アプリではVS Code、Copilot portable形式、Claude Desktop、Gemini CLI向けの
トークンを含まないstdio設定をコピーできる。既存のCodex HTTP設定は維持する。
設定形式と対応確認範囲は[接続ガイド](../guides/AI_CONNECTION_GUIDE.md)、
検証結果は[接続検証記録](../development/AI_MCP_CONNECTION_VERIFICATION.md)を参照。

## 21. PC-1245機種知識API初版（2026-11-01）

セッション・Project・ROMなしでも、インストール済みの知識を次の3ツールで取得できる。
既存15ツールの名前・schemaは維持する。知識ツールは読み取り専用で、Build・Load・実行を行わない。

| Tool | 引数 | 応答format |
| --- | --- | --- |
| `pgp_search_knowledge` | 必須machineId、任意query／topic／offset／limit | `pgp-knowledge-search` |
| `pgp_get_knowledge` | 必須machineId／id | `pgp-knowledge-item` |
| `pgp_get_example` | 必須machineId／exampleId | `pgp-knowledge-example` |

初版のmachineIdは`pc-1245`のみ。他機種は`UNSUPPORTED_KNOWLEDGE_MACHINE`。
queryは最大256文字、空文字／省略は一覧。空白区切りの全語を大小文字無視で照合する。
topicはbasic／memory／display／assembler／execution／example／instruction。
offsetは0〜1000000（既定0）、limitは1〜16（既定8）。totalとnextOffsetでページングする。
本文を検索対象に含むが、検索結果は本文を含まない要約一覧。該当なしは空配列。

項目ID・exampleIdは最大128文字の文字列。未知IDは`KNOWLEDGE_NOT_FOUND`／`EXAMPLE_NOT_FOUND`。
パスやURLの読込み引数はない。未知キー、null、文字列化した数値、範囲外の値は`INVALID_ARGUMENTS`。
structuredContentはschemaVersion 1、catalogVersion `pc1245-v1`。構造化応答は最大64 KiBで、
超過時は`RESPONSE_TOO_LARGE`。クライアント互換性のため、同じJSONをcontentのtextにも格納する。

項目はsources（リポジトリ相対の参照先）、verification（根拠種別・実機未検証・ROM適用条件）を返す。
本文・作例はclasspath内の固定resourceのみを読む。実行時の任意ファイル・ネットワーク・ROM・Project参照は行わない。
BASICコード表、メモリ既定値、LCD情報、命令メタデータは現行Core／既存機種仕様サービスを共用する。
機種知識のcodeTableはTokenizerの認識情報であり、全BASIC構文や実ROM実行を保証しない。
命令の完全な動作・フラグ・cycle数とROMルーチンABIは未提供として明示する。

作例ID `pc1245-hello-world`はmanifest・BASIC・Assemblyの本文3ファイルと適用条件を返す。
既存テストfixtureをビルド時に同梱し、ROM・生成済みバイナリは含めない。
詳しい項目一覧と情報源は[MCP知識カタログ](knowledge/README.md)を参照。
enabled_toolsを制限するクライアントでは、設定を再コピーして追加3ツールを有効にする。

## 22. 解析用プログラムの取り込み・取得（2026-11-01）

StudioのProgram Analysis画面で利用者が選択・確認・公開した1件のスナップショットを、
新しい読み取り専用tool `pgp_get_analysis_context`で取得する。公開toolは合計19件。
任意ファイルパスのMCP引数、リモートの取り込み・承認、直接Memory書込み、Load・実行は追加しない。
入力・公開手順は[プログラム解析ガイド](../guides/AI_PROGRAM_ANALYSIS.md)を参照。

| section | 引数 | 内容 |
| --- | --- | --- |
| metadata（省略時） | section以外は不要・拒否 | available、対象情報、解析結果の根拠の記載方針 |
| source | targetId、revision必須。offset／limit任意 | 行番号付きソース。offsetは0基準の物理行、limitは行数 |
| disassembly | targetId、revision必須。startAddress／instructionCount任意 | 取り込んだRAMバイト列の線形逆アセンブル |

metadataは未公開時もavailable=falseで取得できる。対象にtargetId、内容と条件のrevision、
machineId、ファイル名（絶対パスなし）、形式、入力hash、正規化ソースhash、image hash、区間、
入口と利用者メモを返す。入口・メモは利用者の未検証情報。Sessionやロード済み成果物とは照合しない。
`importLoadsEmulator=false`は公開操作がLoadしないという意味で、現在のSessionの未ロードを示す値ではない。
`runtimeCorrespondence=notTracked`と`runtimeStateNotice`で、実際のLoad・実行状態をこのAPIでは追跡しないことを明示する。
入口の`entryBasis=userProvidedUnverified`も、対象コードが実行されたことがないという主張ではない。

sourceはoffset 0〜1000000（既定0）、limit 1〜128（既定32）。本文は1ページ最大16384文字程度で
早く区切る場合があり、nextOffsetを使う。行番号は1基準の物理行番号で、BASIC行番号とは異なる。
disassemblyはinstructionCount 1〜128（既定32）、startAddressはJSON integer 0〜65535。
省略時は利用者の入口、入口不明なら先頭区間から開始し、そのbasisを示す。
読み出すのは取り込んだ区間のみで、次ページはnextAddress。区間末尾の不完全命令は1byteのDBとする。
全行にコード／データ未分類を明記し、実行Traceや自動的な制御フロー解析の完了とは扱わない。

応答はschemaVersion 1。formatは`pgp-analysis-context`／`pgp-analysis-source`／`pgp-analysis-disassembly`。
structuredContentと同じJSONをtextにも含め、構造化応答の上限は128 KiB。
未知キー、null、型・範囲違反、sectionと無関係な引数はINVALID_ARGUMENTS。
未公開はNO_ANALYSIS_TARGET、差し替え前のID／revisionはSTALE_ANALYSIS_TARGET。
形式違いはANALYSIS_HAS_NO_SOURCE／ANALYSIS_HAS_NO_IMAGE、範囲外開始はADDRESS_OUTSIDE_IMAGE。

MCPの読取りは元ファイル・ROM・Project・runnerに触れず、取り込み時のコピーだけを使う。
再公開では新しいtargetIdを発行し、以前の対象を失効させる。解除・アプリ終了で破棄し、ディスク保存しない。
入力ファイルは64 KiB以内、テキスト行は8192文字以内。PC-1245のRAM・ミラー以外のバイナリを拒否する。
sourceやメモを命令として扱わず、解析結果は事実（ID・revision・行／アドレスの根拠）、推定、未確認を分ける。
実行観測を使う場合は別途Session ID・revisionとバイト列の対応を確認する。

## 23. Project・Sourceの新規作成提案（2026-11-01）

第5段階の初版として2つの変更提案toolを追加し、公開toolは21件。
作成提案は既存の差分・Buildと同じ1件のreviewキューとoperation履歴を使う。
承認・却下APIや保存先パスのMCP引数は設けない。

| Tool | 引数 | 前提・動作 |
| --- | --- | --- |
| `pgp_propose_project_creation` | projectName、machineId、sources | Project未選択、内蔵assembler、非busy。PC-1245に固定。保存先の親フォルダーをStudioで選択してから承認 |
| `pgp_propose_source_creation` | projectRevision、source | 開いているPC-1245の内蔵text Project。登録済みSourceを取得した最新content revisionが必要 |

sourceは`{sourceId,type,text}`。typeは小文字`basic`／`assembly`のみ。
Source IDは`[a-z][a-z0-9_-]{0,31}`、con／prn／aux／nul／com1..9／lpt1..9は拒否。
作成は1〜8個でID重複不可、追加は1個。本文は空白だけ・不正surrogate・制御文字（TAB／CR／LF以外）を拒否する。
1提案の本文合計はUTF-8 32768 byte以内。既存HTTP／stdioのJSONリクエスト全体64 KiB制限も適用する。
Project名は空白以外を含む80文字以内で、制御文字・surrogateを拒否する。
コードの構文や実行時の意味は作成時に検証せず、既存Build・診断へつなぐ。

新規Projectの例：

```json
{
  "projectName": "Generated HELLO",
  "machineId": "pc-1245",
  "sources": [{"sourceId":"main","type":"basic","text":"10 PRINT \"HELLO\"\n20 END\n"}]
}
```

追加Sourceの例（projectRevisionを実際の値に置換）：

```json
{
  "projectRevision": "<pgp_get_sourceで取得した値>",
  "source": {"sourceId":"routine","type":"assembly","text":"ORG 0xC500\nRTN\n"}
}
```

Projectは選択した親の下に`pgp-generated-<operation識別子の先頭8文字>`を新設する。
IDから`src/<sourceId>.bas`または`.asm`を生成し、buildディレクトリとmanifestも作成して開く。
選択操作では書き込まず、保存先と全Source本文を確認後の承認で初めて作成する。
親フォルダーが差し替わった場合、新設先が既に存在する場合、別Projectを開いた場合は拒否する。
Source追加では既存ID・登録パス（大文字小文字を無視した衝突を含む）・未登録同名ファイル・リンクを拒否。
manifest更新前にrevisionを再確認し、失敗時は作成したファイルだけを後始末する。既存ツリーの再帰削除はしない。

応答は既存の`pgp-development-operation` schemaVersion 1。
kindはCREATE_PROJECT／ADD_SOURCE、statusはPENDING_APPROVAL→APPLIED／REJECTED／FAILED。
`pgp_get_operation`で取得する。APPLIEDはファイル作成・登録の反映で、Build・Load・実行成功ではない。
保存先の絶対パスは応答に含めない。親の選択が必要なことと次の再取得手順は応答にも示す。
構造化応答を使わないクライアント向けに、共通の成功応答content.textにもJSONを含める。

主な拒否コード：INVALID_ARGUMENTS、INVALID_PROJECT_NAME、INVALID_SOURCES、INVALID_SOURCE_ID、
INVALID_TEXT、LIMIT_EXCEEDED、UNSUPPORTED_GENERATION_MACHINE、PROJECT_ALREADY_OPEN、BUSY、BUILTIN_ONLY、
STALE_PROJECT、SOURCE_EXISTS、LINK_NOT_ALLOWED、DESTINATION_REQUIRED、DESTINATION_CHANGED、DESTINATION_EXISTS。
その他のI/O失敗はOPERATION_FAILED等にまとめ、例外中の絶対パスを応答へ出さない。
履歴は既存と同様に最新32操作。サーバー停止時は未承認の作成提案も却下する。

APPLIED後はdevelopment metadataとSourceを再取得し、更新後のprojectRevisionでBuildする。
`buildMachineId`は現在Sessionの影響を受けるため、要求する機種と一致することも確認する。
起動・キー入力・LCD取得・実行上限はこの追加に含まない。操作手順は[新規生成ガイド](../guides/AI_PROGRAM_GENERATION.md)を参照。

## 24. 上限付き実行・キー入力・LCD読取り（2026-11-01）

第5段階の追実装として以下の3ツールを追加。公開ツールは24件。
既存のLoad／Continue APIは変更せず、無制限のContinueと上限付きcontinueを分ける。

| Tool | 引数 | 内容 |
| --- | --- | --- |
| `pgp_propose_bounded_execution` | sessionId、sessionRevision、projectRevision、action、maxInstructions、maxCycles。actionに応じkeys／callAddress | Studio承認後に実行開始。停止中PC-1245・RUNモード・現在のPC-1245内蔵text Projectが必要 |
| `pgp_get_bounded_execution` | operationId | 承認・進捗・停止結果。最大32操作を保持 |
| `pgp_get_display` | sessionId、sessionRevision | 停止中PC-1245のLCDピクセル・シンボル・PNG。Projectは不要 |

actionはrun／call／keys／continue。
runはCLEAR,R,U,N,ENTER、callはCLEAR,C,A,L,L,SPACE,10進アドレスの数字,ENTERを入力する。
callAddressは整数`0xC000..0xC7FF`。たとえば`0xC500`（50432）は`CALL 50432`として入力する。
run／callにkeysを指定する等、actionに不要なフィールドは拒否する。
keysは1〜64要素の配列で、A〜Z、NUM_0〜NUM_9、ENTER、SPACE、CLEAR、BREAK、PLUS、MINUS、DOT、
EQUALS、LEFT、RIGHTのみ。continueはキーなし。すべて通常の入力経路で、直接のPC変更・Reset・Loadは行わない。
RUNモードであることとBASICのコマンド入力待ちは別で、ファームウェアの入力準備状態は自動判定しない。

maxInstructionsは整数1〜1000000、maxCyclesは整数1〜5000000で、両方必須。
エミュレートした命令／サイクルを数え、命令境界で上限を確認する。
命令数は指定数で止まり、サイクル数は最後の1命令の分だけ超過し得る。
同じ命令終了時に両方へ到達した場合はinstructionLimitを返す。
既存のBreakpoint／Watch／Faultを尊重し、同一命令でのWatch／Faultは上限より優先する。
実行は既存のホストtickで小さいバッチに分け、MCPの呼出し処理は全実行の終了を待たない。

提案時と承認時にSession ID・revision、runner identity、debug generation、Project identity・content revision、
RUNモード、非busy、未処理キー／ホスト押下なしを照合する。古い操作は実行しない。
新規作成やBuild、成功成果物のLoadとは別の承認であり、Projectが開いていることは現在RAMとの一致の証明ではない。

operation応答は`pgp-bounded-execution` schemaVersion 1。
statusはPENDING_APPROVAL／REJECTED／RUNNING／SUCCEEDED／INTERRUPTED／FAILED。
キー列、両上限、executedInstructions／executedCycles、inputComplete、stopReason、observedPc／observedRevision、
元のprojectRevisionとprojectStillCurrent、codeを返す。未観測値はnull。
実行中は進捗、終端後はその時点の証拠を保持し、後のStep等で上書きしない。
stopReasonはinstructionLimit／cycleLimit／breakpoint／runToAddress／watch／fault／interrupted。
上限・Breakpoint・Watch停止はSUCCEEDED、手動中断等はINTERRUPTED、FaultはFAILED。
提案の検証失敗もFAILEDになり、codeを返す。

programFinishedは常にnotDetermined。SUCCEEDEDはこの要求の停止で、正常終了ではない。
inputCompleteは予定したキー押下／間隔の送信完了で、ROMが受理・処理したことを示さない。
停止時に未送信キーを破棄して押下中の自動キーを解放する。部分実行の副作用はあり得るので自動再送しない。
手動入力・Pause・Reset・モード変更、Project／Session切替、サーバー停止で実行を中断する。
実行中の手動Loadは上限なしに再開しない。Sourceの外部編集はRAMを更新せず、終端のprojectStillCurrentで照合する。
この値はSourceファイルの一致であり、ロード済みバイトやBASIC終了の証明ではない。

Debug Contextのstopには`{"kind":"executionLimit","limit":"cycleLimit"}`等を追加。
従来のUserPauseと区別し、画面・履歴にも上限停止を表示する。

LCD応答はstructuredContentに`pgp-display` schemaVersion 1とSession／revision、PC、runState、operatingMode、
displayRevision、enabled、characterColumns／characterWidth、dotColumns／dotRows、interCharacterColumnGap、symbols、pixelsを返す。
pixelsは行ごとの`0`／`1`文字列。PC-1245は80列×7行。enabled=falseなら可視ピクセルはすべて0。
contentには同じJSONのtextと`image/png`のbase64画像を含む。PNGは文字間隔を挿入したLCDドット表示で、
シンボルの一覧はJSONで別に返す。文字OCR・ROMバイト・任意のファイル画像は含まない。
実行中はSESSION_RUNNING、IDやrevision不一致はSTALE_SESSION／STALE_REVISIONで拒否し、自動Pauseしない。

主な提案拒否コードはSTALE_SESSION、STALE_REVISION、STALE_PROJECT、SESSION_NOT_PAUSED、BUSY、
PC1245_PROJECT_REQUIRED、RUN_MODE_REQUIRED、INPUT_BUSY、INVALID_ARGUMENTS。
fault詳細は停止中Debug Contextで確認する。承認はStudio UIのみで、MCPに承認APIは追加しない。

知識カタログはpc1245-v2へ更新し、`pc1245.example.numeric-labels`とexampleId `pc1245-numeric-labels`を追加。
本文・manifestを配布resourceへ同梱し、実行時にリポジトリを読まない。
1つの登録済みROM・エミュレーターでの観測例で、任意ROM・実機の保証ではない。
手順・画像は[新規生成ガイド](../guides/AI_PROGRAM_GENERATION.md)、確認範囲は[検証記録](../development/AI_PROGRAM_GENERATION_VERIFICATION.md)を参照。

## 25. PC-1251機種知識の追加（2026-11-02）

知識3ツールのmachineIdは`pc-1245`／`pc-1251`を受け付ける。
その他の機種は`UNSUPPORTED_KNOWLEDGE_MACHINE`。公開ツール数は24のまま。
PC-1245の現catalogVersionは`pc1245-v2`、PC-1251は`pc1251-v1`。

PC-1251はBASICコード表・カタログメモリ配置・LCD文字寸法・実行機能の対応範囲と、
共通assembler／命令メタデータを取得できる。
機種固有IDは`pc1251.basic`／`pc1251.memory`／`pc1251.display`／`pc1251.execution`。
検索・取得は対象機種で絞り込み、他機種のIDはNOT_FOUND。
共通項目のmachineIdsには両機種が入る。

PC-1251向け同梱作例は未提供（検索は空、取得はEXAMPLE_NOT_FOUND）。
新規Project作成・解析ファイル公開・上限実行・LCD取得は引き続きPC-1245専用。
知識追加を、その機種の全工程受入れ済みと解釈しない。
[機種別の情報源と制限](knowledge/PC1251_SCOPE.md)、
[第6段階の検証記録](../development/AI_MACHINE_ROLLOUT_VERIFICATION.md)を参照。

## 26. PC-1251の生成・実行対応（2026-11-02）

25節追加時点から、Project／Source新規作成、上限付き実行、停止中LCD取得の対象に
PC-1251を追加した。知識catalogVersionは`pc1251-v2`。ツール数は24のまま。
Project作成のmachineId enumはpc-1245／pc-1251。承認画面に提案機種を表示し、
承認後もその機種をmanifestへ保存する。現在のエミュレーター機種は変更しない。

上限実行は対応機種かつProjectとSessionの機種一致が必要で、不一致は
MATCHING_SUPPORTED_PROJECT_REQUIRED。
CALLのschema範囲は0xA000〜0xC7FFへ拡張したが、提案時・承認時に
SessionのmemoryLayoutでRAM・writable・ミラーでない入口を検証し、違反時は
CALL_ADDRESS_NOT_RAM。PC-1245で下位RAMが使えるようになるわけではない。
RUN／キー列の承認、revision照合、上限・停止時の入力破棄は従来どおり。

pgp_get_displayはPC-1251の120列×7行、24文字分のピクセルとPNGを返す。
任意RAM・ROM読取りやOCRは行わない。directDrawing公開はPC-1245のみのまま。
解析ファイル公開・PC-1251作例・実ROM全工程は後続。

## 27. PC-1251数値入力作例と実ROM観測（2026-11-02）

catalogVersion `pc1251-v3`、項目`pc1251.example.numeric-labels`、
exampleId `pc1251-numeric-labels`を追加。manifestとBASIC本文の2ファイルを返す。
他機種から取得するとEXAMPLE_NOT_FOUND。作例取得でBuild・実行は行わない。
数値入力の生成からLCDまで、1つのPC-1251 ROMで観測した。
CALLでは途中入力・入力完了と実行観測の差も記録した。
詳細と残る再現性確認は[機種展開記録](../development/AI_MACHINE_ROLLOUT_VERIFICATION.md)。
