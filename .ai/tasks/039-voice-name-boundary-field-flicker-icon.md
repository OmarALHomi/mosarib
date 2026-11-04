status: done
<!-- Owner on the phone, 2026-11-04. Logcat checked by Claude. Can run in parallel with 038 (different files). -->
# Task 039: Voice name boundary, «مسلم» without an amount, customer field flicker, launcher icon

## Facts from logcat
- **Spoken:** «مع احمد علي 20 ساعه من 4000 مسلم».
- **Parsed:** `customerQuery=احمد علي مسلم`, `paid=null`, `paidFull=false`.
  - The trailing «مسلم» has no amount, so it was not used as a paid marker. Whatever was left was then glued to the name.
- A second try was heard as «**ما** احمد علي اربع ساعات…»: «مع» was recognised as «ما».

## 1. The name is one contiguous run (`VoiceCommandParser`)
After the customer joiner (مع / من / لـ / حق …), the name is **only the contiguous words up to the first slot token**:
- a number;
- a duration;
- «من/بسعر» + amount;
- a paid marker;
- a description starter.

Rules:
- Words that are left **after** the slots never join the name.
  - If they are a known marker, use them (see 2).
  - Otherwise they go to `notes`, or to `unparsed` if they are a single unknown word.
- Keep the 037 full-name rule.
- **Tests:**
  - «مع احمد علي 20 ساعه من 4000 مسلم» gives name «احمد علي».
  - «مع احمد علي ساعتين من 10 الف ومسلم 5000 باقي على الشهر» gives name «احمد علي» and notes «باقي على الشهر».
  - «احمد علي 20 ساعه من 4000» with no joiner gives name «احمد علي».

## 2. A paid marker with no amount means paid in full
- When «مسلم / سلم / دفع / مدفوع / واصل» stands with **no amount after it** (end of the sentence, or followed by a non-number), set `paidFull = true` and leave `paid = null`. Today «مسلم كامل» works only as a phrase.
- «ما عليه شي» stays as it is.
- **Tests:**
  - «… من 4000 مسلم» gives `paidFull=true`;
  - «… ومسلم 5000» gives `paid=5000`, `paidFull=false`.

## 3. «ما» heard instead of «مع»
- At the **start** of the sentence only, treat «ما» as the joiner «مع» when a name and a slot follow.
- Never do this for «ما عليه شي».
- **Test:** «ما احمد علي اربع ساعات من 4000» gives name «احمد علي», 240 minutes.

## 4. Session form: wrong registered customer picked (`SessionDialogs.kt`)
Two places still take `CustomerMatcher.match(...).firstOrNull()` without a confidence check, so a completely different registered name is selected:
- the customer-field mic, `"customer" ->` (~line 727);
- the in-form voice command, `"form_command"` (~line 770).

Use the 037 rule in both:
- select only when `CustomerMatcher.isHighConfidence(ranked)` is true;
- otherwise put the spoken text in the field, leave `selectedCustomerId = 0`, and show the suggestions together with "إضافة عميل جديد".

Do the same in `VouchersScreen.kt` and `WellOwnerPurchaseBottomSheet.kt` if they have a field mic that uses `firstOrNull()`.

**Home chooser (`HomeScreen.kt` ~294):** when `ranked` is empty, do **not** fill the chooser with `customers.take(3)`, which are unrelated names. Show only «إضافة عميل جديد باسم «…»» and «اختيار من القائمة».

## 5. Customer field flickers and refuses deletion (`SessionDialogs.kt` ~682)
```kotlin
var customerInputText by remember(selectedCustomer?.name, formKey) {
    mutableStateOf(selectedCustomer?.name ?: voiceCommand?.customerQuery ?: "")
}
```
What happens:
- Every key press that changes `selectedCustomerId`, which `onValueChange` does, changes the remember key.
- The state is then re-created from `voiceCommand.customerQuery`, so the deleted text comes back.

Fix:
- Key the state on `formKey` only.
- Set the text explicitly where a customer is chosen. `onItemSelected`, the voice branches and quick-add already do this.
- Never re-derive it from `voiceCommand` after the first composition.

**Check:** voice-fill a name, delete it letter by letter, type another name. No flicker, and nothing comes back.

Check the same pattern in `VouchersScreen.kt` (`custSearchQuery`) and `WellOwnerPurchaseBottomSheet.kt`.

## 6. Launcher icon matches the home logo
The home header logo (`HomeScreen.kt` ~334) is:
- a rounded square with a linear gradient `PrimaryTeal #007A87` → `AccentEmerald #00B894`;
- a white Material `WaterDrop`.

The launcher icon is different: a dark navy background, a gradient drop and a gold ring.

Change it to match the home logo:
- `res/drawable/ic_launcher_background.xml`: the linear gradient #007A87 → #00B894, top-left to bottom-right.
- `res/drawable/ic_launcher_foreground.xml`:
  - the white Material WaterDrop path (filled `#FFFFFF`), centred, about 44 dp inside the 108 dp canvas, so it stays inside the 66 dp safe zone;
  - no gold ring, no extra shapes.
- `<monochrome>` keeps working: the same drop shape.
- Regenerate the legacy `mipmap-*/ic_launcher*.png` / `.webp` if the project has them, so older launchers match.
- Check `web_dashboard/assets/images` / the favicon only if it is the same artwork. Do not change the web files in this task; list them in the report.

**Animated icon (owner asked):**
- Android does not let a normal app animate its launcher icon. The live Clock and Calendar icons are special cases the launcher builds only for its own system apps.
- **Instead:** if the app uses the SplashScreen API (`Theme.SplashScreen` / `windowSplashScreenAnimatedIcon`), add a short `animated-vector` for the splash screen: the drop falls and a ripple spreads, at most 800 ms. If there is no splash setup, skip this and say so in the report.

## Allowed files
- `core/voice/VoiceCommandParser.kt`, `core/voice/VoicePhraseRules.kt`;
- `features/sessions/SessionDialogs.kt`, `features/home/HomeScreen.kt` (chooser list only);
- the customer field / voice parts of `features/vouchers/VouchersScreen.kt` and `features/wellowners/WellOwnerPurchaseBottomSheet.kt`;
- `res/drawable/ic_launcher_*.xml`, `res/mipmap-*/ic_launcher*`;
- splash theme files and a new `res/drawable/splash_drop_anim.xml` (§6 only);
- tests.

## Done when
- Build and tests pass, with the new parser tests from §1–3.
- On the phone:
  - «مع احمد علي 20 ساعه من 4000 مسلم» opens the form with name «احمد علي», 20 hours, 4,000, paid in full;
  - no registered customer is selected when the name is new;
  - deleting the name works with no flicker;
  - the launcher icon looks like the home logo.

## Done by Claude (owner asked), 2026-11-04
- §1–3 in `VoiceCommandParser` (`nameRegionWords`, bare paid marker → paidFull, «ما» → «مع»), with 5 new tests in `VoiceParsersTest`.
- §4 high-confidence rule in the session field mic / form command and the vouchers form; Home chooser no longer lists unrelated customers.
- §5 `customerInputText` keyed on `formKey` only, plus a `LaunchedEffect(selectedCustomer?.id)`; checked on the phone: deleting letters works with no reset.
- §6 adaptive icon = home logo; legacy webp regenerated. The splash is a custom Compose screen that already animates the drop, so no SplashScreen API work.
