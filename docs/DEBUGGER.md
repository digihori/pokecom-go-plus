# Pokecom GO Studio Debugger

**ステータス:** 基本デバッグ機能を実装済み・UIおよび解析支援機能は継続開発中
**更新日:** 2026-10-14

## 1. 目的

Pokecom GO Studio Debuggerは、SC61860プログラムと機種固有のメモリ動作を観測し、停止位置、CPU状態、
メモリ変化、I/Oアクセス、ROM bankを照合するための開発機能である。

DebuggerはCPUやMemory Busの可変内部状態をUIへ直接公開しない。CoreのSnapshot、メモリアクセスイベント、
物理ROM位置などをDesktop Runnerが収集し、Studioが表示する。基本境界は
[ARCHITECTURE.md](ARCHITECTURE.md#12-debugger-core)、公開Snapshotは[CORE_API.md](CORE_API.md)を参照する。

## 2. 現在の機能

現在のDebuggerウィンドウには次の機能がある。

- CPUレジスタ表示とStep前後の変更項目
- 複数の実行Breakpointと登録一覧・編集・有効切替
- 一時的なRun to address
- 現在PCまたは指定アドレスからのDisassembly
- 機種別Memory Map
- 読み取り専用Memory View
- 複数範囲の値の変化を検出するMemory Change Watch
- 複数範囲のCPUのRead／Writeを検出するMemory Access Watch
- 読み書き／bank履歴の表示・絞込み・現在の表示への移動
- 既定256命令、UIで容量変更可能なInstruction Trace
- CPU、選択メモリ、Traceを含むDebug Checkpoint JSON出力
- PC-1360の命令実行位置に対応する物理ROM component、bank、offsetの記録

Debuggerは`Debug` → `Open Debugger`から独立ウィンドウとして開く。
上部にはRun／Pause／Step、実行状態、次の命令位置（PC）、直近の命令位置（CURRENT_PC）、停止理由を表示する。
本文をスクロールしても上部は残る。極端に高さを縮めた場合は上部自身もスクロールできる。

幅1000dp以上では、左にCPU／Disassembly／Trace／保存操作、右にMemory／Memory Mapを配置し、
左右を個別にスクロールできる。狭いウィンドウでは「実行」と「メモリ」のタブで切り替える。
「登録」タブでブレークポイント／監視を管理する。「履歴」タブは広幅・狭幅のどちらからも開き、読み書き履歴／バンク切替履歴を切り替えて表示する。
タブ切替と広幅／狭幅の配置変更では入力中のアドレス、監視範囲、Trace容量を画面内で保持する。
これはデバッグ設定のファイル保存ではない。

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

Debugger内のRun／Pause／Stepはメイン画面と同じ操作経路を使う。Run／Stepは停止中のみ、Pauseは実行中のみ有効。
Fault時はRun／Stepを無効にし、復旧は既存のReset操作を使用する。
Breakpoint、Run to、メモリ監視、Faultで停止した場合は「停止原因の命令へ」からその命令の論理アドレスを表示できる。
メモリ監視では「監視先のメモリへ」から最初の変更／アクセス先を表示できる。これらの移動は実行を進めない。
「PCを追従」で現在PCの表示へ戻る。自動停止時にはCPU表示も同じ更新で取得する。

PCは次の命令位置、CURRENT_PCは直近に命令を取得した位置であり、監視で停止した命令位置とは表示を区別する。
Faultでは次の命令を実行できることを意味しない。現在行の強調は表示範囲の先頭ではなくPCとの一致で判断する。

## 4. CPU状態

CPUパネルは`PC`と`CURRENT_PC`、`OP`、`DP`、`P`、`Q`、`R`、`Z`、`C`、
内部RAM先頭の`I`、`J`、`A`、`B`、`XL`、`XH`、`YL`、`YH`、`K`、`L`、`M`、`N`、
および`IA`、`IB`、`FO`、`OUTC`を表示する。

Step時は直前と直後の`CpuSnapshot`を比較する。Run後のBreakpoint／Run to／監視／Fault、またはユーザーPauseでは
前回停止時からの変化を比較する。値が変わったレジスタは背景色・太字・下線で強調し、変更前→後の値も表示する。
I／J／A／B等は内部RAM全体をまとめて強調せず、個別のbyteを比較する。表示していない内部RAMやCPU項目の変化は
従来の変更項目表示にも残す。これは値の差分であり、レジスタへのアクセス履歴ではない。

実行中は強調を消し、次の停止まで前回停止時の基準を保持する。Reset、ROM／機種／Session切替、
プログラムのロード／生成キーによる入力後は新しい基準を作り、以前の実行との差分を表示しない。
比較基準（Step前後／前回停止時／基準なし／実行中）は日英で表示する。

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

Memory Viewは1行16byte、16行をHEXとASCIIで表示する。アドレスは16-bit空間内で折り返す。
CPU表示の更新および停止原因からの移動時に同じアドレスの内容も再取得する。現状は読み取り専用で、
メモリ編集UIは持たない。マシン語イメージのロード可否はDebuggerではなく各Emulator Sessionのメモリ規則に
従う。

表示中の256byteは停止時に比較し、変化したHEX値と対応するASCII文字を背景色・太字・下線で強調する。
アクセシビリティ説明にはアドレスと変更前→後の値を含める。同値書込みや、Run中に変化して停止までに元へ戻った値は
強調しない。ページを変えた場合は以前の値を推測せず、そのページの新しい基準を作る。
実行中にページを変えた場合は次の停止で基準を作り、そこでは差分を出さない。

物理ROMのcomponent／bank／offset、またはメモリの物理対応先が変わったbyteは比較から除外して件数を表示する。
バンク切替によって別のROM内容が見えても書込みと混同しない。Coreのイベントは消費せず、監視／Trace／Checkpointとは
別の表示用観測である。検証範囲は[ステップ2記録](DEBUGGER_STEP2_VERIFICATION.md)を参照する。

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

- 既定256命令のring buffer。16から65536命令までUIで変更できる
- 容量超過時は最古の命令を破棄
- resetまたは明示的なClearで消去
- sequence、論理アドレス、命令byte、Disassemblyを保持
- 命令実行前のDP、P、Q、R、D、Carry、Zeroを保持
- 対応可能なアドレスでは物理ROM位置を保持

UIは現在、主要レジスタだけを各Trace行へ表示する。保持している情報と画面へ表示する情報は同一とは限らない。

## 9. ROM bank観測と履歴

PC-1360は論理`0x4000..0x7fff`を、選択中の16KiB ROM bankへ割り当てる。Core Sessionは次を公開する。

- 現在選択中のbank番号
- bank切替イベント
- 論理PCに対応する物理ROM component ID
- component内offset

Instruction TraceとCheckpointでは、bank領域の命令を例えば`bank-3+0123`のような物理位置として識別できる。
内部ROMはbank番号を持たないcomponentとして記録する。

PC-1460（4 bank）とPC-1470U（8 bank）も現在bank、物理ROM位置、bank切替イベントを公開する。
「履歴」→「バンク切替履歴」では現在bank、旧／新bank、セレクタ値、イベント連番を新しい順に表示する。
番号で旧・新bankのどちらかに一致するイベントを絞り込める。非bank機種では非対応と表示する。
bankイベントには命令PCやTrace共通の命令番号がないため、特定の命令へ結び付けて表示しない。
「現在のROM窓へ」は今のbankのメモリ表示へ移動する操作で、過去のbankを選択・復元しない。

「読み書き履歴」は命令PC、アクセス先、R／W、値、イベント連番を新しい順に表示する。
「絞込み」を開くとRead／Writeとアクセス先のHEX範囲を指定できる。命令PCの範囲指定ではない。
空欄は制限なし、不正な範囲はエラーとし、全件へ暗黙に戻さない。命令フェッチも含むが、CPU内部RAMやポートI/Oを網羅しない。
読み書き履歴の記録は従来どおりDebuggerを開いている間に有効。bank履歴はDebuggerを閉じている間もRunnerが収集する。

各履歴は最大4096イベントを保持し、容量超過時は古いイベントを破棄する。保持件数、連番の範囲、破棄の有無を表示する。
Resetで両方を消去する。Debuggerを閉じても保持済みの履歴は消さない。
絞込み・タブ切替は表示だけを変更し、記録、監視、Trace、Debug Context／MCPへ渡す内容を変更しない。
履歴リストは遅延描画し、UI取得はCPU表示の更新周期と手動更新に合わせる。新着時は先頭へ戻り、新しい記録を表示する。

履歴から命令・メモリへ移動できるのは停止中／Fault時。移動先は現在のメモリとbankを使い、過去の状態を復元しない。
説明・絞込みは開閉でき、入力条件は同じウィンドウ内で保持する。検証範囲は[ステップ3記録](DEBUGGER_STEP3_VERIFICATION.md)を参照する。

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

- 狭いDebuggerでは実行側とメモリ側を同時表示できず、タブ切替が必要
- Memory Viewから直接値を編集できない
- 条件付きBreakpointは未実装
- Breakpoint、Watch、Trace設定は永続化しない
- Symbol、Source Map、Assembler Listingとの連携は未実装
- Traceのファイル出力はCheckpoint経由に限られる
- 実機Checkpointとの自動比較ツールは未実装
- 履歴に過去のCPU／メモリ／bankを復元する機能や、bankイベントと命令の厳密な対応付けはない

## 12. UI再構成方針

2026-10-14のステップ1で固定操作・停止情報と左右配置／縮小時タブを実装した。
日英の検証範囲と未確認事項は[ステップ1検証記録](DEBUGGER_STEP1_VERIFICATION.md)を参照する。
ステップ3で読み書き／bank履歴をタブ表示した。以下はTraceと履歴の同時表示・切離し等を含む将来の基本案であり、
全体を実装したものではない。

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
- AIが解析できるversion付きDebug Contextの出力
- localhost MCP経由の読み取り専用Debuggerツール
- AIが提案したBreakpoint、Watch、Step、Continueの承認付き実行

機種固有の観測点を追加する場合も、Debugger UIからMemory Bus内部へ直接アクセスせず、Core SessionのSnapshotまたは
明示イベントとして公開する。

## 14. AI Debug Context

AI連携では画面のスクリーンショットを主入力にせず、機種、停止理由、CPU、bank、Disassembly、Memory Watch、
Breakpoint、直近Trace、Symbol／Source Mapを構造化して渡す。各Contextには`schemaVersion`とSession revisionを
含め、取得後にResetや機種切替が行われた古いContextからの操作を拒否する。

ROM由来データは必要な範囲のDisassemblyまたはbyte列に限定し、ROM全体をDebug Contextへ含めない。
詳細な接続モデルと安全方針は[AI_INTEGRATION.md](AI_INTEGRATION.md)を参照する。

初期のApplication Serviceと`pgp-debug-context` version 1 JSON serializerは実装済みである。Context取得は
停止中またはFault状態に限定し、実行中の場合はSessionを自動停止せずエラーを返す。指定Memoryは合計
4096byteまでとし、ROMおよびROM mirrorの生byteは含めない。Studio UIではMemory範囲、Disassembly件数、
Memory Access／Bank／Trace件数を指定し、JSONをプレビューしてから保存できる。プレビュー欄は独立した
縦スクロールとテキスト選択に対応する。同じApplication Serviceは、localhost限定・Bearer token認証付きの
読み取り専用MCP toolからも利用する。

## 15. 登録管理（ステップ4）

「登録」タブでブレークポイントと監視を一覧表示し、追加・編集・削除・有効／無効を操作する。
編集は停止中に行う。監視は値変更／読み取り／書込み／読み書きを複数登録できる。
逆アセンブル行と同じ登録を使い、無効なブレークポイントを行から再有効化できる。
登録IDは編集・切替で維持する。Resetは登録を保持し、値変更の基準を取り直す。
新しいSessionには引き継がない。保存や条件・ヒット回数・バンク指定は後続の範囲。

全ROMバンクに共通の16bit論理アドレスを使う。ブレークポイントは256件、監視は64件まで。
値変更監視は1件4096バイト以内、有効な範囲の合計も4096バイト以内（重複範囲は別々に数える）。
上限・入力不正はUIで翻訳し、失敗時は既存登録を維持する。
重複した監視への一致は停止情報へ一度だけ載せるが、実際に繰り返されたアクセスは保持する。
読み書き監視の停止を値変更より優先し、その命令の値変更基準も更新する。
MCP tool名・Debug Context schema・停止理由の形式は維持する。
検証範囲は[ステップ4記録](DEBUGGER_STEP4_VERIFICATION.md)を参照する。
