# Pokecom SC61 Studio 1.2.1

## 日本語

- MCPでAIクライアントと接続し、PC-1245・PC-1251の機種情報や同梱作例、BASIC・アセンブリのソースを参照できます。対応するRAM範囲の機械語も解析できます。
- AIが提案したプロジェクト、ソースの変更、ビルド、ロード、実行は、Studioで内容を確認して承認できます。エミュレーターで実行するには対象機種のROM登録が必要です。
- Codexで開発・検証し、Claude DesktopとVS Code Copilotからの接続をmacOSとWindowsで確認しました。Google Antigravity CLIはmacOSで機種情報の取得を確認しました。
- macOSはDMGを開き、Studioを終了してからアプリケーションフォルダーにコピーします。WindowsはMSIで更新できます。MSIのUpgradeCodeは従来と同じです。Smart App Controlが有効な一部のWindows 11環境では、未署名のStudio実行ファイルの起動がブロックされる場合があります。

接続はStudioの「デバッグ」→「AI / MCPサーバー」から始めます。[AI接続ガイド](https://github.com/digihori/pokecom-go-plus/blob/v1.2.1/docs/guides/AI_CONNECTION_GUIDE.md) · [プログラム解析ガイド](https://github.com/digihori/pokecom-go-plus/blob/v1.2.1/docs/guides/AI_PROGRAM_ANALYSIS.md)

## English

- Connect AI clients through MCP to access PC-1245 and PC-1251 reference information, bundled examples, BASIC and assembly sources, and machine code analysis for supported RAM ranges.
- Review and approve AI proposals for project creation, source changes, builds, loading, and execution in Studio. Emulator execution requires a registered ROM for the target model.
- Developed and tested with Codex. Claude Desktop and VS Code Copilot connections were verified on macOS and Windows. Google Antigravity CLI was verified on macOS for reading machine capabilities.
- On macOS, quit Studio and copy the application from the DMG into Applications. On Windows, use the MSI to update. The MSI UpgradeCode is unchanged. On some Windows 11 systems, Smart App Control may block the unsigned Studio executable.

Open the AI / MCP Server from Studio's Debug menu. [Connection guide](https://github.com/digihori/pokecom-go-plus/blob/v1.2.1/docs/guides/AI_CONNECTION_GUIDE.md) · [Program analysis guide](https://github.com/digihori/pokecom-go-plus/blob/v1.2.1/docs/guides/AI_PROGRAM_ANALYSIS.md)
