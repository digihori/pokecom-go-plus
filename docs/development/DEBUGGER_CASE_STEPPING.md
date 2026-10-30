# CASE経路とStep Over／Outの対応範囲

調査・検証日：2026-10-30。Debugger強化の作業7です。

## 現行Coreで確認した動作

外部実装の移植は行わず、PGPの現行実装を調査しました。
根拠は`core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/cpu/Sc61860Cpu.kt`の
`case1`、`case2`、`returnFromCall`と、既存の`Sc61860CpuTest`のCASE試験です。

| 命令 | PC・スタックの動作 | Debuggerでの扱い |
| --- | --- | --- |
| CALL／CAL | 次の命令位置を2byte積んで分岐 | 従来の観測済みフレーム |
| CASE1 | Dへ比較数を設定し、オペランドの明示的な戻り先を2byte積む。PCは4byte先へ進む | 明示的な戻り先を持つ新フレーム |
| CASE2 | Aとテーブルを比較し、一致先または既定先へ分岐。戻り先を積まない | 分岐命令として既存フレームを保持 |
| RTN | Rが指す2byteを戻り先として読み、Rを2進める | 観測した最上位フレームと照合して復帰 |

CASE1はCALLとは異なり、命令直後のPCを戻り先としません。
CASE1の24bitオペランドの下位16bitを戻り先PCとしてデコードし、その位置の物理ROM情報を
実行前に取得します。CASE1自身の次のPCとは別々に検証します。Coreの命令実装・命令属性は変更しません。

## 追加した検証と動作

CASE1の実行後、Rが2減っていること、戻り先2byteが一致すること、PCが4byte先であることを確認します。
既存フレームとのスタック領域重複と、128フレーム上限も既存CALLと同じ規則で検証します。
RTNではR・戻り先byte・復帰後のR／PC・物理ROM位置を照合します。
完了はアドレス到達だけでなく一意なフレームIDの復帰イベントで判定します。

- CASE1からのStep Overは、そのCASEフレームのRTNまで進みます。
- CASE1を通常Stepで観測した後は、そのフレームにStep Outできます。
- CALLのStep Over／Out中もCASEを通過できます。CASE内のCALLやCASEの入れ子も同じモデルで扱います。
- CASE2そのもののStep Overは分岐1命令だけ実行します。既存の観測済み戻り先は保持します。
- 途中Breakpoint・監視・Fault・手動Pauseと実行上限の優先度を維持します。

## 未観測スタックと残す制限

内部RAMの2byteがコードらしいアドレスでも、戻り先なのか一時データなのか、どの呼出しに
対応するのかを保証できません。RとRAMだけから過去のフレームを再構成する機能は追加しません。
高速一括実行後は追跡を破棄し、フレームがなければStep Outを無効にします。
スタックの一時書換え、未公開RAM bank、Coreでモデル化していない非同期割込みにも対応を推定しません。

CASE命令の観測だけを理由とする追跡破棄・実行前の未対応停止を取り除きました。
戻り先・スタックの不整合は引き続き追跡不能として停止します。
既存の停止理由とMCP schemaは変更しません。`UNSUPPORTED_FLOW`の状態名は表示互換のため保持します。

## 検証結果

`DebuggerCaseSteppingTest`で以下を合成ROMにより確認しました。

- CASE2の1番目・2番目の一致と、全不一致時の既定分岐。
- CASE1からのStep Over、観測後のStep Out、CASE2の1命令Step Over。
- CALL内のCASE、CASE内のCALL、同じ戻り先を持つCASEの入れ子。
- Breakpoint・書込み監視による中断後のStep Out、Fault・戻り先破損・実行上限。
- 戻り先が別のROM領域にある場合と、戻り先bankを変更したまま／復元した場合。
- 高速一括実行で観測しなかったCASEフレームを復元しないこと。

既存の`DebuggerSteppingTest`も実行し、CALL／CAL、再帰、PUSH／POP、bank、
Load失敗時の追跡保持等への回帰がないことを確認しました。
`DebuggerControlsTest`で日英UIからCASE1を観測し、Step Outの有効化と完了表示を確認しました。

JDK 21で`./gradlew :core:desktopTest :desktopApp:test build`成功。
Core 500件・Studio 466件、失敗0・条件付きskip 2件です。増分buildの未変更タスクは再利用しています。
配布アプリ・実ROM・他OSの手動受入れ、特殊なスタック操作を行う実機プログラムとの一致は未確認です。
