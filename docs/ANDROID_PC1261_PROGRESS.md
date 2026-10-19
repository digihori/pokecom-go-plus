# Android PC-1261 Player 対応記録

更新日: 2026-10-19
状態: PC-1261のAndroid Player対応はユーザー実機確認まで完了。
2026-10-19、ユーザーからPC-1251／PC-1261両機種の実機テスト完了の報告を受領した。
以下の過去段階にある「未実施」「未受領」「未接続」は当時の記録であり、現在の受入れ状態は本節を基準とする。
自動検証・エミュレータ確認の範囲は、それぞれ実際に実施した記録を保持する。

## 受入れ区分

| 区分 | 状況 |
|---|---|
| 実装済み | PC-1261共有52キー、スキン・2行LCDと9シンボル、ホーム・ROM管理・Session接続、Debug専用合成表示 |
| 自動検証済み | 機種別共有4件とAndroid unitをJDK 21で実行し成功。全体build・Core desktopTestも成功 |
| エミュレータ確認済み | 合成LCD・全9シンボル表示、52キーの押下・解放を確認。実ROM未確認 |
| 実機確認済み | 2026-10-19、ユーザーから両機種の実機テスト完了報告を受領 |

## PC-1251との差分

- 既存CoreのPC-1261を再利用。internal ROM 8 KiB、external ROM 32 KiB（8000〜FFFF）。
  legacyは64 KiBのみで、0000〜1FFFと8000〜FFFFを取り出す。
- RAMは4000〜67FF、LCDは2000〜3FFFの既存map。RAM拡張・PC-1260選択は追加しない。
- BASICはOLDからS1へ変更。標準開始4080、start/end pointer 66E1／66E3、格納上限66E0未満。
  Coreのcodec／ProgramImageLoaderを再利用し、ROM版差によるhookは設けない。
- CPUは256,000 cycles/s。自動入力hold 15,360／gap25,600 cycles。
- キーは同じ論理52キー。括弧はSHIFT＋1／2へ戻り、SHIFT＋7=HELP、SHIFT＋8=カナ。
  独立KANA／MODEキーは追加しない。RUN／PRO／RSV接点は既存Coreが扱う。
- LCDは24桁×2行。snapshot 120×14ドット、5×7セル、表示間隔込み143×15を保つ。
  上段左2000〜203B／右2800〜283B、下段左2040〜207B／右2840〜287B。
- シンボルはBUSY／PRINT／KANA／SMALL／SHIFT／DEF／DEG／RAD／ERROR。
  203Dのbit0／1／3／4／5／6、207Cのbit0／1／5を既存Coreから受け取る。
  AndroidのERRORラベルはE。モード表示はRUN／PRO／RSV。

## 実装と配置案

- StudioのPc1261KeyboardLayoutの凡例差分を共有Playerへ移植。desktopAppへの依存なし。
- 既存1206×600画像を目視し、PC-1251画像と同じキー位置を再利用。
  LCDパネルは49,46の1106×164、2行LCDを74〜186.5に配置。
- 上段BUSY／DEF／SHIFT／SML／カナ、下段モード／PRINT／DEG／RAD／Eを初期案とする。
  旧Pokecom GO MainLoop1261.javaの上下分離も参照した。確定位置ではなくユーザー確認で調整する。
- Debug専用Pc1261SkinProbeActivityは独自命令列のcheckerと全シンボルを使用。
  通常のROM Library／設定を読まず、releaseへ含めない。ROM／ROM由来fixtureは追加しない。
- 先に用意した右上RUN／PRO表示条件は維持。PC-1261のホーム選択・ROM decoderへの接続は次段階。
  PC-1360以降は有効化しない。

## 自動検証

- 52キーのCore受理・解放、凡例差、独立MODE／KANA拒否、初期PAUSED。
- 独自LIDP／LIA／STD命令で2行それぞれの左右端・全9フラグ、snapshot独立性、reset、モード切替を検証。
- S1 BASICのBOM／説明行／CRLF、保存と別Sessionへの再ロード、容量超過時の保持。
- DMPロードの4000／67FF、範囲外／ROM／LCDの拒否、BASIC保持。
  共有のダンプ出力はテスト・Core機能として保持するが、Android製品のDMPセーブは削除する。
- 52物理IDと画像領域の一致、143:15比率、LCD・シンボル・キーの上下分離。

## Android共通: BASIC専用セーブ

- ユーザー指示により、全Android機種で保存形式選択と開始／終了アドレス入力を削除。
  セーブ画面は「BASICをセーブ」と保存／キャンセルだけにする。
- AndroidProgramIoのexportはBASIC専用にし、DMPへ到達する保存分岐とアドレスparserを除去。
  不要になったアドレスparser単体テストを削除。既存のBASIC保存・SaveResumeStateとDMPロードは維持。
- Core／StudioとiOSは今回の変更対象外。commit／pushは未実施。

## 後続

LCD・シンボル配置のユーザー確認、ホーム・ROM管理・Session、Physical／モード・LCD拡大、
BASICとDMPロード、ゲームパッド・サウンド・CPU速度・停止／背景復帰、実機確認を区別して進める。

## 今回のチェック結果

- JDK 21（21.0.11）で `build :core:desktopTest :androidApp:assembleDebug` が成功。
  下段シンボルの文字サイズを上段へ揃えた後も同じチェックが成功。差分形式チェックも成功。
- Core desktop 457件、共有Player desktop／Android各80件、共有iOS Simulator 90件、Android unit 47件が失敗なし。
  Androidの件数減少1件は、削除した保存アドレスparserのテストを除去したため。
- ROM不要の合成表示で24桁×2行と全9シンボル、52キーすべての押下／解放を確認。
  上下シンボル帯・モード・LCDの比率は初期案であり、実ROMや実機の受入れとは区別する。
- Android共通BASIC専用セーブ画面で、形式・アドレス入力がないことを確認。
  実行中から開いてキャンセルするとRUNNING、明示PauseからはPAUSEDを維持することを隔離ホストで確認。
- BASIC／DMPの実ファイル往復はPC-1251前段階の記録を保持。BASIC専用変更後の新規保存完了とPC-1261実ROMでの往復は未確認。
- 通常のROM Library／設定を読み書きするホストは起動せず、専用Debugホストだけを使用。
  今回はROMを端末へ転送していない。ROM／ROM由来データ・取得画像はGitへ追加しない。

## ユーザー図によるシンボル再配置

- シンボル帯を高さ20から24へ拡大し、文字を約2割大きくする。2行LCDは143:15比率を保ち、上端だけ0.5座標上げる。
- 提示図に合わせ、上段はBUSY／DEF／SHIFT、間隔を置いてSML／カナ。
  下段はRUN／PRO／RSV、PRINT、DEGRAD、ERRORの順に左側へ配置。
- DEGとRADは同じテキストレイアウトで空白なく連結し、色を別々に指定して点灯bitの独立性を維持する。
- モード名は固定した3位置に並べ、現在のものだけ濃く表示する。ERRORは提示図の表記へ合わせる。
- ユーザー追記に従い、PC-1245もRUN／PROの2位置、PC-1251はRUN／PRO／RSVの3位置へ変更。
  PC-1245にRSV機能やラベルは追加しない。PC-1251のSHIFT位置など既存シンボルは維持。
- 共有テストで3機種の対応モード・固定位置・単一モード点灯、1261の文字サイズとDEG／RADの連結領域を検証する。
- DEGだけ点灯する合成表示でも、RADを隣接した薄い文字として表示できることをエミュレータで確認。
- 初回の全体チェックで、PC-1245の共有スキンを直接変更するとiOSの既存テストが失敗することを検出。
  固定モード表示を純粋なスキン変換として分離し、Androidホストだけが適用する形へ修正した。
  iOSのPC-1245表示方式を維持し、モード機能の追加は行わない。
- 修正後のJDK 21全体build・Core desktopTest・差分チェックが成功。共有Player desktop／Android各82件、
  iOS Simulator 92件、Android unit 47件が失敗なし。Core desktopは457件を維持。
- ROM不要の合成画面で文字の拡大・図の配置・固定モード3位置・DEGRAD連結を確認。実機確認は未実施。
- 最終APKの隔離合成ROMホストで、PC-1245のRUN／PROのみ、PC-1251のRUN／PRO／RSVが
  別々の固定位置に出ることをUI採取で確認。起動直後のUI採取失敗は、起動完了後に取り直した。

## ホーム・ROM管理接続

- ユーザーのホーム未表示の指摘を受け、PC-1261を明示的な選択対象へ追加。
  名前順はPC-1245 → PC-1251 → PC-1261 → PC-1350 → PC-1401。タイル／リスト両方へ反映する。
- pgromは既存の機種・component・サイズ・hash検証、legacyは既存Pc1261FlatRomImporterの64 KiBに接続。
  PC-1260／PC-1360以降は有効化しない。
- 既存AndroidRomManagerの登録・復元・置換・削除・Session切替を利用。
  新しいDebug専用Pc1261RomProbeActivityで通常のLibraryと設定を分離する。
- 合成命令だけの5機種packageで取り違え拒否、復元・置換・削除、保存失敗時の他機種保護、
  SessionのRAMと実行意図を検証する4件のAndroidテストを追加。
- 前段階の「ホーム未接続」は当時の履歴とする。PC-1261対応全体や実機確認の完了とは扱わない。

- JDK 21で全体build・Core desktopTest・Android APK作成が成功。Core desktop 457件、共有Player desktop 82件・
  iOS Simulator 92件、Android unit 51件が失敗なし。差分チェックも成功。
- 隔離エミュレータのホームに5機種と未登録PC-1261を表示。独自JP命令から生成したpgromをDocuments UIで登録し、
  登録済み・選択中表示、PC-1261プレイ開始とRUNNINGを確認。通常Library／設定は使用しない。
- ROMの復元・置換・削除・取り違え拒否・保存失敗保護は今回の自動テストで確認し、エミュレータでは未実施。
  PC-1261実ROM・実機の確認は未実施であり、対応完了とは扱わない。

## 最終受入れ（2026-10-19）

ユーザーからPC-1251／PC-1261の実機テスト完了の報告を受領し、両機種の対応を一区切りとする。
機能別の実機テスト手順・結果の詳細は報告されていないため、個別項目の追加確認を行ったとは記載しない。
音階プログラムのRAM参照先に関する原因切り分けは別記のとおり。確認用BASICの個別再生結果は未報告。
RAM拡張・セーブステート・iOS機能拡張は対象外。commit／pushは未実施。
