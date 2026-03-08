import SwiftUI

struct SeasonalCalendarView: View {
    @ObservedObject var repository: ProduceRepository
    @State private var selectedMonth = Calendar.current.component(.month, from: Date())

    var body: some View {
        VStack(spacing: 16) {
            // 月份選擇器
            Picker("月份", selection: $selectedMonth) {
                ForEach(1...12, id: \.self) { month in
                    Text("\(month)月").tag(month)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)

            // 當季農產品列表
            if repository.seasonal.isEmpty {
                ContentUnavailableView(
                    "載入中",
                    systemImage: "leaf.fill",
                    description: Text("正在載入當季盛產資料...")
                )
            } else {
                ScrollView {
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 12) {
                        ForEach(repository.seasonal) { item in
                            VStack(spacing: 8) {
                                Text("🥬")
                                    .font(.largeTitle)
                                Text(item.cropName)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                Text("$\(String(format: "%.0f", item.avgPrice))")
                                    .font(.caption)
                                    .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))
                            }
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(.ultraThinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    .padding(.horizontal)
                }
            }
        }
        .navigationTitle("當季蔬果日曆")
        .task { await repository.loadSeasonal() }
    }
}
