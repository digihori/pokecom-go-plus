# iOS PC-1245品質向上・手順3：音声とCPU速度

2026-10-17。今回ユーザーが依頼した第3段階。手順1/2の変更へ追加し、外部入力・他機種は含めない。
過去の提案・段階別記録とは区別する。ユーザーは現在実機を用意していない。

## 実装

- 共有PlayerRunnerのtickが消費したPCMをApple facadeからコピーしてSwiftへ渡す。
  sampleRate・frameCount・sampleCount・倍率適用後のplaybackRateを保持する。
  空samplesにも経過した無音framesがある。消音中もRunnerは毎tick drainする。
- Swiftの専用直列workerがAVAudioEngine/AVAudioPlayerNodeを所有する。
  Session操作はMainActorのみ。workerにはSwift所有のPCMコピーだけを渡す。
- 送信待ちとnodeへ予約済みの合計を100ms相当以下に制限する。
  超過時は古いPCMを破棄して新しい世代へ切り替える。worker停止中も予約queueは有界で、
  offerごとにPCMを保持するDispatch closureを増やさない。
  OS/ハードウェア内部のバッファ時間まで100ms以内と保証するものではない。
- アイドルの無音だけではdeviceを開かない。音間の無音はframes分のゼロとして送る。
  最後の有音bufferが再生完了すると自然終了する。継続無音でsessionを保持し続けない。
- Pause・Reset・ホーム・背景・Fault・速度変更・サウンドOFFで旧世代を無効化し、
  workerでnode/engine停止・session非活性化を行う。古いcompletionは新世代へ影響しない。
  停止要求はMainActorを待たせず、現在の短いdevice処理後にworkerが反映する。
- CPU速度0.8／0.9／1.0／1.1／1.2倍は共有PlayerExecutionSpeedを使用。
  音声レートは17640／19845／22050／24255／26460Hz。
  音高・テンポも倍率に追従する。速度変更でRAMをresetせず、停止CPUを再開しない。
  Reset・ROM復元/置換にも設定を保持・適用する。
- ホームの設定順はROM管理→CPU速度→サウンド→コントローラ設定（後続）。
  初期値は1.0倍・サウンドON。UserDefaultsへ設定を保存し、不明な速度IDは1.0倍へ戻す。
  probe/Host単体テストは揮発設定、保存テストは専用UUID domainを使う。
- 出力失敗は音声だけを停止し、CPUをFaultにしない。表示はSwift UIで扱う。
  RunまたはサウンドONで再試行する。ゲームパッド/Physical入力は第4段階。

## 確定した音声方針

今回の選択肢提示に対してユーザーが「消音モードに従い、他アプリ音声と共存」を選択。
AVAudioSession categoryはambient。以前の未決提案を承認済みと読み替えたものではない。
背景再生・マイク・録音・ネットワーク音声は追加しない。

割込み開始ではPCMを破棄する。終了通知のshouldResumeがあり、音声がまだ実行対象なら、
新規PCMだけを受け入れる。Pause/背景/ホーム後は終了通知で再開しない。
ヘッドホン等の旧出力先消失、media servicesの喪失/復元、出力エラーは明示再試行を要求する。
その他の経路変更では旧PCMを捨て、既に再生対象なら次のPCMから出力する。

Apple公式仕様：[ambient](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/ambient)、
[AVAudioPlayerNode](https://developer.apple.com/documentation/avfaudio/avaudioplayernode)。

## 検証区分

JDK 21、Xcode 26.6、iOS 26.5 Simulatorで検証。Simulatorの音声送出完了は聴感・実機の確認とは区別する。

| 対象 | 結果・区分 |
| --- | --- |
| 全体 `build :core:desktopTest :playerShared:iosSimulatorArm64Test` | 成功。578 tasks（26実行・552 UP-TO-DATE） |
| Core desktop | 436件、失敗0（変更なし・UP-TO-DATE） |
| Android unit | 43件、失敗0（変更なし・UP-TO-DATE） |
| 共有Player desktop / Android | 各60件、失敗0（変更なし・UP-TO-DATE） |
| 共有Player iOS Native | 68件を新規実行、失敗0。独立生成の発振コードでPCMコピー、無音、全倍率レート、Reset保持を確認 |
| iPhone向けunsigned build | 成功。deployment target iOS 16。実機実行・署名確認を意味しない |
| Swift host/audio単体 | 最終27件成功。既存Host17＋PCM/設定7＋音声/lifecycle3 |
| UI | 既存4件は初回実行で成功。新規設定1件は操作対象の修正後に成功。分割結果で合計5件 |
| 静的同梱情報 | help/privacy等9ファイルがソースとbyte一致 |
| shell構文 / diff check | 成功 |

最終Swift検証には10,000 offerをworker未開始のまま投入してもwork itemが1個・PCM上限100msを保つこと、
自然終了後の無音completionが次の再生の残量を減らさないことを含む。
offlineエンジンとSimulator live出力は全5レートで確認し、liveはdataPlayedBack完了まで待つ。
割込みの再開可否、途中Pause、出力先消失、明示再試行は通知注入で確認する。

再確認用のローカル成果物（Git対象外）:

- `iosApp/build/Step3-All-first.xcresult`: Swift26件と既存UI4件成功。新規設定UI1件は失敗を保持。
- `iosApp/build/Step3-Verified.xcresult`: 最終Swift27件＋修正後の設定UI1件、全成功。
- `iosApp/build/Step3-DeviceCompile`: 最終ソースのiPhone向けunsignedビルド。

テスト用Simulatorは使い捨てで作成し終了後削除。既存Simulator/ROM Library/実設定を変更していない。

音声検証は、Swift所有PCMの有界queue・取消・失敗・遅延completionの注入テスト、
AVAudioEngineのofflineレンダリング、Simulatorのlive出力完了待機を分ける。
独立生成PCMのみで、実ROMのBEEP・聴感を確認したものではない。

UIの最初の一括実行は既存4件が成功、新規設定1件が失敗。
サウンドのAX Switchはラベル行全体を含み、中央へのtapが画面右端のスイッチを操作しなかった。
画面を確認して、要素内の右端にある実スイッチをtapし状態遷移を待つテストへ修正した。
一括実行全体の成功としては記録しない。

## 未確認・後続受入れ

- 最低対象iOS 16での実行。今回利用可能なruntimeはiOS 26.5のみ。
- 実ROMのBEEPとゲーム音、複数速度の音高/テンポ、無音、音切れ、長時間再生、停止後の鳴り残りの聴感。
- iPhone実機の消音スイッチ、他アプリ音声との共存、電話等の実割込み、経路変更、
  有線/Bluetoothヘッドホン切断と明示再試行。通知注入テストとは別。
- 実機・署名・Developer環境は機材準備時に再確認する。過去の環境を現在の事実と断定しない。
- 外部入力は第4段階、包括的なPC-1245実機受入れと文書整理は第5段階。
- 本段階終了時点ではPC-1350スキンのユーザー修正を保持し、次の更新へ含める方針。commit/pushは未実施だった。
