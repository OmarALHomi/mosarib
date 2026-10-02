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
import com.example.features.farmer.LinkedMusrib
import com.example.features.farmer.LinkedMusribDao
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
import com.example.features.market.CropListing
import com.example.features.market.CropListingDao
import com.example.features.deals.SettlementDeal
import com.example.features.deals.DealPayment
import com.example.features.deals.SettlementDealDao
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        WaterSession::class,
        PumpSource::class,
        Voucher::class,
        AppSetting::class,
        LinkedMusrib::class,
        CropListing::class,
        SettlementDeal::class,
        DealPayment::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun waterSessionDao(): WaterSessionDao
    abstract fun pumpSourceDao(): PumpSourceDao
    abstract fun voucherDao(): VoucherDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun linkedMusribDao(): LinkedMusribDao
    abstract fun cropListingDao(): CropListingDao
    abstract fun settlementDealDao(): SettlementDealDao

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
                db.execSQL("ALTER TABLE customers ADD COLUMN linkCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS linked_musribs (
                        linkCode TEXT NOT NULL PRIMARY KEY,
                        musribName TEXT NOT NULL DEFAULT '',
                        musribPhone TEXT NOT NULL DEFAULT '',
                        farmName TEXT NOT NULL DEFAULT '',
                        currentBalance REAL NOT NULL DEFAULT 0.0,
                        totalDebit REAL NOT NULL DEFAULT 0.0,
                        totalPaid REAL NOT NULL DEFAULT 0.0,
                        lastSyncTimestamp INTEGER NOT NULL DEFAULT 0,
                        addedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS crop_listings (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        cropType TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        district TEXT NOT NULL DEFAULT '',
                        village TEXT NOT NULL DEFAULT '',
                        priceEstimate REAL NOT NULL DEFAULT 0.0,
                        priceUnit TEXT NOT NULL DEFAULT '',
                        dallalName TEXT NOT NULL DEFAULT '',
                        dallalPhone TEXT NOT NULL DEFAULT '',
                        dallalId TEXT NOT NULL DEFAULT '',
                        farmerName TEXT NOT NULL DEFAULT '',
                        farmerPhone TEXT NOT NULL DEFAULT '',
                        hideFarmerPhone INTEGER NOT NULL DEFAULT 1,
                        status TEXT NOT NULL DEFAULT 'AVAILABLE',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        isFeatured INTEGER NOT NULL DEFAULT 0,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS settlement_deals (
                        id TEXT NOT NULL PRIMARY KEY,
                        dealNumber TEXT NOT NULL,
                        cropTitle TEXT NOT NULL,
                        cropType TEXT NOT NULL,
                        location TEXT NOT NULL DEFAULT '',
                        sellerName TEXT NOT NULL,
                        sellerPhone TEXT NOT NULL DEFAULT '',
                        buyerName TEXT NOT NULL,
                        buyerPhone TEXT NOT NULL DEFAULT '',
                        dallalName TEXT NOT NULL DEFAULT '',
                        dallalPhone TEXT NOT NULL DEFAULT '',
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        advancePayment REAL NOT NULL DEFAULT 0.0,
                        dallalCommission REAL NOT NULL DEFAULT 0.0,
                        commissionPaid REAL NOT NULL DEFAULT 0.0,
                        remainingAmount REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        dealDate INTEGER NOT NULL DEFAULT 0,
                        dueDate INTEGER DEFAULT NULL,
                        termsNotes TEXT NOT NULL DEFAULT '',
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deal_payments (
                        id TEXT NOT NULL PRIMARY KEY,
                        dealId TEXT NOT NULL,
                        amount REAL NOT NULL DEFAULT 0.0,
                        paidBy TEXT NOT NULL DEFAULT 'BUYER',
                        paymentType TEXT NOT NULL DEFAULT 'INSTALLMENT',
                        notes TEXT NOT NULL DEFAULT '',
                        paymentDate INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_distributor_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
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
