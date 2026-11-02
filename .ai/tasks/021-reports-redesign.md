status: accepted

<!-- Written 2026-11-02 after Claude's on-device audit, with a mockup shown to the owner. Owner approved
the design on 2026-11-02. Can run in parallel with 019 and 011: touches only reports files. -->
# Task 021: Reports screen redesign (filters, KPIs with words, trend, debtors, groups)

## Audit (device, 384dp, owner-like test data)
- The period filter is a row of plain chips (الكل، اليوم، هذا الأسبوع، هذا الشهر، تحديد فترة). Every
  other screen now uses dropdown chips (task 016).
- Big amounts have **no amount in words**. That breaks the owner's rule: users who cannot read
  digits check amounts by their words.
- "صرف للعملاء (يُضاف لديونهم) 0 ر.ي" shows even when it is zero.
- There is no comparison with the previous period, no trend over time, no list of who owes the
  most, and no breakdown by group or pump.
- "454:00 س" is a clock format, not "454 ساعة".
- `ReportsScreen.kt` still uses `isAppDark()` 3× and `fontSize = 11.sp` 3× (leftovers from 012).

## A. Filters (top, same component as operations)
Use one `LazyRow` of `FilterDropdownChip` (from `core/ui/FilterBarComponents.kt`):
- **الفترة:** الكل، اليوم، أمس، هذا الأسبوع، هذا الشهر، الشهر الماضي، هذه السنة، من - إلى…
  (use `PeriodRange`; add THIS_YEAR there if missing, with a test);
- **المجموعة:** الكل، each group, بدون مجموعة;
- **نوع الحساب:** الكل، مزارع، صاحب بئر.

Add a "مسح الكل ✕" chip when any filter is not the default. The PDF button stays in the top bar
and exports the filtered report.

`ReportsViewModel` filters customers by group and account type, and sessions, vouchers and
purchases by period. Put the filtering in a pure function and test it.

## B. KPI grid (2 × 2), each tile
- label (`labelMedium`, muted);
- amount (`titleLarge` bold);
- **amount in words** (`Formatters.amountToArabicWordsApprox`, `labelSmall`, same colour,
  max 2 lines);
- comparison line, shown only when the period is not "الكل": "↑ 12% عن الفترة السابقة"
  (`success` when better, `debt` when worse; "—" when the previous value is 0).

The previous period is the same length immediately before. For "هذا الشهر" it is last month.

Tiles:
1. **قيمة السقي (الإيراد)** = `totalRevenue`.
2. **المحصّل نقداً** = `totalCollectedCash`.
3. **صافي الربح** = `netOperatingProfit`, in the `credit` colour when ≥ 0 and `debt` when < 0.
4. **على العملاء الآن** = `totalOutstandingDebt`, with the sub line "من N عميلاً". This
   reflects balances now, so it shows no comparison.

## C. Trend chart: "الإيراد والمحصّل"
- Grouped bars drawn with `Canvas` (no new library). Day buckets for periods up to 31 days,
  month buckets for longer periods and for "الكل" (last 12 months).
- Colours: `primary` for revenue, `success` for collected.
- X labels are short ("1", "15", "نوف"), Y has 3 grid lines with compact numbers ("50k").
- Tapping a bar shows its two values in a small tooltip above it.
- Empty period: show the text "لا توجد حركات في هذه الفترة".

## D. Cards below (keep what exists, improve)
1. **توزيع الربح:** the existing stacked bar and legend. Hide "صرف للعملاء" when it is 0. Each
   amount line gets its amount in words in small text.
2. **أكبر المدينين (new):** the top 5 customers by `receivableBalance` > 0: name, amount in
   `debt`, approximate words. Tapping one opens the customer. "عرض الكل" switches to the
   customers tab with the status filter "عليه" (add a callback if needed, no navigation
   library).
3. **حسب المجموعة (new):** rows of group → revenue · collected · remaining, compact numbers,
   sorted by revenue. Hide the card when there are fewer than 2 groups.
4. **كفاءة الضخ:** sold hours, wasted hours, number of sessions, plus "حسب المضخة" rows (pump →
   hours), top 5.
5. **محاسبة مياه الآبار:** keep. Fix "454:00 س" to "454 ساعة" with `formatDurationArabic`.
6. **أكثر الحسابات استهلاكاً:** keep.

## E. Cleanup
Remove `isAppDark()` and literal `fontSize` from `ReportsScreen.kt`. Use tokens and typography
only.

## Allowed files
- `features/reports/ReportsScreen.kt`, `features/reports/ReportsViewModel.kt`
- new `features/reports/ReportsFilter.kt` (pure filter + period comparison) and
  `features/reports/TrendChart.kt`
- `core/util/PeriodRange.kt` (add THIS_YEAR / previous-period helper only)
- `MainScreen.kt` only for the "عرض الكل" callback to the customers tab with the "عليه" filter
- tests under `app/src/test/java/com/example/`

## Out of scope
- `VouchersScreen.kt`: the operations summary card wording is task 022.
- PDF layout changes.

## Tests
- Filtering by period, group and account type.
- Previous-period computation (this month → last month; a custom range of 10 days → the 10 days
  before it).
- Percentage change, including a previous value of 0.
- Bucket building for the trend: days vs months, and empty buckets filled with 0.

## Done when
- Build and tests pass.
- The report has device screenshots or descriptions in light and dark themes.

---
## Review notes (written by Claude)
**Round 1: changes-requested** (2026-11-02). Build and 103 tests pass. Checked on the device (light
theme). It looks very good:
- the filter chips;
- the 2×2 KPI grid with approximate words and "من 52 عميلاً";
- the profit distribution with words, with "صرف للعملاء" hidden at 0;
- the top debtors, the groups, pumps by hours;
- "454 ساعة".

Four fixes:

1. **Trend chart labels are mirrored against the bars (RTL bug).** The bars are drawn on a
   `Canvas`, which does not mirror in RTL. The X labels are laid out by Compose, which does. On
   the device the bars of the latest months sit on the right, while the right-most label is
   "ديس" and the left end shows "أكت نوف" overlapping.

   Make bars, labels, grid and tooltip use one direction: **oldest on the right, newest on the
   left** (Arabic reading order). Compute each bar's x position once, and place both the bar and
   its label at that x. Show a label under every bar when there are ≤ 8 buckets, otherwise every
   second bar, with no overlap. Put the Y-axis labels on the right edge.
2. **Arabic month labels and compact numbers.**
   - Month labels: use full short Arabic names from one list (يناير، فبراير، مارس، أبريل، مايو،
     يونيو، يوليو، أغسطس، سبتمبر، أكتوبر، نوفمبر، ديسمبر), not "فبر/أبر/يون/أغس". If one does not
     fit, use the month number ("11") with the year under the first bar of each year.
   - Compact numbers: in the chart axis, tooltip and group rows use "2.3 مليون" and "771 ألف"
     instead of "2.3M" / "771k". Keep digits 0-9. Update `ReportsFilter`'s formatter and its
     tests.
3. **Missing separator in the pumping card:** "إجمالي التشغيل: 1559 ساعة و 30 دقيقةهدر المسرب: 34
   ساعة و 55 دقيقة". Put "هدر المسرب" on its own line, or separate the two parts with " • ".
4. **Top debtors: names are cut** ("عصام بن حسين الهم..."). Give the name the full width on line 1
   (with the amount at the end, intrinsic width), and the approximate words on line 2 under the
   name, full width, the same pattern as the operation cards.

Allowed files: `features/reports/TrendChart.kt`, `ReportsScreen.kt`, `ReportsFilter.kt`, its test.
Append a "Round 2" section to `.ai/reports/021.md`.

**Round 2 verdict: accepted (2026-11-02, code review). One coordinate source for bars and labels (oldest on the right), full Arabic month names, compact numbers as مليون / ألف, separate rows for total and waste, debtor rows with the name on line 1 and the words on line 2. 104 tests pass. Device check after the merge (phone disconnected).**
