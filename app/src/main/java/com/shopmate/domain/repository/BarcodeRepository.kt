package com.shopmate.domain.repository

import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductBarcode
import com.shopmate.domain.model.UnknownBarcode
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point for barcode lookups (architecture rule 11). It normalises the code,
 * checks `product_barcodes`, and on a miss records to `unknown_barcodes` and returns null.
 *
 * **The caller never blocks on a miss** — an unknown code is named later, the sale continues.
 */
interface BarcodeRepository {

    /**
     * Normalises [rawCode] and resolves it to a product. On a miss, records the sighting to
     * `unknown_barcodes` (bumping `hits` on a repeat) and returns null.
     */
    suspend fun lookup(rawCode: String, till: String? = null): Product?

    fun barcodesFor(productId: String): Flow<List<ProductBarcode>>

    /** Adds a code to a product. Returns false when the code already belongs to another product. */
    suspend fun addBarcode(productId: String, rawCode: String): Boolean

    suspend fun removeBarcode(rawCode: String)

    fun unknownBarcodes(): Flow<List<UnknownBarcode>>

    /** Badge count for the Products header — the § Products "codes scanned but not named" card. */
    fun unknownBarcodeCount(): Flow<Int>

    /** Called once a code has been named: attaches it to [productId] and clears the inbox row. */
    suspend fun claimUnknown(rawCode: String, productId: String)
}
