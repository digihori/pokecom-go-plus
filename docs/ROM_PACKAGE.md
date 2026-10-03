# PGP ROM Package Format

**形式名:** PGP ROM Package  
**拡張子:** `.pgprom`  
**初期バージョン:** 1  
**ステータス:** 実装前仕様

## 1. 目的

PGPでは、ROMの物理的・論理的な構成と、CPUから見えるアドレス空間を分離する。

Pokecom GOは、非バンク機ではROMと未使用領域を含む64KiBイメージ、バンク機では
内部ROMと連結した外部ROMバンクの2ファイルを使用している。PGPはこれらを引き続き
Importerで読み込めるようにし、内部ではROMを意味単位のコンポーネントへ正規化する。

```text
Legacy ROM files
    ↓ Importer
RomSet（正規モデル）
    ↓ Package writer
.pgprom
```

## 2. 設計原則

1. ROMバイト列をメモリ空間全体のダンプとして保持しない。
2. 内部ROM、外部ROM、バンクROM、漢字ROMをコンポーネントとして識別する。
3. ロードアドレス、バンク窓、選択方式は`MachineDefinition`を正本とする。
4. パッケージ内のファイル名ではなく、manifestのcomponent IDで識別する。
5. 各コンポーネントのサイズとSHA-256を検証する。
6. ROMはリポジトリ、CI、PGP配布物へ含めない。
7. Pokecom GO形式との互換性はImporter / Exporterで維持する。

## 3. コンテナ

`.pgprom`はZIPコンテナとする。

- `manifest.json`をルートへ1つ置く。
- JSONはUTF-8でエンコードする。
- ROMデータは`rom/`ディレクトリへ置く。
- ZIPのstoreとdeflateの両方を許可する。
- 暗号化ZIP、分割ZIP、パスワード付きZIPは許可しない。
- manifestに記載のないファイルはv1では無視せず、警告対象とする。

PC-1245の例：

```text
pc-1245.pgprom
├─ manifest.json
└─ rom/
   ├─ internal.bin
   └─ external.bin
```

PC-1470Uの例：

```text
pc-1470u.pgprom
├─ manifest.json
└─ rom/
   ├─ internal.bin
   └─ external-banks.bin
```

## 4. Manifest v1

```json
{
  "format": "pgp-rom-package",
  "formatVersion": 1,
  "machineId": "pc-1245",
  "title": "PC-1245 ROM",
  "components": [
    {
      "id": "internal",
      "role": "internal",
      "file": "rom/internal.bin",
      "size": 8192,
      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    },
    {
      "id": "external",
      "role": "external",
      "file": "rom/external.bin",
      "size": 16384,
      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
  ]
}
```

### 4.1 必須フィールド

| フィールド | 内容 |
|---|---|
| `format` | `pgp-rom-package`固定 |
| `formatVersion` | v1では`1` |
| `machineId` | PGP Machine ID |
| `components` | 1つ以上のROMコンポーネント |
| `components[].id` | 機種定義と照合する一意なID |
| `components[].role` | `internal`、`external`、`kanji`等の役割 |
| `components[].file` | ZIP内の相対パス |
| `components[].size` | byte単位の正確なサイズ |
| `components[].sha256` | ROMデータのSHA-256、小文字16進64桁 |

`title`は任意の表示用文字列であり、機種判定には使用しない。

### 4.2 Manifestへ格納しない情報

次は`MachineDefinition`が所有するため、v1 manifestへ格納しない。

- CPUから見えるロードアドレス
- 外部ROMのバンク窓
- バンク選択レジスタと選択方式
- LCDやRAMのメモリマップ
- CPUクロック

これにより、パッケージの申告値によってMachineのメモリ構成が変更されることを防ぎ、
同じ情報がmanifestと実装で食い違う問題を避ける。

## 5. 正規RomSetモデル

ZIPやJSONはCoreへ渡さない。パッケージまたは旧形式を検証・展開した結果を`RomSet`へ変換する。

```kotlin
data class RomSet(
    val machineId: MachineId,
    val components: List<RomComponent>,
)

data class RomComponent(
    val id: RomComponentId,
    val role: RomRole,
    val bytes: ByteArray,
)

enum class RomRole {
    INTERNAL,
    EXTERNAL,
    KANJI,
}
```

`RomSet`生成時にByteArrayをコピーする。SHA-256はパッケージ読込み時に検証し、検証済みの
バイト列だけを`RomSet`へ渡す。Coreの`MachineFactory`は`MachineDefinition`が要求する
component IDとサイズを検証してからMachineを生成する。

未知のroleを持つ将来形式を読み込むため、実装時にはenumではなく値オブジェクトまたは
`Unknown`を持つ型も検討する。

## 6. バンクROM

バンクROMは、1バンクごとの多数のファイルへ分割せず、バンク番号順に連結した1コンポーネントとする。

PC-1470Uの外部ROMが1バンク16KiB、8バンクの場合：

```text
external-banks.bin (128KiB)
├─ bank 0: offset 0x00000..0x03fff
├─ bank 1: offset 0x04000..0x07fff
├─ bank 2: offset 0x08000..0x0bfff
├─ ...
└─ bank 7: offset 0x1c000..0x1ffff
```

bank size、bank count、CPU側のwindowは`MachineDefinition`で定義する。ファイルサイズが
`bankSize * bankCount`と一致しなければMachine生成を拒否する。

## 7. Pokecom GO互換Importer

### 7.1 Flat 64KiB Importer

PC-1245の`pc1245mem.bin`などを読み込み、必要な領域だけを抽出する。

```text
0x0000..0x1fff → internal
0x4000..0x7fff → external
その他         → 破棄
```

抽出範囲はImporterへハードコードせず、対象機種のLegacy Import Definitionとして定義する。
外部ROMが別アドレスにある機種も同じImporterの機種別定義で扱う。

### 7.2 Split Bank Importer

Pokecom GOのバンク機用2ファイルを読み込む。

```text
pc1470mem.bin  → internal
pc1470bank.bin → external（全バンク連結）
```

ファイル名だけで機種を自動判定せず、ユーザーが選択した機種と期待サイズを検証する。

### 7.3 Exporter

必要になった段階で、正規`RomSet`から次を生成できるようにする。

- Pokecom GO互換64KiBイメージ
- Pokecom GO互換の内部ROM＋連結バンクROM
- `.pgprom`

旧形式へのexportで生じる未使用領域は`0x00`で埋める。

## 8. PC-1245変換

現在の`local-data/roms/pc-1245/pc1245mem.bin`は次のように変換する。

| Component ID | Role | 元データ | サイズ |
|---|---|---|---:|
| `internal` | `internal` | `0x0000..0x1fff` | 8KiB |
| `external` | `external` | `0x4000..0x7fff` | 16KiB |

変換前の実ROM、生成した`.pgprom`、展開後のROMデータはいずれも`local-data/`配下へ置き、
Git管理対象にしない。

## 9. 検証と安全性

Importerは最低限次を検証する。

- ZIP展開前後のサイズ上限
- `..`、絶対パス、シンボリックリンク等を使ったpath traversalの拒否
- `manifest.json`の重複と欠落
- component IDとfile pathの重複
- Machine IDの一致
- componentの必須性と期待サイズ
- manifest記載サイズと実サイズの一致
- SHA-256の一致
- バンクROMのサイズとバンク数の整合

検証失敗時は部分的な`RomSet`を生成しない。

## 10. 実装順

1. 共通の`RomSet`、`RomComponent`、ROM要件モデル
2. PC-1245用Flat 64KiB Importer
3. PC-1245 Machineを2コンポーネントの`RomSet`から生成
4. `.pgprom` manifestモデルとvalidator
5. Desktop用ZIP reader / writer
6. Pokecom GO Split Bank Importer
7. 必要になった段階でPokecom GO互換Exporter

ZIPの読書きやファイル選択はCoreのエミュレーション層に置かない。KMP共通コードには
manifest、validator、正規モデルを置き、実ファイルI/Oはプラットフォーム層または専用の
infrastructure層で扱う。
