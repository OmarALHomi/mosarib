status: ready

<!-- Owner request 2026-11-02. Do this AFTER task 015 is accepted. -->
# Task 016: Organised filters (period, group, customer…), reachable recycle bin and groups

## Background
- **Operations screen** (`features/vouchers/VouchersScreen.kt` + `VouchersViewModel.kt`):
  - the only filters are the type chips (`OperationsFilter`: الكل، سقي، مقبوضات، مصروفات، آجل،
    شراء ساعات) and the search;
  - there is no way to see a period (this month, a custom range), a group, or one customer;
  - the summary card at the top always shows **all-time** totals (`allVouchers` / `allSessions`).
- **Customers screen** (`CustomersFilterRow.kt`, `CustomersScreen.kt`):
  - three dropdown chips (النوع، الحالة، المجموعة) plus a "+ مجموعة" chip share one row;
  - **"سلة المحذوفات"** and **"إدارة المجموعات"** are hidden as the last items of the
    *group filter* dropdown, where nobody looks for them.
- `ReportsViewModel` already has `ReportPeriod` and a custom range with `MosaribDatePickerDialog`.
  Reuse that period logic: move the date math into a shared pure helper rather than copy it.

## A. Shared filter model (pure, unit tested)
New `features/vouchers/OperationsFilterState.kt`:
```kotlin
data class OperationsFilterState(
    val type: OperationsFilter = OperationsFilter.ALL,   // existing enum, the quick chips
    val period: ReportPeriod = ReportPeriod.ALL,         // reuse; CUSTOM uses the range below
    val customRange: Pair<Long, Long>? = null,           // inclusive start-of-day .. end-of-day
    val group: String? = null,                           // null = all; "" = customers with no group
    val customerId: Long? = null,
    val sessionPayment: SessionPaymentFilter = SessionPaymentFilter.ALL, // ALL, PAID, PARTIAL, UNPAID
    val paymentMethods: Set<String> = emptySet(),        // vouchers' paymentMethod values
    val pumpNames: Set<String> = emptySet(),             // sessions' pumpName values
    val sort: OperationsSort = OperationsSort.NEWEST     // NEWEST, OLDEST, AMOUNT_DESC
) { val activeCount: Int /* number of non-default fields except type */ }

object OperationsFilterEngine {
    fun apply(ops: List<UnifiedOperation>, state: OperationsFilterState, customers: Map<Long, Customer>,
              query: String, now: Long): List<UnifiedOperation>
    fun describe(state: OperationsFilterState, customerName: String?): String  // "هذا الشهر · مجموعة الشرق"
}
```
Rules:
- **Group:** the operation's customer belongs to the group (purchases: the owner).
- **Session payment:**
  - PAID: `remainingDebt <= 0.01`;
  - UNPAID: `amountPaid <= 0.01` and debt > 0;
  - PARTIAL: everything else with debt. It applies to sessions only; when it is not ALL, other
    types are hidden.
- **Payment methods and pumps:** the options are the distinct values found in the data, not a
  fixed list.
- **Search:** the existing search is kept inside `apply`.
- `VouchersViewModel` exposes `filterState: StateFlow<OperationsFilterState>` and
  `updateFilter((OperationsFilterState) -> OperationsFilterState)`, plus `clearFilters()`.
- `unifiedOperations` uses the engine.
- The state lives in the ViewModel only. It is not saved across app restarts.

## B. Operations screen UI
1. **Top row:** the search field (`weight 1f`), then a **"فلترة"** button (tonal, tune icon). It
   carries a small red badge with `activeCount` when > 0. The PDF button moves into the top app
   bar actions or stays at its current place; do not lose it.
2. **Quick type chips:** one horizontally scrolling row, as today.
3. **Active filters row** (only when `activeCount > 0`):
   - removable chips, one per active filter ("هذا الشهر ✕", "مجموعة الشرق ✕", "عمر احمد ✕", …);
   - at the end, a text button "مسح الكل".
4. **Filter sheet** (`ModalBottomSheet`). Sections, each a `FlowRow` of `FilterChip`s:
   - **الفترة:** اليوم، أمس، هذا الأسبوع، هذا الشهر، الشهر الماضي، كل الوقت، "من - إلى" (opens
     the existing date range picker). Use the `ReportPeriod` values that exist; add missing ones
     to the shared helper.
   - **المجموعة:** الكل، each group, "بدون مجموعة".
   - **العميل:** a field that opens a searchable customer picker. A clear (✕) button removes it.
   - **حالة السداد** (sessions): الكل، مسدد، جزئي، غير مسدد.
   - **طريقة الدفع / المضخة:** multi-select chips from the data. Hide the section if there are
     fewer than 2 values.
   - **الترتيب:** الأحدث، الأقدم، الأعلى مبلغاً.
   - Footer: "مسح الكل" (text button) and a primary button **"عرض N عملية"** with the live count.
     Filters apply when the user taps it.
5. **Summary card follows the filters.** Received, expenses, debts, net and minutes are
   computed from the **filtered** list, minus the type chip, so the card stays meaningful while a
   type chip is on. Above the numbers, show the filter description in `labelSmall` muted: "ملخص:
   هذا الشهر · مجموعة الشرق". With no filters, show "ملخص: كل الوقت".
6. **PDF:** "طباعة سجل العمليات" exports the **filtered** list, with
   `filterTitle = OperationsFilterEngine.describe(...)` (type name included).
7. **Empty result:** use the existing `EmptyStateView` with "لا توجد عمليات مطابقة للفلاتر" and a
   button "مسح الفلاتر".

## C. Customers screen
1. **Top row:** the search field and a "فلترة" button with a badge, same component as the
   operations screen. Put the shared button and the active-chips row in
   `core/ui/FilterBarComponents.kt`.
2. **Quick chips row:** الكل، عليه، له، مسدد (the balance filter), then "أصحاب الآبار" (the
   account filter WELL_OWNERS) as a toggle.
3. **Filter sheet:**
   - **النوع:** الكل، مزارعون، أصحاب آبار;
   - **المجموعة:** chips, plus a last chip **"+ مجموعة جديدة"** that opens the existing add-group
     dialog;
   - **الترتيب:** the existing `CustomerSort` options.
4. **Remove** from the filter row: the three dropdown chips, the "+ مجموعة" chip, and the
   "إدارة المجموعات" and "سلة المحذوفات" items of the group dropdown.
5. The summary bar (لك / عليك) keeps following the filters, as today.

## D. Recycle bin and groups: easy to reach
- **Customers top bar ⋮ menu** (in `MainScreen.kt`, next to import/export), first two items:
  - "إدارة المجموعات" opens `ManageGroupsDialog`, which already has "add new group";
  - "سلة المحذوفات (N)" opens `RecycleBinDialog`, with N = archived count, hidden when 0.
- **Settings:** add a section **"البيانات"** (or put it in the existing data/maintenance area near
  backup) with two rows:
  - "سلة المحذوفات" with the count, opening `RecycleBinDialog`;
  - "إدارة المجموعات", opening `ManageGroupsDialog`.

  Both dialogs take a `CustomersViewModel`; obtain it with `viewModel()` there, as
  `RecycleBinDialog` already does.
- Keep the restore and delete behaviour exactly as it is (task 001).

## Parallel-work note
- The rewritten `CustomersFilterRow.kt` and every new composable use theme tokens and typography
  only (no `Color(0x…)`, no `fontSize`). Task 012 no longer touches that file.
- Use plain Material 3 `ModalBottomSheet` for the filter sheets with
  `skipPartiallyExpanded = true`. Task 019 moves them onto the shared `MosaribBottomSheet` later.

## Allowed files
- `features/vouchers/VouchersScreen.kt` (header, filters, summary card and PDF call only)
- `features/vouchers/VouchersViewModel.kt`
- new `features/vouchers/OperationsFilterState.kt`
- new `core/ui/FilterBarComponents.kt`
- new `core/util/PeriodRange.kt`, the shared period math; `ReportsViewModel.kt` may call it
  instead of its private copy (no behaviour change)
- `features/customers/CustomersFilterRow.kt`, `CustomersScreen.kt`, `CustomersViewModel.kt`
  (only if a field is missing)
- `MainScreen.kt` (customers ⋮ menu items only)
- `features/settings/SettingsScreen.kt`, `SettingsGeneralSections.kt` (the new rows only)
- unit tests under `app/src/test/java/com/example/`

## Out of scope
- The operation cards (task 015).
- Styling of popup menus and dialogs, planned later (task 017 draft).
- Reports screen UI.

## Tests
- `OperationsFilterEngine.apply`:
  - period this month and a custom range (inclusive end day);
  - group, including "بدون مجموعة";
  - one customer;
  - each session payment state;
  - payment methods;
  - the three sort orders;
  - search combined with a filter.
- `activeCount` and `describe`.
- `PeriodRange`: today, yesterday, this week (week starts **Saturday**), this month, last month,
  and the year boundary (January → December).

## Done when
- Build and tests pass.
- The report includes screenshots or a description of the filter sheet on both screens, and lists
  where the recycle bin and group management can now be opened.

---
## Review notes (written by Claude)
_(empty)_
