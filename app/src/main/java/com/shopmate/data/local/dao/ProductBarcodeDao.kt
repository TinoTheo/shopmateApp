package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shopmate.data.local.entity.ProductBarcodeEntity
import kotlinx.coroutines.flow.Flow

/** Codes are stored normalised — callers go through `BarcodeRepository`, never here directly. */
@Dao
interface ProductBarcodeDao {

    @Query("SELECT * FROM product_barcodes WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): ProductBarcodeEntity?

    @Query("SELECT * FROM product_barcodes WHERE product_id = :productId ORDER BY created_at ASC")
    fun getByProductId(productId: String): Flow<List<ProductBarcodeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(barcode: ProductBarcodeEntity)

    @Delete
    suspend fun delete(barcode: ProductBarcodeEntity)

    @Query("DELETE FROM product_barcodes WHERE code = :code")
    suspend fun deleteByCode(code: String)
}
