# PC-1245の起動・観測と未提供情報

Loadはメモリへの配置です。Continueは現在のPCから再開する操作で、BASICのRUNやCALLを入力しません。
HELLO WORLDの混在ProjectはRUNモードで`RUN`とENTERを入力すると、BASICから`CALL 50432`で
機械語入口`0xC500`を呼びます。Assemblyだけを配置した場合の起動は`CALL 50432`です。
RTNで戻るコードを単にPCの変更で開始すると、正しい戻り先を準備できません。

キー操作・LCD画像取得・新規Project作成のMCP対応状況は実際のtools/listで確認してください。
現開発版ではPC-1245・停止中RUNモードのProjectに、承認付きの`pgp_propose_bounded_execution`を提供します。
runはCLEAR→R→U→N→ENTER、callはCLEAR→CALL＋10進RAMアドレス→ENTERを通常のキー列で入力します。
keysは明示したキー列、continueはキーなしの上限付き再開です。直接PCを変更しません。
1〜1000000命令、1〜5000000サイクルの両上限を指定し、最初に達した上限やBreakpoint等で停止します。
サイクル上限は命令終了時に確認するため、最後の1命令分だけ超える場合があります。
停止時に未送信キーを破棄・解放します。inputCompleteは送信完了で、ROMの処理完了の保証ではありません。
結果は`pgp_get_bounded_execution`、停止中LCDのピクセルとPNGは`pgp_get_display`で取得します。
上限で止まったことやSUCCEEDEDを正常終了と扱わず、表示・入力・終了後の応答を確認してください。
作例`pc1245-numeric-labels`には、INPUTで表示と次の数値入力待ちを行う確認済みの最小例があります。
未提供なら利用者の手動操作を依頼し、実施していない観測を成功と報告しないでください。
Debug Contextは停止中に取得します。Breakpoint、Trace、Stepを使い、登録・操作の承認と
Session ID・revisionを照合します。最終Memory値だけで途中の書込み順を断定しないでください。

キーのlayoutや保持cycleは機種仕様APIから取得できますが、ROMのキー読取りルーチンABIとは別です。
ROM表示・キー読取りルーチンの入口、引数、破壊レジスタは未提供です。
実機タイミング、任意ROM版のワーク領域やBASIC CALL時の全レジスタ保存規約も未保証です。

解析結果は取得した事実、推定、未確認事項を分けてください。作例のレジスタ保存条件を任意のコードへ一般化しません。
出典：AI_INTEGRATION、PC-1245 HELLO WORLD作例と受入れ課題。
