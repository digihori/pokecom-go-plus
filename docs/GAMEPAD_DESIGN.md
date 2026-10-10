# Studio／Player ゲームパッド設計・実装

調査・実装日: 2026-10-11。
StudioとAndroid Playerに実装済み。iOSアプリは未実装のため後続対象。
macOS arm64のSDL仮想パッドと配布版起動・設定画面を確認済み。
ユーザーによるStudioのコントローラー入力・割当設定の動作確認済み。
Windows／Linuxの実行、Android実機、他のコントローラー機種との互換性は後続検証。

## 対象と推奨方針

OSがUSB／Bluetooth接続済みのコントローラーを認識していることを前提とする。
アプリ内でペアリング、USB通信、Bluetooth通信は実装しない。
ただしOS認識済みでも、ゲームパッドAPIの対応範囲と機器のマッピングによって利用可否は異なる。

対象は十字キー4方向、フェイスボタン4個、L1/R1の計10入力。
アナログスティック・トリガー入力は対象外。コントローラーは1台のみ使用し、割当設定は1つだけ保存する。
設定を保存するたびに以前の設定を上書きする。機種別・機器別・ゲーム別の保存は行わない。
各入力から現在のポケコン機種の物理キー1個または「未割当」へ変換する。
例: 左→NUM_4、右→NUM_6、上→NUM_8、下→NUM_2。
マクロ、連射、振動、トグル押下、OS全体へのキー送出は初期対象外。
キーを押した間は保持し、離したときに解放する。

DesktopはSDL3＋LWJGL 3.4.1、Androidは標準InputDevice/KeyEvent/MotionEventを使用する。
将来のiOSはGameControllerを用いる案。接続済み入力の取得のみを行う。

## 調査時点のコード構造

- desktopAppはJVM 21／Compose Desktop。配布対象はmacOS、Windows、Linux。
- Main.ktにはSettingsウィンドウがある。保存はDesktopUiPreferencesのjava.util.prefs.Preferences。
- DesktopKeyboardInputはLOGICAL（文字を機種別キー列に変換）とPHYSICAL（直接押下／解放）を持つ。
  Alt+↑／↓はコマンド履歴呼出し。文字列キューはCoreのKeyInputQueueがエミュレーションのサイクルで処理する。
- DesktopEmulatorRunner.pressKeyは受理したRUNモード入力をDEF以外コマンド履歴へ追加する。
  RUNはポケコンの操作モードであり、ゲーム実行中を意味する判定ではない。
- PlayerはAndroidプロトタイプが存在する。playerSharedはKMPの実行／表示モデルで、共通UIモジュールではない。
  iOS targetはあるがiOSアプリは未実装。
- Android MainActivityはタッチ入力をPocketKeyの集合で管理する。この集合だけでは同じキーを押す複数入力源を区別できない。
- CoreにはOS、UI、デバイス、スレッドを持ち込まないというAGENTS.mdの制約がある。

## ライブラリ比較

| 候補 | 評価 | 判断 |
|---|---|---|
| SDL3＋LWJGL SDL bindings | 標準化された十字キー／フェイス／肩ボタン、機器マッピング、接続管理を利用可能。ネイティブ配布が必要 | Desktop第一候補 |
| GLFW＋LWJGL | gamepad mappingとjoystick取得が可能。初期化・イベント処理のmain thread制約とmacOS/AWT共存を検証する必要 | 第二候補 |
| JInput | Java入力デバイスAPI。標準ボタン位置への正規化、再接続、対象CPUのnative対応を別途確認する必要 | 今回は優先しない |
| OS APIをDesktop各OSで直接使用 | 最適化できるが3種類の実装とJVM bridgeを維持する負担が大きい | 初期案では見送る |
| Android／Apple標準API | モバイルホストに自然に統合できる | 各Playerで採用 |

LWJGL 3.4.1にJava bindingと各OS/CPU nativeのバージョンを固定している。
署名済み配布物での確認は別途必要。ライセンス文書はdocs/licensesと配布物に含める。

SDL_InitおよびSDL_PumpEventsにはmain thread制約がある。
SDL_UpdateGamepadsは任意スレッドから呼べるが、この事実だけで初期化・接続検出をワーカーへ移せるとは判断しない。
動画／音声サブシステムは追加せずgamepad関連のみ初期化する。
SDLを別ウィンドウの描画エンジンにはしない。
macOSはlibdispatchのmain queueで初期化・更新・終了を実行し、AWT EDTへ不変のsnapshotを返す。
その他のDesktop OSではアプリのイベントスレッドでSDLを使用する。
macOSの小型JavaランタイムにはLWJGLが利用するjdk.unsupportedを明示的に追加している。

## OSごとの取り込み

| 対象 | 取り込みと注意点 |
|---|---|
| macOS Studio | SDLで標準入力へ変換。Apple Silicon／Intel向けnative、Compose/AWT共存、署名と同梱を検証 |
| Windows Studio | SDLで取り込み。USB／Bluetooth、機器の動作モードで認識・識別が変わるケースを検証 |
| Linux Studio | SDLで取り込み。配布環境の入力デバイス権限、必要な共有ライブラリ、接続検出を検証 |
| Android Player | ComposeのonPreviewKeyEvent／ActivityのonKeyDown・onKeyUp／onGenericMotionEventで取り込み、InputDeviceListenerで追加／削除監視。SOURCE_GAMEPAD／JOYSTICKを確認し、通常キーと分離 |
| 将来のiOS Player | GameControllerの接続通知とextendedGamepadを使用。非対応profileでは不足する入力をUI表示 |

Androidの十字キーはKeyEventとHAT軸の双方を想定し、同一方向の重複通知を正規化する。
ボタンのOSリピート通知で二重押下しない。未使用入力を不用意に消費せずシステム操作と共存する。

## 設定画面

Studio: Settings→Controller。Player: 3点メニュー→コントローラー設定。
操作順、文言、設定データ、図の位置関係を揃える。
StudioとAndroidはどちらもComposeだが、現在のplayerSharedにはCompose依存がないので、
hostInputに共通モデル、controllerUiにStudio／Androidの共通Compose画面を置く。
将来のiOS UIにもhostInputの同じモデルを渡せる。

画面構成:

1. 有効スイッチ、使用するコントローラー1台の選択、接続状態。
2. 360×260dpの固定サイズのコントローラー図。十字キーとABXY、L1/R1の近くに現在の割当キーを表示。
   ウィンドウ幅に応じて伸縮させず、狭い画面では横スクロールする。
3. 図の箇所をクリック／タップするとキー選択パネルを表示する。
4. キー選択は現在の機種のポケコンキー一覧から選ぶ。
   「未割当」、数字、英字、記号、SHIFT／DEF／ENTER／BREAKなどを選べる。
5. 実際のボタンを押すと図の対応箇所を点灯し、そのボタンの割当を確認できる。
6. 割当や有効／無効の変更時に自動保存し、唯一の設定を上書きする。保存・キャンセルボタンは設けない。

設定中の入力はテスト表示専用とし、エミュレータへ送らない。
機器未接続でも割当を編集できる。不足ボタンは利用不可として示す。
ABXYの印字はXbox／Nintendo等で位置が異なるため、内部IDはSouth/East/West/Northとし、
UIは標準位置のA/下、B/右、X/左、Y/上を表示する。機器の印字が異なる場合も位置で選択できる。
一般的なコントローラーの図をCompose描画等で作り、各箇所をタブ移動できる通常のUI要素にする。

初回は全入力を未割当とし、ユーザーが指定する。
任意キーはその機種に存在する物理キーを意味する。例えばSHIFT＋キーは2入力を同時に押して作れるが、
1ボタンへ複数キー列を登録する機能は初期対象外。

## 共通入力管理

流れ: OS adapter → 標準ボタン状態 → 保存された割当 → 入力源付き押下管理 → EmulatorSession。
hostInputという共通KMPモジュールを追加し、StudioとplayerShared双方から利用する。
そこに正規化モデル、割当、所有者管理、設定モデルを置き、OS API／保存／描画は各ホストへ残す。
CoreのpressKey/releaseKey APIは再利用する。

入力源はkeyboardの物理キー、画面キー、選択中のgamepadのボタン、キー列queue等を区別する。
PocketKeyごとに所有者集合を持ち、0→1でpress、1→0でreleaseを発行する。
キーボードで4を保持しながらパッドの左→4を押して離しても、キーボードの4は保持される。
同一パッドの複数ボタンが同じキーを指す場合も同じ規則にする。
ゲームパッドだけ導入しても問題は解消しないので、既存画面キー・PHYSICALキー・
KeyInputQueueもこの管理を通している。

キー列queueと物理保持が重なる場合、キーの保持を優先する。
同じキーのqueueによるタップは物理保持中には別の押下エッジにならないことを仕様として明示する。
貼付け／履歴再生を確実に完走させたい場合は、物理キーをすべて離して開始する。
Desktopでは約8ms間隔でボタン状態を読む。短いパッドタップがCPUのスキャン間隔より短い問題は
実機比較で確認する。現時点ではタップの最小保持補正は実装せず、押下／解放を直接渡す。

接続切断、アプリのフォーカス喪失、設定開始、有効解除、機種／セッション変更、reset／停止で対象入力を解放する。
復帰時は一度ニュートラルになるまで再押下しない。Androidでフォーカス外の解放通知が欠けた場合は、
そのボタンを押して離すことで再び入力可能になる。割当変更前も旧割当を解放する。
コールバックからSessionを直接並行操作せず、既存ホストのSession実行スレッドへ順序付きで渡す。
button down/upを落とす間引きはしない。

## キーボード・コマンド履歴との整合性

ゲームパッドはPocketKeyへ直接変換し、StudioのLogical/Physical設定やIME、OSショートカットを経由しない。
パッド上→NUM_8なら8、パッド上→UPならポケコンの↑。Alt+↑の履歴操作を呼び出すことはない。
ゲームパッドによる履歴呼出しは初期機能に含めない。

現在のrunner.pressKeyをそのまま使うとRUNモードのゲーム操作が履歴に入り、
割当ENTERがそれを確定する。よって押下処理と履歴記録を分離し、入力originを渡す。
パッド操作は履歴へ記録しない。既存キーボード／画面キーの履歴規則は維持する。
有効なパッド押下を受理したら履歴の未確定入力・呼出し位置を解除し、確定履歴は保存する。
これにより以前の文字とゲーム操作の後のENTERが誤って一つの履歴になるのを防ぐ。
同じキーの他入力源による重複押下は、新しいキー入力として履歴へ追加しない。
Playerには現在コマンド履歴がないので、共有入力層は履歴機能を要求しない。

## 保存・使用するコントローラー

保存する設定は、有効／無効と10入力の割当だけとする。
キーは表示文字でなくPocketKeyの安定IDで保存する。機種を切り替えても同じ割当を使用し、
その機種に存在しないキーは入力せず、設定画面に利用不可と表示する。
設定は各アプリのユーザー設定として1つだけ保存し、変更のたびに全体を自動保存して上書きする。
プロファイル名、機種別保存、機器別保存、import/export、自動同期は設けない。
StudioとPlayerで設定方法は揃えるが、保存先は各アプリで独立する。

Desktopは既存のPreferencesへenabledとボタン別のキーIDを保存する。
AndroidはSharedPreferences、将来のiOSはUserDefaults相当を利用する。
未知キーは未割当として扱い、壊れた値で起動不能にしない。

接続されている機器が1台なら自動選択する。複数接続されている場合のみ設定画面で1台を選び、
他の機器の入力は無視する。同時使用は行わない。
機器の永続的な照合・保存は行わず、選択はアプリ起動中だけ保持する。
選択機器が切断されたら押下を解放し、他の機器へ自動では切り替えない。
再接続後、1台だけなら再選択し、複数ならユーザーが選ぶ。割当設定はそのまま使う。

## 十字キーとアナログ入力

スティック、アナログトリガー、デッドゾーン設定、斜め入力の補正は実装しない。
十字キーがOSからHAT軸として報告される機器では、その値を十字キーの押下／解放へ変換する。
これはデジタル十字キーの取得方式への対応であり、スティック入力対応には含めない。
十字キーを含め各ボタンは通常の独立したキーとして扱い、特殊な同時押し補正は行わない。

## 実装手順と後続検証

1. Desktopだけの小規模検証: Compose起動中のSDL初期化、10入力、押下／解放、切断／再接続、終了、native同梱。
   macOSでmain thread/AWTが共存できることを確認してからライブラリを確定する。
2. 共通入力モデルと所有者管理。既存キーボード／画面キー／queueとの接続、履歴origin分離。
3. StudioとAndroidの同じ設定フロー、10入力の割当、1設定の上書き保存、1台使用。
4. iOSアプリ実装時に標準API adapterと同じ設定フローを追加。

意味のある単体テスト: 2入力源が同じキーを保持、切断で当該所有者だけ解放、
割当上書き、重複通知、履歴混入防止、保存／再読込／破損／未知キー。
既存DesktopKeyboardInput／DesktopCommandHistory／KeyInputQueueの回帰も確認する。
実機: Xbox／PlayStation／Nintendo系または汎用機器、USB／Bluetooth、OS別、選択していない機器からの入力抑止、
フォーカス復帰、設定中の抑止、ゲームの保持入力と短タップ、複数キー同時スキャン。
機器対応数だけでなく、各ポケコン機種のキーマトリクス／ROMが同時押しをどう認識するかを確認する。

検証: JDK 21の全体build、共通入力テスト、既存Core／Studio／Playerの回帰テスト、Android APK生成、
macOS createDistributableと設定画面操作を実施。ユーザーによるStudioの実コントローラー動作確認済み。
キー選択UIの追加改善は今回の範囲を完了した上での後続改善として保留。

native検証はGUIのあるDesktop上で
`PGP_TEST_GAMEPAD_NATIVE=1 ./gradlew :desktopApp:test --tests '*SdlGamepadBackendTest'`
を実行する。SDLの仮想gamepadで10ボタン・analog無視・解放・切断を確認し、
環境変数がない通常のCIではこのテストだけskipする。

## 一次資料

- [SDL gamepad API](https://wiki.libsdl.org/SDL3/CategoryGamepad)
- [SDL mapping](https://wiki.libsdl.org/SDL3/SDL_AddGamepadMapping)
- [SDL初期化とthread制約](https://wiki.libsdl.org/SDL3/SDL_Init)
- [SDLイベント処理](https://wiki.libsdl.org/SDL3/SDL_PumpEvents)
- [SDL手動gamepad更新](https://wiki.libsdl.org/SDL3/SDL_UpdateGamepads)
- [LWJGL対応binding](https://github.com/LWJGL/lwjgl3)
- [LWJGL SDLリリース配布一覧](https://www.lwjgl.org/browse/release/3.4.0/bin/lwjgl-sdl)
- [GLFW入力](https://www.glfw.org/docs/latest/input)
- [GLFW初期化／thread制約](https://www.glfw.org/docs/latest/intro_guide.html)
- [JInput](https://github.com/jinput/jinput)
- [Androidコントローラー入力](https://developer.android.com/games/sdk/game-controller/controller-input)
- [Apple GameController](https://developer.apple.com/documentation/gamecontroller/gcextendedgamepad)
- [Apple公式profile入力の参考](https://github.com/apple/game-porting-toolkit/blob/main/game-porting-skills/skills/using-game-controller/reference/profile-based-input.md)
