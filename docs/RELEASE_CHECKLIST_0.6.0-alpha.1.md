# Technical Preview 0.6 release checklist

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
- [ ] Candidate pushed; macOS, Windows, Linux and Android CI all successful.
- [ ] New annotated tag pushed after candidate checks.
- [ ] Tagged packaging and prerelease publication successful.
- [ ] Seven packages and four checksum lists verified against asset digest metadata.
- [ ] Publication result recorded.

Release authorization includes commit/push, the new tag and prerelease publication.
Existing tags/history are not rewritten. End-user installation of the final assets is not claimed.
