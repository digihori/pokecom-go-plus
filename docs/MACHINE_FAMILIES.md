# SC61860 Machine Families

**ステータス:** 初期分類  
**情報源:** Pokecom GO作者による設計情報

## 1. 目的

SC61860搭載機は、PGPでは中間コードの互換性を基準として3世代に分類する。
世代は単なる発売時期ではなく、BASICプログラムの読み書きと機種間互換性を決める
ドメイン情報として扱う。

## 2. 世代分類

| 世代 | 通称 | 機種 |
|---|---|---|
| 第1世代 | OLD系 | PC-1245、PC-1250、PC-1251、PC-1255 |
| 第2世代 | S1系 | PC-1260、PC-1261、PC-1262、PC-1350、PC-1401、PC-1402、PC-1450、PC-1460系 |
| 第3世代 | S2系 | PC-1360、PC-1360K、PC-1470U |

PC-1402はPC-1401とほぼ同じで、主な差はユーザーメモリ容量である。機種定義では両者の
共通部分を共有可能にするが、容量差を個別の`MemoryLayout`として明示する。

PC-1251系はPC-1250、PC-1251、PC-1255の3機種、PC-1261系はPC-1260、PC-1261、
PC-1262の3機種で構成される。各系統内の違いはユーザーメモリ容量だけである。

PC-1360KはPC-1360の日本語対応版で、漢字ROMが追加されている。対象機種一覧には含めるが、
漢字ROMの配置、選択方法、文字コード、表示方法の詳細が不明なため、現時点では実装保留とする。

## 3. PGPでのモデル化

世代を機種IDの文字列やクラス継承から推測しない。`MachineDefinition`が明示的に保持する。

```kotlin
enum class MachineGeneration {
    OLD,
    S1,
    S2,
}

data class MachineDefinition(
    val id: MachineId,
    val family: MachineFamilyId,
    val generation: MachineGeneration,
    val basicDialect: BasicDialectId,
    val memoryBanking: MemoryBanking,
    // display、keyboard、clock等
)
```

ただし、`MachineGeneration`と`BasicDialectId`は同じ概念ではない。

- `MachineGeneration`: 機種系統の分類
- `BasicDialectId`: トークン表、文字コード、プログラム格納形式の識別子

初期段階ではOLD/S1/S2とBASIC方言が1対1に見えても、将来の機種差やROM差分を表現できるよう、
別の型として保持する。

同一ファミリー内の容量違いは継承クラスで表現せず、共通のハードウェア定義と機種ごとの
`MemoryLayout`を合成する。

```text
PC-125x family definition
├─ PC-1250 + user memory layout
├─ PC-1251 + user memory layout
└─ PC-1255 + user memory layout

PC-126x family definition
├─ PC-1260 + user memory layout
├─ PC-1261 + user memory layout
└─ PC-1262 + user memory layout
```

## 4. 中間コード

OLD、S1、S2では、内部で扱うBASICプログラムの中間コードが異なる。したがって、
Tokenizer / Detokenizerは全機種共通の単一トークン表を持たない。

```text
BasicDialect
├─ TokenTable
├─ CharacterSet
├─ ProgramLayout
└─ NumberEncoding

MachineDefinition ──> BasicDialectId ──> BasicDialect
```

移植時は、世代ごとに最低限次をGolden Testで分離する。

- BASICテキストから中間コードへの変換
- 中間コードからBASICテキストへの変換
- 同じキーワードに割り当てられたトークン値
- 行番号と行長の表現
- 文字列、REM、機種固有文字の扱い
- プログラム開始・終了アドレスの管理方法

異なる世代の中間コードを、そのまま別世代の機種へロードできるとは仮定しない。機種間変換は
一度共通のBASICテキストまたは中間表現へ戻してから、対象方言で再エンコードする。

## 5. メモリバンク

メモリ拡張のためのバンク切替を持つことが確認されている機種は、PC-1360、PC-1460、
PC-1470Uである。同じ世代の全機種が持つわけではないため、バンク切替の有無や方式を
世代enumから判定しない。PC-1360Kは追加の漢字ROMを含む構成の詳細が不明なため、
バンク構成も未確定として扱う。

```kotlin
sealed interface MemoryBanking {
    data object None : MemoryBanking

    data class Banked(
        val window: AddressRange,
        val bankCount: Int,
        val selector: BankSelectorDefinition,
    ) : MemoryBanking
}
```

実装では、CPU命令処理に機種名の条件分岐を加えず、Memory Busが機種定義とバンク選択回路を
所有する。バンク数、ウィンドウ、選択方法は機種ごとに確認する。

## 6. 対象機種と状態

| 機種 | 世代 | バンク切替 | PGPでの状態 | 備考 |
|---|---|---|---|---|
| PC-1245 | OLD | なし | 最初に実装 | — |
| PC-1250 | OLD | なし | 将来対象 | PC-1251系。差はユーザーメモリ容量 |
| PC-1251 | OLD | なし | 将来対象 | PC-1251系。差はユーザーメモリ容量 |
| PC-1255 | OLD | なし | 将来対象 | PC-1251系。差はユーザーメモリ容量 |
| PC-1260 | S1 | なし | 将来対象 | PC-1261系。差はユーザーメモリ容量 |
| PC-1261 | S1 | なし | 将来対象 | PC-1261系。差はユーザーメモリ容量 |
| PC-1262 | S1 | なし | 将来対象 | PC-1261系。差はユーザーメモリ容量 |
| PC-1350 | S1 | なし | 将来対象 | — |
| PC-1401 | S1 | なし | 将来対象 | — |
| PC-1402 | S1 | なし | 将来対象 | PC-1401との差は主にユーザーメモリ容量 |
| PC-1450 | S1 | なし | 将来対象 | — |
| PC-1460 | S1 | あり | 将来対象 | 機種別バンク定義が必要 |
| PC-1360 | S2 | あり | 将来対象 | 機種別バンク定義が必要 |
| PC-1360K | S2 | 未確定 | 対象・実装保留 | PC-1360日本語版、漢字ROM追加 |
| PC-1470U | S2 | あり | 将来対象 | 機種別バンク定義が必要 |

「将来対象」は実装順やリリースを保証しない。Machine Catalogへ登録できる設計対象を示す。
「実装保留」は、機種IDと仕様調査枠は確保するが、不明点を推測して実装しないことを示す。

## 7. PC-1245への適用

最初の対象であるPC-1245は次の構成とする。

```text
Machine ID       pc-1245
Generation       OLD
Basic dialect    old.pc-1245
Memory banking   None
```

PC-1251系とのメモリマップの共通性は、将来OLD系の2機種目を追加するときに
`MachineDefinition`のデータ共有が適切か検証する。最初から共通基底クラスは作らない。

PC-1250/1251/1255は機種モデルとMemory Profileを分離する。実機準拠profileに加えて、PC-1250または
PC-1251のROM・外観を保ったままPC-1255相当の最大RAMを利用できるprofileを用意する。通常利用の既定は
最大RAM、実機互換テストとGolden Testは各モデル本来のRAMとする。切替はSession再作成時に適用する。

## 8. 未確定事項

- PC-1250/1251/1255それぞれのユーザーメモリ容量とアドレス範囲
- PC-1260/1261/1262それぞれのユーザーメモリ容量とアドレス範囲
- PC-1261系等のS1系各機種における中間コード差分
- S2系各機種間の中間コード差分
- 各機種のバンクウィンドウ、バンク数、選択レジスタ
- PC-1360Kの漢字ROM構成、バンク構成、文字コード、LCD表示方法
- ROMバージョンによるトークン表の差
