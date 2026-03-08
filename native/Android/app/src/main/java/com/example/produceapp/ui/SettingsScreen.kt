package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 設定頁面 - 推播、語言、離線快取等選項
 */
@Composable
fun SettingsScreen() {
    var pushEnabled by remember { mutableStateOf(true) }
    var offlineCacheEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("zh-TW") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "⚙️ 設定",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 推播通知
        Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("推播通知", fontWeight = FontWeight.Bold)
                    Text("價格達標時接收通知", fontSize = 12.sp)
                }
                Switch(
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 離線快取
        Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("離線快取", fontWeight = FontWeight.Bold)
                    Text("無網路時使用快取資料", fontSize = 12.sp)
                }
                Switch(
                    checked = offlineCacheEnabled,
                    onCheckedChange = { offlineCacheEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 語言設定
        Card(modifier = Modifier.fillMaxWidth().liquidGlass()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("語言設定", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val languages = listOf("zh-TW" to "繁體中文", "id" to "Bahasa Indonesia", "vi" to "Tiếng Việt")
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == code,
                            onClick = { selectedLanguage = code }
                        )
                        Text(name)
                    }
                }
            }
        }
    }
}
