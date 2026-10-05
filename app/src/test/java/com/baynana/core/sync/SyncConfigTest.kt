package com.baynana.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * إعداد القناة (ح٢٢): العنوان من أصل بنائي، وهوية الجهاز محلية.
 *
 * وأهمّ فحص هنا ليس في الشيفرة بل في **المستودع نفسه**: القالب المشحون في `assets/sync_endpoint.txt`
 * بلا عنوان ⇒ النسخة التي تُبنى من المستودع لا تتّصل بأي خادم إطلاقًا. وهذا ما يجعل «محلي أولًا»
 * حقيقة قابلة للقياس لا شعارًا: لا قناة إلا ببناء يعرف عنوانه.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncConfigTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * فحص الأصل المشحون: إمّا قالب بلا عنوان (وهو الأصل في المستودع)، وإمّا عنوان مؤهَّل بـhttps.
     * ولا ثالث: عنوان نصّي مكشوف أو مشوّه يُسقط الفحص، فلا يُشحن بناء يتكلّم خادمًا غير آمن.
     */
    @Test
    fun `الأصل المشحون إمّا بلا عنوان أو عنوان https مؤهَّل`() {
        val asset = SyncConfig.readAsset(context, SyncConfig.ENDPOINT_ASSET)
        val url = SyncConfig.changesUrl(asset)
        if (url != null) {
            assertTrue("العنوان المشحون يجب أن يكون https", url.startsWith("https://"))
            assertTrue("ويُبنى عليه مسار العقد", url.endsWith(SyncConfig.ENDPOINT_PATH))
        } else {
            assertFalse(
                "بلا عنوان لا قناة: التطبيق لا يتّصل بأي خادم افتراضي، وهذا هو الأصل",
                SyncConfig.isConfigured(context)
            )
        }
    }

    @Test
    fun `عنوان https يُبنى عليه مسار العقد`() {
        assertEquals(
            "https://sync.example.com/api/v1/changes",
            SyncConfig.changesUrl("https://sync.example.com")
        )
        assertEquals(
            "https://sync.example.com/api/v1/changes",
            SyncConfig.changesUrl("  https://sync.example.com/  \n")
        )
    }

    @Test
    fun `المسار المكرر في الأصل يُقلَّم بدل أن يتضاعف`() {
        assertEquals(
            "https://sync.example.com/api/v1/changes",
            SyncConfig.changesUrl("https://sync.example.com/api/v1/changes")
        )
        assertEquals(
            "https://sync.example.com/api/v1/changes",
            SyncConfig.changesUrl("https://sync.example.com/api/v1/changes/")
        )
    }

    @Test
    fun `النصّ المكشوف مرفوض ولو كتبه المالك`() {
        assertNull("التطبيق يمنع النصّ المكشوف أصلًا، فلا يُبنى له باب من هنا", SyncConfig.changesUrl("http://sync.example.com"))
        assertNull(SyncConfig.changesUrl("http://10.0.2.2:8787"))
    }

    @Test
    fun `العنوان غير المؤهَّل يُرفض بلا محاولة اتصال`() {
        assertNull(SyncConfig.changesUrl(null))
        assertNull(SyncConfig.changesUrl(""))
        assertNull(SyncConfig.changesUrl("   \n# تعليق فقط\n"))
        assertNull(SyncConfig.changesUrl("sync.example.com"))
        assertNull(SyncConfig.changesUrl("https://"))
        assertNull(SyncConfig.changesUrl("https://nohost"))
        assertNull(SyncConfig.changesUrl("https://bad host.example.com"))
    }

    @Test
    fun `السطر الاعتباري يُقرأ وأسطر التعليق تُتجاهل`() {
        val asset = "# خادم العائلة\nhttps://sync.family.example\n# سطر لاحق لا يُقرأ\n"
        assertEquals("https://sync.family.example/api/v1/changes", SyncConfig.changesUrl(asset))
    }

    @Test
    fun `معرّف الجهاز ثابت ولا يتغيّر بين التشغيلات`() {
        val first = SyncConfig.deviceId(context)
        val second = SyncConfig.deviceId(context)
        assertTrue(first.isNotBlank())
        assertEquals("تغيّر معرّف الجهاز يُفسد نسبة القيود لأصحابها", first, second)
    }

    @Test
    fun `الرمز غائب افتراضيًّا ولا يُولَّد تلقائيًّا`() {
        assertNull("لا رمز ⇒ لا اتصال: الرمز يأتي من المالك عند ضبط القناة", SyncConfig.deviceToken(context))
        assertFalse(SyncConfig.isConfigured(context))
    }

    @Test
    fun `الرمز يُقرأ من ضبط الجهاز ويُقلَّم`() {
        context.getSharedPreferences(SyncConfig.PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(SyncConfig.KEY_DEVICE_TOKEN, "  token-abc  ")
            .apply()

        assertEquals("token-abc", SyncConfig.deviceToken(context))
    }

    @Test
    fun `الرمز الفارغ أو المسافات يعني غير مضبوط`() {
        context.getSharedPreferences(SyncConfig.PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(SyncConfig.KEY_DEVICE_TOKEN, "   ")
            .apply()

        assertNull(SyncConfig.deviceToken(context))
    }
}
