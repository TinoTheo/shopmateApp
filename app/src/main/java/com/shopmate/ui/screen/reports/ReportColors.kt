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

// Payment method colours — functional per-method mapping for the breakdown bar (v2 allows these
// as non-brand functional colours). Not used anywhere else.
internal fun paymentColor(method: PaymentMethod): Color = when (method) {
    PaymentMethod.CASH -> Color(0xFF005B52)
    PaymentMethod.ECOCASH -> Color(0xFF4CAF50)
    PaymentMethod.MOBILE_MONEY -> Color(0xFF2196F3)
    PaymentMethod.CARD -> Color(0xFF9C27B0)
    PaymentMethod.BANK_TRANSFER -> Color(0xFFFF9800)
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
