package com.baynana.core.database

import androidx.room.TypeConverter
import com.baynana.features.vouchers.VoucherType

class Converters {
    @TypeConverter
    fun fromVoucherType(value: VoucherType): String = value.name

    @TypeConverter
    fun toVoucherType(value: String): VoucherType = runCatching {
        VoucherType.valueOf(value)
    }.getOrDefault(VoucherType.RECEIPT)
}
