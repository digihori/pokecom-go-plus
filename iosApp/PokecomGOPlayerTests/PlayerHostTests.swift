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
        let restoredFrames = ManualFrames()
        let restored = PlayerHost(player: nil, frames: restoredFrames, romStore: store)
        restored.restoreRom()
        waitUntil { !restored.isBusy && restored.hasSession }
        XCTAssertTrue(restored.pendingAutomaticStart)
        restored.setActive(true)
        restoredFrames.send(0); restoredFrames.send(10_000_000)
        XCTAssertEqual(restored.status.runState, "RUNNING")

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
