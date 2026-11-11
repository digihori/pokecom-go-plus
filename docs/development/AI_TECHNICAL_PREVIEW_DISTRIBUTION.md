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
