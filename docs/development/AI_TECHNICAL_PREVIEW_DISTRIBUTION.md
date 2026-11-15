# AI Technical Preview：macOS配布候補の作成と受入れ

対象は PC-1245 のBASIC解析・生成と VS Code Copilot のローカル実行先。
PC-1251は試験対応。[検証範囲と制限](AI_TECHNICAL_PREVIEW_READINESS.md)を参照。

1. GitHub Actionsの **Build Release Packages** を手動実行する。
   対象ブランチは `codex/ai-mcp-expansion`、入力 `ai_preview` を有効にする。
   対象コミットを実行画面で照合する。タグは作らない。
2. `macos-signing` 環境のレビューを経て、署名・公証・stapleの成功を確認する。
   環境がブランチからの実行を制限している場合は管理者が許可範囲を確認する。
   環境の承認や制限を迂回しない。
3. Actionsの成果物 `pokecom-go-studio-ai-preview-macos` を取得し、
   `SHA256SUMS-macos.txt` とDMGを照合する。
   ファイル名に `1.2.0-ai-preview.1` が入る。アプリ内部のバージョンとBundle IDは
   現行Studioと共通。ワークフローはGitHub Releaseを作成しない。
4. `xcrun stapler validate <DMG>` と
   `spctl -a -vv -t open --context context:primary-signature <DMG>` を実行する。
   DMGをマウントし、既存Stableを上書きせず、別フォルダーにアプリをコピーする。
   コピーにも `codesign --verify --deep --strict <app>` と
   `spctl -a -vv -t execute <app>` を実行する。
5. Stable・開発版・MCPサーバーを終了した状態で、コピーしたPreviewだけを起動する。
   実行ファイルのパスがコピー先であることを確認する。別インスタンスでの操作は合格に数えない。
   バックアップしたPC-1245 Projectのコピーを開き、ソース表示、内蔵アセンブラでの
   Build・Load、MCPの接続と読取りを確認する。
6. PreviewとMCPサーバーを終了する。元のStableを起動し、実行パスを確認したうえで
   同じProjectのコピーを開き、ソース表示とBuild・Loadを確認する。
   変更したアセンブラ設定は元へ戻す。同一Bundle IDと設定保存先を使うため、同時起動は行わない。

結果は、Actionsの実行番号・コミット・DMGのSHA-256・署名検査・PreviewとStableの
起動パスの区別・Project操作の結果を受入れ記録へ追記する。
署名済みDMGでの確認を終えるまでは公開保留とする。

## 実行記録

- [検証実行 43015483175](https://github.com/digihori/pokecom-go-plus/actions/runs/43015483175)（b2d9d95）：
  手動実行の受付とmacOS以外のジョブのスキップを確認した。
  `macos-signing` のブランチ制限により署名前に停止。署名済みPreview DMGは未生成。
- 復帰試験用の公式Stable 1.2.0 arm64 DMG：公開SHA-256と一致、staple検証成功、
  GatekeeperがDMGおよび抽出したアプリをNotarized Developer IDとして受理。
  インストール済みアプリを上書きせず、復帰確認用のコピーを準備した。
- 再開条件：署名環境の既存のレビュー要件を維持し、管理者が検証対象ブランチのみを許可。
  同じ実行を再実行し、署名・公証完了後に手順3〜6を行う。

### 署名・公証済み候補の受入れ結果

- [Actions実行 43015483175](https://github.com/digihori/pokecom-go-plus/actions/runs/43015483175)
  の再実行が成功。対象コミットは b2d9d95。既存レビュー要件は変更せず、一時追加したブランチ許可は削除済み。
- `PokecomGOStudio-1.2.0-ai-preview.1-macos-arm64.dmg` のSHA-256：
  `24aa74c9c93f0350ff275c1f2f6c2143464ef3ef2c7e121bc0fc558fd77df0bc`。
  ActionsのSHA-256一覧と一致し、staple検証とGatekeeperのDMG検査に合格。
  コピーしたアプリも厳密な署名検証とGatekeeper検査に合格。
- 署名済みPreviewを単独起動し、PC-1245 Numeric LabelsのProjectを開いた。
  内蔵アセンブラの「選択ファイルをビルドしてロード」でBASIC 95バイトをロード。
  ローカルMCP待受プロセスの実行パスをDMGからのコピー先と照合し、同梱stdioで
  `pgp_get_source` が `10 INPUT A` から始まるBASICを返すことを確認した。
- PreviewとMCPを終了し、一時認証の削除を確認してから、公式Stable 1.2.0のDMGから
  抽出・検証したコピーを単独起動した。実行パスを照合し、同じProjectを開いて
  BASIC 95バイトのBuild・Loadを確認。ProjectのmanifestとBASICソースは試験前の内容と一致した。
- 同一Bundle IDの別インスタンスが選ばれた試行は除外した。
  合格試行は別インスタンスを終了し、使用しない試験コピーを一時退避してから行った。
  既存インストールの上書き、タグ作成、GitHub Release作成・公開は行っていない。

この範囲でmacOS arm64の配布候補は合格。公開は別途判断する。

### 最新候補（ea6cc6a）の受入れ状況

[Actions実行 43406028322](https://github.com/digihori/pokecom-go-plus/actions/runs/43406028322) の
初回は署名環境のブランチ制限で停止した。対象ブランチを一時許可して再実行し、
署名・公証が成功した後に追加した許可を解除した。

DMGのSHA-256は `2e17c6287c8522c75007cc2ef866618bd22486109d7a0cbc693b12303b44bfcb`。
Actionsの一覧と一致し、DMGのstaple・Gatekeeper検査、展開したアプリの厳密な署名検証と
Gatekeeper検査に合格した。

展開したアプリに含まれる実行処理と実ROMを使い、PC-1245の一括入力で小売・まとめ買い各105件、
PC-1251のCALLの途中停止直後の再試行で26件が成功した。
画面操作では同じBundle IDの旧ビルドが選ばれたため、その結果は受入れに含めない。
残りは署名済みアプリを単独で起動した状態でのPC-1251機械語解析、Claude Desktop・Copilotの
接続確認、およびPre-release下書きの作成。

署名済みアプリを単独起動し、MCPの待受プロセスがDMGの展開先と一致することを確認した。
PC-1251のDMPを解析画面から公開し、同梱stdio、Claude Desktop Free、VS Code Copilotから
`0xB800 LIA 0x5A` と `0xB802 RTN` を取得した。検証後はMCPを停止し、クライアントの設定を戻した。
[GitHubのPre-release下書き](https://github.com/digihori/pokecom-go-plus/releases/tag/untagged-15efc228083660bd4bf1)
に検証済みDMGとSHA-256一覧を添付した。下書きとPre-releaseのフラグ、対象コミット、
添付DMGのハッシュ、およびLatestが `v1.2.0` であることを確認した。

### 初回PreviewのWindows x64受入れ

初回Previewの公開対象にWindows x64を含める。macOS arm64とWindows x64の受入れが揃ったら、
同じPre-releaseで公開する。

1. 通常版のWindowsパッケージングを利用し、Preview用のWindows x64配布物とSHA-256を作成する。
   初回の受入れはZIPを優先し、MSIを同梱する場合はStable 1.2.0とのインストール・復元も確認する。
2. 展開先からStudioを起動し、配布物が出力するMCP設定でstdioの初期化、ツール列挙、
   ツール呼び出しを確認する。WindowsのGUIランチャーで標準入出力を扱えない場合は、
   MCP用コンソールランチャーを同梱して設定の出力先を切り替える。
3. Windows上のVS Code CopilotとClaude Desktopから接続し、PC-1245またはPC-1251の
   プログラム解析と生成・ビルドを試す。合格したクライアントと手順を案内に記載し、
   Windows配布物とハッシュをPre-release下書きに追加する。

[最初のWindows x64ビルド](https://github.com/digihori/pokecom-go-plus/actions/runs/43410146410/job/131834235006)
は、Windows上で実行したデスクトップテスト560件のうち2件が失敗し、配布物の生成前に停止した。
MCP設定のテストは実行環境の絶対パスを使うよう修正した。生成先の差し替え検出は、
Windows版JDK 21の `BasicFileAttributes.fileKey()` が常にnullを返すため、Windowsの
ボリューム番号とファイルIDを比較するよう修正した。両テストクラスはmacOS上で合格した。
[修正後のWindows x64ビルド](https://github.com/digihori/pokecom-go-plus/actions/runs/43411066894/job/131837325706)
はコミット `4e6ac94` で成功し、`pokecom-go-studio-windows` 成果物を生成した。
取得したZIPとMSIのSHA-256はいずれもCIの一覧と一致した。ZIPのランチャーはx86-64のGUI実行形式。
Windows 11 ARMのVMでZIPを展開してx64版Studioを起動し、PC-1251の登録済みROMと
LCDのRUNプロンプトを画面で確認した。Windows上でStudioのMCPサーバーの起動も確認した。
PowerShellから同梱ランチャーの標準入出力に接続し、ポート8765のStudioへ送った
`tools/list` に24件のツールが返り、`tools/call` の `pgp_get_capabilities` が成功した。
同じWindows環境で、VS Codeの検証用ワークスペースにstdio設定を追加し、Copilot Agentから
`pgp_get_capabilities` を実行した。Copilotのツール結果に `machineId: pc-1251` を確認した。
Windows版Claude Desktop Freeでも `pgp_get_capabilities` の結果を利用者が確認した。
`machineId` は `pc-1251`、最大逆アセンブル命令数は256、メモリアクセス履歴は対応、
バンク履歴は非対応だった。
同じClaude Desktopから、PC-1251のBASICで42を表示して終了するプロジェクトを提案した。
StudioでWindowsの一時フォルダーを保存先に選んで承認し、`CREATE_PROJECT · APPLIED` を確認した。
Claudeが `pgp_get_source` で `src/main.bas` を読み取り、`10 PRINT 42` と `20 END` の動作を説明した。
続けて `pgp_build_project` を呼び出し、Studioで承認後に `BUILD · SUCCEEDED` を確認した。
Windows版VS Code Copilotでも同じBASICソースを読み取り、動作を説明した。
`10 PRINT 42` を `10 PRINT 43` にする差分をStudioで承認し、`PATCH · APPLIED` を確認した。
Copilotは適用後のソースを再取得し、`10 PRINT 43` と `20 END` を確認した。
続くCopilotからのビルド依頼もStudioで承認し、`BUILD · SUCCEEDED` を確認した。
Windowsでの確認範囲はBASICの生成・ソース読み取り・変更・ビルドまで。
2026年11月15日、受入れ済みのWindows ZIPをPreview名に変更し、SHA-256一覧とともに
既存のPre-release下書きへ追加した。GitHub上の添付ファイルのハッシュを照合した。
Windowsは `4e6ac94`、macOSは署名・公証済みの `ea6cc6a` を使用し、公開案内に両方の
コミットと検証範囲を記載した。GitHub上で下書きとPre-releaseの状態、およびLatestが
`v1.2.0` であることを確認した。

### Google Antigravity CLIの接続確認（macOS arm64）

2026年11月15日、署名・公証済みのmacOS配布候補（`ea6cc6a`）と
Google Antigravity CLI（`agy` 1.5.9）を使用した。配布アプリ同梱のstdioブリッジを
`--mcp-stdio --port 8765` で起動する一時設定を用意し、セッション内で
`pgp_get_capabilities` だけを許可した。会話から同ツールが実行され、
`machineId: pc-1245` と `maxMemoryBytes: 4096` が返った。検証後に一時設定を復元した。

Antigravity CLIで確認した範囲はmacOSでの読み取り呼び出し1件。承認付き操作、
Windows上のAntigravity CLI、および別クライアントであるGemini CLIの実呼び出しは未検証。

### 公開前の最終確認

GitHub下書きのDMGとZIPのSHA-256を、それぞれに添付したチェックサム一覧と受入れ記録に照合した。
下書きには `ai-preview-1.2.0-preview.1` を設定した。通常配布のワークフローが反応する
`v*` の対象外で、公開時のタグの対象は macOS 配布候補の `ea6cc6a`。
Windows配布物のビルド元 `4e6ac94` はリリース本文に記載した。
本文にGitHub Issuesへの報告窓口を追加し、下書きがPre-releaseに設定され、
Latestが `v1.2.0` であることを確認した。

### 公開結果

[AI Technical Preview 1](https://github.com/digihori/pokecom-go-plus/releases/tag/ai-preview-1.2.0-preview.1)
をGitHubのPre-releaseとして公開した。公開後に、タグの対象コミット、添付した4ファイルの
SHA-256、およびLatestが正式版 `v1.2.0` であることを確認した。

## AI Technical Preview 2のインストーラー試行

試行時の表記は `1.2.1-ai-preview.2`。macOS arm64のPKGとWindows x64のMSIを試作した。
正式版1.2.0からの上書きに既存のアプリ名、macOSのBundle ID、WindowsのUpgradeCodeを使う。
インストーラー内部のバージョンは正式版の `1.9.0` から `1.9.1` に上げた。
今後、正式版をPreview 2から上書き更新できるよう、その内部バージョンは `1.9.1` より上にする。

[Actions実行 43714959038](https://github.com/digihori/pokecom-go-plus/actions/runs/43714959038)
のMacジョブは、対象ブランチが `macos-signing` 環境で許可されておらず、署名前に停止した。
同環境にはPKG用Developer ID Installer証明書も登録されていなかった。
ローカルではJDK 21で配布アプリのランタイム検査に合格し、未署名の検証用PKGを展開して
インストール先が `/Applications`、Bundle IDが正式版と同じ、内部バージョンが `1.9.1` であることを確認した。

Windowsジョブは成功。MSIの製品名・バージョン・UpgradeCodeのCI検査に合格し、
[Windows成果物](https://github.com/digihori/pokecom-go-plus/actions/runs/43714959038/artifacts/14161814381)
を取得して、アーカイブとMSIのSHA-256を照合した。

2026年11月15日に、AI機能を区切りごとに `main` に取り込み、通常リリースで公開する方針に変更した。
次の配布は通常のmacOS DMGとWindows MSIを使う。
[開発・配布方針](AI_MCP_EXPANSION_PLAN.md#9-ai機能の開発と通常リリース2026-11-15合意)を参照。
