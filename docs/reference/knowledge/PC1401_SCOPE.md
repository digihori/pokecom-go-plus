# PC-1401の解析・生成・実行

- BASICはPC-1401のS1形式。ソースは行番号付きUTF-8。特殊記号は `\PI`、`\SQR`、`\INS`、`\BX` に対応する。
- 標準の物理RAMは `0x3800..0x47FF`。RAW・DMPの解析はこの範囲に限定する。実行中のRAM構成は `pgp_get_machine_spec` で確認する。
- LCDは16文字×1行、論理80×7ドット。停止中はMCPでピクセルとPNGを取得できる。
- BASICの生成・ビルド・ロード・RUNは、PC-1401のプロジェクトと登録済みROMを使う。ロードと実行はStudioの承認が必要。RUN／CALLの提案はBASICのコマンド待ちで行う。
