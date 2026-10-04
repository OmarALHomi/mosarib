package com.baynana.features.sync

import com.baynana.data.local.ledger.Acknowledgement
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.OutboxItem
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.OutboxState
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * الشاشة التي تُطمئن المستخدم يجب أن تكون صادقة قبل أن تكون جميلة: كل عبارة هنا مربوطة بحالة
 * مخزَّنة فعلًا، ولا «تمّ الإرسال» بلا صفّ في صندوق الصادر.
 */
class SyncStatusModelTest {

    private val now = 1_700_000_000_000L

    private fun room(id: String = "r1", title: String = "بيت الوالد") = LedgerRoom(
        id = id,
        kind = RoomKind.WATER,
        currency = "YER_NEW",
        title = title,
        status = RoomStatus.ACTIVE,
        linkCode = "AB12",
        counterpartName = "أحمد",
        counterpartPhone = "",
        createdAt = now,
        updatedAt = now
    )

    private fun entry(
        id: String,
        status: String,
        type: String = EntryType.WATER_SESSION,
        description: String = "سقية ٥ ساعات",
        roomId: String = "r1",
        occurredAt: Long = now
    ) = LedgerEntry(
        id = id,
        roomId = roomId,
        operationId = "op-$id",
        type = type,
        owedByMemberId = "me",
        owedToMemberId = "them",
        amountMinor = 3_500_000,
        currency = "YER_NEW",
        occurredAt = occurredAt,
        description = description,
        status = status,
        createdByMemberId = "me",
        createdAt = occurredAt,
        updatedAt = occurredAt
    )

    private fun outbox(
        entityId: String,
        state: String,
        error: String = "",
        attempts: Int = 1
    ) = OutboxItem(
        operationId = "op-$entityId",
        entityType = "LEDGER_ENTRY",
        entityId = entityId,
        action = "UPSERT",
        payload = "{}",
        state = state,
        attempts = attempts,
        lastError = error,
        nextAttemptAt = 0L,
        createdAt = now,
        updatedAt = now
    )

    private fun ack(entryId: String, memberId: String, decision: String, note: String = "") =
        Acknowledgement(
            entryId = entryId,
            memberId = memberId,
            decision = decision,
            note = note,
            decidedAt = now,
            createdAt = now
        )

    private fun member(roomId: String = "r1", memberId: String, isMe: Boolean) =
        RoomMember(
            roomId = roomId,
            memberId = memberId,
            displayName = memberId,
            isMe = isMe,
            joinedAt = now
        )

    private fun view(snapshot: SyncStatusModel.Snapshot) = SyncStatusModel.build(snapshot)

    @Test
    fun `قيد أُقرّ يقول أُقرّ ولا يقول شيئًا عن المسح`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.ACKNOWLEDGED)),
                outbox = listOf(outbox("e1", OutboxState.SENT)),
                myMemberships = listOf(member(memberId = "me", isMe = true))
            )
        )
        assertEquals(SyncStatusModel.Kind.ACKNOWLEDGED, view.rows.single().state)
        assertTrue(view.rows.single().detail.contains("لم يُمسح"))
    }

    @Test
    fun `قيد في الطابور يقول محفوظ محليًا لا مُرسل`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.SENT)),
                outbox = listOf(outbox("e1", OutboxState.PENDING))
            )
        )
        val row = view.rows.single()
        assertEquals(SyncStatusModel.Kind.LOCAL, row.state)
        assertTrue(row.detail.contains("محفوظ"))
        assertFalse(row.detail.contains("أُرسل"))
    }

    @Test
    fun `مسودة محلية تقول إنها تظهر لك وحدك`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.DRAFT))
            )
        )
        assertTrue(view.rows.single().detail.contains("تظهر لك وحدك"))
    }

    @Test
    fun `الفشل الدائم يُعرض بسببه ومع حلّ وبعملية إعادة محاولة`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.SENT)),
                outbox = listOf(outbox("e1", OutboxState.DEAD, error = "رقم الطرف غير صالح"))
            )
        )
        val row = view.rows.single()
        assertEquals(SyncStatusModel.Kind.DEAD, row.state)
        assertTrue(row.detail.contains("رقم الطرف غير صالح"))
        assertTrue(row.detail.contains("الحل"))
        assertEquals("op-e1", row.retryOperationId)
    }

    @Test
    fun `الفشل بلا سبب مسجّل لا يخترع سببًا`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.SENT)),
                outbox = listOf(outbox("e1", OutboxState.FAILED, error = ""))
            )
        )
        val row = view.rows.single()
        assertEquals(SyncStatusModel.Kind.PROBLEM, row.state)
        assertTrue(row.detail.contains("بلا سبب مسجّل"))
        assertTrue(row.detail.contains("ستُعاد المحاولة"))
    }

    @Test
    fun `الاعتراض يُنقل بنصّ صاحبه لا بوصف من عندنا`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.DISPUTED)),
                acknowledgements = listOf(
                    ack("e1", memberId = "them", decision = EntryStatus.DISPUTED, note = "السقية كانت ٣ ساعات"),
                    ack("e1", memberId = "me", decision = EntryStatus.ACKNOWLEDGED)
                ),
                myMemberships = listOf(member(memberId = "me", isMe = true))
            )
        )
        val row = view.rows.single()
        assertEquals(SyncStatusModel.Kind.PROBLEM, row.state)
        assertTrue(row.detail.contains("«السقية كانت ٣ ساعات»"))
        assertTrue(row.detail.contains("باقٍ"))
    }

    @Test
    fun `الملغى بقيد عكسي يقول ذلك ولا يختفي`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(entry("e1", EntryStatus.VOIDED))
            )
        )
        val row = view.rows.single()
        assertEquals(SyncStatusModel.Kind.VOIDED, row.state)
        assertTrue(row.detail.contains("قيد عكسي"))
        assertTrue(row.detail.contains("لا يُحذف"))
    }

    @Test
    fun `الأحدث أولًا ولو كان قديمًا فاشلًا`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room()),
                entries = listOf(
                    entry("e1", EntryStatus.SENT, occurredAt = now - 10_000),
                    entry("e2", EntryStatus.SENT, occurredAt = now)
                ),
                outbox = listOf(outbox("e1", OutboxState.DEAD, error = "انقطاع"))
            )
        )
        assertEquals(listOf("e2", "e1"), view.rows.map { it.entryId })
    }

    @Test
    fun `العدّاد يذكر المحفوظ والمُرسل وما يحتاج نظرًا`() {
        val snapshot = SyncStatusModel.Snapshot(
            rooms = listOf(room()),
            entries = listOf(
                entry("e1", EntryStatus.SENT),
                entry("e2", EntryStatus.ACKNOWLEDGED),
                entry("e3", EntryStatus.SENT),
                entry("e4", EntryStatus.SENT)
            ),
            outbox = listOf(
                outbox("e1", OutboxState.PENDING),
                outbox("e2", OutboxState.SENT),
                outbox("e3", OutboxState.FAILED, error = "انقطاع"),
                outbox("e4", OutboxState.DEAD, error = "رفض الخادم")
            )
        )
        val view = SyncStatusModel.build(snapshot)
        assertTrue(view.headline.contains("4 حركة"))
        assertTrue(view.headline.contains("1 محفوظة محليًا"))
        assertTrue(view.headline.contains("1 مُرسلة أو مُقَرّة"))
        assertTrue(view.headline.contains("1 تحتاج نظرك"))
        assertTrue(view.headline.contains("1 فشل دائم"))
    }

    @Test
    fun `الدفتر الفارغ يقول إنه فارغ`() {
        val view = view(SyncStatusModel.Snapshot())
        assertTrue(view.rows.isEmpty())
        assertTrue(view.headline.contains("لا حركة"))
    }

    @Test
    fun `عنوان السطر يقول نوع القيد بلغة الناس لا برمز`() {
        val view = view(
            SyncStatusModel.Snapshot(
                rooms = listOf(room(title = "غرفة السوق")),
                entries = listOf(
                    entry("e1", EntryStatus.SENT, type = EntryType.WATER_SESSION, description = "٥ ساعات"),
                    entry("e2", EntryStatus.SENT, type = EntryType.GENERAL_RECEIPT, description = "")
                )
            )
        )
        assertEquals("سقية: ٥ ساعات", view.rows.first { it.entryId == "e1" }.title)
        assertEquals("قبض عام", view.rows.first { it.entryId == "e2" }.title)
        assertEquals("غرفة السوق", view.rows.first { it.entryId == "e2" }.roomTitle)
    }
}
