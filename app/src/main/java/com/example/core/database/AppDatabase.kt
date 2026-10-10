package com.example.core.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
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
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        WaterSession::class,
        PumpSource::class,
        Voucher::class,
        AppSetting::class,
        WellOwnerPurchase::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun waterSessionDao(): WaterSessionDao
    abstract fun pumpSourceDao(): PumpSourceDao
    abstract fun voucherDao(): VoucherDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun wellOwnerPurchaseDao(): WellOwnerPurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vouchers ADD COLUMN sessionId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE customers ADD COLUMN isBeneficiary INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE water_sessions ADD COLUMN billedToCustomerId INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE water_sessions ADD COLUMN wastedMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE water_sessions ADD COLUMN wastedReason TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE water_sessions ADD COLUMN discountAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE water_sessions ADD COLUMN costPricePerHour REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE water_sessions ADD COLUMN pumpSourceId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE pump_sources ADD COLUMN ownerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pump_sources ADD COLUMN ownerPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pump_sources ADD COLUMN costPricePerHour REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE pump_sources ADD COLUMN ownerCustomerId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE customers ADD COLUMN isWellOwner INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `well_owner_purchases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerCustomerId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `wastedMinutesOnOwner` INTEGER NOT NULL,
                        `purchaseRatePerHour` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`ownerCustomerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_well_owner_purchases_ownerCustomerId` ON `well_owner_purchases` (`ownerCustomerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_well_owner_purchases_date` ON `well_owner_purchases` (`date`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_distributor_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
                db.withTransaction {
                    val existing = db.appSettingDao().getSettingValue("distributor_name")
                    if (existing == null) {
                        populateInitialData(db)
                    }
                }
            } catch (error: Exception) {
                Log.e("AppDatabase", "Failed to initialize default data", error)
            }
        }

        /**
         * Seeds only what a brand-new installation genuinely needs: the settings row that marks the
         * database as initialised plus one starter pump so the distributor can begin recording
         * immediately.
         *
         * The demo customers, water sessions and vouchers that used to be inserted here were
         * deliberately removed - a real distributor must never open the app and find fake debts and
         * fake receipts mixed into his own books.
         */
        private suspend fun populateInitialData(db: AppDatabase) {
            // Default app settings
            db.appSettingDao().saveSetting(AppSetting("distributor_name", "موزع الماء / المسرب"))
            db.appSettingDao().saveSetting(AppSetting("distributor_phone", ""))
            db.appSettingDao().saveSetting(AppSetting("default_price_per_hour", "5000"))
            db.appSettingDao().saveSetting(AppSetting("currency_symbol", "ر.ي"))
            db.appSettingDao().saveSetting(AppSetting("theme_mode", "SYSTEM"))

            // Starter pump source for manual session records
            db.pumpSourceDao().insertPump(
                PumpSource(
                    name = "البئر الرئيسي",
                    locationOrWellNumber = "",
                    defaultPricePerHour = 5000.0,
                    powerType = "ديزل",
                    notes = "",
                    isPrimary = true
                )
            )
        }
    }
}
