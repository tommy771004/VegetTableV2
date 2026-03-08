package com.example.produceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.produceapp.data.FavoriteAlertDto
import com.example.produceapp.viewmodel.ProduceViewModel
import com.example.produceapp.viewmodel.Resource

/**
 * 我的收藏頁面
 * 根據目標價是否達成 (isAlertTriggered) 顯示不同鈴鐺狀態
 */
@Composable
fun FavoritesScreen(viewModel: ProduceViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "⭐ 我的收藏",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val state = favorites) {
            is Resource.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is Resource.Error -> {
                Text("載入失敗：${state.message}", color = Color.Red)
            }
            is Resource.Success -> {
                if (state.data.isEmpty()) {
                    Text("尚未收藏任何農產品", color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.data) { item ->
                            FavoriteItem(
                                item = item,
                                onDelete = { viewModel.removeFavorite(item.produceId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteItem(item: FavoriteAlertDto, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().liquidGlass()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.cropName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("目標價：$${item.targetPrice}", color = Color.Gray, fontSize = 14.sp)
                item.currentPrice?.let { price ->
                    Text("目前均價：$${price}", fontSize = 14.sp)
                }
            }

            // 鈴鐺狀態：isAlertTriggered 為 true 表示已達標
            Icon(
                imageVector = if (item.isAlertTriggered) Icons.Default.Notifications
                             else Icons.Default.NotificationsOff,
                contentDescription = if (item.isAlertTriggered) "已達目標價" else "未達目標價",
                tint = if (item.isAlertTriggered) Color(0xFF4CAF50) else Color.Gray
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "刪除收藏", tint = Color.Red)
            }
        }
    }
}
