# MCPクライアント接続検証記録

実施日：2026-11-01。[共通受入れ課題](AI_MCP_ACCEPTANCE.md)のC01を対象とする。

## 検証環境

- OS：macOS 26.6.2、Apple Silicon。
- Studio：ローカルに生成済みのアプリ。MCP initializeのversionは`1.0.0`。
  実行バイナリの生成元commitは未照合のため、現在のHEADの検証済みとは扱わない。
  アプリのdesktopApp jar SHA-256：`dae3e87defc8e8be575649f70a79dabf8469195a1543b29733b8c4d0e48dc5ee`。
- VS Code：1.143.1（commit `e646868227c3e3a9e3f24277c8c7220be42c0be7`）。
- Copilot：利用者によるサインイン後に確認。会話の表示モデルはGPT-6 Luna（Auto選択）。
- 接続：`127.0.0.1:8765/mcp`への認証付きHTTP。
- Project：`test-data/assembly/pc1245-hello`を一時フォルダーへコピー。
  PC-1245・内蔵assemblerを明示選択。ROMは利用者の登録済みROMを使用し、ROM本体は共有しない。

接続設定は検証用コピー内に限定し、グローバル設定や開発リポジトリには追加しない。
今回の固定ヘッダーは短期トークンの一時利用であり、製品向けの認証方式ではない。
記録・共有する設定例にトークンを含めない。

## HTTPプローブによるStudio側の確認

CodexからPython標準ライブラリの一時HTTPプローブを実行し、Studio UIを操作して確認した。
CodexネイティブMCPクライアントでの呼出しとは区別する。

| 確認項目 | 結果 |
| --- | --- |
| initialize・機能取得 | 成功。protocolVersion `2025-06-18` |
| metadata・登録ソース取得 | PC-1245、内蔵assembler、Source `main`／`hello`を確認 |
| コメント差分の提案 | `PENDING_APPROVAL`。承認前のファイルhashが元と一致 |
| Studioで却下 | `REJECTED`。同じrevision/hashで再提案可能 |
| 再提案をStudioで承認 | `APPLIED`。コメント追加と新しいrevision/hashを取得 |
| 新revisionでBuild提案・Studio承認 | `SUCCEEDED`、`BUILT_NOT_LOADED`、diagnostics 0件 |

差分は`; C01 MCP acceptance`のコメント1行のみ。Load・実行操作は要求していない。

## VS CodeとCopilotの実行環境の差

1. 検証用`.vscode/mcp.json`にHTTP接続を設定した。
2. VS CodeのMCP出力で`Discovered 15 tools`と接続中の状態を確認した。
3. 「Copilot」実行環境では会話からPGPツールを利用できず、Agent Hostログも
   `pgp-c01 not_configured (stopped)`を示した。
4. ワークスペース直下のportable形式`.mcp.json`でも試したが、その時点の新規セッションでは改善しなかった。
5. エディターのチャットでSession Targetを「ローカル」に変更すると、PGPの読み取りと差分提案が実際に呼び出された。

サーバー一覧で接続できたことと、会話でツールを実行できたことは別に確認する。
上記だけではCopilot実行環境の失敗原因をPGP側・クライアント側のどちらとも断定しない。
新しい実行環境を使うために保護設定を無効化する対処は行わない。

公式資料では、VS Code形式とportable Copilot形式、実行環境ごとの設定の扱いが区別されている。
今後の設定生成では対象形式と実行環境を明示する。

- [MCP configuration reference](https://code.visualstudio.com/docs/agents/reference/mcp-configuration)
- [Choose an agent harness](https://code.visualstudio.com/docs/agents/run/agent-harnesses)

## 後続の設計に反映する事項

- クライアント名だけでなく、版・実行環境・設定形式を接続ガイドに記載する。
- 一覧取得、読み取り、変更提案、Studio承認、結果取得までを接続診断の対象にする。
- 固定ヘッダーのコピーはStudio再起動で失効する。短期認証ファイルを毎回読み取る同梱stdioブリッジを
  次の設計候補とする。認証を緩めたり、トークンを共有Projectに恒久保存したりしない。
- `pgp_get_machine_spec`の大きな応答はクライアントがファイル化する場合がある。
  今後の知識APIは話題別取得・検索・サイズ上限を設ける。
- CodexネイティブMCPでのC01と、生成元commitを特定した最新版アプリでの再検証は別途残す。

## Copilotの会話から確認した内容

対象はVS Codeエディターの「ローカル」実行環境。Copilotの読み取りツールと操作結果の報告をUIで確認し、
Studioの確認画面と作業用ファイルを照合した。

- capabilities、machine spec、metadata、Sourceの取得に成功した。
- コメント差分を提案し、Studioで却下した。Copilotがoperationの`REJECTED`とSourceの
  revision/hashの無変更を確認してから、同じ差分を再提案した。
- 再提案をStudioで承認し、作業用ファイルに`; C01 Copilot acceptance`が追加された。
- Copilotが`APPLIED`を取得し、Sourceを再取得した新しいprojectRevisionでBuildを提案した。
  Studio承認後、Copilotが`SUCCEEDED`、`BUILT_NOT_LOADED`、diagnostics 0件を取得・報告した。

C01の結果：VS Code「ローカル」実行環境のCopilotでPASS。
「Copilot」実行環境はツール利用不可のためBLOCKED。CodexネイティブMCPのC01はNOT_RUN。
解析・生成課題A01・A02・G01は今回の検証対象外。

| Copilotからの操作 | operationId | 結果 |
| --- | --- | --- |
| 最初のコメント提案 | `a9fe1930-2d59-4719-8a92-ab0f23c642e2` | REJECTED |
| 再提案 | `aec77b37-1d1c-4fa8-894a-f1fba4b3ca75` | APPLIED |
| 新revisionによるBuild | `e68fc430-04a1-4866-83b7-008b0d290fa0` | SUCCEEDED |

検証終了後はStudioのMCPサーバーを停止してトークンを失効させ、一時フォルダー内の
両形式のMCP設定から認証ヘッダーを除去した。元のサンプルファイルは変更していない。

初回の依頼はUI文字入力で日本語が欠落し、不要なoperation取得を誘発したため、以後は貼付けで
依頼本文を正しく渡した。機種仕様の大きなツール応答をVS Codeが保存し、AIがその応答ファイルを
読む動作もあった。Projectや認証設定を直接読むことを必要条件にしない設計は維持する。

## 同梱stdioブリッジの実装・検証（2026-11-01）

同じ開発ツリーで同梱stdioブリッジを追加し、JDK 21で配布アプリを再生成した。
起動引数は`--mcp-stdio --port <port>`。既存HTTPと同じ認証・承認境界を使い、
トークンをクライアント設定へコピーしない。設定コピーはVS Code、Copilot portable形式、
Claude Desktop、Gemini CLIに対応する。設定形式の実装と実接続の確認は区別する。

| 確認 | 結果 |
| --- | --- |
| 自動テスト | 認証の要求ごとの更新、実HTTPサーバー再起動後の接続、通知の無応答、入力上限後の復旧、リダイレクト拒否、認証権限・symlink拒否、各設定のパス保持を確認 |
| 全チェック | JDK 21で`./gradlew build :core:desktopTest :desktopApp:verifyDistributionRuntime`成功。Desktop 482件、失敗0、skip 3件 |
| macOS配布ランチャー | stdin EOFでGUIなし・stdoutなしで終了 |
| 配布ランチャー→起動中Studio | initialize、initialized通知、15ツール一覧、capabilities取得成功。JSON-RPC応答3件、stderr空、終了code 0 |
| 配布アプリのMCP画面 | 4種類のstdio設定コピーボタンが有効であることを確認 |
| Copilot＋VS Code「ローカル」 | stdio経由で15ツールを認識し、capabilities・PC-1245のmachine spec取得成功。ツール詳細のサーバー名`pgp-stdio`をUIで照合 |

stdio経由では読み取りを実接続確認した。変更提案・承認往復の実接続は前節のHTTPでの確認であり、
stdio経由のC01全工程完了とは扱わない。Windows・Linux配布ランチャー、Copilot実行環境、
Claude Desktop、Gemini CLIでのstdio実接続は未確認。CodexネイティブMCPの受入れも残る。

検証用VS Code設定には実行ファイルと引数だけを設定し、トークンを保存しなかった。
検証後はStudioのサーバーを停止してトークンを失効させ、検証用VS Code設定も空に戻した。

### macOSのClaude Desktop Freeでの実接続

Studioの同梱stdioブリッジをClaude DesktopのローカルMCPに登録した。
公開したPC-1251 BASICの機種と先頭行 `10 INPUT A` を取得できた。
PC-1245の見積もりサンプルの一時コピーで、Claudeから `510 R=5` を `510 R=10` にする差分が届いた。
Studioで承認して適用を確認し、Claudeも `pgp_get_operation` で `APPLIED` を取得した。
承認直後のチャットでは応答が途中で止まり、新しいチャットで操作状態だけを問い合わせると取得できた。
検証後にStudioのMCPサーバーを停止した。
