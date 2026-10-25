# Porting Notes

## 2026-10-19: Android PC-1261 foundation / BASIC-only save

- Source: 既存PGP Core pc1261とStudio Pc1261KeyboardLayout。配置済みAndroid画像を確認。
  LCD上下シンボル帯の参考として旧Pokecom GO MainLoop1261.javaを参照するが、ホスト構造・依存は移植しない。
- 共有52キーと24桁×2行LCD、9シンボルを接続。ユーザー図で配置を調整し、その後PC-1261だけをホーム・ROM decoderへ接続。
- ユーザー方針でAndroid全機種のセーブをBASICだけへ統一し、形式選択・アドレス入力・DMP保存分岐を削除。
  DMPロード、Core／StudioのダンプAPI、iOSの機能は維持する。
- 検証区分: [PC-1261対応記録](ANDROID_PC1261_PROGRESS.md)。ROM／ROM由来データはコミットしない。


## 2026-10-19: Android PC-1251 Player foundation

- Source: 既存PGP Coreのpc1251とStudioのPc1251KeyboardLayout。外部参照プロジェクトからの追加移植・依存なし。
- 共有PlayerへOLD系52キーの凡例差分と配置済みAndroidスキンの暫定座標を接続。LCD／symbol座標はユーザー確認待ち。
- PlayerのPC-1251だけ既存EmulatorConfigurationのHARDWAREを指定し、RAM拡張を有効にしない。Core／Studio既定設定は維持。
- 初期基盤では非選択を維持。その後PC-1251だけをホーム・ROM decoderへ接続し、Debug専用の合成／入力ホストで通常Library／設定を使わずに確認する。
- タイトルバーのRUN／PROは1245／1251／1261を対象とするユーザー指示を反映。1261自体のPlayer選択はこの段階では有効化しない。
- 詳細と検証区分: [Android PC-1251](ANDROID_PC1251_PROGRESS.md)。ROM／ROM由来データは追加しない。

## 2026-10-14: PC-1470U ↕ key recheck

- ユーザーの「キーの取り違えでは」という指摘を受け、STAT（SHIFT＋「=」）と通常↕／SHIFT TITLEを分けてローカルROMで再検証。
- 前回の空入力中心の確認ではbit2の機能を判定できず、入力を無効にしていた。今回はRUNでA=123、CALで123+456を入力後に確認し、IA bit0選択時のbit2が反応することを確認。他の未使用scan bitは同条件で画面変化もSHIFT消費もない。
- PocketKey.TITLEを物理↕キーとしてbit2へ接続する。SHIFTを自動付加しない。通常↕はCALの579表示を456表示へ変更し、別セッションで456を直接評価したsnapshotと一致。RUNでは通常／SHIFTとも入力受付状態へ移行しSHIFTを消費する。
- 途中で通常／SHIFTのpromptが異なると目視判断したが、pixel比較テストで同じと判明し訂正。TITLE機能固有の操作条件・結果は引き続き未確認。キー入力配線の確認とTITLE機能全体の確認を区別する。
- SHIFT＋「=」でSTAT symbolがON／OFFすることは実ROMで確認済み。独立STATキーの推測追加はしない。
- 合成scanテストとStudio全表示キー受入れを更新。ローカルROMテストは期待glyphのROM由来fixtureを保存せず、入力結果のセッション間比較とsymbolで判定する。探索用の一時probeテストは削除し、ROM・描画結果画像はGitへ追加しない。
- 先の「↕キー入力未対応」は本修正で解消。TITLE機能の確認とUnicode矢印の絵文字表示は後続課題として残す。
- Verification: JDK 21で `build :core:desktopTest` 成功。ローカルROM・合成scan・Studio接続・共有Native testsを通過。
- User decision: TITLE機能はユーザーも使用経験がなく、取説にも記載を見つけられないため、詳細確認をペンディングにする。追加調査は止め、確認済みの↕入力とSHIFT＋「=」のSTAT切替は維持する。


## 2026-10-14: 126x / 140x capacity families

- User policy: 1250/1251/1255と同じ考え方で、機能仕様はシリーズ共通、モデル差はユーザーメモリ容量。126x/125xのexternal ROM複数版、1401/1402のexternal ROM差は版差として受け入れる。BASICロード位置は容量により異なりROM版による差とはしない。
- User correction: 1262は1261の価格改定版なので独立ラインナップから外す。RAMミラーはミラーしていない可能性もあるため追加を保留。今回の追加IDはpc-1260とpc-1402。
- Hardware values: 1260=RAM5800..67ff／BASIC5880、1261=4000..67ff／4080、1401=3800..47ff／3800、1402=2000..47ff／2000。start/end pointerは126x=66e1..66e4、140x=46e1..46e4。既存writer上限は維持し、固定変数・work領域を含む最大ユーザー容量を新たに確定しない。
- References: [PC-1260/61/62の実機メモリマップ](https://ht-deko.com/pc1260/)、[1260のRAM交換記録](https://ht-deko.com/pc1260/pc1260_repair.html)、[1401のRAM交換実験](https://hw-by-design.blogspot.com/2018/07/sharp-pc-1401-memory-mod.html)、[MAME pocketc.cpp](https://github.com/mamedev/mame/blob/master/src/mame/sharp/pocketc.cpp)のpc1260_mem/pc1261_mem/pc1401_mem/pc1402_mem（2026-10-14閲覧）。既存Pokecom GOのhook差はROM版を拒否する根拠にはしない。資料にないRAM aliasは実装しない。
- Implementation: Pc1261FamilyModel／Pc1401FamilyModelをCatalog・Factory・Bus・BASIC writer/plannerへ接続。CPU/LCD/キー/codecをシリーズ内で共用し、機種IDは個別。既知hash allowlist・ROM address hook・外部build依存なし。commonMainのplatform API／thread追加なし。
- Studio: component/legacy/package作成・読込みで選択モデルを保持。シリーズROM登録は125x同様に共用し、既存pc-1261／pc-1401のLibrary pathを維持。package hashは整合性検査のみ。別シリーズROMは拒否。1260にもRUN/PRO/RSVボタンを表示する。
- Tests: 合成ROMで版byte差を受入れ、モデルidentity、BASIC開始／pointer／往復／容量超過の原子性、RAM端・範囲外・reset・セッション分離を確認。Studioでcomponent/legacy/package／シリーズ共用／旧Library path／キー／別シリーズ拒否を検証する。
- Local ROM: ignored local-dataの1261 internal/externalを1260/1261の両容量で使い、NEW後pointerとBASIC POKE実行を確認。1401 ROMは1401を確認。手元の1401 ROMを1402配置へ渡すとNEW後も3800となり、全ROM版が異なる容量を自動認識するとは確認できない。1402用ROMは未提供で実ROM検証を残す。ユーザーROMをpatchしたり既知版に限定したりしない。
- Scope: 旧文書の「1260/1262をcapacity-onlyと扱わない」「1402対応保留」は今回のユーザー方針で更新。Player新機種選択／skin、Project機種互換方針、Expanded RAM、1350/1460等のメモリーカード拡張は別項目。既存125x挙動、作成中skin、packageVersion1.3.1を維持。ROM／ROM由来fixtureはGitへ追加しない。
- Verification: JDK 21で `./gradlew build :core:desktopTest` 成功。Core desktop 434件、Studio 223件（既存skip 1）、共有Native tests成功。`:desktopApp:run`は一時homeとメモリ内PreferencesFactoryで実行後に手動停止し、実設定・Libraryは変更しない。検証用起動scriptのclasspath/files参照エラーは一時script内で修正。GUIでの新機種操作・Windows・1402用ROMのローカルBASIC実行は未確認。
- User follow-up: 1260/1261のMEM値は容量に応じて異なることをユーザー確認。1401 ROM共用時は1401/1402で同じMEM値になり、1402 ROMに替えると1401選択時に1401のMEM容量になったとの報告。1402選択時のMEM値／BASIC実行まで確認済みとは扱わない。全版の互換性の保証とは区別する。ROM登録メニューは125xと同様にPC-1261 family／PC-1401 familyの1行ずつへ集約し、削除確認も同じ名称にする。実行機種選択とCreate ROM Setの機種指定は個別のまま。
- 詳細: [126x](../reference/machines/PC-126x.md)、[140x](../reference/machines/PC-1401.md)。


## 2026-10-13: PC-1450 → PC-1460 → PC-1470U Core / Studio

- Baseline: main `1700fe0`。開始時は未追跡の作成中スキン3枚のみ。作業途中の追加スキンも保護。
- PGP files: `core/.../machine/pc14xx`、Catalog / Factory / MemoryLayout / ProgramImageLoader / BasicProgramCodec、
  Studio ROM importer/package creator、キー・LCD presentation、機種別合成テストとoptional local ROM tests。
- Primary reference: 同一作者Pokecom GO `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`。
  `Sc61860_1450/1460/1470.java`（メモリ／bank／INA/INB）、`KeyBoard1460/1470.java`、
  `MainLoop1450/1460/1470.java`（dot wiring / symbol / pitch）、`SubActivity1450/1460/1470.java`
  （BASIC pointers / token table）、README、対応キー刻印画像。READMEは1470クラスの対象を1470Uと明記。
- Supplementary reference: PCWAV `14e1ef53596f0d8791b6d14965061f63846d11a7` のS1/S2 Encode/Decode / TextCodec。
  行形式を照合する。汎用token tableを機種固有の全語彙と扱わず、1450/1460と1470Uの表を分ける。
- Independent cross-check: [PockEmul Keyb1450.h](https://github.com/matsumo/PockEmul/blob/8c07fd1ff027e7ee69056b5d8273afbbf14037a8/src/core/Keyb1450.h)
  （最終file commit `8c07fd1ff027e7ee69056b5d8273afbbf14037a8`）、pc1450.cpp。
  GOの1450キー表にないF↔E／STATのscan位置を照合。[PC-1450実機メモリ観測](https://ht-deko.com/pc1450/)
  の16 KiB card領域も参照。メーカー回路図PDFは今回の取得／表示に失敗し、新たな確認済み根拠には数えない。
- Provenance/license: GO/PCWAVは既存の同一作者参照方針を継続（GOに独立LICENSEファイルなし）。
  仕様・token表を抽出し、PGP既存CPU / S1/S2行処理 / buzzer / host ownershipを使って再実装。
  PockEmulはGPL v2の独立照合資料であり、ソースコードをコピーしない。
  参照checkoutはread-only、外部projectのbuild/runtime依存なし。ROM・ROM由来fixtureを追加しない。
- Design: 1401のメモリ／キー／LCDを1450へ流用しない。1450/1460/1470Uの確認済み共通部分だけを
  `Pc14xxModel`とセッション内Bus/Keyboard/Displayへ集約。旧機種のmemory decodeは維持。
  commonMainにOS/UI/filesystem/time/thread APIなし。3機種は別familyでROM取り違えを拒否。
- Banking: 1460=`3c00` lower 2 bits / 4×16 KiB、1470U=`3400..35ff` lower 3 bits / 8×16 KiB、
  ともに窓`4000..7fff`。bankイベント、debugger physical ROM location、resetとROM保護を接続。
  legacyはmem＋順番通りのbank file、canonical pgromはbank単位のcomponents/hash。
- LCD: 1450=16×1、1460=24×1、1470U=24×2、各5×7。機種別に順序／反転／status registerを定義。
  Studioの1460 dot pitch横:縦=3:4はMainLoop1460に基づくpresentation値。
  1450/1470Uは1:1、Coreの論理dot数／行間と分離。実機寸法の精密測定とは区別する。
- Keys/modes: 1450/1460のmemory selectorはbit mask、1470Uは1-based group番号。
  BRKは保持中KEY ON＋既存短tap latch。1470UはDEL/BS/INS、2位置SHIFT、通常AER / SHIFT CALを刻印で照合。
  AERをOperatingModeとして区別し、CALはSHIFT→AER key。モードqueueのhold/gapを新機種の設定に合わせる。
  Studioの重複SHIFTに独立所有者を与え、1470Uのhost Backspace/DeleteをBS/DELへ接続。
- BASIC: 1450/1460=S1専用語彙・FE kana、1470U=S2専用語彙・FE token / 1F line reference / 1-byte kana。
  1360 codecはtoken table引数を追加して再利用。DEFDBL A等の書出し時の語境界も修正。
  load/save、Project build/loadと原子的なplacement検査へ接続。1460/1470U writerのpointer手前上限は
  ソフトウェア境界であり、実機の変数／work領域を含む最大容量は未確認。
- RAM policy: 1450固定16 KiB card、1460固定32 KiB card、1470Uは参照の32 KiB互換窓。
  広いcontrol窓を全て物理RAMと断定しない。1470のコメントアウトされたRAM/LCD mirrorは採用しない。
  内蔵RAM＋card差／FO RAM bank／容量選択／空き領域拡張は将来項目で、今回推測追加しない。
- Local firmware: ignored local-dataの3機種ROMで起動／モード切替／直接BASIC load、FACT(5)とPOKEで120を確認。
  1460/1470Uの冷RAM起動はMEMORY ALL CLEAR O.K.?へY/ENTERをテストから入力。
  Studioへ自動確認キーを追加しない。ローカルROM結果と合成fixture／実機／ユーザー操作を分ける。
- Scope: 新機種はCore/Studioのみ。Player skin/catalog選択へ追加しない。
  Player仮文言は2026-10-13ユーザーの追加指示で先延ばし。
  Quick Assemble、他Assembler、Project互換、ローカライズ、RAM拡張は今回の追加依頼に含めない。
  packageVersion 1.3.1、公開タグ／履歴を維持。公開配布物の更新は行わない。
- Machine-specific mapping and limitations: [PC-1450](../reference/machines/PC-1450.md)、[PC-1460](../reference/machines/PC-1460.md)、[PC-1470U](../reference/machines/PC-1470U.md)。
- Initial verification（2026-10-13。10-14の最終状態は末尾参照）: JDK 21 Core Desktop 428、Studio 218（既存skip 1）、hostInput Desktop 10、
  iOS Simulator Native Core 398 / hostInput 10 / Player 23、全てfailure/error 0。
  Core/Studio/hostInput tests、Native testsとmacOS `createDistributable`は成功。
  `./gradlew build :core:desktopTest`も実行したが、作業中追加された未追跡`skin_pc1360K.png`を
  Android resource mergerが大文字Kで拒否し、最終全体buildは停止。このファイルの変更／rename／除外はしていない。
  開始時3枚に加え1360/1360K/1401の作成中スキンもcommit対象外。`git diff --check`成功。
- macOS Studio manual: 一時Libraryに準備したcanonical packagesから1450→1460→1470Uを切替、
  LCDと機種固有function panel／キーcaptionを目視。新機種のcaptionはCanvasで実測text layoutを
  fitし、旧機種のText描画を維持。全3機種でhostキーから7+3=10、1470Uは画面SHIFT→AER keyでCALを確認。
  1470Uの計算結果は下行に表示。冷RAMの確認キーY/ENTERは操作者として入力した。
  package importer/library整合性は自動テスト。今回のnative file pickerはOpenがdisabledとなり登録操作を
  完了できなかったため、picker経由の手動import成功とは記録しない。
- QA isolation: 初回のJava prefs root指定はmacOSで分離できず、最近のROMパスが一時Libraryへ更新された。
  元のPC-1360登録packageとのbyte一致を確認して元の登録先へ復旧。以降は検証専用のメモリ内PreferencesFactoryと
  一時Libraryを使い、終了時に生成appのcfg/classpathを復旧。検証helperはrepositoryへ追加していない。
  Windows／実機／最大BASIC容量／実ゲームパッドは未確認。

## 2026-10-12: iOS PC-1245 ROM vertical slice — items 7–8

- User prioritized seeing real ROM boot over completing the menu hierarchy. Added provisional
  machine/ROM and information menu, version and policy/Studio placeholders; other menu work remains.
- Swift Foundation document picker/storage, CryptoKit SHA-256 and system zlib own platform I/O.
  Bounded ZIP store/deflate reader also supports streaming data descriptors and verifies CRC,
  local/central headers, entry overlap, duplicate/unsafe paths, symlinks, encryption and expansion limits.
  No archive extraction or third-party source copy. Current Android archive behavior and Core
  RomPackageValidator/Pc1245FlatRomImporter/PlayerSessionFactory are the repository provenance.
- Apple Facade validates package metadata/hash/component sizes through existing shared Core and
  prepares a Session. Swift serial worker owns file/archive/storage operations; MainActor owns Native
  Session access. Core/commonMain still has no platform APIs or threads. Only PC-1245 is selectable.
- Canonical package plus source metadata are one atomic record under Application Support/rom-library,
  excluded from backup before writing. New Session is published only after save succeeds. Replacement
  failure/cancellation preserves old ROM/Session; confirmed deletion removes the registration.
  Import/replacement/restore success automatically starts CPU on the next active frame. It does not
  type BASIC RUN. Ordinary background return, failed/cancelled picker remain paused; RAM is not restored.
- Normal launch now uses saved real ROM or the missing-ROM screen. Original checkerboard probe is
  DEBUG-only with --structural-probe; UI tests explicitly opt in. Picker starts in app Documents.
- Verification: Swift host 9 tests (import/restore/replace/cancel/delete, hash/CRC/machine/path failures,
  atomic write failure preservation and deflate descriptor), UI 2, shared Native 23. JDK 21 full build
  and core:desktopTest passed: Core 408, Studio 195 (1 skipped), shared desktop Player 20, Android 9.
  iPhone 12/iOS 26.5 Files picker imported the existing local PC-1245 ROM and auto-ran CPU;
  manual touch calculation 7+3 displayed 10. After host tests terminated the app, normal launch
  restored saved ROM and auto-ran CPU; the same calculation passed again. Final host 9 tests and
  unsigned generic arm64 iPhone build passed. No ROM or ROM-derived tracked fixture added.
- iOS 16 runtime, physical devices, signing and distribution remain unverified. bas/dmp and the
  Android BASIC-success auto-resume fix remain item 9. No commit/push/release performed.

## 2026-10-12: PC-1245 iOS skin, touch and display — items 5–6

- Reused current PGP Android-authored `skin_pc1245.png` byte-for-byte (1206×616), shared
  PlayerSkinCatalog/SkinLayoutEngine/PlayerDisplayFrame and HostKeyState. No external source copied.
  UIKit owns image loading, rendering, native touch/cancellation, glyph measurement and accessibility.
- Skin image, LCD panel/dots/symbols/mode, key highlights and accessibility rectangles share one
  placement and inverse coordinate transform. SwiftUI respects safe area at the root, including the
  temporary operation toolbar. Native layout receives that viewport and does not subtract insets twice.
  Both landscape directions retain the session/display choice and release inputs; geometry and safe
  inset changes also release inputs. Portrait remains excluded by the existing iPhone host configuration.
- LCD uses (70,104,760,70), panel (55,60,787,158), centered 80% dots/20% gaps, fitted fixed-size
  glyphs independent of Dynamic Type, left BUSY/P/DEF, adjacent centered DEGRAD and right-aligned SHIFT.
  RUN/PRO machine mode is displayed independently of RUNNING/PAUSED/FAULTED CPU state.
- All 52 keys receive independent touch owner tokens. Moving outside the original rectangle,
  cancellation, backgrounding, reset/mode/display changes release inputs. Multiple fingers holding
  the same key use shared ownership, so one release cannot release the other. Press highlight/fade and
  native accessibility activation are supported. VoiceOver activation holds its own token briefly
  to allow a running CPU frame to scan it; it never owns Core scheduling.
- Controller Display uses the shared crop, suppresses screen touch/accessibility keys and clears
  existing input before toggling. It is manual; no gamepad detection or automatic display change added.
  The direct operation toolbar is temporary until item 7's hierarchy; large-text actions use named
  icons to keep controls identifiable without truncating words.
- Simulator synthetic program now generates a checkerboard across all 80 LCD columns and lights
  status segments through LIDP/LIA/STD, followed by a NOP/jump loop. This is original verification code,
  not firmware or ROM-derived data. Real ROM/keyboard-scan/BASIC behavior remains for ROM management.
- Verification: shared Native Player 23 tests (including all-key ownership and generated display),
  Swift host 6 tests (including drag/cancel/same-key ownership/crop), UI 2 tests on SE3 and iPhone 12.
  Normal and Controller views, repeated manual toggles, both landscape directions, portrait restriction,
  Run/Pause/Reset and background return passed. iPhone 12 maximum Dynamic Type was tested and restored
  to its original large setting. Full screenshots were visually inspected; no ROM bytes were used.
- JDK 21 `build :core:desktopTest` passed (578 tasks, 26 executed). Core 408, Studio 195 (1 skipped),
  shared desktop Player 20, shared Native 23, Android 9; no failures. Unsigned generic arm64 iPhone
  build passed. These do not constitute iOS 16 runtime/physical-device/real-ROM verification.
- Platform references: Apple [multiple touch](https://developer.apple.com/documentation/uikit/uiview/ismultipletouchenabled),
  [touch cancellation](https://developer.apple.com/documentation/uikit/handling-touches-in-your-view),
  [safe area changes](https://developer.apple.com/documentation/uikit/uiview/safeareainsetsdidchange()),
  and prior host/framework sources in IOS_PLAYER_PLAN.md. No commit/push/release performed.

## 2026-10-12: iOS host and shared bridge — implementation items 1–4

- User approved SwiftUI/UIKit, iPhone only, minimum iOS 16, Simulator-only acceptance for
  architecture validation. iPad, Developer enrollment, signing Team and physical devices are later.
- Added `iosApp/PokecomGOPlayer.xcodeproj` with a shared scheme, empty Team, development Bundle ID
  `com.digihori.pgp.player.ios`, both landscape orientations and no multiple scenes. App build and
  test are explicit Xcode operations; Gradle `build` does not compile the Swift host.
- `playerShared` exports one static PgpPlayer framework for arm64 device/Simulator and an iosMain
  ApplePlayerFacade. Swift uses facade methods and immutable display snapshots. Core remains
  unchanged; no platform/thread/time dependency added to commonMain. UIKit touch ownership reuses
  HostKeyState. A tiny original NOP/jump probe is generated for structure verification, not firmware.
- Swift MainActor owns all session calls, CADisplayLink supplies monotonic frame time. Inactive/
  background invalidates display link, pauses and releases input; ordinary active return stays paused.
  Reset restores RUN mode and schedules CPU start at the first active frame. Unconsumed automatic
  starts survive backgrounding; explicit Pause cancels them. Display-link references avoid a retain cycle.
- Temporary Simulator screen exposes Run/Pause/Reset/PRO and a BREAK hold bridge with a copied LCD.
  Real-device startup is ROM-missing. Skin/52 keys, full presentation/menu, ROM storage, program I/O,
  BASIC auto-resume alignment and gamepad remain subsequent items; they were not implemented here.
- JDK 21 integration script rejects other JVM versions. arm64-only KMP configuration excludes
  Intel Simulator architecture explicitly in Xcode rather than adding an unrequested target.
- Verification: Native Player Simulator 22 tests (20 existing + 2 facade tests), Swift host 4 tests,
  UI smoke 1 test on iPhone 17 Pro, SE3 and iPhone 12 Simulators (runtime 26.5). Final SE3 and iPhone 12
  tests passed; host checks cover input release, ordinary resume, deferred Reset start, cancellation
  and no background catch-up. UI verifies real CADisplayLink run, pause, Reset, both landscape
  directions, portrait restriction and Home/activate return paused. Early rapid-rotation UI checks
  raced UIKit animations; UI tests now wait for rotation settling before the next gesture.
  UI attachments use XCUIScreen full-display capture to avoid app-crop artifacts when device
  orientation differs from the supported app interface; direct simctl display capture was also inspected.
- Full JDK 21 `build :core:desktopTest` passed (578 tasks, 34 executed). Reports: Core 408,
  Studio 195 (1 skipped), shared desktop Player 20, shared Native Player 22, Android 9; no failures.
  Unsigned generic iPhone arm64 Xcode build also passed. This is build verification, not device
  installation/operation. iOS 16 runtime and real ROM operation were not verified.
- Sources: current PGP implementation and Apple CADisplayLink/scenePhase/UIDevice orientation
  documentation, JetBrains Apple-framework/direct-integration documentation (linked in the plan).
  No external code copied, ROM/ROM-derived data added, commit/push/release performed.

## 2026-10-12: PC-1245 iOS Player investigation and proposed design

- Investigation only; approval of the implementation approach is pending. See
  [IOS_PLAYER_PLAN.md](IOS_PLAYER_PLAN.md) for environment, ownership boundaries, operations,
  implementation order and separate Simulator/device acceptance conditions.
- Sources: current PGP PlayerSession/Runner/program I/O/skin/input models, Android host,
  ROM library/package/importer and tests; Phase 7 and Player/architecture/format/release documents.
  Apple and JetBrains platform references are linked in the plan. No external code copied.
- Existing Apple targets build shared klibs/tests; there is no iOS host/Xcode project or exported
  framework. Proposed SwiftUI/UIKit host reuses KMP logic and keeps device/storage/time APIs in host.
- Xcode 26.6, SDK/runtime 26.5 and arm64 iPhone/iPad Simulators are available. No connected device
  or valid signing identity was found; Team, Bundle ID and minimum OS remain to be decided.
- Ordinary background return remains paused. Only unconsumed successful-operation automatic-start
  requests survive backgrounding. Proposed follow-up aligns BASIC and DMP success CPU resume through
  shared policy and both hosts; this does not type BASIC RUN. Failed/cancelled operations stay paused.
- Specified JDK 21 `build :core:desktopTest` succeeded (573 tasks; 16 executed, 557 up-to-date).
  Shared Apple compile/Simulator tests were up-to-date; no iOS app or physical-device validation done.
- ROM/ROM-derived fixtures were not added. No commit, push or release performed.

## 2026-10-11: Assembler basic syntax stage 3

- Implemented a PGP-native expression AST/parser and immutable EQU symbols. No external code copied.
- Preserved numeric spellings and label behavior. Chose `.` for statement location to avoid conflicting
  with hexadecimal `$`. EQU captures the definition location, and resolved constants are memoized.
- Layout directives require resolved values; forward numeric constants work, but unresolved forward
  labels cannot drive ORG/DS/ALIGN. Cycles, excessive complexity and signed-32-bit overflow are diagnosed.
- DW emits high byte first, matching SC61860 address operands. DS/ALIGN emit real fill bytes (default zero),
  so existing placement, alias and BASIC overlap validation applies without a separate reservation model.
- DB strings and character literals explicitly encode byte-valued characters, without host or machine
  character conversion. Quoted comments/separators and common byte escapes are supported.
- Both semicolon and hash start comments outside quotes, including body and inline comments;
  Studio retains its separate consecutive leading-hash documentation header behavior.
- Added synthetic expression/data/layout and Studio listing/map/placement tests. Macro, include/link,
  mutable SET, bank output and debugger source integration remain outside this stage.

## 2026-10-11: Assembler correctness and placement stages 1–2

- Source: PGP CPU/bus/BASIC writers and the audit below. No external implementation copied.
- Corrected TEST immediate metadata. Assembly export preserves alternate NOP and undocumented
  opcode bytes using DB; debugger mnemonics remain unchanged. Bounded export checks length before
  reading operands. Relative encoding uses 16-bit wrap, and malformed DB/label boundaries are rejected.
- MachineMemoryLayout describes existing PGP write destinations, including mirrors and the
  PC-1251 family RAM mode. It does not revise PC-1245's compatibility bus or claim physical RAM accuracy.
- Studio preview/build and Session memory loading share placement validation. Template origins
  depend on the model, and combined starters use distinct locations.
- ProgramImageLoader reuses existing BASIC writers with a recording sink, validates planned text,
  pointer writes and old-text clearing against machine code, then loads one validated memory image.
  Build assumes initial pointers; Load replans against actual paused Session memory.
- Tests use synthetic zero-filled ROM components and independently specified instruction expectations;
  no ROM-derived validation data is added. Undocumented opcode semantics and bank output remain deferred.

## 2026-10-11: SC61860 assembler audit (no behavior port)

- Audited main / 2890cd5 assembler, decoder, CPU, Studio build/load paths and tests.
- Supplementary reference: https://github.com/utz82/SC61860-Instruction-Set
  (accessed 2026-10-11; already used by earlier CPU port notes).
- Confirmed TEST immediate metadata omission, lossy NOP/UNDOC assembly export,
  and segment-end decoder overread with temporary synthetic Core tests.
- No external implementation was copied. Undocumented opcode naming/behavior
  differences remain subject to a separate decision; do not infer hardware accuracy
  from shared assembler/decoder metadata alone.
- Findings and proposed implementation order: `ASSEMBLER_AUDIT.md`.

この文書は、既存プロジェクトからPGPへ移植・再実装した技術の由来と判断を記録する。

## 参照リポジトリ

| 参照元 | 用途 | 扱い |
|---|---|---|
| Pokecom GO | CPU、機種定義、LCD、キー、BASIC処理 | 読み取り専用 |
| pcwav | 同一作者によるWAV Codec、転送プロトコルの参照実装 | 読み取り専用 |
| [PC-1251 Emulator](https://github.com/woriguchi/pc1251-emulator) | サウンド生成、PC-1251メモリ・LCD仕様 | MIT License、読み取り専用 |

PGPはこれらのリポジトリへビルド時または実行時に依存しない。

## 記録ルール

移植単位ごとに以下を記録する。

- PGP側の機能・クラス名
- 参照元リポジトリとコミットID
- 参照したファイルと範囲
- 流用した仕様またはアルゴリズム
- PGP向けに変更した設計
- 動作一致を確認するテスト
- ライセンス・由来の確認結果

## 記録テンプレート

```markdown
### 機能名

- Date:
- PGP files:
- Reference repository:
- Reference commit:
- Reference files:
- Provenance/license:
- Reused behavior:
- Design changes:
- Verification:
```

## 移植記録

以下には仕様抽出と、PGP向けに再設計・実装した機能の由来を記録する。

### PC-1401実装前調査と設計案

- Date: 2026-10-11
- PGP baseline: `main` / `eeb247e`、開始時clean。今回コード移植・機種登録は未実施
- PGP files: `docs/reference/machines/PC-1401.md`、本記録
- Reference repositories: Pokecom GO `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`、
  PCWAV `14e1ef53596f0d8791b6d14965061f63846d11a7`（両方読み取り専用）
- Reference files: Pokecom GOの1401/1402 CPU subclass、Keyboard、MainLoop、SubActivity、
  共通CPU / BASIC処理。PCWAVのS1 BASIC / transfer codec。
  別参照はMAMEの`pocketc.cpp` / `pc1401.cpp` / `pc1401_m.cpp`と
  [SHARP PC 1401/02回路図scan](https://pockemul.com/Documents/lm61860.pdf) PDF 119ページ目。
  MAME対象driverの最終commitは`774a180df2b6ef9c53e48337792256817c50f768`。
- Provenance/license: 同一作者の参照から機種仕様を抽出。MAMEはGPL-2.0+であり
  コードコピーは行わず、独立照合資料として使う。メーカー回路図の転載・ROM登録は行わない
- Findings: SC61860A08、内部8 KiB / 外部32 KiB ROM、4 KiB RAM、16×1 LCD、
  IA/IB matrix、CAL / RUN / PRO、S1行構造と専用関数語彙。
  物理RAM候補は`3800..47ff`、LCD apertureとmirrorは参照間で差があり検証が必要
- Decision: CPUとROM layout / S1行処理 / host境界を共用し、memory decode、LCD、
  keyboard / mode、BASIC語彙 / program memoryは1401専用とする。
  Pokecom GOの広いRAM窓やROM内CLOAD / CSAVE / BEEP hookは移植しない。
  PC-1402は別ROM hookとMAMEの共通ROM定義が不一致のため、容量違いだけと断定しない
- Design boundaries: CoreにOS / UI / file / wall-clock / device APIやthread所有を追加しない。
  ROMとROM由来検証結果はignored local-dataに限定する。機種固有値を既存機種へ逆流させない
- UI decision (2026-10-11): ユーザーとPC-1401初版の縦配置
  （LCD → 関数キー6列×3行 → 主キー）を合意。現在の幅基準のサイズ追従を維持する。
  主キーと関数キーのgridを分離し、配置情報はCore外のpresentation層に置く。
  LCD右側配置は初版対象外。Playerの多キー・大型LCD機種向け拡張領域
  （通常はLCD＋主キー、トリガーで関数領域を表示し入力後に収納する案）は後続検討とする。
  トリガーと収納条件は未確定であり、今回実装しない
- Verification: ROM componentとflat内の対応領域の一致、ignore設定をローカル確認。
  PC-1401起動は未実施。実装段階のsynthetic test、実ROM / 実機、Studio / Player検証計画、
  出典の限界と未確定事項は[PC-1401調査文書](../reference/machines/PC-1401.md)に記載
  指定JDK 21で全体buildと`:core:desktopTest`成功（既存testは主にUP-TO-DATE）、diff check成功

### マシン語ファイル形式の統一方針

- Date: 2026-10-08
- Scope: Studio、Project、Assembler、Debugger、AI解析、WAV Toolで扱うマシン語image
- Decision: PGP `.dmp`を内部・Project・標準出力形式、Intel HEXを外部交換形式とする。両形式はaddress付き
  data領域の集合へ正規化してから各機能へ渡す
- Raw Binary: 通常sourceにはせず、互換import時に開始addressを一度だけ指定して`.dmp`相当へ変換する。
  既存Projectの`raw-binary`は移行期間の後方互換入力として維持する
- OLD WAV: Binary formatは単一の開始addressと連続bodyだけを表せるため、`.dmp`／Intel HEX入力が複数領域または
  gapを含む場合は拒否する。暗黙のzero-fillや巨大な連結imageは生成しない
- Boundary: file parser／writerとRaw Binary import UIはDesktop、アドレス付きmemory imageと連続領域の検証は
  共通logicに置き、Emulator SessionやWAV codecへfile pathを渡さない

### OLD系WAV転送仕様

- Final checkpoint 2026-10-12: Studioの統合OLD WAV Tool、二つのEncode入力経路、結果確認/保存分離、
  Raw簡素表示、波形axes/navigation/difference overlay、OLD/S1/S2 selectorとpcwav利用案内をまとめる。
  S1/S2実装はPlayer作業を優先して後回し。READMEと操作文書を現在の形式選択・表示色に合わせた。
  最終コードの指定JDK 21 `build :core:desktopTest :desktopApp:createDistributable`成功、
  Studio 195件（失敗0、skip1）、Core 408件（失敗0）。既にup-to-dateの検証結果も含む。
  ユーザーによるOLD実機往復と二入力経路確認は下記参照。直近の細かなUI変更の全GUI再確認と
  物理トラックパッドの手動確認は未実施。ROM/実機録音fixtureは追加しない。

- User hardware verification 2026-10-12: ユーザー報告により、BASIC sourceのEncode→実機ロード→
  実機セーブの録音WAV作成→PGP Decodeの往復が問題なく動作した。
  Encode入力は既存ファイル指定とProjectツリー指定の両経路を確認。
  対象機種、source長、録音機材/設定、入力WAV仕様の詳細は未記録であり、全OLD機種・録音条件の保証には拡張しない。
  今回fixtureの取得・commitは行っていない。合成試験/参照一致とは別の実機確認として記録する。

- Format selector 2026-10-12: ユーザー指示により機種4個のボタンをOLD/S1/S2形式選択へ変更。
  ProjectのbasicDialectを初期値にする。未対応S1/S2は明示し実行を無効化、OLDへの暗黙fallbackはしない。
  OLD共通token処理は内部の代表機種で呼び出す。機種別S1/S2語彙の選択は実装段階の後続設計。

- Raw display 2026-10-12: ユーザー指定によりRaw欄のbyteごとのLOW/HIGH同期sample一覧と説明を削除。
  offset付きhex数値表示は維持。同期情報の内部保持・波形marker・raw/PCM位置対応は変更しない。

- Difference overlay 2026-10-12: 差分表示でPCMを置換すると、4 kHz leaderが8 kHz入力の2-sample tapで
  差分0となり波形が消えたように見える。PCM（青、±32768）を常時表示し、差分（橙、±65536）と
  差分thresholdを重ねる方式へ修正。振幅guideは紫に分け、差分0区間の説明を表示。
  RawのLOW/HIGHは下位/上位nibble、sampleは0始まりの正規化PCM位置であることをUIに補足。

- UI defaults/layout 2026-10-12: 入力変更時は通常BASIC 02をdefaultとし、前入力のtype 12選択を持ち越さない。
  password文字列の指定機能は未提供とUIで明示。Zoom/左右移動/Fitを波形・水平scrollbarの下へ移動。

- Waveform axes/navigation 2026-10-12: `WavWaveform.kt`へ描画を分離し、生成結果のsample holdと
  WAV読込結果のsample接続を切替可能にした。0線、振幅/time目盛、min/max/DC、0交差marker、
  表示用振幅guide、Decoder差分threshold、水平scrollbar、横scroll/Shift+wheel panを追加。
  `OldSignalDecoder.inspect`が実際のDecoderと共通の差分計算・tap・thresholdを公開する。
  PCM・threshold・Decode方針は変更しない。新しい外部参照コード移植なし。
  viewport境界・zoom中心・時間目盛と、差分の初期状態/振幅範囲/不変コピーをtestで確認。
  指定JDK 21の全体build/Core test/createDistributable成功。Studio 195件（失敗0、skip1）、
  Core 408件（失敗0）。GUIで生成矩形波、合成16 kHzサイン波の0交差/振幅guide/目盛、
  水平scrollbar dragによる範囲移動を確認。実機録音fixtureと物理トラックパッドでの手動確認は未実施。
  横scrollは親の縦scrollより先に処理し、微小な移動量を蓄積する。`git diff --check`成功。

- Waveform display 2026-10-12: 拡大時のsample間の線形補間をsample値の保持＋境界の垂直edgeへ変更。
  合成矩形PCMをノコギリ状に見せないためのStudio描画修正。PCMデータ・codecは変更しない。

- Implementation 2026-10-12: `desktopApp/.../wav/DesktopWavOperation.kt`と`WavToolPanel.kt`へ
  Studio専用の操作・結果保持・保存・Analyzer最小版を分離し、`Main.kt`のTools/Projectツリーから接続。
  既存PGPのOLD codecのみを再利用し、新しい外部コード移植なし。参照commitは下記のまま。
  Coreの失敗result型は変更せず、Desktop orchestrationでPCM/rawを保存し、未検証結果にProgram保存を付けない。
  Player、ROM hook、Sessionロード経路は追加しない。処理のbackground実行/取消はDesktopが所有する。
  合成source、破損checksum、無信号、文字変換失敗、非OLD機種、Binary address/gapをDesktop testで検証。
  GUIでは既存BASIC→WAV変換/保存取消/保存、生成WAVのDecode、Projectツリー指定、raw↔波形移動、
  未保存Tool終了確認を操作確認。実機録音・S1/S2・詳細overlayは未対応。
  指定JDK 21の`build :core:desktopTest :desktopApp:createDistributable`成功。
  Studio 191件（失敗0、skip1）、Core 406件（失敗0、Coreはup-to-date）。`git diff --check`成功。

- Operation decision 2026-10-12: ユーザー指示によりEncode/Decode/Raw Decode/Analyzerを
  統合WAV Toolへまとめる。EncodeはProjectツリーからの指定と既存ファイル指定の両経路を用意する。
  エミュレータROMのCSAVE/CLOADとは連動しない。
  WAV Decodeからエミュレータへの直接ロードも対象外とし、結果確認・ファイル保存までで完結する。
  エミュレータへのロードは保存したファイルを既存の別操作で指定する。
  WAV機能とその起動導線はStudio専用とし、Playerには搭載しない。
  詳細は[WAV操作方針](WAV_OPERATIONS.md)。今回は操作方針の文書更新のみで移植・実装変更なし。

- Review 2026-10-11: `main / cf849bc`のWAVコードと既存構想を再照合し、
  [WAV機能の現状と段階案](WAV_REVIEW_2026-10-11.md)へ記録した。
  pcwav HEADは下記参照commitと一致。今回は移植・codec変更なし。
  中間結果保持は現統合APIでは成功時のみであり、Raw Decodeでは失敗以前の結果保持が必要。
  S1/S2参照動作を実機確定仕様と扱わず、OLD UI/Raw Decodeからの段階案は方針確認待ちとする。

- Date: 2026-10-08
- PGP files: `docs/reference/WAV_OLD_FORMAT.md`、`docs/reference/ARCHITECTURE.md`、`docs/development/planning/ROADMAP.md`、Desktop `Main.kt`、
  Core `wav/WavCodecModel.kt`、`wav/OldTransferChecksum.kt`、`wav/OldPayloadEncoder.kt`、
  `wav/OldPayloadDecoder.kt`、`wav/OldSignalEncoder.kt`、`wav/PcmWavWriter.kt`、
  `wav/PcmWavReader.kt`、`wav/OldSignalDecoder.kt`、`wav/OldWavCodec.kt`とtest
- Reference repository: pcwav
- Reference commit: `14e1ef53596f0d8791b6d14965061f63846d11a7`
- Reference files: `Format/Old.pm`、`Common.pm`、`WavWriter.pm`、`WavReader.pm`、
  `PcmNormalize.pm`、`RawDecode.pm`、OLD BASIC／Binary Encode／Decode、CLI entry points
- Provenance/license: 同一作者のpcwavを読み取り専用で参照。コードは移植せず、観測した形式仕様を再記述
- Reused behavior: type／filename／metadata layout、bodyのnibble swap、8-byte単位かつ80-data-byteごとに
  resetする累積checksum、OLD byte framing、8000 Hz 8-bit mono WAV出力、Decode入力範囲
- Design changes: Program、転送payload、信号、WAV containerを分離し、固定1000 ms skipと入力末尾依存の
  body終端を仕様化しない。Desktop file I/OとEmulator Sessionをcodecへ持ち込まない。raw／logical／PCMは
  コピー所有する不変型で区別し、将来のAnalyzer向けに座標空間付きrangeとmappingを共通model化する。
  公開BASIC payload Encoderは不完全なbodyを許容する参照側低レベルwrapperと異なり、末尾`F0`を検証する。
  Decoderは入力末尾を無条件にbodyへ含めず、BASIC `F0`またはBinary宣言長で停止して余剰rawをwarningにする。
  Signal Encoderはpayload解釈と分離し、任意raw列をPCM化できる入口も持つ。WAV codecはbyte列だけを扱い、
  filesystemをStudio adapterへ残す。ReaderはRIFF外の余剰byteをwarningとして保持する。Signal Decoderは固定の
  1000 ms skipを持たず、完成raw byteのPCM範囲とnibble同期位置を解析用に返す。統合APIは各codecを置換せず、
  中間結果と全段階のdiagnosticを保持して合成する薄いfacadeとする。Studioの最小UIだけがnative file dialogと
  filesystemを担当し、ProjectまたはEmulator SessionなしでBASIC／BinaryのEncodeとDecodeを実行する
- Verification: 参照commitからBASIC／Binary固定vectorを抽出し、参照library内でpayloadおよびPCM→rawの
  round tripを確認。共通modelの不変性、range、result invariantsをCore testで確認し、Desktop／Android／
  iOSを含む全体buildを通過。OLD checksumはname block、20-byte body、0／1／7／8／9／79／80／81／88-byte
  境界、80-byte reset、end-around carryを固定testで確認。BASIC／Binary payloadはpcwav固定vector、filename
  正規化、password type、address範囲、BASIC終端、Analyzer用body mappingを確認。Decoderは同じ境界長、header／
  body checksum位置、truncation、余剰data、未知filename byte、reserved metadata warningを確認。Signal Encoderは
  leader、`A5`のbit順、sample数式、raw-to-PCM mapping、`00` byteのpcwav波形を固定testで確認。WAVはcanonical
  header、末尾odd-length data、未知odd chunk、8-bit mono、16-bit stereo、truncation、非対応sample rateを確認。
  Signal DecoderはEncoder出力、16 kHz相当、先頭無音＋振幅低下、末尾欠損、無信号、および
  WAV Reader→Normalizer→Signal Decoder→Payload Decoderの一貫経路を確認。統合APIはBASIC、パスワード付き
  BASIC、Binaryのround trip、複数層warningの集約、WAV／信号段階の失敗伝播を確認。BASIC／Binaryの
  1／7／8／9／79／80／81／88-byte境界を完全なWAV経路でも往復し、raw／PCM／WAV長を固定式で確認

### S1／S2 WAV参照動作

- Author clarification 2026-10-12: pcwav作者であるユーザーから、S2 type `27`=BASIC、
  `37`=password付きBASIC、pcwav末尾checksum方式の動作実績と採用判断、S2 BinaryはS1と同じとの指定を受領。
  これをPGP実装仕様として採用し、S2 BinaryはS1 Binary `67`の共用対象とする。
  下記2026-10-09の資料待ち/未対応判断を更新した。参照commitの専用S2 Binary実装がない事実と、
  作者提供仕様は区別する。外部コード移植・PGP codec実装は今回はなし。
  更新先: `WAV_S1_S2_FORMAT.md`、`ROADMAP.md`、`ARCHITECTURE.md`、調査report追記。

- Date: 2026-10-09
- PGP files: `docs/reference/WAV_S1_S2_FORMAT.md`、`docs/reference/WAV_OLD_FORMAT.md`、`docs/reference/ARCHITECTURE.md`、
  `docs/development/planning/ROADMAP.md`
- Reference repository: pcwav
- Reference commit: `14e1ef53596f0d8791b6d14965061f63846d11a7`
- Reference files: `WavWriter.pm`、`RawDecode.pm`、`Common.pm`、`Format/S1.pm`、S1 Binary Encode／Decode、
  S1／S2 BASIC Encode／Decode、`TextCodec.pm`、CLI entry points
- Provenance/license: 同一作者のpcwavを読み取り専用で参照。今回コードは移植せず、観測した動作と未確定事項を文書化
- Observed behavior: S1／S2共通の4.096秒leaderと16-unit byte framing、S1 Binary `67`の開始address＋宣言長、
  S1 BASIC `07`、S2 BASIC `27`／`37`、120-byte chunk、nibble swap、BASIC物理trailer `FF`、S1半角カナ、
  S2 `FE` token／`1F` line reference／CP932 text
- Limitations (2026-10-09調査時点。上記作者確認でPGP方針更新済み): S2 Binary実装は存在しない。S2 tail checksumは参照コード自身が暫定と記載し、S1／S2 Decodeには
  checksum byteを読み飛ばして実比較しない経路がある。実機fixtureなしにこれらを確定仕様としない
- PGP decision (2026-10-09時点。現在は上記作者確認を優先): S1 Binaryは`.dmp`／Intel HEXの単一連続領域だけを受理する。S2 Binaryは一次資料または実機fixtureを
  得るまで実装しない。PGP Decoderは各層のchecksumと位置を構造化diagnosticとして検証する

### SC61860機種世代

- Date: 2026-10-02
- PGP files: `docs/reference/MACHINE_FAMILIES.md`、`docs/reference/ARCHITECTURE.md`
- Reference: Pokecom GO作者から提供された設計情報
- Reused behavior: OLD、S1、S2の世代分類と中間コードの相違。PC-1360、PC-1460、
  PC-1470Uのバンク切替。PC-1250/1251/1255、PC-1260/1261/1262、PC-1401/1402の
  ユーザーメモリ容量差。PC-1360Kの存在と漢字ROM追加
- Design changes: 中間コード方言とメモリバンクを別々の明示的な機種属性としてモデル化
- Verification: 各機種の実装時にトークン表とメモリ構成を個別確認する。PC-1360Kは詳細が
  判明するまで実装保留

### PC-1360起動基盤

- Date: 2026-10-07
- PGP files: `Pc1360Rom.kt`、`Pc1360MemoryBus.kt`、`Pc1360Display.kt`、
  `Pc1360Keyboard.kt`、`Pc1360Machine.kt`、Machine Catalog、Desktop ROM入出力とtest
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860_1360.java`、`KeyBoard1360.java`、`MainLoop1360.java`
- Provenance/license: 同一作者のPokecom GOを参照。Android固有構造は移植していない
- Reused behavior: 8KiB内部ROM、16KiB×8 ROMバンク、`0x3400`の下位3bitによる選択、
  `0x4000..0x7fff`のバンク窓、`0x8000..0xffff` RAM、LCD配置、シンボルbit、
  `0x3e00`の1起点キーグループ選択とキーマトリクス
- Design changes: ROMを9個の不変componentへ分割し、Memory Busがbank状態を所有する。
  PC-1350と共通の25×4表示変換を抽出し、UIへ物理VRAM配置を公開しない。
  bank切替はCore APIの明示イベントとして公開する
- Verification: 単体テスト、Catalog／Factory／Desktopテスト、およびローカル実ROMで起動、
  LCD更新、ROM bank切替を確認。FOによるRAM bank処理は仕様不明のため未実装

### PC-1360 S2 BASICと物理ROM履歴

- Date: 2026-10-07
- PGP files: `Pc1360BasicCodec.kt`、`Pc1360BasicProgramMemory.kt`、Core Session、
  Desktop BASIC入出力、Instruction Trace、Checkpoint Writerとtest
- Reference repositories: PCWAV、Pokecom GO
- Reused behavior: S2の`FF` program envelope、big-endian行番号、`FE xx` keyword、
  `1F hh ll`行番号参照、直接格納半角カナ、プログラム開始`0x8030`、管理pointer
  `0xffd7..0xffda`
- Design changes: ファイル転送用header/checksumをRAM内program imageから分離した。
  命令traceは論理PCに加え、不変ROM component ID、bank番号、component内offsetを保持する
- Scope: PC-1360K向け漢字／CP932 2byte文字とFO経由RAM bankは実装しない

### PC-1360国内版キーボード刻印

- Date: 2026-10-07
- Source: 国内向けPC-1360実機のユーザー確認
- Finding: 右端のSHIFT割当は\`, → <\`、\`: → >\`、\`/ → (\`、\`; → )\`、
  \`* → ¥\`。独立した括弧キーとして扱わない
- Design: PC-1360専用Character InputとStudio keyboard layoutへ反映する。
  PC-1360Kの配列は未確認のため追加しない

### PC-1360解析用マシン語ロード範囲

- Date: 2026-10-07
- Scope: PC-1360の`.dmp`、Assembler、プロジェクト成果物を`0x2000..0x3fff`およびRAMへ
  ロード可能にする。1360Kで漢字用ワークメモリとなる`0x2000..0x27ff`も拒否しない
- Design: Studio固有の解除ボタンは設けず、PC-1360 Sessionの`loadMemoryImage`とMemory Busで
  書込み可能範囲を定義する。他機種のガードは変更しない
- Excluded: FO経由RAM bankおよびPC-1360K固有メモリは仕様不明のため実装しない

### PC-1245初期機種仕様

- Date: 2026-10-02
- PGP files: `docs/reference/machines/PC-1245.md`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `README.md`、`Sc61860Base.java`、`Sc61860_1245.java`、
  `KeyBoard1245.java`、`MainLoop1245.java`、`SubActivity1245.java`
- Provenance/license: 同一作者のPokecom GOを参照。コードは未移植
- Reused behavior: ROM配置、メモリ変換、キーマトリクス、LCD配置、リセット値
- Design changes: Android ID、描画座標、ファイルAPI、static状態を機種仕様から分離。
  `cmdHook()`は対象外
- Verification: 未実施。Golden Test Dataの採取後に確認する

### SC61860 CPU状態とリセット

- Date: 2026-10-02
- PGP files: `core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/cpu/Sc61860State.kt`、
  `core/src/commonTest/kotlin/com/digihori/pgp/core/emulator/cpu/Sc61860StateTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:39-79,236-270`、`Sc61860params.java:14-46`
- Provenance/license: 同一作者のPokecom GOから状態項目とリセット値を再設計して移植
- Reused behavior: CPUレジスタ、フラグ、タイマー、ポートラッチ、内部RAMサイズ、リセット値
- Design changes: Android、static状態、メインメモリ、画面、キー、実時間制御を分離。
  フラグをBoolean化し、リセット時に`currentProgramCounter`も0へ戻す
- Verification: commonTestで初期状態、全状態のリセット、インスタンス間の非共有を検証

### SC61860 Bus境界と最小命令実行

- Date: 2026-10-02
- PGP files: `Sc61860Bus.kt`、`Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:277-329,383-493,634-670,2418-2430,2489-2552`
- Provenance/license: 同一作者のPokecom GOから命令フェッチと最初の命令群を再設計して移植
- Reused behavior: 16bit PCラップ、LII/LIJ/LIA/LIB、NOPT aliases、命令サイクル数
- Design changes: メモリアクセスを`Sc61860Bus`へ分離。未実装opcodeは継続せず停止理由を返す。
  コマンドフック、実時間待機、画面更新、命令回数のglobal配列は移植しない
- Verification: commonTestで即値ロード、NOPT、未実装opcode、PCラップ、reset時のBus非変更を検証

### PC-1245 Memory BusとROM入力

- Date: 2026-10-02
- PGP files: `Pc1245MemoryBus.kt`、`Pc1245MemoryBusTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `README.md:13-18,26-30`、`Sc61860_1245.java:29-34,99-260`
- Provenance/license: 同一作者のPokecom GOからPC-1245のメモリ挙動を再設計して移植
- Reused behavior: 64KiB ROMコンテナ、読出しalias、書込みアドレス変換、RAM/LCD mirror、
  `0x8000..0xffff`のリセット
- Design changes: ファイル・URI・Android APIを除去し、`ByteArray`入力を防御的にコピー。
  CPU resetとRAM resetを分離し、範囲外アドレスを拒否
- Verification: commonTestでROMサイズ、コピー所有権、alias、書込み変換、mirror、RAM resetを検証

### RomSetとPC-1245 Flat 64KiB Importer

- Date: 2026-10-02
- PGP files: `RomSet.kt`、`Pc1245Rom.kt`、`Pc1245FlatRomImporterTest.kt`、
  `RomSetTest.kt`、`Pc1245MemoryBus.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `README.md:13-18,26-30`、`Sc61860_1245.java:99-139`
- Provenance/license: 同一作者のPokecom GO ROM配置形式からImporterとして再設計
- Reused behavior: 64KiBイメージの内部ROM `0x0000..0x1fff`、外部ROM `0x4000..0x7fff`
- Design changes: 未使用領域を破棄し、ROMを`internal`と`external`へ正規化。ByteArrayを防御的に
  コピーし、Memory Busは旧ファイル形式ではなく`RomSet`だけを受け取る
- Verification: commonTestで領域抽出、サイズエラー、ID重複、所有権、Memory Bus要件を検証

### PC-1245 Machine実行境界

- Date: 2026-10-02
- PGP files: `Pc1245Machine.kt`、`Pc1245MachineTest.kt`、`Sc61860Cpu.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `MainLoop1245.java:35-48`、`Sc61860Base.java:227-329`
- Provenance/license: 同一作者のPokecom GOからCPUと機種メモリの所有関係を再設計
- Reused behavior: PC-1245生成時のCPU/RAM resetと1命令実行
- Design changes: CPUとMemory BusをMachineインスタンスが所有。実時間ループを除去し、
  `runCycles`を決定論的な命令境界実行として追加。未実装opcodeでは停止する
- Verification: commonTestでcold reset、step、cycle budget超過、未実装opcode停止を検証

### Emulator Session公開APIとSnapshot

- Date: 2026-10-02
- PGP files: `core/api/EmulatorSession.kt`、`core/api/EmulatorFactory.kt`、
  `core/api/EmulatorFactoryTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:39-79,141-220,277-329`、`Sc61860params.java`
- Provenance/license: 同一作者のPokecom GO状態参照・保存対象から公開読出しモデルを再設計
- Reused behavior: CPUレジスタ、内部RAM、メインメモリの読出し
- Design changes: 可変内部状態を公開せず防御的コピーのSnapshotへ変換。生成時のROMエラーを
  result型に変換し、内部停止理由を公開faultへ変換
- Verification: commonTestでFactory、実行、エラー変換、Snapshot所有権、メモリ範囲を検証

### PC-1245 LCD状態とDisplay Snapshot

- Date: 2026-10-02
- PGP files: `Pc1245Display.kt`、`Pc1245DisplayTest.kt`、`EmulatorSession.kt`、
  `EmulatorFactory.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860_1245.java:203-225`、`MainLoop1245.java:20-32,74-162`
- Provenance/license: 同一作者のPokecom GOからLCDメモリ配置とシンボルbitを再設計して移植
- Reused behavior: 80列のLCD byte配置、後半20列の逆順配置、DEF/P/G/DE/BUSY/SHIFT/RAD
- Design changes: Android Canvasとrefresh callbackを除去。論理ドットをrow-major Snapshotへ変換し、
  状態変化時だけrevisionを更新
- Verification: commonTestでLCD両領域、逆順、シンボル、reset、Snapshot境界を検証

### PC-1245キーマトリクスと入力API

- Date: 2026-10-02
- PGP files: `Pc1245Keyboard.kt`、`Pc1245KeyboardTest.kt`、`EmulatorSession.kt`、
  `EmulatorFactory.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `KeyBoard1245.java:7-34`、`Sc61860_1245.java:262-358`、
  `SubActivity1245.java:100-114`、`SubActivityBase.java:425-431`
- Provenance/license: 同一作者のPokecom GOからキーマトリクスとモード接点を再設計して移植
- Reused behavior: 10列の有効matrix、IA/IB scan、RUN/PRO接点、BREAKの10回key-on signal
- Design changes: Android View IDを`PocketKey`へ置換。複数同時押しと冪等なpress/releaseを
  インスタンス状態で管理し、RUN/PROを表示Snapshotへ統合
- Verification: commonTestでmatrix、同時押し、mode、BREAK、reset、公開入力APIを検証

### SC61860 I/O命令とPC-1245入力接続

- Date: 2026-10-02
- PGP files: `Sc61860Io.kt`、`Sc61860Cpu.kt`、`Pc1245Keyboard.kt`、
  `Sc61860CpuTest.kt`、`Pc1245MachineTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:670-677,886-897,1754-1761,2042-2049,2274-2283,2369-2378`、
  `Sc61860_1245.java:262-358`
- Provenance/license: 同一作者のPokecom GOからポート命令とPC-1245入力接続を再設計して移植
- Reused behavior: LIP、EXAM、OUTA、OUTB、INA、INBの状態変化とサイクル数、
  IA/IBによるキーマトリクスおよびRUN/PRO接点の選択
- Design changes: CPUが機種クラスを直接参照せず`Sc61860Io`境界だけに依存する。
  未接続時はゼロを返す実装を既定値とし、入力値をbyteへ正規化する
- Verification: commonTestで命令単体のレジスタ・フラグ・サイクル数と、ROMコードから
  PC-1245のキーおよびPROGRAMモードを読み取る結合動作を検証

### SC61860絶対分岐・呼出し・スタック命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:761-778,2211-2233,2061-2120,2200-2221`
- Provenance/license: 同一作者のPokecom GOから制御フローと内部RAMスタックの挙動を
  再設計して移植
- Reused behavior: LIDP、PUSH、POP、CALL、RTN、JP、JPNZ、JPNC、JPZ、JPC、
  `0xe0..0xff`短縮CALLのアドレス計算、スタック格納順、サイクル数
- Design changes: 16bit/8bitラップとbig-endian word読出しをCPU内の共通処理に集約。
  分岐命令をホスト側の制御フローや例外へ結び付けず、CPU状態だけを更新する
- Verification: commonTestでデータポインタ、PUSH/POP、CALL/RTNの往復、短縮CALL、
  絶対分岐、およびzero/carry条件の成立・不成立を検証

### SC61860相対分岐命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1923-2060`
- Provenance/license: 同一作者のPokecom GOから相対分岐の挙動を再設計して移植
- Reused behavior: JRP、JRM、JRNZP、JRNZM、JRNCP、JRNCM、JRZP、JRZM、JRCP、
  JRCMの分岐基準、zero/carry条件、成立時7・不成立時4サイクル、`R-1`へのoffset記録
- Design changes: 前後方向と条件を共通処理へ集約し、PCと内部RAMのアドレスを明示的に
  ラップする。分岐先の基準がoffset operand自身のアドレスであることをテストで固定する
- Verification: commonTestで全条件の成立・不成立、前後方向、offset記録、16bit境界ラップを検証

### SC61860基本レジスタ・フラグ命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:550-556,983-995,1083-1266,2400-2417`
- Provenance/license: 同一作者のPokecom GOから基本レジスタ演算とフラグ更新を再設計して移植
- Reused behavior: CLRA、SC、RC、I/J/A/B/K/L/M/Nのincrement/decrement、Q選択、
  16bit ALU値、zero/carry判定、命令サイクル数
- Design changes: 8種類のレジスタ演算を共通処理へ集約し、格納する8bit値とフラグ判定に使う
  16bit ALU値を分離。CLRAがフラグを変更しない挙動を明示する
- Verification: commonTestで全opcodeと対象レジスタ、Q、通常値、`0xff + 1`、`0 - 1`、
  `1 - 1`、CLRAのフラグ保持、SC/RCのzero/carryを検証

### SC61860 8bit即値演算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:998-1049,1269-1428,1642-1670`
- Provenance/license: 同一作者のPokecom GOから8bit即値演算の挙動を再設計して移植
- Reused behavior: ADIM、SBIM、ANIM、ORIM、TSIM、CPIM、ADIA、SBIA、ANIA、ORIA、
  TSIA、CPIAの演算対象、書込み有無、zero/carry/ALU更新、4サイクル
- Design changes: 即値の読出しとPC更新を共通化し、AとP指定内部RAMを同じ演算処理で扱う。
  論理演算がcarryを保持し、testがALUも保持する挙動を明示する
- Verification: commonTestでA/P両対象、加減算境界、AND/ORの書込み、test/compareの
  非書込み、ALU・zero・carryの更新範囲を検証

### SC61860 A-P間8bit演算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:538-548,1027-1080,1287-1415`
- Provenance/license: 同一作者のPokecom GOからAレジスタとP指定内部RAM間の演算を
  再設計して移植
- Reused behavior: ADM、SBM、ANMA、ORMA、ADCM、SBCM、TSMA、CPMAのオペランド方向、
  P側への書込み、carry入力、zero/carry/ALU更新、3サイクル
- Design changes: carryなし・ありの加減算を共通処理にし、carryを数値化する箇所を限定。
  test/compareと書込み演算を別処理にして状態変更範囲を明示する
- Verification: commonTestで通常・境界・carry入力、論理演算のcarry保持、Aの保持、
  TSMA/CPMAの両オペランド非書込みとフラグ更新を検証

### SC61860内部RAMポインタ・DPロード命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:594-727,679-718,1171-1177,1267-1273`
- Provenance/license: 同一作者のPokecom GOからP/Q/RとDPの操作を再設計して移植
- Reused behavior: LIDL、LIP、LIQ、LDP、LDQ、LDR、STP、STQ、STR、INCP、DECP、
  `0x80..0xbf`のLP、7bit/6bit mask、命令サイクル数
- Design changes: Pの7bit増減を専用処理にし、通常の8bitレジスタ演算と分離。
  DP下位byteの置換時に上位byteを明示的に保持する
- Verification: commonTestでDP上位byte保持、即値mask、P/Q/RとAの相互転送、
  Pの7bit境界ラップ、LP全範囲の代表値、既存PUSH/POP回帰を検証

### SC61860単byteメモリ転送命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:558-636,729-753,967-979`
- Provenance/license: 同一作者のPokecom GOから単byte転送とnibble交換を再設計して移植
- Reused behavior: STD、MVDM、MVMP、MVMD、LDPC、LDD、SWP、LDMの転送方向、
  PC/DP/P保持、フラグ保持、命令サイクル数
- Design changes: 外部メモリ書込みを`Sc61860Bus`境界だけに限定し、内部RAMとの転送を
  CPU状態内に閉じる。PC相対読出しとDP読出しを別処理として明示する
- Verification: commonTestでA/PからDPへの書込み、DPからA/Pへの読出し、PC上byteの
  非消費、PC/DP/P保持、nibble交換、zero/carry保持を検証

### SC61860 X/Yインデックス命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1480-1575`
- Provenance/license: 同一作者のPokecom GOからX/Yインデックス操作を再設計して移植
- Reused behavior: IX、DX、IY、DY、IXL、DXL、IYS、DYSの16bitアドレス計算、
  pre-increment/decrement、DP/Q更新、Aへのload・Aからのstore、サイクル数
- Design changes: X/Yのlow registerを入力とする共通処理へ集約し、loadとstoreの同時指定を
  禁止。外部メモリアクセスは`Sc61860Bus`だけを経由する
- Verification: commonTestでX/Y各pair、増減、16bit境界ラップ、新アドレスからのload、
  新アドレスへのstore、DP/Qとhigh/low byteの更新を検証

### SC61860 Aレジスタシフト命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1581-1610`
- Provenance/license: 同一作者のPokecom GOからcarry経由シフトを再設計して移植
- Reused behavior: SL、SRのcarry入力、押し出しbitによるcarry更新、ALU値、Aへの8bit格納、
  zero保持、2サイクル
- Design changes: carryのBoolean表現を命令内で明示的に0/1またはbit 8へ変換し、
  8bit格納値と9bit ALU値を分離する
- Verification: commonTestで左右それぞれのcarry入力・出力、AとALU、zero保持を検証

### SC61860 16bit加減算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1432-1478`
- Provenance/license: 同一作者のPokecom GOから16bit加減算を再設計して移植
- Reused behavior: ADB、SBB、P指定little-endian word、B:A register pair、演算後のP更新、
  16bit zero/carry判定、ALU非変更、5サイクル
- Design changes: P側wordとB:A pairの組立てを専用処理に分離し、Pの上位byte位置を
  7bitラップで求める。結果格納とフラグ更新を共通化する
- Verification: commonTestで通常加算、16bit overflow、減算borrow、P境界ラップ、
  A/BとALUの保持、格納byte順を検証

### SC61860内部RAMブロック転送命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:785-821,879-925`
- Provenance/license: 同一作者のPokecom GOから内部RAMブロック転送・交換を再設計して移植
- Reused behavior: MVW、EXW、MVB、EXB、I/Jで符号化された`count - 1`、P/Qの逐次更新、
  Dの終了値、7bitラップ、可変サイクル数
- Design changes: I/Jとcopy/exchangeの差を共通処理へ集約し、命令開始時に転送数を確定する。
  逐次処理を維持して重複領域でも元実装と同じ順序になるようにする
- Verification: commonTestでI/J両系統、copy/exchange、複数byte、P/Q/D終了値、
  可変サイクル数、7bit境界ラップを検証

### SC61860 DPブロック転送命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:823-867,928-964`
- Provenance/license: 同一作者のPokecom GOからDP・内部RAM間のブロック転送と交換を
  再設計して移植
- Reused behavior: MVWD、EXWD、MVBD、EXBD、I/Jで符号化された`count - 1`、
  P/DPの逐次更新、完了時のDP decrement、Dの終了値、可変サイクル数
- Design changes: I/Jとcopy/exchangeの差を共通処理へ集約し、外部メモリアクセスを
  `Sc61860Bus`経由に限定。Pの7bitとDPの16bitラップを別々に扱う
- Verification: commonTestでI/J両系統、copy/exchange、双方向の値、P/DP/D終了値、
  可変サイクル数、P/DP境界ラップを検証

### SC61860ブロック塗りつぶし命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:869-895`
- Provenance/license: 同一作者のPokecom GOからAによるブロック塗りつぶしを再設計して移植
- Reused behavior: FILM、FILD、Iで符号化された`count - 1`、P/DPの逐次更新、
  FILD完了時のDP decrement、Dの終了値、可変サイクル数
- Design changes: 内部RAM用と外部Bus用を分離し、Pの7bit・DPの16bitラップと異なる
  終了位置を明示する。転送数は命令開始時に確定する
- Verification: commonTestで複数byte、P/DP/D終了値、P/DP境界ラップ、書込み値、
  可変サイクル数を検証

### SC61860 packed BCD演算命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1717-1921`
- Provenance/license: 同一作者のPokecom GOからpacked BCD加減算と互換mask規則を
  再設計して移植
- Reused behavior: ADN、SBN、ADW、SBW、Iで符号化された`count - 1`、AまたはQ側operand、
  下位アドレス方向の桁送り、P/Q/D終了値、全桁zero、最終carry/borrow、可変サイクル数
- Design changes: BCD 1byte加算・減算と不正nibble正規化を共通関数へ分離し、複数桁命令は
  carry/borrowだけを次の桁へ渡す。不正A〜F桁を無効化するPokecom GO固有規則を明示する
- Verification: commonTestでA/Q両系統の加算carry・減算borrow、複数桁、P/Q/D終了値、
  zero/carry、可変サイクル数、`0xfa`の不正BCD maskを検証

### SC61860複数byteニブルシフト命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1612-1640`
- Provenance/license: 同一作者のPokecom GOから複数byteの4bitシフトを再設計して移植
- Reused behavior: SRW、SLW、Iで符号化された`count - 1`、Pの処理方向、byte間のnibble伝播、
  P/D終了値、ALU最終値、zero/carry保持、可変サイクル数
- Design changes: 左右の走査方向を別処理として明示し、各反復のshift bufferをIntで保持。
  内部RAMへの格納時だけ8bitへ制限する
- Verification: commonTestで2byteの左右4bitシフト、ALU、P/D終了値、zero/carry保持、
  可変サイクル数、Pの7bit境界ラップを検証

### SC61860 LOOP命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2123-2156`
- Provenance/license: 同一作者のPokecom GOからRスタック上のループ制御を再設計して移植
- Reused behavior: LOOPのoffset基準、分岐判定前のcounter値、判定後のdecrement、
  zero/carry/ALU更新、終了時だけのR increment、継続時10・終了時7サイクル
- Design changes: 分岐判定、counter更新、frame終了を順序どおり明示し、PCは16bit、Rは7bitで
  個別にラップする
- Verification: commonTestでcounter 2/1/0、分岐先、counterとフラグ、R保持・更新、
  7/10サイクル、R境界ラップを検証

### SC61860 CASEテーブル分岐命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2158-2209`
- Provenance/license: 同一作者のPokecom GOからCASEテーブル分岐を再設計して移植
- Reused behavior: CASE1のD設定と2byte stack格納、CASE2のA比較、3byte table entry、
  一致先・既定先への分岐、D残数、比較回数依存サイクル
- Design changes: table走査を明示的な反復処理とし、PC読出しを16bitラップ対応の共通処理へ
  統一。D=0のdo-while相当を256比較として保持する
- Verification: commonTestでCASE1のPC/D/Rと格納byte順、2件目一致、全件不一致、
  分岐先、D残数、可変サイクル数を結合検証

### SC61860レジスタ・スタック補助命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:568-589,755-760,879-885,2418-2433,2497-2560`
- Provenance/license: 同一作者のPokecom GOから純粋な補助命令とopcode aliasを再設計して移植
- Reused behavior: EXAB、LEAVE、RZ aliases (`0x72/73/76/77`)、NOPW aliases
  (`0x4d/ce/d3/d9`)、状態変更範囲、operand skip、命令サイクル数
- Design changes: 副作用のないNOPWはstep分岐から直接結果を返し、RZのaliasを共通処理へ集約。
  3サイクルのNOPT aliasesとは明示的に区別する
- Verification: commonTestでA/B交換とフラグ保持、R位置のclearとR保持、RZのPC/zero/carry、
  全NOPW aliasesの2サイクルと状態保持を検証

### SC61860 DP即値論理命令

- Date: 2026-10-02
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:568-589,1311-1325,1359-1373,1669-1682`
- Provenance/license: 同一作者のPokecom GOからDP上の即値論理演算と退避動作を再設計して移植
- Reused behavior: ANID、ORID、TSID、SZ、DP値の`R-1`退避、R非更新、即値消費、
  書込み後再読出しによるzero判定、carry/DP保持、6サイクル
- Design changes: 元値退避を共通処理にし、外部メモリの読書きを`Sc61860Bus`へ限定。
  ANID/ORIDは書込みを拒否するBusでも正しく判定できるよう結果を再読出しする
- Verification: commonTestでAND/OR結果、元値退避、TEST非書込み、SZのskip、PC/R/DP、
  zero/carry、命令サイクル数を検証

### SC61860 F・Control出力ポート命令

- Date: 2026-10-02
- PGP files: `Sc61860Io.kt`、`Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2287-2354`
- Provenance/license: 同一作者のPokecom GOからF/Control port latchとCPU内作用を再設計して移植
- Reused behavior: OUTF、OUTC、QとFO/Control latch、Control bit 1によるtimer/divider reset、
  bit 3によるpower状態、命令サイクル数
- Design changes: Android画面更新と直接Beep再生をCPUから除去し、`Sc61860Io`へF/Control出力通知を
  追加。表示・音声デバイスは後続の機種側実装で通知を解釈する
- Verification: commonTestで内部portからのlatch、Q、I/O通知、timer/divider resetの有無、
  power状態、3/2サイクルを検証

### SC61860 TEST命令とkey-on入力

- Date: 2026-10-02
- PGP files: `Sc61860Io.kt`、`Sc61860Cpu.kt`、`Pc1245Keyboard.kt`、
  `Sc61860CpuTest.kt`、`Pc1245MachineTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:277-325,365-384,2441-2470`、
  `SubActivityBase.java:425-431`
- Provenance/license: 同一作者のPokecom GOからTEST portとBREAK key-on pulseを再設計して移植
- Reused behavior: TESTのdivider500/divider2/key-on bit、divider latchのread-clear、即値mask、
  zero更新、4サイクル、BREAKによる10回のkey-on観測
- Design changes: key-on取得を`Sc61860Io.consumeKeyOnSignal()`へ分離。Android main loopの回数に
  依存する仮2ms timer生成は移植せず、divider生成用の決定論的時間源は別途設計する
- Verification: commonTestで全信号bit、mask、read-clear、zero、PC、I/O信号消費を検証し、
  PC-1245 Machine結合テストでBREAKからTESTまでの経路を確認

### PC-1245 I/O集約とLCD enable接続

- Date: 2026-10-03
- PGP files: `Pc1245Io.kt`、`Pc1245Machine.kt`、`Pc1245Keyboard.kt`、
  `Pc1245MachineTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2318-2354`、`MainLoop1245.java:74-162`
- Provenance/license: 同一作者のPokecom GOからOUTC bit 0によるLCD有効状態を再設計して移植
- Reused behavior: Control port bit 0のLCD enable、状態変化時の画面更新、reset時の表示有効化
- Design changes: KeyboardをSC61860全I/Oの実装とせず、`Pc1245Io`がKeyboardとDisplayを構成する。
  Android refresh callbackは使わず、Display revisionとSnapshotの`enabled`へ反映する
- Verification: commonTestでROM命令列からLIP/LIA/EXAM/OUTCを実行し、LCD disable、revision、
  Control latch、cold resetによる再有効化を結合検証

### PC-1245論理BuzzerとAudio Snapshot

- Date: 2026-10-03
- PGP files: `Pc1245Buzzer.kt`、`Pc1245Io.kt`、`Pc1245Machine.kt`、
  `EmulatorSession.kt`、`EmulatorFactory.kt`および各test
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2336-2351`、`Beep.java`
- Provenance/license: 同一作者のPokecom GOからControl portによるtone選択仕様だけを再設計して移植
- Reused behavior: Control bits 4-5の`0x00=off`、`0x20=2kHz`、`0x30=4kHz`、
  `0x10=変更なし`、reset時の停止
- Design changes: Android `AudioTrack`、thread、buffer生成をCoreへ持ち込まず、周波数とrevisionを持つ
  論理Buzzerに置換。公開`AudioSnapshot`をホスト音声実装との境界にする
- Verification: commonTestで全Control選択値、同値の冪等性、reset、ROMからOUTCまでの結合、
  公開Snapshot初期値を検証

### SC61860 MVWP内部サブルーチン型ブロック転送命令

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:515-533,2506,2582`
- Provenance/license: 同一作者のPokecom GOからMVWPの転送と特殊な制御フローを再設計して移植
- Reused behavior: B:Aを転送元アドレス、Pを転送先内部RAMアドレスとするI+1 byte転送、
  16-bit転送元と7-bit Pのwrap、Dの0終端、Rスタックを経由した次命令への復帰、`I*4+7`サイクル
- Design changes: 命令本体のPCを明示的にスタックへ退避し、既存の16/8/7-bit演算helperと
  `Sc61860Bus`境界でwrap規則を表現。Iは転送開始前に保存し、転送先との重複を許容する
- Verification: commonTestで3 byte転送、サイクル数、P/D/PC/R、A/B保持、および
  `0xffff -> 0x0000`の転送元wrapと`0x7f -> 0x00`の転送先wrapを検証

### SC61860 CUP・CDN X入力カウント命令

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:1685-1717,2513,2523`
- Provenance/license: 同一作者のPokecom GOからCUP/CDNのX入力極性別カウント動作を再設計して移植
- Additional reference: [utz82/SC61860-Instruction-Set](https://github.com/utz82/SC61860-Instruction-Set)
  （CC0-1.0）の命令表
- Reused behavior: CUPはX入力low、CDNはhighの間Pを加算し、Dが`0xff`に達した場合にzeroを設定。
  反対極性ではPを保持してDをI+1にし、zeroを解除する。Pは7-bitでwrapする
- Design changes: 共通処理へ統合し、mutable globalではなくCPUインスタンス所有の`xInput`を参照する。
  現状の同期stepでは命令実行途中のX入力変化は扱わず、開始時のlevelが命令中継続する。
  Pokecom GOの`I*4`ではI=0時にCoreの正サイクル契約を破るため、命令資料の`1+I*4`を採用した
- Verification: commonTestで両命令の対象極性、反対極性、P/D/zero、サイクル数、7-bit wrapを検証

### SC61860 WAIT命令

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:2433-2439,2513,2586`
- Additional reference: [utz82/SC61860-Instruction-Set](https://github.com/utz82/SC61860-Instruction-Set)
  （CC0-1.0）の命令表
- Provenance/license: Pokecom GOの命令配置を参照し、公開命令資料のサイクル定義で再実装
- Reused behavior: opcode `0x4e`、1 byteオペランド消費、CPU状態を変更しない待機命令
- Design changes: Pokecom GOのコメント付き仮補正`operand*6/4`は移植せず、命令資料に記載された
  `6+operand`サイクルとして表現する。wall clock待機やplatform sleepはCoreへ導入しない
- Verification: commonTestでoperand 0/255の6/261サイクル、PCと16-bit wrap、Pとflag保持を検証

### PC-1245実ROMヘッドレス起動スモークテスト

- Date: 2026-10-03
- PGP files: `RealPc1245RomSmokeTest.kt`、`README.md`
- Input: 利用者が`local-data/roms/pc-1245/pc1245mem.bin`へ配置した64KiBの実ROM
- Provenance/license: ROM内容は読込み時だけ使用し、テストコード、Git、CI成果物には含めない
- Behavior: Flat ROM Importerから公開FactoryとSessionを通し、cold reset後に100万サイクルを実行する
- Design changes: JVM/Desktop専用テストとしてファイルアクセスを`commonMain`から分離。
  `PGP_PC1245_ROM`または既定の`local-data`を探索し、ROMがない環境ではスキップする
- Verification: PC-1245実ROMで100万サイクルを完走し、Core faultなし、命令実行数と消費サイクル、
  machine IDを確認。ROMファイルはテスト後も変更しない

### SC61860決定論的divider生成

- Date: 2026-10-03
- PGP files: `Sc61860Cpu.kt`、`Sc61860CpuTest.kt`、`PC-1245.md`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860Base.java:110,280-319,343-369,2318-2330,2441-2470`
- Additional references: PC-1245の576kHz仕様、`SC61860-Instruction-Set`の命令サイクル定義
- Provenance/license: Pokecom GOのdivider latchとリセット・読出し動作を再設計し、公開仕様の
  クロック値から周期を導出
- Reused behavior: TEST bit 0/1、read-clear latch、OUTC bit 1によるtimer reset
- Design changes: Androidループ100回ごとの仮2ms信号と外部`ticTac()`を廃止。576kHz発振を
  2 clocks/cycleとして、2msを576 cycles、500msを144,000 cyclesで生成する。
  wall clock、thread、Handlerを使わず、各命令が返すサイクルだけで進む
- Verification: commonTestで両周期の境界、余り、ラッチ保持、複数周期、TEST消費、OUTC reset後の
  位相再開を検証。実ROMスモークテストも100万サイクルを完走

### Golden Test Data v1形式

- Date: 2026-10-03
- PGP files: `docs/reference/GOLDEN_TEST_DATA.md`、`test-data/golden/schema-v1.json`、
  `test-data/golden/README.md`
- Reference repository: Pokecom GO
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Provenance/license: Pokecom GOから期待結果を採取するためのPGP独自メタデータ形式。ROM内容は含めない
- Design: `pgp-golden-1` JSONでproducer revision、ROM SHA-256、順序付きAction、名前付きExpectationを
  記録する。CPU、選択RAM範囲、7×80論理LCD、symbol、audio、停止状態を独立に比較可能にする
- Distribution: 未審査の実ROM由来データはGit管理外の`local-data/golden/`へ隔離し、レビュー済みの
  最小データだけを`test-data/golden/`へ配置する
- Verification: Draft 2020-12 JSON Schemaを同梱し、必須項目、hex幅、SHA-256、Action種別、
  PC-1245 LCD寸法を機械検証できるようにした

### Golden Test Data PGP再生・比較基盤

- Date: 2026-10-03
- PGP files: `GoldenScenarioRunner.kt`、`GoldenScenarioRunnerTest.kt`、
  `pc1245-synthetic-reset.json`、Gradle version catalog
- Provenance/license: PGP独自のテスト支援コードと再配布可能な合成NOPT ROM用メタデータ
- Dependency: `kotlinx-serialization-json 1.11.0`をDesktop test source setだけで使用
- Design: CoreへJSON依存を持ち込まず、公開`EmulatorSession` APIでActionを順次実行する。
  ROM SHA-256を実行前に照合し、名前付きcheckpointで指定されたCPU、RAM、LCD、audio、実行状態だけを比較
- Verification: 64KiBをNOPTで埋めた合成ROMについてresetと2命令実行後を再生し、不一致ROMの
  SHA-256拒否もテスト。JSONはunknown fieldを拒否するstrict modeで読み込む

### PGP Golden Snapshotエクスポーター

- Date: 2026-10-03
- PGP files: `GoldenSnapshotExporter.kt`、`GoldenSnapshotExporterTest.kt`、READMEとGolden仕様
- Provenance/license: PGP独自のDesktopテスト支援コード。ROM byteは出力せずSHA-256だけを記録
- Design: `PGP_EXPORT_GOLDEN=1`でのみ書込みを許可し、producer revisionも明示入力を必須にする。
  reset直後と100万サイクル後のCPU、内部RAM、選択メモリ、LCD、audio、実行状態を採取する
- Local output: `local-data/golden/pc1245-pgp-boot-1000000.json`（Git管理外、14,152 byte）
- Verification: 合成ROMのencode/decode/replay round-tripを自動テスト。利用者提供PC-1245 ROMから
  実データを採取し、ROM SHA-256照合後に全checkpointを再生して一致を確認

### Pokecom GO Save State Importer

- Date: 2026-10-03
- PGP files: `PokecomGoStateImporter.kt`、`PokecomGoStateImporterTest.kt`、Golden仕様
- Reference repository: Pokecom GO（read-onlyを維持）
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `Sc61860params.java`、`Sc61860Base.java:141-215`、
  `Sc61860_1245.java:37-67`、`SubActivityBase.java:263-320`
- Provenance/license: 同一作者のPokecom GO既存Gson Save State構造を読込み境界として再設計
- Design: `PREF_SC`のJSONからPC-1245 idを検証し、CPU、256 byte内部RAM、選択main RAM、
  80列LCD、symbol、RUN/PROをGolden Expectationへ変換する。参照リポジトリは変更しない
- Limitation: Save Stateには累積cycle/instruction数がないため、自由走行SnapshotはPGPの固定
  `runCycles`結果と比較しない。実行境界を固定できる採取方法が次段階で必要
- Verification: Gsonと同等の64KiB main RAMを含む合成JSONで全変換、signed byte mask、LCD、symbol、
  PC-1245以外の拒否をDesktop testで検証

### Pokecom GO SharedPreferences XMLブリッジ

- Date: 2026-10-03
- PGP files: `PokecomGoPreferencesReader.kt`、`PokecomGoPreferencesReaderTest.kt`、Golden仕様
- Reference repository: Pokecom GO（read-onlyを維持）
- Reference commit: `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: `SubActivityBase.java:263-320`（default SharedPreferences key `PREF_SC`）
- Design: `adb exec-out run-as`で取得したpreferences XMLから対象stringだけを抽出し、XML entityを
  decodeしてSave State Importerへ接続する。Android APIやPokecom GOビルドへの依存は追加しない
- Security: DTD、外部general/parameter entity、external DTD/schema access、XIncludeを無効化
- Verification: Android SharedPreferences相当XMLの抽出とentity decode、key欠落、空値、XXE入力拒否を
  Desktop testで検証。Huawei ANE-LX2J上のdebug buildから実際のPC-1245状態を採取し、明示実行の
  `PokecomGoDeviceStateIntegrationTest`でCPU、RAM、LCD Expectationへの変換を確認

### PC-1245固定サイクル実機比較

- Date: 2026-10-03
- PGP files: `PokecomGoDeviceStateIntegrationTest.kt`、Golden仕様、本文書
- Reference repository: Pokecom GOはread-onlyを維持。一時debugコピーだけに採取フックを適用
- Capture boundary: Pokecom GOの命令ごとの`iTick`増分を累積し、100万cycles以上となる最初の
  命令境界で停止。PGPの`runCycles(1_000_000)`と同じ境界規則を使用
- Baseline finding: Pokecom GO本来の命令回数ベース2ms timerとAndroid wall-clockベース500ms timerでは
  CPU実行経路が分岐した。論理RAM `0x8000..0x87ff`は一致し、`0xf800..0xf8ff`は1 byte差
- Controlled finding: 一時debugコピーのdividerだけをPGPと同じ576/144,000 cyclesへ揃えると、両RAM範囲は
  byte単位で完全一致。CPUは6フィールド、内部RAMは8 byteが不一致で、アイドルループの位相差が残った
- Interpretation: 残差は累積命令数または命令別cycle値の相違を調べる必要がある。divider補正済み一時版は
  元実装そのもののGolden producerではないため、採取データを承認済み期待値へ昇格させない
- Local data: 元timer版とcycle timer版のpreferences XMLは`local-data/golden/`に保存しGit管理外とする
- Automated comparison: `PGP_VERIFY_POKECOM_GO_STATE=1`の明示的なDesktop integration testで、
  cycle timer版captureとPGPの100万cycle結果を比較する。`0x8000..0x87ff`、`0xf800..0xf8ff`、
  LCD dot/symbolは完全一致し、既知の内部RAM 8 byte差とCPU 6項目
  (`programCounter`、`currentProgramCounter`、`opcode`、`q`、`ib`、`testPort`)を固定する。
  これらの既知差分が増減した場合もテスト失敗として再評価する
- Trace tooling: `InstructionTraceRecorder`が公開Session APIの1命令stepを使い、累積命令/cycle、実行PC、
  opcode、命令cycle、実行後PC、Q、IB、TESTを安定したTSV列で出力する。指定件数のtailだけを保持して
  100万cycle調査時のメモリ使用を制限する。Pokecom GO一時debug copyも同じ列を出力し、最初の分岐を探す
- Trace finding: 起動直後からの全trace比較では、最初の相違は第1命令のWAIT (`0x4e`) のcycle値だった。
  operand `0xa0`に対しPokecom GOは`operand * 6 / 4 = 240`、PGPは公開命令資料に基づく
  `6 + operand = 166`を返す。命令順で比較すると最初の5,340命令はPC/opcode/実行後状態が一致し、
  第5,341命令のTEST (`0x6b`) で2ms dividerの到達時期によりTEST値が初めて分岐する
- Decision: この差は移植漏れではなく、Pokecom GO内にも「長すぎるので仮で1/4」と記された暫定cycle式と、
  PGPが採用した命令資料の定義との差である。Pokecom GOの最終位相へ合わせるためにPGPのWAITを戻さない。
  RAM/LCD一致はCPU機能移植の検証に用いる一方、固定cycle後のCPU位相差は既知の設計差として扱う

### ホスト実時間サイクル予算Planner

- Date: 2026-10-03
- PGP files: `CycleBudgetPlanner.kt`、`CycleBudgetPlannerTest.kt`、Core API、Roadmap
- Provenance: PGP独自設計。Pokecom GOのAndroid `Handler`、wall clock、loop回数による速度調整は移植しない
- Design: ホストが計測した単調経過時間だけを入力とし、PC-1245の288,000 cycles/secへ整数演算で変換。
  1 cycle未満を繰り越し、既定100msのcatch-up上限と破棄時間を返す
- Separation: Plannerは時計、thread、sleep、UI、`EmulatorSession`を所有しない。pause/resumeと実行呼出しは
  各platformのRunnerが担当する
- Speed: 1/2倍、通常、2倍を含む有理数倍率を使用し、浮動小数の累積誤差を避ける。倍率変更とtimeline
  resetでは旧端数を破棄する
- Verification: commonTestで1秒/1 frame換算、端数繰越、長時間停止の上限、倍率、reset、入力拒否を検証。
  Desktop JVMとiOS simulatorを含むKMP全体buildに成功

### Desktop Emulator Runner

- Date: 2026-10-03
- PGP files: `DesktopEmulatorRunner.kt`、`DesktopEmulatorRunnerTest.kt`、Desktop Gradle設定
- Design: JVMの`System.nanoTime()`を単調時計adapterで隔離し、`CycleBudgetPlanner`の予算だけを
  `EmulatorSession.runCycles()`へ渡す。RunnerはPAUSED、RUNNING、FAULTEDを明示的に管理する
- Lifecycle: runは同じ状態で冪等、pause/resumeは停止中の経過時間を破棄、resetはSession、速度、時刻基準を
  初期化する。速度変更時も旧倍率で経過した時間を新倍率へ課金しない
- Fault: Core faultを受けたら自動実行を停止し、resetまでrunを再開しない
- Separation: Compose timer、描画、ROM選択、入力処理はまだ接続せず、RunnerをUI非依存で検証可能に保つ
- Verification: fake monotonic clockとfake Sessionでcycle予算、pause/resume、run冪等性、reset、倍率変更、
  step、fault停止、時計逆行拒否をDesktop unit testで検証

### Desktop ROM選択と最小実行操作

- Date: 2026-10-03
- PGP files: `Main.kt`、`DesktopRomLoader.kt`、`DesktopRomLoaderTest.kt`、Roadmap
- Design: AWT file dialogとファイル読込みをDesktop層に限定し、PC-1245 legacy 64KiB imageを既存Importer、
  Factory、Sessionへ接続。ROM byteやファイルパスはCore APIや永続設定へ持ち込まない
- UI: ROM名、Runner状態、累積実行cycles、状態メッセージとSelect ROM / Run / Pause / Reset / Stepを表示。
  Compose coroutineが約16ms間隔でRunnerをtickするが、実行量はframe数でなく単調経過時間から決定する
- Error handling: 不正サイズ、読込み例外、Session生成失敗、Core faultを画面メッセージとして扱う
- Scope: この段階ではLCD、キー入力、CPUレジスタ表示、音声出力を接続しない
- Verification: legacy imageのSession生成と不正サイズをDesktop unit testで検証。全KMP build成功後、
  Desktopウィンドウの起動スモーク確認を実施

### Desktop PC-1245 LCD表示

- Date: 2026-10-03
- PGP files: `Main.kt`、`DesktopEmulatorRunner.kt`、Roadmap
- Design: Runnerから公開`DisplaySnapshot`だけを取得し、80×7論理dotとsymbolをCompose Canvasへ描画。
  Coreのmemory配列やPokecom GOのAndroid描画座標へ依存しない
- Layout: 利用可能幅からcell寸法を毎回算出し、最大幅800dpを超えないaspect-ratio指定とする。
  dotは各cell内の比率余白で描くため、ウィンドウscaleやHiDPIでも固定座標を使わない
- Refresh: Runner tickは約16ms間隔だが、Snapshot revisionまたは表示有効状態が変わった場合だけ
  Compose stateを更新する。CPU実行頻度とLCD再描画頻度を分離する
- Scope: LCD色は暫定theme値。実機外観skin、残像、contrast、backlight表現は後続UI設計で扱う
- Verification: Coreの既存LCD dot/symbol/revision testを描画入力契約として使用し、全KMP buildで
  Desktop UIのコンパイルを確認

### Desktop PC-1245キー入力

- Date: 2026-10-03
- PGP files: `DesktopKeyMapper.kt`、`DesktopKeyMapperTest.kt`、`Main.kt`、
  `DesktopEmulatorRunner.kt`とtest、Roadmap
- Mapping: Desktopの英字、数字列、numeric keypad、演算子、矢印、Enter、Space、Shift、Backspace/Delete、
  Escape、F1を論理`PocketKey`へ変換。Escape=BREAK、F1=DEFとする
- Key lifecycle: 物理key codeごとに押下中の論理keyを保持し、OS auto-repeatの重複KeyDownを無視。
  KeyUpではKeyDown時と同じ論理keyをreleaseするため、途中でShift状態が変わってもstuck keyを作らない
- Screen input: SHIFT、DEF、方向、BREAK、CLEAR、ENTERをpressからpointer releaseまで保持する画面キーとして追加
- Mode: RUN/PROを`OperatingMode` APIへ接続し、mode revisionによってLCD symbolを即時更新
- Separation: Composeのplatform非依存`Key`定数をDesktop Mapper内だけで使用し、Coreは`PocketKey`を維持。
  `nativeKeyCode`をJava/AWT定数として解釈しないためmacOS/Skikoのkey code差異を受けない
- Focus: `Window.onPreviewKeyEvent` で物理キーを受け取り、ボタンやROMファイルダイアログ後のComposeフォーカス状態に依存しない
- Verification: 文字、数字列、numeric keypad、control、navigation、shift演算子、未割当keyのmappingと、
  RunnerからSessionへのpress/release/mode委譲をDesktop unit testで検証

### Desktop CPUレジスタ表示

- Date: 2026-10-03
- PGP files: `Main.kt`、`DesktopEmulatorRunner.kt`、Roadmap
- Design: 公開`CpuSnapshot`だけをRunner経由で取得し、PC/current PC/opcode/DP、P/Q/R/D、ALU、flag、
  IA/IB/FO/Control/Testを固定幅hexで表示。CPU内部の可変stateへDesktopから直接アクセスしない
- Refresh: ROM読込み、reset、step直後は即時更新。通常実行中はLCD tick 6回ごと（概ね10Hz）に抑え、
  256 byte内部RAMを含むSnapshot copyとCompose再描画の負荷を制限する
- Scope: 内部RAM dump、memory viewer、disassembly、breakpointはDebugger phaseで追加する
- Verification: 全KMP buildで公開Snapshot APIとDesktop表示のコンパイルを確認

### Desktop Audio Snapshot出力

- Date: 2026-10-03
- PGP files: `DesktopAudioPlayer.kt`、`DesktopAudioPlayerTest.kt`、`Main.kt`、
  `DesktopEmulatorRunner.kt`、Roadmap
- Design: Coreの`AudioSnapshot(frequencyHz, revision)`だけを入力とし、Java Soundの16-bit mono PCM Clipへ変換。
  Coreはhost audio API、buffer、thread、volumeを所有しない
- Waveform: 44.1kHz、little-endian、振幅4096のsquare wave。sample rateと周波数のGCDからloop長を決め、
  2kHz/4kHzともperiod途中でloop境界を作らない
- Lifecycle: revision変化時だけClipを交換し、0Hz、Pause、Reset、ROM交換、Window破棄で停止・closeする。
  Resume後は同じrevisionでも再評価してtoneを復帰できる
- Failure handling: audio device取得失敗はResultとしてUIへ通知し、CPU実行や入力を停止しない
- Verification: fake Clipでstart/replace/silence/stop/restart、device failure、PCM長・endianness・入力拒否を
  Desktop unit testで検証

### PC-1245ホスト文字入力マップ

- Date: 2026-10-03
- PGP files: `Pc1245CharacterInput.kt`、`Pc1245CharacterInputTest.kt`
- Reference repository: Pokecom GO（read-onlyを維持）
- Reference files: `pc1245mainkey.png`、`include_keys_1245.xml`、`KeyBoard1245.java`
- Design: ホストの文字とPC-1245実キーを分離し、文字を機種固有のキー列へ変換する。PC-1245の
  SHIFTは同時押しmodifierではなくラッチ操作なので、`!`を`SHIFT`、`Q`の連続tapとして表現する
- Scope: この段階では純粋な変換表だけを追加し、Desktopイベントや実行タイミングには未接続。
  ソフトウェアキーボードは変換表を経由せず実キーを直接操作する
- Verification: 英字の大小、数字、直接入力記号、SHIFT刻印記号、未対応文字をcommonTestで検証

### サイクル駆動キー入力キュー

- Date: 2026-10-03
- PGP files: `KeyInputQueue.kt`、`KeyInputQueueTest.kt`、`DesktopEmulatorRunner.kt`とtest
- Design: ホスト文字から生成したキー列を、壁時計やCompose frameではなくエミュレーターの実行cycleで
  press / releaseする。既定では押下を60ms相当（17,280 cycles）、キー間隔を20ms相当
  （5,760 cycles）とする。Pokecom GOがkey-up後も20ms周期3回分を保持した挙動を下限にした
- Execution: Runnerは遷移境界まで`runCycles`予算を分割し、命令単位の超過は次phaseへ繰り越さず
  遷移をその命令直後へ遅らせる。返却する実行量と命令数は分割結果を合算する
- Lifecycle: pause、faultでは自動入力を破棄して保持キーをreleaseし、resetではSession resetとともに破棄する。
  画面キーと直接物理キーはキューの管理対象外
- Verification: press/hold/release/gap順、命令overshoot、cancel、Runnerでの予算分割とpause解放をテスト

### Desktop文字入力と直接キー入力の分離

- Date: 2026-10-03
- PGP files: `DesktopKeyboardInput.kt`、`DesktopKeyMapper.kt`とtest、`DesktopEmulatorRunner.kt`
- Design: 印字可能なホスト文字はPC-1245文字マップからサイクル駆動キューへ送り、矢印、Enter、
  CLEAR、BREAK、DEFは直接press/releaseする。ホストShift自体はPC-1245 SHIFTへ割り当てない
- Platform behavior: `Shift+1`等は物理キー位置でなくComposeが報告した文字`!`を使用するため、
  OSやキーボード配列にかかわらずPC-1245の`SHIFT`、`Q`へ変換される。Command/Ctrl併用はOSへ渡す
- Repeat: 同じ物理キーのKeyDown repeatはKeyUpまで抑制し、1回の文字入力として扱う
- Verification: shifted文字、host Shift無視、直接操作キー、repeat、Command/Ctrl、入力先交換時の解放を
  Desktop unit testで検証
- Focus lifecycle: native file dialogはmain windowをownerとして開き、閉じた後にownerへfocusを戻す。
  windowがfocusを失った場合は、届かなかったKeyUpによる押下状態を残さないよう、直接keyをreleaseして
  character repeat抑制状態もclearする

### Desktop PC-1245フルソフトウェアキーボード

- Date: 2026-10-03
- PGP files: `Pc1245KeyboardLayout.kt`、`Pc1245KeyboardLayoutTest.kt`、`Main.kt`
- Reference repository: Pokecom GO（read-onlyを維持）
- Reference files: `pc1245mainkey.png`、`include_keys_1245.xml`、`SubActivity1245.java`
- Layout: 実機相当の14列×4段に全キーを配置し、ENTERの2列幅と上段左側の空きを保持する。
  主刻印、SHIFT刻印、BASIC命令刻印をデータとして分離し、利用可能幅に対して等比で配置する
- Input: 各画面キーはホスト文字変換と自動入力キューを経由せず、対応する`PocketKey`をpointer downから
  release/cancelまで直接保持する。SHIFTも通常キーとしてROMへ渡す
- Verification: 全`PocketKey`が重複なく1回現れること、各行が14列内で重ならないこと、QのSHIFT刻印を
  Desktop unit testで検証

### Desktop開発用PC-1245 ROM自動起動

- Date: 2026-10-03
- PGP files: `DesktopRomLocator.kt`とtest、`Main.kt`、`run-pgp.command`、README
- Discovery: `PGP_PC1245_ROM`を優先し、未指定時は作業ディレクトリとその親から
  `local-data/roms/pc-1245/pc1245mem.bin`を探索する。ROM byteや絶対パスは保存しない
- Startup: ROMが見つかれば既存ImporterとFactoryでSessionを生成し、自動的にRUNを開始する。
  見つからない場合はエラーにせず従来のSelect ROM操作を維持する
- Distribution: `local-data`はGit管理外であり、配布物とCIにROMを含めない。自動探索は利用者が明示的に
  配置したローカルROMだけを対象にする
- Verification: 環境変数優先、相対パス、親ディレクトリ探索、ROM不在をDesktop unit testで検証

### Desktop LCD文字間隔

- Date: 2026-10-03
- PGP files: `CharacterCellGeometry.kt`とtest、`Main.kt`
- Design: Core SnapshotはLCD RAMに対応する16文字×5dot＝80列を維持し、Desktop描画geometryだけで
  各文字間へ1dot幅の空白を挿入する。末尾には空白を追加せず、表示上は80dot＋15gap＝95列となる
- Layout: Canvas aspect ratioも80列基準から95列基準へ変更し、空白追加で文字dot自体が横につぶれないようにする
- Verification: 文字境界の4→6、9→10、最終dotの79→94と、合計95列をDesktop unit testで検証

### BASICテキスト共通escape parser

- Date: 2026-10-03
- PGP files: `BasicTextParser.kt`とtest、`docs/reference/BASIC_TEXT_FORMAT.md`
- Reference repository: Pokecom GO、pcwav（ともにread-onlyを維持）
- Reference behavior: Pokecom GOの`\\PI`、`\\SQR`、`\\EX`、`\\BX`、literal backslashと、
  PCWAVの任意byte `\\xNN`およびunknown byteの可逆出力
- Design: UTF-8テキストを通常文字、論理特殊文字、Raw Byte、改行へ機種非依存で字句解析する。
  `π`と`√`は入力aliasとして受理するが、Raw Byteへ機種固有の文字意味を付与しない
- Error handling: unknown、末尾backslash、不完全または非hexのRaw Byteを行・列付きエラーにする
- Scope: この段階ではファイル選択、機種別コード解決、ROMキー列生成、RAM配置には接続しない
- Verification: BOM、CRLF/CR、全named escape、Unicode alias、literal backslash、`\\xNN`、位置付きエラーを
  commonTestで検証

### PC-1245 ROM BASICキー列Compiler

- Date: 2026-10-03
- PGP files: `Pc1245RomBasicInput.kt`とtest、`BasicTextParser.kt`、BASIC Text仕様
- Design: parse済みの通常文字と論理特殊文字をPC-1245の連続tapへ変換し、改行をENTERとする。
  最終行に改行がない場合だけENTERを補完する。元テキストの行・列は各tokenに保持する
- Special symbols: `\\PI`はSHIFT/0、`\\SQR`はSHIFT/DOT、`\\EX`はSHIFT/PLUSへ変換する。
  キーボード入力できない`\\BX`、Raw Byte、未対応文字は直接Tokenizerが必要なため拒否する
- Separation: CompilerはSession、実行cycle、ファイル、UIを所有せず、論理`PocketKey`列だけを返す
- Verification: 通常・shift文字、3種類の特殊文字、行ENTER、最終ENTER、BLOCK、Raw Byte、未対応文字と
  位置情報をcommonTestで検証

### Desktop BASIC ROM経由Merge読込み

- Date: 2026-10-03
- PGP files: `DesktopBasicLoader.kt`とtest、`DesktopEmulatorRunner.kt`とtest、`Main.kt`、README
- File boundary: Desktopだけがファイル選択とbyte読込みを担当し、UTF-8 decoderはmalformed/unmappable入力を
  replacementせず拒否する。CoreにはファイルパスやJVM charset APIを持ち込まない
- Execution: parse・compile済みキー列をPROGRAMモードで入力し、通常のhost-time Plannerを経由せず
  キー保持・間隔に必要なemulated cyclesを同期的に消化する。読込前がRUNNINGなら終了後に通常実行を再開する
- Semantics: 現段階は`NEW`を送信しないMerge方式。既存行と同じ行番号はROMの通常操作として置換される
- Failure: invalid UTF-8、escape parse、ROM入力非対応を区別して表示し、Core faultでは高速入力を停止する
- Verification: UTF-8成功・失敗、parse/compile error伝搬、即時キー遷移、実行状態復帰、pause維持をDesktop testで検証

### BASIC行番号直後のコロン正規化

- Date: 2026-10-03
- PGP files: `Pc1245RomBasicInput.kt`とtest、BASIC Text仕様
- Behavior: 各行の先頭行番号直後にある最初のコロンをSPACEキーへ変換する。行番号とコロンの間の
  空白を許容し、BASIC文本体に現れるコロンはPC-1245のSHIFT/Iとして保持する

### PC-1245 BASIC方言定義

- Date: 2026-10-04
- PGP files: `BasicDialect.kt`、`Pc1245BasicDialect.kt`とtest、PC-1245機種仕様
- Reference repository: Pokecom GOの`SubActivity1245.java`と`SubActivityBase12xx.java`をread-only参照
- Design: 方言ID、プログラム境界、行終端、行番号方式、文字、特殊記号、キーワードを機種依存データとして
  定義する。Tokenizer、RAM操作、UI、ホスト文字入力とは分離する
- Important distinction: 表示記号のπ/√ (`0x19`/`0x1a`) とBASICキーワードのPI/SQR
  (`0xbd`/`0x87`) は別の意味として保持する。予約・未割当コードは推測で埋めない
- Correction: 参照実装のtoken `0xb8`にある`MARGE`はマニュアル表記に合わせて`MERGE`と定義する。
  旧誤記をcanonical出力へ引き継がず、必要なら将来のテキスト入力aliasとして別に扱う
- Memory metadata: BASIC領域`0xc000`、開始pointer `0xc6e1..0xc6e2`、終了pointer
  `0xc6e3..0xc6e4`をPC-1245固有定義として保持する
- Character aliases: `0x11`と`0x50`はいずれも空白へdecodeするが、新規encodeでは`0x11`をcanonicalに使う
- Verification: envelope、3桁OLD行番号方式、代表文字、大小文字正規化、特殊記号とkeywordの区別、
  未対応文字・keywordをcommonTestで検証

### PC-1245 BASIC Tokenizer

- Date: 2026-10-04
- PGP files: `Pc1245BasicTokenizer.kt`とtest、`BasicDialect.kt`、`Pc1245BasicDialect.kt`
- Design: parse済みの共通BASICテキストを、`0xff` program envelope、OLD系2 byte行番号、機種固有の
  keyword/character code、`0x00`行終端へ変換する。エミュレーターSessionやRAMへは直接書き込まない
- Lexical rules: 行番号直後の任意colonを除去し、通常の構文空白は格納しない。文字列内はkeyword化せず、
  REM以降は先頭の区切り空白だけを除いて文字として保持する。`>=`、`<=`、`<>`は単一tokenにする
- Long lines: ROMの編集bufferを経由しないため、100文字を超える行も中間コードへ直接変換できる。
  ROM経由入力は手軽な短い入力用として別経路のまま維持する
- Safety: 行番号は1..999。Raw Byteの`0x00`と`0xff`はprogram framingを壊すため拒否し、
  未対応文字は元テキストのline/column付きerrorを返す
- Verification: 複数行program、文字列/REM、特殊記号、Raw Byte、比較演算子、行頭colon、180文字の行、
  行番号・文字errorをcommonTestで検証

### PC-1245 BASIC Detokenizer

- Date: 2026-10-04
- PGP files: `Pc1245BasicDetokenizer.kt`とtest、PC-1245 BASIC方言定義
- Design: OLD系program envelopeと行番号を検証し、文字・特殊記号・keywordをcanonical BASICテキストへ
  戻す。RAMやファイルI/Oには依存せず、入力破損はbyte offset付きerrorとして返す
- Preservation: 文字列とREM内のkeyword code、および予約・未知codeは命令として展開せず`\\xNN`へ退避する。
  π/√はUnicode alias、指数・block記号は曖昧な隣接escapeを避けるためRaw Byte表記をcanonicalに使う
- Formatting: keyword境界へ必要な空白を補い、Tokenizerが生成したprogramは
  Tokenize → Detokenize → Tokenizeで同じbyte列へ戻る
- Verification: 読みやすい複数行出力、文字列/REM、特殊文字、未知code、往復一致、開始/終了marker、
  不正行番号、行終端欠落、trailing dataをcommonTestで検証

### PC-1245 BASIC Program Memory Loader

- Date: 2026-10-04
- PGP files: `Pc1245BasicProgramMemory.kt`とtest、`EmulatorSession.kt`、`EmulatorFactory.kt`
- Design: program imageとmachine memoryの変換をTokenizer、ファイルI/O、UIから分離する。公開Sessionには
  `loadBasicProgram`と防御的copyを返す`basicProgramSnapshot`だけを追加し、任意メモリ書込みは公開しない
- Layout: ROMが管理する開始pointer `0xc6e1..0xc6e2`を読み、pointer直前`0xc6e1`までを格納上限とする。
  終了pointerはimage最終byteを指すPokecom GO互換のinclusive形式で更新する
- Atomicity: pointer、program構造、容量を全て検証してから書き込む。短いprogramへ置換した場合は、
  以前の終了位置までの残骸をzero clearする
- Verification: load/extract一致、pointer更新、旧tail clear、不正pointer、壊れたimage、容量超過、
  error時のmemory不変をcommonTestで検証

### Desktop BASICファイル入出力

- Date: 2026-10-04
- PGP files: `DesktopBasicLoader.kt`とtest、`Main.kt`、`DesktopEmulatorRunner.kt`
- Load BASIC: UTF-8 `.bas`を厳密decodeし、共通parser、PC-1245 Tokenizer、専用Session loaderを通して
  RAMへ直接配置する。Runnerは先にpauseし、長い行と特殊codeをROM編集bufferなしで扱う。
  load前がRunningなら成功・失敗後ともRunningへ戻し、load前がPausedならPausedを維持する
- Save BASIC: Session snapshotをDetokenizerへ渡し、canonical UTF-8テキストとして保存する
- Type BASIC: 従来のROM経由キー入力は短いprogramを手軽に入力する別機能として残し、UI名で区別する
- Error reporting: UTF-8、parse、tokenize、memory pointer/capacity、detokenize、host file I/Oを段階別に表示する
- Verification: Desktop codecでUTF-8 sourceからprogram image、canonical sourceへの復元、invalid UTF-8、
  tokenize errorをunit testで検証
- Verification: `10:PRINT A:B`と`20 :PRINT`についてprefixだけがSPACEになることをcommonTestで検証

### PC-1245追加VRAMミラー

- Date: 2026-10-03
- PGP files: `Pc1245MemoryBus.kt`、`Pc1245MemoryBusTest.kt`、PC-1245機種仕様
- Provenance: PGP作者から提供された非公式実機情報。Pokecom GO参照実装には未反映
- Behavior: 従来の`0xf800..0xffff`に加え、`0xe800`、`0xe900`、…、`0xef00`の各256 byte pageも
  同じVRAMのmirrorとして扱う。どのpageへの書込みも`0xf800..0xf8ff`へ正規化して全16 pageへ反映する
- Verification: `0xe900`と`0xffff`への書込みが`0xe800..0xefff`、`0xf800..0xffff`の対応offsetから
  同一値として読めることをcommonTestで検証

### `.pgrom` v1 manifestとDesktop ZIP入出力

- Date: 2026-10-04
- PGP files: `RomPackageManifest.kt`とtest、`DesktopRomPackage.kt`とtest、
  `DesktopRomLoader.kt`、`Main.kt`、ROM Package仕様、Roadmap
- Core: manifestとcomponentをKMP共通モデルとして定義し、format/version、ID、role、相対path、
  component ID/path重複、宣言size、SHA-256、実entryとの一致をまとめて検証する。成功時だけ`RomSet`を生成する
- Desktop: ZIPとUTF-8 JSON、SHA-256をJVM層で扱い、`.pgrom`の読込みと`RomSet`からの書出しを実装する。
  Select ROMは拡張子`.pgrom`を判別し、従来64KiB ROMとの両方を読み込める
- Safety: 圧縮ファイルサイズ、entry数、entry単体と展開後合計の上限を設け、絶対path、backslash、空segment、
  `.`、`..`、entry重複を拒否する。manifest未記載entryはv1仕様どおりwarningとして保持する
- Verification: package往復、Session生成、digest不一致、path traversal、共通validatorの複合errorとwarningをテストする

### PC-1245 Legacy ROMから`.pgrom`へのDesktop変換

- Date: 2026-10-04
- PGP files: `DesktopRomPackageConverter.kt`とtest、`Main.kt`、README
- Flow: `Convert ROM`で32KiBまたは64KiB legacy imageを選択し、既存`Pc1245FlatRomImporter`で正規`RomSet`へ変換後、
  `.pgrom` writerで保存する。変換元ファイルを直接ZIPへ入れず、未使用領域はパッケージへ持ち込まない
- Verification: 生成packageをreaderで再読込みし、machine ID、内部8KiB、外部16KiBを確認する。
  不正なlegacy imageはpackageを生成する前に拒否する
- Compatibility: PC-1245で必要な最大ROM addressは`0x7fff`のため32KiB形式を正規入力として許可する。
  Pokecom GO互換64KiBも維持し、後半32KiBのダミー内容に関係なく同じ`.pgrom` componentを生成する
- Real ROM verification: ローカルROMが存在する場合、64KiB実データを`.pgrom`へ変換して再読込みし、
  component構成、warningなし、Session生成、100万cycleでfaultせず起動することをDesktop smoke testで確認する。
  ROMがないCIではテストをskipし、ROM byteと生成packageは保存・コミットしない

### PC-1245物理ROM別Importer

- Date: 2026-10-04
- PGP files: `Pc1245ComponentRomImporter`とtest、`DesktopRomPackageConverter.kt`とtest、
  `Main.kt`、README、ROM Package仕様
- Standard input: PC-1245は内部ROM 8KiBと外部ROM 16KiBを別ファイルとして受け取る。
  ファイル名やPokecom GOのaddress-space containerには依存せず、UIで選択したslotと正確なサイズを検証する
- Compatibility: 32/64KiB Flat ImporterはPokecom GO互換のLegacy経路として維持し、標準package作成経路とは分離する
- UI: `Convert ROM`を`Create ROM Set`へ変更し、内部ROM、外部ROM、保存先を順に選択する。
  manifestとSHA-256はPGPが生成し、利用者による編集を要求しない
- Bank-ready design: 将来のバンク機ではMachine Definitionがbank数を定義し、16KiBの各物理ROMを
  bank番号slotへ割り当て、正規`RomSet`では番号順に連結する
- Temporary UI limitation: 現在の`Create ROM Set`はPC-1245専用の仮実装で、内部ROM、外部ROM、保存先の
  native file dialogを順番に表示する。2ファイルでは利用できるが、バンク機へこの方式を拡張しない
- Deferred Import Wizard: 1つの画面に必要なcomponent/bank slot、選択済みファイル名、期待size、検証結果を
  一覧表示する。物理ROM別入力とPokecom GO互換入力を切り替え、Legacy Flat/Split形式から`.pgrom`を
  作成できるようにする。複数bankの一括選択と順序確認もこの画面で扱う
- Current legacy behavior: PC-1245のPokecom GO互換32/64KiB imageは`Open ROM`から直接実行できるが、
  Desktop UIから`.pgrom`へ保存する操作は未提供。Core/Converterの変換経路は維持し、Wizardから接続する
### Cycle-timed SC61860 buzzer PCM

- Reference repository: [PC-1251 Emulator](https://github.com/woriguchi/pc1251-emulator)
- Reference commit: `6ff6fa9b807925215d753f6a5d940a1290d43089`
- Reference files: `pc1251emu/audio.py` and the C-port event timing in `pc1251emu/machine.py`
- Provenance/license: MIT License, Copyright (c) 2026 origuchi. The notice is retained in
  `THIRD_PARTY_NOTICES.md`.
- PGP files: `Pc1245Buzzer.kt`, `DesktopAudioPlayer.kt`, and their tests.
- Porting decision: Pokecom GO's command hooks, inferred frequencies, `Beep.java`, and fixed
  Android `AudioTrack` clips are not ported. PGP records the SC61860 C-port mode against emulated
  CPU cycles, averages transitions within each PCM sample, applies the piezo-style high-pass
  filter, and streams the resulting PCM through a platform adapter.
- Modes 0/4 are LOW, 1/5 are HIGH, 2 is the internal 2 kHz oscillator, 3 is the internal 4 kHz
  oscillator, and cassette-derived modes 6/7 are currently rendered LOW.

### PC-1251 ROM, memory, and LCD foundation

- Reference repositories: [PC-1251 Emulator](https://github.com/woriguchi/pc1251-emulator) at
  `6ff6fa9b807925215d753f6a5d940a1290d43089`, and Pokecom GO at
  `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`
- Reference files: PC-1251 Emulator `pc1251emu/machine.py`; Pokecom GO
  `Sc61860_1251.java` / `MainLoop1251.java`
- Provenance/license: PC-1251 Emulator is MIT licensed; its notice is retained in
  `THIRD_PARTY_NOTICES.md`. Pokecom GO is a same-author reference implementation.
- PGP files: `core/.../machine/pc1251/Pc1251Rom.kt`, `Pc1251MemoryBus.kt`, and `Pc1251Display.kt`.
- PC-1251 is implemented as a separate Machine rather than adding model branches to PC-1245.
- The physical writable ranges are 0xB800..0xC7FF and 0xF800..0xF8FF; 0xB000..0xB7FF aliases
  0xB800..0xBFFF. The 120 LCD columns use ascending 0xF800..0xF83B and descending
  0xF87B..0xF840 addresses.

### 製品名とプラットフォーム別の役割

- Date: 2026-10-04
- Project codename: リポジトリとプロジェクト全体は引き続き`Pokecom GO Plus（PGP）`と呼ぶ。
- Desktop product: macOS／Windows／Linux向けの統合開発環境を`Pokecom GO Studio`とする。
  エミュレータに加え、デバッガ、アセンブラ、ディスアセンブラ等の開発支援機能を提供する。
- Mobile product: Android／iOS向けの実行専用エミュレータを`Pokecom GO Player`とする。
  既存Pokecom GO相当の実行・操作機能を対象とし、Studioの開発支援機能は搭載しない。
- Shared foundation: StudioとPlayerは同じKotlin Multiplatform Emulator Coreを使用する。
  UI、ファイル選択、音声出力、アプリのライフサイクルは各製品・プラットフォーム層に置く。

### PC-1251機種別文字入力とキーボード表示

- Date: 2026-10-04
- PGP files: `Pc1251CharacterInput.kt`、`Pc1245KeyboardLayout.kt`、`DesktopKeyboardInput.kt`、`Main.kt`とtest
- Character mapping: ホスト文字`(`と`)`は、PC-1245では`SHIFT`→`1`／`2`、PC-1251では
  `SHIFT`→`↓`／`↑`へ変換する。Desktop入力は接続中のEmulator Sessionのmachine IDから変換規則を選ぶ。
- Software keyboard: PC-1251では括弧を矢印キーのSHIFT側へ表示し、数字`1`／`2`から除く。
  PC-1245の固定予約語ラベルはPC-1251へ流用しない。
- Deferred behavior: PC-1251のユーザー登録可能な予約語ショートカットはROMの通常動作に任せ、Studio側での
  登録内容の解釈・表示は当面実装しない。
- Shared hardware: 現時点で同一と確認済みのキーマトリクス走査は`Pc1245Keyboard`を共有し、文字入力規則と
  画面上のlegendだけを機種別に分離する。

### PC-1251 RSVモード入力

- Date: 2026-10-04
- Reference: Pokecom GO `Sc61860_1251.inb()`および`SubActivity1251`の3位置モードスイッチ。
- PGP files: `Pc1251Keyboard.kt`、`EmulatorSession.kt`、`EmulatorFactory.kt`、`Main.kt`とtest
- Design: 物理キーのmatrix走査はPC-1245実装へ委譲し、PC-1251固有のRUN／PRO／RSV接点だけを
  `Pc1251Keyboard`で実装する。`OperatingMode.RESERVE`を公開APIへ追加した。
- Display: RUN／PRO／RSV表示は架空のLCD RAM byteから推測せず、モードスイッチの状態をSnapshotへ反映する。

### PC-1251 ROM・LCDアドレスミラー

- Date: 2026-10-04
- Reference: Pokecom GO `Sc61860_1251.memr()`／`memw()`。
- ROM: `0x2000..0x3fff`の読出しを外部ROM `0x4000..0x5fff`へ割り当てる。書込み可能領域にはしない。
- LCD: `0xe800..0xefff`および`0xf900..0xffff`を、下位8bitが同じ`0xf800..0xf8ff`へ
  正規化する。どのミラーページへ書き込んでもLCD Snapshotと全ミラー読出しへ同じ値が反映される。
- RAM: `0x8000..0x9fff`を`0xa000..0xbfff`へ、`0xd000..0xd7ff`を`0xc000..0xc7ff`へ写す。
  PC-1250はPC-1245と同じく`0xb000..0xb7ff`と`0xb800..0xbfff`を最終的に
  `0xc000..0xc7ff`へ写す。PC-1251は`0xb000..0xb7ff`を`0xb800..0xbfff`へ写す。
  PC-1255では`0xb000..0xbfff`が独立RAMなのでミラーとして潰さない。
  PC-1250実ROMはBASIC program pointerを論理address`0xb830`へ設定し、ミラー経由で物理RAM
  `0xc030`へ格納する。
- Open question: PC-1245／1250の`0xb000..0xb7ff`および`0xb800..0xbfff`が実機で本当に
  `0xc000..0xc7ff`のミラーとして動作するかは未確認。現在はPokecom GOの書込み変換と実ROM起動結果に
  合わせた実装を維持するが、資料または実機上のread/write試験で再検証する。確認できるまでは、この挙動を
  確定仕様として他機種へ一般化しない。

### PC-1251 ROM経由BASIC入力

- Date: 2026-10-04
- PGP files: `Pc1251RomBasicInput.kt`、`Pc1245RomBasicInput.kt`、`DesktopBasicLoader.kt`、`Main.kt`とtest
- Design: BASIC行解析と行番号直後のcolon処理は共有し、文字から実機キー列への変換だけを機種別に注入する。
- PC-1251: `(`／`)`を`SHIFT`→`↓`／`↑`としてROM editorへ入力する。
- Special symbols: PC-1251キーボード資産`pc1251mainkey.png`で、`\\PI`＝`SHIFT`→`0`、
  `\\SQR`＝`SHIFT`→`.`、`\\EX`＝`SHIFT`→`+`を確認して対応する。キーlegendのない`\\BX`は
  行・桁付きのunsupported errorを返す。

### PC-1251 LCDシンボル整理

- Date: 2026-10-04
- Reference: Pokecom GO `MainLoop1251.state`とシンボル描画。
- Symbol bytes: `0xf83c`はDEF／P／G／DE、`0xf83d`はBUSY／SHIFT／RADとして扱う。
- Mode: RUN／PRO／RSVはLCD RAMではなく3位置モードスイッチの状態からSnapshotへ加える。
- Removal: PC-1251には存在しない`E`表示と、モード表示用に仮定していた`0xf83e`の監視を削除した。

### PC-1251実ROMスモークテスト

- Date: 2026-10-04
- PGP file: `RealPc1251RomSmokeTest.kt`
- Local input: `PGP_PC1251_ROM`、または`local-data/roms/pc-1251/pc1251mem.bin`を探索する。
- Verification: 実ROMを100万cycle実行してfaultしないこと、24文字×5dotのLCD Snapshot、
  RUN／PRO／RSVの3位置モード表示、`A`キー入力後のROMによるLCD更新を確認する。さらにPROモードで
  `10 PRINT (1)`を機種別キー列として入力し、ROMが保存した中間コードを再抽出・復号して元の行と照合する。
- Input timing: PC-1245用の短い自動キー間隔ではPC-1251実ROMが連続する`10`の`0`を取りこぼした。
  StudioではPC-1251に38,400cycle保持＋19,200cycle解放間隔を使用し、実ROM統合テストで欠落がないことを確認する。
- CI: ROMが存在しない環境ではテストをskipし、ROM byteやdigestを出力・保存しない。

### PC-1251 OLD BASIC token table確認

- Date: 2026-10-04
- Reference: Pokecom GO `SubActivity1245.cmd_tbl`と`SubActivity1251.cmd_tbl`。
- Finding: 0x00..0xffの全entryが一致するため、PC-1245とPC-1251は現在のOLD系Tokenizer／Detokenizerを
  共有できる。プログラム格納addressとROM経由の文字キー列は引き続き機種別に扱う。
- Studio: ROM選択guide、ROM経由入力error、Tokenizer errorには実行中または選択中の機種名を表示し、
  PC-1251操作中にPC-1245と表示されないようにする。

### PC-1251初回起動ROM検出

- Date: 2026-10-04
- PGP files: `DesktopRomLocator.kt`、`Main.kt`、READMEとtest
- Priority: 前回ROM履歴を最優先する。履歴がない場合は明示した環境変数、PC-1245、PC-1251の
  既定local-dataの順に探索する。
- Override: PC-1251は`PGP_PC1251_ROM`で絶対pathまたは作業directory相対pathを指定できる。

### PC-1250／1251／1255ファミリーとRAM容量

- Date: 2026-10-04
- Shared implementation: 3機種はROM構成、CPU、LCD、キーボード、OLD BASIC処理を共有し、
  `Pc1251FamilyModel`のmachine IDとRAM開始addressだけを機種差として持つ。
- Hardware ranges: PC-1250は`0xc000..0xc7ff`、PC-1251は`0xb800..0xc7ff`、
  PC-1255は`0xa000..0xc7ff`を搭載RAMとして扱う。既存のaddress mirrorは搭載範囲へ正規化した後で判定する。
- Emulator extension: `Pc1251FamilyMemoryMode.HARDWARE`は実機容量を再現し、`EXPANDED`は機種名にかかわらず
  PC-1255相当の10KiB RAMを公開する。従来のPGP動作とエミュレータ上の利便性を保つため、Coreの既定値は
  `EXPANDED`とした。
- ROM identity: 同じ物理ROM layoutを各machine IDのROM setへimportできる。Studioの機種選択、ROM経由入力、
  software keyboard、RSV modeも3機種を同じファミリーとして扱う。
- BASIC memory: Tokenizerが直接配置するprogramの下限は、最大容量モデルに合わせて`0xa000`とする。
  実際の開始位置はROMが初期化したprogram pointerに従う。
- Studio control: PC-1250／1251／1255選択時は`Expanded RAM`と`Hardware RAM`を切り替えられる。
  切替時は同じROMを選択中の設定でcold bootし、切替前が実行中なら実行を再開する。RAM、CPU、表示などの
  runtime stateは保持しない。ROM未読込時の選択は次回loadへ適用する。

### Machine Definition Catalog

- Date: 2026-10-05
- PGP files: `MachineDefinition.kt`、`EmulatorFactory.kt`、StudioのROM／BASIC／keyboard／runner層とtest
- Single source: 実装済み機種のID、表示名、family、OLD/S1/S2世代、bank方式、ROM layout、keyboard、
  BASIC方言、表示桁数、mode、clock、auto key timing、RAM profile対応を`MachineCatalog`へ集約した。
- Core: `EmulatorFactory.supportedMachineIds()`とsession生成時のfamily選択はCatalogを参照する。
- Studio: 機種buttonをCatalogから生成し、ROM Importer、package変換、ROM経由文字入力、物理keyboard、
  software keyboard、RSV表示、実行clock、auto key timingを定義値から選択する。
- Boundary: Memory Bus、LCD controller、I/Oなど機種固有回路の実装はCatalogへ押し込まず、familyごとの
  Machine実装に残す。Catalogは静的な能力と実装選択のmetadataを担当する。
- Validation: IDの一意性、UI順、PC-1251 family全modelの登録、世代・bank・mode・timingをcommonTestで固定する。

### PC-1245 Playerスキン

- Date: 2026-10-08
- Asset: `androidApp/src/main/res/drawable-nodpi/skin_pc1245.png`（1206×616 RGBA PNG）
- Origin: Pokecom GOで使用していた画像を、リポジトリ作者が所有する実機を参考に独自のイラストとして
  描き起こしたもの。メーカー提供画像や取扱説明書の画像を複製したものではない。
- Ownership: 画像の作成者およびPGPリポジトリ作者が同一であり、PGPでの利用・再配布を許諾する。
- Boundary: 画像resourceはAndroidアプリが所有する。設計canvas、表示範囲、LCD領域は`playerShared`の
  `PlayerSkinCatalog`で保持し、画像読込みと描画はCoreへ持ち込まない。
- Calibration: イラストにLCD境界線がないため、初期LCD矩形は調整可能な暫定値として分離した。
  Android端末上の表示確認後に座標だけを微調整する。

### PC-1245 Playerスキンキー入力

- Date: 2026-10-08
- PGP files: `PlayerSkinCatalog.kt`、`SkinDefinition.kt`、Android `PlayerApp.kt`
- Provenance: PGP独自設計。作者自身が作成したPC-1245スキン画像上でキー矩形を計測。
- Design: `PocketKey`と設計画像座標の矩形だけをPlayer共有層へ保持し、Composeのタッチ処理は
  Androidアプリへ限定する。描画と同じ`SkinLayoutEngine`の変換でキー領域を拡縮する。
- Behavior: スキン上のキーを押している間だけ既存の単一セッションへpressを送り、指を離すか
  gestureがキャンセルされたときに必ずreleaseする。スキンキーがある場合は仮設グリッドキーボードを表示しない。
- Verification: PC-1245の52キー領域、ENTERの横長領域、共有層のDesktop／iOS Simulatorテスト、
  Android unit test、Lint、debug APK生成を確認。

### Player Controller Display手動切替

- Date: 2026-10-08
- PGP files: `PlayerScreenState.kt`、`SkinLayoutEngine.kt`、Android `MainActivity.kt`／`PlayerApp.kt`
- Design: 表示モードと切替規則、スキン切出し計算はPlayer共有層に置く。Androidはボタンと画像描画、
  lifecycleだけを担当し、CoreやDesktopへ画面APIを持ち込まない。
- Behavior: `Enlarge display`でLCD周辺の`controllerDisplayRegion`へ切り替え、実行状態を保ったまま
  スキンのキーを非表示にする。切替時は押下中のキーを安全に解放し、`Show full device`で全体表示へ戻る。
- Scope: ゲームパッド接続検出と自動切替は後続。初期プロトはゲームパッドなしでも確認できる手動切替とする。
- Verification: 共有切替規則のDesktop／iOS Simulatorテスト、Android unit test、Lint、debug APK生成を確認。

### Player RUN／PRO／RSV動作モード

- Date: 2026-10-08
- PGP files: `PlayerSession.kt`、`PlayerScreenState.kt`、Android `MainActivity.kt`／`PlayerApp.kt`
- Source of truth: Coreの`MachineCatalog.supportedOperatingModes`と`EmulatorSession.setOperatingMode()`。
- Design: Player共有Sessionが現在モードと対応モード集合を保持し、未対応モードをCoreへ送らない。
  Androidは共有画面状態からボタンを生成するため、機種名によるUI分岐を持たない。
- Behavior: PC-1245にはRUN／PROだけを表示する。RSV対応機種では同じUIにRSVを追加する。
  モード切替前に押下中のキーを解放し、Reset後はCoreとPlayer状態の双方をRUNへ戻す。
- Verification: PROGRAMへの切替、PC-1245でのRESERVE拒否、ResetによるRUN復帰を共有層テストで確認。
  Desktop／iOS Simulatorテスト、Android unit test、Lint、debug APK生成を確認。

### Player固定スキン画面と操作メニュー

- Date: 2026-10-08
- PGP files: Android `PlayerApp.kt`、`strings.xml`
- Layout: エミュレーター画面から縦スクロール、タイトル、スキン外の常設ボタンを除去する。
  スキンは利用可能な幅と高さの両方を`SkinLayoutEngine`へ渡し、縦横比を保って常に一画面へ収める。
- Menu: Run／Pause／Reset、Controller Display切替、機種対応範囲内のRUN／PRO／RSVを右上の
  オーバーフローメニューへ集約する。
- LCD: スキンへ合成するLCD Surfaceの角丸を廃止し、調整可能な矩形領域をそのまま描画する。
- Boundary: この画面構成とメニューはAndroid UIの責務とし、CoreおよびDesktop UIへ持ち込まない。

### Android Playerスキンキー押下アニメーション

- Date: 2026-10-08
- PGP file: Android `PlayerApp.kt`
- Behavior: タッチ開始から解放まで対象キーへ半透明の明色を重ね、解放またはgesture cancellation後に
  120msでフェードアウトする。押下開始側は35msとして入力への即応感を保つ。
- Boundary: 視覚フィードバックはComposeだけが保持し、共有キー座標、PlayerSession、Core入力状態を変更しない。

### Android Playerフルスクリーン表示

- Date: 2026-10-08
- PGP file: Android `MainActivity.kt`
- Behavior: ステータスバーを非表示にし、ジェスチャーナビゲーションではナビゲーションバーも非表示にする。
  Compose側はカットアウト等を含む`safeDrawing`ではなく`navigationBars` Insetsだけを適用し、ステータスバー跡を
  残さず描画する。旧3ボタン／2ボタン方式では表示中のナビゲーションバーだけをスキン領域から除外する。
- Detection: Androidに公開されたナビゲーション方式APIがないため、platformの
  `config_navBarInteractionMode`を参照する。取得できない場合はナビゲーションバーを残す安全側へ倒す。
- Lifecycle: windowがfocusを取り戻したときにもsystem bar方針を再適用し、一時表示後の状態を復元する。
- Boundary: system bar制御はAndroid hostだけの責務とし、Player共有層やCoreへ持ち込まない。

### PlayerモバイルUI後続要件

- Date: 2026-10-08
- Status: Android最小プロトタイプの現時点での区切り。以下は未実装の後続要件。
- Orientation: 縦持ちへは回転させず横向き専用とする。ただし端末を左右反転した場合は、両方の横向きへ
  180度追従する。Androidでは`sensorLandscape`相当を候補とする。
- Menu visibility: スキンと重なっても識別できるよう、3点メニューへ薄いグレーの円形背景を付け、
  アイコンとタッチ領域を現在より大きくする。
- LCD status: 16桁のdot matrixだけでなくSHIFT、DEF等の機種別statusを表示する。PC-1245／1251／1261系の
  RUN／PRO／RSVは実機ではハードスイッチだが、Playerでは現在モードをLCD statusとして明示する。
- Information menu: バージョン、プライバシーポリシー、Pokecom GO Studioへの案内を3点メニューへ追加する。
- Program I/O: 各種プログラムのロード／セーブも3点メニューから実行する。PlayerではStudioのProject、
  Assembler、Debuggerを導入せず、エミュレーターへ読み書きする実行用プログラムのファイル入出力として扱う。
  対象形式、機種別ロード先、上書き確認、Android／iOSのdocument picker／共有方法は実装前に整理する。
- Open decision: 機種切替を3点メニューへ置く案を検討する。ROM Libraryからの選択、Session再生成、
  実行・表示モードの復元まで含めて画面遷移を設計してから実装する。

## PC-1261 initial support (2026-10-10)

- Reference provenance: Pokecom GO commit `b1ab80c0bd8a91266ebb07bee5bd6c1396e91d24`,
  `Sc61860_1261.java`, `MainLoop1261.java`, `SubActivity1261.java`, and `KeyBoard1245.java`.
- The historical `pc1261mem.bin` is accepted only as a 64 KiB import container. PGP retains
  physical ROM bytes at `0000-1FFF` and `8000-FFFF`; bytes at `2000-7FFF` are discarded and
  runtime RAM is initialized independently.
- Writable memory is `2000-67FF`. Writes in `2000-3FFF` use the verified `address & 28FF`
  decode and `2000-2FFF` writes are mirrored to `3000-3FFF`.
- LCD columns use `2000-203B`, `2800-283B`, `2040-207B`, and `2840-287B`; symbol registers
  use `203D` and `207C`. The host snapshot exposes 24 characters by 2 rows of 5x7 dots.
- Keyboard scanning deliberately reuses the PC-1245 matrix while mode contacts use the
  PC-1251 RUN/PRO/RSV behavior. The machine clock is 768 kHz, matching the reference.
- BASIC storage starts at `4080`; start/end pointers are `66E1/66E2` and `66E3/66E4`.
  Encoding shares the S1 container with PC-1350 but uses the smaller PC-1261 command table.
- PC-1260 and PC-1262 remain separate follow-up work. They must not be modeled as capacity-only
  aliases until their ROM and address differences are verified.


## Studio / Android Player gamepad input (2026-10-11)

- This is new host functionality, not a port of an external emulator. Core remains free of device,
  UI, filesystem and threading APIs. Existing PocketKey press/release behavior is reused.
- Desktop uses the published LWJGL 3.4.1 SDL3 binding and its matching native artifact for the
  current OS/CPU. macOS calls run asynchronously on the Cocoa main queue through libdispatch;
  SDL does not own video or audio. The jpackage runtime includes `jdk.unsupported` for LWJGL.
- Android uses public KeyEvent / InputDevice / MotionEvent APIs. Digital d-pad HAT reports are
  normalized alongside key reports; analog sticks and triggers do not generate pocket keys.
- `hostInput` owns the single setting, single-device selection and per-input key ownership.
  `controllerUi` supplies the same diagram / key picker / save flow to both applications.
- Each of the 10 controls maps to one PocketKey or unassigned. There are no profiles, macros,
  analog conversions or simultaneous multi-controller routing. Preferences overwrite one setting.
- Keyboard, on-screen keys and the automatic key queue share key ownership with the pad.
  Gamepad presses clear pending command-history input without adding gameplay to saved history.
- Primary API sources and implementation / validation scope are recorded in `docs/development/GAMEPAD_DESIGN.md`.
  License texts are preserved in `docs/licenses` and copied into application distributions.

## Studio external-editor settings and program provenance

Original host-side UI additions: persisted editor commands reuse the existing direct
ProcessBuilder launch path; no shell is invoked. Explicit OS-editor settings override
PGP_EDITOR, while users without a saved setting retain the legacy environment fallback.
Project close clears workspace monitoring and previews without resetting the emulator.
Program provenance is updated only after a successful project or individual load; individual
loads clear the project BASIC baseline. Display and preferences remain in desktopApp, with
no core API or emulator-thread changes.

On macOS, the `code` executable is resolved from PATH or the bundled VS Code launcher
in system/user Applications directories, preserving all command arguments. This avoids
requiring Finder-launched Studio users to install the terminal CLI or configure shell startup.

Source information now belongs to the immutable project-tree snapshot, refreshed by the
existing host-side polling scan. Header edits change snapshot equality even when the file list,
size, and timestamp stay unchanged, so Compose refreshes the selected description without
reselections or filesystem reads during rendering.


## PC-1401初版実装 (2026-10-11)

- 上記調査のPokecom GO `3e3ae15aa004f10a9959764bfe5472cbeaa449f9`の機種仕様を、
  PGPのSession境界へ移植した。外部プロジェクトの変更、MAMEコードのコピーは行っていない。
- CPUは既存SC61860を共用。8K+32K ROM、4K RAM (`3800..47ff`)、LCD decode / mirror、
  16桁LCDと専用symbol、IA/IB matrix、BREAK入力を専用実装した。
  複数select時はPokecom GOと同じIA優先・最低bit方式を保持する。
- Clockは288,000 cycles/sを暫定採用し、既存CPU timerとブザーのサンプル時間に同じ値を使う。
  実機のBEEP音程・WAIT時間との校正は未実施。既存機種のブザー既定clockは変更しない。
- CAL/RUN/PROは物理スイッチとして合成せず、CAL/BASICキー列をサイクルで投入し、
  ROMがLCDへ書いたmode bitを現在モードとして公開する。Hostの自動文字入力はmode遷移完了を待つ。
  自動文字入力の60ms保持/20ms間隔では実ROMで文字が欠落したため、
  保持60,000 cycles / 間隔30,000 cyclesへ延長した（約208ms / 104ms）。
- S1行構造を共用し、科学関数語彙とPI/SQRT特殊文字、専用キー入力列、
  `46e1/46e3`のprogram pointerを追加した。容量超過や不正形式は書込み前に拒否する。
  最大容量付近での変数・配列領域との競合、およびNEW/MEM全挙動は後続確認が必要。
- 機種Catalog / Factory、ROM登録・変換・locator、StudioのBASIC入出力とホスト入力、
  Playerの機種キー、ゲームパッドの選択可能キーへ統合した。
  関数キー6列×3行と主キーはplayerSharedの共通表示定義に分け、StudioはLCD下へ縦配置する。
  Androidの画像skinなし表示はLCD・関数・主キーを縦スクロールで配置し、主キーには横スクロールを残す。
  Playerの関数drawerは今回実装しない。
- 検証: 自作データによるROM ownership / map / alias / LCD / 全キー / BASIC / 容量拒否 /
  addressed load / ROM identityと、Studio/Playerの統合テストを追加。
  ignoredのローカル実ROMで起動、CAL→RUN→PRO→RUN→CAL、FAC・DIM・BEEP・POKEを含むBASIC実行、
  ROMキー入力と直接コンパイル結果の一致を確認した。ROMや採取データは追加していない。
  指定JDK 21で `build :core:desktopTest` 成功。Android lintのKotlin metadata警告は残る。
  実機比較、Android実機の操作性、PC-1402、CLOAD/CSAVEのテープ処理は未検証／対象外。

- Studio実画面: ローカルROMをROM Libraryへ登録し、PC-1401選択・CAL起動・全キーの縦配置を確認。
  通常サイズとウィンドウ拡大時にLCD・関数・主キーが幅へ追従することを確認した。


### LCD角度単位の連結表示修正 (2026-10-11)

ユーザーの実機仕様指摘に基づき、DE / G / RADをbit順ではなく物理順で連結し、
DegreeはDEG、GradianはGRAD、RadianはRADと表示する。
StudioとAndroid Playerで共通のpresentation formatterを使用する。
PC-1245、PC-1250/1251/1255、PC-1401の分割segmentを対象とし、Coreの生bitは保持する。
PC-1261は参照MainLoop1261.javaで独立したDEGとRADの表示を確認したため、その区切りを保持する。
PC-1350/1360は現在のsnapshotに角度segmentを公開していない。


### PC-1401キー配置調整 (2026-10-11)

ユーザー提示画像に合わせ、Studioの関数キーを主キーと同じ15列gridの右6列に配置し、
主キーは数字・演算・memoryキーを1行ずつ上げて4行へ整理した。
HEX入力captionはEXP=A、POWER=B、ROOT=C、SQUARE=D、LN=E、LOG=Fを追記。
Playerの関数panelは従来の6列を維持し、空白9列をモバイル画面へ持ち込まない。


### StudioのProject編集操作 (2026-10-11)

新規Host機能としてProjectメニューにRename Project / Add Sourceを追加した。
表示名だけをManifestへ保存し、ディレクトリやsource pathは維持する。
ソース追加は選択したbas / asm / dmpだけをsrcへコピーして登録し、元ファイルを保持する。
同名path / source IDには連番を付け、Manifest書込み失敗時は今回のコピーを取り消す。
CoreへのファイルAPI追加はない。テストで永続化・元ファイル保持・衝突回避を検証し、
実画面の名前変更・ファイル選択・ツリー反映もテスト用Projectで確認した。
JDK 21の全体build / Core回帰成功。ツリー内ファイル名変更は今回対象外。


### Project名とフォルダ名の連動 (2026-10-11)

ユーザー指示によりRename Projectは表示名とフォルダ名を連動させる仕様へ変更した。
新規作成と同じfolderName規則を使用し、同一親内でフォルダ全体を移動する。
ソースの絶対パス、tree、監視、ロード元Manifestを移動後の位置へ更新する。
既存の別フォルダには上書きせず、Manifest保存失敗時には元位置への復元を試みる。

## 2026-10-11: SC61860 baseline clock audit

- Supersedes the earlier PC-1245 `576 kHz / 2 = 288,000 cycles/s` decision and
  PC-1350/1360/1261 oscillator-as-cycle-rate values. Android reference `cpuClockWait=2`
  is a host pacing heuristic, not evidence of hardware clocks per instruction cycle.
- Baseline: PC-1245/1250/1251/1255/1401 use 576,000 oscillator clocks/s divided by 3
  (192,000 instruction cycles/s); PC-1261/1350/1360 use 768,000 / 3
  (256,000 instruction cycles/s).
- Cross-check: PockEmul constructors explicitly use these oscillator/3 rates:
  https://github.com/matsumo/PockEmul/blob/master/src/machine/sharp/pc1250.cpp
  (PC-1245 derives from PC-1250),
  https://github.com/matsumo/PockEmul/blob/master/src/machine/sharp/pc1260.cpp
  (includes PC-1261),
  https://github.com/matsumo/PockEmul/blob/master/src/machine/sharp/pc1350.cpp
  (`Cpc13XX`, also inherited by PC-1360), and
  https://github.com/matsumo/PockEmul/blob/master/src/machine/sharp/pc1401.cpp.
  Read-only audit on 2026-10-11; no source copied or build dependency introduced.
- Primary corroboration: SHARP PC-1360 operation manual, printed p.124, BEEP:
  `T=(90+4n)*3.9 microseconds`, consistent with the 256 kcycle/s time unit.
  https://pockemul.com/wp-content/uploads/2021/03/PC1360_OM_DE.pdf
  PC-1250 service manual: https://pockemul.com/download/pc1250_service_manual.pdf
- CPU instruction cycle counts are unchanged. Host budgets, audio cycle-to-time conversion,
  and nominal 2 ms/500 ms CPU timer thresholds now use the same machine cycle rate.
  Timer latch/read/reset semantics remain unchanged; exact silicon divider behavior
  (including 500 vs 512 ms differences in other emulators) requires a separate hardware audit.
  Automatic typing and PC-1401 mode settling preserve their prior wall-time durations.
- Tests cover catalog rates, elapsed-time budgets, and nominal timers at both cycle rates.
  Real ROM boot/input smoke tests provide functional checks, not physical timing calibration.
- Future Settings adjustment multiplies this baseline, default 1.0. A host too slow to
  sustain the baseline cannot be corrected by a multiplier; no host benchmark is needed
  to define hardware time. Unlimited execution will bypass host pacing, not CPU cycle accounting.
- Validation: JDK 21 `build :core:desktopTest` passed (573 tasks), including
  Studio/Player timing budget tests and local-ROM smoke checks where ROMs were supplied.
  `:desktopApp:run` launched successfully. No Settings UI change in this step.

## 2026-10-11: Studio execution speed settings

- Settings / Emulation speed stores one Studio-wide selection, restored at startup and
  applied to newly loaded machines. Level 3 uses the audited machine baseline unchanged.
  Provisional levels 1–5 are 80/90/100/110/120%; ratios are centralized in
  `DesktopExecutionSpeed` for later calibration. Reset preserves the host preference.
- Unlimited bypasses elapsed-time pacing with bounded 4,096-cycle batches, yielding to
  the UI between batches. Pause, debugger and input transitions keep existing handling.
  Audio is muted/drained in unlimited mode so the device output cannot throttle execution.
  CPU timers continue to follow emulated cycles; commonMain gains no host APIs or threads.
- Validation: JDK 21 `build :core:desktopTest` passed. Runner tests check unlimited
  progress without elapsed host time, pause/reset, and return to baseline pacing.

## 2026-10-11: Assembly Workspace emulator load feedback

- PGP-native UI correction: report successful Build & Load with machine and byte
  counts, distinguish save/build failure from load failure, and display emulator
  project-load state directly under the action buttons. Build-only completion is
  marked as not loaded even when a previous version remains in the emulator.
  Explain that loading memory does not start the program.
- Uses the existing project/session identity and source-change tracking. No external
  code or ROM-derived data used. Interactive UI verification remains pending.
- JDK 21 full build, Core tests and createDistributable passed.

## 2026-10-11: Scrollable Assembly Workspace result field

- PGP-native presentation change requested by the user: consolidate Memory,
  Disassembly, Listing and Symbol Map into one bordered, read-only result field.
  Bound its height and provide vertical/horizontal scrollbars without wrapping
  assembly lines. Retain text selection/copy and reset scrolling on result updates.
- JDK 21 build, Core tests, createDistributable and diff checks passed.
  Interactive UI verification remains pending. No external code or ROM data used.

## 2026-10-11: Assembly Workspace output names and save guidance

- PGP-native UI/workflow correction prompted by the user's Workspace screenshot:
  output names were hardcoded to program.* and Assemble only updated the preview.
- Share output-path calculation between display and writer. A single assembly source
  uses its filename stem; multiple assembly sources retain the combined program.* name.
  Explain preview versus Build saving in the UI and Assemble completion status, and show
  whether each output file exists. Existing outputs are not removed when sources change.
- Regression checks cover filename agreement, preview without filesystem output,
  actual three-file generation and combined-source naming. JDK 21 build, Core tests,
  createDistributable and diff checks passed. No external code or firmware data used.

## 2026-10-11: Logical keyboard release spacing

- User reported that Logical Shift+1 left SHIFT active without entering a character;
  Physical input worked. Reproduced in Studio PC-1350 RUN mode. A later manually
  entered Q completed the expected character, isolating generated-tap spacing.
- PGP-native adjustment: increase the 20 ms automatic release gaps for PC-1245,
  PC-1261, PC-1350 and PC-1360 to 100 ms at each machine's instruction-cycle rate.
  Keep hold durations and the already longer PC-1251-family/PC-1401 gaps unchanged.
  No external implementation or ROM data copied.
- JDK 21 build, Core tests and createDistributable passed. Packaged Studio PC-1350
  Logical Shift+1 and consecutive shifted-symbol/letter input succeeded after restart;
  PC-1245 Logical Shift+1 also succeeded.

## 2026-10-11: Unlimited mode UI starvation fix

- User reproduction: selecting Unlimited hung Studio; persistence then repeated the hang
  when ROM autorun started at launch. Saved `executionSpeed=UNLIMITED` was confirmed and
  restored to LEVEL_3 using Java Preferences, leaving other settings intact.
- Supersedes the earlier `yield()` scheduling decision: Compose/AWT dispatch can resume
  the coroutine without returning to native event handling. Each bounded unlimited batch
  now has a real 1 ms suspension so input/rendering can run. Unlimited still bypasses
  machine-clock pacing, but this UI scheduling interval intentionally caps throughput.
- Verification: JDK 21 full build/Core tests and createDistributable passed (579 tasks).
  Packaged Studio UI check with PC-1401: select Unlimited, quit/relaunch with the saved
  selection, then Pause and open Settings successfully. Restored Level 3 afterward.

## PC-1245 Android Player display and lifecycle (2026-10-12)

- Original Player UI work; no external reference code copied. Core DisplaySnapshot symbols and
  PlayerSession operatingMode remain the sources of LCD status and hardware mode.
- Shared SkinDefinition holds LCD panel, symbol rectangles and mode rectangle. User screenshots
  and supplied manual image guided placement: dot rectangle (70,104,760,70), upper symbols y=64,
  SHIFT right-aligned, adjacent DE/G/RAD centered. Android uses the same skin transform in
  Controller Display, with 80% dot fill / 20% cell gap and unchanged character spacing.
- CPU RUNNING/PAUSED/FAULTED appears separately; the menu is a light-grey 48dp circular control.
- ROM import/restoration automatically starts CPU execution once the Activity is resumed.
  onPause stops frame callbacks and releases keys; ordinary resume stays PAUSED. Reset uses
  RUN hardware mode and PAUSED CPU state. Core/session factory create no scheduling resources.
- sensorLandscape requests both landscape directions. Size/orientation changes retain the
  Activity-owned session/RAM, release keys, refresh system bars, and let Compose recalculate.
  Process death restores ROM only, without RAM/save-state restoration.
- Sources: https://developer.android.com/guide/topics/manifest/activity-element and
  https://developer.android.com/develop/adaptive-apps/guides/app-orientation-aspect-ratio-resizability
  Android 16 targetSdk 36 can ignore orientation restrictions on >=600dp screens. The initial
  implementation had no large-screen opt-out; see the tablet correction below for the current policy.
- JDK 21 build and core desktop tests passed (some tasks up-to-date); lint emitted Kotlin
  metadata version mismatch messages while the build succeeded. No ROM test data committed.

- Device screenshots exposed cutout overlap in Controller Display and insufficient protection
  in full-device view. Android Player's root viewport now excludes the union of displayCutout
  and navigationBars insets. This also protects persistent controls and follows the cutout side
  on reverse landscape; overlapping insets use the maximum rather than summed padding.
  Skin image, LCD and touch regions still share one viewport transform.

## Player shared program I/O (2026-10-12)

- Extracted Studio's existing machine-selected tokenizer/detokenizer dispatch into Core
  BasicProgramCodec. This is internal PGP reuse, not a new external port. Studio keeps its
  desktop-specific description metadata/ROM-keyboard loading paths and adapts shared errors.
  Common UTF-8 decoding rejects invalid sequences; leading Studio # description lines are
  accepted without shifting diagnostic line numbers. Existing machine codec output is retained.
- PlayerProgramParser supports only explicit BASIC (.bas) and MEMORY_DUMP (.dmp). Reuses
  Core PgpMemoryDumpParser/Writer and ProgramImageLoader; no bin/hex/WAV/Project/ROM hook.
- PlayerSession exposes non-mutating check, revalidated apply, whole-BASIC export and one
  inclusive-address RAM-range dump export. Calls require PAUSED, never reset or auto-run,
  and leave operating mode unchanged. Host owns key release, user confirmation and serialization.
- Dump loads validate all segments and aliases, restrict writes to RAM, and protect active
  BASIC including its end marker and pointer words via physical-address comparisons. Invalid
  BASIC pointers/content block dump application until the machine has a valid program area.
  BASIC loads use existing all-input planning and pointer/old-text handling before mutation.
- Parsed inputs own BASIC bytes and target machine; applying checks the actual session again.
  Wrong-machine/running/faulted calls fail; export rejects ROM/LCD/control and alias overlap.
  Hosts receive copies of output bytes; no platform file/UI/time/thread API was added to Core.
- Synthetic tests cover BOM/line endings/Studio headers, BASIC and disjoint dump round trips,
  malformed input, protected addresses/aliases, capacity failure with zero mutation, stale
  confirmation revalidation, running/wrong-machine rejection, range checks, and special-symbol
  byte round trips for PC-1245/1401/1350. No ROM-derived fixtures are used.
- Android picker, confirmation dialogs, file writing and sharing are step 4 and remain unconnected.

- Future-target check found PC-1350's existing tokenizer has no named-special-symbol mapping;
  ordinary BASIC round trips are covered for 1350, special escapes for 1245/1401. This existing
  1350 limitation is left for that machine's support phase rather than silently remapping bytes.

## Android Player program document operations (2026-10-12)

- Step 4 connects shared bas/dmp I/O to Android overflow menu, format selection, explicit
  memory overwrite confirmation, whole-BASIC/range-dump export, SAF creation and share sheet.
  File extension is checked against selected format; streaming reads are capped at 2 MiB.
  Metadata uses OpenableColumns.DISPLAY_NAME. Filesystem/picker APIs remain Android-only.
- All emulator calls stay on Activity main thread. An IO executor reads/parses files and writes
  exported byte snapshots. Session identity is checked on completion and before confirmed load.
  Entering operations pauses/releases keys; modal operations suppress gamepad input.
  Cancellation/error/finished operations do not resume CPU or issue BASIC RUN.
- CreateDocument creates a new file (existing-file replacement is not implemented). Binary MIME
  is used for document creation to preserve custom bas/dmp suffixes. Share uses text/plain with
  EXTRA_STREAM, ClipData, read-only grants and FileProvider restricted to program-share cache.
  Unique share directories preserve earlier URI content. Background sharing is not launched.
- Sources: https://developer.android.com/training/data-storage/shared/documents-files and
  https://developer.android.com/reference/androidx/core/content/FileProvider
- Boundary tests cover bounded streaming, unsupported/mismatched extensions and address input.
  JDK 21 full build and Core desktop tests pass; real picker/share/dialog verification pending.
  User steps and device checks: docs/PLAYER_PROGRAM_IO.md. No ROM or ROM-derived fixture added.

## Player review fixes (2026-10-12)

- Supersedes the initial step-3/4 overwrite-confirmation and BASIC-protection policies above,
  per user review. Load now opens picker directly, detects bas/dmp from display-name extension,
  validates and applies without confirmation. Unsupported extensions remain rejected.
- Dump writes can replace BASIC text/end marker/pointer RAM including C000; no BASIC snapshot
  is required. All-segment placement validation, ROM/LCD/control restrictions and mirror checks
  remain. Tests cover C000 overwrite and further dump load after invalidating BASIC pointers.
- Reset now schedules CPU execution after restoring RUN hardware mode, using the resumed-host
  guard. It does not inject a BASIC RUN command. Normal resume after background stays paused.
- LCD status and mode labels use TextMeasurer with fixed measurement density/fontScale, then
  Canvas scales the measured layout uniformly to fit each skin rectangle. This includes actual
  line metrics and avoids a fixed dp text box clipping system-scaled sp text. SHIFT remains
  right-aligned and DE/G/RAD positions stay centered. Menu/dialog text retains system scaling.
- Overflow UI is an in-Activity hierarchy with scrim, bounded scrolling, outside dismissal and
  back navigation, replacing DropdownMenu's separate popup window. Input is released on open
  and gamepad routing is suppressed while the panel is visible. Real-device system-bar and
  font-setting validation remains pending; no ROM/test-data fixtures were added.

## Android Player ROM management and information (2026-10-12)

- New PlayerMachineCatalog exposes only Core machines with registered Player skins (currently
  PC-1245); PlayerRomEntry is a portable listing model. ROM bytes/metadata/archive/filesystem
  remain host-owned. Future 1401/1350 skins can reuse these selection/storage paths.
- Android importer now prepares validated canonical ROM and a fresh session without storing it.
  ROM manager separates import/replace/select/delete, confirms RAM loss, keeps existing session
  paused until success, and checks generation/session machine identity. Startup results cannot
  override a newer ROM operation. Selecting a stored ROM validates and creates before activation.
- App-private library prepares package/metadata/active-id writes before mutation, restores changed
  files on ordinary I/O failure, uses synced writes/atomic moves where supported, validates IDs
  against MachineCatalog, lists per-machine status, and clears active selection on active deletion.
  This is not a power-loss journal; failed rollback is surfaced as failure. No global session state.
- Successful new session uses RUN and automatic CPU start, retaining presentation mode. Deleting
  current ROM drops session to RomMissing; no implicit alternate-machine selection. ROM manager
  and information screens render in the Activity with safe insets and suppress gamepad input.
- Installed versionName/versionCode come from PackageManager. Privacy and Studio pages contain
  user-approved placeholder text only. All entries are accessible from the screen hierarchy.
- User-requested follow-up: successful dmp load schedules automatic CPU resume. BASIC load,
  failed/cancelled load and ordinary background return stay paused. Automatic start is consumed
  when the first frame runs; onPause preserves only an as-yet-unconsumed automatic start request.
- Synthetic library tests cover multi-machine independence, replacement rollback, delete/select
  behavior and invalid/path-traversal IDs. UI smoke on emulator-5554 confirmed saved-ROM startup
  RUNNING, hierarchy without status-bar reveal, ROM list, delete confirmation/cancellation, and
  installed version information. Destructive ROM changes and physical-device picker/share,
  rotation/font-scale and dmp auto-resume remain explicit device checks in PLAYER_PROGRAM_IO.md.
- No external emulator source was copied; no ROM/ROM-derived fixture committed.

## PC-1245 Android milestone acceptance (2026-10-12)

- User confirmed operation and explicitly accepted this as the end of the current PC-1245
  Android scope, requesting documentation review and commit/push. This supersedes the pending
  device-validation status in the preceding chronological implementation records. It does not
  claim exhaustive coverage of all Android devices/providers or release-distribution validation.
- Final behavior: bas/dmp extension detection without load confirmation; dmp may overwrite
  BASIC RAM including C000 and auto-resumes CPU on success; BASIC loads and failed/cancelled
  operations stay paused. ROM import/restore/replace/select and Reset automatically run CPU.
  RUN/PRO machine mode remains separate from RUNNING/PAUSED/FAULTED execution state.
- Landscape in both directions, cutout/navigation-bar insets, fitted LCD labels, 80/20 dot fill,
  in-Activity hierarchy, ROM library and information placeholders are included. ROM operations
  retain the current session until validated replacement succeeds; display selection is retained.
- JDK 21 full build/Core checks pass. Current suites: Core 408; Studio 195 (1 skipped);
  shared Player 20; Android 9; no failures. Some checks are up-to-date. Existing Android lint
  Kotlin-metadata version diagnostics remain while the Gradle build succeeds.
- Subsequent development: PC-1245 iOS, then PC-1401/1350 Android. WAV, Project/debugger, audio,
  save-state, external keyboard and controller-triggered automatic display changes are separate.
  This commit is not a release tag or publication; privacy/Studio text remains placeholder.

## Technical Preview 0.4 publication (2026-10-12)

- User authorized release preparation and publication. Version/tag 0.4.0-alpha.1, Studio
  numeric package version 1.3.0, Android versionCode 4. Privacy/Studio placeholder text and
  existing debug-signing strategy are disclosed; no production key or permissions added.
- Local JDK 21 full build/Core tests and macOS distributable/Run/Pause smoke succeeded.
  CI and manually dispatched four-platform packaging succeeded before annotated tag creation.
- v0.4.0-alpha.1 references 7ce33bc; tag workflow 38203642604 succeeded and published at
  2026-10-12 13:26 JST. All 7 package SHA-256 values match published lists and asset metadata.
- Release URL: https://github.com/digihori/pokecom-go-plus/releases/tag/v0.4.0-alpha.1
  Verification links and exact scope are in RELEASE_CHECKLIST_0.4.0-alpha.1.md. No ROM/keys
  published. Windows/Linux current UI smoke is not claimed; prior device checks are separate.

## External SC61 runner foundation (2026-10-12)

- User-provided SC61 Rev 0.04a Readme/C source and macOS executable informed the integration contract; provenance and measured behavior are in SC61_INTEGRATION.md. No external source was copied into Core and no SC61 build dependency was added.
- Desktop owns global settings, shell-free process execution, console capture, timeout and cancellation. This stage exposes a connection test; build selection and output import follow separately.
- Exit codes are preserved without assuming zero means successful assembly, because measured SC61 success returned 2 and one error returned 0.

## SC61 DMP import (2026-10-12)

- SC61 adapter stages the main source in a fresh temporary directory while preserving its original parent as cwd. Original DMP/LST files are untouched; fresh artifacts are checked using SC61 success text rather than zero exit status or timestamps.
- Desktop dump loading accepts one trailing DOS EOF marker. Core memory placement validation is reused without copying SC61 source. Build/load integration and live BASIC collision checks remain the next stage.
- Actual user-provided macOS SC61/FENSCER produced a 13,568-byte image; PC-1261 placement correctly rejected address 0x6800 as unmapped. Details/provenance: SC61_INTEGRATION.md.

## SC61 build/load integration (2026-10-12)

- Desktop Workspace, Build / Build & Load and Quick Assemble now read the global assembler settings. SC61 output is converted into the existing project artifact rather than adding an alternate memory write path.
- Build reuses one compilation result for diagnostics, checks all inputs, then saves; loading reuses the existing atomic program loader and current BASIC placement checks. External source/symbol mapping remains unavailable until LST adapter work.
- Added integration tests for external build/load, preserving old build output after an error, overlapping sources and restoring the internal assembler. JDK 21 full build passed. Windows/GUI smoke remains unverified.

## SC61 listing and diagnostics (2026-10-12)

- Parsed the user-provided SC61 Rev 0.04a LST format and checked tab/INCLUDE behavior using the user's macOS executable on disposable inputs. Reference C PrintList/ErrPut was inspected read-only; no source was copied. Provenance/results: SC61_INTEGRATION.md.
- Desktop retains raw LST, imports symbols with declaration kinds, and resolves diagnostics to original sources only when content and line uniquely identify a file. SC61 omits INCLUDE file names, so ambiguous navigation remains unavailable.
- Changed workspace byte ownership checks to use the authoritative DMP image, preventing missing LST bytes (INCBIN/NOLIST) from hiding overlaps. Added regression tests; Core remains independent of external tool formats and IO.

## SC61 validation and usage guide (2026-10-12)

- Verified the actual macOS UI: global settings, connection test, Workspace/Build/Load, Quick Assemble, error preservation, VS Code line navigation and internal/external switching, using temporary test-only sources and the existing registered ROM without adding firmware to the repository.
- Verified the Windows SC61 PE32 binary under Wine 11.0 with an isolated prefix. Address/byte output matches native macOS for the 19-byte dialect/include sample; native Windows Studio and Japanese paths remain unverified.
- Fixed Disassembly presentation across adjacent DMP row segments after GUI smoke exposed a split CASE1. Added a boundary/gap regression test; full JDK 21 build and macOS distributable creation passed.
- User guide: EXTERNAL_ASSEMBLER.md; detailed evidence and remaining checks: SC61_INTEGRATION.md.

## Separate artifact build from load validation (2026-10-12)

- User requested that out-of-RAM output (including PC-1261 address 0x6800+) be accepted at least for Build, with Load treated separately. Configured assembly and project Build now retain the full 16-bit image and logical overlap validation while deferring hardware/physical aliases/BASIC placement to the existing atomic loader.
- SC61/FENSCER copied into a temporary PC-1261 project built all 13,568 bytes and 955 symbols successfully. Added a regression confirming unmapped output can be saved but its load fails before any partial memory write. Full JDK 21 build/tests and macOS distributable passed.


### Windows Studio feedback: large assembly preview, project panes and MSI menu

- Date: 2026-10-13
- User observations: Windows Workspace Assemble with SC61 becomes unresponsive although connection test and command-line assembly work; scaled UI leaves almost no project-tree space; MSI installation has no visible Start menu entry.
- Assembly preview: FENSCER produces an approximately 587 KB LST. Previously the UI generated the combined preview during composition and laid it out as one Text. Formatting now runs on Default, and a bounded LazyColumn renders visible lines. Full listing/map content remains available through Copy all and Build outputs.
- Project pane: add an upper draggable divider between project controls and files, with saved control height and a scrollbar for controls. Cap controls to leave room for files. The lower divider can reduce source information to 24 dp, and its effective height is bounded by the available pane. Drag handlers use updated callbacks.
- MSI: explicitly request the Windows Start menu group `Pokecom GO Studio`. Actual MSI registration/reinstallation remains a Windows acceptance check.
- Scope: preserve OS display scaling; do not infer a Windows font/DPI setting from a screenshot. Improve layout under constrained height instead.
- Verification: local JDK 21 full build and tests pass after the final pane refinement. Packaged macOS Workspace assembles 13,568 FENSCER bytes and remains responsive while scrolling the complete result. One earlier run returned a timeout without freezing the UI; its cause is unconfirmed. Windows hang resolution and MSI registration are not claimed as verified.
- Source: current PGP implementation and user feedback; no external code or ROM assets copied.


### SC61 milestone and Windows user feedback follow-up

- Date: 2026-10-13
- User confirms Windows SC61 connection test/Build success and "program registration" success after the fixes; the exact registration destination was not specified. Workspace hang resolution, detailed Unicode/cancellation/editor checks and actual program Load/run remain separate pending checks.
- SC61 integration is paused at the completed initial milestone by user request. Future assembler-type selection (Internal/SC61/yasm.pl/generic) is design work, not current functionality.
- User defers MSI upgrade identity/version handling to the next correction cycle; see ROADMAP. Preserve existing installer identity where possible rather than assigning an unrelated UpgradeCode.

## 2026-10-13 — MSI、音声、BRK、iOS CI

- 公開0.4 MSIのProperty tableとasset SHA-256を確認しUpgradeCodeを継続利用。次の配布版は1.3.1。公開タグ・履歴・assetsは変更しない。
- Coreの無音PCMは空配列となるため、Desktopはqueued音声のdrain後にラインを閉じる。Pause/fault/debugger停止では即時stopする。
- 従来Pokecom GO由来のBRK 10観測pulseは短tap互換として残し、保持中はKEY ONを継続する。全機種合成TEST 8とRunnerの入力所有者で検証。通常matrixやTEST dividerの仕様は変更しない。
- iOS CIはApple Silicon・JDK 21・Native tests/framework・unsigned Simulator host/UI tests。iPhone/iOS 16設定と署名・配布の分離を維持。
- 外部コードのコピー・ROM fixtureの追加なし。Windows音声は2026-10-13ユーザーから改善報告あり。停止経路ごとの個別結果、MSI／BRKのWindows受入れと実ROM／実機照合は未記録。詳細と結果は[修正・受入れ記録](PLATFORM_FIXES_2026-10.md)。

### 2026-10-13 — PC-1360 BRKの追加調査とユーザー確認

- PC-1245ではゲームのBRK認識をユーザー確認。PC-1360非反応の報告を受け、Keyboard→Io→TEST 8経路とPokecom GO参照実装を再確認。
- PC-1360も操作を確認し直した結果、BRKを認識していたとユーザー報告。機種固有の追加修正なし。
- 追加テスト実行は許可されず未実施。先の合成命令テスト成功と今回のユーザー操作確認を分けて記録する。


## 2026-10-14 — 関数キー・Studio LCD比率の最終状態

### 関数キー

- ユーザー提供の1401/1460/1470U実機画像を一次資料として、1460/1470Uの関数panelを6列×3行・18キーへ修正。1450は既存配置を維持。
- 通常／SHIFT刻印: 1401/1450/1460はUnicode「↕︎」／STAT、1470Uは「↕︎」／TITLE。1470UのSTATはSHIFT＋「=」。F↔E位置は1450がF↔E、1460/1470UがFSE（SHIFTはTAB）。
- 旧pokecomの1460/1470刻印画像とscan tableはFSEとSTAT/TITLEを欠いていたため、この2キーの仕様根拠として扱わない。
- FORMATは既存の意味IDを使う。1470UのTITLEは別のPocketKeyを末尾追加したが、入力は未対応。
- 1460のIA bit0選択時bit3（FSE）/bit2（STAT）はローカルROMで表示形式変更とSHIFT逐次入力後のSTAT symbolを確認。1470UのFSEも表示形式変更を確認。
- 1470UのTITLE位置は同一物理位置のbit2を候補として調査したが、CAL/PRO/AERの操作で確認できなかった。候補配線は除去し、TITLE入力はUnsupportedKeyとする。通常「↕︎」の入力も含め、走査位置と操作条件の追加確認が必要。合成テストで未対応を明示する。
- ユーザーは刻印・配置修正を一旦OKとし、矢印の絵文字表示を後続課題として保留。

### 矢印の絵文字表示 — 未解決

- Unicode「↕︎」（U+2195 U+FE0E）を他の主キャプションと同じTextStyle・寸法調整で描画している。
- VS15を付けても実環境では絵文字表示になる。文字コードの指定だけで単色表示は保証できない。
- 線画描画はユーザーの指示訂正により撤回済み。後続でフォント選択・fallbackを調査し、Unicode文字のまま単色表示し、他キーとサイズを揃える方法を確認する。対象環境で実表示を検証する。
- TITLE入力の走査位置未確認とは別の課題。

### Studio LCD比率 — ユーザー確認済み

- 1460だけ細め（横:縦のdot pitch 3:4）を維持し、それ以外は1:1に統一。ユーザーが実表示を確認しOK。
- 旧来の1行LCDに適用していた高さ11相当の計算を廃止し、実際の論理ドット数と文字間・行間からpanel比率を算出。
- 1245/1251系/1401の文字比率が変わる。1261/1350/1360/1450/1470Uの比率は維持。
- 1350/1360の論理6×8セルと他機種の5×7セルは維持。CoreのLCD配線・symbol判定とPlayer描画は変更しない。
- 合成ROMで全Machine Catalogのsnapshotを作り、複数panel幅でdot比率と論理文字セルの縦横比を検証。

### 検証・今回の区切り

- JDK 21の全体 `build :core:desktopTest` 成功。Core desktop 429件、Studio 220件（既存skip 1）。共有Native testsも成功。刻印の最終訂正後にStudio・Player共有キーボードのdesktop testsも成功。
- 初回の全体buildは作成中skinの大文字resource名で停止したが、その後の実行では小文字名の既存skinがあり成功。作成中skinはこちらでは変更せず、全てcommit対象外。
- ローカルROM検証、合成配置／scan検証、ユーザーによる表示確認を区別する。ROM・ROM由来fixture・提供画像はGitへ追加しない。
- Playerの新機種追加・仮文言整理、RAM拡張、他Assembler等は別の後続項目。packageVersionは1.3.1を維持。

## Android Player PC-1401：LCDシンボルとPhysical入力

- LCDの配置は利用者提示のPC-1401配置図を参照し、共有スキンの座標へ反映した。
  上段のBUSY／DEF、SHIFT／HYP、DE・G・RAD、( )・M・Eと、下段のCAL／RUN／PRO、STAT／PRINTを分離する。
  提供画像やROM由来のフォント・fixtureはGitへ追加しない。
- 状態は既存CoreのLCD snapshotをそのまま使用し、PC-1401の本体CAL／BASICキーで切り替える。
  ホスト側の動作モードメニューを追加せず、非同期モード要求を完了表示へ置き換える処理も追加しない。
- 短いPhysical入力は既存MachineDefinitionのPC-1401押下40,000／解放20,000 cyclesへ接続した。
  PC-1245の11,520／19,200 cycles、HostKeyStateの所有権、背景移行・停止時の解放を再利用する。
- F2／F3をCAL／BASICへ対応づけ、テンキーの括弧も直接対応づける。文字変換やIME入力は追加しない。
- 検証範囲は `docs/development/ANDROID_PC1401_PROGRESS.md` へ記録する。実ROMのエミュレータ確認と実機確認を区別する。

### Android PC-1401：ゲームパッド接続の検証（2026-10-15）

- 既存の`hostInput`の`GamepadKeys`／`GamepadInput`／`HostKeyState`と、Androidの
  `AndroidGamepadController`を再利用する。機種別プロファイルは追加せず、共通割当を保持し、
  現在の機種にないキーだけ送出を抑止する。設定画面で対象機種とこの方針を明示する。
- Androidの受信条件にActivityがresumedであることを明示する。CoreへのOS API追加や
  iOSのゲームパッド機能追加、Studioの設定変更は行わない。
- Debug限定の`GamepadProbeActivity`は生成したNOPW命令列のみでSessionを作り、
  ROM Libraryを読み込まない。設定は専用の`gamepad-probe-`名前空間に限定する。
  Releaseの画面・manifestには含めない。
- 仮想機器の検証にはAndroid標準の[uinput](https://android.googlesource.com/platform/frameworks/base/+/HEAD/cmds/uinput/README.md)
  を使う。機器は検証終了時に切断し、USB／Bluetoothの実機確認とは区別する。
  外部参照プロジェクトやuinputへのビルド依存は追加しない。

### Android PC-1350 Player: 共有基盤（2026-10-16）

- 既存CoreのPC-1350 Factory／キーマトリクス／150×32 Display Snapshotをそのまま使用。
  `desktopApp/.../input/Pc1245KeyboardLayout.kt`の`Pc1350KeyboardLayout`から論理キーと
  主キー／SHIFT凡例を共有Playerへ移し、ラベルは既存`skin_pc1350.png`へ合わせた。
  外部参照プロジェクトのコード・ビルド依存、CoreへのOS APIは追加しない。
- 既存1429×603画像を確認して初期スキン座標を定義。画像ファイル自体は変更しない。
  SHIFTが2か所あるため、共通モデルで論理キーと物理IDを分離した。
  PC-1245／PC-1401はデフォルトのキー名IDを維持する。
- 合成命令列と既存HostKeyStateによる検証のみ。実ROM・Android画面でのタッチ一致・実機の
  受入れ結果ではない。ホーム選択とROM decoderはまだ有効化しない。
  詳細と後続作業は[PC-1350対応記録](ANDROID_PC1350_PROGRESS.md)を参照。


### Android PC-1350 Player: ホーム・ROM接続（2026-10-16）

- PC-1350を明示的なホーム選択対象へ追加し、既存Coreの64 KiB Flat ROM importerへ接続。
  pgrom検証とAndroidのROM管理・Session／実行意図の扱いは既存実装を再利用した。
- 既存Android画像の読込みを追加。キー入力・LCD配置の仕上げや機種別入出力は後続。
- 3機種の保存保護・置換失敗のrollback・Session切替を合成データで検証。
  Debug限定で製品MainActivityを継承し、filesDir／設定を分離した同じ導線を確認した。
  詳細と未確認事項は[手順2の対応記録](ANDROID_PC1350_PROGRESS.md#手順2-ホームrom管理session切替)を参照。


### Android PC-1350 input wiring (step 3)

The Android skin and software keyboard now pass shared physical cap IDs to
HostKeyState. SHIFT_LEFT and SHIFT_RIGHT share the Core SHIFT key but retain
independent screen owners. Existing machine IDs remain unchanged.
The existing Window keyboard dispatcher, lifecycle release logic and runner
are reused. Only PC-1350 overrides F2 (MODE), F3 (KANA), Insert and Delete;
its scan timing comes from MachineCatalog. No desktop dependency or Core
platform API was added. See ANDROID_PC1350_PROGRESS.md for automated,
emulator and pending physical-device checks.


### Android PC-1350 program I/O verification (step 5)

Reused the existing shared PlayerProgramIo, Android document adapters, Core S1
codec and PC-1350 memory planner without adding machine-specific host logic.
Added seven ROM-free shared integration tests for S1/kana round trips,
configured BASIC pointers and capacity, atomic RAM-only dump loading,
inclusive dump export/reload, and typed rejection while running.
The isolated Android host also exercised local firmware with original BASIC
and DMP samples. No firmware-derived fixture was added. See
ANDROID_PC1350_PROGRESS.md for automated, emulator and pending device checks.


### Android PC-1350 playback and gamepad verification (step 6)

Reused the shared input owners, gamepad key catalogue, 256,000-cycle runner
and PCM path. Added PC-1350 gamepad/runner regression tests and included the
machine in existing oscillator/direct-output audio tests. Debug-only probes
exercise generated Core PCM through Android audio and virtual gamepad input
with isolated settings. No Core platform API, ROM fixture or iOS feature was
added. Device acceptance remains separate in ANDROID_PC1350_PROGRESS.md.


## 2026-10-16: iOS PC-1245 picker quality — step 1

- Scope: standard document picker only; home/program I/O/audio/input remain later steps.
- Reused the existing as-copy picker, scoped bounded read, MainActor preparation, atomic Library commit.
- Added read cancellation by generation invalidation and injectable acquisition for delayed/failing reads.
  Cancellation discards delivery; it does not forcibly abort a provider operation.
- Added isolated synthetic UI probe data and host tests for stale success, acquisition failure,
  picker cancellation and restore after source deletion. No reference-project code/dependency or ROM fixture added.
- Validation and device/provider limits: [iOS picker step 1](IOS_PICKER_STEP1.md).


## 2026-10-17: iOS PC-1245 home and program I/O — step 2

- Reused PlayerProgramParser/PlayerSession check/apply/export via typed Apple DTOs; no Core OS API added.
- Swift host owns document access, bounded 2MiB reads, temporary export snapshots, lifecycle and execution intent.
- Added native home/menu/information views and build-time static documentation bundling.
- PC-1245 remains RUN/PRO only, matching current Core/Android and the user's clarification; no RSV support added.
- [Current behavior and verification](IOS_PLAYER_STEP2.md). Audio/speed, external input and hardware acceptance remain later steps.


## 2026-10-17: iOS PC-1245 PCM and playback speed — step 3

- Reused PlayerRunner tick audio, AudioPcmSnapshot silence duration and PlayerExecutionSpeed ratios.
- Apple facade exposes immutable PCM copies/rates; Swift owns conversion, bounded serial output,
  AVAudioEngine, AVAudioSession, notification handling and application preferences.
- Audio category ambient follows the user's explicit selection: respect silent mode and mix with other audio.
- No external reference code/dependency, firmware fixture, Core platform API or background playback added.
- Sources: [Apple ambient](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/ambient),
  [AVAudioPlayerNode](https://developer.apple.com/documentation/avfaudio/avaudioplayernode),
  [manual rendering](https://developer.apple.com/documentation/avfaudio/avaudioengine/manualrenderingmode).
- [Verification and remaining hardware/listening checks](IOS_PLAYER_STEP3.md).


## 2026-10-17: iOS PC-1245 external input — step 4

- Reused hostInput GamepadInput/Settings/Keys, SingleGamepadSelection and the existing HostKeyState.
- Moved Android PhysicalKeyboardState timing/chord/repeat/reset logic into hostInput PhysicalKeyInput;
  Android retains its platform keycode adapter, including existing machine-specific mappings.
- Apple facade owns the shared dispatchers. Swift adapts GameController/GCKeyboard on main,
  stores one global assignment set, and handles native menu, responder and key-window lifecycle.
- No Core platform API, external reference dependency, ROM fixture, additional iOS machine or iPad support added.
- [Implementation, Apple primary sources and verification boundaries](IOS_PLAYER_STEP4.md).


## 2026-10-17: documentation consolidation and supplied PC-1350 skin cleanup

- Consolidated current iPhone Player operation and validation in iosApp/README.md and linked the four step records.
- Kept dated proposals and intermediate results as history; hardware/provider/real-ROM acceptance remains incomplete.
- Included the supplied Android PC-1350 skin image cleanup, which only removes unwanted image marks.
  Skin layout, key regions and LCD geometry are unchanged by this image update.
- Kept packageVersion 1.3.1 and existing release tags/history unchanged. This update is not a new release.

## PC-1360K local analysis profile (2026-10-17)

The user supplied local internal and banked firmware and reported that PC-1360K
shares the PC-1360 base hardware, with two Kanji ROM chips and an additional
2 KiB RAM at `0x2000..0x27ff`. This corrects the initial 8 KiB description.
The profile reuses PGP's PC-1360 implementation; no external project code or
firmware is copied into the repository. Studio exposes it only with
`PGP_ENABLE_PC1360K=1`, under the separate `pc-1360k` identity.

User-supplied schematic references: `pc1360-mainboard0.pdf` (printed pp. 10-11)
and `MemoryPWB.pdf` (printed pp. 14-15). They are analysis references, not bundled
assets. Initial reading identifies SC6976S-0225 / SC6976T-0224 and separate
CED, AD16 and AD17 signals; full CPU F-port connectivity and chip-selection
semantics remain subject to verification. No mapping is inferred from this alone.

Writes of 1 and 0 to `0x3600` around Kanji processing are a user observation to
reproduce, not an implemented ROM selector. The existing PC-1360 bus retains
these values without introducing guessed Kanji reads. The five extra K keys are ported from the active scan table in
`KeyBoard1360k.java`, compared with `KeyBoard1360.java`, on the user's
`digihori/pokecom` branch `feature/pc1360k`, commit
`4344f8946d544954ee7b088d4aeed98d9a1cc976`. The requested `feature/pc1350k`
branch does not exist on the remote; `git ls-remote` confirmed the 1360k branch
and the local reference matches that commit. `KeyboardBase.java` confirms
index/8 = group and index%8 = bit. `include_keys_1360k.xml` supplies layout
positions. Only scan positions and layout data are ported, with no Android
UI dependencies. Existing IA scanning already covers groups 7 through 11;
no scan-bound change is needed. The duplicate COMMA entry in the legacy table
is not added: legacy first-match lookup uses its existing group-1 entry.
Tests cover each new key, release/reset and isolation from standard PC-1360.
See `docs/reference/machines/PC-1360K.md`.

## PC-1360K batch capture extraction (2026-10-18)

- Reference: same author's read-only `pcwav`, commit `14e1ef53596f0d8791b6d14965061f63846d11a7`.
- Sources: `src/PCWAV/Binary/S1Encode.pm`, `src/PCWAV/Binary/S1Decode.pm`,
  `src/PCWAV/Format/S1.pm`, `src/PCWAV/Common.pm` and the existing raw decoder.
- Independently described the S1 binary framing, nibble exchange, 120-byte chunk checksum
  and footer in the local analysis utility `docs/analysis/pc1360k/extract-batch.py`.
  This does not add S1 codecs or ROM hooks to Studio/core. The reference repository was not changed.
- Added a new 16-byte analysis packet header to identify selector, source address, sum,
  reread differences and block index. The utility extracts every batch record and combines
  only complete selector images. Captures and ROM payloads remain under ignored local-data.
- Verified with independently encoded synthetic frames, a two-save WAV with a gap, checksum
  and packet corruption, duplicate conflicts, and BASIC loop tests with cassette saves substituted.
  Actual consecutive hardware saves remain to be verified from the user's recording.

### PC-1360K batch hardware receipt: S1 partial-tail correction (2026-10-18)

The user's 32-record capture validates the S1 Binary tail checksum over the final
partial raw data bytes followed by the first FF, excluding the second FF.
The referenced pcwav encoder instead sums FF alone. The analysis extractor now
validates the hardware formula by default; --allow-legacy-tail explicitly accepts
the old encoder variant. Tests cover a corrected synthetic fixture and reject
partial-tail corruption; no firmware is added to tracked files. All 32 hardware
records passed checksums and packet sums and matched prior overlapping captures.
Neither the reference repository nor Studio's production WAV codecs were changed.

## Captured PC-1360K ROM integration (2026-10-18)

- Evidence: user-performed READ/READM probes and 32 checksum-verified 4 KiB
  captures, retained only under ignored local-data. No reference emulator code
  was copied for this mapping. Physical S/T assignment and F-port gating remain unknown.
- Added a machine-owned READ/READM bus operation with address/value recording.
  Default machines preserve next-PC reads; only pc-1360k with both Kanji images
  uses selector 0/1 and DP. Instruction fetch and ordinary RAM/ROM reads are unchanged.
- Manifest v1 gains no schema fields. Two role=kanji components are validated as
  a pair, exposed only by the hidden K ROM Set form, and rejected on ordinary 1360.
  K remains outside the normal machine catalog and Player choices.
- Verified register-instruction semantics, DP boundaries, independent ordinary-ROM
  banks/RAM, reset, access tracing, malformed packages and package round trips.
  Local unmodified firmware displays the recovered A glyph exactly after WIDTH,2
  and Kanji/A input. Conversion candidates are not yet validated.

## Four-selector PC-1360K integration (2026-10-18)

- Extended the capture-backed Kanji component set to selectors 0..3, each 65536
  bytes, without changing manifest v1 or ordinary memory/instruction fetch.
- Preserved the earlier 0/1 pair and empty analysis set; reject noncontiguous sets,
  invalid counts, sizes, roles and ordinary PC-1360 use. Unknown selectors do not alias.
- Updated hidden ROM Set creation, legacy sidecar discovery and local package
  generation. Captured firmware and the regenerated package remain ignored.
- Local unmodified firmware now converts A to 亜 and fixes it. KANJI produces
  かんじ, converts to 感じ, cycles to 幹事 and commits that candidate. The test checks
  recovered A data, selector 2/3 access, candidate display path and candidate changes.
  These scenarios are verified; exhaustive dictionary/keyboard behavior is not claimed.

## PC-1360K catalog promotion and key legends (2026-10-18)

- Promoted `pc-1360k` into the normal Studio catalog and ROM registration. Removed
  the environment gate and forced analysis startup; saved library selection has
  priority, with local K files recognized by the ordinary development fallback.
  Player's explicit supported-machine list remains separate.
- User-confirmed legends replace FIX with 採用 and CHG with 変換. YJ is drawn as
  ▲ above ▼ in one key, with centered multiline layout at both keyboard scales.
- The four-selector Kanji mapping and manifest v1 component validation are
  unchanged. No captured firmware is added to tracked files.
- JDK 21 full build and the opt-in real-ROM Kanji conversion test passed.

## iPhone program document presentation (2026-10-18)

- Investigated CI run [39078310161](https://github.com/digihori/pokecom-go-plus/actions/runs/39078310161):
  shared Native/framework compilation passed; the iPhone UI test failed after
  requesting program export. The initial inspection missed the later picker subtree;
  the follow-up below identifies the actual Cancel query failure.
- Replaced two independent nested picker sheets and separate export visibility/URL
  state with one item-based presentation carrying either import or the export URL.
  The export sheet no longer depends on an optional URL captured by a Boolean
  presentation closure. Cancellation retains the program panel and cleans its
  temporary export; existing resume intent rules are retained.
- Deferred the parent-presentation callback to the next main queue turn so it
  does not publish observed state inside UIKit/SwiftUI view updates.
- Both load and save start in Documents. The UI test recognizes English and
  Japanese local-storage navigation labels rather than waiting on a disabled
  Save button at the storage-provider root. Wait for the Documents fixture after
  folder navigation before tapping Save; the parent button can remain enabled
  during that transition.
- Local validation: JDK 21 full Gradle build passed (578 tasks); the opt-in
  captured-ROM Kanji test passed. iOS 26.5 Simulator passed all six UI smoke
  scenarios, and the final Swift unit suite passed all 35 tests, including the
  new export cancellation/retry case. The earlier iOS CI runtime is not installed
  locally; a new GitHub CI run is still required after publishing these changes.
- Final targeted UI rerun passed the complete BASIC/DMP save and reload flow,
  including Japanese folder navigation and waiting for its destination contents.

## PC-1360K dump BASIC distribution layout (2026-10-18)

- Collected the two hardware-verified batch BASIC sources under
  `tools/pc1360k-rom-dump/`, named `pc1360k-kanji-dump-01.bas` and
  `pc1360k-kanji-dump-23.bas`. Their bytes are unchanged from the analysis copies.
- Added a Japanese README with RAM usage, recording workflow, result format and
  the explicit boundary that WAV codecs and reconstruction tools are not bundled.
- Updated analysis links and the optional BASIC generator destinations. Existing
  probes, assembly and extraction utilities remain in the analysis directory.

## iOS CI export picker Cancel query correction (2026-10-18)

- CI run [39092473953](https://github.com/digihori/pokecom-go-plus/actions/runs/39092473953)
  still failed in `cancelPicker`, while shared Native tests, Swift unit tests and
  the four other platform jobs passed. Full job logs show Xcode 16.4/iOS 18.5.
- The export picker was present. Its Cancel label appeared as an `Other` node
  overlapping the More button; the test searched only `app.buttons`. The previous
  iOS-26-only fallback therefore excluded an environment with the same AX issue.
  This corrects the earlier inference that the picker itself had disappeared.
- Use an available, hittable Cancel button on either OS; otherwise navigate back
  from Documents through local storage to Browse and Close. Derive the tap's Y
  coordinate from the current native navigation bar instead of a fixed iOS 26
  offset. Keep cancellation-message and CPU-intent assertions in the callers.
- The CI capture also showed Save disabled in the initial Documents view. When
  disabled, explicitly select the local destination folder before waiting for
  Save, rather than assuming that visible file cells imply a selected writable
  destination. Continue waiting for the folder contents after navigation.
- Cover both the program export and ROM import picker flows. No application
  presentation or emulator behavior is changed by this follow-up.

## Pin iOS CI SDK and UI runtime (2026-10-18)

- In run [39099737032](https://github.com/digihori/pokecom-go-plus/actions/runs/39099737032),
  the iOS 18.5 export cancellation passed after the AX-query correction. The next
  save remained disabled even after explicitly selecting the destination folder.
- This matches reports of Simulator Files export being disabled in
  [Apple Developer Forums thread 763549](https://developer.apple.com/forums/thread/763549);
  those reports are supporting evidence, not proof that every device or runtime
  has the same cause. Local iOS 26.5 completed the same real export/reload checks.
- Pin CI to macOS 26, Xcode 26.6 and iOS 26.5, matching the successful local
  SDK/runtime combination and listed in the official
  [macOS 26 arm64 runner image](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-arm64-Readme.md).
  Keep every UI scenario and assertion enabled; do not substitute a direct file
  write or a skip for the system export operation.
- `test-simulator.sh` accepts an exact `PGP_IOS_SIMULATOR_RUNTIME` selector and
  fails if unavailable. Without it, select the newest available iOS runtime.
  Print the chosen runtime/device along with the Xcode version for diagnosis.
  Continue using a disposable device and serial, non-cloned UI testing.
- The minimum deployment target is unchanged. This CI environment choice does
  not establish Files export behavior on older iOS releases or physical devices.

## Initialize the disposable Simulator and retain Files diagnostics (2026-10-18)

- Run [39100731974](https://github.com/digihori/pokecom-go-plus/actions/runs/39100731974)
  still failed at the export Save-enabled assertion on Xcode 26.6 (17F113),
  iOS 26.5 (23F77), iPhone 17 Pro. The same toolchain and device passed all
  35 unit tests and 6 UI tests locally. Pinning the runtime alone did not fix CI.
- The CI xcresult system log records repeated `LocalStorageFileProvider`
  `bulkItemChanges` failures with Cocoa error 513 (no permission to save),
  including during export. Comparing the successful local run later showed the
  same errors there, so error 513 alone does not distinguish the CI failure or
  establish its cause. The exact denied path is not established.
- Explicitly wait for disposable-device first boot with `simctl bootstatus -b`,
  launch Files before XCTest, and print installed runtime versions.
  This is an initialization hypothesis to verify in CI, not a confirmed fix.
- If testing fails, retain a filtered File Provider system log before deleting
  the disposable device, and upload it alongside xcresult. Keep real export and
  reload assertions enabled.

## Suspend iOS CI pending Files export investigation (2026-10-18)

- At the project owner's request, remove the `ios-player` job from the automatic
  CI workflow. Keep the three desktop platforms and Android Player checks.
- Run [39106514805](https://github.com/digihori/pokecom-go-plus/actions/runs/39106514805)
  still failed at the Save-enabled assertion after first-boot/Files initialization.
  The CI-only export issue remains unresolved; this change does not fix it.
- Retain the local Simulator script, all unit/UI tests, and diagnostics for future
  investigation. Local Xcode 26.6 / iOS 26.5 passed all 35 unit and 6 UI tests;
  the targeted save/reload test also passed with English/US test settings.

## Publish the PC-1360K page dump and WAV recovery tools (2026-10-18)

- Consolidate the two public batch BASIC programs into
  `tools/pc1360k-rom-dump/pc1360k-kanji-dump.bas`. INPUT selects page 0..3;
  reject out-of-range/fractional values and save exactly sixteen 4 KiB blocks.
  Preserve the verified 284-byte machine-code reader and K136 packet format.
- Publish a one-line transfer test and a Japanese end-to-end guide: BASIC-to-WAV,
  CLOAD/RUN, CSAVE recording, BASIC recovery, then page recordings and ROM recovery.
  Recommend the author's [pcwav](https://github.com/digihori/pcwav) for BASIC transfer
  and [Audacity](https://www.audacityteam.org/) for recording. External references
  remain read-only; do not modify or bundle pcwav, and do not add build dependencies.
- The standalone Python 3.10+ decoder uses only the standard library and bounded
  PCM reads. Its signal detector independently adapts PCWAV RawDecode/PcmNormalize
  at read-only reference `14e1ef53596f0d8791b6d14965061f63846d11a7`;
  packet validation derives from this repository's earlier `extract-batch.py`.
  Validate hardware S1 partial-tail checksums without the legacy encoder bypass.
- Reject missing/duplicate/unexpected blocks, repeat differences, malformed headers,
  and byte-sum/transfer-checksum failures before publishing ROMs. Preserve differing
  existing outputs. Emit `kanji-selector-0.bin` through `-3.bin`, each 64 KiB, plus
  per-page JSON checks. The four-file Studio dialog is unchanged.
- Both historical hardware WAV captures decoded to all 64 blocks, and all four
  resulting images matched the local ROM images byte for byte. No recording,
  firmware or generated image is included in shared source. Synthetic tests cover
  PCM widths, stereo channel selection, truncation/corruption, page completeness,
  duplicates and overwrite protection.
- Local real-ROM emulator checks cover all four page loops, invalid INPUT values,
  packet content and stopping before a bad block is saved. Only CSAVE is replaced
  with a capture callback in that test. Both BASIC files passed pcwav encode/decode
  round trips. The new four separate hardware recording sessions remain untested;
  approximately 33 minutes per page is estimated from the earlier two-page runs.
- Historical analysis documents retain the old two-page experiment description;
  their current-source links now point to the unified tool and guide.

## Normalize BASIC source line endings in cross-platform verification (2026-10-18)

- Windows CI run [39109850015](https://github.com/digihori/pokecom-go-plus/actions/runs/39109850015)
  failed at the published BASIC source comparison in `Pc1360kFastReadTest`;
  macOS, Linux and Android checks passed.
- Normalize CRLF to LF when comparing the checked-out BASIC with the generated
  source. Continue comparing the entire source, including machine-code DATA.
  Exercise the same test locally with the published file temporarily converted
  to CRLF, then restore the original bytes. No emulator or dump behavior changes.

## Correct displayed release version (2026-10-18)

- Technical Preview 0.6 alpha 1 updated Gradle/package versions but left the common
  `ProjectInfo.VERSION` at 0.5.0-alpha.1. Studio About and MCP initialize therefore
  reported the wrong version. The prior common test also expected the stale value.
- Set the common runtime version to 0.6.0-alpha.2 and add a desktop test comparing
  it with the actual root Gradle version passed as a test system property.
- Prepare a new alpha-2 tag rather than replacing published alpha-1 artifacts.
  Native packageVersion increments to 1.4.1 for installation updates; Android
  versionCode increments to 7. Preserve the Windows UpgradeCode.
- Verify both the native package metadata and ProjectInfo class inside the built
  macOS application, in addition to the full build and hosted checks.

## Restore a lightweight iOS CI lane (2026-10-18)

- Measured previous full iOS jobs took 14m37s and 22m12s, compared with roughly
  40–70 seconds for the other platforms. In the 22-minute run, shared Native work
  took 2m54s, Xcode build roughly 1m33s, build-to-first-test waiting roughly 4m29s,
  and six UI tests roughly 9m25s. This was substantially broader than Android's
  unit-test/build lane. Files export failure remains a separate unresolved issue.
- Restore normal iOS CI with shared Native tests, the Debug app build, and all
  35 Swift unit tests. Keep Xcode 26.6 / iOS 26.5 pinned. Do not build the device
  Release framework in this lane; let the Xcode integration build its required
  Simulator framework instead of pre-linking it in a separate step.
- Add shared scheme `PokecomGOPlayerUnitTests`, which excludes the UI test target,
  and select it with `test-simulator.sh --unit`. This mode skips Files warm-up
  and Files-specific failure diagnostics, while retaining isolated-device cleanup
  and xcresult artifacts. Default invocation still runs the complete local suite.
- Use the `test` Xcode action, which includes its build, rather than requesting
  both `build` and `test` and rerunning the always-out-of-date Kotlin build phase.
- UI tests and their assertions remain available for explicit local diagnosis.
  A passing lightweight CI does not validate system Files export or iOS devices.
  No iOS release asset or signing workflow is added.

### Refine the lane after the first hosted measurement

- The initial unit-only run 39119872982 passed all 35 Swift tests in about 30
  seconds, but the whole lane remained around 15 minutes. Shared Native tests
  alone took 5m18s; the additional Framework build took 2m31s. New-device boot
  migration took 1m36s, with further runtime enumeration/startup/post-processing.
- Move Native-specific module tests to the explicit `iOS Native tests (manual)`
  workflow. Normal CI retains app compilation and all Swift unit tests, with
  existing desktop/Android Core tests providing shared-logic coverage.
- Cache ~/.konan with OS/architecture, pinned Xcode and Kotlin/Gradle version keys.
  On ephemeral GitHub Actions unit runs only, use an existing matching Simulator
  (prefer booted) rather than creating another device. Never delete that device.
  Local invocations and full UI runs retain isolated-device creation/cleanup.
  Omit expensive all-runtime enumeration from unit mode.

- Refined lane first hosted run: [39121256568](https://github.com/digihori/pokecom-go-plus/actions/runs/39121256568),
  all five jobs successful, iOS 14m11s. All 35 Swift tests passed in 44.8s.
  This cold measurement is not evidence of a few-minute turnaround: boot migration
  took 2m22s, the Framework Gradle invocation took 3m53s, and further Xcode/SDK
  work and post-processing remained. The new Native toolchain cache missed on
  this run and was successfully saved afterward. Measure a cache-hit run before
  claiming the expected steady-state speedup.

- Cache-hit measurement: [39125762262](https://github.com/digihori/pokecom-go-plus/actions/runs/39125762262),
  all five jobs successful, iOS 11m17s. The Native cache hit and restored about
  386 MiB. This is 2m54s (about 20%) shorter than the 14m11s cache-miss run,
  but does not meet the hoped-for few-minute turnaround. The lane has a smaller
  validation scope; Native Framework generation, Simulator preparation and Xcode
  work still impose substantial overhead. Do not describe it as a sub-minute or
  fully optimized iOS pipeline. System Files/UI and Native-specific tests remain
  outside this normal lane, with the documented manual/local paths retained.

## Android PC-1360 / PC-1360K Player integration (2026-10-20)

- Reused existing Core machine sessions, eight-bank ROM model, S2 BASIC codec, LCD mapping,
  memory classification and PC-1360K kanji READ/READM behavior. No firmware hooks or new RAM expansion modes.
- Ported key legends and the K-specific five controls from
  `desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/Pc1245KeyboardLayout.kt`
  into the portable Player layer, with no desktop dependency. The supplied Player skins omit right SHIFT.
- Measured bundled 1426×615 images for touch geometry. PC-1360 LCD/symbol layout follows the
  PC-1350 implementation and was approved by the user; K shares that geometry.
- Android ROM registration uses Studio pgrom packages, preserving all eight banks and optional kanji selectors.
  The existing package check and Session creation reject malformed firmware before Library installation.
- Added independently authored instruction/pattern tests and optional local-ROM S2 input regression.
  Debug verification namespaces do not reuse normal Android Library/settings. No ROM-derived fixtures added.
- Stage-specific evidence and remaining physical-device acceptance are in
  `ANDROID_PC1360_PROGRESS.md` and `ANDROID_PC1360K_PROGRESS.md`.

## Four-line Player LCD and PC-1360K indicators (2026-10-20)

- Applied the user-approved centered 857×182.83 LCD proposal to PC-1350/1360/1360K;
  grouped lower indicators at the left instead of distributing them across the panel.
  Geometry is shared and preserves the existing 150×32 dot aspect ratio and touch coordinates.
- Corrected the initial PC-1360K reuse of the six PC-1360 flags. Existing local observations
  in `docs/analysis/pc1360k/PC-1360K_ANALYSIS.md` and `PC-1360K.md` distinguish 303C bit5/6/7.
  Added explicit HIRAGANA, KATAKANA and neutral S ([S]) indicators without inventing further semantics.
  Standard PC-1360 still exports KANA for bit6. Android and Studio use the supplied captions.
- A locally supplied, unmodified ROM test verifies WIDTH,2, KANJI and HIRA_KATA produce
  RUN+HIRAGANA+S then RUN+KATAKANA+S. Synthetic instructions independently test all eight bits.
  No firmware glyphs or ROM-derived fixture files were added.
- Studio launch verification uses a temporary HOME and a temporary in-memory PreferencesFactory.
  The initial macOS filesystem-preferences isolation attempt was unavailable because its native backend
  is absent; the temporary adapter resolves launch isolation without changing product persistence.

2026-10-20追記: Android Playerのユーザー指定の[S]は角括弧文字ではなく、正方形の塗りつぶしに明るいSを抜いた反転表示とする。消灯時は背景の塗りを薄くし、Coreのbit7による点灯条件は維持する。

## Android Player checkpoint (2026-10-20)

- User reported physical-device testing was broadly successful for the work through PC-1360/1360K, including the latest LCD changes. This does not imply exhaustive per-device, peripheral or audio-route coverage.
- Home tiles and list share the selected-machine caption: prefix the actual name with `*`, replacing the verbose selected suffix. Name sorting and saved layout preference remain unchanged.
- JDK 21 `build :core:desktopTest :androidApp:assembleDebug` passed after the caption change. Isolated `desktopApp:run` launched successfully and was explicitly stopped. The new caption itself has not received a separate physical-device acceptance report.
- Continue Android model integration in order PC-1450, PC-1460, PC-1470U. Family variant selection is deferred; no RAM expansion or iOS scope added.

## Android PC-1450 foundation and connection (2026-10-20)

- Ported Studio `desktopApp/src/main/kotlin/com/digihori/pgp/desktop/input/Pc14xxKeyboardLayout.kt` PC-1450 key identities and legends into shared Player. Preserve LOCK for the left SHIFT caption and SHIFT for the right; do not invent a common scan.
- Retained CAL/BASIC host keys and added explicit Android F4–F8 mappings for kana, small, dakuten, long vowel and LOCK. Model changes release held physical input.
- Reused existing Pc14xx session, fixed RAM, S1 codec and memory classification. Original instruction tests cover the 1450 LCD wiring, all 18 symbols and oscillator PCM; original BASIC sources exercise vocabulary/kana and rejected DMP placements without changing loaded BASIC.
- Replaced the initial mockup with the user-supplied LCD guide. Connected PC-1450 only after its shared foundation checks. Android supports validated pgrom or the existing 64 KiB flat importer, which discards RAM.
- Debug namespaces isolate real-ROM checks from normal Library/preferences. Verified all 81 touch keys, 18 indicators, BASIC load/RUN/save and paused background return. See `ANDROID_PC1450_PROGRESS.md` for stage-specific verification.

## Android PC-1460 connection (2026-10-20)

- Reused existing Pc14xx four-bank ROM, fixed RAM, S1 codec, LCD wiring and Core key scans after PC-1450 host verification. Android accepts Studio pgrom only for banked machines.
- Ported Studio 1460 labels, retaining LOCK, FSE and SHIFT+UP/DOWN MATRIX. Shared Android F2–F8 mappings use the same scan identities.
- Supplied user LCD guides determine indicator locations. PC-1460 preserves physical pitch 3:4, independently of the 5×7 Core snapshot; only skin geometry changes the rendered aspect.
- Shared and Android tests use independently authored instructions and BASIC text; isolated Debug hosts verify all physical key regions without reading normal ROM Library or settings. See `ANDROID_PC1460_PROGRESS.md`.

## Android PC-1470U connection (2026-10-20)

- Reused Core eight-bank ROM, dedicated S2 token table, numeric keyboard selector, 22-bit LCD symbols and RAM boundaries. Ported Studio 1470U keys, adding skin-specific arrow and DRG captions without changing scan behavior.
- Supplied LCD guide defines two dot rows and the DBL, AER and BATT positions. Square pitch and one-dot row gap are retained. Both physical SHIFTs keep separate IDs and owners for the same logical key.
- Android Backspace/Delete/Insert dispatch BS/DEL/INS only on PC-1470U. Model reconfiguration releases pending input. AER/CAL is the actual machine key, with SHIFT selecting CAL; no host-generated mode label is used for the skin.
- Local firmware cold initialization reached RUN. Verification must inspect LCD mode before sending BASIC: a blind BASIC press can select PRO. An initial PRO-mode RUN command produced ERROR 9; this is distinct from program-load validation.
- Synthetic data, local-ROM checks and user physical-device acceptance are recorded separately in `ANDROID_PC1470U_PROGRESS.md`. No ROM-derived fixtures or iOS feature additions.

### Final validation environment

JDK 21 clean build, Core desktop tests and Android debug packaging passed without configuration/build cache (Core 462; shared desktop/Android 121 each; shared iOS Simulator 131; Android unit 77). Isolated Studio launch passed and was explicitly stopped. Temporary-HOME Native dependency paths had mixed with normal generated outputs; stopping the daemon and rebuilding cleanly resolved this without product or CI changes. Final APK uses the normal debug signing environment; only the fresh isolated test AVD's temporary-signed installation was replaced and final APK launch was verified. Physical-device acceptance remains pending for these three models.

## Android home scroll restoration (2026-10-20)

- Hoisted independent tile/list LazyGridState instances above the home/emulator composition branch. Returning home keeps the previous position instead of recreating the grid at the first machine.
- On home entry or layout change, wait for grid measurement and ensure the selected machine is fully visible. Keep the position if it already is; otherwise scroll to its named item. ROM persistence and emulator Session ownership are unchanged.

- JDK 21 `build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache` passed. On an isolated emulator, PC-1470U remained visible on home entry and after emulator/home round trips in both layouts. Manually adjusted positions stayed identical after return, and tile/list positions remained independent. No new physical-device acceptance or commit/push.

## Android orientation by screen (2026-10-20)

- MainActivity starts in FULL_USER orientation for home. Home, controller/playback settings, ROM management and information follow system rotation preferences; the machine screen and its program dialogs retain SENSOR_LANDSCAPE. Closing settings opened from the machine restores landscape.
- Keep the existing configuration-change handling so rotation preserves the Activity and emulator Session and releases held inputs. Home scroll visibility is reevaluated only after measurement matches the resized grid viewport.

- JDK 21 `build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache` passed. Isolated emulator verification covered both tile/list portrait-home → landscape-machine → portrait-home round trips, selected-card visibility after each resize, the same Activity/Session across rotation, and portrait/landscape CPU settings, controller settings and Player help. Physical-device acceptance remains pending.

## Android home scroll indicator (2026-10-20)

- Added a persistent right-side track/thumb for both home layouts when the grid can scroll. Uses measured visible grid rows, partial-row offsets and maxSpan to estimate position with the existing Compose version. Counts the full-width explanatory footer/error as separate rows and anchors the thumb at the actual scroll endpoints.
- Draw in the existing outer gutter so machine-card sizes, touch targets and adaptive column count stay unchanged. Hide the indicator when all content fits; keep existing scroll restoration and orientation handling.

- JDK 21 `build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache` passed. On the isolated emulator, checked portrait/landscape × tile/list at both scroll endpoints: the visible thumb moved from the top to the bottom of the track in all four cases. Inspected the right gutter to confirm no overlap with machine-card controls. Rotation settings were restored after verification; physical-device acceptance and commit/push remain pending.


## Android 16 tablet machine orientation (2026-10-20)

- A Redmi Pad 2 / Android 16 report exposed the platform ignoring SENSOR_LANDSCAPE on large screens. Reproduced on an isolated API 36 emulator with a 1600×2560 display at 240 dpi: entering the machine kept portrait orientation.
- Added application-level `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY=true` so API 36 honors runtime orientation requests, including MainActivity-derived debug probes. Home and auxiliary screens retain FULL_USER; machine screens retain SENSOR_LANDSCAPE.
- This is the documented temporary opt-out for target SDK 36. It is unavailable when targeting SDK 37; reassess the machine layout/orientation policy before that upgrade. Source: https://developer.android.com/about/versions/16/behavior-changes-16
- Implemented and JDK 21 `build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache` passed. On the isolated API 36 tablet-sized emulator, both tile/list flows kept the machine Activity configuration landscape through portrait/landscape device rotation and returned home to portrait, retaining the same Activity/Session. With the device held portrait, Android may letterbox the landscape app area rather than rotate the entire display. Display size/density and rotation settings were restored. Redmi Pad 2 physical-device confirmation remains pending. No ROM or normal ROM Library/settings were modified; no commit/push.

## Android machine entry rotation presentation (2026-10-20)

- Request SENSOR_LANDSCAPE before switching home to the machine. Keep the existing composition/Session, but draw a black transition surface while a landscape-only screen still has portrait bounds; reveal its content when measured landscape bounds arrive.
- Request `WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT` and keep the opaque Activity window FULLSCREEN on all supported APIs, as required by the local Android SDK WindowManager documentation for this animation preference. Insets controllers continue to manage transient system bars and navigation visibility.
- JDK 21 `build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache` passed. Isolated API 36 tablet emulator confirmed tile/list home→machine→home, landscape machine configuration through rotation, and retained Activity/Session. Window diagnostics confirmed `rotAnim=JUMPCUT` and `FULLSCREEN`. Device-specific visual timing/animation on Redmi Pad 2 remains pending; a brief blank transition can remain and the OS ultimately controls its animations. No new physical-device acceptance, commit or push.


## Android Player user acceptance and closure (2026-10-20)

- After the tablet rotation transition fix, the user reported improved presentation and no problems after checking operation throughout. Record the current Android machine additions (including PC-1450/1460/1470U), home UI and Redmi Pad 2 / Android 16 rotation changes as physically accepted for this work, separate from the automated/emulator evidence above. Earlier pending statements describe the state at those stages; this entry is the final acceptance.
- Close the current scope. Selection within the PC-1250/1251/1255, PC-1260/1261 and PC-1401/1402 series remains deferred. This does not claim exhaustive per-feature/peripheral/ROM-version coverage. RAM expansion, save states, iOS extensions and iOS CI optimization remain outside this work.
- Documentation-only acceptance update; `git diff --check` passed. No additional runtime changes, ROM data, commit or push.


## Android Player capacity-series selection (2026-10-20)

- Source: existing Core Pc1251FamilyModel / Pc1261FamilyModel / Pc1401FamilyModel, Studio DesktopRomRegistrationCatalog and shared-ROM loading policy. No external reference dependency or new Core algorithm. Earlier deferred Player selection is implemented here; historical closure remains scoped to the preceding work.
- Added explicit shared-ROM representative IDs and grouped home rows to PlayerMachineCatalog. Enable 1250/1251/1255, then 1260/1261, then 1401/1402, validating each stage. Reuse family skins/keyboards and enable the existing 125x/126x mode shortcuts for every member; 1402 retains CAL initialization. Player always selects HARDWARE RAM for 125x.
- Preserve Android's existing pc-1251/pc-1261/pc-1401 Library directories; keep exact active model IDs separate. Decode validated family packages into the selected model identity without modifying component bytes. A replacement updates the one shared package; deletion invalidates any active alias in that family. Cross-series packages remain rejected; transaction rollback behavior remains in place.
- Hoist per-series pending dropdown selections into MainActivity and persist them independently of the active Session. Changing a dropdown does not reset RAM. Existing confirmed switch flow runs on Play; cancellation keeps the old Session. Group-based scroll restoration/highlighting retains the active row, and an alternate pending model displays the current active model explicitly. ROM management groups rows and states the chosen launch model.
- Verification levels and remaining physical acceptance: [Android series progress](ANDROID_SERIES_PROGRESS.md). PC-1402 capacity recognition remains ROM-version dependent, matching Studio; do not infer full-capacity support from synthetic tests or patch firmware. No ROM/derived fixture, normal Library migration, RAM expansion, iOS feature addition, commit or push.

- Final verification: JDK 21 full build, Core desktop tests and Android debug APK succeeded. Emulator synthetic-ROM checks covered all seven models, pending selections restored independently of the active Session in both home layouts, shared-ROM deletion/cancellation with an active 1255 alias, external-key delivery/release for 1255/1260/1402, and 1402 background return PAUSED with no held/queued keys. Studio startup was checked with an isolated JavaExec home and in-memory PreferencesFactory, then only that process was stopped. New series selection remains pending user physical acceptance.


## Android Player current-scope closure and PC-1250 MEM follow-up (2026-10-20)

- User observed MEM=3486 instead of the expected 1486 with PC-1250 selected, then accepted this provisionally and considered the Android Player work a stopping point. Close the current scope without claiming exhaustive physical acceptance of all series models.
- Player uses HARDWARE RAM for PC-1250. Existing Core keeps the previously documented, partly unconfirmed PC-1250 RAM mirrors; the local 1251 ROM smoke test expects the same logical BASIC start pointer for 1250 and 1251. ROM-version behavior versus memory mapping/capacity detection remains unresolved. Do not label the MEM result normal or attribute it solely to the ROM. No code or firmware patch in this closure update.
- Preserve this as a known follow-up with minor UI refinements as needed. Documentation-only update; no additional runtime checks, commit or push.

## Android skin palette refresh (2026-10-20)

- User supplied palette-only updates to skin_pc1350.png, skin_pc1360.png and skin_pc1360k.png. Image dimensions remain 1429×603 / 1426×615 / 1426×615; existing shared LCD/key geometry is retained.
- User renamed skin_pc1470.png to skin_pc1470u.png. Updated the Android drawable reference and confirmed no stale old-name references remain.
- JDK 21 `build :core:desktopTest :androidApp:assembleDebug --no-configuration-cache` succeeded. Studio startup checked with isolated JavaExec home and in-memory PreferencesFactory, and only that process was stopped. Android display verification uses ROM-free skin probes on the dedicated emulator; no normal ROM Library/settings or connected physical device was used. Physical acceptance of this palette refresh remains separate; no commit/push.


## 2026-10-20: PC-1360K CE-140F向け吸い出しソース

- `tools/pc1360k-rom-dump/pc1360k-kanji-dump-ce140f.bas`を録音版から派生させた。
  DATAの284バイト、READ(LDPC)による4KiB読出し・再読比較、ページ入力と停止条件は共用する。
- 保存部分のみ、独自ヘッダー付きCSAVE Mから、`SAVE M F$+".BIN",&D000,&DFFF`へ変更。
  5文字のファイル名stemと拡張子の式を分け、固定文字列変数の長さ制限を避ける。
- コマンドの装置名・範囲指定は[CE-140F操作マニュアル](https://github.com/ffxx68/Sharp_ce140f_emul/blob/main/CE140F_OPERATION_MANUAL_EN_DE_FR-compressed.pdf)と
  [操作解説](https://ht-deko.com/pokecom/ce140f.html)を参照。
  [互換機器の紹介](https://ht-deko.com/pokecom/ce140f_emu.html)のSAVE M／LOAD Mの未確認表示も手順に反映した。
  外部ファームウェアのコードは移植していない。
- 合成テスト、pcwav S1 BASICのWAV生成、ローカルROMでのBASIC制御・保存名・4KiB内容を検証。
  保存命令は取得hookへ置換し、読出しは合成データを注入する。実ROM由来のfixtureは追加しない。
  機器上の保存形式、実CE-140F／互換機器での保存・再読込・全ページ回収は未検証。


## 2026-10-20: YASM61860と汎用外部アセンブラ

- 共通Runnerを維持し、SC61／YASM61860／汎用DMPのadapter選択をDesktopへ追加。
  Workspace、Build、Quick Assemble、接続テストを同じdispatcherへ接続した。
- YASMはユーザー提供のローカル`yasm.pl`（表示version 1.02）を読み取り・実行して調査。
  確認したスクリプトのSHA-256は`b3ba48650b72dbf0dd254428cafd7c9436cbd46efbf7c26884530d4c8489a222`。
  [作者の説明](https://www.oit.ac.jp/labs/rd/rssrv/kobayashi-lab/~yagshi/old_web/misc/pocketcom/yasm-e.html)も参照。
  既存プログラムを変更せず、PGPへの同梱・コピー・ビルド時依存は追加しない。
- 8byte境界の行アドレスと先頭空欄、任意のORG間隔、行SUMをPGP側で解釈する専用処理を新規実装。
  汎用adapterでは同じ文字列を推測で変換せず、PGP DMPそのものを要求する。
- 外部出力は今回専用の一時ディレクトリで取得。旧成果物を使わず、終了・形式検査後に既存の配置検査へ渡す。
  SC61の従来の成功コード・ログ・Listing解析は維持する。Core commonMainへのOS API追加はない。
- 合成テストとローカルYASM実行で検証。未検証の他バージョンやWindows／Linuxでの操作を動作保証としない。

- 同日のユーザー報告により、yasm.pl直接起動＋`{source}`のみではダンプが標準出力に出るため
  取り込めない設定上の不足を修正。YASM adapterは`-d`を補完し、明示的な保存先も今回の一時DMPへ差し替える。
  元の設定値・ソースは書き換えず、SC61／汎用adapterにはこの補完を適用しない。

- 後続のUI改善では使用対象と編集中タブを分離し、ツール別Preferencesキーへ実行設定・テスト用ソースを保存。
  旧設定は当時のツールのプロファイルとして読み込み、選択や別タブの保存で上書きしない。
  使用対象はメイン画面とWorkspaceにも反映する。プロジェクト形式は変更しない。
- Workspaceの外部ログは8行超で200dpのLazyColumnとスクロールバーへ表示し、全文コピーを提供。
  失敗時もログを診断本文から分け、生成物と短いエラー概要を保つ。


## 2026-10-20: WAV S1／S2対応の段階1

- 読み取り専用のpcwav commit `14e1ef53596f0d8791b6d14965061f63846d11a7`の
  Common／WavWriter／S1 Binary／S1・S2 BASIC／CLIを再照合。
- 2026-10-18の1360K実機録音検証に従い、S1 Binary tail仕様をpartial raw＋最初のFFへ修正。
  最後のfull chunkのchecksum、BASIC末尾との違い、65536-byte長の参照Decoderの問題を明記した。
- `test-data/wav/s1-s2`へ独自の短いBASICと合成byteによる28件のraw／PCM hashを追加。
  製品コードから期待値を作らず、仕様からの独立生成とpcwavの実行結果を比較した。
  外部コードはコピーせず、参照srcが変更済み・revision不一致なら再採取を拒否する。
- Binary旧tailの参照値を採用期待値と分け、正常な成功fixtureへ混ぜない。
  ROM・録音・端末情報は含めず、pcwavリポジトリと製品codecは変更していない。
- 固定JSONは外部ツールなしで検証可能。次段階のCoreテストから固定期待値として利用する方針で、
  pcwav／PerlはPGPのビルド・実行時依存に追加しない。

## 2026-10-20: WAV S1／S2共通信号（段階2）

- 参照元：pcwav commit `14e1ef53596f0d8791b6d14965061f63846d11a7`のCommon／WavWriter／RawDecode。
- S1/S2の16-unit frameと2048-unit leaderをCoreの独立codecへ実装。
  原実装の固定先頭skipや途中byteの無検証出力は採用せず、搬送波・sync／stopを検査する。
- Decodeは8／16／24／32kHzを対象とし、125msの搬送波で同期取得・再取得する。
  再標本化や速度ずれ補正を行わない範囲を明示。rawとPCMおよびnibbleの位置を保存する。
- 段階1の独立した28合成vectorでPCM hashとraw復元を検証する。
  commonTestで破損・切詰め・無音・極性・振幅・別sample rate・WAV層との接続を検証する。
- 外部リポジトリの変更・通常ビルドからの依存はない。payload／Studioへの接続は後続段階。

## 2026-10-20: WAV s1binとStudio接続（段階3）

- 参照：pcwav commit `14e1ef53596f0d8791b6d14965061f63846d11a7`、Format/S1.pm、Binary/S1Encode.pm・S1Decode.pm。
- Coreへ独立したtype 67 payload codecを実装。name／metadata／chunk／tailの全checksumを検証する。
  最終120 bytesにもchunk checksumを付け、partial tailは実機確認済みのpartial＋最初のFF方式を採用。
  旧参照Decoderの65536-byte wrap、最終full chunk不整合、tail未検証は引き継がない。
- 8件の固定Binary期待値、最大長・範囲・破損・切詰めの合成テストを追加。
- StudioのS1/S2 binaryを共用codecへ接続し、PCM/rawの途中結果と位置情報を保持する。
  `.dmp`／16-bit Intel HEXは単一連続領域を要求する。Playerや音声device I/Oは変更しない。
- 実機確認済みtail仕様の採用と、新PGP codecの実機往復は別であり、後者は未確認。
  外部リポジトリやROM・録音データは変更・追加していない。

## 2026-10-21: WAV s1basicと機種別body adapter（段階4）

- 参照元：pcwav commit `14e1ef53596f0d8791b6d14965061f63846d11a7`、Basic/S1Encode.pm・S1Decode.pm、Common.pm。
- type 07／filename／120-byte chunk／tailをCoreの独立codecへ実装。
  最後のfull chunkをtailへ含め、rawのlow nibble加算後だけcarry補正するchecksumを全箇所で検証する。
  原Decoderのchunk・tail未検証やFF FFの単純探索は採用せず、行長でbodyを復元する。
- BASIC imageの前後FFを変換する純粋adapterと、物理trailerを明示的に扱うStudio接続を追加。
  既存の機種別BASIC codecを再利用し、pcwavの単一token表を全機種へ適用しない。
- 独立fixtureのS1 BASIC 8件、chunk境界・破損・切詰め・行内FF FF、7機種のStudio byte往復を合成テストで確認。
  未知byteや文字変換損失は診断し、raw／PCMを保持する。外部依存・ROM・録音追加はない。
- 新codecの実機往復は未確認。S2 BASICは後続段階とし、Player／音声I/Oは変更しない。

## 2026-10-21: WAV s2basic、行参照・CP932・type（段階5）

- 参照：pcwav commit `14e1ef53596f0d8791b6d14965061f63846d11a7`、Basic/S2Encode.pm・S2Decode.pm、TextCodec.pm。
- S1 BASICの検証済みenvelopeを内部共用し、S2のtype 27／37を明示的に保持するcodecを追加。
  原参照のchecksum検証省略は採用しない。type 37からpassword文字列設定を推測しない。
- 既存Coreの機種別S2 codecを拡張。ON GOTO/GOSUBリスト、THEN、0..65535範囲、quote／REMを扱う。
  PC-1360系とPC-1470Uの語彙表は分離したまま使用する。
- CP932 pair表はPython標準ライブラリのcp932 codecから独立生成した文字コード対応データ。
  `scripts/generate-s2-cp932.py`で再生成・照合可能。実行時の外部依存やCoreのOS charset APIはない。
  JVM windows-31jとの割当済みpair比較、11件の独立S2 vector、機種別byte往復・破損で検証する。
- StudioのS2 BASICとtype選択・機種選択を有効化し、既存の波形/raw/診断/.bas保存へ接続。
  Player UI・音声I/Oを変更しない。実機往復は未確認、ROMや録音由来データの追加はない。

## 2026-10-21: WAV全形式の仕上げ（段階6）

- UIの信号方式を解析結果へ明示し、OLD専用差分表示をS1／S2へ適用しない。
- OLDを含め信号破損警告時のプログラム保存を抑止し、途中結果を保持する。EOF診断の画面遷移範囲を補正。
- 全形式の破損末尾とraw移動の回帰テスト、全ビルド・既存OLDテストを実施。
- 操作ガイドと実機未確認表を追加し、自動検証済みの実装と実機往復を分けて記録する。外部依存・ROM追加なし。

## 2026-10-21: WAVエンコード形式の明示選択

- 利用者指定によりtype一覧・mode／互換性注記・type checkboxを画面から削除。
- OLD／S1のBASIC通常／保護付き／マシン語、S2のBASIC通常／保護付きへ選択を整理。
- S1の保護付き形式17を生成・識別・保持。共通envelopeを利用し、保護flagの往復テストを追加。
- 拡張子でencode種別を決定せず、明示した種別への適合確認のみ実施する。

## 2026-10-21: WAV形式単位の変換とDecode自動判別

- 利用者方針に従いWAV側でS1共通（カナ対応）／S2共通（CP932対応）へ変換経路を固定。
  エミュレータ側の機種別codecを変更せず、Project機種の制約をWAVへ持ち込まない。
- OLD／S1共通信号を両方解析し、ヘッダー・payload検証が唯一成功した形式を表示。
  不確定・競合は出力を禁止し、候補ごとのPCM/raw/診断を保持。形式選択UIはEncodeだけにする。
- 全通常／保護付きBASICとBinaryの自動判別、1401 ProjectでもS1カナを許可する回帰テストを追加。

## 2026-10-21: WAV今回分のユーザー確認と区切り

- S1／S2対応、明示的なEncode選択、Decode自動判別、形式単位の日本語変換を今回の到達点とした。
- PRINT／REM／引用符を含むS1半角カナ・S2漢字／全角カナ／半角カナの変換確認用BASICを提供した。
- ユーザーから一通り確認できたとの報告を受領し、WAV関連の今回分を一区切りとした。
- 実機往復の個別条件・byte比較結果は未記録。残課題・検証範囲は[進捗](WAV_S1_S2_PROGRESS.md)と[操作ガイド](../guides/WAV_TOOL_GUIDE.md)を参照。

## 2026-10-21: iOS PC-1251系の接続

- PGP内のCore `Pc1251FlatRomImporter`、`PlayerSessionFactory`、`PlayerMachineCatalog.forModel`、
  共有スキン・入力・BASIC/DMP処理をApple facadeとSwift hostから再利用する。
- Android Playerのシリーズ共用ROM・モデル別標準RAMという仕様に合わせる。
  Androidの画面構造は移植せず、SwiftUIのシリーズカードと独立した選択／実行モデル保存で接続する。
- iOSの`skin_pc1251.png`は既存PGP Android resourceとbyte一致。外部参照プロジェクトへの
  build依存、外部コードコピー、ROM/ROM由来fixtureは追加しない。
- 実装と検証、PC-1250のMEM表示の保留は[手順2](IOS_MACHINE_SUPPORT_STEP2.md)を参照。

## 2026-10-21: iOS PC-1261系の接続

- PGP内の`Pc1261FlatRomImporter`、共有Session／series対応付け、S1 BASIC／DMP、2行LCDとスキンを再利用。
  Androidのplatform構造は移植せず、既存Swift hostのモデル選択・保存・入力境界へ接続する。
- 画像はPGP Androidの`skin_pc1261.png`とbyte一致。外部リポジトリ依存、Core動作の変更、ROM追加はない。
- [手順3](IOS_MACHINE_SUPPORT_STEP3.md)に実装と合成／実ROMの検証区分を記録する。

## 2026-10-21: iOSの固定モード位置とRUN/PROトグル

- Androidで使用中の共有`withFixedModeIndicators()`をApple facadeにも適用し、
  PC-1245／1251系を固定位置のモード表示に統一する。Coreと元スキン定義は変更しない。
- 右上はRUN/PROのみのトグルとし、RSVはメニューで選ぶ。
  [修正・検証記録](IOS_MODE_INDICATORS_FIX.md)を参照。外部コード・ROM追加はない。

## 2026-10-21: iOS PC-1350の接続

- PGP内のPC-1350 Importer、S1 BASIC/DMP、4行LCD・スキン・キー定義をApple facadeへ接続する。
- Androidの機種別Physical変換を参考に、iOSのHIDコードへ明示的に対応付ける。platform構造は移植しない。
- 2つのSHIFTを扱うためSwiftのタッチ・ハイライト・accessibilityを既存共有region IDへ接続し、
  機種別の割当可否も起動中Sessionのキー集合へ合わせる。
- 画像は既存PGP Android resourceと同じものを同梱。Core動作・外部依存・ROM追加なし。
  [手順4](IOS_MACHINE_SUPPORT_STEP4.md)に実装と受入れ区分を記録する。

## 2026-10-21: iOS PC-1401系・PC-1450の接続

- PGP内の`Pc1401FlatRomImporter`、`Pc14xxFlatRomImporter`、シリーズ対応付けと標準RAM、
  S1 BASIC/DMP、共有キー・スキン・ROM由来LCDシンボル処理をApple facadeへ接続する。
- Android Playerのモデル別Physical変換に合わせ、Apple HIDのF2/F3とPC-1450固有F4〜F8を対応付ける。
  AndroidのUIやplatform構造は移植せず、既存Swift hostのLibrary・選択保存・入力経路を使う。
- 画像は既存PGP Androidの`skin_pc1401.png`／`skin_pc1450.png`と同一。
  外部プロジェクト依存、Core/commonの動作変更、ROM／ROM由来fixture追加はない。
- [手順5](IOS_MACHINE_SUPPORT_STEP5.md)に検証と実ROM・実機の残項目を記録する。

## 2026-10-21: iOS右上RUN/PROの表示対象を限定

- ユーザー指定に合わせ、右上トグルをPC-1245／1251系／1261系の明示的な一覧で制限する。
  PC-1401／1402・1450はCAL／BASICキーとモードメニューを使用し、Coreと共有スキンは変更しない。

## 2026-10-21: iOS動作モードメニューも対象限定

- ユーザー指定により、右上トグルと動作モードメニューを共通の機種条件に統一する。
  PC-1401／1402・1450は本体CAL／BASICキーを使い、Swift hostの直接指定も拒否する。
  共有Coreが持つモードAPI・キー配送・LCD動作は変更しない。

## 2026-10-21: iOS PC-1360／1460／1470Uの接続

- PGP内のバンク機種Core、S2 BASICと機種別dialect、PlayerSession、スキン・キー定義をApple境界へ接続する。
  Android同様、Studio作成のpgromのみ登録し、単体ROM画像からバンクを推定しない。
- バンクのID集合・role・サイズをApple側でも明示検証する。Core/commonの機種動作は変更しない。
- iOS HIDの機種別変換はPGP Androidの`AndroidPhysicalKeyboard`を参考に接続し、
  platformのUI・スレッド・ファイル処理は移植しない。Swiftの既存Library・入力所有権・入出力を再利用する。
- `skin_pc1360.png`／`skin_pc1460.png`／`skin_pc1470u.png`は既存PGP Android resourceを同梱する。
  外部コード依存・ROM／ROM由来fixtureは追加せず、独自の命令列とバンク別RAMマーカーで検証する。
- 実装・自動検証と実ROM・実機の区別は[手順6](IOS_MACHINE_SUPPORT_STEP6.md)を参照。

## iOS PC-1360K（手順7）

既存のPc1360kSkin、Pc1360RomDefinition、AndroidのPC-1360K対応仕様をiOSホストへ接続。画像は同リポジトリのAndroid skin_pc1360k.pngを再利用。漢字ROMは連続したselector 0〜3の0／2／4個だけを許可し、roleと64 KiBサイズをApple境界で検証する。反転SはAndroidと同じ暗い正方形・明るい文字として描画。テストROMは命令定義から生成した独自コードと図形で、実機firmwareを使用・追加していない。

## iOS機種対応の全体受入れ（手順8、2026-10-21）

Core・共有Player・Androidの既存機種定義に対するApple接続14モデルの全体回帰を実施し、対応一覧とヘルプを現状へ更新する。新規の外部コード・firmware追加はない。[受入れ記録](IOS_MACHINE_SUPPORT_STEP8.md)に自動検証と実ROM／実機の確認範囲を分けて記載する。初期PC-1245の実ROM確認履歴は保持し、新規機種へ外挿しない。

手順8の全体UIで一定距離のフリックによるPC-1460カードの通り越しを検出し、UIテスト補助だけを実距離・停止付きドラッグへ修正。全体14成功と修正後3成功を合わせて全15ケースの成功を確認した。Release unsignedビルド成功。実ROM・実機の未確認範囲は受入れ記録に保持する。

## 2026-10-21: iOSホームのAndroid仕様への接続

ユーザーの全機種動作・音声再生確認を受領し、ホームの名前順・タイル初期表示と保存・機種画面のみ横固定を既存Android仕様からiOSへ接続する。ホームのメニューはSwiftUI標準Menuへ置き換える。外部実装・ROMを追加せず、Core／音声処理は変更しない。[修正記録](IOS_HOME_PRESENTATION_FIX.md)。

ホーム修正の検証は全体build・Native143・Swift49が成功。iPhone 17 Proの対象UI5ケースは新規期待値修正後の再実行を含め成功し、SE3のOSダーク／ライト2ケースも成功。Release unsignedコンパイルと同梱文書も確認した。

2026-10-21、ユーザーがホーム修正後も一通り確認し、今回のiOS機種対応・ホーム修正の完了を受領。文書の現行状態を14モデル・10 ROMシリーズへ統一し、旧計画・検証件数を履歴として区別した。音声の途切れ感は後続。

## Standalone pcwav distribution scripts (2026-10-22)

- Source: digihori/pcwav, revision `14e1ef53596f0d8791b6d14965061f63846d11a7`.
- User requested bundling the standalone encoder/decoder under `tools/pcwav/`.
  Generated both version 0.5 scripts with upstream `build.pl` in a temporary copy;
  the reference checkout was not modified. Added the missing Common::parse_num
  function to both bundled scripts: decimal/0x hexadecimal input, rejecting invalid
  or negative values. Upstream address/skip options otherwise fail at runtime.
- No Studio integration or build-time dependency was added. The PC-1360K transfer
  guide now uses the bundled scripts. Ordinary ROM extraction programs remain future work.
- The source revision has no explicit license notice or LICENSE file; no license
  was invented for these files. Provenance and checksums are recorded in the tool README.
- Validation uses synthetic BASIC/binary WAV round trips and Perl syntax checks;
  this does not establish new real-device compatibility.

## Standalone pcwav 0.5.2 update (2026-10-22)

- Source: digihori/pcwav `156bb6a2ed09df3ac03c6e7ccc42e96c87cc406d` (MIT,
  Copyright (c) 2026 digihori). Included upstream LICENSE and NOTICE alongside tools.
- Generated both standalone scripts from an archived temporary copy with upstream
  build.pl; reference checkouts were not modified. Only trailing spaces in generated comment
  lines were removed; no functional local script patch remains.
- Upstream now supplies parse_num and strict S1 Binary checksums/range checks.
  Hardware follow-up corrected the final full-chunk rule: the final 1..120 bytes
  use the tail checksum, without a separate chunk checksum, including multiples of 120.
- Upstream synthetic S1 Binary suite: 176 assertions passed. Bundled Perl syntax
  checks and all five mode round trips passed. Upstream hardware results are
  recorded by the author; no new hardware capture was performed in this update.
- This updates optional standalone tools only. Studio/Core codec boundary behavior
  requires separate follow-up; corrected reference documentation flags the old assumption.

## Studio/Core S1 Binary final full-chunk correction (2026-10-22)

- Reference: pcwav 0.5.2, `156bb6a2ed09df3ac03c6e7ccc42e96c87cc406d`,
  MIT; source modules Binary/S1Encode.pm and S1Decode.pm, hardware follow-up in
  docs/BUGFIX_0.5.2.md. Ported the framing rule into the existing platform-free
  Kotlin codec; no Perl runtime/build dependency or firmware was added.
- Encode/decode now use `(length - 1) / 120` non-final checksums. The final 1..120
  bytes are covered by the tail checksum with the first FF. This also applies
  to S2 Binary through its existing shared S1 codec; BASIC logic is unchanged.
- Corrected independent fixture generation for 120/240 bytes, including PCM
  sample counts/hashes. Kept the original pcwav 0.5 reference output as negative
  fixtures: all eight old Binary vectors are rejected. All 28 independently
  generated raw/PCM vectors match pcwav 0.5.2 in an explicit read-only comparison.
- Added fixed zero-data 120/240-byte expectations, old-structure rejection and
  tail corruption/truncation coverage. Existing boundaries include 4112/65536;
  Studio conversion tests cover 120/240/241 bytes on PC-1350/1360/1360K.
- These are automated checks. The upstream author's hardware receipt is the
  evidence for framing; no new hardware transfer was performed in this task.

- Validation: JDK 21 `./gradlew build :core:desktopTest` succeeded, including
  Native/Android checks. Final Core desktop tests (491) and Studio tests (406,
  one existing skip) passed with no failures. Fixture verification and
  `git diff --check` also passed.

## 2026-10-25: iOSプライバシーポリシーの公開用原稿

既存iOS原稿を、現行Swift実装の保存情報・File Provider・ファイル共有・音声／入力・ROM Libraryのバックアップ除外・削除動作に照合して確定文へ更新。GitHub Issuesでの公開問い合わせと投稿情報の利用・削除の説明をAndroidポリシーと整合させ、日本語／英語のMarkdownと独立したiOS用HTMLを作成した。HTMLは既存の静的Pages構成・CSSを再利用し、外部スクリプト・解析・通信処理を追加しない。Androidのポリシー本文は変更しない。公開用ファイルのローカル作成であり、公開デプロイ・App Store Connectへの設定は実施していない。

iOSポリシーの検証: 日英HTMLの構造・lang属性・見出し／段落とMarkdown原稿の完全一致、日英切替・入口ページの相対リンク、git diff --checkを確認した。静的テキスト／HTMLのみの変更で、アプリの動作コードは変更していない。
