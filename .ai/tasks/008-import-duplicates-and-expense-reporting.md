status: ready

# Task 008: Stop duplicate imported operations, clean existing duplicates, fix expense totals

Priority: **urgent**. It affects balances and reports on the owner's real data.

## Background (verified by Claude on the device and in the code)
1. **Re-import duplicates operations.** `CustomersViewModel.confirmImport`, when a draft matches an
   existing customer (`existing != null`, "تحديث بيانات المسجلين مسبقاً" on), updates the
   customer and then inserts **all** its `attachedOperations` / opening balance again. On the
   owner's phone a customer imported twice (once before task 002, once after) has 3 sessions
   "سقي / قيد مستورد" (0 minutes, total 1,800) **and** 3 EXPENSE vouchers "صرف مستورد"
   (total 1,800). The balance shows 3,600 instead of 1,800.
2. **Customer disbursements are counted as operating expenses.**
   - `ReportsViewModel` (~line 242): `operatingExpenses` = every EXPENSE voucher whose customer is
     null **or not a well owner**. An EXPENSE voucher on a normal customer increases that
     customer's debt (`CustomerRepository` balance math), so it is a receivable, not a cost.
     Today it lowers net profit as well. On the device: operating expenses 371,395 and net
     profit 0.
   - `VouchersScreen.kt` ~line 225: the header "المصروف" / "صافي العائد والربح" sums **all**
     EXPENSE vouchers in the same way.

## Required behaviour
### A. No duplicates on re-import (`CustomersViewModel.confirmImport`)
For a draft that resolves to an **existing** customer, skip each operation (and the single opening
balance) that already exists for that customer as an *imported* record:
- imported records are sessions with `pumpName` in {"سقي / قيد مستورد", "رصيد افتتاحي"}, and
  vouchers with `paymentMethod == "رصيد سابق"`.
- match on: same customer, `abs(amount diff) < 0.01`, same notes text (trimmed; the same
  default text that the import writes when the description is empty), and, when the operation has
  a date, the same calendar day. A DEBIT matches either an imported session or an imported
  EXPENSE voucher. A CREDIT matches an imported RECEIPT voucher.
- Count the skipped operations. Extend the success toast:
  "… وتم تخطي N عملية مستوردة مسبقاً". Add the suffix only when N > 0.
- New customers are unaffected.

### B. One-time cleanup tool for existing duplicates
Add a maintenance entry in Settings (backup section, below the existing backup actions):
**"فحص العمليات المستوردة المكررة"**.
1. Scan (read-only) and build a list of duplicate candidates:
   - **Type mismatch twins:** an imported session (`pumpName == "سقي / قيد مستورد"`,
     `durationMinutes == 0`) that has an imported EXPENSE voucher (`category == "صرف مستورد"`) for
     the same customer with the same amount and notes. The session is the duplicate; keep the
     voucher.
   - **Exact twins:** two or more imported records of the same kind (same customer, type, amount,
     notes, same day). Keep the lowest id; the others are duplicates.
   - Pair each record at most once.
2. Show a preview dialog: total count, total amount, and a list (customer name, amount, notes,
   date, "سقي مستورد ↔ سند صرف" or "مكرر"). If there are none: "لا توجد عمليات مكررة".
3. Button "حذف المكرر (N)". Ask for confirmation first with `MosaribConfirmDialogs` style and the
   text "سيتم إنشاء نسخة احتياطية محلية أولاً، ثم حذف N عملية مكررة. يمكنك الاستعادة من النسخة إن
   لزم.". Then **create a local backup first**, using the same function the "نسخة جديدة" button in
   `SettingsLocalBackupsTab` uses. If the backup fails, stop and delete nothing. Then delete the
   duplicates in one Room transaction and show a toast with the count.
4. Put the scan logic in a pure function (no Android types), e.g.
   `ImportDuplicateFinder.find(sessions, vouchers): List<DuplicateCandidate>`, so it can be unit
   tested.

### C. Correct expense totals
- `ReportsViewModel`: `operatingExpenses` = EXPENSE vouchers with `customerId == null` only.
  Add a new stat `customerDisbursements` = EXPENSE vouchers on non-owner customers, and show it in
  the reports screen as its own line "صرف للعملاء (يُضاف لديونهم)" inside the card that shows
  operating expenses. Do not subtract it from net profit. Owner payments stay as they are.
- `VouchersScreen` header: "المصروف" = EXPENSE vouchers with no customer plus EXPENSE vouchers on
  well owners (real cash out for the business). "صافي العائد والربح" uses that value. Do not move
  the calculation into another file in this task; keep it next to the existing code.

## Allowed files
- `features/customers/CustomersViewModel.kt`
- `features/reports/ReportsViewModel.kt`, `features/reports/ReportsScreen.kt`
- `features/vouchers/VouchersScreen.kt` (header totals only)
- `features/settings/SettingsBackupSection.kt`, `features/settings/SettingsViewModel.kt`,
  `features/settings/SettingsBackupDialogs.kt`
- DAO / repository files only if a delete-by-ids or a query is missing (say which in the report)
- new: `features/customers/ImportDuplicateFinder.kt`, tests under `app/src/test/java/com/example/`

## Tests to add
- Re-import of the same drafts for an existing customer inserts 0 operations (pure helper that
  decides "already imported"; extract it if needed).
- `ImportDuplicateFinder`: type-mismatch twins, exact twins, a single non-duplicate record, and
  pairing each record only once.
- Expense split: EXPENSE with no customer is counted as operating; EXPENSE on a normal customer is
  counted as a customer disbursement; EXPENSE on a well owner is counted as an owner payment.

## Out of scope
- No schema change. No change to `CustomerRepository` balance math.
- Do not run the cleanup automatically.

## Done when
- Build and tests pass.
- The report lists the exact texts added and which backup function was used.

---
## Review notes (written by Claude)
_(empty)_
