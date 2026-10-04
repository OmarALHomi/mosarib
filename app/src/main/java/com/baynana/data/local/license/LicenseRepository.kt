package com.baynana.data.local.license

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.domain.license.LicenseAuthority
import com.baynana.domain.license.LicenseCheck
import com.baynana.domain.license.LicenseRejection
import com.baynana.domain.license.LicenseSignatureVerifier
import com.baynana.domain.license.LicenseToken
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID

/**
 * مستودع الترخيص: المكان الوحيد الذي يُكتب فيه استحقاق.
 *
 * ثلاث قواعد مثبّتة، وكل واحدة منها ردّ على عيب حقيقي في النسخة السابقة:
 * 1. **الاسترداد مرة واحدة**: كل استحقاق يُدرَّج بمفتاحه الفريد (`insertLicenseIfNew`)، فإن كان
 *    موجودًا فهو رفض `ALREADY_REDEEMED` — لا تمديد. (كان إدخال الرمز نفسه يمدّد كل مرة.)
 * 2. **الكتابة والحدث في معاملة واحدة**: لا استحقاق بلا أثر، ولا أثر بلا استحقاق. فيثبت في
 *    السجل أن الرمز استُردّ متى استُردّ، ويسأل المالك: من فعّل هذا الجهاز ومتى؟
 * 3. **المدة للتصاريح الموقّعة موقّعة لا محسوبة**: الجهاز لا يحسب مدة بنفسه ولا يزيدها؛
 *    وإن كان في الجهاز استحقاق أطول للمهنة نفسها فالأطول هو الذي يسري (التمديد المبكر لا يُنقص
 *    ما دفع المستخدم ثمنه).
 */
class LicenseRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    /** المهن التي تعرفها هذه النسخة، أو فراغ إن كنا لا نريد تقييد الفحص بها. */
    private val knownRoles: Set<String> = emptySet()
) {

    private val dao get() = db.licenseDao()

    /** نتيجة محاولة استرداد واحدة. */
    sealed interface Redemption {
        /** قُبل الرمز وكُتب الاستحقاق. [expiresAtBefore] لتُعلن الزيادة بدقة لا بتقدير. */
        data class Granted(
            val licenseId: String,
            val kind: String,
            val role: String,
            val plan: String,
            val durationDays: Int,
            val expiresAt: Long,
            val expiresAtBefore: Long
        ) : Redemption

        data class Rejected(val reason: LicenseRejection) : Redemption
    }

    /**
     * يسترد تصريحًا موقّعًا. [verifier] يُمرَّر من الطبقة العليا لأنه يحتاج المفتاح العام، و`null`
     * يعني أن النسخة لا تحمل مفتاحًا عامًا — وهي حالة تُقال بصراحة ([LicenseRejection.NO_PUBLIC_KEY])
     * ولا تُخلط بـ«رمز غير صالح».
     */
    suspend fun redeemSigned(
        entered: String,
        deviceCode: String,
        verifier: LicenseSignatureVerifier?
    ): Redemption {
        val now = clock()
        val normalizedDevice = LicenseToken.normalizeDeviceCode(deviceCode)
        val tokenSha = sha256Hex(entered.trim())

        return db.withTransaction {
            val redeemed = dao.redeemedKeys().toSet()
            val check = LicenseAuthority.check(
                entered = entered,
                deviceCode = normalizedDevice,
                nowMillis = now,
                verifier = verifier,
                redeemedKeys = redeemed,
                knownRoles = knownRoles
            )

            when (check) {
                is LicenseCheck.Rejected -> {
                    dao.insertEvent(
                        event(
                            // الأثر يربط الحدث **بالاستحقاق نفسه** كلما عُرف: رمز سليم لاستحقاق قائم
                            // يُربط بمفتاحه (`signed:<licenseId>`) فتُقرأ القصة كاملة — «هذا الاستحقاق
                            // استُردّ مرة، وحاول صاحبه ثانيةً». وما لا يُعرف استحقاقه (توقيع فاسد، أو
                            // جهاز آخر، أو رمز مشوّه) يُربط ببصمة المحاولة، فلا يُنسب إلى بريء.
                            licenseId = auditLicenseKey(entered, check.reason) ?: attemptKey(entered),
                            occurredAt = now,
                            outcome = LicenseKinds.REJECTED,
                            reason = check.reason.name,
                            role = "",
                            plan = "",
                            deviceCode = normalizedDevice,
                            expiresAt = 0L,
                            message = check.reason.messageArabic,
                            tokenPrefix = tokenPrefix(entered),
                            before = 0L,
                            after = 0L
                        )
                    )
                    Redemption.Rejected(check.reason)
                }

                is LicenseCheck.Granted -> {
                    val grant = check.grant
                    val current = dao.activeLicense(grant.role, now)?.expiresAt ?: 0L
                    val effectiveExpiresAt = maxOf(grant.expiresAt, current)
                    val row = LicenseRow(
                        licenseId = LicenseToken.redemptionKey(grant),
                        kind = LicenseKinds.SIGNED,
                        deviceCode = normalizedDevice,
                        role = grant.role,
                        plan = grant.plan,
                        durationDays = grant.durationDays,
                        issuedAt = grant.issuedAt,
                        expiresAt = effectiveExpiresAt,
                        grantedAt = now,
                        tokenSha256 = tokenSha
                    )
                    accept(row, now, current)
                }
            }
        }
    }

    /**
     * يسترد مفتاحًا من النسخة القديمة (المولَّد من السرّ المكتوب في الكود).
     *
     * نُبقيه لأن أجهزة كثيرة تعمل به، ولا نُخفف الحقيقة: هذا المسار **أضعف أمنيًا** لأن السرّ
     * موجود في الكود، ويُسجَّل بوسم `LEGACY` حتى يرى المالك حجم ما بقي عليه. لكن الخلل الذي كان
     * يمدّد بلا حدّ أُغلق هنا أيضًا: المفتاح نفسه يُستردّ مرة واحدة على هذا الجهاز.
     */
    suspend fun claimLegacy(
        rawKey: String,
        deviceCode: String,
        role: String,
        plan: String,
        durationDays: Int,
        note: String = ""
    ): Redemption {
        val now = clock()
        val normalizedDevice = LicenseToken.normalizeDeviceCode(deviceCode)
        val fingerprint = sha256Hex(normalizeKey(rawKey))
        return db.withTransaction {
            val current = dao.activeLicense(role, now)?.expiresAt ?: 0L
            val grantedUntil = maxOf(now, current) + durationDays.toLong() * DAY_MS
            val row = LicenseRow(
                licenseId = LicenseToken.legacyRedemptionKey(fingerprint),
                kind = LicenseKinds.LEGACY,
                deviceCode = normalizedDevice,
                role = role,
                plan = plan,
                durationDays = durationDays,
                issuedAt = now,
                expiresAt = grantedUntil,
                grantedAt = now,
                tokenSha256 = fingerprint,
                note = note
            )
            val inserted = dao.insertLicenseIfNew(row)
            if (inserted == -1L) {
                dao.insertEvent(
                    event(
                        licenseId = row.licenseId,
                        occurredAt = now,
                        outcome = LicenseKinds.REJECTED,
                        reason = LicenseRejection.ALREADY_REDEEMED.name,
                        role = role,
                        plan = plan,
                        deviceCode = normalizedDevice,
                        expiresAt = grantedUntil,
                        message = LicenseRejection.ALREADY_REDEEMED.messageArabic,
                        tokenPrefix = tokenPrefix(rawKey),
                        before = current,
                        after = current
                    )
                )
                return@withTransaction Redemption.Rejected(LicenseRejection.ALREADY_REDEEMED)
            }
            dao.insertEvent(
                event(
                    licenseId = row.licenseId,
                    occurredAt = now,
                    outcome = LicenseKinds.GRANTED,
                    reason = "",
                    role = role,
                    plan = plan,
                    deviceCode = normalizedDevice,
                    expiresAt = grantedUntil,
                    message = "استُردّ مفتاح من النسخة القديمة.",
                    tokenPrefix = tokenPrefix(rawKey),
                    before = current,
                    after = grantedUntil
                )
            )
            Redemption.Granted(
                licenseId = row.licenseId,
                kind = LicenseKinds.LEGACY,
                role = role,
                plan = plan,
                durationDays = durationDays,
                expiresAt = grantedUntil,
                expiresAtBefore = current
            )
        }
    }

    /** أحدث استحقاق سارٍ لمهنة على هذا الجهاز، أو `null`. */
    suspend fun activeLicense(role: String): LicenseRow? = dao.activeLicense(role, clock())

    fun observeLicenses(): Flow<List<LicenseRow>> = dao.observeAll()

    fun observeEvents(limit: Int = 50): Flow<List<LicenseEventRow>> = dao.observeEvents(limit)

    suspend fun redeemedKeys(): List<String> = dao.redeemedKeys()

    /** إدراج الاستحقاق ثم أثره في السجل — وإلا فلا هذا ولا ذاك. */
    private suspend fun accept(
        row: LicenseRow,
        now: Long,
        expiresAtBefore: Long
    ): Redemption {
        val inserted = dao.insertLicenseIfNew(row)
        if (inserted == -1L) {
            // سبق أن استُردّ في لحظة موازية (نقرتان على الزر نفسه): لا تمديد، وهذا هو الجواب الصحيح.
            dao.insertEvent(
                event(
                    licenseId = row.licenseId,
                    occurredAt = now,
                    outcome = LicenseKinds.REJECTED,
                    reason = LicenseRejection.ALREADY_REDEEMED.name,
                    role = row.role,
                    plan = row.plan,
                    deviceCode = row.deviceCode,
                    expiresAt = row.expiresAt,
                    message = LicenseRejection.ALREADY_REDEEMED.messageArabic,
                    tokenPrefix = row.licenseId.take(12),
                    before = expiresAtBefore,
                    after = expiresAtBefore
                )
            )
            return Redemption.Rejected(LicenseRejection.ALREADY_REDEEMED)
        }
        dao.insertEvent(
            event(
                licenseId = row.licenseId,
                occurredAt = now,
                outcome = LicenseKinds.GRANTED,
                reason = "",
                role = row.role,
                plan = row.plan,
                deviceCode = row.deviceCode,
                expiresAt = row.expiresAt,
                message = "قُبل التصريح وسُجّل لمرة واحدة.",
                tokenPrefix = row.licenseId.take(12),
                before = expiresAtBefore,
                after = row.expiresAt
            )
        )
        return Redemption.Granted(
            licenseId = row.licenseId,
            kind = row.kind,
            role = row.role,
            plan = row.plan,
            durationDays = row.durationDays,
            expiresAt = row.expiresAt,
            expiresAtBefore = expiresAtBefore
        )
    }

    private fun event(
        licenseId: String,
        occurredAt: Long,
        outcome: String,
        reason: String,
        role: String,
        plan: String,
        deviceCode: String,
        expiresAt: Long,
        message: String,
        tokenPrefix: String,
        before: Long,
        after: Long
    ) = LicenseEventRow(
        id = UUID.randomUUID().toString(),
        licenseId = licenseId,
        occurredAt = occurredAt,
        outcome = outcome,
        reason = reason,
        role = role,
        plan = plan,
        deviceCode = deviceCode,
        expiresAt = expiresAt,
        message = message,
        tokenPrefix = tokenPrefix,
        expiresAtBefore = before,
        expiresAtAfter = after
    )

    /**
     * مفتاح الاستحقاق الذي يُربط به حدث الرفض: يُعرف فقط في حالة «استُردّ سابقًا» حيث الرمز سليم
     * ومفتاحه مسجَّل في الجدول. وما عداها `null` فيُستعمل [attemptKey].
     */
    private fun auditLicenseKey(entered: String, reason: LicenseRejection): String? {
        if (reason != LicenseRejection.ALREADY_REDEEMED) return null
        val parsed = LicenseToken.parse(entered.trim()) as? LicenseToken.Parsed.Signed ?: return null
        val grant = LicenseToken.decodeGrant(parsed.payloadText) ?: return null
        return LicenseToken.redemptionKey(grant)
    }

    private fun attemptKey(entered: String): String =
        "attempt:" + sha256Hex(entered.trim()).take(24)

    private fun tokenPrefix(entered: String): String {
        val trimmed = entered.trim()
        return if (trimmed.length <= 12) trimmed else trimmed.substring(0, 12) + "…"
    }

    private fun normalizeKey(key: String): String =
        key.filter { !it.isWhitespace() && it != '-' }.uppercase()

    companion object {
        /** كان يُسمّى `SHARED_...` في الفحص: مسافة يوم واحد بالمللي ثانية. */
        const val DAY_MS = 24L * 60L * 60L * 1000L

        fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
