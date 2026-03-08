import SwiftUI

struct FavoritesScreen: View {
    @ObservedObject var repository: ProduceRepository

    var body: some View {
        List {
            if repository.favorites.isEmpty {
                ContentUnavailableView(
                    "尚無收藏",
                    systemImage: "heart.slash",
                    description: Text("在首頁瀏覽農產品時可加入收藏")
                )
            } else {
                ForEach(repository.favorites) { item in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.cropName)
                                .fontWeight(.medium)
                            if let currentPrice = item.currentPrice {
                                Text("目前均價：$\(String(format: "%.1f", currentPrice))")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Text("目標價：$\(String(format: "%.1f", item.targetPrice))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        // 根據 isAlertTriggered 顯示不同鈴鐺狀態
                        Image(systemName: item.isAlertTriggered
                              ? "bell.badge.fill" : "bell")
                            .foregroundColor(item.isAlertTriggered ? .green : .gray)
                            .font(.title2)
                    }
                    .padding(.vertical, 4)
                }
                .onDelete { indexSet in
                    for index in indexSet {
                        let item = repository.favorites[index]
                        Task {
                            try? await ProduceService.shared.deleteFavorite(produceId: item.produceId)
                            await repository.loadFavorites()
                        }
                    }
                }
            }
        }
        .navigationTitle("我的收藏")
        .task { await repository.loadFavorites() }
        .refreshable { await repository.loadFavorites() }
    }
}
