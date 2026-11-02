status: accepted
<!-- Owner request 2026-11-03. Runs now in worktree D:\Kotlin\mosarib-t023 (branch task/028), in
parallel with 025. Does NOT touch CustomerDetailScreen.kt (025 and 027 change it). -->
# Task 028: Customers screen balance bar: short labels, totals follow every filter

## A. Labels (owner: "عليك للموردين / لك عند العملاء" is not correct)
In `CustomersScreenComponents.kt` → `CustomersBalanceBar`:
- "عليك للموردين" becomes **"عليك"**;
- "لك عند العملاء" becomes **"لك"**.

Keep the colours (عليك = `debt`, لك = `credit`), the dots, the filter icon, and the layout. The
amount is what the owner owes (عليك) or is owed (لك) in total, whatever the account type.

## B. Totals follow the list on screen
Today the totals come from `computeLedgerTotals(rawCustomers, accountFilter)`
(`CustomersScreen.kt:56`). It ignores the search, the status filter, the group filter and any
other chip.
- Compute the two totals from **exactly the list that is displayed** (`customersWithBalance`,
  after search + every filter), with the same per-customer rule `computeLedgerTotals` uses today
  (owners: payable; customers: receivable; the sign gives عليك / لك).
- When no filter or search is active, the numbers must equal today's numbers. Check this on the
  device and in a unit test.
- When a filter is active, show a small hint under the bar labels: "للمعروضين (N)"
  (`labelSmall`, `textMuted`). Without filters, show nothing extra.
- Keep `AmountWithWords` / approximate words as they are today in the bar (do not change how the
  amount is rendered).

## Tests
- A pure function `ledgerTotalsFor(list): Pair<Double, Double>`:
  - empty list gives (0, 0);
  - mixed owners and customers;
  - the totals for a filtered sublist equal the sum of only those items;
  - the full list equals the old `computeLedgerTotals` result for `AccountFilter.ALL`.

## Allowed files
- `features/customers/CustomersScreen.kt`
- `features/customers/CustomersScreenComponents.kt`
- the file that holds `computeLedgerTotals` (move or reuse it; do not change the per-customer
  rule)
- tests under `app/src/test/java/com/example/`

## Done when
- Build and tests pass.
- On the device:
  - the labels read "عليك" / "لك";
  - choosing a group or a status changes both totals to the shown customers only;
  - clearing the filters brings back the old totals.

---
## Review notes (written by Claude)
**Verdict: accepted (2026-11-03, device check). Labels "عليك" / "لك"; unfiltered totals unchanged (1,975,776 / 983,981); with status "عليه" the totals follow the 53 shown customers and "للمعروضين (53)" shows.**
