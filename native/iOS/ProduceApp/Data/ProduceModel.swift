import Foundation
import SwiftData

// MARK: - API DTOs

struct ProduceDto: Codable, Identifiable {
    var id: String { cropCode }
    let cropCode: String
    let cropName: String
    let marketName: String
    let avgPrice: Double
    let transQuantity: Double
    let transDate: String
}

struct HistoricalPriceDto: Codable, Identifiable {
    var id: String { transDate }
    let transDate: String
    let avgPrice: Double
}

struct PriceAnomalyDto: Codable, Identifiable {
    var id: String { cropCode }
    let cropCode: String
    let cropName: String
    let previousPrice: Double
    let currentPrice: Double
    let changePercent: Double
}

struct PricePredictionDto: Codable {
    let produceId: String
    let currentAvgPrice: Double
    let predictedPrice: Double
    let trend: String
    let movingAverage7Day: Double
}

struct WeatherAlertDto: Codable {
    let alertType: String
    let severity: String
    let title: String
    let message: String
    let affectedCrops: [String]
}

struct BudgetRecipeDto: Codable, Identifiable {
    var id: String { recipeName }
    let recipeName: String
    let mainIngredients: [String]
    let reason: String
    let imageUrl: String
    let steps: [String]
}

struct FavoriteAlertDto: Codable, Identifiable {
    var id: String { produceId }
    let produceId: String
    let cropName: String
    let targetPrice: Double
    let currentPrice: Double?
    let isAlertTriggered: Bool
}

struct CommunityPriceDto: Codable, Identifiable {
    var id: String { "\(cropCode)_\(reportedAt)" }
    let cropCode: String
    let retailPrice: Double
    let market: String
    let reportedAt: String
}

// MARK: - SwiftData Entity (Offline Cache)

@Model
final class ProduceEntity {
    @Attribute(.unique) var cropCode: String
    var cropName: String
    var marketName: String
    var avgPrice: Double
    var transQuantity: Double
    var transDate: String
    var lastUpdated: Date

    init(cropCode: String, cropName: String, marketName: String,
         avgPrice: Double, transQuantity: Double, transDate: String) {
        self.cropCode = cropCode
        self.cropName = cropName
        self.marketName = marketName
        self.avgPrice = avgPrice
        self.transQuantity = transQuantity
        self.transDate = transDate
        self.lastUpdated = Date()
    }

    convenience init(from dto: ProduceDto) {
        self.init(
            cropCode: dto.cropCode,
            cropName: dto.cropName,
            marketName: dto.marketName,
            avgPrice: dto.avgPrice,
            transQuantity: dto.transQuantity,
            transDate: dto.transDate
        )
    }
}
