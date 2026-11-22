# 次期リリース候補のAI連携検証

以下は当時の候補・検証計画です。現在の接続確認と残作業は[AI連携の再開メモ](AI_MCP_RESUME.md)を参照してください。

初回の対象は main の `09e0605` から生成したmacOS・Windows配布物。
VRAM・発音知識の更新を確認するときは、修正を含む新しい配布物を使い、そのコミットを記録する。
[検証用ビルド](https://github.com/digihori/pokecom-go-plus/actions/runs/43960117163)

## 初回接続

StudioとAIクライアントは同じOSで起動する。通常のインストーラーでStudioを入れ、
AIクライアントには利用者としてサインインする。既存のPGP用MCP設定がないプロファイルを使い、
接続手順を見ながら Studio の「デバッグ → AI / MCP Server」で起動し、設定を登録する。
設定ファイルに他のサーバーがある場合は、同じ親キーの中にStudioの項目を追加する。

| クライアント | 試験する接続手順 | macOS | Windows |
| --- | --- | --- | --- |
| Codex | StudioのCodex設定を `config.toml` に追加し、Codexで新しいセッションを開く | 未実施 | 未実施 |
| GitHub Copilot（VS Code） | StudioのCopilot用stdio設定を作業フォルダー直下の `.mcp.json` に登録し、同じフォルダーを開いたVS CodeのCopilotセッションから試す | 未実施 | 未実施 |
| Claude Desktop | StudioのClaude Desktop用stdio設定を「設定 → 開発者 → 設定を編集」で開いたJSONに追加し、アプリを完全に終了して起動し直す | 未実施 | 未実施 |
| Gemini（Google Antigravity CLI） | Studioのstdio設定の `mcpServers` をユーザーの `.gemini/config/mcp_config.json` に登録し、`agy` の新しいセッションでGeminiモデルを選ぶ | 未実施 | 未実施 |

設定の基本は[StudioのAI接続ガイド](../guides/AI_CONNECTION_GUIDE.md)を参照。
[CodexのMCP設定](https://learn.chatgpt.com/docs/extend/mcp?surface=cli) ·
[VS CodeのCopilot MCP設定](https://code.visualstudio.com/docs/agents/reference/mcp-configuration) ·
[Antigravity CLIのMCP設定](https://antigravity.google/docs/cli/gcli-migration)

8通りでお試し1を実施する。ツール呼出し成功は、会話のツール実行結果と
Studio側の要求ログで確認する。OS（WindowsはARM/x64も記録）、アプリとクライアントのバージョン、
手順で迷った箇所、結果を記録する。Windows ARM上でx64配布物を試した場合は、その構成を明記する。
配布版で試した結果を反映してから、初めて使う方向けの接続ガイドを更新する。

## お試し1　PC-1245について聞く

ROMやProjectがない状態から試せます。AIへの入力例です。

> Pokecom GO StudioのMCPを使って、PC-1245の画面に何文字表示できるか、横と縦に何ドットあるかを教えてください。参照した資料名も添えてください。

合格条件は、実際のツール結果から16文字×1行、80×7ドットを説明できること。

## お試し2　機械語でHELLOを表示する

PC-1245の利用可能なROMとStudio内蔵アセンブラを使います。
AIが提案した変更やBuild・LoadはStudioで内容を確認してから承認します。

> Pokecom GO StudioのMCPでPC-1245の表示仕様を調べ、新しい作業用Projectで、LCDに「HELLO」と表示する機械語プログラムを作ってください。内蔵アセンブラ用のソースと、BASICのCALLで起動するプログラムを用意してください。ROMの表示ルーチンを使わず、表示後はBASICに戻してください。変更とBuild・LoadはStudioの承認を通してください。

ロード後は、実行が停止していれば再開し、RUNモードで `RUN` → ENTER。
合格条件はBuildとLoadが成功し、「HELLO」がLCDに表示され、終了後にBASICの入力へ戻ること。
比較用の[HELLO WORLD作例](../../test-data/assembly/pc1245-hello/README.md)にはBuild・Loadの手順もあります。

## お試し3　PC-1350で4行LCDの機械語アニメーションを作る

PC-1350の利用可能なROMとStudio内蔵アセンブラを使います。25文字×4行、150×32ドットの
LCDを使う作例として、タイトルの下をロケットが飛ぶ数秒のアニメーションを作ります。

> StudioのMCPでPC-1350の画面に機械語で描画する方法を調べてください。新しい作業用Projectを作り、上段に「POKECOM GO」、その下にオリジナルのドット絵のロケットを表示し、横に移動させる機械語プログラムを作ってください。LCDの行をまたぐ大きさにし、ロケットと炎を描いてください。内蔵アセンブラとBASICのCALLで起動し、1回で終了してBASICへ戻る構成にしてください。変更とBuild・LoadはStudioの承認を通してください。

ロード後に再開し、RUNモードで `RUN` → ENTER。合格条件は、タイトルと複数行にまたがるロケットを
同時に表示でき、ロケットが画面を横切り、終了後にBASICを操作できること。
[Coffee Break](../../test-data/assembly/pc1350-coffee-break/README.md)はPC-1350の既存の機械語作例です。
比較用に使う場合は作業用コピーを別Projectで開き、描画・BASIC復帰と音付きデモを確認できます。
生成試験では、新しいProjectのソース・Build結果・LCDの動作を記録します。

配布版では、お試し2と3を少なくとも各OSで1回ずつ通します。

### Windows＋Copilotでの試験結果

PC-1350の機種仕様と表示知識の取得は成功。LCDの寸法は取得できたが、
`pgp_get_machine_spec` の `display.directDrawing` が `null` で、描画コードの生成は中断した。

### 対応と再検証　PC-1245以外のLCD VRAM仕様

- [x] PC-1350などの機種について、エミュレーター実装に基づくVRAMアドレス、画素座標と
  ビットの対応、表示制御、描画対象外の記号アドレスを機種仕様に追加した。
- [ ] 各機種で公開内容とLCD描画結果を検証する。
- [ ] WindowsのCopilotでお試し3を再実施する。MCPの機種情報から機械語を生成し、
  Build・Load、LCDでのアニメーション、BASICへの復帰まで確認する。

### 対応と再検証　生成するBASICのCALL先を原則16進にする

- [x] 機械語を呼び出すBASICをAIが生成するときは、対象機種で有効な16進表記を使うようMCPで案内する
  （PC-1245の例は `10 CALL &C500`）。10進表記は対象機種で必要な場合に使う。
- [ ] お試し2と3で、生成されたBASICのCALL先が機械語の開始アドレスと一致し、
  その表記で実行できることを確認する。

### 対応と再検証　機種共通の音階・メロディー生成

- [ ] ユーザーが機種名と鳴らしたい音階や曲を伝えると、AIがMCPでその機種の発音方法を確認し、
  ブザーを鳴らすプログラムを提案できるようにする。作例の事前準備やツール名の指定は不要とする。
- [x] 機械語でのブザー制御、音程と音の長さの計算方法、機種ごとのクロックをMCPの機種知識に追加した。
- [ ] 複数機種で、自然文の依頼からプログラムを生成し、Studioで指定した順番と長さで音が鳴り、
  演奏後にBASICの入力へ戻ることを確認する。

## 追加のお試し　PC-1245の電光掲示板を改造する

[スクロール作例](../../test-data/assembly/pc1245-hello-scroll/README.md)を作業用にコピーしてStudioで開きます。
まず `HELLO WORLD` の動きを確認し、次の入力で既存プログラムの解析と改造を試します。

> Studioで開いているPC-1245のスクロールProjectのアセンブリをMCPで読み、文字を1ドットずつ動かす仕組みを説明してください。表示を「POKECOM GO」に変えてください。約0.1秒ごとに左へ1ドット動き、1回流れてBASICに戻る動きは保ってください。差分をStudioで確認できるように提案してください。

差分を承認し、Build・LoadしてRUN。合格条件は新しい文字列のドット単位の移動をLCDで観察でき、
終了後にBASICを操作できること。

## 追加のお試し　ブザーで音階を奏でる

Studioで使用する機種を選び、プロンプトの機種名を合わせてください。

> Pokecom GO StudioのMCPを使って、PC-1350でド・レ・ミ・ファ・ソを順に鳴らすプログラムを作ってください。各音は約0.25秒で、最後にBASICの入力へ戻るようにしてください。

機種名と音階だけで、使用する機種の発音情報をAIが取得し、音が指定順に鳴ることを確認します。
機械語を使う場合は、生成されたBASICのCALL先とアセンブリの開始位置も照合します。
