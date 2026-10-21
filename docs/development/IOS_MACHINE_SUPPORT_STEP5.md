# iOS 機種追加・手順5：PC-1401系・PC-1450

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-21

## 実装

- iOS対応一覧にPC-1401／1402とPC-1450を追加。ホームにはPC-1401系のモデル選択カードと
  PC-1450カードを追加する。選択変更だけでは実行Sessionを変えず、プレイ時の確認で切り替える。
- PC-1401／1402は`rom-library/pc-1401.json`を共用し、標準RAMはそれぞれ3800–47FF／2000–47FF。
  既存共有のシリーズ対応付けでROMとモデルの識別を分離する。PC-1402の実ROMによる容量認識は既知の保留。
- PC-1450は独立した`rom-library/pc-1450.json`と標準RAM 2000–5FFFを使用する。
  RAM拡張や互換制御領域のDMP操作は追加しない。
- 64 KiB単体imageを既存Importerでinternal 8 KiB／external 32 KiBに変換する。
  pgromは成分・サイズ・hash・シリーズ整合性を検証する。32 KiB image、別シリーズ、hash不一致は拒否する。
  保存は既存atomicレコードを使用し、取消・書込み失敗では従来Libraryを維持する。
- 既存共有の16桁×1行LCD、科学計算関数パネルとスキン・キー領域を接続する。
  PC-1450のカナ・SML・濁点・長音・SHIFTロックも同じ共有キーへ配送する。
- CAL／BASICキーを既存Coreへ配送し、LCDのCAL／RUN／PROや各シンボルはROMが書いた表示メモリに従う。
  PC-1401／1402・1450では動作モードメニューも表示しない。
  host側でモード点灯を生成しない。RSVは公開しない。
  右上のRUN/PROトグルはPC-1245／1251系／1261系だけに表示し、PC-1401／1402・1450では表示しない。
  括弧・メモリ・エラー・小文字カナの表示は、既存Androidと同じ`( )`／`M`／`E`／`小`を使い、
  狭いシンボル領域に長い内部enum名を描画しない。
- Physical入力はPC-1401／1402・1450のF2→CAL、F3→BASIC、カンマ・セミコロン・テンキー括弧に対応。
  PC-1450のみF4→KANA、F5→SMALL、F6→DAKUTEN、F7→LONG_VOWEL、F8→LOCKを追加する。
  Shiftからの記号生成やIMEは含めない。既存機種の変換は維持する。
- コントローラ候補の和集合と機種別の許可キー、入力所有権、Pause／背景／Resetでの解放、
  音声・速度設定、Controller Displayは既存経路を使う。
- S1 BASIC／DMPを実行モデルへ接続する。容量超過・非RAMを含むDMPは既存メモリを変更せず拒否する。
- 画像は既存PGP Androidの`skin_pc1401.png`／`skin_pc1450.png`を同梱する。
  Core/commonの機種動作、外部依存、ROM／ROM由来fixtureは追加・変更しない。

## 検証

JDK 21。独自に作成したLCD書込み命令とJPループによる合成ROMを使い、実firmwareは使用しない。

- `build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。578 tasks中26実行、552 UP-TO-DATE。
  Core desktopの既存490件は失敗0で、今回はUP-TO-DATE。
- iOS Native 141件成功。3モデルのROM識別・標準RAM、LCD両端ドットと不変snapshot、
  CAL/RUN/PRO等のROM由来シンボル、全キー、モード要求とRSV拒否、固有Physical／パッド入力、
  Pause／Reset解放、BASIC往復・容量超過保護、DMP境界と混在時のatomic拒否、32 KiB拒否を確認。
- Swift単体45件成功。iPhone SE（第3世代）で実行し、3モデルの667×300表示面への全キー・
  LCD・シンボルの収まり、accessibility IDの一意性、Controller Displayのタッチ無効化、
  固有Physical入力・フォーカス喪失時の解放、割当の機種別可否、BASIC/DMPの実ファイル入出力、
  ROM保存失敗時の保護と削除を確認した。
  PC-1401／1402ではpackageの両モデル間対応付け、hash拒否、共有保存先、切替取消、選択と実行モデルの
  独立復元、置換・削除も検証した。実音声ノード1件は既存CI方針に従い除外した。
- UIは3モデルの関数キー／固有キーと解放、CAL／PROメニュー要求、全シンボルの描画状態、
  Controller Display、背景復帰Pause、Reset、選択取消、モデル切替と再起動後の復元を確認。
  iPhone SE（第3世代）で3モデルを順に操作する1ケースが成功した。
  ラベル修正後のSwift 45件＋UI 1件の成功結果は`iosApp/build/PC14xx-Small-Final.xcresult`。
  PC-1350の既存UI 1ケースも、先述のiPhone 17 Proでの分割実行で成功している。
- 画面画像の確認で内部名が小さく描画される問題を見つけ、短い表示へ修正し単体45件と上記UIを再検証した。
  XCTestの画像取得では黒い部分や、横画面を縦画面の範囲で切り取る結果も発生した。
  `iosApp/build/PC14xx-Small-AppCapture.xcresult`はUI操作成功だが、画像取得の確認とは区別する。
  PC-1401の再起動後にportrait→反対の横向き→目的の横向きで同期する専用UIケースを追加し、成功した。
  `iosApp/build/PC1401-Display-Verified.xcresult`の通常表示とController Displayの両画像を確認し、
  LCD・キー全体・短縮シンボルの欠けがないことを確認した。PC-1402／1450は
  `PC14xx-Small-Final.xcresult`の両表示画像で確認した。撮影方法だけの問題を避けるため、
  最終コードは全画面撮影を維持し、専用ケースで再起動後の向きを同期する。
- 最終時点のUI成功は新機種3モデルの複合ケース、PC-1401再起動後の表示ケース、既存PC-1350回帰の
  計3種類を分割実行した結果。繰り返し実行分を別ケースとして数えない。
  通常Libraryを操作せず、使用した一時Simulatorは各実行の終了時に削除した。
- `git diff --check`とXcode projectの`plutil -lint`成功。同梱ヘルプはソースとSHA-256が一致。
- 2つのスキン画像は既存Android resourceとSHA-256が一致。同梱ヘルプも更新した。

最初のSwift検証は追加テストのprivate参照と型指定でコンパイルに失敗した。
修正後の実行では、未対応機種の旧期待値、PC-1402の起動モデル設定不足、本体CALとメニューCALの
UI要素名の衝突がテスト側に残っていた。未対応機種をPC-1460へ更新し、隔離した設定に起動モデルを指定し、
メニュー操作は本体キーを除いたnative項目を選択するよう修正した。
途中の失敗結果は`iosApp/build/CI-TestResults-20261021-104129.xcresult`（コンパイル）と
`iosApp/build/CI-TestResults-20261021-104301.xcresult`（Swift 45件中2件失敗、新機種UIはCAL選択で停止、PC-1350 UIは成功）。
これらを全件成功した実行として扱わない。

## 残る受入れ

実ROMでの起動・計算・関数・CAL／BASICとモード遷移、BASIC実行・日本語入力、PC-1402の容量認識、
音声聴感、iPhone実機・最低iOS・実パッド／Physicalキーボードと長時間負荷は未確認。
合成ROMで確認するキー配送とシンボル描画を、実ROMでの計算・モード切替成功とは扱わない。
PC-1360以降は次段階で扱う。

## 右上RUN/PROトグルの対象限定（2026-10-21）

ユーザー指定に合わせ、右上トグルの表示・実行をPC-1245／1250／1251／1255／1260／1261に限定した。
PC-1401／1402・1450のCAL／BASICキーと動作モードメニューは維持する。

- 全体`build :core:desktopTest`成功（578 tasks中14実行、564 UP-TO-DATE）。
- 対象Swift単体2件成功。従来6モデルの表示条件とトグル・RSV挙動、新3モデルの非表示条件を確認。
- UI 1件成功。PC-1402→1401→1450を順に操作し、各モデルの`pro`ボタン不存在とモードメニュー操作、
  表示切替・背景・Reset・モデル切替と再起動後の復元を確認した。
  結果は`iosApp/build/CI-TestResults-20261021-112116.xcresult`。
- 一時Simulatorの終了・削除、`git diff --check`成功。

## 動作モードメニューの対象限定（2026-10-21）

ユーザー指定により、右上RUN/PROと動作モードメニューの両方をPC-1245／1251系／1261系だけに限定する。
PC-1401／1402・1450は本体CAL／BASICキーで操作する。Swift hostからの直接モード指定も拒否する。
上記の過去の検証にある1401系・1450のメニュー操作と維持方針は、この仕様で置き換える。

- 修正後の全体`build :core:desktopTest`成功（578 tasks中14実行、564 UP-TO-DATE）。
- 対象Swift単体2件成功。従来6モデルのモード操作と、新3モデルの空の公開モード一覧・直接指定拒否を確認。
- UI 1件成功。PC-1402／1401／1450すべてで右上トグルと動作モードメニューの不存在、本体CAL／BASIC入力、
  表示切替・背景復帰・Reset・モデル切替と再起動後の復元を確認。
  結果は`iosApp/build/CI-TestResults-20261021-113055.xcresult`。一時Simulatorは終了時に削除。
- `git diff --check`成功。
