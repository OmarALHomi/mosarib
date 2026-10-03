package com.baynana.data.local.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.BackoffPolicy
import androidx.work.workDataOf
import com.baynana.core.database.AppDatabase
import com.baynana.domain.sync.SyncReport
import com.baynana.domain.sync.TransportPort
import java.util.concurrent.TimeUnit

/**
 * عامل المزامنة الخلفي (الخطة §6.1): يشتغل عند توفّر الشبكة، بسياسة إعادة أسّية، ولا يمسح شيئًا.
 *
 * مبادئ ثابتة:
 * - **لا فقد ولا تكرار**: كل عنصر صادر يحمل `operationId` ثابتًا، والعامل لا يعتمد على بقائه في
 *   الذاكرة: كل ما يحتاجه محفوظ في القاعدة، فإعادة التشغيل تستأنف من حيث توقفت.
 * - **لا «نجاح سحابي» كاذب**: نتيجة الجولة تُكتب في مخرجات العمل (`workData`) وتُعرض للمستخدم من
 *   `SyncStatusReader`؛ ولا يُعلن الإرسال إلا بعد قبول فعلي.
 * - **الفشل المؤقت يُعاد**، والرفض الدائم يبقى ظاهرًا بسببه ولا يُعاد صامتًا.
 *
 * القناة تُمرَّر من [SyncTransportProvider]، وهي الواجهة التي ستُربط بالنقل السلكي الفعلي في
 * ح١٩/ح٢٢. وحتى تُربط، فإن غياب قناة يعني نتيجة «لم تُضبط قناة» بلا أي إفساد للبيانات.
 */
class SyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val transport = SyncTransportProvider.transport()
            ?: return Result.success(workDataOf("channel" to "غير مضبوطة", "summary" to "لم تُضبط قناة مزامنة بعد"))
        val database = AppDatabase.getDatabase(applicationContext)
        val now = System.currentTimeMillis()

        val report = try {
            SyncCoordinator(database, transport).syncOnce(now)
        } catch (error: Exception) {
            // انقطاع مفاجئ أو خطأ غير متوقع: يُعاد لاحقًا بسياسة أسّية، والبيانات كما هي.
            return Result.retry()
        }

        val output = workDataOf(
            "accepted" to report.accepted,
            "failed" to report.failed,
            "dead" to report.dead,
            "applied" to report.applied,
            "summary" to report.summaryText()
        )

        return when {
            // عناصر «ميتة» تحتاج تدخل المستخدم: لا فائدة من إعادة العمل، والحالة تُعرض في الشاشة.
            report.dead > 0 -> Result.success(output)
            // إعادة المحاولة لا تحمل مخرجات (Result.retry بلا وسائط)، والحالة تُقرأ من القاعدة
            // عبر SyncStatusReader: المنتظر والفاشل والميت وموعد المحاولة القادمة.
            report.stoppedForRetry -> Result.retry()
            else -> Result.success(output)
        }
    }
}

/**
 * مصدر القناة السلكية. يُضبط مرة واحدة عند بدء التطبيق (أو يبقى فارغًا حتى تُبنى قناة ح١٩/ح٢٢).
 * الهدف: ألا يعرف العامل ولا المحرّك أي شيء عن Firebase أو HTTP.
 */
object SyncTransportProvider {
    @Volatile
    private var current: TransportPort? = null

    fun install(transport: TransportPort?) {
        current = transport
    }

    fun transport(): TransportPort? = current
}

/** جدولة المزامنة: عمل واحد فريد باسم ثابت، فلا تتراكم مهام متوازية. */
object SyncScheduler {
    const val WORK_NAME = "baynana-ledger-sync"
    private const val BACKOFF_SECONDS = 60L

    fun schedule(context: Context, delayMillis: Long = 0L) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

/** ملخّص جولة كما يُكتب في مخرجات العمل، لقراءته من الشاشة بلا إعادة حساب. */
fun SyncReport.toWorkSummary(): String = summaryText()
