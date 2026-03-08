import Foundation
import SwiftData

/// 單一資料來源 (Single Source of Truth)
/// 負責協調網路 API 與 SwiftData 本地快取，實作離線優先 (Offline-First) 邏輯
@MainActor
class ProduceRepository: ObservableObject {
    @Published var dailyPrices: [ProduceDto] = []
    @Published var topVolume: [ProduceDto] = []
    @Published var anomalies: [PriceAnomalyDto] = []
    @Published var weatherAlert: WeatherAlertDto?
    @Published var budgetRecipes: [BudgetRecipeDto] = []
    @Published var favorites: [FavoriteAlertDto] = []
    @Published var seasonal: [ProduceDto] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let service = ProduceService.shared
    private var modelContext: ModelContext?

    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    // MARK: - Load All Home Data

    func loadHomeData() async {
        isLoading = true
        errorMessage = nil

        // 先載入離線快取
        loadFromCache()

        // 並行載入所有資料
        async let pricesTask: () = loadDailyPrices()
        async let topTask: () = loadTopVolume()
        async let anomalyTask: () = loadAnomalies()
        async let weatherTask: () = loadWeatherAlerts()
        async let recipeTask: () = loadBudgetRecipes()
        async let seasonalTask: () = loadSeasonal()

        _ = await (pricesTask, topTask, anomalyTask, weatherTask, recipeTask, seasonalTask)

        isLoading = false
    }

    // MARK: - Individual Loaders

    func loadDailyPrices(keyword: String? = nil, page: Int = 1) async {
        do {
            let prices = try await service.fetchDailyPrices(keyword: keyword, page: page)
            dailyPrices = prices
            saveToCache(prices)
        } catch {
            if dailyPrices.isEmpty {
                errorMessage = "無法載入菜價資料，使用離線快取"
            }
        }
    }

    func loadTopVolume() async {
        do { topVolume = try await service.fetchTopVolume() }
        catch { /* 靜默失敗，不影響主資料 */ }
    }

    func loadAnomalies() async {
        do { anomalies = try await service.fetchAnomalies() }
        catch { /* 靜默失敗 */ }
    }

    func loadWeatherAlerts() async {
        do {
            let alert = try await service.fetchWeatherAlerts()
            weatherAlert = alert.alertType == "None" ? nil : alert
        } catch { /* 天氣警報獨立容錯，靜默失敗 */ }
    }

    func loadBudgetRecipes() async {
        do { budgetRecipes = try await service.fetchBudgetRecipes() }
        catch { /* 食譜獨立容錯，靜默失敗 */ }
    }

    func loadSeasonal() async {
        do { seasonal = try await service.fetchSeasonal() }
        catch { /* 靜默失敗 */ }
    }

    func loadFavorites() async {
        do { favorites = try await service.fetchFavorites() }
        catch { errorMessage = "無法載入收藏" }
    }

    // MARK: - Offline Cache (SwiftData)

    private func saveToCache(_ prices: [ProduceDto]) {
        guard let context = modelContext else { return }
        for dto in prices {
            let entity = ProduceEntity(from: dto)
            context.insert(entity)
        }
        try? context.save()
    }

    private func loadFromCache() {
        guard let context = modelContext else { return }
        let descriptor = FetchDescriptor<ProduceEntity>(
            sortBy: [SortDescriptor(\.cropName)]
        )
        if let cached = try? context.fetch(descriptor), !cached.isEmpty {
            dailyPrices = cached.map { entity in
                ProduceDto(
                    cropCode: entity.cropCode,
                    cropName: entity.cropName,
                    marketName: entity.marketName,
                    avgPrice: entity.avgPrice,
                    transQuantity: entity.transQuantity,
                    transDate: entity.transDate
                )
            }
        }
    }
}
