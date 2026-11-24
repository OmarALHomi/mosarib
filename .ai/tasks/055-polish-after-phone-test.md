status: accepted
<!-- Owner OK 2026-11-23 after phone test of 042–054. -->
# Task 055: Six small fixes from the phone test

1. **Duration row** (`AddEditSessionBottomSheet`, SessionDialogs.kt): the «المدة» hours/minutes fields
   are ~100dp tall. Make them compact (dense fields or −/+ steppers, ≈48dp), minutes shows «0» not empty.
   The «اليوم التالي» info line: keep, but as a small one-line chip under «إلى», not a full-width box.
2. **Menu colour**: `MosaribMenu` (core/ui) shows a lavender M3 background. Use the app surface colour
   (`MaterialTheme.colorScheme.surface` / mosarib tokens) with the usual border/shadow, so the customer
   picker and every other menu match the app.
3. **«تسوية وتصفير باقي الجلسة كخصم»** (orange row in the session form, shown whenever الواصل <
   المطلوب): move it inside «خيارات إضافية» (next to the discount). Same behaviour.
4. **Huge remaining-operations number**: with a big dashboard limit the header circle and Settings
   «حالة الاشتراك» show «1783788676». When remaining > 99,999 show «∞» in the circle and
   «غير محدود» in text. Find every place that renders the remaining count (header badge in
   MainScreen/Settings, the 999999 badge on screens, Settings row) and use one small formatter.
5. **Spoken name pre-filled in a new customer**: when «+ عميل جديد» is opened from a picker that has an
   unrecognised voice name (voucher form, session form), the add-customer sheet opens with that name in
   the name field, in **add** mode (not edit). Add an optional `initialName: String = ""` parameter to
   `AddEditCustomerBottomSheet` (or the quick dialog) and pass it.
6. **Home tour text**: `core/ui/guide/GuideTexts.kt` still says «سند جديد»; it should talk about the
   «قبض» and «صرف» buttons.

## Allowed files
features/sessions/SessionDialogs.kt, core/ui/MosaribMenu*.kt (wherever MosaribMenu lives),
MainScreen.kt, features/settings/SettingsScreen.kt, features/vouchers/VouchersScreen.kt,
the file that defines AddEditCustomerBottomSheet, core/ui/guide/GuideTexts.kt,
a small formatter in core/util/Formatters.kt + test.

## Done when
Build + tests pass; items 1–6 visible as described.
