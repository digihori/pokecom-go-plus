# PC-1245のLCD直接描画

PGPのPC-1245表示は16桁、1桁5×7、論理80列×7行です。1列を1byteで表し、bit 0が最上段、bit 0〜6が画素です。
論理列0〜59は`0xF800 + column`、60〜79は`0xF868 + (79 - column)`に対応します。
後半20列は逆順です。シンボル領域`0xF83C`と`0xF83D`を画素データで上書きしないでください。
これらはPGPの表示実装由来の情報で、実機の電気的仕様を保証しません。

描画だけでは表示できない場合があります。作例では`LIP 0x5F`、`ORIM 0x01`、`OUTC`で
他の制御bitを維持してLCDを有効にします。実ROMのCALL中にLCDが無効になる場合への対処です。
ROMに戻った後はBASICの再描画で表示が変わり得ます。

ROM文字表示ルーチンのアドレス・呼出規約は未提供です。推測したROMアドレスをCALLしないでください。
独自の5×7字形を持つ`pc1245-hello-world`作例を`pgp_get_example`で取得できます。
現在の機種仕様APIにもdirectDrawingを掲載しています。別機種へこの配置や制御方法を適用しないでください。

出典：Pc1245Display、DevelopmentReadServiceのdirectDrawing、Pc1245HelloWorldTestと作例。
任意ROMでの呼出規約と表示タイミング、実機での動作保証は未提供です。
