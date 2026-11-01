status: ready

# Task 005: Home screen polish

## Background (verified by Claude in `features/home/HomeScreen.kt`)
- Section links use plain `Text(...).clickable`, so the touch target is ~18dp tall.
- Arrows point in opposite directions: "عرض الكل →" and "التقرير الكامل ←". In RTL the
  "go forward / see more" arrow is ←.
- The "آخر التسجيلات" section shows only irrigation sessions (`sessions.take(3)`).
- The FloatingActionButton opens the same sheet as the first quick-action card "سقي جديد"
  right above it.
- The empty state is grey text with a hard-coded colour and no action.

## Required behaviour
1. **Links**: replace both clickable texts with `TextButton` (keeps a 48dp touch target),
   labels "عرض الكل ←" and "التقرير الكامل ←", colour `MaterialTheme.colorScheme.primary`,
   style `labelLarge`.
2. **Section title**: rename "آخر التسجيلات" to "آخر دورات السقي".
3. **Remove the duplicate FAB** (the `FloatingActionButton` at the bottom of the Box) and drop the
   now-unneeded extra bottom padding (`contentPadding = PaddingValues(bottom = 90.dp)` →
   `16.dp`). Move the `LuxuryToastNotification` padding from `bottom = 85.dp` to `bottom = 16.dp`.
4. **Empty state** for recent sessions: a centred column with a `WaterDrop` icon (40dp,
   `MaterialTheme.mosarib.textMuted`), the text "لا توجد دورات سقي مسجلة حتى الآن"
   (`bodyMedium`, `textMuted`) and a `TextButton` "سجّل أول دورة سقي" that does the same as the
   "سقي جديد" quick action (`checkOperationAllowed { showAddManualSheet = true }`).
5. **Section titles** "الوصول السريع", "آخر دورات السقي" and "سقي الأسبوع": use
   `MaterialTheme.typography.titleMedium` with `FontWeight.ExtraBold` and no literal `fontSize`.
6. **Quick-action card colours**: replace the hard-coded `iconBgColor` / `iconTint` pairs and the
   `isDarkTheme` checks with tokens from task 003:
   سقي جديد، شراء ساعات، عميل جديد → background `infoContainer`, tint `info`;
   قبض، النسخ الاحتياطي → background `successContainer`, tint `success`;
   صرف → background `debt.copy(alpha = 0.12f)`, tint `debt`.

## Depends on
- Task 003 (uses `MaterialTheme.mosarib`). Do not start before 003 is accepted.

## Allowed files
- `app/src/main/java/com/example/features/home/HomeScreen.kt`

## Out of scope
- No new summary / KPI section (waiting for the owner's decision).
- Do not change `HomeComponents.kt` or the weekly chart.

## Done when
- Build and tests pass.

---
## Review notes (written by Claude)
_(empty)_
