status: accepted
<!-- Owner request 2026-11-23. Same pattern as the irrigation form (tasks 044–049, 055). -->
# Task 056: Well-owner purchase form like the irrigation form

File: `features/wellowners/WellOwnerPurchaseBottomSheet.kt`. Phone today: owner dropdown, tabs
«اختيار (بدء وانتهاء) / من + عدد الساعات», a big «المدة الإجمالية» bar, two tall «ساعات الهدر /
دقائق الهدر» fields, «سعر الساعة», «المدفوع نقداً (اختياري)», a 4-line summary (المدة الصافية بعد
الهدر، إجمالي قيمة الضخ، المستحق لصاحب البئر، المتبقي بذمتك له), notes — needs scrolling.

## Target (top to bottom, one screen on 360×800dp)
1. Owner: `CustomerPickerField` (core/ui) with only well owners and «+ عميل جديد» (new customer as
   well owner — pass `initialName`/well-owner flag if the add sheet supports it; otherwise the quick
   dialog with the «هل هذا مورد» switch on).
2. Time: no tabs. «من» / «إلى» boxes + compact «المدة: [ساعات] [دقائق]» row (reuse the irrigation
   form's pieces: `CompactDurationField`, `sessionEndMillis`, the small «اليوم التالي» chip). Changing
   the duration moves «إلى».
3. One row: «سعر الساعة» | «الواصل (المدفوع الآن)», each with its small تفقيط.
4. Compact card, two columns: «المستحق له» | «المتبقي بذمتك» (red when > 0, green «خالص» at 0),
   تفقيط under each.
5. «خيارات إضافية» (collapsed, auto-open when filled): waste hours/minutes (compact), notes.
Keep the confirmation sheet from 048 and every calculation and save value unchanged.

## Allowed files
features/wellowners/WellOwnerPurchaseBottomSheet.kt; features/sessions/SessionDialogs.kt only to
make `CompactDurationField` / the next-day chip `internal`/public for reuse (no behaviour change).

## Done when
Build + tests pass; the form shows items 1–5 without scrolling.
