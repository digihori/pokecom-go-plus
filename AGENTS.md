# Repository roles

- `pokecom-go-plus` is the active PGP development repository.
- `../pokecom` is the read-only Pokecom GO reference implementation.
- `../pcwav` is the read-only WAV codec reference implementation.
- Do not modify either reference repository unless explicitly requested.
- Do not create build-time dependencies on the reference repositories.
- Port behavior and algorithms into PGP deliberately; do not copy
  platform-specific architecture.
- Record source provenance and porting decisions in
  `docs/PORTING_NOTES.md`.

# Architecture rules

- Keep `core/src/commonMain` free of JVM, Android, Apple, UI, filesystem,
  wall-clock, and device APIs.
- The emulator core must not create or own threads.
- Keep mutable emulator state inside an emulator session; do not introduce
  global mutable state.
- Expose immutable snapshots and explicit host requests across the core/UI
  boundary.
- Add or update tests when porting behavior from a reference repository.
- Do not add ROM images or other copyrighted firmware to the repository.

# Build and verification

- Use JDK 21.
- Run all checks with `./gradlew build`.
- Run the core tests with `./gradlew :core:desktopTest`.
- Run the desktop application with `./gradlew :desktopApp:run`.
