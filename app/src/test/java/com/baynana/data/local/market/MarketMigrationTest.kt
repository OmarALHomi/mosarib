package com.baynana.data.local.market

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.core.database.DATABASE_VERSION
import com.baynana.domain.market.ListingPrivacy
import com.baynana.domain.market.MarketEngine
import com.baynana.domain.market.MarketingRequestStatus
import com.baynana.domain.market.ModerationDecision
import com.baynana.domain.market.PriceMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * بوابة ح٩ (٢/٢) على الترقية: قاعدة **إصدار 10 حقيقية** فيها بيانات دفتر، تُفتح بـ Room على
 * الإصدار 11، فيُشغَّل الترحيل 10→11 ويتحقق Room من المخطط الناتج.
 *
 * الفحص ليس «هل بنى Gradle» بل ثلاثة أسئلة:
 * 1. هل نجت بيانات الدفتر القديمة كما هي؟
 * 2. هل أُنشئت جداول السوق الأربعة فعلًا؟
 * 3. هل **لا يوجد عمود هاتف** في جدول العروض (الخصوصية بنيوية في المخطط لا في الشاشة)؟
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarketMigrationTest {

    private val databaseName = "migration-market-test.db"
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

    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.baynana.core.database.AppDatabase/$version.json"
        return listOf(File(relative), File("app/$relative")).firstOrNull { it.exists() }
            ?: throw AssertionError("مخطط الإصدار $version غير موجود ($relative) — مُصدَّر Room شرط لأي اختبار ترحيل")
    }

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
    fun `الترقية_من_10_إلى_11_تُبقي_الدفتر_وتضيف_جداول_السوق_بلا_عمود_هاتف`() {
        buildDatabaseAtVersion(10) { sqlite ->
            // دفتر قائم: غرفة وعضو وقيد
            sqlite.execSQL(
                "INSERT INTO rooms (id, kind, currency, title, status, linkCode, counterpartName, " +
                    "counterpartPhone, createdAt, updatedAt, closedAt) VALUES " +
                    "('room-1','WATER','YER_NEW','مسرب الوادي','ACTIVE','K7X2M','أحمد','777123456',1000,1000,NULL)"
            )
            sqlite.execSQL(
                "INSERT INTO room_members (roomId, memberId, displayName, phone, role, isMe, joinedAt, lastSeenAt) " +
                    "VALUES ('room-1','me','أنا','','owner',1,1000,0)"
            )
            sqlite.execSQL(
                "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, " +
                    "currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, " +
                    "listingId, createdAt, updatedAt) VALUES " +
                    "('entry-1','room-1','op-1','WATER_SESSION','me','counterpart-room-1',5000000,'YER_NEW',2000," +
                    "'سقية 5 ساعات','','SENT','me',NULL,NULL,NULL,2000,2000)"
            )
        }

        val database = openAppDatabase()
        // أول لمسة: يُشغَّل الترحيل 10→11 ويتحقق Room من المخطط الناتج مقابل تعريف الإصدار 11.
        val rooms = runBlocking { database.ledgerDao().getAllRooms() }
        assertEquals(1, rooms.size)

        val readable = database.openHelper.readableDatabase
        readable.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(DATABASE_VERSION, cursor.getInt(0))
        }

        // 1) الدفتر القديم كما هو
        readable.query("SELECT title, counterpartName FROM rooms WHERE id='room-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("مسرب الوادي", cursor.getString(0))
            assertEquals("أحمد", cursor.getString(1))
        }
        readable.query("SELECT amountMinor, description FROM entries WHERE id='entry-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5_000_000L, cursor.getLong(0))
            assertEquals("سقية 5 ساعات", cursor.getString(1))
        }

        // 2) جداول السوق الأربعة موجودة
        val found = mutableSetOf<String>()
        readable.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) found += cursor.getString(0)
        }
        val expected = setOf("market_listings", "market_contacts", "market_requests", "market_moderations")
        assertTrue("جداول ناقصة: ${expected - found}", found.containsAll(expected))

        // 3) الخصوصية بنيوية في المخطط: لا عمود هاتف مزارع في جدول العروض، والهاتف في جدول الاتصال
        val listingColumns = mutableListOf<String>()
        readable.query("PRAGMA table_info(market_listings)").use { cursor ->
            while (cursor.moveToNext()) listingColumns += cursor.getString(1)
        }
        assertTrue(
            "جدول العروض يجب ألا يحمل عمود هاتف: $listingColumns",
            listingColumns.none { it.contains("phone", ignoreCase = true) && it != "brokerPhoneChoice" }
        )
        val contactColumns = mutableListOf<String>()
        readable.query("PRAGMA table_info(market_contacts)").use { cursor ->
            while (cursor.moveToNext()) contactColumns += cursor.getString(1)
        }
        assertTrue(contactColumns.contains("farmerPhone"))
    }

    @Test
    fun `السوق_يعمل_على_قاعدة_مرقّاة`() = runBlocking {
        buildDatabaseAtVersion(10) { }
        val database = openAppDatabase()
        val repository = MarketRepository(database)
        val now = 1_800_000_000_000L

        // طلب تسويق ثم قبول المزارع، ثم عرض ومصادقة على المراجعة، ثم نشر.
        repository.requestMarketing("req-1", "farmer-أحمد", "me", "رمان", now = now)
        repository.decideRequest("req-1", "farmer-أحمد", MarketingRequestStatus.ACCEPTED)

        val draft = MarketEngine.Draft(
            id = "listing-1",
            brokerMemberId = "me",
            farmerMemberId = "farmer-أحمد",
            farmerName = "أحمد",
            farmerPhone = "777123456",
            brokerName = "الدلال سالم",
            title = "رمان صنف ممتاز",
            cropType = "رمان",
            priceMode = PriceMode.FIXED,
            priceMinor = 3_500_000L,
            unit = "SACK",
            location = ListingPrivacy.PrivateLocation(governorate = "صنعاء", village = "الحصن"),
            createdAt = now
        )
        repository.save(draft, actorMemberId = "me", isFarmer = false, changesPriceOrBody = true, now = now)
        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        assertTrue(repository.publish("listing-1") is MarketRepository.SaveResult.Saved)

        val public = repository.publicListings().single()
        val text = MarketEngine.publicText(public)
        assertTrue("العرض العام بلا هاتف مزارع", !text.contains("777123456"))
        assertTrue("الموقع المنشور محافظة فقط", text.contains("صنعاء") && !text.contains("الحصن"))
        assertEquals(1, database.marketDao().publicCount())
    }
}
