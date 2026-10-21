package com.shopmate.ui.screen.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.theme.WarmBorder
import com.shopmate.ui.util.labelRes

/**
 * One tender choice. 38dp tall, 11dp radius; selected fills [TradeGreen] with white text.
 *
 * The **same** component on the phone tender sheet and in the tablet sale ticket — the two
 * surfaces differ in their container, not in what a payment method looks like. Selection is
 * carried by the green fill, never by Checkout Neon: a choice is not a commit
 * (CLAUDE.md § The Checkout Neon rule).
 */
@Composable
fun PaymentMethodCard(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) TradeGreen else ShopWhite,
        border = if (selected) null else BorderStroke(1.dp, WarmBorder),
        modifier = modifier.height(38.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(method.labelRes()),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) ShopWhite else ShopBlackSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The three tenders that belong at a till.
 *
 * [PaymentMethod] also carries `MOBILE_MONEY` and `BANK_TRANSFER`, and they stay off the till
 * surface: the enum having five values is not a reason for five buttons. Surfacing them is a
 * decision to be taken in CLAUDE.md, not in a composable.
 */
val TILL_PAYMENT_METHODS = listOf(
    PaymentMethod.CASH,
    PaymentMethod.ECOCASH,
    PaymentMethod.CARD,
)
