status: draft

<!-- Claude: waiting for the owner's decision on the bottom-bar style (decision 2 in
.ai/reviews/device-review-2026-11-01.md). Do not start until this says ready. -->

# Task 009: Status-bar icons and a bottom bar that never hides its label

## Background (seen by Claude on the device, 384dp wide)
- Light theme: status-bar icons (clock, battery) are white on the white top bar, so they are
  invisible on every tab. Dark theme is fine.
- `MosaribNavBar`: the label of the selected tab is drawn under the floating circle, so it is
  hidden. On the first and last tabs (التقارير / الإعدادات) the circle is clipped by the
  screen edge.

## Required behaviour
1. **Status bar**: in `MainActivity` (or wherever `enableEdgeToEdge` / the window is set up), make
   the status-bar and navigation-bar icon colours follow the app theme, not the system theme:
   dark icons when the app is light, light icons when it is dark. The user can change the theme
   inside the app, so react to theme changes (e.g. a `SideEffect` with
   `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`).
2. **Bottom bar**: replace the crested bar with Material 3 `NavigationBar` + `NavigationBarItem`:
   - keep the five custom icons (`AnalyticsChartIcon`, `CustomersBadgeIcon`, `HomeDropHouseIcon`,
     `OperationsClipboardIcon`, `SettingsMaintenanceIcon`) and the labels from `mosaribNavItems`;
   - `alwaysShowLabel = true`, labels `labelMedium` (12sp), single line;
   - indicator colour `MaterialTheme.colorScheme.primaryContainer` (light) / `primary` at 24% alpha
     (dark), selected icon/label `MaterialTheme.colorScheme.primary`, unselected
     `onSurfaceVariant`; container `MaterialTheme.colorScheme.surface` with tonal elevation 3dp;
   - keep the `testTag`s and the `selectable`/Role.Tab semantics from task 004 (NavigationBarItem
     gives these by default — check);
   - selected icon gets a small bounce (`animateFloatAsState` scale 1.0 → 1.12 → 1.0 with a spring)
     when it becomes selected.
   Delete the now-unused crest shape and floating-circle code, and `labelLine2`.
3. Re-check every screen's bottom padding that assumed the old bar height (90dp): HomeScreen,
   CustomersScreenComponents (summary bar + FAB), VouchersScreen (`110.dp` content padding),
   SettingsScreen, ReportsScreen. Content must not hide behind the bar or leave a large gap.

## Allowed files
- `app/src/main/java/com/example/MainActivity.kt`, `MainScreen.kt`
- `core/ui/MosaribNavBar.kt`
- bottom paddings only in: `features/home/HomeScreen.kt`, `features/customers/CustomersScreenComponents.kt`,
  `features/vouchers/VouchersScreen.kt`, `features/settings/SettingsScreen.kt`, `features/reports/ReportsScreen.kt`

## Done when
- Build and tests pass. The report lists every padding value changed (old → new).

---
## Review notes (written by Claude)
_(empty)_
