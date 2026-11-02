status: accepted

<!-- Owner chose Western digits 0-9 everywhere on 2026-11-01. -->

# Task 012: Dark-mode-safe colours and one digit style

## Background
- On the device, light-only colours stay light in dark mode (e.g. the "N حركة" badge). The cause
  is ~900 hard-coded `Color(0x…)` values.
- Dates and times use Arabic-Indic digits (٠٤:٠١ ٢٠٢٦/١١/٠١) while amounts use 0-9.

## Required behaviour
1. **Digits**: Western digits **0-9 everywhere** (owner's decision), including dates, times,
   durations, amounts and percentages. Arabic words and the Arabic month/AM-PM text stay Arabic.
   Change it in `core/util/Formatters.kt` only (locale / `DecimalFormatSymbols` / date patterns).
   PDFs and shared messages follow the same choice.
2. **Colours**: in the files below, replace every `Color(0x…)` and every
   `if (isAppDark()/isDark) A else B` colour pair with `MaterialTheme.colorScheme.*` or
   `MaterialTheme.mosarib.*`. Pick the nearest token, and list any colour with no fitting token in
   the report instead of inventing one. Literal `fontSize` → nearest typography style.
   Files: `CustomerCardItem.kt`, `CustomersScreenComponents.kt`,
   `CustomerImportReviewDialog.kt`, `RecycleBinDialog.kt`, `GroupsDialogs.kt`,
   `HomeComponents.kt`, `ReportsScreen.kt`.

## Done when
- Build and tests pass. `grep -c "Color(0x"` per file before/after is in the report.

---
## Review notes (written by Claude)
_(empty)_

## Parallel-work note (Claude, 2026-11-02)
- `CustomersFilterRow.kt` is **removed** from this task. Task 016 rewrites it, with tokens.
- This task runs in parallel with 011, 016 and 018 in its own worktree. Touch only the files
  listed here and `core/util/Formatters.kt`.

---
## Review notes (written by Claude)
**Verdict: accepted** (2026-11-02). Build and tests pass.

Formatters:
- `ar-u-nu-latn` with US digit symbols keeps Arabic month and day names and "ص / م" while
  printing 0-9;
- the short date uses `Locale.US`.

All 76 `Color(0x…)` in the listed files are gone.

Not checked on the device: the phone disconnected. Claude will look after the merge.

Leftovers, accepted and carried to later tasks:
- `isAppDark()` is still used in `ReportsScreen.kt` (3×), `HomeComponents.kt` (3×),
  `CustomerImportReviewDialog.kt` and `RecycleBinDialog.kt`, and `fontSize = 11.sp` in
  `ReportsScreen.kt` (3×). The report says they were all replaced. Reports go to task 021; the
  two dialogs to task 020.
- `MosaribTimePickerDialog.kt` uses `String.format("%02d", …)` with the default locale. That
  prints Eastern digits on a phone set to Arabic. Task 020 adds `Locale.US`.
