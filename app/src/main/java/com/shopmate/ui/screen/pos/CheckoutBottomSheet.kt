package com.shopmate.ui.screen.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopmate.R
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.InkTertiary
import com.shopmate.ui.theme.NumberLarge
import com.shopmate.ui.theme.NumberMedium
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.StockRed
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmBorder
import com.shopmate.ui.theme.WarmSand
import com.shopmate.ui.theme.WarmWhite
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.DeviceLayout
import com.shopmate.ui.util.labelRes
import java.math.BigDecimal

/** Widest the till ever grows — on a tablet it's a centred dialog, never a sprawl. */
private val ModalMaxWidth = 460.dp
private val PadMaxWidth = 320.dp


/** The notes a Zimbabwean till actually sees. "Exact" is handled separately. */
private val denominations = listOf(5, 10, 20, 50, 100)

/**
 * The payment moment, laid out the way a real till does it: choose the method, key the cash,
 * and read the change off the biggest number on screen. Bottom sheet on phones, centred dialog
 * on tablets. One spaced Column throughout — nothing overlaps, nothing stretches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutBottomSheet(
    state: PosUiState,
    deviceLayout: DeviceLayout,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCashDigit: (String) -> Unit,
    onCashBackspace: () -> Unit,
    onDenomination: (BigDecimal) -> Unit,
    onExact: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Cap the height so the confirm action is always on screen — a till whose pay button is
    // below the fold is worse than no till at all.
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp

    if (deviceLayout == DeviceLayout.EXPANDED) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WarmWhite,
                modifier = Modifier
                    .padding(Spacing.xl)
                    .widthIn(max = ModalMaxWidth)
                    .heightIn(max = maxHeight),
            ) {
                CheckoutBody(
                    state = state,
                    onSelectMethod = onSelectMethod,
                    onCashDigit = onCashDigit,
                    onCashBackspace = onCashBackspace,
                    onDenomination = onDenomination,
                    onExact = onExact,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = WarmWhite,
        ) {
            // Centre the measure so the till doesn't sprawl on a wide phone / landscape.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Box(
                    modifier = Modifier
                        .widthIn(max = ModalMaxWidth)
                        .heightIn(max = maxHeight),
                ) {
                    CheckoutBody(
                        state = state,
                        onSelectMethod = onSelectMethod,
                        onCashDigit = onCashDigit,
                        onCashBackspace = onCashBackspace,
                        onDenomination = onDenomination,
                        onExact = onExact,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutBody(
    state: PosUiState,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCashDigit: (String) -> Unit,
    onCashBackspace: () -> Unit,
    onDenomination: (BigDecimal) -> Unit,
    onExact: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isCash = state.selectedPaymentMethod == PaymentMethod.CASH

    Column(modifier = Modifier.padding(horizontal = Spacing.xl)) {
        // Everything above the action scrolls; the action itself never leaves the screen.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            CheckoutHeader(itemCount = state.cartItemCount, onDismiss = onDismiss)

            PaymentMethodSelector(selected = state.selectedPaymentMethod, onSelect = onSelectMethod)

            MoneyStack(
                total = state.cartTotal,
                isCash = isCash,
                method = state.selectedPaymentMethod,
                cashBuffer = state.cashBuffer,
                cashReceived = state.cashReceived,
                changeDue = state.changeDue,
                isShort = state.isShort,
            )

            if (isCash) {
                DenominationRow(onDenomination = onDenomination, onExact = onExact)
                NumberPad(
                    onDigit = onCashDigit,
                    onBackspace = onCashBackspace,
                    modifier = Modifier
                        .widthIn(max = PadMaxWidth)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }

        ConfirmAction(
            state = state,
            isCash = isCash,
            onConfirm = onConfirm,
            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xl),
        )
    }
}

@Composable
private fun CheckoutHeader(itemCount: Int, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pos_checkout_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Ink,
            )
            Text(
                text = pluralStringResource(R.plurals.pos_items, itemCount, itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.checkout_close),
                tint = InkTertiary,
            )
        }
    }
}

/** Method decides what inputs show, so it comes first. Tiles wrap; they never stretch. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodSelector(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    // The same PaymentMethodCard the tablet ticket uses: one implementation, two containers.
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TILL_PAYMENT_METHODS.forEach { method ->
            PaymentMethodCard(
                method = method,
                selected = method == selected,
                onClick = { onSelect(method) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The retail heart: Total / Cash received / Change, right-aligned with tabular figures so the
 * decimal points stack. Change is the largest number here — it's what the cashier reads aloud.
 */
@Composable
private fun MoneyStack(
    total: BigDecimal,
    isCash: Boolean,
    method: PaymentMethod,
    cashBuffer: String,
    cashReceived: BigDecimal?,
    changeDue: BigDecimal,
    isShort: Boolean,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = WarmSand, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MoneyRow(
                label = stringResource(R.string.checkout_total),
                amount = CurrencyFormatter.format(total),
                amountStyle = MaterialTheme.typography.headlineLarge,
                amountColor = Ink,
            )

            if (isCash) {
                HorizontalDivider(color = WarmBorder)
                MoneyRow(
                    label = stringResource(R.string.checkout_cash_received),
                    // Show raw keystrokes so "12." reads back exactly as keyed.
                    amount = if (cashBuffer.isEmpty()) {
                        CurrencyFormatter.format(BigDecimal.ZERO)
                    } else {
                        "${CurrencyFormatter.symbol}$cashBuffer"
                    },
                    amountStyle = MaterialTheme.typography.headlineLarge,
                    amountColor = if (cashReceived == null) InkTertiary else Ink,
                )

                HorizontalDivider(color = WarmBorder)
                ChangeRow(cashReceived = cashReceived, changeDue = changeDue, isShort = isShort, total = total)
            } else {
                HorizontalDivider(color = WarmBorder)
                Text(
                    text = stringResource(R.string.checkout_paid_by, stringResource(method.labelRes())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
            }
        }
    }
}

/** The hero line. Green when the money's there, red "Short" when it isn't, muted before entry. */
@Composable
private fun ChangeRow(
    cashReceived: BigDecimal?,
    changeDue: BigDecimal,
    isShort: Boolean,
    total: BigDecimal,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.checkout_change),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
        if (isShort) {
            Spacer(Modifier.width(Spacing.sm))
            Surface(shape = RoundedCornerShape(8.dp), color = StockRed) {
                Text(
                    text = stringResource(R.string.checkout_short_tag),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmWhite,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))   // the slack lives here, so the amounts keep their size
        Text(
            text = when {
                isShort -> CurrencyFormatter.format(total.subtract(cashReceived ?: BigDecimal.ZERO))
                else -> CurrencyFormatter.format(changeDue)
            },
            style = NumberLarge,
            color = when {
                cashReceived == null -> InkTertiary
                isShort -> StockRed
                else -> TradeGreen
            },
        )
    }
}

@Composable
private fun MoneyRow(
    label: String,
    amount: String,
    amountStyle: androidx.compose.ui.text.TextStyle,
    amountColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = InkSecondary,
        )
        Spacer(Modifier.weight(1f))
        Text(text = amount, style = amountStyle.merge(NumberMedium), color = amountColor)
    }
}

/** The row every till has. Content-sized, wraps to a new line — never stretched. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DenominationRow(
    onDenomination: (BigDecimal) -> Unit,
    onExact: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        DenominationChip(label = stringResource(R.string.checkout_exact), onClick = onExact)
        denominations.forEach { note ->
            DenominationChip(
                label = "${CurrencyFormatter.symbol}$note",
                onClick = { onDenomination(BigDecimal(note)) },
            )
        }
    }
}

@Composable
private fun DenominationChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = WarmWhite,
        border = BorderStroke(1.dp, WarmBorder),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Ink,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

/**
 * One action, never two. The wrapper fills the width purely to centre the button; the button
 * itself is a defined width. The label says exactly what to do with the money.
 */
@Composable
private fun ConfirmAction(
    state: PosUiState,
    isCash: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = CurrencyFormatter.format(state.cartTotal)
    val label = when {
        !isCash -> stringResource(
            R.string.checkout_confirm_method,
            total,
            stringResource(state.selectedPaymentMethod.labelRes()),
        )
        state.cashReceived == null || state.isShort -> stringResource(R.string.checkout_confirm_need_cash)
        state.changeDue.signum() > 0 -> stringResource(
            R.string.checkout_confirm_give_change,
            CurrencyFormatter.format(state.cashReceived),
            CurrencyFormatter.format(state.changeDue),
        )
        else -> stringResource(R.string.checkout_confirm_exact, total)
    }

    // fillMaxWidth is on the WRAPPER only, purely to centre — never on the button itself.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onConfirm,
            enabled = state.canConfirmSale && !state.isProcessingSale,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TradeGreen,
                contentColor = WarmWhite,
                disabledContainerColor = TradeGreen.copy(alpha = 0.38f),
                disabledContentColor = WarmWhite.copy(alpha = 0.7f),
            ),
            modifier = Modifier
                .widthIn(min = 220.dp, max = 340.dp)
                .height(56.dp),
        ) {
            if (state.isProcessingSale) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = WarmWhite,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
