# GitHub Supportへの問い合わせ案（2026-10-27）

以下の英語の件名・本文を問い合わせフォームに転記します。
Chromeのバージョンだけ、`chrome://version`で確認して記入してください。
本書は問い合わせ案です。送信はまだ行っていません。

## Subject

Chrome Safe Browsing blocks a GitHub Actions macOS artifact download as DANGEROUS

## Message

Hello GitHub Support,

I maintain the open-source project Pokecom GO Studio and am preparing its 1.0.0 release. Google Chrome is blocking the macOS artifact ZIP downloaded from GitHub Actions with the warning “This file is dangerous.” The issue was observed on October 24, 2026, and is still reproducible as of October 27, 2026.

I understand that Google Safe Browsing makes this verdict and that GitHub may not be able to change it directly. I would appreciate help determining whether this is a known issue involving Actions artifact downloads, and guidance on an appropriate investigation or escalation route for software hosted on GitHub.

### Affected artifact

Repository:
https://github.com/digihori/pokecom-go-plus

Workflow run:
https://github.com/digihori/pokecom-go-plus/actions/runs/40045502291

Artifact download page:
https://github.com/digihori/pokecom-go-plus/actions/runs/40045502291/artifacts/12541158297

- Artifact name: pokecom-go-studio-macos
- Downloaded filename: pokecom-go-studio-macos.zip (Chrome may append a duplicate-file suffix)
- Artifact ID: 12541158297
- Commit: d4babd6170cb9bf8f4adf39177fff6ddd57d1103
- Size: 156706277 bytes
- SHA-256: 8981d06986b65fb70501b56a4944cce7ec6f380a94effb9300dda33300abfc1e

This SHA-256 was independently verified on an artifact retrieved through the GitHub API and matches the artifact digest returned by GitHub. The browser-blocked local copy has not been independently hashed.

### Environment and reproduction

- macOS: 26.6.2, Apple Silicon
- Chrome version: [INSERT VERSION FROM chrome://version]

1. Sign in to GitHub in Chrome using an account that can download the artifact.
2. Open the workflow run above.
3. Download the pokecom-go-studio-macos artifact from the Artifacts section.
4. Chrome blocks the ZIP and displays “This file is dangerous.”

The Safe Browsing diagnostic page was opened before repeating the download. It showed:

- Download URLs checked: result = SAFE
- Download responses (ClientDownloadResponse) received: verdict = DANGEROUS
- Download TailoredVerdict override: No override set

These diagnostics identify the download-protection verdict but do not identify a specific malicious component or detection rule. I have not overridden the verdict or disabled Safe Browsing.

### Verification already performed

The artifact ZIP contains only:

- PokecomGOStudio-1.0.0-macos-arm64.dmg
- PokecomGOStudio-1.0.0-macos-arm64.zip
- SHA256SUMS-macos.txt

The application is a Kotlin/JVM desktop application with a bundled Java runtime. The application and DMG are signed with an Apple Developer ID Application identity and notarized by Apple.

Checks on the retrieved artifacts succeeded:

- Application: codesign --verify --deep --strict
- DMG: codesign --verify --strict
- Application and DMG: xcrun stapler validate
- Gatekeeper assessment for both: accepted, source=Notarized Developer ID
- The included SHA-256 checksums match the DMG and application ZIP.
- Within this candidate, the application in the DMG and the application in the ZIP have identical file contents and symbolic-link targets.

I also compared this candidate with the earlier artifact from:
https://github.com/digihori/pokecom-go-plus/actions/runs/40042209096

The source changes were the macOS application category, a distinct DMG volume icon, an icon-generation script, and related documentation. There were no changes to application logic, dependency declarations, or the release workflow. Both runs used the same JDK version and the same resolved commits of the GitHub Actions used by the macOS job.

The comparison found no unexpected added payloads. JAR member contents were identical except for signatures on embedded native libraries. For the 33 differing Mach-O files inspected, the bytes preceding the code-signature region were identical. The remaining differences involved application category metadata, signature/notarization data, archive metadata, and references to renamed JARs.

These checks do not establish that the software is free of malware, and I am not claiming a confirmed false positive. They have not explained Chrome’s DANGEROUS verdict.

### Assistance requested

1. Is there a known issue with Chrome Safe Browsing classifying GitHub Actions artifact ZIPs or their download delivery URLs as dangerous?
2. Can GitHub investigate any artifact-specific or delivery-related issue for the artifact ID and digest above?
3. Since I do not control github.com or the artifact delivery domain, what is the appropriate route to request a Safe Browsing review? If this falls outside GitHub Support’s scope, could you direct me to the relevant team or process?

I submitted a report through Chrome’s “Report an issue” feature, but have not received a response and the download remains blocked. I am seeking investigation and guidance rather than instructions to bypass browser protections.

Thank you for your assistance.

## 添付候補・送信前の確認

- Chromeの「このファイルは危険です」の画面。
- `chrome://safe-browsing`のURL判定SAFEとダウンロード判定DANGEROUSの画面。
- 必要に応じて、[成果物比較記録](MACOS_ARTIFACT_COMPARISON_2026-10-24.md)。
- スクリーンショットのtoken、一時的な署名付きURL、無関係な個人情報は隠します。
- 証明書の秘密鍵、p12、公証パスワード、GitHubの認証情報は添付しません。
- Actions artifactの保持期間は14日です。アクセス不能になった場合はその旨を補足します。別の成果物を同一ハッシュのものとして案内しません。

## 問い合わせの要旨

GitHub Actionsから配布しているmacOS候補ZIPがChromeでブロックされています。
URLチェックはSAFEですが、ダウンロード保護のサーバー応答はDANGEROUSです。
署名・公証と構成比較では原因が分からなかったため、GitHub側で確認可能な配信上の問題・既知事例と、
GitHubのドメインを所有していない開発者が使える調査・再評価の窓口を問い合わせます。
