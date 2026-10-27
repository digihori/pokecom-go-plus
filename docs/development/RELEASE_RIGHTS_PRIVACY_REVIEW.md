# 正式版の権利・プライバシー・問い合わせ先確認（M8）

2026-10-23。比較基準は`v0.7.0-alpha.1`、対象は現在の変更ファイルと未追跡の追加文書です。
Git全履歴の再監査や、最終1.0配布物の受入れとは区別します。

## 権利・資産・秘密情報

- 確認時の変更・未追跡56ファイルはテキストのみ。ROMや実機データ、鍵等のバイナリー追加なし。
- 代表的な秘密鍵ヘッダー・GitHub／AWS／Google等のトークン形式を差分対象ファイルで検査し、該当なし。値はログへ出力していない。
- 0.7から依存バージョンカタログ・各モジュールの依存宣言は変更なし。Desktopビルドの変更は署名設定であり、新しい依存の導入なし。
- `LICENSE`はMIT。第三者通知に参照元・Apache 2.0／BSD／MIT／zlib等の通知を維持。
- ローカル配布アプリに現在の第三者通知とruntimeのlegalディレクトリを確認。
- Android debug／release APKのヘルプ・紹介・ポリシー・同梱ライセンス4ファイルをソースとbyte単位で照合。
- Appleの公証用APIキー等を誤って追加しないよう、`.p8`／`.key`／`.pfx`／provisioning profileをGit除外へ追加。既存の`.p12`／`.pem`等の除外は維持。

## Androidのデータ・権限

| 説明対象 | 確認した実装 |
| --- | --- |
| ROMと登録情報 | `AndroidRomLibrary`がfilesDir内にROM・元名・時刻・サイズ・hash等を保存 |
| 選択機種・ホーム設定 | `player-home` preferencesにシリーズ内モデルとタイル／リストを保存 |
| 入力・音声設定 | gamepad／playback preferencesに割当・サウンド・速度を保存 |
| 読込み・保存 | OpenDocument／CreateDocumentとContentResolver。任意の全ファイルアクセス権限なし |
| 外部リンク | ブラウザー等へGitHub／公開セットアップページを開く。Player自身のネットワークデータ取得ではない |
| 実行状態 | RAM・CPUのプロセス終了越しのセーブステートなし |
| 統合Manifest | debug／releaseともINTERNET・マイク・広範なストレージ権限なし。AndroidX由来のアプリ固有signature permissionのみ |
| バックアップ | allowBackup=false、fullBackupContent=false。ROM Libraryのcloud-backup／device-transfer除外を明示 |

## 修正した点

1. ポリシーにホームのタイル／リストとシリーズ内選択機種の保存を補足。
   日本語原文・公開用日本語HTML・英語HTMLを同時修正。適用開始日は2026-10-22を維持し、最終更新日を2026-10-23へ更新。
2. ROM Libraryは`Context.filesDir/rom-library`へ保存するため、data extraction rulesのdomainを`root`から`file`へ訂正。
   cloud-backupとdevice-transferの両方を修正し、debug／releaseの完成APKをaapt2で開いて設定を確認。
   全メーカーの実際の端末移行を確認した結果ではない旨はポリシーに維持。

[Android公式のバックアップ区分と設定](https://developer.android.com/identity/data/autobackup)を基準としています。

## 公開導線・問い合わせ

- 公開中の日英HTMLを取得し、0.7タグの内容と一致することを確認。今回の補足はローカル修正で、まだ公開反映していない。
- Google Playの公開ページがプロジェクトの日本語ポリシーURLを参照していることを確認。
- Play公開ページのサポート用メールの掲載も確認。メールアドレスは共有資料へ転記しない。
- GitHubの公開APIでリポジトリがPublic・Issues有効であることを確認。問い合わせは公開窓口であり、ROM・個人情報・秘密情報を投稿しない案内を維持。
- Issues投稿・メール送信・Play Consoleの設定変更は行っていない。

## 検証とM9への引継ぎ

JDK 21の`./gradlew --gradle-user-home .gradle-local build`が成功しました。
APK内の更新済みポリシーとライセンス、コンパイル済みバックアップ除外設定を確認しました。

正式版公開時に次を確認します。

- ポリシーHTMLの日英公開反映と、アプリ同梱原文との一致。
- Play Consoleの非公開設定（データセーフティ回答・問い合わせ情報等）と、最終提出AABの権限・挙動の照合。
- 最終候補コミット／配布物について、ROM・秘密情報の混入とLICENSE／通知・チェックサムを再確認。
- 変更したバックアップ除外について、端末移行の実測が必要な環境は個別に記録。

iOSの審査・配布・プライバシー確定は別スレッドで進めます。

## 公開準備の再確認（2026-10-28）

- 今回の公開資料・workflow・独自Assemblyサンプル・テストの追加差分26ファイルを検査し、
  代表的な秘密鍵／トークン形式・個人の絶対パス・ROMファイルの混入は検出しませんでした。全履歴の再監査ではありません。
- Assemblyサンプルの合成ROMはテスト内で作った短い命令列で、実ROMの内容は含みません。
  PC-1350の図形・字形・旋律も独自作成で、元ゲームの画像やコードをコピーしていません。
- 追加の外部依存はありません。単独変換ツールの説明書へ既存MIT Licenseの全文を追加しました。
- Androidの公開中ポリシーは日英とも2026-10-22版でした。候補にあるホーム表示形式・シリーズ内選択機種の
  端末内保存の説明が未反映なので、main取り込み後にPagesを確認します。Play Consoleへの提出は開発者が担当します。
