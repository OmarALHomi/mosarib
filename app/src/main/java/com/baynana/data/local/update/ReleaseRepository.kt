package com.baynana.data.local.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.baynana.core.database.AppDatabase
import com.baynana.core.update.HttpReleaseSource
import com.baynana.core.update.NetworkStatus
import com.baynana.core.update.ReleaseChannel
import com.baynana.core.update.ReleaseFetch
import com.baynana.core.update.ReleaseSignatures
import com.baynana.core.update.ReleaseSource
import com.baynana.domain.update.ReleaseManifest
import com.baynana.domain.update.UpdateDecision
import com.baynana.features.settings.AppSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * قناة التحديث على الجهاز (ح٢٠): فحص، وتذكير بما فُحص، وتنزيل **بأمر المستخدم**، وتحقق قبل العرض.
 *
 * المبادئ التي تحكم هذا الملفّ:
 * 1. **لا تنزيل صامت، ولا تثبيت صامت.** [check] يقرأ ويقرّر فقط؛ والتنزيل دالة تُنادى من زرّ؛
 *    والتثبيت يُسلَّم إلى مُثبِّت النظام بعد تحقق البصمة.
 * 2. **آخر ملفّ سليم يبقى محفوظًا.** جهاز بلا إنترنت لا يقول «تعذّر الفحص» وهو يعرف أمس أن هناك
 *    إصدارًا أحدث: يقول «آخر فحص: كذا، ووجد إصدارًا أحدث». والصفّ يُخزَّن موقّعًا كما وصل، فلا
 *    يستطيع من يعدّل قاعدة الجهاز أن يزرع إصدارًا بلا توقيع.
 * 3. **ما لم يُوقَّع لا يُخزَّن ولا يُعرض.** الملفّ المشوّه أو المرفوض لا يمسح حالة سابقة سليمة.
 * 4. **بصمة الـAPK تُقارن قبل أي عرض للتثبيت.** الملفّ الذي لا يطابق بصمته يُحذف فورًا، ويُقال
 *    للمستخدم بصراحة إن شيئًا لم يتغيّر في تطبيقه.
 */
/** نتيجة تنزيل خام. الطبقة التي فوقها (المستودع) هي التي تتحقق من البصمة وتُقرّر. */
sealed interface DownloadFetch {
    data class Saved(val sizeBytes: Long) : DownloadFetch
    data class Failed(val reasonArabic: String) : DownloadFetch
}

/** تنزيل الملفّ إلى مسار مؤقّت. واجهة، لأن الاختبار لا ينزل من شبكة ولا من خادم. */
fun interface ApkDownloader {
    suspend fun fetch(url: String, destination: File, maxBytes: Long): DownloadFetch
}

/** التنزيل الحقيقي: https فقط، بسقف حجم، وبلا متابعة إلى بروتوكول آخر. */
class HttpApkDownloader(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 20_000
) : ApkDownloader {

    override suspend fun fetch(url: String, destination: File, maxBytes: Long): DownloadFetch {
        if (!url.startsWith("https://")) return DownloadFetch.Failed("رابط الملفّ غير مشفّر — لا ننزّل من غير https")
        var connection: java.net.HttpURLConnection? = null
        return try {
            connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code != 200) {
                DownloadFetch.Failed("خادم التنزيل ردّ بحالة $code")
            } else {
                var total = 0L
                connection.inputStream.use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > maxBytes) {
                                destination.delete()
                                return DownloadFetch.Failed("حجم الملفّ أكبر من الحد المسموح — أُلغي التنزيل")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                DownloadFetch.Saved(total)
            }
        } catch (error: Exception) {
            runCatching { destination.delete() }
            DownloadFetch.Failed("تعذّر التنزيل: ${error.javaClass.simpleName}")
        } finally {
            runCatching { connection?.disconnect() }
        }
    }
}

class ReleaseRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val currentVersionCode: Int,
    private val currentVersionName: String,
    private val downloader: ApkDownloader = HttpApkDownloader(),
    /**
     * منفذان للفحص والمعاينة، **والإنتاج لا يمرّر شيئًا**:
     * - `sourceOverride`: مصدر جاهز (اختبار، أو أصل محلي بلا شبكة) بدل القراءة من قناة الشبكة.
     * - `verifierOverride`: فاحص بديل بدل المفتاح العام في الأصول.
     *
     * وهذا ليس ترفًا: الفحص يجب أن يقع على القرار والتخزين والبصمة، لا على وجود ملفّات أصول
     * في بيئة الاختبار. أما الإنتاج فمسار واحد: عنوان من الأصول، ومفتاح من الأصول.
     */
    private val sourceOverride: ReleaseSource? = null,
    private val verifierOverride: ((ByteArray, ByteArray) -> Boolean)? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val onlineOverride: (() -> Boolean)? = null
) {

    companion object {
        const val KEY_LAST_TOKEN = "release_last_token"
        const val KEY_LAST_CHECK_AT = "release_last_check_at"
        const val KEY_SKIPPED_VERSION = "release_skipped_version_code"

        /** سقف حجم الـAPK الذي نقبله: أكبر من أي بناء معقول، وأصغر من أن يملأ الجهاز. */
        const val MAX_APK_BYTES = 200L * 1024 * 1024
    }

    private val settings get() = db.appSettingDao()

    data class CheckResult(
        val state: UpdateDecision.UpdateState,
        /** زمن آخر فحص ناجح (مللي ثانية) أو `null` إن لم ينجح فحص بعد. */
        val checkedAt: Long?,
        /** هل الحالة من آخر ملفّ محفوظ لا من فحص الآن؟ الشاشة تقول ذلك صراحةً. */
        val fromCache: Boolean,
        /** أحدث إصدار معروف (من فحص أو من ملفّ محفوظ)، للعرض في «حول». */
        val knownLatestVersionName: String?,
        /**
         * سبب تعذّر آخر فحص، إن تعذّر. يُعرض **إلى جانب** الحالة لا بدلًا منها: جهاز يعرف أمس أن
         * هناك إصدارًا أحدث لا يجوز أن «ينسى» ذلك لأن شبكة اليوم مقطوعة.
         */
        val failureReasonArabic: String? = null
    )

    /** حالة معروفة الآن من الملفّ المحفوظ وحده — بلا شبكة وبلا انتظار. */
    suspend fun cachedState(): CheckResult = withContext(Dispatchers.IO) {
        val token = settings.getSettingValue(KEY_LAST_TOKEN)
        val checkedAt = settings.getSettingValue(KEY_LAST_CHECK_AT)?.toLongOrNull()
        val skipped = settings.getSettingValue(KEY_SKIPPED_VERSION)?.toIntOrNull()
        if (token.isNullOrBlank()) {
            return@withContext CheckResult(UpdateDecision.UpdateState.Unknown("لم يُفحص التحديث بعد"), null, true, null)
        }
        val readout = readToken(token)
        val state = UpdateDecision.decide(currentVersionCode, readout, skipped)
        CheckResult(state, checkedAt, fromCache = true, knownLatestVersionName = releaseOf(readout)?.versionName)
    }

    /**
     * فحص الآن. إن تعذّر (بلا شبكة، بلا عنوان، بلا مفتاح، أو ملفّ مرفوض) عاد بآخر حالة محفوظة
     * مع سبب التعذّر — فلا يفقد المستخدم ما يعرفه، ولا يُخدع بنجاح كاذب.
     */
    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        val cached = cachedState()
        val source = sourceOverride ?: run {
            val endpoint = ReleaseChannel.endpoint(context)
                ?: return@withContext cached.copy(
                    state = unknownOr(cached, "لم يُضبط عنوان قناة التحديث في هذه النسخة (release_endpoint.txt)"),
                    failureReasonArabic = "لم يُضبط عنوان قناة التحديث في هذه النسخة (release_endpoint.txt)"
                )
            HttpReleaseSource(endpoint) { NetworkStatus.isOnline(context) }
        }
        val verify = verifierOverride ?: ReleaseSignatures.check(context)
            ?: return@withContext cached.copy(
                state = unknownOr(cached, "مفتاح التحقق من التحديث غير مُسلَّم في هذه النسخة (release_public_key.txt)"),
                failureReasonArabic = "مفتاح التحقق من التحديث غير مُسلَّم في هذه النسخة (release_public_key.txt)"
            )
        val online = onlineOverride?.invoke() ?: NetworkStatus.isOnline(context)
        if (!online) {
            return@withContext cached.copy(
                state = unknownOr(cached, "لا إنترنت الآن"),
                failureReasonArabic = "لا إنترنت الآن"
            )
        }

        when (val fetch = source.fetch()) {
            is ReleaseFetch.Failed -> cached.copy(
                state = unknownOr(cached, fetch.reasonArabic),
                failureReasonArabic = fetch.reasonArabic
            )

            is ReleaseFetch.Body -> {
                val readout = ReleaseManifest.read(fetch.text, verify)
                when (readout) {
                    is ReleaseManifest.Readout.Verified -> {
                        // لا يُخزَّن إلا موقّعًا سليمًا: الملفّ المحفوظ دليل يُبنى عليه بعد انقطاع الشبكة.
                        val at = now()
                        settings.saveSetting(AppSetting(KEY_LAST_TOKEN, fetch.text.trim()))
                        settings.saveSetting(AppSetting(KEY_LAST_CHECK_AT, at.toString()))
                        val skipped = settings.getSettingValue(KEY_SKIPPED_VERSION)?.toIntOrNull()
                        CheckResult(
                            state = UpdateDecision.decide(currentVersionCode, readout, skipped),
                            checkedAt = at,
                            fromCache = false,
                            knownLatestVersionName = readout.release.versionName,
                            failureReasonArabic = null
                        )
                    }
                    // الملفّ المرفوض لا يمحو حالة سليمة محفوظة، ويُقال سببه بصراحة إلى جانبها.
                    is ReleaseManifest.Readout.NotARelease -> cached.copy(
                        state = unknownOr(cached, readout.reasonArabic),
                        failureReasonArabic = readout.reasonArabic
                    )
                    is ReleaseManifest.Readout.Malformed -> cached.copy(
                        state = unknownOr(cached, readout.reasonArabic),
                        failureReasonArabic = readout.reasonArabic
                    )
                    is ReleaseManifest.Readout.SignatureRejected -> cached.copy(
                        state = unknownOr(cached, readout.reasonArabic),
                        failureReasonArabic = readout.reasonArabic
                    )
                }
            }
        }
    }

    /**
     * تعذّر الفحص: إن كانت الحالة السابقة «تعذّر» فالأحدث أصدق (سبب اليوم)، وإن كانت حالة معروفة
     * (إصدار أحدث أو إجباري أو أحدث نسخة) فلا تُمحى — المستخدم لا يفقد ما يعرفه بسبب لحظة شبكة.
     */
    private fun unknownOr(cached: CheckResult, reasonArabic: String): UpdateDecision.UpdateState =
        if (cached.state is UpdateDecision.UpdateState.Unknown) UpdateDecision.UpdateState.Unknown(reasonArabic)
        else cached.state

    /** «تخطّي هذا الإصدار»: لا يُسقط الإصدارات الإجبارية، ولا يُخفي إصدارًا أحدث لاحقًا. */
    suspend fun skip(versionCode: Int): Boolean = withContext(Dispatchers.IO) {
        val cached = cachedState()
        val state = cached.state
        val isRequired = state is UpdateDecision.UpdateState.Required &&
            state.release.versionCode == versionCode
        if (isRequired) return@withContext false
        settings.saveSetting(AppSetting(KEY_SKIPPED_VERSION, versionCode.toString()))
        true
    }

    sealed interface DownloadOutcome {
        data class Ready(val file: File, val sizeBytes: Long) : DownloadOutcome
        data class Refused(val reasonArabic: String) : DownloadOutcome
        data class Failed(val reasonArabic: String) : DownloadOutcome
    }

    /**
     * تنزيل الـAPK **بطلب المستخدم**، مع حساب بصمته أثناء التنزيل ومقارنتها بالموقّع.
     * لا تثبيت هنا: الشاشة تعرض زرًّا ثانيًا بعد نجاح التحقق.
     */
    suspend fun download(release: ReleaseManifest.Release): DownloadOutcome = withContext(Dispatchers.IO) {
        if (!release.apkUrl.startsWith("https://")) {
            return@withContext DownloadOutcome.Refused("رابط الملفّ غير مشفّر — لا ننزّل من غير https")
        }
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "baynana-${release.versionCode}.apk")
        val temp = File(directory, "baynana-${release.versionCode}.part")
        runCatching { temp.delete() }

        val fetched = downloader.fetch(release.apkUrl, temp, MAX_APK_BYTES)
        if (fetched is DownloadFetch.Failed) {
            runCatching { temp.delete() }
            return@withContext DownloadOutcome.Failed(fetched.reasonArabic)
        }
        val bytes = (fetched as DownloadFetch.Saved).sizeBytes

        // البصمة تُحسب من الملفّ الذي نزل فعلًا — لا من رأس الاستجابة ولا من ثقة بالخادم.
        val actual = calculateSha256(temp)
        when (val permission = UpdateDecision.allowDownload(release, actual)) {
            is UpdateDecision.DownloadPermission.Refused -> {
                // أخطر لحظة في القناة: الملفّ الذي نزل ليس الملفّ الذي وُقّع. يُحذف فورًا.
                runCatching { temp.delete() }
                runCatching { target.delete() }
                DownloadOutcome.Refused(permission.reasonArabic)
            }
            UpdateDecision.DownloadPermission.Allowed -> {
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) {
                    temp.delete()
                    return@withContext DownloadOutcome.Failed("تعذّر حفظ الملفّ في ذاكرة الجهاز")
                }
                DownloadOutcome.Ready(target, bytes)
            }
        }
    }

    /** بصمة الملفّ المنزَّل. تُحسب على القرص لا في الذاكرة، فلا يبقى الملفّ كله في الرام. */
    private fun calculateSha256(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }.getOrNull()

    /** مقصد التثبيت: مُثبِّت النظام. لا نثبّت بأنفسنا، والمستخدم يرى شاشة النظام ويعرف ما يفعل. */
    fun installerIntent(file: File): Intent? = runCatching {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrNull()

    /** الرسالة التي تُعرض قبل التثبيت: ما الذي سيحدث، وما لا يحدث. */
    fun installSummary(release: ReleaseManifest.Release): String =
        "سيثبّت النظام النسخة ${UpdateDecision.displayVersion(release.versionName, release.versionCode)} " +
            "فوق نسختك الحالية (${UpdateDecision.displayVersion(currentVersionName, currentVersionCode)}). " +
            "دفترك ومبالغك في جهازك لا تُرسل ولا تُمحى، والحسابات لا تتغيّر."

    private suspend fun readToken(token: String): ReleaseManifest.Readout {
        val verify = verifierOverride ?: ReleaseSignatures.check(context)
            ?: return ReleaseManifest.Readout.SignatureRejected(
                "مفتاح التحقق من التحديث غير مُسلَّم في هذه النسخة — لا يُعرض إصدار لم نتحقق منه"
            )
        return ReleaseManifest.read(token, verify)
    }

    private fun releaseOf(readout: ReleaseManifest.Readout): ReleaseManifest.Release? =
        (readout as? ReleaseManifest.Readout.Verified)?.release
}
