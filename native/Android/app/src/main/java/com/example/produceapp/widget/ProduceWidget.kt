package com.example.produceapp.widget

import android.content.Context
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class ProduceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val topItems = fetchTopVolume(context)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(Color(0xFFE8F5E9))
            ) {
                Text(
                    text = "🥬 今日菜價",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))

                if (topItems.isEmpty()) {
                    Text(text = "載入中...", style = TextStyle(fontSize = 12.sp))
                } else {
                    topItems.take(5).forEach { (name, price) ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                        ) {
                            Text(
                                text = name,
                                style = TextStyle(fontSize = 13.sp),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Text(
                                text = "$$price",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = ColorProvider(Color(0xFF2E7D32))
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchTopVolume(context: Context): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("produce_prefs", Context.MODE_PRIVATE)
                val jwt = prefs.getString("jwt_token", null) ?: return@withContext emptyList()

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}top-volume")
                    .addHeader("Authorization", "Bearer $jwt")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)

                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    obj.getString("cropName") to String.format("%.1f", obj.getDouble("avgPrice"))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

class ProduceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProduceWidget()
}
