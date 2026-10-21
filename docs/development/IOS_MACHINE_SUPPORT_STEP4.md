# iOS 機種追加・手順4：PC-1350

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-21

## 実装

- iOSの対応一覧とホームにPC-1350を追加。ROMは`rom-library/pc-1350.json`に保存する。
  64 KiB単体imageは既存CoreのImporterでinternal 8 KiB／external 32 KiBへ変換する。
  pgromも構成・サイズ・hashと選択機種を検証する。32 KiB単体imageと別シリーズは拒否する。
- 既存共有の25桁×4行LCD、150×32ドット、PC-1350のスキンと62キー領域を使用する。
  文字の6列目・8行目もCoreの実ドットとして描画し、別の仮想行間を追加しない。
- モードは本体MODEキーでROM側が切り替える。直接のRUN/PROボタンと動作モードメニューを表示せず、
  facadeでも直接モード指定を拒否する。LCDのRUN/PRO等は実際の表示メモリから読み取る。
  既存PC-1245／1251系／1261系の固定モード位置とRUN/PROトグル・メニューRSVは維持する。
- 2つのSHIFTは独立した領域IDで扱う。指移動時に押下開始領域を照合し、ハイライトとaccessibility要素も
  論理キー名ではなく領域IDを使う。片方を離しても他方の所有者が残る間はSHIFTを保持する。
- PC-1350のPhysical入力にF2→MODE、F3→KANA、Insert→INSERT、前方Delete→DELETEを追加する。
  BackspaceはCLEARを維持し、旧機種の前方DeleteもCLEARのまま。
  カンマ・セミコロン・テンキー括弧も対応する。IMEや文字変換・Shift自動生成は追加しない。
  UIKit responderのOSへの転送判定も同じ機種別の対応表を使用する。
- コントローラの割当候補は対応機種の和集合。割当値は全体共通で保持し、利用可否と配送は起動中機種の
  許可キーで制限する。旧機種へ切り替えてもMODE等の割当値を消さず、利用不可として示す。
- S1 BASIC／DMPは既存共有処理を使用する。RAMは2000–7FFFだが表示メモリ7000–787B等はDMPで拒否する。
  BASIC容量超過、非RAMへの混在DMPなどの失敗では既存データを維持する。
- ROMのatomic保存と失敗時保護、モデル選択・復元、Pause／背景／Resetの入力解放、
  音声・速度設定、Controller Displayは既存経路を再利用する。
- 画像は既存PGP Androidの`skin_pc1350.png`を使用し、外部コード・ROM由来fixture・build依存は追加しない。
  Coreと共有スキンの機種動作・形状を変更していない。

## 検証

JDK 21。独自のLCD書込み命令とJPループで合成ROMを作成し、実機firmwareは使わない。

- `build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。578 tasks中28実行、550 UP-TO-DATE。
- Native 140件成功。追加ケースで4行それぞれの先頭・末尾ドットと不変snapshot、全許可キー・左右SHIFT所有、
  MODE経由の入力、機種別Physicalコード、BASIC往復・容量超過時保護、DMPの範囲と失敗時無変更を確認。
- Swift単体43件成功（実音声ノードのテストは既存CI方針に従い除外）。追加ケースで667×300の表示面に62キーと全accessibility要素が収まること、
  SHIFT_RIGHT上で移動しても保持すること、片方の解放と両SHIFT表示、領域外移動とcancel、
  Controller DisplayのLCD収まり・タッチ無効化を確認した。
- Swiftで固有Physical入力・フォーカス喪失の解放、旧機種に切り替えた割当の利用不可表示と値の保持、
  実際のBASIC/DMPファイル読込み・export、ROM登録／復元・保存失敗時保護・削除を確認した。
- UIはPC-1350、PC-125x、PC-126x、既存PC-1245の起動操作とController Displayの計5ケースを検証。
  最初の実行は4件成功・PC-126xの機種選択操作1件失敗だった。テストのスクロールを短いドラッグと
  表示範囲確認に修正し、iPhone SE（第3世代）でPC-126xとPC-1350の2件を再実行して両方成功した。
  結果は`iosApp/build/CI-TestResults-20261021-102626.xcresult`と
  `iosApp/build/PC1350-Small-Verified.xcresult`。
- PC-1350ではMODE等と左右SHIFT、62領域、直接モード操作の非表示、背景復帰時Pause、Reset、
  機種復元、両向きのController Displayを確認。SEの通常表示とLCD拡大表示の画像も確認し、
  4行の先頭・末尾ドット、キー全体とLCDが画面内に収まることを確認した。
  使用した一時Simulatorは終了時に削除した。

## 残る受入れ

iPhone実機・最低iOS・実ROMでの起動、MODEによる実際のモード遷移、BASIC・ゲーム・音声聴感、
実パッド／Physicalキーボード、長時間負荷と小画面での操作感は未確認。
合成ROMのLCD flagsやMODE押下配送を、実ROMのモード動作確認とは扱わない。
