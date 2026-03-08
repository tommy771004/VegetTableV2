package com.example.produceapp.data.local

import androidx.room.*

@Dao
interface ProduceDao {

    @Query("SELECT * FROM produce_cache ORDER BY transDate DESC")
    suspend fun getAll(): List<ProduceEntity>

    @Query("SELECT * FROM produce_cache WHERE cropCode = :cropCode")
    suspend fun getByCropCode(cropCode: String): ProduceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProduceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ProduceEntity)

    @Query("DELETE FROM produce_cache WHERE cachedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM produce_cache")
    suspend fun clearAll()
}
