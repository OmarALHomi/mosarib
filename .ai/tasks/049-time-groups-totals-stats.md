status: accepted
<!-- Owner request 2026-11-22 after phone check. Starts after 048 is merged (same files). -->
# Task 049: Duration box, easier time picker, manage groups, totals tooltip, clearer operations summary

## 1. Irrigation form time (both modes) — `AddEditSessionBottomSheet`
Remove the two tabs (اختيار / يدوي). One layout: «من» and «إلى» boxes, and under them an editable
**duration** row «المدة: [ساعات] [دقائق]» (two small number fields, or −/+ steppers).
- Changing من or إلى recalculates the duration (as today).
- Changing the duration moves «إلى» = «من» + duration (date rolls over midnight correctly).
- Voice input that gives only hours keeps working (fills the duration, إلى follows).
Keep validation (1..MAX_SESSION_MINUTES, waste ≤ duration).

## 2. Time picker — `core/ui/MosaribTimePickerDialog.kt`
Phone shows lavender/purple surfaces (M3 defaults) and the big display reads «55 : 08» because of RTL.
- Colours from the app theme (`MaterialTheme.mosarib` / colorScheme primary teal, surface), no
  purple tints; selected ص/م uses primary, not orange-brown.
- Big display always left-to-right «08:55» (wrap in `CompositionLocalProvider(LocalLayoutDirection
  provides Ltr)`), with «مساءً»/«صباحاً» next to it.
- Easier input: a grid of hour buttons 1–12 (tap = choose), the existing minute chips :00 :15 :30 :45,
  plus small −/+ for exact minutes, and the ص/م toggle. Keep the title, «تأكيد الوقت» / «إلغاء».

## 3. Manage groups from Home (simple mode) — `features/home/HomeCustomerList.kt`
The «المجموعة ▾» menu gets a last item «إدارة المجموعات…» that opens the existing group screens/
dialogs from `features/customers/GroupsDialogs.kt` (list of groups with add, rename/edit members,
delete — the same flows the Customers screen uses). Reuse them; do not write new group logic.

## 4. Totals box — `HomeCustomerList.kt`
Show only the amounts «لك: X» / «عليك: Y» (one line each, no تفقيط lines). Tapping an amount shows
its تفقيط in a tooltip/popup (Material `PlainTooltip` or a small `Popup`), auto-hides after ~3 s.
The box gets shorter; keep the list bottom padding equal to its new height.

## 5. Operations summary card — `features/vouchers/VouchersScreen.kt` (~455–560)
Make it plain and consistent:
- Title «ملخص الفترة: <period>».
- Three equal tiles: «دخل (قبض)» green, «خرج (صرف)» red, «الباقي في الصندوق» (= دخل − خرج) teal.
- One line «ساعات السقي: 1570 ساعة و5 دقائق» (use the duration words helper).
- The tile «على العملاء / الرصيد الحالي» (625,212) disagrees with Home «لك» (1,223,981) and confuses
  users. Replace it by two small lines «لك عند العملاء: X» / «عليك: Y» computed exactly like the Home
  totals (`ledgerTotalsFor` over all customers), with the note «أرصدة حالية لا تتأثر بالفترة».
- Tap an amount → تفقيط tooltip (same component as item 4).

## Allowed files
features/sessions/SessionDialogs.kt, core/ui/MosaribTimePickerDialog.kt,
features/home/HomeCustomerList.kt, features/customers/GroupsDialogs.kt (only to make a dialog
reusable), features/vouchers/VouchersScreen.kt, a new small `core/ui/AmountTooltip.kt`, tests for the
duration → end-time calculation (put the math in a pure function).

## Done when
Build + tests pass; screenshots-level behaviour as above.
