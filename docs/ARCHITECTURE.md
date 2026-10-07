# Pokecom GO Plus Architecture

**文書名:** ARCHITECTURE.md  
**ステータス:** 初期設計方針  
**対象プロジェクト:** Pokecom GO Plus（PGP、プロジェクト全体の開発コードネーム）

## 1. この文書の目的

この文書は、マルチプラットフォームの統合ポケコン開発環境
「Pokecom GO Plus（PGP）」の初期アーキテクチャ方針を定義する。

PGPは既存Androidアプリ「Pokecom GO」の新バージョンや直接の後継版ではない。
Pokecom GOは現行のAndroidアプリとして維持・保守し、PGPは別のGitリポジトリで
新規開発する。

PGPでは、Pokecom GOで蓄積したエミュレーション技術、機種情報、BASIC変換処理などを
出発点とする。ただし既存クラスをそのままコピーするのではなく、ポケコン固有の仕様と
決定論的なアルゴリズムを抽出し、マルチプラットフォーム向けに再設計する。

本書は次を明確にする。

- Pokecom GOから継承する技術資産
- PGPで再設計する範囲
- Emulator Coreとプラットフォーム層の境界
- Kotlin Multiplatform（KMP）を前提とした構成
- 初期の開発単位とマイルストーン
- 将来のデバッガ、アセンブラ、WAV変換等を追加できる拡張方針

PGPから提供するアプリケーションの名称と役割は次の通りとする。

- **Pokecom GO Studio**: macOS／Windows／Linux向けの統合ポケコン開発環境
- **Pokecom GO Player**: Android／iOS向けの実行専用エミュレータ
- **PGP Emulator Core**: 両製品が共有するプラットフォーム非依存Core

## 2. 基本方針

PGPの設計では次の原則を優先する。

1. Pokecom GOの実績あるエミュレーションロジックと機種情報を活用する。
2. Pokecom GO自体には大規模なリファクタリングを行わない。
3. PGP CoreはUI、OS、ファイルシステム、音声デバイスから独立させる。
4. Coreの動作は実時間ではなく、命令およびサイクル数によって決定する。
5. UIへ可変内部状態を直接公開せず、コマンド、イベント、Snapshotを介して接続する。
6. Pokecom GO Studioを最初の製品ターゲットとし、Pokecom GO PlayerはCore成熟後に追加する。
7. 最初から全機種・全機能を実装せず、1機種の小さな縦切りで設計を検証する。
8. Pokecom GOとPGPの互換性は、コード共有ではなくテストデータと期待結果でも検証する。

PGPの中心は単なるエミュレータではなく、次の開発フローを支援するCoreである。

```text
BASIC / Assembly / Binary
            │
            ▼
       Program Tools
            │
            ▼
       Emulator Core
        ├─ Debugger
        ├─ Display State
        └─ Program Export
            │
            ▼
     WAV等による実機転送
```

## 3. 対象プラットフォーム

### 3.1 初期ターゲット

- macOS（Apple Silicon）
- Windows
- Linux

Pokecom GO Studioでは、エミュレータだけでなくデバッガ、BASICプログラム操作、
アセンブラ、ディスアセンブラなどの開発支援機能を段階的に提供する。

### 3.2 将来ターゲット

- Android
- iOS

Android／iOSではPokecom GO Playerを提供する。Playerはポケコンの実行・操作に特化し、
Studioが持つデバッガ、アセンブラ、ディスアセンブラ、メモリ編集などの開発機能を搭載しない。
機能範囲は既存Pokecom GOの利用体験を基準とし、LCD、実機仕様キーボード、機種／ROM選択、
プログラム入出力、音声、セーブステート、ゲームパッド割当を対象とする。

## 4. システム全体構成

PGPは、プラットフォーム非依存のCoreと、各OS向けアプリケーション層で構成する。

```text
                    PGP Emulator Core
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
 Pokecom GO Studio      Pokecom GO Player（共通Player UI）
         │                  │                  │
  Compose Desktop       Android            iOS
         │                  │                  │
 macOS / Windows / Linux   Android             iOS
```

依存方向は常にプラットフォーム層からCoreへ向ける。

```text
Platform Application ──────> PGP Core
PGP Core              -X---> Platform Application
```

CoreからUI、ファイルダイアログ、OSキーコード、クラウドサービスなどを参照してはならない。

## 5. Pokecom GOからの資産継承方針

### 5.1 コードまたはアルゴリズムを流用するもの

以下はPGPへ移植する価値が高い。

- SC61860の命令実装
- レジスタ、フラグ、BCD演算の動作
- 命令ごとのサイクル情報
- 機種別ROM/RAMマップ
- メモリミラーとバンク切替
- I/Oポートとタイマーの挙動
- LCDメモリと表示ドット・シンボルの対応
- 機種別キーマトリクスとスキャン規則
- BASICテキストと中間コードの相互変換
- 機種別BASICコマンドテーブル
- BASIC領域の開始・終了アドレス等の機種情報

流用時は既存Javaクラスの形を維持することを目的としない。命令の計算式、アドレス、
ビット配置、トークン表など、検証済みの仕様と動作をKotlinの新しい状態モデルへ移す。

### 5.2 仕様のみを流用し、実装を再設計するもの

- `Sc61860Base`のCPU処理
- `Sc61860_xxxx`の機種固有処理
- `KeyboardBase`と`KeyBoardXXXX`のスキャン表
- `MainLoopXXXX`のLCDビット配置
- `Sc61860params`の保存対象項目
- `SubActivityBase`等に含まれるBASIC変換処理

これらはポケコン固有の知識を保持している一方、Android API、static状態、UI、
ファイル処理、スレッド制御が混在しているため、そのままPGPへ持ち込まない。

### 5.3 PGPへ持ち込まないもの

- `MainActivity`
- `SubActivityBase`および各`SubActivityXXXX`
- `MainLoopBase`およびAndroid描画処理
- `FileLoad`、`FileSave`、`FileSelectDialog`
- `MyPreferenceActivity`等の設定画面
- `AboutDialogFragment`
- `CsUncaughtExceptionHandler`
- Android `Context`、`Uri`、`DocumentFile`を使う処理
- `SurfaceView`、`Canvas`、`Handler`を使う処理
- `AudioTrack`を直接操作する処理
- Android View IDをエミュレーターのキー識別子として使う処理
- `/sdcard/pokecom`を前提としたストレージ処理
- `SharedPreferences`とGsonを使ったセッション保存

Android用画像はコードとは別に評価する。作者が権利を持ち、PGPの表示要件に適合するものは
PGP用アセットとして再利用できるが、Androidのリソース構成やレイアウトは引き継がない。

## 6. PGP Coreの責務

PGP CoreはUIなしでテストおよび実行できるヘッドレスなライブラリとする。

### 6.1 Coreが担当するもの

- CPUレジスタと内部RAM
- 命令デコードと命令実行
- 命令サイクル数の算出
- ROM/RAMおよびメモリバス
- 機種別メモリマッピング
- バンク切替
- キーマトリクス
- LCDコントローラーと論理表示状態
- タイマー、I/Oポート、周辺回路
- 電源状態と機種固有モード
- ブレークポイント
- CPUトレース
- CPU、メモリ、表示のSnapshot
- セーブ状態の作成と復元
- BASICトークナイズとデトークナイズ
- BASIC/バイナリプログラムのメモリ展開と抽出

### 6.2 Coreが担当しないもの

- ファイル選択ダイアログ
- OSのファイルパスやURI
- クラウドストレージ
- ウィンドウと画面レイアウト
- LCDの実描画
- OSキーコードからポケコンキーへの変換
- オーディオデバイスへの再生
- Clipboard
- アプリ設定の永続化
- UI通知、Toast、Alert
- OSスレッドの生成と管理
- 実時間との同期
- アプリケーションのライフサイクル

## 7. Emulator Coreの内部構成

アプリケーション層へ公開する具体的な契約、実行状態、
Snapshotの所有権は[CORE_API.md](CORE_API.md)で定義する。本節はCore内部の責務分割を扱う。

### 7.1 CPU

`Sc61860Cpu`はCPU固有の状態と命令処理だけを担当する。

```text
Sc61860Cpu
├─ CpuRegisters
├─ InternalRam
├─ Flags
├─ InstructionDecoder
├─ executeInstruction()
└─ reset()
```

CPUはROMファイル、画面、音声、ファイルダイアログを知らない。
メモリやI/Oには`Bus`インターフェースを介してアクセスする。

```kotlin
interface Bus {
    fun read(address: Int): Int
    fun write(address: Int, value: Int)
    fun readPort(port: Int): Int
    fun writePort(port: Int, value: Int)
}
```

命令実行は、実行したサイクル数や停止理由を返す。

```kotlin
data class StepResult(
    val cycles: Int,
    val stopReason: StopReason? = null
)
```

### 7.2 Machine

`Machine`は特定ポケコン機種のハードウェア構成を表す。

```text
Machine
├─ MachineDefinition
├─ MemoryBus
├─ RomSet
├─ Ram
├─ KeyboardMatrix
├─ LcdController
├─ TimerPeripheral
└─ HostCommandDetector
```

機種ごとの差異を、巨大な条件分岐やAndroid Activityの継承関係で表現しない。
設定データで表現できる差異と、コードが必要な周辺回路の差異を分ける。

```kotlin
data class MachineDefinition(
    val id: MachineId,
    val displayName: String,
    val family: MachineFamily,
    val generation: MachineGeneration,
    val memoryBanking: MachineMemoryBanking,
    val romLayout: MachineRomLayout,
    val keyboardLayout: MachineKeyboardLayout,
    val basicDialect: MachineBasicDialect,
    val characterColumns: Int,
    val supportedOperatingModes: Set<OperatingMode>,
    val cyclesPerSecond: Long,
    val automaticKeyHoldCycles: Long,
    val automaticKeyGapCycles: Long,
    val supportsConfigurableRam: Boolean
)
```

実装済み機種は`MachineCatalog`へ表示順に登録する。Factory、Studioの機種選択、ROM Importerの選択、
文字入力、ソフトウェアキーボード、実行クロックは個別の機種IDを比較せずCatalogを参照する。
ただし、複雑なMemory BusやI/O処理そのものをデータだけで表現しようとはしない。

複雑なバンク切替やI/O挙動は`Machine`実装または周辺回路クラスに置く。
SC61860搭載機のOLD/S1/S2分類、中間コード、バンク切替のモデルは
[MACHINE_FAMILIES.md](MACHINE_FAMILIES.md)で定義する。世代とバンク切替の有無は別の軸として扱う。
ROMの正規モデル、`.pgrom`、Pokecom GO互換Importerは
[ROM_PACKAGE.md](ROM_PACKAGE.md)で定義する。

### 7.3 EmulatorSession

`EmulatorSession`は1台の仮想ポケコンを所有する公開Facadeとする。

```kotlin
class EmulatorSession(
    machine: Machine,
    romSet: RomSet
) {
    fun reset()
    fun step(): StepResult
    fun runCycles(maxCycles: Long): RunResult

    fun pressKey(key: PocketKey)
    fun releaseKey(key: PocketKey)

    fun cpuSnapshot(): CpuSnapshot
    fun displaySnapshot(): DisplaySnapshot
    fun memorySnapshot(range: AddressRange): ByteArray

    fun saveState(): EmulatorState
    fun restoreState(state: EmulatorState)
}
```

RAM、LCD、キー、CPU制御状態はすべてセッションまたはその配下のインスタンスが所有する。
可変状態を`object`や`companion object`へ置かない。

### 7.4 Emulator Runner

RunnerはCoreの外側に置き、実時間とエミュレーターサイクルを調整する。

```text
EmulatorRunner
├─ Start / Stop / Pause
├─ 速度調整
├─ フレーム更新間隔
├─ 高速実行
└─ UIへのSnapshot通知
```

CPUは現在時刻、`Thread.sleep()`、UIフレームレートを参照しない。
これによりテスト、Single Step、高速実行、再現可能なデバッグを可能にする。

## 8. 入力モデル

OSのキーコードやView IDをCoreへ渡さず、論理キーを使用する。

```kotlin
enum class PocketKey {
    A, B, C, D, E, F,
    NUM_0, NUM_1, NUM_2,
    ENTER, SHIFT, DEF, BREAK, MODE
}
```

機種定義は論理キーをキーマトリクス位置へ変換する。

```kotlin
data class KeyMatrixPosition(
    val column: Int,
    val row: Int
)
```

プラットフォーム層が、物理キーボード、画面タップ、ゲームパッドを`PocketKey`へ変換する。

```text
OS Key / Touch / Gamepad
            │
            ▼
      Platform KeyMap
            │
            ▼
        PocketKey
            │
            ▼
      KeyboardMatrix
```

## 9. 表示モデル

Coreは描画命令ではなく、ポケコンLCDの論理状態を提供する。

```kotlin
data class DisplaySnapshot(
    val columns: Int,
    val rows: Int,
    val dots: ByteArray,
    val symbols: Set<DisplaySymbol>,
    val enabled: Boolean,
    val revision: Long
)
```

LCDメモリへの書込みでは内部状態とdirty/revisionだけを更新する。
メモリ書込みごとにUI描画を直接要求しない。

各プラットフォームはSnapshotを取得し、それぞれの描画方式で表示する。

```text
LCD Memory
    ↓
LcdController
    ↓
DisplaySnapshot
    ├─ Compose Desktop
    ├─ Android Canvas / Compose
    └─ iOS Compose / SwiftUI
```

## 10. 外部I/O

Pokecom GOの`cmdHook()`にあるCLOAD、CSAVE、BEEPのROMアドレスフックは、初期Coreへ
移植しない。プログラム転送は停止中のSessionに対する明示的な操作として設計し、音声は
CPUとI/Oポートの実装後に実際の信号を基準として設計する。

ファイルサービスの境界は、パスやURIではなくデータを中心にする。

```kotlin
fun loadRom(romSet: RomSet)
fun loadBasicSource(source: String)
fun loadBinary(program: ByteArray, startAddress: Int)
fun exportBasicSource(): String
fun exportBinary(range: AddressRange): ByteArray
```

## 11. BASIC Program Handling

BASIC処理をAndroid ActivityやエミュレーターのRAM操作から分離する。
OLD、S1、S2では中間コードが異なるため、機種定義から`BasicDialectId`を明示的に選択する。
世代名だけでトークン表を暗黙選択しない。

```text
BasicDialect
├─ CommandTable
├─ CharacterSet
├─ NumberEncoding
└─ ProgramLayout

BasicTokenizer
└─ String -> BasicProgramImage

BasicDetokenizer
└─ BasicProgramImage -> String

BasicProgramLoader
├─ ProgramImage -> Machine Memory
└─ Machine Memory -> ProgramImage
```

次を仕様として明示する。

- BASIC方言とコマンドトークン
- 文字列およびREM中の扱い
- 行番号表現
- 改行コード
- ASCII、半角カナ、機種固有文字
- 不正な文字や不正な中間コードへの対応
- プログラム開始・終了アドレス

可能な限り通常のテキストファイルをユーザー向け正本とし、PGP独自の編集形式を必須にしない。

### 11.1 Memory Profileと実機制約

機種モデルと搭載メモリ構成を同一概念にしない。既定の互換性検証では実機構成を使用する一方、
通常利用では同一ファミリー最大RAMや、明示的な追加RAMを選択できる`MemoryProfile`を設ける。

```text
Machine Configuration
├─ Machine Model       PC-1250
└─ Memory Profile      PC-1250 original / PC-1251 / PC-1255 maximum
```

- Golden Testはoriginal profileを使用する
- 通常利用の既定は、同一ファミリー最大profileに設定できる
- profileはプロジェクトとSave Stateへ保存し、画面にも表示する
- 実行中には切り替えず、Session再作成またはReset境界で適用する
- RAM縮小時は範囲外データが失われることを事前に通知する
- BASIC格納容量、メモリ読書き可否、バンク構成は選択profileから取得する
- PC-1360へ漢字work RAMを追加する場合も、PC-1360Kと偽らず
  `PC-1360 + Kanji Work RAM`という拡張profileとして表現できるようにする

単一の「制約を無視する」flagや任意アドレス指定ではなく、検証可能な名前付きprofileを使用する。

## 12. Debugger Core

デバッグ機能はUIの付加機能ではなく、Coreの正式な機能として設計する。
現在のStudio機能、停止条件、Trace、Checkpoint、既知の制約は[DEBUGGER.md](DEBUGGER.md)にまとめる。

初期Debugger Coreは次を担当する。

- Run / Pause / Stop
- Single Step
- PCブレークポイント
- CPU Snapshot
- メモリ読出し
- 現在命令の逆アセンブル
- 実行トレース

将来的に次を追加できる境界を用意する。

- メモリ読書きWatchpoint
- 条件付きBreakpoint
- 機種固有状態の監視
- シンボルとソース行の対応
- トレースのエクスポート

UIへCPU内部の可変オブジェクトを公開せず、SnapshotとDebugger Commandを介して操作する。

同じ境界を将来のAI連携にも使用する。MCP層はCPUやMemory Busを直接操作せず、Studio UIと共通の
Application Serviceからversion付きSnapshotを取得し、検証済みCommandを発行する。AI接続の構成、
ツール候補、承認境界は[AI_INTEGRATION.md](AI_INTEGRATION.md)を参照する。

## 13. セーブ状態

Java `Serializable`やJVM固有のオブジェクト形式を使用しない。

```kotlin
data class EmulatorState(
    val formatVersion: Int,
    val machineId: MachineId,
    val cpu: CpuState,
    val memory: ByteArray,
    val peripherals: PeripheralState
)
```

保存形式には必ず次を含める。

- フォーマットバージョン
- 機種ID
- Coreバージョンまたは互換性情報
- CPU状態
- RAM
- バンク状態
- LCDおよび周辺回路状態

Kotlinクラスの内部レイアウトをそのまま永続形式とせず、将来の移行処理を定義できる形式にする。
シリアライズにはKMP対応ライブラリを利用できるが、ROMやRAMの大きな配列をJSONへ展開することは
避け、必要に応じて明示的なバイナリ形式を使用する。

## 14. Kotlin Multiplatform方針

### 14.1 Source Set

Coreの共有ロジックは`commonMain`へ置く。

```text
core/src/
├─ commonMain/
├─ commonTest/
├─ jvmMain/
├─ androidMain/       将来
└─ iosMain/           将来
```

`commonMain`ではJVM、Android、Apple固有APIを使用しない。

### 14.2 プラットフォーム機能の扱い

基本方針は、OS機能をCoreへ抽象注入するより、Coreの外へ出すことである。

単純でCore内に必要な差異だけ、共通インターフェースまたは`expect/actual`を検討する。
ファイルダイアログ、音声デバイス、Clipboardなどの大きな機能に`expect/actual`を乱用しない。

### 14.3 JavaからKotlinへの移植

既存Javaコードを自動変換して完了とはしない。

- 移植前にPokecom GOの動作をテストデータ化する。
- 命令または機能単位でKotlinへ移植する。
- 移植前後のCPU状態、RAM、LCDを比較する。
- static状態を`companion object`へ機械的に移さない。
- Javaの内部クラスによる命令表を直訳するかは性能計測後に決める。

### 14.4 メモリの値型

既存実装は主に`IntArray`で0〜255を表現している。Kotlinの`Byte`は符号付きであるため、
初期移植では正しさを優先し、必要なら`IntArray`を維持する。

`ByteArray`へ変更する場合は、読出し時に必ず符号なし変換を行う。

```kotlin
val value = memory[address].toInt() and 0xff
```

`UByteArray`の採用は、APIの扱いや性能を計測してから判断する。

### 14.5 スレッドと状態所有

1つの`EmulatorSession`は1つのRunnerから操作する。
Core自身はスレッドを生成しない。

UIへは可変配列ではなくSnapshotを渡す。これによりJVMとKotlin/Nativeの違いをCore設計へ
持ち込まず、競合の少ない構造にする。

### 14.6 iOS向け公開API

iOSへCPU内部クラスや巨大な可変配列を直接公開しない。Swiftから扱いやすい小さなFacadeを提供する。

```text
PgpEmulator
├─ reset
├─ run / pause
├─ press / release
├─ loadProgram
├─ displaySnapshot
└─ saveProgram
```

## 15. 初期プロジェクト構成

初期段階では過度にGradleモジュールを分割しない。

```text
pokecom-go-plus/
├─ README.md
├─ LICENSE
├─ AGENTS.md
├─ settings.gradle.kts
├─ build.gradle.kts
│
├─ core/
│  └─ src/
│     ├─ commonMain/kotlin/com/digihori/pgp/
│     │  ├─ emulator/
│     │  │  ├─ cpu/
│     │  │  ├─ memory/
│     │  │  ├─ machine/
│     │  │  ├─ input/
│     │  │  ├─ display/
│     │  │  ├─ peripheral/
│     │  │  └─ session/
│     │  ├─ basic/
│     │  └─ debugger/
│     └─ commonTest/kotlin/
│
├─ desktopApp/
│  └─ Compose Desktop application
│
├─ test-data/
│  ├─ cpu/
│  ├─ machine/
│  ├─ basic/
│  └─ display/
│
└─ docs/
   ├─ PGP_CONCEPT.md
   ├─ ARCHITECTURE.md
   ├─ PORTING_NOTES.md
   └─ ROADMAP.md
```

Assembler、WAV、Character Editorは、実装を開始するまで独立モジュールを作らない。
依存境界やビルド時間上の利点が明確になった段階で、次の分割を検討する。

```text
core-emulator
core-basic
core-debugger
tooling-assembler
tooling-wav
tooling-character
studio-desktop
player-android
player-ios
```

## 16. Pokecom GO Studio

最初のUIはCompose Desktopを第一候補とする。

Studio層が担当するものは次の通り。

- ROMおよびプログラムファイルの選択
- エミュレーターのRun / Pause / Reset
- LCD描画
- 画面キーおよび物理キーボード入力
- CPU、レジスタ、メモリ、逆アセンブルの表示
- ウィンドウレイアウト
- 設定保存
- 外部ファイル変更の監視
- Desktop向けパッケージング

Core APIを検証するため、最初のUIは完成したIDEを目指さず、機能確認用の小さな画面とする。

### 16.1 Studio ROM Library

Studioへ取り込んだROMは元ファイルのパスを実行時の正本にせず、OSのアプリ専用データ領域へ
機種別の`.pgrom`としてコピーする。`.pgrom`のmanifest、component size、hashを検証し、
有効なEmulator Sessionを生成できることを確認してから登録する。

各機種またはROMを共有する機種ファミリーは一つの有効なROM setを持ち、再取込みはそのコピーを
置換する。PC-1250／1251／1255は一つのPC-1251系ROMを共有し、機種選択時に同じcomponentから
各モデルのRAM構成を持つSessionを生成する。機種選択UIでは登録済みROMを利用できる機種だけを
実行対象として選択でき、選択後は保存済みROMからSessionを再生成して自動RUNする。
削除はROM管理画面から明示的に行い、プロジェクト、ログ、リポジトリにはROM本体を含めない。
保存処理は一時ファイルからのatomic moveを優先し、不完全な書込みを有効なROMとして扱わない。

### 16.2 Pokecom GO Player

PlayerはAndroid／iOS向けの軽量な実行環境とする。共通Coreを利用するが、Studioの開発支援UIには
依存しない。Player層は次を担当する。

- `.pgrom`とプログラムファイルの選択および前回ROMの復元
- LCD表示と機種別ソフトウェアキーボード
- RUN／PRO／RSV等の実機操作
- PCM音声のプラットフォーム別再生
- Pause／Reset、ライフサイクル、セーブステート
- ゲームパッドのポケコンキー割当
- Android Storage Access Framework／iOS Document Pickerとの接続

CPUレジスタ表示、デバッガ、逆アセンブラ、アセンブラ、メモリエディタはPlayerの対象外とする。
LCDとキー配置など再利用価値の高いUIは将来の共通Player UIとして切り出せるが、ファイル選択、
音声、ライフサイクルは各プラットフォーム層に残す。

## 17. テスト戦略

PGPの移植では、Pokecom GOを参照実装として利用する。

### 17.1 Golden Test Data

交換形式、再現Action、Snapshot表現、実ROM由来データの配置と審査手順は
[GOLDEN_TEST_DATA.md](GOLDEN_TEST_DATA.md)で定義する。包括的な採取結果は`local-data/golden/`、
再配布可能と確認した最小ケースだけを`test-data/golden/`へ置く。

Pokecom GOから次を採取し、PGPの期待値として保存する。

- CPUリセット状態
- 代表命令実行前後のレジスタとフラグ
- 指定命令数またはサイクル数実行後の状態
- RAMの範囲またはハッシュ
- キー入力に対するキースキャン結果
- LCDメモリに対するドット配列とシンボル
- BASICテキストと中間コードの変換結果

```text
Test Input
├─ Machine ID
├─ Initial State / ROM
├─ Key Events
└─ Step or Cycle Count

Expected Output
├─ CPU Snapshot
├─ RAM Hash / Range
├─ Display Snapshot
└─ Stop Reason
```

### 17.2 テスト階層

```text
Unit Test
├─ 算術・BCD・フラグ
├─ 各命令
├─ Memory Mapping
├─ Keyboard Matrix
├─ LCD Mapping
└─ BASIC Codec

Integration Test
├─ ROM Boot
├─ BASIC Program Load / Run
├─ Save / Restore
└─ Debugger Breakpoint

UI Test
└─ Desktopの主要操作のみ
```

ROMイメージはライセンス上再配布可能であることが確認できない限り、リポジトリやCIへ含めない。
ROMが必要なテストと、再配布可能な合成命令列だけで実行できるテストを分離する。

## 18. 初期マイルストーン

### Milestone 0: 仕様と期待値の固定

- Pokecom GOからPC-1245の動作仕様を抽出する。
- CPU、メモリ、LCD、キー、BASICのGolden Test Dataを準備する。
- 既存コードと画像資産の由来およびライセンスを確認する。

### Milestone 1: Headless PC-1245

- PGPの新規KMPリポジトリを作成する。
- SC61860 CPUの最小実装を移植する。
- PC-1245のMachineを実装する。
- ROMを`ByteArray`として読み込む。
- `reset`、`step`、`runCycles`を実装する。
- CPU、RAM、LCD Snapshotを取得する。
- `commonTest`でPokecom GOの期待値と比較する。

完成条件はUI表示ではなく、ヘッドレス実行がテストで一致することである。

### Milestone 2: macOS最小Pokecom GO Studio

- ROM選択
- Run / Pause / Reset
- LCD表示
- 最低限のキー入力
- CPUレジスタ表示

### Milestone 3: BASICクロス開発

- `.BAS`ファイルの読込み
- BASICトークナイズ
- PC-1245メモリへの展開
- エミュレーター上での実行
- メモリからBASICテキストへの書戻し
- 通常のテキストファイルとして保存

### Milestone 4以降

- Windows/Linux対応
- Debugger強化
- Disassembler
- Assembler
- 5×7 Character Editor
- WAV Encoder / Decoder
- Android
- iOS

Android／iOSではPokecom GO Playerを提供し、Studio機能の移植は行わない。

## 19. 将来機能の拡張境界

### 19.1 Assembler / Disassembler

Assemblerはエミュレーターの内部状態へ直接依存させない。

```text
Assembly Source
    ↓
Assembler
    ↓
AssemblyResult
├─ Machine Code
├─ Symbols
├─ Diagnostics
└─ Source Map
```

Debuggerは`Symbols`と`Source Map`を任意に受け取れるようにする。

### 19.2 WAV Codec

WAV Codecは音声デバイスではなくデータ変換としてCoreまたはTooling層へ置く。

```text
ProgramImage <──> TransferProtocol <──> PCM Samples / WAV Data
```

機種依存の転送プロトコルと、WAVコンテナの入出力を分離する。

### 19.3 Character Editor

5×7エディタのデータモデルと変換処理はUIから分離する。

### 19.4 AI / MCP Integration

AI連携はDesktop固有のAdapterとして配置し、`core/commonMain`をAI SDK、ネットワーク、認証へ依存させない。

```text
AI Client ── localhost MCP ── Studio AI Adapter
                                  ↓
                         Application Services
                    ┌─────────────┼─────────────┐
                 Project       Debugger      Emulator
```

初期MCPは読み取り専用とし、状態変更はツール単位の権限とStudio側承認を必要とする。ROM全体や任意ファイルを
公開せず、対象ProjectとSessionにscopeを限定する。Studio UIとMCP AdapterはBuild、Assembly、Debuggerの
同じサービスを利用し、AI専用の別実装を作らない。

```text
DotPattern
├─ width / height
├─ dots
└─ machine format conversion
```

同じ`DotPattern`から、BASIC DATA文、Assemblerデータ定義、バイナリ、LCDプレビューを生成する。

## 20. ライセンスと由来管理

PGPはMIT Licenseを第一候補とする。ただしPokecom GOからコードや画像を移す前に、
各資産の権利と由来を確認する。

確認対象は次の通り。

- CPUおよび機種別コードの作者
- 他エミュレーターや資料を元にしたコードの有無
- ポケコン外観画像の作成元
- 過去に作成したAssembler/WAVツールの権利
- 使用する外部ライブラリのライセンス

PGPへ移植した資産については`PORTING_NOTES.md`等で、元ファイル、移植範囲、変更内容を記録する。

ROMイメージ、マニュアル画像、雑誌掲載プログラムなど、第三者が権利を持つデータは、
再配布可能であることが明確でない限りリポジトリおよび配布物に含めない。

## 21. 非目標

初期PGPでは次を目標としない。

- Pokecom GOの置換
- Pokecom GOの全機能を最初から再現すること
- 全機種同時対応
- Android UIコードの共通化
- 全プラットフォームで同一UIを強制すること
- 本格的な汎用テキストエディタ
- 特定クラウドサービスへのCore依存
- ROMイメージの配布
- 初期段階での過度なGradleモジュール分割

## 22. アーキテクチャ判断の基準

新しい機能または依存をCoreへ追加する際は、次を確認する。

1. これはポケコン固有の決定論的な処理か。
2. UIなしでテストできるか。
3. OSのパス、URI、スレッド、時計、デバイスを直接参照していないか。
4. JVMとKotlin/Nativeの両方で実装可能か。
5. 可変グローバル状態を増やしていないか。
6. Snapshotまたは明示的なイベント境界で外部と接続できるか。
7. 既存のMachine実装へ不要な影響を与えず拡張できるか。
8. 将来のデバッガやテストから観測・制御できるか。

この基準を満たさない機能は、Platform ApplicationまたはTooling層へ置く。

## 23. まとめ

PGPはPokecom GOのコードベースを直接マルチプラットフォーム化するプロジェクトではない。
Pokecom GOで実証されたCPU命令、機種固有挙動、LCD・キー配置、BASIC変換技術を継承し、
Kotlin Multiplatform上で新しいCoreとして再構成する。

最初の成功条件は、多数の画面や開発ツールを備えることではない。

```text
PC-1245をヘッドレスで生成し、
ROMを読み込み、
決定論的に実行し、
CPU・RAM・LCD状態を取得し、
Pokecom GOの期待結果と一致すること
```

この小さなCoreを基盤として、Pokecom GO StudioのUI、BASICクロス開発、Debugger、
Assembler、WAV転送を段階的に追加する。CoreとStudioが安定した後、Android／iOS向けの
Pokecom GO Playerを追加する。
