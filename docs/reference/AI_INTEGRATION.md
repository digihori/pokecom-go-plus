# Pokecom GO Studio AI Integration

**ステータス:** 作業順8〜12を実装し、今回のAI連携強化を一区切り。公開MCP toolは15件
**更新日:** 2026-10-22

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
次の機能追加は未確定とし、実利用で得られた不具合や操作性の改善を個別に判断する。
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
enabled_tools = ["pgp_get_capabilities", "pgp_get_debug_context", "pgp_get_debug_registrations", "pgp_get_machine_spec", "pgp_get_development_context", "pgp_get_source", "pgp_propose_source_patch", "pgp_build_project", "pgp_get_operation", "pgp_propose_debug_action", "pgp_get_debug_operation", "pgp_get_build_artifact", "pgp_propose_load", "pgp_propose_continue", "pgp_get_execution_operation"]
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
sessionにはid・revision・machineId・runState、addressScopeには`logicalAllBanks`を返す。
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
