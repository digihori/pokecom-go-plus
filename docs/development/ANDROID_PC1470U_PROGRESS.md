# Android PC-1470U Player 対応記録

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-20
状態: 実装・自動検証・エミュレータ確認を経て、2026-10-20にユーザーから一通りの実機動作確認で問題なしとの報告を受領。今回の対応は完了。
シリーズ内の機種切り替えは保留。以下の未実施・確認待ちの記述は当時の検証段階の履歴であり、現在の受入れ状況は冒頭と表を優先する。
個別の周辺機器・ROM全版・全機能を網羅した確認結果の報告とは区別する。

## 受入れ区分

| 区分 | 状況 |
|---|---|
| 実装済み | 主キー63＋関数18の81領域（論理80キー）、24桁2行LCD、22シンボル、8バンクpgrom、専用S2 BASIC、BASIC保存とBASIC／DMPロード |
| 自動検証済み | 共有6テスト：Core scanとの一致、全ゲームパッドキー、2行LCD配線・22フラグ、S2 BASIC／カナ保存再ロード、DMP境界と失敗時無変更、左右SHIFT所有権、速度・音声・停止復帰。Android ROM管理・全10機種の取り違え拒否・不完全ROM拒否・Session切替も成功。最終全体チェックは後続 |
| エミュレータ確認済み | 81領域タッチ押下／解放、隔離pgrom復元、実ROM初回Y／ENTER後RUN、BASICロード・標準保存と保存テキスト照合、背景復帰でPAUSED・held/queued=0。RUNへ戻して実行し下段PC1470U表示、AER→SHIFT CAL→RUN、外部BS／DEL／INS、DMPロードも確認 |
| 実機確認済み | 2026-10-20、ユーザーより一通りの動作で問題なしとの報告。今回の対応を完了 |

## 1460との差分

- 内部8 KiB＋16 KiB×8バンク、4000–7FFF窓、selector aperture 3400–35FF下位3bit。
  AndroidはStudio生成pgromのみ。単一flat画像からbankを推測しない。
- RAMは既存Coreの8000–FFFF互換窓。実機カード構成と同一視せず、容量選択は追加しない。
- 専用語彙のS2、FE token・1F line reference・1-byte半角カナ。1360の語彙を代用しない。
  初期text8030、start/end pointer FFD7／FFD9、writer上限FFD7 exclusive。
- LCD24桁2行、各5×7、桁間・行間各1ドット（143×15）。物理pitchは正方形。
  283C／283D／287C／287Dの22bitは既存Coreを使用。
- ユーザー提供画像に従い上段BUSY／DBL／DEF／SHIFT／カナ／小／SML／DE／G／RAD／HYP／括弧／M／E／BATT、
  下段左CAL／AER／RUN／PRO、右MATRIX／STAT／PRINT。
- DEL／BS／INSは固有scan。左右SHIFTは同じCoreキーに異なる物理IDを与える。
  関数「↕︎」はTITLE scan、STATはSHIFT＋「=」。TITLE固有機能の詳細確認は既存方針どおり保留。
- 本体AER（SHIFTでCAL）／BASICを操作。タイトルバーRUN／PROショートカットは設けない。
  外部F2=AER（SHIFTでCAL）、F3=BASIC、F4=カナ、F5=SML、F6=濁点、F7=長音、F8=SHIFT。
  Backspace=BS、Delete=DEL、Insert=INS、Clear=C-CE。
- 初回RAM確認後は今回のローカルROMでRUNを観測。BASICキーを無条件に押すとPROへ変わり得るため、LCDを見て選ぶ。
  初回検証でPROへ切替後RUNと入力してERROR 9になったのは検証操作のモード誤り。ロード失敗とは扱わない。
- 通常ROM Library・設定を使わない専用Debug namespaceで検証。ROM・ROM由来fixtureは追加しない。
- RAM拡張・セーブステート・iOS機能追加・CI高速化は対象外。commit／pushは未実施。

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
