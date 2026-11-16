# PC-1470Uの解析・生成・実行

PC-1470U用S2 BASICの行番号付きUTF-8ソースを使う。StudioのRAM互換窓は `0x8000..0xFFFF`、BASICの標準開始位置は `0x8030`。`0x4000..0x7FFF` は8バンクのROM窓。

LCDは24文字×2行。初期化後にPROモードでロードし、RUNモードへ切り替える。ロードと実行はStudioで承認する。
