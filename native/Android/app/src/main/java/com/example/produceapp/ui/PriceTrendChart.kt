package com.example.produceapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.HistoricalPriceDto

/**
 * Canvas 折線圖 - 農產品歷史價格趨勢
 * 加入無障礙語意 semantics { contentDescription }
 */
@Composable
fun PriceTrendChart(
    data: List<HistoricalPriceDto>,
    predictedPrice: Double? = null,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text("暫無歷史價格資料", color = Color.Gray, modifier = Modifier.padding(16.dp))
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("📈 價格趨勢", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        val prices = data.map { it.avgPrice }
        val maxPrice = prices.max()
        val minPrice = prices.min()
        val priceRange = if (maxPrice == minPrice) 1.0 else maxPrice - minPrice

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    contentDescription =
                        "農產品價格趨勢折線圖，包含歷史價格（綠色實線）與預測價格（橘色虛線）"
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 16f

            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            // 繪製歷史價格折線（綠色實線）
            val path = Path()
            prices.forEachIndexed { index, price ->
                val x = padding + (index.toFloat() / (prices.size - 1).coerceAtLeast(1)) * chartWidth
                val y = padding + ((maxPrice - price) / priceRange).toFloat() * chartHeight

                if (index == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = Color(0xFF4CAF50),
                style = Stroke(width = 3f)
            )

            // 繪製資料點
            prices.forEachIndexed { index, price ->
                val x = padding + (index.toFloat() / (prices.size - 1).coerceAtLeast(1)) * chartWidth
                val y = padding + ((maxPrice - price) / priceRange).toFloat() * chartHeight
                drawCircle(
                    color = Color(0xFF2E7D32),
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            // 繪製預測價格（橘色虛線）
            if (predictedPrice != null && prices.isNotEmpty()) {
                val lastX = padding + chartWidth
                val lastY = padding + ((maxPrice - prices.last()) / priceRange).toFloat() * chartHeight
                val predictX = lastX + 30f
                val predictY = padding + ((maxPrice - predictedPrice) / priceRange).toFloat() * chartHeight

                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset(lastX, lastY),
                    end = Offset(predictX, predictY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                drawCircle(
                    color = Color(0xFFFF9800),
                    radius = 5f,
                    center = Offset(predictX, predictY)
                )
            }
        }

        // 圖例
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("歷史價格", fontSize = 12.sp)
            }
            if (predictedPrice != null) {
                Row {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(Color(0xFFFF9800))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("預測價格", fontSize = 12.sp)
                }
            }
        }
    }
}
