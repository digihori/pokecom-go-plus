import GameController
import AVFoundation
import XCTest
import UIKit
import PgpPlayer
@testable import PokecomGOPlayer

@MainActor
private final class ManualFrames: PlayerFrameDriver {
    var callback: ((Int64) -> Void)?
    func start(_ frame: @escaping (Int64) -> Void) { callback = frame }
    func stop() { callback = nil }
    func send(_ now: Int64) { callback?(now) }
}

@MainActor
final class PlayerHostTests: XCTestCase {
    func testRunDoesNotScheduleFramesBeforeSceneActivation() {
        let frames = ManualFrames()
        let host = PlayerHost.structuralProbe(frames: frames)
        host.run()
        XCTAssertNil(frames.callback)
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.setActive(true)
        host.run()
        frames.send(0)
        XCTAssertEqual(host.status.runState, "RUNNING")
        frames.send(10_000_000)
        XCTAssertGreaterThan(host.status.executedCycles, 0)
        XCTAssertTrue(host.lcd?.symbols.contains(.shift) == true)
    }

    func testOrdinaryResumeStaysPausedAndReleasesInput() {
        let frames = ManualFrames()
        let host = PlayerHost.structuralProbe(frames: frames)
        host.setActive(true)
        host.run()
        frames.send(0)
        frames.send(10_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        XCTAssertGreaterThan(host.status.executedCycles, 0)
        host.pressBreak(owner: "touch")
        XCTAssertTrue(host.status.breakHeld)
        // Even an already-enqueued old display-link callback must not tick while inactive.
        let oldCallback = frames.callback
        host.setActive(false)
        let pausedCycles = host.status.executedCycles
        oldCallback?(20_000_000_000)
        XCTAssertEqual(host.status.executedCycles, pausedCycles)
        XCTAssertFalse(host.status.breakHeld)
        host.setActive(true)
        XCTAssertNil(frames.callback)
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.run()
        frames.send(20_000_000_000)
        frames.send(20_010_000_000)
        XCTAssertGreaterThan(host.status.executedCycles - pausedCycles, 0)
        XCTAssertLessThanOrEqual(host.status.executedCycles - pausedCycles, 1_930)
    }

    func testResetRestoresRunModeAndDefersAutomaticStartUntilFirstActiveFrame() {
        let frames = ManualFrames()
        let host = PlayerHost.structuralProbe(frames: frames)
        host.setActive(true)
        host.setProgramMode(true)
        host.pressBreak(owner: "touch")
        host.reset()
        XCTAssertEqual(host.status.operatingMode, "RUN")
        XCTAssertFalse(host.status.breakHeld)
        XCTAssertTrue(host.pendingAutomaticStart)
        host.setActive(false)
        XCTAssertTrue(host.pendingAutomaticStart)
        XCTAssertNil(frames.callback)
        host.setActive(true)
        frames.send(1_000_000_000)
        XCTAssertFalse(host.pendingAutomaticStart)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.setActive(false)
        host.setActive(true)
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertNil(frames.callback)
    }

    func testExplicitPauseCancelsUnconsumedAutomaticStart() {
        let frames = ManualFrames()
        let host = PlayerHost.structuralProbe(frames: frames)
        host.reset() // Reset while inactive waits.
        XCTAssertTrue(host.pendingAutomaticStart)
        host.pause()
        host.setActive(true)
        XCTAssertFalse(host.pendingAutomaticStart)
        XCTAssertNil(frames.callback)
    }

    func testTwoInputOwnersAndRotationRelease() {
        let host = PlayerHost.structuralProbe(frames: ManualFrames())
        host.setActive(true)
        host.pressBreak(owner: "first")
        host.pressBreak(owner: "second")
        host.release(owner: "first")
        XCTAssertTrue(host.status.breakHeld)
        host.releaseAllKeys()
        XCTAssertFalse(host.status.breakHeld)
    }

    func testSkinTouchOwnershipDragCancelAndDisplaySwitch() throws {
        let host = PlayerHost.structuralProbe(frames: ManualFrames())
        host.setActive(true)
        let skin = try XCTUnwrap(host.skin)
        let view = PlayerSkinView(skin: skin)
        view.frame = CGRect(x: 0, y: 0, width: 667, height: 300)
        view.router.onPress = { host.press(keyName: $0, owner: $1) }
        view.router.onRelease = { host.release(owner: $0) }
        view.layoutIfNeeded()
        let key = try XCTUnwrap(skin.keyRegions.first { $0.key.name == "SHIFT" })
        let rect = view.screenRect(key.bounds)
        let point = CGPoint(x: rect.midX, y: rect.midY)
        view.router.begin(owner: "one", point: point)
        view.router.begin(owner: "two", point: point)
        XCTAssertEqual(host.pressedKeys, ["SHIFT"])
        view.router.end(owner: "one")
        XCTAssertEqual(host.pressedKeys, ["SHIFT"])
        view.router.move(owner: "two", point: CGPoint(x: -1, y: -1))
        XCTAssertTrue(host.pressedKeys.isEmpty)
        view.router.move(owner: "two", point: point)
        XCTAssertTrue(host.pressedKeys.isEmpty) // Moving back in does not invent a new down.
        view.router.begin(owner: "three", point: point)
        view.router.cancel()
        XCTAssertTrue(host.pressedKeys.isEmpty)
        view.router.begin(owner: "four", point: point)
        host.togglePresentation()
        XCTAssertTrue(host.isControllerDisplay)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        XCTAssertFalse(host.press(keyName: "SHIFT", owner: "controller-hidden-key"))
        host.togglePresentation()
        XCTAssertTrue(host.press(keyName: "SHIFT", owner: "normal-key"))
        host.reset()
        XCTAssertFalse(host.isControllerDisplay)
        XCTAssertTrue(host.pressedKeys.isEmpty)
    }

    func testSkinAccessibilityAndCropUseSharedCoordinates() throws {
        let host = PlayerHost.structuralProbe(frames: ManualFrames())
        let skin = try XCTUnwrap(host.skin)
        let lcd = try XCTUnwrap(host.lcd)
        let view = PlayerSkinView(skin: skin)
        view.frame = CGRect(x: 0, y: 0, width: 734, height: 325)
        view.update(lcd: lcd, mode: .fullDevice, operatingMode: "PRO", pressedKeys: [], inputEpoch: 0, active: true)
        view.layoutIfNeeded()
        let elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
        XCTAssertEqual(elements.filter { $0.accessibilityTraits.contains(.button) }.count, 52)
        for element in elements { XCTAssertTrue(view.bounds.contains(element.accessibilityFrameInContainerSpace)) }
        let original = try XCTUnwrap(view.router.placement)
        let point = view.screenRect(skin.lcdContentRegion).origin
        XCTAssertEqual(original.skinX(screenX: Float(point.x)), 70, accuracy: 0.01)
        XCTAssertEqual(original.skinY(screenY: Float(point.y)), 104, accuracy: 0.01)
        view.update(lcd: lcd, mode: .controllerDisplay, operatingMode: "PRO", pressedKeys: [], inputEpoch: 1, active: true)
        view.layoutIfNeeded()
        XCTAssertGreaterThan(try XCTUnwrap(view.router.placement).scale, original.scale)
        XCTAssertEqual((view.accessibilityElements as? [UIAccessibilityElement])?.filter { $0.accessibilityTraits.contains(.button) }.count, 0)
        XCTAssertTrue(view.bounds.contains(view.screenRect(skin.lcdPanelRegion)))
        XCTAssertEqual(view.router.placement?.sourceRegion.x, 30)
    }

    func testRomImportRestoreReplaceFailureCancelAndDelete() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let store = IosRomStore(root: root.appendingPathComponent("library"))
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        let source = root.appendingPathComponent("synthetic.bin")
        var bytes = Data(repeating: 0x4d, count: 0x8000)
        bytes[1] = 0x79; bytes[2] = 0; bytes[3] = 0
        try bytes.write(to: source)
        let frames = ManualFrames()
        let host = PlayerHost(player: nil, frames: frames, romStore: store)
        host.setActive(true)
        host.importRom(source)
        waitUntil { !host.isBusy && host.hasSession }
        XCTAssertTrue(host.hasSavedRom)
        XCTAssertEqual(host.romSourceName, "synthetic.bin")
        XCTAssertTrue(host.pendingAutomaticStart)
        frames.send(0); frames.send(10_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        let saved = try Data(contentsOf: store.recordURL)
        try FileManager.default.removeItem(at: source) // Restore must not depend on the provider URL.
        let restoredFrames = ManualFrames()
        let restored = PlayerHost(player: nil, frames: restoredFrames, romStore: store)
        restored.restoreRom()
        waitUntil { !restored.isBusy && restored.hasSession }
        XCTAssertTrue(restored.pendingAutomaticStart)
        restored.setActive(true)
        restoredFrames.send(0); restoredFrames.send(10_000_000)
        XCTAssertEqual(restored.status.runState, "RUNNING")

        try bytes.write(to: source)
        host.importRom(source)
        waitUntil { !host.isBusy && host.awaitingReplacement }
        host.cancelRomChange()
        XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertFalse(host.pendingAutomaticStart)
        let bad = root.appendingPathComponent("invalid.pgrom")
        try Data([1, 2, 3]).write(to: bad)
        host.importRom(bad)
        waitUntil { !host.isBusy }
        XCTAssertTrue(host.hasSession)
        XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        host.importRom(source)
        waitUntil { !host.isBusy && host.awaitingReplacement }
        host.setActive(false)
        host.confirmRomInstall()
        waitUntil { !host.isBusy && !host.awaitingReplacement }
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertTrue(host.pendingAutomaticStart)
        host.setActive(true); frames.send(20_000_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.openRomPanel()
        host.requestRomDeletion()
        host.cancelRomChange()
        XCTAssertTrue(host.hasSavedRom)
        host.requestRomDeletion()
        host.confirmRomDeletion()
        waitUntil { !host.isBusy && !host.hasSession }
        XCTAssertFalse(host.hasSavedRom)
        XCTAssertNil(host.lcd)
    }

    func testStoredPackageCrcHashMachineAndAtomicWriteFailure() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let store = IosRomStore(root: root)
        var legacy = Data(repeating: 0x4d, count: 0x8000)
        legacy[1] = 0x79; legacy[2] = 0; legacy[3] = 0
        let prepared = try IosRomCodec.prepare(IosRomCodec.read(bytes: legacy, name: "synthetic.bin"))
        let files = try RomArchive.read(prepared.archive)
        XCTAssertEqual(files["rom/internal.bin"]?.count, 8192)
        XCTAssertEqual(files["rom/external.bin"]?.count, 16384)
        XCTAssertNotNil(try IosRomCodec.prepare(IosRomCodec.read(bytes: prepared.archive, name: "synthetic.pgrom")))
        try store.install(archive: prepared.archive, sourceName: prepared.sourceName)
        let before = try Data(contentsOf: store.recordURL)
        let failing = IosRomStore(root: root, write: { _, _ in throw RomFailure(message: "Injected write failure") })
        XCTAssertThrowsError(try failing.install(archive: Data([0]), sourceName: "new"))
        XCTAssertEqual(try Data(contentsOf: store.recordURL), before)
        var corrupted = prepared.archive
        corrupted[50] ^= 1
        XCTAssertThrowsError(try RomArchive.read(corrupted))
        var tampered = files
        tampered["rom/internal.bin"]?[0] ^= 1
        let altered = RomArchive.write(tampered)
        XCTAssertThrowsError(try IosRomCodec.prepare(IosRomCodec.read(bytes: altered, name: "bad-hash.pgrom")))
        XCTAssertThrowsError(try RomArchive.read(RomArchive.write(["../rom.bin": Data([0]) ])))
        var unknown = try JSONSerialization.jsonObject(with: files["manifest.json"]!) as! [String: Any]
        unknown["machineId"] = "pc-1401"
        tampered = files; tampered["manifest.json"] = try JSONSerialization.data(withJSONObject: unknown)
        XCTAssertThrowsError(try IosRomCodec.read(bytes: RomArchive.write(tampered), name: "other.pgrom"))
    }

    func testDelayedReadCancelIgnoresStaleSuccessWhileNextReadFails() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let store = IosRomStore(root: root)
        let input = try IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "original.bin")
        let prepared = try IosRomCodec.prepare(input)
        try store.install(archive: prepared.archive, sourceName: prepared.sourceName)
        let saved = try Data(contentsOf: store.recordURL)
        let firstStarted = expectation(description: "first read on I/O queue")
        let secondStarted = expectation(description: "second read on I/O queue")
        let firstGate = DispatchSemaphore(value: 0), secondGate = DispatchSemaphore(value: 0)
        defer { firstGate.signal(); secondGate.signal() }
        let frames = ManualFrames()
        let host = PlayerHost(player: prepared.player, frames: frames, romStore: store, readRom: { url in
            XCTAssertFalse(Thread.isMainThread)
            if url.lastPathComponent == "first.pgrom" {
                firstStarted.fulfill()
                guard firstGate.wait(timeout: .now() + 10) == .success else { throw RomFailure(message: "test timeout") }
                return try IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "stale.bin")
            }
            secondStarted.fulfill()
            guard secondGate.wait(timeout: .now() + 10) == .success else { throw RomFailure(message: "test timeout") }
            throw RomFailure(message: "Injected acquisition failure")
        })
        host.setActive(true); host.run(); frames.send(0); frames.send(10_000_000)
        host.setProgramMode(true)
        host.pressBreak(owner: "touch")
        let before = host.status
        host.importRom(root.appendingPathComponent("first.pgrom"))
        wait(for: [firstStarted], timeout: 5)
        XCTAssertTrue(host.isBusy)
        XCTAssertTrue(host.canCancelRomRead)
        XCTAssertFalse(host.status.breakHeld)
        // MainActor remains responsive while read is blocked; lifecycle does not resume CPU.
        host.setActive(false); host.setActive(true)
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.cancelRomRead()
        XCTAssertFalse(host.isBusy)
        host.importRom(root.appendingPathComponent("second.pgrom"))
        firstGate.signal()
        wait(for: [secondStarted], timeout: 5)
        let delivered = expectation(description: "stale callback delivered before new result")
        DispatchQueue.main.async { delivered.fulfill() }
        wait(for: [delivered], timeout: 5)
        XCTAssertTrue(host.isBusy)
        XCTAssertFalse(host.awaitingReplacement)
        XCTAssertTrue(host.canCancelRomRead)
        secondGate.signal()
        waitUntil { !host.isBusy }
        XCTAssertTrue(host.romMessage.contains("Injected acquisition failure"))
        XCTAssertFalse(host.canCancelRomRead)
        XCTAssertTrue(host.hasSession)
        XCTAssertEqual(host.status.executedCycles, before.executedCycles)
        XCTAssertEqual(host.status.operatingMode, before.operatingMode)
        XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        XCTAssertFalse(host.pendingAutomaticStart)
    }

    func testPickerCancelAndMissingSourcePreserveSessionAndLibrary() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let store = IosRomStore(root: root)
        let prepared = try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "original.bin"))
        try store.install(archive: prepared.archive, sourceName: prepared.sourceName)
        let saved = try Data(contentsOf: store.recordURL)
        let frames = ManualFrames()
        let host = PlayerHost(player: prepared.player, frames: frames, romStore: store)
        host.setActive(true); host.run(); frames.send(0); frames.send(10_000_000)
        let cycles = host.status.executedCycles
        host.openRomPicker()
        XCTAssertTrue(host.pickerPresented)
        host.cancelRomPicker()
        XCTAssertFalse(host.pickerPresented)
        host.setActive(false); host.setActive(true)
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.importRom(root.appendingPathComponent("missing.pgrom"))
        waitUntil { !host.isBusy }
        XCTAssertTrue(host.romMessage.contains("取込みに失敗"))
        XCTAssertTrue(host.hasSession)
        XCTAssertEqual(host.status.executedCycles, cycles)
        XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        XCTAssertFalse(host.pendingAutomaticStart)
    }

    func testHomeEntryIntentAndBackgroundRemainDistinct() {
        let frames = ManualFrames()
        let host = PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: frames, startsAtHome: true)
        host.setActive(true)
        XCTAssertNil(frames.callback)
        host.enterPlayer(); frames.send(0); frames.send(10_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        let before = host.status.executedCycles
        host.setProgramMode(true)
        host.returnHome()
        XCTAssertTrue(host.showHome)
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertFalse(host.press(keyName: "SHIFT", owner: "home"))
        host.enterPlayer(); frames.send(20_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        XCTAssertEqual(host.status.executedCycles, before)
        XCTAssertEqual(host.status.operatingMode, "PRO")
        host.pause(); host.returnHome(); host.enterPlayer()
        XCTAssertNil(frames.callback)
        host.run(); frames.send(30_000_000)
        host.enterBackground(); host.setActive(true)
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertNil(frames.callback)
    }

    private func programHost(_ root: URL, frames: ManualFrames) throws -> PlayerHost {
        let player = ApplePlayerFactory.shared.structuralProbe()
        let parsed = AppleProgramCodec.shared.parse(bytes: Data("C6E1 00 C0 FF BF".utf8).kotlinBytes, basic: false)
        XCTAssertNil(player.loadProgram(input: try XCTUnwrap(parsed.input)))
        return PlayerHost(player: player, frames: frames, romStore: IosRomStore(root: root))
    }

    func testProgramRoundTripFailuresAndSaveResumeIntent() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: root) }
        let frames = ManualFrames(), host = try programHost(root, frames: frames)
        host.setActive(true)
        let bas = root.appendingPathComponent("test.BAS")
        try Data("10 PRINT 1\n20 END\n".utf8).write(to: bas)
        host.openProgram(save: false); host.importProgram(bas)
        waitUntil { !host.programBusy }
        XCTAssertNil(host.programIssue)
        XCTAssertTrue(host.pendingAutomaticStart)
        frames.send(0)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.closeProgram()
        host.openProgram(save: true)
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.prepareProgramExport(basic: true, start: "", end: "")
        waitUntil { !host.programBusy }
        let output = try XCTUnwrap(host.exportURL)
        XCTAssertEqual(try String(contentsOf: output, encoding: .utf8), "10 PRINT 1\n20 END\n")
        // A local copied export is independent of the current Session and temporary source.
        let saved = root.appendingPathComponent("saved.bas")
        try FileManager.default.copyItem(at: output, to: saved)
        host.finishProgramExport(saved: true)
        XCTAssertFalse(FileManager.default.fileExists(atPath: output.path))
        frames.send(10_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.closeProgram()
        host.pause(); host.openProgram(save: true); host.closeProgram()
        XCTAssertFalse(host.pendingAutomaticStart)
        XCTAssertNil(frames.callback)
        host.openProgram(save: false); host.importProgram(saved)
        waitUntil { !host.programBusy }
        XCTAssertNil(host.programIssue)
        host.closeProgram(); frames.send(20_000_000)
        host.openProgram(save: true); host.prepareProgramExport(basic: false, start: "C500", end: "C501")
        waitUntil { !host.programBusy }
        let before = try Data(contentsOf: XCTUnwrap(host.exportURL))
        host.finishProgramExport(saved: false); host.closeProgram()
        host.openProgram(save: false)
        let invalid = root.appendingPathComponent("invalid.dmp")
        try Data("C500 FF\n0000 FF".utf8).write(to: invalid)
        host.importProgram(invalid); waitUntil { !host.programBusy }
        guard case .shared? = host.programIssue else { return XCTFail("typed shared error") }
        XCTAssertFalse(host.pendingAutomaticStart)
        host.closeProgram(); host.openProgram(save: true)
        host.prepareProgramExport(basic: false, start: "C500", end: "C501")
        waitUntil { !host.programBusy }
        XCTAssertEqual(try Data(contentsOf: XCTUnwrap(host.exportURL)), before)
        host.finishProgramExport(saved: false); host.closeProgram()
        host.openProgram(save: false); host.cancelProgramPicker(); host.closeProgram()
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertFalse(host.pendingAutomaticStart)
    }

    func testSaveCancelFailureAndBackgroundResumeRules() throws {
        let frames = ManualFrames(), root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let host = try programHost(root, frames: frames)
        host.setActive(true); host.run(); frames.send(0)
        host.openProgram(save: true); host.closeProgram(); frames.send(10_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.openProgram(save: true); host.prepareProgramExport(basic: false, start: "oops", end: "FFFF")
        guard case .file(.invalidAddress)? = host.programIssue else { return XCTFail("address error") }
        host.closeProgram(); frames.send(20_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.openProgram(save: true); host.enterBackground(); host.setActive(true); host.closeProgram()
        XCTAssertFalse(host.pendingAutomaticStart)
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.openProgram(save: true); host.openRomPanel(); host.closeProgram()
        XCTAssertFalse(host.pendingAutomaticStart)
    }

    func testProgramReadCancellationRejectsLateResultAndBoundaries() throws {
        let frames = ManualFrames(), root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let started = expectation(description: "read started"), finished = expectation(description: "read completed")
        let gate = DispatchSemaphore(value: 0)
        defer { gate.signal() }
        let host = PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: frames,
                              romStore: IosRomStore(root: root), readProgram: { _ in
            started.fulfill()
            _ = gate.wait(timeout: .now() + 10)
            DispatchQueue.main.async { finished.fulfill() }
            return ReadProgram(name: "late.dmp", bytes: Data("C500 FF".utf8), basic: false)
        })
        host.setActive(true); host.openProgram(save: false); host.importProgram(root.appendingPathComponent("late.dmp"))
        wait(for: [started], timeout: 5)
        host.closeProgram(); host.openRomPanel()
        gate.signal(); wait(for: [finished], timeout: 5)
        let flushed = expectation(description: "queue delivered")
        DispatchQueue.main.async { flushed.fulfill() }; wait(for: [flushed], timeout: 5)
        XCTAssertFalse(host.pendingAutomaticStart)
        XCTAssertFalse(host.programBusy)
        XCTAssertFalse(host.programPanelOpen)
        XCTAssertEqual(host.status.runState, "PAUSED")
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        let large = root.appendingPathComponent("large.dmp")
        try Data(repeating: 0, count: IosProgramFiles.maximumBytes + 1).write(to: large)
        XCTAssertThrowsError(try IosProgramFiles.read(large)) { XCTAssertTrue($0 is ProgramFileError) }
        XCTAssertThrowsError(try IosProgramFiles.read(root.appendingPathComponent("other.bin")))
        XCTAssertEqual(IosProgramFiles.address("0xC500"), 0xc500)
        XCTAssertNil(IosProgramFiles.address("10000"))
    }

    func testProgramOutputWriteFailurePreservesDumpAndResumesOnlyOnClose() throws {
        let frames = ManualFrames()
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let player = ApplePlayerFactory.shared.structuralProbe()
        let initial = AppleProgramCodec.shared.parse(bytes: Data("C500 01 02".utf8).kotlinBytes, basic: false)
        XCTAssertNil(player.loadProgram(input: try XCTUnwrap(initial.input)))
        let before = Data(kotlin: try XCTUnwrap(player.exportDump(start: 0xc500, end: 0xc501).doCopyBytes()))
        let host = PlayerHost(player: player, frames: frames, romStore: IosRomStore(root: root), writeProgram: { _, _ in
            throw RomFailure(message: "Injected export failure")
        })
        host.setActive(true); host.run(); frames.send(0)
        host.openProgram(save: true)
        host.prepareProgramExport(basic: false, start: "C500", end: "C501")
        waitUntil { !host.programBusy }
        guard case .io(let message)? = host.programIssue else { return XCTFail("typed I/O failure") }
        XCTAssertTrue(message.contains("Injected export failure"))
        XCTAssertFalse(host.programExporterPresented)
        XCTAssertNil(host.exportURL)
        XCTAssertEqual(Data(kotlin: try XCTUnwrap(player.exportDump(start: 0xc500, end: 0xc501).doCopyBytes())), before)
        host.closeProgram(); frames.send(10_000_000)
        XCTAssertEqual(host.status.runState, "RUNNING")
    }

    private func waitUntil(_ condition: @escaping () -> Bool) {
        expectation(for: NSPredicate { _, _ in condition() }, evaluatedWith: nil)
        waitForExpectations(timeout: 10)
    }

    func testDeflateArchiveWithStreamingDataDescriptor() throws {
        // Generic original text fixture generated with Python zipfile on a non-seekable stream.
        let fixture = Data(base64Encoded: "UEsDBBQACAAIAGeGTF0AAAAAAAAAAAAAAAAMAAAAcm9tL3Rlc3QuYmluK8gvKklMyklVSCxKzsgsS1UoSS0uAQBQSwcIArCM6hcAAAAVAAAAUEsBAhQDFAAIAAgAZ4ZMXQKwjOoXAAAAFQAAAAwAAAAAAAAAAAAAAIABAAAAAHJvbS90ZXN0LmJpblBLBQYAAAAAAQABADoAAABRAAAAAAA=")!
        XCTAssertEqual(try RomArchive.read(fixture)["rom/test.bin"], Data("portable archive test".utf8))
        XCTAssertThrowsError(try RomArchive.read(Data(fixture.dropLast())))
    }
}

private final class AudioWork {
    var jobs: [() -> Void] = []
    func drain() { while !jobs.isEmpty { jobs.removeFirst()() } }
}
private final class RecordingSink: PlayerPCMSink {
    var packets: [PlayerPCM] = []
    var completions: [() -> Void] = []
    var closed = false
    func schedule(_ packet: PlayerPCM, completion: @escaping () -> Void) throws {
        packets.append(packet); completions.append(completion)
    }
    func close() { closed = true }
}

final class PlayerAudioTests: XCTestCase {
    private let tone = PlayerPCM(samples: Array(repeating: 8192, count: 220), frameCount: 220, rate: 22050)

    func testSilenceBetweenTonesAndNaturalDrain() {
        let work = AudioWork(); let sink = RecordingSink()
        var opens = 0
        let pump = PlayerAudioPump(enqueue: { work.jobs.append($0) }, open: { _ in opens += 1; return sink })
        let silence = PlayerPCM(samples: [], frameCount: 220, rate: 22050)
        pump.offer(silence); work.drain()
        XCTAssertEqual(opens, 0); XCTAssertEqual(pump.pendingFrames, 0)
        pump.offer(tone); pump.offer(silence); pump.offer(tone); work.drain()
        XCTAssertEqual(sink.packets.map(\.frameCount), [220, 220, 220])
        XCTAssertTrue(sink.packets[1].samples.isEmpty)
        XCTAssertEqual(pump.pendingFrames, 660)
        sink.completions[0](); work.drain(); XCTAssertFalse(sink.closed)
        sink.completions[1](); work.drain(); XCTAssertFalse(sink.closed)
        sink.completions[2](); work.drain(); XCTAssertTrue(sink.closed)
        XCTAssertEqual(pump.pendingFrames, 0)
    }

    func testContinuousSilenceClosesAfterLastTonePlayed() {
        let work = AudioWork(); let sink = RecordingSink()
        let pump = PlayerAudioPump(enqueue: { work.jobs.append($0) }, open: { _ in sink })
        pump.offer(tone); pump.offer(PlayerPCM(samples: [], frameCount: 220, rate: 22050)); work.drain()
        sink.completions[0](); work.drain()
        XCTAssertTrue(sink.closed) // Does not keep a device/session alive with zeros forever.
        XCTAssertEqual(pump.pendingFrames, 0)
        pump.offer(tone); work.drain()
        sink.completions[1](); work.drain() // Cancelled zeros cannot consume the new stream.
        XCTAssertEqual(pump.pendingFrames, 220)
        pump.stop(); work.drain()
    }

    func testOverflowStopStaleCompletionAndRateChangeDiscardOldAudio() {
        let work = AudioWork(); var sinks: [RecordingSink] = []
        var rates: [Int] = []
        let pump = PlayerAudioPump(enqueue: { work.jobs.append($0) }, open: { rate in
            rates.append(rate); let sink = RecordingSink(); sinks.append(sink); return sink
        })
        pump.offer(tone); work.drain()
        let stale = sinks[0].completions[0]
        for _ in 0..<12 { pump.offer(tone) }
        XCTAssertLessThanOrEqual(pump.pendingFrames, 2205)
        work.drain(); XCTAssertTrue(sinks[0].closed)
        stale(); work.drain(); XCTAssertGreaterThan(pump.pendingFrames, 0)
        pump.stop(); XCTAssertEqual(pump.pendingFrames, 0); work.drain()
        XCTAssertTrue(sinks.last!.closed)
        pump.offer(PlayerPCM(samples: [1, -1], frameCount: 2, rate: 26460)); work.drain()
        XCTAssertEqual(rates.last, 26460)
        XCTAssertEqual(pump.pendingFrames, 2)
        pump.stop(); work.drain()
        for sink in sinks { for complete in sink.completions { complete() } }
        work.drain(); XCTAssertEqual(pump.pendingFrames, 0)
    }

    func testWorkerBacklogIsBoundedEvenBeforeWorkerStarts() {
        let work = AudioWork(); let sink = RecordingSink()
        let pump = PlayerAudioPump(enqueue: { work.jobs.append($0) }, open: { _ in sink })
        for _ in 0..<10_000 { pump.offer(tone) }
        XCTAssertEqual(work.jobs.count, 1)
        XCTAssertLessThanOrEqual(pump.pendingFrames, 2205)
        pump.stop()
        work.drain()
        XCTAssertTrue(sink.packets.isEmpty)
        XCTAssertEqual(pump.pendingFrames, 0)
    }

    func testOutputFailureLatchesUntilExplicitRetry() {
        let work = AudioWork(); var attempts = 0; var failures = 0
        let pump = PlayerAudioPump(enqueue: { work.jobs.append($0) }, open: { _ in
            attempts += 1; throw CocoaError(.fileReadUnknown)
        }, failure: { failures += 1 })
        pump.offer(tone); work.drain()
        pump.offer(tone); work.drain()
        XCTAssertEqual(attempts, 1); XCTAssertEqual(failures, 1)
        pump.stop(retry: true); pump.offer(tone); work.drain()
        XCTAssertEqual(attempts, 2); XCTAssertEqual(pump.pendingFrames, 0)
    }

    func testFloatPCMAndActualOfflineEnginePathAtAllRates() throws {
        for rate in [17640, 19845, 22050, 24255, 26460] {
            let samples: [Int16] = (0..<2205).map { $0 % 32 < 16 ? 8192 : -8192 }
            let packet = PlayerPCM(samples: samples, frameCount: samples.count, rate: rate)
            let engine = try EnginePCMSink(rate: rate, live: false)
            let rendered = try engine.renderOffline(packet)
            engine.close()
            XCTAssertEqual(rendered.count, samples.count)
            XCTAssertTrue(rendered.contains { $0 > 0.1 }); XCTAssertTrue(rendered.contains { $0 < -0.1 })
            let silence = EnginePCMSink.buffer(PlayerPCM(samples: [], frameCount: 220, rate: rate))!
            XCTAssertEqual(silence.frameLength, 220)
            XCTAssertTrue((0..<220).allSatisfy { silence.floatChannelData![0][$0] == 0 })
        }
    }

    func testPlaybackPreferencesUseIsolatedDomainAndUnknownSpeedFallback() {
        let name = "pgp-playback-test-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        let preferences = PlayerPlaybackPreferences(defaults: defaults)
        XCTAssertEqual(preferences.speed, "1.0"); XCTAssertTrue(preferences.sound)
        preferences.speed = "1.2"; preferences.sound = false
        XCTAssertEqual(PlayerPlaybackPreferences(defaults: defaults).speed, "1.2")
        XCTAssertFalse(PlayerPlaybackPreferences(defaults: defaults).sound)
        defaults.set("unexpected", forKey: "player.speed")
        XCTAssertEqual(preferences.speed, "1.0")
    }
}

@MainActor
private final class RecordingAudioOutput: PlayerAudioOutput {
    var onStatus: ((String) -> Void)?
    var starts = 0
    var stops = 0
    var packets: [PlayerPCM] = []
    func start() { starts += 1 }
    func stop() { stops += 1 }
    func offer(_ packet: PlayerPCM) { packets.append(packet) }
}

@MainActor
final class PlayerPlaybackHostTests: XCTestCase {
    func testMuteDrainsEachTickSpeedChangesDoNotResumeAndStopsDiscard() {
        let frames = ManualFrames(); let audio = RecordingAudioOutput()
        var time: Int64 = 0
        let host = PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: frames,
                              audio: audio, monotonicNow: { time })
        host.setExecutionSpeed("1.2")
        XCTAssertEqual(host.status.runState, "PAUSED")
        XCTAssertEqual(audio.starts, 0)
        host.setActive(true); host.run(); frames.send(0); frames.send(10_000_000)
        XCTAssertEqual(audio.packets.last?.rate, 26460)
        XCTAssertTrue(audio.packets.last!.samples.isEmpty)
        audio.onStatus?("synthetic output failure")
        XCTAssertEqual(host.status.runState, "RUNNING")
        XCTAssertEqual(host.audioMessage, "synthetic output failure")
        host.setSoundEnabled(false)
        let count = audio.packets.count
        frames.send(20_000_000); frames.send(30_000_000)
        XCTAssertEqual(audio.packets.count, count)
        host.setSoundEnabled(true); frames.send(40_000_000)
        XCTAssertLessThan(audio.packets.last!.frameCount, 270) // No accumulated muted audio.
        time = 40_000_000
        host.setExecutionSpeed("0.8"); frames.send(50_000_000)
        XCTAssertEqual(audio.packets.last?.rate, 17640)
        host.pause(); let stops = audio.stops
        host.setExecutionSpeed("0.9")
        XCTAssertEqual(host.status.runState, "PAUSED")
        host.reset(); XCTAssertGreaterThan(audio.stops, stops)
        frames.send(60_000_000); frames.send(70_000_000)
        XCTAssertEqual(audio.packets.last?.rate, 19845)
        host.returnHome(); XCTAssertEqual(host.status.runState, "PAUSED")
        let homeStops = audio.stops
        host.enterPlayer(); frames.send(80_000_000)
        host.enterBackground()
        XCTAssertGreaterThan(audio.stops, homeStops)
        host.setActive(true)
        XCTAssertEqual(host.status.runState, "PAUSED")
    }

    func testInterruptResumeRouteRemovalAndPauseNeverReplayOldPCM() {
        let work = AudioWork(); var sinks: [RecordingSink] = []
        let pump = PlayerAudioPump(enqueue: { work.jobs.append($0) }, open: { _ in
            let sink = RecordingSink(); sinks.append(sink); return sink
        })
        let audio = IosPlayerAudio(pump: pump, observeNotifications: false)
        let packet = PlayerPCM(samples: [100, -100], frameCount: 2, rate: 22050)
        func interruption(_ type: AVAudioSession.InterruptionType, resume: Bool = false) {
            audio.handle(Notification(name: AVAudioSession.interruptionNotification, userInfo: [
                AVAudioSessionInterruptionTypeKey: NSNumber(value: type.rawValue),
                AVAudioSessionInterruptionOptionKey: NSNumber(value: resume ? AVAudioSession.InterruptionOptions.shouldResume.rawValue : 0)]))
        }
        audio.start(); audio.offer(packet); work.drain()
        interruption(.began); audio.offer(packet); work.drain()
        XCTAssertTrue(sinks[0].closed); XCTAssertEqual(pump.pendingFrames, 0)
        interruption(.ended, resume: true); work.drain()
        XCTAssertEqual(sinks.count, 1) // Resume does not replay.
        audio.offer(packet); work.drain(); XCTAssertEqual(sinks.count, 2)
        audio.handle(Notification(name: AVAudioSession.routeChangeNotification, userInfo: [
            AVAudioSessionRouteChangeReasonKey: NSNumber(value: AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue)]))
        audio.offer(packet); work.drain(); XCTAssertEqual(pump.pendingFrames, 0)
        audio.start(); audio.offer(packet); work.drain(); XCTAssertEqual(sinks.count, 3)
        interruption(.began); audio.stop(); interruption(.ended, resume: true)
        audio.offer(packet); work.drain(); XCTAssertEqual(sinks.count, 3)
        audio.start(); interruption(.began); interruption(.ended)
        audio.offer(packet); work.drain(); XCTAssertEqual(sinks.count, 3)
        audio.start(); audio.offer(packet); work.drain(); XCTAssertEqual(sinks.count, 4)
        audio.stop(); work.drain()
    }

    func testLiveSimulatorAudioNodeConsumesSyntheticPCM() throws {
        for rate in [17640, 19845, 22050, 24255, 26460] {
            let completed = expectation(description: "device output consumed PCM at \(rate) Hz")
            let frameCount = rate / 20
            let angularStep: Double = 2.0 * Double.pi * 440.0 / Double(rate)
            var samples: [Int16] = []
            samples.reserveCapacity(frameCount)
            for frame in 0..<frameCount {
                let phase: Double = Double(frame) * angularStep
                let amplitude: Double = sin(phase) * 4096.0
                samples.append(Int16(amplitude))
            }
            let sink = try EnginePCMSink(rate: rate)
            XCTAssertEqual(AVAudioSession.sharedInstance().category, .ambient)
            try sink.schedule(PlayerPCM(samples: samples, frameCount: samples.count, rate: rate)) { completed.fulfill() }
            wait(for: [completed], timeout: 5)
            sink.close()
        }
    }
}

@MainActor
final class PlayerExternalInputTests: XCTestCase {
    private func running(_ input: IosPlayerInput) -> (PlayerHost, ManualFrames) {
        let frames = ManualFrames()
        let host = PlayerHost(player: ApplePlayerFactory.shared.structuralProbe(), frames: frames, input: input)
        host.setActive(true); host.run(); frames.send(0)
        return (host, frames)
    }
    func testPhysicalShiftChordsRepeatCancellationAndFocusReturn() {
        let input = IosPlayerInput()
        let (host, frames) = running(input)
        input.keyEvent(code: 225, down: true)
        input.keyEvent(code: 4, down: true)
        input.keyEvent(code: 4, down: true)
        XCTAssertEqual(host.pressedKeys, ["SHIFT", "A"])
        input.keyEvent(code: 4, down: false)
        XCTAssertTrue(host.pressedKeys.contains("A")) // Minimum emulated hold, not wall time.
        frames.send(100_000_000)
        XCTAssertEqual(host.pressedKeys, ["SHIFT"])
        host.setInputFocus(false); XCTAssertTrue(host.pressedKeys.isEmpty)
        host.setInputFocus(true)
        input.keyEvent(code: 225, down: true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 225, down: false); input.keyEvent(code: 225, down: true)
        XCTAssertEqual(host.pressedKeys, ["SHIFT"])
        input.keyboardDisconnected(); XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 227, down: true); input.keyEvent(code: 4, down: true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 227, down: false)
        input.keyEvent(code: 4, down: true); XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 4, down: false); input.keyEvent(code: 4, down: true)
        XCTAssertEqual(host.pressedKeys, ["A"])
        host.enterBackground(); host.setActive(true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        XCTAssertFalse(input.receiving)
    }

    func testGameControllerSnapshotSharedOwnershipMenuAndControllerDisplay() {
        let input = IosPlayerInput()
        let pad = GCController.withExtendedGamepad()
        input.attachControllers([pad]); input.bind("SOUTH", to: "A"); input.setEnabled(true)
        let (host, _) = running(input)
        pad.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(pad)
        XCTAssertEqual(host.pressedKeys, ["A"])
        host.press(keyName: "A", owner: "touch:1")
        pad.extendedGamepad!.buttonA.setValue(0); input.controllerChanged(pad)
        XCTAssertEqual(host.pressedKeys, ["A"])
        host.release(owner: "touch:1"); XCTAssertTrue(host.pressedKeys.isEmpty)
        pad.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(pad)
        host.setInputMenuOpen(true); XCTAssertTrue(host.pressedKeys.isEmpty)
        host.setInputMenuOpen(false); input.controllerChanged(pad)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        pad.extendedGamepad!.buttonA.setValue(0); input.controllerChanged(pad)
        host.togglePresentation()
        pad.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(pad)
        XCTAssertEqual(host.pressedKeys, ["A"]) // Controller Display hides touch keys, not hardware input.
        pad.extendedGamepad!.buttonA.setValue(0); input.controllerChanged(pad)
        host.setInputFocus(false)
        let handler = pad.extendedGamepad!.valueChangedHandler
        pad.extendedGamepad!.valueChangedHandler = nil
        pad.extendedGamepad!.buttonA.setValue(1) // State changes while callbacks are absent/suspended.
        pad.extendedGamepad!.valueChangedHandler = handler
        host.setInputFocus(true); input.controllerChanged(pad)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        pad.extendedGamepad!.buttonA.setValue(0); input.controllerChanged(pad)
        pad.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(pad)
        XCTAssertEqual(host.pressedKeys, ["A"])
        let stale = pad.extendedGamepad!.valueChangedHandler
        input.attachControllers([]); XCTAssertTrue(host.pressedKeys.isEmpty)
        stale?(pad.extendedGamepad!, pad.extendedGamepad!.buttonA)
        XCTAssertTrue(host.pressedKeys.isEmpty)
    }

    func testSingleSelectionNoSilentHandoffUnsupportedAndAnalogIgnored() {
        let input = IosPlayerInput(); let first = GCController.withExtendedGamepad(); let second = GCController.withExtendedGamepad()
        input.attachControllers([first, second])
        XCTAssertNil(input.selectedId)
        input.select(input.devices[0].id); input.bind("SOUTH", to: "A"); input.setEnabled(true)
        let (host, _) = running(input)
        second.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(second)
        first.extendedGamepad!.leftThumbstick.setValueForXAxis(1, yAxis: 1)
        first.extendedGamepad!.leftTrigger.setValue(1); input.controllerChanged(first)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        first.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(first)
        XCTAssertEqual(host.pressedKeys, ["A"])
        input.attachControllers([second]); XCTAssertNil(input.selectedId)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.select(input.devices[0].id); XCTAssertTrue(host.pressedKeys.isEmpty) // Held at selection blocked.
        second.extendedGamepad!.buttonA.setValue(0); input.controllerChanged(second)
        second.extendedGamepad!.buttonA.setValue(1); input.controllerChanged(second)
        XCTAssertEqual(host.pressedKeys, ["A"])
        input.bind("SOUTH", to: "MODE"); XCTAssertFalse(input.available("SOUTH"))
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.attachControllers([GCController.withMicroGamepad()])
        XCTAssertTrue(input.devices[0].available.isEmpty)
    }

    func testProgramModalResetAndHomeDoNotReplayHeldKeys() {
        let input = IosPlayerInput(); let (host, frames) = running(input)
        input.keyEvent(code: 4, down: true)
        host.openProgram(save: false)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 5, down: true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        host.closeProgram(); host.run(); frames.send(1_000_000_000)
        input.keyEvent(code: 5, down: true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 5, down: false); input.keyEvent(code: 5, down: true)
        XCTAssertEqual(host.pressedKeys, ["B"])
        host.reset(); frames.send(2_000_000_000)
        input.keyEvent(code: 5, down: true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        host.setInputMenuOpen(true); host.returnHome()
        XCTAssertFalse(host.inputMenuOpen)
        host.enterPlayer(); frames.send(3_000_000_000)
        input.keyEvent(code: 5, down: true)
        XCTAssertTrue(host.pressedKeys.isEmpty)
        input.keyEvent(code: 5, down: false); input.keyEvent(code: 5, down: true)
        XCTAssertEqual(host.pressedKeys, ["B"])
    }

    func testAllTenSnapshotButtonsAndKeyboardHidConstants() {
        XCTAssertEqual(GCKeyCode.keyA.rawValue, 4)
        XCTAssertEqual(GCKeyCode.leftShift.rawValue, 225)
        let input = IosPlayerInput(); let controller = GCController.withExtendedGamepad()
        input.attachControllers([controller]); input.setEnabled(true)
        for button in input.buttons { input.bind(button, to: "A") }
        let (host, _) = running(input)
        let pad = controller.extendedGamepad!
        XCTAssertNotNil(pad.valueChangedHandler)
        // Snapshot d-pad directions are derived from its axes; set them through the d-pad API.
        for (x, y) in [(Float(0), Float(1)), (0, -1), (-1, 0), (1, 0)] {
            pad.dpad.setValueForXAxis(x, yAxis: y); pad.valueChangedHandler?(pad, pad.dpad)
            XCTAssertEqual(host.pressedKeys, ["A"])
            pad.dpad.setValueForXAxis(0, yAxis: 0); pad.valueChangedHandler?(pad, pad.dpad)
            XCTAssertTrue(host.pressedKeys.isEmpty)
        }
        for button in [pad.buttonA, pad.buttonB, pad.buttonX, pad.buttonY, pad.leftShoulder, pad.rightShoulder] {
            button.setValue(1); pad.valueChangedHandler?(pad, button)
            XCTAssertEqual(host.pressedKeys, ["A"])
            button.setValue(0); pad.valueChangedHandler?(pad, button)
            XCTAssertTrue(host.pressedKeys.isEmpty)
        }
    }

    func testControllerPreferencesAreIsolatedAndUnknownValuesUnassigned() {
        let name = "pgp-controller-test-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        let input = IosPlayerInput(defaults: defaults)
        XCTAssertFalse(input.enabled)
        input.bind("LEFT", to: "NUM_4"); input.setEnabled(true)
        let restored = IosPlayerInput(defaults: defaults)
        XCTAssertTrue(restored.enabled); XCTAssertEqual(restored.bindings["LEFT"], "NUM_4")
        defaults.set(["LEFT": "invalid", "RIGHT": "MODE"], forKey: "controller.bindings")
        let invalid = IosPlayerInput(defaults: defaults)
        XCTAssertEqual(invalid.bindings["LEFT"], "")
        XCTAssertEqual(invalid.bindings["RIGHT"], "MODE")
    }
}
