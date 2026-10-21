# iOS機種対応 手順8: 全体受入れ・文書更新

文書区分：段階別の実装・検証記録（2026-10-22整理）。
「未確認」「後続」やテスト件数は各記録時点のものです。冒頭の後日受入れ追記と、当時の検証範囲を区別してください。

更新日: 2026-10-21。手順1〜7の統合確認。iPhone専用、最低設定iOS 16。iPad対応・セーブステート・署名付き配布は別工程。

## 後続のユーザー確認

2026-10-21、ユーザーから「対応した機種を一通り動作確認しOK」「サウンド再生できたので一旦OK」との確認を受領。今回追加した機種の対応範囲をユーザー確認済みとする。音声のブツブツ感は調整の後続課題として残す。端末・OS・ROM版・実周辺機器別の明細は未記録であり、以下にある自動検証時点の未確認項目を全環境で解消したとは扱わない。

ホーム表示について追加要望を受領し、[ホーム表示・画面方向修正](IOS_HOME_PRESENTATION_FIX.md)で対応した。修正後もユーザーが一通り確認し、今回の機種対応とホーム修正を完了とした。以下の「左右landscapeのみ」の成果物設定は手順8実行時の履歴で、現行は機種画面だけ横固定とする。

## 対応一覧

14モデルを実装し、ROM保存は10シリーズ。表の自動検証は独自の命令列・合成ROMによるもので、実ROMの計算結果やBASIC実行結果を保証するものではない。

| モデル | ROM保存ID | LCD（桁×行） | BASIC | 登録形式 | 今回の受入れ対象 |
|---|---|---|---|---|---|
| PC-1245 | pc-1245 | 16×1 | OLD | 32／64 KiB単体、pgrom | 共通操作・既存保存形式の回帰 |
| PC-1250 | pc-1251 | 24×1 | OLD | 32／64 KiB単体、pgrom | 選択、標準RAM、共通ROM |
| PC-1251 | pc-1251 | 24×1 | OLD | 同上 | 同上 |
| PC-1255 | pc-1251 | 24×1 | OLD | 同上 | 同上 |
| PC-1260 | pc-1261 | 24×2 | S1 | 64 KiB単体、pgrom | 選択、標準RAM、2行LCD |
| PC-1261 | pc-1261 | 24×2 | S1 | 同上 | 同上 |
| PC-1350 | pc-1350 | 25×4 | S1 | 64 KiB単体、pgrom | 4行LCD、左右SHIFT |
| PC-1401 | pc-1401 | 16×1 | S1 | 64 KiB単体、pgrom | 関数キー、シンボル |
| PC-1402 | pc-1401 | 16×1 | S1 | 同上 | 選択、標準RAM、共通ROM |
| PC-1450 | pc-1450 | 16×1 | S1 | 64 KiB単体、pgrom | 関数・日本語キー |
| PC-1360 | pc-1360 | 25×4 | S2 | pgrom（内部8 KiB＋16 KiB×8） | 全ROMバンク、4行LCD |
| PC-1460 | pc-1460 | 24×1 | S2 | pgrom（内部8 KiB＋16 KiB×4） | 全ROMバンク、固有キー |
| PC-1470U | pc-1470u | 24×2 | S2 | pgrom（内部8 KiB＋16 KiB×8） | 全ROMバンク、2行LCD |
| PC-1360K | pc-1360k | 25×4 | S2 | pgrom（内部8 KiB＋16 KiB×8、漢字ROM） | READ／READM、65キー、平／片／反転S |

PC-1360Kの漢字ROMは各64 KiB、selector 0から連続した0／2／4個を許可する。漢字機能には4個を推奨し、通常PC-1360との共用はしない。BASICファイル変換の全角日本語拡張は含まない。

右上RUN／PROトグルと「動作モード」メニューはPC-1245／1250／1251／1255／1260／1261だけに表示する。RSVは対応モデルのメニューで選び、トグルには含めない。その他は本体のMODE／CAL／BASIC等を使い、LCDシンボルはROMに従う。

## 全体検証

通常のROM Libraryと端末設定を保護するため、使い捨てSimulatorとテスト専用保存先を使用した。

| 検証 | 結果 |
|---|---|
| JDK 21 `./gradlew build :core:desktopTest :playerShared:iosSimulatorArm64Test` | 成功、578 tasks（14実行、564 UP-TO-DATE） |
| Core desktop／共有Player iOS Native | 同一コードの490件／143件、失敗0。今回はGradleのUP-TO-DATE判定で再利用 |
| Swift単体 | 48件成功、失敗0。実時間CoreAudioノードの1件は既存CI方針と同じく分離 |
| 全UI回帰 | 全15ケースの成功結果を確認。初回14成功・1失敗、操作修正後の対象3ケース再実行は全成功 |
| generic iPhone / arm64 / Release / unsigned | `xcodebuild ... -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build`成功 |
| 同梱情報・画像 | Simulator／Release両方で3文書が原本と一致し、全10スキンが存在 |
| 成果物設定 | 両ビルドのInfo.plistでMinimumOSVersion 16.0、UIDeviceFamily [1]、左右landscapeのみを確認 |

SimulatorはiPhone 17 Pro・iOS 26.5。小型画面は各追加段階のSE3記録を参照する（特に手順6・7）。UI結果はGit対象外の`iosApp/build/All-Machines-Acceptance.xcresult`、Release成果物は`iosApp/build/All-Machines-Device/Build/Products/Release-iphoneos/PokecomGOPlayer.app`。後者はコンパイル確認用で、署名済みIPAや実端末での動作確認ではない。

再実行はJDK 21で`sh iosApp/test-simulator.sh -skip-testing:PokecomGOPlayerTests/PlayerPlaybackHostTests/testLiveSimulatorAudioNodeConsumesSyntheticPCM`。これは全UIを含む。`--unit`は通常CI向けのSwift単体のみであり、全機種の画面受入れの代替にはしない。

### UIスクロール操作の修正と再検証

初回の全体実行はSwift 48件成功、UI 15件中14件成功・PC-1460の1件失敗。PC-1460の失敗は、ホーム一覧を一定距離でフリックするテスト補助処理が対象ボタンを行き過ぎ、上下往復して到達できなかったもの。失敗時のアクセシビリティ階層ではプレイボタンがナビゲーションバーの背後（y=26）にあり、その後の起動検証へ到達していなかった。

`revealInMachineList`を対象の実際の位置に応じた距離でドラッグし、指を止めてから離す操作に修正した。実アプリのスクロールや機種動作は変更していない。同じiPhone 17 Proの隔離SimulatorでPC-1460、隣接カードのPC-1470U・PC-1360Kを再実行し、3件すべて成功。結果は`iosApp/build/All-Machines-Scroll-Verified.xcresult`。初回の全体結果を失敗なしと書き換えず、両方の結果を保持する。

成功結果の集合は全15 UIケースと一致する。ROM／モデル選択・切替取消・復元、RUN／PRO／RSV、各機種キー・LCD、Controller Display、背景停止・Reset、Files選択／取消、BASIC／DMP入出力、情報・音声／速度設定・コントローラ設定を確認した。PC-1401／1450の配置は添付画像でも目視確認した。PC-1401はXCTestの再起動後の撮影状態と区別するため、回転同期を明示した専用ケースの画像を参照した。

英日README、iOS README、同梱ヘルプ、プログラム入出力記録、ロードマップと移植記録を現行14モデルへ更新。文書リンク、`git diff --check`、Xcode project plistを確認した。

## 実ROM・実機の受入れ

- 今回の全機種回帰に実機firmwareを使用・追加していない。独自テストで表示・入力配送・BASIC／DMP往復・保存失敗時保護を確認する。
- 過去のPC-1245実ROMのSimulator起動・7+3=10・再起動復元は[2026-10-12の記録](../../iosApp/README.md#2026-10-12の確認結果)に保持する。新規13モデルの実ROM受入れ完了とは扱わない。
- 全機種について、実iPhoneでの署名・インストール、両横向きのsafe area、複数指・長押し・中断、ロック／背景復帰を確認する必要がある。
- 適法に用意した各機種ROMで初期化、計算、短いBASICの実行と保存・再ロード、音声の聴感、外部キーボード・ゲームパッドの実接続／切断を確認する必要がある。PC-1360Kはカナ入力→漢字候補→採用を別途確認する。
- Filesの実際のクラウド提供元で選択・取消・保存・再読込みを確認する必要がある。SimulatorのローカルFiles確認とは別判定。
- 最低iOS 16のruntime／実端末は未確認。現在のSimulator OSでの成功を最低OS受入れとしない。署名付きArchive／TestFlight／App Store配布も未確認。

## 引継ぎ事項

- PC-1250のMEM表示は既知の保留として継続する。
- PC-1402の実ROMによる容量認識は未確認。ROM版により異なり、ROMをpatchして容量を増やさない。
- 実時間のSimulator音声デバイステストはCoreAudio初期化に環境依存がある。CIと同様に当該1件を分離し、オフライン音声・速度・ライフサイクルの検証を維持する。
- iOS機種対応1〜8の実装・自動回帰工程と、上記の実ROM／実機受入れ・配布工程を区別して管理する。

## 段階別記録

[1: 共通基盤](IOS_MACHINE_SUPPORT_STEP1.md)・[2: PC-1251系](IOS_MACHINE_SUPPORT_STEP2.md)・[3: PC-1261系](IOS_MACHINE_SUPPORT_STEP3.md)・[4: PC-1350](IOS_MACHINE_SUPPORT_STEP4.md)・[5: PC-1401系／1450](IOS_MACHINE_SUPPORT_STEP5.md)・[6: バンク機種](IOS_MACHINE_SUPPORT_STEP6.md)・[7: PC-1360K](IOS_MACHINE_SUPPORT_STEP7.md)・[モード表示修正](IOS_MODE_INDICATORS_FIX.md)。
