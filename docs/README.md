# ドキュメント

[English](README.en.md) | [日本語](README.md) | [プロジェクトの紹介](../README.ja.md)

この目次は開発中のソースツリーの文書を案内します。配布版の対応機能・制限は、
利用する版の[リリースノート](#リリース情報)を確認してください。
詳細文書は主に日本語です。技術資料・開発記録は後半に分けています。
本文は分類別フォルダーへ移動しました。旧パスのファイルは公開済みURLを維持する案内です。
新しいリンクや更新には、この目次から開く本文を使ってください。

## はじめに

Playerの初回設定は[日本語ガイド](https://digihori.github.io/pokecom-go-plus/player/ja.html)／[English setup guide](https://digihori.github.io/pokecom-go-plus/player/en.html)を参照してください。
公開ページのソースと更新方法は[公開ページの説明](player/README.md)にあります。

1. [公開済みの配布物](https://github.com/digihori/pokecom-go-plus/releases)から対象OSの版を選びます。
2. Studioでは[はじめに](guides/GETTING_STARTED.md)の順に、起動・機種選択・ROM登録を進めます。
3. Playerでは[Android](guides/PLAYER_HELP.md)または[iPhone](guides/PLAYER_HELP_IOS.md)のヘルプを確認します。

ROMは同梱していません。自分で適法に入手したROMが必要です。

| 目的 | 読むもの |
| --- | --- |
| Studioの初回起動・ROM登録・BASIC実行 | [はじめに](guides/GETTING_STARTED.md) |
| 起動・ROM登録時のエラーを調べる | [よくあるエラー](guides/GETTING_STARTED.md#7-よくあるエラー) |
| 自分でビルドする | [ソースからのビルド](reference/BUILDING.md) |
| StudioとPlayerの役割を知る | [Studioについて](guides/PLAYER_STUDIO_GUIDE.md) |

## Studio

| やりたいこと | ガイド |
| --- | --- |
| Project作成・外部エディタ・Build & Load・入力履歴 | [Projectガイド](guides/PROJECTS.md) |
| Assembly Workspace・Quick Assemble・命令とデータ定義 | [SC61860 Assembler](guides/SC61860_ASSEMBLY.md) |
| SC61・YASM61860等を接続する | [外部アセンブラ](guides/EXTERNAL_ASSEMBLER.md) |
| 実行停止・Breakpoint・Watch・Trace・ソース連携 | [Debugger](guides/DEBUGGER.md)／[English](guides/DEBUGGER.en.md) |
| Debugger強化の実施結果・保留・手動テスト | [受入れ記録](development/DEBUGGER_RELEASE_ACCEPTANCE.md)／[テストケース](development/DEBUGGER_MANUAL_TEST_CASES.md) |
| 外部AIクライアントを接続する | [AI接続ガイド](guides/AI_CONNECTION_GUIDE.md) |
| 実機転送用WAVの作成・録音WAVの解析と復元 | [WAV Tool](guides/WAV_TOOL_GUIDE.md) |
| ドットパターンを作成する | [Character Editor](guides/CHARACTER_EDITOR.md) |

## Player

| 対象 | 操作・制限 |
| --- | --- |
| Android | [同梱ヘルプ](guides/PLAYER_HELP.md)、[BASIC／DMP入出力の詳細](guides/PLAYER_PROGRAM_IO.md) |
| iPhone | [同梱ヘルプ](guides/PLAYER_HELP_IOS.md)（機種、ROM、Files、音声、外部入力） |
| プライバシー | [Androidポリシー](guides/PLAYER_PRIVACY_POLICY.md)、[iPhoneポリシー（日本語）](guides/PLAYER_PRIVACY_POLICY_IOS.md)・[英語](guides/PLAYER_PRIVACY_POLICY_IOS.en.md) |

iPhone版の開発・ビルド状況は[開発用README](../iosApp/README.md)を参照してください。
開発中の機種対応と、署名付き配布の完了は別に確認します。
ポリシーの適用状態は原文と各配布版の説明を確認してください。

## 補助ツール・ROM吸い出し

任意の単独ツールです。実行環境は利用者が用意します。StudioのUIからは呼び出しません。

- [ツール一覧と必要な環境](../tools/README.md)
- [生バイナリー／DMP変換・Intel HEX出力・情報表示](../tools/memory/README.md)
- [pcwav：BASIC／バイナリーとWAVの変換](../tools/pcwav/README.md)
- [PC-1360Kの漢字ROM吸い出し](../tools/pc1360k-rom-dump/README.md)

現在同梱している実機吸い出しプログラムはPC-1360Kの漢字ROM専用です。
通常ROMや他機種の汎用吸い出しツールは今後の対象です。

## 技術資料

操作より詳しい形式・機種仕様・開発情報を調べるときに使います。

| 内容 | 資料 |
| --- | --- |
| BASICテキストの文字・エスケープ表記 | [BASIC Text Format](reference/BASIC_TEXT_FORMAT.md) |
| アドレス付きマシン語データ | [DMP／Intel HEXとRaw Binaryの方針](reference/MACHINE_CODE_FORMATS.md) |
| ROMセット | [pgrom形式](reference/ROM_PACKAGE.md) |
| WAVの仕様と境界 | [OLD](reference/WAV_OLD_FORMAT.md)、[S1／S2](reference/WAV_S1_S2_FORMAT.md)、[操作方針](development/WAV_OPERATIONS.md) |
| AI APIの引数・承認条件・制限 | [AI連携仕様](reference/AI_INTEGRATION.md) |
| 機種の世代分類 | [SC61860 Machine Families](reference/MACHINE_FAMILIES.md) |
| ビルド・自動検証 | [BUILDING](reference/BUILDING.md)、[Golden Test Data](reference/GOLDEN_TEST_DATA.md)、[fixture一覧](../test-data/README.md) |
| 開発用の生成スクリプト | [scripts](../scripts/README.md) |

機種別資料：
[PC-1245](reference/machines/PC-1245.md)／[PC-126x](reference/machines/PC-126x.md)／
[PC-1350](reference/machines/PC-1350.md)／[PC-1360](reference/machines/PC-1360.md)／[PC-1360K](reference/machines/PC-1360K.md)／
[PC-1401系](reference/machines/PC-1401.md)／[PC-1450](reference/machines/PC-1450.md)／
[PC-1460](reference/machines/PC-1460.md)／[PC-1470U](reference/machines/PC-1470U.md)。
[PC-1245／125x／126xのRAM容量・ミラー仕様](reference/machines/RAM_1245_1261.md)。
機種仕様・実装履歴の区別と、資料内の確認範囲を読んでください。

## 解析の読み物

- [PC-1360K：読みを漢字に変える2KiBの仕事](reference/machines/PC-1360K_KANJI_RUNTIME.md)（解析途中の読み物）
- [PC-1360Kのワークメモリ解析記録](analysis/pc1360k/KANJI_WORK_MEMORY.md)（実験条件・観測・仮説）

## リリース情報

[公開済みリリースとダウンロード](https://github.com/digihori/pokecom-go-plus/releases)。
以下は各タグの説明で、現在の開発版の機能一覧ではありません。

| 対象タグ | リリースノート |
| --- | --- |
| v1.0.0 | [Release notes](releases/notes/RELEASE_NOTES_1.0.0.md) · [Checklist](releases/checklists/RELEASE_CHECKLIST_1.0.0.md) |
| v0.7.0-alpha.1 | [リリースノート](releases/notes/RELEASE_NOTES_0.7.0-alpha.1.md) · [公開チェックリスト](releases/checklists/RELEASE_CHECKLIST_0.7.0-alpha.1.md) |
| v0.6.0-alpha.2 | [Technical Preview 0.6 alpha 2](releases/notes/RELEASE_NOTES_0.6.0-alpha.2.md) |
| v0.6.0-alpha.1 | [Technical Preview 0.6](releases/notes/RELEASE_NOTES_0.6.0-alpha.1.md) |
| v0.5.0-alpha.1 | [Technical Preview 0.5](releases/notes/RELEASE_NOTES_0.5.0-alpha.1.md) |
| v0.4.0-alpha.1 | [Technical Preview 0.4](releases/notes/RELEASE_NOTES_0.4.0-alpha.1.md) |
| v0.3.0-alpha.1 | [Technical Preview 0.3](releases/notes/RELEASE_NOTES_0.3.0-alpha.1.md) |
| v0.2.0-alpha.1 | [Technical Preview 0.2](releases/notes/RELEASE_NOTES_0.2.0-alpha.1.md) |
| v0.1.0-alpha.1 | [Technical Preview 0.1](releases/notes/RELEASE_NOTES_0.1.0-alpha.1.md) |

権利情報：[LICENSE](../LICENSE)、[第三者ソフトウェア通知](../THIRD_PARTY_NOTICES.md)。

## 開発記録・初期設計

以下は開発者・調査担当向けです。過去の「未実装」「確認待ち」やテスト件数は記録時点の内容で、
現在の配布版の制限や動作保証を示すものではありません。

- 現在の方針・残作業：[正式リリースのMUST／WANT](development/planning/FORMAL_RELEASE_TASKS.md)、[Roadmap](development/planning/ROADMAP.md)、[検討用一覧](development/planning/TASK_REVIEW.md)、[Debuggerの後続](development/planning/DEBUGGER_FOLLOW_UP.md)
- 初期構想・設計案：[プロジェクト構想](development/PGP_CONCEPT.md)、[Architecture](reference/ARCHITECTURE.md)、[Core API初期契約案](reference/CORE_API.md)
- 由来と判断：[移植記録](development/PORTING_NOTES.md)
- 分離した履歴：[Player入出力](development/PLAYER_PROGRAM_IO_HISTORY.md)、[Debugger](development/DEBUGGER_DESIGN_HISTORY.md)、[AI連携](development/AI_INTEGRATION_HISTORY.md)、[外部Assembler](development/EXTERNAL_ASSEMBLER_VERIFICATION.md)、[Character Editor](development/CHARACTER_EDITOR_DESIGN.md)
- WAVの経緯：[操作・実装](development/WAV_OPERATIONS_HISTORY.md)、[S1／S2 fixture・実装順](development/WAV_S1_S2_HISTORY.md)、[対応進捗](development/WAV_S1_S2_PROGRESS.md)
- 機種解析：[PC-1360Kの調査・検証](analysis/pc1360k/PC-1360K_HISTORY.md)、[個別実験](analysis/pc1360k/README.md)、[PC-1401の調査・実装](development/PC-1401_HISTORY.md)
- モバイルの受入れ入口：[Androidシリーズ](development/ANDROID_SERIES_PROGRESS.md)、[iOS全機種](development/IOS_MACHINE_SUPPORT_STEP8.md)
- 公開担当向けの過去チェックリスト：[0.1](releases/checklists/PUBLIC_RELEASE_CHECKLIST.md)、[0.2](releases/checklists/RELEASE_CHECKLIST_0.2.0-alpha.1.md)、[0.3](releases/checklists/RELEASE_CHECKLIST_0.3.0-alpha.1.md)、[0.4](releases/checklists/RELEASE_CHECKLIST_0.4.0-alpha.1.md)、[0.5](releases/checklists/RELEASE_CHECKLIST_0.5.0-alpha.1.md)、[0.6](releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.1.md)、[0.6 alpha 2](releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.2.md)
- 全文書の索引：[124件の棚卸し・配置案](development/DOCUMENTATION_ORGANIZATION.md)、[手順3で追加・分離した文書](development/DOCUMENTATION_STEP3.md)

段階別のAndroid／iOS・ローカライズ・検証資料などは棚卸しの全件一覧から参照できます。
