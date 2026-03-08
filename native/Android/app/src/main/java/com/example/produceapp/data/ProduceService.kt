package com.example.produceapp.data

import retrofit2.http.*

/**
 * Retrofit 網路請求介面 - 定義所有後端 API 端點
 */
interface ProduceService {

    // === 認證 ===
    @POST("auth/token")
    suspend fun getAuthToken(@Body request: AuthTokenRequest): AuthTokenResponse

    // === 今日菜價 ===
    @GET("daily-prices")
    suspend fun getDailyPrices(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): PaginatedResult<ProduceDto>

    // === 歷史價格 ===
    @GET("history/{produceId}")
    suspend fun getPriceHistory(@Path("produceId") produceId: String): List<HistoricalPriceDto>

    // === 熱門交易 ===
    @GET("top-volume")
    suspend fun getTopVolume(): List<ProduceDto>

    // === 價格異常 ===
    @GET("anomalies")
    suspend fun getAnomalies(): List<PriceAnomalyDto>

    // === 價格預測 ===
    @GET("forecast/{produceId}")
    suspend fun getForecast(@Path("produceId") produceId: String): PricePredictionDto

    // === 當季盛產 ===
    @GET("seasonal")
    suspend fun getSeasonalCrops(): List<SeasonalCropDto>

    // === 天氣警報 ===
    @GET("weather-alerts")
    suspend fun getWeatherAlerts(): WeatherAlertDto

    // === 省錢食譜 ===
    @GET("budget-recipes")
    suspend fun getBudgetRecipes(): List<BudgetRecipeDto>

    // === 市場比價 ===
    @GET("compare/{cropName}")
    suspend fun comparePrices(@Path("cropName") cropName: String): List<ProduceDto>

    // === 收藏管理 ===
    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteAlertDto>

    @DELETE("favorites/{produceId}")
    suspend fun removeFavorite(@Path("produceId") produceId: String)

    // === 社群回報 ===
    @POST("community-price")
    suspend fun submitCommunityPrice(@Body dto: CommunityPriceSubmitDto)

    @GET("community-price/{cropCode}")
    suspend fun getCommunityPrices(@Path("cropCode") cropCode: String): List<CommunityPriceDto>

    // === FCM Token ===
    @POST("fcm/token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest)
}
