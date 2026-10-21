package com.shopmate.ui.navigation

sealed class Screen(val route: String) {
    data object Pos : Screen("pos")
    data object Products : Screen("products")
    data object ProductAdd : Screen("products/add")

    /** Add a product with a scanned code prefilled, from the unknown-barcode inbox. */
    data object ProductAddWithCode : Screen("products/add?code={code}")
    data object ProductEdit : Screen("products/{productId}")
    data object Stock : Screen("stock")
    data object Reports : Screen("reports")
    data object Sync : Screen("sync")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    data object Sales : Screen("sales")

    /** Stocktake session — reached from the Stock FAB, not from the sidebar. */
    data object StockCount : Screen("stock_count")

    /** Codes scanned at the till but not named yet — reached from the Products header. */
    data object BarcodeInbox : Screen("barcode_inbox")

    companion object {
        fun productEdit(productId: String) = "products/$productId"

        fun productAddWithCode(code: String) = "products/add?code=$code"
    }
}
