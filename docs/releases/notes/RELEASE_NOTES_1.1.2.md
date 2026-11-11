# Pokecom GO Player 1.1.2

2026-11-06作成。状態：リリース準備用原稿（未公開）。対象：Android／iOS Player。
変更内容は公開済み1.1.0以降のmain、`4679f00`までを基準とします。
1.1.1の公開状況やiOSの審査結果は、この原稿では確定していません。
[提出前チェックリスト](../checklists/RELEASE_CHECKLIST_1.1.2.md) ·
[ストア更新文（日本語）](../store/1.1.2/whats-new.ja.txt) ·
[Store text (English)](../store/1.1.2/whats-new.en.txt)

## 日本語

### 主な変更

- PC-1250／1251／1255、PC-1260／1261、PC-1401／1402に「RAM制限解除」を追加しました。ホームの機種カードで選択でき、初期値はOFFです。設定は機種ごとに記憶し、次の「プレイ」で適用します。起動し直す場合は現在のRAMとロード済みプログラムを失うため、先に保存してください。
- PC-1245・PC-1251系・PC-1261系のスキンを更新し、LCD下のDEF・SHIFT・上下左右・BRKの表示位置とタップ判定を合わせました。
- PC-1261系のLCD配置を見直しました。カナ・SML・SHIFT・DEFを左側へ、24桁×2行の文字領域を右側へ配置し、上下の余白を調整しました。
- PC-1261系のBUSY・PRINT・DEG・RAD・ERRORは、パネルの印刷文字に対応する長方形マークを表示します。ERROR右側の無名シンボルにも対応しました。
- 「BASICを保存」「マシン語を保存」を分け、指定したメモリ範囲をDMPファイルへ保存できるようにしました。アドレス入力には専用16進数パッドを使用し、入力した範囲は同じSession内で保持します。
- Androidのreleaseビルドにコード・リソースの縮小と最適化を適用しました。

### 利用上の注意

対応機種とROM登録方式は従来どおりです。ROM・ゲームは同梱しません。
RAM制限解除はエミュレータ内のRAM構成を変更する機能で、ROMのBASIC管理領域や容量認識を強制的に書き換える機能ではありません。
メモリ保存にはROM・I/O・未割当領域の読取り値も含められますが、DMPロードは書込み可能なRAMに限られます。保存した全範囲を再ロードできるとは限りません。
RAM・CPU状態のセーブステートはありません。更新・機種切替・RAM構成変更の前に必要なプログラムを保存してください。

今回、PC-1245・1251系・1261系のキー位置とPC-1261の全点灯配置の目視確認を終えています。
これらの確認は、最終の署名済みAAB／APK／iOS Archiveの受入れ完了を意味しません。
PC-1245・1251系のLCDシンボル全面改修は今回の対象外です。
StudioのRAM設定変更は関連する開発履歴ですが、この原稿でStudio 1.1.2の配布を予定・確定するものではありません。

## English

Preparation draft for Android and iOS Player 1.1.2; not a publication announcement.
Changes since the published 1.1.0 baseline, through `4679f00`:

- Added per-model RAM unlock controls for the PC-1251, PC-1261 and PC-1401 families. The default is off. Changes apply on the next Play action; restarting discards current RAM and loaded programs after confirmation.
- Updated PC-1245, PC-1251-family and PC-1261-family artwork and aligned the seven keys below the LCD with their touch regions and pressed highlights.
- Reworked the PC-1261-family LCD: left-side KANA/SML/SHIFT/DEF labels, a right-shifted two-line text area, balanced vertical spacing and rectangular segments for printed panel captions. Added the unlabelled segment to the right of ERROR.
- Separated BASIC and machine-code saving. Export a selected memory range as a DMP file using the hexadecimal address pad; the selected range is retained within the same session.
- Enabled code/resource shrinking and optimization for Android release builds.

ROMs and games are not included. RAM unlock does not patch firmware BASIC pointers or force additional BASIC capacity. Memory exports can include ROM/I/O values, while importing remains limited to writable RAM. There are no RAM/CPU save states: save programs before updating or changing models/RAM settings.
Layout acceptance and automated tests do not replace verification of the final signed packages. A complete PC-1245/PC-1251 LCD-symbol redesign and a Studio 1.1.2 distribution are outside this document's scope.
