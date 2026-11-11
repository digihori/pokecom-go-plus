# Pokecom GO Studio 1.2.0

## 日本語

**Windows 11の既知の制約**

Windows版のMSIとZIPに含まれるStudioの実行ファイルは未署名です。Smart App Controlが有効な一部の環境では、インストールやZIPの展開が完了しても、`PokecomGOStudio.exe` の起動がブロックされることがあります。

- Studioで `.pgmacro` を実行できます。ファイルのロード、モード切替、コマンド送信、待機、キー入力、一時停止に対応し、実行状況をコンソールに表示します。実行中のマクロは中断できます。ROMエラーの自動検出は対応機種で有効です。
- プロジェクトで選択したファイルをビルドしてロードできます。新規ファイルの種類選択、既存ファイルの追加、右クリックからの改名やプロジェクト内ゴミ箱への移動にも対応しました。
- ゲームパッドの左右アナログスティックとL1／L2／R1／R2を割り当てられます。
- macOSでBluetoothヘッドフォンを使うと短いBEEPが鳴らない問題を修正しました。修正後のBluetooth音声再生はmacOS、Windows、Linuxで確認済みです。

公開版数は1.2.0、インストーラ内部版数は1.9.0です。Windows MSIのUpgradeCodeは維持しています。

## English

**Known limitation on Windows 11**

The Studio executable included in the Windows MSI and ZIP packages is unsigned. On some systems with Smart App Control enabled, Windows may block `PokecomGOStudio.exe` from launching after installation or ZIP extraction.

- Run `.pgmacro` files in Studio to load files, switch modes, send commands, wait, send keys, or pause. The console shows progress, and running macros can be stopped. ROM error detection is available for supported models.
- Build and load selected project files. Create files by type, import existing files, or use the context menu to rename files and move them to the project trash.
- Map both analog sticks and the L1, L2, R1, and R2 controls.
- Fixed short BEEP sounds going silent over Bluetooth on macOS. Bluetooth audio playback was verified after the fix on macOS, Windows, and Linux.

Public version 1.2.0 uses native installer version 1.9.0. The Windows MSI UpgradeCode is unchanged.
