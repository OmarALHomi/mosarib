package com.baynana.domain.migration

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * الترحيل بجرد مطابق (ح٢٣) — بوابته: **صفر فرق في الجرد**.
 *
 * والمتجهات هنا ليست مكتوبة بيد: هي **مخرَج أداة Node الحقيقية** (`tools/migrate.mjs`) بعد أن
 * استوردت الأحداث عبر عقد `v1` نفسه وسحبتها من الخادم المرجعي. فالاختبار يقيس أمرًا واحدًا مهمًّا:
 * أن Kotlin وNode يعطيان **الجرد نفسه** و**نصوص الفروق نفسها**. وهذا هو الشرط الذي يجعل «صفر فرق»
 * قابلًا للقياس بدل أن يكون شعارًا: لو انحرف أحد الطرفين في قاعدة حساب (مثل هل يُستثنى القيد
 * العكسي) لسقط الفحص هنا قبل أن يظهر الفرق في دفتر عائلة.
 *
 * (يحتاج Robolectric لأن `org.json` تنفيذ أندرويد — نفس ما يشتغل في الإنتاج.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerMigrationTest {

    private val sourceInventory: LedgerMigration.Inventory =
        LedgerMigration.inventoryFromJson(JSONObject(MigrationVectors.SOURCE_INVENTORY))
    private val targetInventory: LedgerMigration.Inventory =
        LedgerMigration.inventoryFromJson(JSONObject(MigrationVectors.TARGET_INVENTORY))

    private fun events(): List<LedgerMigration.Event> {
        val array = JSONArray(MigrationVectors.EVENTS)
        val result = ArrayList<LedgerMigration.Event>(array.length())
        for (index in 0 until array.length()) {
            val json = array.getJSONObject(index)
            result += LedgerMigration.Event(
                operationId = json.getString("operationId"),
                entityType = json.getString("entityType"),
                entityId = json.getString("entityId"),
                action = json.getString("action"),
                payload = json.getString("payload"),
                createdAt = json.getLong("createdAt")
            )
        }
        return result
    }

    private fun expected(arrayJson: String): List<String> {
        val array = JSONArray(arrayJson)
        val result = ArrayList<String>(array.length())
        for (index in 0 until array.length()) result += array.getString(index)
        return result
    }

    // ---------------------------------------------------------------- الجرد

    @Test
    fun `جرد التطبيق من الأحداث يطابق جرد أداة Node حرفيًّا`() {
        val built = LedgerMigration.inventoryJson(LedgerMigration.inventory(events()))
        assertEquals(
            "انحراف قاعدة حساب بين التطبيق والأداة يعني جردًا كاذبًا",
            JSONObject(MigrationVectors.SOURCE_INVENTORY).toString(),
            built.toString()
        )
    }

    @Test
    fun `الحصيلة اليدوية كما هي في المتجهات`() {
        // سقية 1,500,000 + سداد 1,000,000 نشطة، وسقية ثانية 2,500,000 ملغاة بقيد عكسي.
        assertEquals(2, sourceInventory.activeTotal)
        assertEquals(1, sourceInventory.voidedTotal)
        assertEquals(1, sourceInventory.reversalsTotal)
        assertEquals(0, sourceInventory.drafts)
        val room = sourceInventory.rooms.single()
        assertEquals("room-water-1", room.roomId)
        assertEquals("YER_NEW", room.currency)
        assertEquals(2, room.active)
        assertEquals(1_500_000L, room.byType.getValue("WATER_SESSION").sumMinor)
        assertEquals(1, room.byType.getValue("WATER_SESSION").count)
        assertEquals(1_000_000L, room.byType.getValue("PAYMENT").sumMinor)
        // صافي المزارع: −1,500,000 (سقية) +1,000,000 (سداد) = −500,000، والموزّع +500,000.
        assertEquals(-500_000L, room.netByMember.getValue("farmer"))
        assertEquals(500_000L, room.netByMember.getValue("distributor"))
    }

    @Test
    fun `المسودة تُعدّ ولا تدخل أي رقم يُقارَن`() {
        val withDraft = LedgerMigration.inventoryFromJson(JSONObject(MigrationVectors.INVENTORY_WITH_DRAFT))
        assertEquals(1, withDraft.drafts)
        assertEquals(0, sourceInventory.drafts)
        assertEquals(
            "المسودة لم تُشارك فلا تُغيّر صافيًا ولا نوعًا",
            sourceInventory.rooms.single().netByMember,
            withDraft.rooms.single().netByMember
        )
        assertEquals(sourceInventory.rooms.single().byType, withDraft.rooms.single().byType)
    }

    // ---------------------------------------------------------------- المقارنة

    @Test
    fun `جرد المصدر يطابق جرد ما بعد الاستيراد بلا فرق`() {
        val diff = LedgerMigration.compare(sourceInventory, targetInventory)
        assertEquals("صفر فرق هو شرط الترحيل", 0, diff.differences.size)
        assertTrue(diff.isClean)
    }

    @Test
    fun `مقارنة الأداة ومقارنة التطبيق تعطيان النصوص نفسها`() {
        // نفس النصوص بالبايت: هذا ما يجعل تقرير الترحيل صادقًا في الجهتين.
        val dropped = LedgerMigration.compare(
            LedgerMigration.inventory(events().filter { it.operationId != "op-payment-1" }),
            targetInventory
        )
        assertEquals(expected(MigrationVectors.DROPPED_PAYMENT_DIFFERENCES), dropped.differences)
        assertTrue("المفقود يُسمّى باسمه", dropped.differences.any { it.contains("op-payment-1") })
        assertTrue("وأثره في الصافي يُذكر", dropped.differences.any { it.contains("صافي العضو") })

        val changed = LedgerMigration.compare(
            LedgerMigration.inventory(
                events().map { event ->
                    if (event.operationId == "op-water-1") {
                        event.copy(payload = event.payload.replace("\"1500000\"", "\"1400000\""))
                    } else event
                }
            ),
            targetInventory
        )
        assertEquals(expected(MigrationVectors.CHANGED_AMOUNT_DIFFERENCES), changed.differences)
        assertTrue(changed.differences.any { it.contains("المجموع") })

        // غرفة زائدة عند المصدر: تُسمّى بالاسم، وهذا ما يمنع «ترحيلًا» يخلط غرفتين.
        val extraRoom = LedgerMigration.compare(
            LedgerMigration.inventoryFromJson(JSONObject(MigrationVectors.EXTRA_ROOM_SOURCE_INVENTORY)),
            targetInventory
        )
        assertEquals(expected(MigrationVectors.EXTRA_ROOM_DIFFERENCES), extraRoom.differences)
        assertTrue(extraRoom.differences.any { it.contains("غرفة") })

        // والعكس: قيد ناقص عند الوجهة يُقال «ناقصة» لأنه هو ما يُقلق صاحب الدفتر.
        val missing = LedgerMigration.compare(sourceInventory, LedgerMigration.inventory(events().filter { it.operationId != "op-water-1" }))
        assertTrue(missing.differences.any { it.contains("ناقصة") })
    }

    @Test
    fun `الفرق يُذكر كاملًا لا عند أول اختلاف`() {
        val missingTwo = LedgerMigration.compare(
            LedgerMigration.inventory(events().filter { it.operationId != "op-payment-1" && it.operationId != "op-water-1" }),
            targetInventory
        )
        assertTrue("من ينقل دفترًا يريد كل الاختلالات في جلسة واحدة", missingTwo.differences.size >= 2)
    }

    // ---------------------------------------------------------------- الملفّ

    @Test
    fun `تصدير الأحداث ثم قراءتها يعطي الجرد نفسه`() {
        val text = LedgerMigration.exportText(events(), exportedAt = 1_767_225_600_000, deviceId = "device-1")
        val parsed = LedgerMigration.parse(text)
        assertEquals(events().size, parsed.events.size)
        assertEquals("device-1", parsed.deviceId)
        assertEquals(
            LedgerMigration.inventoryJson(sourceInventory).toString(),
            LedgerMigration.inventoryJson(parsed.inventory).toString()
        )
    }

    @Test
    fun `إعادة التصدير بنفس الأحداث وبترتيب مختلف تعطي الملفّ نفسه`() {
        val first = LedgerMigration.exportText(events(), 1_767_225_600_000, "device-1")
        val second = LedgerMigration.exportText(events().reversed(), 1_767_225_600_000, "device-1")
        assertEquals("ملفّ غير حتمي يجعل كل مقارنة تختلف بلا سبب حقيقي", first, second)
    }

    @Test
    fun `الملفّ التالف يُقال سببه بالعربية لا باستثناء`() {
        val failures = listOf("", "{ ليس JSON", """{"kind":"header","format":"X"}""")
        failures.forEach { text ->
            val error = runCatching { LedgerMigration.parse(text) }.exceptionOrNull()
            assertTrue("يلزم خطأ مفهوم للسطر: ${text.take(24)}", error is IllegalArgumentException || error is IllegalStateException)
            assertTrue("وجملته عربية", error!!.message.orEmpty().any { it in 'ء'..'ي' })
        }
        val truncated = LedgerMigration.exportText(events(), 1L, "device-1").lines().toMutableList()
        truncated[2] = truncated[2].take(20)
        val error = runCatching { LedgerMigration.parse(truncated.joinToString("\n")) }.exceptionOrNull()
        assertTrue("الملفّ المبتور يُقال إنه مبتور", error!!.message.orEmpty().contains("تالف"))
    }

    @Test
    fun `قيد مكرّر بنفس المعرّف يُحسب مرة واحدة عند التصدير`() {
        val duplicated = events() + events()
        val inventory = LedgerMigration.inventory(duplicated)
        assertEquals("حدثان لنفس العملية لا يعنيان قيدين", sourceInventory.activeTotal, inventory.activeTotal)
        assertEquals(sourceInventory.operationIds, inventory.operationIds)
    }

    @Test
    fun `ملفّ ترحيل فارغ من الأحداث لكن صحيح الترويسة لا ينفجر`() {
        val text = LedgerMigration.exportText(emptyList(), 1L, "device-1")
        val parsed = LedgerMigration.parse(text)
        assertTrue(parsed.events.isEmpty())
        assertEquals(0, parsed.inventory.activeTotal)
        assertFalse("لا غرف يعني جردًا فارغًا صحيحًا لا خطأ", parsed.inventory.rooms.isNotEmpty())
    }
}
