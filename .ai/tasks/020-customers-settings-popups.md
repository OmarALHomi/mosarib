status: draft

<!-- Becomes ready when 011, 012, 016 and 018 are accepted and merged into master. -->
# Task 020: Migrate customer, settings, feedback and license popups to the shared components

## Goal
Same as 019, for the rest of the app.

## Files and what to migrate
- `features/customers/AddEditCustomerDialog.kt` (form sheet, ~21 colours),
  `CustomersScreenDialogs.kt`, `CustomerImportReviewDialog.kt` (keep the task-002 badge
  `FlowRow` layout), `RecycleBinDialog.kt`, `GroupsDialogs.kt`.
- `features/customers/CustomerDetailComponents.kt` (2 sheets) and the 7 `DropdownMenu`s plus 2
  dialogs in `CustomerDetailScreen.kt`. Menus and dialogs only: the layout belongs to task 011,
  which must already be merged.
- `features/settings/SettingsBackupDialogs.kt`, `RestoreWarningDialog.kt`,
  `SettingsDebugToolsSection.kt` (dialogs).
- `features/feedback/FeedbackDialog.kt`.
- `core/license/LicenseDialog.kt` (1,942 lines, sheet + 2 dialogs + 2 menus; ~42 colours, ~51
  `fontSize`). Keep every licensing behaviour exactly. Styling only.
- `MainScreen.kt`: the top-bar ⋮ menus on `MosaribMenu`.
- `core/ui/MosaribTextField.kt`: its dropdown on `MosaribMenu`.

## Rules
Same as task 019:
- form sheets set `hasUnsavedChanges`;
- destructive items last, after a divider;
- no emoji in labels;
- tokens and typography only;
- texts and behaviour unchanged.

## Done when
- Build and tests pass.
- The same device checks as 019 on: add customer, import review, recycle bin, groups, license.
- The report has a table: popup → component used.

---
## Review notes (written by Claude)
_(empty)_
