# macOS配布候補の構成比較（2026-10-24）

Chromeが今回のActions成果物ZIPを「このファイルは危険です」と表示したため、
前回候補と今回候補の生成経路・ファイル構成・内容・署名を比較しました。
アプリは起動せず、DMGは読み取り専用で検査して取り出しました。

## 比較対象と同一性

| 項目 | 前回 | 今回 |
| --- | --- | --- |
| Actions run | [40042209096](https://github.com/digihori/pokecom-go-plus/actions/runs/40042209096) | [40045502291](https://github.com/digihori/pokecom-go-plus/actions/runs/40045502291) |
| コミット | `1e5354b47c3504d239a261202383a0f5f8150a66` | `d4babd6170cb9bf8f4adf39177fff6ddd57d1103` |
| Artifact ID | `12539524974` | `12541158297` |
| 外側ZIPのサイズ（bytes） | 158144719 | 156706277 |
| 外側ZIPのSHA-256 | `249ed8b86feac36cf721db49b7df8e34a17362f92ba30d79437269b54db0329c` | `8981d06986b65fb70501b56a4944cce7ec6f380a94effb9300dda33300abfc1e` |

検査した外側ZIPは、それぞれGitHub APIのartifact digestと一致しました。
内部のDMGとアプリ配布ZIPは、同梱SHA256SUMS-macos.txtの値と一致しました。
利用者のChromeがブロックしたローカルファイル自体とのハッシュ照合は未実施です。

## 生成経路

コミット差分はmacOSカテゴリ指定、DMG専用アイコン、アイコン生成用Swiftソース、関連文書です。
アプリ処理コード、依存関係宣言、release workflow、DMG加工スクリプトに変更はありません。
アイコン生成用Swiftソースは配布アプリに含まれていません。
両runともmacos-26-arm64、Temurin 21.0.12+101.0を使用しています。
checkout、setup-java、setup-gradle、upload-artifactの実行されたActionのコミットSHAも一致しました。

## ファイル構成と内容

- 外側ZIPは両方ともDMG・アプリ配布ZIP・checksumの3ファイルです。格納順には差があります。
- アプリ配布ZIPは両方とも209エントリ、ディレクトリを除く179ファイルです。
- JAR名のハッシュ部分が変わった2件を除き、ファイルの追加・削除はありません。
- 共通エントリのUNIXモードと圧縮方式に差はありません。
- 58個のJARを展開内容で比較しました。55個は全ファイル内容が一致しました。
- 残る3個のJARは内包するネイティブライブラリだけに差があり、Mach-Oの署名領域より前のバイト列は一致しました。
- アプリランチャー、Javaランタイム、直接配置されたライブラリとJAR内の差分を合わせ、33件のMach-Oを比較しました。署名領域より前のバイト列はすべて一致しました。
- Info.plistの意味上の差はLSApplicationCategoryTypeのUnknownからpublic.app-category.developer-toolsへの変更だけです。
- 起動設定の差は上記2個のJAR名の参照変更だけです。
- その他の差は署名リソース、公証証票、JARの格納情報です。

両DMGの最上位構成は、アプリ、Applicationsリンク、.DS_Store、.VolumeIcon.icns、
.background、.fseventsdで一致しています。ボリュームアイコンのハッシュは変更されています。
各候補のDMG内アプリと、その候補の配布ZIP内アプリは、全ファイルのSHA-256とシンボリックリンク先が一致しました。

## 署名・公証

- 両候補のアプリ：codesign --verify --deep --strictが成功。
- 両候補のDMG：codesign --verify --strictが成功。
- 両候補のアプリ／DMG：stapler validateが成功。
- 今回のアプリ／DMG：Gatekeeperがaccepted、source=Notarized Developer IDと判定。

## 結論と限界

構成比較で不審な追加物や、今回新しく変更された実行コードは見つかりませんでした。
ただし、同一の既存コードの安全性を保証する検査ではありません。
Apple公証とGoogle Safe Browsingの判定は別であり、Chromeの危険判定の原因や誤検知は確定できません。
追加調査にはChrome側の判定情報やGoogleによる再評価が必要です。

## GitHub Releases配信との比較準備（2026-10-27）

[ダウンロード判定テスト用プレリリース](https://github.com/digihori/pokecom-go-plus/releases/tag/download-test-1.0.0-20261027)を公開しました。
正式版ではなく、Latestにも設定していません。タグは元候補のコミットを対象とします。
同じ外側ZIPを`pokecom-go-studio-macos.zip`として添付しました。
Releases APIに記録されたサイズ156706277 bytesとSHA-256
`8981d06986b65fb70501b56a4944cce7ec6f380a94effb9300dda33300abfc1e`は元のActions成果物と一致しました。
再ビルド・再署名・再圧縮はしていません。外側ZIP用checksumも添付しています。
同日、Chromeの保護設定を変更せず、Releasesからはブロックされずダウンロードできました。
同じSHA-256のZIPでも、ActionsでのDANGEROUS判定とReleasesでのダウンロード可という差を確認しました。
配信URL・経路に関連する情報、判定時刻等の違いが関係する可能性を示しますが、Googleの内部判定理由は確定できません。
署名やZIPの内容だけが原因だとは扱いません。安全性や、別の配布ファイル・将来の判定を保証する結果でもありません。
Googleからの回答だけを公開準備の再開条件にはせず、正式Releaseの最終資産についてもChromeでダウンロードを確認します。
