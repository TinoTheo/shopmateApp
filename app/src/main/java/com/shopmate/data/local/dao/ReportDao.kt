package com.shopmate.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Read-only aggregate queries backing the Reports screen. All timestamps are compared as
 * epoch-millis half-open ranges: `created_at >= start AND created_at < end`.
 *
 * Single-store app: there is no `store_id` column in the schema, so no store filtering is applied.
 */
@Dao
interface ReportDao {

    @Query(
        """
        SELECT date(created_at / 1000, 'unixepoch', 'localtime') AS sale_date,
               COUNT(*) AS sale_count,
               SUM(total_amount) AS total_revenue
        FROM sales
        WHERE created_at >= :startMillis AND created_at < :endMillis
        GROUP BY sale_date
        ORDER BY sale_date ASC
        """
    )
    fun getDailyRevenues(startMillis: Long, endMillis: Long): Flow<List<DailyRevenueRow>>

    @Query(
        """
        SELECT strftime('%H', created_at / 1000, 'unixepoch', 'localtime') AS hour,
               SUM(total_amount) AS total_revenue
        FROM sales
        WHERE created_at >= :startMillis AND created_at < :endMillis
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    fun getHourlyRevenues(startMillis: Long, endMillis: Long): Flow<List<HourlyRevenueRow>>

    @Query(
        """
        SELECT si.product_id AS product_id,
               si.product_name AS product_name,
               si.category_name AS category_name,
               SUM(si.quantity) AS total_qty,
               SUM(si.line_total) AS total_revenue
        FROM sale_items si
        INNER JOIN sales s ON si.sale_id = s.id
        WHERE s.created_at >= :startMillis AND s.created_at < :endMillis
        GROUP BY si.product_id
        ORDER BY total_revenue DESC
        LIMIT :limit
        """
    )
    fun getTopProducts(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopProductRow>>

    @Query(
        """
        SELECT payment_method AS payment_method, SUM(total_amount) AS total
        FROM sales
        WHERE created_at >= :startMillis AND created_at < :endMillis
        GROUP BY payment_method
        """
    )
    fun getPaymentBreakdown(startMillis: Long, endMillis: Long): Flow<List<PaymentBreakdownRow>>

    @Query(
        """
        SELECT COUNT(*) FROM sales
        WHERE created_at >= :startMillis AND created_at < :endMillis
        """
    )
    fun getSaleCount(startMillis: Long, endMillis: Long): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(total_amount), 0) FROM sales
        WHERE created_at >= :startMillis AND created_at < :endMillis
        """
    )
    fun getTotalRevenue(startMillis: Long, endMillis: Long): Flow<String>

    @Query(
        """
        SELECT COALESCE(SUM(si.quantity), 0)
        FROM sale_items si
        INNER JOIN sales s ON si.sale_id = s.id
        WHERE s.created_at >= :startMillis AND s.created_at < :endMillis
        """
    )
    fun getItemsSold(startMillis: Long, endMillis: Long): Flow<String>
}

data class DailyRevenueRow(
    @ColumnInfo(name = "sale_date") val saleDate: String,
    @ColumnInfo(name = "sale_count") val saleCount: Int,
    @ColumnInfo(name = "total_revenue") val totalRevenue: String,
)

data class HourlyRevenueRow(
    @ColumnInfo(name = "hour") val hour: String,
    @ColumnInfo(name = "total_revenue") val totalRevenue: String,
)

data class TopProductRow(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "total_qty") val totalQty: String,
    @ColumnInfo(name = "total_revenue") val totalRevenue: String,
)

data class PaymentBreakdownRow(
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "total") val total: String,
)
