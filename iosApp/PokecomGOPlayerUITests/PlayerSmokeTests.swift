import XCTest

final class PlayerSmokeTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func testRunPauseResetRotationAndBackgroundReturn() {
        let app = launchProbe()
        waitFor("PAUSED", in: app)
        app.buttons["runState"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        app.buttons["pro"].tap()
        XCTAssertEqual(app.buttons["pro"].label, "PRO")
        playerAction(app, "エミュレータ操作", "Reset")
        waitFor("RUNNING", in: app)
        XCTAssertEqual(app.buttons["pro"].label, "RUN")
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        for orientation in [UIDeviceOrientation.landscapeRight, .landscapeLeft, .portrait] {
            rotateAndSettle(orientation)
            XCTAssertTrue(app.buttons["playerMenu"].isHittable)
            XCTAssertGreaterThan(app.frame.width, app.frame.height)
        }
        app.buttons["runState"].tap(); waitFor("RUNNING", in: app)
        XCUIDevice.shared.press(.home); app.activate()
        waitFor("PAUSED", in: app)
        app.buttons["key_BREAK"].press(forDuration: 0.3)
        XCTAssertEqual(app.buttons["key_BREAK"].value as? String, "解放")
        attachScreen("PC-1245 full device")
    }

    func testControllerDisplayKeepsLcdAndAvoidsNotchInBothDirections() {
        let app = launchProbe()
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 52)
        app.buttons["runState"].tap(); waitFor("RUNNING", in: app)
        let lit = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", "点灯"), object: app.staticTexts["symbol_SHIFT"])
        XCTAssertEqual(XCTWaiter.wait(for: [lit], timeout: 10), .completed)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        for orientation in [UIDeviceOrientation.landscapeRight, .landscapeLeft] {
            rotateAndSettle(orientation)
            playerAction(app, "表示", "Controller Display")
            let hidden = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
                app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count == 0
            }, object: nil)
            XCTAssertEqual(XCTWaiter.wait(for: [hidden], timeout: 5), .completed)
            XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
            attachScreen("Controller Display")
            playerAction(app, "表示", "通常表示")
            XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 52)
        }
    }

    func testLocalDocumentPickerSelectionCancelAndReturn() {
        let app = launchProbe("--picker-probe")
        home(app)
        openRomManagement(app)
        app.buttons["romImportManager"].tap()
        cancelPicker(app)
        XCTAssertTrue(app.staticTexts["romMessage"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["romMessage"].label.contains("キャンセル"))
        for name in ["picker-test.bas", "picker-test.dmp", "picker-invalid.pgrom", "picker-test.pgrom"] {
            app.buttons["romImportManager"].tap()
            let file = localFile(app, name)
            if name == "picker-test.bas" {
                XCUIDevice.shared.press(.home); app.activate()
                XCTAssertTrue(file.waitForExistence(timeout: 10))
            }
            file.tap()
            let message = app.staticTexts["romMessage"]
            XCTAssertTrue(message.waitForExistence(timeout: 10))
            expectLabel(message, contains: name == "picker-test.pgrom" ? "ROMを置換" : "取込みに失敗")
            if name == "picker-test.pgrom" { app.buttons["キャンセル"].tap() }
        }
        app.buttons["閉じる"].tap()
        app.buttons["playPC1245"].tap()
        waitFor("PAUSED", in: app)
    }

    func testHomeIntentProgramLoadSaveAndInformation() {
        let app = launchProbe("--navigation-probe", home: true)
        XCTAssertTrue(app.buttons["playPC1245"].exists)
        app.buttons["playPC1245"].tap(); waitFor("RUNNING", in: app)
        // Menus must also work with the CPU running, not just from explicit Pause.
        home(app)
        app.buttons["playPC1245"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        home(app)
        app.buttons["playPC1245"].tap(); waitFor("PAUSED", in: app)
        playerAction(app, "プログラム", "ロード")
        localFile(app, "picker-test.bas").tap()
        expectLabel(app.staticTexts["programMessage"], contains: "ロードしました")
        app.buttons["閉じる"].tap(); waitFor("RUNNING", in: app)
        playerAction(app, "プログラム", "セーブ")
        app.buttons["exportProgram"].tap()
        cancelPicker(app)
        expectLabel(app.staticTexts["programMessage"], contains: "セーブをキャンセル")
        app.buttons["閉じる"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        playerAction(app, "プログラム", "セーブ")
        app.buttons["exportProgram"].tap()
        saveLocalExport(app)
        expectLabel(app.staticTexts["programMessage"], contains: "セーブしました")
        app.buttons["閉じる"].tap(); waitFor("PAUSED", in: app)
        playerAction(app, "プログラム", "ロード")
        localFile(app, "pc-1245-program.bas").tap()
        expectLabel(app.staticTexts["programMessage"], contains: "ロードしました")
        app.buttons["閉じる"].tap(); waitFor("RUNNING", in: app)
        playerAction(app, "プログラム", "ロード")
        localFile(app, "picker-test.dmp").tap()
        expectLabel(app.staticTexts["programMessage"], contains: "ロードしました")
        app.buttons["閉じる"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        playerAction(app, "プログラム", "セーブ")
        app.buttons["ダンプ"].tap()
        XCTAssertTrue(app.textFields["dumpStart"].exists)
        app.buttons["exportProgram"].tap()
        saveLocalExport(app)
        expectLabel(app.staticTexts["programMessage"], contains: "セーブしました")
        app.buttons["閉じる"].tap(); waitFor("PAUSED", in: app)
        playerAction(app, "プログラム", "ロード")
        localFile(app, "pc-1245-program.dmp").tap()
        expectLabel(app.staticTexts["programMessage"], contains: "ロードしました")
        app.buttons["閉じる"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        home(app)
        for title in ["Playerとは", "Studioについて", "プライバシーポリシー", "バージョン・ライセンス"] {
            app.buttons["homeMenu"].tap()
            app.buttons["ヘルプ・情報"].tap()
            app.buttons[title].tap()
            XCTAssertTrue(app.buttons["戻る"].waitForExistence(timeout: 5))
            XCTAssertFalse(app.staticTexts["同梱文書を読み込めません。"].exists)
            attachScreen(title)
            app.buttons["戻る"].tap()
        }
    }

    func testPlaybackSettingsFromHome() {
        let app = launchProbe("--navigation-probe", home: true)
        app.buttons["homeMenu"].tap(); app.buttons["設定"].tap(); app.buttons["CPU速度"].tap()
        for speed in ["0.8", "0.9", "1.0", "1.1", "1.2"] {
            let option = app.buttons[speed + "倍"]
            XCTAssertTrue(option.waitForExistence(timeout: 5), app.debugDescription)
            option.tap()
        }
        attachScreen("CPU speed five choices")
        app.buttons["戻る"].tap()
        app.buttons["homeMenu"].tap(); app.buttons["設定"].tap(); app.buttons["サウンド"].tap()
        let sound = app.switches["soundEnabled"]
        XCTAssertTrue(sound.waitForExistence(timeout: 5))
        XCTAssertEqual(sound.value as? String, "1")
        // SwiftUI exposes the whole labelled row as a Switch. Tap its visible trailing control.
        let thumb = sound.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5))
        thumb.tap()
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == '0'"), object: sound)], timeout: 5), .completed)
        thumb.tap()
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == '1'"), object: sound)], timeout: 5), .completed)
        attachScreen("Sound ambient policy")
        app.buttons["戻る"].tap()
        app.buttons["playPC1245"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        home(app)
        app.buttons["homeMenu"].tap(); app.buttons["設定"].tap(); app.buttons["CPU速度"].tap()
        app.buttons["0.8倍"].tap(); app.buttons["戻る"].tap()
        app.buttons["playPC1245"].tap(); waitFor("PAUSED", in: app)
    }

    func testControllerSettingsAndNativeMenuDismissal() {
        let app = launchProbe("--navigation-probe", home: true)
        app.buttons["homeMenu"].tap(); app.buttons["設定"].tap(); app.buttons["コントローラ設定"].tap()
        let enabled = app.switches["controllerEnabled"]
        XCTAssertTrue(enabled.waitForExistence(timeout: 5))
        XCTAssertEqual(enabled.value as? String, "0")
        enabled.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == '1'"), object: enabled)], timeout: 5), .completed)
        app.swipeUp()
        let binding = app.buttons["binding_LEFT"]
        XCTAssertTrue(binding.waitForExistence(timeout: 5))
        if !binding.isHittable { app.swipeUp() }
        binding.tap()
        let key = app.buttons["assign_A"]
        for _ in 0..<10 {
            if key.exists && key.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(key.isHittable, app.debugDescription); key.tap()
        XCTAssertTrue(binding.waitForExistence(timeout: 5))
        XCTAssertTrue(binding.label.contains("A"), binding.label)
        attachScreen("Controller assignment and disconnected status")
        app.buttons["戻る"].tap()
        app.buttons["playPC1245"].tap(); waitFor("RUNNING", in: app)
        app.buttons["playerMenu"].tap()
        XCTAssertTrue(app.buttons["エミュレータ操作"].waitForExistence(timeout: 5))
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.8)).tap()
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: app.buttons["エミュレータ操作"])], timeout: 5), .completed)
        playerAction(app, "エミュレータ操作", "Pause"); waitFor("PAUSED", in: app)
        playerAction(app, "エミュレータ操作", "Run"); waitFor("RUNNING", in: app)
        home(app)
        app.buttons["playPC1245"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
    }

    private func launchProbe(_ argument: String = "--structural-probe", home: Bool = false) -> XCUIApplication {
        XCUIDevice.shared.orientation = .landscapeLeft
        let app = XCUIApplication()
        app.launchArguments = [argument]
        app.launch()
        XCTAssertTrue(app.buttons[home ? "playPC1245" : "runState"].waitForExistence(timeout: 10))
        rotateAndSettle(.landscapeLeft)
        return app
    }
    private func playerAction(_ app: XCUIApplication, _ group: String, _ action: String) {
        app.buttons["playerMenu"].tap()
        app.buttons[group].tap()
        app.buttons[action].tap()
    }
    private func home(_ app: XCUIApplication) {
        app.buttons["playerMenu"].tap()
        app.buttons["ホームへ戻る"].tap()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 5))
    }
    private func openRomManagement(_ app: XCUIApplication) {
        app.buttons["homeMenu"].tap(); app.buttons["設定"].tap(); app.buttons["ROM管理"].tap()
        XCTAssertTrue(app.buttons["romImportManager"].waitForExistence(timeout: 5))
    }
    private func saveLocalExport(_ app: XCUIApplication) {
        let save = app.buttons.matching(NSPredicate(format: "label IN %@", ["Export", "Save", "書き出す", "保存"])).firstMatch
        XCTAssertTrue(save.waitForExistence(timeout: 10), app.debugDescription)
        if app.buttons["On My iPhone"].exists { app.buttons["On My iPhone"].tap() }
        let folder = app.cells.containing(NSPredicate(format: "label CONTAINS %@", "Pokecom GO Player")).firstMatch
        if folder.exists { folder.tap() }
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate(format: "enabled == true"), object: save)
        XCTAssertEqual(XCTWaiter.wait(for: [ready], timeout: 15), .completed)
        attachScreen("Local export destination")
        save.tap()
        let replace = app.alerts.buttons.matching(NSPredicate(format: "label IN %@", ["Replace", "置き換える", "置き換え"])).firstMatch
        if replace.waitForExistence(timeout: 2) { replace.tap() }
    }
    private func cancelPicker(_ app: XCUIApplication) {
        if #unavailable(iOS 26) {
            let cancel = app.buttons.matching(NSPredicate(format: "label IN %@", ["Cancel", "キャンセル"])).firstMatch
            XCTAssertTrue(cancel.waitForExistence(timeout: 10), app.debugDescription)
            cancel.tap()
            return
        }
        // Fixtures always start in this app's local Documents (set by the native picker).
        // iOS 26 File Provider exposes duplicate/stale Cancel AX nodes, even reporting
        // visible native buttons as non-hittable. The captured picker screenshots show
        // Back and the root close X in the same navigation slot on the test iPhones.
        // Navigate Documents -> On My iPhone -> Browse -> Close using that visible slot.
        // Callers verify the resulting cancellation message and CPU intent separately.
        let local = app.cells.matching(NSPredicate(format: "label CONTAINS %@", "picker-test.bas")).firstMatch
        XCTAssertTrue(local.waitForExistence(timeout: 15), app.debugDescription)
        let navigationSlot = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
            .withOffset(CGVector(dx: 60, dy: 46))
        attachScreen("Standard picker before cancellation navigation")
        navigationSlot.tap()
        navigationSlot.tap()
        navigationSlot.tap()
    }
    private func localFile(_ app: XCUIApplication, _ name: String) -> XCUIElement {
        let file = app.cells.matching(NSPredicate(format: "label CONTAINS %@", name)).firstMatch
        XCTAssertTrue(file.waitForExistence(timeout: 10), app.debugDescription)
        XCTAssertTrue(file.isEnabled)
        return file
    }
    private func waitFor(_ state: String, in app: XCUIApplication) {
        expectLabel(app.buttons["runState"], contains: state)
    }
    private func expectLabel(_ element: XCUIElement, contains text: String) {
        let result = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label CONTAINS %@", text), object: element)
        XCTAssertEqual(XCTWaiter.wait(for: [result], timeout: 10), .completed)
    }
    private func attachScreen(_ title: String) {
        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = title; screenshot.lifetime = .keepAlways; add(screenshot)
    }
    private func rotateAndSettle(_ orientation: UIDeviceOrientation) {
        XCUIDevice.shared.orientation = orientation
        let settled = expectation(description: "UIKit rotation settled")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { settled.fulfill() }
        wait(for: [settled], timeout: 3)
    }
}
