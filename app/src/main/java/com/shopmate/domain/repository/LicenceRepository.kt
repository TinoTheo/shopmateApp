package com.shopmate.domain.repository

import com.shopmate.domain.model.Licence
import kotlinx.coroutines.flow.Flow

/**
 * The organisation's licence (CLAUDE.md § Licensing). Read from the server at sign-in, store pick
 * and every sync, and kept on the device, so a till offline for a week still knows where it
 * stands. Null until it has been read once — and an unread licence never blocks a sale.
 */
interface LicenceRepository {
    val licence: Flow<Licence?>

    /** Reads the licence for the active store. Keeps the last answer when offline. */
    suspend fun refresh()
}
