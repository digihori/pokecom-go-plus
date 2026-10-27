# Documentation

[English](README.en.md) | [日本語](README.md) | [Project overview](../README.md)

This index describes the development source tree. For a downloaded build, check the
[release notes](#releases) for that version. Most detailed guides are in **Japanese**;
English link labels below do not indicate that the linked documents have been translated.
Documents now live in category folders. Files at older paths are compatibility navigation pages;
use the documents linked from this index for updates and new references.

## Start here

Player has matching [English](https://digihori.github.io/pokecom-go-plus/player/en.html) and [Japanese](https://digihori.github.io/pokecom-go-plus/player/ja.html) setup pages.
See [hosting and maintenance notes](player/README.md) for the source files.

1. Choose a build from [GitHub Releases](https://github.com/digihori/pokecom-go-plus/releases).
2. For Studio, follow [Getting started](guides/GETTING_STARTED.md) to launch the app, select a machine, and register ROMs.
3. For Player, use the [Android](guides/PLAYER_HELP.md) or [iPhone](guides/PLAYER_HELP_IOS.md) help.

ROM images are not included. Supply ROMs you have lawfully obtained.
For setup errors, see [Troubleshooting](guides/GETTING_STARTED.md#7-よくあるエラー).
For a source build, see [Building](reference/BUILDING.md). [About Studio](guides/PLAYER_STUDIO_GUIDE.md) explains the product roles.

## Studio

| Task | Guide |
| --- | --- |
| Projects, external editors, Build & Load, command history | [Projects](guides/PROJECTS.md) |
| Assembly Workspace, Quick Assemble, instructions and data | [SC61860 assembler](guides/SC61860_ASSEMBLY.md) |
| SC61, YASM61860 and other adapters | [External assemblers](guides/EXTERNAL_ASSEMBLER.md) |
| Execution, breakpoints, watches, trace and source mapping | [Debugger](guides/DEBUGGER.md) |
| Connect an external AI client | [AI connection guide](guides/AI_CONNECTION_GUIDE.md) |
| Create cassette WAVs and recover recorded programs | [WAV Tool](guides/WAV_TOOL_GUIDE.md) |
| Edit dot patterns | [Character Editor](guides/CHARACTER_EDITOR.md) |

## Player

- [Android help](guides/PLAYER_HELP.md) and [BASIC/DMP file operations](guides/PLAYER_PROGRAM_IO.md).
- [iPhone help](guides/PLAYER_HELP_IOS.md): machines, ROMs, Files, audio and external input.
- Privacy: [Android policy](guides/PLAYER_PRIVACY_POLICY.md) and [iPhone policy (Japanese)](guides/PLAYER_PRIVACY_POLICY_IOS.md) and [English](guides/PLAYER_PRIVACY_POLICY_IOS.en.md).
  Check the policy text and the release description for applicability.

The [iOS development README](../iosApp/README.md) covers building and verification.
Source-tree machine support does not establish availability of a signed iOS release.

## Standalone tools and ROM extraction

These optional tools require an interpreter supplied by the user. Studio does not launch them.

- [Tool index and requirements](../tools/README.md).
- [Raw binary/DMP conversion, Intel HEX export, and inspection](../tools/memory/README.md).
- [pcwav: BASIC/binary and WAV conversion](../tools/pcwav/README.md).
- [PC-1360K kanji ROM extraction](../tools/pc1360k-rom-dump/README.md).

The bundled hardware extraction programs cover the PC-1360K kanji ROM only.
General internal/external ROM extraction for other machines remains future work.

## Technical references

| Topic | Reference |
| --- | --- |
| BASIC encoding and escapes | [BASIC text format](reference/BASIC_TEXT_FORMAT.md) |
| Addressed machine-code data | [DMP, Intel HEX and raw binary policy](reference/MACHINE_CODE_FORMATS.md) |
| ROM packages | [pgrom format](reference/ROM_PACKAGE.md) |
| WAV formats and scope | [OLD](reference/WAV_OLD_FORMAT.md), [S1/S2](reference/WAV_S1_S2_FORMAT.md), [operations policy](development/WAV_OPERATIONS.md) |
| AI API, approvals and limits | [AI integration specification](reference/AI_INTEGRATION.md) |
| Machine generations | [Machine families](reference/MACHINE_FAMILIES.md) |
| Building and testing | [Building](reference/BUILDING.md), [Golden data](reference/GOLDEN_TEST_DATA.md), [fixtures](../test-data/README.md) |
| Source generators | [Development scripts](../scripts/README.md) |

Machine references:
[PC-1245](reference/machines/PC-1245.md), [PC-126x](reference/machines/PC-126x.md), [PC-1350](reference/machines/PC-1350.md),
[PC-1360](reference/machines/PC-1360.md), [PC-1360K](reference/machines/PC-1360K.md), [PC-1401 family](reference/machines/PC-1401.md),
[PC-1450](reference/machines/PC-1450.md), [PC-1460](reference/machines/PC-1460.md), [PC-1470U](reference/machines/PC-1470U.md).
[PC-1245/125x/126x RAM sizes and mirrors](reference/machines/RAM_1245_1261.md).
Read the verification limits and distinguish current specifications from implementation history.

## Releases

[Downloads and published releases](https://github.com/digihori/pokecom-go-plus/releases).
Each note describes its tag rather than the current development tree.

| Tag | Notes |
| --- | --- |
| 1.0.0 (in preparation) | [Release notes](releases/notes/RELEASE_NOTES_1.0.0.md) · [Checklist](releases/checklists/RELEASE_CHECKLIST_1.0.0.md) |
| v0.7.0-alpha.1 | [Release notes](releases/notes/RELEASE_NOTES_0.7.0-alpha.1.md) · [Checklist](releases/checklists/RELEASE_CHECKLIST_0.7.0-alpha.1.md) |
| v0.6.0-alpha.2 | [Technical Preview 0.6 alpha 2](releases/notes/RELEASE_NOTES_0.6.0-alpha.2.md) |
| v0.6.0-alpha.1 | [Technical Preview 0.6](releases/notes/RELEASE_NOTES_0.6.0-alpha.1.md) |
| v0.5.0-alpha.1 | [Technical Preview 0.5](releases/notes/RELEASE_NOTES_0.5.0-alpha.1.md) |
| v0.4.0-alpha.1 | [Technical Preview 0.4](releases/notes/RELEASE_NOTES_0.4.0-alpha.1.md) |
| v0.3.0-alpha.1 | [Technical Preview 0.3](releases/notes/RELEASE_NOTES_0.3.0-alpha.1.md) |
| v0.2.0-alpha.1 | [Technical Preview 0.2](releases/notes/RELEASE_NOTES_0.2.0-alpha.1.md) |
| v0.1.0-alpha.1 | [Technical Preview 0.1](releases/notes/RELEASE_NOTES_0.1.0-alpha.1.md) |

Licensing: [LICENSE](../LICENSE), [third-party notices](../THIRD_PARTY_NOTICES.md).

## Development records and initial designs

These documents are for developers and investigators. Dated statements about missing features,
pending checks, and test counts describe their recorded stage; they are not current release guarantees.

- Direction and follow-up: [Formal release MUST/WANT](development/planning/FORMAL_RELEASE_TASKS.md), [Roadmap](development/planning/ROADMAP.md), [task review](development/planning/TASK_REVIEW.md), [debugger follow-up](development/planning/DEBUGGER_FOLLOW_UP.md).
- Initial proposals: [project concept](development/PGP_CONCEPT.md), [architecture](reference/ARCHITECTURE.md), [initial Core API contract](reference/CORE_API.md).
- [Porting decisions and provenance](development/PORTING_NOTES.md).
- Separated records: [Player file operations](development/PLAYER_PROGRAM_IO_HISTORY.md), [Debugger](development/DEBUGGER_DESIGN_HISTORY.md), [AI](development/AI_INTEGRATION_HISTORY.md), [external assemblers](development/EXTERNAL_ASSEMBLER_VERIFICATION.md), [Character Editor](development/CHARACTER_EDITOR_DESIGN.md).
- WAV history: [operations](development/WAV_OPERATIONS_HISTORY.md), [S1/S2 fixtures and stages](development/WAV_S1_S2_HISTORY.md), [progress](development/WAV_S1_S2_PROGRESS.md).
- Machine research: [PC-1360K](analysis/pc1360k/PC-1360K_HISTORY.md), [individual experiments](analysis/pc1360k/README.md), [PC-1401](development/PC-1401_HISTORY.md).
- Mobile acceptance summaries: [Android series](development/ANDROID_SERIES_PROGRESS.md), [iOS machines](development/IOS_MACHINE_SUPPORT_STEP8.md).
- Historical release checklists for maintainers: [0.1](releases/checklists/PUBLIC_RELEASE_CHECKLIST.md), [0.2](releases/checklists/RELEASE_CHECKLIST_0.2.0-alpha.1.md), [0.3](releases/checklists/RELEASE_CHECKLIST_0.3.0-alpha.1.md), [0.4](releases/checklists/RELEASE_CHECKLIST_0.4.0-alpha.1.md), [0.5](releases/checklists/RELEASE_CHECKLIST_0.5.0-alpha.1.md), [0.6](releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.1.md), [0.6 alpha 2](releases/checklists/RELEASE_CHECKLIST_0.6.0-alpha.2.md).
- Complete inventory: [original 124 documents and proposed layout](development/DOCUMENTATION_ORGANIZATION.md), [documents separated or added in step 3](development/DOCUMENTATION_STEP3.md).

The inventory links to the remaining Android/iOS stage records, localization work, and verification reports.
