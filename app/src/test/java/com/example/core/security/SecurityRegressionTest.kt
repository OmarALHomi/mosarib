package com.example.core.security

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

    private fun assertUnavailableCannotUnlock() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            var successes = 0
            var error: String? = null
            val request = BiometricHelper.authenticate(controller.get(), { successes++ }, { error = it })
            assertEquals(0, successes)
            assertNotNull(error)
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
