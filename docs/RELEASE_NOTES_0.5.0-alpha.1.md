# Pokecom GO Technical Preview 0.5

Git version: `v0.5.0-alpha.1`

This early evaluation release brings Japanese/English Studio UI, debugger improvements and read-only AI registration access, plus the changes since Technical Preview 0.4. It is not a stable or app-store release.

## Downloads

- Studio: macOS Apple Silicon DMG/ZIP, Windows x64 MSI/ZIP, Linux x86_64 DEB/tar.gz
- Android Player: debug-signed APK, Android 8.0 or later
- Platform SHA-256 lists, LICENSE and third-party notices accompany the packages.

No ROMs are included. Supply ROMs lawfully obtained from your own machines. iOS is not distributed in this release.

## Studio UI and debugger

- Japanese/English UI with Follow OS / Japanese / English language selection; restart to apply changes
- Fixed Run/Pause/Step controls, stop reasons and navigation to the stopped instruction or accessed memory
- Register and memory changes highlighted against the previous step or stop
- Memory-access and bank-switch history with filters and navigation
- Breakpoint and monitoring lists with add/edit/delete and enabled/disabled controls
- Multiple value-change, read, write and read/write monitoring ranges
- Read-only `pgp_get_debug_registrations` MCP tool returns enabled and disabled entries with session ID/revision

Existing MCP tools and Debug Context version 1 remain compatible. Add the new tool to an existing enabled_tools list, or copy the connection configuration again from Studio.
Registrations survive Reset but are not saved across sessions/app restarts. Addresses apply to all ROM banks.
Conditional/hit-count breakpoints, bank-specific registrations, Step Over/Out and source-level stepping are future work.

## Other changes since 0.4

- Core/Studio support for PC-1450, PC-1460 and PC-1470U; PC-1260 and PC-1402 capacity variants
- PC-1470U arrow-key input correction
- External SC61 assembler integration and large build-preview responsiveness/Windows layout improvements
- iOS PC-1245 host and ROM management groundwork, with unsigned iPhone Simulator CI
- Additional mobile skin assets; asset inclusion does not imply completed Player machine support

Studio supports PC-1245, PC-1250/1251/1255, PC-1260/1261, PC-1350, PC-1401/1402, PC-1360, PC-1450/1460 and PC-1470U.
Android Player remains PC-1245 only. Player-wide localization and other machine skin integration are not included.

## Validation

Local JDK 21 full build/Core tests and Studio tests pass. The debugger/AI extension has 312 Studio tests, no failures and one existing conditional SDL skip.
Automated checks cover Android, Linux, Windows, macOS and iPhone Simulator; packaging covers the three desktop platforms and Android.
The user confirmed Windows/Linux operation and Windows upgrade installation on 2026-10-14.
macOS Studio-to-AI registration retrieval was confirmed with a paused PC-1470U; isolated UI tests cover Japanese/English debugger interactions.
iPhone Simulator validation is separate from iOS device/signing/App Store readiness. The full native macOS debugger operation checklist remains pending.

## Installation and limitations

Studio native package version is 1.3.1. The Windows MSI retains its published UpgradeCode; update installation was user-confirmed.
Packages are unsigned/unnotarized previews. Android uses the existing debug-signing strategy; a different signing key may prevent in-place APK updates. Do not delete existing app data without backing up your programs.
Android privacy-policy/Studio-information pages remain placeholders. Player has no Project, Assembler, Debugger, WAV Tool, audio output or save states.
External assembler logs, commands, identifiers and paths remain untranslated. This release does not investigate SHIFT TITLE, RAM mirrors/cards or expanded RAM support.
