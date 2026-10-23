# 1.0.0正式リリースチェックリスト

2026-10-23。状態：候補版準備中。未公開、未タグ付け。
公開版数はStudio／Androidとも1.0.0、Studio内部版数1.6.0、Android versionCode 10。
Playへの過去の最大versionCodeは9であることを確認済みです。
iOSのリリースは別スレッドで進め、今回の公開条件に含めません。
Android AABへの署名・Play Consoleの最終提出内容・ストア公開は開発者が担当します。
こちらでは未署名AABの生成・版数確認と、StudioのCI／配布検証を進めます。

## 準備

- [x] 版数・runtime・テスト・MSI検査・workflowの番号を揃える。
- [x] Windows UpgradeCodeを維持。Android debug APKはRelease添付から除外。
- [x] 日英リリースノート案を作成。
- [x] M6〜M8の文書・権利・プライバシー確認を記録。
- [x] macOS試験用0.7アプリ／DMGの公証が両方Acceptedになったことを確認。
- [x] 1.0候補でJDK 21全体build・Core／Studio／Androidテストが成功。
- [x] ローカル1.0候補のmacOS署名・公証・staple・Gatekeeper検証が成功。
- [ ] 最終候補コミットを確定し、全CI成功を確認。
- [ ] Windows／Linuxの配布workflowが成功し、ドラフトReleaseを作成。
- [ ] Windows／Linux最終配布物の版数・MSI identity・ライセンス・チェックサムを確認。

## M3・M4：最終候補での受入れ

- [ ] macOS：初回起動・ROM登録／作成・BASIC／DMP・音声／入力・Project／Build／Load・主要Debugger操作。
- [ ] Windows／Linux：配布物で主要操作を確認。OS・CPU・試験機種・成果物checksumを記録。
- [ ] Android：ROM登録・機種切替・プログラム入出力・背景復帰・外部入力を確認。
- [ ] 0.7からのStudio更新、MSI更新で設定・登録済みROM・Projectを維持。
- [ ] 開発者担当：Google Playで既存署名を使うAndroid更新と、ROM・設定の維持を確認。
- [ ] RAM修正後のBASIC入力・実行・保存／再読込み。MEM値の保留は公開条件に含めない。

## 公開前

- [ ] Windows未署名配布の案内・既知制限を確認。
- [ ] 開発者担当：Play Consoleのデータセーフティ・問い合わせ先・最終署名済みAABを照合。
- [ ] 日英ポリシーの改訂を公開反映し、同梱原文・Play掲載URLとの一致を確認。
- [ ] 対象コミット・依存通知・ROM／秘密情報の非混入・最終checksumを確認。
- [ ] リリースノートの準備中表示を取り除き、公開内容を確定。
- [ ] 公開の最終判断を行う。

## 公開

- [ ] 検証したコミットへv1.0.0タグを作成し、正式Releaseを公開。
- [ ] 公開した全資産のダウンロード・署名／checksumを確認。
- [ ] 開発者担当：Android正式版をGoogle Playへ更新し、公開結果を確認。
- [ ] 日英README・目次の最新案内を1.0へ切替。
- [ ] 公開セットアップページの日英各2リンクを1.0へ切替し、Pagesの反映を確認。
- [ ] 候補SHA・CI／配布run・公開日時・受入れ環境・残制限をここへ記録。

公開済み0.7のタグ・資産・過去記録は変更しません。
CI成果物の生成成功は、M3・M4の手動受入れを代替しません。

## ローカル候補の検証（2026-10-23）

- `build :core:desktopTest :desktopApp:createDistributable :desktopApp:packageDmg :androidApp:bundleRelease`成功。
- Core desktop 495件成功。Studio 406件中1件skip、失敗なし。Android debug 80件成功。
- 公証用helperの5テスト成功。未承認・タイムアウト・ad-hoc署名で先へ進まないこと、再提出せず再開できることを確認。
- workflow YAMLを解析し、17件のbash処理の構文を確認。Releaseの収集対象はStudioのみ、正式公開設定。
- macOS Bundle IDは既存値を維持。Info.plistの内部版1.6.0と同梱ProjectInfoの1.0.0を確認。
- Android debug APKはversionName 1.0.0／versionCode 10／minSdk 26／targetSdk 36。
- Android AABはローカル生成済みですが未署名です。Play提出用署名は別途必要です。
- 1.0アプリ公証ID：`a453c608-1e67-41ec-9dbd-fe3e44e44210`。
- 1.0 DMG公証ID：`178f2fca-9677-4cfc-afc7-287b67c7264e`。
- 公証待機の45秒以内には処理が完了せず、提出IDを保存。承認確認・staple・最終checksumは継続。
- macOSは承認付きEnvironment SecretsでCI署名・公証する方針です。Secrets登録とCI検証は未完了です。
- commit／push／tag作成／GitHub Release公開／Play提出は実施していません。

## 1.0 macOS候補の公証完了

アプリ・DMGとも既存提出IDの状態がAcceptedとなり、staple／validate・署名検証・Gatekeeperの
`source=Notarized Developer ID`判定に成功しました。再提出は行っていません。
検証用DMG／staple済みアプリのZIP／SHA256SUMSを`desktopApp/build/release-candidate-1.0.0/`へ保存しました。
この候補は作業ツリーから生成したもので、最終コミット・CI成果物として確定したものではありません。

## 承認付きCI署名での公開手順

- [x] macos-signing Environmentの承認必須・管理者スキップ禁止・実行元制限を設定。
- [ ] Environment Secretsへ証明書・秘密鍵・公証認証を登録。
- [ ] 手動workflowを承認し、hosted runnerで署名・公証・staple・Gatekeeper検証が成功。
- [ ] 最終タグのworkflowを承認し、全OSの資産とchecksumをドラフトReleaseで照合。
- [ ] 手動受入れと公開文面の確認後、ドラフトを公開。

手順：[macOS署名・公証](../../reference/MACOS_SIGNING.md)。
