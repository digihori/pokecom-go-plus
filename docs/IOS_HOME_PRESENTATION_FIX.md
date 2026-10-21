# iOSホーム表示・画面方向・メニュー修正

2026-10-21。ユーザーの全機種動作・音声再生OKを受けた後のホーム画面修正。

- AndroidのPlayerHomeScreen／PlayerHomeLayout／PlayerMachineCatalogを仕様の参照元とする。iOS homeMachinesを名前順に並べ、ROM管理のシリーズ一覧も同じ順序にする。
- タイル／リストを追加。初期タイル、画面幅で列数を調整し、リストは1列で操作ボタンを右に置く。選択モデル・ROM保存・プレイ操作は共通。表示形式はiOSの既存の保存設定にhome.layoutとして保存し、テストでは独立UserDefaultsを使用する。
- Info.plistでportraitを許可し、UIApplicationDelegateの画面方向ポリシーで機種画面だけlandscapeに制限する。ホーム・設定・ROM管理・プログラム入出力・情報画面はportrait／landscapeを許可し、機種画面に戻る際はlandscapeにする。iOS 16以降のsetNeedsUpdateOfSupportedInterfaceOrientations／UIWindowScene.requestGeometryUpdateを使用する。Coreの実行・音声・入力処理は変更しない。
- ホームのUIKit UIButton埋込みをSwiftUI標準Menuに置換し、ellipsisラベルを明示する。ROM管理の開閉後もライト／ダークで3点を描画する。機種画面の入力解放付きメニューは維持する。
- サウンドのブツブツ感はユーザーが一旦許容した後続調整項目。今回音声処理の調整は含めない。

## ユーザー確認・今回の完了

2026-10-21、修正後の画面をユーザーが一通り確認し、今回のiOS機種対応・ホーム画面修正を完了とした。機種画面のみ横固定、他画面の縦横表示、名前順、タイル／リスト切替、ROM管理後の3点メニューを今回の確定仕様とする。音声再生と全対応機種の動作確認も受領済み。音声のブツブツ感の調整は後続課題として残す。

端末・OS・ROM版・周辺機器別の詳細は未記録のため、この確認を最低OSや全周辺機器・署名付き配布の完了へ拡張しない。

## 検証

- JDK 21の`build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。578 tasks中26実行、552 UP-TO-DATE。iOS Native 143件成功、Core desktop 490件の同一コード結果はUP-TO-DATE。
- Swift単体49件成功。新しい設定テストで初期タイル、リスト保存、モデル選択との独立、不正保存値のフォールバック、10シリーズの名前順を確認。
- 隔離iPhone 17 Pro／iOS 26.5で対象UI 5ケースの成功を確認。Filesの選択・取消、1251系切替・復元、機種画面の横固定／Reset／背景停止、ホームのメニュー開閉、新規の縦横・表示切替・保存を確認。
- 最初の新規ケースは「ROM管理を開いた後もプレイで自動実行する」と誤って期待して失敗した。既存仕様ではROM管理後の入場はPAUSEDを保つため、テストをPAUSED確認→Run操作へ修正し、同じiPhone 17 Proで再実行成功。アプリの既存の停止維持仕様は変更していない。
- 隔離iPhone SE（第3世代）／iOS 26.5でOS appearanceをsimctlによりdark／lightへ明示的に切り替え、新規UI 2ケースが成功。縦向きの2列タイル／1列リスト、横向きタイル、再起動後の形式保存、ROM管理複数回開閉、CPU速度画面の縦向き、機種画面の横固定、セーブ画面の縦向きと閉じた後の横向き、ホーム復帰時の縦向きを確認。
- ライトのiPhone 17 Proと実OSダークのSEの添付画像で、ROM管理後も3点が残ることとカードの配置を目視確認。起動引数AppleInterfaceStyleだけではダークにならなかったため、それを除去し、SEのOS設定を切り替えて確認した。
- generic iPhone／arm64／Release unsignedビルド成功。実機への署名・インストールは実施していない。SimulatorとReleaseのInfo.plistでportrait＋左右landscapeを許可し、同梱ヘルプが原本と一致することを確認。実画面の向きはdelegateが制御する。
- `git diff --check`、Info.plist／Xcode project検証、更新文書の相対リンクを確認。通常のROM Library・設定・Simulatorは操作せず、テスト用端末は終了時に削除した。

実時間CoreAudioノードの既知のデバイス依存1件は既存CI方針で除外し、オフライン音声・ライフサイクルの単体テストは実行した。

### Git対象外の結果

- `iosApp/build/Home-Presentation-Verified.xcresult`: Swift49成功、UI4成功・新規1失敗。
- `iosApp/build/Home-Presentation-Retry.xcresult`: 期待値修正後の新規UI1成功。
- `iosApp/build/Home-SE-Dark.xcresult`／`Home-SE-Light.xcresult`: SEのOSダーク／ライトそれぞれ1ケース成功。
- `iosApp/build/Home-Presentation-Device/Build/Products/Release-iphoneos/PokecomGOPlayer.app`: コンパイル確認用の署名なしRelease成果物。

再現する場合、使い捨てSimulatorで`xcrun simctl ui <device-uuid> appearance dark`／`light`を切り替え、`testHomeMenuSurvivesRomManagementInDarkAppearance`／`testHomePortraitTilesListPersistenceAndLandscapeOnlyPlayer`を実行する。
