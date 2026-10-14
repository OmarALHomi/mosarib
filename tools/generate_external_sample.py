# -*- coding: utf-8 -*-
"""
Generate a sample 50-customer external ledger / statement file in both XLSX and CSV formats.
Formatted realistically as if exported from an external accounting or water distribution system.
"""

import os
import zipfile
import xml.sax.saxutils as saxutils

CUSTOMERS_DATA = [
    ("صالح أحمد بن علي العولقي", "771234001", "مزرعة وادي سهام", "الحديدة - باجل", 3000, "عميل عادي", "مشترك رئيسي - زراعة طماطم وبصل"),
    ("عبد الله محمد بن ناصر الحميري", "772345002", "حقل الجربة الكبرى", "ذمار - رداع", 3500, "عميل عادي", "ري بالتقطير - حقل ذرة"),
    ("محمد يحيى بن سالم السالمي", "773456003", "بستان العنب والنخيل", "صنعاء - بني حشيش", 4000, "صاحب بئر", "مالك بئر الوادي المشترك"),
    ("علي حسين بن قاسم الريمي", "774567004", "مزرعة الجرادين", "إب - السحول", 3000, "عميل عادي", "محاصيل حبوب وخضار"),
    ("أحمد سالم بن عبده الزبيدي", "775678005", "مزرعة باجل الشرقية", "الحديدة - باجل", 3200, "عميل عادي", "سقي منتظم كل أسبوعين"),
    ("يحيى إبراهيم بن مسعد الكندي", "776789006", "حقل العرج والساحل", "الحديدة - بيت الفقيه", 2800, "عميل عادي", "أشجار نخيل ومانجو"),
    ("حسين عمر بن سعيد اليافعي", "777890007", "مزرعة وادي مور", "حجة - عبس", 3500, "عميل عادي", "مضخة فرعية للشبكة"),
    ("إبراهيم منصور بن حسين الحبيشي", "778901008", "بستان الضالع", "لحج - تبن", 3000, "صاحب بئر", "شريك في تشغيل البئر الغربي"),
    ("سالم خالد بن منصور الشميري", "779012009", "مزرعة العواذل", "أبين - دلتا", 3200, "عميل عادي", "زراعة أعلاف وموز"),
    ("عمر عادل بن علي التهامي", "770123010", "حقل الخضراء", "الحديدة - المراوعة", 2900, "عميل عادي", "ري صباحي دوري"),
    ("منصور طارق بن ناصر الأهدل", "733456011", "مزرعة الساحل الغربي", "الحديدة - الدريهمي", 3000, "عميل عادي", "حقل خضروات مشكلة"),
    ("خالد فؤاد بن أحمد الصبيحي", "734567012", "بستان الخير والبركة", "لحج - طور الباحة", 3100, "عميل عادي", "زراعة حبوب صيفية"),
    ("عادل رمزي بن سالم البركاني", "735678013", "مزرعة الروضة", "تعز - الشمايتين", 3500, "صاحب بئر", "بئر البركاني المغذي لخط 2"),
    ("ياسر وليد بن حسن المخلافي", "736789014", "حقل وادي بنا", "إب - النادرة", 3400, "عميل عادي", "مزارع ذرة وبقوليات"),
    ("طارق فيصل بن قاسم الشوافي", "737890015", "مزرعة سهول تهامة", "الحديدة - الزيدية", 3000, "عميل عادي", "مشترك جديد موسم أول"),
    ("فؤاد مجيب بن عبده الضبياني", "738901016", "بستان الموز والمانجو", "أبين - جعار", 3300, "عميل عادي", "بستان مانجو مثمر"),
    ("رمزي عصام بن مسعد الردفاني", "739012017", "مزرعة الردفان الخضراء", "لحج - ردفان", 3200, "عميل عادي", "سقي ليلي بنصف القيمة"),
    ("وليد هائل بن سعيد الأنسي", "730123018", "حقل قاع جهران", "ذمار - جهران", 3600, "صاحب بئر", "مالك بئر السلام الرئيسي"),
    ("فيصل عبد الرقيب بن حسين القحوم", "711234019", "مزرعة شبوة الشرقية", "مأرب - الوادي", 4000, "عميل عادي", "زراعة قمح وبر"),
    ("مجيب قائد بن منصور السيد", "712345020", "بستان الحسينية", "الحديدة - بيت الفقيه", 2900, "عميل عادي", "بستان حمضيات ونخيل"),
    ("عصام مقبل بن علي الهدار", "713456021", "مزرعة الوادي الكبير", "حضرموت - تريم", 3800, "عميل عادي", "نظام ري محوري"),
    ("هائل ناجي بن ناصر الشيباني", "714567022", "حقل ذمار القرن", "ذمار - عنس", 3500, "عميل عادي", "حقول بطاطس وبقوليات"),
    ("عبد الرقيب زيد بن أحمد السنحاني", "715678023", "بستان سنحان الجميل", "صنعاء - سنحان", 3700, "صاحب بئر", "تغذية خزانات القرية"),
    ("قائد عبده بن سالم الخولاني", "716789024", "مزرعة جبال خولان", "صنعاء - خولان", 3600, "عميل عادي", "مدرجات لوز وبن"),
    ("مقبل سعيد بن حسن الهمداني", "717890025", "حقل قاع المنقب", "صنعاء - همدان", 3500, "عميل عادي", "حقول خضار موسمية"),
    ("ناجي صابر بن قاسم الوصابي", "718901026", "مزرعة وصاب العالي", "ذمار - وصاب", 3300, "عميل عادي", "سقي تكميلي للأشجار"),
    ("زيد جميل بن عبده الجعدي", "719012027", "بستان دوعن الفسيح", "حضرموت - دوعن", 4200, "عميل عادي", "نخيل ومناحل عسل"),
    ("عبده توفيق بن مسعد المشرقي", "710123028", "حقل حيران الأخضر", "حجة - حيران", 2800, "عميل عادي", "زراعة بصل وسمسم"),
    ("سعيد صالح بن سعيد الحميلي", "771122029", "مزرعة النور والبركة", "الحديدة - باجل", 3000, "صاحب بئر", "بئر النور الارتوازي"),
    ("صابر عبد الله بن علي الجبلي", "772233030", "بستان وادي السحتين", "إب - جبلة", 3400, "عميل عادي", "مدرجات بن وخضار"),
    ("جميل محمد بن ناصر الصالحي", "773344031", "حقل الخيرات الوفيرة", "لحج - الحوطة", 3100, "عميل عادي", "مزارع ذرة رفيعة"),
    ("توفيق أحمد بن سالم النمري", "774455032", "مزرعة الجوف الشمالية", "مأرب - الجوبة", 3900, "عميل عادي", "قمح وشعير مسقى"),
    ("صالح يحيى بن أحمد العنسي", "775566033", "حقل عنس الأوسط", "ذمار - عنس", 3500, "عميل عادي", "حقول بطاطس مسقية"),
    ("عبد الله إبراهيم بن حسن البكاري", "776677034", "مزرعة وادي سردد", "المحويت - شبام", 3300, "عميل عادي", "بساتين بن وفواكه"),
    ("محمد سالم بن قاسم الحرازي", "777788035", "بستان مناخة الخضراء", "صنعاء - حراز", 3600, "صاحب بئر", "بئر مياه عذبة للشبكة"),
    ("علي عمر بن عبده الحكيمي", "778899036", "حقل التربة الأعلى", "تعز - الشمايتين", 3500, "عميل عادي", "سقي منتظم بالصهاريج"),
    ("أحمد منصور بن مسعد الدبعي", "779900037", "مزرعة ذبحان النموذجية", "تعز - المعافر", 3400, "عميل عادي", "حقل ذرة وأشجار مثمرة"),
    ("يحيى خالد بن سعيد الشرجبي", "771020038", "بستان شوافي الصعيد", "تعز - صبر الموادم", 3600, "عميل عادي", "مدرجات خضار فواكه"),
    ("حسين عادل بن علي المقالح", "772030039", "مزرعة قاع المقالح", "إب - يريم", 3500, "عميل عادي", "محاصيل حبوب شتوية"),
    ("إبراهيم طارق بن ناصر الوادعي", "773040040", "حقل دماج الخصيب", "صعدة - الصفراء", 4000, "عميل عادي", "بستان رمان وعنب صعدي"),
    ("سالم فؤاد بن أحمد الخضري", "774050041", "مزرعة زبيد التاريخية", "الحديدة - زبيد", 2900, "صاحب بئر", "بئر زبيد الزراعي"),
    ("عمر رمزي بن سالم المعمري", "775060042", "حقل المعافر الأوسط", "تعز - المعافر", 3300, "عميل عادي", "سقي خضار وسمسم"),
    ("منصور وليد بن حسن الصامت", "776070043", "بستان وادي المعاين", "إب - المشنة", 3400, "عميل عادي", "خضروات ورقية وبطاطس"),
    ("خالد فيصل بن قاسم اليافعي", "777080044", "مزرعة ردفان الجنوبية", "لحج - حبيل جبر", 3200, "عميل عادي", "مزارع ذرة وأعلاف مواشي"),
    ("عادل مجيب بن عبده الحريبي", "778090045", "حقل خبت الرجاع", "لحج - المضاربة", 3000, "عميل عادي", "ري أحواض موسمية"),
    ("ياسر عصام بن مسعد المسوري", "779010046", "مزرعة جبل مسور", "عمران - مسور", 3700, "صاحب بئر", "مالك بئر المحطة"),
    ("طارق هائل بن سعيد الأهدل", "770120047", "بستان المنصورية الرطب", "الحديدة - المنصورية", 2900, "عميل عادي", "أشجار مانجو بلدي وموز"),
    ("فؤاد عبد الرقيب بن حسين الذماري", "731230048", "حقل وادي زراجة", "ذمار - الحداء", 3500, "عميل عادي", "زراعة ثوم وبصل وحبوب"),
    ("رمزي قائد بن منصور الكبسي", "732340049", "مزرعة قاع سهمان", "صنعاء - خولان", 3600, "عميل عادي", "بساتين مشمش ولوز"),
    ("وليد مقبل بن علي الحيدري", "733450050", "حقل ضوران الأشمور", "عمران - خارف", 3500, "عميل عادي", "محاصيل حبوب وخضار")
]

def generate_csv(filepath):
    """Generate UTF-8 with BOM CSV file compatible with Excel and Mosarib."""
    headers = ["م", "اسم العميل", "رقم الهاتف", "اسم المزرعة", "الموقع", "سعر الساعة", "نوع الحساب", "ملاحظات"]
    with open(filepath, "w", encoding="utf-8-sig") as f:
        f.write(",".join(f'"{h}"' for h in headers) + "\n")
        for i, row in enumerate(CUSTOMERS_DATA, 1):
            line = [str(i), row[0], row[1], row[2], row[3], str(row[4]), row[5], row[6]]
            f.write(",".join(f'"{val}"' for val in line) + "\n")
    print(f"Generated CSV: {filepath} ({len(CUSTOMERS_DATA)} customers)")

def generate_xlsx(filepath):
    """Generate valid OpenXML XLSX file using Python standard zipfile."""
    # Shared strings
    strings = []
    string_map = {}

    def get_str_idx(s):
        if s not in string_map:
            string_map[s] = len(strings)
            strings.append(s)
        return string_map[s]

    headers = ["م", "اسم العميل", "رقم الهاتف", "اسم المزرعة", "الموقع", "سعر الساعة", "نوع الحساب", "ملاحظات"]
    cols_letters = ["A", "B", "C", "D", "E", "F", "G", "H"]

    rows_xml = []

    # Row 1: Header
    header_cells = []
    for col_idx, h in enumerate(headers):
        s_idx = get_str_idx(h)
        ref = f"{cols_letters[col_idx]}1"
        header_cells.append(f'<c r="{ref}" t="s"><v>{s_idx}</v></c>')
    rows_xml.append(f'<row r="1">{"".join(header_cells)}</row>')

    # Data Rows
    for row_num, c in enumerate(CUSTOMERS_DATA, 2):
        row_cells = []
        # Col A: Number
        row_cells.append(f'<c r="A{row_num}"><v>{row_num - 1}</v></c>')
        # Col B: Name
        s_idx = get_str_idx(c[0])
        row_cells.append(f'<c r="B{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col C: Phone
        s_idx = get_str_idx(c[1])
        row_cells.append(f'<c r="C{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col D: Farm
        s_idx = get_str_idx(c[2])
        row_cells.append(f'<c r="D{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col E: Location
        s_idx = get_str_idx(c[3])
        row_cells.append(f'<c r="E{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col F: Hourly Rate (Number)
        row_cells.append(f'<c r="F{row_num}"><v>{c[4]}</v></c>')
        # Col G: Type
        s_idx = get_str_idx(c[5])
        row_cells.append(f'<c r="G{row_num}" t="s"><v>{s_idx}</v></c>')
        # Col H: Notes
        s_idx = get_str_idx(c[6])
        row_cells.append(f'<c r="H{row_num}" t="s"><v>{s_idx}</v></c>')

        rows_xml.append(f'<row r="{row_num}">{"".join(row_cells)}</row>')

    # Shared Strings XML
    sst_entries = "".join(f"<si><t>{saxutils.escape(s)}</t></si>" for s in strings)
    sst_xml = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{len(strings)}" uniqueCount="{len(strings)}">{sst_entries}</sst>'

    # Sheet XML
    sheet_data = "".join(rows_xml)
    sheet_xml = f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>{sheet_data}</sheetData></worksheet>'

    # Content Types
    content_types_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>'''

    # Package Rels
    rels_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>'''

    # Workbook Rels
    wb_rels_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>'''

    # Workbook XML
    workbook_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="العملاء والمشتركون" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>'''

    # Pack into zip
    with zipfile.ZipFile(filepath, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types_xml)
        z.writestr("_rels/.rels", rels_xml)
        z.writestr("xl/_rels/workbook.xml.rels", wb_rels_xml)
        z.writestr("xl/workbook.xml", workbook_xml)
        z.writestr("xl/worksheets/sheet1.xml", sheet_xml)
        z.writestr("xl/sharedStrings.xml", sst_xml)

    print(f"Generated XLSX: {filepath} ({len(CUSTOMERS_DATA)} customers)")

if __name__ == "__main__":
    out_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(out_dir, "external_customers_sample.csv")
    xlsx_path = os.path.join(out_dir, "external_customers_sample.xlsx")
    generate_csv(csv_path)
    generate_xlsx(xlsx_path)
