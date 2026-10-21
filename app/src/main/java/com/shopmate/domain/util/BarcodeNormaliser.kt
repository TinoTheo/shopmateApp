package com.shopmate.domain.util

/**
 * Normalises scanned codes before they are written or looked up.
 *
 * UPC-A is EAN-13 with a leading zero. Without this, roughly half of all "unknown barcode"
 * reports are the same item scanned on a scanner that reports 12 digits instead of 13
 * (CLAUDE.md § Barcode normalisation).
 *
 * Every write to and lookup against `product_barcodes` / `unknown_barcodes` goes through here.
 */
object BarcodeNormaliser {

    /**
     * Returns the canonical form of [raw]: trimmed, inner whitespace stripped, and UPC-A
     * (12 digits) promoted to EAN-13 by prefixing a zero. Non-numeric codes are returned
     * trimmed and uppercased — some suppliers use alphanumeric internal codes.
     *
     * Returns null when [raw] has no content once trimmed.
     */
    fun normalise(raw: String?): String? {
        val stripped = raw?.filterNot { it.isWhitespace() }.orEmpty()
        if (stripped.isEmpty()) return null
        if (!stripped.all { it.isDigit() }) return stripped.uppercase()
        return when (stripped.length) {
            12 -> "0$stripped"
            else -> stripped
        }
    }
}
