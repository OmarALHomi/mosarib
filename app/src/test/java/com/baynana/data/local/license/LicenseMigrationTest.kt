package com.baynana.data.local.license

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.core.database.DATABASE_VERSION
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
 * بوابة ح١٣ على الترقية: قاعدة **إصدار 11 حقيقية** فيها صفّ دفتر، تُفتح بـRoom على الإصدار 12
 * فيُشغَّل الترحيل 11→12 ويتحقق Room من المخطط الناتج.
 *
 * ثلاثة أسئلة، لا سؤال واحد:
 * 1. هل نجا صفّ الدفتر القديم كما هو؟ (الترحيل إضافة لا مسّ)
 * 2. هل جدولا الترخيص موجودان فعلًا؟
 * 3. هل **المفتاح الأساسي** هو مانع الازدواج؟ نُدرج المفتاح نفسه مرتين ونتوقّع رفضًا من SQLite
 *    لا من طبقة Kotlin — فالضمانة يجب أن تكون في القاعدة، فلا يسقطها مسار كتابة جديد يُسهى به.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseMigrationTest {

    private val databaseName = "migration-license-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(databaseName).delete()
    }

    @After
    fun tearDown() {
        context.getDatabasePath(databaseName).delete()
    }

    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.baynana.core.database.AppDatabase/$version.json"
        return listOf(File(relative), File("app/$relative")).firstOrNull { it.exists() }
            ?: throw AssertionError(
                "مخطط الإصدار $version غير موجود ($relative) — مُصدَّر Room شرط لأي اختبار ترحيل"
            )
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
                    sqlite.execSQL(
                        indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)
                    )
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

    private fun openWithRoom(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS.toTypedArray())
            .allowMainThreadQueries()
            .build()

    private fun rawDatabase(): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            context.getDatabasePath(databaseName).path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )

    /** صفّ دفتر قديم: غرفة وقيد بمبلغ 1,500,000 فلس — وهذا ما يجب أن ينجو من الترقية. */
    private fun seedLegacyLedger(sqlite: SQLiteDatabase) {
        sqlite.execSQL(
            """
            INSERT INTO rooms (id, kind, currency, title, status, linkCode, counterpartName,
                counterpartPhone, createdAt, updatedAt, closedAt)
            VALUES ('room-license-1', 'GENERAL', 'YER_NEW', 'غرفة قديمة', 'ACTIVE', 'link-1', '', '', 1, 2, NULL)
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId,
                amountMinor, currency, occurredAt, description, quantityNote, status,
                createdByMemberId, sourceTable, sourceId, listingId, reversesEntryId, createdAt, updatedAt)
            VALUES ('entry-license-1', 'room-license-1', 'op-license-1', 'DEBT', 'm1', 'm2',
                1500000, 'YER_NEW', 10, 'قيد قديم', '', 'ACKNOWLEDGED',
                'm1', NULL, NULL, NULL, NULL, 10, 10)
            """.trimIndent()
        )
    }

    @Test
    fun upgradeFromElevenToCurrentKeepsTheLedgerAndAddsLicenseTables() {
        // الاختبار لا يثبّت رقمًا: الترقية تُقاس إلى **الإصدار الحالي** أيًّا كان، فكل حزمة ترفع
        // الإصدار (١٢ ثم ١٣ ح١٩) تمرّ من هنا بلا تعديل ولا «تحديث رقم» صامت.
        assertTrue("الإصدار الحالي يجب أن يكون 12 أو أحدث", DATABASE_VERSION >= 12)
        buildDatabaseAtVersion(11) { sqlite -> seedLegacyLedger(sqlite) }

        // ١) Room نفسه يرحّل ويتحقق من المخطط الناتج (مقارنة البصمة)، وإن اختلف المخطط يفشل البناء.
        val room = openWithRoom()
        try {
            assertEquals(
                "Room فتح القاعدة على الإصدار الحالي",
                DATABASE_VERSION,
                room.openHelper.writableDatabase.version
            )
        } finally {
            room.close()
        }

        // ٢) فحص الملف مباشرة: الجدولان حاضران، والصفّ القديم كما هو، والمفتاح فريد حقًّا.
        val raw = rawDatabase()
        try {
            val tables = mutableListOf<String>()
            raw.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
            assertTrue("جدول licenses مُنشأ", tables.contains("licenses"))
            assertTrue("جدول license_events مُنشأ", tables.contains("license_events"))

            raw.rawQuery(
                "SELECT amountMinor, description FROM entries WHERE id='entry-license-1'",
                null
            ).use { cursor ->
                assertTrue("صفّ الدفتر القديم ما زال موجودًا", cursor.moveToFirst())
                assertEquals("المبلغ لم يُمَس", 1500000L, cursor.getLong(0))
                assertEquals("الوصف لم يُمَس", "قيد قديم", cursor.getString(1))
            }

            insertLicense(raw, licenseId = "signed:dup", tokenSha = "hash-one")
            var refused = false
            try {
                insertLicense(raw, licenseId = "signed:dup", tokenSha = "hash-two")
            } catch (expected: SQLiteConstraintException) {
                refused = true
            }
            assertTrue("مفتاح الاستحقاق فريد في القاعدة: لا تمديد ثانٍ ممكن", refused)

            raw.execSQL(
                """
                INSERT INTO license_events (id, licenseId, occurredAt, outcome, reason, role, plan,
                    deviceCode, expiresAt, message, tokenPrefix, expiresAtBefore, expiresAtAfter)
                VALUES ('ev-1', 'signed:dup', 5, 'GRANTED', '', 'MUSRIB', 'MONTHLY',
                    'MSRB11112222', 2, 'قُبل التصريح', 'BNNA1.abc…', 0, 2)
                """.trimIndent()
            )
        } finally {
            raw.close()
        }

        // ٣) ثم يعود Room ليقرأ ما كتبناه من الطرف الآخر: الجدول مفهوم من المستودع لا من الملف فقط.
        val reopened = openWithRoom()
        try {
            val redeemed = runBlocking { reopened.licenseDao().redeemedKeys() }
            assertTrue("Room يقرأ جدول الترخيص بعد الترقية", redeemed.contains("signed:dup"))
        } finally {
            reopened.close()
        }
    }

    private fun insertLicense(raw: SQLiteDatabase, licenseId: String, tokenSha: String) {
        raw.execSQL(
            """
            INSERT INTO licenses (licenseId, kind, deviceCode, role, plan, durationDays,
                issuedAt, expiresAt, grantedAt, tokenSha256, note)
            VALUES ('$licenseId', 'SIGNED', 'MSRB11112222', 'MUSRIB', 'MONTHLY', 30,
                1, 2, 1, '$tokenSha', '')
            """.trimIndent()
        )
    }
}
