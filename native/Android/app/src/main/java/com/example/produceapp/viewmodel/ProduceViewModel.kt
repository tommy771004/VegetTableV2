package com.example.produceapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.produceapp.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI 狀態封裝 */
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

@HiltViewModel
class ProduceViewModel @Inject constructor(
    private val repository: ProduceRepository
) : ViewModel() {

    // === 今日菜價 ===
    private val _dailyPrices = MutableStateFlow<Resource<PaginatedResult<ProduceDto>>>(Resource.Loading)
    val dailyPrices: StateFlow<Resource<PaginatedResult<ProduceDto>>> = _dailyPrices.asStateFlow()

    // === 熱門交易 ===
    private val _topVolume = MutableStateFlow<Resource<List<ProduceDto>>>(Resource.Loading)
    val topVolume: StateFlow<Resource<List<ProduceDto>>> = _topVolume.asStateFlow()

    // === 價格異常 ===
    private val _anomalies = MutableStateFlow<Resource<List<PriceAnomalyDto>>>(Resource.Loading)
    val anomalies: StateFlow<Resource<List<PriceAnomalyDto>>> = _anomalies.asStateFlow()

    // === 天氣警報（獨立 try-catch，不影響主資料） ===
    private val _weatherAlerts = MutableStateFlow<Resource<WeatherAlertDto>>(Resource.Loading)
    val weatherAlerts: StateFlow<Resource<WeatherAlertDto>> = _weatherAlerts.asStateFlow()

    // === 省錢食譜（獨立 try-catch，不影響主資料） ===
    private val _budgetRecipes = MutableStateFlow<Resource<List<BudgetRecipeDto>>>(Resource.Loading)
    val budgetRecipes: StateFlow<Resource<List<BudgetRecipeDto>>> = _budgetRecipes.asStateFlow()

    // === 當季盛產 ===
    private val _seasonalCrops = MutableStateFlow<Resource<List<SeasonalCropDto>>>(Resource.Loading)
    val seasonalCrops: StateFlow<Resource<List<SeasonalCropDto>>> = _seasonalCrops.asStateFlow()

    // === 收藏 ===
    private val _favorites = MutableStateFlow<Resource<List<FavoriteAlertDto>>>(Resource.Loading)
    val favorites: StateFlow<Resource<List<FavoriteAlertDto>>> = _favorites.asStateFlow()

    // === 搜尋關鍵字 ===
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureJwtToken()
            loadMainData()
        }
    }

    private fun loadMainData() {
        loadDailyPrices()
        loadTopVolume()
        loadAnomalies()
        loadWeatherAlerts()
        loadBudgetRecipes()
        loadSeasonalCrops()
    }

    fun loadDailyPrices(keyword: String? = null, page: Int = 1) {
        viewModelScope.launch {
            _dailyPrices.value = Resource.Loading
            try {
                val result = repository.getDailyPrices(keyword, page)
                _dailyPrices.value = Resource.Success(result)
            } catch (e: Exception) {
                _dailyPrices.value = Resource.Error(e.message ?: "載入失敗")
            }
        }
    }

    fun search(keyword: String) {
        _searchKeyword.value = keyword
        loadDailyPrices(keyword)
    }

    private fun loadTopVolume() {
        viewModelScope.launch {
            try {
                val result = repository.getTopVolume()
                _topVolume.value = Resource.Success(result)
            } catch (e: Exception) {
                _topVolume.value = Resource.Error(e.message ?: "載入失敗")
            }
        }
    }

    private fun loadAnomalies() {
        viewModelScope.launch {
            try {
                val result = repository.getAnomalies()
                _anomalies.value = Resource.Success(result)
            } catch (e: Exception) {
                _anomalies.value = Resource.Error(e.message ?: "載入失敗")
            }
        }
    }

    private fun loadWeatherAlerts() {
        viewModelScope.launch {
            try {
                val result = repository.getWeatherAlerts()
                _weatherAlerts.value = Resource.Success(result)
            } catch (e: Exception) {
                // 獨立容錯：天氣警報失敗不影響主資料
                _weatherAlerts.value = Resource.Success(WeatherAlertDto(alertType = "None"))
            }
        }
    }

    private fun loadBudgetRecipes() {
        viewModelScope.launch {
            try {
                val result = repository.getBudgetRecipes()
                _budgetRecipes.value = Resource.Success(result)
            } catch (e: Exception) {
                // 獨立容錯：食譜失敗不影響主資料
                _budgetRecipes.value = Resource.Error(e.message ?: "載入失敗")
            }
        }
    }

    private fun loadSeasonalCrops() {
        viewModelScope.launch {
            try {
                val result = repository.getSeasonalCrops()
                _seasonalCrops.value = Resource.Success(result)
            } catch (e: Exception) {
                _seasonalCrops.value = Resource.Error(e.message ?: "載入失敗")
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val result = repository.getFavorites()
                _favorites.value = Resource.Success(result)
            } catch (e: Exception) {
                _favorites.value = Resource.Error(e.message ?: "載入失敗")
            }
        }
    }

    fun removeFavorite(produceId: String) {
        viewModelScope.launch {
            try {
                repository.removeFavorite(produceId)
                loadFavorites()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun refresh() {
        loadMainData()
    }
}
