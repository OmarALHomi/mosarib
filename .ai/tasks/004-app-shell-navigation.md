status: accepted

# Task 004: App shell — bottom bar, tab titles, top bar clean-up

## Background (verified by Claude in the code)
- `MainScreen.kt` hosts the five tabs in a `HorizontalPager`, so a horizontal swipe on a filter
  chip row can switch tabs by accident.
- Tab names differ between the bottom bar (`mosaribNavItems` in `MosaribNavBar.kt`:
  "التقارير / العملاء / الرئيسية / سجل العمليات / الإعدادات") and the top bar title in
  `MainScreen.kt` ("إحصائيات / إدارة العملاء / العمليات / الإعدادات").
- Bottom-bar labels use 8–10.5sp; "سجل العمليات" is forced onto two lines.
- The bottom-bar items have no selected/tab semantics for TalkBack.
- Every non-home top bar shows a theme-toggle button (navigationIcon) and a feedback star.
  Theme is already set in Settings (`ThemeModeSection`) and on the Home header.
- The free-operations counter in the top bar uses 9.5sp text and has no content description.

## Required behaviour
1. **Disable swipe between tabs**: `HorizontalPager(userScrollEnabled = false, …)`. Tab changes
   happen only through the bottom bar, back button and existing in-app links.
2. **One name per tab**, used by both bars:
   REPORTS "التقارير", CUSTOMERS "العملاء", HOME "الرئيسية", VOUCHERS "العمليات",
   SETTINGS "الإعدادات". Make the top-bar title read `currentTab.title` (update the `AppTab`
   titles) instead of its own `when`. In `mosaribNavItems` make every label single-line
   (`labelLine2 = null`).
3. **Readable bottom-bar labels**: unselected 11sp, selected 12sp (compact height: 10.5sp /
   11sp), `maxLines = 1`, `softWrap = false`. Remove the two-line branch if it becomes unused.
   Check that five labels still fit at 360dp width; if not, reduce the horizontal padding, not
   the font size.
4. **Semantics**: each bar item gets `Modifier.semantics { role = Role.Tab; selected = isSelected }`
   (use `selectable(selected, onClick, role = Role.Tab)` instead of `clickable` if simpler), and
   the floating circle icon is hidden from accessibility (it duplicates the selected item).
5. **Top bar clean-up** (non-home tabs):
   - Remove the `LivingThemeToggleButton` from `navigationIcon` (keep it on the Home header and
     in Settings).
   - Show the feedback star only when `currentTab == AppTab.SETTINGS`.
   - Free-operations counter: text uses `MaterialTheme.typography.labelSmall` (11sp after task
     003) without a literal `fontSize`, keep its colours, and give the clickable box
     `semantics { contentDescription = "العمليات المجانية المتبقية: $remaining" ; role = Role.Button }`.
     Keep its 36dp visual size but make the touch target at least 48dp
     (`Modifier.minimumInteractiveComponentSize()`).

## Allowed files
- `app/src/main/java/com/example/MainScreen.kt`
- `app/src/main/java/com/example/core/ui/MosaribNavBar.kt`

## Out of scope
- Do not change the bar's shape, colours, animation or height in this task.
- Do not change what each tab shows.

## Done when
- Build and tests pass.
- The report states the final label sizes and whether five labels fit at 360dp.

---
## Review notes (written by Claude)
**2026-11-01 — accepted.** All five points are done. Build and tests pass (re-run by Claude).
- Claude restored `.clip(CircleShape)` on the free-operations counter. It had been dropped,
  which made its ripple square.
- The 360dp fit in the report is an estimate, not a measurement. Confirm it on the device.
- `MosaribNavItemData.labelLine2` is now always null. Remove it when the nav bar is next touched.
