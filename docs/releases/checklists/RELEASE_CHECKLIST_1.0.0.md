# 1.0.0正式リリースチェックリスト

2026-10-28更新。状態：各環境の受入れ確認済み、公開資料・最終配布物の照合中。未公開、未タグ付け。
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
- [x] Windows／Linux：1.0候補の手動確認済み（2026-10-28）。OSの詳細版・個別操作・取得ファイルのchecksumは未記録。
- [x] Android：1.0候補の手動確認済み（2026-10-28）。個別操作・端末情報の詳細は未記録。
- [ ] 0.7からのStudio更新、MSI更新で設定・登録済みROM・Projectを維持。
- [ ] 開発者担当：Google Playで既存署名を使うAndroid更新と、ROM・設定の維持を確認。
- [ ] RAM修正後のBASIC入力・実行・保存／再読込み。MEM値の保留は公開条件に含めない。

## 公開前

- [x] M10（2026-10-25追加・公開必須条件）：AI連携のCORS全サイト許可を見直し、Origin／Host検証を実装。Bearer認証を維持し、不正Origin／Host・プリフライト・認証なし／不正トークンでtoolが実行されない自動テストと、正規AIクライアントの接続・承認付き操作の回帰確認を完了する。[必須タスクの詳細](../../development/planning/FORMAL_RELEASE_TASKS.md)。入口の実装・HTTP自動テストと、修正版配布アプリへの実接続・承認付き操作の受入れを完了（2026-10-28）。
- [x] Windows未署名配布の案内・既知制限を日英リリースノートで確認。
- [ ] 開発者担当：Play Consoleのデータセーフティ・問い合わせ先・最終署名済みAABを照合。
- [ ] 日英ポリシーの改訂を公開反映し、同梱原文・Play掲載URLとの一致を確認。
- [ ] 対象コミット・依存通知・ROM／秘密情報の非混入・最終checksumを確認。
- [x] リリースノートの準備中表示を取り除き、公開用の文面を整理。公開の最終判断は別途行う。
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
- [x] Environment Secretsへ証明書・秘密鍵・公証認証を登録。
- [x] 手動workflowを承認し、hosted runnerで署名・公証・staple・Gatekeeper検証が成功。
- [ ] 最終タグのworkflowを承認し、全OSの資産とchecksumをドラフトReleaseで照合。
- [ ] 手動受入れと公開文面の確認後、ドラフトを公開。

手順：[macOS署名・公証](../../reference/MACOS_SIGNING.md)。

## CI候補の受入れ（2026-10-24）

[手動run 40042209096](https://github.com/digihori/pokecom-go-plus/actions/runs/40042209096)は全ジョブ成功。
macOSアプリ／DMGの公証・staple・Gatekeeper検証と全OSの成果物生成を確認しました。
開発者によるmacOSインストールは成功しました。
アプリ一覧への未表示と検索結果の重複が報告され、表示元の確認は継続中です。
DMGのボリュームは専用ディスクアイコンへ変更しますが、DMG内アプリはインストール先と同じアイコンです。
ディスクアイコンの変更だけで検索結果の重複が解消するとは扱いません。

Spotlight照会でインストール先と開発用ビルド先の2つのStudioを確認しました。
DMG取り出し後の検索重複は開発用コピーが有力で、配布物の重複インストールとは区別します。
DMG専用ディスクアイコンの生成・組込みとbuild／core:desktopTest／packageDmgは成功。
このアイコン変更を含むCI署名候補の再生成は未実施です。

追加画像で名前順のP付近にもStudioが表示されないことを確認しました。
カテゴリがUnknownだったため、macOS配布設定に`public.app-category.developer-tools`を明示しました。
生成Info.plistへの反映とbuild／core:desktopTestは成功。表示改善の効果は再インストール後に検証します。

## 利用ガイドの追加確認（2026-10-25）

- [x] ガイド前半に、旧Pokecom GOのバイナリー形式からDMP／Intel HEXへの変更を説明する。
  アドレス情報・旧ファイルの移行方法・StudioとPlayerの対応差を整理し、詳細な書式と操作へ誘導する。
  詳細：[M7の文書残タスク](../../development/planning/FORMAL_RELEASE_TASKS.md#追加の気づき文書の残タスク2026-10-25)。

- [x] 旧バイナリーからDMP／Intel HEXへの変換ツールを提供し、移行ガイドと対応を確認。
  2026-10-27：PythonファイルとREADMEで提供。ZIPは不要。12テスト・単独実行・文書例・全体build／core:desktopTestが成功。

## Chromeの配信経路比較（2026-10-27）

- [x] ActionsでブロックされたZIPと同一のZIPをテスト用Releasesへ添付し、GitHub側のSHA-256一致を確認。
- [x] テスト用ReleasesからはChromeでブロックされずダウンロードできることを確認。
- [ ] 正式Releaseの最終資産を、Chromeの保護設定を変えずにダウンロードできることを確認。

[比較結果](../../development/MACOS_ARTIFACT_COMPARISON_2026-10-24.md#github-releases配信との比較準備2026-10-27)。
今回のReleases経由の成功をもって、Actions側の危険判定理由の解明や、すべての配布資産の検証完了とはしません。

## macOSの署名済みDMG受入れ（2026-10-27）

対象はコミット`d4babd6170cb9bf8f4adf39177fff6ddd57d1103`、
[テスト用プレリリース](https://github.com/digihori/pokecom-go-plus/releases/tag/download-test-1.0.0-20261027)の候補です。
macOS 26.6.2／Apple Siliconで次を確認しました。

- [x] ReleasesのZIPをChromeでブロックされずダウンロードできる。
- [x] 署名・公証済みDMGからApplicationsへインストールできる。
- [x] インストール済みアプリの署名、公証証票、Gatekeeper検証が成功する。
- [x] 初回起動で通常のダウンロード確認が表示され、Appleのチェックで悪質なソフトウェアが検出されなかった旨を確認できる。
- [x] 初回確認の「開く」を選択した後、Dockのアプリ一覧にStudioが表示される。

初回確認前には一覧への未表示とLaunch Servicesのlaunch-disabledフラグを観測しました。
初回確認後の一覧表示を確認したため、この候補の一覧未表示は解消済みとします。
カテゴリ設定だけでは解消せず、初回起動確認後に表示されたという観測を記録します。
Spotlightの複数ヒットは開発用コピー等によるもので、一覧未表示とは別件です。
この確認をもって、ROM・Project・全設定の更新互換性や全OSの受入れまで完了したとはしません。

### macOS更新時の保存情報（2026-10-27）

同じ署名・公証済みDMG候補への更新後、既存のROM登録とエディタ設定が引き継がれていることを確認しました。

- [x] 既存のROM登録を維持。
- [x] 既存のエディタ設定を維持。
- [x] 既存Projectを開き、編集・Build・Loadが正常に行えることを確認。

その他の設定項目すべての確認や、Windows／Androidの更新互換性まで完了したとはしません。

2026-10-27、既存プロジェクトの編集・Build・Loadも正常に行えました。
今回のmacOS候補について、インストール、初回起動とアプリ一覧表示、ROM登録・エディタ設定の引き継ぎ、
既存プロジェクトの主要操作を確認済みです。全設定・全機種・全OSの総当たり確認を意味するものではありません。

## 追加修正の確認（2026-10-27）

ProgramメニューからROM経由のBASIC自動入力を外し、現行README・操作ガイド・Projectガイド・
機種別の利用説明・公開チェックリストから機能の案内を削除しました。
通常のBASICロード／保存は継続します。内部実装と過去の開発・検証記録は保持します。
JDK 21のbuild／core:desktopTestとgit diff --checkは成功しました。
この変更と保存ダイアログのキー入力漏れ修正を含む、配布候補の再生成・実操作での受入れは未実施です。

## localhost HTTP入口の対策（2026-10-27）

- [x] CORS全サイト許可を廃止し、Origin付き要求（空・`null`・localhostを含む）を403で拒否。
- [x] Hostは実ポート付き`127.0.0.1`／`localhost`の完全一致だけを許可。複数・欠落・別ホスト・別ポートを拒否。
- [x] ルートURLにも同じ入口検査を適用。MCPでは既存Bearer認証を維持。
- [x] 不正Origin／Host、プリフライト、認証なし／不正トークンでtoolが呼ばれないことを実HTTPで検証。
- [x] Originのない認証済み要求は両方の許可Hostで正常にtoolを呼べることを検証。
- [x] 修正版候補へAI側から認証付きHTTPで実接続し、読み取り・Studio上のソース変更承認・Buildを確認。却下は自動テストで検証。

Appleへの別スレッドの問い合わせは今回のStudio／Android公開条件に含めません。
Googleの判定理由は未解明で、正式配布資産のChromeダウンロード確認を継続します。

JDK 21の`build :core:desktopTest`は成功。既存の操作承認・却下を含むStudioの回帰テストも成功しました。
この時点では実接続と画面での確認は未完了でした。2026-10-28の受入れ結果は末尾に記録します。

## 最新CI候補の確認（2026-10-27）

コミット`49fe6aac069c25f9c4a67e09c504907b32f3cee8`の
[配布run 40495074014](https://github.com/digihori/pokecom-go-plus/actions/runs/40495074014)は、
Windows・Linux・Android・macOS署名公証の全ジョブが成功しました。
手動実行のため、正式タグとドラフトReleaseは作成していません。

- [x] 今回の候補をChromeでブロックされずにダウンロードできることを確認。

この成功だけでは以前の危険判定の原因やGoogle側の修正有無は特定できません。
取得した個別資産名・配信経路・checksumの照合と、正式Releaseの最終資産での確認は別途記録します。
この候補ではM10の実接続・画面上の承認付き操作の確認を継続し、後続の配布ランタイム修正後に完了しました（末尾参照）。

## 配布版MCP起動失敗の修正（2026-10-27）

候補`49fe6aa`のMCP起動時に`com/sun/net/httpserver/HttpServer`エラーを確認しました。
配布版の縮小Javaランタイムには`jdk.httpserver`が含まれず、開発JDK上のテストだけでは検出できませんでした。

- [x] 配布ランタイムへ`jdk.httpserver`を明示的に追加。
- [x] 全OSのRelease CIで、配布ランタイムの必須モジュールを公証・収集前に検査。
- [x] ローカル配布アプリのモジュール一覧と`HttpServer.class`の実在を確認。
- [x] JDK 21の`build :core:desktopTest :desktopApp:verifyDistributionRuntime`が成功。
- [x] 修正版の配布候補でMCP起動・認証付きHTTP接続・ソース変更承認・Buildを確認。却下は自動テストで検証。

同時に、BASIC読込みガイドからROM経由入力との長い行・編集バッファの比較記述を削除しました。

## M10の受入れ完了（2026-10-28）

配布ランタイム修正`e5efbc8`後、インストール済みStudioでMCPサーバーの起動を確認しました。
AI側から認証情報をログへ出さずにHTTP接続し、次を確認しました。

- PC-1245の機種情報を取得。機種を切り替えてPC-1401／PC-1350の情報も取得。
- 登録済みソースを読み取り、変更提案が`PENDING_APPROVAL`になることを確認。
- Studio上の承認後、状態が`APPLIED`となり、取得したソースが提案内容に一致。
- Buildを別途提案し、承認後の状態が`SUCCEEDED`となることを確認。
- 不正Origin／Host等の拒否とtool未実行、承認前の非変更・却下は既存の自動テストで確認。

この範囲でMCPサーバーとM10の受入れを完了します。手動の却下・全tool・全AIクライアントを
総当たり確認したという意味ではありません。承認ボタンの位置改善は次回リリースのタスクとして継続します。

## 正式公開の準備（2026-10-28）

Windows／Linux／Androidについて、1.0候補の手動確認を完了しました。
個別操作やOS詳細版の新しい証跡までは記録していないため、それらを推測で補いません。
修正版`e5efbc86460ef5f774757cbaebc8dbc2d24cc0c7`の
[配布CI 40497995940](https://github.com/digihori/pokecom-go-plus/actions/runs/40497995940)は全OSで成功しています。

- 変換ツールをReleaseへ`pgp_memory.py`／`pgp_memory_README.md`として直接添付する処理を追加。ZIPは作成しない。
- ツール用のSHA-256一覧を作成し、添付前に12件のPythonテストを実行する。
- ツール説明書へMITライセンス全文を含め、2ファイルだけで再配布条件を確認できるようにした。
- 日英READMEを現在の署名・ドラフトRelease作成フローへ更新。正式公開までは0.7の公開リンクを維持する。
- MCP受入れに使った独自Assemblyサンプルと合成ROMによるテストをソースツリーへ保存する。
  これらはアプリのRelease添付や同梱ゲームには追加しない。ROM・商用ゲームのコード／画像は含めない。
- 公開ポリシーの日英HTMLを取得。公開中は2026-10-22版で、候補にあるホーム表示形式・シリーズ内選択機種の保存説明が未反映。
  `main`への取り込み後、Pagesの反映を再確認する。

この準備段階では、正式タグの作成、Releaseの正式公開、Androidの署名・Play提出は行いません。

### 受入れ済みCI資産の再照合

対象は上記runの`e5efbc8`候補です。正式タグからの最終資産とは区別します。

- 全OSのインストーラーと展開型配布物の計6ファイルが、CIのSHA256SUMSと一致。
- Windows ZIP／Linux tar.gz／macOS ZIPに、ソースと一致するLICENSE・第三者通知を同梱。
  Windowsの改行差はCRLF／LFを正規化して照合した。
- 3環境の同梱Javaランタイムに`jdk.httpserver`を確認。
- macOS app／DMGの署名・公証証票の検証が成功。appはGatekeeperで`Notarized Developer ID`として受理。
- macOS内部版は1.6.0（短縮表示1.6）、Linux DEBは1.6.0／amd64。Windows MSI identity・版数のCI検査は成功。

| CI資産 | SHA-256 |
| --- | --- |
| `PokecomGOStudio-1.0.0-linux-amd64.deb` | `e67bcf6fd0d4086b4bd21c14f4f4a41d8dd4efb91c4ec67a61a2bf54bbf6c93c` |
| `PokecomGOStudio-1.0.0-linux-amd64.tar.gz` | `6dde4c13a3663adfa7123325d32a9347ecddc38c70c69cc81dd5be10efd86067` |
| `PokecomGOStudio-1.0.0-macos-arm64.dmg` | `cb172c7fe8ac9cc7d7ffc23b04a6bc5b26a6dd59e9d18471a70302009a0aeb42` |
| `PokecomGOStudio-1.0.0-macos-arm64.zip` | `8ef839a63736531fdf12efab613e84fb375ab2c38c035c7e68edea9c2e12bbf2` |
| `PokecomGOStudio-1.0.0-windows-x64.msi` | `dfa64fbfcb9263dbbae1f6a3035f594609dcf9ded64026b5fb6e2e732cf92742` |
| `PokecomGOStudio-1.0.0-windows-x64.zip` | `6f47b09d0865e7b05f6683971bfa7e2b1770926ba9aeff64bbda944212d79b20` |

公開準備の下書きPR：[Studio／Android 1.0.0](https://github.com/digihori/pokecom-go-plus/pull/1)。
次の順に進めます。

1. PRのCI結果を確認し、mainへの取り込みを判断する。
2. main取り込み後、Pagesのポリシー反映を確認する。
3. 検証対象コミットへv1.0.0タグを作成し、承認付き署名・公証からドラフトReleaseを生成する。
4. 最終資産のchecksum・Chromeダウンロード・公開文面を確認して、正式公開を判断する。

Android AABの署名とPlayへの提出・公開は、引き続き開発者が担当します。
