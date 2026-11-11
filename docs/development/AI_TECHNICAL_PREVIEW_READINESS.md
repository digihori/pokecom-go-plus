# AI連携 Technical Preview の受入れ記録

検証日：2026-11-11。対象：AI作業ブランチの統合コミット f5897db、macOS、PC-1245／PC-1251の登録済みROM。
[配布方針](AI_MCP_EXPANSION_PLAN.md#9-stabletechnical-previewの配布方針2026-11-02合意)に沿った開発中の記録。
実機、複数ROM版、他OSの保証ではない。

## 1. 統合後の起動・既存Project

全体ビルドとCoreテストは統合時に成功。JDK 21で `desktopApp:verifyDistributionRuntime` を実行し、
macOSアプリを再生成した。同梱のjdk.unsupportedとjdk.httpserverを確認。
PC-1245で既存numeric-labels Projectのコピーを開き、同梱stdioからソースを取得。
Build・Loadを別々に提案しStudioで承認した。本文95バイト、管理領域を含む書込み101バイト。
停止中にLoadが成功し、実行は開始しなかった。

## 2. PC-1251 CALLの切り分け

BASIC本文のLoad先が0xB830のセッションで、[再開メモ](AI_MCP_RESUME.md)の検証ルーチンを0xC500へBuild・Load。
通常のBASICプロンプトからのCALLと、途中停止から直ちに新規CALLを送る場合を比較した。
途中停止からの比較では同じルーチンの書込み値を0x6Bに変更し、前回の値0x5Aと区別した。
PC強制移動・直接RAM書込みは使用していない。

| 開始状態・操作 | 上限（命令／サイクル） | 実測サイクル | 入力完了 | LCD | RAM[0xC100] |
| --- | --- | ---: | --- | --- | --- |
| 通常プロンプト → CALL | 400000／1000000 | 1000001 | true | 通常プロンプト | 00 → 5A |
| 通常プロンプト → CALLを途中停止 | 200000／500000 | 500002 | false | CALL 504とカーソル | 5Aのまま |
| 途中停止直後 → CLEARからの新規CALL | 400000／1000000 | 1000001 | true | 通常プロンプト | 5Aのまま |
| 通常プロンプトに戻った後 → 同じCALL | 400000／1000000 | 1000010 | true | 通常プロンプト | 5A → 6B |

LCD PNG：[通常CALL後](../images/ai-generation/pc1251-preview/baseline-after.png)、
[途中停止](../images/ai-generation/pc1251-preview/partial.png)、
[再試行後](../images/ai-generation/pc1251-preview/retry.png)、
[通常プロンプトから再実行後](../images/ai-generation/pc1251-preview/recovered.png)。

4要求はいずれもcycleLimitで停止。新しいマーカーを書かなかった現象を途中停止直後に再現し、
通常プロンプトに戻った後は同じロード内容で書込みを確認した。入力の送信完了はCALL成功を意味しない。
最初のCLEARをROMが受理したか、キー解放間隔が影響したかは未確定。今回は修正を入れず、試験対応の制限とする。
途中停止後はキー列を自動再送せず、LCDで状態を確認し、通常プロンプトへ戻してから操作を選ぶ。

## 3. Copilot受入れ

PC-1245、VS Code 1.143.1のローカル実行先、公開手順のstdio設定を使用。
検証専用フォルダーにはMCP設定だけを配置し、ソースはStudioが開いた別のフォルダーに置いた。

A01はPASS。Copilotの新規チャットで機種と登録ソースを取得し、10行のCALL 50432が
0xC500を呼び、復帰後20行のENDへ進むと説明した。BASICだけでは描画・復帰を保証できないこと、
Assembly本文から確認できること、実行していないことを区別した。
`pgp_get_development_context`、`pgp_get_source`、知識検索・取得などのツール履歴を確認。
解析依頼でBuild・実行・変更は行わなかった。

G01は別の新規チャットで開始。機種仕様・BASIC知識・作例をMCPで取得し、
CopilotがPC-1245用の新規ProjectとBASIC本文を提案。Copilot側でツールを一度のみ許可し、
Studio側で保存先を指定・承認した。
生成されたBASICは最初のINPUTと数値分岐、次のINPUTプロンプトを用いる構成。
Projectの承認後、Copilotが新しいrevisionを取得してBuildを提案し、成功を確認。
Build成果物を指定したLoadも、Studioの別承認で成功した。95バイトのBASIC、総書込み101バイト。

RUNと各入力はCopilotが提案し、都度Copilot側の一回許可とStudio側の承認を通した。
各停止のrevisionでCopilotがLCDを取得し、以下を報告。Studioの停止履歴とも照合した。

| 操作 | 命令数 | サイクル数 | LCD |
| --- | ---: | ---: | --- |
| RUN | 127013 | 500001 | 数値入力待ち |
| 1 → ENTER | 127106 | 500009 | ONE |
| 2 → ENTER | 126759 | 500000 | TWO |
| 3 → ENTER | 126876 | 500001 | OTHER |
| 0 → ENTER | 127245 | 500006 | 通常のBASICプロンプト |

すべてcycleLimit・inputComplete=true。0入力後のプロンプトを終了の根拠にし、
上限停止だけをプログラム終了と扱わなかった。G01はPASS。段階ごとに承認後の続行をチャットで依頼した。
生成経路でファイル直接編集・端末操作による代替は行わなかった。
A02の独立した機械語解析全工程、他クライアント、Windows／Linuxはこの受入れの対象外。

## 4. macOS配布物の準備

`desktopApp:packageDmg` 成功。検証用DMGのチェックサム検査が成功し、読み取り専用でマウントした。
アプリを一時フォルダーへコピーし、元のアプリを上書きせずアンマウントした。
コピーしたInfo.plistの構文は正常。ネイティブ版数は1.9.0、Bundle IDは現行Studioと同一。
このローカル成果物はアドホック署名。Developer ID署名・公証・配布先でのGatekeeper検証は未実施。

同じBundle ID・設定保存先を使うため、StableとPreviewの同時起動や設定分離は保証しない。
検証では既存アプリを上書きせず、Projectはコピーを使用した。公開時にもStableのアプリ本体と
Projectのバックアップを保持し、切り替える際は片方のStudioとMCPを終了する手順が必要。
今回の検証ではProject形式の変更は行っていない。今回の往復はクリーン環境の検証ではない。

公開前にはPreview固有の採番・配布物名・リリースノートを決め、署名・公証した成果物で再確認する。
既存のrelease.ymlは v* タグに反応し、RELEASE_VERSION=1.2.0の文書で通常のドラフトを作る。
Previewタグをそのまま送らず、Stableと区別できる手順を先に用意する。

DMGからコピーしたアプリの起動・PC-1245 ROM自動起動は確認した。
既存ProjectのオープンとMCP読取りも試みたが、待受プロセスの実行パスを確認すると
作業ディレクトリ側の同一Bundle IDアプリに操作が届いていた。
このため、DMGコピーそのものでのProject操作は未確認とする。
公開前に署名済み配布物を用い、同じBundle IDの別インスタンスがない環境で再試験する。

## 配布判断

**PC-1245のmacOS arm64向けPreview候補として、機能受入れと署名済みDMG・Stable復帰試験は合格。公開は未実施。**
初回はPC-1245のBASIC解析・生成、VS CodeのCopilotローカル実行先を検証済み範囲とする。
PC-1251は試験対応で、CALLの途中停止直後に期待したRAM書込みが確認できない制限を明記する。
別試行で署名・公証済みDMGのコピーを単独起動し、既存ProjectのBuild・LoadとMCP読取り、公式Stableへの復帰を確認した。
タグ作成、GitHub Release公開、Stableへのマージは行っていない。

一時MCPサーバーを停止し認証ファイルがないことを確認。Studioのアセンブラ設定は元のSC61に戻した。

署名済み候補の作成とStableへの復帰試験は[Preview配布手順](AI_TECHNICAL_PREVIEW_DISTRIBUTION.md)を参照。
