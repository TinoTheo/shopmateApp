package com.shopmate.data.remote

interface AuthTokenProvider {
    suspend fun getToken(): String?
    suspend fun hasToken(): Boolean
}
