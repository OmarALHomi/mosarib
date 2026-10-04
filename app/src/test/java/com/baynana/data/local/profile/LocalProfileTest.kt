package com.baynana.data.local.profile

import com.baynana.features.settings.AppSetting
import com.baynana.features.settings.AppSettingDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس قاعدة واحدة: العلامة المحلية «أنا» لا تُرسل ولا تُعرض على غير صاحبها.
 * العطب الأصلي: وصلت في دعوة غرفة فقرأ الطرف الآخر «من: أنا».
 */
class LocalProfileTest {

    private class FakeSettingsDao(private val rows: MutableMap<String, String> = mutableMapOf()) : AppSettingDao {
        override fun getSetting(key: String): Flow<AppSetting?> = flowOf(rows[key]?.let { AppSetting(key, it) })
        override suspend fun getSettingValue(key: String): String? = rows[key]
        override fun getAllSettings(): Flow<List<AppSetting>> =
            flowOf(rows.map { (key, value) -> AppSetting(key, value) })
        override suspend fun saveSetting(setting: AppSetting) {
            rows[setting.key] = setting.value
        }

        fun stored(key: String): String? = rows[key]
    }

    @Test
    fun placeholderIsNeverSendable() {
        assertFalse(LocalProfile.isSendable(LocalProfile.PLACEHOLDER))
        assertEquals("", LocalProfile.wireName(LocalProfile.PLACEHOLDER))
        assertEquals(LocalProfile.UNKNOWN_LABEL, LocalProfile.peerLabel(LocalProfile.PLACEHOLDER))
    }

    @Test
    fun blankAndWhitespaceNamesAreRejected() {
        assertFalse(LocalProfile.isSendable(null))
        assertFalse(LocalProfile.isSendable(""))
        assertFalse(LocalProfile.isSendable("   "))
        assertEquals(LocalProfile.UNKNOWN_LABEL, LocalProfile.peerLabel("   "))
    }

    @Test
    fun overlongNameIsRejectedSoTheInviteStaysReadable() {
        assertFalse(LocalProfile.isSendable("ا".repeat(LocalProfile.MAX_LENGTH + 1)))
        assertTrue(LocalProfile.isSendable("ا".repeat(LocalProfile.MAX_LENGTH)))
    }

    @Test
    fun newlinesAreFlattenedBeforeJudging() {
        assertTrue(LocalProfile.isSendable("أبو سالم\nابنه"))
        assertEquals("أبو سالم ابنه", LocalProfile.wireName("أبو سالم\nابنه"))
        assertEquals("أبو سالم", LocalProfile.wireName("  أبو سالم  "))
    }

    @Test
    fun saveRejectsPlaceholderAndStoresTheRealName() = runBlocking {
        val dao = FakeSettingsDao()
        assertFalse(LocalProfile.save(dao, LocalProfile.PLACEHOLDER))
        assertNull(dao.stored(LocalProfile.NAME_KEY))

        assertTrue(LocalProfile.save(dao, "  أبو سالم  "))
        assertEquals("أبو سالم", dao.stored(LocalProfile.NAME_KEY))
        assertEquals("أبو سالم", LocalProfile.read(dao))
    }

    @Test
    fun storedPlaceholderReadsAsUnknownNotAsAName() = runBlocking {
        val dao = FakeSettingsDao(mutableMapOf(LocalProfile.NAME_KEY to LocalProfile.PLACEHOLDER))
        assertNull(LocalProfile.read(dao))
        assertEquals(LocalProfile.UNKNOWN_LABEL, LocalProfile.peerLabel(dao.getSettingValue(LocalProfile.NAME_KEY)))
    }
}
