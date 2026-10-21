package com.shopmate.ui.screen.products

import com.shopmate.domain.model.ProductInsight

/**
 * Products answers **am I still making money on this?** — measured in margin. The sections are
 * ordered by how much money is at stake, not alphabetically and not by category.
 */
enum class ProductSection {
    /** Cost moved against us. The meta line carries the actual movement and the date. */
    CostWentUp,

    /** Margin at or below zero. Stopped tier — already costing money on every sale. */
    SellingAtALoss,

    /** At or above the store's target margin. */
    HoldingTarget,

    /** Below target but still profitable — thin, not bleeding. */
    BelowTarget,
}

/**
 * Places one insight in exactly one section, given the store's target margin.
 *
 * Selling at a loss outranks a cost rise: an item can be both, and the loss is the one that
 * costs money on every sale.
 */
fun ProductInsight.section(targetMargin: Double): ProductSection = when {
    sellingAtALoss -> ProductSection.SellingAtALoss
    costRose -> ProductSection.CostWentUp
    margin >= targetMargin -> ProductSection.HoldingTarget
    else -> ProductSection.BelowTarget
}

/**
 * True when cost has moved upward since it was last recorded. This is what
 * `previous_cost_price` and `cost_price_changed_at` exist for — without them the screen can
 * only show today's margin and never why it changed.
 */
val ProductInsight.costRose: Boolean
    get() {
        val previous = product.previousCostPrice ?: return false
        return product.costPrice > previous
    }
