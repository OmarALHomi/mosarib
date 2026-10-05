package com.baynana.data.local.sync

import android.content.Context
import com.baynana.core.sync.HttpSyncTransport
import com.baynana.core.sync.SyncConfig
import com.baynana.core.sync.SyncTokenSetup
import com.baynana.domain.sync.TransportPort

/**
 * ربط قناة المزامنة عند بدء التطبيق (ح٢٢) — نقطة واحدة، وكلها قابلة للتفسير:
 *
 * - **القناة غيابها هو الأصل**: لا عنوان في الأصل البنائي أو لا رمز جهاز ⇒ لا نحقن شيئًا، فيبقى
 *   `SyncTransportProvider` فارغًا ويعمل التطبيق محليًّا كاملًا، ويقول العامل «لم تُضبط قناة مزامنة
 *   بعد». لا رسالة عطل، ولا محاولة اتصال فاشلة في كل تشغيل.
 *
 * - **لا يعمل هنا أي إرسال**: الدالة تُثبّت القناة فقط. الإرسال يجري في `SyncWorker` عند الطلب،
 *   بنفس سياسة العمل الفريد `baynana-ledger-sync`. فبدء التطبيق لا يمسّ الشبكة إلا إن طلب ذلك.
 *
 * - **لا تُخزَّن نسخة من الرمز في كائن عام**: النقل يقرأ الرمز مرة عند الحقن ويمسكه لنفسه، ولا
 *   يُطبع ولا يُسجَّل (الرأس فقط).
 *
 * - **الفشل صامت للمستخدم لكنه ليس أعمى**: أي خطأ في القراءة يترك القناة غير مضبوطة كما كانت،
 *   فلا يستحيل على المستخدم بسبب إعداد مكسور أن يفتح دفاتره (القاعدة: الفشل أولى من المسح، وأن
 *   يعمل محليًّا دائمًا).
 */
object SyncBridge {

    /**
     * يقرأ الإعداد من الأصول والإعدادات ويعيد `true` إن صارت قناة مضبوطة.
     *
     * وحين تُضبط القناة يقع أمران لا ثالث: تُثبَّت القناة، ويُوصَل **تنبيه الكتابة المحلية** ([LedgerWriteSignal])
     * بعمل مزامنة واحد، ويُجدَّول العمل الدوري الاحتياطي. أي أن الكتابة في دفتر العائلة تُشعر
     * المزامنة فورًا، بلا فحص دوري متكرّر. وإن لم تُضبط قناة: لا قناة، ولا دوري، ولا إشارة —
     * فالتطبيق الذي لا خادم له لا يوقظ شيئًا ولا يستهلك بطارية.
     */
    fun install(context: Context): Boolean = runCatching {
        val appContext = context.applicationContext ?: context
        val url = SyncConfig.changesUrl(appContext) ?: return@runCatching false
        val token = SyncConfig.deviceToken(appContext) ?: return@runCatching false
        val deviceId = SyncConfig.deviceId(appContext)
        SyncTransportProvider.install(
            HttpSyncTransport(changesUrl = url, deviceId = deviceId, token = token)
        )
        LedgerWriteSignal.listener = { SyncScheduler.requestSync(appContext) }
        SyncScheduler.schedulePeriodic(appContext)
        true
    }.getOrDefault(false)

    /**
     * **جولة تحقّق برمز مرشّح** (ح٢٢ب): تثبّت قناةً مؤقّتة بالرمز المُدخَل، وتُجري جولة مزامنة حقيقية،
     * ثم تحكم: قُبل، أم رُفض، أم تعذّر الوصول — **بلا حفظ إن لم يُقبل**.
     *
     * ولماذا هنا لا في الشاشة؟ لأن تنصيب القناة مسموح من هذا الملفّ وحده (حاجز ٢٠): مسار ثانٍ
     * يُنصّب قناة هو مسار ثانٍ يرسل بيانات العائلة. والشاشة تسأل، والجواب يُبنى هنا.
     *
     * وإن قُبل: يُحفظ الرمز (المكان الوحيد الذي يكتبه `SyncConfig`) ويُثبَّت الربط الإنتاجي كاملًا
     * بإشارة الكتابة والعمل الدوري. وإن لم يُقبل: **يُعاد الأثر كما كان**، فلا تبقى قناة نصف مضبوطة.
     */
    suspend fun tryConnect(context: Context, token: String): SyncTokenSetup.Outcome {
        val appContext = context.applicationContext ?: context
        val url = SyncConfig.changesUrl(appContext)
            ?: return SyncTokenSetup.Outcome.Refused("لا عنوان مزامنة في هذا البناء: القناة لا تُضبط من الشاشة")
        val clean = token.trim()
        if (clean.isEmpty()) return SyncTokenSetup.Outcome.Refused("لم تُدخل رمزًا")

        val previous = SyncTransportProvider.transport()
        // القناة المرشّحة تُبنى محلّيًّا ولا تُثبَّت في الموضع العام إلا بعد الحكم: فلا تبقى قناة
        // برمز مرفوض تعمل في الخلفية بينما الشاشة تقول «لم يُقبل».
        val candidate = HttpSyncTransport(
            changesUrl = url,
            deviceId = SyncConfig.deviceId(appContext),
            token = clean
        )
        val verdict = runCatching {
            val report = SyncCoordinator(
                db = com.baynana.core.database.AppDatabase.getDatabase(appContext),
                transport = candidate
            ).syncOnce()
            SyncTokenSetup.interpret(report.errors, report.applied)
        }.getOrElse { failure ->
            SyncTokenSetup.Outcome.Unreachable(failure.message ?: "تعذّر تنفيذ جولة المزامنة")
        }

        if (SyncTokenSetup.shouldSave(verdict)) {
            SyncConfig.saveDeviceToken(appContext, clean)
            install(appContext)
        } else {
            SyncTransportProvider.install(previous)
        }
        return verdict
    }

    /**
     * حقن قناة جاهزة (اختبار، أو قناة يشغّلها المالك يدويًّا في نسخة داخلية). منفصل عن [install]
     * عمدًا: مسار الإنتاج يمرّ بالفحص والتوليد، ومسارات الاختبار لا تُخفّف فحوص الإنتاج.
     */
    fun installTransport(transport: TransportPort?) {
        SyncTransportProvider.install(transport)
    }
}

/**
 * إشارة كتابة محلية: تُنادى من القاعدة بعد نجاح كتابة قيد (لا من الواجهة)، فتبقى المزامنة
 * **تابعة للكتابة** لا للشاشة التي كُتب منها. وإن لم تُضبط قناة فلا مستمع ⇒ لا شيء يحدث.
 *
 * ولا تُخزَّن فيها بيانات القيود: نداء بلا وسائط فقط. من يحتاج التفاصيل يقرأ صندوق الصادر من
 * القاعدة، فلا يُبنى في الذاكرة مسار ثانٍ للبيانات.
 */
object LedgerWriteSignal {
    @Volatile
    var listener: (() -> Unit)? = null

    /** يُنادى في مسار الكتابة؛ لا يرفع استثناء أبدًا حتى لا يُفشل حفظ قيد بسبب المزامنة. */
    fun fire() {
        runCatching { listener?.invoke() }
    }
}
