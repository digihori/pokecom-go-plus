# Studio文言棚卸し・用語表

2026-10-14。今回の対象はStudioの日英UI。以下は実装箇所と分類の索引であり、今後の機能追加を依頼事項として扱わない。

## 用語

| English | 日本語 |
|---|---|
| Project | プロジェクト |
| Build | ビルド |
| Load | ロード |
| Build & Load | ビルドしてロード |
| Assembler / Assemble | アセンブラー／アセンブル |
| Assembly Workspace | アセンブリワークスペース |
| External editor | 外部エディター |
| ROM Library / ROM set | ROMライブラリ／ROMセット |
| Register / Replace / Remove | 登録／置き換え／削除 |
| Debugger / Breakpoint / Watch | デバッガー／ブレークポイント／監視 |
| Run / Pause / Step / Reset | 実行／一時停止／ステップ実行／リセット |
| Character Editor / WAV Tool | ドットエディター／WAVツール |
| Logical / Physical input | 論理入力／物理入力 |
| Memory / Trace | メモリ／トレース |
| Controller / Unassigned | コントローラー／未割当 |

## 分類と移行単位

| 表示領域 | 実装箇所（desktopAppのpackage以下） | 翻訳する内容／残作業 |
|---|---|---|
| メニュー、About、Help | Main.kt | 項目・説明・リンク失敗を移行済み。製品名・URL・著作権表記は原文 |
| メイン、設定 | Main.kt、runner/DesktopExecutionSpeed.kt | ステップ3で速度・入力ラベル・基本状態／通知・外部エディター説明／表示エラーを移行。ROM／Project／Debugger等から渡る機能別通知は後続 |
| ROM管理 | Main.kt、rom/ | ステップ4で登録・置換・削除・family説明・成功失敗通知・型付きROMエラーの表示変換を移行。ZIP／JSON／OS等の原文詳細は翻訳済み概要に添付 |
| Create ROM Set | rom/DesktopRomSetWindow.kt、DesktopRomSetCreator.kt | ステップ4で入力検証・作成例外・選択後の再検証を含め移行。入力ROM保護・保存先確認・パッケージ形式は維持 |
| Project / Build & Load | Main.kt、project/ | ステップ5でツリー・作成・名称変更・追加・更新・成果物・状態・型付きエラーを移行。既定名・manifest・生成ソースを維持 |
| Assembly Workspace | Main.kt、project/DesktopAssemblyWorkspace.kt | ステップ5でプレビュー見出し・ビルド状態・診断・配置説明を移行。生成Listing／Map／DMPと原文ログを維持 |
| 外部Assembler | project/DesktopAssemblerSettingsPanel.kt等 | ステップ5で既知の設定／実行失敗とProject側診断説明を移行。非構造化診断・stdout/stderrの原文詳細を保持 |
| Debugger | Main.kt、debug/ | ステップ7で停止理由、CPU変更項目、Memory/Trace/Watch/Breakpoint、メモリマップ、保存／Debug Contextの説明・通知・エラー・操作説明を移行。CPU名・HEX・命令・JSONは維持。実操作／ダイアログ表示は後続確認 |
| BASIC／メモリ入出力、Quick Assemble | Main.kt、basic/、machine/ | ステップ6で選択タイトル・範囲説明／検証・結果通知・ROM入力の型付きエラー・既知パーサー診断を移行。未知診断・外部ログは説明を添えて原文を保持 |
| WAV Tool | wav/ | ステップ8で画面・状態・保存／確認・波形操作・概要／診断の表示を移行。タブ判定値、原文report、信号／バイト／数値・パスは維持。実操作は後続確認 |
| Character Editor | character/ | ステップ8で画面・書式説明・入力エラー・グリッド操作説明を移行。縮小時は入力欄を折り返す。生成ソース・バイト・書式IDは維持。実操作は後続確認 |
| ゲームパッド | controllerUi、input/DesktopGamepadController.kt | ステップ9で共有画面・保存／利用不可通知を確認し、押下状態の操作説明と名前取得失敗時の表示を補完。図内の英語利用不可はN/A。Androidの日本語既定値、デバイス名・ID・キー刻印・保存値は維持 |
| MCP | Main.kt、mcp/ | ステップ9で接続・初回設定・権限・起動／停止・コピー通知とrequest状態のUIを移行。tool名・コピーする設定・JSON・schema・認証データは維持。実操作は後続確認 |
| アクセシビリティ | 各Compose画面 | contentDescription、semanticsと可視文言以外の操作説明の全体確認は残る |

UI表示はリソースへ移す。保存key、機種ID、JSON field、schema ID、MCP tool名、URL、パス、ソース、予約語、LCD ROM出力、キー刻印、ユーザーデータは翻訳しない。外部ツールの原文ログには必要に応じ翻訳済みのアプリ側説明を添える。

Coreの型付きエラー・codeは保持する。未構造化のreasonを列挙してUI側で説明を変換する作業は未完了。エラー原文を一括翻訳したり、保存用文字列を表示用リソースに置換しない。

共有ControllerSettingsの既存利用元はAndroid MainActivity。省略可能なControllerStrings引数の日本語既定値で既存利用元を維持。共通部にはLocale、ResourceBundle、OS APIを追加していない。

## ステップ1の判断規則（確定）

分類は文字列の言語や見た目ではなく、用途と利用経路で決める。

- **翻訳**: アプリが利用者へ示す操作・説明・状態・確認・進捗・結果・エラー・アクセシビリティ説明。専門用語は下表の日本語表記を使う。
- **原文維持**: 実機・ソース・保存データ・外部仕様と照合する名前や値。表示の前後に付く説明だけを翻訳し、引数で渡した値を変更しない。
- **日本語説明＋原文**: 外部ツール・OS・デバイスの原文診断、未構造化の詳細。アプリの失敗概要を翻訳し、原文詳細は別段落／ログとして保持。Coreの型・codeで判断可能な診断はUI側で翻訳する。
- 同じ文字列が表示と分岐・保存に共用される場合は、表示取得を分離してから翻訳する。翻訳文を比較値、設定key、ファイル名、保存値にしない。
- 引数付き文言は一文を単位にする。件数・機種名・パス・行列位置・容量・アドレスを引数にし、日英の語順を独立させる。HEX／ソース用数値は既存の書式で生成して渡す。
- 可視のキー刻印は維持する。アクセシビリティ説明を追加する場合は、刻印を残しつつ操作の意味を翻訳する。保留中の矢印表現は変更しない。

### 文脈別の表記

| 対象 | 採用する表示／扱い | 理由 |
|---|---|---|
| 実行ボタン／実機モード | 実行／RUN・PRO・RSV | 操作説明と実機刻印を区別 |
| Import / Register / Load | 取り込む／登録／ロード | ファイル解析・ライブラリ保持・メモリ配置を区別 |
| Export / Save | 出力／保存 | 解析情報等の出力と編集中の結果の保存を区別 |
| Remove / Delete / Replace | 削除／削除／置き換え | ROM削除等の影響は確認文で明示。無関係な操作変更はしない |
| Source / Source file | ソース／ソースファイル | ユーザーの内容・ファイル名は原文 |
| Tracked / Untracked | 登録済み／未登録 | Project manifestへの登録状態。Git追跡状態を意味しない |
| Up to date / Changed / Missing | 最新／変更あり／見つかりません | Source・Build・Emulatorの対象を説明文で明示 |
| Listing / Symbol map | リスティング／シンボルマップ | 見出しだけ翻訳。命令・シンボル・成果物本文は維持 |
| Memory dump / Raw binary | メモリダンプ／生バイナリ | 表示だけ翻訳。拡張子・source type保存値は維持 |
| Memory watch / Memory access | メモリ監視／メモリアクセス | 読み取り・書き込みの説明は翻訳、R/Wの略記は維持 |
| Internal RAM / Mirror / Bank | 内部RAM／ミラー／バンク | RAM・ROM・VRAMとレジスター略称、アドレスは維持 |
| Content / Raw / Waveform | 内容／生データ／波形 | タブ表示を翻訳し、内部の分岐値は維持 |
| Encode / Decode / Analyze | エンコード／デコード／解析 | WAVの信号形式名OLD・S1・S2・02・12・62は維持 |
| Sample / Zero crossing / Nibble | サンプル／ゼロクロス／ニブル | 解析説明を翻訳。PCM・Hz・ms・s・ビット・値は既存書式 |
| Fit / Zoom / Pan | 全体表示／拡大・縮小／表示範囲を移動 | 波形操作の意味が分かる表記 |
| MCP server / Loopback / Read-only | MCPサーバー／ループバック／読み取り専用 | MCP、localhost、127.0.0.1等の値は維持 |
| Bearer token / Credential file | Bearerトークン／認証情報ファイル | 表示の説明だけ翻訳。認証ヘッダー・トークン・パスは維持 |
| Logical / Physical | 論理入力／物理入力 | UIラベルを翻訳。設定のenum・保存値は維持 |
| PC-1261 family / PC-1401 family | PC-1261ファミリー／PC-1401ファミリー | familyは説明語。機種名、系列の登録・共用方針は維持 |
| Standard / Unlimited | 標準／無制限 | 速度設定の表示だけ翻訳。ratio・保存値は維持 |
| Character Editor | ドットエディター | ユーザー指定の表記を採用。フォーマットIDは維持 |

### 個別判断と実装時の注意

参照パスは `desktopApp/src/main/kotlin/com/digihori/pgp/desktop/` を基準にする。

| 箇所 | 分類・判断 | 後続ステップ |
|---|---|---|
| Main.ktのメニュー、About／Help、設定主要文言 | 翻訳。移行済みリソースは用語表と整合。製品名・GitHub・著作権本文・テーマID `INSTRUMENT` は維持 | 2・3 |
| Settingsの `code --goto "{file}:{line}"` とAssemblerの `{source}` | コマンド・プレースホルダーは原文。引用方法・省略時の説明だけ翻訳。MessageFormatの波括弧を引用する | 3・5 |
| Main.ktの `New PGP Project`、project/DesktopProjectWorkspaceCreator.ktの既定名・テンプレート | 名前・フォルダー名・manifest・生成ソースは保存データとして維持。作成ダイアログの説明・ラベルだけ翻訳 | 5 |
| Main.ktのProject tree、ProjectRuntimeStatus、ProjectSourceType.displayName | 表示は翻訳。ファイル名・source ID・登録判定・type保存値は維持 | 5 |
| project/DesktopAssemblyWorkspace.ktのrenderListing／renderSymbols／renderDmp | 保存する成果物本文は現行維持。画面の見出し・空結果案内は翻訳。Listing内の生成コメントをUIリソースで変更しない | 5 |
| 同ファイルのrenderMemorySummary／renderMemoryLayout | メモリ配置の説明は翻訳。範囲、HEX、機種ID、ROM/RAM等の略記を保持。呼出し先に保存経路がある場合は表示用を分離 | 5 |
| rom/DesktopRomRegistrationCatalog.ktのlabel | family部分だけ翻訳。1261／1401等の機種名、代表登録ID、共有Libraryは維持 | 4 |
| rom/DesktopRomSetCreator.ktのinputErrorと例外、rom/DesktopRomSetWindow.ktのエラー表示 | アプリ側入力検証は翻訳。OSファイル操作の例外は翻訳済み概要＋原文詳細 | 4 |
| Main.ktの各ファイル選択title、range validation | アプリのタイトル・エラーは翻訳。OS標準ボタンはOS管理。拡張子・選択filter・既定保存名・アドレスは維持 | 4〜7 |
| Main.ktのCoreFault、ROM／Project／BASIC／Memoryのmessage変換 | 型付き分岐の説明を翻訳。実値は引数化。内部のParse.messageやInvalidProgram.reasonは発生元を調べ、既知の型／code変換か原文詳細へ分離 | 4〜7 |
| Main.ktのDebuggerStopReason.displayNameとCheckpoint保存 | **表示／保存共用の要分離箇所**。stopReason表示をJSONへそのまま渡す経路がある。ステップ2でcheckpointDescriptionを分離し、Resetの保存値は従来の英語へ固定。後続の停止理由翻訳でも保存表現を維持する。全状態のJSON比較は後続検証 | 7（必要なら2で先に互換性保護） |
| debug/DesktopDebuggerCheckpointWriter.kt、application/debug/DebugContextJsonWriter.kt、mcp/PgpMcpToolAdapter.kt | JSON field・schema・値の意味・プロトコル診断は維持。Studioに表示する説明と分離。MCP応答のローカライズは今回の対象外 | 7・9 |
| Main.ktのCpuField／memory region label | CURRENT_PC・OP・C・Z・CTRL等は維持。Internal RAM・mirror等の説明語は翻訳。CoreのdisplayNameを一括変更しない | 7 |
| wav/WavToolPanel.ktの `Content`／`Raw`／`Waveform` | **表示／判定共用の要分離箇所**。文字列で状態とwhen分岐を管理している。現在の分岐値を保ち、ボタン表示だけリソース取得する | 8 |
| wav/DesktopWavOperation.ktのsummary・reportとWavToolPanel.ktのreport出力 | **表示／出力共用の要確認箇所**。画面概要は翻訳し、現行レポート・設定記録・生データ出力の内容は維持。説明を翻訳して保存本文まで変えない | 8 |
| wav/WavToolPanel.ktのS1/S2日本語案内 | 英語UIにも表示される直書き。日英の説明へ移行するが、未実装範囲・pcwav案内は現行の意味を保持 | 8 |
| WAV codec diagnostic code／message | codeは維持。アプリ内codecの既知診断をUIで翻訳、range・座標空間・severityの説明も対象。未知の原文は詳細として保持 | 8 |
| character/CharacterEditorPanel.kt、DesktopCharacterEditorModel.kt | ラベル・説明・入力エラーを翻訳。バイト列・BASIC DATA・Assembly DB・書式ID・出力は原文。DotByteTextResult.Failureはmessageだけなので表示変換方法の確認が必要 | 8 |
| controllerUi/ControllerStrings.kt、ControllerSettings.kt | ホスト注入の説明・未割当等は翻訳。日本語既定値はAndroid互換のため維持。デバイス名／ID・L1・ABXY・キー刻印は維持。ステップ2でABXYの位置説明を注入対象に追加し、図内の英語未割当はNone、選択肢はUnassignedとした | 2・9 |
| hostInput/GamepadInput.ktのGamepadKeys.label(null) | 共通の未割当は日本語。StudioではControllerStrings.unassignedを使用済み。共通関数の一括変更はせず、今後の直接呼出しを確認する | 9 |
| Main.ktのMCP手順、接続／request状態、コピー成功通知 | アプリの説明と結果は翻訳。tool名、Authorization、http_headers_helper、config.toml、構成・コピー内容・URL・ポート・トークンは維持 | 9 |
| Main.ktのPocketKeyButton.semantics | 現行contentDescriptionはキー刻印を結合している。刻印を維持し、必要な操作説明は翻訳。アイコン・Canvas・矢印だけのボタンの説明不足も各ステップで確認 | 3・7〜9 |

### 移行済みリソースのレビュー結果

- `Messages.properties`／`Messages_ja.properties`を確認。操作・主要説明の語彙は上記採用表記と整合。今回はリソースの文言を変更していない。
- `Source`と`Source file`、`Import`と`Load`、`Tracked`とGit追跡等を同一語として一括置換しない。
- 「各画面を変更せずにテーマのプリセットを追加できます」等の実装寄りの説明は翻訳済み。後続では日英の意味を揃え、ローカライズを理由に機能・内容を追加しない。文章の製品向け見直しは別の変更として判断する。
- 日本語の長い「アセンブリワークスペース」「ビルドしてロード」は略称を新設せず、ステップ2以降で幅・折返しを確認する。
- 日英key／引数一致の自動テストは既存。棚卸し完了は全画面翻訳完了や実表示確認完了を意味しない。

### ステップ1の完了範囲

対象画面すべての文言所在、共有UI・入力由来の表示、型付きエラー変換、ソース／保存／ログとの共用箇所を確認し、分類規則と用語を確定した。下の全文字列行索引は補助資料として維持する。各ステップで実際の移行対象を絞る際には呼出し経路を再確認する。

今回は棚卸し文書と進捗文書のみ更新。実装・リソース・既存の1470U変更・スキンは変更していない。文書変更のためbuild／Core testsは再実行していない。次はステップ2の基盤・小画面の表示確認。

## 文字列を含む行の索引

ソースの文字列を含む行を抽出した補助索引。UI以外の識別子・データ・ログ書式も含むため、この一覧の全項目を翻訳対象として扱わない。行番号は棚卸し時点の参考値。

### controllerUi/src/commonMain/kotlin/com/digihori/pgp/controller/ui/ControllerSettings.kt

```text
47: Text(devices.find { it.id == selectedId }?.let { "${it.name} (${it.id})" }
53: text = { Text("${device.name} (${device.id})") },
78: it.name.startsWith("NUM_") -> 0
```

### controllerUi/src/commonMain/kotlin/com/digihori/pgp/controller/ui/ControllerStrings.kt

```text
5: val title: String = "ゲームコントローラー",
6: val enable: String = "コントローラー入力を有効にする",
7: val disconnected: String = "未接続",
8: val selectDevice: String = "使用する1台を選択",
9: val instructions: String = "図のボタンを選び、ポケコンのキーを割り当てます。設定中の操作はテスト表示のみです。",
10: val positions: String = "ABXYは標準位置を表示しています。機器の印字が異なる場合は上下左右の位置で選んでください。",
11: val saveHelp: String = "スティックは対象外です。変更は自動保存され、以前の設定を上書きします。",
12: val assignment: (String) -> String = { "$it の割当" },
13: val close: String = "閉じる",
14: val unavailable: String = "利用不可",
15: val unassigned: String = "未割当",
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/Main.kt

```text
246: id = "main",
266: val requestedApi = System.getenv("PGP_RENDER_API")
271: System.setProperty("skiko.renderApi", requestedApi)
275: System.getProperty("skiko.renderApi").isNullOrBlank() &&
276: System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
280: System.setProperty("skiko.renderApi", "SOFTWARE")
285: "DIRECT3D",
286: "OPENGL",
287: "SOFTWARE",
288: "SOFTWARE_FAST",
289: "SOFTWARE_COMPAT",
304: var message by remember { mutableStateOf("Select a PC-1245 legacy 32/64 KiB ROM image.") }
319: var dumpStartAddress by remember { mutableStateOf("C000") }
320: var dumpEndAddress by remember { mutableStateOf("C0FF") }
323: var disassemblyStartAddress by remember { mutableStateOf("0000") }
324: var disassemblyEndAddress by remember { mutableStateOf("1FFF") }
327: var checkpointStartAddress by remember { mutableStateOf("C000") }
328: var checkpointEndAddress by remember { mutableStateOf("C0FF") }
331: var debugContextMemoryStart by remember { mutableStateOf("C000") }
332: var debugContextMemoryEnd by remember { mutableStateOf("C0FF") }
333: var debugContextDisassemblyCount by remember { mutableStateOf("32") }
334: var debugContextAccessHistoryLimit by remember { mutableStateOf("256") }
335: var debugContextBankHistoryLimit by remember { mutableStateOf("256") }
336: var debugContextTraceLimit by remember { mutableStateOf("256") }
370: mutableStateOf(DesktopUiPreferences.externalEditorCommand() ?: System.getenv("PGP_EDITOR").orEmpty())
388: var assemblyStatus by remember { mutableStateOf("Not assembled") }
390: var renamedProjectName by remember { mutableStateOf("") }
393: var newProjectName by remember { mutableStateOf("New PGP Project") }
481: if (file.extension.equals("pgrom", ignoreCase = true)) {
489: File(file.parentFile, "${selectedMachineId.value.replace("-", "").removeSuffix("u")}bank.bin").readBytes()
495: if (reportErrors) showError("Could not read ROM: ${it.message ?: it::class.simpleName}")
528: "ROM loaded automatically and running."
530: "ROM loaded. Press Run to start."
549: if (file.extension.equals("pgrom", ignoreCase = true)) {
553: File(file.parentFile, "${targetMachineId.value.replace("-", "").removeSuffix("u")}bank.bin").readBytes()
562: if (reportErrors) showError("Could not import ROM: ${it.message ?: it::class.simpleName}")
567: if (reportErrors) showError("Could not store ROM: ${installed.message}")
600: showError("Could not remove the stored ${machineId.displayName()} ROM.")
615: message = "Removed the stored ${machineId.displayName()} ROM."
620: is DesktopProjectOpenResult.Failure -> showError(opened.errors.joinToString("\n") { it.message() })
634: assemblyStatus = "Not assembled"
645: .joinToString { "${it.key.displayName()}: ${it.value}" }
651: "Opened project ${opened.workspace.definition.name} ($sourceSummary)."
653: "Opened project ${opened.workspace.definition.name} ($sourceSummary). " +
654: "Register the ${projectMachineId.displayName()} ROM before Build & Load."
671: assemblyStatus = "Not assembled"
674: message = "Project closed. The loaded program is retained."
678: loadedProgramLabel = "Standalone: ${file.name}（${if (additional) "追加ロード・" else ""}プロジェクト外）"
688: selectFile(ownerWindow, "Add Source (.bas, .asm, .dmp)", "*.*")
697: message = "Added ${updated.sources.last().definition.path} to project."
699: onFailure = { showError("Could not add source: ${it.message}") },
709: val skipped = if (result.skippedPaths.isEmpty()) "" else {
710: " Skipped unsupported files: ${result.skippedPaths.joinToString()}."
712: message = "Updated project: ${result.addedCount} added, ${result.removedCount} removed.$skipped"
715: "Could not update project manifest: ${result.message}",
727: check(projectWorkspace === workspace) { "Project changed during assembly; result discarded" }
731: assemblyStatus = "Assembly failed — last successful preview retained"
732: message = "Assembly failed with ${result.diagnostics.size} diagnostic(s)."
739: assemblyStatus = "Assembled preview — $bytes bytes from ${result.sources.size} source(s). Use Build to save outputs."
747: DesktopExternalEditorResult.Opened -> message = "Opened ${file.name}."
749: "Could not open external editor: ${opened.message}",
759: check(projectWorkspace === workspace) { "Project changed during build; result discarded" }
763: assemblyStatus = "Build failed — outputs were not loaded; last successful preview retained"
767: assemblyStatus = "Build failed — last successful preview retained"
772: showError(built.errors.joinToString("\n") { it.message(machineName) })
780: assemblyStatus = "Output save failed — not loaded into the emulator"
781: showError("Could not write build output: ${it.message ?: it::class.simpleName}")
784: check(projectWorkspace === workspace) { "Project changed while saving; build was not loaded" }
788: assemblyStatus = "Built — files saved; this build has not been loaded${output?.let { " — ${it.path}" }.orEmpty()}"
801: assemblyStatus = "Build & Load failed — emulator machine does not match the project"
803: "Project targets ${workspace.definition.machineId.displayName()}, but the loaded " +
804: "ROM is ${activeRunner.machineId.displayName()}.",
809: check(runner === activeRunner && projectWorkspace === workspace) { "Emulator or project changed; build was not loaded" }
813: assemblyStatus = "Files saved — emulator load failed: ${applied.error.message()}"
818: assemblyStatus = "Files saved — emulator load failed: ${applied.error.message()}"
822: loadedProgramLabel = "Project: ${workspace.definition.name}"
827: val summary = "${applied.basicByteCount} BASIC bytes, " +
828: "${applied.memoryByteCount} machine-code bytes in " +
829: "${applied.memorySegmentCount} segments"
830: assemblyStatus = "Built & loaded into ${activeRunner.machineId.displayName()} — $summary"
831: message = "$assemblyStatus. " +
832: "Start it from the pocket-computer keyboard."
855: assemblyStatus = "Assembly / build in progress…"
860: assemblyStatus = "Assembly cancelled — not loaded"
863: } catch (error: Exception) { showError(error.message ?: "Assembly failed") }
879: val file = selectAndRestoreFocus { selectBasicFile(ownerWindow, "Load BASIC source") } ?: return
883: showError("Could not read BASIC source: ${it.message ?: it::class.simpleName}")
899: message = "Loaded ${file.name} directly (${loaded.size} bytes at " +
900: "0x${loaded.startAddress.hex(4)}..0x${loaded.endAddress.hex(4)})."
910: showError("Could not read memory dump: ${it.message ?: it::class.simpleName}")
919: message = "Loaded ${file.name} (${loaded.byteCount} bytes in " +
920: "${loaded.segmentCount} segments)."
939: "BASIC program error at byte ${decoded.error.offset}: ${decoded.error.message}",
942: "BASIC program error at byte ${decoded.offset}: ${decoded.message}",
947: .onSuccess { message = "Saved BASIC source to ${file.name}." }
948: .onFailure { showError("Could not save BASIC source: ${it.message ?: it::class.simpleName}") }
956: val file = selectAndRestoreFocus { selectBasicFile(ownerWindow, "Type BASIC through ROM") } ?: return
960: showError("Could not read BASIC source: ${it.message ?: it::class.simpleName}")
975: "BASIC loading stopped: ${runResult.status}"
977: "Merged ${file.name} through the ${activeRunner.machineId.displayName()} ROM " +
978: "(${loadResult.keys.size} key taps)."
992: is com.digihori.pgp.desktop.project.DesktopSc61AssemblyResult.Failure -> showError(result.message + "\n" + result.log)
996: message = "Assembled ${result.image.byteCount} bytes into ${destination.name}.\n${result.log}"
1002: showError("Could not read assembly source: ${it.message ?: it::class.simpleName}")
1007: "Assembly error at line ${assembled.line}: ${assembled.message}",
1012: .onSuccess { message = "Assembled ${assembled.image.byteCount} bytes into ${destination.name}." }
1013: .onFailure { showError("Could not save assembled output: ${it.message ?: it::class.simpleName}") }
1029: message = "Running: ${executionSpeed.label}."
1039: message = "Paused."
1058: message = "Reset complete."
1072: message = "Executed one instruction (${result.cycles} cycles)."
1087: "Expanded RAM selected. It will apply when a ROM is loaded."
1089: "Hardware RAM selected. It will apply when a ROM is loaded."
1093: "Restarted with expanded PC-1255-size RAM."
1095: "Restarted with ${selectedMachineId.displayName()} hardware RAM."
1175: message = "Audio output unavailable: ${it.message ?: it::class.simpleName}"
1196: message = "Could not check project files: ${it.message ?: it::class.simpleName}"
1204: message = "Could not refresh project tree: ${it.message ?: it::class.simpleName}"
1215: message = "External changes detected: " + changes.joinToString { change ->
1216: "${change.source.definition.path} (${change.kind.name.lowercase()})"
1243: newProjectError = "Could not choose project folder: ${error.message ?: error::class.simpleName}"
1270: uiText("about.menu", ProjectInfo.STUDIO_DISPLAY_NAME),
1274: Item(uiText("action.settings"), enabled = !showSettingsWindow, onClick = { showSettingsWindow = true })
1276: Item(uiText("action.quit", ProjectInfo.STUDIO_DISPLAY_NAME), onClick = onQuit)
1278: Menu(uiText("menu.file")) {
1279: Item(uiText("rom.register"), onClick = { showRomManager = true })
1280: Item(uiText("rom.manage"), onClick = { showRomManager = true })
1281: Item(uiText("rom.create"), enabled = !showCreateRomSetWindow, onClick = { showCreateRomSetWindow = true })
1283: Menu(uiText("menu.project")) {
1284: Item(uiText("project.new"), onClick = {
1288: Item(uiText("project.open"), onClick = ::openProjectFromDisk)
1289: Item(uiText("project.rename"), enabled = projectWorkspace != null, onClick = {
1294: Item(uiText("project.addSource"), enabled = projectWorkspace != null, onClick = ::addProjectSource)
1295: Item(uiText("project.close"), enabled = projectWorkspace != null, onClick = ::closeProject)
1297: uiText("assembly.open"),
1302: uiText("project.buildLoad"),
1306: Item(uiText("project.update"), enabled = projectWorkspace != null, onClick = ::updateProject)
1308: Menu(uiText("menu.program")) {
1310: uiText("basic.load"),
1315: uiText("basic.save"),
1320: uiText("basic.type"),
1326: uiText("memory.load"),
1330: Item(uiText("memory.save"), enabled = runner != null, onClick = {
1334: Item(uiText("assembly.quick"), onClick = ::quickAssemble)
1336: Menu(uiText("menu.tools")) {
1338: uiText("character.title"),
1343: uiText("wav.title"),
1348: Menu(uiText("menu.emulator")) {
1349: Item(uiText("emulator.run"), enabled = runner != null && runnerState == RunnerState.PAUSED, onClick = ::runEmulator)
1350: Item(uiText("emulator.pause"), enabled = runnerState == RunnerState.RUNNING, onClick = ::pauseEmulator)
1351: Item(uiText("emulator.step"), enabled = runner != null && runnerState == RunnerState.PAUSED, onClick = ::stepEmulator)
1352: Item(uiText("emulator.reset"), enabled = runner != null, onClick = ::resetEmulator)
1355: uiText("history.previous"),
1359: uiText("history.recalledPrevious")
1361: uiText("history.empty")
1366: uiText("history.next"),
1370: uiText("history.recalledNext")
1372: uiText("history.noNewer")
1379: uiText("memory.expanded"),
1384: uiText("memory.hardware"),
1390: Menu(uiText("menu.debug")) {
1391: Item(uiText("debug.open"), enabled = !showDebuggerWindow, onClick = { showDebuggerWindow = true })
1392: Item(uiText("debug.saveCheckpoint"), enabled = runner != null && cpu != null, onClick = {
1396: Item(uiText("debug.exportContext"), enabled = runner != null && cpu != null, onClick = {
1409: Item(uiText("mcp.title"), enabled = !showMcpServerWindow, onClick = { showMcpServerWindow = true })
1411: Menu(uiText("menu.help")) {
1412: Item(uiText("help.contents"), enabled = !showHelpWindow, onClick = { showHelpWindow = true })
1414: Item(uiText("help.gettingStarted"), onClick = {
1417: Item(uiText("help.keyboard"), onClick = {
1420: Item(uiText("help.debugger"), onClick = {
1424: Item(uiText("help.github"), onClick = {
1432: id = "about",
1434: title = uiText("about.title", ProjectInfo.STUDIO_DISPLAY_NAME),
1449: Text(uiText("about.version", ProjectInfo.VERSION))
1450: Text(uiText("about.description"))
1456: }) { Text(uiText("about.github")) }
1473: id = "settings",
1475: title = uiText("settings.window", ProjectInfo.STUDIO_DISPLAY_NAME),
1484: Text(uiText("settings.title"), style = MaterialTheme.typography.headlineSmall)
1485: SettingsSection(uiText("settings.appearance")) {
1486: Text(uiText("settings.theme"))
1487: Text("INSTRUMENT", fontFamily = FontFamily.Monospace)
1489: uiText("settings.themeHelp"),
1493: SettingsSection(uiText("settings.speed")) {
1494: Text(uiText("settings.speedHelp"))
1508: Text(uiText("settings.speedNote"),
1511: SettingsSection(uiText("settings.editor")) {
1512: Text(uiText("settings.editorHelp"))
1515: DesktopUiPreferences.saveExternalEditorCommand("")
1516: externalEditorCommand = ""
1517: editorCommandDraft = ""
1519: }) { Text(uiText("settings.osEditor")) }
1521: Text(if (externalEditorCommand.isBlank()) uiText("settings.currentOsEditor")
1522: else uiText("settings.currentEditor", externalEditorCommand), style = MaterialTheme.typography.bodySmall)
1526: label = { Text(uiText("settings.editorCommand")) },
1530: Text("Example: code --goto \"{file}:{line}\". Quote paths containing spaces. " +
1531: "The file path is appended if {file} is omitted.",
1542: }) { Text(uiText("settings.applyEditor")) }
1545: SettingsSection(uiText("settings.assembler")) {
1550: SettingsSection(uiText("settings.keyboard")) {
1551: Text(uiText("settings.keyboardHelp"))
1552: Text(uiText("settings.defHelp"))
1553: Text(uiText("settings.historyHelp"), fontFamily = FontFamily.Monospace)
1555: SettingsSection(uiText("settings.controller")) {
1577: id = "help",
1579: title = uiText("help.window", ProjectInfo.STUDIO_DISPLAY_NAME),
1588: Text(uiText("menu.help"), style = MaterialTheme.typography.headlineSmall)
1589: Text(uiText("help.description"))
1590: HelpLink(uiText("help.gettingStarted"), GETTING_STARTED_URL, onMessage = { message = it })
1591: HelpLink(uiText("help.keyboard"), PROJECTS_HELP_URL, onMessage = { message = it })
1592: HelpLink(uiText("help.debugger"), DEBUGGER_HELP_URL, onMessage = { message = it })
1593: HelpLink(uiText("help.repository"), REPOSITORY_URL, onMessage = { message = it })
1602: id = "character-editor",
1604: title = "${ProjectInfo.STUDIO_DISPLAY_NAME} — Character Editor",
1616: id = "old-wav",
1618: title = "${ProjectInfo.STUDIO_DISPLAY_NAME} — WAV Tool",
1633: id = "debugger",
1635: title = "${ProjectInfo.STUDIO_DISPLAY_NAME} — Debugger",
1652: debuggerStopReason?.let { Text("Debugger: ${it.displayName()}") }
1659: ) { Text("Save Debug Checkpoint") }
1675: ) { Text("Export AI Debug Context") }
1728: id = "mcp-server",
1730: title = "${ProjectInfo.STUDIO_DISPLAY_NAME} — AI / MCP Server",
1739: Text("Read-only localhost MCP server", style = MaterialTheme.typography.headlineSmall)
1741: "Exposes only pgp_get_capabilities and pgp_get_debug_context. " +
1742: "It never pauses, runs, writes memory, changes source, or reads arbitrary files.",
1744: Text("How it works", style = MaterialTheme.typography.titleMedium)
1746: "1. Start server creates a new temporary bearer token.\n" +
1747: "2. Studio writes an Authorization header to an owner-only file outside the project.\n" +
1748: "3. Codex runs the configured http_headers_helper for each connection and reads the current header.\n" +
1749: "4. Studio accepts authenticated requests only from this computer through 127.0.0.1.\n" +
1750: "5. Stop server or quitting Studio invalidates the token and removes its credential file.",
1752: Text("One-time Codex setup", style = MaterialTheme.typography.titleMedium)
1754: "After starting the server, choose Copy Codex config and add it to Codex config.toml. " +
1755: "You do not need to copy the token or update an environment variable after restarting Studio. " +
1756: "Copy the configuration again only if you change the port or move to another OS/user account.",
1762: label = { Text("Local port") },
1770: mcpServerError = "Port must be between 1 and 65535."
1780: }) { Text("Start server") }
1786: }) { Text("Stop server") }
1793: Text("Listening on loopback only")
1794: Text("URL: ${info.url}", fontFamily = FontFamily.Monospace)
1795: Text("Bearer token: ${info.token}", fontFamily = FontFamily.Monospace)
1796: Text("Credential file: ${info.credentialPath}", fontFamily = FontFamily.Monospace)
1802: message = "Copied MCP URL."
1803: }) { Text("Copy URL") }
1806: message = "Copied the temporary MCP token."
1807: }) { Text("Copy token") }
1810: message = "Copied Codex MCP configuration."
1811: }) { Text("Copy Codex config") }
1814: "The credential contains only the temporary Authorization header. It does not contain ROM, " +
1815: "Debug Context, API keys, or project files. If the file cannot be protected for the " +
1816: "current OS user, Studio refuses to start the server.",
1819: Text("Published tools", style = MaterialTheme.typography.titleMedium)
1820: Text("• pgp_get_capabilities\n• pgp_get_debug_context", fontFamily = FontFamily.Monospace)
1821: Text("Last request", style = MaterialTheme.typography.titleMedium)
1824: val target = request.toolName?.let { " / $it" }.orEmpty()
1825: "${request.capturedAt} — ${request.method}$target — " +
1826: if (request.succeeded) "success" else "failed"
1827: } ?: "No authenticated requests yet.",
1837: id = "assembly-workspace",
1839: title = projectWorkspace?.let { "${ProjectInfo.STUDIO_DISPLAY_NAME} — ${it.definition.name} — Assembly" }
1840: ?: "${ProjectInfo.STUDIO_DISPLAY_NAME} — Assembly Workspace",
1852: Text(uiText("project.empty"))
1854: Button(onClick = ::openProjectFromDisk) { Text(uiText("project.open")) }
1858: }) { Text(uiText("project.new")) }
1861: Text(uiText("assembly.title"), style = MaterialTheme.typography.titleLarge)
1864: AssemblyPreviewSection("Assembler log — ${it.source.definition.path}", it.consoleLog)
1872: Text(if (showMemoryLayout) "Hide memory layout" else "Show memory layout")
1874: if (showMemoryLayout) AssemblyPreviewSection("Memory layout", memoryLayoutText)
1880: Text(uiText("assembly.assemble"))
1883: Text(uiText("assembly.build"))
1888: ) { Text(uiText("project.buildLoad")) }
1890: if (assemblyBusy) OutlinedButton(onClick = { assemblyJob?.cancel() }) { Text(uiText("assembly.cancel")) }
1891: Text(uiText("assembly.help"))
1894: !projectIsLoaded -> "Emulator: project not loaded"
1896: "Emulator: loaded in ${runner?.machineId?.displayName().orEmpty()}"
1898: "Emulator: previous version loaded — source changed; use Build & Load to update"
1900: "Emulator: previous version loaded — latest build not loaded; use Build & Load to update"
1901: else -> "Emulator: previous successful load retained — latest result not confirmed loaded"
1909: if (projectIsLoaded) Text(uiText("load.help"))
1910: Text(uiText("project.sources"), style = MaterialTheme.typography.titleMedium)
1915: it.file.extension.equals("asm", ignoreCase = true)
1918: Text("No Assembly sources. Add an .asm file under src, then select Update Project.")
1927: "  ${source.definition.path}" +
1928: if (source.definition.id in changedAssemblySources) " *" else "",
1931: Button(onClick = { openExternalSource(source.file) }) { Text("Open") }
1940: Text("  ${source.relativePath} * (untracked)", fontFamily = FontFamily.Monospace)
1941: Button(onClick = { openExternalSource(source.file) }) { Text("Open") }
1945: Button(onClick = ::updateProject) { Text("Add to Project") }
1947: Text("Build outputs", style = MaterialTheme.typography.titleMedium)
1948: Text(DesktopProjectBuildOutputWriter.outputFiles(workspace).joinToString("\n") {
1949: "  build/${it.name} — ${if (it.isFile) "exists" else "not generated"}"
1951: Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
1952: if (assemblyDiagnostics.isEmpty()) Text("No diagnostics.")
1962: .append(diagnostic.line ?: "?").append(": ").append(diagnostic.message)
1963: diagnostic.sourceText?.let { append("\n  ").append(it) }
1969: }) { Text(if (diagnostic.line == null) "Open source" else "Open at line") }
1973: val assemblyOutput by produceState("Preparing assembly result…", previewSources, memoryLayout) {
1977: appendLine("\nMemory")
1979: appendLine("\nDisassembly")
1981: appendLine("\nListing")
1986: appendLine("Symbol Map")
2019: Text("Machine")
2025: if (displayedMachine in installedRomMachines) " ▾"
2026: else " — ROM not registered ▾",
2039: else "${definition.displayName} — ROM not registered",
2050: text = { Text(uiText("rom.manage")) },
2058: Text("ROM: ${if (runner?.machineId in installedRomMachines) "registered" else loadedRomName ?: "not loaded"}")
2059: projectWorkspace?.let { Text("Open project: ${it.definition.name}") }
2066: ) { Text(if (runnerState == RunnerState.RUNNING) uiText("emulator.pause") else uiText("emulator.run")) }
2067: Button(enabled = runner != null, onClick = ::resetEmulator) { Text(uiText("emulator.reset")) }
2071: ) { Text(uiText("emulator.step")) }
2074: "pc-1245", "pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261",
2082: ) { Text("RUN") }
2086: ) { Text("PRO") }
2092: ) { Text("RSV") }
2117: Text(uiText("menu.project"), style = MaterialTheme.typography.titleMedium)
2121: "${workspace.definition.machineId.displayName()}  •  " +
2127: "Changed: ${changedProjectSources.joinToString()}",
2131: } ?: Text(uiText("project.empty"))
2139: ) { Text(uiText("project.buildLoad"), maxLines = 1, softWrap = false) }
2143: ) { Text("Update", maxLines = 1, softWrap = false) }
2147: ) { Text(uiText("assembly.title"), maxLines = 1, softWrap = false) }
2164: wavToolInput = WavToolInput(file, "Project: ${workspace.definition.name}", workspace.definition.machineId, System.nanoTime())
2169: } ?: Text("Project files are loading…")
2220: "（現在のプロジェクト外）" else "",
2246: append("${runnerState.name}  •  $executedCycles cycles")
2247: if (requestedToneHz != 0) append("  •  $requestedToneHz Hz")
2249: append("  •  ${projectRuntimeStatus.displayName}")
2250: if (changedProjectSources.isNotEmpty()) append("  •  Source changed")
2252: debuggerStopReason?.let { append("  •  ${it.displayName()}") }
2264: title = { Text("ROM Library") },
2270: Text("Registered ROMs are copied into the application data folder and remain available when the original files are moved or deleted.")
2286: "Registered and verified" +
2287: (metadata?.let { " — ${it.sourceName}" } ?: "")
2289: "ROM not registered"
2295: Text(if (installed) "Replace…" else "Register…")
2299: Text("Remove")
2308: TextButton(onClick = { showRomManager = false }) { Text(uiText("common.close")) }
2318: "Remove ${com.digihori.pgp.desktop.rom.DesktopRomRegistrationCatalog.label(machineId)} ROM?",
2321: text = { Text("The application-managed copy will be deleted. The original ROM file is not changed.") },
2326: }) { Text("Remove") }
2329: TextButton(onClick = { pendingRomRemoval = null }) { Text(uiText("common.cancel")) }
2337: title = { Text("Rename Project") },
2343: label = { Text("Project name") },
2346: Text("Folder: ${DesktopProjectWorkspaceCreator.folderName(renamedProjectName)}")
2359: loadedProgramLabel = "Project: ${updated.definition.name}"
2363: message = "Renamed project to ${updated.definition.name}."
2367: }) { Text("Rename") }
2370: TextButton(onClick = { showRenameProjectDialog = false }) { Text(uiText("common.cancel")) }
2378: title = { Text("Create Pokecom GO Studio Project") },
2381: Text("Machine: ${selectedMachineId.displayName()}")
2388: label = { Text("Project name") },
2391: Text("Starter sources")
2408: Text("Existing files will not be overwritten.")
2415: newProjectError = "Project name must not be blank."
2419: newProjectError = "Select at least one starter source."
2425: }) { Text("Create in Folder…") }
2428: TextButton(onClick = { showCreateProjectDialog = false }) { Text(uiText("common.cancel")) }
2436: title = { Text("Save PGP Memory Dump") },
2439: Text("Enter an inclusive 16-bit hexadecimal address range.")
2443: label = { Text("Start address") },
2449: label = { Text("End address") },
2460: start == null -> dumpRangeError = "Start address must be 0000..FFFF."
2461: end == null -> dumpRangeError = "End address must be 0000..FFFF."
2462: end < start -> dumpRangeError = "End address must not be less than start address."
2472: message = "Saved ${snapshot.size} bytes " +
2473: "(0x${start.hex(4)}..0x${end.hex(4)}) to ${destination.name}."
2476: message = "Could not save memory dump: ${it.message ?: it::class.simpleName}"
2480: }) { Text(uiText("common.save")) }
2483: TextButton(onClick = { showSaveMemoryDumpDialog = false }) { Text(uiText("common.cancel")) }
2491: title = { Text("Save Disassembly") },
2494: Text("Enter an inclusive 16-bit hexadecimal address range.")
2498: label = { Text("Start address") },
2504: label = { Text("End address") },
2515: start == null -> disassemblyRangeError = "Start address must be 0000..FFFF."
2516: end == null -> disassemblyRangeError = "End address must be 0000..FFFF."
2517: end < start -> disassemblyRangeError = "End address must not be less than start address."
2530: message = "Saved $instructionCount disassembly lines " +
2531: "(0x${start.hex(4)}..0x${end.hex(4)}) to ${destination.name}."
2533: message = "Could not save disassembly: ${it.message ?: it::class.simpleName}"
2537: }) { Text(uiText("common.save")) }
2540: TextButton(onClick = { showSaveDisassemblyDialog = false }) { Text(uiText("common.cancel")) }
2548: title = { Text("Save Debug Checkpoint") },
2551: Text("Select the inclusive memory range to include (maximum 4096 bytes).")
2555: label = { Text("Start address") },
2561: label = { Text("End address") },
2572: start == null || end == null -> checkpointRangeError = "Addresses must be 0000..FFFF."
2573: end < start -> checkpointRangeError = "End address must not be less than start address."
2575: checkpointRangeError = "A checkpoint range may contain at most 4096 bytes."
2595: checkpointRangeError = "Could not capture checkpoint: ${it.message ?: it::class.simpleName}"
2603: .onSuccess { message = "Saved debug checkpoint to ${destination.name}." }
2605: message = "Could not save debug checkpoint: ${it.message ?: it::class.simpleName}"
2609: }) { Text(uiText("common.save")) }
2612: TextButton(onClick = { showSaveCheckpointDialog = false }) { Text(uiText("common.cancel")) }
2620: title = { Text("Export AI Debug Context") },
2626: Text("Only the explicit non-ROM memory range below is included. The running session is never paused automatically.")
2631: label = { Text("Memory start") },
2638: label = { Text("Memory end") },
2643: Text("Leave both memory fields empty to omit memory bytes.")
2648: label = { Text("Disassembly instructions") },
2655: label = { Text("Trace entries") },
2664: label = { Text("Memory access entries") },
2671: label = { Text("Bank history entries") },
2679: Text("Preview (${preview.size} bytes)")
2711: debugContextError = "Enter both memory addresses or leave both empty."
2718: debugContextError = "Memory addresses must be an increasing range from 0000 to FFFF."
2729: debugContextError = "Instruction and history limits must be decimal integers."
2735: debugContextError = "No active emulator session is available."
2757: }) { Text("Preview") }
2767: message = "Saved AI Debug Context to ${destination.name}."
2770: debugContextError = "Could not save Debug Context: ${it.message ?: it::class.simpleName}"
2772: }) { Text("Save JSON") }
2776: TextButton(onClick = { showDebugContextDialog = false }) { Text(uiText("common.close")) }
2784: title = { Text("ROM Error") },
2787: TextButton(onClick = { errorDialogMessage = null }) { Text(uiText("common.ok")) }
2797: fun register(index: Int): String = ram?.getOrNull(index)?.toInt()?.and(0xff)?.hex(2) ?: "--"
2800: "PC=----  CURRENT_PC=----  OP=--  DP=----  P=--  Q=--  R=--  Z=-  C=-",
2801: "I=--  J=--  A=--  B=--  XL=--  XH=--  YL=--  YH=--",
2802: "K=--  L=--  M=--  N=--  IA=--  IB=--  FO=--  OUTC=--",
2806: "PC=${snapshot.programCounter.hex(4)}  CURRENT_PC=${snapshot.currentProgramCounter.hex(4)}  " +
2807: "OP=${snapshot.opcode.hex(2)}  DP=${snapshot.dataPointer.hex(4)}  " +
2808: "P=${snapshot.p.hex(2)}  Q=${snapshot.q.hex(2)}  R=${snapshot.r.hex(2)}  " +
2809: "Z=${snapshot.zero.bit()}  C=${snapshot.carry.bit()}",
2810: "I=${register(0x00)}  J=${register(0x01)}  A=${register(0x02)}  B=${register(0x03)}  " +
2811: "XL=${register(0x04)}  XH=${register(0x05)}  " +
2812: "YL=${register(0x06)}  YH=${register(0x07)}",
2813: "K=${register(0x08)}  L=${register(0x09)}  M=${register(0x0a)}  N=${register(0x0b)}  " +
2814: "IA=${snapshot.ia.hex(2)}  IB=${snapshot.ib.hex(2)}  " +
2815: "FO=${snapshot.fo.hex(2)}  OUTC=${snapshot.control.hex(2)}",
2820: Text("CPU", style = MaterialTheme.typography.labelLarge)
2824: "Changed: ${changedFields.joinToString { it.displayName }}",
2840: var addressText by remember(runner) { mutableStateOf("") }
2855: Text("Disassembly", style = MaterialTheme.typography.labelLarge)
2866: label = { Text("Address (hex)") },
2877: }) { Text("Go") }
2882: addressText = ""
2885: ) { Text("Follow PC") }
2889: ) { Text("Save range…") }
2892: Text("No CPU state.", fontFamily = FontFamily.Monospace)
2895: val marker = if (index == 0) ">" else " "
2906: ) { Text(if (address in breakpoints) "●" else "○") }
2910: ) { Text("Run to") }
2912: "$marker${address.hex(4)}  " +
2913: "${line.byteText.padEnd(DISASSEMBLY_BYTE_COLUMN_WIDTH)}  ${line.instructionText}",
2951: Text("Memory", style = MaterialTheme.typography.labelLarge)
2962: label = { Text("Start address (hex)") },
2977: }) { Text("Go") }
2983: ) { Text("Previous") }
2989: ) { Text("Next") }
2993: ) { Text("Refresh") }
2996: Text("No memory available.", fontFamily = FontFamily.Monospace)
3000: "${line.address.hex(4)}: ${line.hexText}  |${line.asciiText}|",
3032: Text("Instruction trace", style = MaterialTheme.typography.labelLarge)
3040: ) { Text(if (enabled) "Disable" else "Enable") }
3047: ) { Text(uiText("common.clear")) }
3054: label = { Text("Capacity") },
3073: ) { Text(uiText("common.apply")) }
3075: "${runner?.instructionTrace()?.size ?: 0}/" +
3076: "${runner?.instructionTraceCapacity() ?: DesktopEmulatorRunner.DEFAULT_INSTRUCTION_TRACE_CAPACITY} " +
3077: "instructions",
3081: !enabled && entries.isEmpty() -> Text("Tracing is disabled.", fontFamily = FontFamily.Monospace)
3082: entries.isEmpty() -> Text("No instructions recorded yet.", fontFamily = FontFamily.Monospace)
3085: val bytes = instruction.bytes.joinToString(" ") { it.hex(2) }
3087: "#${entry.sequence.toString().padStart(6)}  " +
3088: "${instruction.address.hex(4)}  ${bytes.padEnd(DISASSEMBLY_BYTE_COLUMN_WIDTH)}  " +
3089: "${Sc61860InstructionFormatter.format(instruction).padEnd(14)}  " +
3090: "DP=${entry.dataPointer.hex(4)} P=${entry.p.hex(2)} " +
3091: "C=${entry.carry.bit()} Z=${entry.zero.bit()}" +
3093: "  ROM=${location.componentId.value}+${location.offset.hex(4)}"
3094: } ?: ""),
3107: var startText by remember(runner) { mutableStateOf("C000") }
3108: var endText by remember(runner) { mutableStateOf("C0FF") }
3117: Text("Memory change watch", style = MaterialTheme.typography.labelLarge)
3128: label = { Text("Start (hex)") },
3139: label = { Text("End (hex)") },
3150: start == null || end == null -> error = "Addresses must be 0000..FFFF."
3151: end < start -> error = "End address must not be less than start address."
3153: error = "A watched range may contain at most 4096 bytes."
3161: ) { Text("Watch") }
3168: ) { Text(uiText("common.clear")) }
3170: activeRange?.let { Text("Watching 0x${it.first.hex(4)}..0x${it.last.hex(4)}") }
3176: start == null || end == null -> error = "Addresses must be 0000..FFFF."
3177: end < start -> error = "End address must not be less than start address."
3185: Button(enabled = runner != null, onClick = { setAccessWatch(true, false) }) { Text("Watch Read") }
3186: Button(enabled = runner != null, onClick = { setAccessWatch(false, true) }) { Text("Watch Write") }
3187: Button(enabled = runner != null, onClick = { setAccessWatch(true, true) }) { Text("Watch R/W") }
3194: ) { Text("Clear R/W") }
3196: activeAccessRange?.let { Text("Watching CPU access at 0x${it.first.hex(4)}..0x${it.last.hex(4)}") }
3199: "Stops when a value changes; read access and same-value writes are not detected.",
3215: Text("Memory map", style = MaterialTheme.typography.labelLarge)
3219: regionsById[id]?.displayName?.let { " → $it" }
3222: "${region.startAddress.hex(4)}–${region.endAddressInclusive.hex(4)}  " +
3223: "${region.displayName} [${region.kind.displayName}]$mirrorTarget",
3233: MachineMemoryRegionKind.ROM -> "ROM"
3234: MachineMemoryRegionKind.RAM -> "RAM"
3235: MachineMemoryRegionKind.DISPLAY -> "VRAM"
3236: MachineMemoryRegionKind.MIRROR -> "mirror"
3241: CpuField.CURRENT_PC -> "CURRENT_PC"
3242: CpuField.OPCODE -> "OP"
3243: CpuField.CARRY -> "C"
3244: CpuField.ZERO -> "Z"
3245: CpuField.CONTROL -> "CTRL"
3246: CpuField.INTERNAL_RAM -> "Internal RAM"
3251: DebuggerStopReason.Reset -> uiText("emulator.reset")
3252: DebuggerStopReason.UserPause -> "Paused by user"
3253: DebuggerStopReason.StepComplete -> "Step complete"
3254: is DebuggerStopReason.Breakpoint -> "Breakpoint at 0x${address.hex(4)}"
3255: is DebuggerStopReason.RunToAddress -> "Run to address reached at 0x${address.hex(4)}"
3258: "${change.address.hex(4)}:${change.before.hex(2)}→${change.after.hex(2)}"
3260: val remaining = if (changes.size > 4) " (+${changes.size - 4} more)" else ""
3261: "Memory changed after 0x${instructionAddress.hex(4)}: $preview$remaining"
3265: val kind = if (access.kind == MemoryAccessKind.READ) "R" else "W"
3266: "$kind:${access.address.hex(4)}=${access.value.hex(2)}"
3268: val remaining = if (accesses.size > 4) " (+${accesses.size - 4} more)" else ""
3269: "Memory access by 0x${instructionAddress.hex(4)}: $preview$remaining"
3271: is DebuggerStopReason.Fault -> "Fault: ${fault.message()}"
3275: DebugContextError.SessionRunning -> "Pause the emulator before capturing a Debug Context."
3278: "ROM bytes are not included automatically. Select a RAM or display-memory range instead."
3286: inputOwner: String = "screen:${cap.key.name}",
3304: label = "software key color",
3313: label = "software key content color",
3318: label = "software key offset",
3323: label = "software key elevation",
3355: val lowerLabel = if (cap.shiftedLabel != null) cap.basicLabel.orEmpty() else ""
3363: contentDescription = listOf(cap.primaryLabel, upperLabel, lowerLabel).filter(String::isNotEmpty).joinToString(", ")
3417: MachineKeyboardLayout.PC_1470U, MachineKeyboardLayout.PC_1460, MachineKeyboardLayout.PC_1450, MachineKeyboardLayout.PC_1401 -> error("PC-1401 panels are rendered separately")
3446: inputOwner = "screen:$rowIndex:${cap.column}:${cap.key.name}",
3473: Text("Host input", style = MaterialTheme.typography.labelMedium)
3474: HostKeyInputModeButton("Logical", mode == HostKeyInputMode.LOGICAL) {
3477: HostKeyInputModeButton("Physical", mode == HostKeyInputMode.PHYSICAL) {
3565: com.digihori.pgp.core.api.DisplaySymbol.MEMORY -> "M"
3566: com.digihori.pgp.core.api.DisplaySymbol.PARENTHESES -> "( )"
3567: com.digihori.pgp.core.api.DisplaySymbol.KANA_SMALL -> "小"
3568: com.digihori.pgp.core.api.DisplaySymbol.BATTERY -> "BATT"
3577: return selectFile(owner, "Open ${machineId.displayName()} ROM", "${machineId.value}.pgrom")
3580: private fun selectBasicFile(owner: Frame, title: String): File? = selectFile(owner, title, "*.bas")
3584: "Choose Pokecom GO Studio Project Folder",
3589: "Choose Folder for New Pokecom GO Studio Project",
3593: if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
3609: val property = "apple.awt.fileDialogForDirectories"
3611: System.setProperty(property, "true")
3627: private fun selectMemoryDumpFile(owner: Frame): File? = selectFile(owner, "Load PGP Memory Dump", "*.dmp")
3629: private fun selectAssemblyFile(owner: Frame): File? = selectFile(owner, "Assemble SC61860 source", "*.asm")
3633: title = "Save PGP Memory Dump",
3634: suggestedFile = "memory.dmp",
3640: title = "Save SC61860 Disassembly",
3641: suggestedFile = "disassembly.asm",
3647: title = "Save assembled PGP Memory Dump",
3648: suggestedFile = "program.dmp",
3654: title = "Save Pokecom GO Studio Debug Checkpoint",
3655: suggestedFile = "checkpoint.pgpdebug.json",
3661: title = "Save Pokecom GO Studio AI Debug Context",
3662: suggestedFile = "debug-context.pgpdebug.json",
3670: private fun codexMcpConfiguration(info: DesktopMcpServerInfo): String = """
3672: url = "${info.url}"
3673: http_headers_helper = "${tomlEscape(info.headerHelperCommand)}"
3674: enabled_tools = ["pgp_get_capabilities", "pgp_get_debug_context"]
3675: default_tools_approval_mode = "auto"
3676: """.trimIndent()
3679: .replace("\\", "\\\\")
3680: .replace("\"", "\\\"")
3684: title = "Save BASIC source",
3685: suggestedFile = "program.bas",
3779: check(Desktop.isDesktopSupported()) { uiText("help.unsupported") }
3781: check(desktop.isSupported(Desktop.Action.BROWSE)) { uiText("help.noBrowser") }
3783: }.exceptionOrNull()?.let { uiText("help.failed" , it.message ?: it::class.simpleName) }
3831: supportsDescription = it.file.extension.lowercase() in setOf("bas", "asm", "dmp"),
3846: Text("Project files", style = MaterialTheme.typography.titleMedium)
3850: ) { Text("Edit") }
3854: enabled = selected?.let { it.file.isFile && it.file.extension.lowercase() in setOf("bas", "dmp") } == true,
3856: ) { Text("Create WAV…") }
3867: ProjectTreeGroup("Tracked", tracked, selectedPath, onSelected)
3868: ProjectTreeGroup("Untracked", untracked, selectedPath, onSelected)
3891: Text("Source information", style = MaterialTheme.typography.titleSmall)
3896: selected == null -> "Select a source file."
3897: !selected.supportsDescription -> "Source information is not available for this file type."
3898: description.isBlank() -> "No leading # description."
3921: Text("$name (${files.size})")
3924: "  ${source.path}",
3944: Text("Assembly result", style = MaterialTheme.typography.titleMedium)
3945: TextButton(onClick = { copyToClipboard(content) }) { Text("Copy all") }
3948: "No assembly result yet. Select Assemble to preview the result."
3975: Text(line.ifEmpty { " " }, style = style, softWrap = false, maxLines = 1)
3996: content.ifBlank { "(empty)" },
4003: DesktopBasicLoadError.InvalidUtf8 -> "BASIC source is not valid UTF-8."
4005: "BASIC text error at ${error.line}:${error.column}: ${error.message}"
4008: is Pc1245RomInputUnsupported.Character -> "character '${value.value}'"
4009: is Pc1245RomInputUnsupported.SpecialSymbol -> "symbol ${value.value}"
4010: is Pc1245RomInputUnsupported.RawByte -> "raw byte 0x${value.value.hex(2)}"
4012: "$machineName ROM input does not support $detail at ${error.line}:${error.column}."
4015: "BASIC loading is not available for $machineName yet."
4020: "Emulation stopped: unsupported opcode 0x${opcode.hex(2)} at PC=0x${address.hex(4)}."
4024: DesktopBasicProgramCompileError.InvalidUtf8 -> "BASIC source is not valid UTF-8."
4026: "BASIC text error at ${error.line}:${error.column}: ${error.message}"
4028: "$machineName OLD BASIC error at ${error.line}:${error.column}: ${error.message}"
4030: "$machineName S1 BASIC error at ${error.line}:${error.column}: ${error.message}"
4032: "$machineName S2 BASIC error at ${error.line}:${error.column}: ${error.message}"
4034: "BASIC compilation is not available for $machineName yet."
4039: "Invalid BASIC memory pointers: start=0x${startAddress.hex(4)}, end=0x${endAddress.hex(4)}."
4041: "BASIC program is too large: $size bytes (capacity $capacity bytes)."
4043: "Invalid BASIC program at byte $offset: $reason"
4045: "BASIC program memory access is not implemented for ${machineId.value}."
4049: DesktopMemoryDumpLoadError.InvalidUtf8 -> "Memory dump is not valid UTF-8."
4051: PgpMemoryDumpError.Empty -> "Memory dump contains no data."
4053: "Memory dump error at ${detail.line}:${detail.column}: ${detail.message}"
4055: "Memory dump line ${detail.line} exceeds address 0xFFFF."
4057: "Memory dump line ${detail.line} overlaps line ${detail.previousLine} at 0x${detail.address.hex(4)}."
4062: is DesktopProjectOpenError.CouldNotReadManifest -> "Could not read project: $message"
4063: DesktopProjectOpenError.InvalidUtf8 -> "Project manifest is not valid UTF-8."
4064: is DesktopProjectOpenError.InvalidManifest -> errors.joinToString("; ") { it.message() }
4068: is DesktopProjectCreateError.DestinationExists -> "Project folder already exists: $path"
4069: is DesktopProjectCreateError.CouldNotCreate -> "Could not create project: $message"
4073: is ProjectManifestError.InvalidJson -> "Invalid project JSON: $message"
4074: is ProjectManifestError.UnsupportedFormat -> "Unsupported project format: $actual"
4075: is ProjectManifestError.UnsupportedVersion -> "Unsupported project version: $actual"
4076: ProjectManifestError.BlankName -> "Project name must not be blank."
4077: is ProjectManifestError.UnsupportedMachine -> "Unsupported project machine: $actual"
4078: is ProjectManifestError.InvalidSourceId -> "Invalid source id at index $sourceIndex: $actual"
4079: is ProjectManifestError.DuplicateSourceId -> "Duplicate source id: $id"
4080: is ProjectManifestError.InvalidSourcePath -> "Invalid path for source '$sourceId': $actual"
4081: is ProjectManifestError.DuplicateSourcePath -> "Duplicate source path: $path"
4082: is ProjectManifestError.InvalidLoadAddress -> "Invalid load address for source '$sourceId': $actual"
4083: is ProjectManifestError.MissingLoadAddress -> "Raw binary source '$sourceId' needs loadAddress."
4084: is ProjectManifestError.UnexpectedLoadAddress -> "Source '$sourceId' must not specify loadAddress."
4088: ProjectSourceType.BASIC -> "BASIC"
4089: ProjectSourceType.MEMORY_DUMP -> "memory dump"
4090: ProjectSourceType.RAW_BINARY -> "raw binary"
4091: ProjectSourceType.ASSEMBLY -> "assembly"
4096: "A project currently supports one BASIC program; found: ${sourceIds.joinToString()}."
4097: is DesktopProjectBuildError.CouldNotReadSource -> "Could not read project source '$sourceId': $message"
4098: is DesktopProjectBuildError.BasicCompile -> "Source '$sourceId': ${error.message(machineName)}"
4099: is DesktopProjectBuildError.MemoryDumpParse -> "Source '$sourceId': ${error.message()}"
4100: is DesktopProjectBuildError.EmptyRawBinary -> "Raw binary source '$sourceId' is empty."
4102: "Source '$sourceId' does not fit in memory at 0x${startAddress.hex(4)} ($size bytes)."
4104: "Source '$sourceId' overlaps '$previousSourceId' at 0x${address.hex(4)}."
4105: is DesktopProjectBuildError.Placement -> "Source '$sourceId': ${error.message()}"
4108: "Assembly source '$sourceId', line $line: $message"
4113: "Source line $sourceLine writes read-only or unmapped address 0x${address.hex(4)}."
4115: "Source line $sourceLine: 0x${address.hex(4)} and 0x${previousAddress.hex(4)} write the same physical address 0x${physicalAddress.hex(4)}."
4117: "Source line $sourceLine: 0x${address.hex(4)} overlaps BASIC text, pointer updates or old-text clearing."
4123: "Unsupported ROM machine: ${reason.machineId.value}."
4125: "Invalid ROM size: ${reason.actual} bytes (expected ${reason.expected.joinToString(" or ")})."
4127: "Invalid ${reason.componentId.value} ROM size: ${reason.actual} bytes " +
4128: "(expected ${reason.expected})."
4130: is DesktopRomLoadError.InvalidPackage -> "Invalid .pgrom package: ${error.message()}"
4131: is DesktopRomLoadError.SessionCreation -> "Could not create emulator session: $error"
4136: "package is too large ($actual bytes; maximum $maximum)"
4138: "package has more than $maximum entries"
4140: "$path exceeds $maximum bytes"
4141: is com.digihori.pgp.desktop.rom.DesktopRomPackageError.UnsafePath -> "unsafe path: $path"
4142: is com.digihori.pgp.desktop.rom.DesktopRomPackageError.DuplicateEntry -> "duplicate entry: $path"
4144: com.digihori.pgp.desktop.rom.DesktopRomPackageError.MissingManifest -> "manifest.json is missing"
4173: private const val REPOSITORY_URL: String = "https://github.com/digihori/pokecom-go-plus"
4174: private const val GETTING_STARTED_URL: String = "$REPOSITORY_URL/blob/main/docs/GETTING_STARTED.md"
4175: private const val PROJECTS_HELP_URL: String = "$REPOSITORY_URL/blob/main/docs/PROJECTS.md#command-history"
4176: private const val DEBUGGER_HELP_URL: String = "$REPOSITORY_URL/blob/main/docs/DEBUGGER.md"
4184: val normalized = value.trim().removePrefix("0x").removePrefix("0X")
4194: NOT_BUILT("not built"),
4195: BUILT_NOT_LOADED("built; latest build not loaded"),
4196: NOT_LOADED("project not loaded"),
4197: UP_TO_DATE("up to date"),
4198: SOURCE_CHANGED("source changed"),
4199: BUILD_FAILED("build failed"),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/application/debug/DebugContext.kt

```text
54: const val FORMAT: String = "pgp-debug-context"
70: data object Reset : DebugStopContext { override val kind = "reset" }
71: data object UserPause : DebugStopContext { override val kind = "userPause" }
72: data object StepComplete : DebugStopContext { override val kind = "stepComplete" }
73: data class Breakpoint(val address: Int) : DebugStopContext { override val kind = "breakpoint" }
74: data class RunToAddress(val address: Int) : DebugStopContext { override val kind = "runToAddress" }
78: ) : DebugStopContext { override val kind = "memoryChanged" }
82: ) : DebugStopContext { override val kind = "memoryAccessed" }
84: override val kind = "fault"
120: val encoding: String = "hex",
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/application/debug/DebugContextJsonWriter.kt

```text
16: put("format", DebugContextV1.FORMAT)
17: put("schemaVersion", DebugContextV1.SCHEMA_VERSION)
18: put("capturedAt", context.capturedAt)
19: put("session", buildJsonObject {
20: put("id", context.session.id)
21: put("revision", context.session.revision)
22: put("machineId", context.session.machineId)
23: put("runState", context.session.runState)
24: putNullable("executedCycles", context.session.executedCycles)
26: context.stop?.let { put("stop", it.toJson()) } ?: put("stop", JsonNull)
27: put("cpu", context.cpu.toJson())
28: put("rom", buildJsonObject {
29: putNullable("selectedBank", context.rom.selectedBank)
30: putNullable("currentLocation", context.rom.currentLocation?.toJson())
32: put("memory", buildJsonArray { context.memory.forEach { add(it.toJson()) } })
33: put("disassembly", buildJsonArray { context.disassembly.forEach { add(it.toJson()) } })
34: put("memoryAccessHistory", buildJsonObject {
35: put("enabled", context.memoryAccessHistory.enabled)
36: put("truncated", context.memoryAccessHistory.truncated)
37: put("entries", buildJsonArray {
41: put("bankHistory", buildJsonObject {
42: put("supported", context.bankHistory.supported)
43: put("truncated", context.bankHistory.truncated)
44: put("entries", buildJsonArray {
47: put("sequence", entry.sequence)
48: put("previousBank", entry.previousBank)
49: put("selectedBank", entry.selectedBank)
50: put("selectorValue", entry.selectorValue)
55: put("trace", buildJsonObject {
56: put("enabled", context.trace.enabled)
57: put("capacity", context.trace.capacity)
58: put("truncated", context.trace.truncated)
59: put("entries", buildJsonArray { context.trace.entries.forEach { add(it.toJson()) } })
61: put("warnings", buildJsonArray { context.warnings.forEach(::add) })
63: return (json.encodeToString(root) + "\n").encodeToByteArray()
67: put("programCounter", programCounter); put("currentProgramCounter", currentProgramCounter)
68: put("opcode", opcode); put("dataPointer", dataPointer)
69: put("p", p); put("q", q); put("r", r); put("d", d); put("alu", alu)
70: put("carry", carry); put("zero", zero); put("xInput", xInput); put("powerOn", powerOn)
71: put("ia", ia); put("ib", ib); put("fo", fo); put("control", control); put("testPort", testPort)
72: put("internalRam", buildJsonObject {
73: put("encoding", "hex"); put("byteLength", internalRamByteLength); put("data", internalRamHex)
78: put("kind", kind)
81: is DebugStopContext.Breakpoint -> put("address", stop.address)
82: is DebugStopContext.RunToAddress -> put("address", stop.address)
84: put("instructionAddress", stop.instructionAddress)
85: put("changes", buildJsonArray {
88: put("address", change.address); put("before", change.before); put("after", change.after)
94: put("instructionAddress", stop.instructionAddress)
95: put("accesses", buildJsonArray { stop.accesses.forEach { add(it.toJson()) } })
98: put("faultKind", stop.faultKind)
99: putNullable("address", stop.address)
100: putNullable("opcode", stop.opcode)
106: put("startAddress", startAddress); put("byteLength", byteLength)
107: putNullable("regionId", regionId); put("encoding", encoding); put("data", data)
111: put("address", address)
112: put("bytes", buildJsonArray { bytes.forEach(::add) })
113: putNullable("mnemonic", mnemonic); putNullable("operandValue", operandValue)
114: putNullable("targetAddress", targetAddress); put("text", text)
115: putNullable("romLocation", romLocation?.toJson())
119: putNullable("sequence", sequence); putNullable("instructionAddress", instructionAddress)
120: put("kind", kind.name.lowercase()); put("address", address); put("value", value)
124: put("sequence", sequence); put("address", address)
125: put("bytes", buildJsonArray { bytes.forEach(::add) }); put("text", text)
126: putNullable("romLocation", romLocation?.toJson())
127: put("dataPointer", dataPointer); put("p", p); put("q", q); put("r", r); put("d", d)
128: put("carry", carry); put("zero", zero)
132: put("component", component); putNullable("bank", bank); put("offset", offset)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/application/debug/DebugContextService.kt

```text
181: return DebugContextError.InvalidRequest("At most $MAX_MEMORY_RANGES memory ranges may be requested")
187: return DebugContextError.InvalidRequest("Memory ranges must be non-empty and remain inside the 16-bit address space")
190: return DebugContextError.InvalidRequest("At most $MAX_MEMORY_BYTES memory bytes may be requested")
193: return DebugContextError.InvalidRequest("Disassembly start address must be a 16-bit address")
196: return DebugContextError.InvalidRequest("Disassembly instruction count is out of range")
199: return DebugContextError.InvalidRequest("Disassembly start address is required when instructions are requested")
204: return DebugContextError.InvalidRequest("History and trace limits must be between 0 and $MAX_HISTORY_ENTRIES")
232: faultKind = "unsupportedOpcode",
251: private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/basic/DesktopBasicLoader.kt

```text
89: is BasicProgramDecodeResult.UnsupportedMachine -> DesktopBasicProgramDecodeResult.GenericFailure(0, "Unsupported machine: ${result.machineId.value}")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/basic/DesktopOldBasicTransferAdapter.kt

```text
12: return DesktopOldBasicTransferResult.Failure("BASIC program must begin with FF.")
15: return DesktopOldBasicTransferResult.Failure("BASIC program must end with FF.")
24: return DesktopOldBasicTransferResult.Failure("OLD BASIC body must end with F0.")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/character/CharacterEditorPanel.kt

```text
55: Text("Character Editor", style = MaterialTheme.typography.titleLarge)
58: "Column-major bytes; top dot is bit 0. Generic prototype format, not a verified machine UDC format.",
71: Button(onClick = { model = model.clear() }) { Text("Clear") }
72: Button(onClick = { model = model.invert() }) { Text("Invert") }
77: label = { Text("Byte values") },
81: ?: "${format.byteCount} decimal, 0xNN, or &NN values",
88: Button(onClick = { model = model.applyByteText() }) { Text("Apply to grid") }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/character/DesktopCharacterEditorModel.kt

```text
30: onFailure = { copy(inputError = it.message ?: "The byte values do not fit this format.") },
44: get() = encodedBytes().joinToString(",") { "&${it.unsignedHex()}" }
47: get() = encodedBytes().joinToString(",") { (it.toInt() and 0xff).toString() }
50: get() = encodedBytes().joinToString(",") { "$${it.unsignedHex()}" }
53: get() = encodedBytes().joinToString(",") { "0x${it.unsignedHex()}" }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/debug/DesktopDebuggerCheckpointWriter.kt

```text
25: put("format", "pgp-debug-checkpoint")
26: put("formatVersion", 1)
27: put("machineId", machineId)
28: put("capturedAt", capturedAt)
29: put("executedCycles", executedCycles)
30: stopReason?.let { put("stopReason", it) }
31: put("cpu", buildJsonObject {
32: put("pc", cpu.programCounter.hex(4))
33: put("currentPc", cpu.currentProgramCounter.hex(4))
34: put("opcode", cpu.opcode.hex(2))
35: put("dp", cpu.dataPointer.hex(4))
36: put("p", cpu.p.hex(2)); put("q", cpu.q.hex(2)); put("r", cpu.r.hex(2)); put("d", cpu.d.hex(2))
37: put("alu", cpu.alu.hex(4)); put("carry", cpu.carry); put("zero", cpu.zero)
38: put("ia", cpu.ia.hex(2)); put("ib", cpu.ib.hex(2)); put("fo", cpu.fo.hex(2))
39: put("control", cpu.control.hex(2)); put("test", cpu.testPort.hex(2))
40: put("internalRam", cpu.copyInternalRam().hex())
42: put("memory", buildJsonObject {
43: put("startAddress", memory.startAddress.hex(4))
44: put("endAddress", (memory.startAddress + memory.size - 1).hex(4))
45: put("bytes", memory.copyBytes().hex())
47: put("trace", buildJsonArray {
50: put("sequence", entry.sequence)
51: put("address", entry.instruction.address.hex(4))
52: put("bytes", entry.instruction.bytes.joinToString("") { it.hex(2) })
53: put("instruction", Sc61860InstructionFormatter.format(entry.instruction))
55: put("romComponent", location.componentId.value)
56: location.bank?.let { put("romBank", it) }
57: put("romOffset", location.offset.hex(4))
59: put("dp", entry.dataPointer.hex(4)); put("p", entry.p.hex(2))
60: put("carry", entry.carry); put("zero", entry.zero)
65: return (json.encodeToString(root) + "\n").encodeToByteArray()
68: private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).hex(2) }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/debug/DesktopDisassembly.kt

```text
26: byteText = instruction.bytes.joinToString(" ") { it.hex(2) },
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/debug/DesktopMemoryView.kt

```text
28: hexText = bytes.joinToString(" ") { it.hex(2) },
31: }.joinToString(""),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/display/CharacterCellGeometry.kt

```text
5: require(characterColumns > 0) { "characterColumns must be positive" }
6: require(characterWidth > 0) { "characterWidth must be positive" }
7: require(gapColumns >= 0) { "gapColumns must not be negative" }
12: require(dotColumn >= 0) { "dotColumn must not be negative" }
13: require(characterWidth > 0) { "characterWidth must be positive" }
14: require(gapColumns >= 0) { "gapColumns must not be negative" }
19: require(characterRows > 0) { "characterRows must be positive" }
20: require(characterHeight > 0) { "characterHeight must be positive" }
21: require(gapRows >= 0) { "gapRows must not be negative" }
26: require(dotRow >= 0) { "dotRow must not be negative" }
27: require(characterHeight > 0) { "characterHeight must be positive" }
28: require(gapRows >= 0) { "gapRows must not be negative" }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/display/DesktopLcdGeometry.kt

```text
15: "pc-1460" -> columns * 0.75f / rows
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/DesktopGamepadController.kt

```text
59: status = uiText("controller.saved")
60: }.onFailure { status = uiText("controller.saveFailed", it.message) }
79: status = uiText("controller.inputUnavailable", failure.message ?: failure::class.simpleName)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/DesktopGamepadPreferences.kt

```text
7: private val preferences: Preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/gamepad"),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/DesktopKeyMapper.kt

```text
8: if (machineId?.value == "pc-1470u") when (key) {
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/DesktopKeyboardInput.kt

```text
111: activeSink.pressHostKey(fixedKey, "keyboard:$physicalKey")
118: activeSink.pressHostKey(directKey, "keyboard:$physicalKey")
138: activeSink.pressHostKey(directKey, "keyboard:$physicalKey")
145: is ActiveInput.Direct -> activeSink.releaseHostKey(input.key, "keyboard:$physicalKey")
173: if (input is ActiveInput.Direct) target.releaseHostKey(input.key, "keyboard:$physicalKey")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/Pc1245KeyboardLayout.kt

```text
19: cap(PocketKey.DEF, "DEF", column = 0),
20: cap(PocketKey.SHIFT, "SHIFT", column = 1),
21: cap(PocketKey.DOWN, "↓", column = 5),
22: cap(PocketKey.UP, "↑", column = 6),
23: cap(PocketKey.LEFT, "←", shifted = "DEL", column = 7),
24: cap(PocketKey.RIGHT, "→", shifted = "INS", column = 8),
25: cap(PocketKey.BREAK, "BRK", shifted = "ON", column = 9),
26: cap(PocketKey.NUM_7, "7", column = 10),
27: cap(PocketKey.NUM_8, "8", column = 11),
28: cap(PocketKey.NUM_9, "9", column = 12),
29: cap(PocketKey.CLEAR, "CL", shifted = "CA", column = 13),
32: cap(PocketKey.Q, "Q", shifted = "!", column = 0),
33: cap(PocketKey.W, "W", shifted = "\"", column = 1),
34: cap(PocketKey.E, "E", shifted = "#", column = 2),
35: cap(PocketKey.R, "R", shifted = "$", column = 3),
36: cap(PocketKey.T, "T", shifted = "%", column = 4),
37: cap(PocketKey.Y, "Y", shifted = "&", column = 5),
38: cap(PocketKey.U, "U", shifted = "?", column = 6),
39: cap(PocketKey.I, "I", shifted = ":", column = 7),
40: cap(PocketKey.O, "O", shifted = ",", column = 8),
41: cap(PocketKey.P, "P", shifted = ";", column = 9),
42: cap(PocketKey.NUM_4, "4", column = 10),
43: cap(PocketKey.NUM_5, "5", column = 11),
44: cap(PocketKey.NUM_6, "6", shifted = "¥", column = 12),
45: cap(PocketKey.DIVIDE, "÷", shifted = "^", column = 13),
48: cap(PocketKey.A, "A", basic = "INPUT", column = 0),
49: cap(PocketKey.S, "S", basic = "IF", column = 1),
50: cap(PocketKey.D, "D", basic = "THEN", column = 2),
51: cap(PocketKey.F, "F", basic = "GOTO", column = 3),
52: cap(PocketKey.G, "G", basic = "FOR", column = 4),
53: cap(PocketKey.H, "H", basic = "TO", column = 5),
54: cap(PocketKey.J, "J", basic = "STEP", column = 6),
55: cap(PocketKey.K, "K", basic = "NEXT", column = 7),
56: cap(PocketKey.L, "L", basic = "LIST", column = 8),
57: cap(PocketKey.EQUALS, "=", basic = "RUN", column = 9),
58: cap(PocketKey.NUM_1, "1", shifted = "(", column = 10),
59: cap(PocketKey.NUM_2, "2", shifted = ")", column = 11),
60: cap(PocketKey.NUM_3, "3", shifted = "@", column = 12),
61: cap(PocketKey.MULTIPLY, "×", shifted = "<", column = 13),
64: cap(PocketKey.Z, "Z", basic = "PRINT", column = 0),
65: cap(PocketKey.X, "X", basic = "USING", column = 1),
66: cap(PocketKey.C, "C", basic = "GOSUB", column = 2),
67: cap(PocketKey.V, "V", basic = "RETURN", column = 3),
68: cap(PocketKey.B, "B", basic = "DIM", column = 4),
69: cap(PocketKey.N, "N", basic = "END", column = 5),
70: cap(PocketKey.M, "M", basic = "CSAVE", column = 6),
71: cap(PocketKey.SPACE, "SPACE", basic = "CLOAD", column = 7),
72: cap(PocketKey.ENTER, "ENTER", basic = "N↔NP", column = 8, span = 2),
73: cap(PocketKey.NUM_0, "0", shifted = "π", column = 10),
74: cap(PocketKey.DOT, ".", shifted = "√", column = 11),
75: cap(PocketKey.PLUS, "+", shifted = "EXP", column = 12),
76: cap(PocketKey.MINUS, "−", shifted = ">", column = 13),
96: PocketKey.DOWN -> cap.copy(shiftedLabel = "(", basicLabel = null)
97: PocketKey.UP -> cap.copy(shiftedLabel = ")", basicLabel = null)
110: PocketKey.NUM_7 -> cap.copy(shiftedLabel = "HELP", basicLabel = null)
111: PocketKey.NUM_8 -> cap.copy(shiftedLabel = "カナ", basicLabel = null)
123: cap(PocketKey.MODE, "MODE", 0), cap(PocketKey.BREAK, "BRK", 1),
124: cap(PocketKey.DOWN, "↓", 2), cap(PocketKey.UP, "↑", 3),
125: cap(PocketKey.LEFT, "←", 4), cap(PocketKey.RIGHT, "→", 5),
126: cap(PocketKey.DELETE, "DEL", 7), cap(PocketKey.INSERT, "INS", 8),
127: cap(PocketKey.SHIFT, "SHIFT", 9), cap(PocketKey.CLEAR, "CE", 10),
128: cap(PocketKey.NUM_7, "7", 11), cap(PocketKey.NUM_8, "8", 12), cap(PocketKey.NUM_9, "9", 13),
129: cap(PocketKey.LEFT_PAREN, "(", 14, shifted = "<"), cap(PocketKey.RIGHT_PAREN, ")", 15, shifted = ">"),
132: cap(PocketKey.SHIFT, "SHIFT", 0),
133: cap(PocketKey.Q, "Q", 1, shifted = "!"), cap(PocketKey.W, "W", 2, shifted = "\""), cap(PocketKey.E, "E", 3, shifted = "#"),
134: cap(PocketKey.R, "R", 4, shifted = "$"), cap(PocketKey.T, "T", 5, shifted = "%"), cap(PocketKey.Y, "Y", 6, shifted = "&"),
135: cap(PocketKey.U, "U", 7, shifted = "?"), cap(PocketKey.I, "I", 8, shifted = "π"), cap(PocketKey.O, "O", 9, shifted = "√"), cap(PocketKey.P, "P", 10, shifted = "@"),
136: cap(PocketKey.NUM_4, "4", 11), cap(PocketKey.NUM_5, "5", 12), cap(PocketKey.NUM_6, "6", 13),
137: cap(PocketKey.DIVIDE, "÷", 14, shifted = "¥"), cap(PocketKey.COLON, ":", 15),
140: cap(PocketKey.DEF, "DEF", 0),
141: cap(PocketKey.A, "A", 1), cap(PocketKey.S, "S", 2), cap(PocketKey.D, "D", 3),
142: cap(PocketKey.F, "F", 4), cap(PocketKey.G, "G", 5), cap(PocketKey.H, "H", 6),
143: cap(PocketKey.J, "J", 7), cap(PocketKey.K, "K", 8), cap(PocketKey.L, "L", 9), cap(PocketKey.EQUALS, "=", 10),
144: cap(PocketKey.NUM_1, "1", 11), cap(PocketKey.NUM_2, "2", 12), cap(PocketKey.NUM_3, "3", 13),
145: cap(PocketKey.MULTIPLY, "×", 14), cap(PocketKey.SEMICOLON, ";", 15),
148: cap(PocketKey.KANA, "KANA", 0, shifted = "SML"),
149: cap(PocketKey.Z, "Z", 1), cap(PocketKey.X, "X", 2), cap(PocketKey.C, "C", 3),
150: cap(PocketKey.V, "V", 4), cap(PocketKey.B, "B", 5), cap(PocketKey.N, "N", 6), cap(PocketKey.M, "M", 7),
151: cap(PocketKey.SPACE, "SPACE", 8), cap(PocketKey.ENTER, "ENTER", 9, span = 2),
152: cap(PocketKey.NUM_0, "0", 11), cap(PocketKey.DOT, ".", 12), cap(PocketKey.PLUS, "+", 13),
153: cap(PocketKey.MINUS, "−", 14, shifted = "^"), cap(PocketKey.COMMA, ",", 15),
177: cap(PocketKey.MODE, "MODE", 0), cap(PocketKey.BREAK, "BRK", 1),
178: cap(PocketKey.DOWN, "↓", 2), cap(PocketKey.UP, "↑", 3),
179: cap(PocketKey.LEFT, "←", 4), cap(PocketKey.RIGHT, "→", 5),
180: cap(PocketKey.KANA, "KANA", 6), cap(PocketKey.DELETE, "DEL", 7),
181: cap(PocketKey.INSERT, "INS", 8), cap(PocketKey.SHIFT, "SHIFT", 9), cap(PocketKey.CLEAR, "CE", 10),
182: cap(PocketKey.NUM_7, "7", 11), cap(PocketKey.NUM_8, "8", 12), cap(PocketKey.NUM_9, "9", 13),
183: cap(PocketKey.COMMA, ",", 14, shifted = "<"), cap(PocketKey.COLON, ":", 15, shifted = ">"),
187: PocketKey.DIVIDE -> cap.copy(shiftedLabel = "(")
188: PocketKey.COLON -> cap.copy(key = PocketKey.SEMICOLON, primaryLabel = ";", shiftedLabel = ")")
193: if (cap.key == PocketKey.MULTIPLY) cap.copy(shiftedLabel = "¥") else cap
197: cap.copy(key = PocketKey.SMALL, primaryLabel = "SML", shiftedLabel = null)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/Pc14xxKeyboardLayout.kt

```text
29: if (cap.key in setOf(PocketKey.UP, PocketKey.DOWN)) cap.copy(shiftedLabel = "MATRIX") else cap
32: if (cap.key == PocketKey.FORMAT) cap.copy(primaryLabel = "FSE") else cap
37: PocketKey.MEMORY_STORE -> cap.copy(key = PocketKey.DELETE, primaryLabel = "DEL", shiftedLabel = "X↔M")
38: PocketKey.MEMORY_RECALL -> cap.copy(key = PocketKey.BACKSPACE, primaryLabel = "BS", shiftedLabel = "RM")
39: PocketKey.MEMORY_ADD -> cap.copy(key = PocketKey.INSERT, primaryLabel = "INS", shiftedLabel = "M+")
40: PocketKey.CAL -> cap.copy(primaryLabel = "AER", shiftedLabel = "CAL")
41: PocketKey.DIVIDE -> cap.copy(shiftedLabel = "DRG")
42: PocketKey.EQUALS -> cap.copy(shiftedLabel = "STAT")
43: PocketKey.LOCK -> cap.copy(key = PocketKey.SHIFT, primaryLabel = "SHIFT", shiftedLabel = null)
48: if (cap.key == PocketKey.STAT) cap.copy(key = PocketKey.TITLE, primaryLabel = "↕︎", shiftedLabel = "TITLE") else cap
52: PocketKey.BREAK -> "BRK"; PocketKey.KANA -> "カナ"; PocketKey.DAKUTEN -> "゛"
53: PocketKey.LONG_VOWEL -> "ー"; PocketKey.SMALL -> "SML"; PocketKey.DOWN -> "↓"; PocketKey.UP -> "↑"
54: PocketKey.LEFT -> "←"; PocketKey.RIGHT -> "→"; PocketKey.MEMORY_STORE -> "X↔M"
55: PocketKey.DIVIDE -> "÷"; PocketKey.NUM_7 -> "7"; PocketKey.NUM_8 -> "8"; PocketKey.NUM_9 -> "9"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/SdlGamepadBackend.kt

```text
48: GamepadDevice(id.toString(), SDL_GetGamepadNameForID(id) ?: "Gamepad")
98: return if (System.getProperty("os.name").startsWith("Mac")) {
101: check(EventQueue.isDispatchThread()) { "SDL must run on the application event thread" }
110: private val dispatch = Library.loadNative(MacGamepadThread::class.java, "org.lwjgl", "/usr/lib/system/libdispatch.dylib")
111: private val queue = dispatch.getFunctionAddress("_dispatch_main_q").also { check(it != 0L) }
112: private val async = dispatch.getFunctionAddress("dispatch_async_f").also { check(it != 0L) }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/machine/DesktopContiguousMemoryImage.kt

```text
13: if (segments.isEmpty()) return DesktopContiguousMemoryImageResult.Failure("The memory image is empty.")
21: "OLD WAV binary data must be contiguous; gap before 0x${segment.startAddress.toString(16).uppercase().padStart(4, '0')}.",
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/machine/DesktopMemoryDumpWriter.kt

```text
11: fun write(image: AddressedMemoryImage): ByteArray = image.segments.joinToString("\n") { segment ->
13: }.plus("\n").encodeToByteArray()
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/mcp/DesktopMcpCredentialStore.kt

```text
47: ".mcp-headers-",
48: ".tmp",
52: Files.createTempFile(directory, ".mcp-headers-", ".tmp")
77: Json.parseToJsonElement(Files.readString(credentialPath)).jsonObject["Authorization"]
78: ?.jsonPrimitive?.content?.removePrefix("Bearer ")
90: ?: error("The filesystem does not support secure POSIX permissions or Windows ACLs")
106: "MCP credential file permissions are not owner-only"
111: ?: error("Could not verify MCP credential file access")
113: "MCP credential file ACL is not owner-only"
118: private val DIRECTORY_PERMISSIONS = PosixFilePermissions.fromString("rwx------")
119: private val FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------")
142: val home = properties["user.home"]?.takeIf(String::isNotBlank)
143: ?: error("Cannot determine the user home directory")
144: val os = properties["os.name"].orEmpty().lowercase()
146: os.contains("mac") -> Path.of(home, "Library", "Application Support", "PokecomGOStudio")
147: os.contains("win") -> Path.of(
148: environment["LOCALAPPDATA"]?.takeIf(String::isNotBlank)
149: ?: error("LOCALAPPDATA is not available"),
150: "PokecomGOStudio",
153: environment["XDG_RUNTIME_DIR"]?.takeIf(String::isNotBlank)
154: ?: Path.of(home, ".local", "state").toString(),
155: "pokecom-go-studio",
158: return directory.resolve("mcp-headers.json")
162: val os = properties["os.name"].orEmpty().lowercase()
164: return if (os.contains("win")) {
165: "cmd.exe /d /s /c type \"${value.replace("\"", "\"\"")}\""
167: "/bin/cat '${value.replace("'", "'\\''")}'"
172: put("Authorization", "Bearer $token")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/mcp/DesktopMcpServer.kt

```text
61: require(port in 0..65535) { "MCP port must be between 0 and 65535" }
66: Thread(runnable, "pgp-mcp-http").apply { isDaemon = true }
69: created.createContext("/mcp", ::handleMcp)
70: created.createContext("/", ::handleRoot)
87: DesktopMcpStartResult.Failure(error.message ?: error::class.simpleName ?: "Could not start MCP server")
105: sendText(exchange, 403, "Forbidden")
106: } else if (exchange.requestMethod == "GET" && exchange.requestURI.path == "/") {
107: sendText(exchange, 200, "Pokecom GO Studio read-only MCP server")
109: sendText(exchange, 404, "Not Found")
117: if (exchange.requestMethod == "OPTIONS") {
122: sendJson(exchange, 403, rpcError(JsonNull, -32001, "Loopback clients only"))
126: exchange.responseHeaders.set("WWW-Authenticate", "Bearer")
127: sendJson(exchange, 401, rpcError(JsonNull, -32002, "Unauthorized"))
130: if (exchange.requestMethod != "POST") {
131: exchange.responseHeaders.set("Allow", "POST, OPTIONS")
132: sendJson(exchange, 405, rpcError(JsonNull, -32600, "Only POST is supported in stateless mode"))
137: sendJson(exchange, 413, rpcError(JsonNull, -32600, "Request exceeds $requestLimitBytes bytes"))
141: sendJson(exchange, 400, rpcError(JsonNull, -32700, "Parse error"))
144: val id = request["id"] ?: JsonNull
145: val method = request["method"]?.jsonPrimitive?.contentOrNull
146: if (request["jsonrpc"]?.jsonPrimitive?.contentOrNull != "2.0" || method == null) {
147: sendJson(exchange, 400, rpcError(id, -32600, "Invalid JSON-RPC request"))
150: if (request["id"] == null) {
155: val response = handleRequest(id, method, request["params"])
161: "initialize" -> rpcResult(id, buildJsonObject {
162: val requested = runCatching { params?.jsonObject?.get("protocolVersion")?.jsonPrimitive?.content }.getOrNull()
163: put("protocolVersion", if (requested in SUPPORTED_PROTOCOL_VERSIONS) requested!! else MCP_PROTOCOL_VERSION)
164: put("capabilities", buildJsonObject {
165: put("tools", buildJsonObject { put("listChanged", false) })
167: put("serverInfo", buildJsonObject {
168: put("name", "pokecom-go-studio")
169: put("title", "Pokecom GO Studio")
170: put("version", ProjectInfo.VERSION)
173: "instructions",
174: "Read-only access to the active paused Pokecom GO Studio emulator session. " +
175: "ROM bytes are not exposed through memory ranges; use bounded disassembly and trace data.",
179: "ping" -> rpcResult(id, buildJsonObject {}).also {
182: "tools/list" -> rpcResult(id, buildJsonObject { put("tools", adapter.tools()) }).also {
185: "tools/call" -> {
187: val objectValue = params?.jsonObject ?: error("params must be an object")
188: val name = objectValue["name"]?.jsonPrimitive?.contentOrNull ?: error("name is required")
189: val arguments = objectValue["arguments"]?.jsonObject ?: buildJsonObject {}
194: val succeeded = result["isError"]?.jsonPrimitive?.contentOrNull != "true"
200: rpcError(id, -32602, error.message ?: "Invalid tool arguments")
204: else -> rpcError(id, -32601, "Method not found: $method").also {
210: val expected = "Bearer $token".toByteArray(StandardCharsets.UTF_8)
211: val actual = exchange.requestHeaders.getFirst("Authorization")
217: exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
218: exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
220: "Access-Control-Allow-Headers",
221: "authorization, content-type, mcp-protocol-version",
223: exchange.responseHeaders.set("Access-Control-Expose-Headers", "MCP-Protocol-Version")
228: exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
229: exchange.responseHeaders.set("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
230: exchange.responseHeaders.set("Cache-Control", "no-store")
237: exchange.responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
238: exchange.responseHeaders.set("Cache-Control", "no-store")
244: put("jsonrpc", "2.0"); put("id", id); put("result", result)
248: put("jsonrpc", "2.0"); put("id", id)
249: put("error", buildJsonObject { put("code", code); put("message", message) })
252: private fun endpoint(port: Int): String = "http://${LOOPBACK.hostAddress}:$port/mcp"
257: const val MCP_PROTOCOL_VERSION: String = "2025-06-18"
258: private val SUPPORTED_PROTOCOL_VERSIONS = setOf(MCP_PROTOCOL_VERSION, "2025-03-26")
259: private val LOOPBACK: InetAddress = InetAddress.getByName("127.0.0.1")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/mcp/PgpMcpToolAdapter.kt

```text
33: put("name", GET_CAPABILITIES)
34: put("title", "Get Pokecom GO capabilities")
35: put("description", "Returns the active machine and read-only Debug Context limits.")
36: put("inputSchema", objectSchema())
37: put("outputSchema", buildJsonObject { put("type", "object") })
38: put("annotations", readOnlyAnnotations())
41: put("name", GET_DEBUG_CONTEXT)
42: put("title", "Get Pokecom GO Debug Context")
44: "description",
45: "Returns versioned CPU, stop, selected memory, disassembly, ROM location, access, bank, and trace data. " +
46: "The emulator must already be paused or faulted.",
48: put("inputSchema", debugContextInputSchema())
49: put("outputSchema", buildJsonObject { put("type", "object") })
50: put("annotations", readOnlyAnnotations())
55: val service = serviceProvider() ?: return@dispatch toolError("NO_ACTIVE_SESSION", "No active emulator session")
59: else -> toolError("UNKNOWN_TOOL", "Unknown tool: $name")
66: put("format", value.format)
67: put("schemaVersion", value.schemaVersion)
68: put("machineId", value.machineId)
69: put("maxMemoryRanges", value.maxMemoryRanges)
70: put("maxMemoryBytes", value.maxMemoryBytes)
71: put("maxDisassemblyInstructions", value.maxDisassemblyInstructions)
72: put("maxHistoryEntries", value.maxHistoryEntries)
73: put("memoryAccessHistorySupported", value.memoryAccessHistorySupported)
74: put("bankHistorySupported", value.bankHistorySupported)
75: put("romBytesAllowed", value.romBytesAllowed)
77: return toolSuccess(structured, "Read-only Pokecom GO capabilities")
82: return toolError("INVALID_ARGUMENTS", it.message ?: "Invalid arguments")
89: toolSuccess(structured, "Captured ${result.context.session.machineId} Debug Context")
93: "SESSION_RUNNING",
94: "Pause the emulator before requesting a Debug Context",
96: is DebugContextError.InvalidRequest -> toolError("INVALID_ARGUMENTS", error.message)
98: "ROM_BYTES_NOT_ALLOWED",
99: "ROM bytes are not exposed through memory ranges; request bounded disassembly instead",
101: put("startAddress", error.startAddress)
102: put("length", error.length)
112: "memoryRanges", "disassemblyStartAddress", "disassemblyInstructionCount",
113: "memoryAccessHistoryLimit", "bankHistoryLimit", "traceLimit",
116: val ranges = get("memoryRanges")?.let { element ->
117: element.asArray("memoryRanges").mapIndexed { index, item ->
118: val range = item.asObject("memoryRanges[$index]")
119: range.rejectUnknownKeys(setOf("startAddress", "length"))
121: startAddress = range.requiredInt("startAddress"),
122: length = range.requiredInt("length"),
128: disassemblyStartAddress = optionalInt("disassemblyStartAddress"),
129: disassemblyInstructionCount = optionalInt("disassemblyInstructionCount") ?: 0,
130: memoryAccessHistoryLimit = optionalInt("memoryAccessHistoryLimit") ?: 256,
131: bankHistoryLimit = optionalInt("bankHistoryLimit") ?: 256,
132: traceLimit = optionalInt("traceLimit") ?: 256,
138: require(unknown.isEmpty()) { "Unknown arguments: ${unknown.sorted().joinToString()}" }
142: optionalInt(name) ?: error("$name must be an integer")
146: require(value !is JsonNull) { "$name must be an integer" }
147: return value.jsonPrimitive.intOrNull ?: error("$name must be an integer")
151: runCatching { jsonArray }.getOrElse { error("$name must be an array") }
154: runCatching { jsonObject }.getOrElse { error("$name must be an object") }
157: put("structuredContent", structured)
158: put("content", buildJsonArray {
159: add(buildJsonObject { put("type", "text"); put("text", summary) })
161: put("isError", false)
165: put("structuredContent", buildJsonObject {
166: put("error", buildJsonObject {
167: put("code", code)
168: put("message", message)
169: details?.let { put("details", it) }
172: put("content", buildJsonArray {
173: add(buildJsonObject { put("type", "text"); put("text", "$code: $message") })
175: put("isError", true)
179: put("type", "object")
180: put("properties", buildJsonObject {})
181: put("additionalProperties", false)
185: put("type", "object")
186: put("properties", buildJsonObject {
187: put("memoryRanges", buildJsonObject {
188: put("type", "array"); put("maxItems", 8)
189: put("items", buildJsonObject {
190: put("type", "object")
191: put("properties", buildJsonObject {
192: put("startAddress", integerSchema(0, 0xffff))
193: put("length", integerSchema(1, 4_096))
195: put("required", buildJsonArray { add(JsonPrimitive("startAddress")); add(JsonPrimitive("length")) })
196: put("additionalProperties", false)
199: put("disassemblyStartAddress", integerSchema(0, 0xffff))
200: put("disassemblyInstructionCount", integerSchema(0, 256))
201: put("memoryAccessHistoryLimit", integerSchema(0, 4_096))
202: put("bankHistoryLimit", integerSchema(0, 4_096))
203: put("traceLimit", integerSchema(0, 4_096))
205: put("additionalProperties", false)
209: put("type", "integer"); put("minimum", minimum); put("maximum", maximum)
213: put("readOnlyHint", true)
214: put("destructiveHint", false)
215: put("openWorldHint", false)
219: const val GET_CAPABILITIES: String = "pgp_get_capabilities"
220: const val GET_DEBUG_CONTEXT: String = "pgp_get_debug_context"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopAssemblerSettingsPanel.kt

```text
26: var source by remember { mutableStateOf("") }
27: var message by remember { mutableStateOf("") }
33: require(seconds != null && seconds in 1..3600) { uiText("assembler.timeoutError") }
35: if (external) it.command(File("source.asm"))
40: Text(uiText("assembler.internal"))
42: Text(uiText("assembler.external"))
44: OutlinedTextField(executable, { executable = it }, label = { Text(uiText("assembler.executable")) },
46: OutlinedTextField(arguments, { arguments = it }, label = { Text(uiText("assembler.arguments")) },
48: Text(uiText("assembler.commandHelp"))
49: OutlinedTextField(timeout, { timeout = it }, label = { Text(uiText("assembler.timeout")) },
53: .onSuccess { message = uiText("assembler.saved") }
55: }) { Text(uiText("assembler.apply")) }
56: Text(uiText("assembler.selectionHelp"))
57: OutlinedTextField(source, { source = it }, label = { Text(uiText("assembler.testSource")) },
59: Text(uiText("assembler.testHelp"))
65: message = uiText("assembler.running")
69: is DesktopSc61AssemblyResult.Success -> uiText("assembler.imported" , result.image.byteCount.toString(), result.exitCode.toString()) +
70: result.image.segments.joinToString("\n") { "%04X..%04X".format(it.startAddress, it.endAddress) } + "\n${result.log}"
71: is DesktopSc61AssemblyResult.Failure -> "${result.message}\n${result.log}"
74: message = uiText("assembler.cancelled")
79: }) { Text(uiText("assembler.test")) }
80: OutlinedButton(enabled = running, onClick = { job?.cancel() }) { Text(uiText("assembler.cancel")) }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopAssemblyWorkspace.kt

```text
21: val consoleLog: String = "",
51: assembled[source.definition.id] = Sc61860AssemblyResult.Failure(0, result.message + "\n" + result.log)
58: Sc61860AssemblyListingLine(0, it.startAddress, it.copyBytes().map { byte -> byte.toInt() and 255 }, "; External DMP; source mapping unavailable")
111: "0x${address.hex(4)} overlaps ${previous.first.definition.path} at 0x${previous.second.hex(4)} (same ${if (validatePlacement) "physical memory" else "address"}).", line?.sourceText)
135: append("; ").append(source.definition.path).append('\n')
137: append("; External assembler source listing is not imported yet.\n")
141: append(entry.address.hex(4)).append("  ")
142: append(entry.bytes.joinToString(" ") { it.hex(2) }.padEnd(13))
143: append("  ").append(entry.line.toString().padStart(5)).append("  ")
155: append(symbol.name).append(" EQU ").append(symbol.value)
156: } else append(address.hex(4)).append("  ").append(symbol.name)
157: append("  ").append(path).append(':').append(if (symbol.line > 0) symbol.line.toString() else "?").append('\n')
169: return groups.joinToString("\n") { group ->
175: }.plus("\n")
178: internal fun DesktopAssemblySourceResult.renderMemorySummary(layout: MachineMemoryLayout): String = image.segments.joinToString("\n") { segment ->
181: "0x${segment.startAddress.hex(4)}..0x${segment.endAddress.hex(4)}  ${segment.size} bytes  ${kinds.joinToString()}" +
182: if (mirrors) " (contains mirrors)" else ""
187: if (com.digihori.pgp.core.api.MachineCatalog.require(machineId).supportsConfigurableRam) append(" — ").append(pc1251MemoryMode)
194: append(start.hex(4)).append("..").append(end.hex(4)).append("  ").append(region.kind)
195: .append(if (region.writable) " (writable)" else " (read-only)").append('\n')
198: if (machineId.value == "pc-1360") append("4000..7FFF: ROM bank 0..7; bank output is not supported.\n")
199: append("LCD/control writes have side effects. BASIC placement is checked again at load time.")
203: is MemoryImageLoadError.ReadOnlyAddress -> "0x${address.hex(4)} is ${layout.locate(address).kind} and cannot be loaded on ${layout.machineId.value}."
204: is MemoryImageLoadError.MirrorOverlap -> "0x${address.hex(4)} and 0x${previousAddress.hex(4)} write the same physical address 0x${physicalAddress.hex(4)}."
205: is MemoryImageLoadError.BasicOverlap -> "0x${address.hex(4)} overlaps BASIC text, pointer updates or old-text clearing."
212: val baseName = assemblySources.singleOrNull()?.file?.nameWithoutExtension ?: "program"
213: val directory = File(workspace.manifestFile.parentFile, "build")
214: return listOf("dmp", "lst", "map").map { File(directory, "$baseName.$it") }
219: val buildDirectory = File(workspace.manifestFile.parentFile, "build")
220: check(buildDirectory.isDirectory || buildDirectory.mkdirs()) { "Could not create ${buildDirectory.path}" }
225: artifact.assemblySources.joinToString("\n") { it.renderListing().trimEnd() }.plus("\n"),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopExternalAssembler.kt

```text
12: val executable: String = "",
13: val arguments: String = "{source}",
17: require(executable.isNotBlank()) { "Assembler executable is required" }
18: require(timeoutSeconds in 1..3600) { "Timeout must be 1..3600 seconds" }
20: return listOf(executable.trim()) + tokens.map { it.replace("{source}", source.absolutePath) } +
21: if (tokens.none { "{source}" in it }) listOf(source.absolutePath) else emptyList()
41: require(source.isFile) { "Source file does not exist: ${source.path}" }
43: val log = Files.createTempFile("pgp-assembler-", ".log").toFile()
70: bytes.toString(Charsets.UTF_8) + if (it.read() >= 0) "\n[Log truncated at 1 MiB]" else ""
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopExternalEditor.kt

```text
12: /** Opens a diagnostic using PGP_EDITOR="code --goto {file}:{line}" or the OS file handler. */
16: ?: System.getenv("PGP_EDITOR")?.trim().orEmpty()
27: if (System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
29: ProcessBuilder("open", "-t", file.absolutePath).start()
32: check(Desktop.isDesktopSupported()) { "Configure an external editor in Settings" }
34: check(desktop.isSupported(Desktop.Action.EDIT)) { "Configure an external editor in Settings" }
41: it.replace("{file}", file.absolutePath).replace("{line}", (line ?: 1).toString())
43: return if (arguments.any { "{file}" in it }) command else command + file.absolutePath
48: osName: String = System.getProperty("os.name"),
49: path: String = System.getenv("PATH").orEmpty(),
51: File("/Applications"), File(System.getProperty("user.home"), "Applications"),
54: if (!osName.startsWith("Mac", ignoreCase = true) || command.first() != "code") return command
58: .map { File(it, "code") }
61: .map { File(it, "Visual Studio Code.app/Contents/Resources/app/bin/code") }
64: "VS Code was not found. Install it in Applications, or specify its full launcher path in Settings."
77: character == '\'' || character == '"' -> quote = character
85: require(quote == null) { "Editor command has an unclosed quote" }
87: require(result.isNotEmpty()) { "Editor command is empty" }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopProjectBuilder.kt

```text
135: ProjectSourceType.ASSEMBLY -> error("Handled before reading source bytes")
170: else basicSources.singleOrNull()?.definition?.id ?: "BASIC"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopProjectEditor.kt

```text
13: require(trimmed.isNotBlank()) { "Project name must not be blank." }
15: val parent = requireNotNull(root.parentFile) { "The filesystem root cannot be renamed." }
18: "A folder named ${destination.name} already exists."
42: require(source.isFile) { "Select an existing source file." }
44: "bas" -> ProjectSourceType.BASIC
45: "asm" -> ProjectSourceType.ASSEMBLY
46: "dmp" -> ProjectSourceType.MEMORY_DUMP
47: else -> error("Supported source files are .bas, .asm and .dmp.")
50: val directory = File(root, "src")
51: require(directory.canonicalFile.parentFile == root) { "The src directory must be inside the project." }
57: destination = File(directory, "${source.nameWithoutExtension}-${suffix++}.${source.extension}")
59: val baseId = source.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "-")
60: .trim('-', '_').let { if (it.firstOrNull()?.isLetter() == true) it else "source-$it" }
64: while (id in ids) id = "$baseId-${suffix++}"
65: val definition = ProjectSource(id, type, "src/${destination.name}")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopProjectManifestUpdater.kt

```text
28: "bas" -> ProjectSourceType.BASIC
29: "dmp" -> ProjectSourceType.MEMORY_DUMP
30: "asm" -> ProjectSourceType.ASSEMBLY
57: .replace(Regex("[^a-zA-Z0-9_-]"), "-")
59: .let { if (it.firstOrNull()?.isLetter() == true) it else "source-$it" }
60: .ifBlank { "source" }
63: while (id in existingIds) id = "$baseId-${suffix++}"
70: val temporary = Files.createTempFile(destination.parent, ".pgp-project-", ".tmp")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopProjectTree.kt

```text
25: val sourceRoot = File(projectRoot, "src")
50: untracked.filter { it.file.extension.lowercase() in setOf("bas", "asm", "dmp") }.forEach {
59: }.getOrDefault("")
62: name.startsWith('.') || name.endsWith('~') || name.endsWith(".tmp", ignoreCase = true)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopProjectWorkspace.kt

```text
36: const val DEFAULT_MANIFEST_NAME: String = "pgp-project.json"
108: if (!isFile) return SourceFingerprint(exists = false, sha256 = "")
109: val digest = MessageDigest.getInstance("SHA-256").digest(readBytes())
112: sha256 = digest.joinToString(separator = "") { "%02x".format(it) },
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopProjectWorkspaceCreator.kt

```text
11: BASIC("BASIC"),
12: MEMORY_DUMP("Machine code (.dmp)"),
13: ASSEMBLY("Assembly (.asm)"),
46: require(starters.isNotEmpty()) { "At least one starter source is required" }
61: check(projectDirectory.mkdirs()) { "Could not create project folder: ${projectDirectory.path}" }
62: val buildDirectory = File(projectDirectory, "build")
63: check(buildDirectory.mkdir()) { "Could not create build folder: ${buildDirectory.path}" }
66: check(source.file.createNewFile()) { "File appeared while creating project: ${source.file.path}" }
69: check(manifestFile.createNewFile()) { "File appeared while creating project: ${manifestFile.path}" }
81: .replace(Regex("[<>:\"/\\\\|?*]"), "-")
82: .replace(Regex("\\s+"), "-")
84: .ifEmpty { "pgp-project" }
91: val candidate = File(parentDirectory, "$baseFolderName-$suffix")
93: return ProjectDestination(candidate, "$DEFAULT_PROJECT_DISPLAY_NAME $suffix")
106: ProjectSource("main", ProjectSourceType.BASIC, "src/main.bas"),
107: File(root, "src/main.bas"),
108: "10 PRINT \"HELLO\"\n",
115: ProjectSource("machine", ProjectSourceType.MEMORY_DUMP, "src/machine.dmp"),
116: File(root, "src/machine.dmp"),
117: "; Replace this starter byte with your machine-code data.\n${dumpStart.hex()}: 00\n",
124: ProjectSource("assembly", ProjectSourceType.ASSEMBLY, "src/main.asm"),
125: File(root, "src/main.asm"),
126: "; Sample SC61860 program. Edit or remove this example.\n\n" +
127: "ORG 0x${start.hex()}\n\n" +
128: "START: LII 0x12\n" +
129: "       LIDP TABLE\n" +
130: "       JRP DONE\n" +
131: "TABLE: DB 0x01, &02, 3\n" +
132: "DONE:  RTN\n",
149: private const val DEFAULT_PROJECT_FOLDER_NAME: String = "New-PGP-Project"
150: private const val DEFAULT_PROJECT_DISPLAY_NAME: String = "New PGP Project"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopSc61Assembler.kt

```text
17: data class Failure(val message: String, val log: String = "",
27: require(source.isFile) { "Source file does not exist: ${source.path}" }
29: val scratch = Files.createTempDirectory("pgp-sc61-").toFile()
31: val staged = File(scratch, "input.asm")
34: is DesktopExternalAssemblerRunResult.Exited -> when (val result = importDump(run, File(scratch, "input.dmp"), layout, validatePlacement = false)) {
37: val listing = File(scratch, "input.lst")
41: .replace("[${staged.path}]", "[${source.absolutePath}]"), source) else null
47: is DesktopExternalAssemblerRunResult.TimedOut -> DesktopSc61AssemblyResult.Failure("Assembler timed out", run.log)
55: if (run.code < 0 || !run.log.lineSequence().any { it.trim() == "ASSEMBLY COMPLETE, NO ERRORS FOUND" } ||
56: Regex("""(?m)^\s*LINE:\d+\s+ERROR CODE:""").containsMatchIn(run.log))
57: return DesktopSc61AssemblyResult.Failure("SC61 assembly failed (exit code ${run.code})", run.log)
58: if (!dump.isFile) return DesktopSc61AssemblyResult.Failure("SC61 did not generate a DMP file", run.log)
59: if (dump.length() > 2 * 1024 * 1024) return DesktopSc61AssemblyResult.Failure("SC61 DMP exceeds 2 MiB", run.log)
62: return DesktopSc61AssemblyResult.Failure("Invalid SC61 DMP: ${parsed.error}", run.log)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/project/DesktopSc61Listing.kt

```text
27: private val numbered = Regex("""^(\d{5}):([0-9A-Fa-f]{4})  (.*)$""")
28: private val continuation = Regex("""^ {12}([0-9A-Fa-f]{2,8})[\s]*$""")
29: private val symbol = Regex("""([A-Za-z_@][A-Za-z0-9_@]*)\s*:\s*([0-9A-Fa-f]{4})h""")
30: private val declaration = Regex("""^\s*([A-Za-z_@][A-Za-z0-9_@]*)\s*:\s*(.*)$""")
35: }.getOrElse { java.nio.charset.Charset.forName("windows-31j").decode(ByteBuffer.wrap(bytes)).toString() }
46: if (line.contains(" - MAP LIST OF [")) { inMap = true; return@forEach }
73: cycles = if (rest.firstOrNull()?.matches(Regex("""[0-9]+(?:[/+~\-][0-9]+)*""")) == true) rest.removeAt(0) else ""
74: content = rest.joinToString(" ").trim()
86: if (it.groupValues[2].startsWith("EQU", true)) Sc61860AssemblySymbolKind.CONSTANT else Sc61860AssemblySymbolKind.LABEL
94: val error = Regex("""(?m)^\s*LINE:(\d+)\s+ERROR CODE:\d+.*$""").find(log) ?: return null
113: Regex("""(?i)^\s*(?:[A-Za-z_@][A-Za-z0-9_@]*\s*:)?\s*INCLUDE\s*\(([^)]+)\)""").find(line)?.let {
131: else if (c == '\'' || c == '"') { quote = c; append(c) }
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomHistory.kt

```text
30: const val PATH_KEY = "lastRomPath"
31: const val MACHINE_ID_KEY = "lastRomMachineId"
47: private val preferences = Preferences.userRoot().node("com/digihori/pgp/desktop")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomLibrary.kt

```text
39: sourceName = objectValue.getValue("sourceName").jsonPrimitive.content,
40: importedAt = objectValue.getValue("importedAt").jsonPrimitive.content,
41: packageSha256 = objectValue.getValue("packageSha256").jsonPrimitive.content,
48: return DesktopRomLibraryResult.Failure("Invalid ROM package: ${read.error}")
52: return DesktopRomLibraryResult.Failure("The ROM package does not create a valid emulator session")
60: put("machineId", machineId.value)
61: put("sourceName", sourceName)
62: put("importedAt", Instant.now().toString())
63: put("packageSize", packageBytes.size)
64: put("packageSha256", packageBytes.sha256())
65: put("validated", true)
84: val temporary = File(destination.parentFile, ".${destination.name}.tmp")
98: private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
99: .digest(this).joinToString("") { "%02x".format(it) }
102: const val PACKAGE_NAME = "active.pgrom"
103: const val METADATA_NAME = "metadata.json"
106: val home = System.getProperty("user.home")
107: val os = System.getProperty("os.name").lowercase()
109: os.contains("mac") -> File(home, "Library/Application Support")
110: os.contains("win") -> System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
111: else -> System.getenv("XDG_DATA_HOME")?.let(::File) ?: File(home, ".local/share")
113: return File(dataRoot, "Pokecom GO Studio/rom-library")
118: MachineFamily.PC_1251 -> "pc-1251-family"
120: MachineFamily.PC_1261 -> "pc-1261"
121: MachineFamily.PC_1401 -> "pc-1401"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomLocator.kt

```text
11: const val PC1245_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1245_ROM"
12: const val PC1251_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1251_ROM"
13: const val PC1360_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1360_ROM"
14: const val PC1261_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1261_ROM"
15: const val DEFAULT_PC1245_ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"
16: const val DEFAULT_PC1251_ROM_PATH: String = "local-data/roms/pc-1251/pc1251mem.bin"
17: const val DEFAULT_PC1360_ROM_PATH: String = "local-data/roms/pc-1360/pc1360mem.bin"
18: const val DEFAULT_PC1261_ROM_PATH: String = "local-data/roms/pc-1261/pc1261mem.bin"
22: workingDirectory: File = File(System.getProperty("user.dir")),
27: workingDirectory: File = File(System.getProperty("user.dir")),
32: workingDirectory: File = File(System.getProperty("user.dir")),
34: ?.takeIf { File(it.parentFile, "pc1360bank.bin").isFile }
38: workingDirectory: File = File(System.getProperty("user.dir")),
41: const val PC1401_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1401_ROM"
42: const val DEFAULT_PC1401_ROM_PATH: String = "local-data/roms/pc-1401/pc1401mem.bin"
45: workingDirectory: File = File(System.getProperty("user.dir")),
51: workingDirectory: File = File(System.getProperty("user.dir")),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomPackage.kt

```text
46: DesktopRomPackageError.InvalidManifest(error.message ?: "Invalid UTF-8 or JSON"),
50: DesktopRomPackageError.InvalidManifest(error.message ?: "Invalid JSON"),
75: file = "rom/${component.id.value}.bin",
131: DesktopRomPackageError.InvalidZip(error.message ?: "Invalid ZIP archive"),
159: removeSuffix("/").split('/').none { it.isEmpty() || it == "." || it == ".." }
161: private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
162: .digest(this).joinToString("") { "%02x".format(it) }
164: private const val MANIFEST_PATH = "manifest.json"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomPackageConverter.kt

```text
48: DesktopRomPackage.write(imported.romSet, title = "${machineId.value.uppercase()} ROM"),
62: is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(DesktopRomPackage.write(imported.romSet, title = "PC-1450 ROM"))
66: is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(DesktopRomPackage.write(imported.romSet, title = "PC-1460 ROM"))
70: is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(DesktopRomPackage.write(imported.romSet, title = "PC-1470U ROM"))
75: DesktopRomPackage.write(imported.romSet, title = "${machineId.value.uppercase()} ROM"),
81: is RomImportResult.Success -> DesktopRomPackageConversionResult.Success(DesktopRomPackage.write(imported.romSet, title = "${machineId.value.uppercase()} ROM"))
95: DesktopRomPackage.write(imported.romSet, title = "PC-1245 ROM"),
107: DesktopRomPackage.write(imported.romSet, title = "${machineId.value.uppercase()} ROM"),
118: DesktopRomPackage.write(imported.romSet, title = "PC-1350 ROM"),
126: DesktopRomPackage.write(imported.romSet, title = "PC-1261 ROM"),
134: DesktopRomPackage.write(imported.romSet, title = "PC-1360 ROM"),
143: DesktopRomPackage.write(imported.romSet, title = "PC-1245 ROM"),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomRegistrationCatalog.kt

```text
12: MachineFamily.PC_1251 -> definition.id.value == "pc-1251"
13: MachineFamily.PC_1261 -> definition.id.value == "pc-1261"
14: MachineFamily.PC_1401 -> definition.id.value == "pc-1401"
20: MachineFamily.PC_1251 -> "PC-1250 / PC-1251 / PC-1255 family"
21: MachineFamily.PC_1261 -> "PC-1261 family"
22: MachineFamily.PC_1401 -> "PC-1401 family"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomSetCreator.kt

```text
19: file == null -> "Select a file."
20: !file.isFile || !file.canRead() -> "The file cannot be read."
21: file.length() != expectedSize -> "Expected ${expectedSize / 1024} KiB; selected ${file.length()} bytes."
25: fun destination(file: File): File = if (file.extension.equals("pgrom", ignoreCase = true)) file
26: else File(file.parentFile, "${file.name}.pgrom")
32: require(target != internal.canonicalFile && target != external.canonicalFile) { "The output must not replace an input ROM." }
33: require(overwrite || !target.exists()) { "The destination already exists." }
37: is DesktopRomPackageConversionResult.Failure -> error("ROM validation failed: ${conversion.error}")
39: val temporary = Files.createTempFile(target.parentFile.toPath(), ".pgrom-", ".tmp")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/rom/DesktopRomSetWindow.kt

```text
28: StudioWindow(id = "create-rom-set", title = uiText("romSet.title"), onCloseRequest = { if (!busy) onClose() }) {
53: result.onSuccess { created = it }.onFailure { error = it.message ?: uiText("romSet.failed") }
61: Text(uiText("romSet.title"), style = MaterialTheme.typography.headlineSmall)
62: Text(uiText("romSet.help"))
64: OutlinedButton(enabled = !busy, onClick = { machineMenu = true }) { Text(uiText("romSet.machine" , definition.displayName)) }
77: RomFileField(uiText("romSet.internal"), internal, !busy,
79: chooseRomSetFile(window, uiText("romSet.selectInternal"), "internal.bin")?.let { internal = it; changed() }
83: RomFileField(if (banked) uiText("romSet.externalBanked" , (externalSize / 1024).toString(), banking.bankCount.toString()) else uiText("romSet.external" , (externalSize / 1024).toString(), externalSize.toString()),
85: chooseRomSetFile(window, uiText("romSet.selectExternal"), if (banked) "${machineId.value.replace("-", "").removeSuffix("u")}bank.bin" else "external.bin")?.let { external = it; changed() }
87: if (banked) Text(uiText("romSet.bankHelp" , (banking.bankCount - 1).toString()), style = MaterialTheme.typography.bodySmall)
88: RomFileField(uiText("romSet.destination"), destination, !busy) {
89: chooseRomSetFile(window, uiText("romSet.save"), destination?.name ?: "${machineId.value}.pgrom", FileDialog.SAVE,
93: created?.let { Text(uiText("romSet.created" , it.absolutePath), color = MaterialTheme.colorScheme.primary) }
99: Text(if (busy) uiText("romSet.creating") else uiText("romSet.create"))
102: OutlinedButton(onClick = { if (onRegister(file, machineId)) onClose() else error = uiText("romSet.registrationFailed") }) {
103: Text(uiText("romSet.register"))
106: TextButton(enabled = !busy, onClick = onClose) { Text(uiText("romSet.close")) }
111: title = { Text(uiText("romSet.replaceConfirm")) },
113: confirmButton = { TextButton(onClick = { overwriteConfirmation = false; create(true) }) { Text(uiText("romSet.replace")) } },
114: dismissButton = { TextButton(onClick = { overwriteConfirmation = false }) { Text(uiText("romSet.cancel")) } },
127: Text(file?.absolutePath ?: uiText("romSet.select"), modifier = Modifier.weight(1f))
128: Text(uiText("romSet.browse"))
131: if (file != null) Text(error ?: uiText("romSet.selected"), color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/runner/DesktopEmulatorRunner.kt

```text
335: override fun pressKey(key: PocketKey): InputResult = pressHostKey(key, "screen:${key.name}")
349: override fun releaseKey(key: PocketKey): InputResult = releaseHostKey(key, "screen:${key.name}")
358: if (hostKeys.press("gamepad:${button.name}", key) is InputResult.Accepted) {
365: releaseHostKey(key, "gamepad:${button.name}")
386: check(state != RunnerState.FAULTED) { "Cannot run input while the emulator is faulted" }
401: check(partialResult.executedCycles > 0) { "Session made no progress" }
464: require(now >= previous) { "Monotonic clock moved backwards" }
495: check(partialResult.executedCycles > 0) { "Session made no progress" }
686: is KeyTransition.Press -> hostKeys.press("queue", transition.key)
687: is KeyTransition.Release -> hostKeys.release("queue")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/runner/DesktopExecutionSpeed.kt

```text
7: LEVEL_1("1 — 80%", SpeedRatio(4, 5)),
8: LEVEL_2("2 — 90%", SpeedRatio(9, 10)),
9: LEVEL_3("3 — 100% (Standard)", SpeedRatio.NORMAL),
10: LEVEL_4("4 — 110%", SpeedRatio(11, 10)),
11: LEVEL_5("5 — 120%", SpeedRatio(6, 5)),
12: UNLIMITED("Unlimited (no wait)", SpeedRatio.NORMAL),
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/source/DesktopSourceHeader.kt

```text
16: val normalized = source.removePrefix("\uFEFF")
21: val line = rawLine.removeSuffix("\r")
23: description += line.trimStart().removePrefix("#").removePrefix(" ")
24: ""
29: }.joinToString("\n")
30: return DesktopSourceHeader(description.joinToString("\n").trim(), body)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/theme/StudioTheme.kt

```text
33: error("Studio component colors are only available inside PgpStudioTheme")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/wav/DesktopWavOperation.kt

```text
23: val content: String = "",
34: append("${severity.name} ${stage.name} / ${code.name}: $message")
35: range?.let { append(" [${it.kind.name} ${it.offset}..${it.endExclusive}]") }
36: if (details.isNotEmpty()) append(" ${details.entries.joinToString { "${it.key}=${it.value}" }}")
42: return DesktopWavResult("Unsupported target", error = "Only OLD WAV is currently implemented for ${machine.value}.")
46: is DesktopMemoryDumpLoadResult.Failure -> return DesktopWavResult("Invalid memory dump", error = parsed.error.toString())
50: is DesktopContiguousMemoryImageResult.Failure -> return DesktopWavResult("Invalid memory image", error = flattened.message)
56: is DesktopBasicProgramCompileResult.Failure -> return DesktopWavResult("BASIC conversion failed", error = result.error.toString())
60: is DesktopOldBasicTransferResult.Failure -> return DesktopWavResult("BASIC conversion failed", error = adapted.message)
66: is CodecResult.Failure -> DesktopWavResult("Encode failed", encoded.diagnostics)
71: summary = "Encoded for ${machine.value}\n${inspected.summary}",
85: is CodecResult.Failure -> return DesktopWavResult("WAV container failed", read.diagnostics)
89: is CodecResult.Failure -> return DesktopWavResult("PCM normalization failed", diagnostics + normalized.diagnostics)
92: val summary = "${wav.format.sampleRate} Hz / ${wav.format.channelCount} channel(s) / ${wav.format.bitsPerSample}-bit\n" +
93: "${pcm.samples.size} samples / ${pcm.samples.size.toDouble() / pcm.sampleRate} seconds"
95: is CodecResult.Failure -> return DesktopWavResult(summary + "\nOLD signal failed", diagnostics + decoded.diagnostics, pcm)
99: summary + "\n${signal.rawBytes.size} raw bytes / ${signal.syncPoints.size} nibble sync points",
106: val name = payload.filename.ifBlank { "program" }
108: summary = partial.summary + "\nValidated OLD ${payload.type.name}, filename: ${payload.filename}\n" +
109: "${payload.body.size} logical bytes" + (payload.startAddress?.let { " / ${hex(it)}..${hex(payload.endAddress!!)}" } ?: ""),
114: return validated.copy(programName = "$name.dmp", program = dump.encodeToByteArray(), content = dump)
117: return validated.copy(error = "Select an OLD target machine to interpret BASIC tokens.")
124: is DesktopBasicProgramDecodeResult.Success -> validated.copy(programName = "$name.bas", program = decoded.utf8Bytes, content = decoded.utf8Bytes.decodeToString())
125: is DesktopBasicProgramDecodeResult.Failure -> validated.copy(error = "BASIC at ${decoded.error.offset}: ${decoded.error.message}")
126: is DesktopBasicProgramDecodeResult.GenericFailure -> validated.copy(error = "BASIC at ${decoded.offset}: ${decoded.message}")
132: "${hex(row * 16)}  " + bytes.joinToString(" ") { (it.toInt() and 255).toString(16).uppercase().padStart(2, '0') }
133: }.joinToString("\n")
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/wav/WavToolPanel.kt

```text
35: var filename by remember { mutableStateOf("") }
40: var settingsAtRun by remember { mutableStateOf("") }
41: var status by remember { mutableStateOf("") }
47: var view by remember { mutableStateOf("Content") }
52: val settings = "${format?.name ?: "UNKNOWN"}/$filename/$type12/$decode"
54: MachineBasicDialect.S1 -> "pc-1350"
55: MachineBasicDialect.S2 -> "pc-1360"
56: else -> "pc-1245"
63: decode = value.file.extension.equals("wav", true)
65: status = ""
82: val destination = choose(owner, "Save $name", name, FileDialog.SAVE) ?: return
84: onSuccess = { status = "Saved: ${destination.absolutePath}"; if (generated) unsaved = false },
85: onFailure = { status = "Save failed: ${it.message}" },
90: Text("WAV Tool", style = MaterialTheme.typography.titleLarge)
91: Text("S1/S2対応は開発中です。S1/S2のWAV変換にはpcwavツールを使用してください。")
92: Text("Studio • OLD 02 / 12 / 62 • file conversion and analysis")
95: choose(owner, "Create WAV — open BASIC or memory dump", "*.bas;*.dmp")?.let {
96: if (it.extension.lowercase() !in setOf("bas", "dmp")) status = "Choose a .bas or addressed .dmp file."
97: else open(WavToolInput(it, "Existing file", inputMachine(), System.nanoTime()))
99: }) { Text("Create WAV…") }
101: choose(owner, "Open WAV", "*.wav")?.let { open(WavToolInput(it, "Existing file", inputMachine(), System.nanoTime())) }
102: }) { Text("Open WAV…") }
105: SelectionContainer { Text("${source.origin}\n${source.file.absolutePath}\nSaved file contents are used; external editor changes require another conversion.") }
106: Text("${if (decode) "Decode / analyze" else "Encode"} • signal: OLD (S1/S2 not implemented) • hardware compatibility not verified")
110: Text((if (format == candidate) "✓ " else "") + candidate.name +
111: (if (candidate != MachineBasicDialect.OLD) " (not implemented)" else ""))
115: if (format != MachineBasicDialect.OLD) Text("${format?.name ?: "Unknown"} WAV is not implemented. Select OLD explicitly to use the OLD codec.")
117: OutlinedTextField(filename, { filename = it }, enabled = !busy, label = { Text("Pocket computer filename") }, singleLine = true)
118: if (source.file.extension.equals("bas", true)) Row {
120: Text("BASIC type 12 (advanced; password entry not supported)")
137: else DesktopWavOperation.encode(bytes, source.file.extension.equals("dmp", true), selectedMachine, selectedFilename, selectedType12)
138: }.getOrElse { DesktopWavResult("Operation failed", error = it.message ?: it.toString()) }
144: "Stopped at an error. Available partial results are retained."
145: else "Conversion complete. Review the result before saving."
148: }) { Text(if (busy) "Processing…" else if (decode) "Analyze / reanalyze" else "Convert") }
149: if (busy) OutlinedButton(onClick = { job?.cancel(); status = "Cancelled; previous completed result retained." }) { Text("Cancel") }
151: } ?: Text("Create WAV from a .bas/.dmp file, or open WAV to decode and inspect it.")
154: if (settings != settingsAtRun) Text("Settings changed — rerun required. Result below uses: $settingsAtRun")
157: output.wav?.let { wav -> Button(onClick = { save("${output.payload?.filename?.ifBlank { "program" } ?: "program"}.wav", wav.copyBytes(), true) }) { Text("Save WAV…") } }
158: output.program?.let { bytes -> Button(onClick = { save(output.programName!!, bytes, true) }) { Text("Save program…") } }
159: output.raw?.let { raw -> OutlinedButton(onClick = { save("raw-transfer.bin", raw.copyBytes(), false) }) { Text("Export raw…") } }
160: OutlinedButton(onClick = { save("wav-analysis.txt", ("Input: ${input?.file?.absolutePath}\nSettings: $settingsAtRun\n" + output.report()).encodeToByteArray(), false) }) { Text("Export report…") }
162: Text("Raw exports are analysis data, not a validated Program. BASIC/.dmp exports do not preserve all transfer metadata.")
164: listOf("Content", "Raw", "Waveform").forEach { label -> OutlinedButton(onClick = { view = label }) { Text(label) } }
167: "Content" -> {
168: if (output.content.length > 16000) Text("Preview limited to 16000 characters; saved program contains the full result.")
169: SelectionContainer { Text(output.content.take(16000).ifBlank { "No program text available. Inspect raw and diagnostics." }, fontFamily = FontFamily.Monospace) }
171: "Raw" -> {
172: Text(if (output.payload == null) "Payload unvalidated" else "OLD payload validated")
175: OutlinedButton(enabled = rawPage > 0, onClick = { rawPage-- }) { Text("Previous bytes") }
176: OutlinedButton(enabled = (rawPage + 1) * 256 < raw.size, onClick = { rawPage++ }) { Text("Next bytes") }
181: Text("Raw $begin..$end of ${bytes.size} • select a row to locate its waveform")
185: Text(DesktopWavOperation.hex(offset) + "  " + bytes.copyOfRange(offset, rowEnd).joinToString(" ") {
188: mapping?.let { selectedSample = it.source.offset; view = "Waveform" }
193: "Waveform" -> WavWaveform(output, selectedSample, { selectedSample = it }, { byte -> rawPage = byte / 256; view = "Raw" })
195: Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
197: if (output.diagnostics.isEmpty()) Text("No codec diagnostics")
202: com.digihori.pgp.core.wav.CodecDataKind.PCM_SAMPLES -> { selectedSample = range.offset; view = "Waveform" }
203: com.digihori.pgp.core.wav.CodecDataKind.RAW_BYTES -> { rawPage = range.offset / 256; view = "Raw" }
214: onDismissRequest = { pendingClose = false }, title = { Text("Close unsaved result?") },
215: text = { Text("The current generated WAV/program has not been saved.") },
216: confirmButton = { TextButton(onClick = { pendingClose = false; onClose() }) { Text("Close") } },
217: dismissButton = { TextButton(onClick = { pendingClose = false }) { Text("Keep open") } },
220: onDismissRequest = { pending = null }, title = { Text("Replace unsaved result?") },
221: text = { Text("The current generated WAV/program has not been saved.") },
222: confirmButton = { TextButton(onClick = { pending?.let { accept(it) }; pending = null }) { Text("Replace") } },
223: dismissButton = { TextButton(onClick = { pending = null }) { Text("Keep current") } },
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/wav/WavWaveform.kt

```text
35: if (pcm == null || samples == null || samples.isEmpty()) { Text("No PCM available"); return }
74: Checkbox(stepDisplay, { stepDisplay = it }); Text("Sample hold (square wave)")
76: Text(if (stepDisplay) "Horizontal hold and vertical sample edges" else "Connected samples — no smoothing or resampling")
77: Text("Signed PCM units: min ${statistics.first}, max ${statistics.second}, DC ${format(statistics.third, 1)}")
78: Row { Checkbox(amplitudeGuides, { amplitudeGuides = it }); Text("Amplitude guides (visual only)") }
80: Text("Amplitude guides ±${guide.roundToInt()} — not the OLD decoder threshold")
83: Row { Checkbox(showDifference, { showDifference = it }); Text("Show OLD decoder difference / threshold") }
84: Text("OLD uses x[n] − x[n−${inspection.tapSamples}], |difference| > ${inspection.threshold}; zero crossings alone do not determine decoding.")
85: Text("Samples $start..${start + span} • cursor $cursor (${time(cursor.toDouble() / pcm.sampleRate)}) • value ${samples[cursor.coerceIn(samples.indices)]}")
89: Text("Blue: PCM amplitude (±32768); bold center line is 0")
91: Text("Orange: decoder difference (separate scale ±65536), dashed lines: difference threshold ±${inspection.threshold}")
93: Text("Difference is 0 in this interval: samples repeat at the decoder tap. The PCM waveform remains visible.")
124: label((if (amplitude > 0) "+" else "") + amplitude.roundToInt(), Offset(0f, y(amplitude) - 7f))
135: val tickLabel = if (seconds < 1) "${format(tick * 1000, (tickDecimals - 3).coerceAtLeast(0))} ms" else "${format(tick, tickDecimals)} s"
139: label("PCM", Offset(0f, 0f))
214: OutlinedButton(onClick = { zoom(span / 2) }) { Text("Zoom +") }
215: OutlinedButton(onClick = { zoom((span.toLong() * 2).coerceAtMost(samples.size.toLong()).toInt()) }) { Text("Zoom −") }
216: OutlinedButton(onClick = { start = (start - span / 2).coerceAtLeast(0) }) { Text("←") }
217: OutlinedButton(onClick = { start = (start + span / 2).coerceAtMost(samples.size - span) }) { Text("→") }
218: OutlinedButton(onClick = { start = 0; span = samples.size }) { Text("Fit") }
220: Text("Two-finger horizontal scroll or Shift+wheel to pan; scrollbar covers the whole recording. Zoom in for samples/zero crossings (green).")
221: Text("Tap for cursor; long press for selection end. Purple marks: nibble sync.")
222: selectionEnd?.let { Text("Selected samples ${minOf(cursor, it)}..${maxOf(cursor, it)}") }
224: Text(raw?.let { "Cursor corresponds to raw byte ${it.target.offset}" } ?: "Cursor is outside decoded byte ranges")
225: raw?.let { OutlinedButton(onClick = { onRaw(it.target.offset) }) { Text("Show raw byte ${it.target.offset}") } }
228: private fun format(value: Double, decimals: Int) = String.format(Locale.ROOT, "%.${decimals}f", value)
229: private fun time(seconds: Double): String = if (abs(seconds) < 1.0) "${format(seconds * 1000, 3)} ms" else "${format(seconds, 3)} s"
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/window/DesktopUiPreferences.kt

```text
8: private val preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/ui")
11: DesktopExecutionSpeed.valueOf(preferences.get("executionSpeed", DesktopExecutionSpeed.LEVEL_3.name))
15: preferences.put("executionSpeed", value.name)
19: fun externalEditorCommand(): String? = preferences.get("externalEditorCommand", null)
22: preferences.put("externalEditorCommand", value.trim())
26: external = preferences.getBoolean("assemblerExternal", false),
27: executable = preferences.get("assemblerExecutable", ""),
28: arguments = preferences.get("assemblerArguments", "{source}"),
29: timeoutSeconds = preferences.getInt("assemblerTimeout", 60).coerceIn(1, 3600),
33: preferences.putBoolean("assemblerExternal", value.external)
34: preferences.put("assemblerExecutable", value.executable.trim())
35: preferences.put("assemblerArguments", value.arguments)
36: preferences.putInt("assemblerTimeout", value.timeoutSeconds)
40: preferences.getFloat("projectPaneWidthDp", defaultValue)
43: preferences.putFloat("projectPaneWidthDp", value)
47: preferences.getFloat("projectControlsHeightDp", defaultValue)
50: preferences.putFloat("projectControlsHeightDp", value)
54: preferences.getFloat("sourceInformationHeightDp", defaultValue)
57: preferences.putFloat("sourceInformationHeightDp", value)
61: HostKeyInputMode.valueOf(preferences.get("hostKeyInputMode", HostKeyInputMode.LOGICAL.name))
65: preferences.put("hostKeyInputMode", value.name)
```

### desktopApp/src/main/kotlin/com/digihori/pgp/desktop/window/DesktopWindowPlacement.kt

```text
17: private val preferences: Preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/windows"),
21: val width = node.getInt("width", -1)
22: val height = node.getInt("height", -1)
25: node.getInt("x", 0),
26: node.getInt("y", 0),
29: node.getBoolean("maximized", false),
36: putInt("x", bounds.x)
37: putInt("y", bounds.y)
38: putInt("width", bounds.width)
39: putInt("height", bounds.height)
40: putBoolean("maximized", maximized)
```



## ステップ10の最終確認

固定keyによる文言取得781箇所のkey存在・引数数を確認し、不一致なし。日英863 keyの一致・重複・引数検査と32件のローカライズtests成功。直接表示する原文のCopyright、INSTRUMENT、RUN／PRO／RSV、CPU、矢印、MCP tool名、保存用ROM package名は今回の原文維持項目として確認した。棚卸しの行番号・直書き一覧は開始時点のスナップショットであり、現行コードの残存一覧とは扱わない。

ユーザー修正を反映し、日本語は「ドットエディター」「OSの設定に従う」、外部エディターはエラー／警告の該当行を開く説明、入力切替はメイン下部の設定、ROMダンプはバイナリー形式とした。外部Assembler設定の冒頭で現時点のsc61専用実装を説明する。

最終の表示・互換性結果と未確認事項は[検証記録](STUDIO_LOCALIZATION_VERIFICATION.md)。本番実操作・他OS実表示と独立描画の文字欠けは未確認／未解決として維持する。


## 追加の表示言語設定

ユーザー追加指示により「OSの設定に従う／日本語／English」の保存を実装し、アプリ再起動後に反映する。実行中のUI言語は固定する。言語名の日本語／Englishは各言語自身の表記で表示し、保存値system／ja／enとuiLanguage keyは翻訳しない。日英870 key・36 tests・固定key785箇所の検査成功。詳細は[追加指示の記録](STUDIO_LOCALIZATION_PLAN.md#17-追加指示設定の表示言語2026-10-14)。
