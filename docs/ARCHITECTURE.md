# Pokecom GO Plus Architecture

> 旧URL向けの案内です。本文は新しい場所へ移動しました。
> This page preserves an older URL. Follow the link below for the document.

[docs/reference/ARCHITECTURE.md](reference/ARCHITECTURE.md)

[ドキュメント目次 / Documentation index](README.md)

## 1. この文書の目的

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#1-この文書の目的)

## 2. 基本方針

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#2-基本方針)

## 3. 対象プラットフォーム

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#3-対象プラットフォーム)

### 3.1 初期ターゲット

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#31-初期ターゲット)

### 3.2 将来ターゲット

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#32-将来ターゲット)

## 4. システム全体構成

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#4-システム全体構成)

## 5. Pokecom GOからの資産継承方針

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#5-pokecom-goからの資産継承方針)

### 5.1 コードまたはアルゴリズムを流用するもの

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#51-コードまたはアルゴリズムを流用するもの)

### 5.2 仕様のみを流用し、実装を再設計するもの

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#52-仕様のみを流用し実装を再設計するもの)

### 5.3 PGPへ持ち込まないもの

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#53-pgpへ持ち込まないもの)

## 6. PGP Coreの責務

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#6-pgp-coreの責務)

### 6.1 Coreが担当するもの

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#61-coreが担当するもの)

### 6.2 Coreが担当しないもの

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#62-coreが担当しないもの)

## 7. Emulator Coreの内部構成

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#7-emulator-coreの内部構成)

### 7.1 CPU

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#71-cpu)

### 7.2 Machine

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#72-machine)

### 7.3 EmulatorSession

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#73-emulatorsession)

### 7.4 Emulator Runner

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#74-emulator-runner)

## 8. 入力モデル

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#8-入力モデル)

## 9. 表示モデル

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#9-表示モデル)

## 10. 外部I/O

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#10-外部io)

## 11. BASIC Program Handling

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#11-basic-program-handling)

### 11.1 Memory Profileと実機制約

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#111-memory-profileと実機制約)

## 12. Debugger Core

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#12-debugger-core)

## 13. セーブ状態

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#13-セーブ状態)

## 14. Kotlin Multiplatform方針

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#14-kotlin-multiplatform方針)

### 14.1 Source Set

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#141-source-set)

### 14.2 プラットフォーム機能の扱い

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#142-プラットフォーム機能の扱い)

### 14.3 JavaからKotlinへの移植

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#143-javaからkotlinへの移植)

### 14.4 メモリの値型

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#144-メモリの値型)

### 14.5 スレッドと状態所有

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#145-スレッドと状態所有)

### 14.6 iOS向け公開API

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#146-ios向け公開api)

## 15. 初期プロジェクト構成

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#15-初期プロジェクト構成)

## 16. Pokecom GO Studio

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#16-pokecom-go-studio)

### 16.1 Studio ROM Library

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#161-studio-rom-library)

### 16.2 Pokecom GO Player

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#162-pokecom-go-player)

## 17. テスト戦略

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#17-テスト戦略)

### 17.1 Golden Test Data

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#171-golden-test-data)

### 17.2 テスト階層

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#172-テスト階層)

## 18. 初期マイルストーン

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#18-初期マイルストーン)

### Milestone 0: 仕様と期待値の固定

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#milestone-0-仕様と期待値の固定)

### Milestone 1: Headless PC-1245

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#milestone-1-headless-pc-1245)

### Milestone 2: macOS最小Pokecom GO Studio

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#milestone-2-macos最小pokecom-go-studio)

### Milestone 3: BASICクロス開発

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#milestone-3-basicクロス開発)

### Milestone 4以降

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#milestone-4以降)

## 19. 将来機能の拡張境界

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#19-将来機能の拡張境界)

### 19.1 Assembler / Disassembler

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#191-assembler--disassembler)

### 19.2 WAV Codec

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#192-wav-codec)

### 19.3 Character Editor

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#193-character-editor)

### 19.4 AI / MCP Integration

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#194-ai--mcp-integration)

## 20. ライセンスと由来管理

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#20-ライセンスと由来管理)

## 21. 非目標

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#21-非目標)

## 22. アーキテクチャ判断の基準

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#22-アーキテクチャ判断の基準)

## 23. まとめ

[移動先の節へ / Open this section](reference/ARCHITECTURE.md#23-まとめ)
