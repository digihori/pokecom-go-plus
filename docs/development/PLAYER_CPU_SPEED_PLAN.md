# モバイルPlayer CPU速度調整：調査と実装案

文書区分：調査・計画・実装／検証の記録（2026-10-22整理）。
過去の時制・件数・未確認事項を保持しています。現在の操作は[導入ガイド](../guides/GETTING_STARTED.md)、
[Androidヘルプ](../guides/PLAYER_HELP.md)、[iPhoneヘルプ](../guides/PLAYER_HELP_IOS.md)または対応する機能ガイドを参照してください。

調査日: 2026-10-15
状態: 調査時の案を保持。Android固定5段階を実装し、[実装・検証記録](PLAYER_AUDIO_SPEED_PROGRESS.md)で進捗管理する。
関連: [サウンド実装案](PLAYER_AUDIO_PLAN.md)

## 調査結果

- Coreの`CycleBudgetPlanner.plan(elapsedNanoseconds, speed)`は有理数の倍率に対応する。
  端数繰越、速度変更時の端数破棄、長い停止時間のcatch-up制限のテストもある。
- 共有`PlayerRunner.tick()`はspeed引数を渡しておらず、常に標準速度で実行する。
  保存済みの速度設定や変更APIはない。
- AndroidはChoreographer、iOSはCADisplayLinkからホスト時刻を渡す。
  Android設定UI、ApplePlayerFacade、Swift hostのいずれも速度変更に未接続。
- Studioの`DesktopExecutionSpeed`は下表の5段階＋無制限。
  通常は経過時間と倍率からサイクル数を算出する。無制限は4,096 cycles単位の実行と
  UIループの短い待機を組み合わせ、通常のフレーム待機とは別にしている。
  Desktopのenumは内部型であり、PlayerからdesktopAppへの依存は作らない。

| 設定 | 標準に対する倍率 |
|---|---:|
| LEVEL_1 | 0.8倍 |
| LEVEL_2 | 0.9倍 |
| LEVEL_3 | 1.0倍（標準） |
| LEVEL_4 | 1.1倍 |
| LEVEL_5 | 1.2倍 |
| UNLIMITED | 等速の時間制約を外す。固定倍率ではない |

確認した実装・テスト:

- `core/src/commonMain/kotlin/com/digihori/pgp/core/runtime/CycleBudgetPlanner.kt`
- `core/src/commonTest/kotlin/com/digihori/pgp/core/runtime/CycleBudgetPlannerTest.kt`
- `playerShared/src/commonMain/kotlin/com/digihori/pgp/player/PlayerRunner.kt`
- `playerShared/src/commonTest/kotlin/com/digihori/pgp/player/PlayerRunnerTest.kt`
- `desktopApp/src/main/kotlin/com/digihori/pgp/desktop/runner/DesktopExecutionSpeed.kt`
- `desktopApp/src/main/kotlin/com/digihori/pgp/desktop/runner/DesktopEmulatorRunner.kt`
- `playerShared/src/iosMain/kotlin/com/digihori/pgp/player/ApplePlayerFacade.kt`
- `iosApp/PokecomGOPlayer/PlayerHost.swift`

## 推奨する第一弾

Android PlayerのPC-1245／PC-1401へ固定5段階を追加する。共有モデルはiOSでも使える形にするが、
iOSの設定画面・保存・Facade接続は別段階として見積もる。今回のAndroid作業へ同時に混ぜない。

- ホームの「設定」→「CPU速度」。表示は0.8倍／0.9倍／標準（1.0倍）／1.1倍／1.2倍。
- 初期値は標準。アプリ共通の設定1セットとして保存し、機種ごとの設定は増やさない。
  基準クロックは現在の`MachineCatalog`から取得する。異なる機種も同じHzに固定する仕様ではない。
- 保存値は安定したIDとし、未知・破損した値は標準へ戻す。
- ROM復元・新規Session・機種切替では、選択した倍率を最初のRUN前に適用する。
  Resetでも設定は維持する。
- 速度変更だけでRAMやPCを初期化しない。PAUSED中ならPAUSEDを維持する。
- RUNNING中の変更は時間基準と端数を切り替え、変更前の経過時間へ新倍率を遡及適用しない。
  ホストが渡す時刻またはtick境界で処理し、共有層に時刻APIを入れない。
- フレーム呼出し頻度やLCD描画速度を倍率そのものにはしない。
  30／60／120 Hzでも、同じホスト経過時間なら目標サイクル数を揃える。
- 倍率は目標速度であり、端末の処理性能が不足する場合の達成を保証しない。
  長い停止後のcatch-up制限は維持する。

共有Playerに専用の速度モデルとRunnerへの接続を追加し、Coreの既存`SpeedRatio`を利用する。
Studioの設定、保存値、無制限ループを変更・流用しない。

## 音声との整合

Coreはエミュレーション時間に応じて22,050 HzのPCMを生成する。
CPUを1.2倍で動かすと、実時間1秒あたりの生成量は概ね26,460 samplesになる。
これを22,050 Hzの出力へそのまま積むと、遅延増大やキューあふれにつながる。
0.8倍では逆に不足する。CPU速度だけを先に公開して音声キュー任せにはしない。

第一候補は、**音程もテンポもCPU速度に比例させる**方式。
Android出力の消費レートを`Core sampleRate × 速度倍率`へ合わせる。
標準は22,050 Hz、0.8倍は17,640 Hz、1.2倍は26,460 Hzを目標とする。
AudioTrackの再生レート変更、またはそのレートでのtrack再生成を検証し、対応可否・エラーを確認する。
変更時は音声の世代を切り替え、旧倍率の未再生音を残さない。CPUやプログラムはリセットしない。

[AudioTrackの再生レート](https://developer.android.com/reference/android/media/AudioTrack#setPlaybackRate(int))
の変更は音程にも影響する。音程を維持してテンポだけ変えるtime-stretchは初回に含めない。
音声レートの設定に失敗した場合は、CPU設定を勝手に変えず、音声停止と案内を行う。
ミュート・無制限でもCore PCMの消費・破棄は継続する。

## 無制限は別段階

Studioの無制限はtickを約1 ms間隔で繰り返す設計。
4,096 cyclesをモバイルの画面フレームごとに1回だけ実行しても、60 Hzなら約245,760 cycles/sとなり、
それ自体は「無制限」ではない。画面更新頻度にも依存してしまう。

導入する場合は、Androidホストが小さな実行バッチと時間上限を管理し、UIへ定期的に制御を返す。
Core／共有Playerにwall-clock・threadを追加しない。初回からSessionの所有スレッド変更も行わない。
バックグラウンドでは停止する。音声はStudioと同様に出力せず、PCMは破棄する。
停止操作の応答、旧端末の継続負荷、発熱・電池消費を測定してから採否を決める。
「無制限」を選べるだけで、正常に使えると判断しない。

## 入力・状態への影響

- Physical入力の最小保持・解放間隔は現在のCPUサイクル基準を維持する。
  速度に合わせて同じ補正をさらに乗算しない。遅い速度では実時間の保持が長くなることを検証する。
- タッチ・ゲームパッドの保持、複数入力源の所有権、キー解放を回帰確認する。
- 速度変更で保持中のキーを勝手に解除しない。背景移行・停止など既存の解除条件は維持する。
- CPU速度とポケコンのRUN／PRO／CAL、およびCPUのRUNNING／PAUSEDを混同しない。
- ホーム往復・ROM／Session変更・BASIC／DMPロード・セーブ・Reset後も、設定と実行状態を個別に保つ。

## 小さな実装・検証手順

1. 共有速度モデルとRunnerの固定5段階を追加。ROM不要の合成Sessionで倍率、端数、速度切替、
   pause／resume／reset、長い停止、異なる画面更新間隔をテストする。
2. Androidの設定UI・保存・Session生成時の適用を接続。初期値・復元・不正値と、停止状態を確認する。
3. サウンド実装と速度情報を接続。各倍率の生成量と消費レート、無音区間、速度変更時の残音破棄、
   長時間のキュー上限を偽sinkと実AudioTrackで検証する。
4. PC-1245／PC-1401でBASIC・キー入力・ゲームパッド・音声・背景復帰を確認する。
   HUAWEI P20 lite（Android 9）と新Androidエミュレータ、実ROMと合成テストを分けて記録する。
5. 無制限は上記の固定速度が安定してから、独立した負荷評価・実装段階として扱う。

全体の推奨順は「標準速度で音声を完成 → 固定5段階と音声レート連動 → 無制限の評価」。
ただし音声キューのパケット設計には、最初から速度情報と再生世代を含める。
実装時にはAndroid・共有Player・iOS回帰・Coreの必要なテストとAGENTS指定の全体buildを行う。
調査段階ではコード・実設定・ROMを変更していない。後続のAndroid実装と検証は
[実装・検証記録](PLAYER_AUDIO_SPEED_PROGRESS.md)を参照。packageVersion・公開済みタグは維持する。
