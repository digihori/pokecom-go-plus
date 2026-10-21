# ドキュメント整理・手順3の実施記録

2026-10-22。対象は手順1・2で作成した[棚卸し・配置案](DOCUMENTATION_ORGANIZATION.md)。
現行の操作・仕様と、当時の計画・検証結果を区別する作業を行いました。
既存の配置は維持し、手順4の総合目次・日英README改訂と手順5の移動は未実施です。

## 現行情報への訂正

| 文書 | 内容 | 照合先 |
| --- | --- | --- |
| PLAYER_STUDIO_GUIDE | Android・iPhone版の14モデルを記載。外部SC61だけに限定した紹介を修正 | PLAYER_HELP、PLAYER_HELP_IOS、PlayerMachineCatalog、iOS手順8 |
| MACHINE_CODE_FORMATS | Intel HEXのWAV Tool入出力対応とrecordの制約を記載。Project・Playerへの対応と区別 | DesktopWavBinaryFiles、WAV_TOOL_GUIDE |
| machines/PC-1245 | 実装前という状態表記を訂正。初期選定方針は当時の記録として維持 | 導入・Playerヘルプと既存公開記録 |
| machines/PC-1360、PC-1450、PC-1460、PC-1470U | Androidの後日ユーザー確認を反映。Core単独の検証・未確認範囲と区別 | 各ANDROID_*_PROGRESS |
| MACHINE_FAMILIES | PC-1360Kの実装保留という古い説明を訂正。世代分類と対応機種一覧を区別 | PC-1360K、Playerヘルプ |
| SC61860_ASSEMBLY、PROJECTS | Debuggerソース連携・実行操作を一律に将来扱いしていた記述を訂正 | DEBUGGER |
| TASK_REVIEW、ROADMAP | 現在の作業は公開前の文書整理と記載。過去の優先順位を新しい指示に見せない | 今回のユーザー指定・既存の到達点 |

この作業は実機テストの追加ではありません。ユーザーの受入れ報告、合成テスト、OS／端末／ROM版別の
未確認事項は、元の記録の区分を維持しています。プライバシーポリシーの適用状態やpcwavのライセンス方針は変更していません。

## 説明と履歴を分離した文書

既存文書の節見出しは案内として残し、過去の本文を以下へ分離しました。
既存のファイルや見出しを参照するリンクを維持し、元の証拠を削除しないためです。

| 現行の説明 | 分離先 | 分離先の分類・将来の配置候補 |
| --- | --- | --- |
| [Player入出力](../guides/PLAYER_PROGRAM_IO.md) | [画面・入出力実装履歴](PLAYER_PROGRAM_IO_HISTORY.md) | 開発記録：docs/development/PLAYER_PROGRAM_IO_HISTORY.md |
| [Debugger](../guides/DEBUGGER.md) | [初期配置・強化候補・検証履歴](DEBUGGER_DESIGN_HISTORY.md) | 開発記録：docs/development/DEBUGGER_DESIGN_HISTORY.md |
| [AI連携仕様](../reference/AI_INTEGRATION.md) | [構想・実装順・検証履歴](AI_INTEGRATION_HISTORY.md) | 開発記録：docs/development/AI_INTEGRATION_HISTORY.md |
| [Character Editor](../guides/CHARACTER_EDITOR.md) | [責務・将来項目・テスト](CHARACTER_EDITOR_DESIGN.md) | 開発記録：docs/development/CHARACTER_EDITOR_DESIGN.md |
| [外部Assembler](../guides/EXTERNAL_ASSEMBLER.md) | [検証・受入れ](EXTERNAL_ASSEMBLER_VERIFICATION.md) | 開発記録：docs/development/EXTERNAL_ASSEMBLER_VERIFICATION.md |
| [WAV操作方針](WAV_OPERATIONS.md) | [実装・確認履歴](WAV_OPERATIONS_HISTORY.md) | 開発記録：docs/development/WAV_OPERATIONS_HISTORY.md |
| [S1／S2参照仕様](../reference/WAV_S1_S2_FORMAT.md) | [fixture・実装順履歴](WAV_S1_S2_HISTORY.md) | 開発記録：docs/development/WAV_S1_S2_HISTORY.md |
| [PC-1360K機種資料](../reference/machines/PC-1360K.md) | [調査・回収・検証履歴](../analysis/pc1360k/PC-1360K_HISTORY.md) | 開発記録：docs/analysis/pc1360k/PC-1360K_HISTORY.md |
| [PC-1401機種資料](../reference/machines/PC-1401.md) | [旧判断・統合・検証履歴](PC-1401_HISTORY.md) | 開発記録：docs/development/PC-1401_HISTORY.md |

現行のAI接続手順は[AI_CONNECTION_GUIDE](../guides/AI_CONNECTION_GUIDE.md)として独立させました。
分類は利用ガイド、将来の配置候補は`docs/guides/AI_CONNECTION_GUIDE.md`です。
本書は開発記録、将来の配置候補は`docs/development/DOCUMENTATION_STEP3.md`です。
手順1・2の全124件一覧は当時の棚卸しとして維持し、追加文書の分類は本節で補完します。

## 初期設計・段階記録の扱い

- ARCHITECTURE、CORE_API、PGP_CONCEPTは設計・契約案・構想であることを明記しました。
  未照合のAPI例を現行の公開契約に見せ替えず、Coreの実装ソースと現行ガイドへ案内します。
  現行アーキテクチャの全面的な書き直しは別の作業です。
- 過去7版のリリースノートと7件の公開チェックリストは、対象タグ・公開時点の資料と明記しました。
  未チェック項目の一括完了や、次回用チェックリストへの流用は行いません。
- Android／iOS対応、Debugger各段階、ローカライズ、音声・速度、WAV進捗、外部SC61調査等は、
  日付別の段階記録として明記しました。後日受入れ追記と当時の検証件数・未確認事項を読み分けられる形にしました。
- iosApp/READMEは開発用入口として維持し、現在の操作はiPhone同梱ヘルプ、全体受入れは手順8へ案内します。

## 検証と次工程

- 変更・追加95文書のローカルリンク・参照先見出しを確認し、新たなリンク切れはありません。
- `git diff --check`で差分の書式を確認しました。
- Androidに同梱するStudio紹介の更新を含め、JDK 21で`./gradlew build`を実行し成功しました。
  多くの既存テストはUP-TO-DATEであり、新規の実機・GUI受入れを行ったという意味ではありません。
- 手順4では総合目次と日英READMEの入口を整理します。新設したAI接続ガイドもここから案内します。
- 手順5の配置変更では、棚卸しの配置案に本書の追加文書を加え、アプリ・同梱・旧URLの参照を更新します。

commit／pushは今回の手順3には含めていません。
