# Studio 1.1.2公開記録

2026-11-06。状態：リリース準備中。GitHub Release公開は未完了。
Playerの署名・ストア提出は今回実施しない。

## 設定

- 公開版数／ProjectInfo：1.1.2
- native package版数：1.8.0（前回1.7.0より増加）
- タグ予定：v1.1.2
- リリースノート：[Studio 1.1.2](../notes/RELEASE_NOTES_STUDIO_1.1.2.md)
- 変更の中心：PC-1251／1261／1401系のRAM構成と系列別の設定保存
- MSI UpgradeCode／Bundle IDは維持

## 確認・公開

- [x] Gradle／ProjectInfo／テスト期待値／native package／MSI検査／release workflowの版数を更新。
- [x] Playerとは別のStudioリリースノートを用意。
- [ ] JDK 21の全体buildとCore desktopTest。
- [x] Studio desktopテスト／配布runtime検査／公証スクリプト5件とメモリ変換ツール12件のテスト。
- [ ] 候補commitのCI成功。
- [ ] タグv1.1.2のrelease workflow成功（macOS署名・公証を含む）。
- [ ] 最終資産の版数・checksum・アーキテクチャ・macOS署名／公証を確認。
- [ ] 配布物の起動・インストール確認と制限事項を記録。
- [ ] GitHub Releaseを正式公開し、認証なしのダウンロードを確認。
- [ ] README・目次・公開状態の案内を更新。

最終candidate commit、Actions URL、資産とSHA-256、確認結果は作業中に追記する。

macos-signing環境にv1.1.2タグだけを追加許可。既存のrequired reviewer設定は維持。
