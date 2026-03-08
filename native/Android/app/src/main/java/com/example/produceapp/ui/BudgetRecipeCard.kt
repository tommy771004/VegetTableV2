package com.example.produceapp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.produceapp.data.BudgetRecipeDto
import com.example.produceapp.viewmodel.Resource

/**
 * 省錢食譜推薦卡片
 * 訂閱 viewModel.budgetRecipes，顯示橫向滾動食譜卡片列表
 * 支援 Loading/Error/Empty/Success 四態 UI
 */
@Composable
fun BudgetRecipeCard(state: Resource<List<BudgetRecipeDto>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("🍳 省錢食譜推薦", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        when (state) {
            is Resource.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF4CAF50)
                )
            }
            is Resource.Error -> {
                Text(
                    "食譜載入失敗，請稍後再試",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is Resource.Success -> {
                if (state.data.isEmpty()) {
                    Text(
                        "暫無食譜推薦",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.data.forEach { recipe ->
                            RecipeItem(recipe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeItem(recipe: BudgetRecipeDto) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .liquidGlass()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(recipe.imageUrl, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(recipe.recipeName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(recipe.reason, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "食材：${recipe.mainIngredients.joinToString("、")}",
                fontSize = 12.sp,
                color = Color(0xFF2E7D32)
            )
        }
    }
}
