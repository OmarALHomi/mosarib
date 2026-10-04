package com.baynana.data.local.ledger

import com.baynana.core.database.AppDatabase
import com.baynana.features.sync.SyncStatusModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * يقرأ كل ما تحتاجه شاشة **حالة المزامنة**: الغرف والقيود وصندوق الصادر والإقرارات.
 *
 * لا يحسب شيئًا: يسلّم القراءة إلى [SyncStatusModel] فيكون القرار في مكان واحد قابل للاختبار.
 * والغاية أن يرى المستخدم حقيقة كل حركة: محفوظة محليًا، أو خرجت، أو أُقرّت، أو فشلت ومعها الحل.
 */
class SyncStatusRepository(private val db: AppDatabase) {

    private val dao get() = db.ledgerDao()

    /** تدفّق واحد يقرأ الجداول الخمسة معًا: لا شاشة تقرأ نصف الحقيقة. */
    fun observe(limit: Int = 80): Flow<SyncStatusModel.Snapshot> = combine(
        dao.observeRooms(),
        dao.observeRecentEntries(limit),
        dao.observeOutboxItems(),
        dao.observeRecentAcknowledgements(),
        dao.observeMyMemberships()
    ) { rooms, entries, outbox, acknowledgements, mine ->
        SyncStatusModel.Snapshot(
            rooms = rooms,
            entries = entries,
            outbox = outbox,
            acknowledgements = acknowledgements,
            myMemberships = mine
        )
    }

    /**
     * إعادة محاولة إرسال حركة فشلت فشلًا دائمًا. لا حذف ولا إعادة صامتة: المستخدم قرّر،
     * والحالة تعود «في الطابور» ويُصفَّر سبب الفشل القديم.
     */
    suspend fun retry(operationId: String, now: Long = System.currentTimeMillis()) {
        require(operationId.isNotBlank()) { "لا عملية لإعادة المحاولة" }
        dao.requeueOutbox(operationId = operationId, now = now)
    }
}
