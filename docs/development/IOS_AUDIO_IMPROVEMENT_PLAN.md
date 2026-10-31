# iOS Player 音声改善の解析・対応方針

2026-11-01。本工程はユーザー確認を受けて完了。PC-1251の音声は大幅に改善し、まれな引っ掛かりは許容範囲として残す。以下は調査・実装・検証の段階別記録であり、原因候補や未実施項目は各段階時点のもの。最終結果は末尾の「予約量超過の再現と修正」「今回の改善の区切り」を参照。

## 現状と原因候補

- `PlayerHost`はMainActor上のCADisplayLink（希望60Hz、30〜60Hz）でCoreを進め、表示を更新してからPCMを渡す。通常のPCMは約16.7msまたは33.3ms単位。UI負荷・Core実行時間・表示生成時間が到着間隔に影響する。
- `Pc1245Buzzer`は22,050Hzで、位相・フィルタ状態・端数の経過時間をdrain間で保持する。空samplesにも無音のframeCountがあり、共有Runnerは毎tick drainする。ソース上、パケットごとに発振位相を初期化する構成ではない。
- `EnginePCMSink`は生成時にengine.startとnode.playを実行し、その後で最初のbufferをscheduleする。開始前に一定量を予約する処理がない。起動直後から供給間隔の揺れに弱い可能性がある。
- `PlayerAudioPump.complete`は最後の有音bufferの完了で、後続の予約済み無音もキャンセルし、engine/nodeとsessionを閉じる。次の有音bufferで再生成する。パケットの枯渇と音声ストリームの終了が区別されていない。
- `reservedFrames`は送信前とnode予約済みの合計で、`.dataPlayedBack`通知を受けて初めて減る。一部だけ再生済みのbufferも全量を数える。上限100ms超過で世代を変え、旧出力を破棄する。
- Appleの`.dataPlayedBack`は下流の処理遅延と出力機器の遅延を含む。長い出力遅延や通知処理の遅れをPCM滞留として数えてしまう可能性がある。Androidの100ms上限は未送信PCMに対するもので、同じ意味ではない。
- 既存Swiftテストは手動の完了通知、停止・世代管理、変換、オフライン描画を確認する。到着間隔と再生時計を同時に動かす連続ストリームの試験や、遅延のある出力での上限判定は未整備。

## 対応順序

### 1. 再現条件と計測を先に用意する

DEBUG専用の集計を追加し、tick到着間隔、Core実行・表示更新・PCMコピーの所要時間、パケット長、未送信量、予約量、開始・停止・overflowの理由別回数を記録する。音声コールバック内でログ出力や重い処理をしない。再生位置と予約末尾の差を可能な範囲で取得し、通知待ちの量と未再生量を区別する。

独立生成した連続波形、短音の反復、音→短い無音→音、長い無音を使う。60Hz/30Hz、遅れ・まとめ到着、完了通知遅延、全5速度を再現できる注入時計付きsinkを用意する。実ROMに依存せず基礎動作を比較する。

### 2. Swift音声経路を修正する

最初の修正は`IosPlayerAudio.swift`と関連テストに集中する。

- sink生成と再生開始を分離し、PCMを予約してからplayする。初期蓄積は40〜60ms程度を比較する暫定値とし、許容遅延と計測で決める。閾値に届かない短いBEEPも一定時間内に開始できる期限を設ける。期限処理はMainActorや新しいofferに依存させない。
- 最後の有音bufferの完了だけで即座に出力を破棄しない。音間の無音を維持し、一定のアイドル猶予後に閉じる。猶予中も無制限にゼロを予約しない。Pause、背景、ホーム、Reset、OFFなどの明示停止は猶予せず破棄する。
- 未送信PCMのメモリ上限と、nodeの予約・再生位置の管理を分離する。`.dataPlayedBack`待ちの全量だけでoverflowを決めない。`.dataConsumed`や`.dataRendered`への単純な差し替えも、未再生量を保証しないため避ける。再生位置・予約上限・完了IDを使い、有界性と古い音の破棄を両立する。
- 供給不足時は出力を毎回作り直さず、少量を再蓄積して再開する。実際に失われた時間を無制限に追いかけない。100ms以上の供給停止を小さな先読みだけで隠せるとは扱わない。
- 世代付き開始期限・アイドル期限を用い、停止後のタイマーや遅い完了通知が新しい再生に影響しないようにする。

消音モードに従うambient、他アプリとの共存、背景再生なし、速度に音高・テンポが追従する仕様は維持する。

### 3. 残る遅延の原因を確認する

修正後もMainActorの停滞に連動して枯渇する場合は、iOSホストが所有する専用直列executorにSession実行を移す案を検討する。Coreには時刻・threadを導入しない。入力・ROM/プログラム入出力・Reset・画面snapshotの所有権まで変わるため、初回の音声修正とは分ける。

PCMコピーやFloat変換の最適化、フェード、サンプルレート変更は、計測・波形比較で必要性が確認された場合に扱う。クリック対策で本来の短音・直接出力波形を変えないようにする。

## 検証と完了条件

- 注入時計の連続供給試験で、小さな到着揺らぎによる反復停止や世代変更がない。短音の末尾と音間の無音が保持される。遅延通知だけでoverflowしない。
- 不足・過剰供給、速度変更、停止、割込み、経路変更、再試行、古い完了通知・タイマーを試験し、キューは有界で停止後の再生がない。
- 既存の全5速度オフライン描画とSwiftのhost/audio試験を実行する。連続パケットの波形・時間長を比較する。変更後はJDK 21で`./gradlew build`も実施する。
- 隔離Simulatorでライブ出力を比較する。既存CIで除外しているデバイス依存テストと通常の単体試験を区別する。
- 実ROMのBEEP・ゲーム音で修正前後を聴き比べる。利用可能なら内蔵出力・有線・Bluetooth、全5速度、長時間再生も確認する。Simulatorやオフライン試験の成功を実機の聴感確認とは扱わない。

## 根拠

- `iosApp/PokecomGOPlayer/IosPlayerAudio.swift`
- `iosApp/PokecomGOPlayer/PlayerHost.swift`
- `iosApp/PokecomGOPlayerTests/PlayerHostTests.swift`
- `playerShared/src/commonMain/kotlin/com/digihori/pgp/player/PlayerRunner.kt`
- `core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245Buzzer.kt`
- [Apple: dataPlayedBack](https://developer.apple.com/documentation/avfaudio/avaudioplayernodecompletioncallbacktype/dataplayedback)
- [Apple: AVAudioPlayerNode](https://developer.apple.com/documentation/avfaudio/avaudioplayernode)

## 初回改善の実装（2026-11-01）

- node.playをsink生成から分離し、約50ms分のPCMを予約してから開始する。短音は最初の予約から50msのworker期限でも開始し、次の画面更新を待ち続けない。
- nodeの再生位置から未renderフレーム数を計算し、機器のpresentation遅延を100msの予約上限に含めない。無効なlastRenderTimeは参照せず、再生開始前は予約量を保持する。underrun後は進んだ再生位置を新しい予約の基点にする。
- 未送信queueとnode予約はそれぞれ100msを上限とする。従来の「合計100ms」とは異なり、両方が最大なら約200msに加えて処理中の1パケットと機器内部の遅延があり得る。上限はOS内部の遅延保証ではない。
- 最後の有音のpresentation完了だけでは即停止しない。250msのアイドル期限、または有音がすべて完了しかつ250ms分の無音を受け取った時点で閉じる。新しい有音は古いアイドル期限を無効化する。無音だけでは最初のdeviceを開かない。
- 予約が尽きた場合はnodeをpauseして再蓄積し、同じengineで再開する。明示停止・世代変更・実際の予約過剰は従来どおり出力を破棄する。
- worker期限は世代・stream・期限tokenで無効化する。遅い旧completionには単調増加buffer IDを使い、新しい予約へ影響させない。
- 注入可能なworker期限、独立したrender進行とpresentation完了を持つテストsinkを追加した。短音の開始、先読み、アイドル猶予、新しい音による期限取消、停止後の期限、underrunからの再開、全5速度でpresentation通知を保留する連続供給を検証する。

この段階では、実環境のtick間隔・処理時間の計測機能、MainActorからのSession移動、実ROM聴感比較は未実施。50ms/250msは暫定調整値であり、実出力での聴感と遅延の確認を次に行う。

検証：JDK 21で`./gradlew --gradle-user-home .gradle-local build`成功（578 tasks、16実行）。Swiftの初回実行では開始前の無効なnode時刻参照をオフライン試験が検出し、有効性チェックを追加した。最終版は`sh iosApp/test-simulator.sh --unit`でSwift56件（音声10件を含む）、失敗0。全5速度の実エンジンのオフライン描画も成功。結果は`iosApp/build/CI-TestResults-20261101-064842.xcresult`。通常CIと同じくライブ機器出力1件は除外し、UI試験・実ROM・聴感の成功とは扱わない。`git diff --check`も成功。

## 聴感確認後の追加調査（2026-11-01）

ユーザーから「いい感じになりましたが、何かのタイミングで引っ掛かる時があります」との確認を受領。音のみか画面も同時に停止するか、発生機種・操作は確認中。

コード調査で`PlayerHost.refresh`が入力設定用のrunStateを取得するためにもLCD込みのsnapshotを作成していた。Apple facadeに軽量runState getterを追加し、入力設定後のsnapshotは維持して重複生成を除いた。tick後のPCM受渡しをrefreshより前に移した。Fault時には受け渡さず停止する条件は維持する。

Debugビルドの`com.digihori.pgp` / `PlayerAudio`ログに、フレーム間隔40ms超または処理20ms超の場合のtick・コピー/受渡し・refresh所要時間、queue overflow、node overflow、underrunを記録する。音声render callback内ではログを出さない。50msの再蓄積・250msのアイドル猶予は変更していない。

追加調査時点では変更後のビルド・テストは未実施だった。前節の56件成功は追加変更前の結果であり、その後の再検証は次節に記録する。

## 追加変更の検証（2026-11-01）

ユーザーから検証の指示を受け、上記の追加変更を含めて再実行した。

- JDK 21で全体`build`成功。578 tasks（28実行、550 UP-TO-DATE）。Core desktopTestもbuildに含まれ、既存520件の結果はUP-TO-DATE。
- 共有iOS Nativeテスト143件、失敗0。ApplePlayerFacade 19件、共有音声3件、PC-1251のRunner 5件・Foundation 4件・Gamepad 3件・ProgramIo 4件を含む。
- `sh iosApp/test-simulator.sh --unit`でSwiftの再ビルドと56件、失敗0。音声10件、外部入力6件、Host 38件、PlaybackHost 2件。音声の全5速度オフライン描画、先読み期限、underrun再開、presentation通知遅延、停止・割込みなどを含む。
- Swift結果：`iosApp/build/CI-TestResults-20261101-070144.xcresult`。通常CIと同じくライブ出力のデバイス依存1件は除外。`git diff --check`成功。

PC-1251の音が途中で一瞬引っ掛かること、発生タイミングは不定、画面の同時停止は不明というユーザー確認を受領。上記は追加変更のコンパイル・回帰確認であり、実ROMでの症状再現・原因確定・聴感上の解消を示さない。次は該当再生中のDebugログでframe遅延、underrun、overflowを照合する。

## 予約量超過の再現と修正（2026-11-01）

実ROM再生中のXcodeコンソールで`node overflow pending=1838 incoming=368 rate=22050`等の反復を確認した。通常の到着パケットが残量に加わると100msを超え、出力を破棄していた。`frame gapMs`が長い例とqueue overflowもあり、すべての引っ掛かりが同一原因とは断定しない。

共有`PlayerRunner`は毎tickの予定サイクル数をそのまま`runCycles`へ渡し、命令を最後まで実行したことによる超過分を次回から差し引いていなかった。PC-1251のWAIT 255は261サイクルのため、tick末尾での超過が累積し、実時間より多いPCMを作り続ける。

独立生成のWAITループによる10秒のテストを修正前に実行し失敗を確認。0.8倍・30Hzで予定1,536,000サイクルに対し実行1,601,994サイクル（約4.3%超過）。従来の短いNOP中心のテストでは長い命令での累積が検出されていなかった。

Runnerに命令完了の超過サイクルを保持し、次tick以降の実行要求から差し引く処理を追加した。超過だけで予算を使い切る極短tickではCPUを実行しない。run/pause/reset/速度変更/Faultの時間境界で繰越をクリアする。公開budgetはホスト時刻に基づく予定量のままとし、実命令のサイクル数・音源波形・音声上限は変更しない。共有Runnerを使うAndroid Playerにも適用される。

回帰テストは、PC-1251の独立生成2kHz発音＋WAITループを全5速度・30/60/120Hzで10秒ずつ実行し、累積超過が1命令以内、PCM長の誤差が31フレーム以内に収まることを確認する。極短tick・停止再開・Reset・速度変更時の繰越も確認する。

検証：修正前の独立WAITループ試験で失敗を確認し、修正後は発音を含む全15条件と極短tick/時間境界の試験が成功。共有desktop126件、iOS Native145件、失敗0。JDK 21の全体build成功（578 tasks、86実行）。Swift単体56件も失敗0、全5速度オフライン音声を含む。結果は`iosApp/build/CI-TestResults-20261101-071814.xcresult`。通常CIと同様にライブ機器出力1件は除外。`git diff --check`成功。

普段の実ROM再生アプリは今回の検証で上書きしていない。実ROMの聴感上の解消とoverflowログの減少は、Xcodeで修正版を再実行した後に確認する必要がある。

## 今回の改善の区切り（2026-11-01）

ユーザーからPC-1251について「だいぶ良くなった。まだごくたまに引っ掛かるが許容範囲」との確認を受領し、「今回のサウンド改善はここまで」と明示された。この工程はここで区切る。まれな引っ掛かりは既知の残件とし、完全解消とは記録しない。
