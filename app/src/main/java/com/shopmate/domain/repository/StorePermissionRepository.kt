package com.shopmate.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * What the signed-in member may do at the active store, beyond selling.
 *
 * Owners and managers add, adjust and count stock; a cashier only when the owner has granted it
 * for this store. The server enforces the same rule on upload — this mirrors it on the till so a
 * cashier is never offered a movement the server will refuse.
 */
interface StorePermissionRepository {

    /**
     * True when the member may record non-sale stock movements here. Kept on the device, so it
     * holds offline; false until the server has confirmed it for the active store.
     */
    val canManageStock: Flow<Boolean>

    /** Asks the server again for the active store. Keeps the last answer if offline. */
    suspend fun refresh()
}
