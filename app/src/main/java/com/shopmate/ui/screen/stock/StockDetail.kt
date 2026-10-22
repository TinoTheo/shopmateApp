package com.shopmate.ui.screen.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.ProductInsight
import com.shopmate.ui.theme.Eyebrow
import com.shopmate.ui.components.LedgerRow
import com.shopmate.ui.components.RunwayGauge
import com.shopmate.ui.components.ShopMateSecondaryButton
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.util.DeviceLayout
import com.shopmate.ui.util.RelativeTimeFormatter

/**
 * The stock detail surface. **One composable, two containers** (CLAUDE.md § Adaptive layouts):
 * a bottom sheet on [DeviceLayout.COMPACT], a 340dp right-hand panel pinned to the sheet edge
 * on [DeviceLayout.EXPANDED]. Do not write two detail screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailSurface(
    insight: ProductInsight,
    ledger: List<LedgerEntry>,
    deviceLayout: DeviceLayout,
    onAddToOrderList: () -> Unit,
    /** Null when the member has no stock rights here — the Adjust action is not offered. */
    onAdjust: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    when (deviceLayout) {
        DeviceLayout.COMPACT -> ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ShopWhite,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        ) {
            StockDetailContent(insight, ledger, onAddToOrderList, onAdjust)
        }

        DeviceLayout.EXPANDED -> Column(
            modifier = Modifier
                .width(DETAIL_PANEL_WIDTH)
                .fillMaxHeight(),
        ) {
            StockDetailContent(insight, ledger, onAddToOrderList, onAdjust)
        }
    }
}

@Composable
private fun StockDetailContent(
    insight: ProductInsight,
    ledger: List<LedgerEntry>,
    onAddToOrderList: () -> Unit,
    onAdjust: (() -> Unit)?,
) {
    val product = insight.product

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(text = product.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ShopBlack)
        Text(
            text = "${product.stockQuantity} ${product.unit}",
            fontSize = 12.sp,
            color = ShopBlackMid,
        )

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RunwayGauge(
                daysOfCover = insight.daysOfCover,
                confident = insight.confident,
                modifier = Modifier.width(80.dp),
            )
            Spacer(Modifier.width(10.dp))
            // The same RunOut the list row reads, so the two can never disagree.
            Column {
                Text(text = insight.runOut.detailLabel(), fontSize = 12.sp, color = ShopBlackMid)
                insight.runOutBasis()?.let { basis ->
                    Text(text = basis, fontSize = 11.sp, color = ShopBlackTertiary)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        // The brand promise made literal: any screen showing a stock number must be able to
        // answer "why does it say 6?". This list is the answer.
        Text(
            text = stringResource(R.string.stock_why_it_says, product.stockQuantity).uppercase(),
            style = Eyebrow,
        )
        HorizontalDivider(color = ReceiptBeige)
    }

    LazyColumn(
        modifier = Modifier
            .heightIn(max = LEDGER_MAX_HEIGHT)
            .padding(horizontal = 16.dp),
    ) {
        items(ledger, key = { it.delta.id }) { entry ->
            LedgerRow(
                what = entry.delta.what(),
                whenText = RelativeTimeFormatter.format(entry.delta.createdAt),
                delta = entry.delta.delta,
                runningBalance = entry.runningBalance,
                reason = entry.delta.reason,
                // Null until auth ships — the row renders time alone rather than a placeholder.
                actor = entry.delta.actor,
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShopMateSecondaryButton(
            text = stringResource(R.string.stock_add_to_order),
            onClick = onAddToOrderList,
            modifier = Modifier.weight(1f),
        )
        if (onAdjust != null) {
            ShopMateSecondaryButton(
                text = stringResource(R.string.stock_adjust),
                onClick = onAdjust,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** What the movement was, named by what the person did — never by the table it came from. */
@Composable
private fun com.shopmate.domain.model.StockDelta.what(): String = note?.takeIf { it.isNotBlank() }
    ?: stringResource(
        when (reason) {
            DeltaReason.SALE -> R.string.reason_sale
            DeltaReason.RESTOCK -> R.string.reason_restock
            DeltaReason.MANUAL_ADJUSTMENT -> R.string.reason_adjustment
            DeltaReason.CORRECTION -> R.string.reason_correction
            DeltaReason.PURCHASE -> R.string.reason_purchase
            DeltaReason.COUNT -> R.string.reason_count
        }
    )

/** Pinned to the sheet edge on tablets; the phone shows the same content in a bottom sheet. */
private val DETAIL_PANEL_WIDTH = 340.dp

/** The ledger scrolls inside the surface rather than pushing the actions off it. */
private val LEDGER_MAX_HEIGHT = 360.dp
