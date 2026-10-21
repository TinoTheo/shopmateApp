package com.shopmate.data.local.converter

import androidx.room.TypeConverter
import com.shopmate.domain.model.DeltaReason
import com.shopmate.domain.model.PaymentMethod
import java.math.BigDecimal
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let { PaymentMethod.valueOf(it) }

    @TypeConverter
    fun fromDeltaReason(value: DeltaReason?): String? = value?.name

    @TypeConverter
    fun toDeltaReason(value: String?): DeltaReason? = value?.let { DeltaReason.valueOf(it) }
}
