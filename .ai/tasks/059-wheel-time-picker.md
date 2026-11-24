status: accepted
<!-- Owner request 2026-11-23. -->
# Task 059: Time picker with scroll wheels

File: `core/ui/MosaribTimePickerDialog.kt` only (time part; date part unchanged, signatures unchanged).
Replace the +/− hour and minute controls with **three scroll wheels** side by side (like the iOS /
Android alarm picker): hours 1–12, minutes 00–59, and ص / م. Each wheel:
- `LazyColumn` + snap fling (`rememberSnapFlingBehavior`), 5 visible rows, the centre row highlighted
  (primary colour, bold, larger), rows above/below fade; infinite-feel looping for hours and minutes
  is welcome but optional.
- Initial position = the current value; scrolling updates the big «08:55» display live.
- Numbers always left-to-right (no «45:» reversals). Fix the quick minute chips too: «:00 :15 :30 :45»
  rendered LTR; keep them under the wheels (tap → minute wheel scrolls there).
- Keep: title, big LTR display with period word, «تأكيد الوقت» / «إلغاء», app colours (no purple).
No new dependency. Add a pure helper for 12h↔24h conversion with a unit test if one does not exist.

## Allowed files
core/ui/MosaribTimePickerDialog.kt, one new test.

## Done when
Build + tests pass; hours, minutes and period are chosen by scrolling wheels.
