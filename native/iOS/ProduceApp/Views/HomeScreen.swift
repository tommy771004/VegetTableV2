import SwiftUI

struct HomeScreen: View {
    @ObservedObject var repository: ProduceRepository
    @State private var searchKeyword = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // 搜尋列
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("搜尋農產品...", text: $searchKeyword)
                        .onSubmit {
                            Task { await repository.loadDailyPrices(keyword: searchKeyword) }
                        }
                }
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))

                // ① 價格異常警報
                if !repository.anomalies.isEmpty {
                    anomalySection
                }

                // ② 今日熱門交易
                if !repository.topVolume.isEmpty {
                    topVolumeSection
                }

                // ③ 今日菜價
                dailyPriceSection

                // ④ 天氣預警
                if let alert = repository.weatherAlert {
                    weatherAlertCard(alert)
                }

                // ⑤ 省錢食譜
                if !repository.budgetRecipes.isEmpty {
                    budgetRecipeSection
                }

                // ⑥ 當季盛產
                if !repository.seasonal.isEmpty {
                    seasonalSection
                }
            }
            .padding()
        }
        .background(
            LinearGradient(
                colors: [Color(red: 0.91, green: 0.96, blue: 0.91), Color.white],
                startPoint: .top, endPoint: .bottom
            )
        )
        .navigationTitle("菜價查詢")
        .task {
            await ProduceService.shared.ensureJwtToken()
            await repository.loadHomeData()
        }
        .refreshable {
            await repository.loadHomeData()
        }
    }

    // MARK: - Sections

    private var anomalySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("價格異常警報", systemImage: "exclamationmark.triangle.fill")
                .font(.headline)
                .foregroundColor(.red)

            ForEach(repository.anomalies) { anomaly in
                HStack {
                    Text(anomaly.cropName).fontWeight(.medium)
                    Spacer()
                    Text("↑ \(String(format: "%.0f", anomaly.changePercent))%")
                        .foregroundColor(.red)
                        .fontWeight(.bold)
                }
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var topVolumeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("今日熱門交易", systemImage: "flame.fill")
                .font(.headline)
                .foregroundColor(.orange)

            ForEach(repository.topVolume.prefix(5)) { item in
                HStack {
                    Text(item.cropName)
                    Spacer()
                    Text("$\(String(format: "%.1f", item.avgPrice))")
                        .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))
                        .fontWeight(.semibold)
                }
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var dailyPriceSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("今日菜價", systemImage: "leaf.fill")
                .font(.headline)
                .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))

            if repository.isLoading && repository.dailyPrices.isEmpty {
                ForEach(0..<5, id: \.self) { _ in
                    SkeletonView()
                }
            } else {
                ForEach(repository.dailyPrices) { item in
                    NavigationLink {
                        PriceChartView(produceId: item.cropCode, produceName: item.cropName)
                    } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(item.cropName).fontWeight(.medium)
                                Text(item.marketName)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("$\(String(format: "%.1f", item.avgPrice))")
                                .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))
                                .fontWeight(.bold)
                        }
                        .padding(12)
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func weatherAlertCard(_ alert: WeatherAlertDto) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(alert.title, systemImage: "cloud.bolt.rain.fill")
                .font(.headline)
                .foregroundColor(.orange)
            Text(alert.message)
                .font(.subheadline)
            if !alert.affectedCrops.isEmpty {
                Text("受影響作物：\(alert.affectedCrops.joined(separator: "、"))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var budgetRecipeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("省錢食譜推薦", systemImage: "fork.knife")
                .font(.headline)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(repository.budgetRecipes) { recipe in
                        VStack(alignment: .leading) {
                            Text(recipe.imageUrl).font(.largeTitle)
                            Text(recipe.recipeName).fontWeight(.medium)
                            Text(recipe.mainIngredients.joined(separator: "、"))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .frame(width: 160)
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
        }
    }

    private var seasonalSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("當季盛產", systemImage: "calendar")
                .font(.headline)
                .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(repository.seasonal) { item in
                        Text(item.cropName)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(red: 0.18, green: 0.49, blue: 0.2).opacity(0.15))
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }
}
