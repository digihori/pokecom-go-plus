import Foundation
import QuartzCore
import PgpPlayer

struct PlayerStatus: Equatable {
    var runState = "PAUSED"
    var operatingMode = "RUN"
    var executedCycles: Int64 = 0
    var breakHeld = false
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
    @Published private(set) var status = PlayerStatus()
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
    private let romStore: IosRomStore
    private let romQueue = DispatchQueue(label: "com.digihori.pgp.ios.rom-io")
    private var preparedRom: PreparedRom?
    private var romGeneration = 0

    var hasSession: Bool { player != nil }
    var hasSavedRom: Bool { FileManager.default.fileExists(atPath: romStore.recordURL.path) }
    var skin: SkinDefinition? { player?.skin }
    var isControllerDisplay: Bool { presentationMode == .controllerDisplay }

    init(player: ApplePlayerFacade?, frames: PlayerFrameDriver, romStore: IosRomStore = .applicationStore()) {
        self.player = player
        self.frames = frames
        self.romStore = romStore
        refresh()
    }

    static func structuralProbe(frames: PlayerFrameDriver) -> PlayerHost {
        PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: frames)
    }

    static func initial() -> PlayerHost {
        #if DEBUG && targetEnvironment(simulator)
        if ProcessInfo.processInfo.arguments.contains("--structural-probe") {
            return structuralProbe(frames: DisplayLinkDriver())
        }
        #endif
        let host = PlayerHost(player: nil, frames: DisplayLinkDriver())
        host.restoreRom()
        return host
    }

    func setActive(_ active: Bool) {
        guard isActive != active else { return }
        isActive = active
        if active {
            if pendingAutomaticStart { run() }
        } else {
            let waiting = pendingAutomaticStart
            pause()
            pendingAutomaticStart = waiting
        }
    }

    func run() {
        guard isActive, hasSession, status.runState == "PAUSED", !startRequested else { return }
        startRequested = true
        frames.start { [weak self] now in self?.onFrame(now) }
    }

    func pause() {
        pendingAutomaticStart = false
        startRequested = false
        frames.stop()
        player?.pause()
        inputEpoch += 1
        refresh()
    }

    func reset() {
        pause()
        guard let player else { return }
        player.reset()
        refresh()
        requestAutomaticRun()
    }

    /// ROM/load success will call this in later stages; activity alone does not resume CPU.
    func requestAutomaticRun() {
        guard hasSession, status.runState == "PAUSED" else { return }
        pendingAutomaticStart = true
        if isActive { run() }
    }

    private func onFrame(_ now: Int64) {
        guard isActive, let player else { return }
        if startRequested {
            startRequested = false
            pendingAutomaticStart = false
            player.run(nowNanoseconds: now)
        }
        player.tick(nowNanoseconds: now)
        refresh()
        if status.runState != "RUNNING" { frames.stop() }
    }

    func pressBreak(owner: String) {
        _ = press(keyName: "BREAK", owner: owner)
    }

    @discardableResult
    func press(keyName: String, owner: String) -> Bool {
        guard isActive, hasSession, !isControllerDisplay, !romPanelOpen, !isBusy else { return false }
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
        releaseAllKeys()
        player?.setProgramMode(enabled: enabled)
        refresh()
    }

    func togglePresentation() {
        releaseAllKeys()
        presentationMode = presentationMode.toggleControllerDisplay()
    }

    func openRomPanel() {
        guard !isBusy else { return }
        pause()
        romPanelOpen = true
    }

    func openRomPicker() {
        guard !isBusy else { return }
        pause()
        preparedRom = nil
        awaitingReplacement = false
        awaitingDeletion = false
        romMessage = "pgromまたはPC-1245の32/64KiB ROMを選択してください。"
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
        romPanelOpen = true
        romGeneration += 1
        let ticket = romGeneration
        isBusy = true
        romMessage = "ROMを検証しています…"
        romQueue.async { [weak self] in
            let result = Result { try IosRomCodec.read(url: url) }
            DispatchQueue.main.async {
                guard let self, self.romGeneration == ticket else { return }
                self.isBusy = false
                do {
                    self.preparedRom = try IosRomCodec.prepare(result.get())
                    if self.hasSession || FileManager.default.fileExists(atPath: self.romStore.recordURL.path) {
                        self.awaitingReplacement = true
                        self.romMessage = "ROMを置換します。現在のRAMとロード済みプログラムは失われます。"
                    } else { self.confirmRomInstall() }
                } catch { self.romMessage = "取込みに失敗しました。\n\(error.localizedDescription)\n現在のROM・Sessionは保持しています。" }
            }
        }
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
                    self.player = prepared.player
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
                    self.player = nil
                    self.lcd = nil
                    self.status = PlayerStatus()
                    self.pressedKeys = []
                    self.romSourceName = nil
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

    func restoreRom() {
        guard !isBusy else { return }
        pause()
        let store = romStore
        isBusy = true
        romMessage = "保存ROMを確認しています…"
        romQueue.async { [weak self] in
            let result = Result { try store.load() }
            DispatchQueue.main.async {
                guard let self else { return }
                self.isBusy = false
                do {
                    if let input = try result.get() {
                        let restored = try IosRomCodec.prepare(input)
                        self.player = restored.player
                        self.romSourceName = restored.sourceName
                        self.romMessage = "保存ROMを復元しました。"
                        self.refresh()
                        self.requestAutomaticRun()
                    } else { self.romMessage = "PC-1245のROMを登録してください。" }
                } catch { self.romMessage = "保存ROMを復元できませんでした。\n\(error.localizedDescription)" }
            }
        }
    }

    private func refresh() {
        guard let snapshot = player?.snapshot() else { return }
        status = PlayerStatus(runState: snapshot.runState, operatingMode: snapshot.operatingMode,
                              executedCycles: snapshot.executedCycles, breakHeld: snapshot.breakHeld)
        lcd = snapshot.display
        pressedKeys = Set(snapshot.heldKeys)
    }
}
