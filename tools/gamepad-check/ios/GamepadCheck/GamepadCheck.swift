import GameController
import SwiftUI

private struct Report: Encodable {
    let schemaVersion = 1
    let platform = "ios"
    let osVersion: String
    let appVersion: String
    let controllerModel: String
    let truncated: Bool
    let events: [Event]
}

private struct Event: Codable {
    let elapsedMs: Int
    let type: String
    var deviceId: String?
    var deviceName: String?
    var productCategory: String?
    var control: String?
    var value: Float?
}

@MainActor
private final class Probe: ObservableObject {
    struct Device {
        let id: String
        let name: String
        let category: String
        var values: [String: Float] = [:]
        var types: [String: String] = [:]
    }

    @Published private(set) var devices: [ObjectIdentifier: Device] = [:]
    @Published private(set) var recording = false
    @Published private(set) var events: [Event] = []
    @Published private(set) var truncated = false
    @Published var model = "不明"
    private var nextId = 1
    private var started = 0.0
    private var recordedModel = ""
    private var observers: [NSObjectProtocol] = []

    func start() {
        guard observers.isEmpty else { return }
        let center = NotificationCenter.default
        for name in [Notification.Name.GCControllerDidConnect, .GCControllerDidDisconnect] {
            observers.append(center.addObserver(forName: name, object: nil, queue: .main) { [weak self] note in
                guard let controller = note.object as? GCController else { return }
                MainActor.assumeIsolated {
                    if name == .GCControllerDidConnect { self?.connect(controller) }
                    else { self?.disconnect(controller) }
                }
            })
        }
        GCController.controllers().forEach(connect)
    }

    func stop() {
        observers.forEach(NotificationCenter.default.removeObserver)
        observers.removeAll()
        GCController.controllers().forEach(disconnect)
    }

    private func connect(_ controller: GCController) {
        let key = ObjectIdentifier(controller)
        guard devices[key] == nil else { return }
        let device = Device(id: String(nextId), name: controller.vendorName ?? "Unknown",
                            category: controller.productCategory)
        nextId += 1
        devices[key] = device
        append("connected", device: device)
        let profile = controller.physicalInputProfile
        controller.handlerQueue = .main
        for (name, button) in profile.buttons {
            update(key, type: "button", name: name, value: button.value)
            button.valueChangedHandler = { [weak self] _, value, _ in
                MainActor.assumeIsolated { self?.update(key, type: "button", name: name, value: value) }
            }
        }
        for (name, axis) in profile.axes {
            update(key, type: "axis", name: name, value: axis.value)
            axis.valueChangedHandler = { [weak self] _, value in
                MainActor.assumeIsolated { self?.update(key, type: "axis", name: name, value: value) }
            }
        }
        for (name, pad) in profile.dpads {
            update(key, type: "axis", name: name + ".x", value: pad.xAxis.value)
            update(key, type: "axis", name: name + ".y", value: pad.yAxis.value)
            pad.valueChangedHandler = { [weak self] _, x, y in
                MainActor.assumeIsolated {
                    self?.update(key, type: "axis", name: name + ".x", value: x)
                    self?.update(key, type: "axis", name: name + ".y", value: y)
                }
            }
        }
    }

    private func disconnect(_ controller: GCController) {
        let profile = controller.physicalInputProfile
        profile.buttons.values.forEach { $0.valueChangedHandler = nil }
        profile.axes.values.forEach { $0.valueChangedHandler = nil }
        profile.dpads.values.forEach { $0.valueChangedHandler = nil }
        if let device = devices.removeValue(forKey: ObjectIdentifier(controller)) {
            append("disconnected", device: device)
        }
    }

    private func update(_ key: ObjectIdentifier, type: String, name: String, value: Float) {
        guard var device = devices[key], value.isFinite else { return }
        if let previous = device.values[name], previous == value ||
            (value != 0 && abs(previous - value) < 0.01) { return }
        device.values[name] = value
        device.types[name] = type
        devices[key] = device
        append(type, device: device, control: name, value: value)
    }

    func toggleRecording() {
        if recording {
            recording = false
            return
        }
        events = []
        truncated = false
        started = ProcessInfo.processInfo.systemUptime
        recordedModel = model
        recording = true
        append("marker", control: "start")
        for device in devices.values.sorted(by: { $0.id < $1.id }) {
            append("connected", device: device)
            for (name, value) in device.values.sorted(by: { $0.key < $1.key }) {
                append(device.types[name] ?? "axis", device: device, control: name, value: value)
            }
        }
    }

    func mark() { append("marker") }

    private func append(_ type: String, device: Device? = nil, control: String? = nil, value: Float? = nil) {
        guard recording else { return }
        guard events.count < 20_000 else { truncated = true; return }
        events.append(Event(elapsedMs: Int((ProcessInfo.processInfo.systemUptime - started) * 1000),
                            type: type, deviceId: device?.id, deviceName: device?.name,
                            productCategory: device?.category, control: control, value: value))
    }

    func export() throws -> URL {
        let report = Report(osVersion: UIDevice.current.systemVersion,
                            appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
                            controllerModel: recordedModel, truncated: truncated, events: events)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let filename = "gamepad-check-\(Int(Date().timeIntervalSince1970 * 1000)).json"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(filename)
        try encoder.encode(report).write(to: url, options: .atomic)
        return url
    }
}

private struct ShareSheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

private struct ExportedLog: Identifiable {
    let url: URL
    var id: URL { url }
}

@main
struct GamepadCheckApp: App {
    @StateObject private var probe = Probe()
    @State private var exported: ExportedLog?
    @State private var shareFailed = false

    var body: some Scene {
        WindowGroup {
            NavigationStack {
                List {
                    Section("診断") {
                        Picker("コントローラ", selection: $probe.model) {
                            ForEach(["不明", "初代 Joy-Con", "Joy-Con 2", "その他"], id: \.self) { Text($0) }
                        }
                        .disabled(probe.recording)
                        Button(probe.recording ? "記録停止" : "記録開始", action: probe.toggleRecording)
                        Button("区切りを記録", action: probe.mark).disabled(!probe.recording)
                        Button("診断結果を共有") {
                            do { exported = ExportedLog(url: try probe.export()) }
                            catch { shareFailed = true }
                        }
                        .disabled(probe.recording || probe.events.isEmpty)
                        Text("\(probe.events.count) 件" + (probe.truncated ? "（上限到達）" : ""))
                    }
                    Section("接続中 \(probe.devices.count) 台") {
                        ForEach(probe.devices.values.sorted { $0.id < $1.id }, id: \.id) { device in
                            DisclosureGroup("#\(device.id) \(device.name)") {
                                Text(device.category)
                                ForEach(device.values.keys.sorted(), id: \.self) { name in
                                    LabeledContent(name, value: String(format: "%.3f", device.values[name] ?? 0))
                                }
                            }
                        }
                    }
                }
                .navigationTitle("Gamepad Check")
                .sheet(item: $exported) { ShareSheet(url: $0.url) }
                .alert("診断ログを書き出せませんでした", isPresented: $shareFailed) {
                    Button("OK", role: .cancel) {}
                }
                .onAppear { probe.start() }
                .onDisappear { probe.stop() }
            }
        }
    }
}
