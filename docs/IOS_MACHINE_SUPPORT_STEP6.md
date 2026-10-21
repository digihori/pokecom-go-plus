# iOS 機種追加・手順6：PC-1360／PC-1460／PC-1470U

更新日: 2026-10-21

## 実装

- iOSカタログとホームに3機種を追加し、各機種の独立したROM Libraryを使う。
  選択だけでは実行Sessionを変えず、プレイ時の確認、取消・Pause・入力解放、再起動時の停止復元を再利用する。
- ROMはAndroidと同様、Studioで作成したpgromを登録する。内部8 KiBと16 KiBバンク
  （PC-1360／1470Uは8個、PC-1460は4個）が必要。単体画像・個別バンクファイルの推定取込みは行わない。
- Apple境界で機種整合性、成分ID集合、role、サイズ、ホストが計算したSHA-256との一致を検証する。
  bank不足、重複entry、内部／外部role不一致、別機種は拒否する。保存は既存のatomicレコードを使用する。
- 既存共有のスキン・LCD・キーを使用する。1360は25桁×4行、150×32ドットと6シンボル、
  1460は24桁×1行と19シンボル、1470Uは24桁×2行と22シンボル。
  1470UのBATTを短縮表示し、その他も既存の( )／M／E／小等を使う。
- 1360は本体MODEキー、1460はCAL／BASIC、1470UはAER（SHIFTでCAL）／BASICキーで操作する。
  右上トグルと動作モードメニューはPC-1245／1251系／1261系だけに表示する。
  1470Uの2つのSHIFTは共有領域IDのSHIFT／LOCKを使い、片方を離しても他方の所有権を保持する。
- Physical入力は1360のF2=MODE、F3=KANA、F4=SMALL、Insert=INS、前方Delete=DELを接続。
  1460／1470UはF2=CAL、F3=BASIC、F4〜F7=KANA／SMALL／DAKUTEN／LONG_VOWEL。
  F8は1460でLOCK、1470UでSHIFT。1470UはBackspace=BS、前方Delete=DEL、Insert=INSも接続する。
- コントローラの共通割当と起動機種別のキー制限、入力所有権・フォーカス喪失時の解放、
  音声・速度・Controller Displayを既存経路へ接続する。
- S2 BASICと機種別の既存語彙を実行機種へ接続する。RAMは8000–FFFFで、DMPはROM窓・制御・LCD領域を拒否する。
  BASIC容量超過と、不正領域を含むDMPは既存RAMを変更せず拒否する。
  実ROMの初回RAM確認等を済ませてからBASICをロードする必要がある。
- Core/common、外部依存、実firmwareの追加・変更はない。既存PGP Android画像をそのまま同梱する。
  PC-1360Kは次段階で扱う。

## 検証

JDK 21。独自のLIDP／LIA／STD／JP命令列で内部ROMから各バンクへ順に分岐し、
各バンクがRAMの9000以降へ異なる識別値を書いて内部ROMへ戻る合成packageを使用する。
最後にLCDの端のドットとシンボルを書き、自己JPする。実ROM／ROM由来fixtureは使用しない。

- `build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。578 tasks中28実行、550 UP-TO-DATE。
  Core desktopの既存490件は失敗0で、今回はUP-TO-DATE。
- iOS Native 142件成功。全8／4／8バンクの実行結果、各LCDの端と不変snapshot、全シンボル、
  S2 BASIC往復・容量超過時保護、DMP境界・不正セグメント混在時の無変更、全キーと機種別Physical、
  SHIFT所有権、Pause／Resetの解放、単体ROM拒否、成分不足・重複・role不一致の拒否を確認した。
- 最初のNative実行は、不正なbank roleを1360が受け付けて1件失敗した。
  Apple側にバンク成分の完全検証を追加し、上記の全体再実行で142件成功した。Core/commonは変更していない。
- Swift単体47件成功（実音声ノード1件は既存CI方針に従い除外）。3機種のpackage成分と正規化時のbytes保持、
  hash・bank不足・role・別機種・単体画像の拒否、固有Libraryの保存・停止復元・保存失敗時保護・削除、
  S2 BASIC/DMPの実ファイル入出力と不正DMP時の保持を確認した。
  667×300表示面の全キー・シンボルの収まり、accessibility IDの一意性、Physical入力とフォーカス解放、
  機種別の割当可否、1470Uの2つのSHIFT所有権、Controller Displayも確認した。
- iPhone SE（第3世代）で新3機種のUIケースを個別に実行。
  最初は1460／1470Uの2件が成功し、1360の1件がSHIFTの識別子で失敗した。
  共有定義は`SHIFT_LEFT`なので、テストの識別子を修正して1360だけ再実行し成功した。
  最初の結果は`iosApp/build/Banked-Small-Verified.xcresult`（Swift47成功、UI2成功・1失敗）、
  再実行は`iosApp/build/PC1360-Small-Verified.xcresult`（UI1成功）。
  UI3件を同一の全件成功実行としては扱わない。
- 各機種のキー押下／解放、LCD・シンボル、モード用トグル／メニューがないこと、両横向きの表示切替、
  背景復帰Pause・Reset・アプリ再起動後のモデル復元を確認した。
  3機種それぞれの通常表示・Controller Displayの計6画像も確認し、キー全体とLCDが画面内に収まることを確認した。
- 通常ROM Libraryを使わず、一時Simulatorは各実行終了時に削除した。
  画像3点は既存Android resourceと、同梱ヘルプはソースとSHA-256が一致。
  `git diff --check`とXcode projectの`plutil -lint`成功。

## 残る受入れ

実ROMでの起動・初回RAM確認・バンク処理とモード遷移、計算・S2 BASIC実行、音声聴感、
iPhone実機・最低iOS・実パッド／Physicalキーボード、長時間負荷は未確認。
合成ROMの全バンク実行とキー配送、シンボル描画を、実ROMでのゲーム・BASIC・計算の受入れとは扱わない。
