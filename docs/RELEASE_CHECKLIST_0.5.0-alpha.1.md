# Technical Preview 0.5 release checklist

- Release: Pokecom GO Technical Preview 0.5
- Project/tag: `0.5.0-alpha.1` / `v0.5.0-alpha.1`
- Studio native packageVersion: `1.3.1` (UpgradeCode retained)
- Android versionCode/name: `5` / `0.5.0-alpha.1`
- Scope: Studio debugger/localization/AI and changes since 0.4; desktop packages and Android APK, no iOS distribution.

## Preparation

- [x] Windows/Linux operation and Windows update installation user-confirmed (2026-10-14)
- [x] macOS paused PC-1470U registration retrieval through the AI client confirmed
- [x] Prior implementation a521a05 passes all five CI jobs and manual four-platform packaging
- [x] Release notes distinguish implemented features, synthetic tests and device checks
- [x] Version references and release workflow updated; native version remains 1.3.1
- [x] Versioned candidate local JDK 21 full build/Core tests and macOS createDistributable
- [ ] Versioned candidate pushed and all five CI jobs successful
- [ ] Annotated tag pushed after candidate checks
- [ ] Tagged packaging workflow publishes prerelease
- [ ] Seven packages and four checksum lists verified
- [ ] Publication outcome recorded

## Expected assets

macOS arm64 DMG/ZIP, Windows x64 MSI/ZIP, Linux amd64 DEB/tar.gz and Android debug APK, each named with 0.5.0-alpha.1.
Four SHA256SUMS platform files and GitHub-generated source archives. No ROM/ROM-derived fixture or private credential is added.

## Verification boundaries

Windows/Linux and Windows upgrade checks were user-reported for the development candidate before the release-version change.
The final versioned packages are checked by CI/packaging; final user installation of those exact assets is not claimed.
Full native macOS debugger interaction remains pending; iOS real devices/signing and other Player machines remain future work.
Existing tags/history are unchanged. Release authorization includes commit/push, new tag and prerelease publication.

Implementation CI: https://github.com/digihori/pokecom-go-plus/actions/runs/38563336723
Implementation packaging: https://github.com/digihori/pokecom-go-plus/actions/runs/38563652964
