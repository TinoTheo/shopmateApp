package com.shopmate.ui.util

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

/**
 * Two-bucket layout switch: phones keep the stacked POS, everything wider gets the
 * three-zone tablet POS. Deliberately coarser than WindowSizeClass — POS layouts must
 * not reflow between Medium and Expanded (cashier muscle memory).
 *
 * **Do not introduce a third bucket** (CLAUDE.md § Adaptive layouts). Medium behaving like
 * Expanded is the decision, not an oversight.
 */
enum class DeviceLayout { COMPACT, EXPANDED }

fun WindowWidthSizeClass.toDeviceLayout(): DeviceLayout =
    when (this) {
        WindowWidthSizeClass.Compact -> DeviceLayout.COMPACT   // phones
        else -> DeviceLayout.EXPANDED                          // tablets / landscape
    }
