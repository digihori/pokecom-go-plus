// Original vector artwork for the distribution disk; no application artwork is modified.
// Run on macOS: swift scripts/generate-dmg-icon.swift desktopApp/icons
import AppKit
import Foundation

let destination = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "desktopApp/icons")
let iconset = destination.appendingPathComponent("pokecom-go-dmg.iconset")
try FileManager.default.createDirectory(at: iconset, withIntermediateDirectories: true)
func render(_ pixels: Int) -> Data {
    let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: pixels, pixelsHigh: pixels,
                                  bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
                                  isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: bitmap)
    let scale = CGFloat(pixels) / 1024
    NSAffineTransform(transform: AffineTransform(scale: scale)).concat()
    func rounded(_ rect: NSRect, _ radius: CGFloat, _ color: NSColor) {
        color.setFill()
        NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
    }
    rounded(NSRect(x: 112, y: 114, width: 800, height: 730), 92, NSColor(calibratedWhite: 0.19, alpha: 1))
    rounded(NSRect(x: 112, y: 174, width: 800, height: 730), 92, NSColor(calibratedWhite: 0.82, alpha: 1))
    rounded(NSRect(x: 150, y: 380, width: 724, height: 468), 54, NSColor(calibratedRed: 0.18, green: 0.32, blue: 0.44, alpha: 1))
    func text(_ value: String, _ size: CGFloat, _ y: CGFloat, _ color: NSColor) {
        let attributes: [NSAttributedString.Key: Any] = [.font: NSFont.boldSystemFont(ofSize: size), .foregroundColor: color]
        let string = value as NSString
        let width = string.size(withAttributes: attributes).width
        string.draw(at: NSPoint(x: (1024 - width) / 2, y: y), withAttributes: attributes)
    }
    text("PGP", 145, 625, .white)
    text("DMG", 112, 451, .white)
    rounded(NSRect(x: 184, y: 246, width: 430, height: 22), 11, NSColor(calibratedWhite: 0.35, alpha: 1))
    NSColor(calibratedRed: 0.24, green: 0.64, blue: 0.58, alpha: 1).setFill()
    NSBezierPath(ovalIn: NSRect(x: 760, y: 234, width: 46, height: 46)).fill()
    NSGraphicsContext.restoreGraphicsState()
    return bitmap.representation(using: .png, properties: [:])!
}
for points in [16, 32, 128, 256, 512] {
    for multiplier in [1, 2] {
        let suffix = multiplier == 2 ? "@2x" : ""
        try render(points * multiplier).write(to: iconset.appendingPathComponent("icon_\(points)x\(points)\(suffix).png"))
    }
}
try render(1024).write(to: destination.appendingPathComponent("pokecom-go-dmg.png"))
let process = Process()
process.executableURL = URL(fileURLWithPath: "/usr/bin/iconutil")
process.arguments = ["-c", "icns", iconset.path, "-o", destination.appendingPathComponent("pokecom-go-dmg.icns").path]
try process.run()
process.waitUntilExit()
guard process.terminationStatus == 0 else { exit(process.terminationStatus) }
try FileManager.default.removeItem(at: iconset)
