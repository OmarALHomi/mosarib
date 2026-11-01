status: ready

# Task 010: Compact customer cards and operation rows

## Background (seen by Claude on the device, 384dp wide)
- Customer card (`CustomerCardItem.kt`) is ~150dp tall: a centred large balance, the amount in
  words ("فقط ثلاثة آلاف…"), a tree icon identical for everyone. Only 3.5 customers fit on screen.
- Operations list (`VouchersScreen.kt`) cards are ~130dp: amount in words, a boxed "البيان" row,
  a separate meta row and a kebab row. Only ~3 fit on screen.
- The "لك / عليك" summary bar in the customers list floats over the last card and hides it.

## Required behaviour
### A. Shared row component (new file `core/ui/OperationRow.kt`)
```
@Composable fun OperationRow(
    icon: ImageVector, iconTint: Color, iconBackground: Color,
    title: String,            // e.g. "سند قبض" / "سقي 2س 30د" / "صرف"
    subtitle: String?,        // notes / البيان, 1 line, ellipsis
    meta: String,             // date • customer or method, labelSmall, textMuted
    amountText: String,       // already formatted, with sign "+ " or "− "
    amountColor: Color,
    trailingNote: String? = null,   // e.g. "متبقي 1,150" or "الرصيد: 3,600", labelSmall
    onClick: () -> Unit,
    menu: (@Composable () -> Unit)? = null   // optional kebab with the existing actions
)
```
Layout: one `Row`, min height 64dp, padding 12dp horizontal / 10dp vertical. Leading 36dp
circle icon. Middle `Column(weight 1f)`: title `titleSmall`, subtitle `bodySmall` onSurfaceVariant,
meta `labelSmall` textMuted. Trailing column aligned to end: amount `titleSmall` bold, trailing
note under it. Then the optional 40dp kebab. Background `surface`, shape `MaterialTheme.shapes.medium`,
1dp outline `outline` colour. No amount in words. All colours from `MaterialTheme.colorScheme` /
`MaterialTheme.mosarib`.

### B. Operations list (`VouchersScreen.kt`)
Render every operation item of the main list with `OperationRow`:
- RECEIPT → icon `Payments`, tint `success`, amount "+ X", colour `success`, title "سند قبض".
- EXPENSE → icon `ReceiptLong`, tint `debt`, amount "− X", colour `debt`, title "سند صرف".
- DISCOUNT → keep its existing icon and colour logic, title "خصم / تسوية".
- Session → icon `WaterDrop`, tint `info`, title "سقي" + duration when > 0, amount = total,
  trailing note "متبقي X" in `debt` when remaining > 0, otherwise "مسدد" in `settled`.
- Purchase → keep its icon, tint `warning`, title "شراء ساعات".
- subtitle = notes (or category when notes are blank), meta = "date • customer name".
- Tapping the row does what tapping the card does today (opens the same detail sheet). The kebab
  menu contains **exactly** the actions the current card's kebab has.

### C. Customer card (`CustomerCardItem.kt`)
Rewrite the visible layout as a single compact row (target ≤ 80dp):
- Leading 44dp avatar circle with up to two initials (first letter of the first two words of the
  name). Background colour is picked from a fixed palette of 8 soft colours by
  `abs(customer.id % 8)`, with light and dark variants. Put the palette in `ui/theme/MosaribColors.kt`
  as `AvatarPalette`. Well owners keep a small water-drop badge on the avatar corner.
- Middle: name (`titleSmall`, 1 line, ellipsis). Second line: group name • "N حركة" • phone (if
  any), `labelSmall` textMuted, 1 line.
- Trailing: balance amount `titleSmall` bold coloured `debt` / `credit` / `settled`, and under it
  `labelSmall` "عليه" / "له" / "مسدد".
- Actions: the call button stays visible when there is a phone (40dp). "إرسال كشف" and
  "كشف حساب PDF" move into the existing "خيارات" menu as its first two items (same handlers).
- Remove the amount in words from the card.
- Keep the TalkBack descriptions added in task 007.

### D. Customers list summary bar (`CustomersScreenComponents.kt`)
Give the list a bottom content padding equal to the summary bar height + 16dp, so the last card
can scroll above the bar. Rename its labels for clarity: "لك" → "لك عند العملاء",
"عليك" → "عليك للموردين". If they do not fit at 360dp, use `labelSmall`.

## Allowed files
- new `core/ui/OperationRow.kt`
- `ui/theme/MosaribColors.kt` (add `AvatarPalette` only)
- `features/vouchers/VouchersScreen.kt` (list items only)
- `features/customers/CustomerCardItem.kt`, `features/customers/CustomersScreenComponents.kt`

## Out of scope
- Customer detail screen (task 011). No change to filters, search, sorting or ViewModels.

## Done when
- Build and tests pass. The report lists, for each old card, which actions went where.

---
## Review notes (written by Claude)
_(empty)_
