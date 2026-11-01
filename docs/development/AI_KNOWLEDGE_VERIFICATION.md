# PC-1245機種知識API初版の実装・検証

実施日：2026-11-01。[拡充計画](AI_MCP_EXPANSION_PLAN.md)第3段階の初版。
実装対象は現在の作業ツリー。公開リリースの対応を意味しない。

## 実装範囲

- `pgp_search_knowledge`、`pgp_get_knowledge`、`pgp_get_example`を追加し、公開ツールは18件。
- PC-1245の解説・作例6項目と、SC61860のmnemonic別メタデータ135項目（計141項目）。
- BASICコード表154件をCore方言定義から生成。表示記号・文字・キーワードを区別。
- メモリ既定配置、BASICポインター、LCD情報、キーレイアウト情報はCore／既存機種仕様サービスを共用。
- HELLO WORLDのmanifest、BASIC、Assemblyの3ファイルを既存fixtureからビルド時に同梱。
- UIのツール一覧とCodex設定のenabled_toolsは、実際のtools/listと共通の一覧から生成。

出典と検証限界は[知識カタログ](../reference/knowledge/README.md)、
引数・応答・上限は[API仕様](../reference/AI_INTEGRATION.md#21-pc-1245機種知識api初版2026-11-01)を参照。

## 自動検証

JDK 21で`./gradlew build :core:desktopTest :desktopApp:verifyDistributionRuntime`を実施。
Desktopは488件、失敗0、skip 3件。Core・他モジュールのチェックと配布ランタイム検証も成功。

追加テストでは次を確認した。

- Session providerへ触れずに検索・取得できること。
- 日本語・大小文字・topicによる検索、ページングの重複や抜け、該当なし。
- 全文取得時のCore由来コード表、同名命令の全opcode、encodableと未提供情報。
- 同梱記事の参照先が存在し、実機未検証とROM適用限界を返すこと。
- 作例本文が既存fixtureと一致し、内蔵assemblerでAssemblyが成功すること。
- 未知キー、null、型・上限違反、未対応機種、未知ID・パス形式入力の拒否。
- 新ツールの読み取り専用属性、コピーするCodex設定への掲載、構造化応答とtext応答の一致。
- 既存HTTP・stdioの18ツール公開と回帰テスト。

## 配布アプリの実接続検証

macOS 26.6.2 / Apple Siliconで配布アプリを再生成し、StudioのMCPサーバーを起動。
リポジトリ外の空の一時ディレクトリを作業ディレクトリとして、同梱実行ファイルを
`--mcp-stdio`で起動した。テスト用プローブから次を呼び出した。

| 確認 | 結果 |
| --- | --- |
| initialize・tools/list | 成功、18ツール |
| 「表示」をdisplay topicで検索→項目取得 | 同じIDを取得し、LCD 80列のデータを確認 |
| BASIC知識取得 | 行番号上限999、コード表154件 |
| LIA命令メタデータ取得 | 成功、Coreのmnemonicを確認 |
| HELLO WORLD作例取得 | 3ファイル、createsFiles=false |
| 未対応機種問い合わせ | UNSUPPORTED_KNOWLEDGE_MACHINE |
| 一覧をlimit=1で取得 | total=141、nextOffset=1 |
| 標準出力・終了 | JSON-RPC応答のみ、stderr空、終了code 0 |
| 作業ディレクトリ | ファイル作成なし |

これは配布resourceとMCP経路の検証であり、Codexネイティブ接続や別AIでの解析・生成課題の
受入れを完了したことにはしない。Sessionなしの取得条件は自動テストで確認した。
実接続時のStudioは登録済みROMで起動していたが、知識APIはそのROMを参照しない。
検証後はMCPサーバーを停止し、一時トークンを失効させた。

## 残る範囲

- BASICの完全な文法・引数・エラー条件、各命令の完全な動作・フラグ・cycle数。
- ROM表示・キー入力ルーチンABI、任意ROM版でのワーク領域・レジスタ保存保証、実機検証。
- PC-1245以外の知識と、複数AIによる知識検索を使った共通課題の受入れ。

未提供情報をAIの推測で確定仕様にしない。次は実利用課題で不足情報を特定し、出典と検証を加えて
知識を拡充する。新規Project作成・キー操作・LCD画像取得等は後続段階として維持する。
