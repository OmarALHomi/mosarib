package com.baynana.data.local.ledger

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.baynana.core.database.AppDatabase
import com.baynana.core.database.DATABASE_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة v6 §15: «الهجرة: قاعدة من كل إصدار 1–6 وتحديث للإصدار القادم — بيانات وأرصدة محفوظة
 * دون destructive migration».
 *
 * هذا الاختبار يبني قاعدة **حقيقية** بالإصدار 6 من المخطط المصدَّر نفسه، ثم يُشغّل الترحيل
 * (اليدوي + التلقائي 6→7) ويطلب من Room التحقق من المخطط الناتج. أي خطأ في الترحيل — أو أي
 * اختلاف بين SQL ومخطط Room — يُفشل الاختبار، وهو ما لا يكشفه فحص نصي.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val databaseName = "migration-ledger-test.db"

    /** الترحيل التلقائي 6→7 يولّده Room من فرق المخططات؛ يُجلب بالاسم لتفادي الاعتماد على ترتيب التوليد. */
    private fun autoMigration6to7(): Migration {
        val generatedName = "${AppDatabase::class.java.name}_AutoMigration_6_7"
        val clazz = try {
            Class.forName(generatedName)
        } catch (error: ClassNotFoundException) {
            throw AssertionError(
                "لم يولّد Room الترحيل التلقائي 6→7 ($generatedName). راجع autoMigrations في AppDatabase.",
                error
            )
        }
        return clazz.getDeclaredConstructor().newInstance() as Migration
    }

    @Test
    fun `upgrading a real version 6 database keeps its data and adds the ledger tables`() {
        // 1) قاعدة إصدار 6 بالمخطط المصدَّر، وفيها عميل وسقية قديمة.
        helper.createDatabase(databaseName, 6).use { old ->
            old.execSQL("INSERT INTO customers (id, name, phone, farmName, location, notes, isBeneficiary, createdAt, isArchived, linkCode) VALUES (1, 'أحمد المزارع', '777111222', '', '', '', 0, 1000, 0, 'ABC12')")
            old.execSQL("INSERT INTO water_sessions (id, customerId, pumpName, startTime, endTime, durationMinutes, pricePerHour, totalAmount, amountPaid, remainingDebt, notes, isLive, createdAt) VALUES (1, 1, 'البئر', 2000, 3000, 60, 5000, 10000, 2000, 5000, '', 0, 2000)")
            old.execSQL("INSERT INTO app_settings (`key`, `value`) VALUES ('distributor_name', 'المسرب')")
        }

        // 2) الترقية إلى الإصدار الحالي: يدوي 1→6 (لا شيء لازمة من 6) + التلقائي 6→7.
        val upgraded = helper.runMigrationsAndValidate(
            databaseName,
            DATABASE_VERSION,
            true,
            autoMigration6to7()
        )

        // 3) البيانات القديمة كما هي: لا مسح ولا إعادة بناء.
        upgraded.query("SELECT name, linkCode FROM customers WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("أحمد المزارع", cursor.getString(0))
            assertEquals("ABC12", cursor.getString(1))
        }
        upgraded.query("SELECT totalAmount, amountPaid, remainingDebt FROM water_sessions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(10000.0, cursor.getDouble(0), 0.0)
            assertEquals(2000.0, cursor.getDouble(1), 0.0)
            assertEquals(5000.0, cursor.getDouble(2), 0.0)
        }
        upgraded.query("SELECT `value` FROM app_settings WHERE `key` = 'distributor_name'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("المسرب", cursor.getString(0))
        }

        // 4) جداول الدفتر الجديدة موجودة وقابلة للاستخدام كما سيكتبها التطبيق.
        upgraded.execSQL(
            "INSERT INTO rooms (id, kind, currency, title, status, linkCode, counterpartName, counterpartPhone, createdAt, updatedAt, closedAt) " +
                "VALUES ('room-1', 'WATER', 'YER_NEW', 'غرفة الري', 'ACTIVE', 'LNK-1', 'أحمد', '', 1000, 1000, NULL)"
        )
        upgraded.execSQL(
            "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, createdAt, updatedAt) " +
                "VALUES ('entry-1', 'room-1', 'op-1', 'WATER_SESSION', 'm2', 'm1', 1000000, 'YER_NEW', 2000, 'سقية', '', 'SENT', 'm1', NULL, NULL, NULL, 2000, 2000)"
        )
        upgraded.query("SELECT amountMinor FROM entries WHERE id = 'entry-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("المبلغ مخزّن بالوحدة الصغرى كما هو", 1_000_000L, cursor.getLong(0))
        }

        // 5) فهرس operationId الفريد يمنع الازدواج على مستوى القاعدة نفسها.
        var duplicated = false
        try {
            upgraded.execSQL(
                "INSERT INTO entries (id, roomId, operationId, type, owedByMemberId, owedToMemberId, amountMinor, currency, occurredAt, description, quantityNote, status, createdByMemberId, sourceTable, sourceId, listingId, createdAt, updatedAt) " +
                    "VALUES ('entry-2', 'room-1', 'op-1', 'PAYMENT', 'm1', 'm2', 5000, 'YER_NEW', 3000, '', '', 'SENT', 'm1', NULL, NULL, NULL, 3000, 3000)"
            )
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            duplicated = true
        }
        assertTrue("معرّف العملية فريد: لا قيدان بنفس العملية", duplicated)
    }

    @Test
    fun `the exported schema for version 7 exists so future migrations can be validated`() {
        // المخطط المصدَّر شرط لأي ترحيل قادم: غيابه يعني أن اختبار الترحيل القادم مستحيل.
        val schema7 = java.io.File("schemas/com.baynana.core.database.AppDatabase/7.json")
            .takeIf { it.exists() }
            ?: java.io.File("app/schemas/com.baynana.core.database.AppDatabase/7.json")
        assertTrue("يجب أن يُصدَّر مخطط الإصدار 7 مع البناء", schema7.exists())
        assertNotNull(schema7.readText())
    }
}
