# Roadmap

このロードマップは方向性を示すものであり、リリース日を保証するものではない。
各段階は小さな動作確認とテストを完了してから次へ進む。

## Phase 0: プロジェクト基盤

- [x] 構想書とアーキテクチャ文書を作成する
- [x] KMP CoreとCompose Desktopのビルド骨格を作成する
- [x] ライセンス、リポジトリ規約、CIを設定する
- [x] Emulator Coreの公開APIと責務を定義する
- [x] 最初の対象機種をPC-1245に決定し、初期機種仕様を整理する
- [x] Pokecom GOから採取するGolden Test Dataの形式を決める
- [ ] Pokecom GOおよび過去資産の由来を確認する

## Phase 1: Headless PC-1245

- [x] CPU状態モデルを定義する
- [x] SC61860の最小命令実装を移植する
- [x] PC-1245のメモリマップを実装する
- [x] ROMを`ByteArray`として読み込む
- [x] ROMをコンポーネント化し、PC-1245 Flat 32/64KiB Importerを実装する
- [x] PC-1245の物理ROM別8KiB＋16KiB Importerを実装する
- [x] `.pgrom` v1のmanifest検証とDesktop ZIP入出力を実装する
- [x] `reset`、`step`、`runCycles`を実装する
- [x] CPU、RAM Snapshotを実装する
- [x] LCD Snapshotを実装する
- [x] PC-1245キーマトリクスと公開キー入力APIを実装する
- [x] SC61860 I/O命令をPC-1245キーマトリクスへ接続する
- [x] SC61860の絶対分岐・呼出し・内部RAMスタック命令を移植する
- [x] SC61860の相対分岐命令を移植する
- [x] SC61860の基本レジスタ・zero/carry命令を移植する
- [x] SC61860の8bit即値演算・比較命令を移植する
- [x] SC61860のA-P間8bit演算・比較命令を移植する
- [x] SC61860の内部RAMポインタ・DPロード命令を移植する
- [x] SC61860の単byteメモリ転送命令を移植する
- [x] SC61860のX/Yインデックス・メモリアクセス命令を移植する
- [x] SC61860のAレジスタcarry経由シフト命令を移植する
- [x] SC61860の16bit加減算命令を移植する
- [x] SC61860の内部RAMブロック転送・交換命令を移植する
- [x] SC61860のDPブロック転送・交換命令を移植する
- [x] SC61860のブロック塗りつぶし命令を移植する
- [x] SC61860のpacked BCD加減算命令を移植する
- [x] SC61860の複数byteニブルシフト命令を移植する
- [x] SC61860のLOOP命令を移植する
- [x] SC61860のCASEテーブル分岐命令を移植する
- [x] SC61860のレジスタ・スタック補助命令とaliasを移植する
- [x] SC61860のDP即値論理・テスト命令を移植する
- [x] SC61860のF・Control出力ポート命令とI/O通知境界を移植する
- [x] SC61860のTEST命令をdivider latchとPC-1245 BREAKへ接続する
- [x] SC61860のMVWP内部サブルーチン型ブロック転送命令を移植する
- [x] SC61860のCUP・CDN X入力カウント命令を移植する
- [x] SC61860のWAIT命令を決定論的なサイクル消費として実装する
- [x] PC-1245 I/Oを集約し、OUTCをLCD有効状態へ接続する
- [x] PC-1245の論理Buzzerとプラットフォーム非依存Audio Snapshotを実装する
- [x] ローカル実ROMによるPC-1245ヘッドレス起動スモークテストを追加する
- [x] SC61860 dividerを命令サイクルから決定論的に生成する
- [x] Golden Test Data v1のPGP側JSON再生・比較基盤を実装する
- [x] 実ROMからPGP側Golden Snapshotを採取するエクスポーターを実装する
- [x] Pokecom GO既存Save State JSONのGolden Expectation Importerを実装する
- [x] Pokecom GO SharedPreferences XMLから`PREF_SC`を抽出するブリッジを実装する
- [ ] Pokecom GOの期待結果と`commonTest`で比較する

## Phase 2: macOS最小Desktop UI

- [x] 実時間をサイクル予算へ変換する共通Plannerを実装する
- [x] PC-1245従来形式ROM選択とSession生成
- [x] Run / Pause / Reset / Step
- [x] PC-1245 LCD Snapshot表示
- [x] 最低限の画面キー・物理キー入力
- [x] CPUレジスタ表示
- [x] Audio SnapshotをDesktop音声出力へ接続

## Phase 3: BASICクロス開発

- [ ] PC-1245 BASIC方言と文字コードを定義する（共通テキストescape parserは実装済み）
- [ ] BASIC Tokenizer / Detokenizerを実装する
- [ ] `.BAS`の読み込みと保存
- [ ] BASICプログラムのRAMへの展開と抽出
- [ ] 外部ファイル変更の再読み込み

## Phase 4: Desktop対応拡大

- [ ] Windowsでのビルド・配布
- [ ] Linuxでのビルド・配布
- [ ] ROM Import Wizardを実装する（入力slot一覧、inline検証、Pokecom GO形式から`.pgrom`への変換）
- [ ] バンク機の物理ROMをBank 0..Nの一覧で割り当て、一括選択できるようにする
- [ ] Desktop設定と最近使ったファイル
- [ ] パッケージ生成とリリース自動化

## Phase 5: 開発支援機能

- [ ] Debugger Core
- [ ] Disassembler
- [ ] Assembler
- [ ] 5×7 Character Editor
- [ ] WAV Encoder / Decoder

## Phase 6: 機種追加

機種追加の順序は、仕様資料、Golden Test Data、利用目的を確認して決める。

- [ ] 2機種目の選定
- [ ] Machine Definitionの拡張性検証
- [ ] 機種固有BASIC方言への対応
- [ ] PC-1250/1251/1255を共通ファミリーとユーザーメモリ容量差で定義する
- [ ] PC-1260/1261/1262を共通ファミリーとユーザーメモリ容量差で定義する
- [ ] PC-1401/1402のユーザーメモリ容量差をデータ定義で表現する
- [ ] PC-1360、PC-1460、PC-1470Uのバンク切替を機種別に実装する
- [ ] PC-1360KをMachine Catalogへ登録する（詳細不明のため実装は保留）

## Phase 7: Mobile

- [ ] Androidアプリ
- [ ] iOSアプリ
- [ ] Desktopとのファイル受け渡し

モバイル版にDesktop版の全機能を実装することは必須としない。
