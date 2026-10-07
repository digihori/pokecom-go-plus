# Pokecom GO Studio Technical Preview 0.2

Git version: `v0.2.0-alpha.1`

Technical Preview 0.2 expands Pokecom GO Studio from the initial PC-1245／1251-family preview into a
multi-machine development and analysis environment. This remains an early preview intended for testing and
feedback, not a stable release.

## Highlights

- PC-1350 and PC-1360 emulation, including their LCD and machine-specific keyboards
- PC-1360 16 KiB × 8 banked ROM and S2 BASIC support
- Application-managed ROM Library with automatic restoration and machine switching
- Reorganized Studio UI with menu bar, compact emulator view, machine selector, status, and notifications
- Independent Project Files and Debugger windows
- Multiple project source types with shared Build & Load
- Assembly Workspace for external-editor workflows
- SC61860 Assembly diagnostics, Memory and Disassembly previews, Listing, and Symbol Map
- `build/program.dmp`, `build/program.lst`, and `build/program.map` generation
- Debugger CPU differences, breakpoints, run to cursor, memory view/watch, trace, and checkpoints
- Initial design documentation for future AI／MCP-assisted development and debugging

## Project and Assembly workflow

New projects can create any combination of BASIC, Memory Dump, and Assembly starter sources. External files
under `src/` are detected without silently rewriting the manifest and can be added explicitly. Assembly
Workspace uses the same assembler pipeline as Build & Load, retains the last successful preview after an
error, and shows source-aware diagnostics.

The generated Assembly starter demonstrates `ORG`, labels, immediate and address operands, relative branches,
and `DB`. Quick Assemble remains available for converting one standalone source file.

## Supported machines

- PC-1245
- PC-1250／PC-1251／PC-1255 shared family ROM
- PC-1350
- PC-1360

ROM images are not included. Users must provide images lawfully obtained from their own machines.

## Platform status

- macOS: the supported machines and the principal Studio workflows have been tested during development.
- Windows x64: automated tests and packaging run in GitHub Actions. Earlier preview packages were also tested
  under Windows ARM64 x64 emulation; a complete native Windows x64 manual pass remains pending.
- Linux x86_64: automated tests and DEB／tar.gz packaging run in GitHub Actions. Earlier preview packages were
  tested for install, launch, audio, and removal; a complete current UI pass remains pending.
- Android／iOS: Pokecom GO Player has not been implemented yet.

The desktop packages are unsigned and the macOS package is not notarized, so operating-system security
warnings may be shown. Each platform includes checksums plus the project license and third-party notices.

## Known limitations

- Assembly Workspace is its first usable iteration; its layout and operation will continue to be refined.
- An in-app External Editor setting is not available yet. `PGP_EDITOR` can provide an executable and
  `{file}`／`{line}` argument template; otherwise Studio uses the operating system's text-edit action.
- The assembler supports labels, `ORG`, `DB`, and currently encodable SC61860 instructions, but not arithmetic
  expressions, constants, macros, include files, or conditional assembly yet.
- Project format version 1 still assigns one target machine to the whole project. Machine-independent BASIC
  and compatible PC-1245／1250／1251／1255 project ranges are planned but not represented yet.
- Physical PC-keyboard input is currently logical-text oriented. A physical-key mode suitable for games,
  including modifier-only input, is planned.
- BRK input responsiveness while some machine-code programs are running requires further investigation.
- ROM import guidance and banked-machine workflows remain provisional.
- Save-state compatibility and stable public APIs are not guaranteed between Technical Preview releases.
- Linux ARM64 packages are not available.

## Feedback

Report reproducible defects and feature requests through GitHub Issues. Include the host OS, selected machine,
input format, and reproduction steps, but do not include ROMs, credentials, personal information, or other
non-redistributable data.

## Licensing and provenance

Pokecom GO Plus is released under the MIT License. Third-party notices are recorded in
`THIRD_PARTY_NOTICES.md`, and implementation provenance is recorded in `docs/PORTING_NOTES.md`.
