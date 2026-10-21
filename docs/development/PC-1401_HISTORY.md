# PC-1401 調査・実装・検証履歴

2026-10-22に機種資料から分離しました。「未実装」「現在」等は各記録時点の状態です。
現行の機種情報は[PC-1401](../reference/machines/PC-1401.md)、実機向けツールは[ツール一覧](../../tools/README.md)を参照してください。

## 7. PC-1402を容量違いだけと扱わない理由

| Item | Pokecom GO 1401 | Pokecom GO 1402 |
|---|---|---|
| CLOAD hook | `9e25` | `9e0c` |
| CSAVE hook | `9d20` | `9d00` |
| BEEP hook | `c014` | `c08e` |
| Keyboard / tokens / LCD | 共通 | 共通 |
| RAM write guard | `2000..7fff` | 同じ広い窓 |

MAMEは`rom_pc1402 = rom_pc1401`とし、RAM mapだけを変える。
Pokecom GOの別ROMファイル / hook差と不一致なので、機種差・ROM版差・参照実装の誤りを
今回だけでは判定できない。1402を1401のRAM設定として公開せず、追加時には独立MachineId、
実ROM版、NEW/MEM、program開始位置、管理pointer、cold/warm resetを個別に検証する。
将来共通化するとしてもRAM decodeとROM identityを別の属性にする。

## 8. PGP統合と実装順序

1. **機種Coreと起動**: `pc1401/`へRomDefinition / importers、MemoryBus、Display、Keyboard、
   Io、Machineを追加。`MachineFamily.PC_1401` / keyboard layout / Catalog / Factoryへ登録。
   ROM layoutは既存8K+32K、generationと基本byte形式はS1。RAM容量設定は初版で公開しない。
   memory trace / physical ROM provenance / checkpointにも対応。synthetic ROM単体テスト後、実ROM起動で未確定事項を解消。
2. **BASICと入力**: PC-1401専用語彙、program memory、CharacterInput / ROM入力列を実装。
   S1共通フレームとPC-1401特殊文字を分離。LCD状態を基準にmode遷移を扱う。
3. **Studio**: ROM library / loader / component converter / locator、機種選択、Project machine ID、
   BASIC compiler / import / export、addressed machine-code loaderへ追加。
   `DesktopKeyboardInput`とlayout、Main.ktのmode / symbol / ROM登録分岐を更新。
   関数キーを省略したPC-1251 layoutへのfallbackはしない。
4. **Player**: `playerShared`の`PlayerKeyboardCatalog`, `PlayerSkinCatalog`, Session / display状態、
   Android ROM登録 / 起動、`controllerUi`のLCD・mode描画を確認して追加。
   Studio export成果物がmachine ID付きでPlayerへ渡る経路を検証。
   既存gamepadのPocketKey mappingを維持し、新しい機種キーの保存・読込みも確認。

共有するのはCPU、ROM component形式、snapshot / session境界、S1の行構造、
project / addressed image / host input / audio出力経路。
専用実装はmemory decode、LCD symbolと並び、キーmatrix / 刻印 / character sequence、
mode遷移、BASIC語彙 / pointerと容量判定。大きなFactory再編はPC-1401追加の前提にしない。

## 8a. 初版の画面レイアウトとPlayer後続検討

2026-10-11、ユーザーとの協議によりPC-1401初版は縦配置を採用する。
LCD → 関数キー6列×3行 → 主キー（英字・数字・矢印・操作キー）の順とする。
現在の幅基準のサイズ追従を維持し、LCDは縦横比を保つ。
LCD右側への関数キー配置や、広幅／狭幅による配置切替は初版では実装しない。
右側配置を後から追加する場合は、上段の基準寸法と共通倍率、最小キーサイズを別途設計する。

関数キーと主キーは別のgridとして定義し、キーIDと通常・SHIFT・BASIC刻印は共通定義を参照する。
配置と表示状態はCoreへ置かず、StudioとPlayerから参照できるpresentation層で所有する。
既存のpress/release経路を使い、CALでも英字キーを自動非表示にしない。
CAL/BASIC・SHIFT・C-CEなどへのアクセスを維持する。
Playerの既存SkinDefinitionは任意rectangle配置が可能だが、画像skinは初版の必須条件にはしない。

### Playerの拡張キー領域（後続検討、今回未実装）

PC-1470Uなど、キー数が多くLCDも24桁×2行となる機種では、全キーの常設により
モバイル画面上のLCDとキーが小さくなる問題がある。
後続案は通常表示をLCD＋主キーとし、関数キー等を拡張領域として必要時に表示する方式。
ユーザーからの例は、トリガーで上からスライド表示し、キーを押すと引っ込む動作。
これは検討候補であり、トリガー、表示位置、自動収納条件は未確定とする。

後続検討ではSHIFT＋関数、長押し、連続入力を妨げない表示・収納タイミング、
LCDを隠す範囲、キーrelease前の収納による押下状態の残留、表示中のサイズ追従を確認する。
初版では関数キーと主キーの定義を分離するところまでとし、拡張領域の仕組みは実装しない。

## 9. 検証計画と今回実施した確認

commit可能なテストは自作synthetic ROMと仕様由来のvectorだけを使う。
ROM、ROMから採取したtrace / RAM snapshot / screenshot / golden dataはcommitしない。

- ROM: componentサイズ、machine ID、所有権コピー、flatからROM領域だけ抽出、ROM書込み禁止。
- Bus: RAM両端、hole、LCD alias、ROM不変、cold resetとprogram保持warm resetの区別。
- LCD: 全80 columns、逆順境界、7 rows、symbol各bit、enable、revision、snapshot不変性。
- Keys: 全物理キー、select各bit、同時押し、IA/IB複数選択、BREAK/KON、mode sequence。
- BASIC: 機種固有関数 / 非対応語彙、文字列 / REM / DATA、line length境界、特殊文字、
  空program、末尾FF、pointer範囲、容量超過時の部分書込み防止、load→LIST→extract。
- 実ROM: cold boot→CAL、CAL/BASIC→RUN/PRO、四則と関数、短いBASIC RUN、
  ROMキー入力と直接loadの一致、NEW/MEM、BEEP / WAIT / TESTの時間、reset時のprogram保持。
  Pokecom GOとは同じROM・同じキーイベント・同じ命令境界で照合し、違いを実機不正と即断しない。
- UI: Studio standalone / Project、ROM登録2形式、外部編集→reload、Player export / launch、
  mode / symbols / 関数キー、既存機種選択とgamepad回帰。

今回実施: baselineとclean tree確認、参照実装・回路図比較。
`local-data/roms/pc-1401/`には8 KiB / 32 KiBのcomponentと64 KiB flat imageがあり、
flatのROM領域がcomponentと一致することだけを確認した。gitignore対象。
PC-1401のエミュレータ実行や実機照合はまだ行っていない。
取扱説明書全体の確認、RAM decodeの厳密なalias、ROM版差、program capacity、
mode遷移 / reset / 時間系は実装初段の検証項目として残る。

指定JDK 21 (`jbr-21.0.11`)で`./gradlew --gradle-user-home .gradle-local build :core:desktopTest`
は成功（既存testは多くがUP-TO-DATE）。`git diff --check`も成功。
変更は調査文書とPORTING_NOTESだけであり、このbuild成功はPC-1401の動作検証を意味しない。


## 10. 初版実装の到達点 (2026-10-11)

第9節の「今回実施」は調査時点の記録。現在はCore、BASIC、Studio、Playerの初版統合を実装し、
ローカル実ROMの起動・モード遷移・科学関数を含むBASIC実行・BEEP PCM生成・
ROM入力と直接ロードの一致を確認した。JDK 21の全体buildとCore回帰テストは成功。
詳細な移植判断と検証範囲は `docs/development/PORTING_NOTES.md` の「PC-1401初版実装」に記録した。

自動入力は実ROMで欠落が出ない保持40,000 / 間隔20,000 cyclesを採用。
192,000 cycles/sを基準とし、実機時間・音程との比較、最大容量と変数領域、
NEW/MEM、warm reset、Android実機の表示と操作感の確認は残る。
PC-1402対応およびPlayerの拡張領域は含まない。

登録メニューはシリーズ代表のfamily表示に集約。実行時の容量は個別の機種選択で指定する。
ユーザー操作では1402 ROMで1401選択時に1401容量のMEM値となった。1402選択時のMEM／BASIC実行や全ROM版互換を確認したとは扱わない。
