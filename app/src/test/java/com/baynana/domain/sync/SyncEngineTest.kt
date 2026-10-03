package com.baynana.domain.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٦ — سيناريوهات §6.3 مختبرة على المحرّك النقي بلا قاعدة ولا شبكة:
 * «قتل التطبيق بعد الحفظ المحلي وقبل الإرسال»، «إعادة تشغيل أثناء مزامنة»، «رجوع cache قديمة بعد
 * إغلاق»، «رفض auth والبيانات محفوظة»، و«رجوع الاتصال بعد شهر».
 */
class SyncEngineTest {

    private val now = 1_700_000_000_000L

    private class FakeOutbox : SyncOutbox {
        class Row(val envelope: OutboxEnvelope, var state: String = "PENDING", var nextAttemptAt: Long = 0L)

        val rows = LinkedHashMap<String, Row>()
        val sentAt = mutableMapOf<String, Long>()
        val failures = mutableMapOf<String, Pair<String, Long>>()
        val dead = mutableMapOf<String, String>()

        fun add(operationId: String, createdAt: Long, attempts: Int = 0, state: String = "PENDING", due: Long = 0L) {
            rows[operationId] = Row(
                OutboxEnvelope(operationId, "entry", "e-$operationId", "UPSERT", "{}", createdAt, attempts),
                state,
                due
            )
        }

        override suspend fun pending(limit: Int, now: Long): List<OutboxEnvelope> = rows.values
            .filter { it.state in listOf("PENDING", "FAILED") && it.nextAttemptAt <= now }
            .sortedBy { it.envelope.createdAt }
            .take(limit)
            .map { it.envelope }

        override suspend fun markSent(operationId: String, at: Long) {
            sentAt[operationId] = at
            val row = rows.getValue(operationId)
            rows[operationId] = Row(row.envelope, "SENT", 0L)
        }

        override suspend fun markFailed(operationId: String, error: String, nextAttemptAt: Long, at: Long) {
            failures[operationId] = error to nextAttemptAt
            val row = rows.getValue(operationId)
            rows[operationId] = Row(row.envelope.copy(attempts = row.envelope.attempts + 1), "FAILED", nextAttemptAt)
        }

        override suspend fun markDead(operationId: String, error: String, at: Long) {
            dead[operationId] = error
            rows[operationId]?.state = "DEAD"
        }
    }

    private class FakeStore : SyncStore {
        var currentCursor = ""
        var syncedAt = 0L
        val tombstones = mutableSetOf<String>()
        val applied = mutableListOf<String>()
        val appliedOnce = mutableSetOf<String>()
        val errors = mutableListOf<String>()
        var outcomes = mutableMapOf<String, ApplyOutcome>()

        fun tombstone(entityId: String) {
            tombstones += entityId
        }

        override suspend fun isTombstoned(entityId: String): Boolean = entityId in tombstones

        override suspend fun apply(change: RemoteChange): ApplyOutcome {
            outcomes[change.entityId]?.let { return it }
            val first = appliedOnce.add(change.operationId)
            applied += change.operationId
            return if (first) ApplyOutcome.APPLIED else ApplyOutcome.DUPLICATE
        }

        override suspend fun recordTombstone(change: RemoteChange) {
            tombstones += change.entityId
        }

        override suspend fun cursor(): String = currentCursor

        override suspend fun setCursor(cursor: String, syncedAt: Long) {
            currentCursor = cursor
            this.syncedAt = syncedAt
        }

        override suspend fun recordError(message: String, at: Long) {
            errors += message
        }
    }

    private class FakeTransport(
        var outcomes: Map<String, PushOutcome> = emptyMap(),
        var pages: MutableList<PullPage> = mutableListOf(),
        var failPullWith: String? = null
    ) : TransportPort {
        val pushed = mutableListOf<String>()

        override suspend fun push(envelopes: List<OutboxEnvelope>): List<PushOutcome> =
            envelopes.map {
                pushed += it.operationId
                outcomes[it.operationId] ?: PushOutcome.Accepted
            }

        override suspend fun pull(cursor: String?): PullPage {
            failPullWith?.let { throw IllegalStateException(it) }
            if (pages.isEmpty()) return PullPage(emptyList(), nextCursor = cursor ?: "0")
            return pages.removeAt(0)
        }
    }

    private fun change(id: String, kind: String = RemoteChange.UPSERT, at: Long = now) =
        RemoteChange(kind, "entry", id, "op-$id", """{"id":"$id"}""", at)

    @Test
    fun `accepted items are marked sent and counted`() = runBlocking {
        val outbox = FakeOutbox().apply { add("op-1", now - 100); add("op-2", now - 50) }
        val report = SyncEngine(outbox, FakeStore()).push(FakeTransport(), now)

        assertEquals(2, report.accepted)
        assertEquals(0, report.failed)
        assertEquals(setOf("op-1", "op-2"), outbox.sentAt.keys)
    }

    @Test
    fun `a transport error defers the item and stops the queue so order is preserved`() = runBlocking {
        val outbox = FakeOutbox().apply { add("op-1", now - 100); add("op-2", now - 50) }
        val transport = FakeTransport(outcomes = mapOf("op-1" to PushOutcome.TransportError("لا يوجد اتصال")))
        val report = SyncEngine(outbox, FakeStore()).push(transport, now)

        assertEquals(1, report.failed)
        assertTrue(report.stoppedForRetry)
        assertEquals("العنصر التالي لم يُرسل قبل سابقه", listOf("op-1"), transport.pushed)
        assertEquals("لا يوجد اتصال", outbox.failures.getValue("op-1").first)
        assertEquals(now + RetryBackoff.BASE_MILLIS, outbox.failures.getValue("op-1").second)
    }

    @Test
    fun `a deferred item is not retried before its due time and is retried after`() = runBlocking {
        val outbox = FakeOutbox().apply { add("op-1", now - 100) }
        val store = FakeStore()
        val engine = SyncEngine(outbox, store)
        val transport = FakeTransport(outcomes = mapOf("op-1" to PushOutcome.TransportError("انقطاع")))

        engine.push(transport, now)
        assertEquals(1, outbox.failures.size)

        // قبل الموعد: لا إرسال إطلاقًا.
        val early = FakeTransport()
        val earlyReport = engine.push(early, now + 1_000)
        assertEquals(0, earlyReport.pushed)
        assertTrue(early.pushed.isEmpty())

        // بعد الموعد: يعود ويُرسل.
        val later = FakeTransport()
        val laterReport = engine.push(later, now + RetryBackoff.BASE_MILLIS + 1)
        assertEquals(1, laterReport.accepted)
        assertEquals(listOf("op-1"), later.pushed)
    }

    @Test
    fun backoffGrowsExponentiallyAndIsCapped() {
        assertEquals(60_000L, RetryBackoff.delayFor(1))
        assertEquals(120_000L, RetryBackoff.delayFor(2))
        assertEquals(240_000L, RetryBackoff.delayFor(3))
        assertEquals(RetryBackoff.MAX_MILLIS, RetryBackoff.delayFor(50))
    }

    @Test
    fun `a permanent rejection becomes dead with a visible reason and is never retried silently`() = runBlocking {
        val outbox = FakeOutbox().apply { add("op-1", now - 100) }
        val transport = FakeTransport(
            outcomes = mapOf("op-1" to PushOutcome.Rejected(retryable = false, reason = "بيانات مرفوضة: مبلغ غير صالح"))
        )
        val report = SyncEngine(outbox, FakeStore()).push(transport, now)

        assertEquals(1, report.dead)
        assertEquals("بيانات مرفوضة: مبلغ غير صالح", outbox.dead.getValue("op-1"))
        assertEquals("DEAD", outbox.rows.getValue("op-1").state)

        // ولا يُعاد في الجولة التالية.
        val next = FakeTransport()
        val nextReport = SyncEngine(outbox, FakeStore()).push(next, now + 10 * 60_000)
        assertEquals(0, nextReport.pushed)
        assertTrue(next.pushed.isEmpty())
    }

    @Test
    fun `pull applies the page and saves the cursor only after applying`() = runBlocking {
        val store = FakeStore()
        val transport = FakeTransport(
            pages = mutableListOf(PullPage(listOf(change("e-1"), change("e-2")), nextCursor = "c1"))
        )
        val report = SyncEngine(FakeOutbox(), store).pull(transport, now)

        assertEquals(2, report.pulled)
        assertEquals(2, report.applied)
        assertEquals(listOf("op-e-1", "op-e-2"), store.applied)
        assertEquals("c1", store.currentCursor)
        assertEquals(now, store.syncedAt)
    }

    @Test
    fun `a crash between applying and saving the cursor replays the page and only counts duplicates`() = runBlocking {
        val store = FakeStore()
        // الجولة الأولى: طُبّقت الصفحة ثم انقطع الجهاز قبل حفظ المؤشر (المؤشر ما زال فارغًا).
        val first = FakeTransport(pages = mutableListOf(PullPage(listOf(change("e-1")), nextCursor = "c1")))
        SyncEngine(FakeOutbox(), store).pull(first, now)
        store.currentCursor = "" // محاكاة انقطاع قبل الحفظ

        val second = FakeTransport(pages = mutableListOf(PullPage(listOf(change("e-1")), nextCursor = "c1")))
        val report = SyncEngine(FakeOutbox(), store).pull(second, now + 1_000)

        assertEquals(0, report.applied)
        assertEquals(1, report.duplicates)
        assertEquals("لا كتابة مكررة", 1, store.appliedOnce.size)
    }

    @Test
    fun `a tombstoned entity is skipped and never revived from a stale cache`() = runBlocking {
        val store = FakeStore().apply { tombstone("e-closed") }
        val transport = FakeTransport(
            pages = mutableListOf(PullPage(listOf(change("e-1"), change("e-closed")), nextCursor = "c1"))
        )
        val report = SyncEngine(FakeOutbox(), store).pull(transport, now)

        assertEquals(1, report.applied)
        assertEquals(1, report.tombstoned)
        assertFalse("الكيان المُغلق لم يُطبَّق", store.applied.contains("op-e-closed"))
    }

    @Test
    fun `a remote delete is recorded as a tombstone before anything else`() = runBlocking {
        val store = FakeStore()
        val transport = FakeTransport(
            pages = mutableListOf(PullPage(listOf(change("e-9", kind = RemoteChange.DELETE)), nextCursor = "c2"))
        )
        SyncEngine(FakeOutbox(), store).pull(transport, now)

        assertTrue("حجر القبر سُجّل", store.tombstones.contains("e-9"))
        assertTrue("لم يُطبَّق أي تغيير عادي", store.applied.isEmpty())
    }

    @Test
    fun `an unsupported change is reported and does not stop the page`() = runBlocking {
        val store = FakeStore().apply { outcomes["e-weird"] = ApplyOutcome.UNSUPPORTED }
        val transport = FakeTransport(
            pages = mutableListOf(PullPage(listOf(change("e-1"), change("e-weird")), nextCursor = "c1"))
        )
        val report = SyncEngine(FakeOutbox(), store).pull(transport, now)

        assertEquals(1, report.unsupported)
        assertEquals(1, report.applied)
        assertEquals("c1", report.cursor)
        assertTrue(store.errors.single().contains("تغيير غير مفهوم"))
    }

    @Test
    fun `a pull failure is recorded and the previous cursor is kept`() = runBlocking {
        val store = FakeStore().apply { currentCursor = "c-old" }
        val transport = FakeTransport(failPullWith = "انقطاع بعد شهر")
        val report = SyncEngine(FakeOutbox(), store).pull(transport, now)

        assertEquals("c-old", report.cursor)
        assertEquals("انقطاع بعد شهر", store.errors.single())
    }

    @Test
    fun `a long outage is reconciled by walking every page after reconnection`() = runBlocking {
        val store = FakeStore().apply { currentCursor = "c0" }
        val pages = mutableListOf(
            PullPage(listOf(change("e-1")), nextCursor = "c1", hasMore = true),
            PullPage(listOf(change("e-2")), nextCursor = "c2", hasMore = true),
            PullPage(listOf(change("e-3")), nextCursor = "c3", hasMore = false)
        )
        val report = SyncEngine(FakeOutbox(), store).pull(FakeTransport(pages = pages), now)

        assertEquals(3, report.applied)
        assertEquals("c3", store.currentCursor)
        assertEquals(3, store.appliedOnce.size)
    }

    @Test
    fun `a restarted engine continues from the stored state, pushing nothing twice`() = runBlocking {
        val outbox = FakeOutbox().apply { add("op-1", now - 100) }
        val store = FakeStore()
        val transport = FakeTransport()

        SyncEngine(outbox, store).run(transport, now)
        // إعادة تشغيل التطبيق: محرّك جديد على نفس المخازن.
        val restarted = SyncEngine(outbox, store).run(FakeTransport(), now + 60_000)

        assertEquals(0, restarted.pushed)
        assertEquals(listOf("op-1"), transport.pushed)
    }

    @Test
    fun `the report speaks Arabic to the user, never a false cloud success`() = runBlocking {
        val outbox = FakeOutbox().apply { add("op-1", now - 100) }
        val report = SyncEngine(outbox, FakeStore())
            .push(FakeTransport(outcomes = mapOf("op-1" to PushOutcome.TransportError("لا يوجد اتصال"))), now)

        assertEquals("بانتظار إعادة المحاولة: 1 • لا يوجد اتصال", report.summaryText())
        assertEquals("لا جديد", SyncReport().summaryText())
    }
}
