package com.shopmate.data.repository

import androidx.room.withTransaction
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.StockCountDao
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.mapper.toEntity
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.StockCount
import com.shopmate.domain.repository.CountedLine
import com.shopmate.domain.repository.ProductRepository
import com.shopmate.domain.repository.StockCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockCountRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val stockCountDao: StockCountDao,
    private val productDao: ProductDao,
    private val productRepository: ProductRepository,
) : StockCountRepository {

    override fun recentCounts(limit: Int): Flow<List<StockCount>> =
        stockCountDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    /**
     * A single transaction: write the session, then push each variance through the same signed
     * delta path `adjustStock` uses, tagged with `reason = COUNT` and the session id. Lines that
     * match are still counted towards `itemCount` but move no stock — there is nothing to record.
     *
     * `countedBy` stays null until auth ships; the ledger renders no attribution rather than a
     * placeholder name.
     */
    override suspend fun commit(startedAt: Instant, lines: List<CountedLine>): StockCount {
        val sessionId = UUID.randomUUID().toString()
        val committedAt = Instant.now()
        val variances = lines.filterNot { it.matches }

        var varianceValue = BigDecimal.ZERO
        for (line in variances) {
            val cost = productDao.getById(line.productId)?.costPrice ?: BigDecimal.ZERO
            varianceValue += cost.multiply(BigDecimal(line.variance))
        }

        val session = StockCount(
            id = sessionId,
            startedAt = startedAt,
            committedAt = committedAt,
            countedBy = null,
            itemCount = lines.size,
            varianceCount = variances.size,
            varianceValue = varianceValue,
        )

        db.withTransaction {
            stockCountDao.upsert(session.toEntity())
            variances.forEach { line ->
                productRepository.adjustStock(
                    productId = line.productId,
                    delta = line.variance,
                    reason = DeltaReason.COUNT,
                    note = "Counted ${line.counted}, expected ${line.expected}",
                    countSessionId = sessionId,
                )
            }
        }
        return session
    }
}
