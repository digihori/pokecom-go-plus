# Pokecom GO application icon

Studio (desktop) uses the soft silver icon. Player (Android/iOS) uses a higher-contrast
slate variant with a yellow upper-left key and red upper-right key.
The Player RGB master is `androidApp/icons/pokecom-go-player.png` (repository-relative).
`pokecom-go.png` is the 1024×1024 RGBA master; keep it when regenerating platform sizes.
The illustration was generated with the built-in ImageGen tool and selected by the
project owner. It depicts a nearly square light silver pocket computer with muted blue trim, a two-line “Pokecom GO”
LCD, and two rows of matching blank keys, with a transparent exterior.

- Desktop packaging uses the PNG (Linux), multi-size ICO (Windows), and ICNS (macOS).
- The mounted macOS DMG volume uses the distinct disk artwork `pokecom-go-dmg.icns` (PGP / DMG), applied by
  `scripts/set-dmg-volume-icon.sh` after jpackage creates the image.
- Desktop windows use the 256×256 copy in `src/main/resources/pokecom-go.png`.
- Android uses density-specific legacy icons and an adaptive icon with an inset
  illustration to keep the device inside launcher masks.
- Android foreground and legacy icons use the Player artwork with a pale yellow-green background.
- iOS uses the opaque 1024×1024 image in `Assets.xcassets/AppIcon.appiconset`.

Platform files are resized/converted from the master without redrawing the artwork.

The DMG icon is original vector artwork defined in `scripts/generate-dmg-icon.swift`.
Regenerate on macOS with `swift scripts/generate-dmg-icon.swift desktopApp/icons`.
This changes the mounted disk icon; the application inside the disk keeps its application icon.
