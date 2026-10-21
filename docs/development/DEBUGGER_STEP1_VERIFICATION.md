# Debugger強化ステップ1の検証

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

2026-10-14。対象は実行操作、停止情報、停止位置への移動と配置の改善。
ソース連携、Step Over／Out、履歴パネル、登録管理、条件、検索／編集、ログポイント、設定保存は含まない。

## 実装

- 上部にRun／Pause／Step、実行状態、PC／CURRENT_PC、停止理由を日英で表示。本文と独立した領域とし、低いウィンドウでは上部自身もスクロール可能。
- 実行操作は既存のrunEmulator／pauseEmulator／stepEmulatorを共用。Core／Runnerの実行処理・停止理由型・再開条件は変更しない。
- Breakpoint／Run to／監視／Faultの命令位置へ移動。監視では最初の変更／アクセス先のメモリへ移動。移動時は実行しない。同じ位置へ繰り返し移動できる。
- 幅1000dp以上はCPU／逆アセンブル等とメモリ／監視の左右配置。それ以下はタブ切替。入力と左右のスクロール状態は同じ画面内で保持し、機種／Sessionの切替では表示状態を分離する。ファイルやPreferencesへ保存しない。
- 指定アドレス表示でもPCと一致する逆アセンブル行だけを強調。Runからの自動停止時はCPUを即時取得し、メモリ内容もCPU更新／移動時に再取得する。
- CheckpointのPause動作、Debug Context／MCPの読み取り専用境界・tool名・JSON、言語設定の再起動反映、数値形式は維持。

## 自動検証

JDK 21／macOSで次を実施。

```sh
./gradlew --gradle-user-home .gradle-local :desktopApp:test :desktopApp:localizationPreview --args=--debugger-step1 :core:desktopTest build
git diff --check
```

全体build／Studio tests／Core testsとgit diff --checkは成功。追加9 testsに失敗なし。
Studioは269 tests中1件（SDL仮想ゲームパッドのネイティブ統合試験）が既存の条件でskip。
Core desktopTestの既存結果は435 tests、失敗・skipなしで、今回のタスクはUP-TO-DATE。

追加テストはDebuggerControlsTestで、停止理由と移動先、日英の実行操作、Fault／Sessionなしの無効化、
狭幅の移動先タブ選択／広幅の両側表示、指定アドレスの現在PC強調、タブ切替後の入力・追従選択保持、
同一アドレスのメモリ再取得、合成CPUプログラムによるRead／Write監視の停止・再開と観測の併用を確認する。
既存Runnerの停止・再開、Step、Breakpoint通過、Run to、値変更監視、同値Write監視、Traceと、
CoreのCPU／Bus、Checkpoint／Debug Context／MCP、ローカライズ整合検査も対象に含む。
Core／共有UIに変更はなく、全体buildには既存のUP-TO-DATE結果の再利用を含む。

## 表示と隔離

- 本番と同じDebuggerControls／DebuggerLayoutと各表示パネルを、合成Sessionと明示した日英Stringsで描画。
- 日英各6状態（1200×800の広幅、420×650の縮小・メモリ・Fault・実行中、640×400の低い画面）を独立Compose描画し、配置、折返し、タブ、操作無効化、数値と現在PC行を確認。
- プレビューはStudio mainを起動しない。実Preferences、ROM Library、実ROM、MCP認証、SDLを使用しない。Java prefs root指定だけで隔離したとは扱わない。
- 本番のOSウィンドウでのマウス操作、リサイズ、キーボードfocus、ファイル選択、アクセシビリティ、実ROM動作は未確認。独立描画／Semantics操作テストと本番実操作を区別する。
- Windows／LinuxのCI・実表示、iOSのCI・実機確認は今回未実施。既存commitの5ジョブ成功とは別の検証結果として扱う。

## 保護対象

Core commonMainへOS／UI／filesystem／時刻／thread APIを追加していない。Coreの型付きエラー／code、
保存形式、機種ID、ソース・命令・HEX、MCPプロトコルを変更していない。日本語は「監視」を使用する。
実設定・ROM Library・参照プロジェクト、作成中スキン、公開タグ・履歴を変更しない。
packageVersionは1.3.1。ROM／ROM由来fixtureを追加せず、実装時点ではcommit／push未実施。今回の公開準備の集約は[準備記録](DEBUGGER_PREVIEW_READINESS.md)を参照。
