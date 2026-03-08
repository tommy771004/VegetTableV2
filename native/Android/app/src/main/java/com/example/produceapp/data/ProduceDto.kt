package com.example.produceapp.data

data class ProduceDto(
    val cropCode: String,
    val cropName: String,
    val marketName: String,
    val upperPrice: Double = 0.0,
    val middlePrice: Double = 0.0,
    val lowerPrice: Double = 0.0,
    val avgPrice: Double = 0.0,
    val transVolume: Double = 0.0,
    val transDate: String = ""
)

data class PaginatedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

data class HistoricalPriceDto(
    val date: String,
    val avgPrice: Double,
    val transVolume: Double
)

data class PricePredictionDto(
    val predictedPrice: Double,
    val trend: String,
    val movingAverage7Day: Double
)

data class PriceAnomalyDto(
    val cropCode: String,
    val cropName: String,
    val previousPrice: Double,
    val currentPrice: Double,
    val changePercent: Double,
    val transDate: String
)

data class WeatherAlertDto(
    val alertType: String = "None",
    val severity: String = "",
    val title: String = "",
    val message: String = "",
    val affectedCrops: List<String> = emptyList()
)

data class BudgetRecipeDto(
    val recipeName: String,
    val mainIngredients: List<String>,
    val reason: String,
    val imageUrl: String, // Emoji character
    val steps: List<String>
)

data class FavoriteAlertDto(
    val id: Int,
    val userId: String,
    val produceId: String,
    val cropName: String,
    val targetPrice: Double,
    val currentPrice: Double?,
    val isAlertTriggered: Boolean
)

data class CommunityPriceDto(
    val id: Int,
    val cropCode: String,
    val retailPrice: Double,
    val location: String,
    val reportedBy: String,
    val reportedAt: String
)

data class CommunityPriceSubmitDto(
    val cropCode: String,
    val retailPrice: Double,
    val location: String
)

data class SeasonalCropDto(
    val cropCode: String,
    val cropName: String,
    val seasonMonths: List<Int>
)

data class AuthTokenRequest(val deviceId: String)
data class AuthTokenResponse(val token: String)
data class FcmTokenRequest(val fcmToken: String)
