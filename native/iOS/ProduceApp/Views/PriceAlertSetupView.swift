import SwiftUI

struct PriceAlertSetupView: View {
    @ObservedObject var repository: ProduceRepository

    var body: some View {
        List {
            if repository.favorites.isEmpty {
                ContentUnavailableView(
                    "尚未設定提醒",
                    systemImage: "bell.slash",
                    description: Text("先在首頁收藏農產品，即可設定目標價格提醒")
                )
            } else {
                ForEach(repository.favorites) { item in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(item.cropName)
                                .font(.headline)
                            Spacer()
                            Image(systemName: item.isAlertTriggered
                                  ? "bell.badge.fill" : "bell")
                                .foregroundColor(item.isAlertTriggered ? .green : .gray)
                        }

                        HStack {
                            VStack(alignment: .leading) {
                                Text("目標價")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text("$\(String(format: "%.1f", item.targetPrice))")
                                    .fontWeight(.bold)
                            }

                            Spacer()

                            if let currentPrice = item.currentPrice {
                                VStack(alignment: .trailing) {
                                    Text("目前價")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text("$\(String(format: "%.1f", currentPrice))")
                                        .fontWeight(.bold)
                                        .foregroundColor(
                                            currentPrice <= item.targetPrice
                                            ? .green : .primary
                                        )
                                }
                            }
                        }

                        if item.isAlertTriggered {
                            Text("已達到目標價格！")
                                .font(.caption)
                                .foregroundColor(.green)
                                .fontWeight(.medium)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("目標提醒設定")
        .task { await repository.loadFavorites() }
        .refreshable { await repository.loadFavorites() }
    }
}
