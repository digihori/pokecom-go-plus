# Debugger強化ステップ3の検証

2026-10-14。既存の読み書き／bank履歴をUIで表示・絞込み・参照する範囲。
ステップ1・2の未commit変更を保持した。Core／Runnerの収集処理、MCP仕様、登録管理、条件、ソース連携、
Step Over／Out、編集、ログポイント、設定保存を本件へ追加しない。

## 実装と制約

- 広幅では「実行 / メモリ」と「履歴」、縮小時は実行・メモリ・履歴のタブ。上部の実行操作・停止情報は維持する。
- 履歴の中で読み書き／bankを選択。読み書きは命令PC、R／W、アクセス先、値、イベント連番。bankは旧／新bank、セレクタ値、イベント連番を新しい順に表示。
- Read／Writeとアクセス先HEX範囲、旧・新bankのいずれかに一致する10進番号で表示を絞り込む。範囲の空欄は制限なし。不正な入力はエラーとし、全件表示へ戻さない。絞込み欄と詳細説明は開閉できる。
- フィルターと選択タブは同じ画面内で保持。機種／Session変更では既存のkey(runner)境界で分離。Preferencesやファイルへ保存しない。
- 記録状態、保持数／表示数／4096件上限、保持連番、古いイベントの破棄を表示。Resetは既存どおり両履歴を消去。閉じて再び開いた場合は既存の保持データを表示する。
- 読み書き履歴は既存のDebugger観測有効時のみ記録し、命令フェッチも含む。内部RAM／ポートI/Oは網羅しない。bank履歴は1360／1460／1470UでDebuggerの開閉と独立して収集。非bank機種は非対応表示。
- 命令／メモリへの移動は停止中／Fault時だけ可能。過去のメモリ／bankの復元や書込みは行わない。bankイベントは命令PC・Trace共通の命令番号を持たないため、特定の命令へ対応付けない。現在のROM窓への移動はMachine Definitionを使う。
- Runnerのリスト取得だけを使い、Coreイベントを追加でdrainしない。履歴タブ表示中はCPU表示更新・状態変化・手動更新で取得し、最速Runの各tickでコピーしない。行はLazyColumnで遅延描画し、新着時に先頭へ戻る。
- UIだけの絞込みは記録・監視・Trace・Debug Context／MCP出力へ影響しない。MCP tool名・プロトコル・JSON schemaを変更しない。

## 自動検証

JDK 21／macOSで全体build・Studio tests・Core testsとgit diff --checkが成功。
追加11 tests（履歴7、表示4）に失敗なし。Studioは294 tests中、既存SDL統合試験1件が条件付きskip。
Core desktopTestは既存435 testsの成功結果を再利用（UP-TO-DATE）し、新規実行とは区別する。

```sh
./gradlew --gradle-user-home .gradle-local :desktopApp:test :desktopApp:localizationPreview --args=--debugger-step3 :core:desktopTest build
git diff --check
```

追加テスト:

- DebuggerHistoryTest（7件）: 範囲の空欄／既存HEX構文／不正入力、アクセス先とR／Wの絞込み、同値Write、並び順、bank両側の絞込み、非bank／Sessionなし。
- 合成CPUの実行で読み書き監視の停止・再開、Traceと履歴の共有、4096件保持／古いイベントの破棄、Reset、Debugger観測の無効／再有効化を確認。
- 1360／1460／1470Uで観測無効のまとめ実行を使い、bank履歴の常時収集・4096件上限・Resetを確認。
- 履歴取得と絞込みの前後でRunner revision／Trace／元の履歴が不変、同じDebug Context serviceのJSONがbyte一致することを確認。
- DebuggerHistoryPanelTest（4件）: 日英の絞込み・不正入力・記録アドレスへの移動、bank番号・現在窓への移動、実行中の移動無効化、非bank表示。広幅／縮小時のタブ移動とフィルター保持をSemantics操作で確認。
- 既存ステップ1・2、Runnerの停止・再開／Step／Breakpoint／Run to／監視、Core、Checkpoint／Debug Context／MCP、日英リソース整合検査を含む。

Core／共有UIに変更はない。既存のCore／共有モジュールのUP-TO-DATE成功結果の再利用は、新しい実行結果と区別する。
SDLネイティブ仮想ゲームパッドの条件付きskipは本件と別。

## 描画と環境

日英で読み書き、bank、絞込み欄、実行中、空履歴、非bankの6状態を広幅1200×900／縮小420×800で24枚独立描画。
代表12枚を目視確認し、タブ選択、列と数値、縮小時の折返し、範囲ラベル、保持・破棄表示、移動操作の無効化を確認した。
出力先は `desktopApp/build/debugger-step3-preview/`。本番DebuggerLayout／Controls／HistoryPanelに見本データを注入する。
各シーンの仮想描画時刻を単調に進め、タブの選択アニメーションが完了してから保存する。

Studio main、実Preferences、ROM Library、実ROM、MCPサーバー／認証、SDLを起動しない。Java prefs rootだけによる隔離は行わない。
独立描画／Semantics検証と、本番ウィンドウでのマウス・キーボードfocus・スクロール・リサイズ・アクセシビリティ・実ROM確認は区別する。
Windows／LinuxのCI・実表示、iOSのCI・実機確認は本件では未実施。

## 保護対象

既存未commit変更、実設定・ROM Library、参照プロジェクト、作成中スキン、公開タグ・履歴を保護。
Core commonMainのOS／UI／filesystem／時刻／thread API、型付きエラー／code、実行・停止・再開、保存形式、機種ID、
命令・予約語・識別子・HEX、外部ログ、MCP仕様を変更しない。日本語の「監視」を維持。
packageVersionは1.3.1。ROM／ROM由来fixtureを追加せず、実装時点ではcommit／push未実施。今回の公開準備の集約は[準備記録](DEBUGGER_PREVIEW_READINESS.md)を参照。
