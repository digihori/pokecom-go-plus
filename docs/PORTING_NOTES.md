# Porting Notes

この文書は、既存プロジェクトからPGPへ移植・再実装した技術の由来と判断を記録する。

## 参照リポジトリ

| 参照元 | 開発時の位置 | 用途 | 扱い |
|---|---|---|---|
| Pokecom GO | `../pokecom` | CPU、機種定義、LCD、キー、BASIC処理 | 読み取り専用 |
| pcwav | `../pcwav` | WAV Codec、転送プロトコル | 読み取り専用 |

PGPはこれらのリポジトリへビルド時または実行時に依存しない。

## 記録ルール

移植単位ごとに以下を記録する。

- PGP側の機能・クラス名
- 参照元リポジトリとコミットID
- 参照したファイルと範囲
- 流用した仕様またはアルゴリズム
- PGP向けに変更した設計
- 動作一致を確認するテスト
- ライセンス・由来の確認結果

## 記録テンプレート

```markdown
### 機能名

- Date:
- PGP files:
- Reference repository:
- Reference commit:
- Reference files:
- Provenance/license:
- Reused behavior:
- Design changes:
- Verification:
```

## 移植記録

コードの移植はまだ開始していない。以下は実装前の仕様抽出記録である。

### SC61860機種世代

- Date: 2026-10-02
- PGP files: `docs/MACHINE_FAMILIES.md`、`docs/ARCHITECTURE.md`
- Reference: Pokecom GO作者から提供された設計情報
- Reused behavior: OLD、S1、S2の世代分類と中間コードの相違。PC-1360、PC-1460、
  PC-1470Uのバンク切替。PC-1250/1251/1255、PC-1260/1261/1262、PC-1401/1402の
  ユーザーメモリ容量差。PC-1360Kの存在と漢字ROM追加
- Design changes: 中間コード方言とメモリバンクを別々の明示的な機種属性としてモデル化
- Verification: 各機種の実装時にトークン表とメモリ構成を個別確認する。PC-1360Kは詳細が
  判明するまで実装保留

### PC-1245初期機種仕様

- Date: 2026-10-02
- PGP files: `docs/machines/PC-1245.md`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `README.md`、`Sc61860Base.java`、`Sc61860_1245.java`、
  `KeyBoard1245.java`、`MainLoop1245.java`、`SubActivity1245.java`
- Provenance/license: 同一作者のPokecom GOを参照。コードは未移植
- Reused behavior: ROM配置、メモリ変換、キーマトリクス、LCD配置、リセット値
- Design changes: Android ID、描画座標、ファイルAPI、static状態を機種仕様から分離。
  `cmdHook()`は対象外
- Verification: 未実施。Golden Test Dataの採取後に確認する

### SC61860 CPU状態とリセット

- Date: 2026-10-02
- PGP files: `core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/cpu/Sc61860State.kt`、
  `core/src/commonTest/kotlin/com/digihori/pgp/core/emulator/cpu/Sc61860StateTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:39-79,236-270`、`Sc61860params.java:14-46`
- Provenance/license: 同一作者のPokecom GOから状態項目とリセット値を再設計して移植
- Reused behavior: CPUレジスタ、フラグ、タイマー、ポートラッチ、内部RAMサイズ、リセット値
- Design changes: Android、static状態、メインメモリ、画面、キー、実時間制御を分離。
  フラグをBoolean化し、リセット時に`currentProgramCounter`も0へ戻す
- Verification: commonTestで初期状態、全状態のリセット、インスタンス間の非共有を検証

### SC61860 Bus境界と最小命令実行

- Date: 2026-10-02
- PGP files: `Sc61860Bus.kt`、`Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:277-329,383-493,634-670,2418-2430,2489-2552`
- Provenance/license: 同一作者のPokecom GOから命令フェッチと最初の命令群を再設計して移植
- Reused behavior: 16bit PCラップ、LII/LIJ/LIA/LIB、NOPT aliases、命令サイクル数
- Design changes: メモリアクセスを`Sc61860Bus`へ分離。未実装opcodeは継続せず停止理由を返す。
  コマンドフック、実時間待機、画面更新、命令回数のglobal配列は移植しない
- Verification: commonTestで即値ロード、NOPT、未実装opcode、PCラップ、reset時のBus非変更を検証

### PC-1245 Memory BusとROM入力

- Date: 2026-10-02
- PGP files: `Pc1245MemoryBus.kt`、`Pc1245MemoryBusTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `README.md:13-18,26-30`、`Sc61860_1245.java:29-34,99-260`
- Provenance/license: 同一作者のPokecom GOからPC-1245のメモリ挙動を再設計して移植
- Reused behavior: 64KiB ROMコンテナ、読出しalias、書込みアドレス変換、RAM/LCD mirror、
  `0x8000..0xffff`のリセット
- Design changes: ファイル・URI・Android APIを除去し、`ByteArray`入力を防御的にコピー。
  CPU resetとRAM resetを分離し、範囲外アドレスを拒否
- Verification: commonTestでROMサイズ、コピー所有権、alias、書込み変換、mirror、RAM resetを検証

### RomSetとPC-1245 Flat 64KiB Importer

- Date: 2026-10-02
- PGP files: `RomSet.kt`、`Pc1245Rom.kt`、`Pc1245FlatRomImporterTest.kt`、
  `RomSetTest.kt`、`Pc1245MemoryBus.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `README.md:13-18,26-30`、`Sc61860_1245.java:99-139`
- Provenance/license: 同一作者のPokecom GO ROM配置形式からImporterとして再設計
- Reused behavior: 64KiBイメージの内部ROM `0x0000..0x1fff`、外部ROM `0x4000..0x7fff`
- Design changes: 未使用領域を破棄し、ROMを`internal`と`external`へ正規化。ByteArrayを防御的に
  コピーし、Memory Busは旧ファイル形式ではなく`RomSet`だけを受け取る
- Verification: commonTestで領域抽出、サイズエラー、ID重複、所有権、Memory Bus要件を検証

### PC-1245 Machine実行境界

- Date: 2026-10-02
- PGP files: `Pc1245Machine.kt`、`Pc1245MachineTest.kt`、`Sc61860Cpu.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `MainLoop1245.java:35-48`、`Sc61860Base.java:227-329`
- Provenance/license: 同一作者のPokecom GOからCPUと機種メモリの所有関係を再設計
- Reused behavior: PC-1245生成時のCPU/RAM resetと1命令実行
- Design changes: CPUとMemory BusをMachineインスタンスが所有。実時間ループを除去し、
  `runCycles`を決定論的な命令境界実行として追加。未実装opcodeでは停止する
- Verification: commonTestでcold reset、step、cycle budget超過、未実装opcode停止を検証

### Emulator Session公開APIとSnapshot

- Date: 2026-10-02
- PGP files: `core/api/EmulatorSession.kt`、`core/api/EmulatorFactory.kt`、
  `core/api/EmulatorFactoryTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:39-79,141-220,277-329`、`Sc61860params.java`
- Provenance/license: 同一作者のPokecom GO状態参照・保存対象から公開読出しモデルを再設計
- Reused behavior: CPUレジスタ、内部RAM、メインメモリの読出し
- Design changes: 可変内部状態を公開せず防御的コピーのSnapshotへ変換。生成時のROMエラーを
  result型に変換し、内部停止理由を公開faultへ変換
- Verification: commonTestでFactory、実行、エラー変換、Snapshot所有権、メモリ範囲を検証

### PC-1245 LCD状態とDisplay Snapshot

- Date: 2026-10-02
- PGP files: `Pc1245Display.kt`、`Pc1245DisplayTest.kt`、`EmulatorSession.kt`、
  `EmulatorFactory.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860_1245.java:203-225`、`MainLoop1245.java:20-32,74-162`
- Provenance/license: 同一作者のPokecom GOからLCDメモリ配置とシンボルbitを再設計して移植
- Reused behavior: 80列のLCD byte配置、後半20列の逆順配置、DEF/P/G/DE/BUSY/SHIFT/RAD
- Design changes: Android Canvasとrefresh callbackを除去。論理ドットをrow-major Snapshotへ変換し、
  状態変化時だけrevisionを更新
- Verification: commonTestでLCD両領域、逆順、シンボル、reset、Snapshot境界を検証

### PC-1245キーマトリクスと入力API

- Date: 2026-10-02
- PGP files: `Pc1245Keyboard.kt`、`Pc1245KeyboardTest.kt`、`EmulatorSession.kt`、
  `EmulatorFactory.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `KeyBoard1245.java:7-34`、`Sc61860_1245.java:262-358`、
  `SubActivity1245.java:100-114`、`SubActivityBase.java:425-431`
- Provenance/license: 同一作者のPokecom GOからキーマトリクスとモード接点を再設計して移植
- Reused behavior: 10列の有効matrix、IA/IB scan、RUN/PRO接点、BREAKの10回key-on signal
- Design changes: Android View IDを`PocketKey`へ置換。複数同時押しと冪等なpress/releaseを
  インスタンス状態で管理し、RUN/PROを表示Snapshotへ統合
- Verification: commonTestでmatrix、同時押し、mode、BREAK、reset、公開入力APIを検証

### SC61860 I/O命令とPC-1245入力接続

- Date: 2026-10-02
- PGP files: `Sc61860Io.kt`、`Sc61860Cpu.kt`、`Pc1245Keyboard.kt`、
  `Sc61860CpuTest.kt`、`Pc1245MachineTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:670-677,886-897,1754-1761,2042-2049,2274-2283,2369-2378`、
  `Sc61860_1245.java:262-358`
- Provenance/license: 同一作者のPokecom GOからポート命令とPC-1245入力接続を再設計して移植
- Reused behavior: LIP、EXAM、OUTA、OUTB、INA、INBの状態変化とサイクル数、
  IA/IBによるキーマトリクスおよびRUN/PRO接点の選択
- Design changes: CPUが機種クラスを直接参照せず`Sc61860Io`境界だけに依存する。
  未接続時はゼロを返す実装を既定値とし、入力値をbyteへ正規化する
- Verification: commonTestで命令単体のレジスタ・フラグ・サイクル数と、ROMコードから
  PC-1245のキーおよびPROGRAMモードを読み取る結合動作を検証

### SC61860絶対分岐・呼出し・スタック命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:761-778,2211-2233,2061-2120,2200-2221`
- Provenance/license: 同一作者のPokecom GOから制御フローと内部RAMスタックの挙動を
  再設計して移植
- Reused behavior: LIDP、PUSH、POP、CALL、RTN、JP、JPNZ、JPNC、JPZ、JPC、
  `0xe0..0xff`短縮CALLのアドレス計算、スタック格納順、サイクル数
- Design changes: 16bit/8bitラップとbig-endian word読出しをCPU内の共通処理に集約。
  分岐命令をホスト側の制御フローや例外へ結び付けず、CPU状態だけを更新する
- Verification: commonTestでデータポインタ、PUSH/POP、CALL/RTNの往復、短縮CALL、
  絶対分岐、およびzero/carry条件の成立・不成立を検証

### SC61860相対分岐命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1923-2060`
- Provenance/license: 同一作者のPokecom GOから相対分岐の挙動を再設計して移植
- Reused behavior: JRP、JRM、JRNZP、JRNZM、JRNCP、JRNCM、JRZP、JRZM、JRCP、
  JRCMの分岐基準、zero/carry条件、成立時7・不成立時4サイクル、`R-1`へのoffset記録
- Design changes: 前後方向と条件を共通処理へ集約し、PCと内部RAMのアドレスを明示的に
  ラップする。分岐先の基準がoffset operand自身のアドレスであることをテストで固定する
- Verification: commonTestで全条件の成立・不成立、前後方向、offset記録、16bit境界ラップを検証

### SC61860基本レジスタ・フラグ命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:550-556,983-995,1083-1266,2400-2417`
- Provenance/license: 同一作者のPokecom GOから基本レジスタ演算とフラグ更新を再設計して移植
- Reused behavior: CLRA、SC、RC、I/J/A/B/K/L/M/Nのincrement/decrement、Q選択、
  16bit ALU値、zero/carry判定、命令サイクル数
- Design changes: 8種類のレジスタ演算を共通処理へ集約し、格納する8bit値とフラグ判定に使う
  16bit ALU値を分離。CLRAがフラグを変更しない挙動を明示する
- Verification: commonTestで全opcodeと対象レジスタ、Q、通常値、`0xff + 1`、`0 - 1`、
  `1 - 1`、CLRAのフラグ保持、SC/RCのzero/carryを検証

### SC61860 8bit即値演算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:998-1049,1269-1428,1642-1670`
- Provenance/license: 同一作者のPokecom GOから8bit即値演算の挙動を再設計して移植
- Reused behavior: ADIM、SBIM、ANIM、ORIM、TSIM、CPIM、ADIA、SBIA、ANIA、ORIA、
  TSIA、CPIAの演算対象、書込み有無、zero/carry/ALU更新、4サイクル
- Design changes: 即値の読出しとPC更新を共通化し、AとP指定内部RAMを同じ演算処理で扱う。
  論理演算がcarryを保持し、testがALUも保持する挙動を明示する
- Verification: commonTestでA/P両対象、加減算境界、AND/ORの書込み、test/compareの
  非書込み、ALU・zero・carryの更新範囲を検証

### SC61860 A-P間8bit演算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:538-548,1027-1080,1287-1415`
- Provenance/license: 同一作者のPokecom GOからAレジスタとP指定内部RAM間の演算を
  再設計して移植
- Reused behavior: ADM、SBM、ANMA、ORMA、ADCM、SBCM、TSMA、CPMAのオペランド方向、
  P側への書込み、carry入力、zero/carry/ALU更新、3サイクル
- Design changes: carryなし・ありの加減算を共通処理にし、carryを数値化する箇所を限定。
  test/compareと書込み演算を別処理にして状態変更範囲を明示する
- Verification: commonTestで通常・境界・carry入力、論理演算のcarry保持、Aの保持、
  TSMA/CPMAの両オペランド非書込みとフラグ更新を検証

### SC61860内部RAMポインタ・DPロード命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:594-727,679-718,1171-1177,1267-1273`
- Provenance/license: 同一作者のPokecom GOからP/Q/RとDPの操作を再設計して移植
- Reused behavior: LIDL、LIP、LIQ、LDP、LDQ、LDR、STP、STQ、STR、INCP、DECP、
  `0x80..0xbf`のLP、7bit/6bit mask、命令サイクル数
- Design changes: Pの7bit増減を専用処理にし、通常の8bitレジスタ演算と分離。
  DP下位byteの置換時に上位byteを明示的に保持する
- Verification: commonTestでDP上位byte保持、即値mask、P/Q/RとAの相互転送、
  Pの7bit境界ラップ、LP全範囲の代表値、既存PUSH/POP回帰を検証

### SC61860単byteメモリ転送命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:558-636,729-753,967-979`
- Provenance/license: 同一作者のPokecom GOから単byte転送とnibble交換を再設計して移植
- Reused behavior: STD、MVDM、MVMP、MVMD、LDPC、LDD、SWP、LDMの転送方向、
  PC/DP/P保持、フラグ保持、命令サイクル数
- Design changes: 外部メモリ書込みを`Sc61860Bus`境界だけに限定し、内部RAMとの転送を
  CPU状態内に閉じる。PC相対読出しとDP読出しを別処理として明示する
- Verification: commonTestでA/PからDPへの書込み、DPからA/Pへの読出し、PC上byteの
  非消費、PC/DP/P保持、nibble交換、zero/carry保持を検証

### SC61860 X/Yインデックス命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1480-1575`
- Provenance/license: 同一作者のPokecom GOからX/Yインデックス操作を再設計して移植
- Reused behavior: IX、DX、IY、DY、IXL、DXL、IYS、DYSの16bitアドレス計算、
  pre-increment/decrement、DP/Q更新、Aへのload・Aからのstore、サイクル数
- Design changes: X/Yのlow registerを入力とする共通処理へ集約し、loadとstoreの同時指定を
  禁止。外部メモリアクセスは`Sc61860Bus`だけを経由する
- Verification: commonTestでX/Y各pair、増減、16bit境界ラップ、新アドレスからのload、
  新アドレスへのstore、DP/Qとhigh/low byteの更新を検証

### SC61860 Aレジスタシフト命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1581-1610`
- Provenance/license: 同一作者のPokecom GOからcarry経由シフトを再設計して移植
- Reused behavior: SL、SRのcarry入力、押し出しbitによるcarry更新、ALU値、Aへの8bit格納、
  zero保持、2サイクル
- Design changes: carryのBoolean表現を命令内で明示的に0/1またはbit 8へ変換し、
  8bit格納値と9bit ALU値を分離する
- Verification: commonTestで左右それぞれのcarry入力・出力、AとALU、zero保持を検証

### SC61860 16bit加減算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1432-1478`
- Provenance/license: 同一作者のPokecom GOから16bit加減算を再設計して移植
- Reused behavior: ADB、SBB、P指定little-endian word、B:A register pair、演算後のP更新、
  16bit zero/carry判定、ALU非変更、5サイクル
- Design changes: P側wordとB:A pairの組立てを専用処理に分離し、Pの上位byte位置を
  7bitラップで求める。結果格納とフラグ更新を共通化する
- Verification: commonTestで通常加算、16bit overflow、減算borrow、P境界ラップ、
  A/BとALUの保持、格納byte順を検証

### SC61860内部RAMブロック転送命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:785-821,879-925`
- Provenance/license: 同一作者のPokecom GOから内部RAMブロック転送・交換を再設計して移植
- Reused behavior: MVW、EXW、MVB、EXB、I/Jで符号化された`count - 1`、P/Qの逐次更新、
  Dの終了値、7bitラップ、可変サイクル数
- Design changes: I/Jとcopy/exchangeの差を共通処理へ集約し、命令開始時に転送数を確定する。
  逐次処理を維持して重複領域でも元実装と同じ順序になるようにする
- Verification: commonTestでI/J両系統、copy/exchange、複数byte、P/Q/D終了値、
  可変サイクル数、7bit境界ラップを検証

### SC61860 DPブロック転送命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:823-867,928-964`
- Provenance/license: 同一作者のPokecom GOからDP・内部RAM間のブロック転送と交換を
  再設計して移植
- Reused behavior: MVWD、EXWD、MVBD、EXBD、I/Jで符号化された`count - 1`、
  P/DPの逐次更新、完了時のDP decrement、Dの終了値、可変サイクル数
- Design changes: I/Jとcopy/exchangeの差を共通処理へ集約し、外部メモリアクセスを
  `Sc61860Bus`経由に限定。Pの7bitとDPの16bitラップを別々に扱う
- Verification: commonTestでI/J両系統、copy/exchange、双方向の値、P/DP/D終了値、
  可変サイクル数、P/DP境界ラップを検証

### SC61860ブロック塗りつぶし命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:869-895`
- Provenance/license: 同一作者のPokecom GOからAによるブロック塗りつぶしを再設計して移植
- Reused behavior: FILM、FILD、Iで符号化された`count - 1`、P/DPの逐次更新、
  FILD完了時のDP decrement、Dの終了値、可変サイクル数
- Design changes: 内部RAM用と外部Bus用を分離し、Pの7bit・DPの16bitラップと異なる
  終了位置を明示する。転送数は命令開始時に確定する
- Verification: commonTestで複数byte、P/DP/D終了値、P/DP境界ラップ、書込み値、
  可変サイクル数を検証

### SC61860 packed BCD演算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1717-1921`
- Provenance/license: 同一作者のPokecom GOからpacked BCD加減算と互換mask規則を
  再設計して移植
- Reused behavior: ADN、SBN、ADW、SBW、Iで符号化された`count - 1`、AまたはQ側operand、
  下位アドレス方向の桁送り、P/Q/D終了値、全桁zero、最終carry/borrow、可変サイクル数
- Design changes: BCD 1byte加算・減算と不正nibble正規化を共通関数へ分離し、複数桁命令は
  carry/borrowだけを次の桁へ渡す。不正A〜F桁を無効化するPokecom GO固有規則を明示する
- Verification: commonTestでA/Q両系統の加算carry・減算borrow、複数桁、P/Q/D終了値、
  zero/carry、可変サイクル数、`0xfa`の不正BCD maskを検証

### SC61860複数byteニブルシフト命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1612-1640`
- Provenance/license: 同一作者のPokecom GOから複数byteの4bitシフトを再設計して移植
- Reused behavior: SRW、SLW、Iで符号化された`count - 1`、Pの処理方向、byte間のnibble伝播、
  P/D終了値、ALU最終値、zero/carry保持、可変サイクル数
- Design changes: 左右の走査方向を別処理として明示し、各反復のshift bufferをIntで保持。
  内部RAMへの格納時だけ8bitへ制限する
- Verification: commonTestで2byteの左右4bitシフト、ALU、P/D終了値、zero/carry保持、
  可変サイクル数、Pの7bit境界ラップを検証

### SC61860 LOOP命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2123-2156`
- Provenance/license: 同一作者のPokecom GOからRスタック上のループ制御を再設計して移植
- Reused behavior: LOOPのoffset基準、分岐判定前のcounter値、判定後のdecrement、
  zero/carry/ALU更新、終了時だけのR increment、継続時10・終了時7サイクル
- Design changes: 分岐判定、counter更新、frame終了を順序どおり明示し、PCは16bit、Rは7bitで
  個別にラップする
- Verification: commonTestでcounter 2/1/0、分岐先、counterとフラグ、R保持・更新、
  7/10サイクル、R境界ラップを検証

### SC61860 CASEテーブル分岐命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2158-2209`
- Provenance/license: 同一作者のPokecom GOからCASEテーブル分岐を再設計して移植
- Reused behavior: CASE1のD設定と2byte stack格納、CASE2のA比較、3byte table entry、
  一致先・既定先への分岐、D残数、比較回数依存サイクル
- Design changes: table走査を明示的な反復処理とし、PC読出しを16bitラップ対応の共通処理へ
  統一。D=0のdo-while相当を256比較として保持する
- Verification: commonTestでCASE1のPC/D/Rと格納byte順、2件目一致、全件不一致、
  分岐先、D残数、可変サイクル数を結合検証

### SC61860レジスタ・スタック補助命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:568-589,755-760,879-885,2418-2433,2497-2560`
- Provenance/license: 同一作者のPokecom GOから純粋な補助命令とopcode aliasを再設計して移植
- Reused behavior: EXAB、LEAVE、RZ aliases (`0x72/73/76/77`)、NOPW aliases
  (`0x4d/ce/d3/d9`)、状態変更範囲、operand skip、命令サイクル数
- Design changes: 副作用のないNOPWはstep分岐から直接結果を返し、RZのaliasを共通処理へ集約。
  3サイクルのNOPT aliasesとは明示的に区別する
- Verification: commonTestでA/B交換とフラグ保持、R位置のclearとR保持、RZのPC/zero/carry、
  全NOPW aliasesの2サイクルと状態保持を検証

### SC61860 DP即値論理命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:568-589,1311-1325,1359-1373,1669-1682`
- Provenance/license: 同一作者のPokecom GOからDP上の即値論理演算と退避動作を再設計して移植
- Reused behavior: ANID、ORID、TSID、SZ、DP値の`R-1`退避、R非更新、即値消費、
  書込み後再読出しによるzero判定、carry/DP保持、6サイクル
- Design changes: 元値退避を共通処理にし、外部メモリの読書きを`Sc61860Bus`へ限定。
  ANID/ORIDは書込みを拒否するBusでも正しく判定できるよう結果を再読出しする
- Verification: commonTestでAND/OR結果、元値退避、TEST非書込み、SZのskip、PC/R/DP、
  zero/carry、命令サイクル数を検証

### SC61860 F・Control出力ポート命令

- Date: 2026-10-02
- PGP files: `Sc61860Io.kt`、`Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2287-2354`
- Provenance/license: 同一作者のPokecom GOからF/Control port latchとCPU内作用を再設計して移植
- Reused behavior: OUTF、OUTC、QとFO/Control latch、Control bit 1によるtimer/divider reset、
  bit 3によるpower状態、命令サイクル数
- Design changes: Android画面更新と直接Beep再生をCPUから除去し、`Sc61860Io`へF/Control出力通知を
  追加。表示・音声デバイスは後続の機種側実装で通知を解釈する
- Verification: commonTestで内部portからのlatch、Q、I/O通知、timer/divider resetの有無、
  power状態、3/2サイクルを検証

### SC61860 TEST命令とkey-on入力

- Date: 2026-10-02
- PGP files: `Sc61860Io.kt`、`Sc61860Cpu.kt`、`Pc1245Keyboard.kt`、
  `Sc61860CpuTest.kt`、`Pc1245MachineTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:277-325,365-384,2441-2470`、
  `SubActivityBase.java:425-431`
- Provenance/license: 同一作者のPokecom GOからTEST portとBREAK key-on pulseを再設計して移植
- Reused behavior: TESTのdivider500/divider2/key-on bit、divider latchのread-clear、即値mask、
  zero更新、4サイクル、BREAKによる10回のkey-on観測
- Design changes: key-on取得を`Sc61860Io.consumeKeyOnSignal()`へ分離。Android main loopの回数に
  依存する仮2ms timer生成は移植せず、divider生成用の決定論的時間源は別途設計する
- Verification: commonTestで全信号bit、mask、read-clear、zero、PC、I/O信号消費を検証し、
  PC-1245 Machine結合テストでBREAKからTESTまでの経路を確認

### PC-1245 I/O集約とLCD enable接続

- Date: 2026-10-03
- PGP files: `Pc1245Io.kt`、`Pc1245Machine.kt`、`Pc1245Keyboard.kt`、
  `Pc1245MachineTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2318-2354`、`MainLoop1245.java:74-162`
- Provenance/license: 同一作者のPokecom GOからOUTC bit 0によるLCD有効状態を再設計して移植
- Reused behavior: Control port bit 0のLCD enable、状態変化時の画面更新、reset時の表示有効化
- Design changes: KeyboardをSC61860全I/Oの実装とせず、`Pc1245Io`がKeyboardとDisplayを構成する。
  Android refresh callbackは使わず、Display revisionとSnapshotの`enabled`へ反映する
- Verification: commonTestでROM命令列からLIP/LIA/EXAM/OUTCを実行し、LCD disable、revision、
  Control latch、cold resetによる再有効化を結合検証

### PC-1245論理BuzzerとAudio Snapshot

- Date: 2026-10-03
- PGP files: `Pc1245Buzzer.kt`、`Pc1245Io.kt`、`Pc1245Machine.kt`、
  `EmulatorSession.kt`、`EmulatorFactory.kt`および各test
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2336-2351`、`Beep.java`
- Provenance/license: 同一作者のPokecom GOからControl portによるtone選択仕様だけを再設計して移植
- Reused behavior: Control bits 4-5の`0x00=off`、`0x20=2kHz`、`0x30=4kHz`、
  `0x10=変更なし`、reset時の停止
- Design changes: Android `AudioTrack`、thread、buffer生成をCoreへ持ち込まず、周波数とrevisionを持つ
  論理Buzzerに置換。公開`AudioSnapshot`をホスト音声実装との境界にする
- Verification: commonTestで全Control選択値、同値の冪等性、reset、ROMからOUTCまでの結合、
  公開Snapshot初期値を検証

### SC61860 MVWP内部サブルーチン型ブロック転送命令

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:515-533,2506,2582`
- Provenance/license: 同一作者のPokecom GOからMVWPの転送と特殊な制御フローを再設計して移植
- Reused behavior: B:Aを転送元アドレス、Pを転送先内部RAMアドレスとするI+1 byte転送、
  16-bit転送元と7-bit Pのwrap、Dの0終端、Rスタックを経由した次命令への復帰、`I*4+7`サイクル
- Design changes: 命令本体のPCを明示的にスタックへ退避し、既存の16/8/7-bit演算helperと
  `Sc61860Bus`境界でwrap規則を表現。Iは転送開始前に保存し、転送先との重複を許容する
- Verification: commonTestで3 byte転送、サイクル数、P/D/PC/R、A/B保持、および
  `0xffff -> 0x0000`の転送元wrapと`0x7f -> 0x00`の転送先wrapを検証

### SC61860 CUP・CDN X入力カウント命令

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1685-1717,2513,2523`
- Provenance/license: 同一作者のPokecom GOからCUP/CDNのX入力極性別カウント動作を再設計して移植
- Additional reference: [utz82/SC61860-Instruction-Set](https://github.com/utz82/SC61860-Instruction-Set)
  （CC0-1.0）の命令表
- Reused behavior: CUPはX入力low、CDNはhighの間Pを加算し、Dが`0xff`に達した場合にzeroを設定。
  反対極性ではPを保持してDをI+1にし、zeroを解除する。Pは7-bitでwrapする
- Design changes: 共通処理へ統合し、mutable globalではなくCPUインスタンス所有の`xInput`を参照する。
  現状の同期stepでは命令実行途中のX入力変化は扱わず、開始時のlevelが命令中継続する。
  Pokecom GOの`I*4`ではI=0時にCoreの正サイクル契約を破るため、命令資料の`1+I*4`を採用した
- Verification: commonTestで両命令の対象極性、反対極性、P/D/zero、サイクル数、7-bit wrapを検証

### SC61860 WAIT命令

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2433-2439,2513,2586`
- Additional reference: [utz82/SC61860-Instruction-Set](https://github.com/utz82/SC61860-Instruction-Set)
  （CC0-1.0）の命令表
- Provenance/license: Pokecom GOの命令配置を参照し、公開命令資料のサイクル定義で再実装
- Reused behavior: opcode `0x4e`、1 byteオペランド消費、CPU状態を変更しない待機命令
- Design changes: Pokecom GOのコメント付き仮補正`operand*6/4`は移植せず、命令資料に記載された
  `6+operand`サイクルとして表現する。wall clock待機やplatform sleepはCoreへ導入しない
- Verification: commonTestでoperand 0/255の6/261サイクル、PCと16-bit wrap、Pとflag保持を検証

### PC-1245実ROMヘッドレス起動スモークテスト

- Date: 2026-10-03
- PGP files: `RealPc1245RomSmokeTest.kt`、`README.md`
- Input: 利用者が`local-data/roms/pc-1245/pc1245mem.bin`へ配置した64KiBの実ROM
- Provenance/license: ROM内容は読込み時だけ使用し、テストコード、Git、CI成果物には含めない
- Behavior: Flat ROM Importerから公開FactoryとSessionを通し、cold reset後に100万サイクルを実行する
- Design changes: JVM/Desktop専用テストとしてファイルアクセスを`commonMain`から分離。
  `PGP_PC1245_ROM`または既定の`local-data`を探索し、ROMがない環境ではスキップする
- Verification: PC-1245実ROMで100万サイクルを完走し、Core faultなし、命令実行数と消費サイクル、
  machine IDを確認。ROMファイルはテスト後も変更しない

### SC61860決定論的divider生成

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`、`PC-1245.md`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:110,280-319,343-369,2318-2330,2441-2470`
- Additional references: PC-1245の576kHz仕様、`SC61860-Instruction-Set`の命令サイクル定義
- Provenance/license: Pokecom GOのdivider latchとリセット・読出し動作を再設計し、公開仕様の
  クロック値から周期を導出
- Reused behavior: TEST bit 0/1、read-clear latch、OUTC bit 1によるtimer reset
- Design changes: Androidループ100回ごとの仮2ms信号と外部`ticTac()`を廃止。576kHz発振を
  2 clocks/cycleとして、2msを576 cycles、500msを144,000 cyclesで生成する。
  wall clock、thread、Handlerを使わず、各命令が返すサイクルだけで進む
- Verification: commonTestで両周期の境界、余り、ラッチ保持、複数周期、TEST消費、OUTC reset後の
  位相再開を検証。実ROMスモークテストも100万サイクルを完走

### Golden Test Data v1形式

- Date: 2026-10-03
- PGP files: `docs/GOLDEN_TEST_DATA.md`、`test-data/golden/schema-v1.json`、
  `test-data/golden/README.md`
- Reference repository: `../pokecom`
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Provenance/license: Pokecom GOから期待結果を採取するためのPGP独自メタデータ形式。ROM内容は含めない
- Design: `pgp-golden-1` JSONでproducer revision、ROM SHA-256、順序付きAction、名前付きExpectationを
  記録する。CPU、選択RAM範囲、7×80論理LCD、symbol、audio、停止状態を独立に比較可能にする
- Distribution: 未審査の実ROM由来データはGit管理外の`local-data/golden/`へ隔離し、レビュー済みの
  最小データだけを`test-data/golden/`へ配置する
- Verification: Draft 2020-12 JSON Schemaを同梱し、必須項目、hex幅、SHA-256、Action種別、
  PC-1245 LCD寸法を機械検証できるようにした

### Golden Test Data PGP再生・比較基盤

- Date: 2026-10-03
- PGP files: `GoldenScenarioRunner.kt`、`GoldenScenarioRunnerTest.kt`、
  `pc1245-synthetic-reset.json`、Gradle version catalog
- Provenance/license: PGP独自のテスト支援コードと再配布可能な合成NOPT ROM用メタデータ
- Dependency: `kotlinx-serialization-json 1.11.0`をDesktop test source setだけで使用
- Design: CoreへJSON依存を持ち込まず、公開`EmulatorSession` APIでActionを順次実行する。
  ROM SHA-256を実行前に照合し、名前付きcheckpointで指定されたCPU、RAM、LCD、audio、実行状態だけを比較
- Verification: 64KiBをNOPTで埋めた合成ROMについてresetと2命令実行後を再生し、不一致ROMの
  SHA-256拒否もテスト。JSONはunknown fieldを拒否するstrict modeで読み込む

### PGP Golden Snapshotエクスポーター

- Date: 2026-10-03
- PGP files: `GoldenSnapshotExporter.kt`、`GoldenSnapshotExporterTest.kt`、READMEとGolden仕様
- Provenance/license: PGP独自のDesktopテスト支援コード。ROM byteは出力せずSHA-256だけを記録
- Design: `PGP_EXPORT_GOLDEN=1`でのみ書込みを許可し、producer revisionも明示入力を必須にする。
  reset直後と100万サイクル後のCPU、内部RAM、選択メモリ、LCD、audio、実行状態を採取する
- Local output: `local-data/golden/pc1245-pgp-boot-1000000.json`（Git管理外、14,152 byte）
- Verification: 合成ROMのencode/decode/replay round-tripを自動テスト。利用者提供PC-1245 ROMから
  実データを採取し、ROM SHA-256照合後に全checkpointを再生して一致を確認

### Pokecom GO Save State Importer

- Date: 2026-10-03
- PGP files: `PokecomGoStateImporter.kt`、`PokecomGoStateImporterTest.kt`、Golden仕様
- Reference repository: `../pokecom`（read-onlyを維持）
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860params.java`、`Sc61860Base.java:141-215`、
  `Sc61860_1245.java:37-67`、`SubActivityBase.java:263-320`
- Provenance/license: 同一作者のPokecom GO既存Gson Save State構造を読込み境界として再設計
- Design: `PREF_SC`のJSONからPC-1245 idを検証し、CPU、256 byte内部RAM、選択main RAM、
  80列LCD、symbol、RUN/PROをGolden Expectationへ変換する。参照リポジトリは変更しない
- Limitation: Save Stateには累積cycle/instruction数がないため、自由走行SnapshotはPGPの固定
  `runCycles`結果と比較しない。実行境界を固定できる採取方法が次段階で必要
- Verification: Gsonと同等の64KiB main RAMを含む合成JSONで全変換、signed byte mask、LCD、symbol、
  PC-1245以外の拒否をDesktop testで検証

### Pokecom GO SharedPreferences XMLブリッジ

- Date: 2026-10-03
- PGP files: `PokecomGoPreferencesReader.kt`、`PokecomGoPreferencesReaderTest.kt`、Golden仕様
- Reference repository: `../pokecom`（read-onlyを維持）
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `SubActivityBase.java:263-320`（default SharedPreferences key `PREF_SC`）
- Design: `adb exec-out run-as`で取得したpreferences XMLから対象stringだけを抽出し、XML entityを
  decodeしてSave State Importerへ接続する。Android APIやPokecom GOビルドへの依存は追加しない
- Security: DTD、外部general/parameter entity、external DTD/schema access、XIncludeを無効化
- Verification: Android SharedPreferences相当XMLの抽出とentity decode、key欠落、空値、XXE入力拒否を
  Desktop testで検証。Huawei ANE-LX2J上のdebug buildから実際のPC-1245状態を採取し、明示実行の
  `PokecomGoDeviceStateIntegrationTest`でCPU、RAM、LCD Expectationへの変換を確認

### PC-1245固定サイクル実機比較

- Date: 2026-10-03
- PGP files: `PokecomGoDeviceStateIntegrationTest.kt`、Golden仕様、本文書
- Reference repository: `../pokecom`はread-onlyを維持。一時debugコピーだけに採取フックを適用
- Capture boundary: Pokecom GOの命令ごとの`iTick`増分を累積し、100万cycles以上となる最初の
  命令境界で停止。PGPの`runCycles(1_000_000)`と同じ境界規則を使用
- Baseline finding: Pokecom GO本来の命令回数ベース2ms timerとAndroid wall-clockベース500ms timerでは
  CPU実行経路が分岐した。論理RAM `0x8000..0x87ff`は一致し、`0xf800..0xf8ff`は1 byte差
- Controlled finding: 一時debugコピーのdividerだけをPGPと同じ576/144,000 cyclesへ揃えると、両RAM範囲は
  byte単位で完全一致。CPUは6フィールド、内部RAMは8 byteが不一致で、アイドルループの位相差が残った
- Interpretation: 残差は累積命令数または命令別cycle値の相違を調べる必要がある。divider補正済み一時版は
  元実装そのもののGolden producerではないため、採取データを承認済み期待値へ昇格させない
- Local data: 元timer版とcycle timer版のpreferences XMLは`local-data/golden/`に保存しGit管理外とする
- Automated comparison: `PGP_VERIFY_POKECOM_GO_STATE=1`の明示的なDesktop integration testで、
  cycle timer版captureとPGPの100万cycle結果を比較する。`0x8000..0x87ff`、`0xf800..0xf8ff`、
  LCD dot/symbolは完全一致し、既知の内部RAM 8 byte差とCPU 6項目
  (`programCounter`、`currentProgramCounter`、`opcode`、`q`、`ib`、`testPort`)を固定する。
  これらの既知差分が増減した場合もテスト失敗として再評価する
- Trace tooling: `InstructionTraceRecorder`が公開Session APIの1命令stepを使い、累積命令/cycle、実行PC、
  opcode、命令cycle、実行後PC、Q、IB、TESTを安定したTSV列で出力する。指定件数のtailだけを保持して
  100万cycle調査時のメモリ使用を制限する。Pokecom GO一時debug copyも同じ列を出力し、最初の分岐を探す
- Trace finding: 起動直後からの全trace比較では、最初の相違は第1命令のWAIT (`0x4e`) のcycle値だった。
  operand `0xa0`に対しPokecom GOは`operand * 6 / 4 = 240`、PGPは公開命令資料に基づく
  `6 + operand = 166`を返す。命令順で比較すると最初の5,340命令はPC/opcode/実行後状態が一致し、
  第5,341命令のTEST (`0x6b`) で2ms dividerの到達時期によりTEST値が初めて分岐する
- Decision: この差は移植漏れではなく、Pokecom GO内にも「長すぎるので仮で1/4」と記された暫定cycle式と、
  PGPが採用した命令資料の定義との差である。Pokecom GOの最終位相へ合わせるためにPGPのWAITを戻さない。
  RAM/LCD一致はCPU機能移植の検証に用いる一方、固定cycle後のCPU位相差は既知の設計差として扱う

### ホスト実時間サイクル予算Planner

- Date: 2026-10-03
- PGP files: `CycleBudgetPlanner.kt`、`CycleBudgetPlannerTest.kt`、Core API、Roadmap
- Provenance: PGP独自設計。Pokecom GOのAndroid `Handler`、wall clock、loop回数による速度調整は移植しない
- Design: ホストが計測した単調経過時間だけを入力とし、PC-1245の288,000 cycles/secへ整数演算で変換。
  1 cycle未満を繰り越し、既定100msのcatch-up上限と破棄時間を返す
- Separation: Plannerは時計、thread、sleep、UI、`EmulatorSession`を所有しない。pause/resumeと実行呼出しは
  各platformのRunnerが担当する
- Speed: 1/2倍、通常、2倍を含む有理数倍率を使用し、浮動小数の累積誤差を避ける。倍率変更とtimeline
  resetでは旧端数を破棄する
- Verification: commonTestで1秒/1 frame換算、端数繰越、長時間停止の上限、倍率、reset、入力拒否を検証。
  Desktop JVMとiOS simulatorを含むKMP全体buildに成功

### Desktop Emulator Runner

- Date: 2026-10-03
- PGP files: `DesktopEmulatorRunner.kt`、`DesktopEmulatorRunnerTest.kt`、Desktop Gradle設定
- Design: JVMの`System.nanoTime()`を単調時計adapterで隔離し、`CycleBudgetPlanner`の予算だけを
  `EmulatorSession.runCycles()`へ渡す。RunnerはPAUSED、RUNNING、FAULTEDを明示的に管理する
- Lifecycle: runは同じ状態で冪等、pause/resumeは停止中の経過時間を破棄、resetはSession、速度、時刻基準を
  初期化する。速度変更時も旧倍率で経過した時間を新倍率へ課金しない
- Fault: Core faultを受けたら自動実行を停止し、resetまでrunを再開しない
- Separation: Compose timer、描画、ROM選択、入力処理はまだ接続せず、RunnerをUI非依存で検証可能に保つ
- Verification: fake monotonic clockとfake Sessionでcycle予算、pause/resume、run冪等性、reset、倍率変更、
  step、fault停止、時計逆行拒否をDesktop unit testで検証

### Desktop ROM選択と最小実行操作

- Date: 2026-10-03
- PGP files: `Main.kt`、`DesktopRomLoader.kt`、`DesktopRomLoaderTest.kt`、Roadmap
- Design: AWT file dialogとファイル読込みをDesktop層に限定し、PC-1245 legacy 64KiB imageを既存Importer、
  Factory、Sessionへ接続。ROM byteやファイルパスはCore APIや永続設定へ持ち込まない
- UI: ROM名、Runner状態、累積実行cycles、状態メッセージとSelect ROM / Run / Pause / Reset / Stepを表示。
  Compose coroutineが約16ms間隔でRunnerをtickするが、実行量はframe数でなく単調経過時間から決定する
- Error handling: 不正サイズ、読込み例外、Session生成失敗、Core faultを画面メッセージとして扱う
- Scope: この段階ではLCD、キー入力、CPUレジスタ表示、音声出力を接続しない
- Verification: legacy imageのSession生成と不正サイズをDesktop unit testで検証。全KMP build成功後、
  Desktopウィンドウの起動スモーク確認を実施

### Desktop PC-1245 LCD表示

- Date: 2026-10-03
- PGP files: `Main.kt`、`DesktopEmulatorRunner.kt`、Roadmap
- Design: Runnerから公開`DisplaySnapshot`だけを取得し、80×7論理dotとsymbolをCompose Canvasへ描画。
  Coreのmemory配列やPokecom GOのAndroid描画座標へ依存しない
- Layout: 利用可能幅からcell寸法を毎回算出し、最大幅800dpを超えないaspect-ratio指定とする。
  dotは各cell内の比率余白で描くため、ウィンドウscaleやHiDPIでも固定座標を使わない
- Refresh: Runner tickは約16ms間隔だが、Snapshot revisionまたは表示有効状態が変わった場合だけ
  Compose stateを更新する。CPU実行頻度とLCD再描画頻度を分離する
- Scope: LCD色は暫定theme値。実機外観skin、残像、contrast、backlight表現は後続UI設計で扱う
- Verification: Coreの既存LCD dot/symbol/revision testを描画入力契約として使用し、全KMP buildで
  Desktop UIのコンパイルを確認

### Desktop PC-1245キー入力

- Date: 2026-10-03
- PGP files: `DesktopKeyMapper.kt`、`DesktopKeyMapperTest.kt`、`Main.kt`、
  `DesktopEmulatorRunner.kt`とtest、Roadmap
- Mapping: Desktopの英字、数字列、numeric keypad、演算子、矢印、Enter、Space、Shift、Backspace/Delete、
  Escape、F1を論理`PocketKey`へ変換。Escape=BREAK、F1=DEFとする
- Key lifecycle: 物理key codeごとに押下中の論理keyを保持し、OS auto-repeatの重複KeyDownを無視。
  KeyUpではKeyDown時と同じ論理keyをreleaseするため、途中でShift状態が変わってもstuck keyを作らない
- Screen input: SHIFT、DEF、方向、BREAK、CLEAR、ENTERをpressからpointer releaseまで保持する画面キーとして追加
- Mode: RUN/PROを`OperatingMode` APIへ接続し、mode revisionによってLCD symbolを即時更新
- Separation: Composeのplatform非依存`Key`定数をDesktop Mapper内だけで使用し、Coreは`PocketKey`を維持。
  `nativeKeyCode`をJava/AWT定数として解釈しないためmacOS/Skikoのkey code差異を受けない
- Focus: `Window.onPreviewKeyEvent` で物理キーを受け取り、ボタンやROMファイルダイアログ後のComposeフォーカス状態に依存しない
- Verification: 文字、数字列、numeric keypad、control、navigation、shift演算子、未割当keyのmappingと、
  RunnerからSessionへのpress/release/mode委譲をDesktop unit testで検証

### Desktop CPUレジスタ表示

- Date: 2026-10-03
- PGP files: `Main.kt`、`DesktopEmulatorRunner.kt`、Roadmap
- Design: 公開`CpuSnapshot`だけをRunner経由で取得し、PC/current PC/opcode/DP、P/Q/R/D、ALU、flag、
  IA/IB/FO/Control/Testを固定幅hexで表示。CPU内部の可変stateへDesktopから直接アクセスしない
- Refresh: ROM読込み、reset、step直後は即時更新。通常実行中はLCD tick 6回ごと（概ね10Hz）に抑え、
  256 byte内部RAMを含むSnapshot copyとCompose再描画の負荷を制限する
- Scope: 内部RAM dump、memory viewer、disassembly、breakpointはDebugger phaseで追加する
- Verification: 全KMP buildで公開Snapshot APIとDesktop表示のコンパイルを確認

### Desktop Audio Snapshot出力

- Date: 2026-10-03
- PGP files: `DesktopAudioPlayer.kt`、`DesktopAudioPlayerTest.kt`、`Main.kt`、
  `DesktopEmulatorRunner.kt`、Roadmap
- Design: Coreの`AudioSnapshot(frequencyHz, revision)`だけを入力とし、Java Soundの16-bit mono PCM Clipへ変換。
  Coreはhost audio API、buffer、thread、volumeを所有しない
- Waveform: 44.1kHz、little-endian、振幅4096のsquare wave。sample rateと周波数のGCDからloop長を決め、
  2kHz/4kHzともperiod途中でloop境界を作らない
- Lifecycle: revision変化時だけClipを交換し、0Hz、Pause、Reset、ROM交換、Window破棄で停止・closeする。
  Resume後は同じrevisionでも再評価してtoneを復帰できる
- Failure handling: audio device取得失敗はResultとしてUIへ通知し、CPU実行や入力を停止しない
- Verification: fake Clipでstart/replace/silence/stop/restart、device failure、PCM長・endianness・入力拒否を
  Desktop unit testで検証

### PC-1245ホスト文字入力マップ

- Date: 2026-10-03
- PGP files: `Pc1245CharacterInput.kt`、`Pc1245CharacterInputTest.kt`
- Reference repository: `../pokecom`（read-onlyを維持）
- Reference files: `pc1245mainkey.png`、`include_keys_1245.xml`、`KeyBoard1245.java`
- Design: ホストの文字とPC-1245実キーを分離し、文字を機種固有のキー列へ変換する。PC-1245の
  SHIFTは同時押しmodifierではなくラッチ操作なので、`!`を`SHIFT`、`Q`の連続tapとして表現する
- Scope: この段階では純粋な変換表だけを追加し、Desktopイベントや実行タイミングには未接続。
  ソフトウェアキーボードは変換表を経由せず実キーを直接操作する
- Verification: 英字の大小、数字、直接入力記号、SHIFT刻印記号、未対応文字をcommonTestで検証

### サイクル駆動キー入力キュー

- Date: 2026-10-03
- PGP files: `KeyInputQueue.kt`、`KeyInputQueueTest.kt`、`DesktopEmulatorRunner.kt`とtest
- Design: ホスト文字から生成したキー列を、壁時計やCompose frameではなくエミュレーターの実行cycleで
  press / releaseする。既定では押下を60ms相当（17,280 cycles）、キー間隔を20ms相当
  （5,760 cycles）とする。Pokecom GOがkey-up後も20ms周期3回分を保持した挙動を下限にした
- Execution: Runnerは遷移境界まで`runCycles`予算を分割し、命令単位の超過は次phaseへ繰り越さず
  遷移をその命令直後へ遅らせる。返却する実行量と命令数は分割結果を合算する
- Lifecycle: pause、faultでは自動入力を破棄して保持キーをreleaseし、resetではSession resetとともに破棄する。
  画面キーと直接物理キーはキューの管理対象外
- Verification: press/hold/release/gap順、命令overshoot、cancel、Runnerでの予算分割とpause解放をテスト

### Desktop文字入力と直接キー入力の分離

- Date: 2026-10-03
- PGP files: `DesktopKeyboardInput.kt`、`DesktopKeyMapper.kt`とtest、`DesktopEmulatorRunner.kt`
- Design: 印字可能なホスト文字はPC-1245文字マップからサイクル駆動キューへ送り、矢印、Enter、
  CLEAR、BREAK、DEFは直接press/releaseする。ホストShift自体はPC-1245 SHIFTへ割り当てない
- Platform behavior: `Shift+1`等は物理キー位置でなくComposeが報告した文字`!`を使用するため、
  OSやキーボード配列にかかわらずPC-1245の`SHIFT`、`Q`へ変換される。Command/Ctrl併用はOSへ渡す
- Repeat: 同じ物理キーのKeyDown repeatはKeyUpまで抑制し、1回の文字入力として扱う
- Verification: shifted文字、host Shift無視、直接操作キー、repeat、Command/Ctrl、入力先交換時の解放を
  Desktop unit testで検証
- Focus lifecycle: native file dialogはmain windowをownerとして開き、閉じた後にownerへfocusを戻す。
  windowがfocusを失った場合は、届かなかったKeyUpによる押下状態を残さないよう、直接keyをreleaseして
  character repeat抑制状態もclearする

### Desktop PC-1245フルソフトウェアキーボード

- Date: 2026-10-03
- PGP files: `Pc1245KeyboardLayout.kt`、`Pc1245KeyboardLayoutTest.kt`、`Main.kt`
- Reference repository: `../pokecom`（read-onlyを維持）
- Reference files: `pc1245mainkey.png`、`include_keys_1245.xml`、`SubActivity1245.java`
- Layout: 実機相当の14列×4段に全キーを配置し、ENTERの2列幅と上段左側の空きを保持する。
  主刻印、SHIFT刻印、BASIC命令刻印をデータとして分離し、利用可能幅に対して等比で配置する
- Input: 各画面キーはホスト文字変換と自動入力キューを経由せず、対応する`PocketKey`をpointer downから
  release/cancelまで直接保持する。SHIFTも通常キーとしてROMへ渡す
- Verification: 全`PocketKey`が重複なく1回現れること、各行が14列内で重ならないこと、QのSHIFT刻印を
  Desktop unit testで検証

### Desktop開発用PC-1245 ROM自動起動

- Date: 2026-10-03
- PGP files: `DesktopRomLocator.kt`とtest、`Main.kt`、`run-pgp.command`、README
- Discovery: `PGP_PC1245_ROM`を優先し、未指定時は作業ディレクトリとその親から
  `local-data/roms/pc-1245/pc1245mem.bin`を探索する。ROM byteや絶対パスは保存しない
- Startup: ROMが見つかれば既存ImporterとFactoryでSessionを生成し、自動的にRUNを開始する。
  見つからない場合はエラーにせず従来のSelect ROM操作を維持する
- Distribution: `local-data`はGit管理外であり、配布物とCIにROMを含めない。自動探索は利用者が明示的に
  配置したローカルROMだけを対象にする
- Verification: 環境変数優先、相対パス、親ディレクトリ探索、ROM不在をDesktop unit testで検証

### Desktop LCD文字間隔

- Date: 2026-10-03
- PGP files: `CharacterCellGeometry.kt`とtest、`Main.kt`
- Design: Core SnapshotはLCD RAMに対応する16文字×5dot＝80列を維持し、Desktop描画geometryだけで
  各文字間へ1dot幅の空白を挿入する。末尾には空白を追加せず、表示上は80dot＋15gap＝95列となる
- Layout: Canvas aspect ratioも80列基準から95列基準へ変更し、空白追加で文字dot自体が横につぶれないようにする
- Verification: 文字境界の4→6、9→10、最終dotの79→94と、合計95列をDesktop unit testで検証

### BASICテキスト共通escape parser

- Date: 2026-10-03
- PGP files: `BasicTextParser.kt`とtest、`docs/BASIC_TEXT_FORMAT.md`
- Reference repository: `../pokecom`、`../pcwav`（ともにread-onlyを維持）
- Reference behavior: Pokecom GOの`\\PI`、`\\SQR`、`\\EX`、`\\BX`、literal backslashと、
  PCWAVの任意byte `\\xNN`およびunknown byteの可逆出力
- Design: UTF-8テキストを通常文字、論理特殊文字、Raw Byte、改行へ機種非依存で字句解析する。
  `π`と`√`は入力aliasとして受理するが、Raw Byteへ機種固有の文字意味を付与しない
- Error handling: unknown、末尾backslash、不完全または非hexのRaw Byteを行・列付きエラーにする
- Scope: この段階ではファイル選択、機種別コード解決、ROMキー列生成、RAM配置には接続しない
- Verification: BOM、CRLF/CR、全named escape、Unicode alias、literal backslash、`\\xNN`、位置付きエラーを
  commonTestで検証

### PC-1245 ROM BASICキー列Compiler

- Date: 2026-10-03
- PGP files: `Pc1245RomBasicInput.kt`とtest、`BasicTextParser.kt`、BASIC Text仕様
- Design: parse済みの通常文字と論理特殊文字をPC-1245の連続tapへ変換し、改行をENTERとする。
  最終行に改行がない場合だけENTERを補完する。元テキストの行・列は各tokenに保持する
- Special symbols: `\\PI`はSHIFT/0、`\\SQR`はSHIFT/DOT、`\\EX`はSHIFT/PLUSへ変換する。
  キーボード入力できない`\\BX`、Raw Byte、未対応文字は直接Tokenizerが必要なため拒否する
- Separation: CompilerはSession、実行cycle、ファイル、UIを所有せず、論理`PocketKey`列だけを返す
- Verification: 通常・shift文字、3種類の特殊文字、行ENTER、最終ENTER、BLOCK、Raw Byte、未対応文字と
  位置情報をcommonTestで検証

### Desktop BASIC ROM経由Merge読込み

- Date: 2026-10-03
- PGP files: `DesktopBasicLoader.kt`とtest、`DesktopEmulatorRunner.kt`とtest、`Main.kt`、README
- File boundary: Desktopだけがファイル選択とbyte読込みを担当し、UTF-8 decoderはmalformed/unmappable入力を
  replacementせず拒否する。CoreにはファイルパスやJVM charset APIを持ち込まない
- Execution: parse・compile済みキー列をPROGRAMモードで入力し、通常のhost-time Plannerを経由せず
  キー保持・間隔に必要なemulated cyclesを同期的に消化する。読込前がRUNNINGなら終了後に通常実行を再開する
- Semantics: 現段階は`NEW`を送信しないMerge方式。既存行と同じ行番号はROMの通常操作として置換される
- Failure: invalid UTF-8、escape parse、ROM入力非対応を区別して表示し、Core faultでは高速入力を停止する
- Verification: UTF-8成功・失敗、parse/compile error伝搬、即時キー遷移、実行状態復帰、pause維持をDesktop testで検証

### BASIC行番号直後のコロン正規化

- Date: 2026-10-03
- PGP files: `Pc1245RomBasicInput.kt`とtest、BASIC Text仕様
- Behavior: 各行の先頭行番号直後にある最初のコロンをSPACEキーへ変換する。行番号とコロンの間の
  空白を許容し、BASIC文本体に現れるコロンはPC-1245のSHIFT/Iとして保持する

### PC-1245 BASIC方言定義

- Date: 2026-10-04
- PGP files: `BasicDialect.kt`、`Pc1245BasicDialect.kt`とtest、PC-1245機種仕様
- Reference repository: `../pokecom`の`SubActivity1245.java`と`SubActivityBase12xx.java`をread-only参照
- Design: 方言ID、プログラム境界、行終端、行番号方式、文字、特殊記号、キーワードを機種依存データとして
  定義する。Tokenizer、RAM操作、UI、ホスト文字入力とは分離する
- Important distinction: 表示記号のπ/√ (`0x19`/`0x1a`) とBASICキーワードのPI/SQR
  (`0xbd`/`0x87`) は別の意味として保持する。予約・未割当コードは推測で埋めない
- Correction: 参照実装のtoken `0xb8`にある`MARGE`はマニュアル表記に合わせて`MERGE`と定義する。
  旧誤記をcanonical出力へ引き継がず、必要なら将来のテキスト入力aliasとして別に扱う
- Memory metadata: BASIC領域`0xc000`、開始pointer `0xc6e1..0xc6e2`、終了pointer
  `0xc6e3..0xc6e4`をPC-1245固有定義として保持する
- Character aliases: `0x11`と`0x50`はいずれも空白へdecodeするが、新規encodeでは`0x11`をcanonicalに使う
- Verification: envelope、3桁OLD行番号方式、代表文字、大小文字正規化、特殊記号とkeywordの区別、
  未対応文字・keywordをcommonTestで検証

### PC-1245 BASIC Tokenizer

- Date: 2026-10-04
- PGP files: `Pc1245BasicTokenizer.kt`とtest、`BasicDialect.kt`、`Pc1245BasicDialect.kt`
- Design: parse済みの共通BASICテキストを、`0xff` program envelope、OLD系2 byte行番号、機種固有の
  keyword/character code、`0x00`行終端へ変換する。エミュレーターSessionやRAMへは直接書き込まない
- Lexical rules: 行番号直後の任意colonを除去し、通常の構文空白は格納しない。文字列内はkeyword化せず、
  REM以降は先頭の区切り空白だけを除いて文字として保持する。`>=`、`<=`、`<>`は単一tokenにする
- Long lines: ROMの編集bufferを経由しないため、100文字を超える行も中間コードへ直接変換できる。
  ROM経由入力は手軽な短い入力用として別経路のまま維持する
- Safety: 行番号は1..999。Raw Byteの`0x00`と`0xff`はprogram framingを壊すため拒否し、
  未対応文字は元テキストのline/column付きerrorを返す
- Verification: 複数行program、文字列/REM、特殊記号、Raw Byte、比較演算子、行頭colon、180文字の行、
  行番号・文字errorをcommonTestで検証

### PC-1245 BASIC Detokenizer

- Date: 2026-10-04
- PGP files: `Pc1245BasicDetokenizer.kt`とtest、PC-1245 BASIC方言定義
- Design: OLD系program envelopeと行番号を検証し、文字・特殊記号・keywordをcanonical BASICテキストへ
  戻す。RAMやファイルI/Oには依存せず、入力破損はbyte offset付きerrorとして返す
- Preservation: 文字列とREM内のkeyword code、および予約・未知codeは命令として展開せず`\\xNN`へ退避する。
  π/√はUnicode alias、指数・block記号は曖昧な隣接escapeを避けるためRaw Byte表記をcanonicalに使う
- Formatting: keyword境界へ必要な空白を補い、Tokenizerが生成したprogramは
  Tokenize → Detokenize → Tokenizeで同じbyte列へ戻る
- Verification: 読みやすい複数行出力、文字列/REM、特殊文字、未知code、往復一致、開始/終了marker、
  不正行番号、行終端欠落、trailing dataをcommonTestで検証

### PC-1245 BASIC Program Memory Loader

- Date: 2026-10-04
- PGP files: `Pc1245BasicProgramMemory.kt`とtest、`EmulatorSession.kt`、`EmulatorFactory.kt`
- Design: program imageとmachine memoryの変換をTokenizer、ファイルI/O、UIから分離する。公開Sessionには
  `loadBasicProgram`と防御的copyを返す`basicProgramSnapshot`だけを追加し、任意メモリ書込みは公開しない
- Layout: ROMが管理する開始pointer `0xc6e1..0xc6e2`を読み、pointer直前`0xc6e1`までを格納上限とする。
  終了pointerはimage最終byteを指すPokecom GO互換のinclusive形式で更新する
- Atomicity: pointer、program構造、容量を全て検証してから書き込む。短いprogramへ置換した場合は、
  以前の終了位置までの残骸をzero clearする
- Verification: load/extract一致、pointer更新、旧tail clear、不正pointer、壊れたimage、容量超過、
  error時のmemory不変をcommonTestで検証

### Desktop BASICファイル入出力

- Date: 2026-10-04
- PGP files: `DesktopBasicLoader.kt`とtest、`Main.kt`、`DesktopEmulatorRunner.kt`
- Load BASIC: UTF-8 `.bas`を厳密decodeし、共通parser、PC-1245 Tokenizer、専用Session loaderを通して
  RAMへ直接配置する。Runnerは先にpauseし、長い行と特殊codeをROM編集bufferなしで扱う。
  load前がRunningなら成功・失敗後ともRunningへ戻し、load前がPausedならPausedを維持する
- Save BASIC: Session snapshotをDetokenizerへ渡し、canonical UTF-8テキストとして保存する
- Type BASIC: 従来のROM経由キー入力は短いprogramを手軽に入力する別機能として残し、UI名で区別する
- Error reporting: UTF-8、parse、tokenize、memory pointer/capacity、detokenize、host file I/Oを段階別に表示する
- Verification: Desktop codecでUTF-8 sourceからprogram image、canonical sourceへの復元、invalid UTF-8、
  tokenize errorをunit testで検証
- Verification: `10:PRINT A:B`と`20 :PRINT`についてprefixだけがSPACEになることをcommonTestで検証

### PC-1245追加VRAMミラー

- Date: 2026-10-03
- PGP files: `Pc1245MemoryBus.kt`、`Pc1245MemoryBusTest.kt`、PC-1245機種仕様
- Provenance: PGP作者から提供された非公式実機情報。Pokecom GO参照実装には未反映
- Behavior: 従来の`0xf800..0xffff`に加え、`0xe800`、`0xe900`、…、`0xef00`の各256 byte pageも
  同じVRAMのmirrorとして扱う。どのpageへの書込みも`0xf800..0xf8ff`へ正規化して全16 pageへ反映する
- Verification: `0xe900`と`0xffff`への書込みが`0xe800..0xefff`、`0xf800..0xffff`の対応offsetから
  同一値として読めることをcommonTestで検証

### `.pgrom` v1 manifestとDesktop ZIP入出力

- Date: 2026-10-04
- PGP files: `RomPackageManifest.kt`とtest、`DesktopRomPackage.kt`とtest、
  `DesktopRomLoader.kt`、`Main.kt`、ROM Package仕様、Roadmap
- Core: manifestとcomponentをKMP共通モデルとして定義し、format/version、ID、role、相対path、
  component ID/path重複、宣言size、SHA-256、実entryとの一致をまとめて検証する。成功時だけ`RomSet`を生成する
- Desktop: ZIPとUTF-8 JSON、SHA-256をJVM層で扱い、`.pgrom`の読込みと`RomSet`からの書出しを実装する。
  Select ROMは拡張子`.pgrom`を判別し、従来64KiB ROMとの両方を読み込める
- Safety: 圧縮ファイルサイズ、entry数、entry単体と展開後合計の上限を設け、絶対path、backslash、空segment、
  `.`、`..`、entry重複を拒否する。manifest未記載entryはv1仕様どおりwarningとして保持する
- Verification: package往復、Session生成、digest不一致、path traversal、共通validatorの複合errorとwarningをテストする

### PC-1245 Legacy ROMから`.pgrom`へのDesktop変換

- Date: 2026-10-04
- PGP files: `DesktopRomPackageConverter.kt`とtest、`Main.kt`、README
- Flow: `Convert ROM`で32KiBまたは64KiB legacy imageを選択し、既存`Pc1245FlatRomImporter`で正規`RomSet`へ変換後、
  `.pgrom` writerで保存する。変換元ファイルを直接ZIPへ入れず、未使用領域はパッケージへ持ち込まない
- Verification: 生成packageをreaderで再読込みし、machine ID、内部8KiB、外部16KiBを確認する。
  不正なlegacy imageはpackageを生成する前に拒否する
- Compatibility: PC-1245で必要な最大ROM addressは`0x7fff`のため32KiB形式を正規入力として許可する。
  Pokecom GO互換64KiBも維持し、後半32KiBのダミー内容に関係なく同じ`.pgrom` componentを生成する
- Real ROM verification: ローカルROMが存在する場合、64KiB実データを`.pgrom`へ変換して再読込みし、
  component構成、warningなし、Session生成、100万cycleでfaultせず起動することをDesktop smoke testで確認する。
  ROMがないCIではテストをskipし、ROM byteと生成packageは保存・コミットしない

### PC-1245物理ROM別Importer

- Date: 2026-10-04
- PGP files: `Pc1245ComponentRomImporter`とtest、`DesktopRomPackageConverter.kt`とtest、
  `Main.kt`、README、ROM Package仕様
- Standard input: PC-1245は内部ROM 8KiBと外部ROM 16KiBを別ファイルとして受け取る。
  ファイル名やPokecom GOのaddress-space containerには依存せず、UIで選択したslotと正確なサイズを検証する
- Compatibility: 32/64KiB Flat ImporterはPokecom GO互換のLegacy経路として維持し、標準package作成経路とは分離する
- UI: `Convert ROM`を`Create ROM Set`へ変更し、内部ROM、外部ROM、保存先を順に選択する。
  manifestとSHA-256はPGPが生成し、利用者による編集を要求しない
- Bank-ready design: 将来のバンク機ではMachine Definitionがbank数を定義し、16KiBの各物理ROMを
  bank番号slotへ割り当て、正規`RomSet`では番号順に連結する
- Temporary UI limitation: 現在の`Create ROM Set`はPC-1245専用の仮実装で、内部ROM、外部ROM、保存先の
  native file dialogを順番に表示する。2ファイルでは利用できるが、バンク機へこの方式を拡張しない
- Deferred Import Wizard: 1つの画面に必要なcomponent/bank slot、選択済みファイル名、期待size、検証結果を
  一覧表示する。物理ROM別入力とPokecom GO互換入力を切り替え、Legacy Flat/Split形式から`.pgrom`を
  作成できるようにする。複数bankの一括選択と順序確認もこの画面で扱う
- Current legacy behavior: PC-1245のPokecom GO互換32/64KiB imageは`Open ROM`から直接実行できるが、
  Desktop UIから`.pgrom`へ保存する操作は未提供。Core/Converterの変換経路は維持し、Wizardから接続する
### Cycle-timed SC61860 buzzer PCM

- Reference: `../pc1251-emulator/pc1251emu/audio.py` and the C-port event timing in
  `../pc1251-emulator/pc1251emu/machine.py` (MIT License).
- PGP files: `Pc1245Buzzer.kt`, `DesktopAudioPlayer.kt`, and their tests.
- Porting decision: Pokecom GO's command hooks, inferred frequencies, `Beep.java`, and fixed
  Android `AudioTrack` clips are not ported. PGP records the SC61860 C-port mode against emulated
  CPU cycles, averages transitions within each PCM sample, applies the piezo-style high-pass
  filter, and streams the resulting PCM through a platform adapter.
- Modes 0/4 are LOW, 1/5 are HIGH, 2 is the internal 2 kHz oscillator, 3 is the internal 4 kHz
  oscillator, and cassette-derived modes 6/7 are currently rendered LOW.

### PC-1251 ROM, memory, and LCD foundation

- Reference: `../pc1251-emulator/pc1251emu/machine.py` (MIT License) and Pokecom GO's
  `Sc61860_1251.java` / `MainLoop1251.java` for comparison.
- PGP files: `core/.../machine/pc1251/Pc1251Rom.kt`, `Pc1251MemoryBus.kt`, and `Pc1251Display.kt`.
- PC-1251 is implemented as a separate Machine rather than adding model branches to PC-1245.
- The physical writable ranges are 0xB800..0xC7FF and 0xF800..0xF8FF; 0xB000..0xB7FF aliases
  0xB800..0xBFFF. The 120 LCD columns use ascending 0xF800..0xF83B and descending
  0xF87B..0xF840 addresses.

### 製品名とプラットフォーム別の役割

- Date: 2026-10-04
- Project codename: リポジトリとプロジェクト全体は引き続き`Pokecom GO Plus（PGP）`と呼ぶ。
- Desktop product: macOS／Windows／Linux向けの統合開発環境を`Pokecom GO Studio`とする。
  エミュレータに加え、デバッガ、アセンブラ、ディスアセンブラ等の開発支援機能を提供する。
- Mobile product: Android／iOS向けの実行専用エミュレータを`Pokecom GO Player`とする。
  既存Pokecom GO相当の実行・操作機能を対象とし、Studioの開発支援機能は搭載しない。
- Shared foundation: StudioとPlayerは同じKotlin Multiplatform Emulator Coreを使用する。
  UI、ファイル選択、音声出力、アプリのライフサイクルは各製品・プラットフォーム層に置く。

### PC-1251機種別文字入力とキーボード表示

- Date: 2026-10-04
- PGP files: `Pc1251CharacterInput.kt`、`Pc1245KeyboardLayout.kt`、`DesktopKeyboardInput.kt`、`Main.kt`とtest
- Character mapping: ホスト文字`(`と`)`は、PC-1245では`SHIFT`→`1`／`2`、PC-1251では
  `SHIFT`→`↓`／`↑`へ変換する。Desktop入力は接続中のEmulator Sessionのmachine IDから変換規則を選ぶ。
- Software keyboard: PC-1251では括弧を矢印キーのSHIFT側へ表示し、数字`1`／`2`から除く。
  PC-1245の固定予約語ラベルはPC-1251へ流用しない。
- Deferred behavior: PC-1251のユーザー登録可能な予約語ショートカットはROMの通常動作に任せ、Studio側での
  登録内容の解釈・表示は当面実装しない。
- Shared hardware: 現時点で同一と確認済みのキーマトリクス走査は`Pc1245Keyboard`を共有し、文字入力規則と
  画面上のlegendだけを機種別に分離する。

### PC-1251 RSVモード入力

- Date: 2026-10-04
- Reference: Pokecom GO `Sc61860_1251.inb()`および`SubActivity1251`の3位置モードスイッチ。
- PGP files: `Pc1251Keyboard.kt`、`EmulatorSession.kt`、`EmulatorFactory.kt`、`Main.kt`とtest
- Design: 物理キーのmatrix走査はPC-1245実装へ委譲し、PC-1251固有のRUN／PRO／RSV接点だけを
  `Pc1251Keyboard`で実装する。`OperatingMode.RESERVE`を公開APIへ追加した。
- Display: RUN／PRO／RSV表示は架空のLCD RAM byteから推測せず、モードスイッチの状態をSnapshotへ反映する。

### PC-1251 ROM・LCDアドレスミラー

- Date: 2026-10-04
- Reference: Pokecom GO `Sc61860_1251.memr()`／`memw()`。
- ROM: `0x2000..0x3fff`の読出しを外部ROM `0x4000..0x5fff`へ割り当てる。書込み可能領域にはしない。
- LCD: `0xe800..0xefff`および`0xf900..0xffff`を、下位8bitが同じ`0xf800..0xf8ff`へ
  正規化する。どのミラーページへ書き込んでもLCD Snapshotと全ミラー読出しへ同じ値が反映される。
- RAM: `0x8000..0x9fff`を`0xa000..0xbfff`へ、`0xd000..0xd7ff`を`0xc000..0xc7ff`へ写す。
  PC-1250はPC-1245と同じく`0xb000..0xb7ff`と`0xb800..0xbfff`を最終的に
  `0xc000..0xc7ff`へ写す。PC-1251は`0xb000..0xb7ff`を`0xb800..0xbfff`へ写す。
  PC-1255では`0xb000..0xbfff`が独立RAMなのでミラーとして潰さない。
  PC-1250実ROMはBASIC program pointerを論理address`0xb830`へ設定し、ミラー経由で物理RAM
  `0xc030`へ格納する。
- Open question: PC-1245／1250の`0xb000..0xb7ff`および`0xb800..0xbfff`が実機で本当に
  `0xc000..0xc7ff`のミラーとして動作するかは未確認。現在はPokecom GOの書込み変換と実ROM起動結果に
  合わせた実装を維持するが、資料または実機上のread/write試験で再検証する。確認できるまでは、この挙動を
  確定仕様として他機種へ一般化しない。

### PC-1251 ROM経由BASIC入力

- Date: 2026-10-04
- PGP files: `Pc1251RomBasicInput.kt`、`Pc1245RomBasicInput.kt`、`DesktopBasicLoader.kt`、`Main.kt`とtest
- Design: BASIC行解析と行番号直後のcolon処理は共有し、文字から実機キー列への変換だけを機種別に注入する。
- PC-1251: `(`／`)`を`SHIFT`→`↓`／`↑`としてROM editorへ入力する。
- Special symbols: PC-1251キーボード資産`pc1251mainkey.png`で、`\\PI`＝`SHIFT`→`0`、
  `\\SQR`＝`SHIFT`→`.`、`\\EX`＝`SHIFT`→`+`を確認して対応する。キーlegendのない`\\BX`は
  行・桁付きのunsupported errorを返す。

### PC-1251 LCDシンボル整理

- Date: 2026-10-04
- Reference: Pokecom GO `MainLoop1251.state`とシンボル描画。
- Symbol bytes: `0xf83c`はDEF／P／G／DE、`0xf83d`はBUSY／SHIFT／RADとして扱う。
- Mode: RUN／PRO／RSVはLCD RAMではなく3位置モードスイッチの状態からSnapshotへ加える。
- Removal: PC-1251には存在しない`E`表示と、モード表示用に仮定していた`0xf83e`の監視を削除した。

### PC-1251実ROMスモークテスト

- Date: 2026-10-04
- PGP file: `RealPc1251RomSmokeTest.kt`
- Local input: `PGP_PC1251_ROM`、または`local-data/roms/pc-1251/pc1251mem.bin`を探索する。
- Verification: 実ROMを100万cycle実行してfaultしないこと、24文字×5dotのLCD Snapshot、
  RUN／PRO／RSVの3位置モード表示、`A`キー入力後のROMによるLCD更新を確認する。さらにPROモードで
  `10 PRINT (1)`を機種別キー列として入力し、ROMが保存した中間コードを再抽出・復号して元の行と照合する。
- Input timing: PC-1245用の短い自動キー間隔ではPC-1251実ROMが連続する`10`の`0`を取りこぼした。
  StudioではPC-1251に38,400cycle保持＋19,200cycle解放間隔を使用し、実ROM統合テストで欠落がないことを確認する。
- CI: ROMが存在しない環境ではテストをskipし、ROM byteやdigestを出力・保存しない。

### PC-1251 OLD BASIC token table確認

- Date: 2026-10-04
- Reference: Pokecom GO `SubActivity1245.cmd_tbl`と`SubActivity1251.cmd_tbl`。
- Finding: 0x00..0xffの全entryが一致するため、PC-1245とPC-1251は現在のOLD系Tokenizer／Detokenizerを
  共有できる。プログラム格納addressとROM経由の文字キー列は引き続き機種別に扱う。
- Studio: ROM選択guide、ROM経由入力error、Tokenizer errorには実行中または選択中の機種名を表示し、
  PC-1251操作中にPC-1245と表示されないようにする。

### PC-1251初回起動ROM検出

- Date: 2026-10-04
- PGP files: `DesktopRomLocator.kt`、`Main.kt`、READMEとtest
- Priority: 前回ROM履歴を最優先する。履歴がない場合は明示した環境変数、PC-1245、PC-1251の
  既定local-dataの順に探索する。
- Override: PC-1251は`PGP_PC1251_ROM`で絶対pathまたは作業directory相対pathを指定できる。

### PC-1250／1251／1255ファミリーとRAM容量

- Date: 2026-10-04
- Shared implementation: 3機種はROM構成、CPU、LCD、キーボード、OLD BASIC処理を共有し、
  `Pc1251FamilyModel`のmachine IDとRAM開始addressだけを機種差として持つ。
- Hardware ranges: PC-1250は`0xc000..0xc7ff`、PC-1251は`0xb800..0xc7ff`、
  PC-1255は`0xa000..0xc7ff`を搭載RAMとして扱う。既存のaddress mirrorは搭載範囲へ正規化した後で判定する。
- Emulator extension: `Pc1251FamilyMemoryMode.HARDWARE`は実機容量を再現し、`EXPANDED`は機種名にかかわらず
  PC-1255相当の10KiB RAMを公開する。従来のPGP動作とエミュレータ上の利便性を保つため、Coreの既定値は
  `EXPANDED`とした。
- ROM identity: 同じ物理ROM layoutを各machine IDのROM setへimportできる。Studioの機種選択、ROM経由入力、
  software keyboard、RSV modeも3機種を同じファミリーとして扱う。
- BASIC memory: Tokenizerが直接配置するprogramの下限は、最大容量モデルに合わせて`0xa000`とする。
  実際の開始位置はROMが初期化したprogram pointerに従う。
- Studio control: PC-1250／1251／1255選択時は`Expanded RAM`と`Hardware RAM`を切り替えられる。
  切替時は同じROMを選択中の設定でcold bootし、切替前が実行中なら実行を再開する。RAM、CPU、表示などの
  runtime stateは保持しない。ROM未読込時の選択は次回loadへ適用する。
