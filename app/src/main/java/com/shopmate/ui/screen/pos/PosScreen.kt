package com.shopmate.ui.screen.pos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shopmate.R
import com.shopmate.domain.model.CartItem
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.domain.model.Product
import com.shopmate.ui.navigation.Screen
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

@Composable
fun PosScreen(
    navController: NavController,
    deviceLayout: DeviceLayout = DeviceLayout.COMPACT,
    viewModel: PosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Brief Checkout-Neon flash on the cart row that just changed.
    var highlightedProductId by remember { mutableStateOf<String?>(null) }
    fun flash(productId: String) {
        highlightedProductId = productId
        scope.launch {
            delay(700)
            if (highlightedProductId == productId) highlightedProductId = null
        }
    }

    // Back out of taking payment without losing the ticket: TENDER returns to SALE.
    BackHandler(enabled = deviceLayout == DeviceLayout.EXPANDED && state.phase == TicketPhase.TENDER) {
        viewModel.cancelTender()
    }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    PosContent(
        state = state,
        categories = categories,
        deviceLayout = deviceLayout,
        highlightedProductId = highlightedProductId,
        snackbarHostState = snackbarHostState,
        onSearchChange = viewModel::searchProducts,
        onClearSearch = viewModel::clearSearch,
        onCategorySelect = viewModel::selectCategory,
        onProductClick = { product ->
            viewModel.addToCart(product)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            flash(product.id)
        },
        onIncrement = { item ->
            viewModel.updateCartItemQty(item.product.id, item.quantity + 1)
            flash(item.product.id)
        },
        onDecrement = { item ->
            viewModel.updateCartItemQty(item.product.id, item.quantity - 1)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onClearCart = viewModel::clearCart,
        onLineClick = { item -> viewModel.selectLineItem(item.product.id) },
        onLineLongClick = { item ->
            viewModel.startManualPrice(item.product.id)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onPadDigit = viewModel::numberPadDigit,
        onPadBackspace = viewModel::numberPadBackspace,
        onCheckout = viewModel::openCheckout,
        onCharge = {
            viewModel.chargeTablet()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onApplyQuantity = viewModel::applyPendingQuantity,
        onCloseCheckout = viewModel::closeCheckout,
        onSelectMethod = viewModel::selectPaymentMethod,
        onCashDigit = viewModel::appendCashDigit,
        onCashBackspace = viewModel::backspaceCash,
        onDenomination = viewModel::setCashReceived,
        onExact = viewModel::setCashExact,
        onConfirmSale = {
            viewModel.confirmSale()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onNavigateToProducts = { navController.navigate(Screen.Products.route) },
    )
}

@Composable
fun PosContent(
    state: PosUiState,
    categories: List<Category>,
    deviceLayout: DeviceLayout,
    highlightedProductId: String?,
    snackbarHostState: SnackbarHostState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCategorySelect: (String?) -> Unit,
    onProductClick: (Product) -> Unit,
    onIncrement: (CartItem) -> Unit,
    onDecrement: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onLineClick: (CartItem) -> Unit,
    onLineLongClick: (CartItem) -> Unit,
    onPadDigit: (String) -> Unit,
    onPadBackspace: () -> Unit,
    onCheckout: () -> Unit,
    onCharge: () -> Unit,
    onApplyQuantity: () -> Unit,
    onCloseCheckout: () -> Unit,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCashDigit: (String) -> Unit,
    onCashBackspace: () -> Unit,
    onDenomination: (BigDecimal) -> Unit,
    onExact: () -> Unit,
    onConfirmSale: () -> Unit,
    onNavigateToProducts: () -> Unit,
) {
    val categoryColours = remember(categories) {
        categories.associate { it.id to it.colour }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (deviceLayout) {
                DeviceLayout.EXPANDED -> {
                    // Tablet: three fixed zones — sidebar (app frame), product area,
                    // sale ticket with the always-visible number pad.
                    PosTabletLayout(
                        state = state,
                        categories = categories,
                        categoryColours = categoryColours,
                        onSearchChange = onSearchChange,
                        onClearSearch = onClearSearch,
                        onCategorySelect = onCategorySelect,
                        onProductClick = onProductClick,
                        onLineClick = onLineClick,
                        onLineLongClick = onLineLongClick,
                        onPadDigit = onPadDigit,
                        onPadBackspace = onPadBackspace,
                        onCharge = onCharge,
                        onApplyQuantity = onApplyQuantity,
                        onSelectMethod = onSelectMethod,
                        onDenomination = onDenomination,
                        onExact = onExact,
                        onNavigateToProducts = onNavigateToProducts,
                    )
                }
                DeviceLayout.COMPACT -> {
                    // Phone: the proven stacked POS — grid on top, cart docked below.
                    // No permanent number pad (no room); quantity edits use the stepper.
                    Column(Modifier.fillMaxSize()) {
                        SaleSuccessBanner(receipt = state.lastSaleReceipt, total = state.lastSaleTotal)
                        CatalogPane(
                            state = state,
                            categoryColours = categoryColours,
                            onSearchChange = onSearchChange,
                            onClearSearch = onClearSearch,
                            onProductClick = onProductClick,
                            onNavigateToProducts = onNavigateToProducts,
                            modifier = Modifier.weight(1f),
                        )
                        CartPanel(
                            cartItems = state.cartItems,
                            cartTotal = state.cartTotal,
                            cartItemCount = state.cartItemCount,
                            highlightedProductId = highlightedProductId,
                            onIncrement = onIncrement,
                            onDecrement = onDecrement,
                            onClearCart = onClearCart,
                            onCheckout = onCheckout,
                        )
                    }
                }
            }
        }

        if (state.isCheckoutSheetOpen) {
            CheckoutBottomSheet(
                state = state,
                deviceLayout = deviceLayout,
                onSelectMethod = onSelectMethod,
                onCashDigit = onCashDigit,
                onCashBackspace = onCashBackspace,
                onDenomination = onDenomination,
                onExact = onExact,
                onConfirm = onConfirmSale,
                onDismiss = onCloseCheckout,
            )
        }
    }
}

@Composable
private fun CatalogPane(
    state: PosUiState,
    categoryColours: Map<String, String>,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onProductClick: (Product) -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ProductSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            onClear = onClearSearch,
        )

        when {
            state.searchResults.isEmpty() && state.searchQuery.isBlank() -> {
                EmptyCatalogState(
                    onNavigateToProducts = onNavigateToProducts,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.searchResults.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.pos_no_products_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                ProductGrid(
                    products = state.searchResults,
                    categoryColours = categoryColours,
                    onProductClick = onProductClick,
                )
            }
        }
    }
}

@Composable
private fun EmptyCatalogState(
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                stringResource(R.string.pos_no_products_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.pos_no_products_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNavigateToProducts,
                colors = ButtonDefaults.buttonColors(containerColor = TradeGreen),
            ) {
                Text(stringResource(R.string.pos_go_to_products), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun previewData(): Triple<List<Category>, List<Product>, List<CartItem>> {
    val categories = listOf(
        Category("c1", "Beverages", "#005B52"),
        Category("c2", "Dairy", "#64B5F6"),
    )
    val products = listOf(
        Product("1", "Coca Cola 500ml", "CC500", null, "c1", BigDecimal("2.50"), BigDecimal("1.50"), stockQuantity = 24, lowStockThreshold = 5, unit = "pcs", createdAt = Instant.now(), updatedAt = Instant.now()),
        Product("2", "Bread Loaf", "BRD", null, null, BigDecimal("3.99"), BigDecimal("2.00"), stockQuantity = 3, lowStockThreshold = 5, unit = "pcs", createdAt = Instant.now(), updatedAt = Instant.now()),
        Product("3", "Milk 1L", "MLK1", null, "c2", BigDecimal("1.20"), BigDecimal("0.80"), stockQuantity = 0, lowStockThreshold = 5, unit = "litre", createdAt = Instant.now(), updatedAt = Instant.now()),
    )
    val cart = listOf(
        CartItem(products[0], 2, BigDecimal("5.00")),
        CartItem(products[1], 1, BigDecimal("3.99")),
    )
    return Triple(categories, products, cart)
}

@Composable
private fun PosPreviewContent(deviceLayout: DeviceLayout) {
    val (categories, products, cart) = previewData()
    ShopMateTheme {
        PosContent(
            state = PosUiState(
                cartItems = cart,
                cartTotal = BigDecimal("8.99"),
                cartItemCount = 3,
                searchResults = products,
            ),
            categories = categories,
            deviceLayout = deviceLayout,
            highlightedProductId = null,
            snackbarHostState = remember { SnackbarHostState() },
            onSearchChange = {},
            onClearSearch = {},
            onCategorySelect = {},
            onProductClick = {},
            onIncrement = {},
            onDecrement = {},
            onClearCart = {},
            onLineClick = {},
            onLineLongClick = {},
            onPadDigit = {},
            onPadBackspace = {},
            onCheckout = {},
            onCharge = {},
            onApplyQuantity = {},
            onCloseCheckout = {},
            onSelectMethod = {},
            onCashDigit = {},
            onCashBackspace = {},
            onDenomination = {},
            onExact = {},
            onConfirmSale = {},
            onNavigateToProducts = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PosPhonePreview() {
    PosPreviewContent(DeviceLayout.COMPACT)
}

@Preview(showBackground = true, widthDp = 1100, heightDp = 800)
@Composable
private fun PosTabletPreview() {
    PosPreviewContent(DeviceLayout.EXPANDED)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PosEmptyPreview() {
    ShopMateTheme {
        PosContent(
            state = PosUiState(),
            categories = emptyList(),
            deviceLayout = DeviceLayout.COMPACT,
            highlightedProductId = null,
            snackbarHostState = remember { SnackbarHostState() },
            onSearchChange = {},
            onClearSearch = {},
            onCategorySelect = {},
            onProductClick = {},
            onIncrement = {},
            onDecrement = {},
            onClearCart = {},
            onLineClick = {},
            onLineLongClick = {},
            onPadDigit = {},
            onPadBackspace = {},
            onCheckout = {},
            onCharge = {},
            onApplyQuantity = {},
            onCloseCheckout = {},
            onSelectMethod = {},
            onCashDigit = {},
            onCashBackspace = {},
            onDenomination = {},
            onExact = {},
            onConfirmSale = {},
            onNavigateToProducts = {},
        )
    }
}
