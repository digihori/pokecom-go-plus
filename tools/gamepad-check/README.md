# Gamepad Check

Android／iOSがゲームパッドとして公開する入力を調べる独立した診断アプリです。
Bluetooth接続は端末の設定で行います。ROMやPlayerの設定は不要です。

## Android

JDK 21とAndroid SDK 36を用意し、`ANDROID_HOME` をSDKの場所に設定します。
診断アプリの `android` フォルダーで次を実行します。

```sh
./gradlew assembleDebug
```

生成先は `tools/gamepad-check/android/build/outputs/apk/debug/GamepadCheck-debug.apk` です。
APKを端末へ渡してインストールするか、ADBでインストールします。
Android Studioでは `tools/gamepad-check/android` を開きます。
Gradleは同梱のWrapper（8.14.4）を使い、Gradle JDKにはJDK 21を指定してください。
既に開いている場合は、Gradle設定で配布元をWrapperに切り替えて再同期します。

## iOS

Xcodeで `ios/GamepadCheck.xcodeproj` を開きます。
GamepadCheckターゲットのSigning & Capabilitiesで自分のTeamを選び、必要に応じてBundle Identifierを変更します。
iOS 16以降のiPhoneを実行先に選び、Runします。

## Joy-Conの確認手順

1. ゲームパッドを切断した状態で診断アプリを開き、機種を選んで記録を開始します。
2. 端末のBluetooth設定で左だけを接続します。診断アプリへ戻り、区切りを記録してからボタンとスティックを操作します。
3. 右だけでも同じ確認を行います。
4. 左右を接続して、左右のボタンやスティックを同時に操作します。
5. 片側を切断して、残った側を操作します。
6. 記録を停止し、「診断結果を共有」からJSONをメールなどに添付します。どの手順で接続できなかったかも本文に添えてください。

記録はアプリを終了する前に共有してください。入力操作は診断アプリを前面にして行います。
接続しても一覧に出ない場合は、台数が0の状態でもログを共有できます。

## ログ

両OSともJSONにOS・アプリの版、選択した機器の世代、接続・切断、ボタン・軸の値と区切りを記録します。
時刻は記録開始からのミリ秒です。機器IDは端末や記録をまたぐ照合には使えません。
Androidは入力元の機器ID・ベンダーID・製品ID・入力ソースを、iOSはセッション内IDとGame Controllerの製品カテゴリを記録します。
軸の値はOSが返した値で、変化量が0.01未満の値は省略します。中立の0への復帰は記録します。
上限は2万イベントで、上限を超えると `truncated` が `true` になります。
送信は共有シートから手動で行います。

Androidは `InputDevice`、iOSは `GCController` の入力を記録します。
Player側で割り当てたキーや、Bluetoothの生の通信はログの対象外です。
