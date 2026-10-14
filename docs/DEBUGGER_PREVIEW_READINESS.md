# Debugger改善後のTechnical Preview準備

更新日: 2026-10-14

## 今回のまとまり

Studioの日英UIに続き、Debugger改善ステップ1〜4を実装した。
固定した実行操作・停止理由と移動、レジスタ／メモリ差分、読み書き／bank履歴、ブレークポイント／複数監視の登録管理を提供する。
詳細と検証は[Debugger仕様](DEBUGGER.md)、[ステップ1](DEBUGGER_STEP1_VERIFICATION.md)、
[ステップ2](DEBUGGER_STEP2_VERIFICATION.md)、[ステップ3](DEBUGGER_STEP3_VERIFICATION.md)、
[ステップ4](DEBUGGER_STEP4_VERIFICATION.md)を参照。

登録はResetで保持するが、新Sessionやアプリ終了後には引き継がない。
条件・ヒット回数・bank指定、Step Over／Out、ソース連携、設定保存は後続の候補である。
今回の公開準備では新しい機能を追加せず、実際の利用環境での確認を優先する。

## 確認状況

- [x] JDK 21・macOSローカルの全体build／Core tests成功
- [x] Studio 309件、失敗0、既存SDL条件付きskip 1件
- [x] Core 435件、失敗0（変更なしの既存成功結果を再利用）
- [x] 日英UIの隔離描画とSemanticsによる操作検証
- [ ] 今回のcommitを対象とするAndroid／Linux／Windows／macOS／iPhone SimulatorのCI成功
- [ ] macOS本番Studioで実ROMを使った停止・再開・Step・複数監視・入力focusの確認
- [ ] Windows／Linuxの実表示・操作確認
- [ ] Windowsの更新インストール確認
- [ ] 公開版の番号・tag確定、Release Notesと配布物の最終確認

既存commitのCI成功は今回の変更のCI結果と区別する。
iOSのDebugger UIは今回の対象ではなく、共有モジュールへの回帰と実機確認を区別する。
実Preferences／ROM Libraryを使う確認は隔離描画とは別に記録する。

## 公開までの扱い

今回は文書整備と実装のcommit／pushまでを行う。tag作成・Release公開・配布物公開は別の指示で行う。
packageVersionは1.3.1を維持し、公開済みtag・履歴は変更しない。
モバイルスキンは先行する別commitに含まれており、Debugger変更には混ぜない。
ROM・ROM由来fixtureは含めない。
