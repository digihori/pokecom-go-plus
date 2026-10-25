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
    func testHomeLayoutDefaultsPersistsAndKeepsModelSelectionIndependent() {
        let domain = "pgp-home-test-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: domain)!
        defer { defaults.removePersistentDomain(forName: domain) }
        let preferences = IosMachinePreferences(defaults: defaults)
        XCTAssertEqual(preferences.homeLayout, .tiles)
        let host = PlayerHost(player: nil, frames: ManualFrames(), machinePreferences: preferences)
        host.setHomeLayout(.list)
        XCTAssertEqual(host.homeLayout, .list)
        XCTAssertEqual(IosMachinePreferences(defaults: defaults).homeLayout, .list)
        preferences.select("pc-1250")
        XCTAssertEqual(preferences.model(for: "pc-1251"), "pc-1250")
        XCTAssertEqual(preferences.homeLayout, .list)
        defaults.set("unknown", forKey: "home.layout")
        XCTAssertEqual(preferences.homeLayout, .tiles)
        XCTAssertEqual(IosMachinePreferences().homeLayout, .tiles)
        XCTAssertEqual(AppleMachineCatalog.shared.homeMachines.map(\.id),
            ["pc-1245", "pc-1251", "pc-1261", "pc-1350", "pc-1360", "pc-1360k", "pc-1401", "pc-1450", "pc-1460", "pc-1470u"])
    }

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

    func testPc1360kConvertKeyAtRightAndBottomEdges() throws {
        try assertCornerKey(machineId: "pc-1360k", keyName: "KANJI_CONVERT", neighbors: ["MINUS", "KANJI_FIX"])
    }

    func testPc1470uEqualsKeyAtRightAndBottomEdges() throws {
        try assertCornerKey(machineId: "pc-1470u", keyName: "EQUALS", neighbors: ["PLUS", "INSERT"])
    }

    private func assertCornerKey(machineId: String, keyName: String, neighbors: [String]) throws {
        let player = try IosRomCodec.prepare(inputFromArchive(PlayerHost.bankedProbeArchive(machineId)), machineId: machineId).player
        let skin = player.skin
        let convert = try XCTUnwrap(skin.keyRegions.first { $0.key.name == keyName })
        for size in [CGSize(width: 667, height: 270), CGSize(width: 844, height: 290), CGSize(width: 844, height: 390)] {
            let view = PlayerSkinView(skin: skin, machine: player.machine)
            view.frame = CGRect(origin: .zero, size: size)
            var presses: [String] = []
            var releases = 0
            view.router.onPress = { key, _ in presses.append(key); return true }
            view.router.onRelease = { _ in releases += 1 }
            view.layoutIfNeeded()
            let placement = try XCTUnwrap(view.router.placement)
            XCTAssertGreaterThanOrEqual(placement.viewport.cgRect.minX, 11.9)
            XCTAssertGreaterThanOrEqual(view.bounds.maxX - placement.viewport.cgRect.maxX, 11.9)
            let rect = view.screenRect(convert.bounds)
            let points = [CGPoint(x: rect.midX, y: rect.midY),
                          CGPoint(x: rect.maxX + 5, y: rect.midY),
                          CGPoint(x: rect.midX, y: rect.maxY + 5),
                          CGPoint(x: rect.maxX + 3, y: rect.maxY + 3)]
            for point in points {
                XCTAssertTrue(view.bounds.contains(point))
                let before = presses.count
                view.router.begin(owner: "convert", point: point)
                XCTAssertEqual(presses.count, before + 1)
                XCTAssertEqual(presses.last, keyName)
                view.router.move(owner: "convert", point: CGPoint(x: rect.maxX + 7, y: rect.midY))
                XCTAssertEqual(view.router.owners["convert"], convert.id)
                view.router.end(owner: "convert")
                XCTAssertTrue(view.router.owners.isEmpty)
            }
            XCTAssertEqual(releases, points.count)
            for name in neighbors {
                let key = try XCTUnwrap(skin.keyRegions.first { $0.key.name == name })
                let r = view.screenRect(key.bounds)
                view.router.begin(owner: name, point: CGPoint(x: r.midX, y: r.midY))
                XCTAssertEqual(presses.last, name)
                view.router.end(owner: name)
            }
        }
    }

    func testBottomRowTouchToleranceExtendsPastImageButNotView() throws {
        let host = PlayerHost.structuralProbe(frames: ManualFrames())
        let skin = try XCTUnwrap(host.skin)
        let bottomNames: Set<String> = ["Z", "X", "C", "V", "B", "N", "M", "NUM_0", "DOT", "PLUS", "MINUS", "ENTER"]
        let bottomKeys = skin.keyRegions.filter { bottomNames.contains($0.key.name) }
        XCTAssertEqual(bottomKeys.count, bottomNames.count)
        for width: CGFloat in [667, 844] {
            let view = PlayerSkinView(skin: skin)
            view.frame = CGRect(x: 0, y: 0, width: width, height: 270)
            var pressed: String?
            var releases = 0
            view.router.onPress = { key, _ in pressed = key; return true }
            view.router.onRelease = { _ in releases += 1 }
            view.layoutIfNeeded()
            let placement = try XCTUnwrap(view.router.placement)
            XCTAssertGreaterThanOrEqual(view.bounds.maxY - placement.viewport.cgRect.maxY, 11.9)
            for key in bottomKeys {
                let rect = view.screenRect(key.bounds)
                let point = CGPoint(x: rect.midX, y: rect.maxY + 5)
                XCTAssertTrue(view.bounds.contains(point))
                pressed = nil
                view.router.begin(owner: key.id, point: point)
                XCTAssertEqual(pressed, key.key.name)
                let before = releases
                view.router.move(owner: key.id, point: CGPoint(x: rect.midX, y: rect.maxY + 7))
                XCTAssertEqual(releases, before)
                view.router.end(owner: key.id)
                XCTAssertEqual(releases, before + 1)
            }
            // The last row's numeric key tolerance crosses the drawn image boundary.
            let zero = try XCTUnwrap(bottomKeys.first { $0.key.name == "NUM_0" })
            XCTAssertGreaterThan(view.screenRect(zero.bounds).maxY + 5, placement.viewport.cgRect.maxY)
            pressed = nil
            view.router.begin(owner: "outside", point: CGPoint(x: view.bounds.midX, y: view.bounds.maxY + 1))
            XCTAssertNil(pressed)
        }
    }

    func testSmallKeyTouchToleranceAndMovementAtDifferentScales() throws {
        let host = PlayerHost.structuralProbe(frames: ManualFrames())
        let skin = try XCTUnwrap(host.skin)
        for width: CGFloat in [667, 1000] {
            let view = PlayerSkinView(skin: skin)
            view.frame = CGRect(x: 0, y: 0, width: width, height: 300)
            var presses: [String] = []
            var releases: [String] = []
            view.router.onPress = { key, _ in presses.append(key); return true }
            view.router.onRelease = { releases.append($0) }
            view.layoutIfNeeded()
            // Above the top row there is no neighboring key; stay inside the skin viewport.
            let key = try XCTUnwrap(skin.keyRegions.min { $0.bounds.cgRect.minY < $1.bounds.cgRect.minY })
            let rect = view.screenRect(key.bounds)
            let edge = CGPoint(x: rect.midX, y: rect.minY - 4)
            view.router.begin(owner: "edge", point: edge)
            XCTAssertEqual(presses, [key.key.name])
            view.router.move(owner: "edge", point: CGPoint(x: rect.midX, y: rect.minY - 7))
            XCTAssertTrue(releases.isEmpty)
            view.router.move(owner: "edge", point: CGPoint(x: rect.midX, y: rect.minY - 9))
            XCTAssertEqual(releases, ["edge"])
            view.router.move(owner: "edge", point: CGPoint(x: rect.midX, y: rect.midY))
            XCTAssertEqual(presses.count, 1) // Returning never synthesizes another press.
            view.router.begin(owner: "far", point: CGPoint(x: rect.midX, y: rect.minY - 9))
            XCTAssertEqual(presses.count, 1)
            for region in skin.keyRegions {
                let r = view.screenRect(region.bounds)
                view.router.begin(owner: region.id, point: CGPoint(x: r.midX, y: r.midY))
                XCTAssertEqual(presses.last, region.key.name) // Neighbor tolerance cannot steal a direct hit.
                view.router.end(owner: region.id)
            }
        }
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
        XCTAssertThrowsError(try failing.install(archive: prepared.archive, sourceName: "new"))
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
        unknown["machineId"] = "pc-9999"
        tampered = files; tampered["manifest.json"] = try JSONSerialization.data(withJSONObject: unknown)
        XCTAssertThrowsError(try IosRomCodec.read(bytes: RomArchive.write(tampered), name: "other.pgrom"))
    }

    func testMachineMetadataAndStoredIdentityValidation() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let store = IosRomStore(root: root)
        XCTAssertEqual(store.recordURL.lastPathComponent, "pc-1245.json")
        let prepared = try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "synthetic.bin"))
        XCTAssertEqual(prepared.player.machine.id, store.machineId)
        XCTAssertEqual(prepared.player.machine.operatingModes, ["RUN", "PRO"])
        try store.install(archive: prepared.archive, sourceName: "synthetic.bin")
        XCTAssertEqual(try store.load()?.manifest?.machineId, store.machineId)
        let corrupt = IosRomStore.Record(version: 1, machineId: "pc-1251", sourceName: "wrong", archive: prepared.archive)
        try JSONEncoder().encode(corrupt).write(to: store.recordURL, options: .atomic)
        XCTAssertThrowsError(try store.load())
    }

    func testSeriesSharedRomSelectionConfirmationAndIndependentRestoration() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let domain = "pgp-series-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: domain)!
        defer { try? FileManager.default.removeItem(at: root); defaults.removePersistentDomain(forName: domain) }
        let preferences = IosMachinePreferences(defaults: defaults)
        var legacy = Data(repeating: 0x4d, count: 0x8000)
        legacy[0] = 0x79; legacy[1] = 0; legacy[2] = 0
        let input = try IosRomCodec.read(bytes: legacy, name: "synthetic.bin")
        let old = try IosRomCodec.prepare(input)
        let family = try IosRomCodec.prepare(input, machineId: "pc-1255")
        let oldStore = IosRomStore(root: root)
        let shared = IosRomStore(root: root, machineId: "pc-1250")
        XCTAssertEqual(shared.recordURL.lastPathComponent, "pc-1251.json")
        XCTAssertEqual(shared.recordURL, IosRomStore(root: root, machineId: "pc-1255").recordURL)
        try oldStore.install(archive: old.archive, sourceName: "old.bin")
        try shared.install(archive: family.archive, sourceName: "family.bin")
        let saved = try Data(contentsOf: shared.recordURL)
        for id in ["pc-1250", "pc-1251", "pc-1255"] {
            let player = try IosRomCodec.prepare(XCTUnwrap(try shared.load()), machineId: id).player
            XCTAssertEqual(player.machine.id, id)
            XCTAssertFalse(player.skin.keyRegions.isEmpty)
        }
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(old.archive), machineId: "pc-1251"))
        XCTAssertThrowsError(try shared.install(archive: old.archive, sourceName: "wrong"))
        XCTAssertEqual(try Data(contentsOf: shared.recordURL), saved)
        let frames = ManualFrames()
        let host = PlayerHost(player: old.player, frames: frames, romStore: oldStore, startsAtHome: true, machinePreferences: preferences)
        host.setActive(true)
        host.requestMachineSelection("pc-1250")
        XCTAssertEqual(host.machine.id, "pc-1245")
        XCTAssertNil(host.pendingMachineId)
        host.enterPlayer()
        XCTAssertEqual(host.pendingMachineId, "pc-1250")
        host.cancelMachineSelection()
        XCTAssertEqual(host.machine.id, "pc-1245")
        XCTAssertTrue(host.showHome)
        host.enterPlayer(); host.confirmMachineSelection()
        waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1250")
        XCTAssertFalse(host.showHome)
        frames.send(0)
        XCTAssertEqual(host.status.runState, "RUNNING")
        host.returnHome()
        host.requestMachineSelection("pc-1255")
        XCTAssertEqual(host.machine.id, "pc-1250")
        XCTAssertEqual(preferences.runningMachineId, "pc-1250")
        XCTAssertEqual(preferences.model(for: "pc-1251"), "pc-1255")
        let restored = PlayerHost(player: nil, frames: ManualFrames(), romStore: oldStore, startsAtHome: true,
                                  machinePreferences: IosMachinePreferences(defaults: defaults))
        restored.restoreRom(); waitUntil { !restored.isBusy }
        XCTAssertEqual(restored.selectedMachineId, "pc-1255")
        XCTAssertEqual(restored.machine.id, "pc-1250")
        XCTAssertEqual(restored.status.runState, "PAUSED")
        // A failed switch preserves the old Session and the user's selected model.
        try Data([0]).write(to: shared.recordURL)
        host.enterPlayer(); host.confirmMachineSelection(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1250")
        XCTAssertEqual(host.selectedMachineId, "pc-1255")
        XCTAssertTrue(host.showHome)
        try saved.write(to: shared.recordURL)
        host.enterPlayer(); host.confirmMachineSelection(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1255")
        XCTAssertEqual(try Data(contentsOf: shared.recordURL), saved)
        host.returnHome(); host.requestMachineSelection("pc-1250")
        host.openRomPanel(); host.requestRomDeletion(); host.cancelRomChange()
        XCTAssertTrue(host.hasSession)
        XCTAssertEqual(try Data(contentsOf: shared.recordURL), saved)
        host.requestRomDeletion(); host.confirmRomDeletion(); waitUntil { !host.isBusy }
        XCTAssertFalse(host.hasSession)
        XCTAssertFalse(host.hasRom("pc-1251"))
        XCTAssertTrue(host.hasRom("pc-1245"))
    }

    func testRunProToggleLeavesReserveInMenuAndKeepsFixedLcdPositions() throws {
        for id in AppleMachineCatalog.shared.machines.filter { $0.id.hasPrefix("pc-12") }.map(\.id) {
            let size = id.hasPrefix("pc-126") ? 0x10000 : 0x8000
            let prepared = try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: size), name: "synthetic.bin"), machineId: id)
            let host = PlayerHost(player: prepared.player, frames: ManualFrames())
            XCTAssertTrue(host.showsRunProToggle)
            XCTAssertEqual(host.modeToggleLabel, "RUN")
            host.toggleOperatingMode(); XCTAssertEqual(host.status.operatingMode, "PRO")
            host.toggleOperatingMode(); XCTAssertEqual(host.status.operatingMode, "RUN")
            if id != "pc-1245" {
                XCTAssertTrue(host.operatingModes.contains("RSV"))
                host.setOperatingMode("RSV")
                XCTAssertEqual(host.status.operatingMode, "RSV")
                XCTAssertEqual(host.modeToggleLabel, "RUN")
                host.toggleOperatingMode(); XCTAssertEqual(host.status.operatingMode, "PRO")
            }
            let player = prepared.player
            let view = PlayerSkinView(skin: player.skin, machine: player.machine)
            view.frame = CGRect(x: 0, y: 0, width: 844, height: 338)
            for mode in host.operatingModes {
                host.setOperatingMode(mode)
                view.update(lcd: player.snapshot().display, mode: .fullDevice, operatingMode: mode, pressedKeys: [], inputEpoch: 0, active: true)
                view.layoutIfNeeded()
                let elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
                let names = id == "pc-1245" ? ["RUN", "PRO"] : ["RUN", "PRO", "RESERVE"]
                let regions = try names.map { name in try XCTUnwrap(elements.first { $0.accessibilityIdentifier == "symbol_" + name }) }
                for (index, element) in regions.enumerated() {
                    XCTAssertEqual(element.accessibilityValue, names[index] == (mode == "RSV" ? "RESERVE" : mode) ? "点灯" : "消灯")
                    if index > 0 { XCTAssertLessThan(regions[index - 1].accessibilityFrameInContainerSpace.maxX, element.accessibilityFrameInContainerSpace.minX) }
                }
                XCTAssertFalse(elements.contains { $0.accessibilityIdentifier == "operatingMode" })
            }
        }
    }

    private func pc1350Fixture() throws -> PreparedRom {
        var bytes = Data(repeating: 0x4d, count: 0x10000)
        bytes[0] = 0x79; bytes[1] = 0; bytes[2] = 0
        return try IosRomCodec.prepare(IosRomCodec.read(bytes: bytes, name: "original.bin"), machineId: "pc-1350")
    }

    func testPc1350SmallViewBothShiftRegionsAndFourLineCrop() throws {
        let prepared = try pc1350Fixture()
        let player = prepared.player
        let host = PlayerHost(player: player, frames: ManualFrames())
        host.setActive(true)
        XCTAssertFalse(host.showsRunProToggle)
        XCTAssertTrue(host.operatingModes.isEmpty)
        host.toggleOperatingMode(); XCTAssertEqual(host.status.operatingMode, "RUN")
        let view = PlayerSkinView(skin: player.skin, machine: player.machine)
        view.router.onPress = { host.press(keyName: $0, owner: $1) }
        view.router.onRelease = { host.release(owner: $0) }
        view.frame = CGRect(x: 0, y: 0, width: 667, height: 300)
        func update(_ mode: PgpPlayer.PresentationMode) {
            view.update(lcd: player.snapshot().display, mode: mode, operatingMode: "RUN", pressedKeys: host.pressedKeys, inputEpoch: 0, active: true)
            view.layoutIfNeeded()
        }
        update(.fullDevice)
        XCTAssertNotNil(UIImage(named: player.machine.skinImageName))
        XCTAssertEqual(host.lcd?.logicalColumns, 150); XCTAssertEqual(host.lcd?.logicalRows, 32)
        var elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
        XCTAssertEqual(elements.filter { $0.accessibilityTraits.contains(.button) }.count, 62)
        XCTAssertEqual(Set(elements.compactMap(\.accessibilityIdentifier)).count, elements.count)
        for e in elements { XCTAssertTrue(view.bounds.contains(e.accessibilityFrameInContainerSpace)) }
        let shifts = player.skin.keyRegions.filter { $0.key.name == "SHIFT" }
        XCTAssertEqual(Set(shifts.map(\.id)), ["SHIFT_LEFT", "SHIFT_RIGHT"])
        let left = try XCTUnwrap(shifts.first { $0.id == "SHIFT_LEFT" })
        let right = try XCTUnwrap(shifts.first { $0.id == "SHIFT_RIGHT" })
        func point(_ region: SkinKeyRegion) -> CGPoint {
            let rect = view.screenRect(region.bounds); return CGPoint(x: rect.midX, y: rect.midY)
        }
        view.router.begin(owner: "left", point: point(left))
        view.router.begin(owner: "right", point: point(right))
        view.router.move(owner: "right", point: point(right))
        XCTAssertEqual(view.router.owners["right"], "SHIFT_RIGHT")
        view.router.end(owner: "left"); XCTAssertEqual(host.pressedKeys, ["SHIFT"])
        update(.fullDevice)
        elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
        for id in ["key_SHIFT_LEFT", "key_SHIFT_RIGHT"] {
            XCTAssertEqual(elements.first { $0.accessibilityIdentifier == id }?.accessibilityValue, "押下中")
        }
        view.router.move(owner: "right", point: CGPoint(x: -1, y: -1)); XCTAssertTrue(host.pressedKeys.isEmpty)
        view.router.begin(owner: "right", point: point(right)); view.router.cancel(); XCTAssertTrue(host.pressedKeys.isEmpty)
        update(.controllerDisplay)
        XCTAssertTrue(view.bounds.contains(view.screenRect(player.skin.lcdContentRegion)))
        XCTAssertEqual((view.accessibilityElements as? [UIAccessibilityElement])?.filter { $0.accessibilityTraits.contains(.button) }.count, 0)
    }

    func testPc1350PhysicalKeysAvailabilityAndSessionSwitchRelease() throws {
        let input = IosPlayerInput()
        let frames = ManualFrames()
        let host = PlayerHost(player: try pc1350Fixture().player, frames: frames, input: input)
        host.setActive(true); host.run(); frames.send(0)
        input.bind("SOUTH", to: "MODE")
        XCTAssertTrue(input.available("SOUTH"))
        for (code, key) in [(59, "MODE"), (60, "KANA"), (73, "INSERT"), (76, "DELETE")] {
            input.keyEvent(code: code, down: true)
            XCTAssertTrue(host.pressedKeys.contains(key))
            host.setInputFocus(false); XCTAssertTrue(host.pressedKeys.isEmpty)
            input.keyEvent(code: code, down: false); host.setInputFocus(true)
        }
        let old = ApplePlayerFactory.shared.structuralProbe()
        input.configure(player: old, receiving: false, epoch: 999)
        XCTAssertFalse(input.available("SOUTH"))
        XCTAssertEqual(input.bindings["SOUTH"], "MODE")
        XCTAssertEqual(AppleInputCatalog.shared.keyForMachine(code: 76, machineId: "pc-1245"), "CLEAR")
        XCTAssertNil(AppleInputCatalog.shared.keyForMachine(code: 59, machineId: "pc-1261"))
    }

    func testPc1350RomStoreAndActualProgramFilesRoundTrip() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let prepared = try pc1350Fixture()
        let store = IosRomStore(root: root, machineId: "pc-1350")
        try store.install(archive: prepared.archive, sourceName: "original.bin")
        XCTAssertEqual(store.recordURL.lastPathComponent, "pc-1350.json")
        XCTAssertThrowsError(try IosRomCodec.prepare(XCTUnwrap(try store.load()), machineId: "pc-1261"))
        let host = PlayerHost(player: nil, frames: ManualFrames(), romStore: store)
        host.restoreRom(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1350")
        let source = root.appendingPathComponent("original.bas")
        try Data("10 PRINT \"FOUR ROWS\"\n20 END\n".utf8).write(to: source)
        host.openProgram(save: false); host.importProgram(source); waitUntil { !host.programBusy }
        XCTAssertNil(host.programIssue)
        host.closeProgram(); host.openProgram(save: true)
        host.prepareProgramExport(basic: true, start: "", end: ""); waitUntil { !host.programBusy }
        let url = try XCTUnwrap(host.exportURL)
        let saved = try Data(contentsOf: url)
        XCTAssertTrue(String(decoding: saved, as: UTF8.self).contains("FOUR ROWS"))
        host.finishProgramExport(saved: true); host.closeProgram()
        host.openProgram(save: false); host.importProgram(source); waitUntil { !host.programBusy }
        XCTAssertNil(host.programIssue); host.closeProgram()
        let dump = root.appendingPathComponent("original.dmp")
        try Data("6000 12 34 56\n6010 AB CD\n".utf8).write(to: dump)
        host.openProgram(save: false); host.importProgram(dump); waitUntil { !host.programBusy }
        XCTAssertNil(host.programIssue); host.closeProgram(); host.openProgram(save: true)
        host.prepareProgramExport(basic: false, start: "6000", end: "6011"); waitUntil { !host.programBusy }
        XCTAssertNotNil(host.exportURL); host.finishProgramExport(saved: false); host.closeProgram()
        let savedRecord = try Data(contentsOf: store.recordURL)
        let failing = IosRomStore(root: root, machineId: "pc-1350", write: { _, _ in throw RomFailure(message: "Injected failure") })
        XCTAssertThrowsError(try failing.install(archive: prepared.archive, sourceName: "replacement"))
        XCTAssertEqual(try Data(contentsOf: store.recordURL), savedRecord)
        host.openRomPanel(); host.requestRomDeletion(); host.cancelRomChange(); XCTAssertTrue(host.hasSession)
        host.requestRomDeletion(); host.confirmRomDeletion(); waitUntil { !host.isBusy }
        XCTAssertFalse(host.hasSession); XCTAssertFalse(host.hasSavedRom)
    }

    func testPc140xSharedPackageSelectionImportAndRestore() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let domain = "pgp-140x-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: domain)!
        defer { try? FileManager.default.removeItem(at: root); defaults.removePersistentDomain(forName: domain) }
        var bytes = Data(repeating: 0x4d, count: 0x10000)
        bytes[0] = 0x79; bytes[1] = 0; bytes[2] = 0
        let input = try IosRomCodec.read(bytes: bytes, name: "original.bin")
        let preferences = IosMachinePreferences(defaults: defaults)
        let store = IosRomStore(root: root, machineId: "pc-1402")
        XCTAssertEqual(store.recordURL.lastPathComponent, "pc-1401.json")
        XCTAssertEqual(store.recordURL, IosRomStore(root: root, machineId: "pc-1401").recordURL)
        let normalized = try IosRomCodec.prepare(input, machineId: "pc-1402")
        var files = try RomArchive.read(normalized.archive)
        XCTAssertEqual(files["rom/internal.bin"]?.count, 8192)
        XCTAssertEqual(files["rom/external.bin"]?.count, 32768)
        for sourceId in ["pc-1402", "pc-1401"] {
            var manifest = try JSONSerialization.jsonObject(with: files["manifest.json"]!) as! [String: Any]
            manifest["machineId"] = sourceId
            files["manifest.json"] = try JSONSerialization.data(withJSONObject: manifest)
            for target in ["pc-1402", "pc-1401"] {
                let prepared = try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: target)
                XCTAssertEqual(prepared.player.machine.id, target)
                XCTAssertEqual(prepared.player.machine.characterRows, 1)
                XCTAssertEqual(try RomArchive.read(prepared.archive)["rom/external.bin"], files["rom/external.bin"])
            }
            XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: "pc-1251"))
        }
        files["rom/external.bin"]?[0] ^= 1
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: "pc-1402"))
        let old = try IosRomCodec.prepare(input, machineId: "pc-1251")
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(old.archive), machineId: "pc-1401"))
        XCTAssertThrowsError(try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "short.bin"), machineId: "pc-1401"))
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        let source = root.appendingPathComponent("original.bin")
        try bytes.write(to: source)
        let frames = ManualFrames()
        let host = PlayerHost(player: nil, frames: frames, romStore: store, startsAtHome: true, machinePreferences: preferences)
        host.requestMachineSelection("pc-1402")
        host.importRom(source); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1402")
        XCTAssertTrue(host.hasRom("pc-1401"))
        let saved = try Data(contentsOf: store.recordURL)
        host.requestMachineSelection("pc-1401")
        host.enterPlayer(); XCTAssertEqual(host.pendingMachineId, "pc-1401")
        host.cancelMachineSelection(); XCTAssertEqual(host.machine.id, "pc-1402")
        host.enterPlayer(); host.confirmMachineSelection(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1401")
        host.returnHome(); host.requestMachineSelection("pc-1402")
        let restored = PlayerHost(player: nil, frames: ManualFrames(), romStore: store, startsAtHome: true,
                                  machinePreferences: IosMachinePreferences(defaults: defaults))
        restored.restoreRom(); waitUntil { !restored.isBusy }
        XCTAssertEqual(restored.machine.id, "pc-1401")
        XCTAssertEqual(restored.selectedMachineId, "pc-1402")
        XCTAssertEqual(restored.status.runState, "PAUSED")
        host.openRomPanel(); host.importRom(source); waitUntil { !host.isBusy }
        XCTAssertTrue(host.awaitingReplacement)
        host.cancelRomChange(); XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        // A failed atomic replacement preserves the running alias and the common library.
        let failing = IosRomStore(root: root, machineId: "pc-1401", write: { _, _ in throw RomFailure(message: "Injected write failure") })
        let failureHost = PlayerHost(player: normalized.player, frames: ManualFrames(), romStore: failing)
        failureHost.importRom(source); waitUntil { !failureHost.isBusy }
        failureHost.confirmRomInstall(); waitUntil { !failureHost.isBusy }
        XCTAssertEqual(failureHost.machine.id, "pc-1402")
        XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        host.importRom(source); waitUntil { !host.isBusy }
        host.confirmRomInstall(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1402")
        host.openRomPanel(); host.requestRomDeletion(); host.cancelRomChange()
        XCTAssertTrue(host.hasSession)
        host.requestRomDeletion(); host.confirmRomDeletion(); waitUntil { !host.isBusy }
        XCTAssertFalse(host.hasSession); XCTAssertFalse(host.hasRom("pc-1401"))
    }

    func testPc126xSharedPackageSelectionImportAndRestore() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let domain = "pgp-126x-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: domain)!
        defer { try? FileManager.default.removeItem(at: root); defaults.removePersistentDomain(forName: domain) }
        var bytes = Data(repeating: 0x4d, count: 0x10000)
        bytes[0] = 0x79; bytes[1] = 0; bytes[2] = 0
        let input = try IosRomCodec.read(bytes: bytes, name: "original.bin")
        let preferences = IosMachinePreferences(defaults: defaults)
        let store = IosRomStore(root: root, machineId: "pc-1260")
        XCTAssertEqual(store.recordURL.lastPathComponent, "pc-1261.json")
        XCTAssertEqual(store.recordURL, IosRomStore(root: root, machineId: "pc-1261").recordURL)
        let normalized = try IosRomCodec.prepare(input, machineId: "pc-1260")
        var files = try RomArchive.read(normalized.archive)
        XCTAssertEqual(files["rom/internal.bin"]?.count, 8192)
        XCTAssertEqual(files["rom/external.bin"]?.count, 32768)
        for sourceId in ["pc-1260", "pc-1261"] {
            var manifest = try JSONSerialization.jsonObject(with: files["manifest.json"]!) as! [String: Any]
            manifest["machineId"] = sourceId
            files["manifest.json"] = try JSONSerialization.data(withJSONObject: manifest)
            for target in ["pc-1260", "pc-1261"] {
                let prepared = try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: target)
                XCTAssertEqual(prepared.player.machine.id, target)
                XCTAssertEqual(prepared.player.machine.characterRows, 2)
                XCTAssertEqual(try RomArchive.read(prepared.archive)["rom/external.bin"], files["rom/external.bin"])
            }
            XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: "pc-1251"))
        }
        files["rom/external.bin"]?[0] ^= 1
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: "pc-1260"))
        let old = try IosRomCodec.prepare(input, machineId: "pc-1251")
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(old.archive), machineId: "pc-1261"))
        XCTAssertThrowsError(try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "short.bin"), machineId: "pc-1261"))
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        let source = root.appendingPathComponent("original.bin")
        try bytes.write(to: source)
        let frames = ManualFrames()
        let host = PlayerHost(player: nil, frames: frames, romStore: store, startsAtHome: true, machinePreferences: preferences)
        host.requestMachineSelection("pc-1260")
        host.importRom(source); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1260")
        XCTAssertTrue(host.hasRom("pc-1261"))
        let saved = try Data(contentsOf: store.recordURL)
        host.requestMachineSelection("pc-1261")
        host.enterPlayer(); XCTAssertEqual(host.pendingMachineId, "pc-1261")
        host.cancelMachineSelection(); XCTAssertEqual(host.machine.id, "pc-1260")
        host.enterPlayer(); host.confirmMachineSelection(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1261")
        host.returnHome(); host.requestMachineSelection("pc-1260")
        let restored = PlayerHost(player: nil, frames: ManualFrames(), romStore: store, startsAtHome: true,
                                  machinePreferences: IosMachinePreferences(defaults: defaults))
        restored.restoreRom(); waitUntil { !restored.isBusy }
        XCTAssertEqual(restored.machine.id, "pc-1261")
        XCTAssertEqual(restored.selectedMachineId, "pc-1260")
        XCTAssertEqual(restored.status.runState, "PAUSED")
        host.openRomPanel(); host.importRom(source); waitUntil { !host.isBusy }
        XCTAssertTrue(host.awaitingReplacement)
        host.cancelRomChange(); XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        // A failed atomic replacement preserves the running alias and the common library.
        let failing = IosRomStore(root: root, machineId: "pc-1261", write: { _, _ in throw RomFailure(message: "Injected write failure") })
        let failureHost = PlayerHost(player: normalized.player, frames: ManualFrames(), romStore: failing)
        failureHost.importRom(source); waitUntil { !failureHost.isBusy }
        failureHost.confirmRomInstall(); waitUntil { !failureHost.isBusy }
        XCTAssertEqual(failureHost.machine.id, "pc-1260")
        XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
        host.importRom(source); waitUntil { !host.isBusy }
        host.confirmRomInstall(); waitUntil { !host.isBusy }
        XCTAssertEqual(host.machine.id, "pc-1260")
        host.openRomPanel(); host.requestRomDeletion(); host.cancelRomChange()
        XCTAssertTrue(host.hasSession)
        host.requestRomDeletion(); host.confirmRomDeletion(); waitUntil { !host.isBusy }
        XCTAssertFalse(host.hasSession); XCTAssertFalse(host.hasRom("pc-1261"))
    }

    func testPc126xTwoRowSkinAndStatusLabels() throws {
        let prepared = try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x10000), name: "synthetic.bin"), machineId: "pc-1261")
        let player = prepared.player
        let lcd = player.snapshot().display
        XCTAssertEqual(lcd.logicalColumns, 143); XCTAssertEqual(lcd.logicalRows, 15)
        XCTAssertEqual(lcd.logicalY(row: 7), 8)
        XCTAssertNotNil(UIImage(named: player.machine.skinImageName))
        let view = PlayerSkinView(skin: player.skin, machine: player.machine)
        view.frame = CGRect(x: 0, y: 0, width: 844, height: 338)
        view.update(lcd: lcd, mode: .fullDevice, operatingMode: "RUN", pressedKeys: [], inputEpoch: 0, active: true)
        view.layoutIfNeeded()
        let elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
        let display = try XCTUnwrap(elements.first { $0.accessibilityIdentifier == "lcd" })
        XCTAssertTrue(display.accessibilityLabel?.contains("24桁×2行") == true)
        XCTAssertTrue(view.bounds.contains(display.accessibilityFrameInContainerSpace))
        XCTAssertEqual(elements.first { $0.accessibilityIdentifier == "symbol_RESERVE" }?.accessibilityLabel, "RSV")
        XCTAssertEqual(elements.first { $0.accessibilityIdentifier == "symbol_SMALL" }?.accessibilityLabel, "SML")
        XCTAssertEqual(elements.filter { $0.accessibilityTraits.contains(.button) }.count, 52)
        view.update(lcd: lcd, mode: .controllerDisplay, operatingMode: "RUN", pressedKeys: [], inputEpoch: 1, active: true)
        view.layoutIfNeeded()
        XCTAssertTrue(view.bounds.contains(view.screenRect(player.skin.lcdContentRegion)))
        XCTAssertEqual((view.accessibilityElements as? [UIAccessibilityElement])?.filter { $0.accessibilityTraits.contains(.button) }.count, 0)
    }

    func testPc140xAnd1450SmallSkinPhysicalInputsAndProgramFiles() throws {
        for id in ["pc-1401", "pc-1402", "pc-1450"] {
            let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
            defer { try? FileManager.default.removeItem(at: root) }
            var bytes = Data(repeating: 0x4d, count: 0x10000)
            bytes[0] = 0x79; bytes[1] = 0; bytes[2] = 0
            let prepared = try IosRomCodec.prepare(IosRomCodec.read(bytes: bytes, name: "original.bin"), machineId: id)
            XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(prepared.archive), machineId: id == "pc-1450" ? "pc-1401" : "pc-1450"))
            let store = IosRomStore(root: root, machineId: id)
            try store.install(archive: prepared.archive, sourceName: "original.bin")
            let domain = "pgp-14xx-files-" + UUID().uuidString
            let defaults = UserDefaults(suiteName: domain)!
            defer { defaults.removePersistentDomain(forName: domain) }
            let preferences = IosMachinePreferences(defaults: defaults)
            preferences.runningMachineId = id
            let input = IosPlayerInput()
            let frames = ManualFrames()
            let host = PlayerHost(player: nil, frames: frames, romStore: store, machinePreferences: preferences, input: input)
            host.restoreRom(); waitUntil { !host.isBusy }
            XCTAssertEqual(host.machine.id, id)
            XCTAssertFalse(host.showsRunProToggle)
            XCTAssertTrue(host.operatingModes.isEmpty)
            let originalMode = host.status.operatingMode
            host.setOperatingMode("PRO"); host.setProgramMode(true)
            XCTAssertEqual(host.status.operatingMode, originalMode)
            let player = prepared.player
            XCTAssertNotNil(UIImage(named: player.machine.skinImageName))
            let view = PlayerSkinView(skin: player.skin, machine: player.machine)
            view.frame = CGRect(x: 0, y: 0, width: 667, height: 300)
            view.update(lcd: player.snapshot().display, mode: .fullDevice, operatingMode: "RUN", pressedKeys: [], inputEpoch: 0, active: true)
            view.layoutIfNeeded()
            let elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
            XCTAssertEqual(elements.filter { $0.accessibilityTraits.contains(.button) }.count, player.skin.keyRegions.count)
            XCTAssertEqual(Set(elements.compactMap(\.accessibilityIdentifier)).count, elements.count)
            for e in elements { XCTAssertTrue(view.bounds.contains(e.accessibilityFrameInContainerSpace), e.accessibilityIdentifier ?? "") }
            for name in ["CAL", "RUN", "PRO", "HYP", "STAT", "PRINT"] {
                XCTAssertNotNil(elements.first { $0.accessibilityIdentifier == "symbol_" + name })
            }
            for (symbol, label) in [("PARENTHESES", "( )"), ("MEMORY", "M"), ("ERROR", "E")] + (id == "pc-1450" ? [("KANA_SMALL", "小")] : []) {
                XCTAssertEqual(elements.first { $0.accessibilityIdentifier == "symbol_" + symbol }?.accessibilityLabel, label)
            }
            view.update(lcd: player.snapshot().display, mode: .controllerDisplay, operatingMode: "RUN", pressedKeys: [], inputEpoch: 1, active: true)
            view.layoutIfNeeded()
            XCTAssertTrue(view.bounds.contains(view.screenRect(player.skin.lcdContentRegion)))
            XCTAssertEqual((view.accessibilityElements as? [UIAccessibilityElement])?.filter { $0.accessibilityTraits.contains(.button) }.count, 0)
            host.setActive(true); host.run(); frames.send(0)
            let codes: [(Int, String)] = [(59, "CAL"), (60, "BASIC")] + (id == "pc-1450" ? [(61, "KANA"), (62, "SMALL"), (63, "DAKUTEN"), (64, "LONG_VOWEL"), (65, "LOCK")] : [])
            for (code, key) in codes {
                input.keyEvent(code: code, down: true); XCTAssertTrue(host.pressedKeys.contains(key))
                host.setInputFocus(false); XCTAssertTrue(host.pressedKeys.isEmpty)
                input.keyEvent(code: code, down: false); host.setInputFocus(true)
            }
            input.bind("SOUTH", to: "CAL"); XCTAssertTrue(input.available("SOUTH"))
            input.configure(player: ApplePlayerFactory.shared.structuralProbe(), receiving: false, epoch: 999)
            XCTAssertFalse(input.available("SOUTH")); XCTAssertEqual(input.bindings["SOUTH"], "CAL")
            host.pause()
            let source = root.appendingPathComponent("original.bas")
            try Data("10 PRINT 1\n20 END\n".utf8).write(to: source)
            host.openProgram(save: false); host.importProgram(source); waitUntil { !host.programBusy }
            XCTAssertNil(host.programIssue); host.closeProgram(); host.openProgram(save: true)
            host.prepareProgramExport(basic: true, start: "", end: ""); waitUntil { !host.programBusy }
            let exported = try Data(contentsOf: XCTUnwrap(host.exportURL))
            XCTAssertTrue(String(decoding: exported, as: UTF8.self).contains("PRINT"))
            host.finishProgramExport(saved: true); host.closeProgram()
            let dump = root.appendingPathComponent("original.dmp")
            let address = id == "pc-1450" ? "5000" : "4400"
            try Data("\(address) 12 34\n".utf8).write(to: dump)
            host.openProgram(save: false); host.importProgram(dump); waitUntil { !host.programBusy }
            XCTAssertNil(host.programIssue); host.closeProgram(); host.openProgram(save: true)
            host.prepareProgramExport(basic: false, start: address, end: address); waitUntil { !host.programBusy }
            XCTAssertNotNil(host.exportURL); host.finishProgramExport(saved: false); host.closeProgram()
            let saved = try Data(contentsOf: store.recordURL)
            let failing = IosRomStore(root: root, machineId: id, write: { _, _ in throw RomFailure(message: "Injected failure") })
            XCTAssertThrowsError(try failing.install(archive: prepared.archive, sourceName: "replacement"))
            XCTAssertEqual(try Data(contentsOf: store.recordURL), saved)
            host.openRomPanel(); host.requestRomDeletion(); host.cancelRomChange(); XCTAssertTrue(host.hasSession)
            host.requestRomDeletion(); host.confirmRomDeletion(); waitUntil { !host.isBusy }; XCTAssertFalse(host.hasSession)
        }
    }

    func testBankedPackagesStoreRestoreAndS2ProgramFiles() throws {
        for id in ["pc-1360", "pc-1460", "pc-1470u", "pc-1360k"] {
            let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
            defer { try? FileManager.default.removeItem(at: root) }
            let archive = try PlayerHost.bankedProbeArchive(id)
            let read = try IosRomCodec.read(bytes: archive, name: "original.pgrom")
            let prepared = try IosRomCodec.prepare(read, machineId: id)
            let files = try RomArchive.read(prepared.archive)
            let count = id == "pc-1460" ? 4 : 8
            XCTAssertEqual(prepared.player.machine.romBankCount, Int32(count))
            XCTAssertFalse(prepared.player.machine.supportsLegacyRom)
            XCTAssertEqual(files.count, count + 2 + (id == "pc-1360k" ? 4 : 0))
            XCTAssertEqual(files["rom/internal.bin"]?.count, 8192)
            for bank in 0..<count { XCTAssertEqual(files["rom/bank-\(bank).bin"]?.count, 16384) }
            let originalFiles = try RomArchive.read(archive)
            for (path, data) in files where path != "manifest.json" { XCTAssertEqual(data, originalFiles[path]) }
            XCTAssertThrowsError(try IosRomCodec.prepare(read, machineId: id == "pc-1360" ? "pc-1460" : "pc-1360"))
            XCTAssertThrowsError(try IosRomCodec.prepare(IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x10000), name: "flat.bin"), machineId: id))
            var bad = files; bad["rom/bank-0.bin"]?[0] ^= 1
            XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(bad)), machineId: id))
            bad = files; bad.removeValue(forKey: "rom/bank-0.bin")
            XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(bad)), machineId: id))
            var manifest = try JSONSerialization.jsonObject(with: files["manifest.json"]!) as! [String: Any]
            var components = manifest["components"] as! [[String: Any]]
            components[0]["role"] = "internal"; manifest["components"] = components
            bad = files; bad["manifest.json"] = try JSONSerialization.data(withJSONObject: manifest)
            XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(bad)), machineId: id))
            let store = IosRomStore(root: root, machineId: id)
            try store.install(archive: prepared.archive, sourceName: "original.pgrom")
            XCTAssertEqual(store.recordURL.lastPathComponent, id + ".json")
            let host = PlayerHost(player: nil, frames: ManualFrames(), romStore: store)
            host.restoreRom(); waitUntil { !host.isBusy }
            XCTAssertEqual(host.machine.id, id); XCTAssertTrue(host.operatingModes.isEmpty); XCTAssertFalse(host.showsRunProToggle)
            XCTAssertEqual(host.status.runState, "PAUSED")
            let source = root.appendingPathComponent("original.bas")
            try Data("10 PRINT \"S2\"\n20 END\n".utf8).write(to: source)
            host.openProgram(save: false); host.importProgram(source); waitUntil { !host.programBusy }
            XCTAssertNil(host.programIssue); host.closeProgram(); host.openProgram(save: true)
            host.prepareProgramExport(basic: true, start: "", end: ""); waitUntil { !host.programBusy }
            let savedBasic = try Data(contentsOf: XCTUnwrap(host.exportURL))
            XCTAssertTrue(String(decoding: savedBasic, as: UTF8.self).contains("S2"))
            host.finishProgramExport(saved: true); host.closeProgram()
            let dump = root.appendingPathComponent("original.dmp")
            try Data("9000 12 34\n9010 AB CD\n".utf8).write(to: dump)
            host.openProgram(save: false); host.importProgram(dump); waitUntil { !host.programBusy }
            XCTAssertNil(host.programIssue); host.closeProgram(); host.openProgram(save: true)
            host.prepareProgramExport(basic: false, start: "9000", end: "9011"); waitUntil { !host.programBusy }
            let savedDump = try Data(contentsOf: XCTUnwrap(host.exportURL)); host.finishProgramExport(saved: false); host.closeProgram()
            try Data("9000 99\n4000 56\n".utf8).write(to: dump)
            host.openProgram(save: false); host.importProgram(dump); waitUntil { !host.programBusy }
            XCTAssertNotNil(host.programIssue); host.closeProgram(); host.openProgram(save: true)
            host.prepareProgramExport(basic: false, start: "9000", end: "9011"); waitUntil { !host.programBusy }
            XCTAssertEqual(try Data(contentsOf: XCTUnwrap(host.exportURL)), savedDump)
            host.finishProgramExport(saved: false); host.closeProgram()
            let record = try Data(contentsOf: store.recordURL)
            let failing = IosRomStore(root: root, machineId: id, write: { _, _ in throw RomFailure(message: "Injected failure") })
            XCTAssertThrowsError(try failing.install(archive: prepared.archive, sourceName: "replacement"))
            XCTAssertEqual(try Data(contentsOf: store.recordURL), record)
            host.openRomPanel(); host.requestRomDeletion(); host.cancelRomChange(); XCTAssertTrue(host.hasSession)
            host.requestRomDeletion(); host.confirmRomDeletion(); waitUntil { !host.isBusy }; XCTAssertFalse(host.hasSession)
        }
    }

    func testBankedSkinsSmallLayoutPhysicalKeysAndDualShiftOwners() throws {
        for id in ["pc-1360", "pc-1460", "pc-1470u", "pc-1360k"] {
            let prepared = try IosRomCodec.prepare(inputFromArchive(PlayerHost.bankedProbeArchive(id)), machineId: id)
            let player = prepared.player
            let input = IosPlayerInput(), frames = ManualFrames()
            let host = PlayerHost(player: player, frames: frames, input: input)
            host.setActive(true); host.run(); frames.send(0); frames.send(10_000_000)
            XCTAssertFalse(host.showsRunProToggle); XCTAssertTrue(host.operatingModes.isEmpty)
            let view = PlayerSkinView(skin: player.skin, machine: player.machine)
            view.frame = CGRect(x: 0, y: 0, width: 667, height: 300)
            view.router.onPress = { host.press(keyName: $0, owner: $1) }
            view.router.onRelease = { host.release(owner: $0) }
            func update(_ mode: PgpPlayer.PresentationMode) {
                view.update(lcd: player.snapshot().display, mode: mode, operatingMode: host.status.operatingMode, pressedKeys: host.pressedKeys, inputEpoch: host.inputEpoch, active: true)
                view.layoutIfNeeded()
            }
            update(.fullDevice)
            XCTAssertNotNil(UIImage(named: player.machine.skinImageName))
            let elements = try XCTUnwrap(view.accessibilityElements as? [UIAccessibilityElement])
            XCTAssertEqual(elements.filter { $0.accessibilityTraits.contains(.button) }.count, id == "pc-1360k" ? 65 : id == "pc-1360" ? 60 : 81)
            XCTAssertEqual(Set(elements.compactMap(\.accessibilityIdentifier)).count, elements.count)
            for element in elements { XCTAssertTrue(view.bounds.contains(element.accessibilityFrameInContainerSpace), element.accessibilityIdentifier ?? "") }
            XCTAssertEqual(elements.filter { $0.accessibilityIdentifier?.hasPrefix("symbol_") == true }.count, id == "pc-1360k" ? 8 : id == "pc-1360" ? 6 : id == "pc-1460" ? 19 : 22)
            if id == "pc-1470u" {
                XCTAssertEqual(elements.first { $0.accessibilityIdentifier == "symbol_BATTERY" }?.accessibilityLabel, "BATT")
                let shifts = player.skin.keyRegions.filter { $0.key.name == "SHIFT" }
                XCTAssertEqual(Set(shifts.map(\.id)), ["SHIFT", "LOCK"])
                for (index, region) in shifts.enumerated() {
                    let rect = view.screenRect(region.bounds)
                    view.router.begin(owner: "shift-\(index)", point: CGPoint(x: rect.midX, y: rect.midY))
                }
                view.router.end(owner: "shift-0"); XCTAssertTrue(host.pressedKeys.contains("SHIFT"))
                view.router.end(owner: "shift-1"); XCTAssertFalse(host.pressedKeys.contains("SHIFT"))
            }
            let codes: [(Int, String)] = id == "pc-1360k" ? [(59, "MODE"), (60, "KANA"), (61, "SMALL"), (62, "KANJI"), (63, "HIRA_KATA"), (64, "KANJI_FIX"), (65, "KANJI_CONVERT"), (66, "YJ")] : id == "pc-1360" ? [(59, "MODE"), (60, "KANA"), (61, "SMALL"), (73, "INSERT"), (76, "DELETE")] :
                [(59, "CAL"), (60, "BASIC"), (61, "KANA"), (62, "SMALL"), (63, "DAKUTEN"), (64, "LONG_VOWEL"), (65, id == "pc-1470u" ? "SHIFT" : "LOCK")] +
                (id == "pc-1470u" ? [(42, "BACKSPACE"), (73, "INSERT"), (76, "DELETE")] : [])
            for (code, key) in codes {
                input.keyEvent(code: code, down: true); XCTAssertTrue(host.pressedKeys.contains(key), key)
                host.setInputFocus(false); XCTAssertTrue(host.pressedKeys.isEmpty)
                input.keyEvent(code: code, down: false); host.setInputFocus(true)
            }
            input.bind("SOUTH", to: ["pc-1360", "pc-1360k"].contains(id) ? "MODE" : "CAL"); XCTAssertTrue(input.available("SOUTH"))
            input.configure(player: ApplePlayerFactory.shared.structuralProbe(), receiving: false, epoch: 999)
            XCTAssertFalse(input.available("SOUTH"))
            update(.controllerDisplay)
            XCTAssertTrue(view.bounds.contains(view.screenRect(player.skin.lcdContentRegion)))
            XCTAssertEqual((view.accessibilityElements as? [UIAccessibilityElement])?.filter { $0.accessibilityTraits.contains(.button) }.count, 0)
        }
    }

    func testKanjiSelectorsHashesAndGeneratedGlyphReachLcd() throws {
        let id = "pc-1360k"
        let files = try RomArchive.read(PlayerHost.bankedProbeArchive(id))
        let manifest = try JSONDecoder().decode(RomManifest.self, from: files["manifest.json"]!)
        for selectors in [[], [0, 1], [0, 1, 2, 3], [0], [0, 2], [2, 3], [0, 1, 2]] as [[Int]] {
            var subset = files
            let allowed = selectors.map { "kanji-selector-\($0)" }
            let components = manifest.components.filter { !$0.id.hasPrefix("kanji-selector-") || allowed.contains($0.id) }
            for component in manifest.components where !components.contains(where: { $0.id == component.id }) {
                subset.removeValue(forKey: component.file)
            }
            subset["manifest.json"] = try JSONEncoder().encode(RomManifest(format: manifest.format, formatVersion: manifest.formatVersion,
                machineId: id, title: nil, components: components))
            let read = try inputFromArchive(RomArchive.write(subset))
            if [0, 2, 4].contains(selectors.count) && selectors == Array(0..<selectors.count) {
                XCTAssertNoThrow(try IosRomCodec.prepare(read, machineId: id))
            } else {
                XCTAssertThrowsError(try IosRomCodec.prepare(read, machineId: id))
            }
        }
        var damaged = files; damaged["rom/kanji-selector-0.bin"]?[0] ^= 1
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(damaged)), machineId: id))
        let prepared = try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: id)
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: "pc-1360"))
        let player = prepared.player
        player.run(nowNanoseconds: 0); player.tick(nowNanoseconds: 10_000_000); player.pause()
        let lcd = player.snapshot().display
        XCTAssertTrue(lcd.isDotOn(column: 1, row: 3)); XCTAssertFalse(lcd.isDotOn(column: 1, row: 2))
        XCTAssertTrue(lcd.symbols.contains(.s)); XCTAssertTrue(lcd.symbols.contains(.hiragana)); XCTAssertTrue(lcd.symbols.contains(.katakana))
        XCTAssertEqual(PlayerSkinView.symbolLabel(.hiragana), "平")
        XCTAssertEqual(PlayerSkinView.symbolLabel(.katakana), "片")
        for start: Int32 in [0xc100, 0xc110] {
            let dump = Data(kotlin: try XCTUnwrap(player.exportDump(start: start, end: start + 3).doCopyBytes()))
            XCTAssertTrue(String(decoding: dump, as: UTF8.self).contains("40 41 42 43"))
        }
    }

    private func inputFromArchive(_ archive: Data) throws -> ReadRom {
        try IosRomCodec.read(bytes: archive, name: "synthetic.pgrom")
    }

    func testSeriesPackageIdentityAndHashMustValidateBeforeRetargeting() throws {
        let input = try IosRomCodec.read(bytes: Data(repeating: 0x4d, count: 0x8000), name: "synthetic.bin")
        let prepared = try IosRomCodec.prepare(input, machineId: "pc-1250")
        var files = try RomArchive.read(prepared.archive)
        let original = files["rom/internal.bin"]!
        for sourceId in ["pc-1250", "pc-1251", "pc-1255"] {
            var manifest = try JSONSerialization.jsonObject(with: files["manifest.json"]!) as! [String: Any]
            manifest["machineId"] = sourceId
            files["manifest.json"] = try JSONSerialization.data(withJSONObject: manifest)
            let read = try IosRomCodec.read(bytes: RomArchive.write(files), name: "model.pgrom")
            for target in ["pc-1250", "pc-1251", "pc-1255"] {
                let normalized = try IosRomCodec.prepare(read, machineId: target)
                XCTAssertEqual(normalized.player.machine.id, target)
                XCTAssertEqual(try RomArchive.read(normalized.archive)["rom/internal.bin"], original)
            }
            XCTAssertThrowsError(try IosRomCodec.prepare(read, machineId: "pc-1245"))
        }
        files["rom/internal.bin"]?[0] ^= 1
        XCTAssertThrowsError(try IosRomCodec.prepare(inputFromArchive(RomArchive.write(files)), machineId: "pc-1255"))
        let domain = "pgp-invalid-model-" + UUID().uuidString
        let defaults = UserDefaults(suiteName: domain)!
        defer { defaults.removePersistentDomain(forName: domain) }
        defaults.set("pc-1401", forKey: "machine.model.pc-1251")
        defaults.set("invalid", forKey: "machine.running")
        let preferences = IosMachinePreferences(defaults: defaults)
        XCTAssertEqual(preferences.model(for: "pc-1251"), "pc-1251")
        XCTAssertNil(preferences.runningMachineId)
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

    func testProgramPickerWaitsForParentPresentationAndDoesNotReopenAfterCancel() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let host = try programHost(root, frames: ManualFrames())
        host.setActive(true)
        host.openProgram(save: false)
        XCTAssertTrue(host.programPanelOpen)
        XCTAssertFalse(host.programPickerPresented)
        host.programPanelDidAppear()
        XCTAssertTrue(host.programPickerPresented)
        host.cancelProgramPicker()
        host.programPanelDidAppear()
        XCTAssertFalse(host.programPickerPresented)
        host.chooseProgramFile()
        XCTAssertTrue(host.programPickerPresented)
        host.closeProgram()
        host.programPanelDidAppear()
        XCTAssertFalse(host.programPickerPresented)
        host.openProgram(save: true)
        host.programPanelDidAppear()
        XCTAssertFalse(host.programPickerPresented)
        host.closeProgram()
    }

    func testExportPresentationCarriesItsURLAndDismissKeepsProgramPanelOpen() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let frames = ManualFrames(), host = try programHost(root, frames: frames)
        host.setActive(true)
        host.openProgram(save: false)
        host.programPanelDidAppear()
        host.cancelProgramPicker()
        host.programDocumentDidDismiss()
        host.closeProgram()
        host.openProgram(save: true)
        host.prepareProgramExport(basic: false, start: "C500", end: "C501")
        waitUntil { !host.programBusy }
        guard case .exporting(let url)? = host.programDocument else { return XCTFail("export picker") }
        XCTAssertEqual(url, host.exportURL)
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
        // SwiftUI clears the item before onDismiss when the user dismisses the sheet.
        host.programDocument = nil
        host.programDocumentDidDismiss()
        XCTAssertTrue(host.programPanelOpen)
        XCTAssertTrue(host.programMessage.contains("セーブをキャンセル"))
        XCTAssertNil(host.exportURL)
        XCTAssertFalse(FileManager.default.fileExists(atPath: url.path))
        XCTAssertEqual(host.status.runState, "PAUSED")
        // A second export has its own URL and is still presentable without reopening the panel.
        host.prepareProgramExport(basic: false, start: "C500", end: "C501")
        waitUntil { !host.programBusy }
        guard case .exporting(let next)? = host.programDocument else { return XCTFail("second export picker") }
        XCTAssertNotEqual(next, url)
        host.finishProgramExport(saved: true)
        host.programDocumentDidDismiss()
        XCTAssertTrue(host.programMessage.contains("ファイルへセーブしました"))
        host.closeProgram()
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
