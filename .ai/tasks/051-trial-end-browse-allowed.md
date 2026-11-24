status: accepted
<!-- Owner request 2026-11-22. After 049 (shares MainScreen/HomeScreen/VouchersScreen). -->
# Task 051: When the trial/subscription ends, allow browsing; block only new operations

## Today
`MainScreen.kt` ~672: `isTrialLimitExceeded` shows `LicenseDialog` with `isMandatory = true`, which
cannot be closed, so the whole app is locked (even reading accounts). Home already has
`checkOperationAllowed` (`LicenseManager.canPerformOperation`) but other screens do not.

## Changes
1. The license sheet is no longer forced over the app. When the limit is exceeded or the
   subscription expired, show it **once per app start** (dismissible), and show a thin banner at the
   top of Home «انتهت الفترة التجريبية — اضغط للاشتراك» that opens it.
2. One shared gate: a CompositionLocal (e.g. `LocalOperationGate` in `core/ui`) provided by
   `MainScreen`, `fun requireOperation(action: () -> Unit)` that runs the action if
   `LicenseManager.canPerformOperation(...)` else opens the license sheet. Use it at **every** entry
   point that creates an operation: Home buttons (replace the local `checkOperationAllowed`),
   Operations FAB and its menu, customer page buttons (سقي/قبض/صرف/شراء ساعات), Sessions screen add,
   well-owner purchase, voice save paths (form and direct save from «فهمت»), duplicate/copy of an
   operation, import of operations. Editing, deleting, viewing, PDFs, messages, backup and restore
   stay allowed.
3. Defence in depth: the view-model insert functions for sessions, vouchers and purchases also check
   `canPerformOperation` and refuse (with the same sheet via the gate) — so no path can bypass it.
   Keep existing operation counting unchanged.
4. Add a unit test for whatever pure logic you add (e.g. "show sheet once per start").

## Allowed files
MainScreen.kt, core/ui/OperationGate.kt (new), features/home/HomeScreen.kt,
features/vouchers/VouchersScreen.kt, features/customers/CustomerDetailScreen.kt,
features/sessions/SessionsScreen.kt, features/sessions/SessionsViewModel.kt,
features/vouchers/VouchersViewModel.kt, features/customers/CustomersViewModel.kt,
features/wellowners/* (only the add entry point / insert), core/license/LicenseManager.kt (read-only
helpers only, no change to how licences are validated).

## Done when
Build + tests pass; with the trial exhausted the user can open every tab and customer, and every
"new operation" button opens the subscription sheet instead of the form.
