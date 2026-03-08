import SwiftUI
import SwiftData

@main
struct ProduceApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
        .modelContainer(for: ProduceEntity.self)
    }
}
