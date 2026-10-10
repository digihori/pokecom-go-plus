# Pokecom GO Studio AI Integration

**ステータス:** 読み取り専用Application Service／Debug Context／localhost MCP初版を実装済み
**更新日:** 2026-10-10

## 1. 目的

Pokecom GO Studioを、AIがポケコンプログラムの作成、解析、デバッグを支援できる開発環境にする。
Studioへ特定のチャットUIやAIモデルを埋め込むことから始めるのではなく、Studioが保持するプロジェクト、
Assembler、Debugger、実行中Sessionを、安全な構造化ツールとして外部AIへ公開する。

AIはSC61860や機種固有仕様を記憶だけで推測せず、Studioが返すMachine Definition、Memory Map、
Disassembly、Symbol、Trace、Snapshotを根拠として解析する。

## 2. 想定ユースケース

### 2.1 BASICプログラムの開発

1. ユーザーが自然言語で作りたいプログラムと操作方法を伝える。
2. AIが対応機種、登録済みROM、BASIC方言、使用可能メモリをStudioから取得する。
3. AIがプロジェクトとBASICソースの作成案を提示する。
4. 承認後にソースを作成し、Studioの共通Build処理で検証する。
5. Diagnosticsを基に修正し、Build成功後に承認を得てEmulatorへLoadする。
6. ユーザーがLCDとキーを操作し、問題発生時は変数、表示、入力履歴等をAIと確認する。

BASICだけの機種非限定プロジェクトでは、AIが現在の実行機種向けにBuildし、必要に応じて複数機種で
互換性を検証する。

### 2.2 既存マシン語の解析

1. AIが`.dmp`、Intel HEX、またはRaw Binary import後のアドレス付きimageをStudio経由で検査する。
2. Memory Mapとロード範囲を取得し、StudioのDisassemblerで命令境界を確定する。
3. 制御フロー、CALL関係、メモリアクセス、ROMルーチン、bank切替を分析する。
4. 仮ラベル、コメント、サブルーチン一覧をAssembly Workspaceのプレビューへ提示する。
5. ユーザーの承認後にAssemblyソース、Listing、Symbol Mapへ反映する。

### 2.3 実行中デバッグ

AIはBreakpoint、Watch、Traceを提案し、承認後に設定する。停止時にはCPU Snapshot、Stack、
Disassembly、Memory差分、キー行列、bank状態、直近Traceをまとめて取得し、原因候補と修正差分を提示する。
BRK入力の取りこぼし、スタック不整合、画面破壊、予期しないbank遷移などを代表的な調査対象とする。

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

### 読み取り専用

- `pgp.get_capabilities`
- `pgp.get_active_machine`
- `pgp.get_rom_status`
- `pgp.get_project`
- `pgp.get_diagnostics`
- `pgp.get_debug_snapshot`
- `pgp.disassemble`
- `pgp.read_memory`
- `pgp.get_trace`
- `pgp.get_breakpoints`
- `pgp.get_symbols`

### 状態変更

- `pgp.create_project`
- `pgp.write_source_patch`
- `pgp.build_project`
- `pgp.load_build`
- `pgp.set_breakpoint`
- `pgp.set_watch`
- `pgp.pause`
- `pgp.step`
- `pgp.continue`

ツールの引数と戻り値には`schemaVersion`、`machineId`、SessionまたはProjectのrevisionを含め、古い状態に
基づく操作を拒否できるようにする。Studio UIとMCPは同じApplication Serviceを利用し、別々のBuildや
Debugger実装を持たない。

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

1. Assembly Workspace、Diagnostics、Listing、Symbol Mapを共通サービスとして整備する。
2. Debug SnapshotとTraceをversion付きの構造化データとして定義する。
3. localhost限定の読み取り専用MCPサーバーを実装する。
4. AIクライアントからBASIC作成と静的マシン語解析を行うE2Eシナリオを作る。
5. Breakpoint、Watch、Pause、Stepを承認付きで公開する。
6. ソース差分、Build、Load、Continueを段階的に公開する。
7. 必要性を確認した後に、内蔵AI Providerと認証方式を検討する。

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

初版が公開するtoolは次の二つだけである。

- `pgp_get_capabilities`
- `pgp_get_debug_context`

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
enabled_tools = ["pgp_get_capabilities", "pgp_get_debug_context"]
default_tools_approval_mode = "auto"
```

短期tokenはStudioを再起動すると変わる。credential fileにはHTTP Authorization headerだけをJSONで保持し、
通常ログ、Debug Context、プロジェクト内ファイルには含めない。保存先はmacOSでは
`~/Library/Application Support/PokecomGOStudio/`、Windowsでは`%LOCALAPPDATA%\\PokecomGOStudio\\`、
Linuxでは`$XDG_RUNTIME_DIR/pokecom-go-studio/`（未設定時は`~/.local/state/pokecom-go-studio/`）とする。
