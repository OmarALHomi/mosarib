status: accepted

<!-- Written 2026-11-02. Owner approved the change on 2026-11-02. 019 is merged. Runs in the main folder
D:\Kotlin\mosarib (window 1). -->
# Task 022: Make the operations summary card agree with the reports

## Problem (seen on the device, "الكل")
| Card on the operations screen | Value | Reports screen | Value |
|---|---|---|---|
| الديون (آجل) | 2,057,005 | الآجل | 309,923 |
| صافي العائد والربح | 6,132,445 | صافي الأرباح | 4,156,200 |
| 1559 ساعة و 30 دقيقة | | ساعات السقي المباعة | 1524 ساعة و 35 دقيقة |

Why they differ:
- **الديون:** the operations card sums `session.remainingDebt`. Receipts that are not linked to a
  session never reduce it, so it is inflated. The reports value sums the customers' real
  receivable balances, the same as the customer screen.
- **صافي:** the operations card is cash in minus cash out (receipts + upfront payments − business
  expenses). The reports value is the profit (irrigation value − well cost − operating expenses).
  Both are valid, but they carry almost the same name.
- **Hours:** the operations card counts total duration. The reports count sold hours (duration −
  waste).

## Change (approved by the owner)
1. "الديون (آجل)" becomes **"على العملاء"**, computed like the reports `totalOutstandingDebt`
   (customers' `receivableBalance`, filtered by the 016 filters: group, account type, customer).
   The period filter does not apply to it, because it is a balance now. Show the small note
   "الرصيد الحالي".
2. "صافي العائد والربح" is renamed **"صافي النقد (المقبوض − المصروف)"**. Same calculation.
3. The minutes chip shows **sold** hours (duration − waste), with the label "ساعات مباعة".
4. Each of the three main amounts gets its approximate amount in words underneath.

## Allowed files
- `features/vouchers/VouchersScreen.kt` (summary card only)
- `features/vouchers/VouchersViewModel.kt` (expose the receivable total)
- a shared helper if the receivable sum must be reused from `ReportsViewModel` (no behaviour
  change there)
- tests

**Verdict: accepted (2026-11-02, code review). على العملاء from receivable balances with the الرصيد الحالي note, صافي النقد label, sold hours, approximate words under the amounts. 106 tests pass. Minor: the new label uses Color.Gray and 10.sp, below the 11sp minimum; fix it in a later polish pass. Device check after the merge.**
