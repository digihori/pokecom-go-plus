# Player public setup pages

These dependency-free HTML pages provide matching Japanese and English Android
setup instructions and separate Japanese/English iOS privacy policies. Open `index.html` locally, or serve this directory with any
static web server. They do not change the app's bundled information screens.

## Pages

- `index.html`: explicit Japanese/English language choice; no automatic redirect.
- `ja.html` / `en.html`: equivalent setup guides.
- `privacy-ja.html` / `privacy-en.html`: finalized Android privacy policies (effective October 22, 2026).
- `privacy-ios-ja.html` / `privacy-ios-en.html`: iOS privacy policies (effective October 25, 2026).
- `style.css`: shared responsive layout, including horizontally scrollable tables.

Update both languages together. The policy's canonical Japanese source remains
`../guides/PLAYER_PRIVACY_POLICY.md`; keep its HTML copy and English translation
consistent when changing it. The developer is digihori (デジホリ工房); privacy inquiries
use public GitHub Issues. Finalization of the text does not publish these pages: verify
the live policy URLs before entering them in Play Console.

## GitHub Pages publication

The site uses branch-based GitHub Pages hosting from `main` → `/docs`.
`docs/.nojekyll` keeps this a static deployment without Markdown/Jekyll processing.
Existing HTML is served without extra dependencies.
`docs/index.html` provides the site entry point. Check the deployment's actual URL
and all language/privacy links before adding URLs to the app or Play Console.

Public page addresses:

- `https://digihori.github.io/pokecom-go-plus/player/`
- `https://digihori.github.io/pokecom-go-plus/player/ja.html`
- `https://digihori.github.io/pokecom-go-plus/player/en.html`
- `https://digihori.github.io/pokecom-go-plus/player/privacy-ja.html`
- `https://digihori.github.io/pokecom-go-plus/player/privacy-en.html`

Check the Pages deployment status after pushing changes.
Before release, verify that a publicly downloadable Studio build supports all
machines and ROM package creation steps described by the guide. The guide points
to release notes rather than assuming every development feature is published.

## iOS privacy policy

The Japanese and English sources are `../guides/PLAYER_PRIVACY_POLICY_IOS.md` and
`../guides/PLAYER_PRIVACY_POLICY_IOS.en.md`. Keep the two HTML pages and Markdown
sources aligned. The app bundles the Japanese source through `iosApp/build-shared.sh`;
create a new Archive after changing it so the submitted app contains the finalized text.
The iOS policy covers local ROM/settings storage, file providers, audio/input, file sharing,
ROM Library backup exclusion, deletion, and the public GitHub Issues contact channel.
Android policy files and their effective date remain separate.

After these changes reach `main` and Pages deployment succeeds, verify both addresses:

- `https://digihori.github.io/pokecom-go-plus/player/privacy-ios-ja.html`
- `https://digihori.github.io/pokecom-go-plus/player/privacy-ios-en.html`

Creating the files locally does not publish them. Do not enter these addresses in
App Store Connect until the live pages can be opened without signing in.
