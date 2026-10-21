package com.shopmate.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Per-product sales velocity over a trailing window, plus the span of history actually
 * available. [distinctDays] and [firstSaleAt] are what the velocity confidence rule reads —
 * the domain layer, not this DAO, decides whether the numbers are trustworthy.
 */
data class ProductVelocityRow(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "units_sold") val unitsSold: Double,
    @ColumnInfo(name = "distinct_days") val distinctDays: Int,
    @ColumnInfo(name = "first_sale_at") val firstSaleAt: Long,
    @ColumnInfo(name = "last_sale_at") val lastSaleAt: Long,
)

/** Last sale per product across all history — drives "not moving" independently of the window. */
data class ProductLastSaleRow(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "last_sale_at") val lastSaleAt: Long,
)

/**
 * Read-only, in the [ReportDao] mould (architecture rule 6). Velocity, days of cover and margin
 * are computed in the domain layer from these rows — no insight is ever written back to
 * `products`, because a stored velocity goes stale the moment a sale lands.
 */
@Dao
interface StockInsightDao {

    @Query(
        """
        SELECT si.product_id AS product_id,
               SUM(si.quantity) AS units_sold,
               COUNT(DISTINCT date(s.created_at / 1000, 'unixepoch', 'localtime')) AS distinct_days,
               MIN(s.created_at) AS first_sale_at,
               MAX(s.created_at) AS last_sale_at
        FROM sale_items si
        INNER JOIN sales s ON si.sale_id = s.id
        WHERE s.created_at >= :sinceMillis
        GROUP BY si.product_id
        """
    )
    fun getVelocitiesSince(sinceMillis: Long): Flow<List<ProductVelocityRow>>

    @Query(
        """
        SELECT si.product_id AS product_id,
               MAX(s.created_at) AS last_sale_at
        FROM sale_items si
        INNER JOIN sales s ON si.sale_id = s.id
        GROUP BY si.product_id
        """
    )
    fun getLastSaleTimes(): Flow<List<ProductLastSaleRow>>
}
