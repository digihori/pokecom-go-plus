import GameController
import SwiftUI
import UIKit
import PgpPlayer

struct IosControllerDevice: Identifiable {
    let id: String
    let name: String
    let available: Set<String>
}

/// OS adapters own no Session threads. GameController delivers on main; all facade calls stay here.
@MainActor
final class IosPlayerInput: ObservableObject {
    @Published private(set) var devices: [IosControllerDevice] = []
    @Published private(set) var selectedId: String?
    @Published private(set) var pressed: Set<String> = []
    @Published private(set) var keyboardConnected = false
    @Published private(set) var enabled: Bool
    @Published private(set) var bindings: [String: String]
    let buttons = AppleInputCatalog.shared.buttons
    let keys = AppleInputCatalog.shared.keys
    @Published private(set) var allowedKeys = AppleInputCatalog.shared.keys
    private let defaults: UserDefaults?
    private let selection = AppleGamepadSelection()
    private var controllers: [String: GCController] = [:]
    private var keyboard: GCKeyboard?
    private var observers: [NSObjectProtocol] = []
    private var player: ApplePlayerFacade?
    private(set) var receiving = false
    private var epoch = -1
    private var downCodes: Set<Int> = []
    private let observeSystem: Bool
    var onInput: (() -> Void)?

    init(defaults: UserDefaults? = nil, observeSystem: Bool = false) {
        self.defaults = defaults; self.observeSystem = observeSystem
        enabled = defaults?.string(forKey: "controller.enabled") == "true"
        let stored = defaults?.dictionary(forKey: "controller.bindings") as? [String: String] ?? [:]
        bindings = AppleInputCatalog.shared.normalizedBindings(buttons: Array(stored.keys), keys: Array(stored.keys).map { stored[$0]! })
        if observeSystem {
            for name in [Notification.Name.GCControllerDidConnect, .GCControllerDidDisconnect,
                         .GCKeyboardDidConnect, .GCKeyboardDidDisconnect] {
                observers.append(NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                    MainActor.assumeIsolated { self?.refreshSystem() }
                })
            }
            refreshSystem()
        }
    }

    func configure(player: ApplePlayerFacade?, receiving: Bool, epoch: Int) {
        let changed = self.player !== player || self.receiving != receiving || self.epoch != epoch
        if changed {
            self.player?.releaseAllKeys()
            player?.releaseAllKeys()
            // A held key on focus return is a repeat, never a fresh press.
            baseline()
        }
        let nextKeys = player?.machine.keys ?? keys
        if allowedKeys != nextKeys { allowedKeys = nextKeys }
        self.player = player; self.receiving = receiving; self.epoch = epoch
        apply()
    }

    private func baseline() {
        downCodes.formUnion(currentKeyboardCodes())
        let current = selectedId.flatMap { controllers[$0] }.map(Self.buttonsDown) ?? []
        if current != pressed { pressed = current }
    }
    private func apply() {
        player?.configureExternalInput(receiving: receiving, enabled: enabled, selectedId: selectedId,
                                      buttons: buttons, keyNames: buttons.map { bindings[$0] ?? "" })
        player?.updateGamepad(id: selectedId, buttons: Array(pressed))
    }
    func setEnabled(_ value: Bool) { enabled = value; save() }
    func bind(_ button: String, to key: String?) {
        guard buttons.contains(button) else { return }
        bindings[button] = key
        bindings = AppleInputCatalog.shared.normalizedBindings(buttons: buttons, keys: buttons.map { bindings[$0] ?? "" })
        save()
    }
    private func save() {
        defaults?.set(enabled ? "true" : "false", forKey: "controller.enabled")
        defaults?.set(bindings, forKey: "controller.bindings")
        player?.releaseAllKeys()
        baseline()
        apply(); onInput?()
    }
    func select(_ id: String) {
        guard devices.contains(where: { $0.id == id }), selectedId != id else { return }
        selectedId = selection.update(ids: devices.map(\.id), requestedId: id)
        player?.releaseAllKeys()
        pressed = selectedId.flatMap { controllers[$0] }.map(Self.buttonsDown) ?? []
        apply(); onInput?()
    }
    var selectedDevice: IosControllerDevice? { devices.first { $0.id == selectedId } }
    func available(_ button: String) -> Bool {
        let key = bindings[button] ?? ""
        return (key.isEmpty || allowedKeys.contains(key)) && (selectedDevice?.available.contains(button) ?? devices.isEmpty)
    }

    private func refreshSystem() {
        attachControllers(GCController.controllers())
        let current = GCKeyboard.coalesced
        if keyboard !== current {
            keyboard?.keyboardInput?.keyChangedHandler = nil
            player?.disconnectKeyboard()
            downCodes.removeAll()
            keyboard = current
            keyboardConnected = current != nil
            current?.handlerQueue = .main
            downCodes.formUnion(currentKeyboardCodes())
            current?.keyboardInput?.keyChangedHandler = { [weak self, weak current] _, _, code, down in
                guard let self, let current, self.keyboard === current else { return }
                self.keyEvent(code: code.rawValue, down: down)
            }
            onInput?()
        }
    }

    /// Also accepts independent GameController snapshots in tests; no synthetic controller enters production enumeration.
    func attachControllers(_ values: [GCController]) {
        for controller in controllers.values { controller.extendedGamepad?.valueChangedHandler = nil }
        let next = Dictionary(uniqueKeysWithValues: values.map { (Self.id($0), $0) })
        controllers = next
        let list = values.map { IosControllerDevice(id: Self.id($0), name: $0.vendorName ?? "GameController", available: $0.extendedGamepad == nil ? [] : Set(buttons)) }
        let previous = selectedId
        devices = list
        selectedId = selection.update(ids: list.map(\.id), requestedId: nil)
        if previous != selectedId { player?.releaseAllKeys() }
        pressed = selectedId.flatMap { next[$0] }.map(Self.buttonsDown) ?? []
        for controller in values {
            controller.handlerQueue = .main
            controller.extendedGamepad?.valueChangedHandler = { [weak self, weak controller] _, _ in
                guard let self, let controller, self.controllers[Self.id(controller)] === controller else { return }
                self.controllerChanged(controller)
            }
        }
        apply(); onInput?()
    }
    private static func id(_ controller: GCController) -> String { String(describing: ObjectIdentifier(controller)) }
    private static func buttonsDown(_ controller: GCController) -> Set<String> {
        guard let pad = controller.extendedGamepad else { return [] }
        let values: [(String, GCControllerButtonInput)] = [
            ("UP", pad.dpad.up), ("DOWN", pad.dpad.down), ("LEFT", pad.dpad.left), ("RIGHT", pad.dpad.right),
            ("SOUTH", pad.buttonA), ("EAST", pad.buttonB), ("WEST", pad.buttonX), ("NORTH", pad.buttonY),
            ("L1", pad.leftShoulder), ("R1", pad.rightShoulder)]
        return Set(values.filter { $0.1.isPressed }.map { $0.0 })
    }
    func controllerChanged(_ controller: GCController) {
        guard Self.id(controller) == selectedId else { return }
        let next = Self.buttonsDown(controller)
        if next != pressed { pressed = next; apply(); onInput?() }
    }
    private func currentKeyboardCodes() -> Set<Int> {
        guard let input = keyboard?.keyboardInput else { return [] }
        return Set((0...231).filter { input.button(forKeyCode: GCKeyCode(rawValue: $0))?.isPressed == true })
    }
    func keyEvent(code: Int, down: Bool) {
        let repeated = downCodes.contains(code)
        if down { downCodes.insert(code) } else { downCodes.remove(code) }
        let systemModifiers: Set<Int> = [226, 227, 230, 231] // Option/Command retain system shortcut behavior.
        if down && systemModifiers.contains(code) { player?.releaseAllKeys() }
        if !down || downCodes.isDisjoint(with: systemModifiers) {
            _ = player?.physicalKey(code: Int32(code), down: down, repeated: repeated)
        }
        onInput?()
    }
    func keyboardDisconnected() {
        player?.disconnectKeyboard(); downCodes.removeAll(); keyboardConnected = false; onInput?()
    }
    deinit {
        for observer in observers { NotificationCenter.default.removeObserver(observer) }
        for controller in controllers.values { controller.extendedGamepad?.valueChangedHandler = nil }
        keyboard?.keyboardInput?.keyChangedHandler = nil
    }
}

/// No UIKeyInput/text/IME interface: this responder prevents mapped keys from navigating Player buttons.
struct PhysicalKeyboardShield: UIViewRepresentable {
    @ObservedObject var host: PlayerHost
    func makeUIView(context: Context) -> KeyShieldView { KeyShieldView() }
    func updateUIView(_ view: KeyShieldView, context: Context) {
        view.receiving = host.externalInputReceiving
        view.mappedKey = { [weak host] in host?.mappedPhysicalKey($0) }
        view.onFocus = { [weak host] in host?.setInputFocus($0) }
        view.onCancel = { [weak host] in host?.releaseAllKeys() }
        if view.receiving && view.window != nil && !view.isFirstResponder { view.becomeFirstResponder() }
        else if !view.receiving && view.isFirstResponder { view.resignFirstResponder() }
    }
}
final class KeyShieldView: UIView {
    var receiving = false
    var mappedKey: (Int32) -> String? = { AppleInputCatalog.shared.keyForCode(code: $0) }
    var onFocus: ((Bool) -> Void)?
    var onCancel: (() -> Void)?
    private var tokens: [NSObjectProtocol] = []
    override var canBecomeFirstResponder: Bool { receiving }
    override func didMoveToWindow() {
        super.didMoveToWindow()
        for token in tokens { NotificationCenter.default.removeObserver(token) }
        tokens.removeAll()
        guard let window else { onFocus?(false); return }
        onFocus?(window.isKeyWindow)
        for name in [UIWindow.didBecomeKeyNotification, UIWindow.didResignKeyNotification] {
            tokens.append(NotificationCenter.default.addObserver(forName: name, object: window, queue: .main) { [weak self] note in
                MainActor.assumeIsolated { self?.onFocus?(note.name == UIWindow.didBecomeKeyNotification) }
            })
        }
        if receiving { becomeFirstResponder() }
    }
    override func pressesBegan(_ presses: Set<UIPress>, with event: UIPressesEvent?) { forwardUnmapped(presses, event, began: true) }
    override func pressesEnded(_ presses: Set<UIPress>, with event: UIPressesEvent?) { forwardUnmapped(presses, event, began: false) }
    override func pressesCancelled(_ presses: Set<UIPress>, with event: UIPressesEvent?) {
        onCancel?()
        super.pressesCancelled(presses, with: event)
    }
    private func forwardUnmapped(_ presses: Set<UIPress>, _ event: UIPressesEvent?, began: Bool) {
        let unmapped = Set(presses.filter { press in
            guard receiving, let key = press.key, key.modifierFlags.intersection([.command, .alternate]).isEmpty else { return true }
            return mappedKey(Int32(key.keyCode.rawValue)) == nil
        })
        guard !unmapped.isEmpty else { return }
        if began { super.pressesBegan(unmapped, with: event) } else { super.pressesEnded(unmapped, with: event) }
    }
    deinit { for token in tokens { NotificationCenter.default.removeObserver(token) } }
}

/// Native menu lifecycle, including dismissal without an action, gates external input exactly once.
final class InputMenuButton: UIButton {
    var onOpen: ((Bool) -> Void)?
    var showing = false
    override func contextMenuInteraction(_ interaction: UIContextMenuInteraction, willDisplayMenuFor configuration: UIContextMenuConfiguration, animator: UIContextMenuInteractionAnimating?) {
        super.contextMenuInteraction(interaction, willDisplayMenuFor: configuration, animator: animator)
        showing = true; onOpen?(true)
    }
    override func contextMenuInteraction(_ interaction: UIContextMenuInteraction, willEndFor configuration: UIContextMenuConfiguration, animator: UIContextMenuInteractionAnimating?) {
        super.contextMenuInteraction(interaction, willEndFor: configuration, animator: animator)
        let finish = { [weak self] in self?.showing = false; self?.onOpen?(false) }
        if let animator { animator.addCompletion(finish) } else { finish() }
    }
}
struct PlayerNativeMenu: UIViewRepresentable {
    @ObservedObject var host: PlayerHost
    func makeUIView(context: Context) -> InputMenuButton {
        let button = InputMenuButton(type: .system)
        button.setImage(UIImage(systemName: "ellipsis"), for: .normal)
        button.tintColor = .white
        button.accessibilityLabel = "メニュー"; button.accessibilityIdentifier = "playerMenu"
        button.showsMenuAsPrimaryAction = true
        button.onOpen = { [weak host] in host?.setInputMenuOpen($0) }
        return button
    }
    func updateUIView(_ view: InputMenuButton, context: Context) {
        guard !view.showing else { return }
        func action(_ title: String, enabled: Bool = true, _ perform: @escaping () -> Void) -> UIAction {
            UIAction(title: title, attributes: enabled ? [] : .disabled) { _ in perform() }
        }
        let controls: [UIMenuElement] = [
            UIMenu(title: "エミュレータ操作", children: [
                action("Run", enabled: host.status.runState == "PAUSED", host.run),
                action("Pause", enabled: host.status.runState == "RUNNING", host.pause),
                action("Reset", host.reset)]),
            UIMenu(title: "プログラム", children: [action("ロード") { host.openProgram(save: false) }, action("セーブ") { host.openProgram(save: true) }]),
            UIMenu(title: "表示", children: [action(host.isControllerDisplay ? "通常表示" : "Controller Display", host.togglePresentation)]),
            action("ホームへ戻る", host.returnHome)]
        let modes: [UIMenuElement] = host.operatingModes.isEmpty ? [] : [UIMenu(title: "動作モード", children: host.operatingModes.map { mode in action(mode) { host.setOperatingMode(mode) } })]
        view.menu = UIMenu(children: Array(controls.prefix(2)) + modes + Array(controls.dropFirst(2)))
    }
}

struct ControllerSettingsPanel: View {
    @ObservedObject var input: IosPlayerInput
    @Environment(\.dismiss) private var dismiss
    @State private var editing: String?
    var body: some View {
        NavigationStack {
            Form {
                Toggle("コントローラを有効", isOn: Binding(get: { input.enabled }, set: input.setEnabled))
                    .accessibilityIdentifier("controllerEnabled")
                Text("設定は全体で1セットです。変更時に自動保存します。")
                if input.devices.isEmpty { Text("コントローラ未接続。割当は編集できます。") }
                else {
                    Picker("使用するコントローラ", selection: Binding(get: { input.selectedId ?? "" }, set: input.select)) {
                        if input.selectedId == nil { Text("選択してください").tag("") }
                        ForEach(input.devices) { Text($0.name).tag($0.id) }
                    }
                    if input.selectedDevice?.available.isEmpty == true { Text("extendedGamepad非対応のため入力を利用できません。") }
                }
                ScrollView(.horizontal) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 40).fill(Color.secondary.opacity(0.12)).frame(width: 360, height: 230)
                        ForEach(input.buttons, id: \.self) { button in
                            let point = position(button)
                            Button { editing = button } label: {
                                VStack(spacing: 2) {
                                    Text(AppleInputCatalog.shared.buttonLabel(name: button)).font(.caption.bold())
                                    Text(AppleInputCatalog.shared.keyLabel(name: input.bindings[button])).font(.caption)
                                    if !input.available(button) { Text("利用不可").font(.caption2) }
                                }.lineLimit(1).minimumScaleFactor(0.7).frame(width: 64, height: 52)
                                    .background(input.pressed.contains(button) ? Color.green.opacity(0.5) : Color.secondary.opacity(0.15))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            }.buttonStyle(.plain).position(x: point.x, y: point.y)
                                .accessibilityIdentifier("binding_" + button)
                        }
                    }.frame(width: 360, height: 260)
                }
                Text("十字キー・フェイス4位置・L1/R1のみ。スティック・トリガーは使いません。設定中は点灯テストだけで、Playerへ送信しません。")
                Text(input.keyboardConnected ? "Physicalキーボード接続中" : "Physicalキーボード未接続")
                Text("文字変換・IMEは使いません。SHIFT単独、左Control/F1→DEF、Esc→BREAK、Backspace→CLEAR。英数字・矢印・テンキーは物理位置で入力します。")
            }.navigationTitle("コントローラ設定").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("戻る") { dismiss() } } }
                .sheet(isPresented: Binding(get: { editing != nil }, set: { if !$0 { editing = nil } })) {
                    NavigationStack {
                        List {
                            Button("未割当") { if let editing { input.bind(editing, to: nil) }; editing = nil }
                            ForEach(input.keys, id: \.self) { key in
                                Button(AppleInputCatalog.shared.keyLabel(name: key)) { if let editing { input.bind(editing, to: key) }; editing = nil }
                                    .accessibilityIdentifier("assign_" + key)
                            }
                        }.navigationTitle("割当キー").navigationBarTitleDisplayMode(.inline)
                            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("戻る") { editing = nil } } }
                    }
                }
        }
    }
    private func position(_ button: String) -> CGPoint {
        switch button {
        case "UP": return CGPoint(x: 95, y: 100)
        case "DOWN": return CGPoint(x: 95, y: 206)
        case "LEFT": return CGPoint(x: 44, y: 153)
        case "RIGHT": return CGPoint(x: 146, y: 153)
        case "NORTH": return CGPoint(x: 265, y: 100)
        case "SOUTH": return CGPoint(x: 265, y: 206)
        case "WEST": return CGPoint(x: 214, y: 153)
        case "EAST": return CGPoint(x: 316, y: 153)
        case "L1": return CGPoint(x: 80, y: 36)
        default: return CGPoint(x: 280, y: 36)
        }
    }
}
