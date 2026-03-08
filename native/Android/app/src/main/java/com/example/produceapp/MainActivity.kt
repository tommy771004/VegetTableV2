package com.example.produceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.produceapp.ui.FavoritesScreen
import com.example.produceapp.ui.HomeScreen
import com.example.produceapp.ui.SettingsScreen
import com.example.produceapp.viewmodel.ProduceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 處理 FCM Deep Link
        val deepLinkProduceId = intent.getStringExtra("produceId")

        setContent {
            MaterialTheme {
                MainApp(deepLinkProduceId = deepLinkProduceId)
            }
        }
    }
}

@Composable
fun MainApp(deepLinkProduceId: String? = null) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val viewModel: ProduceViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "首頁") },
                    label = { Text("首頁") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "收藏") },
                    label = { Text("收藏") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "設定") },
                    label = { Text("設定") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = viewModel)
                1 -> FavoritesScreen(viewModel = viewModel)
                2 -> SettingsScreen()
            }
        }
    }
}
