package com.shopmate.ui.screen.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.ui.components.ShopMatePrimaryButton
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.NumberStyle
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DateFormatter

/**
 * The one card at the top of Sync — see [SyncCard] for the priority. Offline is calm: no error
 * styling and no spinner; the uploads go by themselves when the till is back. Only a refusal is
 * red, and only its dot.
 */
@Composable
fun SyncStatusCard(
    card: SyncCard,
    onSendNow: () -> Unit,
    onSignIn: () -> Unit,
    onSeeWhy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ShopWhite)
            .border(1.dp, ReceiptBeige, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        when (card) {
            is SyncCard.Paused -> {
                Title(stringResource(R.string.sync_card_paused_title))
                Body(pluralStringResource(R.plurals.sync_card_paused_body, card.waiting, card.waiting))
                Action(stringResource(R.string.sync_card_paused_action), onSignIn)
            }

            is SyncCard.NotSent -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Title(pluralStringResource(R.plurals.sync_card_not_sent_title, card.notSent, card.notSent))
                }
                Body(pluralStringResource(R.plurals.sync_card_not_sent_body, card.notSent))
                Text(
                    text = stringResource(R.string.sync_card_not_sent_action),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TradeGreen,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable(onClick = onSeeWhy),
                )
                if (card.waiting > 0) {
                    HorizontalDivider(thickness = 1.dp, color = ReceiptBeige, modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.sync_card_waiting_title, card.waiting, card.waiting),
                            style = NumberStyle(13.5.sp),
                            color = ShopBlackSecondary,
                        )
                        if (card.canSendNow) {
                            Text(
                                text = stringResource(R.string.sync_send_now),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TradeGreen,
                                modifier = Modifier.clickable(onClick = onSendNow),
                            )
                        }
                    }
                }
            }

            is SyncCard.Offline -> {
                Title(pluralStringResource(R.plurals.sync_card_waiting_title, card.waiting, card.waiting))
                Body(stringResource(R.string.sync_card_offline_body))
                card.since?.let { since ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                        Dot(ShopBlackTertiary, hollow = true)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sync_card_offline_since, DateFormatter.formatClock(since)),
                            style = NumberStyle(12.5.sp),
                            color = ShopBlackTertiary,
                        )
                    }
                }
            }

            is SyncCard.Waiting -> {
                Title(pluralStringResource(R.plurals.sync_card_waiting_title, card.waiting, card.waiting))
                Body(waitingBreakdown(card.sales, card.productLists))
                Action(
                    text = stringResource(if (card.sending) R.string.sync_status_sending else R.string.sync_send_now),
                    onClick = onSendNow,
                    enabled = !card.sending,
                )
            }

            is SyncCard.AllSent -> {
                Title(stringResource(R.string.sync_card_all_sent_title))
                card.lastSentAt?.let { Body(stringResource(R.string.sync_card_all_sent_body, DateFormatter.formatClock(it))) }
            }
        }
    }
}

/** "2 sales · 1 product list". The handoff's mock joins with "and"; no key exists for it. */
@Composable
private fun waitingBreakdown(sales: Int, productLists: Int): String {
    val parts = buildList {
        if (sales > 0) add(pluralStringResource(R.plurals.sync_kind_sales, sales, sales))
        if (productLists > 0) add(pluralStringResource(R.plurals.sync_kind_product_lists, productLists, productLists))
    }
    return when (parts.size) {
        2 -> stringResource(R.string.header_status_join, parts[0], parts[1])
        else -> parts.firstOrNull().orEmpty()
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = NumberStyle(17.sp, FontWeight.SemiBold), color = ShopBlack)
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        color = ShopBlackSecondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun Action(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Spacer(Modifier.height(14.dp))
    ShopMatePrimaryButton(text = text, onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth())
}

/** 8dp status dot; hollow for offline and paused. */
@Composable
internal fun Dot(color: androidx.compose.ui.graphics.Color, hollow: Boolean = false) {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .then(if (hollow) Modifier.border(1.5.dp, color, CircleShape) else Modifier.background(color)),
    )
}
