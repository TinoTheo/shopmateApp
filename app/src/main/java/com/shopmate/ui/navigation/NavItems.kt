package com.shopmate.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.shopmate.R

data class SidebarNavItem(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

val sidebarNavItems = listOf(
    SidebarNavItem(Screen.Pos, R.string.screen_pos, Icons.Outlined.ShoppingCart),
    SidebarNavItem(Screen.Products, R.string.screen_products, Icons.Outlined.Inventory2),
    SidebarNavItem(Screen.Stock, R.string.screen_stock, Icons.Outlined.BarChart),
    SidebarNavItem(Screen.Reports, R.string.screen_reports, Icons.Outlined.Assessment),
    SidebarNavItem(Screen.Sync, R.string.screen_sync, Icons.Outlined.CloudSync),
)
