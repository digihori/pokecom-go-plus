# Pokecom GO Studio 1.2.2

## 日本語

- AI／MCPの対応機種に PC-1261、PC-1350、PC-1401、PC-1450、PC-1360、PC-1360K、PC-1460、PC-1470U を追加しました。対象機種の資料や作例を参照し、Studioで承認しながらBASIC・アセンブリの作成、ビルド、ロード、実行に進めます。エミュレーターでの実行には対象機種のROM登録が必要です。
- LCDの境界をまたぐ描画や、ブザーを使った音階・メロディーの作例と生成ガイドを追加しました。PC-1261実機で音が約0.5秒ずつ途切れる場合の対処も、実機で確認して発音ガイドに収録しました。
- Windows向けに日本語のMSIインストーラーを追加しました。英語版MSIも引き続き利用できます。Smart App Controlが有効な一部のWindows 11環境では、未署名のStudio実行ファイルの起動がブロックされる場合があります。

接続はStudioの「デバッグ」→「AI / MCPサーバー」から始めます。[AI接続ガイド](https://github.com/digihori/pokecom-go-plus/blob/v1.2.2/docs/guides/AI_CONNECTION_GUIDE.md) · [プログラム生成ガイド](https://github.com/digihori/pokecom-go-plus/blob/v1.2.2/docs/guides/AI_PROGRAM_GENERATION.md)

## English

- AI and MCP now support eight more models: PC-1261, PC-1350, PC-1401, PC-1450, PC-1360, PC-1360K, PC-1460, and PC-1470U. Access model references and examples, then review and approve BASIC and assembly project creation, builds, loading, and execution in Studio. Emulator execution requires a registered ROM for the target model.
- Added examples and generation guidance for drawing across LCD memory boundaries and playing scales and melodies with the buzzer. The buzzer guide includes a hardware-verified workaround for roughly half-second gaps in playback on the PC-1261.
- Added a Japanese-language Windows MSI alongside the English installer. On some Windows 11 systems, Smart App Control may block the unsigned Studio executable.

Open the AI / MCP Server from Studio's Debug menu. [Connection guide](https://github.com/digihori/pokecom-go-plus/blob/v1.2.2/docs/guides/AI_CONNECTION_GUIDE.md) · [Program generation guide](https://github.com/digihori/pokecom-go-plus/blob/v1.2.2/docs/guides/AI_PROGRAM_GENERATION.md)
