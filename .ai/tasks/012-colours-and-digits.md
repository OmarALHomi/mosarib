status: ready

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
   Files: `CustomerCardItem.kt`, `CustomersScreenComponents.kt`, `CustomersFilterRow.kt`,
   `CustomerImportReviewDialog.kt`, `RecycleBinDialog.kt`, `GroupsDialogs.kt`,
   `HomeComponents.kt`, `ReportsScreen.kt`.

## Done when
- Build and tests pass. `grep -c "Color(0x"` per file before/after is in the report.

---
## Review notes (written by Claude)
_(empty)_
