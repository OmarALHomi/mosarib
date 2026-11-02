status: accepted

<!-- 2026-11-02: second half of the old task 020. Runs in parallel with 020 in its own worktree
D:\Kotlin\mosarib-t023. The files do not overlap with 020. -->
# Task 023: Move the settings, feedback, license and app-bar popups to the shared components

## Goal
Same as task 020, for the remaining popups. Use the components from task 018. The results of
019 are the reference.

## Files (only these)
- `features/settings/SettingsBackupDialogs.kt` (5 dialogs), `RestoreWarningDialog.kt`,
  `SettingsDebugToolsSection.kt` (2 dialogs) → `MosaribDialog`. Restore and delete confirmations
  use `tone = Danger`.
- `features/settings/SettingsScreen.kt` / `SettingsGeneralSections.kt`: only if they hold a
  popup that is not migrated yet.
- `features/feedback/FeedbackDialog.kt`: sheet → `MosaribBottomSheet` with
  `hasUnsavedChanges` once text is typed.
- `core/license/LicenseDialog.kt` (1,942 lines: sheet + 2 dialogs + 2 menus; ~42 colours, ~51
  `fontSize`). **Styling only. Every licensing behaviour, check, text and flow stays exactly
  the same.** Do not change anything in `LicenseManager`, Firebase calls, activation keys or
  device codes. Replace the colours and sizes with tokens and typography.
- `MainScreen.kt`: the top-bar ⋮ menus (customers import/export, groups, recycle bin, …) →
  `MosaribMenu` / `MosaribMenuItem`. Destructive items last; no emoji.
- `core/ui/MosaribTextField.kt`: its dropdown → `MosaribMenu`.
- `core/ui/MosaribTimePickerDialog.kt`: shape and colours with tokens (as `MosaribDialog`), and
  `String.format(Locale.US, "%02d", …)` everywhere, so digits stay 0-9 on phones set to Arabic
  (a 012 leftover).
- `features/vouchers/VouchersScreen.kt`: **only** the "على العملاء" label added in 022, which
  uses `Color.Gray` and `fontSize = 10.sp`. Change it to `MaterialTheme.mosarib.textMuted` and
  `labelSmall`.

## Rules
Same as task 020:
- sheets do not close on content drag;
- form sheets set `hasUnsavedChanges`;
- destructive items last, after a divider;
- no emoji;
- tokens and typography only;
- texts and behaviour unchanged.

## Done when
- Build and tests pass.
- On the device:
  - the license sheet opens, scrolls and closes only with X, back or a scrim tap;
  - the backup restore warning and the delete dialogs show the danger tone;
  - the time picker shows 0-9;
  - the customers ⋮ menu has no grey frame.
- The report has a table: popup → component used. For the license dialog it confirms that no
  logic line changed (a diff of only styling lines).

---
## Review notes (written by Claude)
**Round 1: changes-requested** (2026-11-02). Build and tests pass. Checked on the device:
- the customers top-bar ⋮ menu is a clean single surface;
- the subscription renewal dialog survives drags and closes with back and an outside tap;
- the settings dialogs build.

Claude reviewed the `LicenseDialog.kt` diff line by line. All changes are styling **except one**:

**Restore the original rule for a mandatory license sheet.** Before:
```kotlin
confirmValueChange = { sheetValue ->
    if (isMandatory) sheetValue != SheetValue.Hidden else true
}
```
Now `if (isMandatory) false` rejects **every** transition, including opening to `Expanded`.
That may stop the mandatory sheet from opening, or from expanding fully, when free operations run
out. That is the licensing gate, so it must behave exactly as before.

Fix:
```kotlin
if (isMandatory) {
    sheetValue != SheetValue.Hidden          // exactly as before
} else if (sheetValue == SheetValue.Hidden) {
    … your scrim-tap / drag logic (keep it) …
} else true
```
Do not touch any other logic line.

Allowed file: `core/license/LicenseDialog.kt`. Append a "Round 2" section to
`.ai/reports/023.md`.

**Round 2 verdict: accepted** (2026-11-02). The mandatory rule is back exactly as it was (`sheetValue != Hidden`). The only other `isMandatory` lines in the diff move the "المتابعة مجاناً" button into `MosaribDialog` with the same condition. Build and tests pass.
