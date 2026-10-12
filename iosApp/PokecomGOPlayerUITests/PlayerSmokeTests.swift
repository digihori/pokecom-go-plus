import XCTest

final class PlayerSmokeTests: XCTestCase {
    func testRunPauseResetRotationAndBackgroundReturn() {
        let app = XCUIApplication()
        app.launchArguments = ["--structural-probe"]
        app.launch()
        XCTAssertTrue(app.buttons["run"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.staticTexts["runState"].label, "PAUSED")
        app.buttons["run"].tap()
        waitFor("RUNNING", in: app)
        app.buttons["pause"].tap()
        waitFor("PAUSED", in: app)
        app.buttons["pro"].tap()
        XCTAssertEqual(app.staticTexts["operatingMode"].label, "PRO")
        app.buttons["reset"].tap()
        waitFor("RUNNING", in: app)
        XCTAssertEqual(app.staticTexts["operatingMode"].label, "RUN")
        // Keep accessibility geometry stable while exercising both orientations on older simulators.
        app.buttons["pause"].tap()
        waitFor("PAUSED", in: app)
        rotateAndSettle(.landscapeRight)
        XCTAssertTrue(app.buttons["reset"].isHittable)
        XCTAssertGreaterThan(app.frame.width, app.frame.height)
        rotateAndSettle(.landscapeLeft)
        XCTAssertTrue(app.buttons["reset"].isHittable)
        XCTAssertGreaterThan(app.frame.width, app.frame.height)
        rotateAndSettle(.portrait)
        XCTAssertTrue(app.buttons["reset"].isHittable)
        XCTAssertGreaterThan(app.frame.width, app.frame.height)
        app.buttons["run"].tap()
        waitFor("RUNNING", in: app)
        XCUIDevice.shared.press(.home)
        app.activate()
        waitFor("PAUSED", in: app)
        app.buttons["key_BREAK"].press(forDuration: 0.3)
        XCTAssertEqual(app.buttons["key_BREAK"].value as? String, "解放")
        // Full-device screenshots avoid XCUI app-crop artifacts when the physical device is
        // portrait while the app's supported interface remains landscape.
        rotateAndSettle(.landscapeRight)
        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = "PC-1245 full device"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    func testControllerDisplayKeepsLcdAndAvoidsNotchInBothDirections() {
        let app = XCUIApplication()
        app.launchArguments = ["--structural-probe"]
        app.launch()
        XCTAssertTrue(app.buttons["presentation"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 52)
        app.buttons["run"].tap()
        waitFor("RUNNING", in: app)
        app.buttons["pause"].tap()
        waitFor("PAUSED", in: app)
        XCTAssertEqual(app.staticTexts["symbol_SHIFT"].value as? String, "点灯")
        for orientation in [UIDeviceOrientation.landscapeRight, .landscapeLeft] {
            rotateAndSettle(orientation)
            for _ in 0..<2 {
                app.buttons["presentation"].tap()
                XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 0)
                XCTAssertTrue(app.staticTexts["lcd"].exists)
                XCTAssertTrue(app.frame.contains(app.staticTexts["lcd"].frame))
                XCTAssertTrue(app.frame.contains(app.buttons["presentation"].frame))
                let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
                screenshot.name = "PC-1245 Controller Display"
                screenshot.lifetime = .keepAlways
                add(screenshot)
                app.buttons["presentation"].tap()
                XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'key_' ")).count, 52)
                app.buttons["key_ENTER"].tap()
                XCTAssertEqual(app.buttons["key_ENTER"].value as? String, "解放")
            }
        }
    }

    private func waitFor(_ state: String, in app: XCUIApplication) {
        let predicate = NSPredicate(format: "label == %@", state)
        expectation(for: predicate, evaluatedWith: app.staticTexts["runState"])
        waitForExpectations(timeout: 5)
    }

    private func rotateAndSettle(_ orientation: UIDeviceOrientation) {
        XCUIDevice.shared.orientation = orientation
        // XCUI reports logical orientation before UIKit's visual rotation finishes. Without
        // settling, a rapid second flip can invalidate hit points or send taps into animation.
        let settled = expectation(description: "UIKit rotation settled")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { settled.fulfill() }
        wait(for: [settled], timeout: 3)
    }
}
