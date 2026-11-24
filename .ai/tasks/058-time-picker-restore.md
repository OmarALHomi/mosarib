status: accepted
<!-- Owner request 2026-11-23: the old time picker (before task 049) was easier. -->
# Task 058: Restore the previous time picker, keep only small fixes

File: `core/ui/MosaribTimePickerDialog.kt` only.
1. Restore the time dialog layout from before task 049 (`git show afa1f33^:app/src/main/java/com/example/core/ui/MosaribTimePickerDialog.kt`
   — afa1f33 is the 049 commit): big display, «الفترة» صباحاً/مساءً buttons, «الساعة» and
   «الدقيقة» with +/− buttons, quick minute chips :00 :15 :30 :45, «تأكيد الوقت» / «إلغاء».
   Remove the 1–12 hour grid added by 049.
2. Keep these small fixes on top of the old layout:
   - colours from the app theme (`MaterialTheme.mosarib` / colorScheme surface, surfaceVariant,
     primary) — no lavender/purple M3 tints; the selected ص/م uses primary (not orange-brown);
   - the big display always left-to-right «08:55» (LocalLayoutDirection Ltr around it) with the period
     word next to it.
   - keep the date picker part as it is now.
3. Same public function signatures as now (callers must not change).

## Allowed files
core/ui/MosaribTimePickerDialog.kt.

## Done when
Build + tests pass; the dialog looks like the old one in app colours, display reads «08:55».
