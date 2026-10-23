# 正式版の署名・配布方針（M5確認用）

2026-10-23。コードと0.7の公開記録を確認した資料です。
証明書・鍵・ストア設定は変更していません。

## 確定した方針（2026-10-23）

- macOS：Developer ID署名・公証を導入します。
- Windows：OSS無料署名を第一候補として検討します。利用が難しい場合は当面未署名で配布し、有料証明書の契約を必須にしません。[一次検討](WINDOWS_OSS_SIGNING.md)。
- Android：Google Play配布を継続。debug署名APKは正式Releaseに添付せず、CI成果物のみ残します。
- 公開jobのダウンロード対象を`pokecom-go-studio-*`へ限定しました。Androidのビルド・テスト・artifact保持と、公開jobのAndroid成功依存は維持します。
- 署名・公証の実装は、利用できる証明書・署名サービスと認証方式の確認後に進めます。M5は未完了です。

以下の現状・比較表は方針決定時の確認資料です。

## 現在の状態

- Studio：配布用証明書による署名・macOS公証を行わない配布です。macOSのad-hoc署名はDeveloper ID署名とは別です。
- DesktopのGradle／公開workflowに配布用の署名・公証設定はありません。
- Android：Google Playの署名付き配布と、GitHubのdebug署名APKを分けています。
- 公開workflowは`assembleDebug`を実行し、ファイル名にも`android-debug.apk`と明記します。ストアの署名鍵は扱いません。

## 方針決定時の比較

| 対象 | 案A：現行配布を継続 | 案B：正式版で署名を追加 |
| --- | --- | --- |
| macOS Studio | 未署名・未公証の扱いと導入時の警告をガイドへ明記 | Developer ID署名・公証を導入し、完成配布物で確認 |
| Windows Studio | 未署名の扱いと導入時の警告をガイドへ明記 | コード署名を導入し、完成MSI／アプリと更新で確認 |

正式版という版数だけで署名必須にはしません。新規署名を採用する場合は、
保有証明書・契約状況と鍵の運用を確認してから実装を決めます。
OS全体の保護設定を無効にする導入手順は案内しません。

Androidの一般利用者向け配布はGoogle Playを継続する案です。
GitHubのdebug署名APKについては、次のどちらかを選びます。

- 現行どおり検証用として公開：ストア版との署名差・更新互換性を明記。
- 正式Releaseの添付から除外：必要な検証用APKはworkflow artifactで提供。公開jobの依存とchecksum収集も整合させる。

決定内容は[範囲・版数](FORMAL_RELEASE_SCOPE.md)へ反映しました。
M7のガイドとM9のworkflowは、署名済み配布物の検証結果に合わせて仕上げます。

## 署名実装前に確認する情報

- macOS：Apple Developer Programの利用可否、Developer ID Application証明書と秘密鍵の保有状況、公証の認証方式。
- Windows：コード署名証明書／クラウド署名サービスの保有・契約状況、CIまたは署名端末での利用方法。
- 証明書の新規契約・購入は別途判断します。秘密鍵・パスワードはチャットやGitへ記載せず、実装時に安全な保管先を決めます。
- 署名後の検証・macOSの公証とstaple・最終成果物からのチェックサム生成を公開手順へ組み込みます。


## 証明書・サービスの準備状況（2026-10-23）

Apple Developer Programへ登録済みです。Windowsは署名サービスの登録・契約がありません。
Developer ID Application（G2）証明書を作成・ログインキーチェーンへ取り込み済みです。
AppleのDeveloper ID G2中間証明書の取り込み後に有効表示を確認し、通常のキーチェーン照会でも有効な署名identityを確認しました。
サンドボックス内の照会では0件になるため、証明書未発行とは扱いません。

macOSはAccount HolderとしてDeveloper ID Application証明書を作成し、秘密鍵と対応する証明書を
署名端末で利用できる状態にしました。iOS用のDevelopment／Distribution証明書とは区別します。
その後、公証用の認証を設定して署名・公証の試験に進みます。
[Apple公式の証明書作成手順](https://developer.apple.com/help/account/certificates/create-developer-id-certificates/)。

Windowsの新規署名手段は未確定です。
[Microsoftの利用条件](https://learn.microsoft.com/en-us/azure/artifact-signing/quickstart)では、
Artifact SigningのPublic Trustは日本の組織を対象に含みますが、個人開発者は米国・カナダに限定されています。
日本の個人開発者として契約する場合は、そのまま利用できるとは扱いません。
[Windows署名方式の公式比較](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/code-signing-options)を基に、
契約主体に適合する認証局・サービスの費用と利用条件を確認します。
Microsoft StoreによるMSIXの再署名は別の配布方式であり、GitHubのMSI／ZIPへの署名を代替しません。
署名してもSmartScreen警告が直ちに解消するとは保証しません。

Windowsの有償契約、鍵のエクスポート、CIへの秘密情報登録は未実施です。

## ローカル署名の設定

[macOS署名・公証手順](../../reference/MACOS_SIGNING.md)を追加しました。
環境変数による署名の有効化、既存Bundle IDの明示、アイコン加工後の最終DMG署名を設定しました。
証明書名・Team ID・秘密情報は共有ビルド設定へ固定しません。
公証の認証情報登録と、試験用アプリ・DMGのAppleへのアップロードは完了しました。
0.7署名試験とローカル1.0候補のAccepted確認・staple・Gatekeeper検証が成功しました。
macOSは承認付きEnvironment Secretsを使い、GitHub Actionsで署名・公証します。
署名Environmentは所有者の承認必須・管理者のスキップ禁止・実行元の制限を設定しました。
Secrets登録とhosted runnerでの検証は未完了です。全OSの成果物はドラフトReleaseへまとめ、確認後に公開します。
