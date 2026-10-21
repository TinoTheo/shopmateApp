package com.shopmate.domain.model

/**
 * Derived, never stored (CLAUDE.md § Derived, never stored). Every field here is computed on
 * read from `sale_items` and `products`. A stored velocity goes stale the moment a sale lands,
 * so no insight is ever written back — architecture rule 6.
 */
data class ProductInsight(
    val product: Product,
    /** Null when the trailing window is too short to mean anything. */
    val unitsPerDay: Double?,
    /** `stockQuantity / unitsPerDay`. Null whenever [unitsPerDay] is null. */
    val daysOfCover: Double?,
    val daysSinceLastSale: Int?,
    /** `(sellingPrice - costPrice) / sellingPrice`. */
    val margin: Double,
    /** The same figure against `previousCostPrice`, when cost has moved. */
    val marginBefore: Double?,
    /**
     * False when history is thin. The UI **must not** project a run-out day when this is
     * false — [RunwayGauge][com.shopmate.ui.components.RunwayGauge] renders every segment
     * hollow and the row reads "not enough history yet".
     */
    val confident: Boolean,
) {
    /** Not sold in 14+ days — visible, never urgent (CLAUDE.md § Severity). */
    val dormant: Boolean get() = (daysSinceLastSale ?: Int.MAX_VALUE) >= DORMANT_DAYS

    val sellingAtALoss: Boolean get() = margin <= 0.0

    val outOfStock: Boolean get() = product.stockQuantity <= 0

    companion object {
        const val DORMANT_DAYS = 14

        /** Trailing window the velocity is measured over. */
        const val VELOCITY_WINDOW_DAYS = 28

        /** Fewer days of sales history than this and [confident] is false. */
        const val MIN_CONFIDENT_DAYS = 7

        /** The runway gauge shows one segment per day ahead. */
        const val RUNWAY_DAYS = 7
    }
}
