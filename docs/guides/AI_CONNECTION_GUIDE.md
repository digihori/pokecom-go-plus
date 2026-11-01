# StudioのAI接続ガイド

更新日：2026-11-01。Studioのlocalhost MCPへ外部AIクライアントを接続するためのガイドです。
配布版によって利用できるtoolが異なるため、接続後に`pgp_get_capabilities`で確認してください。

## MCPクライアント対応の拡充

Codexを主な開発環境とし、Copilotなどでも接続・操作を検証する方針です。
[開発計画](../development/AI_MCP_EXPANSION_PLAN.md)と
[接続検証記録](../development/AI_MCP_CONNECTION_VERIFICATION.md)を参照してください。

2026-11-01のVS Code 1.143.1での検証では、エディターのSession Target「ローカル」で
CopilotからPGPツールを呼び出せました。「Copilot」実行環境では利用できておらず、
VS Codeのサーバー一覧で15ツールを認識することと、会話からの実行可否は別に確認が必要です。
通常のブラウザ版Copilotや他のクライアントを検証済みとするものではありません。

最初のHTTP検証は短期トークンを使う一時設定によるものです。その後、開発版に
以下の同梱stdioブリッジと設定コピーを追加しました。トークン入り設定を共有Projectや
リポジトリへ保存しないでください。配布版での利用可否は、その版の機能を確認してください。

## 他のMCPクライアント向けstdio接続

1. Studioの配布アプリを起動し、`Debug` → `AI / MCP Server`でサーバーを起動します。
2. 対象クライアントの「stdio設定をコピー」を選びます。
3. コピーした設定を以下のファイルへ、既存エントリーを残してマージします。
4. クライアントで接続を開始し、ツール一覧と`pgp_get_capabilities`の実行を確認します。

| コピーボタン | 設定先 | 形式・確認範囲 |
| --- | --- | --- |
| VS Code | ワークスペースの`.vscode/mcp.json` | `servers`。Copilotの「ローカル」実行環境を確認対象とする |
| Copilot | ワークスペースの`.mcp.json` | `mcpServers`。Copilot実行環境向けの候補設定。実接続は別途確認 |
| Claude Desktop | Claude Desktopの`claude_desktop_config.json` | `mcpServers`。設定生成済み、実接続未確認 |
| Gemini CLI | `.gemini/settings.json` | `mcpServers`。設定生成済み、実接続未確認 |

コピーされるのはStudio実行ファイルの絶対パスと`--mcp-stdio --port <port>`です。
トークンは含みません。クライアントが同梱ブリッジを別プロセスで起動し、ブリッジは各リクエストで
最新の認証ファイルを読み、起動中のStudioへlocalhost HTTPで接続します。
Node.js、Python、システムJavaの追加導入は不要です。ブリッジはStudioの画面やエミュレーターを起動しません。

Studioのサーバーを停止するとブリッジの呼出しは失敗します。同じポートで再起動すれば、
次の呼出しは更新された認証情報を使います。失敗した操作は自動再送しません。
ポートやアプリのインストール先を変えた場合は設定をコピーし直してください。
ソース起動時は同梱実行ファイルを特定できないため、stdio設定のコピーボタンは無効です。

macOSの配布アプリでstdio起動を検証しています。Windows・Linuxの配布ランチャーと、
各AIクライアントの実接続は検証記録で個別に確認します。MCPのJSON設定を生成できることと、
そのクライアントで利用可能なことは区別してください。

既存のCodex向けHTTP接続は引き続き利用できます。以下はCodex向けの設定です。

## サーバーの起動と接続

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

基本の読み取り専用toolは次の五つである。Source / Build、デバッグ操作、Load / Continueの引数・承認条件は[AI連携仕様](../reference/AI_INTEGRATION.md)を参照。

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


## 操作の基本

Project未登録のプログラムは、[プログラム解析画面](AI_PROGRAM_ANALYSIS.md)で選択・公開してから
`pgp_get_analysis_context`で取得できます。読み取り専用の解析用コピーで、Load・実行は行いません。

機種固有の知識は、まず`pgp_search_knowledge({"machineId":"pc-1245"})`で検索し、
返されたIDを`pgp_get_knowledge`で取得してください。出典とverificationの適用範囲を確認します。
作例IDは`pgp_get_example`で本文を取得できます。これらはSession・Project・ROMなしでも利用できます。
初版の範囲と未提供情報は[知識カタログ](../reference/knowledge/README.md)を参照してください。

1. Studioで対象のProjectと機種を開き、必要なソースを登録します。
2. サーバーを起動して、管理画面の設定をAIクライアントへ登録します。
3. capabilities、機種仕様、開発状態を取得して、対象が正しいことを確認します。
4. ソース差分、Build、Load、実行操作を提案したときは、Studioに表示される内容を確認して承認・却下します。
5. operation取得toolで結果を確認し、古いrevisionや失敗を無視して次の操作を進めないようにします。

Loadは停止を維持し、Continueは現在PCから再開します。BASICのRUNやCALLを自動入力するものではありません。
現在は新規Project作成、キー入力、LCD画像取得、直接Memory書込み、外部assemblerのMCP Buildは対象外です。
Debug Context取得のための自動Pauseは行いません。必要な場合は明示的なPause操作を行います。

ROM全体や認証情報をAIへ渡さず、必要な範囲を確認して利用してください。
仕様・上限は[AI連携仕様](../reference/AI_INTEGRATION.md)、過去の確認範囲は[検証履歴](../development/AI_INTEGRATION_HISTORY.md)を参照してください。

新規生成の初版では、承認付きのProject／Source作成からBuildへ進めます。
[作成手順と現在の範囲](AI_PROGRAM_GENERATION.md)を参照してください。
