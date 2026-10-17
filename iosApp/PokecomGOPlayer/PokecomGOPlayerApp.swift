import SwiftUI

@main
struct PokecomGOPlayerApp: App {
    @StateObject private var host = PlayerHost.initial()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            PlayerProbeView(host: host)
                .onAppear { host.setActive(scenePhase == .active) }
                .onChange(of: scenePhase) { phase in
                    if phase == .background { host.enterBackground() }
                    else { host.setActive(phase == .active) }
                }
        }
    }
}
