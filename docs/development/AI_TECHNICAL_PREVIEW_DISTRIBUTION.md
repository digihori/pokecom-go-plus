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
