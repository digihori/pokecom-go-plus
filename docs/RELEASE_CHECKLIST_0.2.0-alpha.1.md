# Technical Preview 0.2 release checklist

Release name: **Pokecom GO Studio Technical Preview 0.2**  
Project version: `0.2.0-alpha.1`  
Git tag: `v0.2.0-alpha.1`  
Native package version: `1.1.0`

## Release candidate

- [x] PC-1350／PC-1360 implementation and documentation are included
- [x] ROM Library, reorganized UI, Project Files, and Debugger changes are included
- [x] Assembly Workspace, Listing, Symbol Map, Diagnostics, and shared Build & Load are included
- [x] ROM images and local emulator data remain excluded from Git and release assets
- [x] Known Assembly Workspace and cross-platform limitations are listed in the Release Notes
- [x] Local JDK 21 `./gradlew build` succeeds
- [x] `git diff --check` succeeds
- [ ] macOS release-candidate smoke test is completed
- [x] GitHub Actions CI succeeds for the release-candidate commit

## Publication

- [x] Versioned release-preparation commit is pushed to `main`
- [x] Annotated `v0.2.0-alpha.1` tag is created from the verified commit
- [x] Tag is pushed and the release workflow succeeds on macOS, Windows, and Linux
- [x] GitHub prerelease has DMG, MSI, DEB, extracted archives, and SHA-256 files
- [x] Published Release Notes and asset names identify Technical Preview 0.2

The release workflow creates unsigned packages. No ROM image is included in any package or test input.

Release candidate `ecd1be6` passed CI run 37623521538. Tag workflow 37623746528 completed successfully
on 2026-10-07 and published nine release assets. The final packaged macOS application smoke test remains a
separate manual confirmation because this checklist update does not claim GUI interaction performed by CI.
