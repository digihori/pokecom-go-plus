# Android Playerのリリース最適化

2026-11-02、Google PlayのDEX最適化指摘への対応として、Android Playerの
releaseビルドでR8によるコード縮小・難読化・最適化とリソース縮小を有効にした。
設定箇所は`androidApp/build.gradle.kts`の`buildTypes.release`。

- `isMinifyEnabled = true`
- `isShrinkResources = true`
- `proguard-android-optimize.txt`を使用する。

debugビルドは従来の設定を使用する。独自の一括keepや最適化無効化は追加していない。
ライブラリのconsumer rulesとAndroid標準ルールを利用する。

AGP 8.11.0同梱R8はKotlin 2.4のメタデータ解析で警告を出すため、
`settings.gradle.kts`のpluginManagementでR8 9.1.57を明示する。
Kotlin 2.4の最低要件R8 9.1.29を満たす安定版で、AGPは変更しない。
[公式互換性表](https://developer.android.com/build/kotlin-support)と
[R8公式の差替え手順](https://r8.googlesource.com/r8.git/#replacing-r8-in-android-gradle-plugin)を参照。

## リリース確認

JDK 21で以下を実行する。

```sh
./gradlew :androidApp:bundleRelease :androidApp:assembleRelease :androidApp:testReleaseUnitTest
```

生成物は以下。署名・ストアへのアップロードは別途行う。

- AAB：`androidApp/build/outputs/bundle/release/androidApp-release.aab`
- 未署名APK：`androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk`
- 難読化対応表：`androidApp/build/outputs/mapping/release/mapping.txt`

クラッシュ解析用に、配布する版と一致するmapping.txtを保管する。
リリースAPKを署名した端末確認では、ROM取込み・起動・設定保存／復元・
BASIC／マシン語のロード／セーブを確認する。
ユニットテストは難読化後のAPKの実機動作を保証するものではない。
Google Playの指摘解消は、次のAABをアップロード後にConsoleで確認する。

参考：[Android公式：R8によるアプリ最適化](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)

## 検証記録（2026-11-02）

R8 9.1.57でrelease APK/AABとmapping.txtを生成し、アプリ由来のクラス名が
短縮されていること、AABに`BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`
が含まれることを確認した。

`build :core:desktopTest :androidApp:bundleRelease :androidApp:assembleRelease
:androidApp:testReleaseUnitTest`が成功（JDK 21）。

旧R8のKotlinメタデータ解析警告は解消。AGP側ClassFileResourceProviderの
非同期解析非対応警告と、既存Android lintのKotlinメタデータ互換性診断は残る。
ユーザーより、ローカルのrelease APKで実機動作確認OKとの報告を受領した。
Google Play Consoleの判定確認は未実施。
