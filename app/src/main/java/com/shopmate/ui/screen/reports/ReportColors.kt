package com.shopmate.ui.screen.reports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.shopmate.R
import com.shopmate.domain.model.PaymentMethod
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.TradeGreen

// Payment colours for the breakdown bar. Two-colour discipline, no new hues: cash is Trade
// Green, mobile money Trade Green at 55%, card and bank Receipt Beige.
internal fun paymentColor(method: PaymentMethod): Color = when (method) {
    PaymentMethod.CASH -> TradeGreen
    PaymentMethod.ECOCASH, PaymentMethod.MOBILE_MONEY -> TradeGreen.copy(alpha = 0.55f)
    PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER -> ReceiptBeige
}

/** The legend icon's tint. Receipt Beige is a fill, too light to draw an icon in. */
internal fun paymentIconTint(method: PaymentMethod): Color = when (method) {
    PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER -> ShopBlackTertiary
    else -> TradeGreen
}

internal fun paymentIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.CASH -> Icons.Outlined.Payments
    PaymentMethod.ECOCASH -> Icons.Outlined.PhoneAndroid
    PaymentMethod.MOBILE_MONEY -> Icons.Outlined.Smartphone
    PaymentMethod.CARD -> Icons.Outlined.CreditCard
    PaymentMethod.BANK_TRANSFER -> Icons.Outlined.AccountBalance
}

internal fun paymentLabelRes(method: PaymentMethod): Int = when (method) {
    PaymentMethod.CASH -> R.string.payment_cash
    PaymentMethod.ECOCASH -> R.string.payment_ecocash
    PaymentMethod.MOBILE_MONEY -> R.string.payment_mobile_money
    PaymentMethod.CARD -> R.string.payment_card
    PaymentMethod.BANK_TRANSFER -> R.string.payment_bank_transfer
}
