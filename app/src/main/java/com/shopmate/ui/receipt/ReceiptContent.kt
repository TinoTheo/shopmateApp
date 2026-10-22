package com.shopmate.ui.receipt

import android.content.Context
import com.shopmate.R
import com.shopmate.domain.model.Sale
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.labelRes
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What a receipt says, resolved to strings once, so the shared image (handoff 1m) and the printed
 * slip (2b) can never disagree. The store is the headline; ShopMate signs off at the foot.
 */
data class ReceiptContent(
    /** "Receipt" — set as the eyebrow over the store name. */
    val title: String,
    val storeName: String,
    /** "#SM-20261022-0019 · 22 Oct, 15:25" */
    val meta: String,
    val lines: List<Line>,
    val totalLabel: String,
    val total: String,
    /**
     * "Paid via EcoCash". The brand art shows "Cash $10.00 · Change $1.30", but a sale doesn't keep
     * the cash handed over, so the receipt names the method rather than invent the amounts.
     */
    val payment: String,
    /** Set while the sale is still on this device — one of the two places the line belongs. */
    val heldLine: String?,
    val signOff: String,
) {
    data class Line(val name: String, val quantity: String, val total: String)

    companion object {
        private val metaDate = DateTimeFormatter.ofPattern("d MMM, HH:mm")

        fun from(context: Context, sale: Sale, storeName: String): ReceiptContent = ReceiptContent(
            title = context.getString(R.string.receipt_title),
            storeName = storeName.ifBlank { context.getString(R.string.app_name) },
            meta = "#${sale.receiptNumber} · " +
                metaDate.format(sale.createdAt.atZone(ZoneId.systemDefault())),
            lines = sale.items.map { item ->
                Line(
                    name = item.productName,
                    quantity = context.getString(
                        R.string.receipt_qty_at_price,
                        item.quantity.stripTrailingZeros().toPlainString(),
                        CurrencyFormatter.format(item.unitPrice),
                    ),
                    total = CurrencyFormatter.format(item.lineTotal),
                )
            },
            totalLabel = context.getString(R.string.pos_total),
            total = CurrencyFormatter.format(sale.totalAmount),
            payment = context.getString(
                R.string.receipt_paid_via,
                context.getString(sale.paymentMethod.labelRes()),
            ),
            heldLine = if (sale.syncedAt == null) context.getString(R.string.receipt_held) else null,
            signOff = context.getString(R.string.receipt_sign_off),
        )
    }
}
