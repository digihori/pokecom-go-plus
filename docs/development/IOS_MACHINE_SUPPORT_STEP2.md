# iOS 機種追加・手順2：PC-1251系

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-21

## 仕様

- PC-1245を維持し、PC-1250／1251／1255をiOSの明示的な対応機種へ追加する。
  ホームはPC-1245とPC-1251系の2カード。PC-1251系の初期モデルはPC-1251。
- PC-1251系のモデルはプルダウンで選択する。選択変更は設定だけを更新し、
  起動中SessionとRAMを変更しない。別モデルの「プレイ」で破棄確認し、確定後に新Sessionを起動する。
  取消、読込み・検証失敗では元のSessionを保持する。失敗時は停止を維持する。
- 選択モデルと実行モデルを別々に保存する。再起動時は最後の実行モデルを停止状態で復元し、
  プルダウンは最後の選択値を維持する。ホームは「起動中：…」で実際のモデルを示す。
- ROMはシリーズ共用。保存先は`rom-library/pc-1251.json`で、aliasごとに複製しない。
  pc-1250／1251／1255いずれのpgromもmanifest・構成・サイズ・hashを検証後に受け付ける。
  共有の`PlayerMachineCatalog.forModel`で実行モデルへ対応付け、保存packageはpc-1251へ正規化する。
  ROM bytesをpatchしない。PC-1245など別シリーズのpackageは拒否する。
- 32/64 KiBの単体ROMは、選択したシリーズを明示して既存CoreのImporterへ渡す。
  同じサイズから機種を自動判定しない。pgromも選択シリーズとの整合性を確認する。
- ROM管理はシリーズ単位で切り替える。置換・削除はシリーズ共通保存へ適用する。
  同シリーズの別名モデルが起動中でも削除時はSessionを破棄する。別シリーズの保存とRAMは保持する。
  保存成功後だけ新しいSessionを公開し、取消・書込み失敗では従来の保存ROMとSessionを保持する。
- タイトルとプログラム入出力は実行モデルの定義を使う。共有SessionFactoryはHARDWARE RAMを使用し、
  PC-1250はC000–C7FF、PC-1251はB800–C7FF、PC-1255はA000–C7FFの標準構成とする。
  既存CoreのRAMミラーは維持するため、標準領域外の全アドレスが無効という意味ではない。
- LCDは24桁×1行、スキン・タッチ配置・キーはシリーズ共通。RUN／PRO／RSVに対応する。
  Controller Display、速度・音声、パッド・Physicalキーは既存共有経路を再利用する。
- スキン画像はPGP内のAndroid用`skin_pc1251.png`と同じ画像をiOSのresourceへ追加した。
  外部リポジトリからのコード移植・依存追加、ROM/ROM由来fixtureの追加はない。

## 検証

JDK 21、使い捨てiPhone Simulator。合成ROMは独自のJPループ等で、実機firmwareではない。

- 全体`build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。
  578 tasks中28実行、550 UP-TO-DATE。Core desktop490件は失敗0（今回はUP-TO-DATE）。
  共有Player iOS Native137件は再実行して失敗0。
- Nativeの追加ケースで3モデルの標準RAM情報とRAM入出力、ROM領域拒否、失敗時無変更、
  BASIC往復・容量超過時保護、モード、パッド・全許可キーとPauseでの解放を確認。
- Swiftの追加ケースでシリーズ共通保存、同シリーズ9通りのpackage対応付け、byte保持、
  別シリーズとhash不一致の拒否、切替取消・確定・読込み失敗、選択と実行の独立復元、
  別名モデルでの共有ROM削除とPC-1245保存保護、不正な保存設定のfallbackを確認。
- `--unit`のSwift37件、失敗0。実デバイス音声出力1件はCI修正の方針に従って除外。
  結果は`iosApp/build/CI-TestResults-20261021-080708.xcresult`（Git対象外）。

- 全体Simulator検証でもSwift37件とUI7件がすべて成功。デバイス音声出力1件は明示的に除外した。
  新しいUIケースでPC-1250への切替取消・確定、RUN/PRO/RSV、Reset、Controller Display、両横向き、
  PC-1255を選んだままアプリを再起動して実行モデルPC-1250を独立復元すること、
  PC-1255／1251への切替と背景復帰PAUSEDを確認した。既存のFiles入出力等6ケースも成功。
  結果は`iosApp/build/CI-TestResults-20261021-080852.xcresult`（Git対象外）。
  添付のPC-1250画面画像を確認し、タイトル・スキン・LCD・キーの配置を確認した。
- iOS同梱help/privacy/guideはソースとbyte一致、PC-1251画像は既存Android画像とSHA-256一致。
  Xcode projectの形式検証、shell構文、`git diff --check`も成功。
  使い捨てSimulatorの検証スクリプトは正常終了した。

最初のNative実行は、新しいテストが標準RAM直前のアドレスを無効と仮定して失敗した。
既存のRAMミラーを保持する仕様に合わせ、標準RAM情報とROM領域の拒否を確認する形へ修正し、
上記の全体再実行で成功した。容量判定の仕様やCoreのmirrorを変更していない。

## 保留

PC-1250のMEMが1486ではなく3486と表示される既知事項は、Androidでの受入れ方針を引き継いで保留する。
CoreのmirrorとROMの容量判定の寄与は未切り分けで、正常仕様またはROMだけの問題とは断定しない。

iPhone実機、最低iOS、実ROMの起動・MEM・BASIC・音声聴感、実パッド・Physicalキーボードの受入れは未実施。
Simulatorの合成検証を実ROM・実周辺機器の確認として扱わない。
