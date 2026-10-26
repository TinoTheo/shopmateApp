package com.shopmate.di

import com.shopmate.data.repository.BarcodeRepositoryImpl
import com.shopmate.data.repository.CategoryRepositoryImpl
import com.shopmate.data.repository.ProductRepositoryImpl
import com.shopmate.data.repository.ReportRepositoryImpl
import com.shopmate.data.repository.SaleRepositoryImpl
import com.shopmate.data.repository.SettingsRepositoryImpl
import com.shopmate.data.repository.StockCountRepositoryImpl
import com.shopmate.data.repository.StockInsightRepositoryImpl
import com.shopmate.data.repository.StorePermissionRepositoryImpl
import com.shopmate.data.repository.StoreRepositoryImpl
import com.shopmate.data.repository.StaffRepositoryImpl
import com.shopmate.data.repository.LicenceRepositoryImpl
import com.shopmate.data.repository.SyncRepositoryImpl
import com.shopmate.domain.repository.BarcodeRepository
import com.shopmate.domain.repository.CategoryRepository
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.repository.ReportRepository
import com.shopmate.domain.repository.SaleRepository
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.StockCountRepository
import com.shopmate.domain.repository.StockInsightRepository
import com.shopmate.domain.repository.StorePermissionRepository
import com.shopmate.domain.repository.StoreRepository
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.LicenceRepository
import com.shopmate.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBarcodeRepository(impl: BarcodeRepositoryImpl): BarcodeRepository

    @Binds
    @Singleton
    abstract fun bindStockInsightRepository(impl: StockInsightRepositoryImpl): StockInsightRepository

    @Binds
    @Singleton
    abstract fun bindStockCountRepository(impl: StockCountRepositoryImpl): StockCountRepository

    @Binds
    @Singleton
    abstract fun bindStorePermissionRepository(impl: StorePermissionRepositoryImpl): StorePermissionRepository

    @Binds
    @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository

    @Binds
    @Singleton
    abstract fun bindStaffRepository(impl: StaffRepositoryImpl): StaffRepository

    @Binds
    @Singleton
    abstract fun bindLicenceRepository(impl: LicenceRepositoryImpl): LicenceRepository
}
