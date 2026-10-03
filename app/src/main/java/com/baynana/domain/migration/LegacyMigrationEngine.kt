package com.baynana.domain.migration

import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.money.MoneyParse
import com.baynana.domain.money.MoneyWire
import java.math.BigDecimal

/**
 * الترحيل من الإرث (ح٧): نقل ديون العملاء القدامى إلى الدفتر الجديد **بلا اختراع تاريخ** و**بجرد
 * مطابق** قبل/بعد.
 *
 * **المصدر** هو ما كان التطبيق القديم يعرضه فعلًا: `remainingDebt` لكل سقية — هذا هو الرقم الذي
 * رآه المزارع في كشفه، وهو الذي يجب أن يتطابق بعد الترحيل حرفيًا. فالمحرّك يبني لكل سقية مفتوحة
 * قيدًا بمبلغ `remainingDebt` وبـ`occurredAt = startTime` الحقيقي، فلا يُخترع تاريخ، ويبقى الربط
 * بـ`sourceTable/sourceId` للمراجعة.
 *
 * **السندات لا تُرحَّل كقيود مستقلة**: في النموذج القديم سندات القبض مطويّة أصلًا داخل
 * `remainingDebt` (سند مربوط بسقية يخصم منها، وسند عام يخصم من إجمالي العميل). فترحيلها كقيود
 * إضافية يُنقص الدين مرتين. لذلك تُجرد أعدادها ومجاميعها في التقرير للمراجعة ولا تُنشئ قيودًا —
 * وهذا قرار معلن في التقرير لا صمت.
 *
 * **المال**: القديم كان يخزّن الريال كسرًا عشريًا (`Double`)، والجديد بالفلس الصحيح. التحويل يمرّ
 * من نصّ عشري (`BigDecimal.valueOf`) إلى `MoneyWire.decodeLegacyMajor` مرة واحدة، بلا أي حساب
 * عشري في الجديد (ADR-04).
 */
data class LegacyCustomerSnapshot(
    val id: Long,
    val name: String,
    val phone: String = "",
    val farmName: String = "",
    val isArchived: Boolean = false,
    val linkCode: String = ""
)

/** سقية قديمة. المبالغ بالريال العشري كما كانت، وتُحوَّل مرة واحدة. */
data class LegacySessionSnapshot(
    val id: Long,
    val customerId: Long,
    val startTime: Long,
    val totalAmount: Double,
    val amountPaid: Double,
    val remainingDebt: Double,
    val isLive: Boolean = false,
    val pumpName: String = ""
)

data class LegacyVoucherSnapshot(
    val id: Long,
    val customerId: Long?,
    val sessionId: Long?,
    val type: String,
    val amount: Double,
    val date: Long
)

data class LegacySnapshot(
    val customers: List<LegacyCustomerSnapshot>,
    val sessions: List<LegacySessionSnapshot>,
    val vouchers: List<LegacyVoucherSnapshot>
)

/** قيد مخطط له قبل الكتابة، بأنواع نقية. */
data class PlannedEntry(
    val id: String,
    val operationId: String,
    val roomId: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val description: String,
    val sourceTable: String,
    val sourceId: String,
    /** موجب: دين على العميل. سالب: رصيد له (دفع زيادة). */
    val debtDirection: Long
)

data class PlannedRoom(
    val roomId: String,
    val customerId: Long,
    val ownerMemberId: String,
    val customerMemberId: String,
    val title: String,
    val counterpartName: String,
    val counterpartPhone: String,
    val linkCode: String,
    val currency: String,
    val archived: Boolean
)

/** جرد عميل واحد: القديم، والجديد المخطط، والفرق. */
data class CustomerReconciliation(
    val customerId: Long,
    val customerName: String,
    val legacyMinor: Long,
    val plannedMinor: Long,
    val sessionCount: Int,
    val migratedSessions: Int,
    val skippedSettledSessions: Int,
    val skippedLiveSessions: Int,
    val creditMinor: Long,
    val voucherCount: Int,
    val voucherTotalMinor: Long,
    val warnings: List<String>
) {
    val differenceMinor: Long get() = legacyMinor - plannedMinor
    val matches: Boolean get() = differenceMinor == 0L
}

data class LegacyMigrationPlan(
    val currency: String,
    val rooms: List<PlannedRoom>,
    val entries: List<PlannedEntry>,
    val reconciliations: List<CustomerReconciliation>,
    val notes: List<String>
) {
    val allReconcile: Boolean get() = reconciliations.all { it.matches }
    val plannedTotalMinor: Long get() = reconciliations.sumOf { it.plannedMinor }
    val legacyTotalMinor: Long get() = reconciliations.sumOf { it.legacyMinor }
}

object LegacyMigrationEngine {

    const val SOURCE_TABLE = "water_sessions"

    /**
     * يبني خطة الترحيل كاملة قبل أي كتابة. الخطة تُعرض وتُراجَع، ثم تُطبَّق في معاملة.
     *
     * @param currency عملة الوجهة. **قرار مطلوب من المالك**: بيانات الإرث بالريال اليمني بلا تمييز
     *   قديم/جديد، والافتراضي هنا `YER_NEW` لأنه المتداول. تغييره سطر واحد قبل التنفيذ الفعلي.
     */
    fun plan(snapshot: LegacySnapshot, currency: String = "YER_NEW"): LegacyMigrationPlan {
        val sessionsByCustomer = snapshot.sessions.groupBy { it.customerId }
        val vouchersByCustomer = snapshot.vouchers.groupBy { it.customerId }
        // كود الربط فريد في غرف الدفتر: أي كود قديم مكرّر أو فارغ يُستبدل بكود مشتق من رقم العميل،
        // فلا يصطدم عميلان على كود واحد ولا يفقد عميل رمزه الأصلي إن كان فريدًا.
        val linkCodeCounts = snapshot.customers.groupingBy { it.linkCode.trim() }.eachCount()
        val rooms = mutableListOf<PlannedRoom>()
        val entries = mutableListOf<PlannedEntry>()
        val reconciliations = mutableListOf<CustomerReconciliation>()
        val notes = mutableListOf<String>()

        for (customer in snapshot.customers.sortedBy { it.id }) {
            val roomId = roomIdOf(customer.id)
            val ownerMemberId = "legacy-owner"
            val customerMemberId = "legacy-customer-${customer.id}"
            rooms += PlannedRoom(
                roomId = roomId,
                customerId = customer.id,
                ownerMemberId = ownerMemberId,
                customerMemberId = customerMemberId,
                title = customer.name.ifBlank { "عميل قديم ${customer.id}" },
                counterpartName = customer.name,
                counterpartPhone = customer.phone,
                linkCode = customer.linkCode.trim()
                    .takeIf { it.isNotBlank() && linkCodeCounts[it] == 1 }
                    ?: "legacy-${customer.id}",
                currency = currency,
                archived = customer.isArchived
            )

            val customerSessions = sessionsByCustomer[customer.id].orEmpty().sortedBy { it.startTime }
            val warnings = mutableListOf<String>()
            var legacyMinor = 0L
            var plannedMinor = 0L
            var migrated = 0
            var settled = 0
            var live = 0
            var creditMinor = 0L
            var adjustedAmounts = 0

            for (session in customerSessions) {
                if (session.isLive) {
                    live++
                    warnings += "سقية جارية (رقم ${session.id}) لم تُرحَّل: مبالغها لم تُقفل بعد"
                    continue
                }
                val converted = toMinorDetailed(session.remainingDebt, currency)
                val remaining = if (session.remainingDebt < 0.0) -converted.minor else converted.minor
                if (converted.adjusted) adjustedAmounts++
                // «الديون» تُجمع من الموجب وحدها، والدفع الزائد يُجمع في الرصيد الدائن بجانبها،
                // فلا يُنقص أحدهما الآخر ثم يظهر فرق وهمي في الجرد (§4.1: لا صافي يخفي الحقيقة).
                legacyMinor += remaining.coerceAtLeast(0L)

                when {
                    remaining > 0L -> {
                        entries += PlannedEntry(
                            id = "legacy-session-${session.id}",
                            operationId = operationIdOf(session.id),
                            roomId = roomId,
                            type = EntryType.WATER_SESSION,
                            amountMinor = remaining,
                            currency = currency,
                            // تاريخ حقيقي من السقية: لا يُخترع تاريخ ترحيل.
                            occurredAt = session.startTime,
                            description = buildString {
                                append("سقية")
                                if (session.pumpName.isNotBlank()) append(" — ").append(session.pumpName)
                                append(" (مُرحَّلة من الدفتر القديم)")
                            },
                            sourceTable = SOURCE_TABLE,
                            sourceId = session.id.toString(),
                            debtDirection = 1L
                        )
                        plannedMinor += remaining
                        migrated++
                    }

                    remaining < 0L -> {
                        // دفع زائد قديم: يبقى رصيدًا دائنًا في الغرفة، ولا يُنقل لأحد.
                        val credit = -remaining
                        entries += PlannedEntry(
                            id = "legacy-credit-${session.id}",
                            operationId = "legacy:credit:${session.id}",
                            roomId = roomId,
                            type = EntryType.GENERAL_RECEIPT,
                            amountMinor = credit,
                            currency = currency,
                            occurredAt = session.startTime,
                            description = "رصيد دائن مُرحَّل من الدفتر القديم",
                            sourceTable = SOURCE_TABLE,
                            sourceId = session.id.toString(),
                            debtDirection = -1L
                        )
                        creditMinor += credit
                    }

                    else -> settled++
                }
            }

            val vouchers = vouchersByCustomer[customer.id].orEmpty()
            // الجرد هنا للعرض فقط: نأخذ المقدار المطلق حتى لا يُوقف ترحيل عميل كامل بسبب
            // شذوذ في رقم قديم لا يُنشئ قيدًا أصلًا (§لا تدخل أرقام منقوصة إلى الدفتر).
            val voucherTotal = vouchers.filter { it.type == "RECEIPT" }
                .sumOf { toMinorDetailed(kotlin.math.abs(it.amount), currency).minor }
            if (vouchers.isNotEmpty()) {
                warnings += "سندات قديمة: ${vouchers.size} سندًا بمجموع قبض ${voucherTotal} فلسًا — " +
                    "لم تُرحَّل كقيود لأنها مطويّة داخل «المتبقي» في النموذج القديم، وجُردت هنا للمراجعة"
            }
            if (adjustedAmounts > 0) {
                warnings += "قيم عشرية دقيقة في $adjustedAmounts سقية عُدّلت إلى فلسين (أثر حساب عشري قديم)"
            }
            if (customerSessions.isEmpty() && vouchers.isEmpty()) {
                warnings += "عميل بلا سقيات ولا سندات: أُنشئت له غرفة فارغة للربط المستقبلي"
            }
            if (customer.isArchived) {
                warnings += "عميل في الأرشيف (مؤرشف): غرفته تُنشأ للقراءة فقط، وسجلاته محفوظة لا محذوفة"
            }

            reconciliations += CustomerReconciliation(
                customerId = customer.id,
                customerName = customer.name,
                // المقارنة على الديون المفتوحة؛ الرصيد الدائن يُعرض بجانبها لا داخلها (§4.1).
                legacyMinor = legacyMinor,
                plannedMinor = plannedMinor,
                sessionCount = customerSessions.size,
                migratedSessions = migrated,
                skippedSettledSessions = settled,
                skippedLiveSessions = live,
                creditMinor = creditMinor,
                voucherCount = vouchers.size,
                voucherTotalMinor = voucherTotal,
                warnings = warnings
            )
        }

        notes += "قاعدة «لا اختراع تاريخ»: كل قيد مُرحَّل يحمل تاريخ سقيته الحقيقية (startTime)."
        notes += "قاعدة «لا حذف ولا تعديل للإرث»: جداول العملاء والسقيات والسندات لا تُمَس، والترحيل قراءة فقط."
        notes += "قاعدة «لا ازدواج»: معرّف كل قيد مُرحَّل مشتق من رقم سقيته، فإعادة الترحيل لا تُنشئ صفًا ثانيًا."
        notes += "العملة المستهدفة: $currency"

        return LegacyMigrationPlan(
            currency = currency,
            rooms = rooms,
            entries = entries,
            reconciliations = reconciliations,
            notes = notes
        )
    }

    /** معرّف الغرفة: مشتق من رقم العميل فلا يتغير بين تشغيلين. */
    fun roomIdOf(customerId: Long): String = "legacy-room-$customerId"

    /** معرّف العملية: ثابت ومشتق، فهو حاجز منع الازدواج عند إعادة الترحيل. */
    fun operationIdOf(sessionId: Long): String = "legacy:session:$sessionId"

    /**
     * تحويل بإشارة كاملة (للاستخدام الخارجي): الدفع الزائد القديم يصبح رصيدًا دائنًا.
     * المحلّل المالي يرفض الأرقام السالبة عمدًا، فالإشارة تُطبَّق هنا بعد تحويل المقدار.
     */
    fun toMinorSigned(legacyMajor: Double, currency: String): Long {
        val minor = toMinor(legacyMajor, currency)
        return if (legacyMajor < 0.0) -minor else minor
    }

    /** مبلغ قديم بعد التحويل، وهل احتاج تدقيقًا إلى خانتين. */
    data class ConvertedAmount(val minor: Long, val adjusted: Boolean)

    /**
     * تحويل الريال العشري القديم إلى فلس بلا حساب عشري جديد:
     * 1) القيمة تُقرأ كنصّها العشري الدقيق (`BigDecimal.valueOf`)،
     * 2) تُقرَّب إلى خانات العملة (`HALF_UP`) لأن الإرث مخزَّن `Double` وقد يحمل أثرًا عشريًا مثل
     *    `0.30000000000000004` من جمع قديم — ورفضه يعني إيقاف ترحيل عميل كامل لأجل أثر حاسوبي،
     * 3) ثم تمرّ من محلّل المال نفسه فيتحقق السقف والرمز.
     *
     * و**كل تقريب يُعدّ ويُعلن** في تقرير الترحيل، فلا يمرّ تعديل على أرقام الناس بلا إشعار.
     *
     * تُعيد **المقدار** (بلا إشارة) مع علم التقريب؛ الإشارة تُعالَج عند مستدعيها.
     */
    fun toMinorDetailed(legacyMajor: Double, currency: String): ConvertedAmount {
        require(!legacyMajor.isNaN() && !legacyMajor.isInfinite()) {
            "مبلغ قديم غير صالح ($legacyMajor): قيمة غير رقمية في الدفتر القديم"
        }
        // المقدار المطلق عمدًا: `MoneyParser` يرفض السالب (فالأرقام السالبة لا تدخل المال)،
        // والإشارة مسؤولية المستدعي عبر [toMinorSigned] أو المعالجة الصريحة للاتجاه.
        val exact = BigDecimal.valueOf(kotlin.math.abs(legacyMajor))
        val units = when (val found = com.baynana.domain.money.Currency.fromCode(currency)) {
            null -> throw IllegalArgumentException("عملة غير معروفة: $currency")
            else -> found.minorUnits
        }
        val truncated = exact.stripTrailingZeros()
        val adjusted = truncated.scale() > units
        val rounded = exact.setScale(units, java.math.RoundingMode.HALF_UP)
        val text = rounded.toPlainString()
        return when (val parsed = MoneyWire.decodeLegacyMajor(text, currency)) {
            is MoneyParse.Ok -> ConvertedAmount(parsed.money.minor, adjusted)
            is MoneyParse.Error -> throw IllegalArgumentException("مبلغ قديم غير صالح ($text): ${parsed.message}")
        }
    }

    /** التحويل بالبساطة: الفلسات وحدها. */
    fun toMinor(legacyMajor: Double, currency: String): Long = toMinorDetailed(legacyMajor, currency).minor

    /** حالة العلامة: هل الترحيل أُنجز سابقًا؟ ولماذا لا يُعاد؟ */
    fun statusText(plan: LegacyMigrationPlan): String = buildString {
        append("عملاء: ${plan.reconciliations.size}")
        append(" • غرف: ${plan.rooms.size}")
        append(" • قيود: ${plan.entries.size}")
        append(" • مجموع الديون: ${plan.legacyTotalMinor} فلسًا")
        if (!plan.allReconcile) append(" • فرق في الجرد: ${plan.legacyTotalMinor - plan.plannedTotalMinor}")
    }
}
