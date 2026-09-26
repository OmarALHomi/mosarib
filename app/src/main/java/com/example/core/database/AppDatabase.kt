package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.features.customers.Customer
import com.example.features.customers.CustomerDao
import com.example.features.pumps.PumpSource
import com.example.features.pumps.PumpSourceDao
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionDao
import com.example.features.settings.AppSetting
import com.example.features.settings.AppSettingDao
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        WaterSession::class,
        PumpSource::class,
        Voucher::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun waterSessionDao(): WaterSessionDao
    abstract fun pumpSourceDao(): PumpSourceDao
    abstract fun voucherDao(): VoucherDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_distributor_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance

                CoroutineScope(Dispatchers.IO).launch {
                    ensureInitialData(instance)
                }

                instance
            }
        }

        private suspend fun ensureInitialData(db: AppDatabase) {
            try {
                val existing = db.appSettingDao().getSettingValue("distributor_name")
                if (existing == null) {
                    populateInitialData(db)
                }
            } catch (_: Exception) {
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Default pump source
            db.pumpSourceDao().insertPump(
                PumpSource(
                    name = "مضخة البئر الرئيسية",
                    locationOrWellNumber = "البئر رقم 1 - المزرعة الشمالية",
                    defaultPricePerHour = 5000.0,
                    powerType = "ديزل",
                    notes = "المضخة الأساسية للري",
                    isPrimary = true
                )
            )

            // Default app settings
            db.appSettingDao().saveSetting(AppSetting("distributor_name", "موزع الماء / المسرب"))
            db.appSettingDao().saveSetting(AppSetting("distributor_phone", "777000000"))
            db.appSettingDao().saveSetting(AppSetting("default_price_per_hour", "5000"))
            db.appSettingDao().saveSetting(AppSetting("currency_symbol", "ر.ي"))
            db.appSettingDao().saveSetting(AppSetting("theme_mode", "SYSTEM"))

            // Sample initial customers to make the app ready immediately
            val c1 = db.customerDao().insertCustomer(
                Customer(
                    name = "أبو محمد اليافعي",
                    phone = "771234567",
                    farmName = "مزرعة وادي النخيل",
                    location = "القطاع الشرقي",
                    notes = "سداد شهري منتظم"
                )
            )
            val c2 = db.customerDao().insertCustomer(
                Customer(
                    name = "سالم باعباد",
                    phone = "772345678",
                    farmName = "بستان الخير والبركة",
                    location = "القطاع الغربي",
                    notes = "أرض زراعية 4 فدان"
                )
            )
            val c3 = db.customerDao().insertCustomer(
                Customer(
                    name = "حسين القحطاني",
                    phone = "773456789",
                    farmName = "مزرعة الرمان",
                    location = "طريق السد",
                    notes = "ري أسبوعي"
                )
            )

            val now = System.currentTimeMillis()
            val hourMs = 3600_000L

            // Sample past water sessions (historical pricing locked at 5000/hr)
            // Session 1: 3 hours 30 mins = 210 mins => cost = 3.5 * 5000 = 17500. Paid 10000, debt 7500
            db.waterSessionDao().insertSession(
                WaterSession(
                    customerId = c1,
                    pumpName = "مضخة البئر الرئيسية",
                    startTime = now - (24 * hourMs),
                    endTime = now - (24 * hourMs) + (210 * 60_000L),
                    durationMinutes = 210,
                    pricePerHour = 5000.0,
                    totalAmount = 17500.0,
                    amountPaid = 10000.0,
                    remainingDebt = 7500.0,
                    notes = "ري أشجار النخيل والحمضيات",
                    isLive = false
                )
            )

            // Session 2: 2 hours 15 mins = 135 mins => cost = 2.25 * 5000 = 11250. Paid 11250 (Full)
            db.waterSessionDao().insertSession(
                WaterSession(
                    customerId = c2,
                    pumpName = "مضخة البئر الرئيسية",
                    startTime = now - (12 * hourMs),
                    endTime = now - (12 * hourMs) + (135 * 60_000L),
                    durationMinutes = 135,
                    pricePerHour = 5000.0,
                    totalAmount = 11250.0,
                    amountPaid = 11250.0,
                    remainingDebt = 0.0,
                    notes = "ري الخضروات الصيفية",
                    isLive = false
                )
            )

            // Sample Vouchers
            db.voucherDao().insertVoucher(
                Voucher(
                    voucherNumber = "REC-101",
                    type = com.example.features.vouchers.VoucherType.RECEIPT,
                    customerId = c1,
                    amount = 5000.0,
                    category = "سداد حساب",
                    paymentMethod = "نقداً",
                    date = now - (6 * hourMs),
                    notes = "دفعة من الحساب السابق"
                )
            )

            db.voucherDao().insertVoucher(
                Voucher(
                    voucherNumber = "EXP-201",
                    type = com.example.features.vouchers.VoucherType.EXPENSE,
                    customerId = null,
                    amount = 8000.0,
                    category = "ديزل ووقود",
                    paymentMethod = "نقداً",
                    date = now - (18 * hourMs),
                    notes = "تعبئة 40 لتر ديزل للمضخة"
                )
            )
        }
    }
}
