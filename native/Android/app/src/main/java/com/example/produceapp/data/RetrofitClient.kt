package com.example.produceapp.data

import android.content.Context
import com.example.produceapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 與 OkHttp 初始化
 * API URL 使用 BuildConfig.API_BASE_URL（由 build.gradle buildConfigField 編譯期注入）
 */
object RetrofitClient {

    @Volatile
    private var instance: ProduceService? = null

    fun getInstance(context: Context): ProduceService {
        return instance ?: synchronized(this) {
            instance ?: buildService(context).also { instance = it }
        }
    }

    private fun buildService(context: Context): ProduceService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(JwtInterceptor(context.applicationContext))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ProduceService::class.java)
    }
}
