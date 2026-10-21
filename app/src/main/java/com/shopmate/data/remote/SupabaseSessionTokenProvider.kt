package com.shopmate.data.remote

import com.shopmate.data.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AuthTokenProvider] backed by the Supabase session. Sync workers keep their existing
 * "no token → no-op" behaviour: signed out means hasToken() is false and everything stays
 * queued locally. The Retrofit [AuthInterceptor] keeps working for any remaining calls.
 */
@Singleton
class SupabaseSessionTokenProvider @Inject constructor(
    private val authManager: AuthManager,
) : AuthTokenProvider {

    override suspend fun getToken(): String? = authManager.getAccessToken()

    override suspend fun hasToken(): Boolean = getToken() != null
}
