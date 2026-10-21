package com.shopmate.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import javax.inject.Inject

/**
 * Retrofit's base URL is fixed at build time (BuildConfig.BASE_URL). This interceptor lets the
 * merchant override the scheme/host/port at runtime from Settings — the request path is preserved,
 * so `sync/sales/batch`, `health`, etc. still resolve against the configured server.
 */
class BaseUrlInterceptor @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val configured = runBlocking { dataStore.data.first()[BASE_URL_KEY] }
        val override = configured?.toHttpUrlOrNull()
        val request = chain.request()
        val rewritten = if (override != null) {
            request.newBuilder()
                .url(
                    request.url.newBuilder()
                        .scheme(override.scheme)
                        .host(override.host)
                        .port(override.port)
                        .build(),
                )
                .build()
        } else {
            request
        }
        return chain.proceed(rewritten)
    }

    companion object {
        // Mirrors SettingsRepositoryImpl.BASE_URL_KEY (kept local to avoid a data→data dependency).
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
    }
}
