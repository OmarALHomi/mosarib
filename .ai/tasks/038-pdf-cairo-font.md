status: done
<!-- Owner request 2026-11-04: "اجعل الخط كايرو" while checking the comprehensive report. -->
# Task 038: Cairo font in every PDF, and a clean well-owners page

The app UI already uses Cairo (`ui/theme/Type.kt`). `core/util/PdfThemeHelper.kt` already loads it:
- `getCairoRegular`;
- `getCairoSemiBold`;
- `getCairoBold`.

But the PDFs still draw with the system font:
- `PdfReportGenerator.kt` uses `Typeface.DEFAULT` / `Typeface.create(Typeface.DEFAULT, …)` in about 25 places;
- `WellOwnerPdfStatementGenerator.kt` does the same in 3 places.

## Do
1. **Load once per PDF.** At the start of each generator function, load the three Cairo faces from `PdfThemeHelper`: regular, semibold and bold.
2. **Replace every `Typeface.DEFAULT` use:**
   - NORMAL becomes Cairo regular;
   - BOLD becomes Cairo bold.
   - Headers of tables may use semibold.
   - This covers the customer statement, the comprehensive report, the customer directory and any other generator in the file.
3. **Footer:** pass the Cairo face to `drawDocumentFooter` (`fontRegular`) everywhere it is called.
4. **Cairo is wider and taller than the system font. Re-check:**
   - **Row text inside 22 pt rows.** Cairo has a tall line box, so move the baselines if text touches the row lines.
   - **Columns.** Nothing may overflow:
     - customer statement: use `drawClippedCell` (task 037);
     - comprehensive report page 1: names, farm, duration;
     - owners table, including its detail line.
   - **Summary cards:** labels and values stay inside the cards.
   - **Page counts.** The "صفحة X من Y" simulation must still match the real page breaks. Change the row-height constants in both places if needed.
5. Keep every text, colour, column order and the accounting from 037 unchanged.

## Redesign of the well-owners page (comprehensive report)
The owner says this page is scattered, too detailed and not consistent. Today it has:
- six columns;
- a long grey detail line under every row (waste, credit, sold, collected), cut with «…»;
- payable shown in red, although it is what we owe him.

Replace it with one clean table, in the same style as the page-1 customers table.

**1. Summary strip at the top.** Four small cards, the same card style as page 1:
- عدد أصحاب الآبار;
- إجمالي الشراء (`totals.ownerPurchaseAmount`);
- المسدد للمالكين (`totals.ownerPayments`);
- صافي مستحقاتهم (`totals.ownerPayable`).

If the total owner waste is greater than 0, add one short line under the cards:
- «هدر أصحاب الآبار: 10 ساعات — خصم 25,000».
- The duration uses `formatBidiDuration`.

**2. Table.** One row per owner:
- row height 24 pt, zebra rows like page 1;
- **no second detail line**;
- every cell drawn with `drawClippedCell`.

Columns, from right to left:

| Column | Value | Width (about) |
|---|---|---|
| صاحب البئر | name, up to 26 chars | 140 |
| ساعات الشراء | chargeable purchased minutes, `formatBidiDuration` | 75 |
| صافي الشراء | `purchaseAmount` | 75 |
| المسدد له | `ownerPayments` | 70 |
| سقي له | `billed` (his own irrigation, deducted from what we owe) | 70 |
| المحصّل منه | `paid` | 65 |
| الصافي | see below | 60 to the left margin |

The "الصافي" cell uses the same netting model as page 1 (`calculateCustomerBalance`; never `billed - paid`):
- `payableBalance > 0`: «له X» in **green**;
- `receivableBalance > 0`: «عليه X» in red;
- otherwise: «مسدد» in grey.

Reuse `ComprehensiveReportMath.page1BalanceCell` or add a sibling function for this.

**3. Totals row** at the end, bold, on the primary-tint background like page 1. It sums every numeric column, and "الصافي" shows `totals.ownerPayable` as «له X».

**4. Subtitle:**
- remove «حسابا صاحب البئر مستقلان», which is wrong because the account is netted;
- use «${distributorName} | ${periodTitle} | حسابات أصحاب الآبار».

**5. Paging:**
- keep the 037 keep-together rule (the last 1–2 rows may squeeze in);
- keep the "صفحة X من Y" simulation in step with the new 24 pt row height and the summary strip.

**6. Test:** add a unit test for the "الصافي" cell. One owner with a net payable, one with a net receivable (sales bigger than purchases), and one settled.

## Allowed files
- `core/util/PdfReportGenerator.kt`;
- `core/util/WellOwnerPdfStatementGenerator.kt`;
- `core/util/PdfThemeHelper.kt` (only if a helper is needed), `core/util/ComprehensiveReportMath.kt`;
- tests.

## Done when
- Build and tests pass.
- `grep "Typeface.DEFAULT"` in the two generators returns only the fallbacks inside `PdfThemeHelper`.
- The report names every generator function changed.
- Claude checks on the phone:
  - comprehensive report pages 1 and 6;
  - one customer statement;
  - one well-owner statement.

## Review (Claude)
Accepted. Checked on the phone: Cairo in the report, the owners page is clean (cards, 7 columns, «له» green, totals row). Claude fixed one overlap: page-1 header «السقي / صافي المدة» now uses `drawClippedCell`.
