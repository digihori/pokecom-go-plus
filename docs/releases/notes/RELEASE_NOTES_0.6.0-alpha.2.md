# Pokecom GO Technical Preview 0.6 (alpha 2)

文書区分：記載されたGitタグのリリースノート。現在の開発版の機能一覧ではありません。

Git tag: `v0.6.0-alpha.2`

Technical Preview 0.6のバージョン表示を修正した早期評価版です。

## 修正内容

- alpha 1のStudio情報画面とMCPサーバー情報に、`0.5.0-alpha.1`と表示される更新漏れがありました。
- 共通の表示用バージョンを`0.6.0-alpha.2`に修正しました。
- ビルドのバージョンと実行時の表示用定数が一致することを検証する自動テストを追加しました。

PC-1360Kの漢字対応・吸い出しツール、Android Playerなどの機能は
[Technical Preview 0.6のリリースノート](https://github.com/digihori/pokecom-go-plus/blob/v0.6.0-alpha.2/docs/RELEASE_NOTES_0.6.0-alpha.1.md)
を参照してください。alpha 1のMac版の動作はユーザー確認済みです。
修正版の最終配布物に対するWindows／Linuxのユーザー確認は、別途です。

## 配布物

- Studio：macOS Apple Silicon DMG／ZIP、Windows x64 MSI／ZIP、Linux x86_64 DEB／tar.gz
- Android Player：Android 8.0以降向けdebug署名APK
- 4つのプラットフォーム別SHA-256一覧

ROMは同梱していません。iOSの配布物はありません。
Studioの内部パッケージ版は`1.4.1`、Windows MSIのUpgradeCodeは維持しています。
Android versionCodeは7です。署名・公証を行わないプレビュー配布で、Androidはdebug署名を使用します。
更新前にプログラム等をバックアップしてください。
