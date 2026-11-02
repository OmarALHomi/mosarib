status: ready
<!-- ready since 015 was accepted (2026-11-02). Runs in parallel with 012, 016 and 018 in its own worktree. -->

# Task 011: Customer detail — one place per action, compact ledger, clearer header

## Depends on
- Task 010 (`AvatarPalette`), task 014 (day-grouping helper) and **task 015** (`OperationCard`).
  Start only after 015 is accepted.

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
4. **Ledger cards**: render the ledger with `OperationCard` (task 015), one card per operation,
   newest first, with day headers as in the operations list. Title = what happened ("سقي",
   "سند قبض", "سند صرف", "شراء ساعات"), because the customer is already known. The amount in
   words, the coloured border, البيان and the chips work exactly as in 015. Additionally:
   - amount sign and colour from the customer's point of view: what increases what the customer
     owes us → "− X" in `debt`; what decreases it → "+ X" in `credit` (for well owners follow the
     same meaning as the balance card title).
   - running balance **after** this operation. Compute it in a pure
     function `LedgerRunningBalance.compute(items, …)` that walks oldest → newest with the
     **same** signs `CustomerRepository` uses. If the final running balance does not equal the
     balance shown in the header (± 0.01), hide the running-balance note for this customer and
     say so in the report. Never show a wrong balance.
   - the running balance goes in `extraNote` as "الرصيد بعد العملية: X".
   - tapping a row opens the detail bottom sheet that exists today for that type; the kebab menu
     keeps every action the old card had (سداد, edit, delete, share, payment history…).
   - remove the five old card composables if nothing else uses them.

## Parallel-work rules
- Do not migrate the menus, dialogs or the two sheets in `CustomerDetailComponents.kt` to new
  components. Task 020 does that later. Keep them working as they are.
- Any colour or text style you write or touch in these files uses `MaterialTheme.mosarib` /
  `colorScheme` / `typography`, not literals.

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
