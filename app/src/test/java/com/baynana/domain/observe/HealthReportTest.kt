package com.baynana.domain.observe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبار البوابات (ح٢٤) — القاعدة الأهم في هذا الملفّ: **المجهول ليس نجاحًا**.
 *
 * أكثر ما يكذب في تقارير الإطلاق أن يُكتب «سليم» لأن القياس لم يقع أصلًا (لا قناة، لا فحص تحديث).
 * هذه الاختبارات تثبّت الفرق بين الثلاث حالات، وتثبّت أن كل سقوط يحمل **ما العمل**.
 */
class HealthReportTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_767_225_600_000L

    /** جهاز «مثالي»: كل شيء واقع، فلا بوابة ساقطة ولا مجهولة. */
    private fun healthy() = HealthReport.Snapshot(
        now = now,
        rooms = 2,
        members = 4,
        activeEntries = 9,
        voidedEntries = 1,
        reversals = 1,
        drafts = 0,
        membersWithoutName = 0,
        pendingOutbox = 0,
        failedOutbox = 0,
        deadOutbox = 0,
        nextAttemptAt = 0L,
        lastSyncAt = now - 60_000,
        lastSyncError = "",
        channelConfigured = true,
        updateState = "أنت على أحدث إصدار",
        lastUpdateCheckAt = now - 60_000,
        lastBackupAt = now - day,
        migrationFileExported = true
    )

    @Test
    fun `جهاز مستعمل كما ينبغي لا بوابة ساقطة فيه`() {
        val report = HealthReport.build(healthy())
        assertEquals(emptyList<HealthReport.Gate>(), report.failed)
        assertEquals(emptyList<HealthReport.Gate>(), report.unknown)
        assertTrue(report.isHealthy)
        assertEquals("كل البوابات سليمة", report.verdictText())
    }

    @Test
    fun `بلا قناة مزامنة البوابة مجهولة لا سليمة ولا ساقطة`() {
        val report = HealthReport.build(healthy().copy(channelConfigured = false, lastSyncAt = 0L))
        assertTrue("لا شيء سقط", report.isHealthy)
        val gate = report.gates.first { it.id == "G3" }
        assertEquals(HealthReport.GateState.UNKNOWN, gate.state)
        assertTrue("يُقال بصراحة إنها لم تُقس", gate.measured.contains("لا قناة"))
        assertTrue("ومعه ما العمل", gate.action.contains("رمز الجهاز"))
        assertTrue(report.verdictText().contains("لم تُقس"))
    }

    @Test
    fun `مزامنة قديمة تُسقط بوابة المشاركة`() {
        val report = HealthReport.build(healthy().copy(lastSyncAt = now - 5 * day))
        val gate = report.gates.first { it.id == "G3" }
        assertEquals(HealthReport.GateState.FAIL, gate.state)
        assertTrue("العمل مذكور: قناة أو زر تحديث", gate.action.contains("حدّث الآن"))
        assertFalse(report.isHealthy)
    }

    @Test
    fun `سبب الحركة الميتة يُسمّى في البوابة إن لم يوجد خطأ مزامنة`() {
        val report = HealthReport.build(
            healthy().copy(deadOutbox = 1, lastSyncError = "", deadOutboxReason = "الصيغة مرفوضة من الخادم")
        )
        val gate = report.gates.first { it.id == "G2" }
        assertEquals(HealthReport.GateState.FAIL, gate.state)
        assertTrue("لا جملة عامّة بل السبب الحقيقي", gate.measured.contains("الصيغة مرفوضة من الخادم"))
    }

    @Test
    fun `فشل دائم بلا سبب ظاهر يُسقط بوابة الصمت`() {
        val report = HealthReport.build(healthy().copy(deadOutbox = 2, lastSyncError = "رمز الجهاز مرفوض"))
        val gate = report.gates.first { it.id == "G2" }
        assertEquals(HealthReport.GateState.FAIL, gate.state)
        assertTrue("السبب الظاهر مُدرج في القياس", gate.measured.contains("رمز الجهاز مرفوض"))
        assertTrue(gate.action.contains("حالة المزامنة"))
    }

    @Test
    fun `عضو بلا اسم يُسقط بوابة الأسماء ونصّ العمل يشرح لماذا`() {
        val report = HealthReport.build(healthy().copy(membersWithoutName = 1))
        val gate = report.gates.first { it.id == "G4" }
        assertEquals(HealthReport.GateState.FAIL, gate.state)
        assertTrue(gate.action.contains("اسم العضو"))
    }

    @Test
    fun `لا نسخة احتياطية أو نسخة قديمة تُسقط البوابة`() {
        val none = HealthReport.build(healthy().copy(lastBackupAt = 0L))
        assertEquals(HealthReport.GateState.FAIL, none.gates.first { it.id == "G5" }.state)
        assertTrue(none.gates.first { it.id == "G5" }.measured.contains("لا نسخة"))

        val stale = HealthReport.build(healthy().copy(lastBackupAt = now - 30 * day))
        assertEquals(HealthReport.GateState.FAIL, stale.gates.first { it.id == "G5" }.state)
    }

    @Test
    fun `التقرير المُشارَك لا يحمل مبلغًا ولا اسمًا ولا معرّف جهاز`() {
        val text = HealthReport.build(healthy()).shareText()
        listOf("farmer", "distributor", "memberId", "1500000", "deviceId", "token").forEach { forbidden ->
            assertFalse("لا يجوز أن يظهر «$forbidden» في التقرير", text.contains(forbidden))
        }
        assertTrue("وفيه تصريح صريح بعدم الإرسال", text.contains("لم يُرسل إلى أي جهة"))
    }

    @Test
    fun `كل سقوط يحمل عملًا مكتوبًا وكل مجهول يُعلن`() {
        val worst = HealthReport.build(
            healthy().copy(
                channelConfigured = false,
                lastSyncAt = 0L,
                deadOutbox = 1,
                lastSyncError = "سبب",
                membersWithoutName = 3,
                lastBackupAt = 0L,
                lastUpdateCheckAt = 0L,
                updateState = HealthReport.UPDATE_UNKNOWN
            )
        )
        assertTrue("السقوط له عمل", worst.failed.all { it.action.isNotBlank() })
        assertTrue("والمجهول لا يُعطى حكمًا زائفًا", worst.unknown.all { it.measured.isNotBlank() })
        assertTrue("الحكم يذكر الساقط أولًا", worst.verdictText().contains("ساقطة"))
        assertTrue("ويُذكر المجهول كذلك", worst.verdictText().contains("لم تُقس"))
    }
}
