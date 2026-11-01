status: ready

# Task 011: Customer detail — one place per action, compact ledger, clearer header

## Depends on
- Task 010 (`OperationRow`, `AvatarPalette`).

## Background (seen by Claude on the device)
- Recording a payment now has two buttons: the header "قبض" (added in task 006) and the large
  green "له" button under the balance card. Irrigation has three: header "سقي جديد", the FAB and
  the menu item.
- The big buttons say only "له" / "عليه". Users do not know that this means قبض / صرف.
- Avatar shows one letter; "ا" looks like a vertical bar.
- On the gradient, the header icons tinted `PrimaryTeal` ("سقي جديد") and `SecondaryAquaDark`
  ("سقي له") have very low contrast. `QuickActionButton` labels force `fontSize = 10.sp`.
- Stats show "ساعات البيع: 0 دقيقة" for a normal customer.
- Each ledger card is ~265dp (amount in words, "سعر الساعة: 0", boxed البيان, سداد button,
  payment-history toggle). Only two operations fit on screen.

## Required behaviour
1. **Actions, one place each**
   - Header quick actions = contact and documents only: "اتصال" (if phone), "إرسال كشف" (if
     phone), "كشف PDF". Well owners additionally keep "تسجيل هدر". Remove "سقي جديد", "سقي له",
     "قبض" and "شراء ساعات" from the header.
   - The two big buttons under the balance card become **"قبض (له)"** and **"صرف (عليه)"**.
     For a well owner they are **"شراء ساعات"** (opens `showOwnerPurchaseSheet`) and
     **"دفع له (صرف)"** (opens the existing owner payment/disbursement flow, the one the menu item
     "سند صرف (عليه)" opens).
   - FAB stays as task 006 made it ("سقي جديد" / "شراء ساعات"). For well owners also keep
     "سقي له" reachable from the overflow menu.
   - The overflow menu keeps all its items (nothing lost).
2. **Header**
   - Avatar: two initials, background from `AvatarPalette` (same rule as the list card), 56dp.
   - All header quick-action icons are white (`QuickActionButton` default). Labels use
     `labelSmall` (11sp) without the literal `fontSize`.
3. **Stats grid**: hide a stat cell whose value is zero **and** does not apply to the account
   type ("ساعات البيع" for a normal customer, irrigation stats for a well owner with no sessions).
   If a row ends up with one cell, let it fill the width.
4. **Compact ledger**: render the ledger list with `OperationRow` (from task 010), one row per
   operation, newest first, with:
   - amount sign and colour from the customer's point of view: what increases what the customer
     owes us → "− X" in `debt`; what decreases it → "+ X" in `credit` (for well owners follow the
     same meaning as the balance card title).
   - trailing note "الرصيد: X" = running balance **after** this operation. Compute it in a pure
     function `LedgerRunningBalance.compute(items, …)` that walks oldest → newest with the
     **same** signs `CustomerRepository` uses. If the final running balance does not equal the
     balance shown in the header (± 0.01), hide the running-balance note for this customer and
     say so in the report. Never show a wrong balance.
   - tapping a row opens the detail bottom sheet that exists today for that type; the kebab keeps
     every action the old card had (سداد, edit, delete, share, payment history…).
   - remove the five old card composables if nothing else uses them.

## Allowed files
- `features/customers/CustomerDetailScreen.kt`, `features/customers/CustomerDetailComponents.kt`
- new `features/customers/LedgerRunningBalance.kt` + a unit test for it

## Tests
- `LedgerRunningBalance`: normal customer with sessions, receipts, expenses and discounts; a well
  owner with purchases and payments; the final value equals `CustomerRepository`'s balance for the
  same data.

## Done when
- Build and tests pass. The report has a table: old action location → new location.

---
## Review notes (written by Claude)
_(empty)_
