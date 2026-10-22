package com.shopmate.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shopmate.R
import com.shopmate.ui.screen.categories.CategoriesScreen
import com.shopmate.ui.screen.pos.PosScreen
import com.shopmate.ui.screen.products.AddEditProductScreen
import com.shopmate.ui.screen.products.BarcodeInboxScreen
import com.shopmate.ui.screen.products.ProductsScreen
import com.shopmate.ui.screen.reports.ReportsScreen
import com.shopmate.ui.screen.sales.SalesHistoryScreen
import com.shopmate.ui.screen.settings.SettingsScreen
import com.shopmate.ui.screen.stock.StockCountScreen
import com.shopmate.ui.screen.stock.StockScreen
import com.shopmate.ui.screen.sync.SyncScreen
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout

private val subRoutes = setOf(
    Screen.Categories.route,
    Screen.ProductAdd.route,
    Screen.ProductAddWithCode.route,
    Screen.StockCount.route,
    Screen.BarcodeInbox.route,
    Screen.ProductEdit.route,
    Screen.Sales.route,
)

/**
 * App frame: full-height Trade Green sidebar on the left; content lives on a white sheet
 * with a rounded top-start corner floating on the green canvas. The green also fills the
 * status-bar strip (white system icons), so the frame reads as one continuous surface.
 */
@Composable
fun AppNavGraph(
    deviceLayout: DeviceLayout = DeviceLayout.COMPACT,
    navController: NavHostController = rememberNavController(),
    appBarViewModel: AppBarViewModel = hiltViewModel(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val isSubRoute = currentRoute in subRoutes

    // Main destinations show the store name (or "ShopMate") as the identity title — the
    // selected sidebar item already tells the user which section they're in. Sub-screens
    // keep their own specific titles (they have a back arrow).
    val storeName by appBarViewModel.storeName.collectAsState()
    val pendingSyncCount by appBarViewModel.pendingSyncCount.collectAsState()
    val failedSyncCount by appBarViewModel.failedSyncCount.collectAsState()
    val syncOnline by appBarViewModel.online.collectAsState()
    val syncPaused by appBarViewModel.paused.collectAsState()
    val homeTitle = storeName.ifBlank { stringResource(R.string.app_name) }

    val headerTitle = when (currentRoute) {
        Screen.Pos.route, Screen.Products.route, Screen.Stock.route,
        Screen.Reports.route, Screen.Sync.route -> homeTitle
        Screen.Settings.route -> stringResource(R.string.screen_settings)
        Screen.Categories.route -> stringResource(R.string.screen_categories)
        Screen.Sales.route -> stringResource(R.string.screen_sales)
        Screen.ProductAdd.route, Screen.ProductAddWithCode.route ->
            stringResource(R.string.screen_add_product)
        Screen.StockCount.route -> stringResource(R.string.stock_count_stock)
        Screen.BarcodeInbox.route -> stringResource(R.string.barcode_inbox_title)
        Screen.ProductEdit.route -> stringResource(R.string.screen_edit_product)
        else -> homeTitle
    }

    fun navigateTopLevel(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TradeGreen),
    ) {
        AppSidebar(
            currentDestination = currentDestination,
            onNavigate = ::navigateTopLevel,
            deviceLayout = deviceLayout,
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding(),
        )

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .statusBarsPadding(),
            color = ShopWhite,
            shape = RoundedCornerShape(topStart = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(WindowInsets.statusBars),
            ) {
                ContentHeader(
                    title = headerTitle,
                    showBack = isSubRoute,
                    onBack = { navController.navigateUp() },
                    showClock = deviceLayout == DeviceLayout.EXPANDED,
                    pendingSyncCount = pendingSyncCount,
                    failedSyncCount = failedSyncCount,
                    syncOnline = syncOnline,
                    syncPaused = syncPaused,
                ) {
                    when (currentRoute) {
                        Screen.Pos.route -> IconButton(onClick = { navController.navigate(Screen.Sales.route) }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = stringResource(R.string.screen_sales),
                                tint = ShopBlack,
                            )
                        }
                        Screen.Products.route -> IconButton(onClick = { navController.navigate(Screen.Categories.route) }) {
                            Icon(
                                Icons.Outlined.Category,
                                contentDescription = stringResource(R.string.screen_categories),
                                tint = ShopBlack,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .consumeWindowInsets(WindowInsets.navigationBars),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Pos.route,
                    ) {
                        composable(Screen.Pos.route) {
                            PosScreen(navController = navController, deviceLayout = deviceLayout)
                        }
                        composable(Screen.Products.route) { ProductsScreen(navController = navController) }
                        composable(Screen.Stock.route) {
                            StockScreen(navController = navController, deviceLayout = deviceLayout)
                        }
                        composable(Screen.StockCount.route) {
                            StockCountScreen(navController = navController)
                        }
                        composable(Screen.Reports.route) { ReportsScreen() }
                        composable(Screen.Sync.route) {
                            SyncScreen(deviceLayout = deviceLayout)
                        }
                        composable(Screen.Settings.route) { SettingsScreen() }
                        composable(Screen.Categories.route) { CategoriesScreen() }
                        composable(Screen.Sales.route) { SalesHistoryScreen() }
                        composable(Screen.ProductAdd.route) { AddEditProductScreen(navController = navController) }
                        composable(
                            route = Screen.ProductAddWithCode.route,
                            arguments = listOf(
                                navArgument("code") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                            ),
                        ) {
                            AddEditProductScreen(navController = navController)
                        }
                        composable(Screen.BarcodeInbox.route) {
                            BarcodeInboxScreen(navController = navController)
                        }
                        composable(
                            route = Screen.ProductEdit.route,
                            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
                        ) {
                            AddEditProductScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
