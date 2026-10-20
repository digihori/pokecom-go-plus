import AVFoundation
import Foundation
import PgpPlayer

/// Swift-owned immutable packet. An empty sample array means frameCount zeros.
struct PlayerPCM {
    let samples: [Int16]
    let frameCount: Int
    let rate: Int

    init(samples: [Int16], frameCount: Int, rate: Int) {
        self.samples = samples; self.frameCount = frameCount; self.rate = rate
    }
    init(_ packet: AppleAudioPacket) {
        let copy = packet.doCopySamples()
        samples = (0..<Int(copy.size)).map { copy.get(index: Int32($0)) }
        frameCount = Int(packet.frameCount)
        rate = Int(packet.playbackRate)
    }
    var isValid: Bool { rate > 0 && frameCount > 0 && (samples.isEmpty || samples.count == frameCount) }
}

protocol PlayerPCMSink: AnyObject {
    func schedule(_ packet: PlayerPCM, completion: @escaping () -> Void) throws
    func close()
}

/// Owns AV objects on one worker. Completion means output consumed the buffer, not just copied it.
final class EnginePCMSink: PlayerPCMSink {
    private let engine = AVAudioEngine()
    private let node = AVAudioPlayerNode()
    private let format: AVAudioFormat
    private let session: AVAudioSession?

    init(rate: Int, live: Bool = true) throws {
        format = AVAudioFormat(standardFormatWithSampleRate: Double(rate), channels: 1)!
        session = live ? AVAudioSession.sharedInstance() : nil
        if let session {
            try session.setCategory(.ambient, mode: .default)
            try session.setPreferredIOBufferDuration(0.01)
            try session.setActive(true)
        } else {
            // Select offline mode before accessing mainMixerNode. Creating the default
            // graph first can initialize device I/O and deadlock CoreAudio on hosted Simulators.
            try engine.enableManualRenderingMode(.offline, format: format, maximumFrameCount: 4096)
        }
        engine.attach(node)
        engine.connect(node, to: engine.mainMixerNode, format: format)
        if live {
            do { try engine.start(); node.play() }
            catch { close(); throw error }
        }
    }

    static func buffer(_ packet: PlayerPCM) -> AVAudioPCMBuffer? {
        guard packet.isValid,
              let format = AVAudioFormat(standardFormatWithSampleRate: Double(packet.rate), channels: 1),
              let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(packet.frameCount)),
              let samples = buffer.floatChannelData?[0] else { return nil }
        buffer.frameLength = AVAudioFrameCount(packet.frameCount)
        for index in 0..<packet.frameCount {
            samples[index] = packet.samples.isEmpty ? 0 : Float(packet.samples[index]) / 32768
        }
        return buffer
    }

    func schedule(_ packet: PlayerPCM, completion: @escaping () -> Void) throws {
        guard let buffer = Self.buffer(packet) else { throw CocoaError(.coderInvalidValue) }
        node.scheduleBuffer(buffer, completionCallbackType: .dataPlayedBack) { _ in completion() }
    }

    /// Exercises the actual engine/node/converter path without device output or listening claims.
    func renderOffline(_ packet: PlayerPCM) throws -> [Float] {
        try engine.start()
        try schedule(packet) {}
        node.play()
        let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 4096)!
        var result: [Float] = []
        while result.count < packet.frameCount {
            let count = AVAudioFrameCount(min(4096, packet.frameCount - result.count))
            guard try engine.renderOffline(count, to: buffer) == .success else { throw CocoaError(.coderInvalidValue) }
            result.append(contentsOf: UnsafeBufferPointer(start: buffer.floatChannelData![0], count: Int(buffer.frameLength)))
        }
        return result
    }

    func close() {
        node.stop(); engine.stop()
        try? session?.setActive(false, options: .notifyOthersOnDeactivation)
    }
}

/// Commands/counts use a small lock; only the serial worker touches the sink. No Session crosses here.
final class PlayerAudioPump {
    private let lock = NSLock()
    private let enqueue: (@escaping () -> Void) -> Void
    private let open: (Int) throws -> PlayerPCMSink
    private let failure: () -> Void
    private var generation = 0
    private var reservedFrames = 0
    private var rate = 0
    private var failed = false
    private var packets: [PlayerPCM] = []
    private var scheduled = false
    // Worker-owned.
    private var sink: PlayerPCMSink?
    private var workerGeneration = -1
    private var toneBuffers = 0
    private var nextBuffer = 0
    private var inFlight: [Int: (frames: Int, tone: Bool)] = [:]

    init(enqueue: ((@escaping () -> Void) -> Void)? = nil,
         open: @escaping (Int) throws -> PlayerPCMSink = { try EnginePCMSink(rate: $0) },
         failure: @escaping () -> Void = {}) {
        let queue = DispatchQueue(label: "com.digihori.pgp.ios.pcm", qos: .userInteractive)
        self.enqueue = enqueue ?? { queue.async(execute: $0) }
        self.open = open; self.failure = failure
    }
    var isFailed: Bool { lock.lock(); defer { lock.unlock() }; return failed }
    var pendingFrames: Int { lock.lock(); defer { lock.unlock() }; return reservedFrames }

    // Called with the command lock held. A single work item owns the bounded queue.
    private func wake() {
        if !scheduled { scheduled = true; enqueue { [self] in process() } }
    }
    func stop(retry: Bool = false) {
        lock.lock(); defer { lock.unlock() }
        generation += 1; reservedFrames = 0; packets.removeAll()
        if retry { failed = false }
        wake()
    }
    func offer(_ packet: PlayerPCM) {
        guard packet.isValid else { return }
        lock.lock(); defer { lock.unlock() }
        guard !failed else { return }
        if rate != packet.rate || reservedFrames + packet.frameCount > packet.rate / 10 {
            generation += 1; reservedFrames = 0; packets.removeAll(); rate = packet.rate
        }
        if packet.frameCount <= packet.rate / 10 {
            packets.append(packet); reservedFrames += packet.frameCount
        }
        wake()
    }
    private func process() {
        while true {
            lock.lock()
            let current = generation
            if workerGeneration != current {
                lock.unlock()
                sink?.close(); sink = nil; toneBuffers = 0; inFlight.removeAll(); workerGeneration = current
                continue
            }
            guard !packets.isEmpty else { scheduled = false; lock.unlock(); return }
            let packet = packets.removeFirst()
            lock.unlock()
            if sink == nil && packet.samples.isEmpty { _ = consume(packet.frameCount, current); continue }
            do {
                if sink == nil { sink = try open(packet.rate) }
                guard isCurrent(current) else { continue }
                let tone = !packet.samples.isEmpty
                if tone { toneBuffers += 1 }
                nextBuffer += 1
                let bufferID = nextBuffer
                inFlight[bufferID] = (packet.frameCount, tone)
                try sink?.schedule(packet) { [weak self] in
                    guard let self else { return }
                    self.enqueue { self.complete(bufferID, current) }
                }
            } catch {
                sink?.close(); sink = nil; toneBuffers = 0; inFlight.removeAll()
                lock.lock()
                let notify = generation == current
                if notify { failed = true; reservedFrames = 0; packets.removeAll() }
                lock.unlock()
                if notify { failure() }
            }
        }
    }
    private func isCurrent(_ value: Int) -> Bool {
        lock.lock(); defer { lock.unlock() }; return generation == value && !failed
    }
    @discardableResult
    private func consume(_ count: Int, _ current: Int) -> Bool {
        lock.lock(); defer { lock.unlock() }
        guard generation == current && !failed else { return false }
        reservedFrames -= count
        return true
    }
    private func complete(_ bufferID: Int, _ current: Int) {
        guard isCurrent(current), let buffer = inFlight.removeValue(forKey: bufferID),
              consume(buffer.frames, current) else { return }
        if buffer.tone { toneBuffers -= 1 }
        if toneBuffers == 0 {
            // Remaining buffers contain only trailing zeros. Account for their cancellation
            // ourselves; AV completion on stop must never decrement a new stream's count.
            let trailingFrames = inFlight.values.reduce(0) { $0 + $1.frames }
            inFlight.removeAll()
            _ = consume(trailingFrames, current)
            sink?.close(); sink = nil
        }
    }

}

@MainActor
protocol PlayerAudioOutput: AnyObject {
    var onStatus: ((String) -> Void)? { get set }
    func start()
    func stop()
    func offer(_ packet: PlayerPCM)
}

/// Session notifications affect only fresh audio. Route removal requires an explicit Run/ON retry.
@MainActor
final class IosPlayerAudio: PlayerAudioOutput {
    var onStatus: ((String) -> Void)?
    private var running = false
    private var blocked = false
    private var interrupted = false
    private var observers: [NSObjectProtocol] = []
    private let pump: PlayerAudioPump
    private final class FailureTarget {
        weak var owner: IosPlayerAudio?
    }

    init(pump: PlayerAudioPump? = nil, observeNotifications: Bool = true) {
        let target = FailureTarget()
        self.pump = pump ?? PlayerAudioPump(failure: {
            Task { @MainActor in
                guard let owner = target.owner, owner.pump.isFailed else { return }
                owner.blocked = true
                owner.onStatus?("音声出力に失敗しました。RunまたはサウンドONで再試行できます。")
            }
        })
        target.owner = self
        guard observeNotifications else { return }
        let center = NotificationCenter.default
        for name in [AVAudioSession.interruptionNotification, AVAudioSession.routeChangeNotification,
                     AVAudioSession.mediaServicesWereLostNotification, AVAudioSession.mediaServicesWereResetNotification] {
            observers.append(center.addObserver(forName: name, object: nil, queue: .main) { [weak self] note in
                Task { @MainActor in self?.handle(note) }
            })
        }
    }
    func start() {
        running = true
        // A Run during an active interruption cannot override the system's ownership.
        blocked = interrupted
        pump.stop(retry: true)
        onStatus?(interrupted ? "音声割込み中" : "")
    }
    func stop() { running = false; pump.stop() }
    func offer(_ packet: PlayerPCM) { if running && !blocked { pump.offer(packet) } }

    func handle(_ note: Notification) {
        if note.name == AVAudioSession.interruptionNotification {
            let type = (note.userInfo?[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.uintValue
            if type == AVAudioSession.InterruptionType.began.rawValue {
                interrupted = true; blocked = true; pump.stop()
                onStatus?("音声割込み中")
            } else if type == AVAudioSession.InterruptionType.ended.rawValue {
                interrupted = false
                let options = (note.userInfo?[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.uintValue ?? 0
                if running && AVAudioSession.InterruptionOptions(rawValue: options).contains(.shouldResume) {
                    blocked = false; pump.stop(retry: true); onStatus?("")
                } else {
                    blocked = true; onStatus?("音声停止中。RunまたはサウンドONで再試行できます。")
                }
            }
        } else {
            let reason = (note.userInfo?[AVAudioSessionRouteChangeReasonKey] as? NSNumber)?.uintValue
            // Our first ambient activation may report categoryChange. It is not a device switch.
            if note.name == AVAudioSession.routeChangeNotification && reason == AVAudioSession.RouteChangeReason.categoryChange.rawValue { return }
            pump.stop()
            if note.name != AVAudioSession.routeChangeNotification || reason == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue {
                blocked = true
                onStatus?("出力先が変更・切断されました。RunまたはサウンドONで再試行できます。")
            }
            // Other route changes discard old PCM and accept fresh PCM only if already eligible.
        }
    }
    deinit {
        for observer in observers { NotificationCenter.default.removeObserver(observer) }
        pump.stop()
    }
}

/// A nil store is deliberately volatile for structural probes and host tests.
final class PlayerPlaybackPreferences {
    private let defaults: UserDefaults?
    init(defaults: UserDefaults? = nil) { self.defaults = defaults }
    var speed: String {
        get { let value = defaults?.string(forKey: "player.speed") ?? "1.0"; return Self.speeds.contains(value) ? value : "1.0" }
        set { defaults?.set(newValue, forKey: "player.speed") }
    }
    var sound: Bool {
        get { (defaults?.object(forKey: "player.sound") as? Bool) ?? true }
        set { defaults?.set(newValue, forKey: "player.sound") }
    }
    static let speeds = ["0.8", "0.9", "1.0", "1.1", "1.2"]
}
