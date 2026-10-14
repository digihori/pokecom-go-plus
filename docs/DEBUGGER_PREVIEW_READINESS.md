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
追加でAI向け登録状態の読み取りを実装した。実際の利用環境での確認を公開前に行う。
読み取りAPIの仕様と既存クライアントの設定更新は[AI連携](AI_INTEGRATION.md#登録状態の読み取り2026-10-14)を参照。

## 確認状況

- [x] JDK 21・macOSローカルの全体build／Core tests成功
- [x] Studio 312件、失敗0、既存SDL条件付きskip 1件
- [x] Core 435件、失敗0（変更なしの既存成功結果を再利用）
- [x] 日英UIの隔離描画とSemanticsによる操作検証
- [x] macOS本番StudioとAIクライアントの実接続で登録取得成功（停止中PC-1470U、基本取得のみ）
- [ ] 今回のcommitを対象とするAndroid／Linux／Windows／macOS／iPhone SimulatorのCI成功
- [ ] macOS本番Studioで実ROMを使った停止・再開・Step・複数監視・入力focusの確認
- [x] Windows／Linuxの動作をユーザー確認（2026-10-14）
- [x] Windowsの更新インストールをユーザー確認（2026-10-14）
- [ ] 公開版の番号・tag確定、Release Notesと配布物の最終確認

既存commitのCI成功は今回の変更のCI結果と区別する。
iOSのDebugger UIは今回の対象ではなく、共有モジュールへの回帰と実機確認を区別する。
実Preferences／ROM Libraryを使う確認は隔離描画とは別に記録する。

## 公開までの扱い

2026-10-14のユーザー指示によりTechnical Preview 0.5の公開準備・tag・Release公開を進める。
候補のCIと配布物の確認は[0.5チェックリスト](RELEASE_CHECKLIST_0.5.0-alpha.1.md)へ記録する。
packageVersionは1.3.1を維持し、公開済みtag・履歴は変更しない。
モバイルスキンは先行する別commitに含まれており、Debugger変更には混ぜない。
ROM・ROM由来fixtureは含めない。
