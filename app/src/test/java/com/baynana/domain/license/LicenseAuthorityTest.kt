package com.baynana.domain.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قرار الفاحص: من يمرّ ومن يُرفض وبأي رسالة عربية.
 *
 * الفاحص نقيّ تمامًا هنا (بلا قاعدة وبلا Android): نتلاعب بالتوقيع والجهاز والاسترداد يدويًا
 * فنرى القرار نفسه الذي تراه الشاشة عند المستخدم، ونعرف أي شرط أنتج أي رسالة.
 */
class LicenseAuthorityTest {

    private val deviceCode = "MSRB-8F42-9D1B"
    private val now = 1_761_000_000_000L
    private val grant = LicenseToken.Grant(
        licenseId = "lic-2026-0001-abcd",
        deviceCode = deviceCode,
        role = "MUSRIB",
        plan = "MONTHLY",
        durationDays = 30,
        issuedAt = now - 1000L,
        expiresAt = now + 30L * 24L * 3600L * 1000L
    )

    /** توقيع صادق دائمًا: كل ما يهمّنا في هذا الاختبار هو قرار الفاحص لا الرياضيات. */
    private val honestVerifier = object : LicenseSignatureVerifier {
        override fun verify(payloadBytes: ByteArray, signatureBytes: ByteArray): Boolean = true
    }

    private val liarVerifier = object : LicenseSignatureVerifier {
        override fun verify(payloadBytes: ByteArray, signatureBytes: ByteArray): Boolean = false
    }

    private fun tokenFor(grant: LicenseToken.Grant = this.grant): String =
        LicenseToken.encode(LicenseToken.payloadText(grant), ByteArray(64) { 7 })

    private fun check(
        entered: String,
        verifier: LicenseSignatureVerifier? = honestVerifier,
        redeemed: Set<String> = emptySet(),
        currentDevice: String = deviceCode,
        at: Long = now
    ): LicenseCheck = LicenseAuthority.check(entered, currentDevice, at, verifier, redeemed)

    @Test
    fun validGrantPassesAndCarriesTheSignedTerm() {
        val result = check(tokenFor())
        assertTrue("التصريح الصحيح يُقبل", result is LicenseCheck.Granted)
        val granted = (result as LicenseCheck.Granted).grant
        assertEquals("lic-2026-0001-abcd", granted.licenseId)
        assertEquals(30, granted.durationDays)
        assertEquals(grant.expiresAt, granted.expiresAt)
    }

    @Test
    fun missingPublicKeySaysSoInsteadOfAccusingTheUser() {
        val result = check(tokenFor(), verifier = null)
        assertTrue(result is LicenseCheck.Rejected)
        assertEquals(
            "نسخة بلا مفتاح عام تقول ذلك صراحة، ولا تُسمّي التصريح مزوّرًا",
            LicenseRejection.NO_PUBLIC_KEY,
            (result as LicenseCheck.Rejected).reason
        )
    }

    @Test
    fun badSignatureIsRejectedBeforeAnyOtherTalk() {
        val result = check(tokenFor(), verifier = liarVerifier)
        assertEquals(LicenseRejection.BAD_SIGNATURE, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun aGrantForAnotherDeviceIsRejectedWithItsOwnReason() {
        val result = check(tokenFor(), currentDevice = "MSRB-1111-2222")
        assertEquals(LicenseRejection.WRONG_DEVICE, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun redeemedGrantIsRejectedAndThatIsTheReplayBarrier() {
        val redeemed = setOf(LicenseToken.redemptionKey(grant))
        val result = check(tokenFor(), redeemed = redeemed)
        assertEquals(LicenseRejection.ALREADY_REDEEMED, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun aTermThatEndedBeforeRedemptionIsNotSilentlyActivated() {
        val stale = grant.copy(expiresAt = now - 1L)
        val result = check(tokenFor(stale))
        assertEquals(LicenseRejection.PERIOD_ENDED, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun unknownProfessionIsRejectedWhenTheVersionDeclaresItsRoles() {
        val stranger = grant.copy(role = "WATERBARON")
        val result = LicenseAuthority.check(
            entered = tokenFor(stranger),
            deviceCode = deviceCode,
            nowMillis = now,
            verifier = honestVerifier,
            redeemedKeys = emptySet(),
            knownRoles = setOf("MUSRIB", "DALLAL", "BUYER", "FARMER")
        )
        assertEquals(LicenseRejection.UNKNOWN_TERM, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun aWrongDeviceIsReportedBeforeAlreadyRedeemed() {
        // الترتيب مقصود: «استُردّ سابقًا» على جهاز آخر رسالة مضلّلة، والصحيح أن نقول «لجهاز آخر».
        val result = check(
            tokenFor(),
            redeemed = setOf(LicenseToken.redemptionKey(grant)),
            currentDevice = "MSRB-9999-8888"
        )
        assertEquals(LicenseRejection.WRONG_DEVICE, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun legacyLookingInputIsMalformedNotSilentlyIgnored() {
        assertEquals(LicenseRejection.MALFORMED, (check("ACTV-M-8F42-9D1B") as LicenseCheck.Rejected).reason)
    }

    @Test
    fun tamperedPayloadFailsEvenWhenTheDeviceStillMatches() {
        // تغيير مدة في الحمولة يجعل التوقيع لا يطابقها — والقرار يعتمد على التوقيع لا على شكل النصّ.
        val tampered = LicenseToken.encode(
            LicenseToken.payloadText(grant).replace("|30|", "|3650|"),
            ByteArray(64) { 7 }
        )
        val strictVerifier = object : LicenseSignatureVerifier {
            override fun verify(payloadBytes: ByteArray, signatureBytes: ByteArray): Boolean =
                payloadBytes.toString(Charsets.UTF_8) == LicenseToken.payloadText(grant)
        }
        val result = LicenseAuthority.check(tampered, deviceCode, now, strictVerifier, emptySet())
        assertEquals(LicenseRejection.BAD_SIGNATURE, (result as LicenseCheck.Rejected).reason)
    }

    @Test
    fun everyRejectionCarriesAnArabicMessageThatIsNotWithheld() {
        LicenseRejection.entries.forEach { rejection ->
            assertTrue(
                "الرسالة العربية إلزامية لكل سبب رفض (${rejection.name})",
                rejection.messageArabic.length > 10
            )
        }
    }
}
