status: accepted

<!-- Owner decision 2026-11-01, after seeing 014 on the device: keep the floating pill bar, but
bring back the coloured operation cards. Rules from the owner, which must never be dropped again:
(1) the amount in words (تفقيط) under every amount, because most customers cannot read digits and
check the amount by its words; (2) every operation card has a coloured border and accent in its
type colour, so users recognise the type by colour; (3) cards, not flat grouped lists. -->
# Task 015: Coloured operation cards (restored and unified) and customer cards with تفقيط

## Background
- Before task 010 the operations list used coloured cards. The owner likes them; see commit
  `6efa92f`:
  - `features/vouchers/VouchersScreen.kt`: the voucher `Card` around line 733 and the purchase
    card around line 1199;
  - sessions used `SessionCardItem` in `features/sessions/SessionComponents.kt`.

  You may **read** old versions with `git show 6efa92f:<path>`. That is read-only and allowed. Do
  not checkout, reset or stash.
- Each card has:
  - a 1.4dp border in the type colour;
  - a vertical accent bar at the start edge;
  - an icon circle;
  - the customer name and type badges;
  - a large signed amount in the type colour with the تفقيط under it;
  - a boxed "البيان" row;
  - a meta row: date • method • reference number.
- Tasks 010 and 014 removed the تفقيط and the colours. That was a mistake in the spec, not in
  the implementation.
- Problems of the old cards, to fix in this task:
  - session cards and voucher cards have **different layouts**;
  - session cards show **no date and no البيان**, although for imported data the البيان is the
    most important text;
  - "0 دقيقة" and "سعر الساعة: 0" appear for imported sessions;
  - the kebab sits on its own row and wastes ~40dp.

## A. One card for every operation type: new `core/ui/OperationCard.kt`
```kotlin
data class CardBadge(val text: String, val color: Color)          // shown as a tinted chip
data class CardChip(val text: String, val container: Color, val content: Color, val icon: ImageVector? = null)

@Composable fun OperationCard(
    accent: Color,                    // type colour: border, bar, icon, amount, words
    icon: ImageVector,
    title: String,                    // customer name ("صاحب بئر: X", or category for general expense)
    badges: List<CardBadge>,          // "سند قبض", "سند صرف", "خصم", "سداد سقي", "سقي", "ساقية #2", …
    amountText: String,               // signed and formatted: "+ 60 ر.ي", "- 1,000 ر.ي", "5,000 ر.ي"
    amountInWords: String,            // Formatters.amountToArabicWords(...) — ALWAYS shown
    chips: List<CardChip> = emptyList(),   // session: duration, rate, status
    statement: String? = null,        // البيان; hidden when blank
    meta: String,                     // "4:01 م • نقداً • #EXP-2259"
    extraNote: String? = null,        // used by task 011: "الرصيد بعد العملية: X"
    onClick: () -> Unit,
    menu: @Composable () -> Unit,     // kebab button + DropdownMenu, placed in the header
    footer: (@Composable () -> Unit)? = null, // e.g. the session payment-history expander
    modifier: Modifier = Modifier
)
```

**Card**
- Shape `MaterialTheme.shapes.large` (12dp).
- Background `surface`.
- Border 1.4dp in `accent` with alpha 0.30 in light and 0.45 in dark.
- Shadow: 1dp in light only.
- Accent bar: 4dp wide and 28dp high, at the **start** edge (right in RTL), vertically centred on
  the header, rounded, in `accent`. This is the same as the old cards.
- Inner padding: 14dp. Outer margin: 12dp horizontal, 5dp vertical.

**Header row**
- Icon circle: 40dp, `accent` at 12% alpha, icon in `accent`.
- Middle column:
  - title: `titleMedium` bold, 1 line, ellipsis;
  - under it: the badges in a `FlowRow` (`labelSmall` on `color` at 12% alpha, 8dp radius, as the
    old "سند صرف" chip).
- End column, end-aligned:
  - `amountText` in `titleLarge` bold, `accent`;
  - under it `amountInWords` in `labelMedium` (not bold), `accent` at 85% alpha, up to 2 lines.
- Kebab: 40dp target, at the top end corner of the header. No separate kebab row.

**Chips row** (only when `chips` is not empty): a `FlowRow` with 6dp spacing, under the header.

**البيان box** (only when `statement` is not blank):
- full width, `surfaceVariant` at 50% alpha (dark: `surfaceContainerHigh`), 8dp radius, 10dp
  padding;
- leading lines icon (`Icons.AutoMirrored.Filled.Notes`) in `accent`;
- text "البيان: …" in `bodyMedium`, **up to 3 lines**, ellipsis.

**Meta row**: `labelSmall`, `textMuted`, 1 line, ellipsis.
**`extraNote`** (when given): `labelMedium`, bold, `onSurface`, under the meta row.
**Accessibility**: the whole card is one click target, with a merged `contentDescription` =
"title، amountText، amountInWords". The kebab keeps `contentDescription = "خيارات"`.

## B. Operations list (`features/vouchers/VouchersScreen.kt`, list items only)
Replace the `OperationRow` items from 014 with `OperationCard`. Keep the day headers from 014
("اليوم · …"); cards are **separate** (no grouping shapes). The date is in the day header, so meta
shows the time.

| Type | accent | icon | badges | amountText | chips |
|---|---|---|---|---|---|
| Receipt | `success` | `Payments` | "سند قبض", or "سداد سقي" for a session payment | "+ X ر.ي" | none |
| Expense | `debt` | `ArrowUpward` (as the old card) | "سند صرف" | "- X ر.ي" | none |
| Discount | `discount` (new token, below) | `Bookmarks` | "خصم" + the old extra badges (e.g. "ساقية #2") | "- X ر.ي" | none |
| Session | `info` | `WaterDrop` | "سقي" | total, "X ر.ي" | duration with `Schedule` icon (hide if 0); "سعر الساعة: X" (hide if 0); status: "مسدد" (`success`), "غير مسدد" (`debt`) or "مسدد X · باقي Y" (`warning`) |
| Purchase | `warning` | as the old card | "شراء ساعات" | "X ر.ي" | duration (hide if 0) |

- Title rules: same as the old cards ("صاحب بئر: X" for owner expenses; a general expense uses
  notes or category, else "مصروف عام"; a receipt without customer is "عميل نقدي").
- Statement: the voucher or session notes. Do not repeat it in the title.
- Meta: time • method (or pump name for sessions) • reference (`#EXP-…`, `#REC-…`, `#DSC-…`, as
  the old cards built it).
- Session footer: keep the old payment-history expander and the "سداد" button for sessions
  with debt, from `SessionCardItem`, through `footer`.
- Menu: exactly the old kebab items, plus a first item "فتح حساب العميل" when there is a
  customer (from 014).
- Tap: the existing detail sheet.

## C. Discount colour token (`ui/theme/MosaribColors.kt`)
Add `discount` to `MosaribExtendedColors`: light `0xFF7C3AED`, dark `0xFFA78BFA`. Use it instead
of the literal `Color(0xFF8B5CF6)` in the cards.

## D. Customer cards (`features/customers/CustomerCardItem.kt`, `CustomersScreenComponents.kt`)
The owner does not like the old customer cards (no border, no identity) or the 014 grouped list.
Give customers the **same visual language as the operation cards**, coloured by the **balance
state**:

| State | `stateColor` | Label |
|---|---|---|
| owes us | `debt` | "عليه" |
| we owe | `credit` | "له" |
| zero | `settled` | "مسدد" |

Undo the grouped container from 014. Each customer is a separate card, laid out like
`OperationCard`:
- shape 12dp, background `surface`;
- 1.4dp border in `stateColor` with alpha 0.30 in light and 0.45 in dark;
- 1dp shadow in light only;
- a 4dp × 28dp accent bar at the start edge in `stateColor`;
- 14dp inner padding, 12dp horizontal margin, 5dp vertical margin.

**Header row**
- Avatar: 44dp circle with two initials and the `AvatarPalette` colour, as in 010. Well owners
  keep the small water-drop badge on the avatar corner.
- Middle column:
  - name in `titleMedium` bold, 1 line, ellipsis;
  - under it, a `FlowRow` of small tinted chips (`labelSmall`, 8dp radius): the group name (if
    any), "N حركة", and "صاحب بئر" for well owners (in `info`).
- End column, end-aligned:
  - "عليه: 40,000 ر.ي" (label + amount) in `titleMedium` bold, `stateColor`; "مسدد" when zero;
  - under it `Formatters.amountToArabicWords(abs(balance))` in `labelMedium`, `stateColor` at
    85% alpha, up to 2 lines. Hide it when the balance is zero.
- Top end corner: the kebab (40dp, "خيارات"). The call button (40dp, "اتصال") sits next to it
  when there is a phone.

**Meta row** (only when there is a phone): the phone number in `labelSmall`, `textMuted`, under
the header.

Keep:
- every menu item and handler from 010 (send statement and PDF first);
- the TalkBack descriptions;
- tap = open the customer.

Merged `contentDescription` for the card: "name، عليه/له X، amount in words".

## E. Leave in place
- The pill bottom bar from 014 (accepted).
- `core/ui/OperationRow.kt` stays for now. Task 011 will move to `OperationCard`, and Claude
  will then decide on deleting it.

## Allowed files
- new `core/ui/OperationCard.kt`
- `features/vouchers/VouchersScreen.kt` (list items only)
- `features/customers/CustomerCardItem.kt`, `features/customers/CustomersScreenComponents.kt`
- `ui/theme/MosaribColors.kt` (`discount` token only)
- `features/sessions/SessionComponents.kt` only to reuse the payment-history expander and the
  settle button (extract them as small composables; do not change their behaviour)

## Out of scope
- Digits (task 012).
- The summary card at the top of the operations screen, filters, search and ViewModels.

## Done when
- Build and tests pass.
- The report lists the menu of every type and one example card per type: the texts of each
  part.
- Claude checks it on the device in light and dark, with the owner's real data (imported sessions
  with long البيان).

---
## Review notes (written by Claude)
**Round 1: changes-requested.** The full notes are in `.ai/reviews/015-review-round1.md`: read
that file and follow it exactly. In short: names are cut or missing, badges break letter by
letter, and cards are about twice as tall as the old ones. Restore the old header arrangement
(name + amount on line 1, badges + amount in words on line 2, kebab in the meta row), use one
row for session info, and use the same structure for customer cards.

**Round 2: changes-requested** (2026-11-02). Names, badges and layout are fixed. Remaining:
- card heights;
- an approximate amount in words on cards (owner's decision);
- avatar initials.

The full notes are in `.ai/reviews/015-review-round2.md`.

**Round 3: changes-requested** (2026-11-02). Everything is accepted except card heights. The measured
heights in the report do not match the device. The full notes are in `.ai/reviews/015-review-round3.md`.

**Round 4 verdict: accepted** (2026-11-02). Claude installed the build and measured it on the device
with uiautomator:

| Card | Measured | Target |
|---|---|---|
| Session | 171dp | 160dp |
| Voucher | 144dp | 135dp |
| Customer | 109dp (124dp when the words wrap to 2 lines) | 110dp |

Within ~10dp of the targets. Squeezing further would hurt readability of the amount in words,
which matters for the users, so the heights are accepted. 72 tests pass.

Process note for Antigravity: this round did not update `.ai/reports/015.md` or the status line.
Always finish a round by appending to the report and setting `done`.
