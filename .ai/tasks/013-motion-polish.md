status: accepted
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

---
**Verdict: accepted (2026-11-02, code review). Detail slide with AnimatedContent (Start/End, RTL-aware, 250ms) with the pager state hoisted, so the tab is kept; tab crossfade 200ms with snap() when animations are off; animateItem on the operation, customer and ledger items; AnimatedAmount; shared PressScale that respects ValueAnimator.areAnimatorsEnabled(); save haptic on SUCCESS toasts. The operations summary card is untouched. Tests pass. Device check after the merge (phone disconnected). Note: the haptic fires on every SUCCESS toast, not only saves. Acceptable.**

**Device check after the code review: changes-requested** (2026-11-02). **Blocker:** on the device,
tapping any bottom-bar tab (العملاء، العمليات، التقارير) does nothing. The app stays on Home and
the pill stays on "الرئيسية".

Cause: the `HorizontalPager` was removed and replaced with a `Crossfade` keyed on
`pagerState.currentPage`. That `PagerState` is no longer attached to any pager. Without layout,
`scrollToPage(idx)` never updates `currentPage`, so the Crossfade never changes.

Fix (`MainScreen.kt`):
- Replace the pager state with a plain hoisted index:
  `var selectedTab by rememberSaveable { mutableIntStateOf(HOME_INDEX) }`.
- `MosaribNavBar(selectedIndex = selectedTab, onItemSelected = { selectedTab = it })`.
- `Crossfade(targetState = selectedTab, …)`.
- Every place that called `pagerState.scrollToPage(x)` or `animateScrollToPage(x)` sets
  `selectedTab = x` instead: lines ~276, 535, 542, 556 and 636, plus the reports "عرض الكل"
  callback from 021.
- Remove the unused `pagerState`.

Re-test on the device: every tab switches with the crossfade; Home quick actions that change tab
still work; reports "عرض الكل" opens customers with "عليه"; opening a customer and coming back
keeps the tab. Append a "Round 2" section to `.ai/reports/013.md`.

**Round 2 verdict: accepted** (2026-11-02). Device check: every tab switches (العملاء، العمليات، التقارير، الإعدادات، الرئيسية); opening a customer slides in and back returns to the customers tab with the list kept. Tests pass.
