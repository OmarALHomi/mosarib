package com.baynana.core.security

import android.app.Activity
import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityRegressionTest {
    @Test
    @Config(sdk = [24])
    fun `android seven does not unlock when biometrics are unavailable`() = assertUnavailableCannotUnlock()

    @Test
    @Config(sdk = [27])
    fun `android eight does not unlock when biometrics are unavailable`() = assertUnavailableCannotUnlock()

    @Test
    fun `modern android also fails closed when biometrics are unavailable`() = assertUnavailableCannotUnlock()

    /**
     * اختبارات Robolectric تُبلّغ افتراضيًا أن المصادقة متاحة، لذلك تُحقن حالة «غير متاحة»
     * صراحةً. الاختبار يثبت السلوك عند غياب البصمة، لا سلوك الظل الافتراضي.
     */
    private fun assertUnavailableCannotUnlock() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            var successes = 0
            var error: String? = null
            val request = BiometricHelper.authenticate(
                controller.get(),
                { successes++ },
                { error = it },
                availabilityCheck = { false }
            )
            assertEquals("لا فتح بلا مصادقة ناجحة", 0, successes)
            assertNotNull("يجب إبلاغ المستخدم بسبب عدم الفتح", error)
            request.cancel()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    /**
     * مجرد بدء المصادقة لا يفتح شيئًا: النجاح لا يأتي إلا من callback النظام نفسه.
     * حتى لو أبلغ النظام أن البصمة متاحة، لا يجوز أن يُحسب الطلب ناجحًا بذاته.
     */
    @Test
    fun `starting biometric authentication never unlocks by itself`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            var successes = 0
            val request = BiometricHelper.authenticate(
                controller.get(),
                { successes++ },
                { /* أي رسالة خطأ مقبولة هنا؛ المهم ألا يُفتح */ },
                availabilityCheck = { true }
            )
            assertEquals("بدء التحقق ليس تحققًا ناجحًا", 0, successes)
            request.cancel()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `file provider exposes reports and backups but not arbitrary private files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authority = "${context.packageName}.fileprovider"
        val files = listOf(
            File(context.cacheDir, "reports/test.pdf"),
            File(context.filesDir, "backups/test.back"),
            File(context.filesDir, "private_test.txt"),
            File(context.cacheDir, "private_test.txt")
        )
        try {
            files.forEach { it.parentFile!!.mkdirs(); it.writeText("fixture") }
            assertNotNull(FileProvider.getUriForFile(context, authority, files[0]))
            assertNotNull(FileProvider.getUriForFile(context, authority, files[1]))
            assertThrows(IllegalArgumentException::class.java) { FileProvider.getUriForFile(context, authority, files[2]) }
            assertThrows(IllegalArgumentException::class.java) { FileProvider.getUriForFile(context, authority, files[3]) }
        } finally {
            files.forEach { it.delete() }
        }
    }
}
