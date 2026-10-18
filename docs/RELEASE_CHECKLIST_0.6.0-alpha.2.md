# Technical Preview 0.6 alpha 2 release checklist

- Project/tag/runtime version: `0.6.0-alpha.2` / `v0.6.0-alpha.2`
- Studio native packageVersion: `1.4.1`; Windows UpgradeCode retained.
- Android versionCode/name: `7` / `0.6.0-alpha.2`.
- Fix stale ProjectInfo.VERSION used by About and MCP; add a build/runtime consistency test.
- Prior alpha-1 Mac operation user-confirmed; exact alpha-2 assets require separate user checks.
- Existing alpha-1 tag and assets are not replaced.

## Gates

- [x] Local full build, runtime-version regression and macOS distribution inspection.
- [x] Candidate CI passes on all four platforms.
- [x] New annotated tag and packaging/publication succeed.
- [x] Seven package digests and four checksum lists verified.
- [x] Publication result recorded.

## Publication result

Published 2026-10-18 at 18:40 JST as a prerelease:
https://github.com/digihori/pokecom-go-plus/releases/tag/v0.6.0-alpha.2

- Candidate/tag: `8b11ec9`.
- Candidate CI: https://github.com/digihori/pokecom-go-plus/actions/runs/39115666171
- Packaging/publication: https://github.com/digihori/pokecom-go-plus/actions/runs/39115950346
- All four CI platforms and all packaging/publication jobs succeeded.
- Seven packages and four checksum lists verified against GitHub asset digest metadata.
  Published notes match the repository notes.
- The local macOS distribution contains native version 1.4.1 and runtime version
  0.6.0-alpha.2 in ProjectInfo, About and MCP classes. No packaged PGP class still
  contains the stale 0.5.0-alpha.1 value.
- Existing alpha-1 release/tag remains unchanged. This record does not rewrite alpha 2.
