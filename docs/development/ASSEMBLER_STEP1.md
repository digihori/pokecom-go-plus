# Assembler整備 第1段階

2026-10-31。対象は残項目の整理、処理共通化、Quick Assemble結果画面。

## 今回の完了条件と実装

- Quick／Workspace／Buildが同じ設定済みコンパイラを利用する。
- Quickの内蔵ソースヘッダ処理と診断行番号をWorkspaceに揃える。
- 結果のMemory／Disassembly／Listing／Symbol Map描画テキストを共有する。
- Quickの成功結果を保存前に表示し、明示操作でDMPを保存できる。
- 保存取消後も結果を残す。保存成功はパス、失敗は理由を表示する。
- アセンブル失敗時は診断・外部ログを表示し、ソースを開ける。
- Quickの結果・保存状態はProjectのBuild／Load状態と独立して保持する。

## 既に対応済みの項目

命令・境界の修正、式／EQU／DW／文字列DB／DS／ALIGN、配置・ミラー・BASIC衝突の検証、
ロード失敗時の非変更、内蔵／SC61のロード済みソース表示は既存実装。
古い監査の未実装記述を現在の残項目として再利用しない。

## 後続の順序

1. 複数診断・列・コード・関連定義位置。
2. byteを出さない行のListing、Symbolから定義・アドレスへの移動。
3. 外部ツールの位置情報とWindows／Linuxの詳細受入れ。
4. ソース／SymbolからのBreakpoint登録、entry指定。
5. INCLUDE・共有定義・INCBINと循環検出。
6. 条件アセンブル・マクロ・ローカルラベル・CASE支援。bank成果物／ROMパッチは別案件。

今回、内蔵構文の追加、複数診断、外部Listing parserの追加は含めない。
SC61の既存位置情報は維持し、YASM／汎用で提供されない情報は推測しない。

## 検証

自動テストではQuick／Workspace／Buildのbyte列一致、Listing／Symbol一致、
ヘッダ付きソースの診断行番号、明示保存前のファイル非生成を検証する。
外部実行は共通コンパイラの既存adapter／Runnerテストで回帰確認する。
結果画面・保存ダイアログ・取消の実画面検証およびWindows／Linuxでの手動確認は未実施。

JDK 21で`./gradlew --gradle-user-home .gradle-local build :core:desktopTest`成功。
Studio 471件（既存skip 1）、Core desktop 500件で失敗なし。
追加したQuick／Workspace／Build一致テストも成功。`git diff --check`成功。
