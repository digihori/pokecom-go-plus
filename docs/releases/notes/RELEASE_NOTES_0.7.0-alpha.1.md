# Pokecom GO Studio Technical Preview 0.7 (alpha 1)

公開準備中 / Release candidate — not yet published.

予定Git tag / Planned Git tag: `v0.7.0-alpha.1`
比較対象 / Changes since: `v0.6.0-alpha.2`

## 日本語

Playerで使うROMを準備し、PCではプログラムを作成・解析するためのTechnical Previewです。
Android Player `0.7.0-alpha.1`のGoogle Play初公開に合わせたStudioの更新です。
Android Playerは[Google Play](https://play.google.com/store/apps/details?id=com.digihori.pgp.player)で公開済みです（2026-10-23 JSTに掲載を確認）。

### PlayerとつながるROM・プログラムの準備

- 対応14モデル：PC-1245、PC-1250／1251／1255、PC-1260／1261、PC-1350、
  PC-1360／1360K、PC-1401／1402、PC-1450、PC-1460、PC-1470U。
- `File` → `Create ROM Set…`（ファイル → ROMセットを作成）で、所有する機種から
  適法に取得した内部／外部ROMを`.pgrom`へまとめ、Playerへ登録できます。
  バンクROMは番号順に連結したファイルが必要です。PC-1360Kの漢字機能には漢字ROMが必要です。
- PC-1360Kの漢字ROM吸い出し補助ツールにCE-140F向けの手順を追加しました。
  ROMの抽出は別途、対応実機・周辺機器・手順が必要です。
- [日本語セットアップ](https://digihori.github.io/pokecom-go-plus/player/ja.html)と
  [英語セットアップ](https://digihori.github.io/pokecom-go-plus/player/en.html)を公開しました。
  StudioからPlayerへは`.pgrom`、機種に合う`.bas`／`.dmp`をファイルとして渡します。自動同期はありません。

### Studioの主な更新

- **WAV Tool**：OLDに加えてS1／S2 BASICと共通Binary形式のファイル変換・自動判別を整備。
  raw・波形・診断の照合と、検証済みプログラムの保存に対応しました。
  S1 Binaryの末尾処理をpcwav 0.5.2と整合させました。
- **Debugger**：ブレークポイント／監視の登録管理、読み書き・バンク履歴、
  ソース位置との連携、設定の保存・復元などを強化しました。
- **AI / MCP**：localhostサーバー経由で機種仕様・開発状態を取得し、ソース差分、
  Build、デバッグ操作、Load／ContinueをStudio上の承認付きで行えます。
  MCPサーバーは利用者が起動する機能で、AIサービスやアカウントは同梱しません。
- **外部アセンブラ**：SC61、YASM61860、汎用DMP出力ツールの個別プロファイルと
  タブ付き設定を整備しました。外部ツール本体や必要な実行環境は利用者が用意します。
- **補助ツール**：単独のメモリ操作ツールとpcwavツールをリポジトリの`tools/`へ収録し、MIT通知を追加しました。
  これらは別途実行するツールで、Studioの配布アプリへ組み込んだ機能ではありません。
- macOS標準のアプリメニューへ「このアプリについて・設定・終了」を接続し、
  重複して表示されていたStudioメニューを解消しました。
- StudioとPlayerのアプリアイコンを追加しました。Studioは柔らかいシルバー、
  Playerは黄・赤キーを使ったコントラストの強いデザインです。
- Android／iPhone Playerで全14モデルの統合とホーム画面・入力・音声対応を進めました。
  機種対応と端末・OSごとの詳細検証は別に管理しています。

詳しい操作：
[WAV Tool](../../guides/WAV_TOOL_GUIDE.md) · [Debugger](../../guides/DEBUGGER.md) ·
[AI接続](../../guides/AI_CONNECTION_GUIDE.md) · [外部アセンブラ](../../guides/EXTERNAL_ASSEMBLER.md)

### 配布と制限

- Studio：macOS Apple Silicon DMG／ZIP、Windows x64 MSI／ZIP、Linux x86_64 DEB／tar.gz。
- Android Player：GitHubの配布workflowでは従来どおり**debug署名APK**を生成します。
  Google Playへ提出した署名済みAABとは別の配布経路です。署名が異なるアプリへの上書きはできません。
- iOSのインストール用配布物はありません。iPhone版は開発中で、ストア配布とは別工程です。
- ROM、ゲーム、外部アセンブラは同梱しません。ROMは利用者が用意してください。
- Studioは配布用証明書による署名・macOS公証を行わないプレビューです。
  OSの警告が表示される場合があります。内部パッケージ版は`1.5.0`、Windows UpgradeCodeは維持します。
- WAVはファイル変換・解析です。音声デバイスからの直接録音、実機往復の保証、
  44100／48000HzのS1／S2 Decode、Player側のWAV入出力は対象外です。
- AI連携のLoadは停止を維持し、Continueは現在PCから再開します。BASICのRUNやCALLを自動入力しません。
- Playerのセーブステートはありません。終了前にBASICを保存してください。
  音声や機種・ROM版・OSによる差は継続確認中です。
- 更新前にプログラム、プロジェクト、ROM原本をバックアップしてください。
  今回の配布物に対する検証状態は[チェックリスト](../checklists/RELEASE_CHECKLIST_0.7.0-alpha.1.md)を参照してください。

## English

This Studio preview accompanies Android Player `0.7.0-alpha.1`, now available on
[Google Play](https://play.google.com/store/apps/details?id=com.digihori.pgp.player)
(listing verified October 23, 2026 JST).
Prepare ROM packages for Player and create, run and inspect programs on your desktop.

### Highlights

- Support for 14 models: PC-1245, PC-1250/1251/1255, PC-1260/1261, PC-1350,
  PC-1360/1360K, PC-1401/1402, PC-1450, PC-1460 and PC-1470U.
- Create `.pgrom` packages from your lawfully obtained internal/external ROM dumps with
  **File → Create ROM Set…**. Banked ROMs require a file concatenated in bank order;
  PC-1360K kanji features require the corresponding kanji ROMs.
- Published [English](https://digihori.github.io/pokecom-go-plus/player/en.html) and
  [Japanese](https://digihori.github.io/pokecom-go-plus/player/ja.html) setup guides.
  Transfer `.pgrom` and machine-compatible `.bas`/`.dmp` files manually; no automatic synchronization.
- S1/S2 WAV conversion and automatic decoding alongside OLD, with linked raw/waveform
  diagnostics and validated program export. S1 Binary tails now match pcwav 0.5.2.
- Debugger improvements for breakpoint/watch management, access/bank history,
  source integration and saving/restoring settings.
- Localhost MCP integration for machine/development context and Studio-approved source
  changes, builds, debugging, loading and continuing execution. Start the server explicitly;
  an AI client/service is not bundled. Load stays paused; Continue resumes at the current PC.
- On macOS, About, Preferences and Quit use the native application menu, eliminating
  the duplicate Studio menu.
- Separate external-assembler profiles and tabbed settings for SC61, YASM61860 and
  generic DMP tools. Supply external tools and their runtimes separately.
- Standalone memory utilities and pcwav tools in the repository’s `tools/` directory
  (run separately, not integrated into the Studio application), with MIT notices; a CE-140F variant
  of the PC-1360K kanji ROM extraction helper; new Studio and Player icons.
- Android/iPhone Player integration across all models. Per-device/OS verification remains distinct.

### Downloads and limitations

Studio packages: macOS Apple Silicon DMG/ZIP, Windows x64 MSI/ZIP, Linux x86_64 DEB/tar.gz.
The workflow also produces an Android **debug-signed APK**, separate from the signed
AAB submitted to Google Play. Differently signed installations cannot update each other.
There is no iOS installable release asset. ROMs, games and external assemblers are not included.

Studio preview packages have no distribution-certificate signing or macOS notarization.
Native package version: `1.5.0`; Windows MSI UpgradeCode is unchanged.
WAV support is file conversion/analysis, not direct audio recording or guaranteed hardware
round trips. S1/S2 decoding at 44100/48000Hz and Player WAV I/O are not supported.
Player has no save states; save BASIC before exiting. Audio and device/ROM/OS differences
remain under investigation. Back up programs, projects and original ROMs before updating.
See the [release checklist](../checklists/RELEASE_CHECKLIST_0.7.0-alpha.1.md) for verification status.
