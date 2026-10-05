package com.baynana.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * إعداد قناة المزامنة من الجهاز (ح٢٢ب) — والأهمّ فيه: **الخلط بين الرفض والتعذّر**.
 *
 * لو قيل للمستخدم «الرمز مرفوض» لأن الشبكة مقطوعة، لبدّل رمزًا صحيحًا وأفسد إعداده. ولو قيل «تعذّر
 * الوصول» لرمز خاطئ، لأعاد المحاولة أبدًا ولم يصلح شيئًا. فهذان الحكمان يجب ألّا يختلطا.
 */
class SyncTokenSetupTest {

    @Test
    fun `جولة بلا أخطاء تعني القبول`() {
        val outcome = SyncTokenSetup.interpret(errors = emptyList(), applied = 3)
        assertTrue(outcome is SyncTokenSetup.Outcome.Accepted)
        assertTrue(SyncTokenSetup.shouldSave(outcome))
        assertFalse("المقبول لا يُعاد عليه", SyncTokenSetup.shouldRetry(outcome))
        assertTrue("والرسالة تُخبر بما وصل", SyncTokenSetup.message(outcome).contains("3"))
    }

    @Test
    fun `الرمز المرفوض لا يُحفظ ويُطلب إصلاحه`() {
        val outcome = SyncTokenSetup.interpret(listOf("رمز الجهاز غير صالح"), applied = 0)
        assertTrue(outcome is SyncTokenSetup.Outcome.Refused)
        assertFalse("لا حفظ لرمز مرفوض", SyncTokenSetup.shouldSave(outcome))
        assertFalse("والرفض الدائم لا يُعاد تلقائيًّا", SyncTokenSetup.shouldRetry(outcome))
        val message = SyncTokenSetup.message(outcome)
        assertTrue(message.contains("مرفوض"))
        assertTrue("ويُوجَّه المستخدم إلى المالك لا إلى التخمين", message.contains("المالك"))
    }

    @Test
    fun `انقطاع الشبكة تعذّر لا رفض`() {
        val outcome = SyncTokenSetup.interpret(listOf("تعذّر الوصول إلى الخادم"), applied = 0)
        assertTrue("بلا شبكة ليس رفضًا للرمز", outcome is SyncTokenSetup.Outcome.Unreachable)
        assertTrue("بل يُعاد", SyncTokenSetup.shouldRetry(outcome))
        assertFalse(SyncTokenSetup.shouldSave(outcome))
        assertTrue(
            "والرسالة تمنع المستخدم من تبديل رمز صحيح",
            SyncTokenSetup.message(outcome).contains("الرمز لم يُرفض")
        )
    }

    @Test
    fun `السبب الفارغ يُسمّى ولا يُخترع له تفسير`() {
        val outcome = SyncTokenSetup.interpret(listOf("   "), applied = 0)
        assertTrue(outcome is SyncTokenSetup.Outcome.Refused)
        assertTrue(
            "لا نقول «رُفض الرمز» بلا سبب مكتوب",
            (outcome as SyncTokenSetup.Outcome.Refused).reason.contains("بلا سبب مكتوب")
        )
    }

    @Test
    fun `السبب غير المصنَّف يُعالَج مؤقّتًا لا رفضًا`() {
        // قاعدة مقصودة: إعادة المحاولة أهون من إقناع المستخدم بأن رمزه الصحيح خاطئ.
        val outcome = SyncTokenSetup.classify("حدث شيء غريب على السلك")
        assertTrue(outcome is SyncTokenSetup.Outcome.Unreachable)
    }
}
