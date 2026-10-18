# Technical Preview 0.6 alpha 2 release checklist

- Project/tag/runtime version: `0.6.0-alpha.2` / `v0.6.0-alpha.2`
- Studio native packageVersion: `1.4.1`; Windows UpgradeCode retained.
- Android versionCode/name: `7` / `0.6.0-alpha.2`.
- Fix stale ProjectInfo.VERSION used by About and MCP; add a build/runtime consistency test.
- Prior alpha-1 Mac operation user-confirmed; exact alpha-2 assets require separate user checks.
- Existing alpha-1 tag and assets are not replaced.

## Gates

- [x] Local full build, runtime-version regression and macOS distribution inspection.
- [ ] Candidate CI passes on all four platforms.
- [ ] New annotated tag and packaging/publication succeed.
- [ ] Seven package digests and four checksum lists verified.
- [ ] Publication result recorded.
