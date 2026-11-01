# Studio内蔵SC61860 assembler

対象はStudio内蔵assemblerです。外部SC61等の構文・設定とは区別してください。
`;`または`#`から行末まではコメント。ラベルは英字か`_`で始まり、大文字小文字を区別しません。
ORGで配置を指定し、複数のORGも使えます。数値は10進と`0x`・`&`・`$`接頭辞の16進です。
この表記をBASICへそのまま転用しないでください。

DBはbyte・文字列、DWは16bit値を上位byte→下位byteの順で出力します。
DSは指定byte数を実際に埋め、ALIGNも実際に埋めます。未出力の穴を作る指示ではありません。
EQUは不変定数。式にはHIGH/LOW、四則、剰余、シフト、bit演算等を使用できます。
文字列は機種文字コードへ自動変換しません。Unicodeコード値0〜255をbyteとして扱います。
PC-1245のLCD字形データと、BASICの文字コードも別の表現です。

相対分岐は飛び先のラベルまたは絶対アドレスを記述し、assemblerが相対値へ変換します。
LPのoperandは`0x00..0x3F`。CALの飛び先は`0x0000..0x1FFF`。
TESTには即値が必要です（例：`TEST 0x01`）。

INCLUDE、INCBIN、マクロ、条件アセンブル、ファイル間シンボル共有は未対応です。
命令のopcode・長さ・operandEncoding・control flowは`sc61860.instruction.<mnemonic>`の知識項目で
CoreのSc61860InstructionSetから取得できます。encodable=falseのopcodeを命令生成に使わないでください。
同名NOP等は複数のopcodeを持つ場合があります。逆アセンブルでDBになるbyteを勝手に別命令へ置換しないでください。

この初版は各命令の完全な動作・フラグ変化・サイクル数を提供しません。推測による補完を仕様扱いしないでください。
出典：SC61860_ASSEMBLY、Sc61860InstructionSet、Sc61860AssemblerTest。
