import Foundation
import QuartzCore
import PgpPlayer

struct PlayerStatus: Equatable {
    var runState = "PAUSED"
    var operatingMode = "RUN"
    var executedCycles: Int64 = 0
    var breakHeld = false
}

/// One presentation value carries the export URL so SwiftUI never builds an empty picker.
enum ProgramDocument: Identifiable {
    case importing
    case exporting(URL)

    var id: String {
        switch self {
        case .importing: return "import"
        case .exporting(let url): return url.absoluteString
        }
    }
}

@MainActor
protocol PlayerFrameDriver: AnyObject {
    func start(_ frame: @escaping (Int64) -> Void)
    func stop()
}

/// Display link keeps a weak target, so it cannot retain the host after scene destruction.
@MainActor
private final class DisplayLinkTarget: NSObject {
    weak var driver: DisplayLinkDriver?
    @objc func update(_ link: CADisplayLink) { driver?.update(link) }
}

@MainActor
final class DisplayLinkDriver: PlayerFrameDriver {
    private var link: CADisplayLink?
    private let target = DisplayLinkTarget()
    private var frame: ((Int64) -> Void)?

    func start(_ frame: @escaping (Int64) -> Void) {
        guard link == nil else { return }
        self.frame = frame
        target.driver = self
        let link = CADisplayLink(target: target, selector: #selector(DisplayLinkTarget.update(_:)))
        link.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: 60, preferred: 60)
        link.add(to: .main, forMode: .common)
        self.link = link
    }

    func stop() {
        link?.invalidate()
        link = nil
        frame = nil
    }

    fileprivate func update(_ link: CADisplayLink) {
        frame?(Int64(link.timestamp * 1_000_000_000))
    }

    deinit { link?.invalidate() }
}

/// All emulator access is serialized by MainActor. Core owns no scheduling resources.
@MainActor
final class PlayerHost: ObservableObject {
    private(set) var status = PlayerStatus()
    @Published private(set) var lcd: PlayerDisplayFrame?
    @Published private(set) var pressedKeys: Set<String> = []
    @Published private(set) var presentationMode: PresentationMode = .fullDevice
    @Published private(set) var inputEpoch = 0
    private var player: ApplePlayerFacade?
    private let frames: PlayerFrameDriver
    @Published private(set) var isActive = false
    private(set) var pendingAutomaticStart = false
    private var startRequested = false
    @Published var romPanelOpen = false
    @Published var pickerPresented = false
    @Published private(set) var isBusy = false
    @Published private(set) var romMessage = ""
    @Published private(set) var romSourceName: String?
    @Published private(set) var awaitingReplacement = false
    @Published private(set) var awaitingDeletion = false
    private let readRom: @Sendable (URL) throws -> ReadRom
    @Published private(set) var canCancelRomRead = false
    private var romStore: IosRomStore
    @Published private(set) var selectedMachineId: String
    @Published private(set) var pendingMachineId: String?
    @Published private(set) var modelSelections: [String: String]
    private let machinePreferences: IosMachinePreferences
    private let romQueue = DispatchQueue(label: "com.digihori.pgp.ios.rom-io")
    private var preparedRom: PreparedRom?
    private var romGeneration = 0

    @Published private(set) var homeLayout: PlayerHomeLayout
    func setHomeLayout(_ layout: PlayerHomeLayout) {
        homeLayout = layout
        machinePreferences.homeLayout = layout
    }
    @Published private(set) var showHome = false
    private var homeShouldRun = true
    private var hasEnteredPlayer = false
    @Published var programPanelOpen = false
    @Published var programDocument: ProgramDocument?
    var programPickerPresented: Bool {
        if case .importing? = programDocument { return true }
        return false
    }
    var programExporterPresented: Bool {
        if case .exporting? = programDocument { return true }
        return false
    }
    @Published private(set) var programSaving = false
    @Published private(set) var programBusy = false
    @Published private(set) var programMessage = ""
    @Published private(set) var programIssue: ProgramIssue?
    @Published private(set) var exportURL: URL?
    private var programSource: ApplePlayerFacade?
    private var programGeneration = 0
    private var saveShouldResume = false
    private var awaitingProgramSelection = false
    private let programQueue = DispatchQueue(label: "com.digihori.pgp.ios.program-io")
    private let readProgram: @Sendable (URL) throws -> ReadProgram
    private let writeProgram: @Sendable (Data, URL) throws -> Void

    @Published private(set) var executionSpeed: String
    @Published private(set) var soundEnabled: Bool
    @Published private(set) var audioMessage = ""
    private let audio: PlayerAudioOutput?
    private let playbackPreferences: PlayerPlaybackPreferences
    private let monotonicNow: () -> Int64

    func setExecutionSpeed(_ value: String) {
        let speed = PlayerPlaybackPreferences.speeds.contains(value) ? value : "1.0"
        guard speed != executionSpeed else { return }
        audio?.stop()
        executionSpeed = speed
        playbackPreferences.speed = speed
        player?.setSpeed(id: speed, nowNanoseconds: monotonicNow())
        if soundEnabled && status.runState == "RUNNING" && isActive && !showHome { audio?.start() }
    }

    func setSoundEnabled(_ enabled: Bool) {
        soundEnabled = enabled
        playbackPreferences.sound = enabled
        audio?.stop()
        audioMessage = ""
        if enabled && status.runState == "RUNNING" && isActive && !showHome { audio?.start() }
    }

    private func applyPlaybackSettings() {
        player?.setSpeed(id: executionSpeed, nowNanoseconds: monotonicNow())
    }

    let input: IosPlayerInput
    @Published private(set) var inputMenuOpen = false
    @Published private(set) var inputFocused = true
    var externalInputReceiving: Bool { canReceiveExternal(status.runState) }
    private func canReceiveExternal(_ runState: String) -> Bool {
        isActive && !showHome && inputFocused && !inputMenuOpen && !romPanelOpen &&
            !programPanelOpen && !isBusy && !programBusy && runState == "RUNNING"
    }
    func setInputFocus(_ focused: Bool) {
        guard inputFocused != focused else { return }
        inputFocused = focused
        releaseAllKeys()
    }
    func setInputMenuOpen(_ open: Bool) {
        guard inputMenuOpen != open else { return }
        inputMenuOpen = open
        releaseAllKeys()
    }

    var hasSession: Bool { player != nil }
    var hasSavedRom: Bool { FileManager.default.fileExists(atPath: romStore.recordURL.path) }
    var selectedMachine: AppleMachineInfo { AppleMachineCatalog.shared.find(id: selectedMachineId)! }
    var machine: AppleMachineInfo { player?.machine ?? selectedMachine }
    var selectedRomSourceName: String? {
        guard hasSavedRom else { return nil }
        return player?.machine.romId == selectedMachine.romId ? romSourceName : "登録済み"
    }
    func selectedModel(_ seriesId: String) -> String { modelSelections[seriesId] ?? seriesId }
    func hasRom(_ id: String) -> Bool {
        let store = IosRomStore(root: romStore.root, machineId: id)
        return FileManager.default.fileExists(atPath: store.recordURL.path)
    }
    func isRunningSeries(_ id: String) -> Bool { player?.machine.romId == AppleMachineCatalog.shared.find(id: id)?.romId }
    func sourceName(_ id: String) -> String {
        isRunningSeries(id) ? (romSourceName ?? "Session") : (hasRom(id) ? "登録済み" : "ROM未登録")
    }
    func playMachine(_ id: String) {
        requestMachineSelection(id)
        guard selectedMachineId == id else { return }
        enterPlayer()
    }
    func manageRom(_ id: String) {
        requestMachineSelection(id)
        guard selectedMachineId == id else { return }
        openRomPanel()
    }
    func mappedPhysicalKey(_ code: Int32) -> String? {
        AppleInputCatalog.shared.keyForMachine(code: code, machineId: machine.id)
    }
    var operatingModes: [String] { showsRunProToggle ? machine.operatingModes : [] }
    var showsRunProToggle: Bool { ["pc-1245", "pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261"].contains(machine.id) }
    var modeToggleLabel: String { status.operatingMode == "PRO" ? "PRO" : "RUN" }
    func toggleOperatingMode() {
        guard showsRunProToggle else { return }
        setOperatingMode(status.operatingMode == "PRO" ? "RUN" : "PRO")
    }
    var skin: SkinDefinition? { player?.skin }
    var isControllerDisplay: Bool { presentationMode == .controllerDisplay }

    init(player: ApplePlayerFacade?, frames: PlayerFrameDriver, romStore: IosRomStore = .applicationStore(),
         readRom: @escaping @Sendable (URL) throws -> ReadRom = { try IosRomCodec.read(url: $0) },
         readProgram: @escaping @Sendable (URL) throws -> ReadProgram = { try IosProgramFiles.read($0) },
         startsAtHome: Bool = false,
         machinePreferences: IosMachinePreferences = IosMachinePreferences(),
         audio: PlayerAudioOutput? = nil,
         input: IosPlayerInput? = nil,
         playbackPreferences: PlayerPlaybackPreferences = PlayerPlaybackPreferences(),
         monotonicNow: @escaping () -> Int64 = { Int64(CACurrentMediaTime() * 1_000_000_000) },
         writeProgram: @escaping @Sendable (Data, URL) throws -> Void = { try $0.write(to: $1, options: .atomic) }) {
        self.input = input ?? IosPlayerInput()
        self.audio = audio
        self.playbackPreferences = playbackPreferences
        self.monotonicNow = monotonicNow
        self.executionSpeed = playbackPreferences.speed
        self.soundEnabled = playbackPreferences.sound
        self.player = player
        self.frames = frames
        self.machinePreferences = machinePreferences
        self.homeLayout = machinePreferences.homeLayout
        self.modelSelections = Dictionary(uniqueKeysWithValues: AppleMachineCatalog.shared.homeMachines.map { ($0.id, machinePreferences.model(for: $0.id)) })
        let selection = machinePreferences.selectedMachineId ?? romStore.machineId
        self.selectedMachineId = selection
        self.romStore = IosRomStore(root: romStore.root, machineId: selection, write: romStore.write)
        self.readRom = readRom
        self.readProgram = readProgram
        self.writeProgram = writeProgram
        self.showHome = startsAtHome
        audio?.onStatus = { [weak self] message in self?.audioMessage = message }
        self.input.onInput = { [weak self] in self?.refresh() }
        applyPlaybackSettings()
        refresh()
    }

    static func structuralProbe(frames: PlayerFrameDriver) -> PlayerHost {
        PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: frames)
    }

    #if DEBUG && targetEnvironment(simulator)
    /// Original instructions and bank markers only; shared by isolated Simulator tests.
    static func bankedProbeArchive(_ id: String) throws -> Data {
        guard ["pc-1360", "pc-1460", "pc-1470u", "pc-1360k"].contains(id) else { throw RomFailure(message: "Unsupported bank probe") }
        let count = id == "pc-1460" ? 4 : 8
        let selector = id == "pc-1460" ? 0x3c00 : 0x3400
        var code: [UInt8] = []
        func write(_ address: Int, _ value: UInt8) { code += [0x10, UInt8(address >> 8), UInt8(address & 255), 0x02, value, 0x52] }
        var files: [String: Data] = [:]
        for bank in 0..<count {
            write(selector, UInt8(bank)); code += [0x79, 0x40, 0]
            let target = 0x9000 + bank, back = code.count
            let bankCode: [UInt8] = [0x10, UInt8(target >> 8), UInt8(target & 255), 0x02, UInt8(bank + 1), 0x52, 0x79, UInt8(back >> 8), UInt8(back & 255)]
            var bytes = Data(repeating: 0x4d, count: 0x4000)
            bytes.replaceSubrange(0..<bankCode.count, with: bankCode)
            files["rom/bank-\(bank).bin"] = bytes
        }
        if id == "pc-1360" || id == "pc-1360k" {
            [0x2800, 0x2840, 0x281e, 0x285e].forEach { write($0, 1) }
            [0x301d, 0x305d, 0x303b, 0x307b].forEach { write($0, 0x80) }
            write(0x303c, id == "pc-1360k" ? 0xff : 0x5f)
        } else if id == "pc-1460" {
            write(0x3000, 1); write(0x3040, 64)
            write(0x303c, 31); write(0x303d, 127); write(0x307c, 127)
        } else {
            write(0x2800, 1); write(0x2a3b, 64); write(0x2840, 1); write(0x2a7b, 64)
            write(0x283c, 127); write(0x283d, 15); write(0x287c, 127); write(0x287d, 15)
        }
        if id == "pc-1360k" {
            for selector in 0..<4 {
                var kanji = Data(repeating: UInt8(0x40 + selector), count: 0x10000)
                kanji.replaceSubrange(0x1200..<0x1208, with: [0xff, 0x89, 0x89, 0x89, 0x89, 0x89, 0x89, 0xff])
                files["rom/kanji-selector-\(selector).bin"] = kanji
                write(0x3600, UInt8(selector))
                code += [0x10, 0x12, 0x34, 0x56, 0x10, 0xc1, UInt8(selector), 0x52]
                // READM into internal RAM address 2 (A), then save the result.
                code += [0x12, 0x02, 0x10, 0x12, 0x34, 0x54, 0x10, 0xc1, UInt8(0x10 + selector), 0x52]
            }
            for column in 0..<8 {
                code += [0x10, 0x12, UInt8(column), 0x56, 0x10, 0x28, UInt8(column), 0x52]
            }
        }
        let loop = code.count; code += [0x79, UInt8(loop >> 8), UInt8(loop & 255)]
        var internalBytes = Data(repeating: 0x4d, count: 0x2000)
        internalBytes.replaceSubrange(0..<code.count, with: code); files["rom/internal.bin"] = internalBytes
        let components = files.keys.sorted().map { path in
            RomManifest.Component(id: URL(fileURLWithPath: path).deletingPathExtension().lastPathComponent,
                role: path.contains("kanji-selector-") ? "kanji" : path == "rom/internal.bin" ? "internal" : "external", file: path,
                size: files[path]!.count, sha256: files[path]!.sha256)
        }
        files["manifest.json"] = try JSONEncoder().encode(RomManifest(format: "pgp-rom-package", formatVersion: 1, machineId: id, title: nil, components: components))
        return RomArchive.write(files)
    }
    #endif

    static func initial() -> PlayerHost {
        #if DEBUG && targetEnvironment(simulator)
        if ProcessInfo.processInfo.arguments.contains("--series-probe"),
           let token = ProcessInfo.processInfo.environment["PGP_SERIES_PROBE_ID"], UUID(uuidString: token) != nil {
            let root = FileManager.default.temporaryDirectory.appendingPathComponent("series-probe-" + token)
            let defaults = UserDefaults(suiteName: "pgp-series-probe-" + token)!
            let preferences = IosMachinePreferences(defaults: defaults)
            let host = PlayerHost(player: nil, frames: DisplayLinkDriver(), romStore: IosRomStore(root: root),
                                  startsAtHome: true, machinePreferences: preferences)
            do {
                var bytes = Data(repeating: 0x4d, count: 0x8000)
                bytes[0] = 0x79; bytes[1] = 0; bytes[2] = 0
                let input = try IosRomCodec.read(bytes: bytes, name: "Original synthetic loop")
                for id in ["pc-1245", "pc-1251", "pc-1261", "pc-1350", "pc-1401", "pc-1450", "pc-1360", "pc-1460", "pc-1470u", "pc-1360k"] {
                    let store = IosRomStore(root: root, machineId: id)
                    if !FileManager.default.fileExists(atPath: store.recordURL.path) {
                        let probeInput: ReadRom
                        if ["pc-1360", "pc-1460", "pc-1470u", "pc-1360k"].contains(id) {
                            probeInput = try IosRomCodec.read(bytes: Self.bankedProbeArchive(id), name: "Original bank and LCD exercise.pgrom")
                        } else if ["pc-1261", "pc-1350", "pc-1401", "pc-1450"].contains(id) {
                            var image = Data(repeating: 0x4d, count: 0x10000)
                            // Original LCD exercise: distinct pixels in both rows and all status flags.
                            var code: [UInt8] = []
                            func write(_ address: Int, _ value: UInt8) {
                                code += [0x10, UInt8(address >> 8), UInt8(address & 255), 0x02, value, 0x52]
                            }
                            if id == "pc-1350" {
                                [0x7000, 0x7040, 0x701e, 0x705e].forEach { write($0, 1) }
                                [0x781d, 0x785d, 0x783b, 0x787b].forEach { write($0, 0x80) }
                                write(0x783c, 0xf3)
                            } else if id == "pc-1401" || id == "pc-1450" {
                                let base = id == "pc-1401" ? 0x6000 : 0x7000
                                write(base, 1); write(base + (id == "pc-1401" ? 0x40 : 0x68), 64)
                                write(base + 0x3c, 0x0f); write(base + 0x3d, 0x7f); write(base + 0x7c, 0x7f)
                            } else {
                                write(0x2000, 1); write(0x283b, 64); write(0x2040, 1); write(0x287b, 64)
                                write(0x203d, 0x7b); write(0x207c, 0x23)
                            }
                            let loop = code.count; code += [0x79, UInt8(loop >> 8), UInt8(loop & 255)]
                            image.replaceSubrange(0..<code.count, with: code)
                            probeInput = try IosRomCodec.read(bytes: image, name: "Original two-row LCD exercise")
                        } else { probeInput = input }
                        let prepared = try IosRomCodec.prepare(probeInput, machineId: id)
                        try store.install(archive: prepared.archive, sourceName: "Original synthetic loop")
                    }
                }
                if preferences.runningMachineId == nil { preferences.runningMachineId = "pc-1245" }
                host.restoreRom()
            } catch { host.romMessage = "Series probe failed: \(error.localizedDescription)" }
            return host
        }
        if ProcessInfo.processInfo.arguments.contains("--picker-probe") || ProcessInfo.processInfo.arguments.contains("--navigation-probe") {
            // Opt-in UI test data only. Never reads/writes the normal ROM Library.
            let root = FileManager.default.temporaryDirectory.appendingPathComponent("picker-probe-" + UUID().uuidString)
            let host = PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: DisplayLinkDriver(),
                                  romStore: IosRomStore(root: root))
            if ProcessInfo.processInfo.arguments.contains("--navigation-probe") {
                let initDump = AppleProgramCodec.shared.parse(bytes: Data("C6E1 00 C0 FF BF".utf8).kotlinBytes, basic: false)
                if let input = initDump.input { _ = host.player?.loadProgram(input: input) }
                let empty = AppleProgramCodec.shared.parse(bytes: Data().kotlinBytes, basic: true)
                if let input = empty.input { _ = host.player?.loadProgram(input: input) }
                host.showHome = true
            }
            let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            do {
                try FileManager.default.createDirectory(at: documents, withIntermediateDirectories: true)
                let input = try IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "synthetic.bin")
                let prepared = try IosRomCodec.prepare(input)
                for (name, data) in [("picker-test.pgrom", prepared.archive),
                                     ("picker-test.bas", Data("10 PRINT 1\n".utf8)),
                                     ("picker-test.dmp", Data("C500 01 02\nC510 FF\n".utf8)),
                                     ("picker-invalid.pgrom", Data([1, 2, 3]))] {
                    try data.write(to: documents.appendingPathComponent(name), options: .atomic)
                }
            } catch { host.romMessage = "Picker probe setup failed: \(error.localizedDescription)" }
            return host
        }
        if ProcessInfo.processInfo.arguments.contains("--structural-probe") {
            return structuralProbe(frames: DisplayLinkDriver())
        }
        #endif
        let host = PlayerHost(player: nil, frames: DisplayLinkDriver(), machinePreferences: IosMachinePreferences(defaults: .standard), audio: IosPlayerAudio(),
                              input: IosPlayerInput(defaults: .standard, observeSystem: true),
                              playbackPreferences: PlayerPlaybackPreferences(defaults: .standard))
        host.showHome = true
        host.restoreRom()
        return host
    }

    func setActive(_ active: Bool) {
        guard isActive != active else { return }
        isActive = active
        if active {
            if pendingAutomaticStart && !showHome { run() }
        } else {
            inputMenuOpen = false
            let waiting = pendingAutomaticStart
            pause()
            pendingAutomaticStart = waiting
        }
    }

    func run() {
        guard isActive, !showHome, hasSession, status.runState == "PAUSED", !startRequested else { return }
        startRequested = true
        if soundEnabled { audio?.start() }
        frames.start { [weak self] now in self?.onFrame(now) }
    }

    func pause() {
        pendingAutomaticStart = false
        startRequested = false
        frames.stop()
        audio?.stop()
        player?.pause()
        inputEpoch += 1
        refresh()
    }

    func reset() {
        invalidateProgramOperation()
        pause()
        guard let player else { return }
        player.reset()
        lcd = nil
        refresh()
        requestAutomaticRun()
    }

    /// ROM/load success will call this in later stages; activity alone does not resume CPU.
    func requestAutomaticRun() {
        guard hasSession, status.runState == "PAUSED" else { return }
        pendingAutomaticStart = true
        if isActive && !showHome { run() }
    }

    private func onFrame(_ now: Int64) {
        guard isActive, let player else { return }
        if startRequested {
            startRequested = false
            pendingAutomaticStart = false
            player.run(nowNanoseconds: now)
        }
        let packet = player.tick(nowNanoseconds: now)
        refresh()
        if status.runState != "RUNNING" { frames.stop(); audio?.stop() }
        else if soundEnabled, let packet { audio?.offer(PlayerPCM(packet)) }
    }

    func pressBreak(owner: String) {
        _ = press(keyName: "BREAK", owner: owner)
    }

    @discardableResult
    func press(keyName: String, owner: String) -> Bool {
        guard isActive, !showHome, hasSession, !isControllerDisplay, !inputMenuOpen, !romPanelOpen, !programPanelOpen, !isBusy else { return false }
        let accepted = player?.press(keyName: keyName, owner: owner) ?? false
        refresh()
        return accepted
    }

    func release(owner: String) {
        player?.release(owner: owner)
        refresh()
    }

    func releaseAllKeys() {
        player?.releaseAllKeys()
        inputEpoch += 1
        refresh()
    }

    func setProgramMode(_ enabled: Bool) {
        setOperatingMode(enabled ? "PRO" : "RUN")
    }

    func togglePresentation() {
        releaseAllKeys()
        presentationMode = presentationMode.toggleControllerDisplay()
    }

    func openRomPanel() {
        guard !isBusy else { return }
        homeShouldRun = false
        pause()
        invalidateProgramOperation()
        romPanelOpen = true
    }

    func openRomPicker() {
        guard !isBusy else { return }
        homeShouldRun = false
        pause()
        invalidateProgramOperation()
        preparedRom = nil
        awaitingReplacement = false
        awaitingDeletion = false
        romMessage = selectedMachine.supportsLegacyRom
            ? "\(selectedMachine.displayName)のpgromまたは\(selectedMachine.legacyRomSizes)の単体ROMを選択してください。"
            : "\(selectedMachine.displayName)のpgromを選択してください。内部ROMと全バンクを含むpackageをStudioで作成できます。"
        romPanelOpen = true
        pickerPresented = true
    }

    func cancelRomPicker() {
        pickerPresented = false
        romMessage = "取込みをキャンセルしました。"
    }

    func importRom(_ url: URL) {
        pickerPresented = false
        guard !isBusy else { return }
        pause()
        invalidateProgramOperation()
        romPanelOpen = true
        romGeneration += 1
        let ticket = romGeneration
        let read = readRom
        canCancelRomRead = true
        isBusy = true
        romMessage = "ROMを検証しています…"
        romQueue.async { [weak self] in
            let result = Result { try read(url) }
            DispatchQueue.main.async {
                guard let self, self.romGeneration == ticket else { return }
                self.isBusy = false
                self.canCancelRomRead = false
                do {
                    self.preparedRom = try IosRomCodec.prepare(result.get(), machineId: self.selectedMachineId)
                    if self.hasSession || FileManager.default.fileExists(atPath: self.romStore.recordURL.path) {
                        self.awaitingReplacement = true
                        self.romMessage = "ROMを置換します。現在のRAMとロード済みプログラムは失われます。"
                    } else { self.confirmRomInstall() }
                } catch { self.romMessage = "取込みに失敗しました。\n\(error.localizedDescription)\n現在のROM・Sessionは保持しています。" }
            }
        }
    }

    /// Invalidate delivery immediately; a provider/file read already in progress may finish later.
    /// The I/O queue never owns a Session or commits the selected file to the ROM Library.
    func cancelRomRead() {
        guard canCancelRomRead else { return }
        romGeneration += 1
        canCancelRomRead = false
        isBusy = false
        preparedRom = nil
        pause()
        romMessage = "取込みをキャンセルしました。"
    }

    func confirmRomInstall() {
        guard !isBusy, let prepared = preparedRom else { return }
        let archive = prepared.archive, name = prepared.sourceName, store = romStore
        awaitingReplacement = false
        isBusy = true
        romMessage = "ROMを保存しています…"
        romQueue.async { [weak self] in
            let result = Result { try store.install(archive: archive, sourceName: name) }
            DispatchQueue.main.async {
                guard let self else { return }
                self.isBusy = false
                switch result {
                case .success:
                    self.pause()
                    self.lcd = nil
                    self.player = prepared.player
                    self.machinePreferences.runningMachineId = prepared.player.machine.id
                    self.applyPlaybackSettings()
                    self.homeShouldRun = true
                    self.hasEnteredPlayer = false
                    self.romSourceName = name
                    self.preparedRom = nil
                    self.romMessage = prepared.unlistedCount == 0 ? "ROMを登録しました。" : "ROMを登録しました（未使用entry \(prepared.unlistedCount)件）。"
                    self.romPanelOpen = false
                    self.refresh()
                    self.requestAutomaticRun()
                case .failure(let error):
                    self.preparedRom = nil
                    self.romMessage = "保存に失敗しました。現在のROMは保持しています。\n\(error.localizedDescription)"
                }
            }
        }
    }

    func cancelRomChange() {
        guard !isBusy else { return }
        preparedRom = nil
        awaitingReplacement = false
        awaitingDeletion = false
        romMessage = "変更をキャンセルしました。"
    }

    func requestRomDeletion() {
        guard !isBusy, hasSavedRom else { return }
        pause()
        awaitingDeletion = true
        awaitingReplacement = false
        romMessage = "保存ROMを削除します。現在のSession・RAMも破棄します。"
    }

    func confirmRomDeletion() {
        guard !isBusy, awaitingDeletion else { return }
        let store = romStore
        isBusy = true
        awaitingDeletion = false
        romQueue.async { [weak self] in
            let result = Result { try store.delete() }
            DispatchQueue.main.async {
                guard let self else { return }
                self.isBusy = false
                switch result {
                case .success:
                    self.pause()
                    self.invalidateProgramOperation()
                    if self.player?.machine.romId == store.machineId {
                        self.player = nil
                        self.machinePreferences.runningMachineId = nil
                        self.lcd = nil
                        self.status = PlayerStatus()
                        self.pressedKeys = []
                        self.romSourceName = nil
                    }
                    self.showHome = true
                    self.romMessage = "ROMを削除しました。"
                case .failure(let error): self.romMessage = "削除できませんでした。\n\(error.localizedDescription)"
                }
            }
        }
    }

    func closeRomPanel() {
        guard !isBusy else { return }
        cancelRomChange()
        romPanelOpen = false
    }

    func restoreRom(modelId: String? = nil, enter: Bool = false) {
        guard !isBusy else { return }
        invalidateProgramOperation()
        pause()
        let id = modelId ?? machinePreferences.runningMachineId ?? selectedMachineId
        let store = IosRomStore(root: romStore.root, machineId: id, write: romStore.write)
        isBusy = true
        romMessage = "保存ROMを確認しています…"
        romQueue.async { [weak self] in
            let result = Result { try store.load() }
            DispatchQueue.main.async {
                guard let self else { return }
                self.isBusy = false
                do {
                    if let input = try result.get() {
                        let restored = try IosRomCodec.prepare(input, machineId: id)
                        self.lcd = nil
                        self.player = restored.player
                        self.machinePreferences.runningMachineId = id
                        self.applyPlaybackSettings()
                        self.homeShouldRun = true
                        self.hasEnteredPlayer = false
                        self.romSourceName = restored.sourceName
                        self.romMessage = "保存ROMを復元しました。"
                        self.refresh()
                        self.requestAutomaticRun()
                        if enter { self.enterPlayerNow() }
                    } else { self.romMessage = "\(self.machine.displayName)のROMを登録してください。" }
                } catch { self.romMessage = "保存ROMを復元できませんでした。\n\(error.localizedDescription)" }
            }
        }
    }

    /// Choosing a model changes preferences only. RAM belongs to the running Session until Play is confirmed.
    func requestMachineSelection(_ id: String) {
        guard !isBusy, preparedRom == nil, AppleMachineCatalog.shared.find(id: id) != nil else { return }
        pendingMachineId = nil
        selectedMachineId = id
        modelSelections[selectedMachine.romId] = id
        machinePreferences.select(id)
        romStore = IosRomStore(root: romStore.root, machineId: id, write: romStore.write)
    }

    func cancelMachineSelection() { pendingMachineId = nil }

    func confirmMachineSelection() {
        guard let id = pendingMachineId, !isBusy else { return }
        pendingMachineId = nil
        restoreRom(modelId: id, enter: true)
    }

    func enterPlayer() {
        guard !isBusy else { return }
        if player?.machine.id == selectedMachineId { enterPlayerNow() }
        else if !hasSavedRom { openRomPicker() }
        else if hasSession { pendingMachineId = selectedMachineId }
        else { restoreRom(modelId: selectedMachineId, enter: true) }
    }

    private func enterPlayerNow() {
        guard hasSession, !isBusy else { return }
        inputMenuOpen = false
        showHome = false
        hasEnteredPlayer = true
        if homeShouldRun { requestAutomaticRun() }
    }

    func returnHome() {
        guard !programPanelOpen, !romPanelOpen else { return }
        homeShouldRun = status.runState == "RUNNING" || pendingAutomaticStart || startRequested
        pause()
        inputMenuOpen = false
        showHome = true
    }

    func enterBackground() {
        saveShouldResume = false
        if hasEnteredPlayer { homeShouldRun = false }
        setActive(false)
    }

    func setOperatingMode(_ name: String) {
        guard operatingModes.contains(name) else { return }
        releaseAllKeys()
        _ = player?.setOperatingMode(name: name)
        refresh()
    }

    func toggleRunState() {
        if status.runState == "RUNNING" || startRequested { pause() }
        else if status.runState == "PAUSED" { run() }
    }

    func openProgram(save: Bool) {
        guard !showHome, let player, !isBusy else { return }
        invalidateProgramOperation()
        programSource = player
        saveShouldResume = save && (status.runState == "RUNNING" || startRequested || pendingAutomaticStart)
        pause()
        programSaving = save
        programMessage = save ? "BASIC全体またはRAM範囲をセーブします。" : "bas／dmpを選択してください。"
        programIssue = nil
        programPanelOpen = true
        if !save { awaitingProgramSelection = true }
    }

    func programPanelDidAppear() {
        guard programPanelOpen, !programSaving, awaitingProgramSelection, !programPickerPresented else { return }
        programDocument = .importing
    }

    func chooseProgramFile() {
        guard programSource === player, !programBusy else { return }
        pause()
        programIssue = nil
        programMessage = "bas／dmpを選択してください。"
        awaitingProgramSelection = true
        programDocument = .importing
    }

    func cancelProgramPicker() {
        awaitingProgramSelection = false
        programDocument = nil
        programMessage = "ロードをキャンセルしました。"
    }

    func importProgram(_ url: URL) {
        awaitingProgramSelection = false
        programDocument = nil
        guard let source = programSource, source === player, !programSaving else { return }
        programGeneration += 1
        let ticket = programGeneration, read = readProgram
        let machineId = source.machine.id
        programBusy = true
        programIssue = nil
        programMessage = "読み込み・検証しています…"
        programQueue.async { [weak self] in
            let result = Result { try read(url) }
            // Parsing owns copied bytes and does not touch the Session.
            let parsed = result.map { AppleProgramCodec.shared.parseForMachine(bytes: $0.bytes.kotlinBytes, basic: $0.basic, machineId: machineId) }
            DispatchQueue.main.async {
                guard let self, self.programGeneration == ticket, source === self.player else { return }
                self.programBusy = false
                switch parsed {
                case .failure(let error): self.setProgramFailure(error)
                case .success(let value):
                    if let error = value.error { self.programIssue = .shared(error); return }
                    guard let input = value.input else { return }
                    self.pause()
                    if let error = source.loadProgram(input: input) { self.programIssue = .shared(error); return }
                    self.refresh()
                    self.programMessage = "ロードしました。CPUを再開します。BASICのRUNコマンドは自動入力しません。"
                    self.requestAutomaticRun()
                }
            }
        }
    }

    func prepareProgramExport(basic: Bool, start: String, end: String) {
        guard let source = programSource, source === player, programSaving, !programBusy else { return }
        programIssue = nil
        if status.runState == "RUNNING" || startRequested { saveShouldResume = true }
        pause()
        let result: AppleProgramExport
        if basic { result = source.exportBasic() }
        else {
            guard let first = IosProgramFiles.address(start), let last = IosProgramFiles.address(end), first <= last else {
                programIssue = .file(.invalidAddress); return
            }
            result = source.exportDump(start: first, end: last)
        }
        if let error = result.error { programIssue = .shared(error); return }
        guard let content = result.doCopyBytes() else { return }
        let bytes = Data(kotlin: content)
        let write = writeProgram
        let machineId = source.machine.id
        programGeneration += 1
        let ticket = programGeneration
        programBusy = true
        programMessage = "保存データを準備しています…"
        programQueue.async { [weak self] in
            let output = Result { () -> URL in
                let folder = FileManager.default.temporaryDirectory.appendingPathComponent("program-export-" + UUID().uuidString)
                try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
                let url = folder.appendingPathComponent(machineId + "-program." + (basic ? "bas" : "dmp"))
                do { try write(bytes, url) }
                catch { try? FileManager.default.removeItem(at: folder); throw error }
                return url
            }
            DispatchQueue.main.async {
                guard let self, self.programGeneration == ticket, source === self.player else {
                    if case .success(let url) = output { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }
                    return
                }
                self.programBusy = false
                switch output {
                case .success(let url):
                    self.exportURL = url
                    self.programDocument = .exporting(url)
                    self.programMessage = "保存先を選択してください。"
                case .failure(let error): self.setProgramFailure(error)
                }
            }
        }
    }

    func finishProgramExport(saved: Bool) {
        programDocument = nil
        programMessage = saved ? "ファイルへセーブしました。クラウド同期の完了を示すものではありません。" : "セーブをキャンセルしました。"
        cleanupProgramExport()
        restoreSaveIntent()
    }

    func programDocumentDidDismiss() {
        if exportURL != nil { finishProgramExport(saved: false) }
        else if awaitingProgramSelection { cancelProgramPicker() }
    }

    func closeProgram() {
        let resume = saveShouldResume && programSource === player
        invalidateProgramOperation()
        programPanelOpen = false
        if resume { requestAutomaticRun() }
    }

    private func restoreSaveIntent() {
        let resume = saveShouldResume && programSource === player
        saveShouldResume = false
        if resume { requestAutomaticRun() }
    }

    private func cleanupProgramExport() {
        if let url = exportURL { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }
        exportURL = nil
    }

    private func invalidateProgramOperation() {
        programGeneration += 1
        programPanelOpen = false
        programSource = nil
        awaitingProgramSelection = false
        saveShouldResume = false
        programBusy = false
        programDocument = nil
        programIssue = nil
        cleanupProgramExport()
    }

    private func setProgramFailure(_ error: Error) {
        if let file = error as? ProgramFileError { programIssue = .file(file) }
        else { programIssue = .io(error.localizedDescription) }
    }

    private func refresh() {
        let state = player?.snapshot().runState ?? "PAUSED"
        input.configure(player: player, receiving: canReceiveExternal(state), epoch: inputEpoch)
        guard let snapshot = player?.snapshot() else { return }
        let next = PlayerStatus(runState: snapshot.runState, operatingMode: snapshot.operatingMode,
                                executedCycles: snapshot.executedCycles, breakHeld: snapshot.breakHeld)
        // Cycle accounting is not UI text. Avoid rebuilding native menus on every idle LCD tick.
        if next.runState != status.runState || next.operatingMode != status.operatingMode || next.breakHeld != status.breakHeld {
            objectWillChange.send()
        }
        status = next
        let display = snapshot.display
        if lcd?.revision != display.revision || lcd?.enabled != display.enabled || lcd?.symbols != display.symbols {
            lcd = display
        }
        let held = Set(snapshot.heldKeys)
        if pressedKeys != held { pressedKeys = held }
    }
}

/// Selection and running identity are separate; no RAM or firmware is stored in preferences.
enum PlayerHomeLayout: String, CaseIterable {
    case tiles, list
    var label: String { self == .tiles ? "タイル" : "リスト" }
}

final class IosMachinePreferences {
    private let defaults: UserDefaults?
    init(defaults: UserDefaults? = nil) { self.defaults = defaults }
    var homeLayout: PlayerHomeLayout {
        get { defaults?.string(forKey: "home.layout").flatMap(PlayerHomeLayout.init(rawValue:)) ?? .tiles }
        set { defaults?.set(newValue.rawValue, forKey: "home.layout") }
    }
    var selectedMachineId: String? {
        defaults?.string(forKey: "machine.selected").flatMap { AppleMachineCatalog.shared.find(id: $0)?.id }
    }
    var runningMachineId: String? {
        get { defaults?.string(forKey: "machine.running").flatMap { AppleMachineCatalog.shared.find(id: $0)?.id } }
        set { defaults?.set(newValue, forKey: "machine.running") }
    }
    func model(for seriesId: String) -> String {
        guard let id = defaults?.string(forKey: "machine.model." + seriesId),
              AppleMachineCatalog.shared.models(id: seriesId).contains(where: { $0.id == id }) else { return seriesId }
        return id
    }
    func select(_ id: String) {
        guard let machine = AppleMachineCatalog.shared.find(id: id) else { return }
        defaults?.set(id, forKey: "machine.selected")
        defaults?.set(id, forKey: "machine.model." + machine.romId)
    }
}
