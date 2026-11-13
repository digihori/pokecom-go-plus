# AI連携拡充：中断・再開メモ

更新日：2026-11-12。署名済みmacOS arm64候補とStable復帰試験は合格。
公開準備は保留。PC-1245の実用BASICでCopilotによる解析・部分修正とStudioでの実行を確認済み。
連続するBASIC INPUTの一括入力を修正し、Studio実行処理と実ROMで検証済み。PC-1251 CALLの途中停止後にキー間隔を確保する修正も実ROMで検証済み。PC-1251のBASIC解析取り込みを実装し、Copilotで取得と分岐解析を確認済み。
[次の作業と区切り](AI_TECHNICAL_PREVIEW_NEXT.md)を参照。

**最新の結果は[Technical Preview受入れ記録](AI_TECHNICAL_PREVIEW_READINESS.md)を参照。**
PC-1245でCopilotのBASIC解析・生成を確認。PC-1251のCALL途中停止後の現象を再現し、
試験対応の制限として残した。公開は未実施。
以下の実装・再現情報は中断時の記録として残す。
再開時は本書を起点にし、追加の機種展開を自動的に開始しない。

## 方針と現在地

対象を特定のClaude製品に限定せず、MCP対応AIクライアントへ広げる。
開発はCodex中心、Copilot等で相互運用を検証する。Claudeの契約を前提にしない。
インストール済みStudioのMCPがポケコン固有の情報を提供し、解析と新規生成を支える。

PC-1245の解析・生成の基本経路を実装し、PC-1251にも知識・生成・上限実行・LCD取得を展開済み。
公開MCPツールは24件。第6段階全体や全機種対応の完了ではない。

- 同梱stdioブリッジ、設定コピー、短期認証、Studioでの承認を実装。
- PC-1245：知識、選択した解析ファイルの公開、Project／Source生成、Build、Load、上限付きRUN／CALL・入力、LCD取得。
- PC-1251：知識、Project／Source生成、Build、Load、上限付きRUN／CALL・入力、120列LCD取得。解析ファイル公開はUTF-8のBASIC・アセンブリ、標準RAM範囲のDMP・生の機械語に対応。
- 両機種のnumeric-labels作例を同梱。PC-1251知識catalogVersionはpc1251-v3。
- 実ROMを通常起動し、両機種で1→ONE、2→TWO、3→OTHER、0→通常プロンプト、その後の数値応答を確認。
- Copilotでは接続・解析情報・LCD取得等を確認済み。新規チャットでの生成全工程の独立受入れは未完了。

Codexからの配布アプリstdioプローブ＋Studio UI操作と、CodexネイティブMCP接続・
外部AIによる独立受入れ・実機確認は区別する。

## PC-1251 CALLの再試行

途中停止から再試行する際のキー解放の間隔を修正済み。修正後のStudio実行処理と実ROMで、
追加のContinueを挟まない再試行のRAM書込みを確認した。再現条件と検証手順は
[機種展開検証記録](AI_MACHINE_ROLLOUT_VERIFICATION.md)を参照する。
修正版の配布アプリでの再確認は受入れ時に行う。

## 配布方針の追加合意（2026-11-02）

正式版をStable、AI連携の先行版をTechnical Previewとして並行配布し、
小さな機能単位で更新する。検証した変更を順次Stableへ取り込む。
初回候補はPC-1245を主対象・PC-1251を試験対応とし、Copilotで解析・生成の
全工程受入れを終えた時点を区切りにする。
プレビューでも配布物の基本確認と互換性確認を行う。
採番・同時インストール・設定分離・更新経路は配布準備時に検討する。
詳細は[全体計画の配布方針](AI_MCP_EXPANSION_PLAN.md#9-stabletechnical-previewの配布方針2026-11-02合意)。
まだRelease公開やタグ作成は行っていない。実装作業の中断は継続する。

## その後の順序

1. PC-1245／PC-1251を当面のAI対応対象として仕上げる。PC-1251のBASIC解析取り込みは検証済み。
2. Copilotの新規チャットから公開手順だけで解析・生成の全工程を受け入れ確認する。
3. 他OSの配布物・開発環境を持たないインストール環境で確認する。
4. 他機種は別の大きな後続フェーズとして扱う。系列ごとの代表機種で試し、各機種の受入れを行う。

実機、複数ROM版、他AI、他OSの確認を、機能実装済みという理由で完了扱いにしない。

## 機種展開の暫定見積もり

利用者へ説明した規模感。実作業時間の粗い目安であり、納期の確約や正式見積もりではない。
機種別の棚卸し・ROM利用可否・代表機種での実測後に更新する。

| 作業 | 暫定目安 |
| --- | --- |
| 近い同系列の1機種 | 1〜3日程度 |
| BASIC・LCD・メモリ構造が異なる系列の最初の1機種 | 3〜7日程度 |
| ROM依存問題、バンク、日本語表示等の調査 | 追加で数日以上 |
| 全対応機種を同水準へ展開 | 数週間〜数か月規模 |

知識・作例・解析／生成API・実ROMでの基本課題・文書を含む目安。
共通処理の整理も必要で、同系列でも各機種の実行確認を省略できない。
実機、複数ROM版、複数AI、他OSは別枠。全機種を一括の完成条件にしない。

## 検証と作業ツリー

最終確認：JDK 21で以下が成功。デスクトップ525件、failure 0、error 0、skip 3。

```sh
./gradlew --gradle-user-home .gradle-local build :core:desktopTest :desktopApp:verifyDistributionRuntime
```

配布アプリ内のPC-1251作例BASIC・同梱解説と元ファイルの一致、文書リンク、git diff --checkも確認。
その後は文書のみ変更している。

中断時点の実装・文書・作例は、作業ブランチ`codex/ai-mcp-expansion`へまとめて保存する。
利用者の指示によりcommit・originへpushする。mainへはマージしない。
再開時は同ブランチを選び、git statusと最新commitを確認する。CALLの未解決事項は残したままの開発途中の記録である。
ROM画像は追加しない。共有記録へ個人用パス・一時認証・privateな識別子を持ち込まない。

Studioは終了済み、一時MCP認証ファイルは削除確認済み。
一時検証Project・プローブはOSの一時領域に置いたため、再開に必須としない。
再現に必要なBASICはtest-data/basic/ai/pc1251-numeric-labels、CALLコードは本書に保存した。

## 関連文書

- [全体計画](AI_MCP_EXPANSION_PLAN.md)
- [接続検証](AI_MCP_CONNECTION_VERIFICATION.md)
- [PC-1245解析検証](AI_PROGRAM_ANALYSIS_VERIFICATION.md)
- [PC-1245生成検証](AI_PROGRAM_GENERATION_VERIFICATION.md)
- [PC-1251展開・実ROM観測](AI_MACHINE_ROLLOUT_VERIFICATION.md)
- [生成操作ガイド](../guides/AI_PROGRAM_GENERATION.md)
