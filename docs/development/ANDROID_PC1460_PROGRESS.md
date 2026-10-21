# Android PC-1460 Player 対応記録

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-20
状態: 実装・自動検証・エミュレータ確認を経て、2026-10-20にユーザーから一通りの実機動作確認で問題なしとの報告を受領。今回の対応は完了。
シリーズ内の機種切り替えは保留。以下の未実施・確認待ちの記述は当時の検証段階の履歴であり、現在の受入れ状況は冒頭と表を優先する。
個別の周辺機器・ROM全版・全機能を網羅した確認結果の報告とは区別する。

## 受入れ区分

| 区分 | 状況 |
|---|---|
| 実装済み | 81キー、24桁1行LCD、19シンボル、4バンクpgrom、固定RAM、S1 BASIC保存／ロードとDMPロード |
| 自動検証済み | 共有5テスト：Core対応キー・ゲームパッド、LCD区画端と19フラグ、BASIC／カナ保存再ロード、DMP境界・失敗時無変更、速度・音声・停止復帰。Android ROM管理・機種間取り違え拒否・Session切替も成功 |
| エミュレータ確認済み | 81キー押下／解放、隔離pgrom復元、実ROMの初回Y／ENTER確認後CAL表示。BASICロード→RUNでPC1460表示、標準保存ピッカー・保存テキスト照合、DMPロード、背景復帰でPAUSED・held/queued=0 |
| 実機確認済み | 2026-10-20、ユーザーより一通りの動作で問題なしとの報告。今回の対応を完了 |

## 1450との差分

- 内部8 KiB＋16 KiB×4バンク、4000–7FFFの窓、3C00下位2bit。AndroidはStudio生成pgromのみ。
  単一flat dumpからbankを推測しない。従来の2ファイルはStudioで取り込む。
- RAM8000–FFFFの固定32 KiB構成。2000–3FFFは制御／LCDとしてDMPロードを拒否。
- S1語彙・カナは1450同様。初期text8030、start/end pointer FF01／FF03、writer上限FF01 exclusive。
  これはソフトウェア境界であり最大実機空き容量の保証ではない。
- LCD24桁・論理5×7、桁間込み143×7。物理dot pitchはStudio同様の横3：縦4。
  6区画の独自配線と19シンボルは既存Coreを再利用。
- ユーザー提供1460画像に従う上段配置、下段は左CAL／RUN／PRO、右MATRIX／STAT／PRINT。
- キーmatrixは1450共通。左はLOCK刻印、上下キーSHIFTはMATRIX、関数キーFSE。
  右上RUN／PROは設けず本体CAL／BASIC。外部F2=CAL、F3=BASIC、F4–F8は1450同様。
- 通常ROM Library・設定を使わないDebugホストで検証。実ROM・ROM由来fixtureは追加しない。
- RAM拡張・セーブステート・iOS追加は対象外。commit／pushは未実施。

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
