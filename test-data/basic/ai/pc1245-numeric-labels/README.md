# PC-1245 Numeric Labels

AI新規生成の共通課題G01を確認する自作BASIC例。ROMは含まない。
1を入力してENTER→ONE、2→TWO、それ以外→OTHER、0→ENDで通常入力へ戻る。
ラベルを次のINPUTのプロンプトにして、表示と次の数値入力待ちをまとめている。
先頭は通常のINPUTの`?`、ラベルの後ろにはカーソルが表示される。

作業用フォルダーへコピーしてPC-1245でBuild・承認付きLoadし、RUNモードで起動する。
MCPでは上限付き実行のrunを使い、各NUMキー＋ENTERを別の承認付き要求として送る。
検証では上限200000命令／500000サイクルを指定し、停止後のLCD画像を取得した。
上限停止自体をプログラム終了の証拠にしない。

2026-11-01：macOS配布アプリ、利用者登録済みROM、stdioブリッジで、
新規作成→Build→Load→RUN→1／2／3／0の表示と、END後の通常BASIC応答を確認。
実機・他ROM版・他OS・独立した他AI受入れは未検証。
詳細：[生成検証記録](../../../../docs/development/AI_PROGRAM_GENERATION_VERIFICATION.md)。
