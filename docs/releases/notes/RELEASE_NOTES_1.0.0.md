# Pokecom GO Studio / Player 1.0.0

公開準備中 / Release preparation. 正式公開前に配布物と受入れ結果を確認します。
Git tag: `v1.0.0`。比較対象 / Changes since: `v0.7.0-alpha.1`。

## 日本語

Pokecom GO Studioは、PC上でポケコンのプログラムを作成・実行・解析する開発環境です。
Android版Pokecom GO Playerは実行に特化したアプリです。ROM・ゲームは同梱しません。

### 1.0.0の対象

- Studio：macOS Apple SiliconのDMG／ZIP、Windows x64のMSI／ZIP、Linux x86_64のDEB／tar.gz。
- Android Player：Android 8.0以降。Google Playで更新します。
- 14モデル：PC-1245、PC-1250／1251／1255、PC-1260／1261、PC-1350、PC-1360／1360K、PC-1401／1402、PC-1450、PC-1460、PC-1470U。
- StudioのProject・Assembler・Debugger・WAV・Character Editor・承認付きAI連携は0.7の実装範囲を継続します。
- iOS版のリリースは別途進めます。今回のインストール用配布物には含みません。

### 0.7からの変更

- PC-1245／1250の不要なRAMミラーと、1245のRAM外書込み許可を削除。1251の9000／9800からB800へのミラーを追加しました。
- RAMのCPUアクセスとロード配置検査を整合。125xの拡張設定は1255相当のRAM配置を使います。1260／1261のRAMミラーなしの仕様を確認しました。
- AndroidのROM Libraryをバックアップ・端末移行から除外する設定を、実際の保存先に合わせて修正しました。
- Androidポリシーにホーム表示形式・シリーズ内の選択機種の保存を補足。初回ガイド・機種表・同梱案内を整理しました。
- 正式版のAndroid情報画面から固定のTechnical Preview表記を外しました。
- macOSのDeveloper ID署名・公証を配布工程へ追加しました。最終配布物での確認は公開チェックリストに記録します。

### 配布・更新・制限

- Windowsの無料OSS署名を検討しています。採用までは未署名で配布し、OSの警告が表示される場合があります。
- Androidのdebug署名APKはCIの検証用成果物です。GitHub Releaseには添付しません。Google Play版と署名が異なります。
- 公開版数は1.0.0、Studioの内部パッケージ版数は1.6.0です。Windows MSIのUpgradeCodeは維持します。
- 更新前にプログラム・Project・ROM原本をバックアップしてください。Playerにセーブステートはありません。
- MEM表示の期待値との不一致は保留中です。機種・ROM版ごとの完全な実機一致を保証するものではありません。
- Step Over／OutのCASE経路等、WAVの44.1／48kHz S1／S2 Decodeなど、既存の未対応範囲は維持します。
- ROM・ソース・秘密情報を不具合報告へ添付しないでください。

操作は[ドキュメント目次](https://github.com/digihori/pokecom-go-plus/blob/v1.0.0/docs/README.md)、
候補版の確認範囲は[公開チェックリスト](https://github.com/digihori/pokecom-go-plus/blob/v1.0.0/docs/releases/checklists/RELEASE_CHECKLIST_1.0.0.md)を参照してください。

## English

Pokecom GO Studio is a desktop environment for creating, running and inspecting pocket-computer programs.
Android Player focuses on execution. ROMs and games are not included.

### Scope and changes

- Studio packages: macOS Apple Silicon DMG/ZIP, Windows x64 MSI/ZIP, Linux x86_64 DEB/tar.gz.
- Android Player: Android 8.0 or later, updated through Google Play. iOS distribution is separate.
- The same 14 models and the implemented 0.7 development features remain available.
- Corrected PC-1245/1250 RAM aliases, added PC-1251 lower-RAM aliases, and aligned CPU and loader mapping.
  Expanded 125x profiles use the PC-1255 RAM configuration; 1260/1261 have no RAM mirrors.
- Corrected Android ROM Library backup exclusions, clarified locally saved preferences in the privacy policy,
  and updated setup guides, model tables and bundled information.
- Stable Android versions no longer display the Technical Preview label.
- Added Developer ID signing and notarization to the macOS distribution process.

### Distribution and limitations

Windows packages remain unsigned until a suitable free OSS signing arrangement is adopted; OS warnings may appear.
Android debug APKs remain CI artifacts and are not GitHub Release attachments. Use Google Play for normal Android updates.
Public version: 1.0.0; Studio native package version: 1.6.0, preserving the Windows MSI UpgradeCode.
Back up programs, projects and original ROMs before updating. Player has no save states.
Unexpected MEM values remain pending. Existing debugger/WAV limitations and per-model/ROM/OS verification limits remain.
Do not attach ROMs or private data to public issue reports.

See the [documentation index](https://github.com/digihori/pokecom-go-plus/blob/v1.0.0/docs/README.en.md)
and the [release checklist](https://github.com/digihori/pokecom-go-plus/blob/v1.0.0/docs/releases/checklists/RELEASE_CHECKLIST_1.0.0.md).
