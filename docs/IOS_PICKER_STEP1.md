# iOS PC-1245 品質向上 — 手順1: ファイルピッカー

2026-10-16。今回の依頼の第1段階。ユーザーは現在実機を用意していないため、
Simulatorで可能な範囲と注入テストを実施し、実provider・実機は未確認として残す。
ホーム・BASIC/DMP入出力・音声・外部入力は次の段階。

## 既存経路と今回の変更

既存の標準 `UIDocumentPickerViewController(forOpeningContentTypes: [.data], asCopy: true)` を使用。
複数選択は無効。コピーURLに対するsecurity scopeはI/O queue内の読込み期間だけ保持し、
`defer`で成功・失敗時とも終了する。32MiB上限の読込み・archive検証後、MainActorで
新Sessionを準備する。保存はアプリ内Libraryへatomicに行い、成功後だけSessionを差し替える。
ROM復元はLibrary内の内容を使い、元の選択URLやbookmarkへ依存しない。

今回、選択後の読込み待機中に「取込みをキャンセル」を追加した。
世代番号を更新して後着の結果を無効化し、旧Sessionと保存ROMを保持してPAUSEDへ戻る。
保存のcommit途中にはこの取消を表示しない。
I/O読込み関数を注入可能にし、遅延・失敗・古い結果を実ファイルとは独立して検証する。

取消は結果を採用しない操作であり、開始済みFileHandle/provider処理の強制中断ではない。
直列I/O queueのため、次の読込みは先行処理の終了を待つ。UIやSession操作は待たない。
pickerがコピーURLを返す前のprovider内取得待ちは別であり、アプリ内取消の検証で代替しない。

## データ分離

- Swift hostテストはUUID付きtemporary directoryにLibraryを作る。
- UI確認は今回作成した専用iPhone 12 Simulator、iOS 26.5を使用。
- DEBUGかつSimulator限定の `--picker-probe` は新しいtemporary Libraryを使用し、通常Libraryを復元しない。
  専用SimulatorのDocumentsに、独自NOP配列から生成したPGROM・短いBASIC/DMP・不正PGROMを配置する。
  この引数は専用テスト端末だけで使用する。既存データのあるSimulatorでは使用しない。
- 合成データは実行時生成。ROM・ROM由来fixtureをGitへ追加しない。
- クラウドaccountの追加・認証、通常のROM Libraryや保存設定の変更は行っていない。

## 検証

### Swift hostの自動検証

- 読込みをI/O queueで待たせたままMainActorで背景往復・取消を処理できる。
- 取消後に次の読込みを開始し、先行する成功結果が新操作のbusy状態を解除せず、置換確認も出さない。
- 次の読込みの取得失敗を表示し、既存Sessionの実行cycle・動作モードとLibraryの保存bytesを保持。
- picker取消・存在しないURLの失敗で停止維持、自動開始要求なし。
- 元ファイルを削除した後でも保存ROMを復元できる。
- 既存の破損ZIP・hash・機種不一致・atomic保存失敗・復元・置換取消・削除・入力・lifecycleテストも実施。

### 標準picker UI

- ローカルDocumentsの `.bas` / `.dmp` / `.pgrom` が選択可能。
- BAS/DMPと不正PGROMをROM取込みで選ぶと内容検証で拒否する。
  これは選択可否・失敗経路の確認であり、BASIC/DMPのプログラムロードではない。
- 有効な合成PGROMは置換確認へ進み、そこで取消できる。
- システムpickerのCancelで戻り、停止を維持する。
- picker表示中と終了後の背景往復を確認する。

## 実機・providerで残す確認

| 対象 | 状態・確認内容 |
|---|---|
| iOS 16 | runtime未導入、未検証。deployment target 16のbuild成功とは区別 |
| 実iPhoneの「このiPhone内」 | 未検証。選択・取消・長時間待機・画面復帰 |
| iCloud Drive | 未検証。取得済み/未取得、offline、取得失敗、取消、復帰 |
| Google Drive | 未検証。Driveアプリ設定とFilesでの有効化後、認証・network・取得失敗・取消・復帰 |
| 保存先・同期 | BASIC/DMPセーブ実装後の手順2で確認。保存完了とクラウド同期完了を区別 |

標準Files/provider側の通信とPlayer独自の通信を区別する。
今回Google SDK・Player独自login・cloud同期・通信処理は追加していない。
providerの設定・認証・networkによって候補表示や取得可否が変わる。

## 実行結果

- 指定JDK 21で `./gradlew --gradle-user-home .gradle-local build :core:desktopTest` 成功。
  578 tasks: 14 executed / 564 up-to-date。共有コード・Androidは変更していない。
- 既存XML結果はCore desktop 436、Android unit 43、共有Player desktop/Android各60、
  iOS Native Player 64件、failure/error 0。今回のGradleではテストはUP-TO-DATEであり、
  全件を新規実行した結果とは扱わない。
- Xcode 26.6 / 専用iPhone 12 Simulator / iOS 26.5で署名なしbuild/test成功。
  Swift host **12件**、UI **3件**、failure 0。最終結果は `iosApp/build/Picker-Step1-final.xcresult`。
- 最初のUI実行はRUNNING中のSwiftUIメニューidle待ちが続いたため中断。
  ピッカー試験は明示Pause後にメニューを開く形へ修正し、単独再実行と最終一式が成功。
  RUNNINGからpickerを開いた場合の停止・Session保持はhostテストで確認する。
  実行中メニューのUI自動化待機は、クラウド取得待機の検証結果には含めない。
- `git diff --check`成功。packageVersion 1.3.1・公開済みタグ/履歴は維持。
  commit/pushはしていない。

手順1のSimulator範囲を完了。実機/providerの項目は未確認のまま保持する。
