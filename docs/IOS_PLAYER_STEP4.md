# iOS PC-1245品質向上・手順4：ゲームパッドとPhysicalキーボード

2026-10-17。今回ユーザーが依頼した第4段階。PC-1245・iPhoneのみ。
手順1〜3の変更へ追加し、他機種・iPad・Logical文字入力・マクロは含めない。
ユーザーは現在実機を用意していないため、合成入力・Simulatorと周辺機器の実受入れを分ける。

## 実装と共有境界

- `hostInput`のGamepadInput、GamepadSettings、GamepadKeys、SingleGamepadSelection、HostKeyStateを再利用。
  タッチ・ゲームパッド・Physicalキーの所有者はApple facadeの同じHostKeyStateを使う。
  同じキーの複数所有者のうち一つを離しても、残る所有者の押下を維持する。
- AndroidのPhysicalKeyboardStateにあった短いタップ・同時押し・repeat・解放間隔の処理を
  OS APIなしの`hostInput/PhysicalKeyInput`へ移す。Androidは既存keycode変換を渡す薄いadapterを維持。
  PC-1350等のAndroid固有変換と公開API・既存テストを維持し、iOSの機種追加は行わない。
- iOSはGameControllerの接続通知、GCExtendedGamepad、GCKeyboardの物理コードを使用。
  handlerQueueをmainへ指定し、全Session操作はMainActorで直列化する。
  Core commonMainにOS/時刻/thread/filesystem APIを追加しない。
- パッド対象は十字4方向、フェイス4位置、L1/R1。スティック・トリガー・振動は対象外。
  フェイスは印字によらずSouth/East/West/Northで扱う。
- 使用機器は1台。複数接続時は選択し、非選択機器を無視する。
  選択機器が切断されても、すでに接続中の別機器へ自動では切り替えない。
  選択IDは起動中のみで、機器の恒久IDを保存しない。非対応profileは利用不可を表示する。
- ホーム「設定」→「コントローラ設定」。初期OFF・全入力未割当。
  1セットの有効状態と割当をUserDefaultsへ自動保存する。機種別・ゲーム別の保存は行わない。
  未接続でも編集可能。固定360×260の図を横スクロールでき、対応箇所からキーを選ぶ。
  設定画面では押下を点灯するだけでCPUへ入力しない。不明なキーIDは未割当とし、
  有効だがPC-1245にないキーの保存値は保持して送出を抑止・利用不可表示する。
- Controller Displayでも外部入力は利用可能。タッチキーの非表示と受信可否を混同しない。

## Physicalキーとライフサイクル

文字列・characters・IMEを変換せず、GCKeyCodeのUSB HID物理位置を使う。
英字、数字、テンキー、矢印、SHIFT左右、左Control/F1→DEF、Esc/Pause→BREAK、
Enter、Backspace/Delete→CLEAR、四則・小数点・等号を接続する。
Shift+数字などから記号を生成する処理やBASICのRUN自動入力は追加しない。
Option/CommandはOS操作に残し、その保持中の通常キーをCPUへ送らない。

UIKitの専用responderはUIKeyInputを実装せず、ソフトウェアキーボードを表示しない。
Player受信中の対応キーをUIボタンの移動・活性化へ流さず、未対応キーとOSショートカットはUIKitへ渡す。
実際の入力源はGCKeyboardで、UIKitから同じキーを二重送出しない。
GCKeyboardはOSが複数キーボードをcoalescedへまとめるため、物理機器ごとの区別・選択は行わない。

短いタップの最低保持と次の押下までの間隔は、既存MachineCatalogのサイクル数を使う。
実際にCPUが実行したサイクルだけで進め、壁時計タイマーは使わない。
SHIFT単独・同時押し・左右SHIFTの所有者を扱い、repeatで二重押下しない。

Pause・Reset・ホーム・背景・フォーカス喪失・モーダル・割当変更・機器切断で保持と未送出キーを解放する。
Playerの3点メニューはUIKitの表示/終了通知で受信を抑止し、操作せず閉じた場合も復帰する。
CPUはメニューを開いただけでは停止しない。
パッドはニュートラルを経るまで、キーボードは解放または新しい物理押下を確認するまで再送しない。
復帰時の現在キー状態をbaselineとして読み、保持中のキーを新しい押下にしない。
モーダル内の通常テキストフィールドは、Player用responderを退かせてOS入力を受ける。

## 検証

JDK 21、Xcode 26.6、iOS 26.5 Simulator。合成snapshot/コード注入は実キーボード・実パッドの確認ではない。
テストは揮発設定またはUUID専用UserDefaults domainと、使い捨てSimulatorを使用する。

| 対象 | 結果・区分 |
| --- | --- |
| 全体 `build :core:desktopTest :playerShared:iosSimulatorArm64Test` | 成功、578 tasks（134実行・444 UP-TO-DATE） |
| Core desktop | 436件、失敗0（Core変更なし・UP-TO-DATE） |
| Android unit | 43件、失敗0。共有化直後に再実行し成功、全体buildではUP-TO-DATE |
| 共有Player desktop / Android | 各60件を再実行、失敗0 |
| 共有Player iOS Native | 70件を再実行、失敗0。Apple facadeの所有権、Pause後の中立、割当変更、非対応キー、物理コードを含む |
| hostInput desktop / Android / iOS Native | 各12件を再実行、失敗0。新規の短い同時押し、解放間隔、resetとrepeat抑止を含む |
| iPhone向けunsigned build | 成功。deployment target iOS 16。実機実行・署名環境の確認を意味しない |
| Swift host/audio/input単体 | 33件成功。既存Host17＋audio/設定7＋音声/lifecycle3＋外部入力6 |
| UI | 6件すべて成功。Nativeメニューへ変更後の既存5件＋コントローラ設定/メニュー取消1件。後者は最終コードで再実行も成功 |
| 静的同梱情報 | help/privacy等9ファイルがソースとbyte一致 |
| project形式 / shell構文 / diff check | 成功 |

33件とは別に、同じ外部入力ケースでcallbackを意図的に外して状態を変える通知欠落の再現を補強し、最終個別再実行も成功した。
同じケースの再実行を新しい1件として加算しない。

ローカル成果物（Git対象外）:

- `iosApp/build/Step4-All.xcresult`: Swiftの十字キーsnapshot作成テスト1件は失敗を保持。UI6件はすべて成功。
- `iosApp/build/Step4-Verified.xcresult`: 修正後Swift33件＋コントローラ設定/NativeメニューUI1件、全成功。
- `iosApp/build/Step4-Resume-snapshot.xcresult`: callback欠落を明示する同一ケースの最終個別確認、成功。
- `iosApp/build/Step4-DeviceCompile`: 最終ソースのiPhone向けunsignedビルド。

既存Simulator/ROM Library/実設定を操作せず、テスト用Simulatorは終了時に削除した。残存する `PGP CI isolated` は0台と確認した。

Swift検証では独立GameController snapshotを使い、登録済みcallbackへ状態変更を注入する。
十字キーはsnapshotのd-pad軸APIで設定し、ボタン4位置と肩2個を含む10入力の読取りを確認する。
背景中に通知がなかった状態変化も、復帰時に実profileの現在値をbaselineとして読んで再送を防ぐ。
Physical側はHIDコード注入でSHIFT、同時押し、repeat、短い保持、メニュー/モーダル/背景/フォーカス/Resetを確認する。
GCKeyboardの実接続や、UIKit responderに実キーボードから届くイベントは未確認。

最初のSwift一括実行では、新規の10入力テスト1件が十字キー4方向で失敗した。
snapshotの方向ボタンへ個別setValueしても方向状態が反映されないため、d-padのsetValueForXAxis/yAxisへ修正した。
実装のAPI読取りと、合成snapshotの作り方を区別し、この失敗を含む一括実行全体を成功とは記録しない。

## 未確認・第5段階の受入れ

- iPhone実機のUSB/Bluetooth接続、機器差・配列差、切断/再接続、複数台選択、フォーカス・OS操作。
- 実キーボードの単独/左右SHIFT、同時押し、短タップ、repeat、IMEを使わない物理入力とOSショートカット。
- 実ROMによるゲームの短押し・保持・キー走査、BASICでの高速打鍵、CPU5倍率それぞれでの操作感。
- 最低iOS 16での実行、実機ノッチ/ホームインジケータ/文字サイズと両横向き。
- 機材準備時に署名・Developer環境を再確認する。過去記録から現在の環境を断定しない。
- 本段階終了時点ではPC-1350スキンのユーザー修正を保持し、commit/pushを明示指示待ちとした。packageVersionは1.3.1を維持。

## 出典

共有処理は本リポジトリのhostInputとAndroid adapterから移したもので、外部参照プロジェクトへの依存はない。

- [Apple GCExtendedGamepad](https://developer.apple.com/documentation/gamecontroller/gcextendedgamepad)
- [Apple GCKeyboardInput](https://developer.apple.com/documentation/gamecontroller/gckeyboardinput)
- [Apple GCKeyboard callback](https://developer.apple.com/documentation/gamecontroller/gckeyboardvaluechangedhandler)
- [Apple controller snapshot](https://developer.apple.com/documentation/gamecontroller/gccontroller/withextendedgamepad%28%29)
- [共有ゲームパッド仕様と過去の段階別記録](GAMEPAD_DESIGN.md)
