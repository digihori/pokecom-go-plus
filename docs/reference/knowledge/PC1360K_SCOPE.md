# PC-1360Kの解析・生成・実行

S2 BASICの行番号付きUTF-8ソースを使う。BASIC用RAMは `0x8000..0xFFFF`。漢字作業RAMは `0x2000..0x27FF` にあり、BASIC用RAMと用途が異なる。MCPのCALL先は主RAMに限定する。`0x4000..0x7FFF` はROMバンク窓。

LCDは25文字×4行。初回の初期化を済ませ、本体のMODEキーでRUN表示とコマンド待ちを確認する。ロードと実行はStudioで承認する。
