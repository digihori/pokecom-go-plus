# MCP機種知識カタログ

更新日：2026-11-02。PC-1245に加え、PC-1251の実装由来の知識を提供する。
Studioのインストールだけで取得できるよう、このフォルダーの解説と既存HELLO WORLD作例を
ビルド時にアプリのresourceへ同梱する。実行時に開発リポジトリや任意のファイルを読む機能ではない。

| 分野 | 項目ID | 情報源と初版の範囲 |
| --- | --- | --- |
| BASIC・文字 | `pc1245.basic` | [解説](PC1245_BASIC.md)＋Core方言定義のコード表。完全な実行時言語仕様ではない |
| メモリ | `pc1245.memory` | [解説](PC1245_MEMORY.md)＋Core既定配置・管理ポインター。Session固有配置は既存機種仕様API |
| LCD | `pc1245.display` | [解説](PC1245_DISPLAY.md)＋既存機種仕様APIと同じdirectDrawing |
| assembler | `sc61860.assembler` | [内蔵構文](SC61860_ASSEMBLER.md)。外部assemblerとは区別 |
| 起動・観測 | `pc1245.execution` | [RUN／CALLと未提供情報](PC1245_EXECUTION.md) |
| 作例 | `pc1245.example.hello-world` | [適用条件・確認手順](PC1245_HELLO_EXAMPLE.md)。exampleIdは`pc1245-hello-world` |
| 入力・分岐の作例 | `pc1245.example.numeric-labels` | [G01の観測条件と手順](PC1245_NUMERIC_LABELS.md)。exampleIdは`pc1245-numeric-labels` |
| CPU命令 | `sc61860.instruction.<小文字mnemonic>` | Coreの全定義をmnemonicごとにまとめたopcode・長さ・operand・制御フロー。フラグ・完全な動作・cycle数は未提供 |

知識は対象機種、出典、検証根拠の種類、ROMへの適用限界を返す。
`implementationDerived`は実装由来、`documentedExample`は既存作例の記録由来。
出典にテストがあることと、その環境で実ROMテストを実行したことは区別する。
全項目の実機検証は`notVerified`。任意ROMのルーチンABIを保証しない。
現カタログは`pc1245-v2`。numeric-labelsの`emulatorObservedExample`は記録した1つのROMと
エミュレーター環境での観測を意味し、任意ROM・実機の保証ではない。

検索例：

```json
{"machineId":"pc-1245","query":"表示","topic":"display","limit":8}
```

返されたIDを取得する。

```json
{"machineId":"pc-1245","id":"pc1245.display"}
```

作例は`pgp_get_example`で取得する。これはファイル作成やBuildの実行ではない。

```json
{"machineId":"pc-1245","exampleId":"pc1245-hello-world"}
```

既存資料には初期設計や過去の検証記録も含まれる。丸ごと実機仕様として公開せず、
この解説に確認範囲を記載し、数値やコード表は可能な限りCore定義から生成する。
機種追加時は機種別の出典・検証を用意し、PC-1245の配置やABIを流用しない。

## PC-1251追加（現pc1251-v3）

machineIdを`pc-1251`にして検索・取得する。機種固有の項目は
`pc1251.basic`／`pc1251.memory`／`pc1251.display`／`pc1251.execution`。
[適用条件と制限](PC1251_SCOPE.md)を各項目に同梱する。
共通assembler・SC61860命令項目も取得できる。PC-1245用の項目・作例は検索に含めず、
取得を指定した場合もNOT_FOUNDとする。PC-1251作例は`pc1251.example.numeric-labels`（exampleId `pc1251-numeric-labels`）。

PC-1251のProject／Source生成・上限実行・停止中LCD取得にも対応する。
これらの自動検証と、実ROM・実機の検証は区別する。
Coreから取得するmemoryLayoutはcatalogDefaultsで、SessionのRAM設定を読み取らない。
PC-1251の数値入力作例は1つのROMでエミュレーター観測済み。
実機・任意ROM・外部AIでの用途受入れは未実施。
