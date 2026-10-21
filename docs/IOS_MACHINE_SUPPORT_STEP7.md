# iOS機種対応 手順7: PC-1360K

PC-1360Kを専用カード・ROM保存先で有効化。内部8 KiB、8バンク各16 KiB、漢字selector各64 KiBを検証。漢字selectorは0／2／4個の連続構成を許可する。65キー、物理F5〜F9、平／片／反転S、25桁4行表示、S2 BASICと追加RAM 2000〜27FFに対応。右上のRUN／PROと動作モードメニューは表示しない。

検証（2026-10-21）:

- JDK 21の`./gradlew build :core:desktopTest :playerShared:iosSimulatorArm64Test`成功。Core 490件、iOS Native 143件、失敗なし。
- 隔離したiPhone SE（第3世代）SimulatorでSwift 48件、UI 2件（PC-1360／PC-1360K）成功。既存CI方針に合わせ、実時間CoreAudioノードの1件を除外。オフライン音声テストは実行。
- 漢字ROMの0／2／4個構成、不正な穴・role・サイズ・SHA-256、通常1360との誤用を検証。独自READ／READMで4 selectorの結果をRAMへ保存し、漢字形ビットパターンをLCDへ転送。
- 全65キー、F5〜F9、追加RAM、S2 BASIC／DMP、ROM保存・復元、通常／Controller Display、背景復帰、Resetを確認。
- 通常表示とController Displayのスクリーンショットを目視し、平／片／反転S、4行の上下端、固有キー配置を確認。右上トグル・動作モードメニューなし。
- `git diff --check`、Xcode projectのplist検証、AndroidとiOSのskin画像SHA-256一致、同梱ヘルプと原本の一致を確認。

実ROMでの漢字入力・変換は未確認。独自のREAD／READMコードと漢字形ビットパターンを使用する。
