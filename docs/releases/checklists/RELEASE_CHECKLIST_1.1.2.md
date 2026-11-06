# Player 1.1.2 リリース準備・引継ぎ

2026-11-06。状態：文書整備済み、リリース作業は担当者が実施。
対象：Android／iOS Player。Studioの版数変更・配布はこの手順に含めない。
この文書の作成では署名、Archive、ストア提出、公開、タグ作成を実施していない。

[リリースノート](../notes/RELEASE_NOTES_1.1.2.md) ·
[日本語の更新文](../store/1.1.2/whats-new.ja.txt) ·
[English update text](../store/1.1.2/whats-new.en.txt)

## 版数と現状

| 対象 | ソース上の値 | 次回の予定／担当者による確認 |
| --- | --- | --- |
| Android | versionName `1.1.2`、versionCode `13` | Play Consoleの使用済み番号と最終AAB内の値を確認 |
| iOS Debug／Release | MARKETING_VERSION `0.7.0`、CURRENT_PROJECT_VERSION `3` | 予定は`1.1.2 (13)`。Xcodeで更新し、Archive内の値も確認 |
| Studio／共有ProjectInfo | `1.1.0` | 今回のPlayer文書整備では変更しない |

Android設定：`androidApp/build.gradle.kts`。
iOS設定：`iosApp/PokecomGOPlayer.xcodeproj/project.pbxproj`。
`Info.plist`はMARKETING_VERSION／CURRENT_PROJECT_VERSIONを参照する。
iOSの提出済み0.7.0 (3)に関する審査資料は過去の提出記録として保持する。
審査結果や1.1.1の公開有無は未確認で、予定を公開済みと扱わない。

## 整備済み・既存の確認記録

- [x] 機能変更の基準commit：`4679f00`。RAM制限解除、3系統のキー位置、PC-1261 LCD、関連テストを含む。
- [x] 1.1.0以降のメモリ保存UI改善とAndroid release最適化もリリースノートへ記載。
- [x] 変更・新規文書のローカルリンク先と`git diff --check`を確認。コードと版数設定は今回変更していない。
- [x] 日英ストア更新文を用意。ストアの入力欄・文字数と実際の前版に応じて提出時に調整する。
- [x] Android／iOSに同じ更新画像・キー座標・PC-1261 LCD配置を反映。
- [x] キー位置とPC-1261の全点灯画面は目視確認済み。最終文字領域は`(125, 59.75, 1015.3, 106.5)`で確定。
- [x] 最終位置調整後のAndroid debug APK生成と共有desktopTestは成功。
- [x] PC-1261追加シンボルの独立点灯・リセットと、文字領域／シンボルの分離を自動テストで確認。
- [x] Swift単体テスト59件中、旧LCD配置を前提にした1件を修正。関連2件の再実行は成功（最終上下位置の微調整前）。

直近の全体buildは、Android lintのKotlinメタデータ互換性診断とdebug APKのpackageエラーを記録。
その後の対象限定debugビルドと共有desktopTestは成功したが、最終候補の全体build成功として扱わない。
実行記録は[ポーティング記録](../../development/PORTING_NOTES.md)、
[Android最適化](../../development/ANDROID_RELEASE_OPTIMIZATION.md)、
[メモリ保存：Android](../../development/PLAYER_ANDROID_MEMORY_SAVE.md)／
[iOS](../../development/PLAYER_IOS_MEMORY_SAVE.md)を参照。

## 担当者が行う提出前確認

- [ ] 最終ソースrevisionと版数を固定し、未コミット変更の有無を記録する。
- [ ] JDK 21で`./gradlew build :core:desktopTest`を実行し、成功・警告・失敗を記録する。
- [ ] Android releaseを生成する：`./gradlew :androidApp:bundleRelease :androidApp:assembleRelease :androidApp:testReleaseUnitTest`。
- [ ] Android署名に用いる鍵とPlay Console登録情報を照合し、最終署名済み配布物の版数を確認する。
- [ ] 配布物に対応する`androidApp/build/outputs/mapping/release/mapping.txt`を保管する。
- [ ] iOSのVersion／Buildを予定値へ更新し、`sh iosApp/test-simulator.sh --unit`を実行する。
- [ ] iOS ArchiveのBundle ID・Version・Build・署名を確認し、ArchiveとdSYMを保管する。
- [ ] 最終署名済みAPK／iOSビルドで、ROM登録・起動・キー入力・BASICロード／保存・DMP保存／再読込みを確認する。
- [ ] RAM制限解除の初期OFF・機種別保存・再起動時の復元・Play時の変更適用・キャンセル時のRAM保持を確認する。
- [ ] 機種／RAM構成の変更前に保存が必要なことを確認し、起動し直す場合の消去確認を確認する。
- [ ] PC-1245・1251系・1261系の7キーと、PC-1261 LCDの文字・全6マークを確認する。
- [ ] AndroidではR8適用後の署名済みreleaseで操作を確認する。debug成功だけで完了にしない。
- [ ] 更新前のアプリから、保存済みROM・モデル選択・既存設定を保持して更新できることを確認する。
- [ ] ストア文、画像、サポート／プライバシーURLと実際のアプリの内容を照合する。
- [ ] ストア提出・公開を実施し、日時・Version／Build・対象revisionをこの文書へ追記する。

## 配布メモ

- 確認用debug APKはストア提出物ではない。Androidの正式更新はGoogle Play向けの署名済みAABで行う。
- Android署名画面の「登録済みの鍵と異なる」という表示は、アップロード鍵とアプリ署名鍵を区別して確認する。スクリーンショットだけで鍵の正否は確定しない。
- ストア公開後のREADME・公開状態の更新は担当者が実施する。未公開タグへのリンクを先に追加しない。
- 現行ヘルプ／セットアップHTMLの変更はローカル原稿。mainへpushするとPages公開対象になるため、公開タイミングは担当者が判断する。
- ROM／ゲームの同梱、RAM・CPUセーブステートの提供はない。
