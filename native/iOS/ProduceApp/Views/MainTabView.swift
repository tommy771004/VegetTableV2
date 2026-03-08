import SwiftUI

struct MainTabView: View {
    @StateObject private var repository = ProduceRepository()

    var body: some View {
        TabView {
            NavigationStack {
                HomeScreen(repository: repository)
            }
            .tabItem {
                Label("首頁", systemImage: "house.fill")
            }

            NavigationStack {
                FavoritesScreen(repository: repository)
            }
            .tabItem {
                Label("收藏", systemImage: "heart.fill")
            }

            NavigationStack {
                ExploreMenuView(repository: repository)
            }
            .tabItem {
                Label("探索", systemImage: "safari.fill")
            }

            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("設定", systemImage: "gearshape.fill")
            }
        }
        .tint(Color(red: 0.18, green: 0.49, blue: 0.2))
    }
}

// MARK: - Settings View

struct SettingsView: View {
    @AppStorage("push_enabled") private var pushEnabled = true
    @AppStorage("offline_cache") private var offlineCacheEnabled = true
    @AppStorage("app_language") private var selectedLanguage = "zh-TW"

    var body: some View {
        Form {
            Section("通知") {
                Toggle("推播通知", isOn: $pushEnabled)
                Toggle("離線快取", isOn: $offlineCacheEnabled)
            }

            Section("語言設定") {
                Picker("語言", selection: $selectedLanguage) {
                    Text("繁體中文").tag("zh-TW")
                    Text("Bahasa Indonesia").tag("id")
                    Text("Tiếng Việt").tag("vi")
                }
                .pickerStyle(.inline)
            }
        }
        .navigationTitle("設定")
    }
}
