package com.shopmate.ui.screen.stock

import com.shopmate.domain.model.ProductInsight
import com.shopmate.domain.model.RunOut

/**
 * The four sections the Stock screen reads top to bottom, in this order. The screen answers
 * "when do I run out?", so the ordering is by urgency in **time**, not alphabetically and not
 * by table order.
 */
enum class StockSection {
    /** Runs out today or in the next six days — the strip's seven. Warning tier. */
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
fun ProductInsight.section(): StockSection = when (runOut) {
    is RunOut.Out -> StockSection.Stopped
    is RunOut.NoHistory, is RunOut.Dormant -> StockSection.NotMoving
    RunOut.Today, RunOut.Tomorrow, is RunOut.OnDay -> StockSection.RunsOutThisWeek
    RunOut.OverAWeek, is RunOut.OverWeeks, RunOut.OverAMonth -> StockSection.Healthy
}

/** The window the strip and the "Runs out this week" section share. */
const val WEEK_AHEAD_DAYS = ProductInsight.RUNWAY_DAYS
