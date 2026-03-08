package com.example.produceapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.produceapp.data.ProduceDto
import com.example.produceapp.viewmodel.ProduceViewModel
import com.example.produceapp.viewmodel.Resource

/**
 * 毛玻璃效果 Modifier
 * 半透明淡綠色底色 + 白色細邊框 + 圓角
 */
fun Modifier.liquidGlass(): Modifier = this
    .background(
        color = Color(0x40A5D6A7),
        shape = RoundedCornerShape(16.dp)
    )
    .border(
        width = 1.dp,
        color = Color(0x60FFFFFF),
        shape = RoundedCornerShape(16.dp)
    )

/**
 * 首頁 UI - 10 個資訊區塊依緊急程度排序
 * ① 價格異常警報 → ② 今日熱門交易 → ③ 語音搜尋
 * → ④ 今日菜價 → ⑤ 天氣預警 → ⑥ 省錢食譜
 * → ⑦ 趨勢圖表 → ⑧ 當季盛產 → ⑨ 最近市場 → ⑩ 購物清單入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProduceViewModel = hiltViewModel(),
    onNavigateToChart: (String, String) -> Unit = { _, _ -> },
    onNavigateToShoppingList: () -> Unit = {}
) {
    val dailyPrices by viewModel.dailyPrices.collectAsState()
    val topVolume by viewModel.topVolume.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()
    val weatherAlerts by viewModel.weatherAlerts.collectAsState()
    val budgetRecipes by viewModel.budgetRecipes.collectAsState()
    val seasonalCrops by viewModel.seasonalCrops.collectAsState()
    var searchText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 標題
        item {
            Text(
                text = "台灣農產品價格",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // ① 價格異常警報
        item {
            when (val state = anomalies) {
                is Resource.Success -> {
                    if (state.data.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x30FF5722))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("⚠️ 價格異常警報", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                state.data.forEach { anomaly ->
                                    Text(
                                        "${anomaly.cropName}：漲幅 ${anomaly.changePercent}%",
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // ② 今日熱門交易
        item {
            when (val state = topVolume) {
                is Resource.Success -> {
                    Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🔥 今日熱門交易", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            state.data.take(5).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.cropName)
                                    Text("$${item.avgPrice}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                is Resource.Loading -> CircularProgressIndicator()
                else -> {}
            }
        }

        // ③ 語音搜尋 + 搜尋列
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜尋農產品...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                VoiceSearchButton(onResult = { text ->
                    searchText = text
                    viewModel.search(text)
                })
            }
        }

        // ④ 今日菜價
        item {
            Text("📋 今日菜價", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        when (val state = dailyPrices) {
            is Resource.Success -> {
                items(state.data.items) { produce ->
                    ProduceCard(produce = produce, onClick = {
                        onNavigateToChart(produce.cropCode, produce.cropName)
                    })
                }
            }
            is Resource.Loading -> {
                item { CircularProgressIndicator(modifier = Modifier.padding(32.dp)) }
            }
            is Resource.Error -> {
                item {
                    Text("載入失敗：${state.message}", color = Color.Red)
                }
            }
        }

        // ⑤ 天氣預警
        item { WeatherAlertCard(weatherAlerts) }

        // ⑥ 省錢食譜
        item { BudgetRecipeCard(budgetRecipes) }

        // ⑦ 趨勢圖表入口
        item {
            Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 趨勢圖表", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("點擊任一農產品查看歷史價格走勢", color = Color.Gray)
                }
            }
        }

        // ⑧ 當季盛產
        item {
            when (val state = seasonalCrops) {
                is Resource.Success -> {
                    Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🌿 當季盛產", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            state.data.forEach { crop ->
                                Text("• ${crop.cropName}")
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // ⑨ 最近市場（靜態入口）
        item {
            Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 最近市場", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("查看您附近的傳統市場與超市", color = Color.Gray)
                }
            }
        }

        // ⑩ 購物清單入口
        item {
            Button(
                onClick = onNavigateToShoppingList,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🛒 購物清單", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ProduceCard(produce: ProduceDto, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(produce.cropName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(produce.marketName, color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${produce.avgPrice}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2E7D32)
                )
                Text("交易量 ${produce.transVolume}", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
