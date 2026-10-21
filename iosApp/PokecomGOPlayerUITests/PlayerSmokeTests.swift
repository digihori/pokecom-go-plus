import XCTest

final class PlayerSmokeTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func testHomePortraitTilesListPersistenceAndLandscapeOnlyPlayer() {
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        XCUIDevice.shared.orientation = .portrait
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        expectOrientation(app, portrait: true)
        XCTAssertEqual(app.buttons["homeLayout-tiles"].value as? String, "選択中")
        let first = app.buttons["playPC1245"], second = app.buttons["play-pc-1251"]
        XCTAssertEqual(first.frame.midY, second.frame.midY, accuracy: 30)
        XCTAssertLessThan(first.frame.minX, second.frame.minX)
        attachScreen("Home portrait tiles")
        app.buttons["homeLayout-list"].tap()
        XCTAssertEqual(first.frame.minX, second.frame.minX, accuracy: 2)
        XCTAssertLessThan(first.frame.maxY, second.frame.minY)
        attachScreen("Home portrait list")
        app.terminate(); app.launch()
        XCTAssertTrue(app.buttons["homeLayout-list"].waitForExistence(timeout: 15))
        XCTAssertEqual(app.buttons["homeLayout-list"].value as? String, "選択中")
        for _ in 0..<2 {
            openRomManagement(app)
            expectOrientation(app, portrait: true)
            app.buttons["閉じる"].tap()
            XCTAssertTrue(app.buttons["homeMenu"].isHittable)
        }
        attachScreen("Home menu after ROM management light")
        app.buttons["homeMenu"].tap(); app.buttons["設定"].tap(); app.buttons["CPU速度"].tap()
        expectOrientation(app, portrait: true)
        app.buttons["戻る"].tap()
        app.buttons["playPC1245"].tap(); waitFor("PAUSED", in: app)
        // Opening ROM management deliberately preserves a paused entry intent.
        app.buttons["runState"].tap(); waitFor("RUNNING", in: app)
        expectOrientation(app, portrait: false)
        rotateAndSettle(.landscapeRight); expectOrientation(app, portrait: false)
        rotateAndSettle(.portrait); expectOrientation(app, portrait: false)
        playerAction(app, "プログラム", "セーブ")
        expectOrientation(app, portrait: true)
        attachScreen("Program panel portrait from landscape player")
        app.buttons["閉じる"].tap(); expectOrientation(app, portrait: false)
        home(app); expectOrientation(app, portrait: true)
        app.buttons["homeLayout-tiles"].tap()
        rotateAndSettle(.landscapeLeft); expectOrientation(app, portrait: false)
        XCTAssertLessThan(app.buttons["playPC1245"].frame.minX, app.buttons["play-pc-1251"].frame.minX)
        attachScreen("Home landscape tiles")
    }

    func testHomeMenuSurvivesRomManagementInDarkAppearance() {
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        XCUIDevice.shared.orientation = .portrait
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        for _ in 0..<3 {
            openRomManagement(app)
            app.buttons["閉じる"].tap()
            XCTAssertTrue(app.buttons["homeMenu"].isHittable)
        }
        attachScreen("Home menu after ROM management dark")
        app.buttons["homeMenu"].tap(); app.buttons["ヘルプ・情報"].tap(); app.buttons["Playerとは"].tap()
        expectOrientation(app, portrait: true)
        app.buttons["戻る"].tap()
    }

    private func expectOrientation(_ app: XCUIApplication, portrait: Bool) {
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            portrait ? app.frame.height > app.frame.width : app.frame.width > app.frame.height
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [ready], timeout: 10), .completed)
    }

    func testRunPauseResetRotationAndBackgroundReturn() {
        let app = launchProbe()
        waitFor("PAUSED", in: app)
        app.buttons["runState"].tap(); waitFor("RUNNING", in: app)
        app.buttons["runState"].tap(); waitFor("PAUSED", in: app)
        app.buttons["pro"].tap()
        XCTAssertEqual(app.buttons["pro"].label, "PRO")
        XCTAssertEqual(app.staticTexts["symbol_PRO"].value as? String, "点灯")
        XCTAssertLessThan(app.staticTexts["symbol_RUN"].frame.minX, app.staticTexts["symbol_PRO"].frame.minX)
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

    func testPc125xSelectionSwitchCancelModesAndProcessRestoration() {
        XCUIDevice.shared.orientation = .landscapeLeft
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        func select(_ name: String) {
            let picker = app.buttons["model-pc-1251"]
            for _ in 0..<3 { if picker.isHittable { break }; app.swipeUp() }
            XCTAssertTrue(picker.isHittable, app.debugDescription)
            picker.tap(); app.buttons[name].tap()
        }
        select("PC-1250")
        app.buttons["play-pc-1251"].tap()
        XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 5))
        app.alerts.buttons["キャンセル"].tap()
        app.buttons["play-pc-1251"].tap(); app.alerts.buttons["切替"].tap()
        waitFor("RUNNING", in: app)
        XCTAssertTrue(app.staticTexts["PC-1250"].exists)
        app.buttons["pro"].tap(); XCTAssertEqual(app.buttons["pro"].label, "PRO")
        app.buttons["pro"].tap(); XCTAssertEqual(app.buttons["pro"].label, "RUN")
        playerAction(app, "動作モード", "RSV")
        XCTAssertEqual(app.buttons["pro"].label, "RUN")
        XCTAssertEqual(app.staticTexts["symbol_RESERVE"].value as? String, "点灯")
        playerAction(app, "エミュレータ操作", "Reset")
        XCTAssertEqual(app.buttons["pro"].label, "RUN")
        playerAction(app, "表示", "Controller Display")
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
        rotateAndSettle(.landscapeRight)
        XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
        playerAction(app, "表示", "通常表示")
        XCTAssertTrue(app.buttons["key_BREAK"].exists)
        attachScreen("PC-1250 fixed mode symbols")
        home(app); select("PC-1255")
        app.terminate(); app.launch()
        XCTAssertTrue(app.staticTexts["起動中：PC-1250"].waitForExistence(timeout: 15))
        let picker = app.buttons["model-pc-1251"]
        for _ in 0..<3 { if picker.isHittable { break }; app.swipeUp() }
        XCTAssertTrue(picker.label.contains("PC-1255"), app.debugDescription)
        for name in ["PC-1255", "PC-1251"] {
            select(name)
            app.buttons["play-pc-1251"].tap(); app.alerts.buttons["切替"].tap()
            waitFor("RUNNING", in: app)
            XCTAssertTrue(app.staticTexts[name].exists)
            app.buttons["key_BREAK"].press(forDuration: 0.2)
            XCUIDevice.shared.press(.home); app.activate(); waitFor("PAUSED", in: app)
            home(app)
        }
    }

    func testPc126xTwoRowLcdModesSwitchAndRestore() {
        XCUIDevice.shared.orientation = .landscapeLeft
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        rotateAndSettle(.landscapeLeft)
        func select(_ name: String) {
            let picker = app.buttons["model-pc-1261"]
            revealInMachineList(picker, app: app)
            picker.tap()
            let choice = app.buttons[name]
            XCTAssertTrue(choice.waitForExistence(timeout: 5), app.debugDescription)
            choice.tap()
        }
        select("PC-1260")
        app.buttons["play-pc-1261"].tap()
        XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 5))
        app.alerts.buttons["キャンセル"].tap()
        app.buttons["play-pc-1261"].tap(); app.alerts.buttons["切替"].tap()
        waitFor("RUNNING", in: app)
        XCTAssertTrue(app.staticTexts["PC-1260"].exists)
        XCTAssertTrue(app.staticTexts["lcd"].label.contains("24桁×2行"))
        expectLabel(app.staticTexts["symbol_KANA"], contains: "KANA")
        for id in ["symbol_KANA", "symbol_SMALL", "symbol_SHIFT", "symbol_ERROR"] {
            XCTAssertEqual(app.staticTexts[id].value as? String, "点灯")
        }
        app.buttons["pro"].tap(); XCTAssertEqual(app.buttons["pro"].label, "PRO")
        XCTAssertEqual(app.staticTexts["symbol_PRO"].value as? String, "点灯")
        app.buttons["pro"].tap(); XCTAssertEqual(app.buttons["pro"].label, "RUN")
        playerAction(app, "動作モード", "RSV")
        XCTAssertEqual(app.buttons["pro"].label, "RUN")
        XCTAssertEqual(app.staticTexts["symbol_RESERVE"].value as? String, "点灯")
        app.buttons["key_BREAK"].press(forDuration: 0.2)
        attachScreen("PC-1260 two-row LCD and RSV")
        playerAction(app, "表示", "Controller Display")
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
        rotateAndSettle(.landscapeRight)
        XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
        attachScreen("PC-1260 two-row Controller Display")
        playerAction(app, "表示", "通常表示")
        home(app); select("PC-1261")
        app.terminate(); app.launch()
        XCTAssertTrue(app.staticTexts["起動中：PC-1260"].waitForExistence(timeout: 15))
        select("PC-1261")
        app.buttons["play-pc-1261"].tap(); app.alerts.buttons["切替"].tap()
        waitFor("RUNNING", in: app)
        XCTAssertTrue(app.staticTexts["PC-1261"].exists)
        XCUIDevice.shared.press(.home); app.activate(); waitFor("PAUSED", in: app)
        playerAction(app, "エミュレータ操作", "Reset")
        waitFor("RUNNING", in: app); XCTAssertEqual(app.buttons["pro"].label, "RUN")
    }

    func testPc1350FourRowsModeKeyBothShiftsAndControllerDisplay() {
        XCUIDevice.shared.orientation = .landscapeLeft
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        rotateAndSettle(.landscapeLeft)
        let play = app.buttons["play-pc-1350"]
        revealInMachineList(play, app: app)
        play.tap(); app.alerts.buttons["切替"].tap(); waitFor("RUNNING", in: app)
        XCTAssertTrue(app.staticTexts["PC-1350"].exists)
        XCTAssertFalse(app.buttons["pro"].exists)
        XCTAssertTrue(app.staticTexts["lcd"].label.contains("25桁×4行"))
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 62)
        for id in ["key_SHIFT_LEFT", "key_SHIFT_RIGHT", "key_MODE", "key_KANA", "key_INSERT", "key_DELETE"] {
            XCTAssertTrue(app.buttons[id].isHittable, id)
            app.buttons[id].press(forDuration: 0.15)
            XCTAssertEqual(app.buttons[id].value as? String, "解放")
        }
        for id in ["symbol_RUN", "symbol_PRO", "symbol_KANA", "symbol_SMALL", "symbol_SHIFT", "symbol_DEF"] {
            XCTAssertEqual(app.staticTexts[id].value as? String, "点灯")
        }
        attachScreen("PC-1350 full four-row display")
        app.buttons["playerMenu"].tap(); XCTAssertFalse(app.buttons["動作モード"].exists)
        app.buttons["表示"].tap(); app.buttons["Controller Display"].tap()
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
        rotateAndSettle(.landscapeRight)
        XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
        attachScreen("PC-1350 Controller Display")
        playerAction(app, "表示", "通常表示")
        XCUIDevice.shared.press(.home); app.activate(); waitFor("PAUSED", in: app)
        playerAction(app, "エミュレータ操作", "Reset"); waitFor("RUNNING", in: app)
        home(app); app.terminate(); app.launch()
        XCTAssertTrue(app.staticTexts["起動中：PC-1350"].waitForExistence(timeout: 15))
    }

    func testPc140xAnd1450ScientificKeysSymbolsSwitchAndRestore() {
        XCUIDevice.shared.orientation = .landscapeLeft
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        rotateAndSettle(.landscapeLeft)
        func select(_ name: String) {
            let picker = app.buttons["model-pc-1401"]
            revealInMachineList(picker, app: app); picker.tap()
            let choice = app.buttons[name]
            XCTAssertTrue(choice.waitForExistence(timeout: 5)); choice.tap()
        }
        select("PC-1402")
        app.buttons["play-pc-1401"].tap(); app.alerts.buttons["キャンセル"].tap()
        app.buttons["play-pc-1401"].tap(); app.alerts.buttons["切替"].tap()
        waitFor("RUNNING", in: app)
        for name in ["PC-1402", "PC-1401", "PC-1450"] {
            XCTAssertTrue(app.staticTexts[name].exists)
            XCTAssertFalse(app.buttons["pro"].exists)
            XCTAssertTrue(app.staticTexts["lcd"].label.contains("16桁×1行"))
            for key in ["CAL", "BASIC", "SIN", "COS", "TAN", "LEFT_PAREN", "RIGHT_PAREN"] {
                XCTAssertTrue(app.buttons["key_" + key].isHittable, key)
                app.buttons["key_" + key].press(forDuration: 0.1)
                XCTAssertEqual(app.buttons["key_" + key].value as? String, "解放")
            }
            if name == "PC-1450" {
                for key in ["KANA", "SMALL", "DAKUTEN", "LONG_VOWEL", "LOCK"] {
                    app.buttons["key_" + key].press(forDuration: 0.1)
                    XCTAssertEqual(app.buttons["key_" + key].value as? String, "解放")
                }
            }
            for symbol in ["CAL", "RUN", "PRO", "HYP", "STAT", "PRINT", "ERROR"] {
                XCTAssertEqual(app.staticTexts["symbol_" + symbol].value as? String, "点灯")
            }
            app.buttons["playerMenu"].tap()
            XCTAssertFalse(app.buttons["動作モード"].exists)
            app.buttons["表示"].tap(); app.buttons["Controller Display"].tap()
            playerAction(app, "表示", "通常表示")
            attachScreen(name + " scientific keys and LCD symbols")
            playerAction(app, "表示", "Controller Display")
            XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
            rotateAndSettle(.landscapeRight)
            XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
            attachScreen(name + " Controller Display")
            playerAction(app, "表示", "通常表示")
            XCUIDevice.shared.press(.home); app.activate(); waitFor("PAUSED", in: app)
            playerAction(app, "エミュレータ操作", "Reset"); waitFor("RUNNING", in: app)
            home(app)
            if name == "PC-1402" {
                select("PC-1401"); app.terminate(); app.launch()
                XCTAssertTrue(app.staticTexts["起動中：PC-1402"].waitForExistence(timeout: 15))
                select("PC-1401"); app.buttons["play-pc-1401"].tap(); app.alerts.buttons["切替"].tap()
            } else if name == "PC-1401" {
                let play = app.buttons["play-pc-1450"]
                revealInMachineList(play, app: app); play.tap(); app.alerts.buttons["切替"].tap()
            }
            if name != "PC-1450" { waitFor("RUNNING", in: app) }
        }
        app.terminate(); app.launch()
        XCTAssertTrue(app.staticTexts["起動中：PC-1450"].waitForExistence(timeout: 15))
    }

    func testPc1401DisplayAfterRelaunch() {
        XCUIDevice.shared.orientation = .landscapeLeft
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        rotateAndSettle(.landscapeLeft)
        let play = app.buttons["play-pc-1401"]
        revealInMachineList(play, app: app); play.tap(); app.alerts.buttons["切替"].tap()
        waitFor("RUNNING", in: app)
        home(app); app.terminate(); app.launch()
        XCTAssertTrue(app.staticTexts["起動中：PC-1401"].waitForExistence(timeout: 15))
        func synchronize(_ orientation: UIDeviceOrientation) {
            // Relaunch can leave XCTest's screenshot orientation behind the app's landscape window.
            XCUIDevice.shared.orientation = .portrait
            rotateAndSettle(orientation == .landscapeLeft ? .landscapeRight : .landscapeLeft)
            rotateAndSettle(orientation)
        }
        synchronize(.landscapeLeft)
        revealInMachineList(play, app: app); play.tap(); waitFor("RUNNING", in: app)
        XCTAssertFalse(app.buttons["pro"].exists)
        app.buttons["playerMenu"].tap(); XCTAssertFalse(app.buttons["動作モード"].exists)
        app.buttons["表示"].tap(); app.buttons["Controller Display"].tap()
        playerAction(app, "表示", "通常表示")
        synchronize(.landscapeLeft)
        for key in ["CAL", "BASIC", "SIN", "NUM_7", "ENTER"] { XCTAssertTrue(app.buttons["key_" + key].isHittable) }
        let display = app.staticTexts["lcd"]
        XCTAssertTrue(display.label.contains("16桁×1行"))
        XCTAssertGreaterThan(display.frame.width, 200); XCTAssertGreaterThan(display.frame.height, 10)
        XCTAssertTrue(app.frame.contains(display.frame))
        attachScreen("PC-1401 full display after orientation synchronization")
        playerAction(app, "表示", "Controller Display")
        synchronize(.landscapeRight)
        XCTAssertTrue(app.frame.contains(display.frame)); XCTAssertGreaterThan(display.frame.width, 200)
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
        attachScreen("PC-1401 Controller Display after orientation synchronization")
    }

    func testPc1360kKanjiPlayer() {
        checkBankedMachine("pc-1360k", columns: 25, rows: 4, count: 65,
            keys: ["MODE", "KANJI", "HIRA_KATA", "YJ", "KANJI_FIX", "KANJI_CONVERT", "SHIFT_LEFT"],
            symbols: ["RUN", "PRO", "HIRAGANA", "KATAKANA", "S", "SMALL", "SHIFT", "DEF"])
    }
    func testPc1360BankedFourRowPlayer() {
        checkBankedMachine("pc-1360", columns: 25, rows: 4, count: 60,
            keys: ["MODE", "KANA", "SMALL", "INSERT", "DELETE", "SHIFT_LEFT"], symbols: ["RUN", "PRO", "KANA", "SMALL", "SHIFT", "DEF"])
    }
    func testPc1460BankedScientificPlayer() {
        checkBankedMachine("pc-1460", columns: 24, rows: 1, count: 81,
            keys: ["CAL", "BASIC", "KANA", "SMALL", "LOCK", "SIN"], symbols: ["CAL", "RUN", "PRO", "MATRIX", "STAT", "PRINT"])
    }
    func testPc1470uBankedTwoRowPlayer() {
        checkBankedMachine("pc-1470u", columns: 24, rows: 2, count: 81,
            keys: ["CAL", "BASIC", "LOCK", "SHIFT", "MEMORY_STORE", "MEMORY_RECALL", "MEMORY_ADD", "STAT"], symbols: ["CAL", "AER", "RUN", "PRO", "MATRIX", "DBL", "BATTERY"])
    }
    private func checkBankedMachine(_ id: String, columns: Int, rows: Int, count: Int, keys: [String], symbols: [String]) {
        let app = XCUIApplication()
        app.launchArguments = ["--series-probe"]
        app.launchEnvironment["PGP_SERIES_PROBE_ID"] = UUID().uuidString
        app.launch()
        XCTAssertTrue(app.buttons["playPC1245"].waitForExistence(timeout: 15))
        func synchronize(_ orientation: UIDeviceOrientation) {
            XCUIDevice.shared.orientation = .portrait
            rotateAndSettle(orientation == .landscapeLeft ? .landscapeRight : .landscapeLeft)
            rotateAndSettle(orientation)
        }
        synchronize(.landscapeLeft)
        let play = app.buttons["play-" + id]
        revealInMachineList(play, app: app); play.tap(); app.alerts.buttons["切替"].tap(); waitFor("RUNNING", in: app)
        XCTAssertTrue(app.staticTexts["lcd"].label.contains("\(columns)桁×\(rows)行"))
        XCTAssertFalse(app.buttons["pro"].exists)
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, count)
        for key in keys {
            XCTAssertTrue(app.buttons["key_" + key].isHittable, key)
            app.buttons["key_" + key].press(forDuration: 0.1)
            XCTAssertEqual(app.buttons["key_" + key].value as? String, "解放")
        }
        for symbol in symbols { XCTAssertEqual(app.staticTexts["symbol_" + symbol].value as? String, "点灯") }
        attachScreen(id + " full display")
        app.buttons["playerMenu"].tap(); XCTAssertFalse(app.buttons["動作モード"].exists)
        app.buttons["表示"].tap(); app.buttons["Controller Display"].tap()
        synchronize(.landscapeRight)
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
        XCTAssertGreaterThan(app.staticTexts["lcd"].frame.width, 200)
        XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
        attachScreen(id + " Controller Display")
        playerAction(app, "表示", "通常表示")
        XCUIDevice.shared.press(.home); app.activate(); waitFor("PAUSED", in: app)
        playerAction(app, "エミュレータ操作", "Reset"); waitFor("RUNNING", in: app)
        home(app); app.terminate(); app.launch()
        XCTAssertTrue(app.staticTexts["起動中：" + id.uppercased()].waitForExistence(timeout: 15))
    }

    private func revealInMachineList(_ element: XCUIElement, app: XCUIApplication) {
        XCTAssertTrue(element.waitForExistence(timeout: 10))
        let scroll = app.scrollViews.firstMatch
        for _ in 0..<24 {
            let frame = element.frame
            if element.isHittable && frame.minY >= app.frame.minY + 90 && frame.maxY <= app.frame.maxY - 60 { return }
            // A fixed flick can jump across the visible range repeatedly on a tall iPhone.
            // Move toward the target by its measured distance and stop before releasing.
            let distance = max(-scroll.frame.height * 0.25,
                               min(scroll.frame.height * 0.25, frame.midY - app.frame.midY))
            let start = scroll.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.6))
            let end = start.withOffset(CGVector(dx: 0, dy: -distance))
            start.press(forDuration: 0.05, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 0.2)
        }
        XCTAssertTrue(element.isHittable, app.debugDescription)
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
        // Scientific skins also expose a CAL key; select the native menu action explicitly.
        app.buttons.matching(NSPredicate(format: "(label == %@ OR identifier == %@) AND NOT (identifier BEGINSWITH 'key_')", action, action)).firstMatch.tap()
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
        let fixture = app.cells.matching(NSPredicate(format: "label CONTAINS %@", "picker-test.bas")).firstMatch
        // iOS 18.5 may show Documents while Save remains disabled until a
        // destination is explicitly selected through the local-storage browser.
        if !fixture.waitForExistence(timeout: 3) || !save.isEnabled {
            let local = app.buttons.matching(NSPredicate(format: "label IN %@", ["On My iPhone", "このiPhone内"])).firstMatch
            if local.exists { local.tap() }
            let folder = app.cells.containing(NSPredicate(format: "label CONTAINS %@", "Pokecom GO Player")).firstMatch
            XCTAssertTrue(folder.waitForExistence(timeout: 5), app.debugDescription)
            folder.tap()
        }
        // Folder navigation is asynchronous; an enabled Save may still belong to its parent.
        XCTAssertTrue(fixture.waitForExistence(timeout: 10), app.debugDescription)
        let ready = XCTNSPredicateExpectation(predicate: NSPredicate(format: "enabled == true"), object: save)
        XCTAssertEqual(XCTWaiter.wait(for: [ready], timeout: 15), .completed, app.debugDescription)
        attachScreen("Local export destination")
        save.tap()
        let replace = app.alerts.buttons.matching(NSPredicate(format: "label IN %@", ["Replace", "置き換える", "置き換え"])).firstMatch
        if replace.waitForExistence(timeout: 2) { replace.tap() }
    }
    private func cancelPicker(_ app: XCUIApplication) {
        // Export pickers on iOS 18.5 also expose Cancel as a stale Other node,
        // overlapping More, rather than a tappable Button. Do not key this on OS version.
        let local = app.cells.matching(NSPredicate(format: "label CONTAINS %@", "picker-test.bas")).firstMatch
        XCTAssertTrue(local.waitForExistence(timeout: 15), app.debugDescription)
        for step in 0..<3 {
            let cancel = app.buttons.matching(NSPredicate(format: "label IN %@", ["Cancel", "キャンセル"])).firstMatch
            if cancel.exists && cancel.isHittable {
                cancel.tap()
                return
            }
            // Documents -> On My iPhone -> Browse -> Close. Re-read the real bar
            // each time: its top is 0 on iOS 18.5 and 24 on the iOS 26 test iPhone.
            let bar = app.navigationBars["FullDocumentManagerViewControllerNavigationBar"]
            XCTAssertTrue(bar.waitForExistence(timeout: 5), app.debugDescription)
            let frame = bar.frame
            XCTAssertGreaterThanOrEqual(frame.height, 44, app.debugDescription)
            attachScreen("Picker cancellation navigation \(step)")
            app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
                .withOffset(CGVector(dx: frame.minX + 60 - app.frame.minX,
                                     dy: frame.minY + 22 - app.frame.minY)).tap()
        }
        // Every caller asserts the cancellation result and restored CPU intent.
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
        // Accessibility can settle before the native menu's closing animation and skin redraw.
        let drawn = expectation(description: "Skin redraw before screenshot")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { drawn.fulfill() }
        wait(for: [drawn], timeout: 3)
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
