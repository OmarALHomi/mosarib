package com.baynana.core.util

import com.baynana.core.database.AppDatabase
import com.baynana.features.customers.Customer
import com.baynana.features.deals.DealPayment
import com.baynana.features.deals.SettlementDeal
import com.baynana.features.farmer.FarmExpense
import com.baynana.features.farmer.LinkedMusrib
import com.baynana.features.market.CropListing
import com.baynana.features.pumps.PumpSource
import com.baynana.features.sessions.WaterSession
import com.baynana.features.settings.AppSetting
import com.baynana.features.vouchers.Voucher
import com.baynana.features.vouchers.VoucherType
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * محرّك النسخة الاحتياطية الكاملة: يقرأ الجداول العشرة، يتحقق من الملف قبل أي كتابة،
 * ويكتب داخل معاملة واحدة. لا يُستخدم الترحيل الاحتياطي المدمّر ولا الكتابة الجزئية.
 *
 * قواعد ثابتة (الخطة v4 §9 و§14):
 * - النسخة تُعلن عن نفسها بصراحة: `formatVersion` و`dbVersion` و`tables`. لا تسمية «كاملة»
 *   لملف قديم ناقص.
 * - ملف قديم (صيغة 1) يُقرأ لجداوله الخمسة مع تحذير واضح أنه ناقص، ولا يُمنع الاسترجاع.
 * - ملف تالف أو من نسخة أحدث أو بمعرّفات مكررة أو مبالغ غير صالحة أو روابط مفقودة:
 *   يُرفض قبل اعتماد أي تغيير، والمعاملة تُرجع كل شيء كما كان.
 */
object BackupSnapshot {

    /** صيغة الملف الحالية. أي زيادة لاحقة ترفع هذا الرقم ولا تكسر قراءة الأقدم. */
    const val FORMAT_VERSION = 2

    const val KEY_CUSTOMERS = "customers"
    const val KEY_SESSIONS = "sessions"
    const val KEY_VOUCHERS = "vouchers"
    const val KEY_PUMPS = "pumps"
    const val KEY_SETTINGS = "settings"
    const val KEY_LINKED_MUSRIBS = "linkedMusribs"
    const val KEY_CROP_LISTINGS = "cropListings"
    const val KEY_SETTLEMENT_DEALS = "settlementDeals"
    const val KEY_DEAL_PAYMENTS = "dealPayments"
    const val KEY_FARM_EXPENSES = "farmExpenses"

    /** ترتيب الجداول محفوظ: الأب قبل الابن حتى لا يفشل الإدخال على المفاتيح الأجنبية. */
    val TABLE_ORDER = listOf(
        KEY_CUSTOMERS,
        KEY_PUMPS,
        KEY_SETTINGS,
        KEY_LINKED_MUSRIBS,
        KEY_SESSIONS,
        KEY_VOUCHERS,
        KEY_CROP_LISTINGS,
        KEY_SETTLEMENT_DEALS,
        KEY_DEAL_PAYMENTS,
        KEY_FARM_EXPENSES
    )

    /** الجداول التي كانت تُنسخ في الصيغة الأولى. ما عداها لم يكن يُنسخ إطلاقًا. */
    val LEGACY_TABLES = listOf(KEY_CUSTOMERS, KEY_SESSIONS, KEY_VOUCHERS, KEY_PUMPS, KEY_SETTINGS)

    val TABLE_LABELS = mapOf(
        KEY_CUSTOMERS to "العملاء والمستفيدون",
        KEY_SESSIONS to "جلسات الري",
        KEY_VOUCHERS to "السندات",
        KEY_PUMPS to "مصادر المياه",
        KEY_SETTINGS to "الإعدادات",
        KEY_LINKED_MUSRIBS to "روابط المسربين والمزارعين",
        KEY_CROP_LISTINGS to "عروض السوق",
        KEY_SETTLEMENT_DEALS to "الصلوح",
        KEY_DEAL_PAYMENTS to "دفعات الصلوح",
        KEY_FARM_EXPENSES to "مصروفات المزرعة"
    )

    /** كل صفوف القاعدة، مقروءة بلا أي تصفية. */
    data class SnapshotSource(
        val customers: List<Customer>,
        val sessions: List<WaterSession>,
        val vouchers: List<Voucher>,
        val pumps: List<PumpSource>,
        val settings: List<AppSetting>,
        val linkedMusribs: List<LinkedMusrib>,
        val cropListings: List<CropListing>,
        val settlementDeals: List<SettlementDeal>,
        val dealPayments: List<DealPayment>,
        val farmExpenses: List<FarmExpense>
    ) {
        fun countOf(tableKey: String): Int = when (tableKey) {
            KEY_CUSTOMERS -> customers.size
            KEY_SESSIONS -> sessions.size
            KEY_VOUCHERS -> vouchers.size
            KEY_PUMPS -> pumps.size
            KEY_SETTINGS -> settings.size
            KEY_LINKED_MUSRIBS -> linkedMusribs.size
            KEY_CROP_LISTINGS -> cropListings.size
            KEY_SETTLEMENT_DEALS -> settlementDeals.size
            KEY_DEAL_PAYMENTS -> dealPayments.size
            KEY_FARM_EXPENSES -> farmExpenses.size
            else -> 0
        }
    }

    data class TableReport(
        val tableKey: String,
        val label: String,
        val rowsInFile: Int,
        val rowsInDatabase: Int,
        /** صفوف موجودة مسبقًا بنفس المفتاح وستُستبدل عند الاسترجاع. */
        val rowsToOverwrite: Int
    )

    data class RestorePlan(
        val formatVersion: Int,
        val dbVersion: Int,
        val isComplete: Boolean,
        val tables: Map<String, JSONArray>,
        val reports: List<TableReport>,
        val warnings: List<String>,
        val errors: List<String>
    ) {
        val canRestore: Boolean get() = errors.isEmpty()

        val totalRowsInFile: Int get() = reports.sumOf { it.rowsInFile }
        val totalRowsToOverwrite: Int get() = reports.sumOf { it.rowsToOverwrite }

        /** ملخص عربي جاهز للعرض في شاشة المعاينة. */
        fun summaryText(): String = buildString {
            if (!canRestore) {
                appendLine("لا يمكن الاسترجاع:")
                errors.forEach { appendLine("- $it") }
                return@buildString
            }
            appendLine(if (isComplete) "نسخة كاملة من الجداول العشرة." else "نسخة قديمة ناقصة: تحتوي ${tables.keys.count { it in LEGACY_TABLES }} من 10 جداول.")
            appendLine()
            reports.filter { it.rowsInFile > 0 }.forEach { report ->
                val overwrite = if (report.rowsToOverwrite > 0) " (سيُستبدل ${report.rowsToOverwrite} موجود)" else ""
                appendLine("- ${report.label}: ${report.rowsInFile}$overwrite")
            }
            if (warnings.isNotEmpty()) {
                appendLine()
                warnings.forEach { appendLine("تنبيه: $it") }
            }
        }
    }

    suspend fun readAll(database: AppDatabase): SnapshotSource = SnapshotSource(
        customers = database.customerDao().getAllCustomersForBackup(),
        sessions = database.waterSessionDao().getAllSessions().first(),
        vouchers = database.voucherDao().getAllVouchers().first(),
        pumps = database.pumpSourceDao().getAllPumpsForBackup(),
        settings = database.appSettingDao().getAllSettings().first(),
        linkedMusribs = database.linkedMusribDao().getAllLinkedMusribsForBackup(),
        cropListings = database.cropListingDao().getAllListingsForBackup(),
        settlementDeals = database.settlementDealDao().getAllDealsForBackup(),
        dealPayments = database.settlementDealDao().getAllPaymentsForBackup(),
        farmExpenses = database.farmExpenseDao().getAllExpensesForBackup()
    )

    // ---------------------------------------------------------------- الكتابة

    fun toJson(source: SnapshotSource): JSONObject {
        val root = JSONObject()
        root.put("app", "Baynana")
        root.put("appName", "بيننا")
        root.put("legacyApp", "Mosarib")
        root.put("formatVersion", FORMAT_VERSION)
        root.put("dbVersion", CURRENT_DB_VERSION)
        root.put("createdAt", System.currentTimeMillis())
        root.put("formattedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("complete", true)

        val tables = JSONObject()
        TABLE_ORDER.forEach { tableKey ->
            val array = when (tableKey) {
                KEY_CUSTOMERS -> customersArray(source.customers)
                KEY_SESSIONS -> sessionsArray(source.sessions)
                KEY_VOUCHERS -> vouchersArray(source.vouchers)
                KEY_PUMPS -> pumpsArray(source.pumps)
                KEY_SETTINGS -> settingsArray(source.settings)
                KEY_LINKED_MUSRIBS -> linkedMusribsArray(source.linkedMusribs)
                KEY_CROP_LISTINGS -> cropListingsArray(source.cropListings)
                KEY_SETTLEMENT_DEALS -> dealsArray(source.settlementDeals)
                KEY_DEAL_PAYMENTS -> paymentsArray(source.dealPayments)
                KEY_FARM_EXPENSES -> farmExpensesArray(source.farmExpenses)
                else -> JSONArray()
            }
            tables.put(tableKey, array)
        }
        root.put("tables", tables)
        root.put("tableCounts", countsObject(source))
        return root
    }

    private fun countsObject(source: SnapshotSource): JSONObject = JSONObject().apply {
        TABLE_ORDER.forEach { put(it, source.countOf(it)) }
    }

    private fun customersArray(rows: List<Customer>) = JSONArray().apply {
        rows.forEach { c ->
            put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("farmName", c.farmName)
                put("location", c.location)
                put("notes", c.notes)
                if (c.customPricePerHour != null) put("customPricePerHour", c.customPricePerHour) else put("customPricePerHour", JSONObject.NULL)
                put("isBeneficiary", c.isBeneficiary)
                put("createdAt", c.createdAt)
                put("isArchived", c.isArchived)
                put("linkCode", c.linkCode)
            })
        }
    }

    private fun sessionsArray(rows: List<WaterSession>) = JSONArray().apply {
        rows.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("customerId", s.customerId)
                put("pumpName", s.pumpName)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("durationMinutes", s.durationMinutes)
                put("pricePerHour", s.pricePerHour)
                put("totalAmount", s.totalAmount)
                put("amountPaid", s.amountPaid)
                put("remainingDebt", s.remainingDebt)
                put("notes", s.notes)
                put("isLive", s.isLive)
                if (s.billedToCustomerId != null) put("billedToCustomerId", s.billedToCustomerId) else put("billedToCustomerId", JSONObject.NULL)
                put("createdAt", s.createdAt)
            })
        }
    }

    private fun vouchersArray(rows: List<Voucher>) = JSONArray().apply {
        rows.forEach { v ->
            put(JSONObject().apply {
                put("id", v.id)
                put("voucherNumber", v.voucherNumber)
                put("type", v.type.name)
                if (v.customerId != null) put("customerId", v.customerId) else put("customerId", JSONObject.NULL)
                if (v.sessionId != null) put("sessionId", v.sessionId) else put("sessionId", JSONObject.NULL)
                put("amount", v.amount)
                put("category", v.category)
                put("paymentMethod", v.paymentMethod)
                put("date", v.date)
                put("notes", v.notes)
                put("createdAt", v.createdAt)
            })
        }
    }

    private fun pumpsArray(rows: List<PumpSource>) = JSONArray().apply {
        rows.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("locationOrWellNumber", p.locationOrWellNumber)
                put("defaultPricePerHour", p.defaultPricePerHour)
                put("powerType", p.powerType)
                put("notes", p.notes)
                put("isPrimary", p.isPrimary)
                put("isActive", p.isActive)
            })
        }
    }

    private fun settingsArray(rows: List<AppSetting>) = JSONArray().apply {
        rows.forEach { s -> put(JSONObject().apply { put("key", s.key); put("value", s.value) }) }
    }

    private fun linkedMusribsArray(rows: List<LinkedMusrib>) = JSONArray().apply {
        rows.forEach { m ->
            put(JSONObject().apply {
                put("linkCode", m.linkCode)
                put("musribName", m.musribName)
                put("musribPhone", m.musribPhone)
                put("farmName", m.farmName)
                put("currentBalance", m.currentBalance)
                put("totalDebit", m.totalDebit)
                put("totalPaid", m.totalPaid)
                put("lastSyncTimestamp", m.lastSyncTimestamp)
                put("addedAt", m.addedAt)
            })
        }
    }

    private fun cropListingsArray(rows: List<CropListing>) = JSONArray().apply {
        rows.forEach { l ->
            put(JSONObject().apply {
                put("id", l.id)
                put("title", l.title)
                put("cropType", l.cropType)
                put("description", l.description)
                put("district", l.district)
                put("village", l.village)
                put("priceEstimate", l.priceEstimate)
                put("priceUnit", l.priceUnit)
                put("dallalName", l.dallalName)
                put("dallalPhone", l.dallalPhone)
                put("dallalId", l.dallalId)
                put("farmerName", l.farmerName)
                put("farmerPhone", l.farmerPhone)
                put("hideFarmerPhone", l.hideFarmerPhone)
                put("status", l.status)
                put("createdAt", l.createdAt)
                put("isFeatured", l.isFeatured)
                put("syncStatus", l.syncStatus)
            })
        }
    }

    private fun dealsArray(rows: List<SettlementDeal>) = JSONArray().apply {
        rows.forEach { d ->
            put(JSONObject().apply {
                put("id", d.id)
                put("dealNumber", d.dealNumber)
                put("cropTitle", d.cropTitle)
                put("cropType", d.cropType)
                put("location", d.location)
                put("sellerName", d.sellerName)
                put("sellerPhone", d.sellerPhone)
                put("buyerName", d.buyerName)
                put("buyerPhone", d.buyerPhone)
                put("dallalName", d.dallalName)
                put("dallalPhone", d.dallalPhone)
                put("totalAmount", d.totalAmount)
                put("advancePayment", d.advancePayment)
                put("dallalCommission", d.dallalCommission)
                put("commissionPaid", d.commissionPaid)
                put("remainingAmount", d.remainingAmount)
                put("status", d.status)
                put("dealDate", d.dealDate)
                if (d.dueDate != null) put("dueDate", d.dueDate) else put("dueDate", JSONObject.NULL)
                put("termsNotes", d.termsNotes)
                put("syncStatus", d.syncStatus)
            })
        }
    }

    private fun paymentsArray(rows: List<DealPayment>) = JSONArray().apply {
        rows.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("dealId", p.dealId)
                put("amount", p.amount)
                put("paidBy", p.paidBy)
                put("paymentType", p.paymentType)
                put("notes", p.notes)
                put("paymentDate", p.paymentDate)
            })
        }
    }

    private fun farmExpensesArray(rows: List<FarmExpense>) = JSONArray().apply {
        rows.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id)
                put("farmName", e.farmName)
                put("expenseCategory", e.expenseCategory)
                put("amount", e.amount)
                put("notes", e.notes)
                put("date", e.date)
            })
        }
    }

    // ---------------------------------------------------------------- القراءة

    /**
     * يتحقق من الملف ويبني خطة استرجاع بلا أي كتابة.
     *
     * @param current الجداول الموجودة حاليًا في القاعدة، تُستخدم لفحص الروابط وحساب المستبدَل.
     */
    fun plan(root: JSONObject, current: SnapshotSource?): RestorePlan {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val formatVersion = root.optInt("formatVersion", 1)
        val dbVersion = root.optInt("dbVersion", 1)

        if (formatVersion > FORMAT_VERSION) {
            errors += "الملف من نسخة أحدث من التطبيق (صيغة $formatVersion). حدِّث التطبيق ثم أعد المحاولة."
        }
        if (dbVersion > CURRENT_DB_VERSION) {
            errors += "الملف أُنشئ على قاعدة بيانات أحدث (إصدار $dbVersion). لا يمكن استرجاعه على هذا الإصدار."
        }

        val tablesObject = root.optJSONObject("tables")
        val tables = linkedMapOf<String, JSONArray>()
        if (tablesObject != null) {
            TABLE_ORDER.forEach { key ->
                tablesObject.optJSONArray(key)?.let { tables[key] = it }
            }
        } else {
            LEGACY_TABLES.forEach { key ->
                root.optJSONArray(key)?.let { tables[key] = it }
            }
        }

        val isComplete = tablesObject != null && TABLE_ORDER.all { tables.containsKey(it) }
        if (tablesObject == null) {
            warnings += "هذه نسخة قديمة: تحفظ ${tables.keys.size} جداول من 10 فقط. " +
                "الصلوح ودفعاتها وعروض السوق ومصروفات المزرعة وروابط المسربين غير مشمولة."
        } else if (!isComplete) {
            warnings += "الملف لا يذكر كل الجداول العشرة؛ ما لم يُذكر لن يُستبدل ولن يُحذف."
        }

        if (tables.isEmpty()) {
            errors += "الملف لا يحتوي أي بيانات معروفة."
        }

        // فحص المفاتيح المكرّرة داخل الملف لكل جدول.
        val duplicateCheck = mapOf(
            KEY_CUSTOMERS to "id",
            KEY_SESSIONS to "id",
            KEY_VOUCHERS to "id",
            KEY_PUMPS to "id",
            KEY_SETTINGS to "key",
            KEY_LINKED_MUSRIBS to "linkCode",
            KEY_CROP_LISTINGS to "id",
            KEY_SETTLEMENT_DEALS to "id",
            KEY_DEAL_PAYMENTS to "id",
            KEY_FARM_EXPENSES to "id"
        )
        duplicateCheck.forEach { (tableKey, idField) ->
            tables[tableKey]?.let { array ->
                val seen = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    val row = array.optJSONObject(i) ?: continue
                    val id = row.optString(idField, "")
                    if (id.isBlank()) {
                        errors += "سجل بلا معرّف في ${labelOf(tableKey)}."
                        continue
                    }
                    if (!seen.add(id)) {
                        errors += "معرّف مكرر في ${labelOf(tableKey)}: $id"
                    }
                }
            }
        }

        val fileCustomerIds = idsOf(tables[KEY_CUSTOMERS], "id") { it.optLong("id", 0) > 0 }
        val fileDealIds = idsOf(tables[KEY_SETTLEMENT_DEALS], "id") { it.optString("id", "").isNotBlank() }
        val knownCustomerIds = fileCustomerIds + (current?.customers?.map { it.id.toString() }?.toSet() ?: emptySet())
        val knownDealIds = fileDealIds + (current?.settlementDeals?.map { it.id }?.toSet() ?: emptySet())

        validateAmounts(tables, errors)
        validateRequiredText(tables, errors)
        validateSessions(tables, knownCustomerIds, errors)
        validateVouchers(tables, knownCustomerIds, warnings)
        validatePayments(tables, knownDealIds, errors)

        val reports = TABLE_ORDER.map { tableKey ->
            val array = tables[tableKey]
            val rowsInFile = array?.length() ?: 0
            val existing = existingKeysOf(current, tableKey)
            val overwrite = if (array == null || existing.isEmpty()) 0 else {
                var count = 0
                for (i in 0 until array.length()) {
                    val row = array.optJSONObject(i) ?: continue
                    if (existing.contains(keyOf(tableKey, row))) count++
                }
                count
            }
            TableReport(
                tableKey = tableKey,
                label = labelOf(tableKey),
                rowsInFile = rowsInFile,
                rowsInDatabase = current?.countOf(tableKey) ?: 0,
                rowsToOverwrite = overwrite
            )
        }

        return RestorePlan(
            formatVersion = formatVersion,
            dbVersion = dbVersion,
            isComplete = isComplete,
            tables = tables,
            reports = reports,
            warnings = warnings,
            errors = errors
        )
    }

    private fun labelOf(tableKey: String): String = TABLE_LABELS[tableKey] ?: tableKey

    private fun idsOf(array: JSONArray?, field: String, predicate: (JSONObject) -> Boolean): Set<String> {
        if (array == null) return emptySet()
        val result = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            if (predicate(row)) result.add(row.optString(field, ""))
        }
        return result
    }

    private fun existingKeysOf(current: SnapshotSource?, tableKey: String): Set<String> {
        if (current == null) return emptySet()
        return when (tableKey) {
            KEY_CUSTOMERS -> current.customers.map { it.id.toString() }.toSet()
            KEY_SESSIONS -> current.sessions.map { it.id.toString() }.toSet()
            KEY_VOUCHERS -> current.vouchers.map { it.id.toString() }.toSet()
            KEY_PUMPS -> current.pumps.map { it.id.toString() }.toSet()
            KEY_SETTINGS -> current.settings.map { it.key }.toSet()
            KEY_LINKED_MUSRIBS -> current.linkedMusribs.map { it.linkCode }.toSet()
            KEY_CROP_LISTINGS -> current.cropListings.map { it.id }.toSet()
            KEY_SETTLEMENT_DEALS -> current.settlementDeals.map { it.id }.toSet()
            KEY_DEAL_PAYMENTS -> current.dealPayments.map { it.id }.toSet()
            KEY_FARM_EXPENSES -> current.farmExpenses.map { it.id }.toSet()
            else -> emptySet()
        }
    }

    private fun keyOf(tableKey: String, row: JSONObject): String = when (tableKey) {
        KEY_SETTINGS -> row.optString("key", "")
        KEY_LINKED_MUSRIBS -> row.optString("linkCode", "")
        else -> row.optString("id", "")
    }

    /** حقول المبالغ في كل جدول. أي قيمة سالبة أو غير منتهية تُرفض. */
    private val AMOUNT_FIELDS = mapOf(
        KEY_CUSTOMERS to listOf("customPricePerHour"),
        KEY_SESSIONS to listOf("pricePerHour", "totalAmount", "amountPaid", "remainingDebt"),
        KEY_VOUCHERS to listOf("amount"),
        KEY_PUMPS to listOf("defaultPricePerHour"),
        KEY_LINKED_MUSRIBS to listOf("currentBalance", "totalDebit", "totalPaid"),
        KEY_CROP_LISTINGS to listOf("priceEstimate"),
        KEY_SETTLEMENT_DEALS to listOf("totalAmount", "advancePayment", "dallalCommission", "commissionPaid", "remainingAmount"),
        KEY_DEAL_PAYMENTS to listOf("amount"),
        KEY_FARM_EXPENSES to listOf("amount")
    )

    private fun validateAmounts(tables: Map<String, JSONArray>, errors: MutableList<String>) {
        for ((tableKey, fields) in AMOUNT_FIELDS) {
            val array = tables[tableKey] ?: continue
            for (i in 0 until array.length()) {
                val row = array.optJSONObject(i) ?: continue
                for (field in fields) {
                    if (!row.has(field) || row.isNull(field)) continue
                    val value = row.optDouble(field, Double.NaN)
                    when {
                        value.isNaN() -> errors += "قيمة غير رقمية في ${labelOf(tableKey)} ($field)."
                        !value.isFinite() -> errors += "قيمة غير منتهية في ${labelOf(tableKey)} ($field)."
                        value < 0 -> errors += "قيمة سالبة في ${labelOf(tableKey)} ($field): $value"
                    }
                }
            }
        }
    }

    /** الحقول النصية الأساسية التي لا يجوز أن تكون فارغة، وإلا فسد السجل بعد الكتابة. */
    private val REQUIRED_TEXT_FIELDS = mapOf(
        KEY_CUSTOMERS to listOf("name"),
        KEY_SESSIONS to listOf("customerId"),
        KEY_PUMPS to listOf("name"),
        KEY_CROP_LISTINGS to listOf("title"),
        KEY_SETTLEMENT_DEALS to listOf("dealNumber"),
        KEY_DEAL_PAYMENTS to listOf("dealId"),
        KEY_FARM_EXPENSES to listOf("farmName")
    )

    private fun validateRequiredText(tables: Map<String, JSONArray>, errors: MutableList<String>) {
        for ((tableKey, fields) in REQUIRED_TEXT_FIELDS) {
            val array = tables[tableKey] ?: continue
            for (i in 0 until array.length()) {
                val row = array.optJSONObject(i) ?: continue
                for (field in fields) {
                    if (row.optString(field, "").isBlank()) {
                        errors += "حقل ناقص في ${labelOf(tableKey)}: $field"
                    }
                }
            }
        }
    }

    private fun validateSessions(tables: Map<String, JSONArray>, knownCustomerIds: Set<String>, errors: MutableList<String>) {
        val array = tables[KEY_SESSIONS] ?: return
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            val customerId = row.optLong("customerId", -1L)
            if (customerId <= 0 || !knownCustomerIds.contains(customerId.toString())) {
                errors += "سقية رقم ${row.optLong("id", 0)} تشير إلى عميل غير موجود ($customerId)."
            }
            val billed = row.optLong("billedToCustomerId", 0L)
            if (row.has("billedToCustomerId") && !row.isNull("billedToCustomerId") && !knownCustomerIds.contains(billed.toString())) {
                errors += "سقية رقم ${row.optLong("id", 0)} محسوبة على حساب مستفيد غير موجود ($billed)."
            }
        }
    }

    private fun validateVouchers(tables: Map<String, JSONArray>, knownCustomerIds: Set<String>, warnings: MutableList<String>) {
        val array = tables[KEY_VOUCHERS] ?: return
        var orphaned = 0
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            if (!row.has("customerId") || row.isNull("customerId")) continue
            val customerId = row.optLong("customerId", -1L)
            if (!knownCustomerIds.contains(customerId.toString())) orphaned++
        }
        if (orphaned > 0) {
            warnings += "$orphaned سندًا مرتبطًا بعميل غير موجود؛ ستُسترجع بلا ربط بدل أن تُرفض أو تُحذف."
        }
    }

    private fun validatePayments(tables: Map<String, JSONArray>, knownDealIds: Set<String>, errors: MutableList<String>) {
        val array = tables[KEY_DEAL_PAYMENTS] ?: return
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            val dealId = row.optString("dealId", "")
            if (dealId.isBlank() || !knownDealIds.contains(dealId)) {
                errors += "دفعة رقم ${row.optString("id", "")} تشير إلى صلح غير موجود ($dealId)."
            }
        }
    }

    // ------------------------------------------------------------- الاسترجاع

    /**
     * يكتب خطة مُتحققًا منها داخل معاملة واحدة. تُستدعى فقط بعد [plan] ونجاحها.
     * أي خطأ أثناء الكتابة يُلغي كل شيء (Room.withTransaction).
     */
    suspend fun apply(database: AppDatabase, plan: RestorePlan): Int {
        check(plan.canRestore) { "لا يمكن تطبيق خطة فيها أخطاء" }
        var count = 0

        plan.tables[KEY_CUSTOMERS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.customerDao().insertCustomer(
                    Customer(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        farmName = o.optString("farmName", ""),
                        location = o.optString("location", ""),
                        notes = o.optString("notes", ""),
                        customPricePerHour = if (o.isNull("customPricePerHour")) null else o.getDouble("customPricePerHour"),
                        isBeneficiary = o.optBoolean("isBeneficiary", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        isArchived = o.optBoolean("isArchived", false),
                        linkCode = o.optString("linkCode", "")
                    )
                )
                count++
            }
        }

        plan.tables[KEY_PUMPS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.pumpSourceDao().insertPump(
                    PumpSource(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        locationOrWellNumber = o.optString("locationOrWellNumber", ""),
                        defaultPricePerHour = o.optDouble("defaultPricePerHour", 0.0),
                        powerType = o.optString("powerType", "ديزل"),
                        notes = o.optString("notes", ""),
                        isPrimary = o.optBoolean("isPrimary", false),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
                count++
            }
        }

        plan.tables[KEY_SETTINGS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.appSettingDao().saveSetting(AppSetting(o.getString("key"), o.optString("value", "")))
                count++
            }
        }

        plan.tables[KEY_LINKED_MUSRIBS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.linkedMusribDao().insert(
                    LinkedMusrib(
                        linkCode = o.getString("linkCode"),
                        musribName = o.optString("musribName", ""),
                        musribPhone = o.optString("musribPhone", ""),
                        farmName = o.optString("farmName", ""),
                        currentBalance = o.optDouble("currentBalance", 0.0),
                        totalDebit = o.optDouble("totalDebit", 0.0),
                        totalPaid = o.optDouble("totalPaid", 0.0),
                        lastSyncTimestamp = o.optLong("lastSyncTimestamp", 0L),
                        addedAt = o.optLong("addedAt", System.currentTimeMillis())
                    )
                )
                count++
            }
        }

        plan.tables[KEY_SESSIONS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.waterSessionDao().insertSession(
                    WaterSession(
                        id = o.getLong("id"),
                        customerId = o.getLong("customerId"),
                        pumpName = o.optString("pumpName", ""),
                        startTime = o.optLong("startTime", System.currentTimeMillis()),
                        endTime = o.optLong("endTime", System.currentTimeMillis()),
                        durationMinutes = o.optInt("durationMinutes", 0),
                        pricePerHour = o.optDouble("pricePerHour", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        amountPaid = o.optDouble("amountPaid", 0.0),
                        remainingDebt = o.optDouble("remainingDebt", 0.0),
                        notes = o.optString("notes", ""),
                        isLive = o.optBoolean("isLive", false),
                        billedToCustomerId = if (o.isNull("billedToCustomerId")) null else o.getLong("billedToCustomerId"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                count++
            }
        }

        plan.tables[KEY_VOUCHERS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val customerId = if (o.isNull("customerId")) null else o.getLong("customerId")
                database.voucherDao().insertVoucher(
                    Voucher(
                        id = o.getLong("id"),
                        voucherNumber = o.optString("voucherNumber", ""),
                        type = runCatching { VoucherType.valueOf(o.optString("type", "RECEIPT")) }.getOrDefault(VoucherType.RECEIPT),
                        customerId = customerId,
                        sessionId = if (o.isNull("sessionId")) null else o.getLong("sessionId"),
                        amount = o.optDouble("amount", 0.0),
                        category = o.optString("category", "عام"),
                        paymentMethod = o.optString("paymentMethod", "نقداً"),
                        date = o.optLong("date", System.currentTimeMillis()),
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                count++
            }
        }

        plan.tables[KEY_CROP_LISTINGS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.cropListingDao().insertListing(
                    CropListing(
                        id = o.getString("id"),
                        title = o.optString("title", ""),
                        cropType = o.optString("cropType", ""),
                        description = o.optString("description", ""),
                        district = o.optString("district", ""),
                        village = o.optString("village", ""),
                        priceEstimate = o.optDouble("priceEstimate", 0.0),
                        priceUnit = o.optString("priceUnit", "شروة كاملة"),
                        dallalName = o.optString("dallalName", ""),
                        dallalPhone = o.optString("dallalPhone", ""),
                        dallalId = o.optString("dallalId", ""),
                        farmerName = o.optString("farmerName", ""),
                        farmerPhone = o.optString("farmerPhone", ""),
                        hideFarmerPhone = o.optBoolean("hideFarmerPhone", true),
                        status = o.optString("status", "AVAILABLE"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        isFeatured = o.optBoolean("isFeatured", false),
                        syncStatus = o.optString("syncStatus", "SYNCED")
                    )
                )
                count++
            }
        }

        plan.tables[KEY_SETTLEMENT_DEALS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.settlementDealDao().insertDeal(
                    SettlementDeal(
                        id = o.getString("id"),
                        dealNumber = o.optString("dealNumber", ""),
                        cropTitle = o.optString("cropTitle", ""),
                        cropType = o.optString("cropType", ""),
                        location = o.optString("location", ""),
                        sellerName = o.optString("sellerName", ""),
                        sellerPhone = o.optString("sellerPhone", ""),
                        buyerName = o.optString("buyerName", ""),
                        buyerPhone = o.optString("buyerPhone", ""),
                        dallalName = o.optString("dallalName", ""),
                        dallalPhone = o.optString("dallalPhone", ""),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        advancePayment = o.optDouble("advancePayment", 0.0),
                        dallalCommission = o.optDouble("dallalCommission", 0.0),
                        commissionPaid = o.optDouble("commissionPaid", 0.0),
                        remainingAmount = o.optDouble("remainingAmount", 0.0),
                        status = o.optString("status", "ACTIVE"),
                        dealDate = o.optLong("dealDate", System.currentTimeMillis()),
                        dueDate = if (o.isNull("dueDate")) null else o.getLong("dueDate"),
                        termsNotes = o.optString("termsNotes", ""),
                        syncStatus = o.optString("syncStatus", "SYNCED")
                    )
                )
                count++
            }
        }

        plan.tables[KEY_DEAL_PAYMENTS]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.settlementDealDao().insertPayment(
                    DealPayment(
                        id = o.getString("id"),
                        dealId = o.getString("dealId"),
                        amount = o.optDouble("amount", 0.0),
                        paidBy = o.optString("paidBy", "BUYER"),
                        paymentType = o.optString("paymentType", "INSTALLMENT"),
                        notes = o.optString("notes", ""),
                        paymentDate = o.optLong("paymentDate", System.currentTimeMillis())
                    )
                )
                count++
            }
        }

        plan.tables[KEY_FARM_EXPENSES]?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                database.farmExpenseDao().insertExpense(
                    FarmExpense(
                        id = o.getString("id"),
                        farmName = o.optString("farmName", ""),
                        expenseCategory = o.optString("expenseCategory", ""),
                        amount = o.optDouble("amount", 0.0),
                        notes = o.optString("notes", ""),
                        date = o.optLong("date", System.currentTimeMillis())
                    )
                )
                count++
            }
        }

        return count
    }

    /**
     * إصدار قاعدة البيانات المتوافق مع هذا الملف. يُقرأ من الثابت نفسه الذي يستخدمه تعليق
     * Room، لا بالانعكاس: تعليقات Room ليست محفوظة وقت التشغيل، فالانعكاس كان سيعيد قيمة خاطئة.
     */
    private val CURRENT_DB_VERSION: Int = com.baynana.core.database.DATABASE_VERSION
}
