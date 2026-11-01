# PC-1251 数値入力・分岐の作例

PGP独自の小さなBASIC作例。2026-11-02にPC-1251エミュレーターと登録済みの1つのROMで観測した。
実機・任意ROM・PC-1250／PC-1255の動作保証ではない。

StudioでProjectを開きBuild、停止中の同じ機種へLoadする。
通常のBASICプロンプトを確認してから上限付きRUNを提案する。
RUNおよび各入力は200000命令／500000サイクルで観測した。
1 ENTER→ONE、2 ENTER→TWO、3 ENTER→OTHER、0 ENTER→END後の通常プロンプト。
表示ラベルは次のINPUTのプロンプトで、末尾の下線は入力カーソル。
END後にCLEAR、1、ENTERを送ると通常の数値1.を表示する。

inputComplete=trueはキー列の送信完了であり、ROMの入力受理やプログラム終了を保証しない。
上限でエミュレーターが停止しても、BASICプログラムがENDしたとは限らない。
本文はPC-1245版と同じだが、manifest・検証結果・適用機種を分けて提供する。

[検証記録](../../../../docs/development/AI_MACHINE_ROLLOUT_VERIFICATION.md)を参照。
