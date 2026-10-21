# Player プログラムのロード・保存

iPhone版の現在の操作は[同梱ヘルプ](PLAYER_HELP_IOS.md)、全機種の実装・受入れ区分は[手順8](../development/IOS_MACHINE_SUPPORT_STEP8.md)を参照。
以下の操作説明・機種範囲はAndroid版について記載する。実装・端末確認の履歴は[別記録](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離した。

PC-1245／PC-1251／PC-1261／PC-1350／PC-1360／PC-1360K／PC-1401／PC-1450／PC-1460／PC-1470U Android Playerの3点メニューから操作する。ロード形式はUTF-8 BASICテキスト
`.bas`とアドレス付きPGP Memory Dump `.dmp`。Androidのセーブは全機種でBASIC（.bas）のみ。
DMPはロード専用で、形式選択と保存アドレス指定は提供しない。bin、hex、WAV、Projectは対象外。

## ロード

1. 「プログラム」→「ロード」でCPUを一時停止し、Androidのファイル選択画面を開く。
2. ファイル名の拡張子からbas／dmpを自動判定し、内容を検証する。
3. 検証成功後は上書き確認を挟まず直接ロードする。
4. BASICはプログラム全体を置換する。dmpは記載アドレスのRAMを上書きする。
   BASIC開始位置（PC-1245はC000、PC-1401は3800、PC-1350は2030）やBASIC管理ポインタへの書込みも許可するため、BASICが壊れる場合がある。
5. BASIC／dmpロード成功後はROMのCPU実行を自動再開する。BASICプログラムを実行するには、
   ポケコン側のRUNコマンドを使用する。ロードがRUNを自動入力することはない。

ファイルは2 MiBまで。bas／dmp以外の拡張子、不正なUTF-8／書式、容量超過、
ROM／LCD／制御領域への書込み、ミラー重複は拒否する。
BASICの保存は有効なBASIC管理領域を必要とする。BASICロードは機種別のCore配置規則に従い、dmpロードはBASICの状態に依存しない。
キャンセル・検証失敗ではメモリを変更せず、PAUSEDを維持する。
Resetは「エミュレータ操作」から行う。Reset後はPC-1245がRUN、PC-1401がCALとなり、CPUを自動実行（RUNNING）する。


## PC-1251の形式・範囲

- 既存CoreのOLD BASIC codecを使用。標準開始B800、開始／終了pointer C6E1／C6E3。
  実ROMが初期化した有効pointerを尊重し、格納上限はC6E1未満。管理情報が不正な場合は拒否する。
- Playerは実機容量（HARDWARE）を使用し、RAM拡張の選択は提供しない。
- DMPはB800〜C7FFとCoreのRAM aliasだけを扱う。A000等の拡張専用範囲・ROM・LCDは拒否し、
  同じ物理RAMへ重なるmirror範囲も拒否する。管理領域の直接上書きでBASICを壊す場合がある。
- DMPロード例はC500〜C5FF。専用予約領域ではなくBASICと共有するRAM。検証状況は[対応記録](../development/ANDROID_PC1251_PROGRESS.md)を参照。

## PC-1350の形式・範囲

- BASICは既存CoreのS1形式を使用する。標準開始位置は2030、
  開始ポインタは6F01〜6F02、終了ポインタは6F03〜6F04（リトルエンディアン）。
  有効な開始ポインタを尊重し、保存領域は6F00未満とする。
  容量超過は書込み前に拒否する。
- UTF-8のASCII・半角カナを扱う。全角日本語などの未対応文字はエラーになる。
  S1の任意バイトや特殊文字すべてのテキスト往復を保証するものではない。
- DMPは2000〜7FFFのうちCoreがRAMと分類するアドレスだけを扱う。
  この範囲には不連続なLCD領域と783Cのシンボル領域があり、いずれも拒否する。
  BASIC管理ポインタ等のRAMは直接上書きできるため、BASICを破壊し得る。
- DMPロード例は6000〜60FF。BASICと共有するRAMであり、予約領域や安全な専用領域ではない。
  DMPロードは非連続セグメントも受け付ける。


## PC-1360／PC-1360Kの形式・範囲

- 既存CoreのS2 BASIC codecを使用。標準開始8030、開始／終了pointer FFD7／FFD9、格納上限FFD7未満。
- DMPは8000〜FFFFのRAMへロードできる。PC-1360Kのみ2000〜27FFの追加RAMも許可する。
  通常ROM・LCD・バンク／漢字／キー制御レジスタは拒否する。
- 初回起動のRAM消去確認が出た場合は、本体と同様に確認操作を済ませてからBASICをロードする。
  ロード後にROMがRAMを初期化すると、ロードした内容が失われる。
- BASICのみセーブできる。DMPセーブ・RAM拡張設定は提供しない。
- 自動検証・エミュレータ確認と実機確認は[PC-1360](../development/ANDROID_PC1360_PROGRESS.md)／[PC-1360K](../development/ANDROID_PC1360K_PROGRESS.md)の記録で区別する。

## PC-1450／PC-1460／PC-1470Uの形式・範囲

| 機種 | BASIC | 標準開始 | start/end pointer | writer上限（exclusive） | DMPロード先 |
|---|---|---|---|---|---|
| PC-1450 | 専用S1 | 2030 | 5F01／5F03 | 5C30 | 2000–5FFF |
| PC-1460 | 1450系S1 | 8030 | FF01／FF03 | FF01 | 8000–FFFF |
| PC-1470U | 専用語彙のS2 | 8030 | FFD7／FFD9 | FFD7 | 8000–FFFF |

有効な開始pointerを尊重し、容量超過や不正pointerは書込み前に拒否する。
writer上限はCoreのソフトウェア境界であり、変数・作業領域を含む実機最大空き容量の保証ではない。
制御／LCD／バンクROM領域はDMPで書き換えられない。RAM上のBASIC／管理情報は上書きできる。
初回RAM消去確認が出た場合はY／ENTERなどの本体操作を済ませてからBASICをロードする。
BASICのみ保存でき、DMP保存・RAM容量設定は提供しない。検証区分は各機種の対応記録を参照。

## セーブ

「プログラム」→「セーブ」でCPUを一時停止し、現在のBASICプログラムをbasへ変換する。
「ファイルへセーブ」でAndroidの保存先選択画面から新規ファイルを作成する。
全Android機種でBASICのみとし、DMPセーブ・形式選択・開始／終了アドレス入力は設けない。
DMPのロードとCore／Studioのダンプ機能は引き続き利用できる。

他のアプリへの送信・共有機能はない。既存ファイルへの直接上書きは含めない。
セーブの成功・キャンセル・失敗でエミュレータのメモリは変えない。
書込み先でのI/O失敗では作成したファイルが不完全に残る場合があるため、完了表示を確認する。
実行中に開いた場合は終了後に同じSessionのCPUを再開し、停止中に開いた場合は停止を維持する。
キャンセル時も同じ方針。Sessionが変わった場合は再開しない。
通常の画面反転では操作状態を保持するが、プロセス終了後に未完了の操作は復元しない。

## 端末確認項目

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## メニューとLCD表示

ホームは設定（ROM管理／コントローラ設定）とヘルプ・情報の階層パネル。
プレイ画面はエミュレータ操作／プログラム／動作モード／表示／ホームへ戻る。
端末の戻る操作は子階層から親へ、親から閉じる。メニュー表示中はゲームパッド入力を抑止する。
LCDのシンボル・動作モード文字は計測して枠内に収め、端末の文字サイズ設定で欠けない描画にする。
メニュー・ダイアログは通常の文字サイズ設定に追従する。
端末では文字サイズを変えて通常／拡大表示、メニュータップ時のステータスバー、Reset後の自動実行を確認する。

## 機種・ROM

- ホームの「設定」→「ROM管理」で保存済みROM一覧を開く。ホームに表示される対応機種を選択可能。
  PC-1401は2026-10-15に今回の対応を一区切りとした。機器別の未確認事項は[対応記録](../development/ANDROID_PC1401_PROGRESS.md)を参照。PC-1350も2026-10-16にユーザーが一通りの確認を終え、今回の対応を一区切りとした。段階別の自動検証・エミュレータ確認と機器別の未確認事項は[対応記録](../development/ANDROID_PC1350_PROGRESS.md)に分ける。
- 未登録なら「取込み」、登録済みなら「置換」。pgromまたは選択機種のlegacy ROMを選択する。
  pgromの機種不一致は拒否し、legacyは選択機種を明示して解釈する。
- 取込みは検証・新Session生成の後に保存する。既存ROMの置換または使用中Sessionを変更する場合は、
  RAM／ロード済みプログラムが失われることを確認してから確定する。
- 「使用する」は保存済みROMから新Sessionを生成して切り替える。現在使用中の機種には無効。
- 「削除」は確認後に保存ROMを削除する。使用中ならSessionも破棄し、ROM未登録画面へ戻る。
  別機種へ自動で切り替えることはない。
- ROM管理を開くと一時停止し、キャンセル・失敗時は既存SessionとRAMを保持する。
  検証失敗時は保存済みROMを変更しない。保存途中で失敗した場合は変更済みファイルの復元を試みる。
- 成功した取込み／置換／機種切替はRUNモード・CPU自動実行。通常／拡大の表示選択は維持する。
  未実行の自動開始は、背景に移った場合は画面復帰まで待機する。

## 情報

ホームの「ヘルプ・情報」にはPlayerとは、Studioについて、プライバシーポリシー、バージョン・ライセンスを置く。
原文はビルド時に同梱し通信なしで整形表示する。Studioの外部リンクはブラウザ等を開く。プライバシーポリシーの原文案・適用状態と配布時の注意事項は、同梱原文と対象リリースの説明を確認する。

## PC-1245 Androidの今回の区切り・確認結果

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Androidホーム分離・ステップ1（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Androidメニュー再配置・ステップ2（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Android RUN/PRO常設切替・ステップ3（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Androidヘルプ・情報・ステップ4（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## 文言・操作レビューの反映（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## セーブ・SHIFT・モードボタンの追加修正（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Physical入力のフォーカス修正（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## 短押し欠落と復帰後抑止の修正（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Player専用フォーカスの所有（2026-10-15）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## PC-1401対応の今回の完了

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## iOS PC-1245・手順2（2026-10-17）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## 2026-10-19: AndroidセーブをBASICに統一

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## Androidシリーズ内のモデル選択（2026-10-20）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。

## iOS複数機種対応（2026-10-21）

この節は記録時点の内容です。[Player入出力・画面操作の実装履歴](../development/PLAYER_PROGRAM_IO_HISTORY.md)へ分離しました。
