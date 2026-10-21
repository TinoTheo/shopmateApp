package com.shopmate.ui.screen.stock

import com.shopmate.domain.model.ProductInsight
import kotlin.math.ceil

/**
 * The four sections the Stock screen reads top to bottom, in this order. The screen answers
 * "when do I run out?", so the ordering is by urgency in **time**, not alphabetically and not
 * by table order.
 */
enum class StockSection {
    /** Cover runs out inside the seven-day window. Warning tier. */
    RunsOutThisWeek,

    /** Already out. Stopped tier — a black chip, never red. */
    Stopped,

    /** Enough cover, moving normally. */
    Healthy,

    /** Not sold in 14+ days, or too little history to say. Visible, never urgent. */
    NotMoving,
}

/**
 * Places one insight in exactly one section.
 *
 * Order matters: out-of-stock outranks everything (it is already costing money), and an item
 * we cannot project for is Not moving rather than being guessed into Healthy. **Never project
 * a run-out day from thin data** (§ Velocity confidence rule).
 */
fun ProductInsight.section(): StockSection = when {
    outOfStock -> StockSection.Stopped
    !confident || dormant -> StockSection.NotMoving
    (daysOfCover ?: Double.MAX_VALUE) <= WEEK_AHEAD_DAYS -> StockSection.RunsOutThisWeek
    else -> StockSection.Healthy
}

/**
 * Which day in the week ahead this item runs out on, as an offset from today (0 = today), or
 * null when it does not run out inside the window or we cannot honestly say.
 */
fun ProductInsight.runsOutOnDayOffset(): Int? {
    if (!confident || outOfStock) return null
    val cover = daysOfCover ?: return null
    val day = ceil(cover).toInt()
    return day.takeIf { it in 0..WEEK_AHEAD_DAYS }
}

/** The window the strip and the "Runs out this week" section share. */
const val WEEK_AHEAD_DAYS = ProductInsight.RUNWAY_DAYS
