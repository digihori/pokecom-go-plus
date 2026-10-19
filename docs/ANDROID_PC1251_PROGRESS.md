# Android PC-1251 Player 対応記録

更新日: 2026-10-19
状態: PC-1251のAndroid Player対応はユーザー実機確認まで完了。
2026-10-19、ユーザーからPC-1251／PC-1261両機種の実機テスト完了の報告を受領した。
以下の過去段階にある「未実施」「未受領」「未接続」は当時の記録であり、現在の受入れ状態は本節を基準とする。
自動検証・エミュレータ確認の範囲は、それぞれ実際に実施した記録を保持する。

## 受入れ区分

| 区分 | 状況 |
|---|---|
| 実装済み | キーボード・スキン、実機容量Session、ホーム・ROM decoder、既存ROM管理・入力・入出力・実行系への接続、右上RUN／PRO |
| 自動検証済み | JDK 21で全体build・Core desktopTest成功。共有Player desktop／Android各76件・iOS Simulator 86件、Android unit 48件が失敗なし |
| エミュレータ確認済み | 合成表示・52キー、合成ROM登録、モード切替・Pause／背景停止。隔離した実ROMのRUN起動・計算・BASICロード／保存。詳細は後続節 |
| 実機確認済み | 2026-10-19、ユーザーから両機種の実機テスト完了報告を受領 |

PC-1401／PC-1350の既存進捗文書は冒頭の最終受入れを基準とし、過去段階の未実装記述を現在の欠落とは扱わない。
現在の有効機種はPC-1245／PC-1251／PC-1261／PC-1350／PC-1401。
PC-1360以降やPC-1250／PC-1255は有効にしない。commit／pushは未実施。

## 手順1: PC-1245との差分（既存Core／Studioの確認）

| 項目 | PC-1245 | PC-1251 |
|---|---|---|
| ROM | internal 8 KiB + external 16 KiB | 同じ構成。internal 0000〜1FFF、external 4000〜7FFF。読取り時2000〜3FFFにalias |
| legacy | 既存importerを使用 | 32／64 KiBを既存importerが受理。先頭8 KiBと4000から16 KiBを取り出す |
| CPU速度 | 192,000 cycles/s | 同じ。自動入力hold 38,400／gap 19,200 cycles |
| キー | OLD系52キー | 同じ論理matrix。SHIFT＋↓／↑が括弧、1／2のSHIFT括弧を削除。BASIC略語凡例なし |
| LCD | 16桁、80×7ドット | 24桁、120×7ドット。5×7セル間に共有表示の1列間隔を加え143×7の表示比率 |
| VRAM | F800〜F83BとF868〜F87B | F800〜F83Bが列0〜59、F840〜F87Bが列119〜60 |
| シンボル | BUSY／P／DEF／SHIFT／DE／G／RADとモード | 同じ公開symbol種別。F83C bit0=DEF、bit1=P、bit2=G、bit3=DE、F83D bit0=BUSY、bit1=SHIFT、bit2=RAD |
| モード | Player公開はRUN／PRO | PC-1251はRUN／PRO／RSV。固有のINB接点を既存Coreが扱う。本体MODEキーはない |
| BASIC | OLD形式、標準開始C000 | OLD codecを再利用、標準開始B800。start/end管理pointer C6E1／C6E3、格納上限C6E1未満 |

### RAMに関する注意

- PC-1251の実機容量はB800〜C7FF。B000〜B7FFはB800〜BFFF、D000〜D7FFはC000〜C7FFのalias。
- 既存Core／Studioの既定`EXPANDED`はA000側にもRAMを提供する。今回のPlayerはPC-1251だけ
  `HARDWARE`を明示し、Core／Studioの既定値と既存機種の挙動を変更しない。RAM拡張UIは追加しない。
- 8000〜9FFF側のaliasは実際のbus mapと`MachineMemoryLayout`に従う。LCD aliasもRAMと混同しない。
- BASIC writerは有効な開始pointerを尊重する。標準B800開始時の上限までの容量は3809 bytesで、
  フォーマットの終端を含む。これは既存loaderの格納境界で、公称ユーザー容量やMEM表示値を表すものではない。
  ROM版差・MEM表示は未確認。
- 共有PlayerのDMPはCoreのsession memory layoutを使い、LCDと範囲外を拒否する。

## 手順2: 共有基盤と暫定配置

- `PlayerKeyboardCatalog`へStudio既存のPC-1251凡例差分を移植。desktopAppへの依存は追加しない。
- 配置済み1205×600画像を目視し、52個のキー領域を共有スキンへ追加。PC-1245画像とは縦位置が異なるため、
  PC-1245座標をそのまま使わない。画像自体は変更しない。
- LCDパネル内へ24桁領域、上段7シンボル、下段モード表示の初期座標を置く。
  位置は画像に刻印されていないため、既存PC-1245配置を参考とした暫定案。実機準拠とは確定しない。
- Debug専用`Pc1251SkinProbeActivity`は独自作成のchecker模様と全シンボルを使う。
  ROM Library・設定を読み書きせず、release manifestと通常ナビゲーションには含めない。
- `PlayerMachineCatalog`はPC-1245／PC-1401／PC-1350のまま維持。
  ROM登録／復元／置換／削除／Session切替、quick mode、外部入力の機種別接続は次段階。

## 自動検証

独自作成のJP／LIDP／LIA／STD命令列とDMP文字列を使用。ROMやROM由来fixtureは追加しない。

- 52キーのpress/release受入れ、凡例差、MODE拒否、初期PAUSED。
- B800とC7FFのDMPロード、A000への書込み拒否、後続不正segmentによる先行RAMの変更防止、Session分離。
- 24桁LCD両端ドット、全7フラグとRUN、モード切替、reset、snapshot独立性、192,000 cycles/s。
- スキンの物理IDとキー一致、画像寸法、143:7比率、LCDとキー・シンボルの上下配置。
- PC-1251非選択とPC-1261未接続を維持。

最初の実行でテストの期待キー数を53と誤記して失敗し、既存配列どおり52へ修正した。
最終チェックはJDK 21（21.0.11）で `./gradlew --gradle-user-home .gradle-local build :core:desktopTest :androidApp:assembleDebug` が成功。
Core desktop 457件、共有Player desktop／Android unit各64件・iOS Simulator 74件、Android unit 43件が失敗なし。
`git diff --check`も成功。共有iOSの既存回帰は通過したが、iOSの選択機種は追加していない。

## Androidエミュレータでの基盤確認

- API 36のエミュレータを一時userdataで起動し、Debug専用画面だけを使用。
  通常のROM Library／設定をコピーせず、ROMを登録しない。
- 24桁checker模様、全7シンボルとRUN、配置済み画像を描画。シンボル位置はユーザーレビュー待ち。
- 52個の中心へADBでタップし、Debugホストで52個すべての物理IDのpress／releaseを確認。
  最初の一括入力で上下キーが取りこぼされたため、個別再タップで両キーを確認した。
  ROMでのキー応答・自然な指操作・短押しの保証とは区別する。
- 既存AVDへのデータ保持APK更新は容量不足で失敗。OSキャッシュ整理でも解消せず、
  アプリデータの削除を行わずに一時userdataへ切り替えた。この作業で起動した元AVDは終了。
- 元AVDでは通常アプリを起動せず、ROM Library・設定の編集操作は行っていない。
  前後hashによる同一性照合は未実施であり、hash一致確認済みとは扱わない。
- LCD拡大／逆横向き／safe area／実ROM／Physical／モード操作／実機は未確認。

## 後続

1. 暫定LCD・シンボル配置を表示してユーザー確認。座標確定と実ROMの点灯条件検証を区別する。
2. ホーム・ROM decoder・ROM管理・Session切替にPC-1251だけを接続し、隔離ホストで検証。
3. タッチ全領域、Physical、RUN／PRO／RSV操作、表示切替・両横向き。
4. BASIC／DMP往復と境界、ゲームパッド、サウンド、CPU速度、停止・背景復帰。
5. Android実機でユーザーが確認後、最終受入れとドキュメントを整理。

通常ROM Library・設定を使う検証は行っていない。RAM拡張、セーブステート、iOS機能拡張、iOS CI高速化は対象外。

## LCD配置レビュー: SHIFTの位置

- ユーザー指示により、SHIFTの左端をLCD文字領域の左端から約3/4の位置へ移動。
  図面座標は `70 + 1072.5 × 0.75 = 874.375`。高さ・文字サイズ・他シンボルは維持。
- 配置変更の実装と、変更後の表示に対するユーザー確認は区別する。実機確認は未実施。
- 変更後もJDK 21で全体build・Core desktopTestが成功。隔離Androidエミュレータの合成表示でSHIFTの移動を確認。

## ユーザー配置確認

2026-10-19、SHIFTを約3/4へ移した合成プレビューに対してユーザーがOKと回答。
LCD／シンボルの配置案を確認済みとして保持する。実ROMの点灯条件やAndroid実機確認の完了とは扱わない。

## 手順3: ホーム・ROM管理・Session切替

- 明示的なPlayer選択対象へPC-1251だけを追加。既存3機種の順序を維持して末尾へ追加。
- pgromの機種・サイズ・hash検証を再利用し、legacyはPC-1251の32／64 KiB importerへ接続。
- Debug専用Pc1251RomProbeActivityは通常Library／設定と別の名前空間を使い、独自JP命令から生成したpackageだけを扱う。
- 旧段階の「非選択」「ホーム未接続」は当時の記録であり、この段階の実装で更新する。

## 手順4の修正: タイトルバーのRUN／PRO

- ユーザー指摘により、PC-1245だけに限定されていた右上のモード切替ボタンをPC-1251にも接続。
- RUN時はPROへ、PRO／RSV時はRUNへ切替。RSVの選択は動作モードメニューに残す。
- 既存PC-1245の表示を維持し、PC-1401／PC-1350には追加しない。
- ユーザー追記: PC-1261も右上RUN／PROボタンの対象とする。表示条件を先に追加するが、
  PC-1261のホーム・ROM decoder・Session接続は有効化せず、次機種として別途進める。

## ロード完了表示の簡略化

ユーザー指摘により、Android共通のロード完了ダイアログからCPU自動実行とBASIC RUNに関する説明を削除。
本文は「ファイル名 をロードしました。」だけとする。ロード後の処理は既存のまま。

## 手順3〜6の追加検証

### 自動検証

- Android ROM統合4件: PC-1251の32／64 KiB legacy、4機種pgromの全取り違え拒否、破損package拒否、
  復元・置換・使用中／未使用ROM削除、保存失敗時の既存packageと他機種・選択状態の保持、SessionのRAMと実行意図。
- Physical入力1件: PC-1251でF2／F3のCAL／BASICを拒否し、上下キーの最小保持38,400／gap19,200 cyclesと停止による解放。
- 共有BASIC／DMP4件: BOM・説明行・CRLFを含むOLD BASIC往復、B800の開始pointer、checkの読取り専用、
  容量超過・機種不一致のRAM不変、RAM端・alias・mirror重複・ROM／LCD／範囲外の拒否、管理pointer破損と停止条件。
  最初はROM起動のない合成Sessionで管理pointer未初期化のため4件失敗し、テスト側で空のBASIC範囲を初期化して再実行し成功。
- 共有ゲームパッド3件: 全52キー割当とCore受付、タッチ／キーボード／複数ボタンの入力所有権、
  停止・モーダル・Reset・切断・機種切替の解放と押し直し、他機種用CAL割当のPC-1251への送信抑止。
- 共有Runner5件: 192,000 cycles/s、pause／reset、全速度×30／60／120 Hzと音声時間、速度変更時の停止維持。
- 既存音声3件の対象にPC-1251を追加: 2種類の発振、直接出力、無音時間とsnapshot独立性。
  実ROMのBEEP・実際の音量／遅延／出力先は別途確認する。

### Androidエミュレータ

- 専用ROMホストで4機種表示、PC-1251未登録、Documents UIから合成pgromの登録、RAM破棄の説明、選択状態とPlayer自動実行。
- PRO／RSV切替、明示Pause、別機種切替のキャンセル、同機種ホーム往復と背景復帰でPAUSED維持。
- 既存ローカルPC-1251 pgromを専用Inputホストへ渡し、実ROMのRUN起動と外部keyboard相当の123＋456＝579を確認。
  入力キュー消化後に保持キー0・queued0を確認。実キーボード／指操作とは区別する。
- 右上ボタンのRUN→PRO→RUNとモード表示の連動。RSVはメニューに保持。
- 独自作成のBASICをDocuments UIからロードし、ロード完了を確認。RUNを別途キー入力。
  BASICセーブで新規ファイルを作成し、独自の元テキストと保存結果が一致。
- ROMの置換・削除・異機種拒否・プロセス再起動は今回のエミュレータでは未確認（上記の自動テストと区別）。
- 通常Library／設定を扱うホストは起動せず、専用namespaceと一時userdataを使用。ROM／ROM由来画像はGitへ追加しない。

### 未確認

実機、実コントローラ、サウンド実聴、BEEP、全点灯条件、
各provider、Activity再生成、逆横向き／実機safe areaは未確認。
PC-1261以降への着手・commit／pushはしない。
- 説明文削除後のJDK 21全体build・Core desktopTest・差分チェックが成功。
  エミュレータでDMPを実ロードし、本文が「pc1251-check.dmp をロードしました。」だけになったこととRUNNINGを確認。

- DMPもDocuments UIからロードし、C500〜C503を新規保存した結果が独自の`12 34 56 78`と一致。
  BASIC／DMPの通常ファイルは上書きせず、検証専用の新規ファイルだけを使用。

- 保存BASICの再ロード後に別途RUNを入力し、実ROMのLCDに`PC1251`が出ることを目視確認。
  右上RUNボタンの表示、入力キュー消化・heldKeys=0も確認した。
- BASIC／DMPは新規保存したファイルを照合して一致。実機確認と音声／実コントローラの受入れは未実施。

## ホーム表示の変更

- ユーザー依頼により、ホームにタイル／リスト切替を追加。初期表示はタイルで、画面幅に応じて列数を調整する。
- 表示形式はAndroidの専用player-home設定へ保存。Debug専用ホストでは従来どおり別namespaceへ隔離。
- 機種名でソートし、ホーム・ROM一覧はPC-1245 → PC-1251 → PC-1350 → PC-1401の順。
  前段階の「既存順序の末尾へ追加」はこのユーザー指示で更新する。機種の有効範囲は追加しない。
- リスト／タイルで同じ機種の登録状態・選択状態・ROM登録／プレイ操作を使う。
- JDK 21の全体build・Core desktopTestと差分チェックが成功。
- 隔離Androidエミュレータで4列タイル・リスト切替・名前順、プロセス再起動後のリスト表示復元を確認。
  実機の画面幅や文字サイズ変更時の表示は未確認。通常のROM Library・設定は編集しない。

## 共通セーブ仕様の更新

PC-1261着手と同時のユーザー指示により、Android全機種のDMPセーブ・形式選択・アドレス入力を削除。
本記録のDMP保存確認は旧仕様の検証履歴として保持し、現在のAndroid製品ではDMPロードだけを提供する。
BASICセーブは維持。PC-1251の実機確認結果はまだ受領していない。

## 固定位置のモード表示

ユーザー指示により、RUN／PRO／RSVを下段の別々の固定位置に並べ、現在のモードだけ濃く表示する。
従来の同じ位置へ1つだけ表示する方式を更新。PC-1245はRUN／PROの2つに限定する。

## 音階プログラムの単音継続調査

- 提供されたBASICをローカルROMで再現。BEEP 1はPCM出力後に無音へ戻る。
- 音階プログラムはカウンターに8000番地（A000のミラー）を使用する。
  PlayerのHARDWARE設定では書き込みできず、音階インデックスが0のまま発音ループを続ける。
  既存Core／StudioのEXPANDED設定ではインデックスが進み、音階が変化する。
- 元ファイルを保存したまま、2か所のカウンター参照だけを未使用の実機RAM C02Fへ移したコピーを作成。
  HARDWARE設定でもインデックスが4、12、18、26…と進み、PCMの周期が変わることをCoreで確認。
  コピーは検証用のローカルファイルで、ROMや提供ソースをリポジトリに追加しない。
- サウンド実装とRAM設定は変更していない。RAM拡張は今回の対象外。
- 自動確認：ローカルROMによる原因切り分けとBEEPの無音復帰。
  変更版BASICのAndroidエミュレータ確認・実機確認は未実施。

## 最終受入れ（2026-10-19）

ユーザーからPC-1251／PC-1261の実機テスト完了の報告を受領し、両機種の対応を一区切りとする。
機能別の実機テスト手順・結果の詳細は報告されていないため、個別項目の追加確認を行ったとは記載しない。
音階プログラムのRAM参照先に関する原因切り分けは別記のとおり。確認用BASICの個別再生結果は未報告。
RAM拡張・セーブステート・iOS機能拡張は対象外。commit／pushは未実施。
