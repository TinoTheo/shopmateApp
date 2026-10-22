package com.shopmate.print

/**
 * Encodes a 1-bit image as ESC/POS raster commands (`GS v 0`), which every common 58mm and 80mm
 * thermal printer understands. Printing the receipt as an image — rather than as printer text —
 * is what keeps the brand type and the knocked-out header on any printer, mono or not.
 *
 * Pure Kotlin so it can be tested without a device. The transport (Bluetooth, USB) sits elsewhere
 * and only ever sees these bytes.
 */
object EscPosRaster {

    /**
     * Rows per `GS v 0` command. Many cheap printers buffer a limited height per command and print
     * garbage past it; 255 rows (about 32mm at 203dpi) is safe on all of them.
     */
    const val BAND_ROWS = 255

    /** Dots across 58mm paper at 203dpi. */
    const val WIDTH_58MM = 384

    /**
     * [isBlack] is asked once per pixel. Rows are packed MSB-first, eight pixels to a byte, and
     * padded with white to a whole byte.
     */
    fun encode(width: Int, height: Int, isBlack: (x: Int, y: Int) -> Boolean): ByteArray {
        require(width > 0 && height > 0) { "Empty image" }
        val bytesPerRow = (width + 7) / 8
        val out = java.io.ByteArrayOutputStream()
        var bandTop = 0
        while (bandTop < height) {
            val rows = minOf(BAND_ROWS, height - bandTop)
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            out.write(bytesPerRow and 0xFF); out.write(bytesPerRow shr 8 and 0xFF)
            out.write(rows and 0xFF); out.write(rows shr 8 and 0xFF)
            for (y in bandTop until bandTop + rows) {
                for (byteIndex in 0 until bytesPerRow) {
                    var b = 0
                    for (bit in 0 until 8) {
                        val x = byteIndex * 8 + bit
                        if (x < width && isBlack(x, y)) b = b or (0x80 ushr bit)
                    }
                    out.write(b)
                }
            }
            bandTop += rows
        }
        return out.toByteArray()
    }
}
