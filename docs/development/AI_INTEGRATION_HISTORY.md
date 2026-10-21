# AI連携の初期構想・実装順・検証履歴

2026-10-22に現行ガイドから分離した計画・検証履歴です。
各節の「現在」「未実装」「後続」は記録時点を表し、現在の対応範囲を示しません。
現行の説明は[AI_INTEGRATION.md](../reference/AI_INTEGRATION.md)を参照してください。
元の本文を保持し、当時の未確認事項を後から完了扱いにはしていません。

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

## 7. 実装順

1. Assembly Workspace、Diagnostics、Listing、Symbol Mapを共通サービスとして整備する。
2. Debug SnapshotとTraceをversion付きの構造化データとして定義する。
3. localhost限定の読み取り専用MCPサーバーを実装する。
4. AIクライアントからBASIC作成と静的マシン語解析を行うE2Eシナリオを作る。
5. Breakpoint、Watch、Pause、Stepを承認付きで公開する。
6. ソース差分、Build、Load、Continueを段階的に公開する。
7. 必要性を確認した後に、内蔵AI Providerと認証方式を検討する。

## 11. マシン語生成の進め方と当面の優先順位（2026-10-21）

本節は再開時の進め方の記録であり、今回の実装着手を意味しない。
当面はiOSの実装を優先し、AIによるマシン語生成の機能追加は後続とする。
Studioはユーザーの認識としてデバッグ機能の充実を除き概ね一区切りであり、
既存の未完了項目をすべて完了扱いに変更するものではない。

### 機種仕様をStudioから取得する

機種仕様はStudio側で管理し、AIがMCP経由で必要な情報を問い合わせる構成を目指す。
固定の機種仕様（CPU、命令、LCD、キー入力、タイミング）と、現在のProject／Sessionの状態
（使用アセンブラ、RAM構成、使用領域、ロード済み成果物）を分ける。
既に実装にある機種定義・メモリマップは同じ定義から公開し、AI向けの別表への転記による乖離を避ける。
描画・入力手順、起動・復帰、ROMルーチンの呼出規約は不足分を補い、機種・ROM版への依存と
確認済み／未確認を明示する。これらの生成向けAPIは今後の実装対象であり、現在の読み取りAPIで
すべて取得できるわけではない。

### 小さなユースケースで段階的に確認する

最初からゲーム全体や全操作の自動化を目指さず、1機種・1アセンブラを選び、
生成・実行で不足した情報やAPIを追加していく。機種・アセンブラの具体的な選択は再開時に決める。

| 段階 | ユースケース | 主に確認すること |
| --- | --- | --- |
| 1 | マシン語で`HELLO WORLD`を表示して終了 | 仕様取得、ソース生成、Build・Load、起動と復帰 |
| 2 | 表示位置の変更・画面消去 | LCDの座標・画面構成・描画方法 |
| 3 | キーで表示を変更 | キー読取り、ループ、終了操作 |
| 4 | 文字・小さな図形を移動 | 待ち時間、画面更新、残像処理 |
| 5 | 簡単なゲーム・デモ | 描画・入力・時間制御の組合せ |

最初の合格条件は「AIがStudioから必要な仕様を取得してAssemblyソースを生成し、
ユーザーがStudioでBuild・Load・実行して表示と終了を確認できること」とする。
ROMの文字表示ルーチンを呼ぶかLCDへ直接描画するかは再開時に決め、必要な仕様と依存を確認する。
結果を踏まえて次の段階を決め、初回から実行操作の自動化を必須にしない。

### 必要に応じて追加する連携

1. 機種仕様・Project・アセンブラ設定の取得と、確認済み最小サンプルを整える。
2. ソース差分の提示・適用、共通Build呼出し、構造化Diagnostics取得を接続し、修正の往復を可能にする。
3. 手動操作での評価後、Load・実行・停止・Breakpoint等と、LCD画像・キー操作・実行上限を段階追加する。

状態変更は既存の承認・revision検証・操作履歴の方針に沿って設計する。
Claude Codeの接続・認証更新・設定案内・実接続検証は共通APIと分けて扱う。
内蔵チャットUIや全機種対応は初回ユースケースの成立条件に含めない。

## 2026-10-21: 条件付きBreakpointとの関係

Studio UIに値条件・ヒット回数条件を追加した。`pgp_get_debug_registrations`のversion 1は
引き続きID・アドレス・有効状態だけを返し、条件と到達回数は含まない。アドレスの取得だけで
無条件停止を保証しない。既存MCP tool名・引数・JSON形状は維持し、条件を含むAPIは後続のAI連携で設計する。


## 13. 作業順9: PC-1245 Assembly HELLO WORLD

対象をPC-1245と内蔵SC61860 assemblerに固定した最小サンプルを
[`test-data/assembly/pc1245-hello`](../../test-data/assembly/pc1245-hello/README.md) に用意した。
`pgp_get_machine_spec` の `display.directDrawing` に、この機種に限り列アドレス式、
画素ビット順、LCD有効化手順、シンボル除外アドレスを追加。他機種は `null`。
根拠はPGP Coreの `Pc1245Display` / `Pc1245Io` / `displaySnapshot` であり、実機仕様の独立検証ではない。

ROM文字表示ルーチンを呼ばず、独自5×7字形をLCDへ書く。BASIC `CALL 50432` から入り、
147 bytesのAssemblyが描画を約3秒保持してRTNする。実ROMではCALL中にLCDが無効になるため
`LIP 0x5F; ORIM 0x01; OUTC` が必要。上位RAMのBASIC作業領域への書き込みを実ROMで検出したため、コードと字形を0xC600未満へ収める。
通常のBASIC CALLで得られるスタックを前提とし、任意PCジャンプによる起動は対象外。

自動検証は実際のProject Build/Load処理、合成呼び出し元の復帰・全画素照合と、
ローカル実ROMのBASIC RUN/CALL・描画・RTNを対象とする。ROMなしのCIでは後者をskipする。
初回は仕様APIと同一のサービスをテストから取得して検証し、後述の追試で実クライアントのMCP接続と再生成版のロード済みbyte照合まで確認した。
Source書換え、Build、Load、実行を行うMCP mutationは追加しない。

画面確認でも、プロジェクトのBuild/Load（BASIC 15 bytes / Assembly 147 bytes、2 segments）、
RUNによる約3秒のHELLO WORLD表示と、終了後のキーボード入力による `1+1=2` を確認した。復帰後の通常演算は、実ROMテストで
CALL前後の `1+1` のLCD全画素一致を確認する。

2026-10-21 MCP実接続の追試：CodexのPGP connectorからcapabilities、PC-1245 machine spec、
開いているprojectがない場合のmetadata / diagnosticsを取得できた。LCD列アドレス式、有効化手順、
コードと字形を0xC600未満へ置く制約を実際のMCP応答で確認。字形tableを0xC590へ配置した
再生成版は、`PGP_HELLO_PROJECT` を使い共通Build/Load・全画素・CALL/RTN・復帰後の演算比較の
2テスト（実ROMを含む）で成功。通常の全buildとCoreテストも成功。
ユーザーが同じ接続先へ再生成版をBuild/Loadし、一時停止した後に追試を完了した。
MCP metadataはPC-1245 / SC61860_BUILTIN / UP_TO_DATE / paused、source変更なし、diagnostics 0件。
Assembly metadataはfreshness=current、8 symbols / 76 listing rowsを返した。
Debug ContextからRAMの0xC500..0xC556（code 87 bytes）と0xC590..0xC5CB（glyph 60 bytes）を取得し、
Studioのbuild/hello.dmpと全147 bytesが一致。要求間のsession ID / revisionも同一で、warningsは空。
現在PCはROM内なのでloadedLocationは未対応を返したが、これは現在位置のソース対応を表し、
ロード失敗を意味しない。ロード内容は上記のRAM byte比較で確認した。
MCP上でのビルド・実行mutationは行っていない。実行・全画素・CALL/RTN・復帰後の演算は
再生成版の実ROM受け入れテストで確認済み。これにより作業順9の実接続確認を完了。


## 18. CI追試とOS間パス照合の修正（2026-10-22）

初回統合commitのCIではmacOS／Linux／Androidが成功し、WindowsのProject外リンク拒否テストが
失敗した。`File.canonicalFile` のリンク解決に依存していたため、読取APIのProject境界を
NIO `toRealPath()` で照合するよう修正。未作成ファイルの診断は既存の祖先を解決し、
残りの相対要素を付けて範囲を検証する。既存の外部リンク拒否・メタデータのパス非公開・
未作成ファイルのページ上限テストを維持する。

iOSはSwift 49件が失敗0で `TEST SUCCEEDED`、結果artifactもアップロード済みだった。
ジョブ全体の20分制限へGradleキャッシュ後処理中に到達し、cancelledとなった。
アプリのテスト失敗とは区別し、iOSジョブを30分枠へ変更、キャッシュ自動整理を無効化して
後処理でシステムGradleを追加起動しないよう調整した。

## API拡充・承認操作の検証記録

### 読み取り登録APIの検証

JDK 21・macOSローカルの全体buildとCore testsは成功。
Studio 312件は失敗0（既存SDL条件付きskip 1件）、Core 435件は変更なしの既存成功結果を再利用。
新規3件で全監視種別・無効登録・空一覧・Session ID／revision・取得前後のContext JSON不変・
Snapshotの独立性・Reset時のID保持・引数拒否・実行中の非停止・Sessionなし・Fault時取得を確認。
既存MCPサーバーテストへ認証付きHTTPのtools/list・新tool呼出しを追加し、既存toolと認証テストも成功。
自動検証は日英リソース整合テストを含む。実設定・ROM Library・本番MCPサーバーは使用せず、合成Sessionとテスト専用サーバーで検証した。
Windows／Linux／iOSの今回のCI・実機確認は未実施。packageVersionは1.3.1を維持。

2026-10-14にmacOS本番StudioとAIクライアントの実接続でも、引数なしの新tool呼出しが成功した。
一時停止中のPC-1470Uから、有効なブレークポイント1件と値変更監視1件、Session ID／revision、
logicalAllBanks、schemaVersion 1を取得できた。ユーザーからも動作に問題なしとの確認を得た。
この確認では登録・実行・設定を変更せず、接続と基本取得だけを確認した。
無効登録・編集後の再取得・実行中拒否・Reset後の保持は自動テストでの確認であり、本番操作確認とは区別する。
実Session ID・revision・取得内容は共有文書へ保存しない。

### 接続と検証

既存のenabled_toolsが旧3ツールに限定されている場合、Studioの`AI / MCP Server`で
`Copy Codex config`を再取得するか、新2ツールを追加する。設定の自動書換えはしない。
新ツールも`readOnlyHint=true`、`destructiveHint=false`、`openWorldHint=false`。

合成Project／Sessionと一時HTTPサーバーで、全機種定義・命令metadata、Projectなし／Sessionなし、
ページング、条件・回数・Step状態、旧schemaの維持、取得前後の不変性、
Load済み対応、Source変更・命令byte不一致、symlink境界、サイズ／文字コードの未確認、入力不正を検証する。
実ROM・本番MCP・外部AIクライアントでの実接続受入れは別途確認する。
作業順9のサンプルと検証は次節を参照。後続は作業順10のSource差分とBuild診断の往復。

2026-10-21検証結果：JDK 21で`./gradlew build :core:desktopTest`成功。
Desktop 380件、失敗0、既存条件付きskip 1件。認証付きHTTPで新2ツールを取得・呼出しし、
日英・広幅／狭幅のMCP画面を隔離描画した。既存APIのJSONと取得前後のSession不変性も確認した。
本番接続設定・実ROM・ROM Library・本番MCPサーバーは変更／使用していない。

### 検証

構文エラーを含む差分→Studio承認→共通Build失敗→構造化診断→修正差分→再Build成功の往復を検証。
その前後でエミュレーターのPCと対象RAMが変わらないことを確認する。
承認前の無変更、却下、競合、Project切替、外部assembler、manifest変更、link、上限・文字コード、
複数置換の途中失敗、Build中の変更、MCPからの承認不可、認証付き一時HTTPでの要求・状態取得も検証する。
日英の確認画面を広幅・狭幅で隔離描画した。本番MCPでの人による承認の往復は、最新Studioと
クライアント設定を反映した後の実接続受け入れとして別途確認する。

2026-10-21検証結果：JDK 21で `./gradlew build :core:desktopTest` 成功。
Desktop 389件、失敗0、既存skip 1件。`git diff --check` 成功。


2026-10-21 実接続追試：表示保持をLIA 8からLIA 4へ短くする差分をMCPから提案し、
ユーザーがStudioで承認後にAPPLIEDを取得。新しいprojectRevisionでBuildを要求し、
再度Studioで承認後にSUCCEEDEDを取得した。出力dumpはLIA 4、MCPで取得したロード済みRAMは
LIA 8のままで、session revisionも変化なし。BuildだけでLoad・実行しないことを確認。

この追試で、operationは最新なのにdevelopment metadata / diagnosticsの参照状態が古いまま残る
Compose配線の不整合を検出した。SideEffectの中だけでCompose stateを読むと、その変更が
再compositionの購読対象にならないため、公開snapshotが更新されなかった。
状態をcomposition中に評価し、commit後のSideEffectで公開するbindingへ修正した。
UIの別操作なしでSOURCE_CHANGED、busy、BUILD_FAILEDとdiagnostics、BUILT_NOT_LOADEDが
MCPへ届く回帰テストを追加。全build / CoreテストとDesktop 390件（失敗0、既存skip 1）成功。
最新版Studioを再起動し、同じProjectでMCP Buildを再要求、ユーザー承認後に実接続で再確認した。
operation=SUCCEEDED、project.buildStatus=BUILT_NOT_LOADED、busy=false、diagnostics 0件、
assembly freshness=currentを取得。metadataRevisionもBuild前から更新され、状態通知の修正を確認した。
この再確認ではエミュレーターは既にrunningであり、停止時のrevision不変性を測ったものではない。
Load未実施の状態も正しく返り、作業順10の実接続での差分承認・Build・成功診断取得を完了。
失敗診断からの修正往復・競合拒否は自動テストで検証済み。


### 検証と接続更新

合成Sessionで承認前の無変更、登録・更新・削除・却下、条件の保持、Watch停止、Traceと1命令Stepによる
RAM書込み、revision変更、Session交換、Reset・LoadによるPause無効化、Fault時Step拒否、履歴上限を検証する。
認証付き一時HTTPとAWT dispatcherで提案→ローカル承認→成功取得→新しいDebug Context取得→古いrevision拒否も検証する。
日英・広幅／狭幅でBreakpoint / Pause / Step / Traceの確認画面を隔離描画する。
本番の実ROM Sessionの状態や既存MCP設定は、この実装作業では変更していない。

実接続で使う場合は最新版Studioを起動し、MCP画面の「Codex設定をコピー」で追加2ツールを反映する。
公開toolは合計11。最新サーバーとクライアントを接続した後、人による承認の往復を実接続で確認する。

2026-10-22検証結果：JDK 21で `./gradlew build :core:desktopTest` 成功。
Desktop 398件、失敗0、既存skip 1件。日英・広幅／狭幅の4種類の承認画面を隔離描画・確認し、
`git diff --check` も成功。既存の本番MCP接続や実ROM Sessionの設定変更は行っていない。
