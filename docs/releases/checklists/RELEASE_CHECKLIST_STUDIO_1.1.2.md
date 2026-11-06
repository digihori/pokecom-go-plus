# Studio 1.1.2公開記録

2026-11-06。状態：正式公開済み（Latest）。公開日時：2026-11-06 19:18 JST。
Playerの署名・ストア提出は今回実施しない。

## 設定

- 公開版数／ProjectInfo：1.1.2
- native package版数：1.8.0（前回1.7.0より増加）
- 公開タグ：v1.1.2（対象commit `f198dff3c31b5d9d7ea19cb4cf6fc2e4b5470e8f`）
- リリースノート：[Studio 1.1.2](../notes/RELEASE_NOTES_STUDIO_1.1.2.md)
- 変更の中心：PC-1251／1261／1401系のRAM構成と系列別の設定保存
- MSI UpgradeCode／Bundle IDは維持

## 確認・公開

- [x] Gradle／ProjectInfo／テスト期待値／native package／MSI検査／release workflowの版数を更新。
- [x] Playerとは別のStudioリリースノートを用意。
- [x] JDK 21の全体buildとCore desktopTest（585 tasks、成功）。
- [x] Studio desktopテスト／配布runtime検査／公証スクリプト5件とメモリ変換ツール12件のテスト。
- [x] 候補commitのCI成功。Windows／Linux／macOS／共有Player・モバイルCIを含む。
- [x] タグv1.1.2のrelease workflow成功（macOS署名・公証を含む）。
- [x] 最終12資産を取得し、全SHA-256を照合。macOS／Windows ZIPとLinux tarのProjectInfoは1.1.2、macOS bundle／DEB／MSI検査のnative版数は1.8.0。macOSはarm64。
- [x] macOS配布ZIPのアプリを起動し、PC-1251のROM復元・実行とAboutの1.1.2表示を確認。Windows／LinuxのGUI起動・インストールは今回手動未実施。CIのビルド・テスト・MSI識別子・runtime検査の成功と区別する。
- [x] GitHub Releaseを正式公開。Latest判定、公開ページ、全12資産の認証なしダウンロード先HTTP 200を確認。
- [x] README・目次・日英セットアップページのStudioダウンロード先を更新。Playerの準備中表示は維持。

## 公開先と実行記録

- [Studio v1.1.2（正式・Latest）](https://github.com/digihori/pokecom-go-plus/releases/tag/v1.1.2)
- [候補commitのCI](https://github.com/digihori/pokecom-go-plus/actions/runs/42198977445)
- [タグのrelease workflow](https://github.com/digihori/pokecom-go-plus/actions/runs/42199013533)
- Release ID：436347093。prereleaseではない。
- macOSアプリ／DMGともcodesign検査、stapler validate、Gatekeeper検査を通過（Notarized Developer ID）。
- Windowsは未署名。macOS以外の手動GUI確認は今回の検証に含めない。
- iOSの版数／署名／App Store Connectの提出内容は変更していない。

macos-signing環境にv1.1.2タグだけを追加許可。既存のrequired reviewer設定は維持。

## 配布資産のSHA-256

| 資産 | SHA-256 |
| --- | --- |

| pgp_memory.py | `86225411b56818417932d5fa41aee5f168b77271e9865cc15c75628d44c5a2a5` |
| pgp_memory_README.md | `cc891b4290b794a56b99a81fc6828c0bba57a067447844771c5cad86dabd5492` |
| PokecomGOStudio-1.1.2-linux-amd64.deb | `4fd70978df542d719239e9b9245bacfec1f3d4c46f82f0cee43f4912a7eb822a` |
| PokecomGOStudio-1.1.2-linux-amd64.tar.gz | `d63da286d56a98155adf63c4a2a3e544e1d4797a0ba0063ea990c13d9ddab6c9` |
| PokecomGOStudio-1.1.2-macos-arm64.dmg | `597a668aa4b87afe29dfcc2c3e4bb4fd5b7d0ef5597cee1518df3e6975a22e1c` |
| PokecomGOStudio-1.1.2-macos-arm64.zip | `f2d2b8b07d2d140cce00a0b383abc1dddc3f386bbe7f4b75bbc1ea09f6d3f805` |
| PokecomGOStudio-1.1.2-windows-x64.msi | `f816d7f4fd91ae6ce7cbc9f8053c69bcbb3550b9da4cfa0ebca4e7772aa63459` |
| PokecomGOStudio-1.1.2-windows-x64.zip | `1c1533c440bbe43242af0c61d32968c7e801b3c10116f47362a0cfed8ef05736` |
| SHA256SUMS-linux.txt | `097bf1e1cfac8535df475608d16a0a42798b8d075ba9229450faaa9a1fad0f4a` |
| SHA256SUMS-macos.txt | `c654abacbc6b7e64831f446d81d6d13355dba1b5945c2a79ceefb45f2fd179af` |
| SHA256SUMS-tools.txt | `600ff12c823faf1a9471c9560a9c7eeccb5e1402e908cace7f67a3b0cf7fff1c` |
| SHA256SUMS-windows.txt | `e94445635bea13e0d5da71ce1a917efd20177362a68446328cc303c9f1494975` |
