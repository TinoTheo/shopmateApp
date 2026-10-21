package com.shopmate.ui.screen.stock

import androidx.compose.ui.graphics.Color
import com.shopmate.domain.model.Product
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.StockRed

// Shared stock colour coding, drawn from the single brand palette (see ui/theme/Color.kt).
internal val StockGood: Color = StockGreen
internal val StockLow: Color = StockOrange
internal val StockOut: Color = StockRed

internal val DeltaPositive: Color = StockGreen
internal val DeltaNegative: Color = StockRed

// <= 0: overselling is allowed at the till, so stock can legitimately go negative.
internal fun stockColorFor(product: Product): Color = when {
    product.stockQuantity <= 0 -> StockOut
    product.stockQuantity <= product.lowStockThreshold -> StockLow
    else -> StockGood
}
