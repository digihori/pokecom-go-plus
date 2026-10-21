# Android PC-1450 Player 対応記録

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-20
状態: 実装・自動検証・エミュレータ確認を経て、2026-10-20にユーザーから一通りの実機動作確認で問題なしとの報告を受領。今回の対応は完了。
シリーズ内の機種切り替えは保留。以下の未実施・確認待ちの記述は当時の検証段階の履歴であり、現在の受入れ状況は冒頭と表を優先する。
個別の周辺機器・ROM全版・全機能を網羅した確認結果の報告とは区別する。

## 受入れ区分

| 区分 | 状況 |
|---|---|
| 実装済み | Studio由来の81キー（主キー63＋関数18）、左SHIFT刻印のLOCKと右SHIFTの区別、外部キー追加、初期CAL状態 |
| 自動検証済み | 共有Playerのキーとゲームパッド、LCD配線・18シンボル、S1語彙・カナ・BASIC保存／再ロード、DMPのRAM境界と失敗時の無変更、速度・音声・停止復帰の5テスト（スキン配置を含む）が成功。全体チェックは後続 |
| エミュレータ確認済み | 通常／拡大LCDと18シンボル、81キー押下・解放、隔離pgrom登録・復元、実ROMのBASICロード→RUNでPC1450表示、標準保存ピッカーと保存テキスト照合、DMPロード、通常背景復帰でPAUSED・held/queued=0 |
| 実機確認済み | 2026-10-20、ユーザーより一通りの動作で問題なしとの報告。今回の対応を完了 |

## PC-1401との差分と再利用

- CPUは256,000 cycles/s、buzzer・モード入力queue・S1行構造は既存Coreを使用。
  自動キー保持40,000 cycles、解放25,600 cycles。
- ROMは内部8 KiB（0000–1FFF）、外部32 KiB（8000–FFFF）。既存の64 KiB flat importerはRAMを捨てる。
- RAMは固定16 KiB（2000–5FFF）。6000–7FFFは互換制御窓。DMPロードはRAMだけに許可する。
- BASIC初期2030、start/end pointer 5F01／5F03、writer上限5C30 exclusive。
  PC-1401のRAM範囲や語彙で代用しない。FACT、DECI、FE prefixのカナも既存codecを使う。
- LCD16桁1行5×7、論理95×7（桁間1ドット）。7000–703Bと7068–707Bの逆順配線。
  703C／703D／707Cの18シンボルをCore snapshotから表示し、モードをホストで点灯させない。
- CAL／BASICは本体キーで操作。PC-1401同様、タイトルバーのRUN／PROショートカットは追加しない。
- キー刻印・scanはStudioのPc14xxKeyboardLayout、画像座標は配置済みスキンから整備する。
  左SHIFT刻印は既存LOCK scan、右はSHIFT。左右を同じ論理キーに統合しない。
- 外部キーはF2=CAL、F3=BASICを維持し、F4=カナ、F5=SML、F6=濁点、F7=長音、F8=左SHIFT刻印（LOCK）。
  Backspace／Deleteは既存1401同様CLEAR、DEL／INSは画面SHIFT＋左右キーで操作する。

## 接続前の進め方（当初記録）

LCD案承認後、スキン定義とAndroid画像接続、ホーム・ROM管理・Sessionを接続する。
JDK 21の全体build・Core desktopTest・隔離Studio起動確認、Androidエミュレータで表示／入力／ROM管理／BASIC入出力／背景復帰を検証する。
ROM Libraryと設定はDebug専用namespaceで分離し、ROMとROM由来fixtureは追加しない。
実機確認後に受入れ状況を更新する。RAM拡張・セーブステート・iOS機能拡張は対象外。commit／pushは指示時のみ。

## Android接続確認

ユーザー提供の1450 LCD画像に従い、上段はBUSY／DEF／SHIFT／カナ／小／SML／DE／G／RAD／HYP／括弧／M／E、下段はCAL／RUN／PRO／STAT／PRINT。
共有5テストとAndroid ROM Libraryの登録・復元・置換・削除・ロールバック・Session切替、外部キーの回帰テストが成功。
初回全体buildのReleaseパッケージ生成が失敗したが、同タスクの再実行は成功。最終全体buildは後続の機種追加後に再確認する。
Studioは一時HOME・一時PreferencesFactoryで隔離起動し、確認後にそのプロセスのみ終了。
通常ROM Library・設定から分離したDebug専用ホストを使用。実機・実周辺機器・音声聴感は未確認。

## 最終チェック（2026-10-20）

- JDK 21（21.0.11）で `clean build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache --no-build-cache --no-daemon` が成功。
  Core desktop 462件、共有Player desktop／Android各121件、iOS Simulator 131件、Android unit 77件、いずれも失敗なし。
- 3機種とも既存のローカルROM回帰テストで起動・モード・FACT／POKE実行を確認。ROM全版・最大容量の保証とは区別する。
- `desktopApp:run` は一時HOME・一時PreferencesFactoryで隔離起動し、確認したプロセスだけを終了。
  途中の全体buildでは一時HOMEと通常環境のNative依存パスが混在したため、デーモンを停止し生成物・キャッシュを使わず再検証して解消した。
- 検証途中のAPKは一時HOMEのDebug署名を参照していた。最終APKは通常環境で再生成したため、今回新規作成した隔離AVD内の検証アプリだけを入れ直し、最終APKの起動も確認した。
  通常のAVD・実機・通常ROM Libraryや設定は対象にしない。
- エミュレータの画像確認は通常表示・LCD拡大表示、各81領域の押下／解放、実ROMのBASIC表示と標準保存、DMPロード、外部入力・背景停止を含む。
  ゲームパッド割当・音声PCM・速度比は自動検証。実コントローラ・音声聴感・Android実機の受入れは未実施。
- ドキュメントと同梱ヘルプを更新。ROM・ROM由来fixtureは追加せず、検証用一時ROMコピーも削除した。commit／pushは未実施。
