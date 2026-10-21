# Technical Preview 0.6 release checklist

文書区分：対象リリース当時の公開作業記録（2026-10-22整理）。
チェック状態・未確認項目は当時の記録です。次回プレリリースの公開判定には別のチェックリストを用意します。

- Release/tag: `Pokecom GO Technical Preview 0.6` / `v0.6.0-alpha.1`
- Project/Android version: `0.6.0-alpha.1`; Android versionCode: `6`
- Studio native packageVersion: `1.4.0`; published Windows UpgradeCode retained.
- Scope: desktop Studio and Android Player packages. No iOS distribution/CI.

## Public-content audit

- [x] Tracked text checked for personal local paths, private machine identifiers, private email and credential patterns.
- [x] Public author/copyright and third-party license contacts retained; example paths use placeholders.
- [x] Both tracked documentation screenshots visually inspected; no personal information visible.
- [x] No tracked ROM images, user recordings, pgrom packages or local-data files.
- [x] PC-1360K page-0 hardware result recorded without personal paths or firmware data.
- [x] Release notes distinguish hardware, emulator, automated tests and unresolved iOS CI behavior.

## Release gates

- [x] Versioned candidate local JDK 21 full build and Core tests.
- [x] Synthetic decoder tests and macOS createDistributable.
- [x] Candidate pushed; macOS, Windows, Linux and Android CI all successful.
- [x] New annotated tag pushed after candidate checks.
- [x] Tagged packaging and prerelease publication successful.
- [x] Seven packages and four checksum lists verified against asset digest metadata.
- [x] Publication result recorded.

Release authorization includes commit/push, the new tag and prerelease publication.
Existing tags/history are not rewritten. End-user installation of the final assets is not claimed.

## Publication result

Published as a prerelease on 2026-10-18 at 18:07 JST:
https://github.com/digihori/pokecom-go-plus/releases/tag/v0.6.0-alpha.1

- Candidate and annotated tag target: `98fdf86`.
- Candidate CI: https://github.com/digihori/pokecom-go-plus/actions/runs/39113621092
  (macOS, Windows, Linux and Android all successful).
- Tagged packaging/publication: https://github.com/digihori/pokecom-go-plus/actions/runs/39113836461
  (all four packaging jobs and GitHub Release publication successful).
- Seven package files and four SHA256SUMS files are public. Each package hash in
  the downloaded checksum lists matches GitHub's asset SHA-256 digest metadata;
  the downloaded checksum files' own hashes also match asset metadata.
- Published release notes match the repository notes. The Windows packaging
  identity check verifies version 1.4.0 and the retained UpgradeCode.
- Page 0 hardware dump result matches the previously verified 65536-byte image.
  No firmware, user recording, personal local path or private credential is added.

This publication-record commit does not rewrite the release tag or released code.
