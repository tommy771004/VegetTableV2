package com.example.produceapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produce_cache")
data class ProduceEntity(
    @PrimaryKey
    val cropCode: String,
    val cropName: String,
    val marketName: String,
    val avgPrice: Double,
    val transVolume: Double,
    val transDate: String,
    val cachedAt: Long = System.currentTimeMillis()
)
