# Review 037, round 1 (Claude)

**Build and tests:** pass (compileDebugKotlin and testDebugUnitTest).

**Accepted:**
- §1 voice name match and `isHighConfidence` in Home and the three forms. The tests cover the required cases.
- §2 statement columns, using `drawClippedCell`.
- §3 Drive list refresh and the list error.
- §4.2 title spacing, `formatBidiDuration`, "صفحة X من Y", LRI/PDI.

## Blocker: §4.1 now counts the owner's irrigation debt twice
The mistake in the task wording was Claude's: "never net the two" is wrong for this app.

The app's accounting model nets the well-owner account. `features/customers/CustomerRepository.kt` `calculateCustomerBalance`:

```
netOwnerPayable = (netPurchase + collectedCash) - (disbursed + (sales - discounts))
payable    = netOwnerPayable if > 0, else 0
receivable = -netOwnerPayable if < 0, else 0
```

So for «عمر بن سالم الأهدل»:
- `payableBalance` 102,853 is **already net** of his irrigation debt of 31,397.
- `receivableBalance` = 0.

The customer card and the statement use the same model.

The round-1 code shows the 31,397 as owed by him (page 1 row, owners table "دين السقي عليه", and the "ديون العملاء" card) **and** the 102,853 as owed to him. That subtracts the 31,397 twice. The "ديون العملاء" card was already correct before: `totals.totalOutstandingDebt` = sum of `receivableBalance`.

**Fix (`PdfReportGenerator.kt`):**
1. **Page 1, "المتبقي بذمته" column:**
   - farmer: `row.receivableBalance` (red if > 0, green otherwise), the same as today's farmer value;
   - well owner with `payableBalance > 0`: write «له 102,853» in **green** (paidGreen), never red;
   - well owner otherwise: `row.receivableBalance` (red if > 0).
   - Never use `billed - paid` and never use `row.balance`.
2. **Page 1 totals row and the "ديون العملاء" card:** go back to `totals.totalOutstandingDebt`. Remove `totalCustomerDebt`.
3. **"مستحقات أصحاب الآبار" card:** `totals.ownerPayable`. The fallback is harmless but not needed.
4. **Owners table, "دين السقي عليه":** go back to `row.receivableBalance`, as before round 1. Keep `payableBalance` under "متبقي له علينا".
5. **Test:** `ComprehensiveReportAccountingTest` only repeats arithmetic and does not call app code.
   - Move the page-1 cell choice into a small pure function, for example `internal fun page1BalanceCell(row): Pair<String, Boolean /*isDebt*/>`, in `PdfReportGenerator.kt` or a new `core/util/ComprehensiveReportMath.kt`.
   - Build the two rows with `calculateCustomerBalance`:
     - one farmer;
     - one owner with sales 78,250, collected 46,853, and purchases that give a net payable.
   - Assert:
     - the owner cell is «له …» and isDebt = false;
     - the farmer cell equals his receivable;
     - the debt total excludes the owner.

## Minor (fix in the same round)
6. **Keep-together:** only the last single row may squeeze in. Apply it to the last **1–2** rows (down to `pageHeight - 48`), as the task asked for fewer than 3. Keep the `ownerPagesCount` simulation in step, so "صفحة X من Y" stays right.
7. **Drive refresh (`DriveAutoBackupSection`):** `anySucceeded` stays true while an old SUCCEEDED immediate job is in WorkManager history. Every `workInfos` emission then reloads the list, together with the `LaunchedEffect(googleAccount)` load. Refresh only when `lastBackupTime` changes or a job moves from RUNNING to SUCCEEDED.
8. **Owners table, purchase column:** the three BiDi durations at 8.2 pt can be wider than the 130 pt column. Draw it with `drawClippedCell(…, 295f, 425f, …)` instead of `truncate(…, 50)`.

## Allowed files
- `core/util/PdfReportGenerator.kt` and an optional new `core/util/ComprehensiveReportMath.kt`;
- `features/settings/SettingsBackupScreen.kt`;
- tests.

## Done when
- Build and tests pass.
- Report: one sentence per item 1–8.
- Claude checks pages 1, 6 and 7 on the phone after this round.
