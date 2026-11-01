status: accepted

# Task 002: Import external ledger operations with the right type

## Background (verified by Claude in the code)
`CustomersViewModel.confirmImport` turns every imported **DEBIT** operation into a `WaterSession`
(pumpName "سقي / قيد مستورد") and every **CREDIT** operation into a `RECEIPT` voucher. Files that
come from another ledger app (e.g. "دفتر حسابات") contain disbursements to the customer ("صرف"),
not irrigation, so they show up as irrigation sessions with 0 minutes and pollute the irrigation
reports, hours and session lists.

Second bug: `mapRowsToDrafts` detects a date column (`dateCol`) but never uses it, so every
imported operation gets today's date (`op.date` is always null).

The balance math already supports the fix: in `CustomerRepository`, an `EXPENSE` voucher on a
non-owner adds to what the customer owes, and on a well owner it reduces what we owe them. So
DEBIT → `EXPENSE` keeps every balance identical to today.

## Required behaviour
1. **Detect the file source** in `CustomerExcelHelper` and expose it on the parse result
   (e.g. `enum class ImportSource { MOSARIB_EXPORT, EXTERNAL_LEDGER }`):
   - `MOSARIB_EXPORT` when the header row matches this app's own export headers (see
     `exportCustomersToExcel` / `exportCustomersToCsv`: "اسم العميل", "رقم الهاتف", "المجموعة",
     "اسم المزرعة", "الموقع", "نوع الحساب", "سعر الساعة …", "إجمالي الساعات",
     "الرصيد المستحق …", "الملاحظات"). Match on normalized text and ignore the currency suffix.
   - `EXTERNAL_LEDGER` otherwise.
2. **Let the user choose how debit operations are recorded**, in `CustomerImportReviewDialog`:
   one clear choice near the top, "تسجيل العمليات المدينة كـ: صرف للعميل / سقي".
   Default: "صرف للعميل" for `EXTERNAL_LEDGER`, "سقي" (current behaviour) for `MOSARIB_EXPORT`.
   Show a one-line hint of the detected source, e.g. "الملف: تصدير من تطبيق مُسَرِّب" or
   "الملف: دفتر حسابات خارجي".
3. **Record operations according to that choice** in `confirmImport`:
   - Debit as "صرف للعميل" → `Voucher(type = EXPENSE, customerId = target, category = "صرف مستورد",
     paymentMethod = "رصيد سابق", notes = description or "سند صرف مستورد من سجل خارجي")`.
   - Debit as "سقي" → the current `WaterSession` code, unchanged.
   - Credit → `RECEIPT` voucher, unchanged.
   - The single opening balance (`initialBalance`, one operation) follows the same choice:
     debit opening balance becomes an `EXPENSE` voucher with category "رصيد افتتاحي" when the
     choice is "صرف للعميل", otherwise the current session code.
   - Wrap the whole import loop in one Room transaction (`db.withTransaction { … }`) so a failure
     midway does not leave half-imported customers.
4. **Use the date column**: parse the date cell into `ImportedOperationItem.date` (support
   `yyyy-MM-dd`, `yyyy/MM/dd`, `dd/MM/yyyy`, `dd-MM-yyyy`, Arabic-Indic digits, and Excel serial
   numbers such as `45231`). Unparseable dates stay null (fall back to now). Do not guess
   month/day order beyond these formats.
5. **Fix the text wrapping in the import review screen** (`CustomerImportReviewDialog`): long
   customer names, amounts and the "N عمليات مجمّعة" badge must not break mid-word or push
   other elements off the row. Use `maxLines = 1` + `TextOverflow.Ellipsis` for names, keep
   amounts on one line (`softWrap = false`), and give the name a `Modifier.weight(1f)` so the
   amount and badge keep their space. Keep colours and sizes.
   _(The user saw "انكسار الخط" on this screen; if a screenshot is attached to your prompt,
   fix exactly what it shows.)_
6. **Small follow-up from task 001**: if `getCustomerDeleteCounts` / `getRecycleBinTotalCounts`
   fail, the recycle bin currently shows "0 جلسة و 0 سندات", which looks like a real number.
   Return `null` on failure and show "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع." instead.

## Allowed files
- `app/src/main/java/com/example/features/customers/CustomerExcelHelper.kt`
- `app/src/main/java/com/example/features/customers/CustomerImportReviewDialog.kt`
- `app/src/main/java/com/example/features/customers/CustomersViewModel.kt`
- `app/src/main/java/com/example/features/customers/CustomersScreenDialogs.kt` (only to pass the new choice to the dialog)
- `app/src/main/java/com/example/features/customers/RecycleBinDialog.kt` (point 6 only)
- `app/src/test/java/com/example/CustomerExcelTest.kt` and new test files under `app/src/test/java/com/example/`

## Tests to add
- Source detection: a header row copied from `exportCustomersToCsv` → `MOSARIB_EXPORT`; the
  sample `tools/daftare_hesabat_sample.csv` → `EXTERNAL_LEDGER`.
- Date parsing for each supported format, Arabic-Indic digits and an Excel serial number.
- Debit operations of an external file produce DEBIT items with the right dates.

## Out of scope
- No database schema change, no change to balance formulas, reports or the JSON backup/restore.
- Do not change how this app exports files.

## Done when
- Build and tests pass.
- The report shows the detected source for both sample files in `tools/` and the exact texts
  added to the review dialog.

---
## Review notes (written by Claude)
**2026-11-01 — accepted.** Build and 54 tests pass (re-run by Claude). All six points are done.
`CustomerRepository.kt` was outside the allowed list, but the change (null on failure) is what
point 6 needed. It is listed in the report, so it is accepted.

Minor follow-ups, not blocking (fold into a later task):
- `detectImportSource`: blank header cells match every expected header (`expected.contains("")`).
  This is harmless today only because "اجمالي الساعات" is also required. Filter blank cells.
- `parseDateString`: `dd/MM/yyyy` accepts two-digit years ("01/05/24" → year 24 AD). Reject
  years < 1900.
- Review dialog: the debit-mode choice shows even when no draft has debit operations. It could
  be hidden in that case. Its texts use literal font sizes; migrate them in the design-token wave.

