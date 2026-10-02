# Roadmap

このロードマップは方向性を示すものであり、リリース日を保証するものではない。
各段階は小さな動作確認とテストを完了してから次へ進む。

## Phase 0: プロジェクト基盤

- [x] 構想書とアーキテクチャ文書を作成する
- [x] KMP CoreとCompose Desktopのビルド骨格を作成する
- [x] ライセンス、リポジトリ規約、CIを設定する
- [ ] Pokecom GOから採取するGolden Test Dataの形式を決める
- [ ] Pokecom GOおよび過去資産の由来を確認する

## Phase 1: Headless PC-1245

- [ ] CPU状態モデルを定義する
- [ ] SC61860の最小命令実装を移植する
- [ ] PC-1245のメモリマップを実装する
- [ ] ROMを`ByteArray`として読み込む
- [ ] `reset`、`step`、`runCycles`を実装する
- [ ] CPU、RAM、LCD Snapshotを実装する
- [ ] Pokecom GOの期待結果と`commonTest`で比較する

## Phase 2: macOS最小Desktop UI

- [ ] ROM選択
- [ ] Run / Pause / Reset
- [ ] LCD表示
- [ ] 最低限の画面キー・物理キー入力
- [ ] CPUレジスタ表示

## Phase 3: BASICクロス開発

- [ ] PC-1245 BASIC方言と文字コードを定義する
- [ ] BASIC Tokenizer / Detokenizerを実装する
- [ ] `.BAS`の読み込みと保存
- [ ] BASICプログラムのRAMへの展開と抽出
- [ ] 外部ファイル変更の再読み込み

## Phase 4: Desktop対応拡大

- [ ] Windowsでのビルド・配布
- [ ] Linuxでのビルド・配布
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

## Phase 7: Mobile

- [ ] Androidアプリ
- [ ] iOSアプリ
- [ ] Desktopとのファイル受け渡し

モバイル版にDesktop版の全機能を実装することは必須としない。
