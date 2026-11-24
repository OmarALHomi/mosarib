status: accepted
<!-- Owner request 2026-11-24. -->
# Task 069: One add-customer form everywhere; keep the last 10 daily backups

## 1. One form for adding a customer or supplier
`AddEditCustomerBottomSheet` (features/customers/AddEditCustomerDialog.kt, redone in 068: big
«استيراد من جهات الاتصال», basics, collapsed «المزيد») is the only add/edit customer form.
The only difference for a supplier is `initialIsWellOwner = true` (the «مورد» switch starts on).

The irrigation form (`AddEditSessionBottomSheet`, features/sessions/SessionDialogs.kt) still has its
own small dialog `showQuickCustomerDialog` (title «تسجيل عميل جديد», name, phone, switch, «حفظ وتحديد
العميل»), opened from 4 places (voice name not found ×2, save with an unknown typed name, the picker's
«+ عميل جديد»). The same file already has a `showAddCustomerSheet` block that shows
`AddEditCustomerBottomSheet` and selects the new customer.
- Make all 4 places open `showAddCustomerSheet` instead (pass the spoken/typed name as `initialName`,
  as the quick dialog did through `quickName`).
- Delete the quick dialog and its state (`quickName`, `quickPhone`, `quickIsWellOwner`, …) if unused.
- After saving in the sheet: the new customer is selected, `customerInputText` = its name, price
  filled as today (sale price only for non-owners), the sheet closes, and the toast «تم حفظ وتحديد: X»
  shows as the quick dialog did. Errors keep using `customerSaveError`.
- `features/customers/CustomersScreen.kt`: when the list filter is «صاحب بئر» (WELL_OWNERS), adding a
  new customer opens the sheet with `initialIsWellOwner = true`. Skip this if the filter state is not
  reachable in that file without other changes.

## 2. Daily auto backup keeps the last 10
`core/util/AutoBackupEngine.kt`: `MAX_DAILY_BACKUPS` 7 → 10 and the KDoc «آخر 7 أيام» → 10. Pruning
already runs after each daily backup (deletes older files automatically); keep that. If any
user-visible text mentions how many daily copies are kept, update it to 10.

## Allowed files
features/sessions/SessionDialogs.kt, features/customers/CustomersScreen.kt, core/util/AutoBackupEngine.kt,
and the settings/backup screen file only if it shows the "7" count as text.

## Done when
Build + tests pass; every «+ عميل جديد» / unknown name opens the same 068 sheet; daily backups keep 10.
