# Pokecom GO Studio AI Integration

**ステータス:** 構想・設計段階  
**更新日:** 2026-10-07

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

1. AIが`.dmp`、Raw Binary、メモリ上のコードをStudio経由で検査する。
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
