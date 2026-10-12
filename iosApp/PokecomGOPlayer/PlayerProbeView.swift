import SwiftUI
import UIKit
import PgpPlayer
import UniformTypeIdentifiers

/// Skin screen; the complete hierarchical menu and ROM registration follow in later stages.
struct PlayerProbeView: View {
    @ObservedObject var host: PlayerHost
    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Text(host.status.runState).font(.caption.monospaced())
                        .lineLimit(1).minimumScaleFactor(0.6)
                        .accessibilityIdentifier("runState")
                    Spacer(minLength: 0)
                    Menu {
                        Button("機種／ROM", action: host.openRomPanel)
                        Button("バージョン情報") { showInformation = true }
                        Button("プライバシーポリシー（準備中）") { showInformation = true }
                        Button("Studio案内（準備中）") { showInformation = true }
                    } label: { Image(systemName: "ellipsis").frame(minWidth: 28, minHeight: 28) }
                        .accessibilityLabel("メニュー").accessibilityIdentifier("playerMenu")
                        .disabled(host.isBusy)
                    Button(action: host.run) { Image(systemName: "play.fill").frame(minWidth: 28, minHeight: 28) }
                        .accessibilityLabel("Run").accessibilityIdentifier("run")
                        .disabled(!host.hasSession || host.status.runState != "PAUSED")
                    Button(action: host.pause) { Image(systemName: "pause.fill").frame(minWidth: 28, minHeight: 28) }
                        .accessibilityLabel("Pause").accessibilityIdentifier("pause")
                    Button(action: host.reset) { Image(systemName: "arrow.counterclockwise").frame(minWidth: 28, minHeight: 28) }
                        .accessibilityLabel("Reset").accessibilityIdentifier("reset")
                    Button(host.status.operatingMode == "PRO" ? "RUN" : "PRO") {
                        host.setProgramMode(host.status.operatingMode != "PRO")
                    }.accessibilityIdentifier("pro")
                    Button(action: host.togglePresentation) {
                        Image(systemName: host.isControllerDisplay ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                            .frame(minWidth: 28, minHeight: 28)
                    }
                        .accessibilityLabel(host.isControllerDisplay ? "通常表示" : "Controller Display")
                        .accessibilityIdentifier("presentation")
                        .accessibilityValue(host.isControllerDisplay ? "Controller Display" : "通常表示")
                }
                .font(.caption).buttonStyle(.bordered).lineLimit(1).minimumScaleFactor(0.7)
                .padding(.horizontal, 8).frame(minHeight: 44)
                if let skin = host.skin, let lcd = host.lcd {
                    PlayerSkinSurface(host: host, skin: skin, lcd: lcd)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    VStack(spacing: 16) {
                        Text("PC-1245").font(.title2)
                        Text(host.romMessage).multilineTextAlignment(.center)
                        if host.isBusy { ProgressView() }
                        Button("ROMを取り込む", action: host.openRomPicker)
                            .buttonStyle(.borderedProminent).accessibilityIdentifier("romImport")
                            .disabled(host.isBusy)
                    }.padding().frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            // SwiftUI applies safe area once, including to the toolbar and native skin viewport.
            .onChange(of: geometry.size) { _ in host.releaseAllKeys() }
            .onChange(of: geometry.safeAreaInsets) { _ in host.releaseAllKeys() }
        }
        .background(Color.black.ignoresSafeArea())
        .foregroundStyle(.white)
        .statusBarHidden()
        .sheet(isPresented: $host.romPanelOpen) { RomLibraryPanel(host: host) }
        .sheet(isPresented: $showInformation) {
            VStack(spacing: 16) {
                Text("Pokecom GO Player")
                Text("\(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "") (\(Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""))")
                Text("プライバシーポリシーとStudio案内は準備中です。")
                Button("閉じる") { showInformation = false }
            }.padding()
        }
        .onChange(of: showInformation) { open in if open { host.releaseAllKeys() } }
        .onAppear { UIDevice.current.beginGeneratingDeviceOrientationNotifications() }
        .onDisappear {
            UIDevice.current.endGeneratingDeviceOrientationNotifications()
            host.releaseAllKeys()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIDevice.orientationDidChangeNotification)) { _ in
            host.releaseAllKeys() // 180-degree flips do not change viewport size.
        }
    }
    @State private var showInformation = false
}

private struct RomLibraryPanel: View {
    @ObservedObject var host: PlayerHost
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("PC-1245").font(.title2)
                    Text(host.romSourceName ?? (host.hasSavedRom ? "保存データを検証できません" : "未登録"))
                        .accessibilityIdentifier("romSourceName")
                    Text(host.romMessage).accessibilityIdentifier("romMessage")
                    if host.isBusy { ProgressView() }
                    if host.awaitingReplacement {
                        Button("置換して実行", role: .destructive, action: host.confirmRomInstall)
                            .accessibilityIdentifier("confirmRomReplacement")
                        Button("キャンセル", action: host.cancelRomChange)
                    } else if host.awaitingDeletion {
                        Button("削除する", role: .destructive, action: host.confirmRomDeletion)
                            .accessibilityIdentifier("confirmRomDeletion")
                        Button("キャンセル", action: host.cancelRomChange)
                    } else {
                        Button(host.hasSavedRom ? "ROMを置換" : "ROMを取り込む", action: host.openRomPicker)
                            .accessibilityIdentifier("romImportManager")
                        Button("保存ROMを削除", role: .destructive, action: host.requestRomDeletion)
                            .accessibilityIdentifier("deleteROM").disabled(!host.hasSavedRom)
                    }
                }.padding().disabled(host.isBusy)
            }
            .navigationTitle("機種／ROM")
            .toolbar { ToolbarItem(placement: .cancellationAction) {
                Button("閉じる", action: host.closeRomPanel).disabled(host.isBusy)
            } }
        }.navigationViewStyle(.stack)
        .interactiveDismissDisabled(host.isBusy)
        .sheet(isPresented: $host.pickerPresented) {
            RomDocumentPicker(onSelect: host.importRom, onCancel: host.cancelRomPicker)
        }
    }
}

private struct RomDocumentPicker: UIViewControllerRepresentable {
    let onSelect: (URL) -> Void
    let onCancel: () -> Void
    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }
    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        // All data types remain selectable; the host validates actual contents and machine.
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.data], asCopy: true)
        picker.allowsMultipleSelection = false
        picker.directoryURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        picker.delegate = context.coordinator
        return picker
    }
    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: RomDocumentPicker
        init(parent: RomDocumentPicker) { self.parent = parent }
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            if let url = urls.first { parent.onSelect(url) } else { parent.onCancel() }
        }
        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) { parent.onCancel() }
    }
}

private struct PlayerSkinSurface: UIViewRepresentable {
    let host: PlayerHost
    let skin: SkinDefinition
    let lcd: PlayerDisplayFrame
    func makeUIView(context: Context) -> PlayerSkinView {
        let view = PlayerSkinView(skin: skin)
        view.router.onPress = { [weak host] key, owner in host?.press(keyName: key, owner: owner) ?? false }
        view.router.onRelease = { [weak host] owner in host?.release(owner: owner) }
        return view
    }
    func updateUIView(_ view: PlayerSkinView, context: Context) {
        view.update(lcd: lcd, mode: host.presentationMode, operatingMode: host.status.operatingMode,
                    pressedKeys: host.pressedKeys, inputEpoch: host.inputEpoch, active: host.isActive)
    }
    static func dismantleUIView(_ view: PlayerSkinView, coordinator: ()) { view.router.cancel() }
}

extension SkinRect {
    var cgRect: CGRect { CGRect(x: CGFloat(x), y: CGFloat(y), width: CGFloat(width), height: CGFloat(height)) }
}

/// One token per finger; leaving the original key releases it and requires a new touch-down.
/// Geometry is shared with drawing. A second finger holding the same key owns an independent token.
@MainActor
final class SkinTouchRouter {
    let skin: SkinDefinition
    var placement: SkinPlacement?
    var enabled = true
    var onPress: (String, String) -> Bool = { _, _ in false }
    var onRelease: (String) -> Void = { _ in }
    private(set) var owners: [String: String] = [:]
    init(skin: SkinDefinition) { self.skin = skin }
    func begin(owner: String, point: CGPoint) {
        guard enabled, let p = placement, p.mode != .controllerDisplay,
              p.viewport.cgRect.contains(point) else { return }
        let local = CGPoint(x: CGFloat(p.skinX(screenX: Float(point.x))),
                            y: CGFloat(p.skinY(screenY: Float(point.y))))
        guard let key = skin.keyRegions.first(where: { $0.bounds.cgRect.contains(local) }) else { return }
        if onPress(key.key.name, owner) { owners[owner] = key.key.name }
    }
    func move(owner: String, point: CGPoint) {
        guard let name = owners[owner], let p = placement,
              let key = skin.keyRegions.first(where: { $0.key.name == name }) else { return }
        let local = CGPoint(x: CGFloat(p.skinX(screenX: Float(point.x))),
                            y: CGFloat(p.skinY(screenY: Float(point.y))))
        if !p.viewport.cgRect.contains(point) || !key.bounds.cgRect.contains(local) { end(owner: owner) }
    }
    func end(owner: String) {
        if owners.removeValue(forKey: owner) != nil { onRelease(owner) }
    }
    func cancel() {
        let tokens = Array(owners.keys)
        owners.removeAll()
        tokens.forEach(onRelease)
    }
    /// Host already released these inputs on pause/reset/rotation/display change.
    func discard() { owners.removeAll() }
}

@MainActor
private final class SkinKeyAccessibility: UIAccessibilityElement {
    var activate: () -> Bool = { false }
    override func accessibilityActivate() -> Bool { activate() }
}

@MainActor
final class PlayerSkinView: UIView {
    let skin: SkinDefinition
    let router: SkinTouchRouter
    private let image = UIImage(named: "skin_pc1245")
    private var lcd: PlayerDisplayFrame?
    private var mode: PgpPlayer.PresentationMode = .fullDevice
    private var operatingMode = "RUN"
    private var pressedKeys: Set<String> = []
    private var inputEpoch = -1
    private var touchOwners: [ObjectIdentifier: String] = [:]
    private var highlights: [String: CAShapeLayer] = [:]
    private var keyElements: [String: SkinKeyAccessibility] = [:]
    private var modeElement: UIAccessibilityElement?
    private var symbolElements: [String: UIAccessibilityElement] = [:]
    private var layoutSize = CGSize.zero
    private let activeColor = UIColor(red: 38/255, green: 53/255, blue: 44/255, alpha: 1)
    private let panelColor = UIColor(red: 183/255, green: 200/255, blue: 167/255, alpha: 1)

    init(skin: SkinDefinition) {
        self.skin = skin
        router = SkinTouchRouter(skin: skin)
        super.init(frame: .zero)
        isMultipleTouchEnabled = true
        isAccessibilityElement = false
        backgroundColor = .black
        clipsToBounds = true
        contentMode = .redraw
        for key in skin.keyRegions {
            let overlay = CAShapeLayer()
            overlay.fillColor = UIColor.white.cgColor
            overlay.opacity = 0
            layer.addSublayer(overlay)
            highlights[key.key.name] = overlay
        }
    }
    required init?(coder: NSCoder) { fatalError("Use init(skin:)") }

    func update(lcd: PlayerDisplayFrame, mode: PgpPlayer.PresentationMode, operatingMode: String,
                pressedKeys: Set<String>, inputEpoch: Int, active: Bool) {
        let changedMode = self.mode != mode
        if self.inputEpoch != inputEpoch {
            router.discard()
            touchOwners.removeAll()
            self.inputEpoch = inputEpoch
        }
        self.lcd = lcd
        self.operatingMode = operatingMode
        self.pressedKeys = pressedKeys
        self.mode = mode
        router.enabled = active && mode != .controllerDisplay
        if changedMode { layoutSize = .zero; setNeedsLayout() }
        updateHighlights()
        updateAccessibilityValues()
        setNeedsDisplay()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard bounds.width > 0, bounds.height > 0, layoutSize != bounds.size else { return }
        if !router.owners.isEmpty {
            let oldTokens = Array(router.owners.keys)
            router.discard()
            touchOwners.removeAll()
            // Avoid publishing SwiftUI state during its layout pass. Tokens are unique per down.
            DispatchQueue.main.async { [weak self] in oldTokens.forEach { self?.router.onRelease($0) } }
        }
        layoutSize = bounds.size
        router.placement = SkinLayoutEngine.shared.calculate(definition: skin, request: SkinLayoutRequest(
            availableSize: SkinSize(width: Float(bounds.width), height: Float(bounds.height)),
            safeInsets: SkinInsets(left: 0, top: 0, right: 0, bottom: 0), mode: mode))
        rebuildAccessibility()
        updateHighlights()
        setNeedsDisplay()
    }

    func screenRect(_ rect: SkinRect) -> CGRect {
        guard let p = router.placement else { return .zero }
        return CGRect(x: CGFloat(p.screenX(skinX: rect.x)), y: CGFloat(p.screenY(skinY: rect.y)),
                      width: CGFloat(rect.width * p.scale), height: CGFloat(rect.height * p.scale))
    }

    override func draw(_ rect: CGRect) {
        guard let p = router.placement, let cg = UIGraphicsGetCurrentContext(), let lcd else { return }
        cg.saveGState()
        cg.clip(to: p.viewport.cgRect)
        cg.translateBy(x: CGFloat(p.viewport.x - p.sourceRegion.x * p.scale),
                       y: CGFloat(p.viewport.y - p.sourceRegion.y * p.scale))
        cg.scaleBy(x: CGFloat(p.scale), y: CGFloat(p.scale))
        image?.draw(in: CGRect(x: 0, y: 0, width: CGFloat(skin.designSize.width), height: CGFloat(skin.designSize.height)))
        panelColor.setFill()
        cg.fill(skin.lcdPanelRegion.cgRect)
        let dots = skin.lcdContentRegion.cgRect
        let cellW = dots.width / CGFloat(lcd.logicalColumns), cellH = dots.height / CGFloat(lcd.logicalRows)
        for row in 0..<Int(lcd.dotRows) {
            for column in 0..<Int(lcd.dotColumns) {
                let on = lcd.isDotOn(column: Int32(column), row: Int32(row))
                (on ? activeColor : activeColor.withAlphaComponent(31/255)).setFill()
                cg.fill(CGRect(x: dots.minX + CGFloat(lcd.logicalX(column: Int32(column))) * cellW + cellW * 0.1,
                               y: dots.minY + CGFloat(lcd.logicalY(row: Int32(row))) * cellH + cellH * 0.1,
                               width: cellW * 0.8, height: cellH * 0.8))
            }
        }
        let angleNames = ["DE", "G", "RAD"]
        for region in skin.statusRegions where !angleNames.contains(region.symbol.name) {
            let on = lcd.enabled && lcd.symbols.contains(region.symbol)
            drawLabel(region.symbol.name, in: region.bounds.cgRect, on: on, alignEnd: region.symbol == .shift)
        }
        let angleRegions = angleNames.compactMap { name in skin.statusRegions.first { $0.symbol.name == name } }
        if let first = angleRegions.first {
            let text = NSMutableAttributedString(string: "")
            var bounds = first.bounds.cgRect
            for region in angleRegions {
                bounds = bounds.union(region.bounds.cgRect)
                let on = lcd.enabled && lcd.symbols.contains(region.symbol)
                text.append(NSAttributedString(string: region.symbol.name, attributes: [
                    .font: UIFont.systemFont(ofSize: 20, weight: .semibold),
                    .foregroundColor: on ? activeColor : activeColor.withAlphaComponent(31/255)]))
            }
            // Adjacent angle segments form one word, without font-dependent gaps between regions.
            drawFittedText(text, in: bounds, centered: true)
        }
        if let modeBounds = skin.operatingModeRegion {
            drawLabel(operatingMode, in: modeBounds.cgRect, on: true)
        }
        cg.restoreGState()
    }

    /// Skin glyphs fit fixed design rectangles; Dynamic Type affects controls, not LCD geometry.
    private func drawLabel(_ text: String, in rect: CGRect, on: Bool, alignEnd: Bool = false) {
        let attributed = NSAttributedString(string: text, attributes: [
            .font: UIFont.systemFont(ofSize: 20, weight: .semibold),
            .foregroundColor: on ? activeColor : activeColor.withAlphaComponent(31/255)])
        drawFittedText(attributed, in: rect, alignEnd: alignEnd)
    }

    private func drawFittedText(_ text: NSAttributedString, in rect: CGRect,
                                alignEnd: Bool = false, centered: Bool = false) {
        let size = text.size()
        let scale = min(rect.width / max(size.width, 1), rect.height / max(size.height, 1))
        guard let cg = UIGraphicsGetCurrentContext() else { return }
        cg.saveGState()
        let offset = alignEnd ? rect.width - size.width * scale : (centered ? (rect.width - size.width * scale) / 2 : 0)
        cg.translateBy(x: rect.minX + offset,
                       y: rect.minY + (rect.height - size.height * scale) / 2)
        cg.scaleBy(x: scale, y: scale)
        text.draw(at: .zero)
        cg.restoreGState()
    }

    private func updateHighlights() {
        for key in skin.keyRegions {
            guard let overlay = highlights[key.key.name] else { continue }
            let target: Float = mode == .fullDevice && pressedKeys.contains(key.key.name) ? 0.28 : 0
            CATransaction.begin(); CATransaction.setDisableActions(true)
            overlay.path = UIBezierPath(rect: screenRect(key.bounds)).cgPath
            overlay.isHidden = mode == .controllerDisplay
            let previous = overlay.presentation()?.opacity ?? overlay.opacity
            if overlay.opacity != target {
                overlay.opacity = target
                let animation = CABasicAnimation(keyPath: "opacity")
                animation.fromValue = previous; animation.toValue = target
                animation.duration = target > 0 ? 0.035 : 0.12
                overlay.add(animation, forKey: "press")
            }
            CATransaction.commit()
        }
    }

    private func rebuildAccessibility() {
        guard let p = router.placement else { return }
        var elements: [UIAccessibilityElement] = []
        keyElements.removeAll(); symbolElements.removeAll()
        let display = UIAccessibilityElement(accessibilityContainer: self)
        display.accessibilityTraits = .staticText
        display.accessibilityLabel = "PC-1245 16桁LCD"
        display.accessibilityIdentifier = "lcd"
        display.accessibilityFrameInContainerSpace = screenRect(skin.lcdContentRegion)
        elements.append(display)
        for region in skin.statusRegions {
            let element = UIAccessibilityElement(accessibilityContainer: self)
            element.accessibilityTraits = .staticText
            element.accessibilityLabel = region.symbol.name
            element.accessibilityIdentifier = "symbol_\(region.symbol.name)"
            element.accessibilityFrameInContainerSpace = screenRect(region.bounds)
            symbolElements[region.symbol.name] = element
            elements.append(element)
        }
        if let region = skin.operatingModeRegion {
            let element = UIAccessibilityElement(accessibilityContainer: self)
            element.accessibilityTraits = .staticText
            element.accessibilityIdentifier = "operatingMode"
            element.accessibilityFrameInContainerSpace = screenRect(region)
            modeElement = element; elements.append(element)
        }
        if p.mode != .controllerDisplay {
            for key in skin.keyRegions {
                let element = SkinKeyAccessibility(accessibilityContainer: self)
                element.accessibilityTraits = .button
                element.accessibilityLabel = key.key.name.replacingOccurrences(of: "NUM_", with: "")
                element.accessibilityIdentifier = "key_\(key.key.name)"
                element.accessibilityFrameInContainerSpace = screenRect(key.bounds)
                element.activate = { [weak self] in
                    guard let self, self.router.enabled else { return false }
                    let owner = "accessibility:\(UUID().uuidString)"
                    guard self.router.onPress(key.key.name, owner) else { return false }
                    // Give the running ROM a frame to scan this key, then release its own token.
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak self] in self?.router.onRelease(owner) }
                    return true
                }
                keyElements[key.key.name] = element; elements.append(element)
            }
        }
        accessibilityElements = elements
        updateAccessibilityValues()
    }

    private func updateAccessibilityValues() {
        modeElement?.accessibilityLabel = operatingMode
        for (name, element) in keyElements {
            element.accessibilityValue = pressedKeys.contains(name) ? "押下中" : "解放"
        }
        for region in skin.statusRegions {
            symbolElements[region.symbol.name]?.accessibilityValue =
                lcd?.enabled == true && lcd?.symbols.contains(region.symbol) == true ? "点灯" : "消灯"
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let owner = "touch:\(UUID().uuidString)"
            touchOwners[ObjectIdentifier(touch)] = owner
            router.begin(owner: owner, point: touch.location(in: self))
        }
    }
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            if let owner = touchOwners[ObjectIdentifier(touch)] { router.move(owner: owner, point: touch.location(in: self)) }
        }
    }
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { finish(touches) }
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { finish(touches) }
    private func finish(_ touches: Set<UITouch>) {
        for touch in touches {
            if let owner = touchOwners.removeValue(forKey: ObjectIdentifier(touch)) { router.end(owner: owner) }
        }
    }
}
