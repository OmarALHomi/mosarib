status: ready

<!-- ready 2026-11-02: 016 and 018 are merged. Runs in its own worktree D:\Kotlin\mosarib-t019. -->
# Task 019: Migrate irrigation, voucher and well-owner popups to the shared components

## Goal
Every sheet, dialog and menu in these files uses `MosaribBottomSheet` / `MosaribDialog` /
`MosaribMenu` from task 018. That fixes the accidental sheet dismiss there too, and makes the
popups consistent.

## Files and what to migrate
- `features/sessions/SessionDialogs.kt` (2,685 lines: add/edit irrigation sheet, settle dialog,
  pickers, menus). Also replace its ~90 `Color(0x…)` and ~16 `fontSize` with theme tokens and
  typography.
- `features/sessions/SessionComponents.kt` (its menu).
- `features/wellowners/WellOwnerPurchaseBottomSheet.kt` (sheet + menu; ~11 colours).
- `features/vouchers/VouchersScreen.kt`: add-voucher / add-expense sheets, delete dialogs, the
  kebab menus of the cards, the speed-dial. Do not change the cards or the 016 filters.

- `core/ui/FilterBarComponents.kt`: the dropdown of `FilterDropdownChip` on `MosaribMenu` /
  `MosaribMenuItem`, keeping the check mark on the selected option.
- The two "اختيار عميل…" / date-range dialogs that 016 added in `VouchersScreen.kt` go on
  `MosaribDialog` / `MosaribBottomSheet`.

## Rules
- Forms (add/edit irrigation, voucher, purchase) pass `hasUnsavedChanges = true` once any field
  is touched.
- Menus: destructive items ("حذف") last, after a divider. No emoji.
- Texts, validation, behaviour and handlers stay exactly the same.

## Done when
- Build and tests pass.
- On the device:
  - each migrated sheet survives "scroll to top, fling down ×5";
  - a half-filled form asks "تجاهل التغييرات؟" on X, back and scrim tap.
- The report has a table: popup → component used.

---
## Review notes (written by Claude)
_(empty)_
