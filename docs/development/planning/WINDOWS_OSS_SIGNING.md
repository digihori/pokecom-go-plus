# Windows OSS無料署名の検討

2026-10-23。第一候補はSignPath Foundationです。
利用が難しい場合は、有料証明書の購入を必須とせず、当面はWindows版を未署名で配布します。
申請・アカウント登録・外部への送信はまだ行っていません。

## 一次確認の結論

PGPはMIT Licenseでソースを公開し、既にWindowsのMSI／ZIPを配布しています。
リポジトリ内のLICENSE・第三者通知・公開workflowからは、検討を続けられる条件が揃っています。
ただし、無料提供の採否、プロジェクトの実績評価、同梱runtimeの扱いはSignPath側の判断です。
無料署名の承認を正式版公開の前提にはしません。

## 公式条件との照合

| 条件 | PGPの現状・残確認 |
| --- | --- |
| OSI承認のOSSライセンス、非公開コンポーネントを含めない | 本体はMIT。第三者通知にはApache 2.0／BSD／zlib／MIT等を記載。最終配布物のruntime・native依存も確認する |
| 継続開発・公開済み配布物・機能説明 | ソース・README・0.7公開記録・Windows配布workflowあり。実績が審査に十分かは未確定 |
| 検証可能なソースビルド | GitHub Actionsでテスト・MSI／ZIP生成を実装済み。採用時はCI成果物から署名要求へ接続する |
| プロジェクト自身のバイナリーのみ署名 | PGP launcher／MSIを対象にする案。JREやLWJGL／SDL等の他プロジェクト由来EXE／DLLを一括で再署名しない。同梱の可否・署名対象は申請時に確認 |
| 製品名・版数の制約 | launcher／MSIの実際のメタデータを確認し、SignPath側の制約へ合わせる。公開版1.0.0と内部packageVersionの違いも確認する |
| 多要素認証・役割の明示・リリースごとの手動承認 | GitHub／SignPathの設定、開発・レビュー・署名承認の担当を確認する。現在のタグによる自動公開へ署名承認待ちを組み込む必要あり |
| Code signing policyと通信方針の掲載 | 採用時に日英READMEから参照できるページを用意。StudioのMCP・ブラウザー・外部ツールの実際の挙動を踏まえる。Android専用ポリシーをStudio用として流用しない |
| プロジェクトの評価・審査 | 採用は未確定。採用前に署名提供済みの表示は行わない |

この表は一次確認であり、全依存の再監査やサービスの承認完了ではありません。

## 導入する場合の流れ

1. 上記の残確認を済ませ、プロジェクト説明・対象配布物・署名対象の案を準備。
2. 申請内容を確認してから申請し、サービス側の審査・設定案内に従う。
3. CIの生成物→署名要求→手動承認→署名検証→最終ZIP／MSIとチェックサム生成→Releaseの順へ変更。
4. 署名済みMSIの新規導入・更新と、ZIPの起動を確認。

ZIP配布のlauncher署名と、MSI内のlauncher／最終MSI署名の順序を整合させます。
署名後のファイルを改変したり、別ビルドの未署名ファイルを混在させたりしないようにします。

## 利用が難しい場合

Windowsは現行のMSI／ZIPを未署名で配布します。
Release Notes・導入ガイドに未署名であること、警告が出る場合があること、
公式ダウンロード先とチェックサムの確認方法を記載します。
チェックサムは署名の代わりに発行者を証明するものではありません。
OS全体の保護設定を無効化する手順は案内しません。
macOSのDeveloper ID署名・公証の導入方針は維持します。

## 参照

- [SignPath Foundationの条件](https://signpath.org/terms.html)
- [SignPath申請の入口](https://signpath.org/apply.html)
- [Microsoft：SmartScreenと署名](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation)
- [配布方針](FORMAL_RELEASE_DISTRIBUTION.md)
