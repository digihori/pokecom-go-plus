# PC-1251 数値入力・分岐の作例

exampleId：pc1251-numeric-labels。machineId：pc-1251。
manifestとBASIC本文をpgp_get_exampleから取得できる。取得だけでファイルは作らない。

BASICはINPUT AとIF／THENの分岐で、1→ONE、2→TWO、その他→OTHER、0→END。
各ラベルを次のINPUTのプロンプトとして表示し、続けて次の数値を入力する。
PC-1245版と本文は共通だが、対象機種・manifest・観測記録は別に管理する。

2026-11-02、macOS配布アプリの同梱stdio MCP、PC-1251とローカル登録済みの1つのROMで、
新規Project作成→Build→Load→RUN→1／2／3／0入力→LCD PNGを確認した。
実機確認ではなく、任意ROM・他OS・他AIクライアントの受入れを保証しない。

RUNと各入力の観測上限は200000命令／500000サイクル。
停止結果がcycleLimitでもプログラム終了とは限らない。
inputCompleteはキー列の送信完了だけを表す。表示と通常入力への復帰を別に照合する。
0で「>」へ戻った後、CLEAR、1、ENTERで通常の数値1.になることも確認した。

この作例はBASICのみ。CALL検証の機械語は同梱作例に含めない。
PC-1251では長いCALLキー列が500000サイクルで途切れた観測がある。
未送信キーは破棄されるため、表示を確認せず残りを自動再送しない。
CALL入力完了後もRAMの観測を使って実行を確認する必要がある。
