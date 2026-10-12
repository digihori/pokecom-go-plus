# Technical Preview 0.4 release checklist

- Release name: **Pokecom GO Technical Preview 0.4**
- Project version / Git tag: `0.4.0-alpha.1` / `v0.4.0-alpha.1`
- Studio native package version: `1.3.0`
- Android version code / name: `4` / `0.4.0-alpha.1`
- Scope: PC-1245 Android milestone plus Studio/Core changes since 0.3; iOS is not included.

## Candidate preparation

- [x] PC-1245 Android milestone accepted after user operation checks (2026-10-12)
- [x] Release Notes include Studio/Core delta and Player behavior/limitations
- [x] Privacy/Studio placeholders, debug-signature upgrade implications and non-production status documented
- [x] Version references, ProjectInfo test, release workflow title/notes and native package version updated
- [x] Local JDK 21 full build and Core tests pass after version changes
- [x] Local macOS distributable builds and launch smoke passes
- [x] Versioned Android APK installs on the existing emulator; ROM operation accepted in the PC-1245 milestone
- [x] Candidate GitHub Actions CI passes on macOS/Windows/Linux/Android
- [x] Manual release-workflow dispatch builds all packages without publication
- [x] ROMs, credentials and local emulator files remain excluded from source/package inputs
- [x] `git diff --check` passes

Windows/Linux current UI smoke is not a release acceptance claim; earlier preview testing is recorded in
previous checklists. This preview reports current automated tests/packaging separately from user/device checks.

## Expected assets

- `PokecomGOStudio-0.4.0-alpha.1-macos-<arch>.dmg`
- `PokecomGOStudio-0.4.0-alpha.1-macos-<arch>.zip`
- `PokecomGOStudio-0.4.0-alpha.1-windows-x64.msi`
- `PokecomGOStudio-0.4.0-alpha.1-windows-x64.zip`
- `PokecomGOStudio-0.4.0-alpha.1-linux-amd64.deb`
- `PokecomGOStudio-0.4.0-alpha.1-linux-amd64.tar.gz`
- `PokecomGOPlayer-0.4.0-alpha.1-android-debug.apk`
- `SHA256SUMS-macos.txt`, `SHA256SUMS-windows.txt`, `SHA256SUMS-linux.txt`, `SHA256SUMS-android.txt`
- GitHub-generated source ZIP/tar.gz

## Publication

- [x] Release-preparation commit pushed to main
- [ ] Verified candidate annotated tag created/pushed
- [ ] Tag workflow succeeds on all platforms and publishes prerelease
- [ ] Release title/notes/version and complete asset set verified
- [ ] Published platform checksum lists match packaged assets' SHA-256 digest metadata
- [ ] Publication outcome recorded and main working tree clean

The authorized release task includes prerelease publication after candidate checks. Source ROMs and
ROM-derived fixtures are never published. Retain the existing debug signing strategy; do not introduce
new production signing credentials or publish an app-store build.

## Verification record (2026-10-12)

Candidate `9ad74e8` passes [CI #62](https://github.com/digihori/pokecom-go-plus/actions/runs/38203020330).
Local JDK 21 full build/Core checks and macOS createDistributable succeeded. The generated macOS app
restored the registered PC-1245 ROM and passed Run/Pause checks. The versioned Android APK installed
without deleting its app-private ROM registration. The PC-1245 milestone has user operation acceptance.
Package-resource audit found license/notices and no ROM/keystore payloads in the macOS/Android products.
Manual packaging verification: [Build Technical Preview #9](https://github.com/digihori/pokecom-go-plus/actions/runs/38203075953).

Manual run #9 completed successfully on all four platforms and produced all four expected workflow artifacts. Publication was skipped as intended for a branch dispatch. The following verification commit changes documentation only; release code/version/workflow remain the tested candidate.
