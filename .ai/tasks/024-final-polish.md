status: accepted

<!-- Collected by Claude during device checks on 2026-11-02. 020 and 023 are merged. Runs in the
existing worktree D:\Kotlin\mosarib-t020 (branch task/024), in parallel with 013. Owner decision
2026-11-02: on statistics cards the amount in words is shown on tap, not inline. -->
# Task 024: Amount-in-words on tap for statistics cards, and final polish

## Owner decision
On the **statistics cards** of the operations and reports screens, the inline amount in words
makes the cards large. The owner wants:
- no inline words there;
- a **tap on the amount** opens a professional tooltip with the amount in words.

Inline words **stay unchanged** everywhere else: operation cards, customer cards, the customer
ledger, sheets, vouchers and PDFs. Customers see those, and some cannot read digits.

## A. New shared component: `core/ui/AmountWithWords.kt`
```kotlin
@Composable fun AmountWithWords(
    amount: Double,
    currency: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = ""            // e.g. "+ " / "− " / "عليه: "
)
```
- Shows the formatted amount (`Formatters.formatCurrency`) as today.
- **Discoverability:** a subtle dashed underline under the amount (1dp, `color` at 40% alpha,
  drawn with `drawBehind`, 3dp dash / 3dp gap). No extra icons.
- **Tap** (and long-press) opens a tooltip anchored above the amount. Use material3
  `TooltipBox` + `RichTooltip` from material3 1.3, with
  `rememberTooltipState(isPersistent = true)`:
  - title: "المبلغ كتابةً" (`labelMedium`, muted);
  - text: the **exact** words `Formatters.amountToArabicWords(abs(amount), currency)`, i.e.
    "فقط … لا غير", in `bodyMedium`, `onSurface`, wrapping freely;
  - container `surfaceContainerHigh`, shape 12dp, the caret on;
  - it closes on a tap outside or on back.
- Light haptic (`TextHandleMove`) when it opens.
- **Accessibility:** `semantics { contentDescription = "<amount>، <words>"; role = Role.Button;
  onClickLabel = "عرض المبلغ كتابةً" }`.
- Tokens and typography only.

## B. Use it on the statistics cards (remove their inline words)
1. **Operations summary card** (`VouchersScreen.kt`, summary card only): "صافي النقد", "المقبوض",
   "المصروف", "على العملاء". Remove the four inline word lines. The three small tiles keep equal
   height and get the same label style (`labelSmall`, `mosarib.textMuted`), including "على
   العملاء" and its "الرصيد الحالي" note.
2. **Reports KPI grid** (`ReportsScreen.kt`): the four tiles. Remove the inline words and keep
   the comparison line ("↑ 12% …") and "من N عميلاً".
3. **Reports "توزيع الربح"**: the amount lines. Remove the inline words.
4. Reports "أكبر المدينين": **keep** the inline approximate words. That is a list of people, not
   a statistics card.

## C. Other polish
1. **Reports "حسب المجموعة":** the group name is cut ("بدون مج…"). Put the name on line 1 (full
   width) and "إيراد · محصّل · آجل" on line 2.

Do not touch the operation cards or the list items: task 013 adds animations there in parallel.

## Allowed files
- new `core/ui/AmountWithWords.kt`
- `features/vouchers/VouchersScreen.kt` (summary card only)
- `features/reports/ReportsScreen.kt`

## Done when
- Build and tests pass.
- On the device:
  - tapping each statistic opens the tooltip with the full exact words;
  - a tap outside closes it;
  - the cards are clearly shorter than before. Write the heights before and after in the
    report.

---
**Verdict: accepted (2026-11-02, code review). AmountWithWords with TooltipBox + RichTooltip and the exact words, used on the operations summary card (4) and the reports statistics; inline words removed there and kept on the debtors list; group rows on 2 lines. The report estimates about 25% shorter cards. Tests pass. Device check after the merge (phone disconnected).**
