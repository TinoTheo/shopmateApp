package com.shopmate.data.remote

import com.shopmate.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the app's single [SupabaseClient]: Postgrest + Auth, nothing else. Only the anon key
 * ships in the app — every table is protected by RLS (CLAUDE.md § Backend & sync).
 *
 * No Realtime: a till on a thin connection does not hold a socket open.
 */
@Singleton
class SupabaseClientProvider @Inject constructor(
    sessionStore: SessionStore,
) {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            sessionManager = sessionStore
        }
        install(Postgrest)
    }
}
