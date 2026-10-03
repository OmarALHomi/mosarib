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
            "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                "VALUES ('room-1', 'm-me', 'أنا', '', 'distributor', 1, 1000, 0)"
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

        // 5) قيد بطرف ليس عضوًا في الغرفة مرفوض على قاعدة مرقّاة (حماية المخطط، لا اجتهاد الشاشة).
        var strangerRefused = false
        try {
            writable.execSQL(
                "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, createdAt, updatedAt) " +
                    "VALUES ('entry-3', 'room-1', 'op-3', 'PAYMENT', 'm-stranger', 'm-me', 5000, 'YER_NEW', 4000, '', '', 'SENT', 'm-me', NULL, NULL, NULL, 4000, 4000)"
            )
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            strangerRefused = true
        }
        assertTrue("طرف القيد يجب أن يكون عضوًا في غرفته", strangerRefused)

        // 6) الرفض لا الحذف المتسلسل: غرفة فيها قيود لا تُمحى.
        var refused = false
        try {
            writable.execSQL("DELETE FROM rooms WHERE id = 'room-1'")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            refused = true
        }
        assertTrue("حذف غرفة فيها قيود مرفوض (RESTRICT)", refused)
    }


    @Test
    fun `upgrading a real version 8 database adds tombstones and the retry column without losing anything`() {
        // الإصدار 8 كان آخر إصدار قبل ح٦: غرف وقيود وخارج دون جدول الشواهد ودون عمود إعادة المحاولة.
        buildDatabaseAtVersion(8) { sqlite ->
            sqlite.execSQL(
                "INSERT INTO rooms (id, kind, currency, title, status, linkCode, counterpartName, counterpartPhone, createdAt, updatedAt, closedAt) " +
                    "VALUES ('room-8', 'WATER', 'YER_NEW', 'غرفة قديمة', 'ACTIVE', 'LNK-8', 'أحمد', '777111222', 1000, 2000, NULL)"
            )
            sqlite.execSQL(
                "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                    "VALUES ('room-8', 'm-me', 'أنا', '', 'distributor', 1, 1000, 0)"
            )
            sqlite.execSQL(
                "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                    "VALUES ('room-8', 'm-other', 'أحمد', '', 'farmer', 0, 1000, 0)"
            )
            sqlite.execSQL(
                "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, reversesEntryId, createdAt, updatedAt) " +
                    "VALUES ('entry-8', 'room-8', 'op-8', 'WATER_SESSION', 'm-other', 'm-me', 1500000, 'YER_NEW', 2000, 'سقية قديمة', '', 'ACKNOWLEDGED', 'm-me', NULL, NULL, NULL, NULL, 2000, 2000)"
            )
            sqlite.execSQL(
                "INSERT INTO outbox (operationId, entityType, entityId, action, payload, state, attempts, lastError, createdAt, updatedAt) " +
                    "VALUES ('op-8', 'entry', 'entry-8', 'UPSERT', '{}', 'PENDING', 2, 'انقطاع شبكة', 2000, 2000)"
            )
            sqlite.execSQL("INSERT INTO sync_state (`key`, cursor, lastSyncAt, lastError) VALUES ('pull', '42', 3000, '')")
        }

        val database = openAppDatabase()

        // أول لمسة تشغّل AutoMigration(8→9): إضافة جدول الشواهد وعمود nextAttemptAt ثم التحقق من المخطط.
        assertEquals("الغرفة القديمة باقية", "ACTIVE", runBlocking { database.ledgerDao().getRoom("room-8")!!.status })
        assertEquals(1_500_000L, runBlocking { database.ledgerDao().getEntry("entry-8")!!.amountMinor })

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("القاعدة على الإصدار الحالي", DATABASE_VERSION, cursor.getInt(0))
        }

        // القيود المعلّقة بقيت بحالتها وعدّاد محاولاتها ولم تُصفَّر.
        readable.query("SELECT operationId, attempts, lastError, nextAttemptAt FROM outbox").use { cursor ->
            assertTrue("صف الخارج القديم موجود", cursor.moveToFirst())
            assertEquals("op-8", cursor.getString(0))
            assertEquals("عدد المحاولات محفوظ", 2, cursor.getInt(1))
            assertEquals("انقطاع شبكة", cursor.getString(2))
            assertEquals("العمود الجديد يبدأ صفرًا", 0L, cursor.getLong(3))
        }
        readable.query("SELECT cursor FROM sync_state WHERE `key` = 'pull'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("مؤشر السحب محفوظ", "42", cursor.getString(0))
        }

        // جدول الشواهد جديد وقابل للكتابة فعلًا (حاجز منع عودة القيد المحذوف).
        val writable = database.openHelper.writableDatabase
        writable.execSQL(
            "INSERT INTO tombstones (entityId, entityType, operationId, reason, deletedAt, recordedAt) " +
                "VALUES ('entry-9', 'entry', 'op-9', 'حُذف من الطرف الآخر', 4000, 4000)"
        )
        readable.query("SELECT entityId, reason FROM tombstones").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("entry-9", cursor.getString(0))
            assertEquals("السبب محفوظ لا مهمَل", "حُذف من الطرف الآخر", cursor.getString(1))
        }

        // ولا صف واحد ضاع من الجداول القديمة.
        readable.query("SELECT COUNT(*) FROM entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("قيد واحد كما كان", 1, cursor.getInt(0))
        }
    }


    @Test
    fun `upgrading a real version 9 database adds the settlement tables without touching the ledger`() {
        // الإصدار 9 كان آخر إصدار قبل ح٨: غرف وقيود وصندوق صادر وشواهد، وبلا جداول صلح.
        buildDatabaseAtVersion(9) { sqlite ->
            sqlite.execSQL(
                "INSERT INTO rooms (id, kind, currency, title, status, linkCode, counterpartName, counterpartPhone, createdAt, updatedAt, closedAt) " +
                    "VALUES ('room-9', 'MARKET', 'YER_NEW', 'غرفة قائمة', 'ACTIVE', 'LNK-9', 'صالح', '', 1000, 2000, NULL)"
            )
            sqlite.execSQL(
                "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                    "VALUES ('room-9', 'me', 'أنا', '', 'owner', 1, 1000, 0)"
            )
            sqlite.execSQL(
                "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                    "VALUES ('room-9', 'm-farmer', 'أحمد', '', 'farmer', 0, 1000, 0)"
            )
            sqlite.execSQL(
                "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, reversesEntryId, createdAt, updatedAt) " +
                    "VALUES ('entry-9', 'room-9', 'op-9', 'SETTLEMENT', 'm-farmer', 'me', 250000, 'YER_NEW', 2000, 'قيد قديم', '', 'SENT', 'me', NULL, NULL, NULL, NULL, 2000, 2000)"
            )
            sqlite.execSQL(
                "INSERT INTO outbox (operationId, entityType, entityId, action, payload, state, attempts, lastError, nextAttemptAt, createdAt, updatedAt) " +
                    "VALUES ('op-9', 'entry', 'entry-9', 'UPSERT', '{}', 'PENDING', 1, '', 0, 2000, 2000)"
            )
            sqlite.execSQL(
                "INSERT INTO tombstones (entityId, entityType, operationId, reason, deletedAt, recordedAt) " +
                    "VALUES ('entry-old', 'entry', 'op-old', 'حُذف قديمًا', 1500, 1500)"
            )
        }

        val database = openAppDatabase()

        // أول لمسة تشغّل AutoMigration(9→10): جداول الصلح تُضاف، والدفتر القديم لا يُلمس.
        assertEquals(250_000L, runBlocking { database.ledgerDao().getEntry("entry-9")!!.amountMinor })
        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(DATABASE_VERSION, cursor.getInt(0))
        }

        val found = mutableSetOf<String>()
        readable.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) found += cursor.getString(0)
        }
        val expected = setOf("deals", "deal_installments", "deal_commissions")
        assertTrue("جداول الصلح ناقصة بعد الترقية: ${expected - found}", found.containsAll(expected))

        // لا صف واحد ضاع من الدفتر: قيد، وسطر صادر، وشاهد واحد.
        readable.query("SELECT COUNT(*) FROM entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        readable.query("SELECT attempts FROM outbox WHERE operationId = 'op-9'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("محاولات الإرسال محفوظة", 1, cursor.getInt(0))
        }
        readable.query("SELECT COUNT(*) FROM tombstones").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        // والجداول الجديدة قابلة للكتابة فعلًا كما سيكتبها التطبيق.
        val writable = database.openHelper.writableDatabase
        writable.execSQL(
            "INSERT INTO deals (id, roomId, listingId, cropTitle, place, currency, totalMinor, advanceMinor, " +
                "sellerMemberId, sellerName, buyerMemberId, buyerName, brokerMemberId, brokerName, " +
                "commissionTotalMinor, commissionSellerMinor, commissionBuyerMinor, commissionPayer, " +
                "commissionRateBasisPoints, status, dealAt, firstDueAt, intervalDays, terms, createdByMemberId, " +
                "createdAt, updatedAt, closedAt) " +
                "VALUES ('deal-9', 'room-9', NULL, 'قمح', 'صنعاء', 'YER_NEW', 500000, 100000, 'm-farmer', 'أحمد', " +
                "'m-buyer', 'صالح', 'me', 'أنا', 12500, 0, 12500, 'BUYER', 250, 'PENDING', 2000, 3000, 30, '', 'me', 2000, 2000, NULL)"
        )
        writable.execSQL(
            "INSERT INTO deal_installments (id, dealId, seq, dueAt, amountMinor, paidMinor, status, createdAt, updatedAt) " +
                "VALUES ('ins-deal-9-1', 'deal-9', 1, 3000, 400000, 0, 'SCHEDULED', 2000, 2000)"
        )
        writable.execSQL(
            "INSERT INTO deal_commissions (id, dealId, roomId, memberId, payerMemberId, currency, totalMinor, entryId, status, createdAt, updatedAt) " +
                "VALUES ('fee-deal-9', 'deal-9', 'room-9', 'me', 'm-buyer', 'YER_NEW', 12500, NULL, 'OPEN', 2000, 2000)"
        )
        readable.query("SELECT totalMinor FROM deals WHERE id = 'deal-9'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("الصلح الجديد بالوحدة الصغرى", 500_000L, cursor.getLong(0))
        }

        // الغرفة التي فيها قيود وصلح لا تُمحى: RESTRICT يحمي تاريخ الطرفين على مستوى القاعدة.
        var roomRefused = false
        try {
            writable.execSQL("DELETE FROM rooms WHERE id = 'room-9'")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            roomRefused = true
        }
        assertTrue("غرفة فيها قيود وصلح لا تُمحى", roomRefused)
    }

    @Test
    fun `the exported schema of the current version is committed`() {
        // المخطط المصدَّر شرط لأي ترحيل قادم: غيابه يعني أن اختبار الترحيل القادم مستحيل،
        // وأن ترحيلًا تلقائيًا لاحقًا لا يجد ما يقارن به. لهذا يُسحب المخطط من CI ويُحفظ.
        assertTrue("مخطط الإصدار 6 مطلوب لبناء قاعدة قديمة حقيقية", schemaFile(6).exists())
        assertTrue("مخطط الإصدار 8 مطلوب لاختبار الترقية إلى 9", schemaFile(8).exists())
        assertTrue("مخطط الإصدار 9 مطلوب لاختبار الترقية إلى 10", schemaFile(9).exists())
        assertTrue(
            "مخطط الإصدار الحالي ($DATABASE_VERSION) يجب أن يكون محفوظًا في المستودع",
            schemaFile(DATABASE_VERSION).exists()
        )
    }
}
