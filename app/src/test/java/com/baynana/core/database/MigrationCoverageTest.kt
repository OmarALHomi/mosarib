package com.baynana.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.room.migration.Migration

/**
 * حواجز ثابتة على قاعدة البيانات، تعمل على JVM بلا محاكي:
 *
 * 1. اسم القاعدة ثابت (تغييره يقطع وصول المستخدمين لدفاترهم) — قرار موثّق لا يُنقض سهوًا.
 * 2. الترحيلات متصلة من الإصدار 1 إلى الإصدار الحالي بلا فجوات.
 * 3. لا يوجد أي مسار ترحيل مدمّر (فحص نصي على المصدر نفسه).
 */
class MigrationCoverageTest {

    @Test
    fun databaseNameNeverChanges() {
        assertEquals("water_distributor_db", AppDatabase.DATABASE_NAME)
    }

    @Test
    fun migrationsCoverEveryVersionWithoutGaps() {
        val migrations = AppDatabase.ALL_MIGRATIONS
        assertTrue("لا توجد ترحيلات مسجلة", migrations.isNotEmpty())

        val expectedStart = 1
        var cursor = expectedStart
        migrations.sortedBy { it.startVersion }.forEach { migration: Migration ->
            assertEquals(
                "فجوة في الترحيلات: التالي يجب أن يبدأ من الإصدار $cursor",
                cursor,
                migration.startVersion
            )
            assertEquals(
                "الترحيل يجب أن ينتهي عند الإصدار التالي مباشرة",
                migration.startVersion + 1,
                migration.endVersion
            )
            cursor = migration.endVersion
        }

        assertEquals(
            "إصدار قاعدة البيانات يجب أن يساوي نهاية آخر ترحيل",
            DATABASE_VERSION,
            cursor
        )
    }

    @Test
    fun noDestructiveMigrationInSource() {
        val source = java.io.File("src/main/java/com/baynana/core/database/AppDatabase.kt")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/java/com/baynana/core/database/AppDatabase.kt")
        assertTrue("لم يُعثر على ملف AppDatabase.kt لفحصه", source.exists())

        // تُستثنى سطور التعليقات، لأن ذكر الاسم في شرح القاعدة ليس استخدامًا لها.
        val code = source.readText().lines()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
            }
            .joinToString("\n")
        assertTrue(
            "ممنوع استخدام الترحيل الاحتياطي المدمّر: يفقد بيانات المستخدم صامتًا",
            !code.contains("fallbackToDestructive" + "Migration")
        )
        assertTrue(
            "يجب إضافة كل الترحيلات عبر ALL_MIGRATIONS حتى يراها الاختبار",
            code.contains("addMigrations(*ALL_MIGRATIONS.toTypedArray())")
        )
    }
}
