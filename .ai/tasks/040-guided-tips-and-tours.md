status: done
<!-- Owner request 2026-11-05: professional in-app guidance (tooltips and first-time tours) like the big apps. -->
# Task 040: Spotlight tours and first-time tips in every screen

## What exists today
- `features/onboarding/OnboardingScreen.kt`: welcome pages on first launch. **Keep it.**
- `features/home/HomeShowcaseTour.kt`: `HomeShowcaseTourDialog`, a paged **dialog** (title, icon, description, tip).
  - It does not point at the real buttons.
  - It is opened by `showHomeTour` in `HomeScreen.kt` (~174, ~384, ~849) and the «؟» button «دليل الاستخدام السريع».
  - The completion flag is `LicenseManager.isHomeTourCompleted` / `setHomeTourCompleted`.
- **Do not change `LicenseManager`** (license logic is frozen). Read the old flag only to migrate it.

## Goal
Guidance that points at the real element on screen, shows once, and never gets in the way.

## 1. Reusable spotlight component (new `core/ui/guide/`)
1. **`GuideTarget`**
   - A `Modifier.guideTarget(id: String)` that records the element's bounds with `onGloballyPositioned`.
   - It writes the bounds into a `GuideRegistry` held by `CompositionLocal`.
2. **`SpotlightTour(steps, onFinish)` overlay**, full screen, drawn above the content (not a Dialog, so the target stays visible). Each step has:
   - a dimmed scrim at about 70% black, with a rounded **cut-out** around the target (8 dp padding), drawn with `BlendMode.Clear` on an offscreen layer;
   - a soft pulsing ring around the cut-out (`rememberInfiniteTransition`, subtle);
   - a **bubble** with:
     - the title, bold, Cairo;
     - one or two short lines of text;
     - a step counter «2 من 4»;
     - buttons «التالي» / «إنهاء» and «تخطّي»;
     - an optional 🔊 button that uses the existing `core/speech/SpeakButton`.
   - **Bubble placement:**
     - below the target if there is room, otherwise above;
     - an arrow pointing at the target;
     - kept inside the screen with a 16 dp margin;
     - RTL-correct.
   - **Behaviour:**
     - tapping the cut-out = «التالي»;
     - back button = skip;
     - a step whose target is not on screen (bounds missing, or scrolled off) is skipped silently.
   - **Motion:** fade and scale on step change (about 200 ms).
   - **Theme:** colours from the theme, light and dark (`MaterialTheme.mosarib` / `PrimaryTeal`). Western digits.
3. **`FirstTimeHint(id, text)`**
   - A small inline banner: icon 💡, one line, and a «×».
   - Shown above a field or section the first time only; it disappears after «×» or after the user uses that field.
4. **`GuidePreferences`** (own SharedPreferences file `mosarib_guides`):
   - `isTourDone(screenId)`, `markTourDone`, `isHintDone(id)`, `markHintDone`, `resetAll()`.
   - **Migration:** if `LicenseManager.isHomeTourCompleted` is true, treat the `home` tour as done.
5. **`GuideTexts.kt`:** every tour and hint text in one Kotlin file (screen id → steps), so wording can change in one place.
   - Rule: each line is at most about 70 characters, simple Yemeni-friendly Arabic.

**Start rules:**
- A tour starts automatically only when:
  - the screen is fully laid out (about 500 ms after the first frame);
  - no dialog or bottom sheet is open;
  - the license / blocked screens are not showing;
  - onboarding is finished.
- Never two tours at once.
- A tour that was skipped counts as done.

## 2. Tours (first phase)
Steps and wording are a draft; keep the meaning and polish the Arabic.

1. **Home (`home`).** Replaces the dialog. `HomeShowcaseTour.kt` may be deleted once nothing uses it.
   1. «سقي جديد»: «سجّل دورة الري: العميل، المدة، والسعر.»
   2. «إدخال صوتي»: «قل العملية بصوتك، مثل: مع أحمد ساعتين من 10 آلاف.»
   3. «قبض» / «صرف»: «سندات القبض من العملاء، والصرف للمصروفات.»
   4. «آخر دورات السقي»: «اضغط أي دورة لتعديلها أو مشاركتها.»
   5. «التقرير الكامل»: «كشف شامل PDF لكل الحسابات.»
2. **Session form (`session_form`)**, the first time `AddEditSessionBottomSheet` opens:
   1. the customer field: «اختر عميلاً أو اكتب اسماً جديداً.»
   2. the field mic: «أو قل الاسم بصوتك.»
   3. the start/manual time choice: «من الساعة إلى الساعة، أو عدد ساعات يدوي.»
   4. the price: «سعر الساعة يُملأ من سعر العميل تلقائياً.»
   5. the save button.
   - **Hint:** above «احتساب السقي على صاحب بئر»: «فعّله إذا كانت القيمة تُخصم من مستحقات صاحب البئر.»
3. **Voucher form (`voucher_form`)**, `AddVoucherBottomSheet`: the type, customer, amount (with words shown under it) and save.
4. **Well-owner purchase (`purchase_form`)**, `WellOwnerPurchaseBottomSheet`: the owner, the hours and waste, the price.
5. **Bottom bar (`nav`)**, after the home tour, as a separate short tour: العمليات، العملاء، التقارير، الإعدادات (one line each).

**Second phase (same component, short tours):**
- `CustomersScreen`: card, balance colours, statement.
- `VouchersScreen` (operations list): filter, long-press.
- `ReportsScreen`: period filter, PDF export.
- Backup settings: «انسخ الآن», Drive.

## 3. Help controls
1. **«؟» button:**
   - The «؟» in the home header restarts the **home** tour.
   - Add the same small «؟» icon in the top bar of each screen and form that has a tour; it restarts that tour.
2. **Settings, support section (`SettingsSupportSection.kt`):** «إعادة عرض التعليمات» calls `GuidePreferences.resetAll()` and shows a toast.
3. **Help page (`features/help/HelpScreen.kt`, new):** reached from Settings → «مركز المساعدة».
   - About 8 short Q&A cards, each opening to an answer of 2 to 4 lines:
     - تسجيل سقي بالصوت
     - الفرق بين القبض والصرف
     - شراء الساعات وحساب صاحب البئر
     - الكشف عبر واتساب
     - النسخ الاحتياطي واستعادته
     - تعديل وحذف العمليات
     - التقارير
     - الدعم
   - Each card has a «أرني» button that navigates to the screen and starts its tour.

## Rules
- No new library. Compose only.
- No change to accounting, voice parsing, license or Firebase.
- Accessibility:
  - the bubble has `contentDescription`;
  - the buttons are at least 48 dp;
  - it works with font scale 1.3.
- Performance: the overlay is not composed at all when no tour is running.
- Keep every existing string, colour and layout outside the guide.

## Allowed files
- New: `core/ui/guide/*` (the component, `GuidePreferences`, `GuideTexts`), `features/help/HelpScreen.kt`.
- Add `guideTarget(...)` modifiers, the «؟» button and the start logic in:
  - `features/home/HomeScreen.kt` (and delete `HomeShowcaseTour.kt`);
  - `features/sessions/SessionDialogs.kt`;
  - `features/vouchers/VouchersScreen.kt`;
  - `features/wellowners/WellOwnerPurchaseBottomSheet.kt`;
  - `core/ui/MosaribNavBar.kt`;
  - `features/customers/CustomersScreen.kt` and `features/reports/ReportsScreen.kt` (phase 2);
  - the backup settings screens (phase 2).
- `features/settings/SettingsSupportSection.kt` (reset and help entry);
- `MainScreen.kt` (help route only);
- tests.

## Tests
Unit tests for:
- `GuidePreferences`: done/reset, and the migration from the old home flag;
- the bubble placement as a pure function: target near the bottom puts the bubble above; target at the edge keeps the bubble inside the margins.

## Done when
- Build and tests pass.
- On the phone, after «إعادة عرض التعليمات»:
  - the home tour points at the real buttons;
  - the session form tour runs on first open;
  - nothing shows a second time.
  - Dark mode and light mode both look right.
- The report has screenshots:
  - home step 1 and 2;
  - session form step 1;
  - one hint;
  - the help page.
- Phase 2 may be a separate round. Say in the report what is left.

## Review round 1 (Claude)
Home tour works. Blockers: form tours are drawn under the bottom sheet (invisible), the nav tour is skipped because the overlay lives inside HomeScreen, and a duplicate hint. See `.ai/reviews/040-round1.md`.

## Round 2 done by Claude (owner asked), checked on the phone
- `SpotlightTour` now runs in a focusable full-window `Popup`; target bounds use screen coordinates (`localToScreen`), so it works across the main window and bottom sheets.
- Form tours are composed from inside the sheet (footer slot), so the popup is a child of the sheet window and draws above it.
- `key(activeTour)` around each tour: the step counter restarts for the nav tour (it was finished instantly before).
- Targets scroll into view with `BringIntoViewRequester` before each step.
- Duplicate well-owner hint removed; it shows only for a well-owner customer.
- `GuideHelpButton` is a 30 dp box; sheet titles stay on one line and shrink up to 20% (`SheetTitleText` in `MosaribSheets.kt`).
- Verified: home 5 steps, nav 4, session form 5, voucher form 4, purchase form 4.
