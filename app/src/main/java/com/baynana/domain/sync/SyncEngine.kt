package com.baynana.domain.sync

/**
 * مخزن الصادر كما يراه المحرّك: لا Room هنا. التنفيذ الفعلي في طبقة البيانات.
 */
interface SyncOutbox {
    /** عناصر بانتظار الإرسال بترتيب الإنشاء (الأقدم أولًا) فالترتيب حتمي. */
    suspend fun pending(limit: Int, now: Long): List<OutboxEnvelope>

    suspend fun markSent(operationId: String, at: Long)

    /** فشل مؤقت: يُحفظ الموعد الجديد للمحاولة القادمة. */
    suspend fun markFailed(operationId: String, error: String, nextAttemptAt: Long, at: Long)

    /** فشل دائم يحتاج تدخلًا: يبقى الصف بسبب ظاهر ولا يُعاد صامتًا. */
    suspend fun markDead(operationId: String, error: String, at: Long)
}

/**
 * مخزن الطرف المحلي كما يراه المحرّك: تطبيق التغييرات، المؤشر، وحجر القبر.
 */
interface SyncStore {
    /** هل هذا الكيان مُغلق/محذوف عن بعد؟ (يمنع عودته من ذاكرة قديمة) */
    suspend fun isTombstoned(entityId: String): Boolean

    suspend fun apply(change: RemoteChange): ApplyOutcome

    /** تسجيل حجر قبر لكيان أُغلق/حُذف عن بعد. */
    suspend fun recordTombstone(change: RemoteChange)

    suspend fun cursor(): String

    /** يُحفظ بعد تطبيق الصفحة كاملة، فلا تُفقد تغييرات لو انقطع الجهاز في منتصفها. */
    suspend fun setCursor(cursor: String, syncedAt: Long)

    /** تسجيل خطأ ظاهر للمستخدم (يُعرض في شاشة حالة المزامنة). */
    suspend fun recordError(message: String, at: Long)
}

/**
 * مؤقّت إعادة المحاولة: أسّي بحد أعلى، بلا عشوائية حتى تبقى الاختبارات حتمية.
 *   المحاولة 1 → دقيقة، 2 → دقيقتان، 3 → 4، 4 → 8 … بحد أقصى 6 ساعات.
 */
object RetryBackoff {
    const val BASE_MILLIS = 60_000L
    const val MAX_MILLIS = 6 * 60 * 60 * 1000L

    fun delayFor(attempts: Int): Long {
        if (attempts <= 0) return BASE_MILLIS
        var delay = BASE_MILLIS
        repeat(attempts - 1) {
            delay = if (delay >= MAX_MILLIS) MAX_MILLIS else delay * 2
        }
        return if (delay > MAX_MILLIS) MAX_MILLIS else delay
    }

    fun nextAttemptAt(attempts: Int, now: Long): Long = now + delayFor(attempts)
}

/**
 * محرّك المزامنة: قطعة نقية بلا آثار جانبية خارج الواجهتين [SyncOutbox] و[SyncStore]، فهو
 * يُختبر بلا قاعدة ولا شبكة، ويمكن تشغيله من عامل خلفي أو من زر «مزامنة الآن» بنفس السلوك.
 *
 * قواعد ثابتة (الخطة §6.2 و§6.3):
 * - **لا فقد**: الصف يُحفظ قبل الإرسال، وإعادة التشغيل تجد الصف كما تركه.
 * - **لا تكرار**: كل إرسال يحمل `operationId` ثابتًا، والطرف الآخر يتجاهل المكرر.
 * - **ترتيب محفوظ**: عند فشل مؤقت يتوقف الإرسال عند أول عنصر فاشل، فلا يُرسل لاحق قبل سابق.
 * - **فشل ظاهر لا كاذب**: لا «نجح سحابيًا» قبل قبول فعلي؛ والرفض الدائم بسبب عربي يُعرض للمستخدم.
 * - **المؤشر بعد التطبيق**: لا يُحفظ المؤشر إلا بعد تطبيق الصفحة، فتسليم مكرر يُكتشف ولا يُضاعف.
 * - **لا إنعاش من ذاكرة قديمة**: أي تغيير لكيان له حجر قبر يُتجاهل.
 */
class SyncEngine(
    private val outbox: SyncOutbox,
    private val store: SyncStore,
    private val batchSize: Int = 20,
    private val maxPullPages: Int = 50
) {

    suspend fun run(transport: TransportPort, now: Long): SyncReport {
        val pushReport = push(transport, now)
        val pullReport = pull(transport, now)
        return pushReport.merge(pullReport)
    }

    /** الإرسال وحده، مفصولًا ليستطيع المستدعي تشغيل الاثنين أو أحدهما بحسب الشبكة. */
    suspend fun push(transport: TransportPort, now: Long): SyncReport {
        val pending = outbox.pending(batchSize, now)
        if (pending.isEmpty()) return SyncReport()

        var accepted = 0
        var failed = 0
        var dead = 0
        var stopped = false
        val errors = mutableListOf<String>()

        for (envelope in pending) {
            val outcome = try {
                transport.push(listOf(envelope)).firstOrNull()
                    ?: PushOutcome.TransportError("لم تُرجع القناة نتيجة لهذه العملية")
            } catch (error: Exception) {
                PushOutcome.TransportError(error.message ?: "انقطاع أثناء الإرسال")
            }

            when (outcome) {
                PushOutcome.Accepted -> {
                    outbox.markSent(envelope.operationId, now)
                    accepted++
                }

                is PushOutcome.Rejected -> {
                    if (outcome.retryable) {
                        val nextAt = RetryBackoff.nextAttemptAt(envelope.attempts + 1, now)
                        outbox.markFailed(envelope.operationId, outcome.reason, nextAt, now)
                        failed++
                        errors += outcome.reason
                        stopped = true
                        break
                    }
                    // رفض دائم: لا نُعيد صامتًا، ونُبقي البيانات، ونُظهر السبب للمستخدم.
                    outbox.markDead(envelope.operationId, outcome.reason, now)
                    dead++
                    errors += outcome.reason
                }

                is PushOutcome.TransportError -> {
                    val nextAt = RetryBackoff.nextAttemptAt(envelope.attempts + 1, now)
                    outbox.markFailed(envelope.operationId, outcome.message, nextAt, now)
                    failed++
                    errors += outcome.message
                    stopped = true
                    break
                }
            }
        }

        return SyncReport(
            pushed = accepted + failed + dead,
            accepted = accepted,
            failed = failed,
            dead = dead,
            stoppedForRetry = stopped,
            errors = errors
        )
    }

    /** السحب وحده: صفحات متتابعة، وكل صفحة تُطبَّق ثم يُحفظ مؤشرها. */
    suspend fun pull(transport: TransportPort, now: Long): SyncReport {
        var cursor = store.cursor()
        var pulled = 0
        var applied = 0
        var duplicates = 0
        var tombstoned = 0
        var unsupported = 0
        val errors = mutableListOf<String>()
        var pages = 0

        while (pages < maxPullPages) {
            val page = try {
                transport.pull(cursor.ifBlank { null })
            } catch (error: Exception) {
                val message = error.message ?: "انقطاع أثناء السحب"
                store.recordError(message, now)
                errors += message
                break
            }
            pages++
            pulled += page.changes.size

            for (change in page.changes) {
                if (change.kind == RemoteChange.DELETE) {
                    store.recordTombstone(change)
                }
                if (store.isTombstoned(change.entityId)) {
                    tombstoned++
                    continue
                }
                when (store.apply(change)) {
                    ApplyOutcome.APPLIED -> applied++
                    ApplyOutcome.DUPLICATE -> duplicates++
                    ApplyOutcome.SKIPPED_TOMBSTONED -> tombstoned++
                    ApplyOutcome.UNSUPPORTED -> {
                        unsupported++
                        val message = "تغيير غير مفهوم من الطرف الآخر: ${change.kind} (${change.entityType})"
                        errors += message
                        store.recordError(message, now)
                    }
                }
            }

            // المؤشر يُحفظ **بعد** تطبيق الصفحة: لو انقطع الجهاز قبلها، تُعاد الصفحة ويُكتشف
            // المكرر بـ operationId فلا يتضاعف شيء.
            cursor = page.nextCursor
            store.setCursor(cursor, now)

            if (!page.hasMore) break
        }

        return SyncReport(
            pulled = pulled,
            applied = applied,
            duplicates = duplicates,
            tombstoned = tombstoned,
            unsupported = unsupported,
            cursor = cursor,
            errors = errors
        )
    }
}

private fun SyncReport.merge(other: SyncReport) = SyncReport(
    pushed = pushed + other.pushed,
    accepted = accepted + other.accepted,
    deferred = deferred + other.deferred,
    failed = failed + other.failed,
    dead = dead + other.dead,
    pulled = pulled + other.pulled,
    applied = applied + other.applied,
    duplicates = duplicates + other.duplicates,
    tombstoned = tombstoned + other.tombstoned,
    unsupported = unsupported + other.unsupported,
    cursor = other.cursor.ifBlank { cursor },
    stoppedForRetry = stoppedForRetry || other.stoppedForRetry,
    errors = errors + other.errors
)
