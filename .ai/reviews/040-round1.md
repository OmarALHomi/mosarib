# Review 040, round 1 (Claude, on the phone)

**Build and tests:** pass.

**Good:**
- The home tour points at the real buttons.
- The bubble, counter, «التالي / تخطّي / إنهاء» and the speak button work.
- `GuidePreferences` and the migration are correct, and `LicenseManager` is unchanged.

Claude already fixed one thing: the «إدخال صوتي» card in `HomeScreen.kt` had no `guideTarget("home_voice")`, so step 2 of the home tour was skipped. Keep that fix.

## Blockers
1. **Form tours never show (session, voucher, purchase).**
   - `SpotlightTour` is composed **after** `MosaribBottomSheet` in the parent composition:
     - `SessionDialogs.kt` ~2532;
     - `VouchersScreen.kt` ~2972;
     - `WellOwnerPurchaseBottomSheet.kt` ~1090.
   - A bottom sheet is a separate window, so the overlay is drawn **under** the sheet and is invisible. The tour also stays "active" and is never marked done.
   - **Fix:**
     - draw the overlay **inside** the sheet content (a `Box` that wraps the sheet's content, with the overlay as its last child, filling the sheet);
     - or show it in a `Popup` with `PopupProperties(focusable = true)` above the sheet window.
   - The target bounds must be measured in the same coordinate space as the overlay; `boundsInWindow` differs between windows.
   - Check all three forms on the phone.
2. **Bottom-bar tour (`nav`) never shows, and the last home step is cut.**
   - The home `SpotlightTour` lives inside `HomeScreen`, whose bounds end above the bottom bar:
     - the scrim does not cover the bar;
     - the nav targets are outside the overlay, so every nav step is skipped and the tour is marked done;
     - the «التقرير الكامل» cut-out is half hidden behind the bar.
   - **Fix:** host the screen-level tours (home, nav) in `MainScreen` above the `Scaffold`, full screen. For example, a `GuideHost` that `HomeScreen` asks to start a tour through a callback or a `CompositionLocal`.
   - Before showing a step whose target is partly off screen, scroll it into view with `BringIntoViewRequester`, or skip it.
3. **Duplicate hint.** `FirstTimeHint(HINT_SESSION_WELL_OWNER)` is added twice in `SessionDialogs.kt` (~978 and ~989), with stray blank lines. Keep one.
   - Show it **only when the well-owner switch is visible** (`selectedCustomer?.isWellOwner == true`). Today it shows above an empty customer field.

## Minor
4. **Sheet header:**
   - The new «؟» button makes the title «تسجيل دورة سقي جديدة» wrap to two lines.
   - Use the smaller 36 dp `GuideHelpButton` style and keep the title on one line (`maxLines = 1`, ellipsis if needed).
   - Same in the voucher and purchase sheets.
5. **Touches outside the scrim.** While a tour is running, taps on the bottom bar still go through. This is solved by item 2, because the scrim then covers the whole screen.

## Done when
- Build and tests pass.
- On the phone, after «إعادة عرض التعليمات»:
  - home tour, 5 steps;
  - then the nav tour, 4 steps, highlighting the bar items;
  - the session form tour on first open;
  - voucher and purchase forms likewise;
  - one hint, only for a well-owner customer.
- Screenshots of each in the report.
