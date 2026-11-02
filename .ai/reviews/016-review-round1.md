# Review of task 016, round 1 → owner change for round 2 (Claude, 2026-11-02)

Status: **changes-requested**.

The owner looked at the result and does **not** want:
- the "فلترة" button with a bottom sheet;
- the removable active-chips row;
- the separate type-chips row.

He wants the idea the customers screen used before: **one horizontal row of dropdown chips**.
Each chip shows its current value; tapping it opens a dropdown list.

Keep from round 1:
- the pure `OperationsFilterEngine` and its tests;
- `PeriodRange`;
- the summary card that follows the filters, with its "ملخص: …" line;
- the PDF of the filtered list;
- the empty state;
- recycle bin and group management in the customers ⋮ menu and in Settings;
- the search field.

Remove:
- the "فلترة" button and its badge;
- both filter bottom sheets;
- the active-chips row;
- the type-chips row on the operations screen;
- the quick balance chips on the customers screen.

## A. Shared component (`core/ui/FilterBarComponents.kt`, rewrite)
```kotlin
data class DropdownOption<T>(val value: T, val label: String)

@Composable fun <T> FilterDropdownChip(
    label: String,                 // the dimension, e.g. "الحالة"
    selected: T,
    options: List<DropdownOption<T>>,
    isDefault: Boolean,            // true → neutral look; false → highlighted
    onSelect: (T) -> Unit,
    footer: (@Composable () -> Unit)? = null   // e.g. "+ مجموعة جديدة"
)
```
- **Chip text:**
  - default value: "label: الكل", for example "الحالة: الكل";
  - other value: the value alone, for example "غير مسدد" or "هذا الشهر".
- **Chip look:**
  - followed by a ▾ icon (`ArrowDropDown`, 18dp);
  - height 36dp, 12dp horizontal padding, `CircleShape`;
  - default: 1dp `outline` border, `onSurfaceVariant` text;
  - not default: `primaryContainer` background with `onPrimaryContainer` text, no border.
- **Dropdown:** a `DropdownMenu` anchored under the chip.
  - Each option is a row: text, then a check icon on the selected one.
  - The selected option is in `primary`.
  - Minimum width 180dp.
  - No emoji.
- The row of chips: `LazyRow`, `contentPadding = 12dp` horizontal, 8dp spacing.
- The last item, shown only when any chip is not default, is a small text chip
  **"مسح الكل ✕"** in `debt` that resets everything except the search.
- Tokens and typography only. Task 020 will restyle the dropdown with `MosaribMenu` later; do
  not wait for it.

## B. Operations screen: chips in this order
| # | Chip (label) | Options | Maps to |
|---|---|---|---|
| 1 | الترتيب | الأحدث، الأقدم، الأعلى مبلغاً، الأقل مبلغاً، الاسم (أ-ي)، الاسم (ي-أ) | `OperationsSort` (add AMOUNT_ASC, NAME_ASC, NAME_DESC) |
| 2 | النوع | الكل، سقي، قبض، صرف، خصم، شراء ساعات، آجل / ديون | `type` (add DISCOUNTS if missing) |
| 3 | نوع الحساب | الكل، مزارع، صاحب بئر | new `accountType` (operation's customer `isWellOwner`; purchases count as صاحب بئر) |
| 4 | الحالة | الكل، مسدد، جزئي، غير مسدد | `sessionPayment` |
| 5 | المجموعة | الكل، each group, بدون مجموعة | `group` |
| 6 | الفترة | الكل، اليوم، أمس، هذا الأسبوع، هذا الشهر، الشهر الماضي، من - إلى… | `period` / `customRange` |
| 7 | العميل | الكل، then "اختيار عميل…" | `customerId` |

- For the sort chip, the default is "الأحدث" and the text is "الترتيب: الأحدث".
- "من - إلى…" opens the existing `MosaribDatePickerDialog` twice (from, then to). The chip then
  shows "1/10 - 15/10".
- "اختيار عميل…" opens a simple searchable list dialog. The chip then shows the customer name.
- Payment method and pump: keep them in the engine, but they get no chip in this round.
- Layout from the top: search field, then the chip row, then the summary card, then the list.

## C. Customers screen: chips in this order
| # | Chip | Options |
|---|---|---|
| 1 | الترتيب | الاسم (أ-ي)، الاسم (ي-أ)، الأعلى رصيداً، الأقل رصيداً، الأحدث إضافة، الأكثر ساعات سقي |
| 2 | نوع الحساب | الكل، مزارع، صاحب بئر |
| 3 | الحالة | الكل، عليه، له، متوازن |
| 4 | المجموعة | الكل، each group with its count, بدون مجموعة; footer item "+ مجموعة جديدة" (opens the existing editor) |

- Extend `CustomerSort` as needed. Keep the current sorting behaviour for the existing values.
- The summary bar (لك / عليك) keeps following the filters.

## D. The two engine fixes from my first notes (still needed)
1. Payment methods and pumps together: vouchers matching a method **OR** sessions matching a
   pump. Add a test.
2. PDF title = type name + `describe(...)`.

## Allowed files
- `core/ui/FilterBarComponents.kt`
- `features/vouchers/VouchersScreen.kt` (filter area only), `VouchersViewModel.kt`,
  `OperationsFilterState.kt`
- `features/customers/CustomersFilterRow.kt`, `CustomersScreen.kt`, `CustomersViewModel.kt`
- tests

Append a "Round 2" section to `.ai/reports/016.md`. Do not rewrite the report or the task file.
