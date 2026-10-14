# -*- coding: utf-8 -*-
"""
Generate realistic export files matching popular accounting and debt-book applications
used by pump distributors and merchants in Yemen (e.g. "دفتر الحسابات", "الدفتر", "كاشف الديون", Khatabook).
Columns:
- م (المسلسل)
- اسم الحساب (Customer / Account Name)
- رقم الهاتف (Phone)
- العنوان (City / Village / Address)
- الرصيد الحالي (Current Balance)
- الحالة (Status: مطلوب منه (مدين) / مطلوب له (دائن))
- العملة (Currency: ريال يمني)
- تاريخ آخر حركة (Last Transaction Date)
- ملاحظات / البيان (Notes / Transaction details)
"""

import os
import zipfile
import xml.sax.saxutils as saxutils

LEDGER_CUSTOMERS = [
    ("صالح أحمد بن علي العولقي", "771234001", "الحديدة - باجل", 45000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/25", "باقي حساب سقي طماطم وبصل لشهر 9"),
    ("عبد الله محمد بن ناصر الحميري", "772345002", "ذمار - رداع", 28500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/01", "أجور ري حقل الذرة - الموسم الخريفي"),
    ("محمد يحيى بن سالم السالمي", "773456003", "صنعاء - بني حشيش", 15000, "مطلوب له (دائن)", "ريال يمني", "2026/10/03", "دفعة مقدمة تحت الحساب لبستان العنب"),
    ("علي حسين بن قاسم الريمي", "774567004", "إب - السحول", 62000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/18", "حساب سقي حبوب وخضار قاع الحقل"),
    ("أحمد سالم بن عبده الزبيدي", "775678005", "الحديدة - باجل", 18000, "مطلوب منه (مدين)", "ريال يمني", "2026/10/04", "سقي منتظم كل أسبوعين - دفعة مؤجلة"),
    ("يحيى إبراهيم بن مسعد الكندي", "776789006", "الحديدة - بيت الفقيه", 53000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/20", "ري نخيل وأشجار مانجو موسم الصيف"),
    ("حسين عمر بن سعيد اليافعي", "777890007", "حجة - عبس", 34000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/28", "أجور ساعات ماء للشبكة الفرعية"),
    ("إبراهيم منصور بن حسين الحبيشي", "778901008", "لحج - تبن", 22000, "مطلوب له (دائن)", "ريال يمني", "2026/10/02", "مستحقات تشغيل وتغذية بئر غربي"),
    ("سالم خالد بن منصور الشميري", "779012009", "أبين - دلتا", 41500, "مطلوب منه (مدين)", "ريال يمني", "2026/09/15", "ري مزارع أعلاف وموز دلتا أبين"),
    ("عمر عادل بن علي التهامي", "770123010", "الحديدة - المراوعة", 12500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/05", "ري صباحي دوري - متبقي الدفعة الأولى"),
    ("منصور طارق بن ناصر الأهدل", "733456011", "الحديدة - الدريهمي", 37000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/22", "سقي خضروات مشكلة وحقل بطاطا"),
    ("خالد فؤاد بن أحمد الصبيحي", "734567012", "لحج - طور الباحة", 29000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/30", "أجور ري حبوب صيفية"),
    ("عادل رمزي بن سالم البركاني", "735678013", "تعز - الشمايتين", 75000, "مطلوب له (دائن)", "ريال يمني", "2026/10/06", "مستحقات شراء ماء شبكة من بئر البركاني"),
    ("ياسر وليد بن حسن المخلافي", "736789014", "إب - النادرة", 48000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/19", "ري حقول ذرة وبقوليات وادي بنا"),
    ("طارق فيصل بن قاسم الشوافي", "737890015", "الحديدة - الزيدية", 26000, "مطلوب منه (مدين)", "ريال يمني", "2026/10/01", "حساب ري سهول تهامة الموسم الأول"),
    ("فؤاد مجيب بن عبده الضبياني", "738901016", "أبين - جعار", 33000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/27", "ري بستان مانجو مثمر"),
    ("رمزي عصام بن مسعد الردفاني", "739012017", "لحج - ردفان", 19500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/04", "سقي ليلي مخفض لحقل الأعلاف"),
    ("وليد هائل بن سعيد الأنسي", "730123018", "ذمار - جهران", 85000, "مطلوب له (دائن)", "ريال يمني", "2026/10/07", "حساب توريد وتغذية مياه قاع جهران"),
    ("فيصل عبد الرقيب بن حسين القحوم", "711234019", "مأرب - الوادي", 65000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/24", "ري محصول القمح والبر الصحراوي"),
    ("مجيب قائد بن منصور السيد", "712345020", "الحديدة - بيت الفقيه", 21000, "مطلوب منه (مدين)", "ريال يمني", "2026/10/02", "سقي بستان حمضيات ونخيل الحسينية"),
    ("عصام مقبل بن علي الهدار", "713456021", "حضرموت - تريم", 58000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/17", "تشغيل ري محوري لمحاصيل الأعلاف"),
    ("هائل ناجي بن ناصر الشيباني", "714567022", "ذمار - عنس", 36000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/29", "ري حقول بطاطس وبقوليات ذمار القرن"),
    ("عبد الرقيب زيد بن أحمد السنحاني", "715678023", "صنعاء - سنحان", 40000, "مطلوب له (دائن)", "ريال يمني", "2026/10/03", "مستحقات بئر مياه سنحان المغذي للقرية"),
    ("قائد عبده بن سالم الخولاني", "716789024", "صنعاء - خولان", 32000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/21", "ري مدرجات اللوز والبن الجبلي"),
    ("مقبل سعيد بن حسن الهمداني", "717890025", "صنعاء - همدان", 44000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/26", "أجور سقي خضار موسمية قاع المنقب"),
    ("ناجي صابر بن قاسم الوصابي", "718901026", "ذمار - وصاب", 17500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/05", "ري تكميلي لأشجار البن والفواكه"),
    ("زيد جميل بن عبده الجعدي", "719012027", "حضرموت - دوعن", 72000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/16", "سقي نخيل ومناحل سدر وادي دوعن"),
    ("عبده توفيق بن مسعد المشرقي", "710123028", "حجة - حيران", 23000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/28", "ري حقول بصل وسمسم حيران"),
    ("سعيد صالح بن سعيد الحميلي", "771122029", "الحديدة - باجل", 92000, "مطلوب له (دائن)", "ريال يمني", "2026/10/08", "مستحقات توريد مياه ارتوازية من بئر النور"),
    ("صابر عبد الله بن علي الجبلي", "772233030", "إب - جبلة", 39000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/23", "ري مدرجات بن وخضار وادي السحتين"),
    ("جميل محمد بن ناصر الصالحي", "773344031", "لحج - الحوطة", 27000, "مطلوب منه (مدين)", "ريال يمني", "2026/10/02", "أجور سقي حقول ذرة رفيعة"),
    ("توفيق أحمد بن سالم النمري", "774455032", "مأرب - الجوبة", 55000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/14", "ري مزارع القمح والشعير"),
    ("صالح يحيى بن أحمد العنسي", "775566033", "ذمار - عنس", 31000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/30", "ري حقول بطاطس عنس الأوسط"),
    ("عبد الله إبراهيم بن حسن البكاري", "776677034", "المحويت - شبام", 43000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/25", "سقي بساتين بن وفواكه وادي سردد"),
    ("محمد سالم بن قاسم الحرازي", "777788035", "صنعاء - حراز", 60000, "مطلوب له (دائن)", "ريال يمني", "2026/10/05", "مستحقات توريد مياه عذبة للشبكة"),
    ("علي عمر بن عبده الحكيمي", "778899036", "تعز - الشمايتين", 38000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/20", "أجور سقي بالصهاريج للتربة الأعلى"),
    ("أحمد منصور بن مسعد الدبعي", "779900037", "تعز - المعافر", 29500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/01", "حساب ري حقل ذرة وأشجار مثمرة"),
    ("يحيى خالد بن سعيد الشرجبي", "771020038", "تعز - صبر الموادم", 46000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/22", "سقي مدرجات فواكه وخضار الصعيد"),
    ("حسين عادل بن علي المقالح", "772030039", "إب - يريم", 51000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/19", "ري محاصيل حبوب قاع المقالح الشتوية"),
    ("إبراهيم طارق بن ناصر الوادعي", "773040040", "صعدة - الصفراء", 68000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/18", "ري بساتين رمان وعنب صعدي دماج"),
    ("سالم فؤاد بن أحمد الخضري", "774050041", "الحديدة - زبيد", 48000, "مطلوب له (دائن)", "ريال يمني", "2026/10/04", "مستحقات تشغيل بئر زبيد الزراعي"),
    ("عمر رمزي بن سالم المعمري", "775060042", "تعز - المعافر", 24000, "مطلوب منه (مدين)", "ريال يمني", "2026/10/03", "أجور ري سمسم وخضار المعافر الأوسط"),
    ("منصور وليد بن حسن الصامت", "776070043", "إب - المشنة", 35500, "مطلوب منه (مدين)", "ريال يمني", "2026/09/27", "سقي خضروات ورقية وبطاطس وادي المعاين"),
    ("خالد فيصل بن قاسم اليافعي", "777080044", "لحج - حبيل جبر", 31500, "مطلوب منه (مدين)", "ريال يمني", "2026/09/26", "ري مزارع ذرة وأعلاف مواشي ردفان"),
    ("عادل مجيب بن عبده الحريبي", "778090045", "لحج - المضاربة", 22500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/02", "أجور ري أحواض موسمية خبت الرجاع"),
    ("ياسر عصام بن مسعد المسوري", "779010046", "عمران - مسور", 65000, "مطلوب له (دائن)", "ريال يمني", "2026/10/06", "مستحقات بئر مياه محطة جبل مسور"),
    ("طارق هائل بن سعيد الأهدل", "770120047", "الحديدة - المنصورية", 18500, "مطلوب منه (مدين)", "ريال يمني", "2026/10/04", "سقي أشجار مانجو بلدي وموز المنصورية"),
    ("فؤاد عبد الرقيب بن حسين الذماري", "731230048", "ذمار - الحداء", 47000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/21", "ري محصول الثوم والبصل وادي زراجة"),
    ("رمزي قائد بن منصور الكبسي", "732340049", "صنعاء - خولان", 38500, "مطلوب منه (مدين)", "ريال يمني", "2026/09/24", "سقي بساتين مشمش ولوز قاع سهمان"),
    ("وليد مقبل بن علي الحيدري", "733450050", "عمران - خارف", 42000, "مطلوب منه (مدين)", "ريال يمني", "2026/09/28", "ري محاصيل حبوب وخضار ضوران الأشمور")
]

def generate_csv(filepath):
    """Generate UTF-8 with BOM CSV matching external ledger apps like 'دفتر الحسابات'."""
    headers = ["م", "اسم الحساب", "رقم الهاتف", "العنوان", "الرصيد الحالي", "الحالة", "العملة", "تاريخ آخر حركة", "ملاحظات / البيان"]
    with open(filepath, "w", encoding="utf-8-sig") as f:
        f.write(",".join(f'"{h}"' for h in headers) + "\n")
        for i, row in enumerate(LEDGER_CUSTOMERS, 1):
            line = [str(i), row[0], row[1], row[2], str(row[3]), row[4], row[5], row[6], row[7]]
            f.write(",".join(f'"{val}"' for val in line) + "\n")
    print(f"Generated Ledger CSV: {filepath}")

def generate_xlsx(filepath):
    """Generate valid OpenXML XLSX matching external ledger apps like 'دفتر الحسابات'."""
    strings = []
    string_map = {}

    def get_str_idx(s):
        s_str = str(s)
        if s_str not in string_map:
            string_map[s_str] = len(strings)
            strings.append(s_str)
        return string_map[s_str]

    headers = ["م", "اسم الحساب", "رقم الهاتف", "العنوان", "الرصيد الحالي", "الحالة", "العملة", "تاريخ آخر حركة", "ملاحظات / البيان"]
    cols_letters = ["A", "B", "C", "D", "E", "F", "G", "H", "I"]

    rows_xml = []

    # Header Row
    header_cells = []
    for col_idx, h in enumerate(headers):
        s_idx = get_str_idx(h)
        ref = f"{cols_letters[col_idx]}1"
        header_cells.append(f'<c r="{ref}" t="s"><v>{s_idx}</v></c>')
    rows_xml.append(f'<row r="1">{"".join(header_cells)}</row>')

    # Data Rows
    for row_num, c in enumerate(LEDGER_CUSTOMERS, 2):
        row_cells = []
        # Col A: م
        row_cells.append(f'<c r="A{row_num}"><v>{row_num - 1}</v></c>')
        # Col B: اسم الحساب
        s_idx = get_str_idx(c[0])
        row_cells.append(f'<c r="B{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col C: رقم الهاتف
        s_idx = get_str_idx(c[1])
        row_cells.append(f'<c r="C{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col D: العنوان
        s_idx = get_str_idx(c[2])
        row_cells.append(f'<c r="D{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col E: الرصيد الحالي (Number)
        row_cells.append(f'<c r="E{row_num}"><v>{c[3]}</v></c>')
        # Col F: الحالة (مطلوب منه / مطلوب له)
        s_idx = get_str_idx(c[4])
        row_cells.append(f'<c r="F{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col G: العملة
        s_idx = get_str_idx(c[5])
        row_cells.append(f'<c r="G{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col H: تاريخ آخر حركة
        s_idx = get_str_idx(c[6])
        row_cells.append(f'<c r="H{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col I: ملاحظات / البيان
        s_idx = get_str_idx(c[7])
        row_cells.append(f'<c r="I{row_num}" t="s"><v>{s_idx}</v></c>')

        rows_xml.append(f'<row r="{row_num}">{"".join(row_cells)}</row>')

    sst_entries = "".join(f"<si><t>{saxutils.escape(s)}</t></si>" for s in strings)
    sst_xml = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{len(strings)}" uniqueCount="{len(strings)}">{sst_entries}</sst>'

    sheet_data = "".join(rows_xml)
    sheet_xml = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>{sheet_data}</sheetData></worksheet>'

    content_types_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>'''

    rels_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>'''

    wb_rels_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>'''

    workbook_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="كشف حسابات العملاء" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>'''

    with zipfile.ZipFile(filepath, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types_xml)
        z.writestr("_rels/.rels", rels_xml)
        z.writestr("xl/_rels/workbook.xml.rels", wb_rels_xml)
        z.writestr("xl/workbook.xml", workbook_xml)
        z.writestr("xl/worksheets/sheet1.xml", sheet_xml)
        z.writestr("xl/sharedStrings.xml", sst_xml)

    print(f"Generated Ledger XLSX: {filepath}")

if __name__ == "__main__":
    out_dir = os.path.dirname(os.path.abspath(__file__))
    # 1. Generate files named 'daftare_hesabat_sample'
    csv_path = os.path.join(out_dir, "daftare_hesabat_sample.csv")
    xlsx_path = os.path.join(out_dir, "daftare_hesabat_sample.xlsx")
    generate_csv(csv_path)
    generate_xlsx(xlsx_path)

    # 2. Also update 'external_customers_sample' so both files provide this genuine external format
    ext_csv = os.path.join(out_dir, "external_customers_sample.csv")
    ext_xlsx = os.path.join(out_dir, "external_customers_sample.xlsx")
    generate_csv(ext_csv)
    generate_xlsx(ext_xlsx)
