package com.shopmate.domain.util

import com.shopmate.data.local.TillCode
import com.shopmate.data.local.dao.ReceiptCounterDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates human-readable receipt numbers of the form `SM-YYYYMMDD-TTT-XXXX`: the day, this
 * till's [TillCode], and a zero-padded sequence that resets each calendar day. The daily counter
 * is persisted in Room (see [ReceiptCounterDao]) so numbers survive process death; the till code
 * keeps two tills' sequences from ever producing the same number in one store.
 */
@Singleton
class ReceiptNumberGenerator @Inject constructor(
    private val receiptCounterDao: ReceiptCounterDao,
    private val tillCode: TillCode,
) {
    suspend fun next(today: LocalDate = LocalDate.now()): String {
        val sequence = receiptCounterDao.nextNumber(today.format(KEY_FORMAT))
        return "SM-${today.format(DATE_FORMAT)}-${tillCode.get()}-%04d".format(sequence)
    }

    companion object {
        private val KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
