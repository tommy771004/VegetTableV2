package com.example.produceapp.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp 攔截器：自動從 SharedPreferences 讀取 JWT Token 並注入 Authorization Header
 * 若收到 401 回應，嘗試重新取得 token 並重試一次
 */
class JwtInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val PREFS_NAME = "produce_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 跳過 auth/token 端點（避免循環）
        if (originalRequest.url.encodedPath.contains("auth/token")) {
            return chain.proceed(originalRequest)
        }

        val token = getToken()
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // 若收到 401，清除 token（讓 Repository 重新取得）
        if (response.code == 401) {
            clearToken()
        }

        return response
    }

    private fun getToken(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    private fun clearToken() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_JWT_TOKEN).apply()
    }
}
