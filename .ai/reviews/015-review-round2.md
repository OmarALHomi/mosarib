# Review of task 015, round 2 (Claude, 2026-11-02)

Verdict: **changes-requested** (round 3). This round is small.

Build and tests pass. Checked on the owner's phone (384dp, light).

Fixed and good, keep:
- full names;
- badges on one line;
- the old header arrangement;
- the session info row;
- "سداد", chevron and kebab in the meta row;
- the reference is no longer duplicated;
- customer cards show name, chips, balance and words.

Customer cards keep the send and PDF icons in the bottom row. The bar has room for them and they
are useful, so this is accepted.

## 1. Cards are still too tall
Measured on the device. Targets are from round 1.

| Card | Now | Target |
|---|---|---|
| Session with البيان | ~200dp | ≤ 160dp |
| Voucher/discount with البيان | ~170dp | ≤ 130dp |
| Customer | ~128dp | ≤ 110dp |

Where the height goes:
- the empty space between the البيان box and the meta row (~25dp);
- the meta row itself (~48dp: M3 `IconButton` enforces a 48dp minimum touch size);
- the bottom padding.

Fix:
- 6dp between blocks, 12dp inner padding, nothing more.
- Meta/actions row height 36dp. Inside the card, wrap it in
  `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp)`.
- Kebab and chevron: 36dp `IconButton`, 20dp icon.
- "سداد": 30dp high, `contentPadding = PaddingValues(horizontal = 12.dp)`.
- Customer card: same bottom-row treatment (36dp icon buttons).

## 2. Approximate amount in words on cards (owner's decision)
Add `Formatters.amountToArabicWordsApprox(amount: Double, currency: String): String`. Keep
`amountToArabicWords` unchanged; vouchers, PDFs, messages and detail sheets stay exact.

Rules:
- `abs(amount) < 100`: exact words, e.g. "خمسون ر.ي".
- Otherwise round to the nearest 100, half up:
  - if the rounded value equals the amount, use the exact words without "تقريباً"
    ("اثنا عشر ألفاً وخمسمائة ر.ي");
  - if it differs, use the words of the rounded value followed by " تقريباً"
    ("ثلاثة عشر ألفاً ومائتا ر.ي تقريباً").
- Never use "فقط" / "لا غير" in the approximate form.
- Negative amounts: use the absolute value. The sign is already on the amount.

Use it only in `OperationCard` and `CustomerCardItem`. Unit tests:
- 50 → exact;
- 12,500 → exact, no "تقريباً";
- 13,163 → 13,200 + "تقريباً";
- 13,140 → 13,100 + "تقريباً";
- 150 → 200 + "تقريباً";
- 99.5 → exact.

## 3. Avatar initials
Every customer shows "أب", because names are "أحمد بن …" and the second word is "بن". Use the
first letter of the **first** word and the first letter of the **last** word. Strip a leading
"ال" from the last word. Example: "أحمد بن حسين العولقي" → "أع". A one-word name gives one
letter.

## Before you report
Measure the three card heights on the device and write them in a "Round 3" section of
`.ai/reports/015.md`. Do not rewrite the report or the task file: append only.

Allowed files: `core/ui/OperationCard.kt`, `features/customers/CustomerCardItem.kt`,
`core/util/Formatters.kt` (add the approx function only), and a unit test file.
