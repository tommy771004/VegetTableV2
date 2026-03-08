package com.example.produceapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProduceEntity::class], version = 1, exportSchema = false)
abstract class ProduceDatabase : RoomDatabase() {

    abstract fun produceDao(): ProduceDao

    companion object {
        @Volatile
        private var INSTANCE: ProduceDatabase? = null

        fun getInstance(context: Context): ProduceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProduceDatabase::class.java,
                    "produce_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
