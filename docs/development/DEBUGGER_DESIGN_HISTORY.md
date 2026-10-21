# Debugger設計・強化・検証履歴

2026-10-22に現行ガイドから分離した計画・検証履歴です。
各節の「現在」「未実装」「後続」は記録時点を表し、現在の対応範囲を示しません。
現行の説明は[DEBUGGER.md](../guides/DEBUGGER.md)を参照してください。
元の本文を保持し、当時の未確認事項を後から完了扱いにはしていません。

## 12. UI再構成方針

2026-10-14のステップ1で固定操作・停止情報と左右配置／縮小時タブを実装した。
日英の検証範囲と未確認事項は[ステップ1検証記録](DEBUGGER_STEP1_VERIFICATION.md)を参照する。
ステップ3で読み書き／bank履歴をタブ表示した。以下はTraceと履歴の同時表示・切離し等を含む将来の基本案であり、
全体を実装したものではない。

```text
┌ CPU registers / stop reason / Run / Pause / Step ─────────┐
├ Disassembly ─────────────────┬ Memory / Memory Map / Watch ┤
│                              │                             │
├──────────────────────────────┴─────────────────────────────┤
│ Instruction Trace | Memory Access | Bank History           │
└────────────────────────────────────────────────────────────┘
```

PC-1360K解析ではDisassembly、Memory、Access History、Bank Historyの同時参照が重要になる。必要に応じて各パネルを
独立ウィンドウへ切り離せる構造も検討する。

## 13. 今後の課題

2026-10-15のユーザー方針により、今回の強化は0.5で一区切りとする。
残作業・再開時の前提は[フォローアップ記録](planning/DEBUGGER_FOLLOW_UP.md)にまとめた。
以下は将来候補であり実装依頼ではない。完了状態は[ROADMAP.md](planning/ROADMAP.md#phase-5-開発支援機能)を正とする。主な候補は次のとおり。

- 条件付きBreakpoint
- Symbol／Source Mapとソース行表示
- Memory編集と変更履歴
- Trace、Memory Access、Bank Historyのexport
- Checkpoint間および実機Checkpointとの差分表示
- PC-1360Kの漢字ROM／work RAM観測
- Assembly Workspace、Listing、Symbol Mapとの連携
- AIが提案したBreakpoint、Watch、Step、Continueの承認付き実行

機種固有の観測点を追加する場合も、Debugger UIからMemory Bus内部へ直接アクセスせず、Core SessionのSnapshotまたは
明示イベントとして公開する。


## 強化2の検証記録（2026-10-21）

検証：合成ROMによる命令前停止、全比較演算子、回数条件3種、再開時の二重計数防止、
Reset／有効切替／Load、入力拒否の不変性、Watch・Run to・Faultとの優先関係と、日英UIの追加・編集・解除を自動検証済み。
本番Studio・実ROMによる手動受入れは未実施。

2026-10-21検証結果：JDK 21の`./gradlew build :core:desktopTest`成功。
Desktop 355件、失敗0、既存条件付きskip 1件。日英・広幅／狭幅の登録画面を合成Sessionで隔離描画し、
条件入力欄・回数条件・一覧と折りたたみ説明を確認した。実設定・ROM Libraryは使用していない。


## 強化3の検証記録（2026-10-21）

合成ROMによる通常CALL・CAL、ネスト・再帰、PUSH／POP、スタック破損、bank切替と復元、
Breakpoint／Watch／Fault／Pauseの中断、上限、Load／Reset、既存Debug Contextとの互換性と、
日英UIのボタン状態・操作を自動検証済み。本番Studio・実ROMでの手動受入れは別途確認する。

2026-10-21検証結果：JDK 21で`./gradlew build :core:desktopTest`成功。
Desktop 364件、失敗0、既存条件付きskip 1件。合成Sessionの広幅／狭幅／低いウィンドウ、
日英・実行中・Faultの隔離描画を確認した。実設定・ROM Library・本番MCPは使用していない。


## 強化4の検証記録（2026-10-21）

合成Sessionと一時ディレクトリで保存／読取り／新Sessionと同一Sessionへの復元、対象不一致、
不正version・型・上限、基準値の再取得、新ID・回数0、失敗時の不変性、保存先の保護と、
日英UIの有効状態・編集フォーム／Trace表示の同期を自動検証済み。
本番Studio・実ROM・OS標準ファイル選択の手動受入れは別途確認する。

2026-10-21検証結果：JDK 21で`./gradlew build :core:desktopTest`成功。
Desktop 373件、失敗0、既存条件付きskip 1件。Trace容量縮小時の履歴切詰めとFault状態の保持も検証。
合成Sessionで日英・広幅／狭幅の設定パネルを隔離描画し、ボタンと説明の表示を確認した。
実設定・ROM Library・本番MCPは使用していない。
