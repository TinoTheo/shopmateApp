package com.shopmate.data.repository

import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.StockInsightDao
import com.shopmate.data.mapper.toDomain
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductInsight
import com.shopmate.domain.repository.StockInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockInsightRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val stockInsightDao: StockInsightDao,
) : StockInsightRepository {

    override fun getInsights(): Flow<List<ProductInsight>> {
        val since = Instant.now()
            .minus(Duration.ofDays(ProductInsight.VELOCITY_WINDOW_DAYS.toLong()))
            .toEpochMilli()
        return combine(
            productDao.getAll(),
            stockInsightDao.getVelocitiesSince(since),
            stockInsightDao.getLastSaleTimes(),
        ) { products, velocities, lastSales ->
            val byProduct = velocities.associateBy { it.productId }
            val lastSaleByProduct = lastSales.associate { it.productId to it.lastSaleAt }
            val now = Instant.now()
            products.map { entity ->
                insightFor(
                    product = entity.toDomain(),
                    velocity = byProduct[entity.id],
                    lastSaleAt = lastSaleByProduct[entity.id],
                    now = now,
                )
            }
        }
    }

    override fun getInsight(productId: String): Flow<ProductInsight?> =
        getInsights().map { list -> list.firstOrNull { it.product.id == productId } }

    private fun insightFor(
        product: Product,
        velocity: com.shopmate.data.local.dao.ProductVelocityRow?,
        lastSaleAt: Long?,
        now: Instant,
    ): ProductInsight {
        // Velocity confidence rule (CLAUDE.md § Derived, never stored): fewer than 7 days of
        // sales history in the trailing window, or none at all, and we do not project.
        val confident = velocity != null &&
            velocity.distinctDays >= ProductInsight.MIN_CONFIDENT_DAYS &&
            velocity.unitsSold > 0.0

        val unitsPerDay = if (confident && velocity != null) {
            val spanDays = Duration.ofMillis(now.toEpochMilli() - velocity.firstSaleAt)
                .toDays()
                .coerceIn(1L, ProductInsight.VELOCITY_WINDOW_DAYS.toLong())
            velocity.unitsSold / spanDays.toDouble()
        } else {
            null
        }

        val daysOfCover = unitsPerDay
            ?.takeIf { it > 0.0 }
            ?.let { product.stockQuantity / it }

        val daysSinceLastSale = lastSaleAt?.let {
            Duration.ofMillis(now.toEpochMilli() - it).toDays().toInt()
        }

        return ProductInsight(
            product = product,
            unitsPerDay = unitsPerDay,
            daysOfCover = daysOfCover,
            daysSinceLastSale = daysSinceLastSale,
            margin = marginOf(product.sellingPrice, product.costPrice),
            marginBefore = product.previousCostPrice
                ?.let { marginOf(product.sellingPrice, it) },
            confident = confident,
        )
    }

    /** `(selling - cost) / selling`. A zero selling price has no margin to speak of. */
    private fun marginOf(selling: BigDecimal, cost: BigDecimal): Double {
        if (selling.signum() <= 0) return 0.0
        return (selling - cost).toDouble() / selling.toDouble()
    }
}
