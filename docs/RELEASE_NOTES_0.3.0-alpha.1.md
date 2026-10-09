# Pokecom GO Technical Preview 0.3

Git version: `v0.3.0-alpha.1`

Technical Preview 0.3 is the first preview to distribute both Pokecom GO Studio and the Android version of
Pokecom GO Player. It also improves Studio's main workspace, input handling, debugger, and small utility tools.
This remains an early preview intended for testing and feedback, not a stable release.

## Downloads

- **Studio for macOS:** DMG and application ZIP
- **Studio for Windows x64:** MSI and application ZIP
- **Studio for Linux x86_64:** DEB and application tar.gz
- **Player for Android 8.0 or later:** debug-signed preview APK

ROM images are not included. Users must provide images lawfully obtained from their own machines. SHA-256
checksum files are provided for every platform.

## Studio highlights

- Instrument-inspired theme with a compact main workspace
- Resizable Project pane beside the LCD and software keyboard
- Emulator controls moved into the top information row
- Independent Debugger, Project Files, Assembly Workspace, and Tools windows
- Selectable text and visible scrollbars in information-heavy views
- More reliable paced input from a physical PC keyboard
- Emulator command history and menu access
- Debugger register overview, 16-row memory dump, adjustable trace buffer, and clearer section boundaries
- Minimal 5×7 Character Editor with drawing, erase, clear, invert, numeric input, and BASIC／assembly output formats
- OLD WAV encoder／decoder prototype
- Source-build guide for macOS, Windows, Linux, and Android
- Windows audio shutdown handling to prevent residual clicks after emulation stops

## Android Player preview

The first distributed Player preview focuses on running PC-1245 software on Android:

- `.pgrom` and compatible PC-1245 legacy ROM import through Android's system file picker
- Validated ROM storage in the application-private area and restoration after restart
- PC-1245 device skin with LCD overlay and all 52 touch keys
- Key press highlighting and release animation
- Run, Pause, Reset, and RUN／PRO mode controls
- Full-device and enlarged Controller Display modes
- Full-screen layout that preserves the skin aspect ratio

The APK uses Android's development debug signature. It is intended only for this Technical Preview and is not
an app-store or production-signed build. A later differently signed build may require uninstalling this preview
before installation.

## Supported Studio machines

- PC-1245
- PC-1250／PC-1251／PC-1255 shared family ROM
- PC-1350
- PC-1360

The Android Player preview currently provides the PC-1245 skin and interaction model only.

## Platform status

- macOS: principal Studio workflows have been tested during development; packaged-app confirmation remains a
  release-candidate check.
- Windows x64: automated tests and packaging run in GitHub Actions. Audio shutdown changes require a manual
  release-candidate check on Windows.
- Linux x86_64: automated tests and DEB／tar.gz packaging run in GitHub Actions; a current manual UI pass remains
  pending.
- Android: automated tests and APK generation run in GitHub Actions. Installation and ROM-based operation require
  a manual check on a physical device or emulator.
- iOS: Player is not implemented.

## Known limitations

- All distributed packages are preview builds. Desktop packages are unsigned and the macOS package is not
  notarized; operating-system security warnings may be shown.
- The Android Player has no sound output, save states, gamepad mapping, program load／save menu, or iOS counterpart.
- The Android Player does not yet provide explicit ROM replacement or deletion controls.
- The Player APK is debug-signed and is not suitable for store distribution or long-term upgrade compatibility.
- The Character Editor currently edits one character at a time. Undo／Redo, multi-character editing, and file
  import／export are future work.
- OLD WAV support is an early interoperability prototype.
- Assembly Workspace and the assembler remain early implementations; expressions, constants, macros, include
  files, and conditional assembly are not yet supported.
- Physical PC-keyboard input is logical-text oriented. A game-oriented physical-key mode is planned.
- Save-state compatibility and stable public APIs are not guaranteed between Technical Preview releases.
- Linux ARM64 and native Windows ARM64 packages are not available.

## Feedback

Report reproducible defects and feature requests through GitHub Issues. Include the product, version, host OS or
Android version, selected machine, input format, and reproduction steps. Do not include ROMs, credentials,
personal information, or other non-redistributable data.

## Licensing and provenance

Pokecom GO Plus is released under the MIT License. Third-party notices are recorded in
`THIRD_PARTY_NOTICES.md`, and implementation provenance is recorded in `docs/PORTING_NOTES.md`.
