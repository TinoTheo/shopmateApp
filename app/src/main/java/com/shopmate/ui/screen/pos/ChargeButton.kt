package com.shopmate.ui.screen.pos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal

/**
 * THE charge button — the largest touch target on the POS, tapped every sale.
 * Same component on phone and tablet; only its anchor differs. It must never
 * move or resize with content (cashier muscle memory).
 *
 * Filled [CheckoutNeon]: § The Checkout Neon rule names the POS Charge button as a **content
 * sheet commit** — a thing that succeeded and cannot be undone. This button previously filled
 * [com.shopmate.ui.theme.TradeGreen]; v3.1 ratified the colour table over the composable. The
 * geometry (14dp radius, 18dp padding, 17sp Medium, 38% alpha disabled) is unchanged.
 *
 * Neon does not repeat within one commit path — the success banner that follows uses a Neon
 * edge over a Neon-subtle fill, and nothing else in the ticket competes.
 */
@Composable
fun ChargeButton(
    total: BigDecimal,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Overrides the default `Charge $X`. The tablet ticket passes `Next sale` in
     * [TicketPhase.DONE] — the button stays put and says what it now does, per § Copy and voice.
     */
    label: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CheckoutNeon,
            contentColor = ShopBlack,
            disabledContainerColor = CheckoutNeon.copy(alpha = 0.38f),
            disabledContentColor = ShopBlack,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label ?: stringResource(R.string.pos_charge, CurrencyFormatter.format(total)),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
