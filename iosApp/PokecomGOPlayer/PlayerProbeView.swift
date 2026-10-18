import SwiftUI
import UIKit
import PgpPlayer
import UniformTypeIdentifiers

struct PlayerProbeView: View {
    @ObservedObject var host: PlayerHost
    @State private var information: String?
    @State private var playbackPanel: String?
    @State private var controllerPanel = false
    var body: some View {
        Group {
            if host.showHome { home }
            else { player }
        }
        .sheet(isPresented: Binding(get: { host.romPanelOpen }, set: { if !$0 { host.closeRomPanel() } })) { RomLibraryPanel(host: host) }
        .sheet(isPresented: Binding(get: { host.programPanelOpen }, set: { if !$0 { host.closeProgram() } })) { ProgramPanel(host: host) }
        .sheet(isPresented: Binding(get: { information != nil }, set: { if !$0 { information = nil } })) {
            if let information { InformationPanel(title: information) }
        }
        .sheet(isPresented: Binding(get: { playbackPanel != nil }, set: { if !$0 { playbackPanel = nil } })) {
            NavigationStack {
                Form {
                    if playbackPanel == "CPU速度" {
                        Picker("CPU速度", selection: Binding(get: { host.executionSpeed }, set: host.setExecutionSpeed)) {
                            ForEach(PlayerPlaybackPreferences.speeds, id: \.self) { Text($0 + "倍").tag($0) }
                        }.pickerStyle(.inline).accessibilityIdentifier("cpuSpeed")
                        Text("音高・テンポもCPU速度の倍率に追従します。")
                    } else {
                        Toggle("サウンド", isOn: Binding(get: { host.soundEnabled }, set: host.setSoundEnabled))
                            .accessibilityIdentifier("soundEnabled")
                        Text("消音モードに従い、他アプリの音声と共存します。背景では再生しません。")
                        if !host.audioMessage.isEmpty { Text(host.audioMessage) }
                        Button("音声を再試行") { host.setSoundEnabled(true) }
                            .disabled(!host.soundEnabled)
                        Text("出力先の切断・音声エラー後は、PlayerでRunするかサウンドONで再試行できます。")
                    }
                }.navigationTitle(playbackPanel ?? "設定").navigationBarTitleDisplayMode(.inline)
                    .toolbar { ToolbarItem(placement: .cancellationAction) { Button("戻る") { playbackPanel = nil } } }
            }
        }
        .sheet(isPresented: $controllerPanel) { ControllerSettingsPanel(input: host.input) }
        .onChange(of: information) { _ in host.releaseAllKeys() }
    }

    private var home: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("機種を選択").font(.headline)
                    if !host.audioMessage.isEmpty { Text(host.audioMessage).font(.callout) }
                    Text("PC-1245").font(.title2)
                    Text(host.romSourceName ?? (host.hasSession ? "PC-1245 Session" : "ROM未登録"))
                    if host.isBusy { ProgressView() }
                    if host.hasSession {
                        Button("プレイ", action: host.enterPlayer).buttonStyle(.borderedProminent)
                            .accessibilityIdentifier("playPC1245").disabled(host.isBusy)
                    } else {
                        Button("ROMを登録", action: host.openRomPicker).buttonStyle(.borderedProminent)
                            .accessibilityIdentifier("romImport").disabled(host.isBusy)
                    }
                    Text("ROMは同梱していません。登録したROMはアプリ内に保存します。RAM・CPU状態はアプリ終了後に復元しません。")
                        .font(.callout)
                    if !host.romMessage.isEmpty { Text(host.romMessage) }
                }.padding().frame(maxWidth: .infinity, alignment: .leading)
            }
            .navigationTitle("Pokecom GO Player").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .primaryAction) {
                HomeNativeMenu(isBusy: host.isBusy, openRom: host.openRomPanel,
                    openPlayback: { playbackPanel = $0 }, openController: { controllerPanel = true },
                    openInformation: { information = $0 })
                    .frame(width: 44, height: 44)
            } }
        }
    }

    private var player: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Text("PC-1245").font(.headline)
                    Spacer(minLength: 0)
                    Button(action: host.toggleRunState) { Text(host.status.runState).font(.caption.monospaced()) }
                        .frame(minWidth: 80, minHeight: 44)
                        .accessibilityIdentifier("runState")
                        .disabled(host.status.runState == "FAULTED")
                    Spacer(minLength: 0)
                    Button(host.status.operatingMode) {
                        host.setOperatingMode(host.status.operatingMode == "RUN" ? "PRO" : "RUN")
                    }.frame(minWidth: 44, minHeight: 44).background(Color(red: 0.1, green: 0.37, blue: 0.13))
                        .clipShape(Circle()).accessibilityIdentifier("pro")
                    PlayerNativeMenu(host: host).frame(width: 44, height: 44)
                }.lineLimit(1).minimumScaleFactor(0.7).padding(.horizontal, 8).frame(minHeight: 52)
                if !host.audioMessage.isEmpty {
                    Text(host.audioMessage).font(.caption).padding(.horizontal, 8)
                        .accessibilityIdentifier("audioMessage")
                }
                if let skin = host.skin, let lcd = host.lcd {
                    PlayerSkinSurface(host: host, skin: skin, lcd: lcd)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .onChange(of: geometry.size) { _ in host.releaseAllKeys() }
            .onChange(of: geometry.safeAreaInsets) { _ in host.releaseAllKeys() }
        }
        .background(PhysicalKeyboardShield(host: host).frame(width: 1, height: 1))
        .background(Color.black.ignoresSafeArea()).foregroundStyle(.white).statusBarHidden()
        .onAppear { UIDevice.current.beginGeneratingDeviceOrientationNotifications() }
        .onDisappear {
            UIDevice.current.endGeneratingDeviceOrientationNotifications()
            host.releaseAllKeys()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIDevice.orientationDidChangeNotification)) { _ in host.releaseAllKeys() }
    }
}

private struct RomLibraryPanel: View {
    @ObservedObject var host: PlayerHost
    var body: some View {
        NavigationStack {
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
                if host.canCancelRomRead {
                    Button("取込みをキャンセル", action: host.cancelRomRead)
                        .accessibilityIdentifier("cancelRomRead").padding()
                }
            }.navigationTitle("ROM管理").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) {
                    Button("閉じる", action: host.closeRomPanel).disabled(host.isBusy)
                } }
        }.interactiveDismissDisabled(host.isBusy)
            .sheet(isPresented: $host.pickerPresented) {
                PlayerDocumentPicker(onSelect: host.importRom, onCancel: host.cancelRomPicker)
            }
    }
}

struct PlayerDocumentPicker: UIViewControllerRepresentable {
    var exportURL: URL? = nil
    let onSelect: (URL) -> Void
    let onCancel: () -> Void
    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }
    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker: UIDocumentPickerViewController
        if let exportURL {
            picker = UIDocumentPickerViewController(forExporting: [exportURL], asCopy: true)
        }
        else {
            picker = UIDocumentPickerViewController(forOpeningContentTypes: [.data], asCopy: true)
        }
        picker.directoryURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        picker.allowsMultipleSelection = false
        picker.delegate = context.coordinator
        return picker
    }
    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: PlayerDocumentPicker
        private var completed = false
        init(parent: PlayerDocumentPicker) { self.parent = parent }
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard !completed else { return }; completed = true
            if let url = urls.first { parent.onSelect(url) } else { parent.onCancel() }
        }
        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            guard !completed else { return }; completed = true
            parent.onCancel()
        }
    }
}

private struct ProgramPanel: View {
    @ObservedObject var host: PlayerHost
    @State private var basic = true
    @State private var start = "C500"
    @State private var end = "C5FF"
    var body: some View {
        NavigationStack {
            Form {
                if let issue = host.programIssue { Text(issue.userMessage).foregroundStyle(.red).accessibilityIdentifier("programError") }
                else { Text(host.programMessage).accessibilityIdentifier("programMessage") }
                if host.programBusy { ProgressView() }
                if host.programSaving {
                    Picker("形式", selection: $basic) {
                        Text("BASIC").tag(true)
                        Text("ダンプ").tag(false)
                    }.pickerStyle(.segmented)
                    if !basic {
                        TextField("開始（16進数）", text: $start).accessibilityIdentifier("dumpStart")
                        TextField("終了（16進数）", text: $end).accessibilityIdentifier("dumpEnd")
                        Text("終了アドレスを含むRAM範囲を保存します。")
                    }
                    Button("ファイルへセーブ") { host.prepareProgramExport(basic: basic, start: start, end: end) }
                        .disabled(host.programBusy).accessibilityIdentifier("exportProgram")
                } else if !host.programBusy {
                    Button("ファイルを選択", action: host.chooseProgramFile)
                }
            }.textInputAutocapitalization(.never).autocorrectionDisabled()
                .navigationTitle(host.programSaving ? "セーブ" : "ロード").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("閉じる", action: host.closeProgram) } }
        }
        .background(PanelAppearance { host.programPanelDidAppear() }.frame(width: 0, height: 0))
        .sheet(item: $host.programDocument, onDismiss: host.programDocumentDidDismiss) { document in
            switch document {
            case .importing:
                PlayerDocumentPicker(onSelect: host.importProgram, onCancel: host.cancelProgramPicker)
            case .exporting(let url):
                PlayerDocumentPicker(exportURL: url, onSelect: { _ in host.finishProgramExport(saved: true) },
                                     onCancel: { host.finishProgramExport(saved: false) })
            }
        }
    }
}

private extension ProgramIssue {
    var userMessage: String {
        switch self {
        case .file(let error):
            switch error {
            case .unsupportedExtension: return "bas／dmpファイルを選択してください。"
            case .tooLarge: return "ファイルは2 MiBまでです。"
            case .invalidAddress: return "開始・終了を0000〜FFFFの16進数で指定してください。"
            case .staleSession: return "Sessionが変わりました。ファイルを選び直してください。"
            }
        case .shared(let error):
            if error is PlayerProgramErrorInvalidUtf8 { return "UTF-8として読み込めません。" }
            if error is PlayerProgramErrorNotPaused { return "CPUを停止してから操作してください。" }
            if error is PlayerProgramErrorNonRam { return "ROM・LCDなどRAM以外の範囲は扱えません。" }
            if error is PlayerProgramErrorInvalidRange { return "保存範囲が不正です。" }
            if error is PlayerProgramErrorBasicCompile { return "BASICの行番号・文字・構文を確認してください。" }
            if error is PlayerProgramErrorBasicMemory { return "BASIC管理領域または容量が不正です。必要ならポケコン側でNEWを実行してください。" }
            if error is PlayerProgramErrorBasicDecode { return "BASICをテキストへ変換できません。管理情報・文字を確認してください。" }
            if error is PlayerProgramErrorDumpParse { return "DMPのアドレス・16進データの書式を確認してください。" }
            if error is PlayerProgramErrorPlacement { return "RAM範囲・容量・ミラー重複を確認してください。メモリは変更していません。" }
            if error is PlayerProgramErrorWrongMachine { return "機種が一致しません。" }
            return "プログラムの内容を検証できません。メモリは変更していません。"
        case .io(let message): return "ファイルを読み込み・保存できませんでした。\n" + message
        }
    }
}

private struct InformationPanel: View {
    let title: String
    private var versionLabel: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""
        return "Pokecom GO Player \(version) (\(build))"
    }
    @Environment(\.dismiss) private var dismiss
    private var files: [String] {
        switch title {
        case "Playerとは": return ["PLAYER_HELP_IOS.md"]
        case "Studioについて": return ["PLAYER_STUDIO_GUIDE.md"]
        case "プライバシーポリシー": return ["PLAYER_PRIVACY_POLICY_IOS.md"]
        default: return ["LICENSE", "THIRD_PARTY_NOTICES.md", "licenses/HIDAPI.txt", "licenses/LWJGL.txt", "licenses/SDL.txt", "licenses/libffi.txt"]
        }
    }
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if title == "バージョン・ライセンス" {
                        Text(versionLabel)
                    }
                    ForEach(files, id: \.self) { file in InformationDocument(file: file) }
                    if title == "Studioについて" {
                        Link("GitHubでStudioを見る", destination: URL(string: "https://github.com/digihori/pokecom-go-plus")!)
                    }
                }.padding().frame(maxWidth: .infinity, alignment: .leading)
            }.navigationTitle(title).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("戻る") { dismiss() } } }
        }
    }
}

private struct InformationDocument: View {
    let file: String
    private var paragraphs: [String] {
        let url = Bundle.main.bundleURL.appendingPathComponent("PlayerInformation/" + file)
        let text = (try? String(contentsOf: url, encoding: .utf8)) ?? "同梱文書を読み込めません。"
        return text.components(separatedBy: "\n\n")
    }
    var body: some View {
        ForEach(Array(paragraphs.enumerated()), id: \.offset) { item in
            if item.element.hasPrefix("#") {
                Text(item.element.drop(while: { $0 == "#" || $0 == " " }))
                    .font(.headline).textSelection(.enabled)
            } else if file == "LICENSE" || file.hasSuffix(".txt") {
                Text(verbatim: item.element).font(.system(.body, design: .monospaced)).textSelection(.enabled)
            } else {
                Text((try? AttributedString(markdown: item.element)) ?? AttributedString(item.element))
                    .textSelection(.enabled)
            }
        }
    }
}

private struct PlayerSkinSurface: UIViewRepresentable {
    @ObservedObject var host: PlayerHost
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

// UIKit reports completion of the parent sheet's presentation before opening its picker.
private struct PanelAppearance: UIViewControllerRepresentable {
    let appeared: () -> Void
    func makeUIViewController(context: Context) -> Controller { Controller(appeared: appeared) }
    func updateUIViewController(_ controller: Controller, context: Context) {}
    final class Controller: UIViewController {
        private let appeared: () -> Void
        private var delivered = false
        init(appeared: @escaping () -> Void) { self.appeared = appeared; super.init(nibName: nil, bundle: nil) }
        required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            guard !delivered else { return }
            delivered = true
            DispatchQueue.main.async { [weak self] in self?.appeared() }
        }
    }
}

private struct HomeNativeMenu: UIViewRepresentable {
    let isBusy: Bool
    let openRom: () -> Void
    let openPlayback: (String) -> Void
    let openController: () -> Void
    let openInformation: (String) -> Void
    func makeUIView(context: Context) -> UIButton {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: "ellipsis"), for: .normal)
        button.accessibilityLabel = "メニュー"
        button.accessibilityIdentifier = "homeMenu"
        button.showsMenuAsPrimaryAction = true
        return button
    }
    func updateUIView(_ button: UIButton, context: Context) {
        button.isEnabled = !isBusy
        func action(_ title: String, _ perform: @escaping () -> Void) -> UIAction {
            UIAction(title: title) { _ in perform() }
        }
        button.menu = UIMenu(children: [
            UIMenu(title: "設定", children: [action("ROM管理", openRom),
                action("CPU速度") { openPlayback("CPU速度") }, action("サウンド") { openPlayback("サウンド") },
                action("コントローラ設定", openController)]),
            UIMenu(title: "ヘルプ・情報", children:
                ["Playerとは", "Studioについて", "プライバシーポリシー", "バージョン・ライセンス"].map { title in
                    action(title) { openInformation(title) }
                })])
    }
}
