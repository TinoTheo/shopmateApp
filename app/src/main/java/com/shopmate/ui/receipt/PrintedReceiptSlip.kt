package com.shopmate.ui.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.shopmate.print.EscPosRaster
import com.shopmate.ui.receipt.ReceiptPainter.Weight

/**
 * The 58mm thermal slip (brand handoff 2b): 1m's layout in black only, so it prints the same on
 * any mono printer. The green header becomes a solid black band with the store name and M
 * knocked out in white; no grey anywhere.
 *
 * Rendered at 384px (58mm at 203dpi) — sizes are the mock's at about 1.65× — then thresholded to
 * 1 bit and sent as ESC/POS raster.
 */
object PrintedReceiptSlip {

    private const val WIDTH = EscPosRaster.WIDTH_58MM

    fun render(context: Context, receipt: ReceiptContent): Bitmap =
        ReceiptPainter.render(context, WIDTH) { p -> layout(p, receipt) }

    /** The slip as printer bytes. Black where the rendered pixel is darker than mid-grey. */
    fun escPos(context: Context, receipt: ReceiptContent): ByteArray {
        val bitmap = render(context, receipt)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val bytes = EscPosRaster.encode(bitmap.width, bitmap.height) { x, y ->
            val c = pixels[y * bitmap.width + x]
            val luminance = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
            luminance < 128
        }
        bitmap.recycle()
        return bytes
    }

    private fun layout(p: ReceiptPainter, r: ReceiptContent): Float {
        val black = Color.BLACK
        val white = Color.WHITE
        val side = 20f
        val right = WIDTH - side

        val eyebrow = p.paint(15f, Weight.SemiBold, white, tracking = 0.11f)
        val store = p.paint(23f, Weight.SemiBold, white)
        val meta = p.paint(16.5f, Weight.Regular, white, figures = true)
        val name = p.paint(19f, Weight.Medium, black)
        val qty = p.paint(16.5f, Weight.Regular, black, figures = true)
        val total = p.paint(19f, Weight.Medium, black, figures = true)
        val totalLabel = p.paint(20f, Weight.Regular, black)
        val totalAmount = p.paint(31f, Weight.SemiBold, black, tracking = -0.02f, figures = true)
        val small = p.paint(17f, Weight.Regular, black)
        val footer = p.paint(16.5f, Weight.Regular, black)

        // Paper is white; everything printed is black.
        val bandPad = 20f
        val bandHeight = bandPad + p.lineHeight(eyebrow) + 6f + p.lineHeight(store) + 3f +
            p.lineHeight(meta) + bandPad
        val lineHeight = 8f + p.lineHeight(name) + p.lineHeight(qty) + 8f
        val height = 23f + bandHeight + 13f + lineHeight * r.lines.size + 10f +
            p.lineHeight(totalAmount) + 5f + p.lineHeight(small) +
            (r.heldLine?.let { 12f + p.lineHeight(small) * 2 } ?: 0f) + 16f +
            15f + p.lineHeight(footer) + 33f
        p.rect(0f, 0f, WIDTH.toFloat(), height, white)

        // Header band, knocked out.
        val bandTop = 23f
        p.rect(side, bandTop, right, bandTop + bandHeight, black)
        val markW = 25f
        var y = bandTop + bandPad
        val textMax = right - side - bandPad * 2 - markW - 16f
        y += p.text(r.title.uppercase(), side + bandPad, y, eyebrow) + 6f
        y += p.text(r.storeName, side + bandPad, y, store, textMax) + 3f
        // The M sits beside the first two lines only, so the meta line gets the band's full width.
        p.text(r.meta, side + bandPad, y, meta, right - side - bandPad * 2)
        p.mark(right - bandPad - markW, bandTop + bandPad, markW, 33f, white)

        // Lines over dashed rules.
        y = bandTop + bandHeight + 13f
        r.lines.forEach { line ->
            val top = y + 8f
            val totalW = p.width(line.total, total)
            p.text(line.name, side, top, name, right - side - totalW - 12f)
            p.text(line.quantity, side, top + p.lineHeight(name), qty)
            p.textEnd(line.total, right, top, total)
            y += lineHeight
            p.line(side, y, right, black, 1f, dashPx = 4f)
        }

        // Totals.
        y += 10f
        val amountH = p.lineHeight(totalAmount)
        p.text(r.totalLabel, side, y + amountH - p.lineHeight(totalLabel), totalLabel)
        p.textEnd(r.total, right, y, totalAmount)
        y += amountH + 5f
        y += p.text(r.payment, side, y, small)
        r.heldLine?.let {
            // Two lines at most on 58mm paper; the sentence breaks after "device."
            y += 12f
            val parts = it.split(". ", limit = 2)
            y += p.text(parts[0] + if (parts.size > 1) "." else "", side, y, small, right - side)
            if (parts.size > 1) y += p.text(parts[1], side, y, small, right - side)
        }
        y += 16f

        // Sign-off: rule, then the M and "Kept by ShopMate", centred.
        p.line(side, y, right, black, 1f)
        y += 15f
        val markH = 18f
        val gap = 10f
        val signW = 13f + gap + p.width(r.signOff, footer)
        val start = (WIDTH - signW) / 2
        p.mark(start, y + (p.lineHeight(footer) - markH) / 2, 13f, markH, black)
        p.text(r.signOff, start + 13f + gap, y, footer)

        return height
    }
}
