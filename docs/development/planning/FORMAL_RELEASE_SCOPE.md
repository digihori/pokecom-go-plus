# 正式版のリリース範囲・版数

2026-10-23。M1の確認用資料です。現在の配布設定・対応機種を基準に準備します。
Studio／Androidの公開版数は1.0.0に確定しました。
ローカルのアプリ設定を1.0.0へ更新しました。ストア・公開済みReleaseはまだ変更していません。

## 対象と配布

| 製品 | 対象 | 正式版の配布案 |
| --- | --- | --- |
| Studio | macOS Apple Silicon | GitHub Release：DMG／ZIP |
| Studio | Windows x64 | GitHub Release：MSI／ZIP |
| Studio | Linux x86_64 | GitHub Release：DEB／tar.gz |
| Player | Android 8.0以上（minSdk 26、現在のtargetSdk 36） | Google Play：既存アプリの更新 |

Studioの3環境・配布形式は現在の配布workflowを継続します。
AndroidのAPI設定は現行コードから確認した値であり、ストア提出要件の最新確認はM9で行います。
GitHubのdebug署名APKは正式Releaseに添付せず、CI成果物のみ残します（M5で確定）。
macOSは承認付きCIによるDeveloper ID署名・公証と配布版の受入れを完了しました（2026-10-28）。Windowsは当面未署名で配布し、OSS無料署名を後続で検討します。
iOSの配布・音声改善は別スレッドで進め、Studio／Androidの公開を待たせる条件にはしません。

Desktopの最低OSバージョン・Linuxの確認ディストリビューションは、
既存資料に正式版の保証範囲として確定した一覧がないため、候補版のM3で実際の確認環境とともに記録します。
ソースビルドに必要なJDK 21と、ランタイム同梱配布物の利用条件は区別します。

## 版数

| 対象 | 公開済み0.7 | 正式版候補 | 更新上の注意 |
| --- | --- | --- | --- |
| Studio公開版／runtime／Git tag | `0.7.0-alpha.1` | `1.0.0`／`v1.0.0`（確定） | 定数・テスト・公開workflowを一括更新済み |
| Studioの内部packageVersion | `1.5.0` | `1.6.0` | 公開版数とは別管理。MSIをダウングレードさせず、既存UpgradeCodeを維持 |
| Android versionName | `0.7.0-alpha.1` | `1.0.0`（確定） | Studioと同じ公開版数 |
| Android versionCode | `9` | `10`（確定） | Play提出済みの最大9を確認して増加 |

2026-10-23にStudio／Androidとも1.0.0で進める方針を確定しました。
内部packageVersionは更新互換性を保つため1.6.0に更新しました。公開日は未定です。
release workflowは1.0のタイトル・ノート参照へ更新し、`--prerelease`を外しました。
macOSのEnvironment Secrets設定とhosted runnerでの署名・公証を確認済みです（2026-10-28）。

## 対応機種・機能

Studio／Androidとも現行の14モデルを対象とします。
PC-1245、PC-1250／1251／1255、PC-1260／1261、PC-1350、PC-1360／1360K、
PC-1401／1402、PC-1450、PC-1460、PC-1470Uです。

機能は0.7のROM管理、BASIC／DMP入出力、Project／Assembler、Debugger、WAV Tool、
Character Editor、承認付きAI連携の現在の実装範囲を基準にします。
Studioの制作・解析機能と、Playerの実行用機能はそれぞれの現行ガイドに従います。
ROM・ゲーム・外部Assembler・AIサービスは同梱しません。

既知問題の修正対象は[残タスク一覧](FORMAL_RELEASE_TASKS.md)に従います。
1245／125xのRAM配置・ミラー処理は修正済みで自動検証が成功しています。
MEM表示はペンディングです。BASIC入出力等の手動受入れはM3に含めます。
新機種・セーブステート・追加Debugger機能・新しい対応CPU／OSは今回の必須範囲に追加しません。

## 後続の作業

1. M1：公開版数1.0.0を確定。OS別の詳細受入れ環境はM3で具体化。
2. M5：署名・公証とGitHub APKの配布方針を決定。
3. M6・M7：公開文書の語り口・現行説明・同梱案内を修正。
4. M8：権利・プライバシー・問い合わせ案内を確認。
5. M3・M4・M9：正式版候補を生成し、主要操作・更新互換性・CI・配布物を確認して公開。

最終候補で行う確認を、文書修正前の古い配布物で完了扱いにはしません。
