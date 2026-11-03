status: ready
<!-- Owner decision 2026-11-03. Runs AFTER 027 is merged: it touches CustomerDetailScreen.kt and
SettingsGeneralSections.kt, which 027 changes. Small task. -->
# Task 029: Voice reading free for everyone; say the hourly rate in the sentences

## A. Reading is free (owner decision)
The free tier is already limited by the number of operations, and reading aloud is a strong first
impression that leads to subscribing.
- **Free for everyone:**
  - `SpeakButton` (card icons, the `AmountWithWords` tooltip, the balance button);
  - the "قراءة العملية" menu items;
  - "تجربة الصوت" / "تجربة النطق";
  - the voice choice.
  - Remove the lock badge and the license check from these.
- **Stays for subscribers only:** "إرسال كشف صوتي" (WhatsApp voice statement). Keep its lock
  badge, `LicenseDialog`, and the reason line "الكشف الصوتي متاح للمشتركين." (the word القراءة
  is removed from the reason).
- Keep the check in one place: add `SpeechAccess.canSendVoiceStatement(context)` in
  `core/speech/` that calls `LicenseManager.isActivated` only. Do not change `LicenseManager`.
  Remove the now-unused `isSubscribed` states.

## B. Say the hourly rate after the hours (owner)
The owner's spoken style is "ساعتين ونص **من عشرة آلاف**". In `SpeechTextBuilder`:
- session:
  - "سقي لـ{name}، {duration} من {rate words}، المبلغ {total words}، المدفوع …، الباقي …";
  - or "…، مسدد بالكامل";
- purchase: "شراء ساعات من {owner}، {duration} من {rate words}، {amount words}";
- the voice statement lines (short form): "{day}: سقي {duration} من {rate}، الباقي {…}";
- the rate uses the same speech number words ("عشرة آلاف"), with no currency word after it.

Update the tests.

## C. Pronunciation of مِئَة (owner, 2026-11-03)
The engine reads "مِئِه" / "مِئِة" because the speech text writes "مِئة" with a kasra on the م
only, so the hamza has no vowel. In `SpeechPronunciation` (all hundreds replacements, the
dictionary and `SAMPLE_PRONUNCIATION_TEXT`), write **"مِئَة"** (fatha on the hamza), so it is read
"مِئَه" in pause and "مِئَتْ" before ألف. Check that "مِئَتَيْن" keeps its marks. Update the tests
(`"مِئَهْ وَخَمْسِينْ"`, `"مِئَتْ أَلْفْ"`).

## D. Voice preview reads "تجربه قُراءه" with a broken sound (owner, 2026-11-03)
The preview text in `MosaribSpeaker.previewVoice` is "مرحباً، هذه تجربة قراءة الصوت العربي في تطبيق
المسرّب.". Its words are not in the dictionary, so they get only the pause ending and the engine
guesses the vowels ("قُراءه").
1. Add to the dictionary, with marks:
   - تجربة → تَجْرِبَة، قراءة → قِرَاءَة، القراءة → القِرَاءَة؛
   - صوت → صَوْت، الصوت → الصَّوْت، العربي → العَرَبِي، تطبيق → تَطْبِيق؛
   - مرحبا / مرحباً → مَرْحَبًا، هذه → هَذِهِ.
2. **Dictionary lookup must ignore marks already in the text:** remove diacritics and the shadda
   from the word before the lookup. Today "المسرّب" (with a shadda) does not match the key
   "المسرب".
3. Use a better preview sentence that also tests a number: "مَرْحَبًا، هَذَا صَوْت تَطْبِيق المُسَرِّب.
   رَصِيد أحمد عَلَيْه خمسة آلاف وخمس مِئَة رِيَال."

## E. Choppy sound: compare two pause modes, the owner chooses by ear
A sukun on **every** word (today) may make the engine pause between words. Add the switch
**"قراءة متصلة"** in the voice page (default on):
- **On:** put the pause ending only on the **last word of each phrase** (before ، . : ؛ ? or
  the end), plus the dictionary words (they carry their own marks). Do not add a sukun to the
  other words inside a phrase. The ة rule (ه in pause, ت before ألف/آلاف/مليون) applies at those
  same positions.
- **Off:** today's behaviour (pause ending on every word).
- Save the choice; `forSpeech(text, connected: Boolean)`.
- Test both modes.
- In the report, write the preview sentence's speech text in both modes, so the owner can compare.

## Allowed files
- `core/speech/*` (new `SpeechAccess.kt`)
- `core/ui/AmountWithWords.kt`, `core/ui/OperationCard.kt` (lock removal only)
- `features/customers/CustomerCardItem.kt`, `features/customers/CustomerDetailScreen.kt`,
  `features/vouchers/VouchersScreen.kt`, `features/settings/SettingsGeneralSections.kt` (lock
  removal only)
- tests

## Done when
- Build and tests pass.
- On the device, with a non-activated build state (or a unit-tested `SpeechAccess`):
  - reading works without a lock;
  - the voice statement still shows the lock.
- A session read aloud says "… من عشرة آلاف …".

---
## Review notes (written by Claude)
_(empty)_
