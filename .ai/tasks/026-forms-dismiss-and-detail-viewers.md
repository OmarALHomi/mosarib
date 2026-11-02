status: ready
<!-- Ready 2026-11-02: 013 is merged. Runs in worktree D:\Kotlin\mosarib-t023 (branch task/026), in parallel with 025. -->

<!-- Owner request 2026-11-02. Runs IN PARALLEL with 025 after 013 is merged. Files do not overlap
with 025: do NOT touch CustomerDetailScreen.kt, the Settings files, AmountWithWords.kt or the card
kebab menu items in VouchersScreen.kt. -->
# Task 026: Fix the "تجاهل التغييرات؟" behaviour, redesign the operation detail viewers, add-group
in the customers screen

## A. Bug 1: the confirmation shows even when nothing changed (owner: "غير مقبول")
Opening a form (سقي، قبض، صرف، شراء ساعات، عميل جديد/تعديل) and closing it **without touching
anything** shows "تجاهل التغييرات؟". Causes in the code:
- **Edit voucher** (`VouchersScreen.kt` ~1948): `amountStr != initialVoucher.amount.toString()`
  compares the formatted input ("5,000") with "5000.0", so it is always true. `notes` against
  `initialVoucher.notes ?: ""` has the same kind of problem.
- **New voucher:** `paymentMethod != "نقداً"` is true when the default method is something else,
  and a customer preselected from the customer page counts as a change.
- **New irrigation** (`SessionDialogs.kt` ~533): `selectedCustomerId > 0L` and
  `customerInputText.isNotBlank()` are true when the form opens with a preselected customer.
  `manualHoursStr` may be prefilled.

**Fix (all form sheets):** stop guessing. When the form opens, take a **snapshot** of the
user-editable field values exactly as the form shows them:
`val initial = remember { FormSnapshot(...) }`, a small data class per form. Then
`hasUnsavedChanges = currentSnapshot != initial`. Normalise text fields with `trim()` and amounts
with `Formatters.parseAmountInput`, so "5,000" equals "5000".

Forms:
- add/edit voucher (`VouchersScreen.kt`);
- add/edit irrigation (`SessionDialogs.kt`);
- purchase (`WellOwnerPurchaseBottomSheet.kt`);
- add/edit customer (`AddEditCustomerDialog.kt`);
- any other sheet that passes `hasUnsavedChanges`.

Unit test a pure `FormSnapshot` comparison for each form: unchanged gives false; a changed amount
gives true; "5,000" vs 5000.0 gives false.

## B. Bug 2: closing a changed form flickers
Owner's report: when the form **has** changes and he closes it, the sheet **hides for a moment,
comes back**, then the dialog shows. After "تجاهل", the sheet **appears and disappears quickly**.

Required behaviour, in `core/ui/MosaribSheets.kt` → `MosaribBottomSheet` /
`MosaribBottomSheetLazy`:
- With changes, X / back / scrim tap must show the dialog **immediately**, while the sheet stays
  exactly where it is. No hide animation may start before the user decides.
- "متابعة التعديل": the dialog closes and the sheet does not move.
- "تجاهل": **one** smooth hide animation, then `onDismissRequest()`. The sheet must not reappear.
- Without changes, X / back / scrim close at once with one smooth hide.

Hints, verify on the device:
- Make sure nothing calls `sheetState.hide()` while `showDiscardDialog` is pending.
- Check whether the M3 back handler of the sheet window starts a hide before
  `confirmValueChange` rejects it. If so, set `properties = ModalBottomSheetProperties(
  shouldDismissOnBackPress = false)` and handle back **inside** the sheet content with a
  `BackHandler`.
- On "تجاهل", close the dialog first, then `hide()` in the sheet's own scope, then call
  `onDismissRequest()` in `invokeOnCompletion`. Guard against a second call.
- Make sure the parent does not recompose the sheet back in (the parent's `show…` flag must turn
  false only in `onDismissRequest`).

**Acceptance on the device:** for the irrigation and voucher forms, take screenshots every
~100ms (`adb exec-out screencap` in a loop) during close with changes, and confirm there is no
frame with the sheet hidden before the dialog. Describe it in the report.

## C. Redesign the operation detail viewers (owner: "عارض تفاصيل الكرت" not organised)
Files: `core/ui/SessionDetailBottomSheet.kt`, `core/ui/VoucherDetailBottomSheet.kt`,
`core/ui/WellOwnerPurchaseDetailBottomSheet.kt`, `core/ui/OperationDetailComponents.kt`.

One layout for all three, in the same visual language as `OperationCard`. Keep every action and
text.
1. **Header** (`MosaribBottomSheet` header): the type icon in its type colour (session `info`,
   receipt `success`, expense `debt`, discount `discount`, purchase `warning`), the title
   ("دورة سقي #604", "سند قبض #REC-…", …), and a subtitle with the full date ("الاثنين، 2
   نوفمبر 2026 · 02:32 ص"). The status chip (مسدد / غير مسدد / باقي X) sits under the title.
2. **Amount block:** a full-width panel in the type colour at 8%, 12dp radius. The big amount
   (`headlineSmall`, bold, type colour) with the **exact** amount in words under it
   (`bodySmall`, inline: customers see this sheet). For sessions, also two small columns:
   "المدفوع" and "الباقي", each with its amount.
3. **Details list:** one card (`surface`, 1dp `outlineVariant`, 12dp radius) with key–value
   rows, each 48dp, icon + label (muted) on the start and value on the end, 1dp dividers. Show
   only the rows that have a value:
   - العميل (name, tap opens the customer), الهاتف (with call and WhatsApp icon buttons);
   - session: المدة، من – إلى، سعر الساعة، المضخة، الهدر، الخصم، محمّل على (owner);
   - voucher: طريقة الدفع، رقم السند، الفئة، الدورة المرتبطة (tap opens it);
   - purchase: صاحب البئر، المدة، الهدر على المالك، سعر الساعة.
4. **البيان:** the same box as on the cards (type colour at 8%, lines icon), full text.
5. **Linked payments** (sessions): a compact list "سداد · 4,558 · 2/11" with the existing
   expand behaviour.
6. **Footer** (sticky, `MosaribSheets` buttons):
   - primary "إرسال" (WhatsApp or SMS, existing handler), secondary "PDF", then an "تعديل"
     icon button;
   - "حذف" moves into a small ⋮ menu in the header (destructive, last). No big red button
     at the bottom.
   - For an unpaid session, the primary button is **"سداد"** and "إرسال" becomes secondary.

## D. Add group from the customers screen (owner)
In `CustomersFilterRow.kt` / `CustomersScreen.kt`, put a visible chip **"+ مجموعة"** at the end of
the customers dropdown chip row (as before task 016). It opens the existing group editor
(`CustomerGroupEditorDialog`). Keep "إدارة المجموعات" in the ⋮ menu and in Settings.

## Allowed files
- `core/ui/MosaribSheets.kt`
- `features/vouchers/VouchersScreen.kt`: **only** the add/edit voucher sheet's
  `hasUnsavedChanges`, and the call sites of the detail viewers if their parameters change. Not
  the card menus (task 025).
- `features/sessions/SessionDialogs.kt`, `features/wellowners/WellOwnerPurchaseBottomSheet.kt`,
  `features/customers/AddEditCustomerDialog.kt` (snapshot only)
- `core/ui/SessionDetailBottomSheet.kt`, `VoucherDetailBottomSheet.kt`,
  `WellOwnerPurchaseDetailBottomSheet.kt`, `OperationDetailComponents.kt`
- `features/customers/CustomersFilterRow.kt`, `CustomersScreen.kt`
- tests

## Done when
- Build and tests pass.
- On the device:
  - every form closes **without** a question when untouched, and asks at once with no flicker
    when changed;
  - the three detail viewers follow the new layout in light and dark themes;
  - "+ مجموعة" opens the editor.

---
## Review notes (written by Claude)
_(empty)_
