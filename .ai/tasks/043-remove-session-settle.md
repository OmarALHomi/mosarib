status: accepted
<!-- Owner request 2026-11-22: remove the per-session «سداد» button and the session settle list (both modes). -->
# Task 043: Remove per-session settle button and the settle list in the receipt form

## Goal
Payments are entered only as a normal «قبض» receipt. No «سداد» button on irrigation sessions and no
"choose which sessions this receipt pays" list. Sessions must still show as paid after a receipt.

## Changes
1. Remove every trigger of `SettleSessionDialog` and its mounts:
   - `features/sessions/SessionComponents.kt`: the «سداد» / «سداد (amount)» buttons (~517, ~857, ~993, ~1093)
     and the `onSettleClick` parameters that only feed them.
   - `core/ui/SessionDetailBottomSheet.kt` ~106: the «سداد (amount)» footer button and the `onSettle` param.
   - `features/customers/CustomerDetailScreen.kt`: «سداد» button ~1488, `onSettle` ~2237, mount ~2318,
     `sessionToSettle` state.
   - `features/sessions/SessionsScreen.kt`: ~355, ~489, mount ~506, state.
   - `features/vouchers/VouchersScreen.kt`: ~818, ~966, ~1462, mount ~1572, state.
   - Leave the function `SettleSessionDialog` in `SessionDialogs.kt` untouched (another task edits that file).
2. Receipt form `AddEditVoucherBottomSheet` (`VouchersScreen.kt`): remove the card
   «جلسات سقي غير مسددة (N)» (~2536–2680: list, تحديد الكل، إلغاء, FIFO preview) **but keep the
   behaviour of the default**: on save of a RECEIPT, pass all of the customer's unsettled sessions
   (oldest first, as today's auto-select at ~2334 does) as `selectedSessionIds`, so
   `VouchersViewModel.addVoucher` still spreads the amount FIFO over sessions and the surplus becomes
   credit exactly as now. Keep the amount prefill with total debt for new receipts.
   Do not change `VouchersViewModel`.
3. Remove imports, params and state that become unused. Do not change any other text or colour.

## Allowed files
features/sessions/SessionComponents.kt, core/ui/SessionDetailBottomSheet.kt,
features/customers/CustomerDetailScreen.kt, features/sessions/SessionsScreen.kt,
features/vouchers/VouchersScreen.kt

## Done when
Build + tests pass; no «سداد» button anywhere on a session; saving a receipt for a customer with
unpaid sessions still marks the oldest sessions paid (same result as selecting all before).
