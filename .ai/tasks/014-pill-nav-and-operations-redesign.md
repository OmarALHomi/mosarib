status: ready

<!-- Owner reviewed 009/010 on the device on 2026-11-01: "not professional"; the bottom bar looks
classic and old, and the irrigation cards in the operations screen look bad. The owner chose
option A (floating pill bar) and the grouped-by-day list from Claude's mockup. -->
# Task 014: Floating pill bottom bar and a modern operations list

Do this task **before** 011. 011 builds on the `OperationRow` from this task.

## Background (seen by Claude on the device, 384dp)
- The bottom bar is the plain M3 `NavigationBar`. With the hand-drawn canvas icons (ID badge,
  maintenance tools) it looks dated.
- Operations list rows (after 010):
  - the title is the word "سقي" on every row;
  - the customer name is buried in the meta line and cut off ("حساب الوالد حق م...");
  - the amount repeats ("300" then "متبقي 300 ر.ي");
  - the same date and time repeats on every row;
  - each row is a separate bordered card with a kebab;
  - a row is ~100dp tall.

## A. Floating pill bottom bar (`core/ui/MosaribNavBar.kt`)
Replace the M3 `NavigationBar` with a custom floating pill. Keep the public signature
`MosaribNavBar(selectedIndex, onItemSelected, modifier)`, the five tabs, their order, labels and
test tags.

**Container**
- `Surface`, shape `RoundedCornerShape(28.dp)`, height 64dp.
- Outer padding: 16dp horizontal, and `8.dp` + `WindowInsets.navigationBars` at the bottom.
  Nothing is painted behind the pill except the screen background, so the area around it must
  be transparent.
- Light theme: colour `surface`, `shadowElevation = 6.dp`, 1dp `outlineVariant` border.
- Dark theme: colour `surfaceContainerHigh`, no shadow, 1dp `outlineVariant` border.

**Items** (`Row`, `SpaceEvenly`, vertically centred)
- **Unselected:** icon only, 24dp, `onSurfaceVariant`, inside a 48dp touch target.
- **Selected:** a pill indicator, height 40dp, `CircleShape`, colour `primaryContainer` in
  light and `primary.copy(alpha = 0.18f)` in dark. Inside it are the filled icon and the label
  (`labelLarge`, 1 line), both in `onPrimaryContainer` in light and `primary` in dark.
  Horizontal padding is 14dp.
- Animate selection change with `animateContentSize(spring(dampingRatio = 0.8f, stiffness =
  StiffnessMediumLow))` on the item and `animateColorAsState` for the indicator.
- Light haptic on selection: `LocalHapticFeedback` with `HapticFeedbackType.TextHandleMove`.

**Icons:** replace the five canvas icons with Material icons from `material-icons-extended`,
already a dependency. Use `Icons.Outlined.*` when unselected and `Icons.Rounded.*` when selected:

| Tab | Icon |
|---|---|
| التقارير | `BarChart` |
| العملاء | `Group` |
| الرئيسية | `Home` |
| العمليات | `ReceiptLong` (use the `AutoMirrored` variant if the deprecation warning asks for it) |
| الإعدادات | `Settings` |

Delete the canvas icon composables if nothing else uses them (Claude checked: only this file).

**Accessibility**
- Each item: `selectable(selected, role = Role.Tab)` and `contentDescription` = the tab label.
  Unselected items show no text, so TalkBack needs the description.
- Keep the 48dp minimum target.

**Keep:** the status-bar and navigation-bar icon logic from task 009 in `MainScreen.kt`.

## B. `OperationRow` redesign (`core/ui/OperationRow.kt`)
New visual: a flat row with no border, card or own shadow. Grouping is drawn by the list.

```kotlin
enum class RowGroupPosition { Single, First, Middle, Last }

data class RowStatus(val text: String, val container: Color, val content: Color)

@Composable fun OperationRow(
    icon: ImageVector, iconTint: Color, iconBackground: Color,
    title: String,                  // who: customer / owner name, or category for general expenses
    subtitle: String,               // what: "سقي 2س 30د · 4:01 م · notes", 1 line, ellipsis
    amountText: String,             // "300", "+ 60", "− 120" (no currency symbol in rows)
    amountColor: Color,
    status: RowStatus? = null,      // small chip under the amount
    trailingNote: String? = null,   // small text under the amount (used by 011 for "الرصيد: X")
    position: RowGroupPosition = RowGroupPosition.Single,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

- Shape by position, radius 16dp: `First` = top corners rounded, `Last` = bottom corners,
  `Single` = all corners, `Middle` = none.
- Background `surface` in light and `surfaceContainer` in dark. The 1dp `outlineVariant` border
  is drawn only on the outer edges of the group: draw it on the shape, or draw the group
  container in the list. Use whichever is simpler, as long as it looks like one card per day.
- Inside a group, draw a 0.5dp `outlineVariant` divider under every row except `Last`/`Single`,
  inset 60dp from the start (it starts under the text, not under the icon).
- Layout: min height 60dp, padding 12dp horizontal / 10dp vertical.
  - Leading 36dp circle icon.
  - Middle `Column(weight 1f)`: title `titleSmall` in `onSurface`, 1 line, ellipsis; subtitle
    `bodySmall` in `onSurfaceVariant`, 1 line, ellipsis.
  - Trailing `Column`, end-aligned: amount `titleSmall`, `FontWeight.SemiBold`, `amountColor`.
    Under it the status chip (`labelSmall`, padding 6dp/1dp, `CircleShape`, its colours) or the
    trailing note (`labelSmall`, `textMuted`).
- `combinedClickable(onClick, onLongClick, onLongClickLabel = "خيارات")`.
- Remove the old `meta` / `menu` parameters and the overloads.

## C. Operations list (`features/vouchers/VouchersScreen.kt`, list items only)
1. **Group by day.** Before the first item of each calendar day, insert a header item:
   - start: "اليوم · الأحد 1 نوفمبر", "أمس · السبت 31 أكتوبر", or "الخميس 29 أكتوبر" for older
     days (add the year only when it is not the current year);
   - end: "N عمليات";
   - `labelMedium` in `onSurfaceVariant`, padding 16dp horizontal, 14dp top / 6dp bottom.

   Rows of that day get `First/Middle/Last/Single` positions. Keep the existing sort order and
   the `key`s, and give headers their own keys (`"d_<yyyymmdd>"`).
2. **Row content per type:**

   | Type | Icon (tint / background) | Title | Subtitle | Amount | Status |
   |---|---|---|---|---|---|
   | Session | `WaterDrop` (`info` / `infoContainer`) | customer name | "سقي" + duration when > 0, time, notes | total in `onSurface` | `remainingDebt == 0` → "مسدد" (`successContainer`/`success`); `amountPaid == 0` → "غير مسدد" (`debt` 12% alpha / `debt`); otherwise "باقي X" (`warningContainer`/`warning`) |
   | Receipt | `SouthWest` (`success` / `successContainer`) | customer name, or "عميل نقدي" | "قبض" or "سداد سقي" for a session payment, method, time, notes | "+ X" in `success` | none |
   | Expense | `NorthEast` (`debt` / `debt` 12% alpha) | customer name; well owner = owner name; general expense = notes or category, or "مصروف عام" | "صرف" or "دفع لصاحب البئر", category, time | "− X" in `debt` | none |
   | Discount | keep its icon and colour | customer name | "خصم / تسوية", time, notes | as today | none |
   | Purchase | keep its icon (`warning` / `warningContainer`) | owner name | "شراء ساعات" + duration, time | amount in `warning` | none |

   Time format: "4:01 م", through `Formatters`.
3. **No kebab on rows.** A long-press opens a `DropdownMenu` anchored to the row with
   **exactly** the actions the current kebab has, plus a new first item **"فتح حساب العميل"**
   (`onNavigateToCustomer(id)`) when the operation has a customer. Since 010 there is no way to
   open the customer from a voucher or purchase row. Tap = the existing detail sheet.
4. Rows sit inside the list with 12dp horizontal margin. There is no vertical gap between rows
   of one day; the day header gives the spacing between groups.

## D. Customers list (`features/customers/CustomerCardItem.kt`, `CustomersScreenComponents.kt`)
Same visual language as the operations list:
- the customer list becomes one grouped container (`First/Middle/Last` shapes, dividers inset
  under the text), not separate bordered cards;
- keep the content from 010: avatar, name, group • N حركة • phone, balance with "عليه/له/مسدد",
  call button, "خيارات" menu;
- do not add day headers here.

## Allowed files
- `core/ui/MosaribNavBar.kt`, `core/ui/OperationRow.kt`
- `features/vouchers/VouchersScreen.kt` (list items, day headers and long-press menu only)
- `features/customers/CustomerCardItem.kt`, `features/customers/CustomersScreenComponents.kt`
  (container and shape only)
- `MainScreen.kt` only if the bar needs a different `bottomBar` setup (say why in the report)
- A pure helper for day headers (for example `core/util/DayGrouping.kt`) and its unit test

## Out of scope
- The financial summary card at the top of the operations screen, filters, search, sorting and
  ViewModels.
- Customer detail (task 011). Digits and colours elsewhere (task 012).

## Tests
- Day grouping helper: today / yesterday / older labels, year shown only for other years, and
  positions `Single`, `First/Middle/Last` for a day with 1 and with 3 items.

## Done when
- Build and tests pass.
- The report includes the long-press menu of every operation type and the item count per day for
  a sample list.
- Claude checks it on the device in light and dark themes.

---
## Review notes (written by Claude)
_(empty)_
