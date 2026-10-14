# Studio 日英UI — 最終検証記録

2026-10-14。ステップ1〜10の実装・自動検証と、残る実表示確認を区別する。
対象は起動時の日本語／英語選択。ユーザー追加指示により、設定でOSの設定に従う／日本語／Englishを保存し、再起動後に反映する機能も実装。実行中切替・Player全面対応は含まない。

## 結果

- 文言移行・自動検証を完了し、2026-10-14にユーザーから「一通り確認しました」と受入れ報告を得た。今回の実装を完了とする。ユーザー確認のOS・個別操作の内訳は未指定であり、以下の自動検証・独立描画と区別する。OS言語設定を実変更する起動試験、Windows／Linuxの個別結果は未記録。
- JDK 21／macOSで `./gradlew --gradle-user-home .gradle-local :desktopApp:test :core:desktopTest build` 成功。ローカライズ関連10 suite／36 tests、日英870 keyの一致・重複・引数検査に失敗なし。
- 固定keyの文言取得785箇所を静的確認。未定義keyと引数数の不一致なし。条件分岐・enum由来の動的keyは対応featureのテストと描画で確認しており、この785箇所には含めない。
- 独立JVMにuser.language=ja／en／fr／zhを渡し、起動時のStudioStrings.systemが日本語／英語／英語／英語を選ぶことを確認。ドットエディターの見出し、1.25、C000、既定Locale不変を確認。これはOS言語設定を実変更した検証とは別。
- desktopApp.jarに日英Messages.propertiesが同梱され、ソースリソースとバイト一致。テスト用Fallback.propertiesは含まれない。DMG／MSI／DEBのインストール後確認は未実施。
- JAR確認後に設定／ROMセット説明を更新し、全体buildを再実行。最終JARも日英リソースの一致を再確認。

## 表示・互換性の確認範囲

| 対象 | macOSでの独立Compose描画 | 自動検証・原文維持 | 本番Mac／Windows／Linux実操作 |
| --- | --- | --- | --- |
| 表示言語の設定 | 実選択部品の日英600×480／420×500、折り返し・再起動案内 | system／ja／en保存・復元、未知値、既存key保持、保存失敗復元、独立JVM起動12組、OnClick後も現在言語保持 | 未確認。実Preferencesの保存と本番再起動を含む |
| メニュー・メイン・設定・About／Help | 代表状態・日英・縮小・ダイアログ。メニュー画像はCompose DropdownMenu | 速度／入力保存名、パス、コマンド例、既定Locale | 未確認。ネイティブMenuBarを含む |
| ROM管理／Create ROM Set | 本番の行・ファイル入力部品と確認見本 | ROM package項目とバイト、family、型付き検証エラー | 未確認。実ROM Libraryを使わない |
| Project／Workspace／Build & Load | 本番の初期ソース選択・出力部品と代表状態 | 合成Projectのmanifest・初期ソース・DMP／Listing／Map一致 | 未確認。ツリー操作・外部変更・実ロードを含む |
| 外部Assembler／エディター | 説明・実選択部品・縮小。操作行を折り返す | 外部生ログ・コマンド・パスを維持。既知アプリ診断を表示変換 | 未確認。外部ツールとの接続操作は追加検証から起動しない |
| BASIC／Memory／Quick Assemble | 通知・診断・実範囲入力部品 | BASICバイト・ソースの小数点、DMP／Assembly出力・HEX一致 | 未確認。ROM入力と実メモリ保存を含む |
| Debugger | 本番CPU／Memory／Disassembly／Trace／Watch／Map。範囲・件数ラベルは狭いダイアログで縦配置 | 全停止理由の従来英語Checkpoint表現とJSON、構造化Debug Context JSON一致 | 未確認。Breakpoint／Watch／保存・出力操作を含む |
| WAV Tool | 初期／入力・実波形部品・確認見本 | WAV・Program・Raw・report・mapping保持、診断code・座標保持 | 未確認。解析／タブ／Raw連動／zoom／pan／保存を含む |
| ドットエディター | 本番グリッド・入力エラー・縮小 | バイト・DATA／DB・書式ID・失敗時pattern保持 | 未確認。タップ／ドラッグ／適用操作を含む |
| ゲームパッド・共有UI | 本番ControllerSettingsに合成機器・押下／割当／利用不可を注入 | 設定key／bindings／キー刻印保持、Androidの日本語既定一致 | 未確認。接続／切断／割当／保存・Android実機を含む |
| MCP | 本番McpServerPanelに非認証用見本・request・権限エラーを注入 | tool一覧／schema／JSON応答／コピーTOML一致 | 未確認。起動／停止／コピー・権限エラーの発生操作を含む |

すべての描画画像で文字・ボタン・スクロールの全面合格を確認したわけではない。ステップ8で記録した一部文字欠けは未解決。ステップ10でもスクロール後のダイアログ画像で見出し等の欠けを確認した。初回フレームだけの保存ではダイアログの出現アニメーション途中となり、解決しなかった。既存の2フレーム生成に戻し、本番の文言や数値を画像の欠けに合わせて変更していない。

GUIツールはJavaアプリを一覧に出したが、bundle IDでの選択がInvalid appとなったため本番の操作確認には使えなかった。実設定やROM Libraryを使う本番Appを検証のために起動していない。

## 最終の文言・レイアウト修正

- 日本語のCharacter Editorはユーザー指定により「ドットエディター」。英語表示・内部class名・書式IDは維持。
- 設定のOSエディター選択を「OSの設定に従う」に変更。「診断位置」はソースファイルとエラー／警告の該当行を開く説明に変更。
- 外部アセンブラー設定の冒頭に、現時点ではsc61に特化した実装であることを明記。
- 入力設定はメイン画面下部で論理入力／物理入力を切り替えられるという説明に変更。
- ROMセット作成の入力は「バイナリー形式のROMダンプ」と明記。日英で意味を揃えた。
- Debug Contextの入力ペアは狭い領域では縦配置。本文を縦スクロール可能にし、アドレス／件数の解析・出力処理は維持。
- 外部Assemblerの選択肢はRadioButtonと説明を組にして折り返し、操作説明を付与。Workspaceなどの操作行も折り返す。
- DebuggerのWatchはユーザーとの用語見直しにより、日本語表示を「監視」へ統一。見出し・操作・範囲説明・状態表示も揃え、英語と内部key・動作は維持。
- 原文維持: 製品・機種・キー刻印、CPU／レジスター、INSTRUMENTテーマ名、著作権、MCP tool、JSON／schema／設定key、URL／パス／token／時刻・method、ユーザーデータ、ソース・予約語・HEX、外部ログ、WAV解析レポート。

## 追加指示：表示言語の設定

設定の選択を自動保存し、次回起動で反映する。既存の設定にuiLanguage=system／ja／enを追加し、現在のプロセスではStudioStrings.systemを切り替えない。OS言語と上書きの組合せを独立JVMで検証し、実設定へ触れずメモリー内Preferencesで保存と失敗時の復元を確認。LanguageSettingsPanelのOnClickを呼んだ後の選択・保存・再起動案内と現在言語保持も検証した。これは本番GUI／実Preferencesでの保存・再起動確認とは別。

## 受入れと後続の確認

2026-10-14、ユーザーの一通りの確認とcommit／push指示を受領。言語設定（再起動反映）と用語修正を含む今回分は完了。以下は記録を補完する後続確認であり、今回の受入れを妨げない。独立画像の欠けは本番UIの不具合が確認されたものとして扱わない。

## 保護対象と後続確認

- 本番Macで全画面の実操作・メニュー・縮小・スクロール・アクセシビリティを確認する。OS言語変更試験とWindows／Linuxの結果を別記録にする。
- 画像の文字欠けは本番表示と独立ImageComposeSceneを比較して発生条件を調べる。未確認を合格にしない。
- 実操作試験に実設定・ROM Libraryを使わない分離方法を先に確認する。macOSでJava prefs root指定だけでは分離できなかった既往を踏まえ、本番Appの起動だけで安全に分離したとは扱わない。
- 既存1470Uキー修正・関連テスト／文書、スキン6枚を保護。CoreにLocale／OS／UI依存を追加していない。ROM／ROM由来fixtureを追加していない。
- packageVersionは1.3.1。ユーザー指示によりローカライズ変更をcommit／pushする。既存1470U差分と作成中スキンは対象外。公開済みタグ・履歴は変更しない。

詳細なステップ別記録は[作業整理](STUDIO_LOCALIZATION_PLAN.md)、用語・分類は[棚卸し](STUDIO_LOCALIZATION_INVENTORY.md)。
