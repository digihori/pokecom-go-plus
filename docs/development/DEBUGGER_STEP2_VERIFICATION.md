# Debugger強化ステップ2の検証

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

2026-10-14。対象はレジスタと表示メモリの値変更強調。ステップ1の未commit変更を保持して追加した。
履歴パネル、登録管理、条件、検索／編集、ソース連携、Step Over／Out、ログポイント、設定保存は含まない。

## 比較規則

- Stepは命令実行の直前／直後を比較。Run後の自動停止、ユーザーPause、Checkpoint保存に伴うPauseは前回停止時を基準にする。次の停止後、その状態を次回の基準として保持する。
- 実行中は差分強調を非表示にする。表示更新ごとに比較基準を動かさない。
- Reset、ROM／機種／Session切替、Project／BASIC／メモリのロード、生成キーによるBASIC入力後は差分を消して基準を作る。実行状態を変更するための追加Pause／Stepは行わない。
- メモリは表示中の256byteだけを観測し、アドレス末尾で折り返す。停止中のページ変更は新しい基準を作る。実行中のページ変更は次の停止を基準とし、その停止では差分を推測しない。同じページへの表示移動・更新では直前の差分を保持する。
- ROM component／bank／offsetと物理メモリ位置を照合する。対応先が変わったセルは比較から除外し、件数を表示する。1360／1460／1470UのROM bank切替とRAMの値変化を分ける。
- 値の最終差分であり、アクセス履歴ではない。同値Write、およびRun中に変化して停止前に元へ戻った値は強調しない。

## 表示

変更したレジスタ値、メモリのHEX値と対応するASCII文字に背景色・太字・下線を付ける。
レジスタの変更前→後を表示し、メモリ行のアクセシビリティ説明にもアドレスと変更前→後を含める。
I／J／A／B等の内部RAMレジスタは個別に比較する。非表示の内部RAM／CPU項目の変化は従来の変更項目にも残す。
比較基準と強調の意味を日英で表示。識別子・数値・HEX／ASCIIの文字列・保存データは変えない。

## 自動検証

JDK 21／macOSで次を実施し、全体build／Studio tests／Core testsとgit diff --checkは成功。
追加14 tests（比較10、表示4）に失敗なし。Studioは283 tests中、既存SDL統合試験1件が条件付きskip。
Core desktopTestは既存435 testsの成功結果を再利用（UP-TO-DATE）し、新規実行とは区別する。

```sh
./gradlew --gradle-user-home .gradle-local :desktopApp:test :desktopApp:localizationPreview --args=--debugger-step2 :core:desktopTest build
git diff --check
```

- DebuggerComparisonTest: Step前後の個別レジスタ、表示メモリ／同値Write、Breakpoint／Run toでの停止基準更新、Pause、Reset／別Session、ページ切替、FFFF折返し、3機種のbank切替／RAM差分、ホストLoadの基準更新、Fault、観測がRunner revisionを変えないことを検証。
- Read監視・Traceを有効にした合成CPU実行で、比較のSnapshot読取りがアクセス履歴／Traceを消費せず、余分なRead監視イベントを生成しないことを検証。
- DebuggerValueHighlightsTest: 日英の個別レジスタ強調、隣接する未変更値、変更前→後、HEX／ASCII両方の強調、アクセシビリティ説明、実行中／新ページ／現在値と一致しない古い差分の非表示を検証。
- 既存ステップ1、Runnerの停止・再開、Step、Breakpoint、Run to、値変更／読み書き監視、Trace、Core、Checkpoint／Debug Context／MCP、日英リソース整合検査を含む。
- Core／共有UIは変更しない。Core／共有モジュールのタスクは既存のUP-TO-DATE成功結果の再利用を含む。SDLネイティブ仮想ゲームパッドの条件付きskipは本件と別。

## 表示確認と隔離

日英でStep／Run後停止／bank切替／Reset／実行中を、広幅1200×850と縮小420×700のCPU／メモリ表示で30枚独立描画した。
代表12枚を目視確認し、個別レジスタとHEXの強調、変更前→後の折返し、比較基準、bank除外件数、Reset／実行中の強調解除を確認。
ASCIIの強調は日英Semanticsテストで検証し、横スクロール先の本番実操作確認とは区別する。
出力先は `desktopApp/build/debugger-step2-preview/`。合成ROM・合成Sessionと明示Stringsを使う本番表示部品であり、
Studio main、実Preferences、ROM Library、実ROM、MCP認証、SDLは起動しない。Java prefs rootだけでの隔離は行わない。
独立描画とSemantics検証は本番OSウィンドウでの実操作・実ROM確認とは区別する。
Windows／LinuxのCI・実表示、iOSのCI・実機確認は今回未実施。

## 保護対象

Core commonMainへOS／UI／filesystem／時刻／thread APIを追加しない。型付きエラー／code、実行・停止・再開処理、
Checkpoint／Debug Context／MCP仕様、保存形式、機種IDを変更しない。実設定・ROM Library・参照プロジェクト、
作成中スキン、公開タグ・履歴を変更しない。packageVersionは1.3.1。ROM／ROM由来fixtureを追加せず、実装時点ではcommit／push未実施。今回の公開準備の集約は[準備記録](DEBUGGER_PREVIEW_READINESS.md)を参照。
