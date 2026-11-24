status: accepted
<!-- Owner request 2026-11-22, agreed in chat. Simple mode only; full mode unchanged. -->
# Task 047: Simple mode — 3 tabs, home = 4 buttons + customer list + totals box

## Changes (only when `LocalSimpleMode.current` is true)
1. Bottom bar (`MainScreen.kt`, `AppTab`): show only الرئيسية، العمليات، الإعدادات. If the current tab
   is CUSTOMERS or REPORTS when simple mode is on, switch to HOME. Deep links / voice that navigate to
   CUSTOMERS go to HOME in simple mode.
2. Home (`features/home/HomeScreen.kt`):
   - The four actions (سقي، قبض، صرف، صوت) become **one row of 4 compact buttons** (icon + one word),
     same colours/actions as today.
   - Hide the KPI/summary cards and every other home section in simple mode.
   - Under the buttons: a filter row of small chips **الكل | لك | عليك | المجموعة ▾** and a search
     icon that expands a search field (filters the list by name/phone).
     «لك» = customers who owe the owner (`CustomerBalanceFilter.DEBTOR`), «عليك» = owner owes them
     (`CREDITOR`). المجموعة ▾ = the existing custom groups (same source as CustomersScreen).
   - Customer list: the existing `CustomerCardItem` (keeps تفقيط, colours), tap opens the customer
     page exactly as from the Customers tab. Sorted by **last operation** (latest session / voucher /
     well-owner purchase date of that customer, newest first; customers without operations last, by name).
   - A fixed totals box at the bottom of the home (above the nav bar, list scrolls behind it):
     «لك: X» and «عليك: Y» for the **currently filtered** customers, using the existing
     `ledgerTotalsFor` (it already handles well-owner netting — do not compute billed−paid yourself).
     تفقيط under each amount (small).
3. `CustomersViewModel`: add `CustomerSort.LAST_ACTIVITY` (computed from data the view model already
   loads; add a field to `CustomerWithBalance` only if needed). Reuse it from Home; do not duplicate
   filtering logic — expose what Home needs from `CustomersViewModel`.
4. Full mode: nothing changes (Home, tabs, Customers screen as today).

## Allowed files
MainScreen.kt, features/home/HomeScreen.kt, features/customers/CustomersViewModel.kt,
features/customers/CustomerRepository.kt (only if a field is needed), features/customers/CustomersScreen.kt
(only to make `ledgerTotalsFor` / filter helpers reusable), new file features/home/HomeCustomerList.kt,
a unit test for the LAST_ACTIVITY ordering.

## Done when
Build + tests pass; simple mode shows 3 tabs; Home shows 4 small buttons, filters, list by last
operation and a totals box that changes with the filter; full mode unchanged.
