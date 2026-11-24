status: superseded-by-052 (merged)
<!-- Owner request 2026-11-22. Only LicenseDialog.kt; can run in parallel with 048/049. -->
# Task 050: Subscription screen easier to understand

File: `core/license/LicenseDialog.kt` (the «الاشتراكات والتفعيل» sheet). Today it is one long form
(name, phone, region, transfer method, transfer number, device, plan cards that show
«جاري التحميل…», features list) — the user does not know what to do first.

## Target layout (top to bottom)
1. Status box at the top, one sentence:
   - trial running: «أنت في الفترة التجريبية: بقي لك N عملية مجانية»
   - trial ended: «انتهت الفترة التجريبية. بياناتك محفوظة، ويمكنك التصفح، لكن إضافة العمليات تحتاج اشتراكاً»
   - subscription active: «اشتراكك فعّال حتى <date>» ; expired: «انتهى اشتراكك في <date>».
   (use the values LicenseManager already exposes; `mandatoryReason` text can feed the ended case).
2. Numbered steps, each a small card with a big number:
   **١ اختر الباقة** (شهري / سنوي with price; if prices are still loading show the last cached price
   or «—» with a «إعادة المحاولة» button, never an endless «جاري التحميل…»).
   **٢ ادفع** (the transfer method menu + the account/wallet details it already shows, + optional
   transfer number).
   **٣ أرسل الطلب** (name + phone fields, then one big button «إرسال الطلب عبر واتساب» doing what the
   current send/copy action does).
   **٤ أدخل رمز التفعيل** (the existing activation-code field and «تفعيل» button).
3. Region field and device line move into a collapsed «تفاصيل إضافية». Features list stays at the
   bottom, collapsed by default.
Keep all logic, network calls, validation and the security rules exactly as they are (no new
Firebase writes; activation only through the existing path). Layout and wording only.

## Allowed files
core/license/LicenseDialog.kt only.

## Done when
Build + tests pass; the four steps read top to bottom; nothing functional changed.
