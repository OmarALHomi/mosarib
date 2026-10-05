package com.baynana.domain.update

/**
 * قرار التحديث (ح٢٠) — منطق خالص بلا شبكة وبلا واجهة، لأن هذا القرار هو ما يخطئ الناس فيه:
 * إمّا تحديث صامت يقطع عملًا جاريًا، أو رسالة «أنت غير مدعوم» لمن هو مدعوم، أو تحديث يُخفى
 * عن صاحب أحدث نسخة فيظلّ يفحص بلا داعٍ.
 *
 * القواعد، وكل واحدة تقابل عطبًا حقيقيًا:
 * 1. **الفحص لا يوقف العمل أبدًا.** أي تعذّر (لا شبكة، ملفّ مفقود، توقيع مرفوض) يُرجع
 *    [UpdateState.Unknown] برسالة عربية، ويبقى الدفتر يعمل كما هو.
 * 2. **التنزيل فعل مستخدم.** لا حالة في هذا الملفّ تعني «ابدأ التنزيل»: كل الحالات تعرض زرًّا
 *    يضغطه الإنسان، ولا شيء يعمل في الخلفية.
 * 3. **«تخطّي هذا الإصدار» لا يُسقط الإصدارات الإجبارية.** من هو أقدم من الحد المدعوم يرى
 *    [UpdateState.Required] ولو تخطّى؛ لأن الإصدار القديم حينها معطوب لا «مؤجَّل».
 * 4. **من هو على الأحدث لا يُزعج.** لا شارة حمراء ولا زر كبير: [UpdateState.UpToDate].
 */
object UpdateDecision {

    /**
     * ما تعرضه الشاشة. [Unknown] ليس فشلًا: هو الوضع الطبيعي على جهاز بلا إنترنت.
     */
    sealed interface UpdateState {
        /** تعذّر الفحص، والتعليق يشرح السبب بلغة الإنسان. */
        data class Unknown(val reasonArabic: String) : UpdateState

        /** أنت على الأحدث. */
        data class UpToDate(val currentVersionCode: Int, val latestVersionCode: Int?) : UpdateState

        /** هناك أحدث، وهو اختياري — وللمستخدم أن يخطّيه. */
        data class Optional(val release: ReleaseManifest.Release, val skipped: Boolean) : UpdateState

        /** نسختك أقدم من الحد المدعوم: التحديث ليس اختياريًا، والبيانات تبقى سليمة حتى تحدّث. */
        data class Required(val release: ReleaseManifest.Release) : UpdateState

        /** الحالة التي تُعرض للإنسان في سطر واحد. */
        val titleArabic: String
            get() = when (this) {
                is Unknown -> "تعذّر فحص التحديث"
                is UpToDate -> "أنت على أحدث إصدار"
                is Optional -> if (skipped) "إصدار أحدث متاح (تخطّيته)" else "إصدار أحدث متاح"
                is Required -> "تحديث لازم ليكمل التطبيق عمله"
            }
    }

    /**
     * يقرّر ما يُعرض. [currentVersionCode] من التطبيق نفسه، [skippedVersionCode] ما اختار المستخدم
     * تأجيله سابقًا (أو `null`).
     */
    fun decide(
        currentVersionCode: Int,
        readout: ReleaseManifest.Readout,
        skippedVersionCode: Int? = null
    ): UpdateState = when (readout) {
        is ReleaseManifest.Readout.NotARelease -> UpdateState.Unknown(readout.reasonArabic)
        is ReleaseManifest.Readout.Malformed -> UpdateState.Unknown(readout.reasonArabic)
        is ReleaseManifest.Readout.SignatureRejected -> UpdateState.Unknown(readout.reasonArabic)
        is ReleaseManifest.Readout.Verified -> {
            val release = readout.release
            when {
                release.isUnsupported(currentVersionCode) -> UpdateState.Required(release)
                release.isNewerThan(currentVersionCode) ->
                    UpdateState.Optional(release, skipped = skippedVersionCode == release.versionCode)
                else -> UpdateState.UpToDate(currentVersionCode, release.versionCode)
            }
        }
    }

    /**
     * هل يجوز تنزيل هذا الملفّ؟ شرطان لا ثالث لهما: أن يُطلب فعلًا (زرّ يضغطه الإنسان)، وأن
     * تطابق بصمته ما في الملفّ الموقّع. البصمة تُقارَن نصًّا بعد التطبيع، فلا حسّاسات حالة.
     */
    fun allowDownload(release: ReleaseManifest.Release, actualSha256: String?): DownloadPermission {
        val actual = actualSha256?.trim()?.lowercase()
        if (actual.isNullOrEmpty()) return DownloadPermission.Refused("لم تُحسب بصمة الملفّ بعد")
        if (actual != release.apkSha256) {
            // أخطر لحظة في القناة: الملفّ الذي نزل ليس الملفّ الذي وُقّع. يُحذف ولا يُعرض على المستخدم.
            return DownloadPermission.Refused(
                "الملفّ الذي نزل لا يطابق البصمة الموقّعة — حُذف، وأبلغنا المطوّر. " +
                    "لم يتغيّر شيء في تطبيقك."
            )
        }
        return DownloadPermission.Allowed
    }

    sealed interface DownloadPermission {
        data object Allowed : DownloadPermission
        data class Refused(val reasonArabic: String) : DownloadPermission
    }

    /**
     * صياغة رقم الإصدار كما يقرأه الإنسان (`1.2` و`1.2.3`). الرقم وحده لا يُعرض للمستخدم
     * أبدًا: الأرقام للمطوّر، والأسماء للناس.
     */
    fun displayVersion(versionName: String, versionCode: Int): String =
        "${versionName.trim()} ($versionCode)"
}
