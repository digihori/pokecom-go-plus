# 1.1.0公開チェックリスト

2026-10-31作成。状態：Studio `v1.1.0`正式公開済み・Latest。Androidの実機確認・ストア提出／公開は保留。
対象はStudio／Android Player。機能修正・既存テスト結果の基準はmainの`d493f16`。
過去の確認と最終候補の確認を区別し、未確認項目は完了にしません。

## 版数と配布

| 対象 | 現在の準備値 | 備考 |
| --- | --- | --- |
| Gradle／共通ProjectInfo | 1.1.0 | Studioの表示・共有コードの版数 |
| Studio native package | 1.7.0 | 1.0.0公開時の1.6.0より増加。MSI UpgradeCode維持 |
| Android | versionName 1.1.0 / versionCode 11 | 前回10。提出直前にPlay Consoleの最大値を再確認 |
| iOS | MARKETING_VERSION 0.7.0 / CURRENT_PROJECT_VERSION 3 | 審査中の設定を維持。次回は別途1.1.0へ更新 |

Studio：macOS arm64 DMG／ZIP、Windows x64 MSI／ZIP、Linux amd64 DEB／tar.gz。
Releaseには単独メモリー変換ツールとSHA-256一覧も含めます。Android APK／AABは添付しません。
署名付きAABの作成・Play Console提出・公開は開発者が担当します。

## 準備と既存の検証記録

- [x] 版数、MSI検査、release workflowの資産名・公証入力・ノート参照を1.1.0向けに更新。
- [x] [日英リリースノート案](../notes/RELEASE_NOTES_1.1.0.md)とドキュメント索引・READMEを整備。
- [x] BASICのPC-1245／1401／1360K修正・手動OK報告と、自動build記録を確認：[BASIC I/O](../../../test-data/basic/io/README.md)。
- [x] WAV正常6ケースの実機往復OK報告を確認：[結果](../../../test-data/wav/hardware/RESULTS_2026-10-31.md)。
- [x] DebuggerのmacOS 24ケースOK、Windows／Linuxの総括OK報告とT22保留を確認：[受入れ記録](../../development/DEBUGGER_RELEASE_ACCEPTANCE.md)。
- [x] 操作性・BASIC互換性・Linux分類の既存確認を整理：[修正記録](../../development/planning/NEXT_RELEASE_FIXES.md)。

## 最終候補の確認（今回の版数更新後）

- [x] JDK 21で版数更新後の`./gradlew build :core:desktopTest`を成功させる（下記記録）。
- [ ] 最終候補commitを確定・記録し、そのcommitでCIと配布検証を行う。
- [ ] macOS／Windows／Linux・AndroidのCIを成功させ、対象commitとrun URLを記録。
- [ ] Studio全OSでパッケージ生成、必須runtime、表示1.1.0／内部1.7.0、MSI identity・更新インストールを確認。
- [ ] macOS最終DMG／ZIPのDeveloper ID署名・公証・staple・Gatekeeperを確認。
- [ ] 最終配布物で起動・ROM復元・BASICロード／保存／短縮ロード後の編集・キー入力・Debugger・MCP・Quick Assemble・WAVをスモーク確認。Linux分類と各OSのフォーカスも確認。
- [ ] AndroidのversionName／versionCodeとAAB署名・更新インストールを確認。実端末でROM登録・BASIC入出力・実行・機種切替を確認。
- [ ] Play Consoleの最大versionCodeが11未満であることを確認し、日英更新文・データセーフティ・対象API・配布範囲を確認。
- [ ] 既知制限・T22保留を最終ノートへ反映し、権利・ライセンス・ROM／秘密情報非同梱を確認。

## タグ・公開

- [x] 最終候補commitとノートを確定し、`v1.1.0`タグを作成・push（`027cd6d`）。
- [ ] release workflowを成功させ、ドラフトReleaseの資産・SHA-256・対象commit・非prerelease設定を確認。
- [ ] ダウンロードした最終配布物の起動・インストールを確認し、公開判断を記録。
- [ ] Studio Release／Android Playの公開完了日時とURLを記録。
- [ ] 公開後、認証なしダウンロード・checksum・Pages／READMEの最新導線を確認し、1.1.0へ更新。

## iOS：審査中0.7.0の維持と次回1.1.0

- [x] Xcodeの0.7.0／build 3、Info.plist、提出済み審査返信・サンプルを変更しない。
- [ ] 審査結果と提出済みarchiveの所在・識別情報を開発者側で記録。再アーカイブを提出済みビルドの代替にしない。
- [ ] 次回1.1.0候補を切る時点でMARKETING_VERSIONと新build番号を設定。審査中の提出を差し替えない。
- [ ] 共通CoreのBASIC修正・文字互換性をiOS実機で確認し、Player固有の回帰確認・CIを実施。
- [ ] iOS向けの差分だけで更新文・スクリーンショット・審査説明を整え、別途提出判断を行う。

共通ProjectInfoは1.1.0に進みますが、App Storeの版数はXcode設定で管理します。
今後mainから作るiOSビルドは提出済み0.7.0 build 3と同一物ではありません。
この準備でApp Store Connectの提出内容・archive・配信設定は操作していません。

## 今回の検証結果

2026-10-31、`d493f16`に本リリース準備の変更を加えた作業ツリーで、JDK 21の
`./gradlew --gradle-user-home .gradle-local build :core:desktopTest --max-workers=2`
が成功しました（Gradle heapは実行時のみ4 GiB、2分54秒）。
Core desktopは520件成功、Studioは476件中473件成功・3件skip、失敗・エラーは0でした。
Android lintに既存のKotlin metadataバージョン診断が出ましたが、全体buildは成功しました。
`git diff --check`、新規リリース文書の相対リンク、版数設定とiOSの0.7.0／build 3維持も確認済みです。
最終配布物・CI・ストアの結果は上記で別途管理します。


## 1.1.0確認用配布物の受入れ報告

対象commit：`1b775f5b6404864c9c8a38d1950190417cc16417`。
[配布workflow 41199341887](https://github.com/digihori/pokecom-go-plus/actions/runs/41199341887)は
Windows／Linux／Androidの生成と、承認付きmacOS app／DMGの署名・公証を含め成功しました。
Windows MSIのidentity／version検査も成功しています。タグなしの手動実行のため、ドラフトRelease作成はskipです。
[同commitのCI 41198985220](https://github.com/digihori/pokecom-go-plus/actions/runs/41198985220)も成功しました。

ユーザーより「mac/windows/linuxすべて確認しました」との報告を受領し、
この確認用配布物の3 OSでの動作確認済みとして記録します。
OS詳細版・利用機種・操作別の結果・取得ファイルのchecksumは未記録です。
この総括報告を、全スモーク項目・更新インストール・Gatekeeper検査・T22の個別合格へ拡大しません。
Android実機の確認完了報告はまだありません。
正式タグで生成する最終配布物のhash・受入れ・公開判断は引き続き上記チェックリストで管理します。


## 正式タグへ進む判断

ユーザー指示によりAndroid実機確認は後日に保留し、Studio 3 OSの確認済み候補から
正式タグ・ドラフトRelease作成へ進みます。Android実機確認は未完了のまま維持し、
Play提出・公開完了とは扱いません。GitHub Releaseの一般公開も別途判断します。
機能・ビルド設定は検証済み`1b775f5`から変更せず、以後の変更は受入れ・リリース文書のみです。


正式タグ`v1.1.0`は`027cd6d`に作成・pushしました。
[正式タグの配布workflow 41200803433](https://github.com/digihori/pokecom-go-plus/actions/runs/41200803433)を起動済みです。
macOS署名ジョブはステップ開始前にenvironment保護ルールで拒否されました。
GitHubの診断：`Tag "v1.1.0" is not allowed to deploy to macos-signing due to environment protection rules.`
`macos-signing`環境の許可タグ設定を確認・修正した後、失敗ジョブを再実行し、署名・公証とドラフト作成を確認します。


## 正式タグ配布workflow再実行の結果

`macos-signing`のタグ許可設定後、実行41200803433の失敗ジョブと依存ジョブを再実行（attempt 2）。
承認後、macOS app／DMGの署名・公証、資産アップロード、ドラフトRelease作成がすべて成功しました。
承認時に画面へgateのエラー通知が出ましたが、実行は承認を通過して正常完了しています。

[1.1.0ドラフトRelease](https://github.com/digihori/pokecom-go-plus/releases)
（Draftの一時URLは保存で変わるため一覧へリンク。Release ID `429927651`、tag `v1.1.0`、commit `027cd6d`）のDraft表示を確認しました。
添付資産はStudio配布6点・変換ツール2点・SHA256SUMS 4点の計12点です。
GitHub自動生成のソースarchive 2点を含むUIの表示はAssets 14です。Android APK／AABは含みません。
資産名とGitHub表示のSHA-256を確認済みです。ダウンロード内容とSHA256SUMS本文の照合は未実施です。
正式タグ資産の動作確認報告は次節に記録します。Android実機確認は引き続き保留です。
一般公開・ストア提出・Latest切替は行っていません。


## 正式タグ配布物の動作確認報告（2026-10-31）

ドラフトRelease作成後、ユーザーから「動作確認OKです」と報告を受領しました。
`v1.1.0`（`027cd6d`）のStudio配布物に対する動作確認OKとして記録します。
OS別・操作別の内訳、取得ファイルのchecksumは今回の報告には含まれていません。
先の3 OS確認報告と併せて管理し、個別の全チェック項目の合格には置き換えません。
Android実機確認は後日とする指示を維持します。
一般公開の指示はまだ受けていないため、ReleaseはDraftのままです。


## Studio正式公開（2026-10-31）

ユーザーの正式公開指示を受け、既存のドラフトReleaseを正式公開しLatestに設定しました。
公開日時：2026-10-31 21:39:41 JST。
公開URL：https://github.com/digihori/pokecom-go-plus/releases/tag/v1.1.0
`draft=false`／`prerelease=false`、添付資産12点を未認証APIで確認しました。
対象タグ`v1.1.0`・commit `027cd6d`・配布資産は変更していません。
Androidの実機確認とPlay提出・公開は保留、iOSの審査中0.7.0 build 3も維持します。
README日英、ドキュメント索引、PlayerセットアップページのStudioダウンロード導線を1.1.0へ更新します。
