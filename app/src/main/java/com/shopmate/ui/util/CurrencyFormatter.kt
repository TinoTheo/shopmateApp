package com.shopmate.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal

/**
 * Formats money with the merchant's chosen currency symbol.
 *
 * [symbol] is Compose snapshot state: reading it inside a composable subscribes that composable,
 * so changing the symbol (from Settings, via the collector started in ShopMateApp) recomposes
 * every price on screen — with zero changes at the ~13 call sites. Reads outside composition
 * (e.g. ViewModel tooltip strings) simply see the current value.
 */
object CurrencyFormatter {
    var symbol by mutableStateOf("$")

    fun format(amount: BigDecimal): String = "$symbol%.2f".format(amount)
    fun format(amount: Int): String = format(amount.toBigDecimal())

    /** Format with an explicit symbol (when the caller already collected it). */
    fun format(amount: BigDecimal, symbolOverride: String): String =
        "$symbolOverride%.2f".format(amount)
}
