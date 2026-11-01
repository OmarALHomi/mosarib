package com.example.features.customers

import android.content.Context
import android.net.Uri
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * مسودة العميل المستورد من ملف إكسل أو CSV قبل اعتماده في قاعدة البيانات.
 */
/**
 * حركة أو عملية تابعة للحساب في ملف العمليات/اليومية المستورد.
 */
data class ImportedOperationItem(
    val type: String, // "DEBIT" (مدين/سقي/دين) or "CREDIT" (دائن/قبض/سداد)
    val amount: Double,
    val description: String = "",
    val currency: String = "",
    val date: Long? = null
)

/**
 * مصدر ملف الاستيراد لتحديد السلوك الافتراضي لقيد العمليات المدينة.
 */
enum class ImportSource {
    MOSARIB_EXPORT,
    EXTERNAL_LEDGER
}

/**
 * طريقة قيد العمليات المدينة المستوردة.
 */
enum class ImportDebitMode {
    EXPENSE, // صرف للعميل (سند صرف)
    SESSION  // سقي (جلسة سقي)
}

/**
 * نتيجة تحليل وقراءة ملف العملاء بما في ذلك المسودات والمصدر المكتشف.
 */
data class CustomerParseResult(
    val drafts: List<ImportedCustomerDraft>,
    val source: ImportSource = ImportSource.EXTERNAL_LEDGER
)

/**
 * مسودة العميل المستورد من ملف إكسل أو CSV قبل اعتماده في قاعدة البيانات.
 */
data class ImportedCustomerDraft(
    val localId: String = UUID.randomUUID().toString(),
    var name: String = "",
    var phone: String = "",
    var farmName: String = "",
    var location: String = "",
    var customPricePerHour: Double? = null,
    var isWellOwner: Boolean = false,
    var customGroup: String = "",
    var initialBalance: Double = 0.0,
    var isDebit: Boolean = true, // true = مدين (عليه دين)، false = دائن (له رصيد)
    var notes: String = "",
    var isSelected: Boolean = true,
    var isExistingInDb: Boolean = false,
    var existingCustomerId: Long? = null,
    var existingCustomerName: String? = null,
    var validationError: String? = null,
    var currency: String = "",
    var attachedOperations: List<ImportedOperationItem> = emptyList()
)

object CustomerExcelHelper {

    private val NAME_KEYWORDS = listOf(
        "اسم الحساب", "الحساب", "الطرف الثاني", "جهة التعامل", "الاسم / الحساب",
        "الاسم", "اسم العميل", "اسم المزارع", "العميل", "المزارع", "الزبون", "المستفيد",
        "اسم الزبون", "اسم الشخص", "name", "customer", "farmer", "client", "full name", "fullname", "account", "party"
    )
    private val PHONE_KEYWORDS = listOf(
        "الهاتف", "رقم الهاتف", "الجوال", "الموبايل", "التلفون", "تلفون", "رقم الجوال",
        "الاتصال", "واتساب", "رقم الاتصال", "phone", "mobile", "tel", "cell", "contact", "whatsapp"
    )
    private val FARM_KEYWORDS = listOf(
        "المزرعة", "اسم المزرعة", "الحقل", "البستان", "farm", "farm name", "field", "orchard"
    )
    private val LOCATION_KEYWORDS = listOf(
        "العنوان", "الموقع", "المنطقة", "القرية", "البلاد", "المدينة", "المحافظة", "location", "address", "area", "village", "city"
    )
    private val BALANCE_KEYWORDS = listOf(
        "الرصيد", "الرصيد الحالي", "رصيد الحساب", "الرصيد السابق", "الرصيد الافتتاحي",
        "صافي الرصيد", "المبلغ المتبقي", "المتبقي", "رصيد", "balance", "current balance", "net balance", "due"
    )
    private val DEBIT_KEYWORDS = listOf(
        "مدين", "عليه", "مطلوب منه", "debit"
    )
    private val CREDIT_KEYWORDS = listOf(
        "دائن", "له", "مطلوب له", "credit"
    )
    private val BALANCE_TYPE_KEYWORDS = listOf(
        "نوع الرصيد", "طبيعة الرصيد", "الحالة", "balance type", "status"
    )
    private val PRICE_KEYWORDS = listOf(
        "سعر الساعة", "سعر الماء", "التعرفة", "سعر خاص", "hourly price", "hourly rate"
    )
    private val TYPE_KEYWORDS = listOf(
        "صاحب بئر", "نوع الحساب", "النوع", "مالك البئر", "بئر", "owner", "well owner", "type", "account type"
    )
    private val GROUP_KEYWORDS = listOf(
        "المجموعة", "مجموعة", "التصنيف", "تصنيف", "الفرقة", "group", "category", "custom group", "tag"
    )
    private val NOTES_KEYWORDS = listOf(
        "ملاحظات", "ملاحظة", "البيان", "الوصف", "تفاصيل", "notes", "note", "description", "remarks", "comment"
    )
    private val CURRENCY_KEYWORDS = listOf(
        "العملة", "العمله", "نوع العملة", "رمز العملة", "currency", "curr"
    )
    private val DATE_KEYWORDS = listOf(
        "التاريخ", "تاريخ", "تاريخ العملية", "تاريخ القيد", "date"
    )

    /**
     * يقرأ ملف إكسل (.xlsx) أو ملف نصي (.csv, .tsv) بذكاء ومرونة تامة.
     */
    fun parseCustomerFile(
        context: Context,
        uri: Uri,
        existingCustomers: List<Customer>
    ): CustomerParseResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return CustomerParseResult(emptyList(), ImportSource.EXTERNAL_LEDGER)
        val buffered = BufferedInputStream(inputStream)
        return parseCustomerStream(buffered, existingCustomers)
    }

    fun parseCustomerStream(
        stream: InputStream,
        existingCustomers: List<Customer>
    ): CustomerParseResult {
        val buffered = if (stream is BufferedInputStream) stream else BufferedInputStream(stream)

        // التحقق من نوع الملف عبر البصمة السحرية (Magic Bytes: PK..)
        buffered.mark(8)
        val header = ByteArray(4)
        val readBytes = buffered.read(header)
        buffered.reset()

        val isZipXlsx = readBytes == 4 &&
            header[0] == 0x50.toByte() &&
            header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()

        val rawRows: List<List<String>> = if (isZipXlsx) {
            parseXlsx(buffered)
        } else {
            parseCsvOrDelimited(buffered)
        }

        if (rawRows.isEmpty()) return CustomerParseResult(emptyList(), ImportSource.EXTERNAL_LEDGER)

        return mapRowsToParseResult(rawRows, existingCustomers)
    }

    /**
     * استخراج الصفوف من ملف XLSX (XML Zip Archive).
     */
    private fun parseXlsx(stream: InputStream): List<List<String>> {
        val zip = ZipInputStream(stream)
        var entry: ZipEntry? = zip.nextEntry
        val sharedStrings = ArrayList<String>()
        var sheetBytes: ByteArray? = null

        while (entry != null) {
            val name = entry.name.lowercase(Locale.ENGLISH)
            if (name.endsWith("sharedstrings.xml")) {
                val ssBytes = zip.readBytes()
                parseSharedStrings(ByteArrayInputStream(ssBytes), sharedStrings)
            } else if (name.contains("worksheets/sheet") && name.endsWith(".xml") && sheetBytes == null) {
                // حفظ أول ورقة عمل للمعالجة بعد استكمال النصوص المشتركة
                sheetBytes = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        if (sheetBytes == null) return emptyList()
        return parseSheetXml(ByteArrayInputStream(sheetBytes), sharedStrings)
    }

    private fun parseSharedStrings(stream: InputStream, outList: ArrayList<String>) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var inSi = false
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        if (tagName.equals("si", ignoreCase = true)) {
                            inSi = true
                            currentText.setLength(0)
                        } else if (inSi && tagName.equals("t", ignoreCase = true)) {
                            currentText.append(parser.nextText())
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("si", ignoreCase = true)) {
                            outList.add(currentText.toString())
                            inSi = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // معالجة صامتة لحماية الاستيراد
        }
    }

    private fun parseSheetXml(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var currentRow = mutableListOf<String>()
            var currentCellRef: String? = null
            var currentCellType: String? = null
            var cellValue = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name.equals("row", ignoreCase = true)) {
                            currentRow = mutableListOf()
                        } else if (name.equals("c", ignoreCase = true)) {
                            currentCellRef = parser.getAttributeValue(null, "r")
                            currentCellType = parser.getAttributeValue(null, "t")
                            cellValue.setLength(0)
                        } else if (name.equals("v", ignoreCase = true) || name.equals("t", ignoreCase = true)) {
                            cellValue.append(parser.nextText())
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (name.equals("c", ignoreCase = true)) {
                            val raw = cellValue.toString().trim()
                            val parsedValue = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else raw
                                }
                                "b" -> if (raw == "1") "نعم" else "لا"
                                else -> raw
                            }
                            // تحديد موقع العمود بناءً على مرجع الخلية مثل A1, B2
                            val colIdx = if (currentCellRef != null) {
                                columnRefToIndex(currentCellRef)
                            } else {
                                currentRow.size
                            }
                            while (currentRow.size < colIdx) {
                                currentRow.add("")
                            }
                            currentRow.add(parsedValue)
                        } else if (name.equals("row", ignoreCase = true)) {
                            if (currentRow.any { it.isNotBlank() }) {
                                result.add(currentRow)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // استرجاع ما تم استخراجه حتى موضع الخطأ
        }
        return result
    }

    private fun columnRefToIndex(cellRef: String): Int {
        var idx = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                idx = idx * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                idx = idx * 26 + (ch - 'a' + 1)
            }
        }
        return (idx - 1).coerceAtLeast(0)
    }

    /**
     * استخراج الصفوف من ملفات CSV / TSV مع دعم الفواصل المختلفة وعلامات التنصيص وحقول الأسطر المتعددة والترميز العربي.
     */
    internal fun parseCsvOrDelimited(stream: InputStream): List<List<String>> {
        val bytes = stream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        val text = decodeCsvBytes(bytes)
        if (text.isBlank()) return emptyList()

        val firstLine = text.lineSequence().firstOrNull()?.removePrefix("\uFEFF").orEmpty()
        val delimiter = detectDelimiter(firstLine)

        return parseCsvTextToRecords(text.removePrefix("\uFEFF"), delimiter)
    }

    private fun decodeCsvBytes(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        try {
            val charBuffer = utf8Decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            return charBuffer.toString()
        } catch (_: Exception) {
            return try {
                val win1256 = java.nio.charset.Charset.forName("windows-1256")
                String(bytes, win1256)
            } catch (_: Exception) {
                String(bytes, StandardCharsets.UTF_8)
            }
        }
    }

    private fun detectDelimiter(line: String): Char {
        val commas = line.count { it == ',' }
        val semicolons = line.count { it == ';' }
        val tabs = line.count { it == '\t' }
        val pipes = line.count { it == '|' }

        return when {
            semicolons > commas && semicolons > tabs -> ';'
            tabs > commas && tabs > semicolons -> '\t'
            pipes > commas && pipes > semicolons -> '|'
            else -> ','
        }
    }

    private fun parseCsvTextToRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentTokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < len && text[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    currentTokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                (c == '\r' || c == '\n') && !inQuotes -> {
                    if (c == '\r' && i + 1 < len && text[i + 1] == '\n') {
                        i++
                    }
                    currentTokens.add(sb.toString().trim())
                    sb.setLength(0)
                    if (currentTokens.any { it.isNotBlank() }) {
                        records.add(currentTokens.toList())
                    }
                    currentTokens.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        if (sb.isNotEmpty() || currentTokens.isNotEmpty()) {
            currentTokens.add(sb.toString().trim())
            if (currentTokens.any { it.isNotBlank() }) {
                records.add(currentTokens.toList())
            }
        }
        return records
    }

    private fun matchesKeywordWordOrPhrase(cleanText: String, keywords: List<String>): Boolean {
        val normalized = cleanText.replace(Regex("[^\\p{L}\\p{Nd}\\s]+"), " ").trim()
        val words = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return keywords.any { kw ->
            val normKw = normalizeArabicText(kw).replace(Regex("[^\\p{L}\\p{Nd}\\s]+"), " ").trim()
            if (normKw.contains(" ")) {
                normalized.contains(normKw)
            } else {
                words.contains(normKw)
            }
        }
    }

    /**
     * استكشاف مصدر الملف المرفوع إذا كان تصديراً من تطبيق مُسَرِّب أو دفتر حسابات خارجي.
     */
    fun detectImportSource(headerRow: List<String>): ImportSource {
        if (headerRow.isEmpty()) return ImportSource.EXTERNAL_LEDGER
        val cleanCells = headerRow.map { cell ->
            normalizeArabicText(cell.replace(Regex("\\([^)]*\\)"), "").trim())
        }
        val mosaribHeaders = listOf(
            "اسم العميل", "رقم الهاتف", "المجموعه", "اسم المزرعه", "الموقع", "نوع الحساب",
            "سعر الساعه", "اجمالي الساعات", "الرصيد المستحق", "الملاحظات"
        )
        val matchCount = mosaribHeaders.count { expected ->
            cleanCells.any { it.contains(expected) || expected.contains(it) }
        }
        return if (matchCount >= 7 && cleanCells.any { it.contains("اجمالي الساعات") }) {
            ImportSource.MOSARIB_EXPORT
        } else {
            ImportSource.EXTERNAL_LEDGER
        }
    }

    /**
     * تحليل ذكي للتواريخ المدخلة بدعم الصيغ المحددة والأرقام الهندية والأرقام التسلسلية لإكسل.
     */
    fun parseDateString(rawDate: String): Long? {
        val clean = FileSharingHelper.convertArabicDigitsToAscii(rawDate).trim()
        if (clean.isBlank()) return null

        val asNumber = clean.toDoubleOrNull()
        if (asNumber != null && asNumber in 10000.0..100000.0) {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(1899, java.util.Calendar.DECEMBER, 30, 0, 0, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, asNumber.toInt())
            val fraction = asNumber - asNumber.toInt()
            val millis = (fraction * 86_400_000.0).toInt()
            calendar.add(java.util.Calendar.MILLISECOND, millis)
            return calendar.timeInMillis
        }

        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * تحليل ذكي للأعمدة والربط التلقائي بالحقول المناسبة مع كشف مصدر الملف.
     */
    fun mapRowsToParseResult(
        rawRows: List<List<String>>,
        existingCustomers: List<Customer>
    ): CustomerParseResult {
        if (rawRows.isEmpty()) return CustomerParseResult(emptyList(), ImportSource.EXTERNAL_LEDGER)

        // 1. البحث عن صف العناوين (Header Row) من بين أول 5 صفوف
        var headerRowIndex = 0
        var bestHeaderScore = -1
        val maxHeaderScan = minOf(5, rawRows.size)

        for (r in 0 until maxHeaderScan) {
            val row = rawRows[r]
            var score = 0
            for (cell in row) {
                val clean = normalizeArabicText(cell)
                if (matchesKeywordWordOrPhrase(clean, NAME_KEYWORDS)) score += 3
                if (matchesKeywordWordOrPhrase(clean, PHONE_KEYWORDS)) score += 3
                if (matchesKeywordWordOrPhrase(clean, BALANCE_KEYWORDS)) score += 3
                if (matchesKeywordWordOrPhrase(clean, DEBIT_KEYWORDS)) score += 2
                if (matchesKeywordWordOrPhrase(clean, CREDIT_KEYWORDS)) score += 2
                if (matchesKeywordWordOrPhrase(clean, FARM_KEYWORDS)) score += 2
                if (matchesKeywordWordOrPhrase(clean, LOCATION_KEYWORDS)) score += 2
                if (matchesKeywordWordOrPhrase(clean, PRICE_KEYWORDS)) score += 2
                if (matchesKeywordWordOrPhrase(clean, TYPE_KEYWORDS)) score += 2
                if (matchesKeywordWordOrPhrase(clean, CURRENCY_KEYWORDS)) score += 2
            }
            if (score > bestHeaderScore) {
                bestHeaderScore = score
                headerRowIndex = r
            }
        }

        val hasHeader = bestHeaderScore >= 3
        val headerRow = if (hasHeader) rawRows[headerRowIndex] else emptyList()
        val detectedSource = if (hasHeader) detectImportSource(headerRow) else ImportSource.EXTERNAL_LEDGER
        val dataStartIndex = if (hasHeader) headerRowIndex + 1 else 0

        // 2. تعيين مؤشرات الأعمدة
        var nameCol = -1
        var phoneCol = -1
        var farmCol = -1
        var locationCol = -1
        var priceCol = -1
        var typeCol = -1
        var groupCol = -1
        var notesCol = -1
        var balanceCol = -1
        var debitCol = -1
        var creditCol = -1
        var balanceTypeCol = -1
        var currencyCol = -1
        var dateCol = -1

        if (hasHeader) {
            for (c in headerRow.indices) {
                val clean = normalizeArabicText(headerRow[c])
                when {
                    nameCol == -1 && matchesKeywordWordOrPhrase(clean, NAME_KEYWORDS) -> nameCol = c
                    phoneCol == -1 && matchesKeywordWordOrPhrase(clean, PHONE_KEYWORDS) -> phoneCol = c
                    priceCol == -1 && matchesKeywordWordOrPhrase(clean, PRICE_KEYWORDS) -> priceCol = c
                    debitCol == -1 && matchesKeywordWordOrPhrase(clean, DEBIT_KEYWORDS) -> debitCol = c
                    creditCol == -1 && matchesKeywordWordOrPhrase(clean, CREDIT_KEYWORDS) -> creditCol = c
                    balanceTypeCol == -1 && matchesKeywordWordOrPhrase(clean, BALANCE_TYPE_KEYWORDS) -> balanceTypeCol = c
                    balanceCol == -1 && matchesKeywordWordOrPhrase(clean, BALANCE_KEYWORDS) -> balanceCol = c
                    farmCol == -1 && matchesKeywordWordOrPhrase(clean, FARM_KEYWORDS) -> farmCol = c
                    locationCol == -1 && matchesKeywordWordOrPhrase(clean, LOCATION_KEYWORDS) -> locationCol = c
                    typeCol == -1 && matchesKeywordWordOrPhrase(clean, TYPE_KEYWORDS) -> typeCol = c
                    groupCol == -1 && matchesKeywordWordOrPhrase(clean, GROUP_KEYWORDS) -> groupCol = c
                    notesCol == -1 && matchesKeywordWordOrPhrase(clean, NOTES_KEYWORDS) -> notesCol = c
                    currencyCol == -1 && matchesKeywordWordOrPhrase(clean, CURRENCY_KEYWORDS) -> currencyCol = c
                    dateCol == -1 && matchesKeywordWordOrPhrase(clean, DATE_KEYWORDS) -> dateCol = c
                }
            }
        }

        // استدلال بديل في حال لم يتم التعرف على العمود أو غياب العناوين
        val firstDataRow = rawRows.getOrNull(dataStartIndex) ?: emptyList()
        if (nameCol == -1) {
            nameCol = firstDataRow.indices.firstOrNull { idx ->
                val v = firstDataRow[idx]
                v.isNotBlank() && !isPhoneLike(v) && v.toDoubleOrNull() == null
            } ?: 0
        }
        if (phoneCol == -1) {
            phoneCol = firstDataRow.indices.firstOrNull { idx ->
                idx != nameCol && isPhoneLike(firstDataRow[idx])
            } ?: -1
        }

        // 3. تجميع الصفوف تحت اسم العميل الواحد لمنع تكرار العميل بعدد عملياته
        class CustomerAccumulator(
            var name: String,
            var phone: String = "",
            var farmName: String = "",
            var location: String = "",
            var customPricePerHour: Double? = null,
            var isWellOwner: Boolean = false,
            var customGroup: String = "",
            var currency: String = "",
            var singleNotes: String = "",
            val operations: MutableList<ImportedOperationItem> = mutableListOf()
        )

        val accumulators = LinkedHashMap<String, CustomerAccumulator>()

        for (r in dataStartIndex until rawRows.size) {
            val row = rawRows[r]
            if (row.all { it.isBlank() }) continue

            val rawName = row.getOrNull(nameCol)?.trim().orEmpty()
            val rawPhone = if (phoneCol >= 0) cleanPhoneString(row.getOrNull(phoneCol).orEmpty()) else ""
            val rawFarm = if (farmCol >= 0) row.getOrNull(farmCol)?.trim().orEmpty() else ""
            val rawLocation = if (locationCol >= 0) row.getOrNull(locationCol)?.trim().orEmpty() else ""
            val rawPrice = if (priceCol >= 0) Formatters.parseAmountInput(row.getOrNull(priceCol).orEmpty()) else 0.0
            val rawType = if (typeCol >= 0) row.getOrNull(typeCol).orEmpty() else ""
            val rawGroup = if (groupCol >= 0) row.getOrNull(groupCol)?.trim().orEmpty() else ""
            val rawNotes = if (notesCol >= 0) row.getOrNull(notesCol)?.trim().orEmpty() else ""
            val rawCurrency = if (currencyCol >= 0) row.getOrNull(currencyCol)?.trim().orEmpty() else ""
            val rawDate = if (dateCol >= 0) row.getOrNull(dateCol)?.trim().orEmpty() else ""
            val parsedDate = if (rawDate.isNotBlank()) parseDateString(rawDate) else null

            if (rawName.isBlank() && rawPhone.isBlank()) continue

            // استخراج الرصيد أو مبلغ العملية
            var computedBalance = 0.0
            var computedIsDebit = true // الافتراضي: مدين (عليه دين)

            if (debitCol >= 0 || creditCol >= 0) {
                val debitVal = if (debitCol >= 0) Formatters.parseAmountInput(row.getOrNull(debitCol).orEmpty()) else 0.0
                val creditVal = if (creditCol >= 0) Formatters.parseAmountInput(row.getOrNull(creditCol).orEmpty()) else 0.0
                if (debitVal > 0.0 && debitVal >= creditVal) {
                    computedBalance = debitVal - creditVal
                    computedIsDebit = true
                } else if (creditVal > 0.0) {
                    computedBalance = creditVal - debitVal
                    computedIsDebit = false
                }
            } else if (balanceCol >= 0) {
                val rawBalStr = row.getOrNull(balanceCol).orEmpty().trim()
                val parsedBal = Formatters.parseAmountInput(rawBalStr)
                val typeStr = if (balanceTypeCol >= 0) row.getOrNull(balanceTypeCol).orEmpty().trim() else ""
                val cleanType = normalizeArabicText(typeStr)
                val isCredit = matchesKeywordWordOrPhrase(cleanType, CREDIT_KEYWORDS) || rawBalStr.startsWith("-")
                if (parsedBal > 0.0) {
                    computedBalance = parsedBal
                    computedIsDebit = !isCredit
                }
            }

            val isOwner = isWellOwnerString(rawType)

            // مفتاح التجميع: بالهاتف أولاً، أو بالاسم المطابق لدمج العمليات تحت نفس العميل
            val cleanNameKey = normalizeArabicText(rawName)
            val groupKey = if (rawPhone.isNotBlank()) "p:$rawPhone" else "n:$cleanNameKey"

            val acc = accumulators.getOrPut(groupKey) {
                CustomerAccumulator(name = rawName)
            }

            if (acc.name.isBlank() && rawName.isNotBlank()) acc.name = rawName
            if (acc.phone.isBlank() && rawPhone.isNotBlank()) acc.phone = rawPhone
            if (acc.farmName.isBlank() && rawFarm.isNotBlank()) acc.farmName = rawFarm
            if (acc.location.isBlank() && rawLocation.isNotBlank()) acc.location = rawLocation
            if (acc.customPricePerHour == null && rawPrice > 0.0) acc.customPricePerHour = rawPrice
            if (isOwner) acc.isWellOwner = true
            if (acc.customGroup.isBlank() && rawGroup.isNotBlank()) acc.customGroup = rawGroup
            if (acc.currency.isBlank() && rawCurrency.isNotBlank()) acc.currency = rawCurrency
            if (acc.singleNotes.isBlank() && rawNotes.isNotBlank()) acc.singleNotes = rawNotes

            if (computedBalance > 0.0) {
                acc.operations.add(
                    ImportedOperationItem(
                        type = if (computedIsDebit) "DEBIT" else "CREDIT",
                        amount = computedBalance,
                        description = rawNotes,
                        currency = rawCurrency,
                        date = parsedDate
                    )
                )
            }
        }

        // 4. بناء مسودات العملاء المجمعة
        val drafts = mutableListOf<ImportedCustomerDraft>()
        for ((_, acc) in accumulators) {
            val ops = acc.operations
            var totalBal = 0.0
            var isDebit = true

            if (ops.size == 1) {
                totalBal = ops[0].amount
                isDebit = (ops[0].type == "DEBIT")
            } else if (ops.size > 1) {
                val sumDebit = ops.filter { it.type == "DEBIT" }.sumOf { it.amount }
                val sumCredit = ops.filter { it.type == "CREDIT" }.sumOf { it.amount }
                val net = sumDebit - sumCredit
                if (net >= 0) {
                    totalBal = net
                    isDebit = true
                } else {
                    totalBal = -net
                    isDebit = false
                }
            }

            // المطابقة مع العملاء المسجلين مسبقاً في قاعدة البيانات
            val existingMatch = existingCustomers.find { existing ->
                val matchName = normalizeArabicText(existing.name) == normalizeArabicText(acc.name) && acc.name.isNotBlank()
                val matchPhone = existing.phone.isNotBlank() && acc.phone.isNotBlank() &&
                    cleanPhoneString(existing.phone) == acc.phone
                matchName || matchPhone
            }

            val validationErr = if (acc.name.isBlank()) "اسم العميل مطلوب" else null

            drafts.add(
                ImportedCustomerDraft(
                    name = acc.name,
                    phone = acc.phone,
                    farmName = acc.farmName,
                    location = acc.location,
                    customPricePerHour = acc.customPricePerHour,
                    isWellOwner = acc.isWellOwner,
                    customGroup = acc.customGroup,
                    initialBalance = totalBal,
                    isDebit = isDebit,
                    notes = if (ops.size > 1) "مجمّع ${ops.size} عملية من ملف خارجي" else (acc.singleNotes.ifBlank { ops.firstOrNull()?.description.orEmpty() }),
                    isSelected = validationErr == null,
                    isExistingInDb = existingMatch != null,
                    existingCustomerId = existingMatch?.id,
                    existingCustomerName = existingMatch?.name,
                    validationError = validationErr,
                    currency = acc.currency,
                    attachedOperations = ops
                )
            )
        }

        return CustomerParseResult(drafts = drafts, source = detectedSource)
    }

    internal fun mapRowsToDrafts(
        rawRows: List<List<String>>,
        existingCustomers: List<Customer>
    ): List<ImportedCustomerDraft> {
        return mapRowsToParseResult(rawRows, existingCustomers).drafts
    }

    private fun normalizeArabicText(text: String): String {
        return text.trim().lowercase(Locale.ROOT)
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[\\s_\\-]+"), " ")
    }

    private fun isPhoneLike(text: String): Boolean {
        val clean = cleanPhoneString(text)
        return clean.length in 7..14 && clean.all { it.isDigit() }
    }

    private fun cleanPhoneString(text: String): String {
        val ascii = FileSharingHelper.convertArabicDigitsToAscii(text)
        return ascii.replace(Regex("[^0-9+]"), "").trim()
    }

    private fun isWellOwnerString(typeText: String): Boolean {
        val t = normalizeArabicText(typeText)
        return t.contains("بئر") || t.contains("مالك") || t.contains("صاحب") ||
            t.contains("owner") || t.contains("well") || t == "نعم" || t == "yes" || t == "true" || t == "1"
    }

    /**
     * تصدير العملاء إلى ملف إكسل XLSX رسمي أصلي متوافق مع كافة برامج الجداول الحسابية.
     */
    fun exportCustomersToExcel(
        context: Context,
        customers: List<CustomerWithBalance>,
        currencySymbol: String
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(exportDir, "mosarib_customers_$dateStamp.xlsx")

        val zipOut = ZipOutputStream(FileOutputStream(file))

        // 1. [Content_Types].xml
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        zipOut.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8)
        )
        zipOut.closeEntry()

        // 2. _rels/.rels
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        zipOut.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8)
        )
        zipOut.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zipOut.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zipOut.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8)
        )
        zipOut.closeEntry()

        // 4. xl/workbook.xml
        zipOut.putNextEntry(ZipEntry("xl/workbook.xml"))
        zipOut.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="قائمة العملاء والمزارع" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8)
        )
        zipOut.closeEntry()

        // جمع كافة النصوص لبناء sharedStrings.xml
        val headers = listOf(
            "اسم العميل", "رقم الهاتف", "المجموعة", "اسم المزرعة", "الموقع", "نوع الحساب",
            "سعر الساعة ($currencySymbol)", "إجمالي الساعات", "الرصيد المستحق ($currencySymbol)", "الملاحظات"
        )

        val uniqueStrings = mutableListOf<String>()
        val stringMap = mutableMapOf<String, Int>()

        fun getStringId(str: String): Int {
            return stringMap.getOrPut(str) {
                val idx = uniqueStrings.size
                uniqueStrings.add(str)
                idx
            }
        }

        // تسجيل العناوين
        headers.forEach { getStringId(it) }

        // تسجيل البيانات
        val rowsData = customers.map { item ->
            val c = item.customer
            val typeStr = if (c.isWellOwner) "صاحب بئر" else "مزارع"
            val priceStr = c.customPricePerHour?.let { Formatters.formatCurrency(it, "").trim() } ?: "افتراضي"
            val hoursStr = Formatters.formatDurationArabic(item.totalMinutes)
            val balanceStr = Formatters.formatCurrency(item.balance, "").trim()

            listOf(
                getStringId(c.name),
                getStringId(c.phone),
                getStringId(c.customGroup),
                getStringId(c.farmName),
                getStringId(c.location),
                getStringId(typeStr),
                getStringId(priceStr),
                getStringId(hoursStr),
                getStringId(balanceStr),
                getStringId(c.notes)
            )
        }

        // 5. xl/sharedStrings.xml
        zipOut.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
        val ssBuilder = java.lang.StringBuilder()
        ssBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        ssBuilder.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${uniqueStrings.size}" uniqueCount="${uniqueStrings.size}">""")
        for (str in uniqueStrings) {
            ssBuilder.append("<si><t>${escapeXml(str)}</t></si>")
        }
        ssBuilder.append("</sst>")
        zipOut.write(ssBuilder.toString().toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 6. xl/worksheets/sheet1.xml
        zipOut.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheetBuilder = java.lang.StringBuilder()
        sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        // تفعيل اتجاه الورقة من اليمين لليسار (RTL) لعرض أنيق باللغة العربية
        sheetBuilder.append("""<sheetPr><pageSetUpPr rtl="true"/></sheetPr>""")
        sheetBuilder.append("<sheetData>")

        // صف العناوين (الصف 1)
        sheetBuilder.append("""<row r="1">""")
        headers.indices.forEach { col ->
            val cellRef = "${indexToColLetter(col)}1"
            sheetBuilder.append("""<c r="$cellRef" t="s"><v>${getStringId(headers[col])}</v></c>""")
        }
        sheetBuilder.append("</row>")

        // صفوف العملاء (الصف 2 وما بعده)
        rowsData.forEachIndexed { rIdx, rowStringIds ->
            val rowNum = rIdx + 2
            sheetBuilder.append("""<row r="$rowNum">""")
            rowStringIds.forEachIndexed { cIdx, strId ->
                val cellRef = "${indexToColLetter(cIdx)}$rowNum"
                sheetBuilder.append("""<c r="$cellRef" t="s"><v>$strId</v></c>""")
            }
            sheetBuilder.append("</row>")
        }

        sheetBuilder.append("</sheetData>")
        sheetBuilder.append("</worksheet>")
        zipOut.write(sheetBuilder.toString().toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        zipOut.finish()
        zipOut.close()

        return file
    }

    /**
     * تصدير العملاء إلى ملف CSV عالمي بترميز UTF-8 مع BOM لضمان قراءة الحروف العربية في إكسل.
     */
    fun exportCustomersToCsv(
        context: Context,
        customers: List<CustomerWithBalance>,
        currencySymbol: String
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(exportDir, "mosarib_customers_$dateStamp.csv")

        val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
        // كتابة UTF-8 BOM لتنبيه إكسل بأن الملف باللغة العربية
        writer.write("\uFEFF")

        val headers = listOf(
            "اسم العميل", "رقم الهاتف", "المجموعة", "اسم المزرعة", "الموقع", "نوع الحساب",
            "سعر الساعة ($currencySymbol)", "إجمالي الساعات", "الرصيد المستحق ($currencySymbol)", "الملاحظات"
        )
        writer.write(headers.joinToString(",") { escapeCsv(it) })
        writer.write("\r\n")

        for (item in customers) {
            val c = item.customer
            val typeStr = if (c.isWellOwner) "صاحب بئر" else "مزارع"
            val priceStr = c.customPricePerHour?.let { Formatters.formatCurrency(it, "").trim() } ?: ""
            val hoursStr = Formatters.formatDurationArabic(item.totalMinutes)
            val balanceStr = Formatters.formatCurrency(item.balance, "").trim()

            val row = listOf(
                c.name,
                c.phone,
                c.customGroup,
                c.farmName,
                c.location,
                typeStr,
                priceStr,
                hoursStr,
                balanceStr,
                c.notes
            )
            writer.write(row.joinToString(",") { escapeCsv(it) })
            writer.write("\r\n")
        }

        writer.flush()
        writer.close()
        return file
    }

    private fun indexToColLetter(index: Int): String {
        var num = index
        val sb = StringBuilder()
        while (num >= 0) {
            sb.append(('A'.code + (num % 26)).toChar())
            num = (num / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeCsv(str: String): String {
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }
}
