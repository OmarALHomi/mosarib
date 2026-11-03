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
