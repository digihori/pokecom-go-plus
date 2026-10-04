# Emulator Core Public API

**ステータス:** Initial API contract  
**対象:** `core/src/commonMain`  
**互換性:** 実装開始前のため変更可能

## 1. 目的

この文書は、PGP Emulator CoreとPokecom GO Studio（macOS／Windows／Linux）、
Pokecom GO Player（Android／iOS）の境界を定義する。
CPU命令や機種固有回路の内部設計ではなく、アプリケーションがCoreを安全に操作するための
公開契約を対象とする。

公開APIは`com.digihori.pgp.core.api`パッケージへ置く。CPU、Bus、メモリマップ、周辺回路などの
実装詳細は`pgp.emulator`配下へ置き、公開APIから参照させない。

## 2. 設計原則

1. 1つの`EmulatorSession`は1台の仮想ポケコンを表す。
2. APIは同期的であり、Core自身はスレッドやCoroutineを生成しない。
3. 同じ初期状態と入力列から、同じ実行結果とイベント列を得られるようにする。
4. UI、時計、ファイルパス、URI、音声デバイス、OSキーコードをCoreへ渡さない。
5. 外部へ返すSnapshotとバイト列はコピーとし、Coreの可変状態を公開しない。
6. 実行制御と実時間同期はアプリケーション層のRunnerが担当する。

## 3. API全体像

```text
Platform App
   │
   ├── MachineCatalog ── 対応機種と必要ROMの照会
   ├── EmulatorFactory ─ Session生成
   │
   └── EmulatorSession
        ├── reset / step / runCycles
        ├── pressKey / releaseKey
        ├── snapshot / readMemory
        └── saveState / restoreState
```

初期APIの概念形を以下に示す。実装時に命名や細部を変更する場合は、本書も同じ変更で更新する。

```kotlin
package com.digihori.pgp.core.api

interface EmulatorFactory {
    fun supportedMachines(): List<MachineDescriptor>
    fun create(machineId: MachineId, romSet: RomSet): CreateSessionResult
}

sealed interface CreateSessionResult {
    data class Success(val session: EmulatorSession) : CreateSessionResult
    data class Failure(val error: CoreError) : CreateSessionResult
}

interface EmulatorSession {
    val machine: MachineDescriptor

    fun reset(kind: ResetKind = ResetKind.COLD)
    fun step(): StepResult
    fun runCycles(cycleBudget: Long): RunResult

    fun pressKey(key: PocketKey): InputResult
    fun releaseKey(key: PocketKey): InputResult

    fun snapshot(): EmulatorSnapshot
    fun readMemory(range: AddressRange): MemoryReadResult

    fun saveState(): SaveState
    fun restoreState(state: SaveState): RestoreStateResult
}
```

`EmulatorFactory`の具象実装はライブラリ側が提供する。アプリケーションが`Machine`や
`Sc61860Cpu`を直接組み立てる構造にはしない。

## 4. 識別子と機種情報

識別子を文字列や整数の即値としてAPI全体へ拡散させない。

```kotlin
data class MachineId(val value: String)

data class MachineDescriptor(
    val id: MachineId,
    val displayName: String,
    val family: MachineFamilyId,
    val generation: MachineGeneration,
    val requiredRoms: List<RomRequirement>,
    val display: DisplayDescriptor,
)

data class RomRequirement(
    val id: RomId,
    val expectedSize: Int,
    val required: Boolean,
)

data class RomImage(
    val id: RomId,
    val bytes: ByteArray,
)

data class RomSet(val images: List<RomImage>)
```

CoreはROMのファイル名や保存場所を決めない。プラットフォーム層がファイルまたはURIから
バイト列を読み、Coreは機種ID、ROM種別、サイズなどを検証する。ROMイメージをリポジトリや
配布物へ含めない。

### 4.4 BASICプログラムメモリ

Tokenizerが生成した機種固有のprogram imageは、任意メモリ書込みではなく専用APIでSessionへ渡す。

```kotlin
session.loadBasicProgram(programBytes)
session.basicProgramSnapshot()
```

Loaderはprogram構造、開始・終了pointer、格納容量を全て確認してから書き込む。Snapshotは内部配列を
公開せず、`copyBytes()`で防御的copyを返す。ファイル選択、UTF-8 decode、Tokenizer/Detokenizerの選択は
このAPIの外側で行う。

## 5. 実行モデル

### 5.1 Step

`step()`は原則としてSC61860命令を1命令実行する。命令に付随するタイマー、I/O、LCDの進行も、
その命令が消費したサイクル分だけ同じ呼出し内で進める。

```kotlin
data class StepResult(
    val cycles: Int,
    val status: ExecutionStatus,
)
```

### 5.2 RunCycles

`runCycles(cycleBudget)`は命令境界を維持しながら、指定サイクル数を上限の目安として実行する。
最後の1命令により`executedCycles`が`cycleBudget`を超えることを許容する。ゼロ以下のbudgetは
プログラミングエラーとして拒否する。

```kotlin
data class RunResult(
    val executedCycles: Long,
    val instructions: Long,
    val status: ExecutionStatus,
)

sealed interface ExecutionStatus {
    data object Ready : ExecutionStatus
    data object Halted : ExecutionStatus
    data class Breakpoint(val address: Int) : ExecutionStatus
    data class Faulted(val error: CoreError) : ExecutionStatus
}
```

`run()`、`pause()`、速度倍率、フレーム更新はCore APIに含めない。これらはRunnerの責務とする。

### 5.3 実時間からサイクル予算への変換

`CycleBudgetPlanner`は、ホストが単調時計から計測した経過nanosecondsを`runCycles`へ渡すcycle数へ
変換する。Planner自身は時計を読まず、sleep、thread、Session操作を行わない。

PC-1245では576kHz発振を2 clocks/cycleとして、通常速度を288,000 cycles/secとする。1 cycle未満の
端数は次回へ繰り越す。UI停止後などの長い経過時間は既定100msで打ち切り、無制限な追いつき実行を
防ぐ。打ち切った時間は`droppedElapsedNanoseconds`としてホストへ返す。

pause、reset、ROM交換、ホスト時刻基準の再設定時には`reset()`を呼ぶ。速度は浮動小数ではなく
`SpeedRatio`の有理数で指定し、倍率変更時には旧倍率の端数を破棄する。

## 6. 入力

CoreはAndroid View IDや各OSのキーコードではなく、機種非依存の論理キーを受け取る。

```kotlin
enum class PocketKey {
    A, B, C, D, E, F, G, H, I, J, K, L, M,
    N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    NUM_0, NUM_1, NUM_2, NUM_3, NUM_4,
    NUM_5, NUM_6, NUM_7, NUM_8, NUM_9,
    ENTER, SPACE, SHIFT, DEF, BREAK, MODE,
    PLUS, MINUS, MULTIPLY, DIVIDE, DOT, EQUALS,
    LEFT, RIGHT, UP, DOWN, CLEAR,
}

sealed interface InputResult {
    data object Accepted : InputResult
    data class UnsupportedKey(val key: PocketKey) : InputResult
}
```

同じキーへの重複したpress/releaseは冪等に扱う。複数キーの同時押し状態はSessionが保持する。
物理キーボードのauto-repeatはプラットフォーム層で処理する。

## 7. Snapshotとメモリ参照

通常の画面更新・デバッグ表示には、個別のgetterを多数呼ぶのではなく一貫したSnapshotを使う。

```kotlin
data class EmulatorSnapshot(
    val revision: Long,
    val totalCycles: Long,
    val executionStatus: ExecutionStatus,
    val cpu: CpuSnapshot,
    val display: DisplaySnapshot,
    val power: PowerSnapshot,
)

data class CpuSnapshot(
    val programCounter: Int,
    val dataPointer: Int,
    val p: Int,
    val q: Int,
    val r: Int,
    val carry: Boolean,
    val zero: Boolean,
    val internalRam: ByteArray,
)

data class DisplaySnapshot(
    val columns: Int,
    val rows: Int,
    val dots: ByteArray,
    val symbols: List<DisplaySymbol>,
    val enabled: Boolean,
    val revision: Long,
)

data class AudioSnapshot(
    val frequencyHz: Int, // 0は無音
    val revision: Long,
)
```

Snapshot取得は状態を進めず、副作用を持たない。`revision`は表示などの再描画要否判定に使えるが、
永続IDや時刻としては扱わない。任意メモリ参照はDebugger用途として範囲検証済みの結果を返す。
`AudioSnapshot`はCoreが要求する論理信号であり、音声再生やbuffer生成はプラットフォーム側が担当する。

## 8. コマンドフック

Pokecom GOの`cmdHook()`にある特定ROMアドレスの検出と、CLOAD、CSAVE、BEEPの直接実行は、
初期Coreへ移植しない。CPUの通常実行を迂回するため、エミュレーションCoreの成立確認には
不要であり、機種・ROMバージョンへの依存も増やすためである。

プログラムの読み書きは、まず停止中のSessionに対する明示的なimport/export APIとして設計する。
音声は、CPUとI/Oポートの挙動を実装した後に信号出力として設計する。ROMアドレスを使った
ショートカットが必要になった場合だけ、用途と互換性を整理して再検討する。

## 9. セーブ状態

`EmulatorSnapshot`は表示・デバッグ用、`SaveState`は完全復元用であり、同じ型にしない。

```kotlin
data class SaveState(
    val formatVersion: Int,
    val machineId: MachineId,
    val coreCompatibility: String,
    val payload: ByteArray,
)
```

`restoreState`では最低限、フォーマットバージョン、機種ID、payload整合性を検証する。
失敗時はSessionを変更しない。エンコード形式はCPU状態モデル確定後に別途定義する。

## 10. エラー方針

- 呼出し側の明白な契約違反（負のアドレス、ゼロ以下のcycle budgetなど）は
  `require`による例外を許容する。
- ROM不足、ROMサイズ不一致、非対応機種、セーブ状態不一致など、通常起こり得る失敗は
  sealed resultとして返す。
- 命令実行中の未実装opcodeや不正状態は`ExecutionStatus.Faulted`とし、診断情報を保持する。
- Coreからログ出力、Toast、Alertを直接行わない。
- ファイルパス、URI、OS例外を`CoreError`へ格納しない。

## 11. スレッド安全性と所有権

`EmulatorSession`はスレッドセーフを保証しない。1つのSessionは常に1つのRunnerまたは
呼出しコンテキストから操作する。UIはSnapshotを受け取り、Coreの実行スレッドを直接共有しない。

Sessionへ渡した`ByteArray`とSessionから返した`ByteArray`は境界でコピーする。
性能上コピーが問題になった場合でも、計測なしに可変配列の共有へ変更しない。

## 12. 初期実装の範囲

最初のPC-1245縦切りでは、次の順でAPIを実体化する。

1. `MachineId`、ROM入力、Session生成結果
2. `reset`、`step`、`runCycles`
3. CPU・メモリSnapshot
4. `PocketKey`とキーマトリクス
5. Display Snapshot
6. Save State

Debugger、BASIC編集、WAV変換の公開APIは、それぞれの機能を実装する段階で追加する。
推測で先に汎用化しない。

## 13. 未決事項

- Swift公開用Facadeを共通APIと同一にするか、薄いApple向けAdapterを置くか
- Save Stateのバイナリ形式と互換性ポリシー
- ROMのハッシュ検証を必須にするか
- Display Snapshotのdot配列をbit-packedにするか1byte-per-dotにするか

これらはPC-1245の状態モデルとGolden Test Dataを用いて判断する。
