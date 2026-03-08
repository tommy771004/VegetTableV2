package com.example.produceapp.data

import android.content.Context
import android.provider.Settings
import com.example.produceapp.data.local.ProduceDao
import com.example.produceapp.data.local.ProduceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 單一資料來源 (Single Source of Truth)
 * 協調網路 API (ProduceService) 與本地資料庫 (Room ProduceDao)
 * 實作 Offline-First：先嘗試 API，失敗時退回讀取 SQLite 離線快取
 */
@Singleton
class ProduceRepository @Inject constructor(
    private val api: ProduceService,
    private val dao: ProduceDao,
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "produce_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
    }

    /** 確保 JWT Token 已取得 */
    suspend fun ensureJwtToken() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_JWT_TOKEN, null)
        if (!existing.isNullOrEmpty()) return

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val response = api.getAuthToken(AuthTokenRequest(deviceId))
        prefs.edit().putString(KEY_JWT_TOKEN, response.token).apply()
    }

    /** 今日菜價（含分頁與搜尋） */
    suspend fun getDailyPrices(keyword: String? = null, page: Int = 1): PaginatedResult<ProduceDto> {
        return try {
            val result = api.getDailyPrices(keyword, page)
            // 快取至本地資料庫
            val entities = result.items.map { it.toEntity() }
            dao.insertAll(entities)
            result
        } catch (e: Exception) {
            // 離線容錯：讀取本地快取
            val cached = dao.getAll()
            PaginatedResult(
                items = cached.map { it.toDto() },
                totalCount = cached.size,
                page = 1,
                pageSize = cached.size
            )
        }
    }

    /** 歷史價格 */
    suspend fun getPriceHistory(produceId: String): List<HistoricalPriceDto> {
        return api.getPriceHistory(produceId)
    }

    /** 今日熱門交易 */
    suspend fun getTopVolume(): List<ProduceDto> {
        return try {
            api.getTopVolume()
        } catch (e: Exception) {
            dao.getAll().map { it.toDto() }.sortedByDescending { it.transVolume }.take(10)
        }
    }

    /** 價格異常警報 */
    suspend fun getAnomalies(): List<PriceAnomalyDto> {
        return api.getAnomalies()
    }

    /** 價格預測 */
    suspend fun getForecast(produceId: String): PricePredictionDto {
        return api.getForecast(produceId)
    }

    /** 當季盛產 */
    suspend fun getSeasonalCrops(): List<SeasonalCropDto> {
        return api.getSeasonalCrops()
    }

    /** 天氣警報 */
    suspend fun getWeatherAlerts(): WeatherAlertDto {
        return try {
            api.getWeatherAlerts()
        } catch (e: Exception) {
            WeatherAlertDto(alertType = "None")
        }
    }

    /** 省錢食譜 */
    suspend fun getBudgetRecipes(): List<BudgetRecipeDto> {
        return try {
            api.getBudgetRecipes()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 市場比價 */
    suspend fun comparePrices(cropName: String): List<ProduceDto> {
        return api.comparePrices(cropName)
    }

    /** 收藏管理 */
    suspend fun getFavorites(): List<FavoriteAlertDto> {
        return api.getFavorites()
    }

    suspend fun removeFavorite(produceId: String) {
        api.removeFavorite(produceId)
    }

    /** 社群回報 */
    suspend fun submitCommunityPrice(dto: CommunityPriceSubmitDto) {
        api.submitCommunityPrice(dto)
    }

    suspend fun getCommunityPrices(cropCode: String): List<CommunityPriceDto> {
        return api.getCommunityPrices(cropCode)
    }

    // === Entity 轉換 ===

    private fun ProduceDto.toEntity() = ProduceEntity(
        cropCode = cropCode,
        cropName = cropName,
        marketName = marketName,
        avgPrice = avgPrice,
        transVolume = transVolume,
        transDate = transDate
    )

    private fun ProduceEntity.toDto() = ProduceDto(
        cropCode = cropCode,
        cropName = cropName,
        marketName = marketName,
        avgPrice = avgPrice,
        transVolume = transVolume,
        transDate = transDate
    )
}
