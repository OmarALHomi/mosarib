status: ready

# Task 006: Customer detail — make the main actions visible and consistent

## Background (verified by Claude in `features/customers/CustomerDetailScreen.kt`)
- The header quick-action row (around line 605) has "سقي جديد / اتصال / إرسال كشف" for a normal
  customer, but **no button to record a payment (قبض)**. Payments are only reachable through the
  overflow menu (`CustomerActionsDropdownMenu`, ~line 3616) as "سند (له)" / "سند (عليه)".
  The receipt sheet already exists: state `showAddReceiptSheet` (sheet at ~line 1482).
  The empty-ledger hint even says "يمكنك إضافة دورة سقي جديدة أو تسجيل سند قبض / صرف".
- The `ExtendedFloatingActionButton` (~line 413) always says "تسجيل سقي", also for a well owner,
  whose main action is "شراء ساعات" (`showOwnerPurchaseSheet`).
- The same action has four labels: "سقي جديد", "سقي له", "تسجيل سقي", and on Home "سجل سقي جديد".
- Header meta text (phone • location • price) is `labelSmall` white at 75% alpha on a gradient,
  and notes at 65% alpha — hard to read outdoors.

## Required behaviour
1. **Payment button in the header** (normal customer, not well owner): add
   `QuickActionButton(icon = Icons.Default.Payments, label = "قبض", accentColor = MaterialTheme.mosarib.success,
   onClick = { showAddReceiptSheet = true })` right after "سقي جديد" and before "اتصال".
   Respect the same licence check that the existing add actions use in this screen (if the
   receipt path from the menu checks `LicenseManager.canPerformOperation`, do the same; if it does
   not, do not add a new check — mention it in the report).
2. **FAB by account type**:
   - normal customer: label "سقي جديد", icon `WaterDrop`, opens `showAddSessionSheet` (as today);
   - well owner: label "شراء ساعات", icon `Add`, opens `showOwnerPurchaseSheet`.
   Remove the literal `fontSize = 13.5.sp` (use `labelLarge`).
3. **One label for irrigation** in this file: every button/menu item that opens the add-session
   sheet for this customer says "سقي جديد" (the well-owner variant "سقي له" stays, because it
   means irrigation billed to the owner — keep it). Menu items "تسجيل سقي" → "سقي جديد".
4. **Clearer voucher menu items**: "سند (له)" → "سند قبض (له)", "سند (عليه)" → "سند صرف (عليه)".
   Do not change what they open.
5. **Header readability**: meta line alpha 0.75 → 0.9 and style `bodySmall`; notes alpha
   0.65 → 0.85. Do not change the gradient.

## Depends on
- Task 003 (uses `MaterialTheme.mosarib.success`).

## Allowed files
- `app/src/main/java/com/example/features/customers/CustomerDetailScreen.kt`

## Out of scope
- No change to balances, the ledger list, the card items or any sheet's content.
- Do not split the file in this task.

## Done when
- Build and tests pass.
- The report lists every label that was renamed (old → new) with line numbers.

---
## Review notes (written by Claude)
_(empty)_
