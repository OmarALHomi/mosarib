status: ready

<!-- Collected by Claude during device checks on 2026-11-02. 020 and 023 are merged. Runs in the
existing worktree D:\Kotlin\mosarib-t020 (branch task/024), in parallel with 013. Small fixes only. -->
# Task 024: Final polish found on the device

1. **Operations summary card (`VouchersScreen.kt`): the amount in words is cut with "…".** The
   words under "صافي النقد" and under "المقبوض" end with "…". The whole point of the words is
   that users who cannot read digits can read the full amount. Let these texts wrap to **up to 3
   lines** (no ellipsis). If the tile then gets too tall, use `labelSmall` for the words. The
   three small tiles keep equal height (`IntrinsicSize.Min`, as now).
2. **Reports, "حسب المجموعة" (`ReportsScreen.kt`):** the group name is cut ("بدون مج…") because
   the numbers share its line. Put the name on line 1 (full width) and "إيراد · محصّل · آجل" on
   line 2, the same pattern as the debtor rows.
3. **Operations summary card:** "على العملاء" uses `MaterialTheme.mosarib.textMuted` and
   `labelSmall` since task 023. Check that it is still readable next to the other labels, and
   give all three small tiles the same label style.

Do not touch the operation cards or the list items: task 013 adds animations there in parallel.

## Allowed files
`features/vouchers/VouchersScreen.kt` (summary card only), `features/reports/ReportsScreen.kt`.
