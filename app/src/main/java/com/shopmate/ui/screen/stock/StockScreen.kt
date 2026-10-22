package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shopmate.R
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.Product
import com.shopmate.ui.navigation.Screen
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout

/**
 * Stock answers one question: **when do I run out?** — measured in time.
 *
 * The week-ahead strip answers it before a single row is read; the four sections order the
 * catalogue by urgency in time. There are no tabs: a movement history that belongs to a product
 * lives in that product's detail surface, where "why does it say 6?" is actually asked.
 */
@Composable
fun StockScreen(
    navController: NavController,
    deviceLayout: DeviceLayout,
    viewModel: StockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    val canManageStock by viewModel.canManageStock.collectAsState()

    StockContent(
        state = state,
        canManageStock = canManageStock,
        categories = categories,
        deviceLayout = deviceLayout,
        snackbarHostState = snackbarHostState,
        onSearchChange = viewModel::searchProducts,
        onProductClick = viewModel::selectProduct,
        onNavigateToProducts = { navController.navigate(Screen.Products.route) },
        onCountStock = { navController.navigate(Screen.StockCount.route) },
        onAdjust = viewModel::adjustStock,
        onCloseSheet = viewModel::closeAdjustmentSheet,
    )
}

@Composable
fun StockContent(
    state: StockUiState,
    /** Owners, managers, and cashiers given stock rights. Everyone else sees stock read-only. */
    canManageStock: Boolean,
    categories: List<Category>,
    deviceLayout: DeviceLayout,
    snackbarHostState: SnackbarHostState,
    onSearchChange: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onNavigateToProducts: () -> Unit,
    onCountStock: () -> Unit,
    onAdjust: (String, Int, DeltaReason, String?) -> Unit,
    onCloseSheet: () -> Unit,
) {
    val selectedInsight = state.sections.values.flatten()
        .firstOrNull { it.insight.product.id == state.selectedProduct?.id }
        ?.insight

    // "Adjust" opens the existing adjustment sheet over the detail surface — the detail answers
    // "why does it say 6?", and adjusting is a separate act with its own reason and note.
    var showAdjustment by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canManageStock) {
                ExtendedFloatingActionButton(
                    onClick = onCountStock,
                    containerColor = TradeGreen,
                    contentColor = ShopWhite,
                    shape = RoundedCornerShape(14.dp),
                    icon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
                    text = { Text(stringResource(R.string.stock_count_stock)) },
                )
            }
        },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            StockInsightList(
                state = state,
                deviceLayout = deviceLayout,
                onSearchChange = onSearchChange,
                onProductClick = onProductClick,
                onNavigateToProducts = onNavigateToProducts,
                modifier = Modifier.weight(1f),
            )

            // EXPANDED pins the detail to the sheet edge; COMPACT gets the same content as a
            // bottom sheet, below.
            if (deviceLayout == DeviceLayout.EXPANDED &&
                state.isAdjustmentSheetOpen &&
                selectedInsight != null
            ) {
                VerticalDivider(thickness = 1.dp, color = ReceiptBeige)
                StockDetailSurface(
                    insight = selectedInsight,
                    ledger = state.selectedLedger,
                    deviceLayout = deviceLayout,
                    onAddToOrderList = onCloseSheet,
                    onAdjust = if (canManageStock) ({ showAdjustment = true }) else null,
                    onDismiss = onCloseSheet,
                )
            }
        }

        if (deviceLayout == DeviceLayout.COMPACT &&
            state.isAdjustmentSheetOpen &&
            selectedInsight != null
        ) {
            StockDetailSurface(
                insight = selectedInsight,
                ledger = state.selectedLedger,
                deviceLayout = deviceLayout,
                onAddToOrderList = onCloseSheet,
                onAdjust = if (canManageStock) ({ showAdjustment = true }) else null,
                onDismiss = onCloseSheet,
            )
        }

        if (showAdjustment && canManageStock && state.selectedProduct != null) {
            StockAdjustmentSheet(
                product = state.selectedProduct,
                history = state.selectedHistory,
                onConfirm = { delta, reason, note ->
                    onAdjust(state.selectedProduct.id, delta, reason, note)
                    showAdjustment = false
                },
                onDismiss = { showAdjustment = false },
            )
        }
    }
}
