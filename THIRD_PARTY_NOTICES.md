# Third-Party Notices

Pokecom GO Plus is distributed under the MIT License in `LICENSE`.
It also uses or refers to the following third-party software.

## PC-1251 Emulator

- Project: [woriguchi/pc1251-emulator](https://github.com/woriguchi/pc1251-emulator)
- Reference revision: `6ff6fa9b807925215d753f6a5d940a1290d43089`
- Used for: SC61860 C-port audio timing and PC-1251 memory/LCD behavior
- License: MIT License

```text
MIT License

Copyright (c) 2026 origuchi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Runtime and build dependencies

The runtime dependency graph was reviewed from Gradle metadata on 2026-10-05.
The following principal components and their transitive component families are licensed under
the Apache License 2.0:

- Kotlin standard library and Kotlin Gradle plugins
- kotlinx.serialization, kotlinx.coroutines, kotlinx.datetime, and AtomicFU
- Compose Multiplatform and Material / Material 3
- AndroidX Collection, Annotation, Lifecycle, SavedState, and NavigationEvent components
- Skiko and JetBrains Runtime API
- JetBrains Annotations and JSpecify

The Gradle Wrapper is also distributed under the Apache License 2.0.
Binary distributions must retain the license and notice files supplied by these dependencies.
The authoritative license metadata packaged with each dependency takes precedence over this summary.

Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>

## Pokecom GO

Pokecom GO is a reference implementation by the same copyright holder as Pokecom GO Plus.
Its CPU, machine, keyboard, display, and BASIC behavior was restructured for the PGP architecture.
Per-feature source revisions and design decisions are recorded in `docs/PORTING_NOTES.md`.
Pokecom GO is not a runtime or build dependency of this repository.

## pcwav

pcwav is a reference implementation by the same copyright holder as Pokecom GO Plus.
It has been consulted when designing future WAV and transfer support. No pcwav source code is
currently included in Pokecom GO Plus, and it is not a runtime or build dependency.
