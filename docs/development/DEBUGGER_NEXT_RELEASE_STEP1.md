# Debugger強化：既存機能の受入れ

確認日：2026-10-30。main `49b16a4`を起点とするDebugger強化ブランチで確認しました。
作業1の自動検証と修正記録です。配布アプリ・実ROM・各OSの手動受入れは未完了です。

## 自動検証

JDK 21.0.11、macOSで以下が成功しました。

```sh
./gradlew :core:desktopTest :desktopApp:test build --console=plain
git diff --check
```

初回の変更前buildも成功。修正後の最終実行は増分buildで、未変更タスクは
up-to-dateまたはキャッシュを再利用しています。

| 対象 | テスト数 | 失敗／エラー | skip |
| --- | ---: | ---: | ---: |
| Core desktopTest | 500 | 0 | 0 |
| Studio test | 422 | 0 | 2 |

Studioのskipはローカル実ROMを使う`Pc1245HelloWorldTest.localFirmwareRunsBasicCallDrawsAndReturns`と、
ネイティブSDL条件付きの`SdlGamepadBackendTest.virtualGamepadExercisesNativeDetectionButtonsAndDisconnect`です。
これらの動作確認成功を示すものではありません。

## 確認範囲

| 項目 | 自動検証の主な根拠 | 残る受入れ |
| --- | --- | --- |
| 実行制御・停止理由・位置移動 | `DebuggerControlsTest`、`DesktopEmulatorRunnerTest` | 配布アプリで実ROMの停止・再開・Step・Run To |
| 条件・ヒット回数・登録管理 | `DebuggerBreakpointConditionTest`、`DebuggerRegistrationsTest`、`DebuggerRegistrationPanelTest` | 実操作による編集・無効化・Reset／Load |
| 監視・差分・履歴・bank | `DebuggerHistoryTest`、`DebuggerHistoryPanelTest`、`DebuggerValueHighlightsTest`、`DebuggerControlsTest` | 実ROMのアクセス停止とbank切替、複数監視の併用 |
| Step Over／Out | `DebuggerSteppingTest`、日英の`DebuggerControlsTest` | 実ROMで追跡可能／追跡不能な経路の表示 |
| ソース連携 | `DesktopDebugSourceMapTest`、日英の`DebuggerControlsTest` | 外部エディタへの実際のファイル・行移動 |
| 保存・復元 | `DebuggerSettingsTest`、`DebuggerSettingsPanelTest` | ファイル選択・キャンセル・対象違いを含む実操作 |
| focus・入力漏れ | `DesktopKeyboardInputTest`、`EmulatorTextInputTest` | 配布アプリのタブ・入力欄・ダイアログをまたぐ操作 |
| MCP | `DebugChangeServiceTest`の認証付きHTTP経路、`DesktopDebugContextServiceTest` | 外部AIクライアントとStudio承認UIを通した登録・Trace・Step |

隔離したCompose描画とSemantics操作は配布アプリの操作とは区別します。
MCPの自動HTTP接続も、外部AIクライアントの設定・接続確認とは区別します。

## 見つかった不具合と修正

BASIC単独またはメモリ単独のロードを拒否した場合にも、Runnerがロード済みソース対応と
観測済みCALLフレームを破棄していました。一括ロードでは成功時だけ破棄するため、
ロード方法によって失敗後のデバッグ継続可否が異なっていました。

単独ロードも成功時だけソース対応・CALLフレーム・監視の基準値を更新するよう修正しました。
要求世代の更新や一時実行操作の中断は維持します。ロード失敗前の自動実行を
そのまま再現するという意味ではありません。

追加した`DebuggerSteppingTest.failedLoadsPreserveObservedFramesAndSourceMetadata`で、
合成プログラムのCALLを観測してから不正なBASIC／ROM宛メモリロードを試し、
失敗後も同じソース対応オブジェクト・PC・Step Out可能状態が残り、元の呼出しから
復帰できることを検証します。一括ロードの拒否も同じテストで確認します。
修正前はソース対応の同一性検証で失敗し、修正後は全経路が成功しました。

## 配布アプリでの確認手順

各OSの結果にはアプリ版・機種・RAM構成を記録し、実設定に影響しない検証用Projectと
保存ファイルを使います。まずPC-1245の自作サンプルを使い、bank確認は対応機種で行います。

1. サンプルをBuild & Loadし、Breakpointで停止、Continue、Pause、Step、Run Toを試す。
   停止理由・PC・移動先が一致し、移動操作だけでは命令を実行しないことを確認する。
2. 値条件・回数条件を登録し、編集・無効化・再有効化・Reset・再Loadを試す。
   複数監視で停止し、値の差分と命令位置／アクセス位置を確認する。
3. CALL内外でStep Over／Outを試し、途中Breakpointで中断する。
   未対応経路や追跡不能が、正常復帰として表示されないことを確認する。
4. ソース行から外部エディタを開く。ソース編集後は古いリンクを使えず、
   再Build & Loadで新しい行へ移動できることを確認する。
5. 設定を保存し、変更後に復元する。対象違い・破損ファイル・キャンセルでは
   元の設定が残ることを確認する。拒否されるロード後もソース対応とStep Outを確認する。
6. Debuggerの入力欄・タブ・ダイアログで文字入力、Tab移動、Enter、キャンセルを試す。
   入力中はエミュレータへキーが漏れず、入力を終えた後に通常操作へ戻ることを確認する。
7. 外部AIクライアントから登録・Trace・1命令Stepを提案してStudioで承認する。
   Reset／Load／機種切替後の古い要求が拒否され、取得操作ではCPUが進まないことを確認する。

作業1の手動受入れを完了扱いにせず、結果を追記してから次段階の判定に使います。
