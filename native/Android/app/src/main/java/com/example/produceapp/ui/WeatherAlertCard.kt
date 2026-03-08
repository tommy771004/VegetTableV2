package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.WeatherAlertDto
import com.example.produceapp.viewmodel.Resource

/**
 * 天氣警報卡片
 * 訂閱 viewModel.weatherAlerts StateFlow
 * 當 alertType == "None" 時自動隱藏（不顯示卡片）
 */
@Composable
fun WeatherAlertCard(state: Resource<WeatherAlertDto>) {
    when (state) {
        is Resource.Success -> {
            val alert = state.data
            // alertType 為 "None" 時不顯示
            if (alert.alertType == "None") return

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(),
                colors = CardDefaults.cardColors(
                    containerColor = when (alert.alertType) {
                        "Typhoon" -> Color(0x40E65100)
                        "HeavyRain" -> Color(0x401565C0)
                        else -> Color(0x40FFA000)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (alert.alertType) {
                            "Typhoon" -> "🌀 颱風警報"
                            "HeavyRain" -> "🌧️ 豪大雨警報"
                            else -> "⚠️ 天氣警報"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(alert.title, fontWeight = FontWeight.SemiBold)
                    Text(alert.message, color = Color.DarkGray, fontSize = 14.sp)

                    if (alert.affectedCrops.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("可能受影響作物：", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(alert.affectedCrops.joinToString("、"), color = Color.DarkGray)
                    }
                }
            }
        }
        is Resource.Loading -> {
            // 天氣警報載入中不顯示任何內容
        }
        is Resource.Error -> {
            // 天氣警報失敗時靜默，不影響主畫面
        }
    }
}
