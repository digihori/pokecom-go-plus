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

## 現在の範囲

この手順はローカル署名用です。GitHub Actionsへの証明書導入・公証認証・署名強制は別途実装します。
CIで生成した従来の未署名成果物を、ローカル署名済みの結果と混同しないようにします。

参照：
[JetBrainsの署名設定](https://github.com/JetBrains/compose-multiplatform/blob/master/tutorials/Signing_and_notarization_on_macOS/README.md)、
[Appleの公証手順](https://developer.apple.com/documentation/security/customizing-the-notarization-workflow)。

## ローカル確認結果（2026-10-23）

Developer ID署名付きのアプリ・DMG生成に成功しました。
`codesign --verify --deep --strict`でアプリを、`codesign --verify --strict`で最終DMGを検証し、
Developer ID→Developer ID Certification Authority→Apple Root CAのチェーンと安全なタイムスタンプを確認しました。
アプリにはHardened Runtimeが有効です。JDK 21の`./gradlew build`も成功しました。
公証・staple・Gatekeeperの受入れはまだ完了していません。
現在の署名試験用成果物は0.7／内部版1.5.0であり、正式版1.0.0の最終候補ではありません。

## 公証申請（2026-10-23）

ローカルの`pgp-notary`プロファイルへ認証を登録し、試験用の署名済みアプリZIPとDMGを提出しました。
両方のアップロードは成功しました。最終審査・staple・Gatekeeper検証は確認中です。

- アプリ提出ID：`47dd555f-e8e3-4c17-b6c2-4c53f0a36d8e`
- DMG提出ID：`f385b3e9-acca-4645-996e-b574c842ba14`

提出・状態のJSONは`desktopApp/build/notarization/`へ保存します。
認証情報や個人のアカウント識別子はこの記録へ含めません。

## 1.0.0のCI署名・公証

release workflowに署名・公証処理を追加しました。以下のGitHub Actions Secretsが必要です。
まだSecretsの登録・hosted runnerでの試験は行っていません。

| Secret | 内容 |
| --- | --- |
| MACOS_CERTIFICATE_P12_BASE64 | Developer ID Application証明書と秘密鍵を含む、パスワード付きp12のBase64 |
| MACOS_CERTIFICATE_PASSWORD | p12の書出し時パスワード |
| MACOS_SIGNING_IDENTITY | 証明書名のDeveloper ID Application: より後の部分 |
| MACOS_NOTARY_APPLE_ID | 公証に使うApple Account |
| MACOS_NOTARY_TEAM_ID | 証明書と同じDeveloper Team ID |
| MACOS_NOTARY_PASSWORD | 公証用のアプリ用パスワード |

証明書を書き出す場合はキーチェーンアクセスの「自分の証明書」で、対象Developer ID証明書と
対応する秘密鍵の組をp12形式で書き出します。ほかの証明書や秘密鍵は含めません。
書き出すp12はリポジトリ外に置き、パスワードとともにチャットへ送らないでください。
Base64は暗号化ではありません。Secretsへの登録後も通常の文書やログへ貼り付けません。

CIでは使い捨てのキーチェーンへ取り込み、公証認証も同じキーチェーンへ保存し、終了時に削除します。
Secretsが不足する場合は失敗し、未署名配布へ自動で切り替えません。
アプリを署名・公証・stapleした後、createDistributableを再実行せずDMGを生成して署名・公証します。
最終ZIPはstaple済みアプリから生成します。全検証の成功後にchecksumを生成します。

`python3 scripts/notarize-macos.py TARGET --profile pgp-notary`はローカルでも使えます。
Accepted以外の状態・タイムアウト・ad-hoc署名ではstapleへ進みません。
初回のApple審査がタイムアウトを超えた場合は提出IDを確認し、同じ内容の再提出を繰り返さないでください。

0.7の試験提出はアプリ／DMGともAccepted、staple／validateとGatekeeperの
`source=Notarized Developer ID`判定を確認しました。1.0候補は別途検証します。

1.0.0候補の提出ID：アプリ`a453c608-1e67-41ec-9dbd-fe3e44e44210`、
DMG`178f2fca-9677-4cfc-afc7-287b67c7264e`。待機時間内に完了しない場合も再提出せず、
`--submission-id ID`で同じ申請の待機・検証を再開できます。

1.0候補もアプリ・DMGともAcceptedとなり、staple／validateとGatekeeperの
`source=Notarized Developer ID`判定に成功しました。上記提出IDで確認済みです。
