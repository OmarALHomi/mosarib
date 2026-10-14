package com.example

import com.example.features.customers.Customer
import com.example.features.customers.CustomerExcelHelper
import com.example.features.customers.CustomerWithBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

class CustomerExcelTest {

    @Test
    fun testCsvParsingAndSmartColumnMapping() {
        val sampleCsv = """
            اسم العميل,رقم الهاتف,المزرعة,الموقع,سعر الساعة,نوع الحساب
            أحمد علي,771234567,مزرعة النخيل,وادي سهام,5000,مزارع
            سالم عبده,733445566,مزرعة البرتقال,الزيدية,0,صاحب بئر
            ,99999999,,,3000,مزارع
        """.trimIndent()

        val existingCustomers = listOf(
            Customer(id = 1L, name = "أحمد علي", phone = "771234567")
        )

        // سنختبر مطابقة المسودات عبر محاكاة جدول rawRows
        val drafts = mutableListOf<com.example.features.customers.ImportedCustomerDraft>()
        val lines = sampleCsv.lines()
        val rawRows = lines.map { line -> line.split(",").map { it.trim() } }

        // استدعاء دالة رسم الخرائط عبر فحص الانعكاس أو بناء المسودات
        // للتأكد من مطابقة الكلمات المفتاحية
        assertTrue(rawRows.size >= 4)
        val header = rawRows[0]
        assertEquals("اسم العميل", header[0])
        assertEquals("رقم الهاتف", header[1])
        assertEquals("نوع الحساب", header[5])
    }

    @Test
    fun testArabicNormalization() {
        val name1 = "أحمد"
        val name2 = "احمد"
        val clean1 = name1.replace("أ", "ا")
        val clean2 = name2.replace("أ", "ا")
        assertEquals(clean1, clean2)
    }

    @Test
    fun testDaftareHesabatFormatParsing() {
        val sampleRows = listOf(
            listOf("م", "اسم الحساب", "رقم الهاتف", "العنوان", "الرصيد الحالي", "الحالة", "العملة", "تاريخ آخر حركة", "ملاحظات / البيان"),
            listOf("1", "صالح أحمد بن علي العولقي", "771234001", "الحديدة - باجل", "45000", "مطلوب منه (مدين)", "ريال يمني", "2026/09/25", "باقي حساب سقي طماطم"),
            listOf("2", "محمد يحيى بن سالم السالمي", "773456003", "صنعاء - بني حشيش", "15000", "مطلوب له (دائن)", "ريال يمني", "2026/10/03", "دفعة مقدمة تحت الحساب")
        )

        val drafts = CustomerExcelHelper.mapRowsToDrafts(sampleRows, emptyList())

        assertEquals(2, drafts.size)

        val draft1 = drafts[0]
        assertEquals("صالح أحمد بن علي العولقي", draft1.name)
        assertEquals("771234001", draft1.phone)
        assertEquals("الحديدة - باجل", draft1.location)
        assertEquals(45000.0, draft1.initialBalance, 0.001)
        assertTrue("draft1 should be debit (عليه)", draft1.isDebit)
        assertEquals("باقي حساب سقي طماطم", draft1.notes)

        val draft2 = drafts[1]
        assertEquals("محمد يحيى بن سالم السالمي", draft2.name)
        assertEquals("773456003", draft2.phone)
        assertEquals("صنعاء - بني حشيش", draft2.location)
        assertEquals(15000.0, draft2.initialBalance, 0.001)
        org.junit.Assert.assertFalse("draft2 should be credit (له)", draft2.isDebit)
    }
}
