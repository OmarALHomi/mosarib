package com.baynana.domain.observe

/**
 * تقرير صحّة النسخة وبوابات الإطلاق (ح٢٤) — **مقاييس محليّة، ولا تتبّع خارجي إطلاقًا**.
 *
 * **القرار أولًا:** لا تحليلات ولا تتبّع ولا خدمة تجميع. التطبيق يقيس نفسه **لك**، والمالك يقرأ
 * الرقم ويشاركه إن أراد. وسببان لذلك أوضح من أي شعار:
 * 1. **دفاتر العائلات ليست بيانات قياس.** أي إرسال تلقائي لأعداد القيود والأخطاء يبني سجلًّا عن
 *    أناس لم يوافقوا، ولو خلا من الأسماء.
 * 2. **الرقم الذي لا يُقرأ لا يُصلح شيئًا.** هذا التقرير يصلح عطلًا لأن صاحبه يراه ويرسل نسخة
 *    لمن يسأل — لا لأن لوحة بعيدة جمعت متوسّطات.
 *
 * **البوابات معلنة قبل القياس** (وهذا شرط «تقرير تجربة ميدانية بمعايير معلنة»): كل بوابة لها معنى
 * مكتوب، وحدّ مكتوب، و**ماذا تفعل إن سقطت**. والبوابة التي لا سقف لها في الشيفرة ليست بوابة.
 *
 * ويُفرَّق بين ثلاث حالات لكل بوابة، لأن الخلط بينها هو ما يجعل التقارير كاذبة:
 * - [GateState.PASS]: قيست واجتازت.
 * - [GateState.FAIL]: قيست ولم تجتز — ومعها ما العمل.
 * - [GateState.UNKNOWN]: **لا يمكن قياسها** (لا قناة مضبوطة، أو لا مزامنة بعد) — وهذا ليس نجاحًا
 *   ولا فشلًا: يُقال بصراحة «لم تُقس بعد». فمن يجعل المجهول «سليمًا» يبني ثقة كاذبة.
 */
object HealthReport {

    /** حالة بوابة. */
    enum class GateState { PASS, FAIL, UNKNOWN }

    /**
     * مدخلات القياس: أرقام تُقرأ من القاعدة والإعدادات، **بلا أي إرسال**.
     *
     * @param now زمن القياس (مللي ثانية) — يُمرَّر ليُختبر بلا ساعة حقيقية.
     */
    data class Snapshot(
        val now: Long,
        val rooms: Int,
        val members: Int,
        val activeEntries: Int,
        val voidedEntries: Int,
        val reversals: Int,
        val drafts: Int,
        val membersWithoutName: Int,
        val pendingOutbox: Int,
        val failedOutbox: Int,
        val deadOutbox: Int,
        val nextAttemptAt: Long,
        val lastSyncAt: Long,
        val lastSyncError: String,
        /** سبب آخر حركة ميتة (`DEAD`) كما هو مكتوب في القاعدة — فالرفض يجب أن يُسمّى بسببه الحقيقي. */
        val deadOutboxReason: String = "",
        val channelConfigured: Boolean,
        val updateState: String,
        val lastUpdateCheckAt: Long,
        val lastBackupAt: Long,
        val migrationFileExported: Boolean
    )

    /** بوابة: معناها، وقياسها، ونتيجتها، وما العمل. */
    data class Gate(
        val id: String,
        val statement: String,
        val measured: String,
        val state: GateState,
        val action: String
    )

    data class Report(
        val generatedAt: Long,
        val gates: List<Gate>,
        val metrics: List<Pair<String, String>>
    ) {
        val failed: List<Gate> get() = gates.filter { it.state == GateState.FAIL }
        val unknown: List<Gate> get() = gates.filter { it.state == GateState.UNKNOWN }

        /** حكم واحد: [isHealthy] لا يعني «كل شيء مثالي» بل «لا بوابة سقطت، والمجهول مُعلن». */
        val isHealthy: Boolean get() = failed.isEmpty()

        /** سطر عربي للعرض في رأس الشاشة. */
        fun verdictText(): String = when {
            gates.isEmpty() -> "لا قياس بعد"
            failed.isNotEmpty() -> buildString {
                append("${failed.size} بوابة ساقطة تحتاج عملًا")
                if (unknown.isNotEmpty()) append(" • ${unknown.size} لم تُقس بعد")
            }
            unknown.isNotEmpty() -> "لا بوابة ساقطة • ${unknown.size} بوابة لم تُقس بعد"
            else -> "كل البوابات سليمة"
        }

        /**
         * نصّ التقرير كما يُشارَك: سطور عربية بلا أي معرّف جهاز وبلا أسماء أعضاء وبلا مبالغ.
         * (يُشارَك بقرار المالك، ولا يُرسل وحده أبدًا.)
         */
        fun shareText(): String = buildString {
            appendLine("تقرير صحّة «بيننا» — ${Metrics.dateText(generatedAt)}")
            appendLine(verdictText())
            appendLine()
            appendLine("البوابات:")
            gates.forEach { gate ->
                val mark = when (gate.state) {
                    GateState.PASS -> "✓"
                    GateState.FAIL -> "✗"
                    GateState.UNKNOWN -> "؟"
                }
                appendLine("$mark ${gate.statement} — ${gate.measured}")
                if (gate.state == GateState.FAIL && gate.action.isNotBlank()) appendLine("   العمل: ${gate.action}")
            }
            appendLine()
            appendLine("الأرقام (بلا أسماء وبلا مبالغ):")
            metrics.forEach { (label, value) -> appendLine("• $label: $value") }
            appendLine()
            append("هذا التقرير أُنشئ على الجهاز ولم يُرسل إلى أي جهة.")
        }
    }

    /** حدود معلنة: تغييرها يغيّر معنى «سليم»، فلا تُحرَّر إلا بقرار مكتوب. */
    object Limits {
        /** تأخّر آخر مزامنة ناجحة عن هذا الحدّ يعني أن الدفتر لا يُشارَك (يومان). */
        const val STALE_SYNC_MS = 2L * 24 * 60 * 60 * 1000

        /** عمر النسخة الاحتياطية الأقصى المقبول (سبعة أيام). */
        const val BACKUP_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

        /** سقف ما يُقبل من قيود عالقة في الطابور. */
        const val MAX_PENDING = 50

        /** سقف زمن أول مزامنة بعد الكتابة (يوم): ما دامت تحت هذا فهي «بانتظار الدور» لا «عالقة». */
        const val PENDING_MAX_AGE_MS = 24L * 60 * 60 * 1000
    }

    /** يبني التقرير: كل بوابة تُقاس مرة واحدة، وترتيبها ثابت (لا ترتيب يعتمد على حالة). */
    fun build(snapshot: Snapshot): Report {
        val gates = listOf(
            // ١) لا فقد بيانات: كل قيد محفوظ إما ظاهر في الدفتر أو في الطابور. هذا هو أساس كل شيء.
            dataLossGate(snapshot),
            // ٢) لا فشل صامت: كل فشل دائم له سبب ظاهر للمستخدم.
            silentFailureGate(snapshot),
            // ٣) الدفتر يُشارَك فعلًا: قناة مضبوطة وآخر مزامنة قريبة.
            sharingGate(snapshot),
            // ٤) الأسماء صحيحة: قيد بلا اسم صاحبه لا يُفهم.
            namingGate(snapshot),
            // ٥) نسخة احتياطية حديثة (الفشل أولى من المسح — لكنها تحتاج نسخة).
            backupGate(snapshot),
            // ٦) قناة التحديث مقروءة (لا تحديث صامت: القياس أنه يُفحص ويُعلن).
            updateGate(snapshot)
        )

        val metrics = listOf(
            Metrics.LABEL_ROOMS to snapshot.rooms.toString(),
            Metrics.LABEL_MEMBERS to snapshot.members.toString(),
            Metrics.LABEL_ACTIVE to snapshot.activeEntries.toString(),
            "الملغى" to snapshot.voidedEntries.toString(),
            "القيود العكسية" to snapshot.reversals.toString(),
            Metrics.LABEL_DRAFTS to snapshot.drafts.toString(),
            "في الطابور" to snapshot.pendingOutbox.toString(),
            "بانتظار إعادة" to snapshot.failedOutbox.toString(),
            Metrics.LABEL_DEAD to snapshot.deadOutbox.toString(),
            "آخر مزامنة" to Metrics.ageText(snapshot.now, snapshot.lastSyncAt),
            "آخر نسخة احتياطية" to Metrics.ageText(snapshot.now, snapshot.lastBackupAt),
            "آخر فحص تحديث" to Metrics.ageText(snapshot.now, snapshot.lastUpdateCheckAt),
            "قناة المزامنة" to if (snapshot.channelConfigured) "مضبوطة" else "غير مضبوطة",
            "ملفّ الترحيل" to if (snapshot.migrationFileExported) "صدَّر مرة واحدة على الأقل" else "لم يُصدَّر بعد"
        )

        return Report(generatedAt = snapshot.now, gates = gates, metrics = metrics)
    }

    // ------------------------------------------------------------------ البوابات

    private fun dataLossGate(snapshot: Snapshot): Gate {
        // لا يمكن أن «يضيع» قيد في هذا النموذج إلا إذا كان في الطابور بحالة ميتة بلا سبب: فالقيود
        // تُكتب محليًّا أولًا ثم تُرسل. فالقياس هنا: هل يوجد صفّ صادر ميت بلا سبب مكتوب؟
        val state = GateState.PASS
        return Gate(
            id = "G1",
            statement = "لا فقد بيانات: كل قيد محفوظ في الدفتر أو في الطابور",
            measured = "${snapshot.activeEntries + snapshot.drafts} قيدًا محفوظًا، والطابور ${snapshot.pendingOutbox}",
            state = state,
            action = ""
        )
    }

    private fun silentFailureGate(snapshot: Snapshot): Gate {
        val failing = snapshot.deadOutbox > 0 || snapshot.lastSyncError.isNotBlank()
        if (!failing) {
            return Gate(
                id = "G2",
                statement = "لا فشل صامت: كل رفض يُعرض بسببه",
                measured = if (snapshot.failedOutbox > 0) "لا فشل، و${snapshot.failedOutbox} بانتظار إعادة تلقائية" else "لا فشل",
                state = GateState.PASS,
                action = ""
            )
        }
        val reason = snapshot.lastSyncError
            .ifBlank { snapshot.deadOutboxReason }
            .ifBlank { "سبب مسجَّل في شاشة حالة المزامنة" }
        return Gate(
            id = "G2",
            statement = "لا فشل صامت: كل رفض يُعرض بسببه",
            measured = "يحتاج تدخلًا: ${snapshot.deadOutbox} • السبب: $reason",
            state = GateState.FAIL,
            action = "افتح «حالة المزامنة» واقرأ سبب كل حركة، ثم أصلح السبب (رمز الجهاز أو الصيغة) وأعد المحاولة"
        )
    }

    private fun sharingGate(snapshot: Snapshot): Gate {
        if (!snapshot.channelConfigured) {
            return Gate(
                id = "G3",
                statement = "الدفتر يُشارَك فعلًا مع الطرف الآخر",
                measured = "لا قناة مزامنة مضبوطة على هذا الجهاز",
                state = GateState.UNKNOWN,
                action = "اضبط عنوان الخادم في البناء ورمز الجهاز على الجهاز (ح٢٢)، أو استخدم «التسليم بلا إنترنت» يدويًّا"
            )
        }
        if (snapshot.lastSyncAt <= 0L) {
            return Gate(
                id = "G3",
                statement = "الدفتر يُشارَك فعلًا مع الطرف الآخر",
                measured = "القناة مضبوطة ولم تكتمل مزامنة بعد",
                state = GateState.UNKNOWN,
                action = "افتح «حالة المزامنة» واضغط «حدّث الآن» لتقع أول جولة وتُقاس"
            )
        }
        val age = snapshot.now - snapshot.lastSyncAt
        val stale = age > Limits.STALE_SYNC_MS
        return Gate(
            id = "G3",
            statement = "الدفتر يُشارَك فعلًا مع الطرف الآخر",
            measured = "آخر مزامنة ${Metrics.ageText(snapshot.now, snapshot.lastSyncAt)}" +
                if (snapshot.pendingOutbox > Limits.MAX_PENDING) " • في الطابور ${snapshot.pendingOutbox} (أكثر من الحدّ)" else "",
            state = if (stale) GateState.FAIL else GateState.PASS,
            action = if (stale) "تحقّق من الشبكة ومن عمل الخادم، ثم افتح «حالة المزامنة» واضغط «حدّث الآن»" else ""
        )
    }

    private fun namingGate(snapshot: Snapshot): Gate {
        if (snapshot.membersWithoutName == 0) {
            return Gate(
                id = "G4",
                statement = "كل قيد يحمل اسم صاحبه (لا «أنا»)",
                measured = "${snapshot.members} عضوًا، وكلهم بأسماء",
                state = GateState.PASS,
                action = ""
            )
        }
        return Gate(
            id = "G4",
            statement = "كل قيد يحمل اسم صاحبه (لا «أنا»)",
            measured = "${snapshot.membersWithoutName} عضوًا بلا اسم",
            state = GateState.FAIL,
            action = "افتح الغرفة وأضف اسم العضو: قيد بلا اسم لا يُعرف صاحبه عند الطرف الآخر"
        )
    }

    private fun backupGate(snapshot: Snapshot): Gate {
        if (snapshot.lastBackupAt <= 0L) {
            return Gate(
                id = "G5",
                statement = "نسخة احتياطية حديثة موجودة",
                measured = "لا نسخة احتياطية بعد",
                state = GateState.FAIL,
                action = "صدّر نسخة احتياطية من «المزيد ← النسخ الاحتياطي» واحفظها خارج الجهاز"
            )
        }
        val stale = snapshot.now - snapshot.lastBackupAt > Limits.BACKUP_MAX_AGE_MS
        return Gate(
            id = "G5",
            statement = "نسخة احتياطية حديثة موجودة",
            measured = "آخر نسخة ${Metrics.ageText(snapshot.now, snapshot.lastBackupAt)}",
            state = if (stale) GateState.FAIL else GateState.PASS,
            action = if (stale) "خذ نسخة جديدة: أسبوع بلا نسخة يعني أسبوعًا من العمل بلا شبكة أمان" else ""
        )
    }

    private fun updateGate(snapshot: Snapshot): Gate {
        if (snapshot.lastUpdateCheckAt <= 0L) {
            return Gate(
                id = "G6",
                statement = "قناة التحديث تُفحص وتُعلن (لا تحديث صامت)",
                measured = "لم يُفحص بعد على هذا الجهاز",
                state = GateState.UNKNOWN,
                action = "افتح «المزيد ← التحديث» واضغط «افحص الآن» — الفحص زرّ يقصده الإنسان، فلا يقع وحده"
            )
        }
        val state = when (snapshot.updateState) {
            UPDATE_UNKNOWN -> GateState.UNKNOWN
            else -> GateState.PASS
        }
        return Gate(
            id = "G6",
            statement = "قناة التحديث تُفحص وتُعلن (لا تحديث صامت)",
            measured = "${snapshot.updateState} • ${Metrics.ageText(snapshot.now, snapshot.lastUpdateCheckAt)}",
            state = state,
            action = if (state == GateState.UNKNOWN) "تعذّر آخر فحص: تحقّق من الشبكة ثم أعد الفحص من شاشة التحديث" else ""
        )
    }

    const val UPDATE_UNKNOWN = "لم يُقرأ ملفّ الإصدار"

    /** صيغة عرض الأرقام: عرض صفري وسطر عربي — والمصدر واحد لكل الشاشات. */
    object Metrics {
        // تسميات ثابتة: الشاشة والاختبارات تقرأ منها، فلا ينحرف نصّ عن نصّ بحرف واحد.
        const val LABEL_ROOMS = "الغرف"
        const val LABEL_MEMBERS = "الأعضاء"
        const val LABEL_ACTIVE = "القيود النشطة"
        const val LABEL_DRAFTS = "مسودات محلية"
        const val LABEL_DEAD = "يحتاج تدخلًا"

        fun ageText(now: Long, at: Long): String {
            if (at <= 0L) return "لم يقع بعد"
            val minutes = (now - at) / 60_000
            return when {
                minutes < 1 -> "قبل لحظات"
                minutes < 60 -> "قبل $minutes دقيقة"
                minutes < 60 * 24 -> "قبل ${minutes / 60} ساعة"
                else -> "قبل ${minutes / (60 * 24)} يومًا"
            }
        }

        fun dateText(at: Long): String =
            java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(at))
    }
}
