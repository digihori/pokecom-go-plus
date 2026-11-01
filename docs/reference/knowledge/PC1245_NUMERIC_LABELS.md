# PC-1245の数値入力・分岐・次入力待ちの作例

exampleIdは`pc1245-numeric-labels`。新規生成課題G01の自作BASIC例で、
数値1＋ENTERでONE、2でTWO、その他でOTHER、0でENDに到達する。
ラベルを`INPUT "ONE";A`等のプロンプトに使い、表示した状態で次の数値入力を待つ。
先頭は`INPUT A`の`?`、ラベル表示時は入力カーソルも現れる。
`IF A=1 THEN 70`はこの作例で利用する行番号分岐で、全IF構文の検証ではない。

2026-11-01にPC-1245の利用者登録済みROMとmacOS配布アプリで、作成・Build・Load・
上限付きRUN・キー入力・停止後LCD画像の取得を確認した。
1→ONE、2→TWO、3→OTHER、0→通常の`>`を観測し、終了後の1＋ENTERは
ONEではなく通常の数値1の応答になった。
利用したROM以外や実機の保証ではなく、非数値入力の処理も対象外。

利用時は本文のUTF-8 Sourceを新規作成提案に使う。保存先はStudioで選び、BuildとLoadも別に承認する。
停止中のRUNモードで`pgp_propose_bounded_execution`のrunを提案し、
200000命令／500000サイクル等の上限とCLEAR→R→U→N→ENTERのキー列を確認して承認する。
operation停止後に、NUM_1→ENTER等のキー入力を1要求ずつ承認する。
`pgp_get_display`を、その停止時のSession IDとrevisionで取得して結果を確認する。

上限停止はプログラム終了ではなく、inputComplete=trueというキー列の送信完了もROMが入力を処理した証明ではない。
入力途中で停止すると残りを破棄・解放し、自動再送しない。副作用が途中まで発生し得るため、
LCDとContextを見てから次の操作を提案する。
ROMルーチンABI、実機タイミング、他機種への移植可能性は保証しない。

出典：test-data/basic/ai/pc1245-numeric-labels、docs/development/AI_PROGRAM_GENERATION_VERIFICATION.md。
