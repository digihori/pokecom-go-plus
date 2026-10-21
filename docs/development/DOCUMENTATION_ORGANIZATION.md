# ドキュメント棚卸しと分類・配置案

作成日：2026-10-22。手順1（棚卸し）・手順2（分類と配置案）の成果物。
対象は作業開始時のGit管理下Markdown全件。分類は文書の入口・見出し・状態記載と参照箇所に基づく一次判定です。
本文と実装の照合・履歴分離は手順3、目次は手順4、配置変更と参照更新は手順5で実施しました。
手順1・2の時点では本書だけを追加しました。手順3の実施状況は以下の進捗を参照してください。
本書自体は整理作業用の開発記録として、`docs/development/DOCUMENTATION_ORGANIZATION.md`へ移動済みです。

## 1. 対象と分類

「公開用／内部用」は読者と入口の区別です。公開リポジトリ内の開発記録も公開情報であり、機密扱いにはなりません。
主要な4分類に加え、README・権利情報・リポジトリ規約は、それぞれの役割を維持します。
「現行ガイド候補」は内容の正確性を確認済みという意味ではありません。
「履歴」も全課題が完了したことを意味せず、未確認事項・当時の証拠を保持する区分です。

| 分類 | 件数 | 主な読者 |
| --- | ---: | --- |
| リリース資料 | 14 | 利用者（notes）／公開担当（checklists） |
| 公開入口 | 2 | 全利用者 |
| 利用ガイド | 17 | Studio／Player・補助ツール利用者 |
| 技術資料 | 26 | 開発者・形式や機種仕様を調べる人 |
| 権利情報 | 1 | 全利用者・配布担当 |
| 運用規約 | 1 | 開発者・エージェント |
| 開発記録 | 63 | 開発・調査・検証担当 |

合計：**124件**（本書は集計外）。各ファイルの分類・配置候補は末尾の一覧に全件掲載します。

## 2. 配置案

```text
README.md / README.ja.md       日英の公開入口
THIRD_PARTY_NOTICES.md         権利情報（LICENSEも現位置）
docs/
  README.md / README.en.md    読者別の総合目次（日英）
  guides/                     導入・操作・制限・プライバシーポリシー
  reference/                  構成・形式・API・ビルド・機種仕様
    machines/
  development/                調査・実装・検証記録
    planning/                 現在の残作業・方針と過去計画
  analysis/pc1360k/            実験文書とソースを一体で維持
  releases/
    notes/                    バージョン別の公開説明
    checklists/               バージョン別の公開作業記録
  images/                     画像は当面維持
  licenses/                   同梱ライセンスは現位置を維持
tools/                        利用者向け補助ツールとREADME（維持）
scripts/                      開発用スクリプトとREADME（維持）
iosApp/README.md              iOS開発入口（維持）
test-data/**/README.md         テストデータの隣に維持
```

- ファイル名は原則維持し、分類によるフォルダー変更を中心にする。
- 複数用途が混在する文書は、一覧の配置先を「主たる本文の置き場所」とし、必要部分を別分類へ抽出する。
- 外部公開済みURL・アプリ内リンクを一斉に壊さない。旧パスに短い案内を残す方法と、参照更新を組み合わせる。
  特にアプリの旧配布版が開くURLは、最新コードの更新だけでは救えないため案内を残す。
- 総合目次は「はじめに／Studio／Player／補助ツール／技術資料／リリース情報／開発記録」の順。
  日英READMEの長い文書一覧は、主要ガイドと総合目次への案内へ整理する。
- 操作ガイドは対応版・OS・現在の制限を示す。履歴のテスト結果を新しい配布版の保証として扱わない。
- リリースノートは利用者向け、チェックリストは公開担当向けとして、同じリリース分類内でも入口を分ける。

## 3. 優先して内容を確認する文書

| 文書 | 観測した問題 | 次工程での扱い |
| --- | --- | --- |
| PLAYER_STUDIO_GUIDE.md | Android 3機種・iPhone PC-1245という古い対応表が残る | 同梱Playerヘルプと突き合わせて更新する |
| MACHINE_CODE_FORMATS.md | Intel HEXを未実装と記載。READMEでは入出力対応済み | 実装・テストを確認し、現行仕様へ更新する |
| machines/PC-1245.md | 冒頭が「実装前」 | 初期計画と現行機種定義を区別する |
| machines/PC-1360.md、PC-1450.md、PC-1460.md、PC-1470U.md | 実機確認待ちの記載とAndroid受入れ記録の差 | OS・日付・確認主体を区別し、対応記録と照合する |
| PUBLIC_RELEASE_CHECKLIST.md | 汎用的な名前だが内容は0.1初公開用 | 0.1の履歴と明記し、次版用チェックリストとは分ける |
| PLAYER_PROGRAM_IO.md | 現行操作と過去の実装・検証が同居 | 操作説明をガイド、段階記録を開発記録へ分離する |
| DEBUGGER.md、AI_INTEGRATION.md | 現行機能、設計、強化の経緯が同居 | 利用方法・API仕様・検証記録を読者別に分ける |
| ARCHITECTURE.md、CORE_API.md、PGP_CONCEPT.md | 初期設計・契約・構想という状態表記 | 実装済み・計画・当初の方針を区別する |
| WAV_OPERATIONS.md、WAV_S1_S2_FORMAT.md | 実装前の表現と実装後の追記が同居 | 操作はWAV_TOOL_GUIDE、仕様はreference、経過はdevelopmentへ |
| iosApp/README.md | ビルド・現行操作・PC-1245時点の段階記録が同居 | 開発入口を維持し、利用者操作の入口を独立させる |
| ROADMAP.md、TASK_REVIEW.md | 最新の方針と過去の公開・優先順位が混在 | 最新判断を先頭に置き、履歴へ明示的に案内する |

上表は一次棚卸しで見つけた例であり、未記載文書を確認不要とするものではありません。
pcwavのライセンス追加は、ユーザー判断に従い後続へ保留します。

## 4. 移動前に更新対象を追跡する箇所

| 参照元 | 依存内容 | 移動時の注意 |
| --- | --- | --- |
| desktopApp/src/main/kotlin/com/digihori/pgp/desktop/Main.kt | GETTING_STARTED、PROJECTS、DEBUGGERのGitHub URL | 現行URL更新に加え、過去配布版向けに旧パスの案内を残す。PROJECTSの`#command-history`も維持を検討 |
| androidApp/build.gradle.kts | PLAYER_HELP、PLAYER_PRIVACY_POLICY、PLAYER_STUDIO_GUIDEのコピー元 | 配置変更時にコピー元更新。アプリ内のリソース名を維持 |
| iosApp/build-shared.sh | iOSヘルプ・ポリシー・Studio紹介のコピー元 | パッケージ内の名前を維持し、コピー元だけ更新 |
| iosApp/PokecomGOPlayer/PlayerProbeView.swift | 同梱Markdownのファイル名 | ビルド側との整合を維持 |
| desktopApp/build.gradle.kts、androidApp/build.gradle.kts、iosApp/build-shared.sh | docs/licenses | ライセンスフォルダーは移動対象にしない |
| core/src/desktopTest/.../Pc1360kLocalAnalysisTest.kt、Pc1360kFastReadTest.kt | docs/analysis/pc1360k内のASM読込み・BASIC生成 | 分析資料をソースと一体で維持。移動する場合はテストも更新 |
| 日英README・全Markdown・toolsのREADME | 相対リンク、画像、見出しアンカー | 新旧パスと外部URLを区別して照合 |
| docs内のPowerShell／cmd | ソース同期・MSI調査補助 | BUILDING等からの参照を確認し、scriptsへ移動する候補 |

この表は確認した主要参照です。手順5の直前に、全Git管理ファイルで旧パスを再検索します。

## 5. Markdown以外の関連資産

- `docs/images/`：公開ガイド用の画像。維持して、移動した文書の相対参照を更新する。
- `docs/licenses/`：配布時に参照される権利情報。維持する。
- `docs/analysis/pc1360k/`のASM・BASIC・Python：解析の再現資料。文書と同じ場所に保持する。
- `scripts/sync-from-mac.ps1`、`sync-from-mac.cmd`、`test-sync-from-mac.ps1`、`inspect-msi.ps1`：
  利用者ガイドではなく開発補助。将来の候補は`scripts/`直下の同名ファイル。
- ルート`LICENSE`、`AGENTS.md`：用途を維持し、一般利用者向けガイドと混ぜない。

## 6. 今回と次工程の境界

手順1・2の124件の棚卸しを保持した上で、手順3として現行説明と履歴を分離しました。
実施内容・確認範囲・追加文書の分類は[手順3の記録](DOCUMENTATION_STEP3.md)を参照してください。
旧ファイルの配置と見出しは維持し、分離した履歴への案内を残しています。
手順4の総合目次・日英README改訂も実施しました。手順5の配置変更・参照更新も完了しました。commit／pushは未実施です。
棚卸し表の「要更新」等は手順1・2時点の観測記録です。更新結果は手順3記録で管理します。
プレリリース公開可否は、この整理とは別に配布版を対象として判断します。

## 手順4の進捗（2026-10-22）

- [日本語総合目次](../README.md)と[英語総合目次](../README.en.md)を新設しました。
  両方とも「はじめに → Studio → Player → 補助ツール → 技術資料 → リリース → 開発記録」の順に案内します。
- 日英のプロジェクトREADMEは、冒頭から総合目次・導入ガイドへ進めるようにし、末尾の文書一覧を主要ガイドへ絞りました。
- 英語目次は英語の案内です。リンク先の詳細文書が主に日本語であることを明記し、翻訳済みとは扱いません。
- AI連携を一律に将来扱いしていたREADMEの紹介を開発版の到達点へ合わせ、iPhoneの操作と開発記録の入口を分けました。
- 配布版の対応範囲は対象タグのリリースノートで確認する導線にしました。過去の公開チェックリストは開発担当の記録として分けました。
- 追加の2目次は公開入口です。将来も`docs/README.md`と`docs/README.en.md`を維持し、手順5の移動後はリンク先を更新します。
- ローカルリンク・見出しアンカー、日英目次の案内先を確認しました。ファイル移動やアプリ変更はありません。

## 手順5の進捗（2026-10-22）

本文109件を分類別フォルダーへ、Windows用補助スクリプト4件を`scripts/`へ移動しました。
公開済みMarkdownの旧URL97件には、新本文への案内と旧見出しの入口を残しています。
現在の文書・アプリ内ヘルプ・同梱処理・公開ワークフローは新パスを参照します。
[実施記録と検証範囲](DOCUMENTATION_STEP5.md)を参照してください。
以下の124件表は初回分類の記録を維持し、リンクと表示パスは新しい本文へ更新しています。
「要更新」等の当初所見の解消状況は、手順3以降の実施記録を参照してください。

## 手順6の進捗（2026-10-22）

[最終確認記録](DOCUMENTATION_STEP6.md)を作成しました。ユーザー指摘を反映して日英READMEの
Android／iPhoneの対応を14モデルへ揃え、入出力説明も現行ヘルプへ合わせました。
リンク・旧URL・同梱文書・補助ツールを確認し、文書整理としてコミット可能な状態です。
本書と手順5・6の実施記録は`docs/development/`で管理します。commit／pushは未実施です。

## 7. 全件棚卸し

各行のリンクは現在の実在ファイル、配置候補は将来のパスです。「維持」は移動しない意味です。

### 公開入口

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [README.ja.md](../../README.ja.md) | 全利用者・開発者 | `現位置を維持` | 日英README。主要操作と総合目次へ導線を絞る。 |
| [README.md](../../README.md) | 全利用者・開発者 | `現位置を維持` | 日英README。主要操作と総合目次へ導線を絞る。 |

### 権利情報

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md) | 全利用者・配布担当 | `現位置を維持` | 配布・ビルド参照を維持。pcwavのライセンス対応は保留。 |

### 利用ガイド

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [docs/guides/CHARACTER_EDITOR.md](../guides/CHARACTER_EDITOR.md) | Studio／Player利用者 | `docs/guides/CHARACTER_EDITOR.md` | 混在：操作と実装・検証・将来項目を分離。 |
| [docs/guides/DEBUGGER.md](../guides/DEBUGGER.md) | Studio／Player利用者 | `docs/guides/DEBUGGER.md` | 混在：操作と実装・検証・将来項目を分離。 |
| [docs/guides/EXTERNAL_ASSEMBLER.md](../guides/EXTERNAL_ASSEMBLER.md) | Studio／Player利用者 | `docs/guides/EXTERNAL_ASSEMBLER.md` | 混在：操作と実装・検証・将来項目を分離。 |
| [docs/guides/GETTING_STARTED.md](../guides/GETTING_STARTED.md) | Studio／Player利用者 | `docs/guides/GETTING_STARTED.md` | 現行ガイド候補。次工程で対応範囲・導線を照合。 |
| [docs/guides/PLAYER_HELP.md](../guides/PLAYER_HELP.md) | Studio／Player利用者 | `docs/guides/PLAYER_HELP.md` | 現行ガイド候補。次工程で対応範囲・導線を照合。 |
| [docs/guides/PLAYER_HELP_IOS.md](../guides/PLAYER_HELP_IOS.md) | Studio／Player利用者 | `docs/guides/PLAYER_HELP_IOS.md` | 現行ガイド候補。次工程で対応範囲・導線を照合。 |
| [docs/guides/PLAYER_PRIVACY_POLICY.md](../guides/PLAYER_PRIVACY_POLICY.md) | Studio／Player利用者 | `docs/guides/PLAYER_PRIVACY_POLICY.md` | 現行ガイド候補。次工程で対応範囲・導線を照合。 |
| [docs/guides/PLAYER_PRIVACY_POLICY_IOS.md](../guides/PLAYER_PRIVACY_POLICY_IOS.md) | Studio／Player利用者 | `docs/guides/PLAYER_PRIVACY_POLICY_IOS.md` | 現行ガイド候補。次工程で対応範囲・導線を照合。 |
| [docs/guides/PLAYER_PROGRAM_IO.md](../guides/PLAYER_PROGRAM_IO.md) | Studio／Player利用者 | `docs/guides/PLAYER_PROGRAM_IO.md` | 混在：Android操作・古い段階記録・iOS PC-1245への案内。操作と検証履歴を分離。 |
| [docs/guides/PLAYER_STUDIO_GUIDE.md](../guides/PLAYER_STUDIO_GUIDE.md) | Studio／Player利用者 | `docs/guides/PLAYER_STUDIO_GUIDE.md` | 要更新：Android 3機種・iPhone PC-1245のみという記載が現行ヘルプと不一致。アプリ同梱対象。 |
| [docs/guides/PROJECTS.md](../guides/PROJECTS.md) | Studio／Player利用者 | `docs/guides/PROJECTS.md` | 混在：操作と実装・検証・将来項目を分離。 |
| [docs/guides/SC61860_ASSEMBLY.md](../guides/SC61860_ASSEMBLY.md) | Studio／Player利用者 | `docs/guides/SC61860_ASSEMBLY.md` | 混在：操作と実装・検証・将来項目を分離。 |
| [docs/guides/WAV_TOOL_GUIDE.md](../guides/WAV_TOOL_GUIDE.md) | Studio／Player利用者 | `docs/guides/WAV_TOOL_GUIDE.md` | 現行ガイド候補。次工程で対応範囲・導線を照合。 |
| [tools/README.md](../../tools/README.md) | 補助ツール利用者 | `現位置を維持` | 各ツールと一緒に配布。tools/README.mdから案内。 |
| [tools/memory/README.md](../../tools/memory/README.md) | 補助ツール利用者 | `現位置を維持` | 各ツールと一緒に配布。tools/README.mdから案内。 |
| [tools/pc1360k-rom-dump/README.md](../../tools/pc1360k-rom-dump/README.md) | 補助ツール利用者 | `現位置を維持` | 各ツールと一緒に配布。tools/README.mdから案内。 |
| [tools/pcwav/README.md](../../tools/pcwav/README.md) | 補助ツール利用者 | `現位置を維持` | 各ツールと一緒に配布。tools/README.mdから案内。 |

### 技術資料

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [docs/reference/AI_INTEGRATION.md](../reference/AI_INTEGRATION.md) | 開発者・形式を利用する人 | `docs/reference/AI_INTEGRATION.md` | 混在：現行15公開tool・接続仕様と構想・段階別記録。利用ガイドを抽出し、本体は技術資料へ。 |
| [docs/reference/ARCHITECTURE.md](../reference/ARCHITECTURE.md) | 開発者・形式を利用する人 | `docs/reference/ARCHITECTURE.md` | 要照合：「初期設計方針」と現行構成・将来設計の切り分けが必要。 |
| [docs/reference/BASIC_TEXT_FORMAT.md](../reference/BASIC_TEXT_FORMAT.md) | 開発者・形式を利用する人 | `docs/reference/BASIC_TEXT_FORMAT.md` | 技術資料。操作ガイドへのリンクと現行仕様を確認。 |
| [docs/reference/BUILDING.md](../reference/BUILDING.md) | 開発者・形式を利用する人 | `docs/reference/BUILDING.md` | 技術資料。操作ガイドへのリンクと現行仕様を確認。 |
| [docs/reference/CORE_API.md](../reference/CORE_API.md) | 開発者・形式を利用する人 | `docs/reference/CORE_API.md` | 要照合：「Initial API contract」。実装済みAPIと初期契約案を区別。 |
| [docs/reference/GOLDEN_TEST_DATA.md](../reference/GOLDEN_TEST_DATA.md) | 開発者・形式を利用する人 | `docs/reference/GOLDEN_TEST_DATA.md` | 技術資料。操作ガイドへのリンクと現行仕様を確認。 |
| [docs/reference/MACHINE_CODE_FORMATS.md](../reference/MACHINE_CODE_FORMATS.md) | 開発者・形式を利用する人 | `docs/reference/MACHINE_CODE_FORMATS.md` | 要更新：Intel HEXを未実装と記載。READMEのWAV対応説明と照合。 |
| [docs/reference/MACHINE_FAMILIES.md](../reference/MACHINE_FAMILIES.md) | 開発者・形式を利用する人 | `docs/reference/MACHINE_FAMILIES.md` | 要照合：「初期分類」。対応機種とメモリ構成の現行表へ照合。 |
| [docs/reference/ROM_PACKAGE.md](../reference/ROM_PACKAGE.md) | 開発者・形式を利用する人 | `docs/reference/ROM_PACKAGE.md` | 技術資料。操作ガイドへのリンクと現行仕様を確認。 |
| [docs/reference/WAV_OLD_FORMAT.md](../reference/WAV_OLD_FORMAT.md) | 開発者・形式を利用する人 | `docs/reference/WAV_OLD_FORMAT.md` | 混在：仕様・当初の実装予定・確認範囲。仕様の時制を照合。 |
| [docs/reference/WAV_S1_S2_FORMAT.md](../reference/WAV_S1_S2_FORMAT.md) | 開発者・形式を利用する人 | `docs/reference/WAV_S1_S2_FORMAT.md` | 混在：冒頭の「将来実装」と実装後の追記。参照仕様と履歴を分離。 |
| [docs/reference/machines/PC-1245.md](../reference/machines/PC-1245.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1245.md` | 要更新：冒頭「実装前」。現在の機種対応と整合させる。 |
| [docs/reference/machines/PC-126x.md](../reference/machines/PC-126x.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-126x.md` | 機種定義。現行仕様と確認範囲の更新要否を精査。 |
| [docs/reference/machines/PC-1350.md](../reference/machines/PC-1350.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1350.md` | 機種定義。現行仕様と確認範囲の更新要否を精査。 |
| [docs/reference/machines/PC-1360.md](../reference/machines/PC-1360.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1360.md` | 要更新：Android実機確認待ちの記載を機種別対応記録と照合。 |
| [docs/reference/machines/PC-1360K.md](../reference/machines/PC-1360K.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1360K.md` | 混在：現行メモリ仕様・実機解析・取得手順・検証履歴。仕様と由来を分離。 |
| [docs/reference/machines/PC-1401.md](../reference/machines/PC-1401.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1401.md` | 混在：現行機種定義と調査・実装方針。仕様と経緯を分離。 |
| [docs/reference/machines/PC-1450.md](../reference/machines/PC-1450.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1450.md` | 要照合：実機未確認の記載とAndroid対応記録のユーザー確認を区別・整合。 |
| [docs/reference/machines/PC-1460.md](../reference/machines/PC-1460.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1460.md` | 要照合：実機未確認の記載とAndroid対応記録のユーザー確認を区別・整合。 |
| [docs/reference/machines/PC-1470U.md](../reference/machines/PC-1470U.md) | 機種実装者・詳細を調べる利用者 | `docs/reference/machines/PC-1470U.md` | 要照合：実機未確認の記載とAndroid対応記録のユーザー確認を区別・整合。 |
| [iosApp/README.md](../../iosApp/README.md) | iOS開発者 | `現位置を維持` | 混在：ビルド・操作・段階記録。利用者向け操作はガイドへ抽出。 |
| [test-data/README.md](../../test-data/README.md) | 開発者・検証担当 | `現位置を維持` | fixtureやサンプルと同じ場所に維持。 |
| [test-data/assembly/pc1245-hello/README.md](../../test-data/assembly/pc1245-hello/README.md) | 開発者・検証担当 | `現位置を維持` | fixtureやサンプルと同じ場所に維持。 |
| [test-data/golden/README.md](../../test-data/golden/README.md) | 開発者・検証担当 | `現位置を維持` | fixtureやサンプルと同じ場所に維持。 |
| [test-data/wav/s1-s2/README.md](../../test-data/wav/s1-s2/README.md) | 開発者・検証担当 | `現位置を維持` | fixtureやサンプルと同じ場所に維持。 |
| [test-data/wav/samples/README.md](../../test-data/wav/samples/README.md) | 開発者・検証担当 | `現位置を維持` | fixtureやサンプルと同じ場所に維持。 |

### 開発記録

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [docs/development/ANDROID_PC1251_PROGRESS.md](ANDROID_PC1251_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1251_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1261_PROGRESS.md](ANDROID_PC1261_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1261_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1350_PROGRESS.md](ANDROID_PC1350_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1350_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1360K_PROGRESS.md](ANDROID_PC1360K_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1360K_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1360_PROGRESS.md](ANDROID_PC1360_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1360_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1401_PROGRESS.md](ANDROID_PC1401_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1401_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1450_PROGRESS.md](ANDROID_PC1450_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1450_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1460_PROGRESS.md](ANDROID_PC1460_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1460_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_PC1470U_PROGRESS.md](ANDROID_PC1470U_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_PC1470U_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ANDROID_SERIES_PROGRESS.md](ANDROID_SERIES_PROGRESS.md) | 開発・検証担当 | `docs/development/ANDROID_SERIES_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/ASSEMBLER_AUDIT.md](ASSEMBLER_AUDIT.md) | 開発・検証担当 | `docs/development/ASSEMBLER_AUDIT.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/planning/DEBUGGER_FOLLOW_UP.md](planning/DEBUGGER_FOLLOW_UP.md) | 開発・計画担当 | `docs/development/planning/DEBUGGER_FOLLOW_UP.md` | 現行の残作業と以前の区切りが混在。2026-10-22時点を入口にする。 |
| [docs/development/DEBUGGER_PREVIEW_READINESS.md](DEBUGGER_PREVIEW_READINESS.md) | 開発・検証担当 | `docs/development/DEBUGGER_PREVIEW_READINESS.md` | 履歴：2026-10-14の準備記録。10-22の強化完了とは別。 |
| [docs/development/DEBUGGER_STEP1_VERIFICATION.md](DEBUGGER_STEP1_VERIFICATION.md) | 開発・検証担当 | `docs/development/DEBUGGER_STEP1_VERIFICATION.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/DEBUGGER_STEP2_VERIFICATION.md](DEBUGGER_STEP2_VERIFICATION.md) | 開発・検証担当 | `docs/development/DEBUGGER_STEP2_VERIFICATION.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/DEBUGGER_STEP3_VERIFICATION.md](DEBUGGER_STEP3_VERIFICATION.md) | 開発・検証担当 | `docs/development/DEBUGGER_STEP3_VERIFICATION.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/DEBUGGER_STEP4_VERIFICATION.md](DEBUGGER_STEP4_VERIFICATION.md) | 開発・検証担当 | `docs/development/DEBUGGER_STEP4_VERIFICATION.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/GAMEPAD_DESIGN.md](GAMEPAD_DESIGN.md) | 開発・検証担当 | `docs/development/GAMEPAD_DESIGN.md` | 混在：設計・調査・OS別実装記録。利用者向けの設定説明を別途抽出。 |
| [docs/development/IOS_HOME_PRESENTATION_FIX.md](IOS_HOME_PRESENTATION_FIX.md) | 開発・検証担当 | `docs/development/IOS_HOME_PRESENTATION_FIX.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP1.md](IOS_MACHINE_SUPPORT_STEP1.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP1.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP2.md](IOS_MACHINE_SUPPORT_STEP2.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP2.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP3.md](IOS_MACHINE_SUPPORT_STEP3.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP3.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP4.md](IOS_MACHINE_SUPPORT_STEP4.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP4.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP5.md](IOS_MACHINE_SUPPORT_STEP5.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP5.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP6.md](IOS_MACHINE_SUPPORT_STEP6.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP6.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP7.md](IOS_MACHINE_SUPPORT_STEP7.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP7.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MACHINE_SUPPORT_STEP8.md](IOS_MACHINE_SUPPORT_STEP8.md) | 開発・検証担当 | `docs/development/IOS_MACHINE_SUPPORT_STEP8.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_MODE_INDICATORS_FIX.md](IOS_MODE_INDICATORS_FIX.md) | 開発・検証担当 | `docs/development/IOS_MODE_INDICATORS_FIX.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_PICKER_STEP1.md](IOS_PICKER_STEP1.md) | 開発・検証担当 | `docs/development/IOS_PICKER_STEP1.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_PLAYER_PLAN.md](IOS_PLAYER_PLAN.md) | 開発・検証担当 | `docs/development/IOS_PLAYER_PLAN.md` | 履歴＋現況追記：PC-1245単体計画と14モデル受入れを区別。 |
| [docs/development/IOS_PLAYER_STEP2.md](IOS_PLAYER_STEP2.md) | 開発・検証担当 | `docs/development/IOS_PLAYER_STEP2.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_PLAYER_STEP3.md](IOS_PLAYER_STEP3.md) | 開発・検証担当 | `docs/development/IOS_PLAYER_STEP3.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/IOS_PLAYER_STEP4.md](IOS_PLAYER_STEP4.md) | 開発・検証担当 | `docs/development/IOS_PLAYER_STEP4.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/PGP_CONCEPT.md](PGP_CONCEPT.md) | 開発・検証担当 | `docs/development/PGP_CONCEPT.md` | 構想と現行実装が混在。製品紹介へ使う内容だけ抽出し、構想原本を保持。 |
| [docs/development/PLATFORM_FIXES_2026-10.md](PLATFORM_FIXES_2026-10.md) | 開発・検証担当 | `docs/development/PLATFORM_FIXES_2026-10.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/PLAYER_AUDIO_PLAN.md](PLAYER_AUDIO_PLAN.md) | 開発・検証担当 | `docs/development/PLAYER_AUDIO_PLAN.md` | 履歴：調査当時の案を保持済み。現在の音声動作を示すガイドにはしない。 |
| [docs/development/PLAYER_AUDIO_SPEED_PROGRESS.md](PLAYER_AUDIO_SPEED_PROGRESS.md) | 開発・検証担当 | `docs/development/PLAYER_AUDIO_SPEED_PROGRESS.md` | 混在：先頭のiOS接続済みと後段の未接続表現は記録時点が異なる。時点を明記。 |
| [docs/development/PLAYER_CPU_SPEED_PLAN.md](PLAYER_CPU_SPEED_PLAN.md) | 開発・検証担当 | `docs/development/PLAYER_CPU_SPEED_PLAN.md` | 履歴：調査当時の案。現在の速度設定とは区別。 |
| [docs/development/PORTING_NOTES.md](PORTING_NOTES.md) | 開発・検証担当 | `docs/development/PORTING_NOTES.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/planning/ROADMAP.md](planning/ROADMAP.md) | 開発・計画担当 | `docs/development/planning/ROADMAP.md` | 混在：2026-10-22の方針と過去公開の履歴。現在の方針を残し履歴を分離。 |
| [docs/development/SC61_INTEGRATION.md](SC61_INTEGRATION.md) | 開発・検証担当 | `docs/development/SC61_INTEGRATION.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/STUDIO_LOCALIZATION_INVENTORY.md](STUDIO_LOCALIZATION_INVENTORY.md) | 開発・検証担当 | `docs/development/STUDIO_LOCALIZATION_INVENTORY.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/STUDIO_LOCALIZATION_PLAN.md](STUDIO_LOCALIZATION_PLAN.md) | 開発・検証担当 | `docs/development/STUDIO_LOCALIZATION_PLAN.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/STUDIO_LOCALIZATION_VERIFICATION.md](STUDIO_LOCALIZATION_VERIFICATION.md) | 開発・検証担当 | `docs/development/STUDIO_LOCALIZATION_VERIFICATION.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/planning/TASK_REVIEW.md](planning/TASK_REVIEW.md) | 開発・計画担当 | `docs/development/planning/TASK_REVIEW.md` | 混在：現行の検討事項と過去の優先順位。最新日付と未採用案を区別。 |
| [docs/development/WAV_OPERATIONS.md](WAV_OPERATIONS.md) | 開発・検証担当 | `docs/development/WAV_OPERATIONS.md` | 混在：操作方針・実装境界・段階記録。日常手順はWAV_TOOL_GUIDEへ集約。 |
| [docs/development/WAV_REVIEW_2026-10-11.md](WAV_REVIEW_2026-10-11.md) | 開発・検証担当 | `docs/development/WAV_REVIEW_2026-10-11.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/development/WAV_S1_S2_PROGRESS.md](WAV_S1_S2_PROGRESS.md) | 開発・検証担当 | `docs/development/WAV_S1_S2_PROGRESS.md` | 段階別の計画・実装・検証記録。記録時点を保持し、現況の案内を追加。 |
| [docs/analysis/pc1360k/HIRAGANA_GLYPH.md](../analysis/pc1360k/HIRAGANA_GLYPH.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/HIRAGANA_GLYPH.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/HIRAGANA_TABLE.md](../analysis/pc1360k/HIRAGANA_TABLE.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/HIRAGANA_TABLE.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/README.md](../analysis/pc1360k/README.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/README.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READM_COMPARISON.md](../analysis/pc1360k/READM_COMPARISON.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READM_COMPARISON.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_ADDRESS_RANGE.md](../analysis/pc1360k/READ_ADDRESS_RANGE.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_ADDRESS_RANGE.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_ALL.md](../analysis/pc1360k/READ_ALL.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_ALL.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_ALL_23.md](../analysis/pc1360k/READ_ALL_23.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_ALL_23.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_BLOCK.md](../analysis/pc1360k/READ_BLOCK.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_BLOCK.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_BOUNDARY_REPEAT.md](../analysis/pc1360k/READ_BOUNDARY_REPEAT.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_BOUNDARY_REPEAT.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_CAPTURE_CHECKLIST.md](../analysis/pc1360k/READ_CAPTURE_CHECKLIST.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_CAPTURE_CHECKLIST.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_FAST_4K.md](../analysis/pc1360k/READ_FAST_4K.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_FAST_4K.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/READ_MATRIX.md](../analysis/pc1360k/READ_MATRIX.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/READ_MATRIX.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/PC-1360K_ANALYSIS.md](../analysis/pc1360k/PC-1360K_ANALYSIS.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/PC-1360K_ANALYSIS.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [docs/analysis/pc1360k/PC-1360_WIDTH_ANALYSIS.md](../analysis/pc1360k/PC-1360_WIDTH_ANALYSIS.md) | 解析・機種実装担当 | `docs/analysis/pc1360k/PC-1360_WIDTH_ANALYSIS.md` | 履歴：個別解析・実験記録。実験ソース・テストと一体で保持。 |
| [scripts/README.md](../../scripts/README.md) | 開発者 | `現位置を維持` | 開発用スクリプトの入口。 |

### リリース資料

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [docs/releases/checklists/PUBLIC_RELEASE_CHECKLIST.md](../releases/checklists/PUBLIC_RELEASE_CHECKLIST.md) | 公開担当 | `docs/releases/checklists/PUBLIC_RELEASE_CHECKLIST.md` | 履歴：0.1初公開用。今回の公開判定には再利用せず、次版用を別作成。 |
| [docs/releases/checklists/RELEASE_CHECKLIST_0.2.0-alpha.1.md](../releases/checklists/RELEASE_CHECKLIST_0.2.0-alpha.1.md) | 公開担当 | `docs/releases/checklists/RELEASE_CHECKLIST_0.2.0-alpha.1.md` | 履歴：対象版の確認・公開結果。次回用の必須条件と区別。 |
| [docs/releases/checklists/RELEASE_CHECKLIST_0.3.0-alpha.1.md](../releases/checklists/RELEASE_CHECKLIST_0.3.0-alpha.1.md) | 公開担当 | `docs/releases/checklists/RELEASE_CHECKLIST_0.3.0-alpha.1.md` | 履歴：対象版の確認・公開結果。次回用の必須条件と区別。 |
| [docs/releases/checklists/RELEASE_CHECKLIST_0.4.0-alpha.1.md](../releases/checklists/RELEASE_CHECKLIST_0.4.0-alpha.1.md) | 公開担当 | `docs/releases/checklists/RELEASE_CHECKLIST_0.4.0-alpha.1.md` | 履歴：対象版の確認・公開結果。次回用の必須条件と区別。 |
| [docs/releases/checklists/RELEASE_CHECKLIST_0.5.0-alpha.1.md](../releases/checklists/RELEASE_CHECKLIST_0.5.0-alpha.1.md) | 公開担当 | `docs/releases/checklists/RELEASE_CHECKLIST_0.5.0-alpha.1.md` | 履歴：対象版の確認・公開結果。次回用の必須条件と区別。 |
| [docs/releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.1.md](../releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.1.md) | 公開担当 | `docs/releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.1.md` | 履歴：対象版の確認・公開結果。次回用の必須条件と区別。 |
| [docs/releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.2.md](../releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.2.md) | 公開担当 | `docs/releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.2.md` | 履歴：対象版の確認・公開結果。次回用の必須条件と区別。 |
| [docs/releases/notes/RELEASE_NOTES_0.1.0-alpha.1.md](../releases/notes/RELEASE_NOTES_0.1.0-alpha.1.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.1.0-alpha.1.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |
| [docs/releases/notes/RELEASE_NOTES_0.2.0-alpha.1.md](../releases/notes/RELEASE_NOTES_0.2.0-alpha.1.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.2.0-alpha.1.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |
| [docs/releases/notes/RELEASE_NOTES_0.3.0-alpha.1.md](../releases/notes/RELEASE_NOTES_0.3.0-alpha.1.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.3.0-alpha.1.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |
| [docs/releases/notes/RELEASE_NOTES_0.4.0-alpha.1.md](../releases/notes/RELEASE_NOTES_0.4.0-alpha.1.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.4.0-alpha.1.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |
| [docs/releases/notes/RELEASE_NOTES_0.5.0-alpha.1.md](../releases/notes/RELEASE_NOTES_0.5.0-alpha.1.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.5.0-alpha.1.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |
| [docs/releases/notes/RELEASE_NOTES_0.6.0-alpha.1.md](../releases/notes/RELEASE_NOTES_0.6.0-alpha.1.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.6.0-alpha.1.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |
| [docs/releases/notes/RELEASE_NOTES_0.6.0-alpha.2.md](../releases/notes/RELEASE_NOTES_0.6.0-alpha.2.md) | 利用者 | `docs/releases/notes/RELEASE_NOTES_0.6.0-alpha.2.md` | 履歴：対象タグの説明を保持。現行機能に書き換えない。公開済みURLを保護。 |

### 運用規約

| 現在の文書 | 読者 | 配置候補 | 状態・整理方針 |
| --- | --- | --- | --- |
| [AGENTS.md](../../AGENTS.md) | 開発者・エージェント | `現位置を維持` | リポジトリ指示。利用者目次には載せない。 |
