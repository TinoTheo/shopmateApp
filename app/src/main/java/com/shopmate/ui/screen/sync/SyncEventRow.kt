package com.shopmate.ui.screen.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.EventType
import com.shopmate.domain.model.SyncFailureReason
import com.shopmate.ui.components.ShopMateSecondaryButton
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.NumberStyle
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopOffWhite
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.DateFormatter

/**
 * One history row, named by what was sent ("Sale · $3.40"), never by the mechanism. A refused
 * row expands on tap ([expanded]) to say why and what to do; on tablets the same content sits in
 * the side panel instead ([RefusedRowContent]).
 */
@Composable
fun SyncEventRow(
    row: SyncRowUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSendAgain: () -> Unit,
    onCopyDetails: (String) -> Unit,
    tillCode: String,
    modifier: Modifier = Modifier,
    /** Tablet: refused rows open in the side panel, so the row never expands in place. */
    expandInPlace: Boolean = true,
    selected: Boolean = false,
) {
    val refused = row.status == RowStatus.NOT_SENT
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (selected || (expanded && expandInPlace)) {
                    Modifier
                        .background(ShopWhite, RoundedCornerShape(12.dp))
                        .border(1.dp, ReceiptBeige, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = refused || !expandInPlace, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(text = row.title(), style = NumberStyle(14.5.sp, FontWeight.Medium), color = ShopBlack)
                row.metaLines().forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = NumberStyle(12.5.sp),
                        color = if (index == 0) ShopBlackSecondary else ShopBlackTertiary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            StatusLabel(row.status)
        }
        if (expandInPlace) {
            AnimatedVisibility(visible = refused && expanded) {
                RefusedRowContent(row, onSendAgain, onCopyDetails, tillCode, Modifier.padding(top = 12.dp))
            }
        }
    }
}

/** Why it was refused, what to do, Send again, and the raw answer tucked under Details. */
@Composable
fun RefusedRowContent(
    row: SyncRowUi,
    onSendAgain: () -> Unit,
    onCopyDetails: (String) -> Unit,
    tillCode: String,
    modifier: Modifier = Modifier,
) {
    var detailsOpen by remember(row.id) { mutableStateOf(false) }
    val (reason, todo) = row.reasonAndTodo()
    Column(modifier) {
        Text(text = reason, fontSize = 13.5.sp, lineHeight = 19.sp, color = ShopBlack)
        Spacer(Modifier.height(10.dp))
        Text(text = stringResource(R.string.sync_todo_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ShopBlack)
        Text(text = todo, fontSize = 13.5.sp, lineHeight = 19.sp, color = ShopBlackMid, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(12.dp))
        ShopMateSecondaryButton(
            text = stringResource(R.string.sync_send_again),
            onClick = onSendAgain,
            modifier = Modifier.heightIn(min = 40.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { detailsOpen = !detailsOpen }
                .padding(vertical = 6.dp),
        ) {
            Text(text = stringResource(R.string.sync_details_toggle), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TradeGreen)
            Icon(
                if (detailsOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = TradeGreen,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = detailsOpen) {
            val context = stringResource(
                R.string.sync_details_context,
                row.title(),
                DateFormatter.formatDayClock(row.at),
                tillCode,
            )
            val raw = row.rawError.orEmpty()
            Column(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .background(ShopOffWhite, RoundedCornerShape(10.dp))
                    .border(1.dp, ReceiptBeige, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                Text(text = stringResource(R.string.sync_details_label), fontSize = 11.sp, color = ShopBlackTertiary)
                Text(text = raw, fontSize = 12.sp, color = ShopBlackMid, modifier = Modifier.padding(top = 4.dp))
                Text(text = context, style = NumberStyle(12.sp), color = ShopBlackMid, modifier = Modifier.padding(top = 6.dp))
                Text(
                    text = stringResource(R.string.sync_details_copy),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TradeGreen,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onCopyDetails("$raw\n$context") },
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(status: RowStatus) {
    val (text, color, weight) = when (status) {
        RowStatus.WAITING -> Triple(stringResource(R.string.sync_status_waiting), ShopBlackSecondary, FontWeight.Normal)
        RowStatus.SENDING -> Triple(stringResource(R.string.sync_status_sending), ShopBlackSecondary, FontWeight.Normal)
        RowStatus.SENT -> Triple(stringResource(R.string.sync_status_sent), ShopBlackSecondary, FontWeight.Normal)
        RowStatus.PAUSED -> Triple(stringResource(R.string.sync_status_paused), ShopBlackSecondary, FontWeight.Normal)
        RowStatus.NOT_SENT -> Triple(stringResource(R.string.sync_status_not_sent), ErrorRed, FontWeight.Medium)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            RowStatus.SENT -> { Dot(TradeGreen); Spacer(Modifier.width(6.dp)) }
            RowStatus.NOT_SENT -> { Dot(ErrorRed); Spacer(Modifier.width(6.dp)) }
            else -> Unit
        }
        Text(text = text, fontSize = 12.5.sp, fontWeight = weight, color = color)
    }
}

/** "Sale · $3.40" or "Product list". */
@Composable
private fun SyncRowUi.title(): String = when (type) {
    EventType.SALE -> stringResource(R.string.sync_row_sale_title, amount?.let(CurrencyFormatter::format).orEmpty())
    EventType.SNAPSHOT -> stringResource(R.string.sync_row_snapshot_title)
}

/** First line: what and when; then any line the state adds. */
@Composable
private fun SyncRowUi.metaLines(): List<String> {
    val time = DateFormatter.formatClock(at)
    val first = when {
        status == RowStatus.PAUSED -> stringResource(R.string.sync_reason_signed_out_meta)
        type == EventType.SNAPSHOT -> stringResource(R.string.sync_row_snapshot_meta, time)
        originalReceiptNumber != null && status == RowStatus.SENT ->
            stringResource(R.string.sync_reason_renumbered_meta, receiptNumber.orEmpty(), DateFormatter.formatClock(sentAt ?: at))
        else -> stringResource(R.string.sync_row_meta, receiptNumber.orEmpty(), time)
    }
    return buildList {
        add(first)
        if (originalReceiptNumber != null && status == RowStatus.SENT) {
            add(stringResource(R.string.sync_reason_renumbered, originalReceiptNumber))
        }
        if (noAnswer) add(stringResource(R.string.sync_reason_no_answer))
    }
}

/** The plain reason and what to do. The raw server text never appears here. */
@Composable
private fun SyncRowUi.reasonAndTodo(): Pair<String, String> = when (val r = reason) {
    is SyncFailureReason.NotAllowed -> when (r.kind) {
        SyncFailureReason.Kind.SALE ->
            stringResource(R.string.sync_reason_forbidden_sale) to stringResource(R.string.sync_todo_forbidden_sale)
        SyncFailureReason.Kind.STOCK ->
            stringResource(R.string.sync_reason_forbidden_stock) to stringResource(R.string.sync_todo_forbidden_stock)
        SyncFailureReason.Kind.PRICE ->
            stringResource(R.string.sync_reason_forbidden_price) to stringResource(R.string.sync_todo_forbidden_price)
    }
    // A taken receipt number is renumbered automatically on the next send, so it reads as any
    // other refusal: Send again fixes it.
    else -> stringResource(
        if (type == EventType.SALE) R.string.sync_reason_other_sale else R.string.sync_reason_other_change,
    ) to stringResource(R.string.sync_todo_other)
}
