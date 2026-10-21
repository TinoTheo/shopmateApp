package com.shopmate.scan

import kotlinx.coroutines.flow.Flow

/**
 * Where scanned codes come from.
 *
 * Two implementations sit behind this interface and the caller cannot tell them apart:
 * [HidScanSource] (a keyboard-wedge scanner, the common case at a Zimbabwean till) and a
 * camera source built on CameraX + ML Kit. The scanner overlay and the unknown-barcode path
 * both depend only on this interface, never on either implementation.
 *
 * Codes arrive **raw**. Normalisation belongs to
 * [BarcodeNormaliser][com.shopmate.domain.util.BarcodeNormaliser], applied by
 * [BarcodeRepository][com.shopmate.domain.repository.BarcodeRepository] — not here.
 */
interface ScanSource {

    /** Raw codes as they are scanned. Cold: collecting starts the source, cancelling stops it. */
    val codes: Flow<String>

    /** Whether this source can run right now — no camera permission, no camera source. */
    suspend fun isAvailable(): Boolean
}

/** Which source the POS is listening to. HID needs no permission and no overlay. */
enum class ScanMode { HID, CAMERA }
