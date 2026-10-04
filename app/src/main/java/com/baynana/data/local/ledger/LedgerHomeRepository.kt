package com.baynana.data.local.ledger

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.domain.ledger.AckDecision
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.RoomEntryFeed
import com.baynana.domain.ledger.RoomFeed

/**
 * قراءة وكتابة ما تحتاجه الشاشات الرئيسية من الدفتر: خلاصة كل غرفة، خلاصة غرفة واحدة، والإقرار.
 *
 * هذا الملف **لا يحسب أي رقم**: يقرأ القيود والإسقاطات من القاعدة، ويسلّمها إلى `RoomEntryFeed`
 * (وهو يبني [com.baynana.domain.ledger.LedgerSnapshot] داخله). فالشاشة لا ترى إلا نتيجة واحدة.
 */
class LedgerHomeRepository(private val db: AppDatabase) {

    private val dao get() = db.ledgerDao()

    /** معرّف صاحب هذا الجهاز في كل غرفة (نفس الاصطلاح في محرّك الدفتر). */
    companion object {
        const val MY_MEMBER_ID = "me"
    }

    suspend fun roomFeeds(): List<RoomFeed> = dao.getAllRooms().map { room ->
        feedOf(room.id) ?: emptyFeed(room)
    }

    suspend fun feedOf(roomId: String): RoomFeed? {
        val room = dao.getRoom(roomId) ?: return null
        val entries = dao.getEntriesIncludingVoided(roomId).map { it.toView() }
        val allocations = dao.getAllocationsInRoom(roomId).map { it.toView() }
        // القيود والعكس: `effectiveEntries` داخل اللقطة تُسقط الملغى والعكسي من الأرقام، أما الأسطر
        // فتُعرض كما هي مع حالتها، لأن المستخدم يجب أن يرى القيد الملغى وقيده العكسي.
        val mine = dao.getMyMembership(roomId)?.memberId ?: MY_MEMBER_ID
        return RoomEntryFeed.build(
            roomId = room.id,
            title = room.title,
            kind = room.kind,
            currency = room.currency,
            status = room.status,
            counterpartName = room.counterpartName,
            counterpartPhone = room.counterpartPhone,
            linkCode = room.linkCode,
            updatedAt = room.updatedAt,
            myMemberId = mine,
            entries = entries,
            allocations = allocations
        )
    }

    private fun emptyFeed(room: LedgerRoom): RoomFeed = RoomEntryFeed.build(
        roomId = room.id,
        title = room.title,
        kind = room.kind,
        currency = room.currency,
        status = room.status,
        counterpartName = room.counterpartName,
        counterpartPhone = room.counterpartPhone,
        linkCode = room.linkCode,
        updatedAt = room.updatedAt,
        myMemberId = MY_MEMBER_ID,
        entries = emptyList(),
        allocations = emptyList()
    )

    // ------------------------------------------------------------------ الإقرار

    data class AckResult(val entryId: String, val status: String, val created: Boolean)

    /**
     * إقرار أو اعتراض أو طلب تعديل على قيد أرسله الطرف الآخر.
     *
     * القواعد المنفّذة (وإن كانت الشاشة تشرحها، فهي تُفرض هنا أيضًا):
     * - الإقرار **يغيّر الحالة ولا يمسح** القيد ولا المبلغ.
     * - لا يُقرّ أحدٌ قيدًا على نفسه من جهازه (الطرف الآخر هو من يُقرّ).
     * - الرفض وطلب التعديل **لا بدّ لهما من سبب** مكتوب، لأن السبب هو ما يُصلح الأمر بعد ذلك.
     * - الملغى بقيد عكسي لا يُقرّ ولا يُعترض عليه: انتهى أمره بحالة معلنة.
     */
    suspend fun respondToEntry(
        entryId: String,
        decision: String,
        note: String,
        decidedAt: Long = System.currentTimeMillis()
    ): AckResult = db.withTransaction {
        val entry = dao.getEntry(entryId) ?: error("قيد غير موجود: $entryId")
        if (entry.status == EntryStatus.VOIDED) {
            error("قيد ملغى بقيد عكسي: لا يُقرّ ولا يُعترض عليه")
        }
        if (decision !in AckDecision.all) error("قرار غير معروف: $decision")
        val resulting = AckDecision.resultingStatus(decision)
        if (resulting != EntryStatus.ACKNOWLEDGED && note.isBlank()) {
            error("الرفض وطلب التعديل يحتاجان سببًا مكتوبًا يُعرض للطرف الآخر")
        }
        // من يُقرّ؟ من صار المال عليه بحكم القيد: هو من يقول «نعم هذا عليّ» أو «لا ليس عليّ».
        // فقيودي التي أدين بها للطرف يقرّرها هو، وقيوده التي يدين بها إليّ أقرّرها أنا.
        val myMemberId = dao.getMyMembership(entry.roomId)?.memberId ?: MY_MEMBER_ID
        if (entry.owedByMemberId != myMemberId) {
            error("هذا القيد مالٌ لك لا عليك: الإقرار يكون من الطرف الذي عليه المال")
        }

        dao.recordDecision(
            entryId = entryId,
            memberId = myMemberId,
            decision = decision,
            note = note,
            decidedAt = decidedAt,
            resultingStatus = resulting,
            updatedAt = decidedAt
        )
        AckResult(entryId = entryId, status = resulting, created = true)
    }
}
