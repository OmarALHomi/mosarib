package com.baynana.data.local.observe

import android.content.Context
import com.baynana.core.database.AppDatabase
import com.baynana.core.sync.SyncConfig
import com.baynana.data.local.sync.SyncStatusReader
import com.baynana.data.local.update.readReleaseState
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.OutboxState
import com.baynana.domain.observe.HealthReport
import com.baynana.domain.update.UpdateDecision
import java.io.File

/**
 * قارئ تقرير الصحّة (ح٢٤): يقرأ أرقام **هذا الجهاز** ويبني التقرير كاملًا بلا شبكة وبلا إرسال.
 *
 * أربع قواعد في هذه الطبقة:
 * 1. **لا يقرأ مبالغ ولا أسماء**: أعداد وحالات فقط. فالتقرير المُشارَك لا يحمل قيدًا ولا ريالًا ولا
 *    اسم عضو — يمكن إرساله لمن يسأل بلا أن يكشف دفتر عائلة.
 * 2. **لا يكتب شيئًا** إلا بتنبيه صريح من المالك ([noteMigrationExported]): قراءة صافية لا تُحرّك
 *    حالة ولا تُصفّر خطأً ولا تعدّل إعدادًا.
 * 3. **لا يخترع رقمًا**: ما لم يقع يُقال «لم يقع بعد» لا صفرًا موهِمًا. (الصفر يعني: قيس وكان صفرًا.)
 * 4. **يعيد استعمال القرّاء القائمين** (`SyncStatusReader` و`ReleaseRepository.cachedState`): مصدر
 *    واحد لكل حقيقة، فلا يختلف التقرير عن الشاشة التي يراها المستخدم.
 */
class HealthRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val dao get() = db.ledgerDao()

    /** يبني التقرير الآن (وقت التنفيذ اختياري ليُختبر بلا ساعة حقيقية). */
    suspend fun report(now: Long = System.currentTimeMillis()): HealthReport.Report {
        val entries = dao.getAllEntries()
        val rooms = dao.getAllRooms()
        val members = dao.getAllMembers()
        val outbox = dao.getAllOutboxItems()
        val sync = SyncStatusReader(db).status(now)
        // قراءة صافية لحالة الإصدار المعروفة (بلا فحص وبلا شبكة): القارئ في طبقة التحديث لي قراءةً
        // كي لا يوجد مصدر ثانٍ لمعنى «أحدث إصدار» ينحرف عن الشاشة.
        val update = runCatching { readReleaseState(context, db) }.getOrNull()

        val snapshot = HealthReport.Snapshot(
            now = now,
            rooms = rooms.size,
            members = members.size,
            activeEntries = entries.count {
                it.status != EntryStatus.VOIDED && it.status != EntryStatus.DRAFT && it.reversesEntryId == null
            },
            voidedEntries = entries.count { it.status == EntryStatus.VOIDED },
            reversals = entries.count { it.reversesEntryId != null },
            drafts = entries.count { it.status == EntryStatus.DRAFT },
            membersWithoutName = members.count { it.displayName.isBlank() },
            pendingOutbox = outbox.count { it.state == OutboxState.PENDING },
            failedOutbox = outbox.count { it.state == OutboxState.FAILED },
            deadOutbox = outbox.count { it.state == OutboxState.DEAD },
            nextAttemptAt = sync.nextAttemptAt,
            lastSyncAt = dao.getSyncState(SYNC_STATE_KEY)?.lastSyncAt ?: 0L,
            lastSyncError = sync.lastError,
            channelConfigured = SyncConfig.isConfigured(context),
            updateState = updateText(update?.state, update?.knownLatestVersionName),
            lastUpdateCheckAt = update?.checkedAt ?: 0L,
            lastBackupAt = lastBackupAt(),
            migrationFileExported = reportPrefs().getBoolean(KEY_MIGRATION_EXPORTED, false)
        )
        return HealthReport.build(snapshot)
    }

    /** يعلّم أن المالك صدّر ملفّ ترحيل مرة (يُستعمل في التقرير، ولا يُرسل لأي جهة). */
    fun noteMigrationExported() {
        reportPrefs().edit().putBoolean(KEY_MIGRATION_EXPORTED, true).apply()
    }

    private fun reportPrefs() = context.getSharedPreferences(PREFS_REPORTS, Context.MODE_PRIVATE)

    /** حالة قناة التحديث كما يقرأها التطبيق من ملفّه المحفوظ، بجملة عربية واحدة. */
    private fun updateText(state: UpdateDecision.UpdateState?, latest: String?): String = when (state) {
        null -> HealthReport.UPDATE_UNKNOWN
        is UpdateDecision.UpdateState.UpToDate -> "أنت على أحدث إصدار"
        is UpdateDecision.UpdateState.Optional -> "إصدار أحدث اختياري" + (latest?.let { " ($it)" } ?: "")
        is UpdateDecision.UpdateState.Required -> "إصدار أحدث مطلوب" + (latest?.let { " ($it)" } ?: "")
        is UpdateDecision.UpdateState.Unknown -> HealthReport.UPDATE_UNKNOWN
    }

    /**
     * أحدث نسخة احتياطية في `files/backups` — من **الملفّ فعلًا** لا من إعداد يُحدَّث يدويًّا:
     * الإعداد قد يكذب، وزمن تعديل الملفّ لا.
     */
    private fun lastBackupAt(): Long {
        val directory = File(context.filesDir, "backups")
        if (!directory.isDirectory) return 0L
        return directory.listFiles()?.maxOfOrNull { it.lastModified() } ?: 0L
    }

    private companion object {
        const val PREFS_REPORTS = "baynana_reports_prefs"
        const val KEY_MIGRATION_EXPORTED = "migration_exported"
        const val SYNC_STATE_KEY = "ledger"
    }
}
