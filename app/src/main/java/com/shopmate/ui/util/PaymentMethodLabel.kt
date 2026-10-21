package com.shopmate.ui.util

import androidx.annotation.StringRes
import com.shopmate.R
import com.shopmate.domain.model.PaymentMethod

/** Display label for a payment method — shared by checkout and sales history. */
@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.payment_cash
    PaymentMethod.ECOCASH -> R.string.payment_ecocash
    PaymentMethod.MOBILE_MONEY -> R.string.payment_mobile_money
    PaymentMethod.CARD -> R.string.payment_card
    PaymentMethod.BANK_TRANSFER -> R.string.payment_bank_transfer
}
