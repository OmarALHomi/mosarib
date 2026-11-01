# Review of task 015, round 3 (Claude, 2026-11-02)

Verdict: **changes-requested** (round 4: heights only). Everything else in 015 is accepted.

Build and tests pass. Checked on the owner's phone (384dp, light).

Accepted:
- approximate amount in words ("ثمانمائة ر.ي تقريباً", "ثلاثة عشر ألفاً ومائتا ر.ي تقريباً");
- exact words for round amounts;
- avatar initials ("أع", "أر", "أص");
- the tinted البيان box;
- names, badges and chips.

## The measured heights in the report are wrong
Measured from screenshots of the device (1dp = 1.875px):

| Card | Report says | Real on device | Target |
|---|---|---|---|
| Session with البيان | 152dp | **~194dp** | ≤ 160dp |
| Voucher / discount / receipt with البيان | 124dp | **~164dp** | ≤ 135dp |
| Customer | 104dp | **~124dp** | ≤ 110dp |

The voucher card is almost as tall as in round 2 (170dp). Do not estimate heights from code.
Measure them on the device:

```
adb shell uiautomator dump /sdcard/ui.xml
adb exec-out cat /sdcard/ui.xml
```

Find the card node: its `content-desc` starts with the name. Height in dp =
(bottom − top) / (density / 160). On this phone density is 300, so divide by 1.875.

## Why the cards are still tall
The Cairo font has tall line boxes, so every text line costs more than its `sp` size suggests.
Each line of the card is a few dp too tall:
- the name/amount line is ~32dp;
- the words line is ~26dp;
- the البيان box is ~30dp;
- the meta row is 36dp;
- the spacings and paddings add up.

## Exact changes (round 4)
1. Text styles inside `OperationCard` and `CustomerCardItem`: add
   `lineHeight = <fontSize> * 1.25` and
   `lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)`
   to the name, amount, words, badge, البيان and meta texts. Use a small helper, for example
   `private fun TextStyle.tight() = copy(...)`.
2. Amount: `titleMedium` bold (was `titleLarge`). Name stays `titleMedium` bold.
3. Card inner padding: 12dp horizontal, **10dp vertical**. Block spacing: **4dp**. Spacing
   between line 1 and line 2: **0dp**.
4. البيان box: vertical padding **4dp**.
5. Meta/actions row: **32dp** high, with `LocalMinimumInteractiveComponentSize` set to 32dp.
   Kebab and chevron: 32dp `IconButton`, 18dp icon. "سداد": 28dp high.
6. Customer card: same text tightening, 10dp vertical padding, a 32dp bottom row.
7. Do not remove or shorten any content: names, amount in words, البيان, meta and buttons stay.

## Report
Append a "Round 4" section with the three heights **measured with uiautomator** (paste the
bounds you used). Do not rewrite the report or the task file.

Allowed files: `core/ui/OperationCard.kt`, `features/customers/CustomerCardItem.kt`.
