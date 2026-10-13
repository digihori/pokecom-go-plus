# 機種追加前の修正と受入れ確認（2026-10-13）

今回の対象はMSI、Windows音声、BRK、iOS CI。公開済みタグ・履歴は変更しない。
スキン作成、Quick Assemble、Assembler拡張、ローカライズ、RAM拡張は対象外。
実装・自動テストとWindows／実機の受入れを分ける。

## 今回の区切り（2026-10-13ユーザー確認）

細かな残作業４項目の今回分は一旦完了。
Windows音声の改善、PC-1245／1360ゲームのBRK認識をユーザー確認した。
MSIの更新・スタートメニュー・設定／ROM保持は次回テクニカルプレビュー時に確認する。
iOS CIはローカル検証済みで、GitHub hosted runnerの実行結果はpush後に確認する。
以下の未記録・未検証項目は後続として保持する。commit／push、残りの機種対応は別途指示時に進める。

## Windows MSI

公開済み `v0.4.0-alpha.1` の `PokecomGOStudio-0.4.0-alpha.1-windows-x64.msi` を確認した。
GitHub assetのsizeとSHA-256に一致するファイルのProperty tableから以下を取得した。

| Property | 値 |
|---|---|
| ProductVersion | 1.3.0 |
| UpgradeCode | `{D173EFD1-57EF-3753-A618-92481F262D95}` |
| ProductCode | `{4CCD29D1-FED9-3E89-9362-0A024D3FD5C0}` |
| SHA-256 | `2a823fe71f17b3cf92ca013d89c65b26998727797652f6d6ab9062eda6f27aae` |

`desktopApp/build.gradle.kts` の `windows.upgradeUuid` に既存UpgradeCodeを固定した。
次の修正版の `packageVersion` は **1.3.1**。menu/menuGroupは継続する。
既存0.4のソース・タグ・配布物はそのまま残す。プロジェクトの表示バージョン・次のreleaseタグは
公開時に別途決める。未公開修正版を旧公開版の同名assetで置き換えない。

配布物の内容を更新して公開するたびにnumeric packageVersionの3桁以内を単調増加させる。
alpha番号や同一表示バージョンの修正でも増加する（次の配布は1.3.2以降）。
UpgradeCodeは同じStudio系列で保持し、ProductCodeを固定しない。
変更時には `inspect-msi.ps1` の期待値も更新する。release workflowは新MSIの識別子・バージョンを検証する。
release workflowのRELEASE_VERSION／notes／titleは今回変更せず、次の公開指示時に合わせる。

Windowsで次のread-only確認ができる。

```powershell
& .\docs\inspect-msi.ps1 -Path .\old.msi
& .\docs\inspect-msi.ps1 -Path .\desktopApp\build\compose\binaries\main\msi\PokecomGOStudio-1.3.1.msi -VerifyCurrent
```

設定はJava Preferencesのユーザー領域、ROM Libraryは `%APPDATA%\Pokecom GO Studio\rom-library`。
インストール先配下へ保存していない。保存先や設定keyは今回変更していないが、保持の実測はWindowsで行う。

Windows受入れ（未実施・次回テクニカルプレビュー時に実施）:

2026-10-13ユーザー方針: MSIの実測確認は次回テクニカルプレビュー時に行う。
固定UpgradeCodeとpackageVersion 1.3.1の実装は保持し、今回の作業では追加のMSI受入れを実施しない。

1. StudioとGradleを閉じ、共有の同期スクリプトを `/preview`、`/run` の順で実行する。
2. 新ソースから `gradlew.bat :desktopApp:packageMsi` を実行し、新MSIを上記スクリプトで検査する。
3. 公開0.4をインストールした状態でROM Libraryと設定を記録し、念のためROMを別途保管する。
4. 旧版を手動アンインストールせず新MSIを実行する。必要なら `msiexec /i <new.msi> /L*v <log>` でログを保存する。
5. 「インストールされているアプリ」に新バージョンが1件だけあること、スタートメニューから新Studioを起動できることを確認する。
6. ROM Library、選択機種、設定、既存Projectが保持されていることを確認する。
7. Studioを閉じて起動し直し、保持を再確認する。旧MSIの上書き／タグの移動は行わない。

## Windows音声

CoreのbuzzerはDCをhigh-pass filterで減衰させ、全サンプルが0になると空PCMを返す。
旧DesktopAudioPlayerは空PCMを無視してSourceDataLineを開いたままにしていた。
Windowsでunderflow時にクリックが継続する原因候補として、無音になった時点でラインを閉じる。
frequencyHz=0だけでは判定しない（OUT Cによる手動波形やフレーム内の短音があるため）。

- 空または全0のPCM: queued音声をdrainしてからstop/flush/close。無音中にラインを新規作成しない。
- 次の非0 PCM: 新しいラインを開いて再生する。
- Pause／Reset／アプリ終了／unlimited／fault／デバッガ停止: 即座にstop/flush/closeし、残PCMを破棄する。
- device write failure: ラインを閉じて次の再試行を可能にする。

Fake sinkテストはPCM byte順序、再利用、rate変更、無音の終了・再開、デバイス失敗を確認する。
2026-10-13、ユーザーからWindowsで「サウンドは改善されたようです」と報告あり。
報告された範囲で改善を確認。各停止経路の個別結果はまだ記録されていない。
通常再生→自然終了→次の再生、BREAK後の無音、Pause、Reset、
アプリ終了を同じサウンド使用プログラムで確認する。短い単発クリックと継続ノイズを分けて記録する。
BREAKはCPUの強制Pauseではなく、プログラムがキーを走査し発音を終了した後に音声ラインを閉じる。

## BRK入力

EscapeはDesktopKeyboardInputから、画面BRKはPocketKeyButtonから、同じRunnerのHostKeyStateへ渡る。
所有tokenにより片方の解放で他方の保持を消さない。物理key repeatは重複pressを抑止する。
画面キーは機種のminimumSoftwareKeyHoldMillisecondsを満たして解放する。
通常Runnerは16msごと、unlimitedは1msごとにUI dispatcherへ戻り、後者のbatchは4096 cycles。
入力イベントとCPU処理は同じdispatcherで直列実行され、batch中には新しい入力を処理しない。

BRKは通常matrixから独立したKEY ON信号。SC61860の **TEST 8** がzero flagで観測する。
PC-1245／1250／1251／1255はPc1245 matrix、PC-1261はPc1350 keyboardを再利用し、
PC-1350／1360／1401はそれぞれのkeyboardからKEY ONへ接続する。
PC-1360のmemory selectorによるgroup走査等、通常キーの機種別走査は変更していない。

従来は保持中でも10回のTESTで信号が消えた。今回は保持中はKEY ONを継続し、
解放済みの短いtapは従来の最大10観測ラッチを残す。各TESTはmaskによらず観測ラッチを消費する現行CPU仕様を保持する。
したがってタイマーTESTだけを長く繰り返してからBRKを走査するプログラムの極短tap、
そもそもBRKを走査しないプログラムの停止は保証しない。BRKを割込み・強制停止へ置き換えない。

全対応機種で合成TEST 8命令列を使い、100回保持、解放、reset、tick間の短いtap、再押下を検証する。
Runnerは物理／画面同時保持・片方の解放・Pause解放を検証する。ROM由来のfixtureは追加しない。
実ROMプログラムと実機の照合は別項目。WindowsでEscape／画面BRK双方の短押し・長押し・解放後再押下、
通常／高速／unlimitedを確認し、TEST 8を走査するプログラムとROM経由の終了動作を分けて記録する。

### Windowsゲームでのユーザー確認（2026-10-13）

PC-1245のゲームでBRKが反応することをユーザー確認。
PC-1360も、操作を確認し直した結果「BRKはちゃんと認識していました」と報告あり。
当初のPC-1360非反応報告は操作の取り違えによるものとして更新し、機種固有の追加修正は行わない。
物理／画面それぞれの短押し・保持・解放、全速度、実機照合の個別結果は引き続き未記録。
今回の再調査での追加テスト実行は許可されなかったため実施していない。先の自動テスト結果と分ける。

## iOS CI

CIに `ios-player` jobを追加。`macos-15` のApple Silicon runner・JDK 21を使用する。
Core／hostInput／playerSharedのiosSimulatorArm64Test、Simulator Debug framework、
実機arm64 Release frameworkのリンク、署名なしSimulator build・Swift host/UI testsを実行する。
利用可能なiPhone Simulatorを自動選択し、機種名・端末UUIDを個人環境へ固定しない。
XcodeのGradle build phaseとCIで同じ `.gradle-local` を使用する。結果はxcresult・Native test XMLとして保存する。

ローカルではJDK 21を選んで `sh iosApp/test-simulator.sh` を実行できる。
result bundleは実行時刻を付けて保存し、`PGP_IOS_RESULT_BUNDLE` で出力先を指定できる。署名・証明書・Team・配布を追加していない。
対象はiPhone、deployment targetはiOS 16を維持。新しいSimulator OSでの成功をiOS 16動作保証とは扱わない。
CI hosted runnerでの成功はcommit/push後の実行で確認する。実機署名・Archive・配布は別途。

出典: [JDK 21 jpackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)、
[MSI major upgrade](https://learn.microsoft.com/en-us/windows/win32/msi/preparing-an-application-for-future-major-upgrades)、
[GitHub runner一覧](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)。

## ローカル検証結果（2026-10-13）

JDK 21で `build :core:desktopTest` とiOS Native tests/frameworkリンクをまとめて実行し成功
（578 tasks、140実行、438 up-to-date）。Escape入力テスト追加後のDesktop testsも成功した。

| 対象 | 結果 |
|---|---|
| Core Desktop | 410 tests、failure 0 |
| Desktop Studio | 215 tests、failure 0、既存skip 1 |
| hostInput／Player Desktop | 9／20 tests、failure 0 |
| iOS Native Core／hostInput／Player | 383／9／23 tests、failure 0 |
| Android Player Debug／Release | 各9 tests、failure 0 |
| PgpPlayer frameworks | Simulator Debug／実機arm64 Releaseリンク成功 |
| 署名なしSimulator build | Xcode 26.6／iOS 26.5、iPhone 17 Proで成功 |
| Swift host／UI tests | 9／2 tests、failure 0 |
| CIスクリプト | 最終版 `sh iosApp/test-simulator.sh` がexit 0 |
| workflow YAML／shell構文／差分 | YAML読込み、`sh -n`、`git diff --check` 成功 |

Windows音声はユーザーから改善報告あり（上記参照）。各停止経路の個別確認は未記録。
Windowsの新MSI生成／COM検査スクリプト／実際の更新、SMB／Robocopy同期の
実測結果は未記録。実ROM／実機BRK照合、iOS 16 runtime、hosted CIは未検証。
既存の未コミット文書・同期スクリプトを保護し、作成中スキン3枚は変更・stageしていない。
commit／pushは行っていない。
