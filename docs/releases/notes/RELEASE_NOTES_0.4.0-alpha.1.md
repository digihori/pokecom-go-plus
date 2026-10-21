# Pokecom GO Technical Preview 0.4

文書区分：記載されたGitタグのリリースノート。現在の開発版の機能一覧ではありません。

Git version: `v0.4.0-alpha.1`

This preview completes the current PC-1245 Android Player milestone and includes Studio/Core improvements
since Technical Preview 0.3. It is an early evaluation release, not a stable or app-store release.

## Downloads

- **Studio for macOS:** application ZIP and DMG (Apple Silicon)
- **Studio for Windows x64:** application ZIP and MSI
- **Studio for Linux x86_64:** application tar.gz and DEB
- **Player for Android 8.0 or later:** debug-signed APK

No ROM images are included. Supply ROMs lawfully obtained from your own machines. Platform SHA-256 lists,
LICENSE and third-party notices accompany the packages.

## PC-1245 Android Player

- Refined 16-character LCD, fixed-position SHIFT/DEF and angle indicators, and visible RUN/PRO mode
- RUNNING/PAUSED/FAULTED indicator separate from the pocket computer's operating mode
- Automatic CPU execution after ROM import/restoration/replacement and Reset
- Both landscape orientations, display-cutout/navigation-bar avoidance, and manual Controller Display
- Larger circular menu control and in-screen hierarchical menu without a separate popup window
- `.bas`/`.dmp` loading through Android's file picker, with automatic detection by filename extension
- Direct validated load without an overwrite prompt; dmp can overwrite BASIC RAM including C000
- Automatic CPU resume after successful dmp load; BASIC loads and failed/cancelled loads remain paused
- Whole-BASIC export, inclusive-address RAM-range dmp export, new-file save and Android sharing
- ROM library listing, replacement/deletion confirmation and safe session replacement
- Gamepad input and per-button assignment, sharing input ownership with the touch keyboard
- Installed version information; privacy-policy and Studio-information pages currently show placeholders

Menu Run resumes the emulated CPU; it does not type the BASIC RUN command. Backgrounding pauses execution.
The current Android Player supports **PC-1245 only**. Instructions:
[Android Player guide](https://github.com/digihori/pokecom-go-plus/blob/v0.4.0-alpha.1/docs/PLAYER_PROGRAM_IO.md).

## Studio and shared Core

- PC-1261 and PC-1401 emulation, machine-specific keyboards and BASIC conversion
- Gamepad input/assignment, external-editor settings and program-source tracking
- Studio layout, keyboard input and emulator speed-control refinements
- Improved ROM-set creation window and machine-specific ROM-size guidance
- SC61860 assembler expressions, EQU constants, DB/DW/DS/ALIGN and HIGH/LOW, with source diagnostics
- Assembly build naming and clearer build/load state in the Studio workspace
- Integrated Studio WAV Tool with OLD BASIC/binary encode/decode, Raw Decode and basic waveform analysis
- WAV Encode can use an existing file or a Project-tree source
- Read-only AI/debug integration and portable tests; this is not remote execution or autonomous debugging
- Shared machine-selected BASIC conversion reused by Studio and Player

Studio machines: PC-1245, PC-1250/1251/1255 family, PC-1261, PC-1350, PC-1401 and PC-1360.
Core support does not imply that the same machine has a Player skin.

## Validation and platform status

PC-1245 Android operation was confirmed by the user during development. Emulator smoke checks also cover
ROM restoration/automatic execution, menu behavior, ROM listing/deletion cancellation and version display.
The Studio WAV BASIC encode → physical-machine load/save recording → decode flow was confirmed by the user.
Local JDK 21 tests cover Core, Studio, shared Player and Android; package generation and automated checks run
on GitHub Actions for macOS, Windows, Linux and Android. Current Windows/Linux package UI operation is not
claimed as newly tested by this release. Earlier previews received manual checks.

## Known limitations

- iOS Player and Android Player skins for PC-1401/1350 are subsequent development.
- Player has no WAV Tool, ROM CSAVE/CLOAD integration, Project, Assembler, Debugger, audio output or save states.
- Player external-keyboard support and automatic display switching on controller connection are future work.
- Privacy-policy and Studio-information text remains placeholder; this is not a production privacy notice.
- The APK is debug-signed. Different CI/local builds may use different signing keys. Updating an older build
  may require uninstalling it, which removes app-private ROM registration and emulator data; retain your
  original ROM/program files before reinstalling. This is not a store-distribution or stable-signing build.
- Android 16 large-screen behavior may override the requested landscape orientation.
- Loading dmp onto BASIC RAM intentionally replaces that data and can invalidate BASIC management pointers.
  BASIC loading/export then requires valid BASIC memory initialization. bin and Intel HEX are not supported.
- File saving creates a new document; direct overwrite of an existing document is not provided.
- Studio WAV S1/S2 codecs are still under development; use pcwav for these formats. WAV remains Studio-only.
- PC-1350 named BASIC special-character escapes remain incomplete. Macro/include/conditional assembly is
  not yet implemented. Character Editor remains a single-character tool.
- On Windows, click noise can persist after a program using sound finishes; the previously documented
  audio shutdown limitation remains.
- Desktop packages are not signed with platform distribution identities and macOS is not notarized; OS warnings
  may appear. Linux ARM64 and native Windows ARM64 packages are not supplied.
- Save-state compatibility and public APIs are not guaranteed between previews.

## Feedback and licensing

Report defects through GitHub Issues with product/version, machine, host OS/Android version, file format and
reproduction steps. Do not upload ROMs, credentials or private/non-redistributable data.
Pokecom GO Plus is MIT-licensed; third-party notices and implementation provenance are included in the repository.
