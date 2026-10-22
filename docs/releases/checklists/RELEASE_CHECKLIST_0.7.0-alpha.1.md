# Technical Preview 0.7 alpha 1 release checklist

Status: publication in progress; owner authorized release after completing operation checks on 2026-10-23 (JST).
Previous release: `v0.6.0-alpha.2`. Do not rewrite previous tags or assets.

## Version and scope

- Project/runtime/tag: `0.7.0-alpha.1` / `v0.7.0-alpha.1`.
- Studio native package: `1.5.0`; preserve Windows UpgradeCode
  `D173EFD1-57EF-3753-A618-92481F262D95`.
- Android Player: `0.7.0-alpha.1`, versionCode `9`; Google Play release verified on October 23, 2026 JST. Do not change its version or store release in this task.
- GitHub Android asset remains debug-signed. It is not the store AAB.
- No iOS installable artifact. No ROMs, signing keys or user data in artifacts.

## Preparation gates

- [x] Review changes since previous tag; prepare bilingual release notes and guide links.
- [x] Align Gradle version, ProjectInfo runtime constant, tests and release workflow.
- [x] Advance native package version and MSI inspection expectation together.
- [x] JDK 21 full `./gradlew build` succeeds, including runtime-version regression.
- [x] Local macOS application and DMG generated; bundled version, icon and legal resources inspected.
- [x] Candidate commit CI passes for macOS, Windows, Linux, Android and iOS unit tests.
- [x] Manual packaging workflow produces macOS/Windows/Linux distributions and Android debug APK.
- [x] Verify package versions, expected filenames and SHA-256 checksums for candidate artifacts.

## User-facing acceptance

- [x] Owner completed overall operation checks and authorized release on 2026-10-23 JST; version metadata was independently inspected. Individual GUI test steps were not itemized.
- [x] DMG-installed Studio: create a `.pgrom`, transfer it to Player, register it and boot the corresponding model (owner confirmed 2026-10-23 JST; see acceptance record below).
- [x] Windows/Linux packaged application smoke tests: owner reported both OK on 2026-10-23 JST (see acceptance record below).
- [x] Review release notes, unsigned-distribution warnings and known limitations.

Earlier development tests and user confirmations are not exact-candidate acceptance.
An artifact being generated does not verify its GUI or physical-device behavior.

### Owner acceptance: Studio to Player (2026-10-23 JST)

- Owner confirmed that Studio installed from the DMG could create a ROM package and that Player could register it and boot.
- This verifies the Studio-to-Player ROM package workflow. The tested model, Player platform and exact DMG checksum were not recorded in the report; do not infer coverage of all models or a later rebuilt artifact.
- Owner subsequently reported overall operation checks complete and authorized release; this does not imply exhaustive model coverage.

### Owner acceptance: Windows and Linux (2026-10-23 JST)

- Owner confirmed that Linux and Windows application operation checks were OK.
- Linux deb installation emitted the APT `_apt` access notification when reading from Downloads under a home directory with mode 700. Owner confirmed that home-directory permission; the notification was discussed separately from application operation.
- Linux application-menu classification under “Other” and the English Windows setup wizard are recorded in the roadmap as formal-release follow-ups, not blockers for this Technical Preview.
- OS/distribution versions, exact artifact checksums and individual test operations were not specified in the report; this records the owner's smoke-test result without claiming exhaustive coverage.

## Publication (only after preparation and acceptance)

- [x] Confirm final code candidate commit and CI; release preparation commit changes documentation only.
- [ ] Remove the release-note preparation notice, then create and push annotated `v0.7.0-alpha.1` tag; workflow publishes GitHub prerelease.
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

## Candidate CI and artifact verification (2026-10-23 JST)

- Tested code candidate: `049877baaeeca35c2d9570b530554a57f281848c`.
- [CI](https://github.com/digihori/pokecom-go-plus/actions/runs/39785372931):
  macOS, Windows, Linux, Android and iOS unit jobs all succeeded.
- [Packaging](https://github.com/digihori/pokecom-go-plus/actions/runs/39785631573):
  all four platform jobs succeeded. Publication was skipped because this was a manual run.
- Four workflow artifact archive SHA-256 digests match GitHub metadata.
- All seven package files match the four included SHA256SUMS lists; filenames carry `0.7.0-alpha.1`.
- macOS/Windows ZIP and Linux tar.gz contain runtime version `0.7.0-alpha.1`.
- macOS Info.plist and Debian control report native version `1.5.0`.
- Windows CI's `inspect-msi.ps1 -VerifyCurrent` passed, checking version `1.5.0`
  and the preserved UpgradeCode.
- Android debug APK metadata: package `com.digihori.pgp.player`, versionCode `9`,
  versionName `0.7.0-alpha.1`, min SDK 26, target SDK 36. Store signing is separate.
- Additional standalone tools: memory utilities 8 tests, pcwav 2 tests passed with synthetic data.
- Local Gradle results: Core desktop 491 tests, Studio 406 tests (1 skipped),
  Android debug 80 tests; no failures or errors.
- Downloaded candidate artifacts are available under the OS temporary directory
  `pgp-studio-070-candidate/`, separated by workflow artifact name. Workflow artifacts
  are retained for 14 days; copy anything needed for later acceptance elsewhere.
- No release tag was created and no GitHub prerelease was published.
- Documentation-only follow-up records Player's public Play listing and these verification results;
  the code/assets tested above are unchanged.

## macOS menu correction after candidate acceptance (2026-10-23 JST)

- Owner's DMG install revealed two application menus: the OS menu and a cross-platform Studio menu.
- Connect About, Preferences and Quit to native AWT Desktop handlers on macOS.
  Preserve the custom application menu on Windows/Linux and unsupported environments.
- Native Quit delegates to Compose shutdown so emulator/audio/MCP cleanup is retained.
- An isolated application using the packaged runtime verified all three native callbacks,
  native-menu selection and absence of the duplicate menu, without reading user ROMs/preferences.
- Desktop tests and application/DMG regeneration succeeded. JDK 21 full `./gradlew build` also succeeded.
- New local DMG SHA-256: `8ebb703440bd8459ced5ff3fae6dcc509995eebd75b562d325adad3fe8e25785`.
- Earlier CI artifacts for `049877b` predate this correction. Re-run candidate CI and
  packaging for the final accepted commit; preparation gates above are reopened.

## Mounted DMG volume icon (2026-10-23 JST)

- The packageDmg task post-processes a writable temporary copy to set the Studio
  `.VolumeIcon.icns` and Finder custom-icon flag, then verifies/recompresses the DMG.
- Mounted the finished image read-only in a private mountpoint: icon bytes match
  `desktopApp/icons/pokecom-go.icns` and `GetFileInfo -aC` reports `1`.
- User-mounted volumes are not modified. Previously mounted images must be ejected
  and the regenerated DMG mounted to see the new icon.
- New local DMG SHA-256: `50d961a0104620d3fc74b94bcc2baa89e771c8089907ee2902aad971bada9521`.
- Earlier CI artifacts predate this packaging correction and remain superseded.

## Final candidate verification (2026-10-23 JST)

- Code and packaging candidate: `498f47580bcb8b96e6d56a3e9fdbe5e06027e163`; subsequent release preparation changes are documentation only.
- [Final code CI](https://github.com/digihori/pokecom-go-plus/actions/runs/39853425177) and [manual packaging](https://github.com/digihori/pokecom-go-plus/actions/runs/39856190181) succeeded, including both macOS corrections.
- Owner confirmed macOS Studio-to-Player ROM workflow and Windows/Linux operation, then authorized publication.
- Verified four final workflow archive digests and all seven package checksums; packaged runtime is `0.7.0-alpha.1`, macOS/Debian native version is `1.5.0`.
