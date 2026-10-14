package com.example.core.database

import android.content.Context
import com.example.BuildConfig
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

/**
 * Seeder to generate 100 realistic customers and 1000 operations in Debug mode.
 * Strictly forbidden and disabled in Release builds.
 */
object DebugDataSeeder {

    private val FIRST_NAMES = listOf(
        "صالح", "عبد الله", "محمد", "علي", "أحمد", "يحيى", "حسين", "إبراهيم",
        "سالم", "عمر", "منصور", "خالد", "عادل", "ياسر", "طارق", "فؤاد",
        "رمزي", "وليد", "فيصل", "مجيب", "عصام", "هائل", "عبد الرقيب", "قائد",
        "مقبل", "ناجي", "زيد", "عبده", "سعيد", "صابر", "جميل", "توفيق"
    )

    private val MIDDLE_NAMES = listOf(
        "بن علي", "بن ناصر", "بن محمد", "بن أحمد", "بن سالم", "بن حسن",
        "بن قاسم", "بن عبده", "بن مسعد", "بن سعيد", "بن حسين", "بن منصور"
    )

    private val SURNAMES = listOf(
        "العولقي", "الحميري", "الحميلي", "السالمي", "الجبلي", "الريمي",
        "الزبيدي", "الكندي", "اليافعي", "الحبيشي", "الشميري", "التهامي",
        "الأهدل", "الصبيحي", "البركاني", "المخلافي", "الشوافي", "الضبياني",
        "الردفاني", "الأنسي", "القحوم", "السيد", "الهدار", "الشيباني",
        "السنحاني", "الخولاني", "الهمداني", "الوصابي", "الجعدي", "المشرقي"
    )

    private val FARMS = listOf(
        "مزرعة وادي سهام", "حقل الجربة الكبرى", "مزرعة الجرادين", "بستان العنب والنخيل",
        "مزرعة باجل الشرقية", "حقل العرج", "مزرعة وادي مور", "بستان الضالع",
        "مزرعة العواذل", "حقل الخضراء", "مزرعة الساحل", "بستان الخير والبركة",
        "مزرعة الروضة", "حقل وادي بنا", "مزرعة سهول تهامة", "بستان الموز والمانجو"
    )

    private val LOCATIONS = listOf(
        "الحديدة - باجل", "ذمار - رداع", "لحج - تبن", "أبين - دلتا",
        "صنعاء - بني حشيش", "حجة - عبس", "إب - السحول", "مأرب - الوادي",
        "تعز - الشمايتين", "حضرموت - القطن", "عمران - خارف", "المحويت - شبام"
    )

    private val PUMPS = listOf(
        "مضخة السلام 1", "مضخة البركة الكبرى", "مضخة بئر الجربة", "مضخة الوادي 3"
    )

    private val EXPENSE_CATEGORIES = listOf(
        "شراء ديزل لتشغيل البئر", "تغيير زيت المحرك وفلاتر", "صيانة وإصلاح أنابيب السقي",
        "أجرة فني كهرباء المضخة", "شراء بطارية وقطع غيار", "مصاريف نقل وتوصيل ديزل"
    )

    suspend fun seedDebugData(context: Context, db: AppDatabase): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            return@withContext Result.failure(IllegalStateException("توليد البيانات متاح فقط في نسخة الديباج (Debug Mode)"))
        }

        try {
            val random = Random(42) // Fixed seed for reproducible realistic generation
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 3600L * 1000L

            // 1. Generate 100 Customers (15 well owners, 85 farmers)
            val customersToInsert = mutableListOf<Customer>()
            val phonesGenerated = mutableSetOf<String>()

            for (i in 1..100) {
                val fName = FIRST_NAMES[i % FIRST_NAMES.size]
                val mName = MIDDLE_NAMES[(i / 2) % MIDDLE_NAMES.size]
                val sName = SURNAMES[(i + i / 3) % SURNAMES.size]
                val fullName = "$fName $mName $sName"

                val prefix = listOf("77", "73", "71", "70")[i % 4]
                var phone = "$prefix${1000000 + random.nextInt(8999999)}"
                while (phonesGenerated.contains(phone)) {
                    phone = "$prefix${1000000 + random.nextInt(8999999)}"
                }
                phonesGenerated.add(phone)

                val isOwner = i <= 15
                val customPrice = if (isOwner || i % 7 == 0) (3500 + (random.nextInt(4) * 500)).toDouble() else null
                val farm = FARMS[i % FARMS.size]
                val loc = LOCATIONS[i % LOCATIONS.size]

                customersToInsert.add(
                    Customer(
                        id = 0,
                        name = fullName,
                        phone = phone,
                        farmName = farm,
                        location = loc,
                        customPricePerHour = customPrice,
                        isWellOwner = isOwner,
                        notes = if (isOwner) "مالك بئر ومزود معتمد" else "عميل سقي منتظم",
                        isArchived = false,
                        createdAt = now - (90L * dayMillis) + (i * 3600000L)
                    )
                )
            }

            val insertedCustomerIds = db.customerDao().insertCustomers(customersToInsert)
            val wellOwnerIds = insertedCustomerIds.take(15)
            val farmerIds = insertedCustomerIds.drop(15)

            // 2. Generate Operations (Total 1000: 650 Sessions, 250 Vouchers, 100 WellOwnerPurchases)
            val sessionsToInsert = mutableListOf<WaterSession>()
            val vouchersToInsert = mutableListOf<Voucher>()
            val purchasesToInsert = mutableListOf<WellOwnerPurchase>()

            // 2.A: 650 Water Sessions
            for (i in 1..650) {
                val custId = insertedCustomerIds[random.nextInt(insertedCustomerIds.size)]
                val daysAgo = random.nextInt(90)
                val sessionStart = now - (daysAgo * dayMillis) + (random.nextInt(12) * 3600000L)
                val durationMins = (60 + random.nextInt(7) * 30) // 60 to 240 mins
                val sessionEnd = sessionStart + (durationMins * 60000L)

                val price = (3500 + random.nextInt(3) * 500).toDouble()
                val grossCost = Formatters.roundMoney((durationMins.toDouble() / 60.0) * price)

                val hasWaste = i % 8 == 0
                val wastedMins = if (hasWaste) (15 + random.nextInt(3) * 10) else 0
                val wastedReason = if (hasWaste) listOf("عطل مضخة مؤقت", "تبديل زيت", "انقطاع كهرباء")[random.nextInt(3)] else ""

                val hasDiscount = i % 10 == 0
                val discount = if (hasDiscount) (100 + random.nextInt(5) * 50).toDouble() else 0.0
                val finalCost = Formatters.roundMoney(maxOf(0.0, grossCost - discount))

                // Payment distribution: 55% fully paid, 25% partially paid, 20% unpaid
                val payScenario = i % 10
                val paid = when {
                    payScenario < 5 -> finalCost // Fully paid
                    payScenario in 5..7 -> Formatters.roundMoney(finalCost * (0.3 + random.nextDouble() * 0.5)) // Partial
                    else -> 0.0 // Credit
                }
                val debt = Formatters.roundMoney(maxOf(0.0, finalCost - paid))

                val pumpSourceId = wellOwnerIds[random.nextInt(wellOwnerIds.size)]
                val costRate = 2500.0

                sessionsToInsert.add(
                    WaterSession(
                        id = 0,
                        customerId = custId,
                        pumpName = PUMPS[random.nextInt(PUMPS.size)],
                        startTime = sessionStart,
                        endTime = sessionEnd,
                        durationMinutes = durationMins,
                        pricePerHour = price,
                        totalAmount = finalCost,
                        amountPaid = paid,
                        remainingDebt = debt,
                        notes = "جلسة سقي دورية #${i}",
                        isLive = false,
                        wastedMinutes = wastedMins,
                        wastedReason = wastedReason,
                        discountAmount = discount,
                        costPricePerHour = costRate,
                        pumpSourceId = pumpSourceId,
                        createdAt = sessionStart
                    )
                )
            }

            val insertedSessionIds = db.waterSessionDao().insertSessions(sessionsToInsert)

            // 2.B: 250 Vouchers (180 Receipts, 50 Expenses, 20 Settlement Discounts)
            for (i in 1..180) {
                val custId = insertedCustomerIds[random.nextInt(insertedCustomerIds.size)]
                val daysAgo = random.nextInt(90)
                val vDate = now - (daysAgo * dayMillis)
                val amt = (5000 + random.nextInt(20) * 1000).toDouble()
                val method = listOf("نقداً", "حوالة الكريمي", "شبكة بنكية", "شيك")[random.nextInt(4)]
                val linkedSessionId = if (i % 3 == 0 && insertedSessionIds.isNotEmpty()) {
                    insertedSessionIds[random.nextInt(insertedSessionIds.size)]
                } else null

                vouchersToInsert.add(
                    Voucher(
                        id = 0,
                        voucherNumber = "REC-${vDate.toString().takeLast(6)}-$i",
                        type = VoucherType.RECEIPT,
                        customerId = custId,
                        sessionId = linkedSessionId,
                        amount = amt,
                        category = if (linkedSessionId != null) "سداد سقي" else "دفعة على الحساب",
                        paymentMethod = method,
                        date = vDate,
                        notes = "سند تحصيل نقدي دوري #$i",
                        createdAt = vDate
                    )
                )
            }

            for (i in 1..50) {
                val daysAgo = random.nextInt(90)
                val vDate = now - (daysAgo * dayMillis)
                val amt = (3000 + random.nextInt(15) * 2000).toDouble()
                val cat = EXPENSE_CATEGORIES[random.nextInt(EXPENSE_CATEGORIES.size)]

                vouchersToInsert.add(
                    Voucher(
                        id = 0,
                        voucherNumber = "EXP-${vDate.toString().takeLast(6)}-$i",
                        type = VoucherType.EXPENSE,
                        customerId = null,
                        sessionId = null,
                        amount = amt,
                        category = cat,
                        paymentMethod = "نقداً",
                        date = vDate,
                        notes = "مصروف تشغيلي: $cat",
                        createdAt = vDate
                    )
                )
            }

            for (i in 1..20) {
                val custId = insertedCustomerIds[random.nextInt(insertedCustomerIds.size)]
                val daysAgo = random.nextInt(90)
                val vDate = now - (daysAgo * dayMillis)
                val discountAmt = (200 + random.nextInt(10) * 100).toDouble()

                vouchersToInsert.add(
                    Voucher(
                        id = 0,
                        voucherNumber = "DSC-${vDate.toString().takeLast(6)}-$i",
                        type = VoucherType.DISCOUNT,
                        customerId = custId,
                        sessionId = null,
                        amount = discountAmt,
                        category = "خصم تسوية",
                        paymentMethod = "تسوية ومسامحة",
                        date = vDate,
                        notes = "خصم تسوية ومسامحة لتصفير حساب العميل",
                        createdAt = vDate
                    )
                )
            }

            db.voucherDao().insertVouchers(vouchersToInsert)

            // 2.C: 100 Well Owner Purchases
            for (i in 1..100) {
                val ownerId = wellOwnerIds[random.nextInt(wellOwnerIds.size)]
                val daysAgo = random.nextInt(90)
                val pDate = now - (daysAgo * dayMillis)
                val durationMins = (120 + random.nextInt(6) * 60) // 2 to 7 hours
                val wasteMins = if (i % 5 == 0) 30 else 0
                val rate = (2500 + random.nextInt(3) * 500).toDouble()
                val grossAmt = Formatters.roundMoney((durationMins.toDouble() / 60.0) * rate)
                val paid = if (i % 2 == 0) grossAmt else Formatters.roundMoney(grossAmt * 0.5)

                purchasesToInsert.add(
                    WellOwnerPurchase(
                        id = 0,
                        ownerCustomerId = ownerId,
                        date = pDate,
                        durationMinutes = durationMins,
                        wastedMinutesOnOwner = wasteMins,
                        purchaseRatePerHour = rate,
                        amountPaid = paid,
                        notes = "شراء ساعات ماء لتغذية الشبكة #${i}",
                        createdAt = pDate
                    )
                )
            }

            db.wellOwnerPurchaseDao().insertPurchases(purchasesToInsert)

            context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("debug_unlimited_ops", true)
                .apply()

            val totalOps = sessionsToInsert.size + vouchersToInsert.size + purchasesToInsert.size
            Result.success(Pair(customersToInsert.size, totalOps))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllDebugData(context: Context, db: AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            return@withContext Result.failure(IllegalStateException("مسح البيانات متاح فقط في نسخة الديباج"))
        }
        try {
            db.waterSessionDao().clearAllSessions()
            db.voucherDao().clearAllVouchers()
            db.wellOwnerPurchaseDao().clearAllPurchases()
            db.customerDao().clearAllCustomers()
            context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("debug_unlimited_ops")
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
