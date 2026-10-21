# iOS PC-1245 品質向上 — 手順2

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

2026-10-17。ホーム・階層メニューとBASIC/DMP入出力を対象とする。
[手順1](IOS_PICKER_STEP1.md)の変更を保持。機種追加・音声・速度・外部入力は含めない。
ユーザーは現在実機を用意していないため、Simulatorと自動テストを先行する。

## 操作

- 通常起動は「Pokecom GO Player」ホーム。PC-1245の登録・プレイボタンを表示。
- ホームのメニューは「設定」「ヘルプ・情報」。設定の順序はROM管理、CPU速度、サウンド、
  コントローラ設定。後3項目は後続段階として無効表示し、設定やデバイス機能はまだ追加しない。
- PlayerタイトルバーはPC-1245、RUNNING/PAUSED/FAULTED、RUN/PROボタン、3点メニュー。
  状態表示タップでCPU Run/Pause。メニューにも「エミュレータ操作」→Run/Pause/Resetを維持。
- PC-1245は現行Core/AndroidともRUN/PROのみ。今回ユーザーがこれに揃える方針を選択した。
  RSVをCoreへ追加せず、メニューにも表示しない。RUN/PROはCPU状態と独立。
- 「プログラム」→「ロード」「セーブ」。「表示」→通常/Controller Display。「ホームへ戻る」。
- 案内画面はPlayerとは、Studioについて、プライバシーポリシー、バージョン・ライセンス。
  上部に戻る操作を置く。原文をbuild時にコピーして同梱し、実行時取得はしない。
  iOS固有の範囲・Files・ROM保存・未接続機能は専用原文に明記する。
  プライバシーは公開前の原文案として表示し、公開・適用開始を確定したものではない。

## Sessionと実行意図

初回/新Sessionのプレイ入場はCPUを自動実行。ホームでROMを復元・登録してもCPUは進めない。
同じSessionで実行中からホームへ戻った場合は再入場で再開し、明示Pause後は停止を維持する。
ホーム往復はRAM・モード・表示を保持し、セーブステートは追加しない。
通常の背景復帰は停止・全キー解放。セーブ中の通常背景移行はセーブ後の自動再開意図も取り消す。
ROM管理の取消・失敗後も停止を維持する。新ROM登録/復元成功は次のプレイ入場で実行する。

ロードは拡張子bas/dmp自動判定、2MiB上限、コピーURLをsecurity scope内で読込む。
読込み・純粋parserは専用queue、Sessionへの再検証/一括適用はMainActor。
成功後はCPU自動再開し、BASICのRUN文字列は入力しない。取消・失敗ではメモリを変更せず停止。
C000を含むRAMのDMPは受け付けるため、BASIC管理領域を壊し得る。ROM/LCD/制御領域・ミラー重複を拒否する。

セーブはBASIC全体、または両端を含む単一RAM範囲。MainActorで共有exportを呼びコピーsnapshotを取得し、
専用queueで固有temporary directoryへ書く。標準export picker(asCopy)で利用者の保存先へコピーする。
他アプリへの共有は追加しない。開始前の実行意図を同じSessionへ一度だけ戻す。
明示Pause、Session変更、通常背景移行では再開しない。エラー表示終了/取消も開始前の意図に従う。
保存完了はクラウド同期完了を意味しない。外部書込みのエラー表示・再試行はシステムpickerにも依存する。

非同期操作はSession identityとgenerationで保護。閉じる/Reset/ROM操作で旧結果を無効化し、
旧snapshotは新Sessionを変更しない。閉じる操作で一時ファイルを破棄する。プロセス終了後の未完了操作は復元しない。
型付きPlayerProgramErrorはFacadeでも維持し、Swift UI側で利用者向け文言へ変換する。
ファイル拡張子・容量・アドレス等のhost境界エラーも型を持たせる。

メニューの安定性のため、UIへ表示していないcycle数だけの変更ではView更新を発行せず、
LCDはrevision/enabled/symbolsが変わった時に更新する。cycle値自体は毎tick記録する。
Native skin viewもHostを監視し、LCD自体が変わらない表示モード・キー状態・動作モード変更を反映する。
新Session/ResetではLCDを明示的に更新し、Session間で同じrevisionでも表示を再利用しない。

## 検証と区分

- 開発確認は専用iPhone 12 Simulator / iOS 26.5 / Xcode 26.6。通常Libraryを使わない合成probeを使用。
- 最終scriptは使い捨てiPhone Simulatorを作り、終了時に作成した端末だけを削除する。
  UI probeのDocuments・temporary Library・設定は既存Simulatorから分離する。
- Native境界: BASIC/DMP往復、C000、RAM以外/ミラー重複の全体拒否、snapshotコピー独立、
  実行中操作拒否、型付きparse/exportエラー、PC-1245の未対応モード拒否。
- Swift host: ホーム往復と背景の区別、BASICセーブ/再ロード、DMP失敗時の内容保持、
  実行/停止からのセーブ終了・取消・失敗、背景時の再開取消、古いread結果の破棄、2MiB超過。
- UI: 標準pickerによるローカル読込み/保存、ホーム往復、CPU状態、階層メニュー、情報、回転。
- iOS 16 runtime、実機、実ROMのNEW/ロード/BEEP/ゲーム、iCloud/Google Driveの実providerは未検証。
  Simulatorの合成確認を実機受入れや全クラウド確認に置き換えない。
- LCDドットはiOSの既存80%のまま。88%適用判断は実機視認性の確認後。

## 実行結果

- 指定JDK 21で全体 `build :core:desktopTest` 成功。最終Gradleは578 tasks、25 executed / 553 up-to-date。
- Core desktop 436、Android unit 43、共有Player desktop/Android各60、共有iOS Native Player 67件、failure/error 0。
  Nativeの追加3件は明示的に実行して成功。変更のないCore/Android等のGradleテストはUP-TO-DATEを含み、全件の新規実行とは扱わない。
- `Step2-Final-verified.xcresult`: 使い捨てiPhone 17 Pro相当/iOS 26.5でSwift host 17件成功。
  Controller Display両横向きとRun/Pause/Reset/背景復帰のUIも成功。
- `Step2-Picker-complete.xcresult`: ローカルROMピッカー選択/取消/失敗/復帰のUI成功。
- `Step2-Export-final.xcresult`: ホーム実行意図、BASIC/DMPの標準Filesへのセーブ→実ファイル再ロード、
  セーブ取消後のRUNNING、明示Pause後の保存でPAUSED、情報4画面と上部戻るのUI成功。
- 最終的にSwift host 17件・UI **計4件**が成功。UIは不具合修正後の対象別再実行を含む集計で、
  1回の全suite成功を示すものではない。各結果bundleのケース結果を区別する。
- 保存pickerは開始フォルダを強制せず標準の初期表示を使用し、出力URLごとにcontrollerを作る。
  保存ボタンの有効化を確認して操作し、保存完了表示と実ファイルの再ロードまで検証した。
- 初回UIの表示切替で、LCD更新抑制後にNative skin viewへモード変更が届かない問題を検出。
  `ObservedObject`でHostを監視するよう修正し、両横向きの拡大・キー無効化をUIで再確認した。
- iOS 26の標準pickerは不可視の旧Cancel AX要素も公開する。テストは実際のナビゲーション/画像と
  一致するローカル階層の戻る/閉じる操作を使い、取消後のアプリ表示と実行意図も確認する。
  この取消自動化は、Documents→このiPhone内→場所というテスト用経路と、iOS 26.5の
  確認済みナビゲーション位置に限定する。別OS/providerの取消まで確認済みとは扱わない。
  単なる要素存在を操作成功と扱わない。
- build時の同梱本文・LICENSE・第三者通知/原文ファイルがapp内に存在することを確認。
- `git diff --check`、shell script構文確認成功。

手順2のSimulator/合成範囲を完了。実機・実ROM・最低OS・実providerの受入れは後続として保持する。

## Gitと今回の境界

第1段階の未commit差分を保持。PC-1350スキンの差分はユーザーによるゴミ除去であり、
次回のcommit/更新指示時に一緒に含める。こちらでは画像を変更しない。
packageVersionは1.3.1を維持。公開済みタグ・履歴を変更せず、commit/pushは明示指示後に行う。
Core commonMainへOS/UI/filesystem/時刻/thread APIを追加しない。参照プロジェクト依存・ROM fixtureも追加しない。
