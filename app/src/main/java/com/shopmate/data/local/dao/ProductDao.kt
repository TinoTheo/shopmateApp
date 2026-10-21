package com.shopmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shopmate.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET stock_quantity = stock_quantity + :delta WHERE id = :id")
    suspend fun incrementStock(id: String, delta: Int)

    /**
     * Server-wins metadata update (conflict resolution). Prices are stored as TEXT via the
     * BigDecimal converter, so raw string values write directly. cost_price is preserved when
     * the server omits it (COALESCE). Stock is deliberately NOT touched here — local unsynced
     * deltas own stock until they upload.
     */
    @Query(
        """UPDATE products SET
               name = :name,
               selling_price = :sellingPrice,
               cost_price = COALESCE(:costPrice, cost_price),
               updated_at = :updatedAt
           WHERE id = :id"""
    )
    suspend fun updateMetadata(
        id: String,
        name: String,
        sellingPrice: String,
        costPrice: String?,
        updatedAt: Long,
    )

    @Query(
        """SELECT * FROM products
           WHERE name LIKE '%' || :query || '%'
              OR sku LIKE '%' || :query || '%'
              OR barcode LIKE '%' || :query || '%'
           ORDER BY name ASC"""
    )
    fun search(query: String): Flow<List<ProductEntity>>
}
