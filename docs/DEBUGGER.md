# Pokecom GO Studio Debugger

**ステータス:** 基本デバッグ機能を実装済み・UIおよび解析支援機能は継続開発中
**更新日:** 2026-10-07

## 1. 目的

Pokecom GO Studio Debuggerは、SC61860プログラムと機種固有のメモリ動作を観測し、停止位置、CPU状態、
メモリ変化、I/Oアクセス、ROM bankを照合するための開発機能である。

DebuggerはCPUやMemory Busの可変内部状態をUIへ直接公開しない。CoreのSnapshot、メモリアクセスイベント、
物理ROM位置などをDesktop Runnerが収集し、Studioが表示する。基本境界は
[ARCHITECTURE.md](ARCHITECTURE.md#12-debugger-core)、公開Snapshotは[CORE_API.md](CORE_API.md)を参照する。

## 2. 現在の機能

現在のDebuggerウィンドウには次の機能がある。

- CPUレジスタ表示とStep前後の変更項目
- 複数の実行Breakpoint
- 一時的なRun to address
- 現在PCまたは指定アドレスからのDisassembly
- 機種別Memory Map
- 読み取り専用Memory View
- 値の変化を検出するMemory Change Watch
- CPUのRead／Writeを検出するMemory Access Watch
- 最大256命令のInstruction Trace
- CPU、選択メモリ、Traceを含むDebug Checkpoint JSON出力
- PC-1360の命令実行位置に対応する物理ROM component、bank、offsetの記録

DebuggerはStudioメイン画面の`Open Debugger`から開く。現状は単一の縦スクロールウィンドウであり、
パネル分割や独立ウィンドウ化は今後の課題である。

## 3. 実行制御と停止理由

Studio Runnerは次の停止理由を区別する。

| 停止理由 | 意味 |
|---|---|
| Reset | Sessionをresetした |
| User Pause | ユーザーが実行を一時停止した |
| Step Complete | 1命令のStepが完了した |
| Breakpoint | PCが登録済みBreakpointへ到達した |
| Run to Address | 一時的な実行先へ到達した |
| Memory Changed | 監視範囲の値を変更した命令が完了した |
| Memory Accessed | 監視範囲をReadまたはWriteした命令が完了した |
| Fault | 未対応opcode等のCore Faultが発生した |

BreakpointはDisassemblyのアドレス行から追加／解除する。複数アドレスを同時に登録できる。同じ位置から
実行を再開したときは、そのBreakpointを1回だけ通過させて即時再停止を避ける。

`Run to`は選択したDisassembly行を一時停止位置にする。永続Breakpointには追加しない。

## 4. CPU状態

CPUパネルは次の値を表示する。

- PC、現在命令PC、opcode、DP
- P、Q、R、D
- ALU、Carry、Zero
- IA、IB、FO、Control、Test port
- 内部RAM

Step時は直前と直後の`CpuSnapshot`を比較し、変更された項目を表示する。これは値の表示差分であり、
レジスタへのアクセス履歴ではない。

## 5. Disassembly

Disassemblyは指定された論理アドレスからSC61860命令を順にdecodeする。既定では現在PCを追従し、任意の
16-bitアドレスへ移動することもできる。

各行には次を表示する。

- 論理アドレス
- 命令byte列
- mnemonicとoperand
- 現在行
- Breakpoint状態

指定範囲はAssembler向けのテキストとして保存できる。現状はSymbol／Source Mapを受け取らないため、
ラベル名や元ソース行は表示しない。

## 6. Memory MapとMemory View

Memory MapはMachine Definitionに登録されたROM、RAM、VRAM、mirror領域を表示する。領域を選択すると
Memory Viewがその開始アドレスへ移動する。

Memory Viewは1行16byteをHEXとASCIIで表示する。アドレスは16-bit空間内で折り返す。現状は読み取り専用で、
メモリ編集UIは持たない。マシン語イメージのロード可否はDebuggerではなく各Emulator Sessionのメモリ規則に
従う。

## 7. Memory Watch

### 7.1 Change Watch

Change Watchは指定範囲のbyte列を命令実行前後で比較し、値が変化した命令で停止する。

- 監視可能範囲は最大4096byte
- 停止時にアドレス、変更前、変更後、実行命令PCを記録する
- Readは検出しない
- 同じ値を書き込むWriteは検出しない

### 7.2 Access Watch

Access WatchはMemory Busが公開するアクセスイベントを使用し、指定範囲に対するCPUアクセスで停止する。

- Readのみ、Writeのみ、Read／Writeのいずれかを選択できる
- 同じ値を書き込むWriteも検出する
- 値が変化しないI/O操作の調査に使用できる
- 検出精度は機種のMemory Busがアクセスイベントを実装している範囲に依存する

Change WatchとAccess Watchは目的が異なる。値の差分を調べる場合はChange Watch、アクセス自体を調べる場合は
Access Watchを使用する。

## 8. Instruction Trace

Instruction Traceは明示的に有効化した場合だけ記録する。有効時は高速なまとめ実行ではなく命令単位で
Sessionを進めるため、通常実行より負荷が高い。

- 最大256命令のring buffer
- 容量超過時は最古の命令を破棄
- resetまたは明示的なClearで消去
- sequence、論理アドレス、命令byte、Disassemblyを保持
- 命令実行前のDP、P、Q、R、D、Carry、Zeroを保持
- 対応可能なアドレスでは物理ROM位置を保持

UIは現在、主要レジスタだけを各Trace行へ表示する。保持している情報と画面へ表示する情報は同一とは限らない。

## 9. PC-1360のROM bank観測

PC-1360は論理`0x4000..0x7fff`を、選択中の16KiB ROM bankへ割り当てる。Core Sessionは次を公開する。

- 現在選択中のbank番号
- bank切替イベント
- 論理PCに対応する物理ROM component ID
- component内offset

Instruction TraceとCheckpointでは、bank領域の命令を例えば`bank-3+0123`のような物理位置として識別できる。
内部ROMはbank番号を持たないcomponentとして記録する。

FO経由のRAM bank処理およびPC-1360K固有の漢字ROM構成は、仕様が確定していないため実装していない。

## 10. Debug Checkpoint

`Save Debug Checkpoint`は、その時点の解析情報を`pgp-debug-checkpoint` version 1 JSONとして保存する。

主な内容は次のとおり。

| 項目 | 内容 |
|---|---|
| machineId | 対象機種 |
| capturedAt | 取得時刻 |
| executedCycles | Runnerの累積実行cycle |
| stopReason | 保存時の停止理由 |
| cpu | CPUレジスタと内部RAM |
| memory | ユーザーが指定した論理アドレス範囲とbyte列 |
| trace | 保存時点のInstruction Trace |

各Trace要素は利用可能な場合に`romComponent`、`romBank`、`romOffset`を含む。CheckpointはSave Stateではなく、
実機結果や別実行との比較に使用する観測データである。Checkpointを読み戻して実行状態を復元する機能はない。

## 11. 現在の制約

- Debugger UIは単一の縦スクロール構成で、複数情報の同時比較が難しい
- Memory Viewから直接値を編集できない
- 条件付きBreakpointは未実装
- Breakpoint、Watch、Trace設定は永続化しない
- Symbol、Source Map、Assembler Listingとの連携は未実装
- Traceのファイル出力はCheckpoint経由に限られる
- 実機Checkpointとの自動比較ツールは未実装
- bank切替履歴専用の表示パネルは未実装

## 12. UI再構成方針

将来のDebuggerウィンドウは、単一の縦配置ではなく次の構成を基本案とする。

```text
┌ CPU registers / stop reason / Run / Pause / Step ─────────┐
├ Disassembly ─────────────────┬ Memory / Memory Map / Watch ┤
│                              │                             │
├──────────────────────────────┴─────────────────────────────┤
│ Instruction Trace | Memory Access | Bank History           │
└────────────────────────────────────────────────────────────┘
```

PC-1360K解析ではDisassembly、Memory、Access History、Bank Historyの同時参照が重要になる。必要に応じて各パネルを
独立ウィンドウへ切り離せる構造も検討する。

## 13. 今後の課題

実装順と完了状態は[ROADMAP.md](ROADMAP.md#phase-5-開発支援機能)を正とする。主な候補は次のとおり。

- 条件付きBreakpoint
- Symbol／Source Mapとソース行表示
- Memory編集と変更履歴
- Trace、Memory Access、Bank Historyのexport
- Checkpoint間および実機Checkpointとの差分表示
- PC-1360Kの漢字ROM／work RAM観測
- Assembly Workspace、Listing、Symbol Mapとの連携

機種固有の観測点を追加する場合も、Debugger UIからMemory Bus内部へ直接アクセスせず、Core SessionのSnapshotまたは
明示イベントとして公開する。
