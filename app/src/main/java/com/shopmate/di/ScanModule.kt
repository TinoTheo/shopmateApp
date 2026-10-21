package com.shopmate.di

import com.shopmate.scan.HidScanSource
import com.shopmate.scan.ScanSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * HID ships first (§ Build order step 4). A CameraX + ML Kit source binds behind the same
 * [ScanSource] interface when it lands — no caller changes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScanModule {

    @Binds
    @Singleton
    abstract fun bindScanSource(impl: HidScanSource): ScanSource
}
