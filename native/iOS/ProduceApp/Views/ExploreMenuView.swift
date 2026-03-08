import SwiftUI

struct ExploreMenuView: View {
    @ObservedObject var repository: ProduceRepository

    var body: some View {
        List {
            Section("進階功能") {
                NavigationLink {
                    ElderlyModeView(repository: repository)
                } label: {
                    Label("長輩友善模式", systemImage: "figure.and.child.holdinghands")
                }

                NavigationLink {
                    SeasonalCalendarView(repository: repository)
                } label: {
                    Label("當季蔬果日曆", systemImage: "calendar")
                }

                NavigationLink {
                    CommunityReportView()
                } label: {
                    Label("社群物價回報", systemImage: "person.3.fill")
                }

                NavigationLink {
                    PriceAlertSetupView(repository: repository)
                } label: {
                    Label("目標提醒設定", systemImage: "bell.badge")
                }
            }
        }
        .navigationTitle("探索")
    }
}
