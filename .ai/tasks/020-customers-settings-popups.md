status: ready

<!-- 2026-11-02: split into two parallel halves. 020 = customer popups (worktree
D:\Kotlin\mosarib-t020). 023 = settings, feedback, license, app bar, shared fields (worktree
D:\Kotlin\mosarib-t023). The files do not overlap. -->
# Task 020: Move the customer popups to the shared components

## Goal
Every sheet, dialog and menu in the files below uses `MosaribBottomSheet` /
`MosaribBottomSheetLazy` / `MosaribDialog` / `MosaribMenu` / `MosaribMenuItem` from task 018.
That stops sheets from closing when a list is dragged, and makes the popups look the same.
The results of 019 (irrigation and voucher popups) are the reference.

## Files (only these)
- `features/customers/AddEditCustomerDialog.kt`: the add/edit customer form sheet (~21
  colours) → `MosaribBottomSheet` with `hasUnsavedChanges` and a sticky footer.
- `features/customers/CustomersScreenDialogs.kt`: its sheet and dialog.
- `features/customers/CustomerImportReviewDialog.kt`: the import review sheet →
  `MosaribBottomSheetLazy` (long list). Keep the task-002 badge `FlowRow` layout and the
  full-width name. Remove `isAppDark()` (a 012 leftover); use tokens only.
- `features/customers/RecycleBinDialog.kt` (3 dialogs): remove `isAppDark()`. The confirm
  dialogs for "حذف نهائي" and "إفراغ السلة" use `MosaribDialog(tone = Danger)`.
- `features/customers/GroupsDialogs.kt` (3 dialogs, including `ManageGroupsDialog` and the
  group editor).
- `features/customers/CustomerDetailComponents.kt`: its 2 sheets.
- `features/customers/CustomerDetailScreen.kt`: **only** its 7 `DropdownMenu`s → `MosaribMenu`,
  and its 2 dialogs → `MosaribDialog`. Do not touch the layout, which belongs to task 011.
- `features/customers/CustomerCardItem.kt`: its "خيارات" menu → `MosaribMenu`.
- `features/customers/CustomersFilterRow.kt`: any remaining `DropdownMenu` / dialog.

## Rules
- Form sheets set `hasUnsavedChanges` as soon as a field changes.
- Destructive items ("حذف", "أرشفة", "حذف نهائي") go last, after `MosaribMenuDivider()`, with
  `destructive = true`.
- No emoji in labels.
- Tokens and typography only: no `Color(0x…)`, no `fontSize`, no `isAppDark()` in the code you
  touch.
- Texts, validation, behaviour and handlers stay exactly the same. Do not remove any menu item.

## Done when
- Build and tests pass.
- On the device: the add customer and import review sheets survive "scroll to top, fling down
  ×5"; a half-filled add-customer form asks "تجاهل التغييرات؟" on X, back and a scrim tap; the
  recycle bin and the groups dialogs open and work.
- The report has a table: popup → component used, and the menu items before → after for every
  migrated menu.

---
## Review notes (written by Claude)
_(empty)_
