status: accepted
<!-- Owner request 2026-11-23. -->
# Task 057: Type filter on Home, 4 summary tiles, clearer numbers on operation cards

## 1. Home filter by type (`features/home/HomeCustomerList.kt`)
Add chips «مزارع» and «صاحب بئر» to the filter row (after الكل | لك | عليك; the row may scroll
horizontally). They combine with لك/عليك and المجموعة (use the existing `AccountFilter.FARMERS /
WELL_OWNERS` in CustomersViewModel). Totals box follows the filter as today.

## 2. Operations summary = four equal tiles (`features/vouchers/VouchersScreen.kt`)
2×2 grid: «دخل» (green), «خرج» (red), «لك» (receivables, same as Home «لك»), «عليك» (red, same as
Home «عليك»). Small caption under لك/عليك: «أرصدة حالية». Keep the «ساعات السقي» line under the grid.
Drop the separate «الباقي في الصندوق» tile (or show it as one small line «الباقي في الصندوق: X»).
Amounts use `AmountWithWords` (tap → تفقيط + read aloud).

## 3. Operation cards: bigger, clearer numbers (`core/ui/OperationCard.kt` + its callers)
Today hours «2س 30د», price «4,000/س» and «باقي 5,442» are tiny chips. For irrigation (and
well-owner purchase) cards add one **stat strip** under the name: four equal mini-blocks, each with a
small grey label on top and a bold ~16sp value:
«الساعات 2:30» | «سعر الساعة 4,000» | «الواصل 4,558» | «الباقي 5,442» (orange/red; «خالص» green
when 0). Remove the old small chips that the strip replaces. Keep: total amount + تفقيط, type chip,
coloured border per type, البيان box, footer. Put the strip in `OperationCard.kt` as a reusable
`OperationStatStrip` and use it wherever session/purchase cards are built (operations list,
customer page ledger).

## 4. Operations «+» menu (`VouchersScreen.kt`)
«سند (قبض / صرف)» becomes two items «قبض» and «صرف» (open the voucher sheet with that type).

## Allowed files
features/home/HomeCustomerList.kt, features/customers/CustomersViewModel.kt (only to expose the type
filter if needed), features/vouchers/VouchersScreen.kt, core/ui/OperationCard.kt,
features/customers/CustomerDetailScreen.kt (only to use the stat strip).

## Done when
Build + tests pass; items 1–4 visible.
