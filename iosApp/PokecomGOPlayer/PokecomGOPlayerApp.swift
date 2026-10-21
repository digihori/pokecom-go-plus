import SwiftUI
import UIKit

@main
struct PokecomGOPlayerApp: App {
    @UIApplicationDelegateAdaptor(PlayerOrientationDelegate.self) private var orientation
    @StateObject private var host = PlayerHost.initial()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            PlayerProbeView(host: host, updateOrientation: orientation.setMachineScreenVisible)
                .onAppear { host.setActive(scenePhase == .active) }
                .onChange(of: scenePhase) { phase in
                    if phase == .background { host.enterBackground() }
                    else { host.setActive(phase == .active) }
                }
        }
    }
}

/// Orientation is a host presentation policy; emulator state stays in PlayerHost.
@MainActor
final class PlayerOrientationDelegate: NSObject, UIApplicationDelegate {
    private(set) var supportedOrientations: UIInterfaceOrientationMask = .allButUpsideDown

    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        supportedOrientations
    }

    func setMachineScreenVisible(_ visible: Bool) {
        supportedOrientations = visible ? .landscape : .allButUpsideDown
        for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
            guard scene.activationState != .unattached else { continue }
            refresh(scene)
            // A sheet may still be dismissing when SwiftUI changes its binding.
            let root = scene.windows.first(where: \.isKeyWindow)?.rootViewController
            let presented = root?.presentedViewController
            (presented?.transitionCoordinator ?? root?.transitionCoordinator)?.animate(alongsideTransition: nil) { [weak self, weak scene] _ in
                if let scene { self?.refresh(scene) }
            }
        }
    }

    private func refresh(_ scene: UIWindowScene) {
        var controller = scene.windows.first(where: \.isKeyWindow)?.rootViewController
        while let current = controller {
            current.setNeedsUpdateOfSupportedInterfaceOrientations()
            controller = current.presentedViewController
        }
        let requested: UIInterfaceOrientationMask
        if supportedOrientations == .landscape {
            requested = scene.interfaceOrientation.isLandscape ? UIInterfaceOrientationMask(rawValue: 1 << scene.interfaceOrientation.rawValue) : .landscape
        } else {
            switch UIDevice.current.orientation {
            case .portrait: requested = .portrait
            case .landscapeLeft: requested = .landscapeRight
            case .landscapeRight: requested = .landscapeLeft
            default: requested = supportedOrientations
            }
        }
        scene.requestGeometryUpdate(.iOS(interfaceOrientations: requested))
    }
}
