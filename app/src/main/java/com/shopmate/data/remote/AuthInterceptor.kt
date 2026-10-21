package com.shopmate.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import javax.inject.Inject

/** Attaches `Authorization: Bearer <token>` when a token is available; otherwise passes through. */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking { tokenProvider.getToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
