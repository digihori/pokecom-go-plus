# Technical Preview 0.3 release checklist

- Release name: **Pokecom GO Technical Preview 0.3**
- Project version: `0.3.0-alpha.1`
- Git tag: `v0.3.0-alpha.1`
- Studio native package version: `1.2.0`
- Android version code／name: `3`／`0.3.0-alpha.1`

## Release candidate

- [x] Studio layout, input, debugger, Character Editor, OLD WAV, and build-documentation changes are included
- [x] Android Player minimum prototype and PC-1245 skin are included
- [x] Release workflow builds the Android Player and publishes its APK and SHA-256 checksum
- [x] Android Player is identified as debug-signed and its limitations are documented
- [x] Studio and Player packages include the project license and third-party notices
- [x] ROM images and local emulator data remain excluded from Git and release assets
- [x] Local JDK 21 `./gradlew build` succeeds after the versioned release changes
- [x] `git diff --check` succeeds
- [x] macOS Studio release-candidate package is installed or opened and smoke-tested
- [x] Windows Studio package receives a current launch, sound, and basic-operation smoke test
  - [x] Sound playback checked; persistent click noise after a sounding program finishes is recorded as a known issue
- [x] Linux Studio package receives a current launch and basic-operation smoke test
- [x] Android Player APK is installed and smoke-tested; an older debug-signed build had to be uninstalled first
- [x] GitHub Actions CI succeeds for the release-candidate commit, including the Android Player job
- [x] Manually dispatched release workflow produces every expected artifact without publishing a Release

## Expected release assets

- `PokecomGOStudio-0.3.0-alpha.1-macos-<arch>.dmg`
- `PokecomGOStudio-0.3.0-alpha.1-macos-<arch>.zip`
- `PokecomGOStudio-0.3.0-alpha.1-windows-x64.msi`
- `PokecomGOStudio-0.3.0-alpha.1-windows-x64.zip`
- `PokecomGOStudio-0.3.0-alpha.1-linux-amd64.deb`
- `PokecomGOStudio-0.3.0-alpha.1-linux-amd64.tar.gz`
- `PokecomGOPlayer-0.3.0-alpha.1-android-debug.apk`
- `SHA256SUMS-macos.txt`
- `SHA256SUMS-windows.txt`
- `SHA256SUMS-linux.txt`
- `SHA256SUMS-android.txt`
- GitHub-generated source archives

## Publication

- [x] Versioned release-preparation commit is pushed to `main`
- [x] Annotated `v0.3.0-alpha.1` tag is created from the verified commit
- [x] Tag is pushed and the release workflow succeeds on macOS, Windows, Linux, and Android
- [x] GitHub prerelease contains every expected Studio and Player asset
- [x] Published Release Notes and asset names identify Technical Preview 0.3
- [x] Published checksums match GitHub's SHA-256 digest metadata for every packaged asset

The release workflow creates unsigned Desktop packages and a debug-signed Android APK. No ROM image is included
in any package or test input. Do not create or push the release tag until the release-candidate checks above have
been reviewed.

Release candidate packaging was checked on macOS, Windows, Linux, and Android. Windows retains the documented
post-program click-noise issue.
