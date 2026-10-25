# Pokecom GO Plus

[English](README.md) | [日本語](README.ja.md)

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

> **[Technical Preview 0.7 (alpha 1)](https://github.com/digihori/pokecom-go-plus/releases/tag/v0.7.0-alpha.1)** — Prepare ROM packages for Android Player, with improved debugging, WAV tools and Studio-approved AI integration.

Pokecom GO Plus (PGP) is a multiplatform development environment for vintage pocket computers.
It aims to help you write, run, and analyze programs on a modern PC, then transfer them to the original hardware.

PGP consists of two products and a shared emulator core:

- **Pokecom GO Studio** — A pocket computer development environment for macOS, Windows, and Linux.
- **Pokecom GO Player** — An emulator focused on running programs on Android and iOS.
- **PGP Emulator Core** — The Kotlin Multiplatform core shared by Studio and Player.

“Pokecom GO Plus” and “PGP” are development code names for the project as a whole.
The development version of Studio exposes machine and development context through localhost MCP,
with Studio approval for source patches, Build, Load, and execution changes. See the [AI connection guide (Japanese)](docs/guides/AI_CONNECTION_GUIDE.md) for setup and limits.

Android Player setup pages are available in [English](https://digihori.github.io/pokecom-go-plus/player/en.html) and [Japanese](https://digihori.github.io/pokecom-go-plus/player/ja.html) ([viewing and hosting](docs/player/README.md)).

For usage, see the [documentation index](docs/README.en.md) or start with [Getting started](docs/guides/GETTING_STARTED.md). Detailed guides are mostly in Japanese.

Android Player is now available on [Google Play](https://play.google.com/store/apps/details?id=com.digihori.pgp.player). Supply your own lawfully obtained ROMs.

## Current release

Studio **0.7.0-alpha.1** is available alongside Android Player. See the
[release notes](docs/releases/notes/RELEASE_NOTES_0.7.0-alpha.1.md) and
[release checklist](docs/releases/checklists/RELEASE_CHECKLIST_0.7.0-alpha.1.md).

## Current status

The development versions of Studio and Android/iPhone Player support the following 14 models.
Registering a kanji ROM enables kana input and kanji conversion on PC-1360K.

| Machine or series | Android Player | iPhone Player |
| --- | --- | --- |
| PC-1245 | Supported | Supported |
| PC-1250, PC-1251, PC-1255 | Supported | Supported |
| PC-1260, PC-1261 | Supported | Supported |
| PC-1350 | Supported | Supported |
| PC-1401, PC-1402 | Supported | Supported |
| PC-1450 | Supported | Supported |
| PC-1360, PC-1360K | Supported | Supported |
| PC-1460 | Supported | Supported |
| PC-1470U | Supported | Supported |

The Android scope reached its agreed milestone on October 20, 2026. On October 21, the user confirmed
operation across the integrated iPhone models, sound playback, and the updated home screen.
Machine integration and environment-specific coverage remain distinct; see the
[Android acceptance record](docs/development/ANDROID_SERIES_PROGRESS.md) and
[iOS acceptance record](docs/development/IOS_MACHINE_SUPPORT_STEP8.md), both in Japanese.
The iPhone app requires iOS 16 or later and targets iPhone. Signed distribution is a separate step;
audio roughness remains a follow-up. For operation, use the [Android help](docs/guides/PLAYER_HELP.md)
or [iPhone help](docs/guides/PLAYER_HELP_IOS.md).

Studio checks with actual ROMs on macOS cover booting, physical and on-screen keys, BASIC and machine-code
file operations, LCD output, and sound. Manual Windows checks cover booting, keys, BASIC, and sound;
Linux x86_64 checks cover booting, basic operation, sound, and tar.gz/DEB packages.
For published-build support, check the [release notes for that version](docs/README.en.md#releases).

Main features include:

- Booting from user-supplied ROM images and `.pgrom` packages.
- RUN/PRO/RSV controls where supported, physical keyboards, and machine-specific on-screen keyboards.
- BASIC text entry through the ROM, or direct tokenization and extraction.
- Machine-code import and export in `.dmp` format.
- Multiple-source projects, external editor change detection, and Build & Load.
- LCD output, CPU state display, and sound.
- Hardware RAM and expanded RAM profiles on supported machines.
- File, Project, Program, Emulator, and Debug menus.
- A machine-specific ROM Library, project tree, and separate Debugger window.

## Download and get started

Download the preview from [GitHub Releases](https://github.com/digihori/pokecom-go-plus/releases).
Studio packages include their runtime; JDK 21 is required when building from source.

1. Extract or install the package for your platform:
   - **macOS:** Open the `.app`, or the application supplied in the DMG.
   - **Windows:** Run `PokecomGOStudio.exe` from the ZIP, or install the MSI.
   - **Linux x86_64:** Run `bin/PokecomGOStudio` from the tar.gz, or install the DEB.
2. Prepare ROM images lawfully obtained for your machine. **ROM images are not included.**
3. In Studio, use `File` → `Register ROM…` or `Manage ROMs…` to register a compatible image or `.pgrom` package.
   Use `Create ROM Set…` if you need to package separate ROM components first.
4. Studio copies validated ROMs into its private ROM Library and restores the last-used machine on later launches.
5. Use the on-screen keyboard or your physical keyboard to operate the emulated machine.

Studio previews are not signed with a distribution certificate or notarized for macOS.
The Android preview APK uses a debug signature, not a production store signature.
Your operating system may display a security warning. Packages include the MIT license and third-party
notices; releases provide SHA-256 checksums.

The detailed [getting started guide](docs/guides/GETTING_STARTED.md) is currently in Japanese.

## Workspace

The main window focuses on the LCD, machine-specific keyboard, Run/Pause/Reset/Step,
and RUN/PRO/RSV controls where supported. The machine selector lists machines registered in the ROM Library.

- `File`: Register and manage ROMs; create ROM sets.
- `Project`: Create/open projects, open Assembly Workspace, and Build & Load.
- `Program`: Import/export BASIC and machine code; Quick Assemble.
- `Emulator`: Execution controls and RAM profiles.
- `Debug`: Debugger and checkpoints.

The project tree appears on the left. CPU, Disassembly, Memory, and Trace views live in the separate Debugger.
Machine-specific keyboards reflect the original layouts, SHIFT legends, and keyword labels.

In Assembly Workspace, **Assemble** previews results, **Build** writes output files, and **Build & Load**
also loads them into the emulator. See the [SC61860 assembler guide (Japanese)](docs/guides/SC61860_ASSEMBLY.md)
for expressions, EQU, data definitions, memory placement, output names, and restrictions.

## Relationship to Pokecom GO

- PGP is a separate project, rather than a new version of the existing Pokecom GO Android app.
- Pokecom GO continues to be maintained independently.
- PGP analyzes and redesigns technology from Pokecom GO, including the CPU, machine definitions, and BASIC conversion.
- Neither the repository nor its distributions contain ROM images.

## Platforms

Studio development focuses on **macOS, Windows, and Linux**.
Linux packages currently target x86_64; ARM64 Linux packages are not yet available.

Player focuses on execution, without development tools such as a debugger, assembler, or disassembler.
Android and iPhone Player support the 14 models listed above. Device/OS/ROM-specific coverage and signed distribution are tracked separately. Save states are not implemented.

## Repository layout

```text
pokecom-go-plus/
├── core/          Shared Kotlin Multiplatform emulator core
├── hostInput/     Gamepad mappings and input ownership
├── controllerUi/  Controller settings shared by Studio and Android
├── playerShared/  Execution and display models shared by Android/iOS Player
├── desktopApp/    Pokecom GO Studio (Compose Desktop)
├── androidApp/    Pokecom GO Player for Android
├── iosApp/        Multi-machine iPhone host
├── docs/          Concepts, design, porting notes, and roadmap
├── local-data/    Local ROMs and other private data (ignored by Git)
└── test-data/     Redistributable test data
```

Place development ROMs in `local-data/roms/<machine-id>/`.
The default legacy paths are `local-data/roms/pc-1245/pc1245mem.bin` and
`local-data/roms/pc-1251/pc1251mem.bin`. The entire `local-data/` directory is ignored by Git.
Do not commit ROM images.

## Development requirements

- JDK 21.
- Android SDK 36, when building Android Player.
- Gradle Wrapper (included).
- Kotlin 2.4.20.
- Compose Multiplatform 1.12.1.

Use JDK 21 for normal builds and execution. Android Studio's bundled JBR can be used only if it is Java 21;
do not use Java 25. Creating DMG/MSI/DEB packages requires a full JDK 21 with `jpackage`;
the current Android Studio JBR alone cannot create these packages.

See [Building from source (Japanese)](docs/reference/BUILDING.md) for complete setup, platform-specific packaging,
output locations, and troubleshooting. Gradle does not need to be installed separately.

### Run Studio

On macOS, double-click `run-pgp.command` in Finder, or run:

```bash
./run-pgp.command
```

The script locates an installed JDK 21 and configures the local Gradle cache; it stops if JDK 21 is unavailable.
If macOS asks for confirmation, right-click the script in Finder and choose Open.
Studio restores the last-used machine from its private ROM Library. Without history, it checks the default
PC-1245 path followed by the PC-1251 path and runs the first ROM found.

On Windows:

```powershell
.\gradlew.bat :desktopApp:run
```

On Linux, or for a direct Gradle launch on macOS:

```bash
./gradlew :desktopApp:run
```

Windows and Linux have CI builds and automated tests. Manual coverage of UI operations, sound,
and file selection varies by platform and release; see the verification records.

### Build Android Player (14 models)

With Android SDK 36 and JDK 21 configured:

```bash
./gradlew :androidApp:assembleDebug
```

Output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
All 14 models listed above are selectable. Import a `.pgrom` package or a supported legacy ROM for the selected machine; ROMs are stored in private app storage.
Banked machines may require a Studio-created `.pgrom`; see the machine-specific documentation.
ROMs are restored to the home screen at startup, and the CPU runs automatically on first play and after Reset.

Player features include machine-specific skins and LCD symbols, landscape layouts, notch/navigation-area
handling, manual Controller Display, hierarchical menus, ROM replacement/deletion, gamepad mappings,
and physical keyboard input. PC-1245 has 52 skin keys and a 16-character LCD; other machines have their own layouts.

Program loading detects `.bas`/`.dmp` by extension. Android exports whole BASIC programs; DMP is load-only. Successful loading resumes the CPU; failure or cancellation leaves it paused.
The app includes settings, help, product information, version/license notices, and a draft privacy policy.
Android also provides sound on/off and five CPU speed settings.
See the [Player guide](docs/guides/PLAYER_PROGRAM_IO.md) and [audio/speed verification record](docs/development/PLAYER_AUDIO_SPEED_PROGRESS.md),
both currently in Japanese, for machine-specific behavior and limits.
Project, Assembler, Debugger, WAV tools, and save states are outside the current Player scope.

### Load BASIC text

After booting a ROM, load a UTF-8 `.bas` file. The tokenizer normally converts it directly into intermediate code,
allowing lines longer than the original interactive input limit and escaped special characters.
Input through the ROM's own editor is also available as a compatibility option.
Character encodings, keywords, and special characters vary by machine;
see [BASIC text format (Japanese)](docs/reference/BASIC_TEXT_FORMAT.md).

### WAV conversion and analysis (Studio only)

[Step-by-step guide and hardware verification checklist (Japanese)](docs/guides/WAV_TOOL_GUIDE.md).

Open `Tools` → `WAV Tool` to convert OLD BASIC (02/12), OLD Binary (62), S1 BASIC (07/17), S2 BASIC (27/37), and S1/S2 Binary (67).
Encode an existing `.bas`, or a `.dmp` / Intel HEX (`.hex` / `.ihx`) containing one contiguous region, or choose `Create WAV…` from the project tree.
After `Convert`, inspect the result and use `Save WAV…`.
For recordings, use `Open WAV…` → `Analyze / reanalyze`, then export validated BASIC/`.dmp`, raw bytes,
or an analysis report separately. PCM/raw data can still be inspected when payload validation fails.
The waveform view provides scales, zoom, horizontal navigation, and a difference-threshold display.

Select S1 or S2 for Binary 67; validated results can be saved as `.dmp` or Intel HEX.
WAV Encode uses explicit BASIC without password / BASIC with password / machine-code selection (S2: BASIC only). Input extensions are checked against the selected kind.

For S1/S2 BASIC, enter a printable ASCII transfer name. Names longer than seven characters are shortened, with a note in the report.
There is no target-machine picker. Project launch selects the initial dialect and BASIC/binary kind from the project machine and file; these selections remain editable. Project information is used only for the initial encode selection; validated BASIC can be saved as `.bas`.
S2 supports Japanese CP932 text and line references, with format-wide vocabulary, without individual machine restrictions.
Type 27 is the default; the protected BASIC option selects type 37 without setting a password string. The new S1/S2 codec has passed synthetic tests; hardware round trips are unverified.
The tool does not integrate with ROM CSAVE/CLOAD or directly load decoded data into the emulator.
See [WAV operations and scope (Japanese)](docs/development/WAV_OPERATIONS.md).

### Create a ROM set

Open `File` → `Create ROM Set…`, select the machine, internal/external ROM components,
destination folder, and filename, then choose `Create`.
For the machines described here, internal ROMs are 8 KiB. External ROM sizes are:

- PC-1245/1250/1251/1255: 16 KiB.
- PC-1261/1350/1401: 32 KiB.
- PC-1360/1360K: 128 KiB, concatenated in bank 0–7 order.

The tool validates the inputs and creates a `.pgrom` v1 package with a manifest and SHA-256 hashes.
Use `Register in ROM Library` afterward, or register it later through `Manage ROMs…`.
Keep source ROMs and generated packages outside Git, for example under `local-data/`.
Existing Pokecom GO 32/64 KiB images can also be registered as compatible legacy inputs.
See the [ROM set creation guide (Japanese)](docs/guides/GETTING_STARTED.md#43-romダンプからpgromを作る).

### Build and test

```bash
./gradlew build
```

The first build downloads Gradle, Kotlin/Native, Compose, and other dependencies. It may take several minutes
and use hundreds of megabytes or more. Later builds reuse the local cache.

Run Core tests:

```bash
./gradlew :core:desktopTest
```

PC-1245 and PC-1251 real-ROM smoke tests automatically detect the default `local-data` paths.
To use another location, provide an absolute or repository-relative path:

```bash
PGP_PC1245_ROM=/path/to/pc1245mem.bin \
  ./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.RealPc1245RomSmokeTest

PGP_PC1251_ROM=/path/to/pc1251mem.bin \
  ./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.RealPc1251RomSmokeTest
```

These tests are skipped when their ROMs are unavailable. Do not add ROMs to CI or Git.

Replay redistributable golden JSON scenarios:

```bash
./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.GoldenScenarioRunnerTest
```

Explicitly export a local golden snapshot from a supplied ROM:

```bash
PGP_EXPORT_GOLDEN=1 \
PGP_PC1245_ROM=/path/to/pc1245mem.bin \
PGP_GOLDEN_PRODUCER_REVISION=<git-revision> \
  ./gradlew :core:desktopTest \
  --tests com.digihori.pgp.core.integration.GoldenSnapshotExporterTest.exportsLocallySuppliedRomOnlyWhenExplicitlyRequested
```

The output includes the ROM SHA-256 and is replay-validated immediately. ROMs and generated JSON remain
under `local-data/`, outside Git.

### CI and preview packages

Pushes and pull requests run Core desktop tests and Studio builds/tests on macOS, Windows, and Linux.
A separate Android job runs shared Player and Android-specific tests and builds the APK.
CI uses redistributable synthetic data, without ROM images.
iOS app builds and Swift unit tests run in regular CI; Native-only tests use a manual workflow,
and UI interactions are verified locally.

Running `.github/workflows/release.yml` manually creates Studio and Android Player packages,
retained as workflow artifacts for 14 days. Release tags such as `v0.7.0-alpha.1` use those packages
to publish a GitHub prerelease. Verify release candidates before creating tags.
See [Building from source (Japanese)](docs/reference/BUILDING.md) for local DMG/MSI/DEB packaging.

## Known limitations

- You must supply the appropriate ROMs; none are bundled.
- Windows defaults to software rendering because some button hover effects were corrupted with Skiko
  Direct3D/OpenGL when running the x64 build under Windows ARM64 in VMware on Apple Silicon.
  Native Windows x64 verification is needed before reconsidering this default.
  Use `PGP_RENDER_API=DIRECT3D` or `OPENGL` to compare renderers.
- Linux x86_64 tar.gz launch, audio, and DEB installation/launch/removal have been checked.
  ARM64 Linux packages are unavailable; the DEB menu entry currently appears under “Other.”
- ROM import guidance and bulk selection of banked ROM components remain provisional.
- ROM-driven BASIC input retains the original per-line input limit; use the tokenizer for longer lines.
- Sound may differ from the hardware or break up with some programs. The Japanese README records Windows
  clicking after sound playback; a later [follow-up record (Japanese)](docs/development/planning/TASK_REVIEW.md) reports improvement
  confirmed on October 13, with detailed checks still outstanding.
- The PC-1245/1250 mirror behavior near `0xb000..0xbfff` needs further verification.
- Android and iPhone Player support the 14 models above. Verification details remain machine- and device-specific; see the Android/iOS acceptance records. Save states are not implemented. The iPhone privacy policy is available in [Japanese](docs/guides/PLAYER_PRIVACY_POLICY_IOS.md) and [English](docs/guides/PLAYER_PRIVACY_POLICY_IOS.en.md), with public HTML pages linked from [Player setup](docs/player/index.html).

See the [documentation index](docs/README.en.md#releases) for versioned release notes and historical checklists.

## Feedback

Please report bugs and feature requests through [GitHub Issues](https://github.com/digihori/pokecom-go-plus/issues).
Do not attach ROM images, secrets, or data you do not have permission to redistribute.

## Documentation

Use the [documentation index](docs/README.en.md) to find guides, technical references, releases, and development records.
Most detailed documents are in Japanese; the index provides English navigation.

- [Getting started and ROM registration](docs/guides/GETTING_STARTED.md).
- [Projects and Build & Load](docs/guides/PROJECTS.md), [assembler](docs/guides/SC61860_ASSEMBLY.md), [Debugger](docs/guides/DEBUGGER.md).
- [WAV Tool](docs/guides/WAV_TOOL_GUIDE.md), [AI connection](docs/guides/AI_CONNECTION_GUIDE.md).
- [Android Player](docs/guides/PLAYER_HELP.md), [iPhone Player](docs/guides/PLAYER_HELP_IOS.md).
- [Standalone tools](tools/README.md).
- [Building from source](docs/reference/BUILDING.md).
- [Releases](docs/README.en.md#releases), [development records and initial designs](docs/README.en.md#development-records-and-initial-designs).

## License

This project is released under the [MIT License](LICENSE).
The provenance and rights of code, specifications, and images ported from Pokecom GO and other sources
are reviewed and recorded in [PORTING_NOTES.md](docs/development/PORTING_NOTES.md).

WAV BASIC conversion uses the upper format specification rather than individual machine restrictions. Decode automatically detects the validated format; it does not require format or machine selection.
