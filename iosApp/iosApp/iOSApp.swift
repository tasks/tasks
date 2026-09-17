import SwiftUI

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { phase in
            if #available(iOS 27.0, *), phase == .active || phase == .background {
                _Concurrency.Task { try? await SpotlightIndex.reindexAll() }
            }
        }
    }
}
