package com.baynana.data.local.ledger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.core.database.DATABASE_VERSION
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * بوابة v6 §15: «الهجرة: قاعدة من كل إصدار 1–6 وتحديث للإصدار القادم — بيانات وأرصدة محفوظة
 * دون destructive migration».
 *
 * الطريقة: نبني قاعدة **إصدار 6 حقيقية** من ملف المخطط المصدَّر نفسه (جداول + فهارس + بصمة
 * الهوية من `setupQueries`)، ثم نزرع فيها بيانات قديمة، ثم نفتحها بـ Room على الإصدار الحالي.
 * Room هو من يشغّل الترحيل 6→7 ثم **يتحقق من المخطط الناتج** مقابل تعريفات الإصدار 7؛ فإن
 * نقص جدول أو عمود أو فهرس، أو اختلّ شيء، فتح الفتحة يُفشل الاختبار بنفسه.
 *
 * لا نستخدم MigrationTestHelper لأنه يقرأ المخططات من assets، وAGP لا يدمج assets الخاصة
 * بمجموعة اختبارات الوحدة (فشل فعلي في CI)، فبنينا القاعدة من الملف مباشرة.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerMigrationTest {

    private val databaseName = "migration-ledger-test.db"
    private lateinit var context: Context
    private var opened: AppDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(databaseName).delete()
    }

    @After
    fun tearDown() {
        opened?.close()
        opened = null
        context.getDatabasePath(databaseName).delete()
    }

    /** ملف المخطط المصدَّر: مجلد العمل في اختبارات الوحدة هو جذر الوحدة، لكن نجرّب الاثنين. */
    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.baynana.core.database.AppDatabase/$version.json"
        return listOf(File(relative), File("app/$relative")).firstOrNull { it.exists() }
            ?: throw AssertionError("مخطط الإصدار $version غير موجود ($relative) — مُصدَّر Room شرط لأي اختبار ترحيل")
    }

    /** يبني قاعدة إصدار [version] على القرص من المخطط المصدَّر كما ولّده Room بالضبط. */
    private fun buildDatabaseAtVersion(version: Int, seed: (SQLiteDatabase) -> Unit) {
        val file = schemaFile(version)
        val database = JSONObject(file.readText()).getJSONObject("database")
        val target = context.getDatabasePath(databaseName)
        target.parentFile?.mkdirs()
        target.delete()

        val sqlite = SQLiteDatabase.openOrCreateDatabase(target, null)
        try {
            val entities = database.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            database.optJSONArray("setupQueries")?.let { queries ->
                for (i in 0 until queries.length()) sqlite.execSQL(queries.getString(i))
            }
            seed(sqlite)
            sqlite.version = version
        } finally {
            sqlite.close()
        }
    }

    private fun openAppDatabase(): AppDatabase {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        opened = database
        return database
    }

    @Test
    fun `upgrading a real version 6 database keeps its data and adds the ledger tables`() {
        buildDatabaseAtVersion(6) { sqlite ->
            sqlite.execSQL(
                "INSERT INTO customers (id, name, phone, farmName, location, notes, customPricePerHour, isBeneficiary, createdAt, isArchived, linkCode) " +
                    "VALUES (1, 'أحمد المزارع', '777111222', 'الجربة', 'صنعاء', '', 5000.0, 1, 1000, 0, 'ABC12')"
            )
            sqlite.execSQL(
                "INSERT INTO water_sessions (id, customerId, pumpName, startTime, endTime, durationMinutes, pricePerHour, totalAmount, amountPaid, remainingDebt, notes, isLive, billedToCustomerId, createdAt) " +
                    "VALUES (1, 1, 'البئر', 2000, 5000, 60, 5000.0, 10000.0, 2000.0, 5000.0, '', 0, NULL, 2000)"
            )
            sqlite.execSQL("INSERT INTO app_settings (`key`, `value`) VALUES ('distributor_name', 'المسرب')")
        }

        val database = openAppDatabase()

        // أول لمسة لـ Room تفتح القاعدة: تشغيل الترحيل 6→7 ثم التحقق من المخطط مقابل الإصدار 7.
        assertNull("لا غرف في قاعدة قديمة", runBlocking { database.ledgerDao().getRoom("room-1") })

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("القاعدة على الإصدار 7", DATABASE_VERSION, cursor.getInt(0))
        }

        // 1) البيانات القديمة كما هي: لا مسح ولا إعادة بناء.
        readable.query("SELECT name, linkCode FROM customers WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("أحمد المزارع", cursor.getString(0))
            assertEquals("ABC12", cursor.getString(1))
        }
        readable.query("SELECT totalAmount, amountPaid, remainingDebt FROM water_sessions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(10000.0, cursor.getDouble(0), 0.0)
            assertEquals(2000.0, cursor.getDouble(1), 0.0)
            assertEquals(5000.0, cursor.getDouble(2), 0.0)
        }
        readable.query("SELECT `value` FROM app_settings WHERE `key` = 'distributor_name'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("المسرب", cursor.getString(0))
        }

        // 2) كل جداول الدفتر السبعة أُنشئت.
        val expectedTables = setOf(
            "rooms", "room_members", "entries", "entry_allocations",
            "acknowledgements", "outbox", "sync_state"
        )
        val found = mutableSetOf<String>()
        readable.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) found += cursor.getString(0)
        }
        assertTrue("جداول ناقصة بعد الترحيل: ${expectedTables - found}", found.containsAll(expectedTables))

        // 3) الجداول الجديدة قابلة للاستخدام كما سيكتبها التطبيق (المبلغ بالوحدة الصغرى).
        val writable = database.openHelper.writableDatabase
        writable.execSQL(
            "INSERT INTO rooms (id, kind, currency, title, status, linkCode, counterpartName, counterpartPhone, createdAt, updatedAt, closedAt) " +
                "VALUES ('room-1', 'WATER', 'YER_NEW', 'غرفة الري', 'ACTIVE', 'LNK-1', 'أحمد', '', 1000, 1000, NULL)"
        )
        writable.execSQL(
            "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                "VALUES ('room-1', 'm-other', 'أحمد', '', 'farmer', 0, 1000, 0)"
        )
        writable.execSQL(
            "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, createdAt, updatedAt) " +
                "VALUES ('entry-1', 'room-1', 'op-1', 'WATER_SESSION', 'm-other', 'm-me', 1000000, 'YER_NEW', 2000, 'سقية 5 ساعات', '', 'SENT', 'm-me', NULL, NULL, NULL, 2000, 2000)"
        )
        readable.query("SELECT amountMinor FROM entries WHERE id = 'entry-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("المبلغ مخزّن بالوحدة الصغرى (فلس)", 1_000_000L, cursor.getLong(0))
        }

        // 4) حاجز منع الازدواج على مستوى القاعدة نفسها: operationId فريد.
        var duplicated = false
        try {
            writable.execSQL(
                "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, createdAt, updatedAt) " +
                    "VALUES ('entry-2', 'room-1', 'op-1', 'PAYMENT', 'm-me', 'm-other', 5000, 'YER_NEW', 3000, '', '', 'SENT', 'm-me', NULL, NULL, NULL, 3000, 3000)"
            )
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            duplicated = true
        }
        assertTrue("معرّف العملية فريد: لا قيدان بنفس العملية", duplicated)

        // 5) الرفض لا الحذف المتسلسل: غرفة فيها قيود لا تُمحى.
        var refused = false
        try {
            writable.execSQL("DELETE FROM rooms WHERE id = 'room-1'")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            refused = true
        }
        assertTrue("حذف غرفة فيها قيود مرفوض (RESTRICT)", refused)
    }

    @Test
    fun `schema export is wired so migrations can be validated`() {
        // المخطط المصدَّر شرط لأي ترحيل قادم: غيابه يعني أن اختبار الترحيل القادم مستحيل.
        // يُبنى اختبار الترحيل أعلاه من هذا الملف نفسه، فهو حجر الأساس لهذا الاختبار.
        assertTrue("مخطط الإصدار 6 مطلوب لبناء قاعدة قديمة حقيقية", schemaFile(6).exists())
    }
}
