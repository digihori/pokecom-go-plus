# Pokecom GO Studio 1.1.2

Changes since Studio 1.1.0 / Studio 1.1.0からの変更。

## 日本語

PC-1251系・PC-1261系・PC-1401系のRAM設定を改善しました。

- 各系列で「実機RAM」と「RAM制限解除」を選択し、設定を系列ごとに保存・復元できます。未設定時は実機RAMを使用します。
- PC-1251系の制限解除時は8000〜CFFF、PC-1261系は4000〜7FFF、PC-1401系は2000〜5FFFのRAM構成を使用します。
- Debuggerのメモリマップ、Projectの配置検査、BASIC／メモリイメージの読込みを、選択したRAM構成へ合わせました。
- 制限解除したRAMをROMがBASIC用に使用する場合も、有効な管理ポインタを尊重して入出力します。PGP側からBASIC開始位置や容量認識を強制変更しません。

RAM構成の変更前に、必要なプログラムをファイルへ保存してください。
機種・ROM版によってBASICが認識する容量は異なります。RAM制限解除がBASICの使用可能容量増加を保証するものではありません。
対応14モデル・ROM登録方式は従来どおりで、ROM・ゲームは同梱しません。

配布対象はmacOS、Windows x64、Linux x86_64です。macOSの実際の対象アーキテクチャは配布ファイル名を確認してください。
macOS資産はDeveloper ID署名・公証付き、Windows資産は未署名です。
公開版数は1.1.2、インストーラ内部版数は1.8.0です。Windows MSIのUpgradeCodeは維持しています。

今回のGitHub ReleaseはStudio向けです。Playerの変更内容・ストア公開状況は[Player 1.1.2のノート](https://github.com/digihori/pokecom-go-plus/blob/main/docs/releases/notes/RELEASE_NOTES_1.1.2.md)を参照してください。Playerのストア公開は別作業です。

## English

Improved RAM configuration for the PC-1251, PC-1261 and PC-1401 families.

- Choose physical RAM or unrestricted RAM independently for each family; settings are saved and restored. Unconfigured families default to physical RAM.
- Unrestricted ranges are 8000–CFFF for PC-1251, 4000–7FFF for PC-1261 and 2000–5FFF for PC-1401 families.
- Debugger memory maps, Project placement checks and BASIC/memory-image loading follow the selected RAM configuration.
- BASIC I/O respects valid firmware-managed pointers, including lower start addresses selected by firmware with expanded RAM. It does not patch firmware capacity detection or force BASIC start addresses.

Save your programs before changing RAM configurations. BASIC capacity depends on the model and ROM version; unrestricted RAM does not guarantee more BASIC program space.
The same 14 models remain supported. ROMs and games are not included.

Packages target macOS, Windows x64 and Linux x86_64. Check macOS asset names for the actual architecture. macOS assets are Developer ID signed and notarized; Windows assets remain unsigned.
Public version: 1.1.2. Native installer version: 1.8.0. The Windows MSI UpgradeCode is unchanged.
This GitHub Release distributes Studio. Player store releases are handled separately.
