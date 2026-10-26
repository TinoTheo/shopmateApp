package com.shopmate.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.shopmate.data.local.AppDatabase
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.dao.StockDeltaDao
import com.shopmate.data.local.entity.StockDeltaEntity
import com.shopmate.data.remote.ShopMateRemote
import com.shopmate.data.remote.dto.LedgerRowDto
import com.shopmate.domain.model.DeltaReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The store's whole ledger on this till (CLAUDE.md § Multi-till). Pulls the movements every
 * other till uploaded — by the server's `received_at`, never a till's own clock — adds the ones
 * this till doesn't hold, and recomputes stock as the sum of the ledger. Movements are only ever
 * added, so two tills' records merge without conflict.
 *
 * The first full pull for a store also *rebuilds*: a movement this till holds as sent but the
 * server doesn't have (a fresh till's old snapshot-based "Opening stock") is replaced by the
 * server's real history, so nothing is counted twice. Unsent movements are never touched.
 */
@Singleton
class LedgerSync @Inject constructor(
    private val db: AppDatabase,
    private val remote: ShopMateRemote,
    private val stockDeltaDao: StockDeltaDao,
    private val productDao: ProductDao,
    private val dataStore: DataStore<Preferences>,
) {

    /** When this till last heard from the other tills, by the server's clock. Null if never. */
    val othersUpTo: Flow<Instant?> = dataStore.data.map { prefs -> prefs[PULLED_AT]?.let(Instant::ofEpochMilli) }

    suspend fun pull(storeId: String) {
        val prefs = dataStore.data.first()
        val cursor = prefs[CURSOR].takeIf { prefs[CURSOR_STORE] == storeId }
        // A little overlap: a movement committed just before the last page was read but stamped
        // a moment earlier is still caught. Known ids are skipped, so overlap costs nothing.
        val since = cursor?.let { OffsetDateTime.parse(it).minus(OVERLAP).toString() }

        val rows = buildList {
            var from = 0L
            while (true) {
                val page = remote.fetchLedgerPage(storeId, since, from, PAGE_SIZE)
                // Narrow, never widen: only this store's movements reach the till.
                addAll(page.filter { it.storeId == storeId })
                if (page.size < PAGE_SIZE) break
                from += PAGE_SIZE
            }
        }

        // A movement for a product this till doesn't have yet (its product arrives with the next
        // catalogue pull) is left for next time: the cursor stops before it.
        val known = rows.filter { productDao.getById(it.productId) != null }
        val waiting = rows - known.toSet()

        db.withTransaction {
            if (cursor == null) {
                val onServer = rows.mapTo(HashSet()) { it.id }
                stockDeltaDao.sentIds().filterNot { it in onServer }
                    .chunked(500).forEach { stockDeltaDao.deleteByIds(it) }
            }
            stockDeltaDao.insertIgnoringKnown(known.map { it.toEntity() })
            productDao.recomputeStockFromLedger()
        }

        val newCursor = waiting.minByOrNull { it.receivedAt }?.receivedAt
            ?: rows.maxByOrNull { it.receivedAt }?.receivedAt
            ?: cursor
        dataStore.edit {
            it[CURSOR_STORE] = storeId
            newCursor?.let { c -> it[CURSOR] = c }
            it[PULLED_AT] = Instant.now().toEpochMilli()
        }
    }

    /** Called when the till moves to another store: its ledger starts again from nothing. */
    suspend fun reset() {
        dataStore.edit {
            it.remove(CURSOR)
            it.remove(CURSOR_STORE)
            it.remove(PULLED_AT)
        }
    }

    private fun LedgerRowDto.toEntity() = StockDeltaEntity(
        id = id,
        productId = productId,
        delta = delta,
        reason = runCatching { DeltaReason.valueOf(reason) }.getOrDefault(DeltaReason.MANUAL_ADJUSTMENT),
        note = note,
        actor = actor,
        createdAt = OffsetDateTime.parse(createdAt).toInstant().toEpochMilli(),
        // It's on the server already: never upload it again.
        synced = true,
    )

    private companion object {
        const val PAGE_SIZE = 1000L
        val OVERLAP: Duration = Duration.ofMinutes(2)
        val CURSOR = stringPreferencesKey("ledger_cursor")
        val CURSOR_STORE = stringPreferencesKey("ledger_cursor_store")
        val PULLED_AT = longPreferencesKey("ledger_pulled_at")
    }
}
