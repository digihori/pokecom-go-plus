# AI連携拡充：中断・再開メモ

更新日：2026-11-11。初回Technical Previewの機能受入れまで確認し、公開準備の前で区切る。

**最新の結果は[Technical Preview受入れ記録](AI_TECHNICAL_PREVIEW_READINESS.md)を参照。**
PC-1245でCopilotのBASIC解析・生成を確認。PC-1251のCALL途中停止後の現象を再現し、
試験対応の制限として残した。次はPreviewの配布手順と署名済みDMGでの確認。
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
- PC-1251：知識、Project／Source生成、Build、Load、上限付きRUN／CALL・入力、120列LCD取得。解析ファイル公開は未対応。
- 両機種のnumeric-labels作例を同梱。PC-1251知識catalogVersionはpc1251-v3。
- 実ROMを通常起動し、両機種で1→ONE、2→TWO、3→OTHER、0→通常プロンプト、その後の数値応答を確認。
- Copilotでは接続・解析情報・LCD取得等を確認済み。新規チャットでの生成全工程の独立受入れは未完了。

Codexからの配布アプリstdioプローブ＋Studio UI操作と、CodexネイティブMCP接続・
外部AIによる独立受入れ・実機確認は区別する。

## 再開時の最初の作業：PC-1251 CALLの再現性調査

詳細な入力、観測数値、PNGは[機種展開検証記録](AI_MACHINE_ROLLOUT_VERIFICATION.md)の
「PC-1251実ROMの配布アプリ検証」を参照する。

1. PC-1251を登録済みROMで通常起動し、BASICプロンプトを確認する。
2. 専用の検証Projectを用意し、下記ルーチンをBuild・承認付きLoadする。
3. 通常状態からのCALLと、入力途中の上限停止後のCALLを分けて観測する。
4. キー押下・解放・間隔とROMの入力受理を照合し、原因を特定する。
   停止直後のキー解放をROMが観測するまでの時間も調査候補だが、現時点では仮説。
5. 原因を修正する場合は、意味のある回帰テストと実ROM再検証を追加する。

```asm
ORG 0xC500
LIDP 0xC100
LIA 0x5A
STD
RTN
```

観測済みの事実：

- CALL 50432はCLEARから始まる12キー列。200000命令／500000サイクルでは途中で停止し、inputComplete=false、表示はCALL 504、RAM値は00。
- 表示確認後の新しいCLEAR始まり要求を400000命令／1000000サイクルで実行すると、inputComplete=trueで「>」へ戻ったが、RAM値は00だった。原因は未特定。
- 続くWRITE Watch付きCALLではSTD直後にPC=0xC506、RAM[0xC100]=0x5Aを確認。
- RTNの1命令StepでROM内0x51CCへ戻った。Watch削除後の上限付きContinueでも0x5Aを保持し、「>」へ戻った。

したがって、最初の要求で値を確認できなかった原因を「ROMが値を消した」と断定しない。
inputCompleteは送信スケジュールの完了だけで、入力受理・CALL成功・プログラム終了を保証しない。
未送信キーは停止時に破棄される。表示を確認せず残りを再送しない。
PC強制移動や直接RAM書込みで成功を作らず、通常のROM入力経路で検証する。

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

1. PC-1245／PC-1251を当面のAI対応対象として仕上げる。PC-1251の解析取り込みも残る。
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
