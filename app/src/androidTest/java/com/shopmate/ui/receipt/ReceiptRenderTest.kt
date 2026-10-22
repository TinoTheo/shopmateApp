package com.shopmate.ui.receipt

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.domain.model.Sale
import com.shopmate.domain.model.SaleItem
import com.shopmate.print.EscPosRaster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.Instant

/**
 * Renders both receipts on a device. The images are also written to the app's cache
 * (`cache/receipt-render-test/`) so a person can look at them.
 */
@RunWith(AndroidJUnit4::class)
class ReceiptRenderTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun sale(syncedAt: Instant?) = Sale(
        id = "test-sale",
        receiptNumber = "SM-20261022-0019",
        totalAmount = BigDecimal("8.70"),
        paymentMethod = PaymentMethod.CASH,
        cashierName = "Cashier",
        items = listOf(
            item("Coca-Cola 500ml", "1.10", 2),
            item("Star Africa Sugar 2kg", "3.40", 1),
            item("Bakers Inn Bread", "1.50", 1),
            item("Matches, box of 10, a name long enough to need ellipsising on 58mm paper", "0.40", 4),
        ),
        createdAt = Instant.parse("2026-10-22T13:25:00Z"),
        syncedAt = syncedAt,
    )

    private fun item(name: String, price: String, qty: Int) = SaleItem(
        id = name, productId = name, productName = name, categoryName = null,
        unitPrice = BigDecimal(price), quantity = BigDecimal(qty),
        lineTotal = BigDecimal(price).multiply(BigDecimal(qty)),
    )

    @Test
    fun sharedImageIsTheCardWidthAt3x() {
        val receipt = ReceiptContent.from(context, sale(syncedAt = null), "Mbare Market Stop")
        val image = SharedReceiptImage.render(context, receipt)

        assertEquals(960, image.width)
        assertTrue(image.height > image.width / 2)
        save(image, "shared-held.png")
    }

    @Test
    fun printedSlipIs58mmAndOnlyBlackOrWhite() {
        val receipt = ReceiptContent.from(context, sale(syncedAt = Instant.now()), "Mbare Market Stop")
        val slip = PrintedReceiptSlip.render(context, receipt)
        save(slip, "printed.png")

        assertEquals(EscPosRaster.WIDTH_58MM, slip.width)
        val pixels = IntArray(slip.width * slip.height).also {
            slip.getPixels(it, 0, slip.width, 0, 0, slip.width, slip.height)
        }
        // Antialiased edges are grey, but nothing the layout fills is: no grey surfaces.
        val mostlyBinary = pixels.count { Color.red(it) in 40..215 }.toDouble() / pixels.size
        assertTrue("too much grey: $mostlyBinary", mostlyBinary < 0.05)

        val bytes = PrintedReceiptSlip.escPos(context, receipt)
        assertEquals(0x1D, bytes[0].toInt()); assertEquals(0x76, bytes[1].toInt())
        assertEquals(48, bytes[4].toInt())
    }

    private fun save(bitmap: Bitmap, name: String) {
        val dir = File(context.cacheDir, "receipt-render-test").apply { mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
