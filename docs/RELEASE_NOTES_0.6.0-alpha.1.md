# Pokecom GO Technical Preview 0.6

Git tag: `v0.6.0-alpha.1`

PC-1360Kの漢字対応とROM吸い出しツール、Android Playerの対応機種・音声・操作を更新した早期評価版です。
完成版・ストア配布版ではありません。

## ダウンロード

- Studio：macOS Apple Silicon DMG／ZIP、Windows x64 MSI／ZIP、Linux x86_64 DEB／tar.gz
- Android Player：Android 8.0以降向けのdebug署名APK
- 各プラットフォームのSHA-256一覧、アプリ内のLICENSE・第三者通知

ROMは同梱していません。利用者自身が正当に入手したROMを用意してください。iOSの配布物はありません。

## PC-1360Kの漢字対応

- Studioの機種としてPC-1360Kを選択でき、かな入力・漢字変換を利用できます。
- ROMセット作成時に、通常ROMに加えてページ0〜3の漢字ROMを各64KiB、4ファイル指定します。
- 「採用」「変換」などのキーキャプションを整えました。
- 漢字ROMアクセスの解析結果と、確認できた論理領域・未確定の物理配線を文書化しました。

詳細：[PC-1360K 漢字ROM解析とStudio対応](https://github.com/digihori/pokecom-go-plus/blob/v0.6.0-alpha.1/docs/machines/PC-1360K.md)

## 実機からの漢字ROM吸い出し

ソースアーカイブ／リポジトリの`tools/pc1360k-rom-dump/`に以下を用意しました。

- ページ0〜3を入力して1ページ64KiBを回収するBASIC。1回約33分の見込みで、4回録音します。
- BASICの転送・実機実行・録音・復元を事前に確認する1行サンプル。
- PCM WAVからROMを復元するPython 3.10以降用デコーダー。追加パッケージは不要です。
- pcwavでのBASIC転送、Audacityでの録音、デコード、Studio登録までの手順とBASICの処理解説。

デコーダーは転送チェックサム、実機の再読比較、データ合計、ブロックの欠落・重複を検証します。
復元結果は`kanji-selector-0.bin`〜`kanji-selector-3.bin`の4ファイルと、検証情報のJSONです。

ページ0は本ツールでの実機吸い出しからデコードまで確認し、検証済みROMと全65536バイト一致しました。
全4ページの読出しループ・入力検証・異常時停止は実ROMを使ったエミュレータ試験で確認しています。
ページ1〜3について、ページ指定BASICによる実機録音の追加確認は未実施です。

手順：[PC-1360K 漢字ROM吸い出しツール](https://github.com/digihori/pokecom-go-plus/blob/v0.6.0-alpha.1/tools/pc1360k-rom-dump/README.md)

## Android Player

- PC-1245に加えてPC-1401・PC-1350に対応し、機種別スキン・キー入力・ROM管理・BASIC/DMP入出力を整備しました。
- ホーム画面とメニュー、ゲームコントローラー割当、画面配置・復帰時の操作を改善しました。
- 音声出力とCPU速度設定を追加しました。
- PC-1401・PC-1350の今回の対応範囲はユーザー確認済みです。接続機器ごとの網羅的な入力検証を意味しません。

## 検証範囲と既知制限

- ローカルJDK 21全ビルド、Core／Studio／Androidの自動テストと、macOS／Windows／Linux／AndroidのCIを実施します。
- macOS／Windows／Linuxの実行配布物とAndroid APKを生成します。最終成果物ごとの実機インストール確認は別途です。
- iOS PC-1245はホーム・ROM管理・プログラム入出力・音声／速度・外部入力を開発中です。
  Simulatorのローカル試験は通過していますが、CI環境でのFiles保存失敗は未解決のため、iOS自動CIを一旦外しています。
  iOS実機・署名・ストア配布・最低対応OSでの確認は完了していません。
- 漢字ROMの4ページは回収・利用できた論理領域です。物理チップとの対応やFポート配線の意味は未確定です。
- 対応機種ごとの実機との差、RAMミラー／拡張RAM等の継続課題は各機種ドキュメントを参照してください。

## インストール

Studioの内部パッケージ版は`1.4.0`です。Windows MSIのUpgradeCodeは維持しています。
署名・公証を行わないプレビュー配布のため、OSの警告が表示される場合があります。
Androidはdebug署名を使用するため、既存APKと署名が異なる場合は上書き更新できないことがあります。
更新前にプログラム等をバックアップしてください。
