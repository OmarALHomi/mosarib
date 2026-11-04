status: ready
<!-- Found by Claude on the phone, 2026-11-04, while taking promo screenshots. Three bugs. -->
# Task 037: Wrong customer picked by voice, overlapping PDF columns, Drive list not refreshing

## 1. Voice picks the wrong customer (important)
Said: «مع احمد علي ساعتين ونص من 10,000 ومسلم 5000». No customer is named "احمد علي". There is a
customer "يوسف احمد".
- **Result:** the summary was "فهمت: سقي · يوسف احمد · ساعتان ونصف · من 10,000 · دفع 5,000 · علي",
  and the form opened with **يوسف احمد** selected.
  - The name was cut to "احمد" and matched by one shared word.
  - "علي" became the notes.
- **Fix in `VoiceCommandParser` (name/notes split, task 032 §6):**
  - A known name counts as a match only when **all** its words match the spoken words in order, or
    the whole spoken run equals an alias.
  - One shared word ("احمد") is never a match.
  - Without a full match, the name runs until the duration, an amount, a rate/paid marker or a
    description starter, so here it is "احمد علي".
- **Fix in `HomeScreen.handleVoiceCandidates` and the forms' `initialCustomerFromVoice`:**
  - Auto-select a customer only when `CustomerMatcher.isHighConfidence(ranked)` is true.
  - Otherwise, **even with a single candidate**, open the form with the spoken name in the field
    and show the chooser: the closest customers plus "إضافة عميل جديد باسم «احمد علي»".
  - Never silently pick a low-score match.
- **Tests:**
  - knownNames = [يوسف احمد، احمد سعيد] and the phrase above → customerQuery "احمد علي", notes null;
  - "مع يوسف احمد ساعتين من 10 الف" → "يوسف احمد";
  - `isHighConfidence` is false for a single match below 0.85.

## 2. PDF statement: text overlaps between columns
In the customer statement PDF, the "المدة والسعر" text runs into the "المبلغ" column. Seen in
`docs/promo-video/screens/06b_statement_pdf.png`:
- "ساعتان و 30 دقيقة @ 4,500" overlaps "11,250", which shows as "11,2504,500";
- "6,7504" in another row.

Fix the PDF table so each cell is clipped or wrapped inside its column:
- measure the text and wrap to 2 lines, or shrink the font down to a minimum;
- give "المدة والسعر" more width, taken from "البيان";
- numbers never touch the next column (padding of at least 4 pt).

Check: a long row ("ساعتان و 30 دقيقة @ 12,500"), and a 7-digit amount.

## 3. Google Drive list says "no cloud copies" after an automatic backup
The backup page shows "آخر نسخة إلى Drive: 2026/11/04 - 05:03 ص", but the "سحابة Google Drive (الملفات
المحفوظة)" card shows "لا توجد نسخ سحابية بعد".
- Load the Drive list when the page opens, if an account is linked.
- Reload it when a backup work (periodic or "انسخ الآن") finishes successfully: observe the
  WorkInfo state.
- If the list still comes back empty while a Drive backup is recorded, show the error from the
  list call instead of "لا توجد نسخ". Check that `DriveBackupWorker` uploads to the same folder
  (appDataFolder) and with the same name pattern that the list reads.

## 4. Comprehensive report PDF (`core/util/PdfReportGenerator.kt`), checked by owner and Claude on the phone
**4.1 Accounting (most important): well owners shown as debtors on page 1**
Row "عمر بن سالم الأهدل" in the customers table on page 1:
- billed 78,250 and paid 46,853, so his irrigation debt should be **31,397**;
- the "المتبقي بذمته" column shows **102,853** in red;
- 102,853 is his **payable balance** (what the distributor owes him for water bought), shown on page 6
  under "متبقي له علينا".

The same happens for the other owners (عادل بن قاسم 92,853، سالم بن سالم 87,022 …).
- **Fix:**
  - Page 1 "المتبقي بذمته" must be the **receivable** (irrigation debt) only.
  - Owner payables appear only in the owners table.
  - Never net the two or swap their signs (same rule as the separate owner ledger).
- **Recheck the cards:**
  - "ديون العملاء" (408,873) must sum receivables only;
  - "مستحقات أصحاب الآبار" (1,371,718) must sum payables only.
- **Add a unit test** with one farmer and one owner, checking the per-row value and both totals.

**4.2 Layout**
1. Page 1: the title "جدول كشف حسابات العملاء وساعات التوزيع:" touches the bottom row of summary
   cards. Add at least 12 pt of space.
2. Owners table, "شراء: مسجل / محتسب / هدر" column:
   - values are cut with "…01" or "…00";
   - names are cut too ("عادل بن قاسم ال…");
   - fix: widen the column, or print the three values on a second line, and allow names up to about
     26 characters.
3. **Orphan page:** page 7 holds a full header and a single owner row. Keep-together rule: if fewer
   than 3 rows would start a new page, move them, or fit the last row on the previous page when
   space allows. Never start a page for one row plus a header when it fits.
4. **Bidi of durations:**
   - "(23س و 45د)", "س 01:30" and "24:15 س" print in a mixed order;
   - wrap each duration in RTL marks, or format it as "23 س 45 د" built with U+200F (RLM) around the
     parts, like the app does;
   - check page 1 and the owners table.
5. **Footer:**
   - "1 صفحة" / "7 صفحة" shows reversed, so write "صفحة 1 من 7";
   - Latin "wa.me/…" wrapped in U+2066 … U+2069 (LRI/PDI).

Add before/after screenshots of pages 1, 6 and 7 to the report.

## Allowed files
- `core/voice/VoiceCommandParser.kt`, `core/voice/CustomerMatcher.kt`;
- `features/home/HomeScreen.kt`, plus the `initialCustomerFromVoice` parts of `SessionDialogs.kt`,
  `VouchersScreen.kt` and `WellOwnerPurchaseBottomSheet.kt`;
- `core/util/PdfReportGenerator.kt` (statement and comprehensive report);
- `core/backup/DriveBackupWorker.kt`, `core/util/GoogleDriveBackupHelper.kt`,
  `features/settings/SettingsBackupScreen.kt` / `SettingsBackupSection.kt`;
- tests.

## Done when
- Build and tests pass.
- The report includes the PDF page before and after, and the backup page showing the Drive list
  after "انسخ الآن".
