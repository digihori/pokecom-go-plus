# Technical Preview 0.7 alpha 1 release checklist

Status: preparation, not published. Started 2026-10-22 (JST).
Previous release: `v0.6.0-alpha.2`. Do not rewrite previous tags or assets.

## Version and scope

- Project/runtime/tag: `0.7.0-alpha.1` / planned `v0.7.0-alpha.1`.
- Studio native package: `1.5.0`; preserve Windows UpgradeCode
  `D173EFD1-57EF-3753-A618-92481F262D95`.
- Android Player: `0.7.0-alpha.1`, versionCode `9`; Google Play submission already
  reported by the owner. Do not change its version or store release in this task.
- GitHub Android asset remains debug-signed. It is not the store AAB.
- No iOS installable artifact. No ROMs, signing keys or user data in artifacts.

## Preparation gates

- [x] Review changes since previous tag; prepare bilingual release notes and guide links.
- [x] Align Gradle version, ProjectInfo runtime constant, tests and release workflow.
- [x] Advance native package version and MSI inspection expectation together.
- [x] JDK 21 full `./gradlew build` succeeds, including runtime-version regression.
- [x] Local macOS application and DMG generated; bundled version, icon and legal resources inspected.
- [ ] Candidate commit CI passes for macOS, Windows, Linux, Android and iOS unit tests.
- [ ] Manual packaging workflow produces macOS/Windows/Linux distributions and Android debug APK.
- [ ] Verify package versions, expected filenames and SHA-256 checksums for candidate artifacts.

## User-facing acceptance

- [ ] Exact candidate Studio: launch, About `0.7.0-alpha.1`, ROM registration and input/audio smoke test.
- [ ] Exact candidate Studio: create a `.pgrom`, transfer it to Player and boot the corresponding model.
- [ ] Windows/Linux packaged application smoke tests, or clearly record unverified environments.
- [ ] Review release notes, unsigned-distribution warnings and known limitations.

Earlier development tests and user confirmations are not exact-candidate acceptance.
An artifact being generated does not verify its GUI or physical-device behavior.

## Publication (only after preparation and acceptance)

- [ ] Confirm final release candidate commit and CI.
- [ ] Create and push annotated `v0.7.0-alpha.1` tag; workflow publishes GitHub prerelease.
- [ ] Verify uploaded assets and checksum lists against downloaded packages.
- [ ] Change README latest-release banner and remove preparation notices after publication.
- [ ] Verify Player setup links reach the published compatible Studio release.
- [ ] Record release URL, candidate SHA, CI and packaging runs, and publication time here.

## Local verification (2026-10-22 JST)

- JDK 21: `./gradlew --gradle-user-home .gradle-local build :desktopApp:createDistributable :desktopApp:packageDmg` succeeded.
- Inspected macOS `Info.plist`: native version `1.5.0`; packaged ProjectInfo class:
  `0.7.0-alpha.1`, with no stale `0.6.0-alpha.2` value.
- Packaged ICNS matches the selected Studio icon; LICENSE and THIRD_PARTY_NOTICES are present.
- DMG: `desktopApp/build/compose/binaries/main/dmg/PokecomGOStudio-1.5.0.dmg`.
- Local DMG SHA-256: `bf9a898134b5652a9635e4285ea0013b3924fdc3380f21b0d520d3a8df1ca0c7`.
- This verifies packaging and automated tests, not exact-candidate GUI or hardware acceptance.
