status: accepted
<!-- Owner request 2026-11-22 after phone check (720x1600 px, ~360dp wide). Waits for owner OK. -->
# Task 046: Irrigation form fits one screen, clear «الواصل» / «المتبقي» names

## What the phone shows today (simple mode, new session)
Order: customer → time card (hint line + two big boxes) → «سعر الساعة» + تفقيط → «خيارات إضافية» →
summary card (المدة الكلية / الصافي / صافي الدفع) → «المبلغ المستلم» + «دفع كامل» → red «المتبقي (آجل)».
It needs two scrolls. Big empty gaps (~30–40dp) between every block; «المدة الكلية» and «الصافي»
repeat the same value when there is no waste; the header subtitle repeats "اختر العميل...".

## Target (both modes; full mode keeps its time tabs above the time row)
Everything from customer to «خيارات إضافية» visible without scrolling on a 360×800dp phone:
1. Customer picker (unchanged). Header subtitle: show the selected customer name, or nothing.
2. Time row, compact: two side-by-side boxes «من» / «إلى» (time big, date small under it),
   height ≈ 72dp; under them one line «المدة: ساعة واحدة». Remove the hint line
   "انقر على الساعة لتغيير الوقت…" (the tour already explains it).
3. «سعر الساعة» field (keep its تفقيط, one line, small).
4. Summary card, one compact card with three rows:
   - «المطلوب» = total amount (today «صافي الدفع»)
   - «الواصل» = amount received now
   - «المتبقي عليه» = remaining (red when > 0, green «خالص» when 0)
   Each amount keeps its تفقيط line (small). Show «المدة الكلية / الصافي» only when there is waste.
5. «الواصل» input field: label «الواصل (المدفوع الآن)», with «دفع كامل» as a small chip on the
   same line as the label (not a big box beside the field). Remove the separate red «المتبقي (آجل)»
   box (now in the card).
6. «خيارات إضافية» collapsed row is the **last** element.
7. Spacing between blocks 8–10dp (use `Spacing.kt` tokens), no 24dp+ gaps.
Keep: colours per type, تفقيط under every amount, voice mics, guide targets (move with elements).

## Allowed files
features/sessions/SessionDialogs.kt (AddEditSessionBottomSheet only), core/ui/guide/GuideTexts.kt
(only text that names the renamed labels)

## Done when
Build + tests pass; on the phone a new session shows customer, من/إلى, price, الواصل, the summary
with «المتبقي عليه» and «خيارات إضافية» without scrolling; screenshots in the report.
