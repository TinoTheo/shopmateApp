package com.shopmate.print

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EscPosRasterTest {

    @Test
    fun `packs a row MSB-first and pads to a whole byte`() {
        // 10 px wide: black at x = 0, 7 and 9. Row needs 2 bytes.
        val black = setOf(0, 7, 9)
        val bytes = EscPosRaster.encode(width = 10, height = 1) { x, _ -> x in black }

        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, 2, 0, 1, 0) // GS v 0, 2 bytes x 1 row
        assertArrayEquals(header + byteArrayOf(0b1000_0001.toByte(), 0b0100_0000), bytes)
    }

    @Test
    fun `splits tall images into bands of at most 255 rows`() {
        val height = 600 // 255 + 255 + 90
        val bytes = EscPosRaster.encode(width = 8, height = height) { _, _ -> false }

        // Three commands, each an 8-byte header plus one byte per row.
        assertEquals(3 * 8 + height, bytes.size)
        assertEquals(255, bytes[6].toInt() and 0xFF)
        assertEquals(255, bytes[8 + 255 + 6].toInt() and 0xFF)
        assertEquals(90, bytes[2 * (8 + 255) + 6].toInt() and 0xFF)
    }

    @Test
    fun `a 58mm row is 48 bytes`() {
        val bytes = EscPosRaster.encode(width = EscPosRaster.WIDTH_58MM, height = 1) { _, _ -> true }

        assertEquals(48, bytes[4].toInt() and 0xFF)
        assertEquals(8 + 48, bytes.size)
        assertEquals(0xFF, bytes.last().toInt() and 0xFF)
    }
}
