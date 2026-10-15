# Android Player サウンド調査と実装案

調査日: 2026-10-15
状態: 調査時の案を保持。Android初期実装を追加し、[実装・検証記録](PLAYER_AUDIO_SPEED_PROGRESS.md)で進捗管理する。

## 対象

現在のAndroid PlayerのPC-1245／PC-1401で、実機のブザーに相当する音を再生する。
PC-1350追加、iOS音声出力、Studio音声の改修、WAV／CSAVE／CLOAD、録音は今回に含めない。
既存のPC-1401対応の完了記録は変更せず、その後の別作業として扱う。

## 調査結果

| 層 | 現在の実装 | 今回の扱い |
|---|---|---|
| Core | `EmulatorSession.audioSnapshot()`／`drainAudioSamples()`あり | 既存のPCM生成を使う |
| PC-1245／PC-1401 | `Pc1245Buzzer`を使用。CPUサイクルで進め、OUT Cの制御を取り込む | 機種別に検証する |
| PCM | 22,050 Hz、符号付き16-bit、mono。DCをhigh-pass filterで減衰 | AndroidへPCMのまま渡す |
| 共有Player | `PlayerSession`に音声取得APIなし。Runnerも音声を消費しない | 小さな共有APIとテストを追加する |
| Android | AudioTrack、音声用出力キュー、音声フォーカス処理なし | ホスト側へ新設する |
| Studio | `DesktopAudioPlayer`がPCMを再生。無音の自然終了と強制停止を区別 | 振る舞いとテスト観点を参考にする。Java Soundへ依存しない |
| iOS | 現在のhost／Facadeに音声再生接続なし | 共有変更の回帰確認のみ |

確認した主な実装:

- `core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245Buzzer.kt`
- `core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1401/Pc1401Machine.kt`
- `core/src/commonMain/kotlin/com/digihori/pgp/core/api/EmulatorSession.kt`
- `playerShared/src/commonMain/kotlin/com/digihori/pgp/player/PlayerSession.kt`
- `playerShared/src/commonMain/kotlin/com/digihori/pgp/player/PlayerRunner.kt`
- `androidApp/src/main/kotlin/com/digihori/pgp/player/android/MainActivity.kt`
- `desktopApp/src/main/kotlin/com/digihori/pgp/desktop/audio/DesktopAudioPlayer.kt`
- `docs/PLATFORM_FIXES_2026-10.md`のWindows音声記録

既存テストにはブザーPCM、無音への減衰、Desktopのsink再利用・自然終了・強制停止・デバイス失敗がある。
Playerの音声接続とAndroid出力についてのテストはまだない。

## 接続だけでは済まない点

1. **音声を溜めない。** Coreは前回drainからの実行サイクルと制御イベントを保持する。
   現在のPlayerはdrainしていないため、長時間動かしてから取得すると大きなPCMを生成する可能性がある。
   接続後は各tick後に1回消費し、ミュート・出力不可でも破棄して進める。
2. **周波数値だけで発音判定しない。** `frequencyHz == 0`でも、OUT Cの手動波形や
   フレーム内の短い発音があり得る。Coreが生成したPCMを再生する。
3. **空PCMと時間ゼロを区別する。** Coreは生成結果が全0なら空配列を返す。
   非空の配列だけを連結すると、無音区間を詰める可能性がある。
   第1段階で「短音→無音→短音」の時間関係を検証し、出力パケットに必要な時間情報を定義する。
   既存の空PCMの意味は変えず、まずtickの実行サイクルとホスト側の時系列で扱う案を検証する。
   不十分なら、Coreの純粋な生成フレーム数メタデータ追加を別途判断する。
4. **自然終了と強制停止を分ける。** 空PCMを受けた瞬間のflushでは、まだ機器に残る短音を切ってしまう。
   自然終了は末尾の再生完了を確認して休止する。PAUSE／背景移行などは待たずに破棄する。
5. **音声でUIと入力を止めない。** AudioTrackへの書込み待ちをChoreographerのフレーム処理に入れない。
   部分書込み・0件・機器エラー・キュー上限を扱う。

## 推奨構成

```text
CoreのCPUサイクル／ブザー → 共有PlayerでPCMを取得（Sessionと同じスレッド）
  → Androidの上限付き音声キュー → 専用出力worker → AudioTrack
```

- Core／Sessionは現在どおりUIスレッドで直列操作する。音声workerからCoreを読まない。
- workerへ渡すのはコピー済みPCM、時系列情報、Session／再生世代の番号。
- Android標準のAudioTrack（MODE_STREAM、PCM 16-bit、mono）を第一候補とする。
  初期は既存の22,050 Hzを使い、端末での初期化可否・必要バッファ量を確認する。
- 部分書込みを扱える専用workerを設ける。非ブロッキング書込みと待機を組み合わせ、busy loopにしない。
  UIは出力完了を待たず、停止要求をworkerが優先して処理できる構成にする。
- キューと機器バッファは時間で上限を設ける。初期評価の目安は合計100〜200 ms程度とし、
  端末の最小バッファと実測に合わせる。上限超過時は古い音を後から長く鳴らさず、音声だけ再同期する。
- PAUSE／Reset／機種・ROM・Session切替／ホーム／背景／終了／Faultで世代を更新し、
  キューと機器内の未再生音を破棄する。古いworker結果で再開しない。
- CPUが動き続ける通常メニュー表示は、音声停止の理由にしない。
  キーボードの入力フォーカスとAndroidの音声フォーカスは別物として扱う。
- 音声エラーでCPUをFaultにしない。音声を停止し、UI側で一度案内する。
  毎フレームの再初期化・ダイアログ連発を避け、明示的な操作で再試行できるようにする。

## 設定・OSとの関係（提案）

- ホームの「設定」に全機種共通の「サウンド」を追加する。初期値はONを提案。
  音量はOSのメディア音量を使い、独自の音量スライダーや機種別設定は初回に入れない。
- 音量キーはメディア音量を操作する。システム音量をアプリが勝手に変更しない。
- ミュート中もPCMを消費し、ONに戻した際は新しい音だけを再生する。
- 音声属性はゲーム用途を基本に、AudioFocusRequestを扱う。フォーカス取得失敗なら無音で継続する。
  フォーカス喪失時もCPUは止めず、音声だけ停止・破棄する。
  一時喪失からの復帰は前景・設定ON・CPU実行中の場合に限り、新しい音から再開する。
  恒久喪失後は明示操作で再開する方針とし、他アプリとの競合を実機で確認する。
- 出力先はOSの通常ルーティングに従う。ヘッドホン切断による突然のスピーカー出力は抑止し、
  一時的な音声停止として案内する。Bluetoothのペアリングや出力先固定は実装しない。
- 背景再生・Foreground Service・録音・ネットワーク処理は追加しない。
  Android 15以降の音声フォーカス制約とも整合させる。

Android仕様の確認先:

- [AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)
- [音声フォーカス](https://developer.android.com/media/optimize/audio-focus)
- [AudioAttributes](https://developer.android.com/reference/android/media/AudioAttributes)

## 小さく進める実装手順

1. **共有Playerの音声受渡しと時間関係を固める。** ROM不要の独自命令列でPC-1245／PC-1401の
   2／4 kHz、OUT C手動波形、短音・無音・再発音、drain後の再取得、Resetを検証する。
   ミュート相当で長時間消費した後に過去音が残らないことを確認する。
   iOSへの機能追加をせず、共有APIのコンパイル・既存テスト回帰を確認する。
2. **Androidの最小再生系を作る。** AudioTrack wrapperと偽sinkを用意し、部分書込み・0件・
   バッファ上限・世代更新・初期化／機器エラー・自然終了と強制停止を自動テストする。
   ROM不要のDebug確認で合成PCMを鳴らし、PCM経路と実際の可聴確認を分ける。
3. **Player本体とライフサイクルへ接続する。** tick後の消費、PAUSE／Reset／ホーム／背景／
   ROM変更／機種切替／Fault、ロード・セーブ前後に接続する。入力フォーカスやゲームパッドを回帰確認する。
4. **サウンド設定とOSイベントを仕上げる。** ON／OFF保存、メディア音量、音声フォーカス、
   ヘッドホン切断、出力不可時の案内・再試行を接続し、ヘルプ・プライバシー文書を実装に合わせる。
5. **機種別に実ROM・実機を確認して区切る。** PC-1245とPC-1401それぞれでBEEP、短音、
   連続音、ゲームの手動波形、自然終了後のクリック、停止・復帰を確認する。
   HUAWEI P20 lite（Android 9）と新Androidエミュレータ、スピーカー／ヘッドホンを分けて記録する。
   Bluetoothは機器を用意できた場合に別記し、スピーカー確認だけで全出力先確認済みとしない。

実装時は変更範囲のテストに加え、AGENTS指定の全体buildとCore desktop testsを行う。
自動PCM検証、Androidエミュレータでの出力経路確認、実際に聞いた結果、実機確認を区別する。
ROM／ROM由来fixtureをGitへ追加せず、参照プロジェクトへの依存を作らない。
Core commonMainへOS／UI／filesystem／時刻／thread APIを追加しない。
packageVersion・公開済みタグ・実際の設定やROMを調査のために変更しない。

## CPU速度調整との連携

後続調査は[CPU速度調整案](PLAYER_CPU_SPEED_PLAN.md)を参照。
音声の最初の可聴確認は標準速度で行い、その後の固定5段階では消費レートも倍率へ追従させる。
CPU倍率だけ変えて22,050 Hzの出力へ積み続ける方式は採用しない。
音声パケットには速度情報と再生世代を持たせ、変更時に旧倍率の未再生音を破棄する。
音程も速度に比例させる案を第一候補とし、音程維持のtime-stretchと無制限の音声出力は初回対象外。
