package com.shopmate.di

import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.SupabaseAuthManager
import com.shopmate.data.remote.AuthTokenProvider
import com.shopmate.data.remote.SupabaseSessionTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthManager(
        impl: SupabaseAuthManager,
    ): AuthManager

    // Token now comes from the Supabase session; the old DataStore token provider
    // (DataStoreAuthTokenProvider) is kept for the legacy hand-typed token plumbing.
    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        impl: SupabaseSessionTokenProvider,
    ): AuthTokenProvider
}
