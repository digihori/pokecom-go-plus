# Pokecom GO Plus

[English](README.md) | [日本語](README.ja.md)

[![CI](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml/badge.svg)](https://github.com/digihori/pokecom-go-plus/actions/workflows/ci.yml)

> **[Technical Preview 0.6 (alpha 2)](https://github.com/digihori/pokecom-go-plus/releases/tag/v0.6.0-alpha.2)** — An early preview with PC-1360K kanji support, ROM extraction tools, and Android Player updates.

Pokecom GO Plus (PGP) is a multiplatform development environment for vintage pocket computers.
It aims to help you write, run, and analyze programs on a modern PC, then transfer them to the original hardware.

PGP consists of two products and a shared emulator core:

- **Pokecom GO Studio** — A pocket computer development environment for macOS, Windows, and Linux.
- **Pokecom GO Player** — An emulator focused on running programs on Android and iOS.
- **PGP Emulator Core** — The Kotlin Multiplatform core shared by Studio and Player.

“Pokecom GO Plus” and “PGP” are development code names for the project as a whole.
Future AI integration aims to expose Studio's Project, Assembler, and Debugger through localhost MCP,
supporting BASIC development, machine-code analysis, and debugging with explicit approval for changes.

## Current status

The project is in early development. Studio implements PC-1245, the PC-1250/1251/1255 family,
and PC-1261/1350/1401. PC-1360/1360K support includes booting user-supplied ROMs, eight 16 KiB ROM banks,
LCD output, keyboard input, and S2 BASIC. Registering a kanji ROM enables kana input and kanji conversion on PC-1360K.

On macOS, testing with actual ROMs covers booting, physical and on-screen keyboards, BASIC and machine-code
file operations, LCD output, and sound. Manual checks on Windows cover ROM booting, keyboard input, BASIC,
and sound. Linux x86_64 checks cover ROM booting, basic operation, sound, and tar.gz/DEB packages.
These checks do not establish complete compatibility across every machine, ROM revision, or peripheral.

The PC-1245 Android Player scope was user-confirmed on October 12, 2026. PC-1401 reached its agreed milestone
on October 15; see its [verification record (Japanese)](docs/ANDROID_PC1401_PROGRESS.md).
PC-1350 reached its milestone on [October 16 (Japanese)](docs/ANDROID_PC1350_PROGRESS.md).
These are dated milestones; development in the source tree may extend beyond the published preview.

The iPhone app supports PC-1245, PC-1250/1251/1255, PC-1260/1261, PC-1350, PC-1401/1402,
PC-1450, PC-1360/1360K, PC-1460, and PC-1470U through the shared Player implementation.
It implements the home screen, ROM management, BASIC/DMP operations, audio and speed settings,
GameController support, and physical keyboard input. Current machine regression checks use Simulator
and original synthetic ROMs; the historical PC-1245 actual-ROM check is recorded separately.
Environment-specific checks for physical devices, real file providers, peripherals, and the minimum iOS 16 runtime
remain separately tracked. See the [iPhone guide and verification records (Japanese)](iosApp/README.md).
The user confirmed operation across the integrated models, sound playback, and the updated home screen on October 21, 2026, completing this scope. Audio roughness remains a follow-up. The [home presentation update (Japanese)](docs/IOS_HOME_PRESENTATION_FIX.md) adds sorted tiles/list layouts and restricts landscape orientation to the emulator screen.
A signed iOS release is not yet available.

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

The detailed [getting started guide](docs/GETTING_STARTED.md) is currently in Japanese.

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
also loads them into the emulator. See the [SC61860 assembler guide (Japanese)](docs/SC61860_ASSEMBLY.md)
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
The Android milestones and iPhone limitations are described above. iOS physical-device acceptance and save states remain future work.

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

See [Building from source (Japanese)](docs/BUILDING.md) for complete setup, platform-specific packaging,
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

### Build Android Player

With Android SDK 36 and JDK 21 configured:

```bash
./gradlew :androidApp:assembleDebug
```

Output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
Import a `.pgrom` package or a supported legacy ROM for the selected machine; ROMs are stored in private app storage.
Banked machines may require a Studio-created `.pgrom`; see the machine-specific documentation.
ROMs are restored to the home screen at startup, and the CPU runs automatically on first play and after Reset.

Player features include machine-specific skins and LCD symbols, landscape layouts, notch/navigation-area
handling, manual Controller Display, hierarchical menus, ROM replacement/deletion, gamepad mappings,
and physical keyboard input. PC-1245 has 52 skin keys and a 16-character LCD; other machines have their own layouts.

Program loading detects `.bas`/`.dmp` by extension. The documented file operations include BASIC export
and selected RAM-range export where supported. Successful loading resumes the CPU; failure or cancellation leaves it paused.
The app includes settings, help, product information, version/license notices, and a draft privacy policy.
Android also provides sound on/off and five CPU speed settings.
See the [Player guide](docs/PLAYER_PROGRAM_IO.md) and [audio/speed verification record](docs/PLAYER_AUDIO_SPEED_PROGRESS.md),
both currently in Japanese, for machine-specific behavior and limits.
Project, Assembler, Debugger, WAV tools, and save states are outside the current Player scope.

### Load BASIC text

After booting a ROM, load a UTF-8 `.bas` file. The tokenizer normally converts it directly into intermediate code,
allowing lines longer than the original interactive input limit and escaped special characters.
Input through the ROM's own editor is also available as a compatibility option.
Character encodings, keywords, and special characters vary by machine;
see [BASIC text format (Japanese)](docs/BASIC_TEXT_FORMAT.md).

### WAV conversion and analysis (Studio only)

[Step-by-step guide and hardware verification checklist (Japanese)](docs/WAV_TOOL_GUIDE.md).

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
See [WAV operations and scope (Japanese)](docs/WAV_OPERATIONS.md).

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
See the [ROM set creation guide (Japanese)](docs/GETTING_STARTED.md#43-romダンプからpgromを作る).

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
retained as workflow artifacts for 14 days. Release tags such as `v0.6.0-alpha.2` use those packages
to publish a GitHub prerelease. Verify release candidates before creating tags.
See [Building from source (Japanese)](docs/BUILDING.md) for local DMG/MSI/DEB packaging.

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
  clicking after sound playback; a later [follow-up record (Japanese)](docs/TASK_REVIEW.md) reports improvement
  confirmed on October 13, with detailed checks still outstanding.
- The PC-1245/1250 mirror behavior near `0xb000..0xbfff` needs further verification.
- Player verification is machine- and device-specific. iOS physical-device and actual-ROM acceptance for the expanded machine set,
  and save states remain future work. The bundled privacy policy is a draft.

See the [preview checklist (Japanese)](docs/PUBLIC_RELEASE_CHECKLIST.md) for recorded release checks.

## Feedback

Please report bugs and feature requests through [GitHub Issues](https://github.com/digihori/pokecom-go-plus/issues).
Do not attach ROM images, secrets, or data you do not have permission to redistribute.

## Documentation

Most detailed documentation is currently in **Japanese**. The links below retain the existing documents;
their translation will be handled separately. See the [Japanese README](README.ja.md) for the original overview.

- [Getting started](docs/GETTING_STARTED.md)
- [Building from source](docs/BUILDING.md)
- [Project concept](docs/PGP_CONCEPT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Emulator Core public API](docs/CORE_API.md)
- [Debugger](docs/DEBUGGER.md)
- [AI integration](docs/AI_INTEGRATION.md)
- [SC61860 machine families](docs/MACHINE_FAMILIES.md)
- [PC-1245 machine definition](docs/machines/PC-1245.md)
- [PC-1350 machine definition](docs/machines/PC-1350.md)
- [PC-1360 machine definition](docs/machines/PC-1360.md)
- [PC-1360K and kanji ROM](docs/machines/PC-1360K.md)
- [PC-1360K kanji ROM extraction tools](tools/pc1360k-rom-dump/README.md)
- [PGP ROM package format](docs/ROM_PACKAGE.md)
- [Golden test data format](docs/GOLDEN_TEST_DATA.md)
- [External editors and project format](docs/PROJECTS.md)
- [SC61860 assembler/disassembler](docs/SC61860_ASSEMBLY.md)
- [WAV operations and scope](docs/WAV_OPERATIONS.md)
- [OLD WAV format](docs/WAV_OLD_FORMAT.md)
- [S1/S2 WAV specification and author confirmation](docs/WAV_S1_S2_FORMAT.md)
- [iPhone Player operation, building, and verification](iosApp/README.md)
- [Standalone utility tools (binary/DMP conversion and ROM extraction; Japanese guide)](tools/README.md)
- [Roadmap](docs/ROADMAP.md)
- [Technical Preview checklist](docs/PUBLIC_RELEASE_CHECKLIST.md)
- [Technical Preview 0.6 alpha 2 release notes](docs/RELEASE_NOTES_0.6.0-alpha.2.md)
- [Technical Preview 0.6 release notes](docs/RELEASE_NOTES_0.6.0-alpha.1.md)
- [Technical Preview 0.5 release notes](docs/RELEASE_NOTES_0.5.0-alpha.1.md)
- [Technical Preview 0.4 release notes](docs/RELEASE_NOTES_0.4.0-alpha.1.md)
- [Technical Preview 0.3 release notes](docs/RELEASE_NOTES_0.3.0-alpha.1.md)
- [Technical Preview 0.2 release notes](docs/RELEASE_NOTES_0.2.0-alpha.1.md)
- [Technical Preview 0.1 release notes](docs/RELEASE_NOTES_0.1.0-alpha.1.md)
- [Porting notes](docs/PORTING_NOTES.md)
- [Third-party software notices](THIRD_PARTY_NOTICES.md)

## License

This project is released under the [MIT License](LICENSE).
The provenance and rights of code, specifications, and images ported from Pokecom GO and other sources
are reviewed and recorded in [PORTING_NOTES.md](docs/PORTING_NOTES.md).

WAV BASIC conversion uses the upper format specification rather than individual machine restrictions. Decode automatically detects the validated format; it does not require format or machine selection.
