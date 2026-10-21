package com.shopmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.domain.model.DeltaReason
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.NumberLedgerBalance
import com.shopmate.ui.theme.NumberLedgerDelta
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.TrendUp

/**
 * One line of the append-only stock ledger: **what · when · signed delta · running balance**.
 *
 * This is the § brand promise made literal — any screen showing a stock number must be able to
 * answer "why does it say 6?", and this row is the answer. Stock is the sum of these, never an
 * overwrite.
 *
 * **Renders no actor attribution while `StockDelta.actor` is null** — the [when] slot carries
 * time alone until auth ships. A ledger that claims a person's name it does not know is worse
 * than one that omits it.
 *
 * @param what the movement, e.g. "Restock" or receipt number.
 * @param whenText relative time. Appends the actor only once [actor] is non-null.
 * @param delta signed: positive restocks, negative sales and shortfalls.
 * @param runningBalance stock after this movement.
 */
@Composable
fun LedgerRow(
    what: String,
    whenText: String,
    delta: Int,
    runningBalance: Int,
    reason: DeltaReason,
    modifier: Modifier = Modifier,
    actor: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = what,
                fontSize = 13.sp,
                color = ShopBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // Attribution appears only when the data can support it.
                text = if (actor != null) "$whenText · $actor" else whenText,
                fontSize = 11.sp,
                color = ShopBlackMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (delta > 0) "+$delta" else "$delta",
            style = NumberLedgerDelta,
            color = deltaColour(reason),
            textAlign = TextAlign.End,
            modifier = Modifier.defaultMinSize(minWidth = 34.dp),
        )
        Text(
            text = "$runningBalance",
            style = NumberLedgerBalance,
            color = ShopBlackMid,
            textAlign = TextAlign.End,
            modifier = Modifier.defaultMinSize(minWidth = 26.dp),
        )
    }
}

/** Restocks read as growth, count corrections as Warning, sales as ordinary business. */
private fun deltaColour(reason: DeltaReason) = when (reason) {
    DeltaReason.RESTOCK, DeltaReason.PURCHASE -> TrendUp
    DeltaReason.COUNT, DeltaReason.CORRECTION -> StockOrange
    DeltaReason.SALE, DeltaReason.MANUAL_ADJUSTMENT -> ShopBlack
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFCFA)
@Composable
private fun LedgerRowPreview() {
    ShopMateTheme {
        Column(Modifier.padding(16.dp)) {
            LedgerRow("Restock", "28 Aug", 24, 30, DeltaReason.RESTOCK)
            LedgerRow("SM-20260830-0014", "Yesterday, 14:20", -2, 28, DeltaReason.SALE)
            LedgerRow("Counted 26, expected 28", "Yesterday, 18:00", -2, 26, DeltaReason.COUNT)
        }
    }
}
