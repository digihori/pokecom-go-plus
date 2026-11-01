# PC-1245 BASIC・文字コード

対象はPGPの`old.pc-1245`方言定義です。実ROMの全構文・関数動作を保証する言語リファレンスではありません。
機種が違うBASICの構文や文字コードをそのまま移植しないでください。

登録するソースはUTF-8の行番号付きBASICテキストです。PC-1245の行番号は1〜999。
Tokenizerは文字列・REM内のキーワードを通常の命令トークンへ置換しません。
取得結果のcodeTableでkindがkeywordの項目はTokenizerが認識する語であり、各語の実行時動作の検証を意味しません。
Build成功も、実ROMで実行可能・要求仕様どおりという意味ではありません。

最小の表示例は`10 PRINT "HELLO"`と`20 END`です。実行結果は利用するROMで確認してください。
既存の混在ProjectのBASICは`10 CALL 50432`と`20 END`で、機械語入口`0xC500`を呼び出します。
説明は16進でも、BASIC入力には作例で確認した10進`CALL 50432`を使います。
assemblerの`0x`・`$`・`&`接頭辞がBASICでも同じ意味で使えるとは推定しません。

表示文字のπとBASIC関数PIは別のコードです。codeTableのkind（character／special／keyword）を区別してください。
PGPテキストでは`\PI`、`\SQR`、`\EX`、`\BX`、`\xNN`等のエスケープを使用します。
PC-1245は`\INS`非対応。リテラルのバックスラッシュは`\\`。
エスケープは文字列・REM内でも解釈されます。文字表示コードと実行可能なBASIC命令を混同しないでください。

メモリ上のトークン列は開始・終了`0xFF`、行終端`0x00`、OLDの3桁packed decimal行番号を使います。
実際のコード表・上限は応答のCore由来のstructured dataを確認してください。
Load時の管理ポインター更新はStudioの共通loaderに任せ、AIが独自の直接書込みで代用しないでください。

出典：Pc1245BasicDialect、Pc1245BasicTokenizer、PGP BASIC Text Format。
命令ごとの完全な文法、引数範囲、エラー条件、全ROM版での実行動作は未提供です。
