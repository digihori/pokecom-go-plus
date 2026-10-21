# iOS 機種追加・手順3：PC-1261系

更新日: 2026-10-21

## 実装

- iOSの明示的な対応一覧にPC-1260／1261を追加。ホームのPC-1261系カードでモデルを選び、
  初期値はPC-1261とする。PC-1262は今回の対応一覧に含めない。
- PC-1260／1261は`rom-library/pc-1261.json`のROMを共用する。
  2モデルいずれのpgromも構成・サイズ・hashを検証した後、共有の`forModel`で実行モデルへ対応付ける。
  保存packageはpc-1261へ正規化し、ROM bytesは変更しない。別シリーズは拒否する。
- 単体ROMは64 KiBのアドレス空間imageのみ。既存Coreの`Pc1261FlatRomImporter`を使用し、
  internal 8 KiBとexternal 32 KiBを正しい領域から取り込む。32 KiB imageは拒否し、案内も64 KiBとする。
- モデル選択、選択／起動モデルの独立保存、プレイ時の切替確認、Session公開前のatomic保存、
  取消・失敗時保護、シリーズ共通の置換・削除は手順2の経路を使用する。
- 標準RAMはPC-1260が5800–67FF、PC-1261が4000–67FF。BASIC/DMPは実行モデルのS1定義と範囲を使う。
  RAM拡張、ROM patch、セーブステートは追加しない。
- LCDは24桁×2行。共有`PlayerDisplayFrame`の143×15論理格子を使用し、2行目の開始に1ドット分の行間を置く。
  スキン・52キー・LCD状態領域は既存共有PC-1261定義を使用する。
- RUN／PRO／RSVに対応。状態表示のRESERVEはRSV、SMALLはSMLと描画・読み上げする。
  accessibilityの識別子は従来どおりenum名を保持し、テストや状態追跡との整合性を維持する。
- Controller Display、両横向き、Pause・背景・モーダル時の入力解放、パッド・Physicalキーは共有経路を使用する。
- スキン画像は既存PGP Androidの`skin_pc1261.png`とSHA-256一致。
  外部コードやbuild依存、ROM/ROM由来fixtureを追加せず、Coreの機種動作も変更しない。

## 検証

JDK 21、Xcode/iPhone Simulator。独自のLCD書込み命令とJPループを使い、実機firmwareは使用しない。

- `build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。
  578 tasks中26実行、552 UP-TO-DATE。Core desktop490件は失敗0（今回はUP-TO-DATE）。
  共有Player iOS Native138件を再実行し、失敗0。
- Nativeの追加ケースで両モデルの標準RAM、S1 BASIC往復と容量超過時保護、DMPのRAM／非RAM境界、
  2行の四隅ドット・論理行間・スナップショットの不変性、状態表示、3モード、パッドとPhysicalの
  同一キー所有、Pause・Reset、全許可キー、32 KiB拒否を確認した。
- Swiftの追加ケースで全4通りの同シリーズpackage対応付け、ROM bytes保持、hash不一致・別シリーズ・
  32 KiB拒否、共有保存先、実際のROM登録、切替確認と取消、独立復元、置換取消／書込み失敗の保護、
  置換・共有削除を確認した。
- SkinViewの追加ケースで画像resource、24桁×2行のaccessibility、52キー、RSV／SML表記、
  Controller Displayでのキー非表示とLCDの収まりを確認した。
- Swift単体39件、失敗0。実音声デバイス依存の1件はCI修正の方針に従って除外した。

- UIは既存7ケースの成功と、新規PC-1261系ケースの修正後個別再実行の成功を確認した。
  新規ケースはPC-1260への切替取消・確定、2行LCDの状態表示、RUN/PRO/RSV、通常/Controller Display、
  両横向き、選択と実行の独立したプロセス再起動後復元、PC-1261への切替、背景復帰PAUSED、Resetを確認する。
  通常表示とController Displayの添付画像を目視し、2行の行間・四隅のドット・状態文字が収まることを確認した。
- 最初の全UI実行は、新規ケースが下方カードへ移動できず失敗した。全体を成功とは記録しない。
  テストのswipe対象をApplicationからScrollViewへ変更し、起動後の向きの安定待ちを追加した。
  アプリ本体を変更せず、同ケースの個別再実行が成功した。
- 結果（Git対象外）：`iosApp/build/CI-TestResults-20261021-094858.xcresult`はSwift39件成功／UI7成功・1失敗。
  `iosApp/build/CI-TestResults-20261021-095746.xcresult`は修正後PC-1261系UI1件成功。
  同ケースの再実行を新しい1件として加算しない。
- 同梱ヘルプはソースとbyte一致。Xcode project形式と`git diff --check`も成功。
  隔離Simulatorの検証スクリプトは正常終了した。

## 残る受入れ

実ROMの起動・MEM・S1 BASIC・日本語・音声聴感、iPhone実機・最低iOS、実パッド／Physicalキーボードの
受入れは未実施。合成Simulator検証と区別する。PC-1250の既知MEM表示など前段階の保留も継続する。
