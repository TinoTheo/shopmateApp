package com.shopmate.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.local.entity.CategoryEntity
import com.shopmate.data.local.entity.ProductEntity
import com.shopmate.data.remote.ShopMateRemote
import com.shopmate.data.remote.dto.CatalogueCategoryDto
import com.shopmate.data.remote.dto.CatalogueProductDto
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The store's products and categories, shared through the server (CLAUDE.md § Multi-till).
 *
 * Push: what changed on this till since its last push. The server keeps the newest edit
 * (`products_keep_newest`), so a till holding a stale price can't overwrite a newer one. Only
 * the owner's session can write the catalogue; a refusal leaves this till's copy as it is.
 *
 * Pull: every product of the store. One this till doesn't have arrives with no stock — the
 * ledger, pulled next, brings its movements. One it has takes the server's details when the
 * server's edit is newer. Stock is never taken from the server's row: it is the ledger's.
 */
@Singleton
class CatalogueSync @Inject constructor(
    private val remote: ShopMateRemote,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun push(storeId: String) {
        val prefs = dataStore.data.first()
        val sameStore = prefs[PUSHED_STORE] == storeId
        val pushedAt = prefs[PUSHED_AT].takeIf { sameStore }
        val startedAt = Instant.now().toEpochMilli()

        // Categories first: products point at them.
        val categories = categoryDao.getAll().first()
        val categoriesHash = categories.map { Triple(it.id, it.name, it.colour) }.hashCode()
        if (!sameStore || prefs[CATEGORIES_HASH] != categoriesHash) {
            val now = Instant.now().toString()
            remote.upsertCategories(categories.map { CatalogueCategoryDto(it.id, storeId, it.name, it.colour, now, now) })
        }

        val changed = if (pushedAt == null) productDao.getAll().first() else productDao.changedSince(pushedAt)
        remote.upsertProducts(changed.map { it.toDto(storeId) })

        dataStore.edit {
            it[PUSHED_STORE] = storeId
            it[PUSHED_AT] = startedAt
            it[CATEGORIES_HASH] = categoriesHash
        }
    }

    suspend fun pull(storeId: String) {
        remote.fetchCategories(storeId).filter { it.storeId == storeId }.forEach { row ->
            val local = CategoryEntity(row.id, row.name, row.colour ?: DEFAULT_COLOUR)
            // Update in place: replacing a category row could touch the products that use it.
            if (categoryDao.getAll().first().any { it.id == row.id }) categoryDao.update(local) else categoryDao.insert(local)
        }

        val rows = buildList {
            var from = 0L
            while (true) {
                val page = remote.fetchCataloguePage(storeId, from, PAGE_SIZE)
                addAll(page.filter { it.storeId == storeId }) // narrow, never widen
                if (page.size < PAGE_SIZE) break
                from += PAGE_SIZE
            }
        }
        rows.forEach { row ->
            val serverUpdated = OffsetDateTime.parse(row.updatedAt).toInstant()
            val local = productDao.getById(row.id)
            when {
                local == null -> productDao.insert(row.toEntity(stock = 0, serverUpdated))
                serverUpdated.isAfter(local.updatedAt) ->
                    productDao.update(
                        row.toEntity(stock = local.stockQuantity, serverUpdated).copy(
                            previousCostPrice = local.previousCostPrice,
                            costPriceChangedAt = local.costPriceChangedAt,
                            sellingPriceSetAt = local.sellingPriceSetAt,
                            supplier = local.supplier,
                            createdAt = local.createdAt,
                        ),
                    )
            }
        }
    }

    /** Called when the till moves to another store: its first push there sends everything. */
    suspend fun reset() {
        dataStore.edit {
            it.remove(PUSHED_STORE)
            it.remove(PUSHED_AT)
            it.remove(CATEGORIES_HASH)
        }
    }

    private fun ProductEntity.toDto(storeId: String) = CatalogueProductDto(
        id = id,
        storeId = storeId,
        categoryId = categoryId,
        name = name,
        sku = sku.ifBlank { null },
        barcode = barcode,
        sellingPrice = sellingPrice.toPlainString(),
        costPrice = costPrice.toPlainString(),
        stockQuantity = stockQuantity, // informational: tills read stock from the ledger
        lowStockThreshold = lowStockThreshold,
        unit = unit,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

    private fun CatalogueProductDto.toEntity(stock: Int, updated: Instant) = ProductEntity(
        id = id,
        name = name,
        sku = sku.orEmpty(),
        barcode = barcode,
        categoryId = categoryId,
        sellingPrice = BigDecimal(sellingPrice),
        costPrice = costPrice?.let(::BigDecimal) ?: BigDecimal.ZERO,
        stockQuantity = stock,
        lowStockThreshold = lowStockThreshold,
        unit = unit,
        createdAt = OffsetDateTime.parse(createdAt).toInstant(),
        updatedAt = updated,
    )

    private companion object {
        const val PAGE_SIZE = 1000L
        const val DEFAULT_COLOUR = "#005B52"
        val PUSHED_STORE = stringPreferencesKey("catalogue_pushed_store")
        val PUSHED_AT = longPreferencesKey("catalogue_pushed_at")
        val CATEGORIES_HASH = intPreferencesKey("catalogue_categories_hash")
    }
}
