package com.shopmate.di

import com.shopmate.data.remote.SupabaseClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Singleton

/** Exposes the two plugins of the single client [SupabaseClientProvider] owns. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuth(provider: SupabaseClientProvider): Auth = provider.client.auth

    @Provides
    @Singleton
    fun providePostgrest(provider: SupabaseClientProvider): Postgrest = provider.client.postgrest
}
