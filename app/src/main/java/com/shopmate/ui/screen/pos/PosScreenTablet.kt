package com.shopmate.ui.screen.pos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.CartItem
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.domain.model.Product
import com.shopmate.ui.components.ShopMateFilterChip
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.NumberSmall
import com.shopmate.ui.theme.ShopOffWhite
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.StockOrangeSubtle
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

/**
 * Tablet POS: three fixed zones that NEVER reflow — nav sidebar (owned by the app frame),
 * product area, and the 240dp sale ticket with the always-visible number pad. Cashiers tap
 * at ~2× normal speed on muscle memory; zones that reflow force re-reading. Stability wins.
 */
@Composable
fun PosTabletLayout(
    state: PosUiState,
    categories: List<Category>,
    categoryColours: Map<String, String>,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCategorySelect: (String?) -> Unit,
    onProductClick: (Product) -> Unit,
    onLineClick: (CartItem) -> Unit,
    onLineLongClick: (CartItem) -> Unit,
    onPadDigit: (String) -> Unit,
    onPadBackspace: () -> Unit,
    onCharge: () -> Unit,
    onApplyQuantity: () -> Unit,
    onSelectMethod: (PaymentMethod) -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize()) {
        ProductArea(
            state = state,
            categories = categories,
            categoryColours = categoryColours,
            onSearchChange = onSearchChange,
            onClearSearch = onClearSearch,
            onCategorySelect = onCategorySelect,
            onProductClick = onProductClick,
            onNavigateToProducts = onNavigateToProducts,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(thickness = 1.dp, color = ReceiptBeige)
        TicketPanel(
            state = state,
            onLineClick = onLineClick,
            onLineLongClick = onLineLongClick,
            onPadDigit = onPadDigit,
            onPadBackspace = onPadBackspace,
            onCharge = onCharge,
            onApplyQuantity = onApplyQuantity,
            onSelectMethod = onSelectMethod,
        )
    }
}

/** Zone A — search, category chips, product grid. */
@Composable
private fun ProductArea(
    state: PosUiState,
    categories: List<Category>,
    categoryColours: Map<String, String>,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCategorySelect: (String?) -> Unit,
    onProductClick: (Product) -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = 8.dp)) {
        SaleSuccessBanner(receipt = state.lastSaleReceipt, total = state.lastSaleTotal)

        ProductSearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchChange,
            onClear = onClearSearch,
        )

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    ShopMateFilterChip(
                        label = stringResource(R.string.stock_filter_all),
                        selected = state.selectedCategoryId == null,
                        onClick = { onCategorySelect(null) },
                    )
                }
                items(categories, key = { it.id }) { category ->
                    ShopMateFilterChip(
                        label = category.name,
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onCategorySelect(category.id) },
                    )
                }
            }
        }

        when {
            state.searchResults.isEmpty() &&
                state.searchQuery.isBlank() && state.selectedCategoryId == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.pos_no_products_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.pos_go_to_products),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable(onClick = onNavigateToProducts),
                        )
                    }
                }
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

/**
 * Zone B — the fixed-width sale ticket: lines, the phase band, the always-visible number pad,
 * total and Charge.
 *
 * The **zones do not reflow between phases** (CLAUDE.md § Adaptive layouts). Only the band
 * between the lines and the pad changes: the quantity hint in [TicketPhase.SALE], the payment
 * method plus cash/change pair in [TicketPhase.TENDER]. The pad, the total and Charge are in the
 * same place in every phase.
 */
@Composable
private fun TicketPanel(
    state: PosUiState,
    onLineClick: (CartItem) -> Unit,
    onLineLongClick: (CartItem) -> Unit,
    onPadDigit: (String) -> Unit,
    onPadBackspace: () -> Unit,
    onCharge: () -> Unit,
    onApplyQuantity: () -> Unit,
    onSelectMethod: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(TICKET_PANEL_WIDTH)
            .fillMaxHeight()
            .background(ShopOffWhite),
    ) {
        // Ticket header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.pos_current_sale),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ShopBlack,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.pos_items, state.cartItemCount, state.cartItemCount),
                fontSize = 12.sp,
                color = ShopBlackTertiary,
            )
        }

        // Line items
        if (state.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.pos_cart_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShopBlackTertiary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(state.cartItems, key = { it.product.id }) { item ->
                    val isSelected = item.product.id == state.selectedLineItemId
                    SaleLineItem(
                        item = item,
                        selected = isSelected,
                        editingQty = state.numberPadBuffer.takeIf {
                            isSelected && state.numberPadMode == NumberPadMode.QUANTITY
                        },
                        editingPrice = state.numberPadBuffer.takeIf {
                            isSelected && state.numberPadMode == NumberPadMode.MANUAL_PRICE
                        },
                        onClick = { onLineClick(item) },
                        onLongClick = { onLineLongClick(item) },
                    )
                }
            }
        }

        // The phase band — the only part of the ticket that changes between phases.
        TicketPhaseBand(
            state = state,
            onApplyQuantity = onApplyQuantity,
            onSelectMethod = onSelectMethod,
        )

        // Number pad — always visible: quantity, manual price, cash change. Loose goods
        // (sugar by kg) need manual entry constantly; hiding it costs time on every sale.
        HorizontalDivider(thickness = 1.dp, color = ReceiptBeige)
        NumberPad(
            onDigit = onPadDigit,
            onBackspace = onPadBackspace,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )

        // Total + Charge — anchored bottom, never moves or resizes.
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.pos_total),
                    fontSize = 13.sp,
                    color = ShopBlackSecondary,
                )
                Spacer(Modifier.weight(1f))
                AnimatedContent(
                    targetState = state.cartTotal,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ticketTotal",
                ) { total ->
                    Text(
                        text = CurrencyFormatter.format(total),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = ShopBlack,
                    )
                }
            }
            Spacer(Modifier.padding(top = 10.dp))
            ChargeButton(
                total = state.cartTotal,
                enabled = state.canCharge,
                onClick = onCharge,
                label = if (state.phase == TicketPhase.DONE) {
                    stringResource(R.string.pos_next_sale)
                } else {
                    null
                },
            )
        }
    }
}

/**
 * The one band that changes with [TicketPhase]. It keeps a fixed height so the pad, the total
 * and Charge never move between phases — the muscle-memory rule applied within a screen.
 */
@Composable
private fun TicketPhaseBand(
    state: PosUiState,
    onApplyQuantity: () -> Unit,
    onSelectMethod: (PaymentMethod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PHASE_BAND_HEIGHT)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.phase) {
            TicketPhase.SALE, TicketPhase.DONE -> QuantityHintRow(
                pendingQuantity = state.pendingQuantity,
                onApplyQuantity = onApplyQuantity,
            )
            TicketPhase.TENDER -> TenderBlock(state = state, onSelectMethod = onSelectMethod)
        }
    }
}

/**
 * Idle: how to edit a quantity. Typing: the pending figure and its own apply control.
 * **Quantity editing is never bound to Charge** (§ Adaptive layouts).
 */
@Composable
private fun QuantityHintRow(
    pendingQuantity: String?,
    onApplyQuantity: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pendingQuantity == null) {
            Text(
                text = stringResource(R.string.pos_quantity_hint),
                fontSize = 11.5.sp,
                color = ShopBlackTertiary,
            )
        } else {
            Text(
                text = pendingQuantity,
                style = NumberSmall,
                fontWeight = FontWeight.SemiBold,
                color = TradeGreen,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onApplyQuantity,
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                border = BorderStroke(1.dp, TradeGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TradeGreen),
            ) {
                Text(stringResource(R.string.pos_set_quantity), fontSize = 12.sp)
            }
        }
    }
}

/**
 * TENDER: payment method, then the cash/change pair. Card and mobile-money **skip the cash
 * pair entirely** — there is no change to give, and a zeroed calculator would imply otherwise.
 */
@Composable
private fun TenderBlock(
    state: PosUiState,
    onSelectMethod: (PaymentMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Three across at 240dp. PaymentMethod carries five values; the enum having five
            // is not a reason for five buttons at the till.
            TILL_PAYMENT_METHODS.forEach { method ->
                PaymentMethodCard(
                    method = method,
                    selected = state.selectedPaymentMethod == method,
                    onClick = { onSelectMethod(method) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.showsCashPair) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TenderFigure(
                    label = stringResource(R.string.pos_cash_in),
                    value = state.cashBuffer.ifEmpty { "—" },
                    modifier = Modifier.weight(1f),
                )
                TenderFigure(
                    // A shortfall is its own state, not "minus change".
                    label = if (state.isShort) {
                        stringResource(R.string.pos_still_owing)
                    } else {
                        stringResource(R.string.pos_change_out)
                    },
                    value = CurrencyFormatter.format(
                        if (state.isShort) state.cartTotal - (state.cashReceived ?: BigDecimal.ZERO)
                        else state.changeDue
                    ),
                    warning = state.isShort,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TenderFigure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    warning: Boolean = false,
) {
    Column(
        modifier = modifier
            .background(
                if (warning) StockOrangeSubtle else ShopWhite,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, fontSize = 10.5.sp, color = ShopBlackTertiary)
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = null,
            color = if (warning) StockOrange else ShopBlack,
        )
    }
}

private val TICKET_PANEL_WIDTH = 240.dp

/** Fixed so the pad below it never moves when the phase changes. */
private val PHASE_BAND_HEIGHT = 84.dp

