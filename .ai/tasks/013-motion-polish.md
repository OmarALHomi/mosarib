status: draft
<!-- 2026-11-02: held until 011, 012, 016, 018, 019 and 020 are merged; it touches files of all of them. -->

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
3. **Lists**: `Modifier.animateItem()` on items in the customers list, the operations list and the
   customer ledger (insert, remove, reorder after filter).
4. **Numbers**: a small `AnimatedAmount(value, …)` composable in `core/ui` that counts from the
   previous value to the new one over 400ms. Use it for the customer balance in the detail balance
   card and the two totals of the customers summary bar.
5. **Press feedback**: `QuickActionCard` on Home and the two big money buttons in customer detail
   scale to 0.97 while pressed (spring back).
6. **Save feedback**: a light haptic (`HapticFeedbackType.LongPress` or `Confirm`) when a session
   or voucher is saved successfully. Hook it where the success toast is shown today.

## Allowed files
- `MainScreen.kt`, new `core/ui/AnimatedAmount.kt`
- `features/home/HomeComponents.kt` (QuickActionCard), `features/home/HomeScreen.kt`
- `features/customers/CustomersScreenComponents.kt`, `features/customers/CustomerDetailScreen.kt`
- `features/vouchers/VouchersScreen.kt` (list modifier and haptic only)

## Done when
- Build and tests pass. Nothing waits for an animation to finish before the user can act.

---
## Review notes (written by Claude)
_(empty)_
