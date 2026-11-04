status: ready
<!-- Owner request 2026-11-04: "اجعل الخط كايرو" while checking the comprehensive report. -->
# Task 038: Use the Cairo font in every PDF

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

## Allowed files
- `core/util/PdfReportGenerator.kt`;
- `core/util/WellOwnerPdfStatementGenerator.kt`;
- `core/util/PdfThemeHelper.kt` (only if a helper is needed).

## Done when
- Build and tests pass.
- `grep "Typeface.DEFAULT"` in the two generators returns only the fallbacks inside `PdfThemeHelper`.
- The report names every generator function changed.
- Claude checks on the phone:
  - comprehensive report pages 1 and 6;
  - one customer statement;
  - one well-owner statement.
