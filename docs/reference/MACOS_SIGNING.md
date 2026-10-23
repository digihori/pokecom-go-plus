# macOSの署名・公証（ローカル手順）

Developer ID Application証明書と対応する秘密鍵をログインキーチェーンへ取り込み、
証明書が有効であることを確認してから進めます。通常の開発ビルドは署名を要求しません。

## 署名付きビルド

JDK 21を使用し、リポジトリルートで実行します。
`PGP_MAC_SIGNING_IDENTITY`は証明書名の`Developer ID Application: `より後の部分です。
個人の証明書名やTeam IDは共有設定へ書き込みません。

```sh
export PGP_MAC_SIGNING_IDENTITY='YOUR NAME (TEAM_ID)'
./gradlew :desktopApp:createDistributable :desktopApp:packageDmg
```

Composeの標準署名処理と標準Java用entitlementsを使用します。
同梱ランタイムも署名対象です。Bundle IDは既存の`com.digihori.pgp.desktop`を維持します。
DMGのボリュームアイコン加工後に最終DMGを署名・検証します。
キーチェーンがcodesignの秘密鍵利用の許可を求めた場合は、署名端末で許可します。

## 公証用の認証情報（初回のみ）

Apple Accountでアプリ用パスワードを作成し、ターミナルで次を実行します。
Apple Accountの通常のパスワードとは別です。

```sh
xcrun notarytool store-credentials 'pgp-notary'   --apple-id 'YOUR_APPLE_ACCOUNT_EMAIL'   --team-id 'YOUR_TEAM_ID'
```

パスワードは対話入力します。コマンドの引数・Git・チャットへ書き込みません。
認証情報はキーチェーンへ保存されます。APIキーを使用する認証も選択できます。

## アプリとDMGの公証・最終ZIP

以下は現在の内部パッケージ版1.6.0の例です。正式版候補で内部版数を更新した場合はDMG名も変更します。
同梱ROM・ユーザーデータがないビルド成果物だけをAppleへ提出します。

```sh
app='desktopApp/build/compose/binaries/main/app/PokecomGOStudio.app'
dmg='desktopApp/build/compose/binaries/main/dmg/PokecomGOStudio-1.6.0.dmg'
mkdir -p desktopApp/build/notarization
codesign --verify --deep --strict --verbose=2 "$app"
codesign --verify --strict --verbose=2 "$dmg"
ditto -c -k --keepParent "$app" desktopApp/build/notarization/submit.zip
xcrun notarytool submit desktopApp/build/notarization/submit.zip   --keychain-profile pgp-notary --wait
```

必ず`Accepted`を確認してから次へ進みます。失敗時は提出IDを使って
`xcrun notarytool log`で理由を調べ、署名を修正して再提出します。

```sh
xcrun stapler staple "$app"
xcrun stapler validate "$app"
ditto -c -k --keepParent "$app" desktopApp/build/notarization/PokecomGOStudio-macos.zip
xcrun notarytool submit "$dmg" --keychain-profile pgp-notary --wait
```

DMGも`Accepted`を確認後、次を実行します。

```sh
xcrun stapler staple "$dmg"
xcrun stapler validate "$dmg"
codesign --verify --deep --strict --verbose=2 "$app"
codesign --verify --strict --verbose=2 "$dmg"
spctl --assess --type execute --verbose=2 "$app"
spctl --assess --type open --context context:primary-signature --verbose=2 "$dmg"
shasum -a 256 "$dmg" desktopApp/build/notarization/PokecomGOStudio-macos.zip
```

ZIP自体にはstapleしません。公証済みアプリへstapleした後に最終ZIPを作ります。
DMG内アプリも同じ署名済みコードであり、DMGの公証・stapleと併せて配布検証します。
署名・公証・staple後にチェックサムを生成します。その後DMGのアイコン等を加工しません。
最終配布物を展開・マウントし、Gatekeeper判定と隔離環境の起動を確認します。

## GitHub Actionsによる署名・公証

正式配布では`Build Release Packages`の専用ジョブ`macos-signing`を使用します。
GitHub Environment `macos-signing`の承認後にだけ署名用Secretsへアクセスします。
Windows／LinuxとAndroid検証用APKの生成は署名承認を待たず実行できます。
タグ実行では全ジョブの成功後、macOSを含むStudio成果物をドラフトReleaseへまとめます。
公開は資産と手動受入れを確認してから開発者が行います。
Androidの署名・ストア公開は開発者が別途担当します。

### Environmentの保護

- Required reviewers：リポジトリ所有者。単独運用のため、自分が開始した実行も承認できます。
- 管理者による保護ルールのスキップは禁止します。
- 実行元：`main`、準備用`codex/release-1.0.0`ブランチ、`v1.0.0`タグのみ。
- 次版のタグを使う際は許可ルールを更新します。準備完了後は準備用ブランチの許可を削除します。
- 署名ジョブのGITHUB_TOKENは`contents: read`のみ。Release作成ジョブだけ`contents: write`です。

承認前に実行元コミットとworkflowの変更を確認します。
Environmentの管理権限を持つ人は設定自体を変更できるため、承認設定は管理権限の代わりにはなりません。
SecretsはRepository Secretsではなく、このEnvironmentに登録します。

### Secretsの登録（初回・更新時）

GitHubのSettings → Environments → macos-signing → Environment secretsで次を登録します。
秘密鍵やパスワードはGit・チャット・通常のログへ貼り付けません。

| Secret | 内容 |
| --- | --- |
| MACOS_CERTIFICATE_P12_BASE64 | Developer ID Application証明書と対応する秘密鍵だけを含む、パスワード付きp12のBase64 |
| MACOS_CERTIFICATE_PASSWORD | p12を書き出す際に設定したパスワード |
| MACOS_SIGNING_IDENTITY | 証明書名の`Developer ID Application: `より後の部分（`YOUR NAME (TEAM_ID)`） |
| MACOS_NOTARY_APPLE_ID | 公証用Apple Accountのメールアドレス |
| MACOS_NOTARY_TEAM_ID | 証明書のDeveloper Team ID |
| MACOS_NOTARY_PASSWORD | 公証用のアプリ用パスワード。通常のApple Accountパスワードは使いません |

キーチェーンアクセスの「自分の証明書」で対象Developer ID証明書を選択し、
対応する秘密鍵を含むp12を書き出します。保存先はリポジトリ外にし、強いパスワードを設定します。
Base64は暗号化ではありません。以下は登録用データをクリップボードへコピーする例です。

```sh
base64 -i /path/outside/repository/developer-id.p12 | pbcopy
```

Secrets登録後はクリップボードを消去します。p12の保管・削除は自身の鍵管理方針に従います。
ローカルの`pgp-notary`プロファイルからパスワードを取り出す必要はありません。
必要ならApple AccountでCI専用のアプリ用パスワードを新規作成します。
CIは一時キーチェーンを作成し、ジョブ終了時に削除します。
Secrets不足・署名エラー・公証失敗時はジョブを失敗させ、未署名へ切り替えません。

### 各リリースの手順

1. 最終候補を確定し、手動のBuild Release Packagesで配布処理を確認します。
2. Actionsの実行画面でReview deploymentsを開き、`macos-signing`を承認します。
3. 自動の署名・公証・staple・Gatekeeper検証とchecksum生成の成功を確認します。
4. 最終タグの実行でも署名を承認し、生成されたドラフトReleaseの全資産を確認します。
5. 手動受入れと公開文面の確認後、ドラフトを公開します。

アプリを署名・公証・stapleした後、再生成せずDMGを作成・署名・公証します。
最終ZIPはstaple済みアプリから生成します。checksum生成後には資産を加工しません。
helperはAccepted以外・タイムアウト・ad-hoc署名では先へ進みません。
ローカルで申請を再開する場合は`--submission-id ID`を使用できます。
CIのタイムアウト時は提出IDを確認し、同じ成果物を繰り返し提出しないでください。

## ローカル候補の確認結果（2026-10-23）

1.0.0候補のアプリ・DMGともAccepted、staple／validate・署名検証・Gatekeeperの
`source=Notarized Developer ID`判定に成功しました。
最終コミットでの再生成・手動受入れ・公開前の資産照合は別途必要です。

参照：
[JetBrainsの署名設定](https://github.com/JetBrains/compose-multiplatform/blob/master/tutorials/Signing_and_notarization_on_macOS/README.md)、
[Appleの公証手順](https://developer.apple.com/documentation/security/customizing-the-notarization-workflow)。
