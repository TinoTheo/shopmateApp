package com.shopmate.di

import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.SupabaseAuthManager
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
}
