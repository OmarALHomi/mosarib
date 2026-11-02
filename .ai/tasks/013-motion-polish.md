status: ready
<!-- 2026-11-02: all earlier tasks are merged. Runs in the existing worktree D:\Kotlin\mosarib-t023 (branch task/013), in parallel with 024 in D:\Kotlin\mosarib-t020. -->

# Task 013: Motion polish (subtle, fast, consistent)

## Depends on
- Tasks 010 and 011 (the rows they create get item animations here).

## Background (verified by Claude in `MainScreen.kt`)
Opening a customer swaps the whole screen with no transition (`if (selectedCustomerId > 0)`).
Tab changes jump. Lists have no item animations. Numbers appear instantly.

## Required behaviour
Keep durations short (150–300ms), use `FastOutSlowInEasing` or springs, and respect the system
"remove animations" setting. `ValueAnimator.areAnimatorsEnabled()` false → no animation.
1. **Open / close customer detail**: wrap the `selectedCustomerId` branch in `AnimatedContent`.
   Detail enters sliding in from the start edge (RTL-aware) with a fade; back reverses it.
   The main pager must keep its state, including the selected tab.
2. **Tab change**: crossfade (200ms) between tab pages instead of the pager scroll animation
   (`pagerState.scrollToPage` + `Crossfade`/`AnimatedContent` keyed by page).
3. **Lists**: `Modifier.animateItem()` on items in the customers list, the operations list
   (the `OperationCard`s and the day headers) and the customer ledger (insert, remove, reorder
   after a filter or sort change from task 016).
4. **Numbers**: a small `AnimatedAmount(value, …)` composable in `core/ui` that counts from the
   previous value to the new one over 400ms. Use it for the customer balance in the detail balance
   card and the two totals of the customers summary bar.
5. **Press feedback**: `QuickActionCard` on Home and the two big money buttons in customer detail
   scale to 0.97 while pressed (spring back). `OperationCard` and the customer card
   (`CustomerCardItem`) scale to 0.98 while pressed. Use one shared
   `Modifier.pressScale(interactionSource)` in `core/ui/AnimatedAmount.kt` or a new
   `core/ui/PressScale.kt`.
6. **Save feedback**: a light haptic (`HapticFeedbackType.LongPress` or `Confirm`) when a session
   or voucher is saved successfully. Hook it where the success toast is shown today.

## Allowed files
- `MainScreen.kt`, new `core/ui/AnimatedAmount.kt`
- `features/home/HomeComponents.kt` (QuickActionCard), `features/home/HomeScreen.kt`
- `features/customers/CustomersScreenComponents.kt`, `features/customers/CustomerDetailScreen.kt`
- `features/vouchers/VouchersScreen.kt`: list item modifiers and the save haptic only. **Do not
  touch the summary card** (task 024 edits it in parallel).
- `core/ui/OperationCard.kt`, `features/customers/CustomerCardItem.kt` (press scale only)
- new `core/ui/PressScale.kt`
- `features/sessions/SessionDialogs.kt` and `features/wellowners/WellOwnerPurchaseBottomSheet.kt`
  only if the save haptic must be triggered there

## Done when
- Build and tests pass. Nothing waits for an animation to finish before the user can act.

---
## Review notes (written by Claude)
_(empty)_
