# Android Playerのマシン語保存

2026-11-02。mainでAndroid側を先行実装。Androidの利用者確認後にiOSへ展開する。
iOSの提出済み0.7.0 build 3、版数、R8設定は今回変更しない。

- 保存メニューを「BASICを保存」「マシン語を保存」へ分割。
- 16進数0000〜FFFF、開始≦終了、終了byteを含む。保存byte数を表示。
- 現在の論理アドレス空間をDMPへ保存。RAM限定・ミラー重複の制限は設けない。
- ROM・未割当・I/OはCoreの読取り値、bankは現在値。Load側の書込み制約は維持。
- 入力ダイアログから保存先選択まで停止し、immutableな出力byte列を保存する。
  開始時に実行中なら保存・キャンセル後に再開。停止中なら停止を維持する。
- 共通の既存RAM限定exportMemoryDumpは維持し、exportAddressSpaceDumpを追加。
  iOS facadeは既存APIのままで、iOS UIは未変更。

Androidでの受入れ項目：BASIC保存の回帰、RAMのDMP往復、ROMを含む範囲、
FFFFの単一byte、不正入力、キャンセル・書込み失敗、実行／停止状態の復元。

## 自動検証

JDK 21で全体buildとcore:desktopTestが成功。最終UI調整後も
androidApp:assembleDebug、androidApp:testDebugUnitTest、playerShared:desktopTestが成功。
全64KBの保存とCore読取り値の一致・保存後のメモリ不変・FFFF単一byte・
不正範囲・実行中拒否、Androidの16進数入力を確認。git diff --check成功。
既存のAndroid lint Kotlin metadata診断は出力されたが、全体buildは成功。
実機での保存先選択・DMP往復・キャンセル等は利用者確認待ち。
確認用APK：androidApp/build/outputs/apk/debug/androidApp-debug.apk。

## 16進数専用パッド

アドレス入力をアプリ内パッドへ置換。開始／終了をボタンで選択し、選択先を色で表示。0〜9・A〜Fの4列4行、最大4桁、クリア・1文字削除に対応。
標準IMEを使うTextFieldは配置しない。狭い画面では内容をスクロール可能。
変更後、JDK 21でbuild :core:desktopTestとgit diff --checkが成功。
実端末でのタップ操作・見やすさは利用者確認待ち。iOSは未変更。

## 横画面向け配置

利用者の配置案に合わせ、左に開始／終了と保存サイズ・操作、中央に
CDEF／89AB／4567／0123の4行、右に削除・クリアを配置。
クリアは16進数Cと区別するため「クリア」と表記。ダイアログを横へ広げて
縦の占有を削減し、画面方向の制御は変更しない。狭い高さではスクロールできる。
変更後のbuild :core:desktopTest、git diff --check成功。実端末の表示は確認待ち。

## 縦横の不足と固定操作ボタンの検証

保存・キャンセルを入力部分のスクロール領域から分離し、下部へ固定。
入力部分は最小540dp幅を保ち、幅不足時は横スクロール、高さ不足時は縦スクロール。
低い画面（ダイアログ内高さ240dp未満）は見出しを省略する。
システムバーを避けつつ余白を縮め、画面方向のアプリ設定は変更しない。

ROM不要のdebug専用MemorySaveProbeActivityで、本番と同じComposableを検証。
エミュレータの密度160dpi・fontScale 1.0で以下を確認した。

| 条件 | 画面サイズ（dp） | 結果 |
| --- | --- | --- |
| 通常 | 640×360 | C〜Fの入力・保存、キャンセル成功 |
| 幅不足 | 480×360 | 横スクロールしてFへ到達、入力・保存・キャンセル成功 |
| 高さ不足 | 640×240 | 縦スクロールして終了入力へ到達、入力・保存・キャンセル成功 |

各条件で開始C／終了Fを選び、保存コールバックの値12／15を検証。
スクロール前後の保存・キャンセルの画面座標一致とキャンセルcallbackを検証。
画像・UI XML・results.jsonはandroidApp/build/reports/memory-save-layout/へ保存。
これはダイアログの操作検証であり、実ファイル書込み検証とは区別する。
検証後にエミュレータのサイズ・密度・回転設定を復元した。
最終のbuild :core:desktopTestとgit diff --check成功。
文字拡大・全端末の画面寸法・実機ナビゲーション方式の網羅は未実施。

## 4桁入力後の自動切替

開始／終了のどちらも4桁目を追加した時点で反対の入力先へ切り替える。
すでに4桁の欄への追加は無視し、既存値を上書きしない。削除・クリアでは切り替えない。
JDK 21でbuild :core:desktopTest、git diff --check成功。
debugプローブを720×420dpで操作し、開始先行CDEF→DCBAと終了先行DCBA→CDEFの
両方で手動の欄切替なしに開始CDEF／終了DCBAとなり、保存callbackが52719／56506を
受け取ることを確認。終了先行時の初回だけ終了欄を選択する。
結果はandroidApp/build/reports/memory-save-layout/auto-target.json。
エミュレータの画面・密度・回転設定は復元済み。

## 同名ファイルの扱い

利用者の実機確認により、Android標準ピッカーが同名ファイルへ(1)等を付けることを確認。
一度追加した独自の連番処理・永続カウンタ・専用テストを取り除き、元の候補名を
CreateDocumentへ渡す方式に戻した。BASIC／DMP保存、入力パッド、自動切替、
固定フッターと縦横スクロールは維持する。

## 保存アドレスの保持

入力値をダイアログ内rememberからAndroidProgramIo所有の状態へ移し、閉じた後も
開始・終了の文字列を保持する。保存・キャンセル・失敗・入力途中を区別せず直近値を
次回表示する。新しいPlayerSessionをpublishする際に両方をクリア。
同じSessionでのホーム往復やCPU Resetでは保持し、アプリ再起動には永続化しない。
iOSは未変更。JDK 21でbuild :core:desktopTestとgit diff --check成功。
実機での再表示・機種切替操作は確認待ち。

## Android受入れ

2026-11-02、アドレス保持まで反映した版について利用者から「OKです」の報告を受領。
Android側の今回の変更を受入れ済みとして扱い、iOS側へ展開を開始した。
OS別・端末別の網羅確認を意味するものではない。

2026-11-02、iOSも実機確認を終えたとの報告を受領し、両OSの今回の保存機能を
一区切りとした。詳細はPLAYER_IOS_MEMORY_SAVE.mdの利用者受入れ記録を参照。
Androidの準備版数は1.1.1／versionCode 12。
