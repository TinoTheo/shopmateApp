package com.shopmate.ui.receipt

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.shopmate.ui.receipt.ReceiptPainter.Weight
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.InkMid
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen

/**
 * The receipt a customer gets over WhatsApp (brand handoff 1m): a 320dp card rendered at 3×,
 * Trade Green header with the store as the headline and a Neon M, white body, and ShopMate
 * signing off at the foot. Shared through the system share sheet.
 */
object SharedReceiptImage {

    private const val SCALE = 3f
    private const val CARD_DP = 320f

    fun render(context: Context, receipt: ReceiptContent): Bitmap =
        ReceiptPainter.render(context, (CARD_DP * SCALE).toInt()) { p -> layout(p, receipt) }

    private fun dp(value: Float) = value * SCALE

    private fun layout(p: ReceiptPainter, r: ReceiptContent): Float {
        val width = dp(CARD_DP)
        val pad = dp(20f)

        // Card: white body with 18dp corners; the header paints over its top.
        val eyebrow = p.paint(dp(11f), Weight.Medium, CheckoutNeon.argb, tracking = 0.11f)
        val store = p.paint(dp(18f), Weight.SemiBold, ShopWhite.argb, tracking = -0.01f)
        val meta = p.paint(dp(12f), Weight.Regular, ShopWhite.copy(alpha = 0.72f).argb, figures = true)
        val headerHeight = dp(20f) + p.lineHeight(eyebrow) + dp(6f) + p.lineHeight(store) +
            dp(2f) + p.lineHeight(meta) + dp(18f)

        val name = p.paint(dp(13.5f), Weight.Medium, ShopBlack.argb)
        val qty = p.paint(dp(11.5f), Weight.Regular, InkMid.argb, figures = true)
        val lineTotal = p.paint(dp(13.5f), Weight.Medium, ShopBlack.argb, figures = true)
        val totalLabel = p.paint(dp(14f), Weight.Regular, InkSecondary.argb)
        val totalAmount = p.paint(dp(24f), Weight.SemiBold, ShopBlack.argb, tracking = -0.02f, figures = true)
        val small = p.paint(dp(12.5f), Weight.Regular, InkSecondary.argb)
        val footer = p.paint(dp(11f), Weight.Regular, InkMid.argb)

        // Measure the body first so the card background can be drawn behind it in one go.
        val lineHeight = dp(7f) + p.lineHeight(name) + p.lineHeight(qty) + dp(7f)
        val bodyHeight = dp(14f) + lineHeight * r.lines.size + dp(6f) +
            dp(10f) + p.lineHeight(totalAmount) + dp(4f) + p.lineHeight(small) +
            (r.heldLine?.let { dp(10f) + p.lineHeight(small) } ?: 0f) + dp(20f)
        val footerHeight = dp(11f) + p.lineHeight(footer) + dp(11f)
        val height = headerHeight + bodyHeight + footerHeight

        // Solid behind the rounded header: WhatsApp re-encodes shared images as JPEG, which would
        // turn transparent corners black.
        p.rect(0f, 0f, width, height, ShopWhite.argb)
        // Header: rounded at the top only — a square block covers the bottom corners.
        p.rect(0f, 0f, width, headerHeight, TradeGreen.argb, radius = dp(18f))
        p.rect(0f, headerHeight - dp(18f), width, headerHeight, TradeGreen.argb)

        var y = dp(20f)
        val markW = dp(24f)
        val textMax = width - pad * 2 - markW - dp(14f)
        y += p.text(r.title.uppercase(), pad, y, eyebrow) + dp(6f)
        y += p.text(r.storeName, pad, y, store, textMax) + dp(2f)
        p.text(r.meta, pad, y, meta, textMax)
        p.mark(width - pad - markW, dp(20f), markW, dp(32f), CheckoutNeon.argb)

        // Lines, each over a 1dp Receipt Beige rule.
        y = headerHeight + dp(14f)
        r.lines.forEach { line ->
            val top = y + dp(7f)
            val totalW = p.width(line.total, lineTotal)
            p.text(line.name, pad, top, name, width - pad * 2 - totalW - dp(10f))
            p.text(line.quantity, pad, top + p.lineHeight(name), qty)
            p.textEnd(line.total, width - pad, top, lineTotal)
            y += lineHeight
            p.line(pad, y, width - pad, ReceiptBeige.argb, dp(1f))
        }

        // Totals.
        y += dp(6f) + dp(10f)
        val amountH = p.lineHeight(totalAmount)
        p.text(r.totalLabel, pad, y + amountH - p.lineHeight(totalLabel), totalLabel)
        p.textEnd(r.total, width - pad, y, totalAmount)
        y += amountH + dp(4f)
        y += p.text(r.payment, pad, y, small)
        r.heldLine?.let { y += dp(10f); y += p.text(it, pad, y, small, width - pad * 2) }

        // Sign-off.
        val footerTop = headerHeight + bodyHeight
        p.line(0f, footerTop, width, ReceiptBeige.argb, dp(1f))
        val footerText = footerTop + dp(11f)
        val markH = dp(12f)
        p.mark(pad, footerText + (p.lineHeight(footer) - markH) / 2, dp(9f), markH, TradeGreen.argb)
        p.text(r.signOff, pad + dp(9f) + dp(7f), footerText, footer)

        return height
    }

    private val Color.argb: Int get() = toArgb()
}
