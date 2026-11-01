# Review of task 015, round 1 (Claude, 2026-11-01)

Verdict: **changes-requested**.

Build and tests pass. Checked on the owner's phone (384dp wide, light theme), card by card.

Right, keep as is:
- type colours, border, accent bar;
- amount in words (تفقيط) under every amount;
- البيان box;
- hiding of zero duration and zero rate;
- menus, including "فتح حساب العميل".

## What is wrong (seen on the device)
1. **Names are cut or missing.** Examples: "سعيد بن ح…", "وليد بن …", "إبراهي…", "عب..", and one
   session card shows only "…". The owner needs the farmer's name on every card.
2. **Badges break letter by letter.** "سقي" shows as س / ق / ي in a vertical strip, and
   "شراء ساعات" as "شرا / ء / ساع / ات".
3. **Cards are far too tall.** The old cards before task 010 were about 128dp (voucher) and
   140dp (session). Now a receipt card is ~165dp and a session card ~277dp. Causes:
   - the kebab moved into the header and squeezed it;
   - the status chip wraps to a second line;
   - there is a separate footer row for "سداد" / "تفاصيل السداد";
   - the البيان box padding is large.
4. **Customer cards are broken the same way.** The name, chips and kebab are invisible, and each
   card is ~190dp, mostly empty.
5. The reference is duplicated: "REC-REC-144109-144#".

**Root cause of 1, 2 and 4:** in a Compose `Row`, children without a weight are measured first.
The end column (amount + long amount-in-words) has no width limit, so it takes almost the whole
width. The weighted title column then gets a few dp, and its texts wrap one letter per line.

## Required layout for `OperationCard`, matching the old card
Measured at 384dp. Targets:
- voucher card with a one-line البيان: **≤ 130dp**;
- session card with البيان: **≤ 160dp**;
- each card is about 30dp shorter without البيان.

```
┌──────────────────────────────────────────────┐
│▌ (icon)  Name of farmer……………      − 1,000 ر.ي │  line 1
│          [سند صرف]          فقط ألف ر.ي لا غير │  line 2
│  [2س 30د] [4,500/س]                [باقي 5,442] │  sessions only, ONE line
│  ≡ البيان: …… (1 line, up to 2)                 │
│  4:01 م • نقداً • #EXP-2259   [سداد] [⌄]  [⋮] │  meta + actions, one row
└──────────────────────────────────────────────┘
```

### Header
- The icon (36dp circle) is outside the text column, vertically centred on lines 1 and 2.
- **Line 1:** `Row` with the name `Text(Modifier.weight(1f), maxLines = 1, ellipsis)` in
  `titleMedium` bold, then the amount (`titleLarge` bold, accent, intrinsic width,
  `maxLines = 1, softWrap = false`).
- **Line 2:** `Row` with the badges (each badge `maxLines = 1, softWrap = false`, never shrinks),
  then a `Spacer(8.dp)`, then the amount in words
  `Text(Modifier.weight(1f, fill = false), textAlign = TextAlign.End, maxLines = 2)` in
  `labelMedium`, accent 85%.
- No kebab in the header. It goes back to the meta row, as in the old card.

### Session info row (sessions only)
- One `Row` that never wraps. The duration uses the short form "2س 30د" (add
  `Formatters.formatDurationShort` if missing), and the rate is "4,500/س". Hide either when it
  is zero.
- The status chip sits at the end: "مسدد" (`success`), "غير مسدد" (`debt`), or "باقي Y"
  (`warning`) for a partial payment. Do not show the paid amount in the chip. It is in the
  detail sheet.

### البيان box
- Shown when not blank. Padding 10dp horizontal / 6dp vertical, `maxLines = 2`.

### Meta and actions row (replaces the separate footer row)
- `Row`: meta text (`Modifier.weight(1f)`, 1 line, ellipsis, `labelSmall` muted), then:
  - "سداد": a small tonal button, 32dp high, only when debt > 0;
  - the expand chevron: `IconButton`, 40dp target, `contentDescription = "تفاصيل السداد"`, only
    when the session has linked payments; the expanded history still opens under the card;
  - the kebab: 40dp target, "خيارات".

### Spacing
- Inner padding 12dp. 6dp between blocks. Outer margin 12dp horizontal and 4dp vertical.

### Reference
- Build the "#REC-…" text once. If the stored reference already starts with the prefix, do not
  add it again.

## Required layout for `CustomerCardItem` (same idea)
Target ≤ 110dp.

- **Header:** avatar (44dp) outside the text column. Line 1: name `weight(1f)`, 1 line, then
  "عليه: X ر.ي" (intrinsic, `stateColor`). Line 2: chips (group, "N حركة", "صاحب بئر"; no shrink,
  no wrap), then the amount in words (`weight(1f, fill = false)`, end-aligned, up to 2 lines).
  Zero balance shows "مسدد" and no words.
- **Bottom row:** phone number (`weight(1f)`, muted; empty space when there is no phone), then
  the call button (40dp, only with a phone), then the kebab (40dp).
- Keep the border and accent bar in `stateColor`, as now.

## Before you report
- Check on the device at 384dp **and** at 360dp. You can use `adb shell wm size 720x1520` and
  `adb shell wm density 320`, then `adb shell wm size reset` and `adb shell wm density reset`.
- Check that a 4-word name like "عبد الله بن علي الحميري" shows, possibly with an ellipsis at
  the end, and no badge wraps.
- In the report, write the measured card heights: voucher, session and customer.

Allowed files: `core/ui/OperationCard.kt`, `features/vouchers/VouchersScreen.kt` (card call
sites only), `features/customers/CustomerCardItem.kt`, `core/util/Formatters.kt` (only to add
`formatDurationShort`).
