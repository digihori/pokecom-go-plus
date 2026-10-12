import Foundation
import CryptoKit
import zlib
import PgpPlayer

struct RomFailure: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/// Bounded ZIP v1 subset for pgrom: stored/raw-deflate, central directory, no encryption/ZIP64.
/// Entries stay in memory; names are never used as filesystem destinations.
enum RomArchive {
    static let maximumBytes = 32 * 1024 * 1024
    static let maximumEntryBytes = 16 * 1024 * 1024
    static func read(_ data: Data) throws -> [String: Data] {
        guard data.count >= 22, data.count <= maximumBytes else { throw RomFailure(message: "ROM packageのサイズが不正です。") }
        func word(_ offset: Int, _ bytes: Int = 2) throws -> Int {
            guard offset >= 0, offset <= data.count - bytes else { throw RomFailure(message: "ZIPが途中で切れています。") }
            return (0..<bytes).reduce(0) { $0 | Int(data[offset + $1]) << (8 * $1) }
        }
        var end: Int?
        for offset in stride(from: data.count - 22, through: max(0, data.count - 65_557), by: -1) {
            if try word(offset, 4) == 0x06054b50, offset + 22 + (try word(offset + 20)) == data.count { end = offset; break }
        }
        guard let end, try word(end + 4) == 0, try word(end + 6) == 0 else { throw RomFailure(message: "分割ZIPには対応していません。") }
        let count = try word(end + 10), centralSize = try word(end + 12, 4), central = try word(end + 16, 4)
        guard count > 0, count <= 128, try word(end + 8) == count, central + centralSize == end else {
            throw RomFailure(message: "ZIPのentry数・directoryが不正です。")
        }
        var cursor = central, expanded = 0, names = Set<String>(), files: [String: Data] = [:]
        var spans: [Range<Int>] = []
        for _ in 0..<count {
            guard cursor <= end - 46, try word(cursor, 4) == 0x02014b50 else { throw RomFailure(message: "ZIP directoryが不正です。") }
            let flags = try word(cursor + 8), method = try word(cursor + 10), checksum = try word(cursor + 16, 4)
            let packed = try word(cursor + 20, 4), size = try word(cursor + 24, 4)
            let nameSize = try word(cursor + 28), extra = try word(cursor + 30), comment = try word(cursor + 32)
            let local = try word(cursor + 42, 4), attrs = try word(cursor + 38, 4)
            guard flags & ~0x080e == 0, method == 0 || method == 8,
                  size <= maximumEntryBytes, expanded + size <= maximumBytes,
                  local < central, try word(cursor + 34) == 0,
                  (attrs >> 16) & 0xf000 != 0xa000,
                  cursor + 46 + nameSize + extra + comment <= end else { throw RomFailure(message: "未対応または不正なZIP entryです。") }
            let nameBytes = data.subdata(in: cursor + 46..<cursor + 46 + nameSize)
            guard let name = String(data: nameBytes, encoding: .utf8), safeName(name), names.insert(name).inserted else {
                throw RomFailure(message: "ZIPに不正・重複したpathがあります。")
            }
            guard try word(local, 4) == 0x04034b50, try word(local + 6) == flags, try word(local + 8) == method else {
                throw RomFailure(message: "ZIP headerが一致しません。")
            }
            let localNameSize = try word(local + 26), localExtra = try word(local + 28)
            let start = local + 30 + localNameSize + localExtra
            guard start <= central, packed <= central - start, local + 30 + localNameSize <= central,
                  data.subdata(in: local + 30..<local + 30 + localNameSize) == nameBytes else { throw RomFailure(message: "ZIP entryが途中で切れています。") }
            if flags & 8 == 0 {
                guard try word(local + 14, 4) == checksum, try word(local + 18, 4) == packed, try word(local + 22, 4) == size else {
                    throw RomFailure(message: "ZIPのサイズ・CRCが一致しません。")
                }
            }
            let span = local..<start + packed
            guard !spans.contains(where: { $0.overlaps(span) }) else { throw RomFailure(message: "ZIP entryが重複しています。") }
            spans.append(span)
            let payload = data.subdata(in: start..<start + packed)
            let bytes = try method == 0 ? payload : inflateRaw(payload, size: size)
            guard bytes.count == size, crc(bytes) == UInt32(checksum) else { throw RomFailure(message: "ZIPのCRC・展開サイズが不正です。") }
            if name.hasSuffix("/") {
                guard bytes.isEmpty else { throw RomFailure(message: "ZIP directoryにデータがあります。") }
            } else { files[name] = bytes }
            expanded += size
            cursor += 46 + nameSize + extra + comment
        }
        guard cursor == end else { throw RomFailure(message: "ZIP directoryの長さが不正です。") }
        return files
    }
    private static func safeName(_ name: String) -> Bool {
        let trimmed = name.hasSuffix("/") ? String(name.dropLast()) : name
        return !trimmed.isEmpty && !name.hasPrefix("/") && !name.contains("\\") && !name.contains("\0") &&
            trimmed.split(separator: "/", omittingEmptySubsequences: false).allSatisfy { !$0.isEmpty && $0 != "." && $0 != ".." }
    }
    private static func inflateRaw(_ data: Data, size: Int) throws -> Data {
        var stream = z_stream()
        guard inflateInit2_(&stream, -MAX_WBITS, ZLIB_VERSION, Int32(MemoryLayout<z_stream>.size)) == Z_OK else { throw RomFailure(message: "ZIP展開を開始できません。") }
        defer { inflateEnd(&stream) }
        var output = Data(count: max(1, size))
        let result = data.withUnsafeBytes { input in
            output.withUnsafeMutableBytes { out in
                stream.next_in = UnsafeMutablePointer(mutating: input.bindMemory(to: Bytef.self).baseAddress)
                stream.avail_in = uInt(data.count)
                stream.next_out = out.bindMemory(to: Bytef.self).baseAddress
                stream.avail_out = uInt(max(1, size))
                return inflate(&stream, Z_FINISH)
            }
        }
        guard result == Z_STREAM_END, Int(stream.total_out) == size, Int(stream.total_in) == data.count else { throw RomFailure(message: "ZIPのdeflateデータが不正です。") }
        return output.prefix(size)
    }
    static func crc(_ data: Data) -> UInt32 {
        data.withUnsafeBytes { UInt32(crc32(0, $0.bindMemory(to: Bytef.self).baseAddress, uInt(data.count))) }
    }
    static func write(_ files: [String: Data]) -> Data {
        var result = Data(), central = Data()
        for name in files.keys.sorted() {
            let bytes = files[name]!, path = Data(name.utf8), offset = result.count, checksum = crc(bytes)
            result.appendLE(0x04034b50, 4); result.appendLE(20, 2); result.appendLE(0x800, 2)
            result.appendLE(0, 2); result.appendLE(0, 2); result.appendLE(0, 2); result.appendLE(Int(checksum), 4)
            result.appendLE(bytes.count, 4); result.appendLE(bytes.count, 4); result.appendLE(path.count, 2); result.appendLE(0, 2)
            result.append(path); result.append(bytes)
            central.appendLE(0x02014b50, 4); central.appendLE(20, 2); central.appendLE(20, 2); central.appendLE(0x800, 2)
            central.appendLE(0, 2); central.appendLE(0, 2); central.appendLE(0, 2); central.appendLE(Int(checksum), 4)
            central.appendLE(bytes.count, 4); central.appendLE(bytes.count, 4); central.appendLE(path.count, 2)
            for _ in 0..<4 { central.appendLE(0, 2) }
            central.appendLE(0, 4); central.appendLE(offset, 4); central.append(path)
        }
        let offset = result.count
        result.append(central); result.appendLE(0x06054b50, 4); result.appendLE(0, 2); result.appendLE(0, 2)
        result.appendLE(files.count, 2); result.appendLE(files.count, 2); result.appendLE(central.count, 4); result.appendLE(offset, 4); result.appendLE(0, 2)
        return result
    }
}

extension Data {
    fileprivate mutating func appendLE(_ value: Int, _ count: Int) {
        for byte in 0..<count { append(UInt8(truncatingIfNeeded: value >> (8 * byte))) }
    }
    var kotlinBytes: KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() { result.set(index: Int32(index), value: Int8(bitPattern: byte)) }
        return result
    }
    init(kotlin: KotlinByteArray) {
        self.init((0..<kotlin.size).map { UInt8(bitPattern: kotlin.get(index: $0)) })
    }
    var sha256: String { SHA256.hash(data: self).map { String(format: "%02x", $0) }.joined() }
}

struct RomManifest: Codable {
    struct Component: Codable { let id: String; let role: String; let file: String; let size: Int; let sha256: String }
    let format: String
    let formatVersion: Int
    let machineId: String
    let title: String?
    let components: [Component]
}
struct ReadRom {
    let sourceName: String
    let legacy: Data?
    let manifest: RomManifest?
    let files: [String: Data]
    let unlistedCount: Int
}
struct PreparedRom {
    let player: ApplePlayerFacade
    let archive: Data
    let sourceName: String
    let unlistedCount: Int
}

enum IosRomCodec {
    static func read(url: URL) throws -> ReadRom {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        return try read(bytes: boundedRead(url), name: url.lastPathComponent)
    }
    static func boundedRead(_ url: URL) throws -> Data {
        let handle = try FileHandle(forReadingFrom: url)
        defer { try? handle.close() }
        var result = Data()
        while let chunk = try handle.read(upToCount: 65_536), !chunk.isEmpty {
            guard result.count + chunk.count <= RomArchive.maximumBytes else { throw RomFailure(message: "ROMファイルは32MiBまでです。") }
            result.append(chunk)
        }
        return result
    }
    static func read(bytes: Data, name: String) throws -> ReadRom {
        if bytes.count == 0x8000 || bytes.count == 0x10000, !bytes.starts(with: [0x50, 0x4b]) {
            return ReadRom(sourceName: name, legacy: bytes, manifest: nil, files: [:], unlistedCount: 0)
        }
        let files = try RomArchive.read(bytes)
        guard let json = files["manifest.json"] else { throw RomFailure(message: "manifest.jsonがありません。") }
        guard let fields = try JSONSerialization.jsonObject(with: json) as? [String: Any],
              Set(fields.keys).isSubset(of: ["format", "formatVersion", "machineId", "title", "components"]),
              let components = fields["components"] as? [[String: Any]],
              components.allSatisfy({ Set($0.keys).isSubset(of: ["id", "role", "file", "size", "sha256"]) }) else {
            throw RomFailure(message: "ROM manifestの形式が不正です。")
        }
        let manifest = try JSONDecoder().decode(RomManifest.self, from: json)
        guard manifest.machineId == "pc-1245", manifest.components.count == 2 else { throw RomFailure(message: "内部・外部ROMを含むPC-1245 packageを選択してください。") }
        let keys = Set(manifest.components.map(\.file))
        let listed = files.filter { keys.contains($0.key) }
        guard listed.values.allSatisfy({ $0.count <= 16_384 }) else { throw RomFailure(message: "PC-1245のcomponentサイズが不正です。") }
        return ReadRom(sourceName: name, legacy: nil, manifest: manifest, files: listed,
                       unlistedCount: files.keys.filter { $0 != "manifest.json" && !keys.contains($0) }.count)
    }
    @MainActor static func prepare(_ input: ReadRom) throws -> PreparedRom {
        let prepared: AppleRomPreparation
        if let legacy = input.legacy { prepared = ApplePlayerFactory.shared.prepareLegacy(bytes: legacy.kotlinBytes) }
        else if let manifest = input.manifest {
            guard manifest.formatVersion >= 0, manifest.formatVersion <= Int(Int32.max),
                  manifest.components.allSatisfy({ $0.size >= 0 && $0.size <= Int(Int32.max) }) else { throw RomFailure(message: "ROM manifestの数値が不正です。") }
            let native = RomPackageManifest(format: manifest.format, formatVersion: Int32(manifest.formatVersion), machineId: manifest.machineId,
                title: manifest.title, components: manifest.components.map { RomPackageComponentManifest(id: $0.id, role: $0.role, file: $0.file, size: Int32($0.size), sha256: $0.sha256) })
            let entries = input.files.map { AppleRomArchiveEntry(path: $0.key, sha256: $0.value.sha256, bytes: $0.value.kotlinBytes) }
            prepared = ApplePlayerFactory.shared.preparePackage(manifest: native, entries: entries)
        } else { throw RomFailure(message: "ROMを読み込めませんでした。") }
        guard let player = prepared.player, let internalRom = prepared.internalBytes(), let externalRom = prepared.externalBytes() else {
            throw RomFailure(message: prepared.errorMessage ?? "Sessionを生成できませんでした。")
        }
        let components = [("internal", Data(kotlin: internalRom)), ("external", Data(kotlin: externalRom))]
        let manifest = RomManifest(format: "pgp-rom-package", formatVersion: 1, machineId: "pc-1245", title: nil,
            components: components.map { RomManifest.Component(id: $0.0, role: $0.0, file: "rom/\($0.0).bin", size: $0.1.count, sha256: $0.1.sha256) })
        var files = Dictionary(uniqueKeysWithValues: components.map { ("rom/\($0.0).bin", $0.1) })
        files["manifest.json"] = try JSONEncoder().encode(manifest)
        return PreparedRom(player: player, archive: RomArchive.write(files), sourceName: input.sourceName, unlistedCount: input.unlistedCount)
    }
}

/// Single PC-1245 record: package and metadata are committed in one atomic file replacement.
final class IosRomStore: Sendable {
    struct Record: Codable { let version: Int; let machineId: String; let sourceName: String; let archive: Data }
    let root: URL
    let write: @Sendable (Data, URL) throws -> Void
    var recordURL: URL { root.appendingPathComponent("pc-1245.json") }
    init(root: URL, write: @escaping @Sendable (Data, URL) throws -> Void = { try $0.write(to: $1, options: .atomic) }) {
        self.root = root; self.write = write
    }
    static func applicationStore() -> IosRomStore {
        let root = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("rom-library", isDirectory: true)
        return IosRomStore(root: root)
    }
    func load() throws -> ReadRom? {
        guard FileManager.default.fileExists(atPath: recordURL.path) else { return nil }
        let record = try JSONDecoder().decode(Record.self, from: IosRomCodec.boundedRead(recordURL))
        guard record.version == 1, record.machineId == "pc-1245" else { throw RomFailure(message: "保存ROMの機種・形式が不正です。") }
        return try IosRomCodec.read(bytes: record.archive, name: record.sourceName)
    }
    func install(archive: Data, sourceName: String) throws {
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        var directory = root
        var values = URLResourceValues(); values.isExcludedFromBackup = true
        try directory.setResourceValues(values)
        let record = Record(version: 1, machineId: "pc-1245", sourceName: sourceName, archive: archive)
        try write(JSONEncoder().encode(record), recordURL)
    }
    func delete() throws {
        if FileManager.default.fileExists(atPath: recordURL.path) { try FileManager.default.removeItem(at: recordURL) }
    }
}
