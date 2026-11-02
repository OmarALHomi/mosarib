status: accepted

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
**Round 1: changes-requested** (2026-11-02). Build and 93 tests pass. Every old menu action is still
there ("فتح حساب العميل", share, PDF, edit, delete). Checked on the device:
- new irrigation form: content flings keep it open ✅;
- clean form + scrim tap closes ✅;
- changed price + back shows "تجاهل التغييرات؟" with "تجاهل" / "متابعة التعديل" ✅.

Two small fixes:

1. **Menus have a grey frame (halo).** The speed-dial menu ("دورة سقي / سند / شراء ساعات") shows a
   thick grey border around a lighter box. `MosaribMenu` adds `Modifier.shadow(6.dp,
   RoundedCornerShape(16.dp))` on top of `DropdownMenu`, which draws its own container, shape and
   elevation, so two surfaces show.

   Fix in `core/ui/MosaribSheets.kt` → `MosaribMenu`: remove the `.shadow(...)` modifier and use
   the `DropdownMenu` parameters (material3 1.3):
   - `shape = RoundedCornerShape(16.dp)`;
   - `containerColor = surfaceContainer`;
   - `tonalElevation = 0.dp`;
   - `shadowElevation = 6.dp` in light, `0.dp` in dark;
   - `border` only in dark.

   Check the speed dial, a card kebab and a filter dropdown on the device.
2. **Wrong "ينتهي السقي في اليوم التالي" notice.** On a new irrigation from 04:45 to 05:45 on the
   same day, the form says "ينتهي السقي في اليوم التالي (2026/11/02)". The bug was already there
   before this task: `SessionDialogs.kt` ~line 300 uses `endDateMillis > startDateMillis`, which is
   true for any later time on the same day. Compare **calendar days** (year + day of year) of the
   start and end, here and in `isInitiallyOvernight`. Show the notice only when the end day is
   after the start day. Add a small pure helper with a unit test (same day, crossing midnight,
   crossing the year end).

Allowed files: `core/ui/MosaribSheets.kt` (the `MosaribMenu` part only),
`features/sessions/SessionDialogs.kt`, a test file. Append a "Round 2" section to
`.ai/reports/019.md`.

**Round 2 verdict: accepted** (2026-11-02). Build and 99 tests pass. On the device:
- the speed-dial menu is a clean single rounded surface, with no grey frame;
- a new irrigation from 05:05 to 06:05 on the same day no longer shows the "اليوم التالي" notice.
