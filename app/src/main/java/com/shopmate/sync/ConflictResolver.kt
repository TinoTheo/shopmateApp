package com.shopmate.sync

import com.shopmate.data.local.dao.ProductDao
import com.shopmate.data.remote.dto.ServerProductDto

/**
 * Server-wins for product *metadata* (name, prices). Stock is intentionally left alone: the
 * server value is a baseline, and any local unsynced stock deltas still sit in the event queue
 * and will be applied when they upload. Products absent locally are skipped — snapshot sync
 * creates them server-side, not the other way around (single-store, offline-first).
 */
object ConflictResolver {

    suspend fun resolveProduct(
        serverProduct: ServerProductDto,
        productDao: ProductDao,
    ) {
        // Skip products we don't know about locally.
        productDao.getById(serverProduct.id) ?: return

        productDao.updateMetadata(
            id = serverProduct.id,
            name = serverProduct.name,
            sellingPrice = serverProduct.sellingPrice,
            costPrice = serverProduct.costPrice,
            updatedAt = serverProduct.updatedAt,
        )
    }
}
