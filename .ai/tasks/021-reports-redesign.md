status: draft

<!-- Owner asked on 2026-11-02 for a better reports screen. Claude audits it on the device and
writes the spec (with a mockup for the owner) before this becomes ready. It runs after task 012,
which migrates the colours in ReportsScreen.kt. -->
# Task 021: Reports screen redesign

To be specified after Claude's on-device audit. Known from code:
- `features/reports/ReportsScreen.kt` is 1,110 lines, with ~30 hard-coded colours and ~21
  `fontSize`;
- the period filter should reuse `core/util/PeriodRange.kt` from task 016.
