status: ready
<!-- Owner request 2026-11-03: fix the visitors page (web_dashboard/index.php) and the admin before
uploading 1.4.0. Keep the design; text and data fixes only. No secrets in PHP/JS files. -->
# Task 033: Visitors page and admin ready for release 1.4.0

## A. Visitors page (`web_dashboard/index.php`)
1. **Invented numbers:** lines ~480–500 show 1240 wells, 38000 vouchers and 99.9%. Read them from
   `data/portal_settings.json` (new keys `stats_wells`, `stats_vouchers`, editable in the admin
   portal settings). Hide the stats strip when the values are empty. Remove the 99.9% item.
2. **Testimonials** (~line 610–650, full real-looking names): read them from `portal_settings.json`
   (a `testimonials` list editable in admin). Hide the section when the list is empty. Ship it empty.
3. **Fallback version** (line ~142): '1.2.0' and '2.88' → when no release record exists, hide the
   version and size instead of showing old values.
4. **Features (line ~552):** add cards, same style, for the 1.4.0 features:
   - القراءة الصوتية للمبالغ والعمليات (free);
   - الإدخال الصوتي: قل العملية ويعبّئها التطبيق (free);
   - كشوف حساب PDF وواتساب;
   - نسخ احتياطي على الهاتف و Google Drive.
   Wrap Latin words in Arabic lines with `⁨…⁩` or `<bdi>`.
5. **Wording:** "حساب بالثواني والدقائق" → "حساب دقيق بالدقيقة". "تاريخ لا يقبل التلاعب" → remove
   that clause. Remove emojis from buttons and messages (💬، ❌، 📥 …).
6. Western digits everywhere on the page.

## B. Admin (`web_dashboard/admin.php`, `api/*`)
1. Portal settings form: fields for `stats_wells`, `stats_vouchers` and the testimonials list
   (name, place, quote; add / remove), saved with the existing CSRF-protected save.
2. Release manager (`api/release_manager.php`): the "تحديث إجباري" checkbox shows the warning
   "لا تفعّله إلا بعد أن يحدّث أغلب المستخدمين؛ يمنع الإصدارات الأقدم من العمل" and is unchecked by default.
3. Change nothing in the Firebase rules.

## C. Security (do these first, in order; findings verified by Claude 2026-11-04)
1. **Stored XSS public → admin:** subscription fields (name, region, receipt, phone), feedback
   fields, user phone and broadcast title/message are put into `innerHTML` raw in
   `assets/js/admin-core.js` (~1239, 1257, 1273, 1276, 1671, 2016–2028, 2115, 159, 846–849). Use the
   existing `escapeHtml()` on every interpolated value; pass ids via `data-*` attributes, not inline
   `onclick` strings. Server side (`api/public_data.php` ~318–362): trim and limit each field
   (`mb_substr` 80; phone `^[0-9+ ]{7,15}$`).
2. **CSRF on every admin write:** `api/public_data.php` (~370–580: update_settings, add_news,
   delete_news, save_payment_methods, save_subscription_pricing, delete_*, cancel_broadcast) and
   `api/release_manager.php` call only `requireAuth()`. Add `verifyCsrfToken(...)` after it for every
   non-GET action, and make the admin JS send the header. Login (`auth.php` ~181) also checks CSRF.
   Session cookie `SameSite=Strict`.
3. **Public form flooding:** the subscription form and analytics endpoints have no limit. Add a
   per-IP throttle (5 per hour, same file pattern as login attempts, with `flock`) and a hidden
   honeypot field; never let spam evict older real requests.
4. **APK upload** (`release_manager.php` ~36–60): enforce `MAX_UPLOAD_SIZE`, check the ZIP magic
   bytes (the file starts with "PK" then bytes 03 04), and allow only ADMIN / SUPER_ADMIN (not MODERATOR) to upload or publish a
   force update.
5. **set_cloud_link** (~107–140): accept only `https://` URLs (`filter_var`), and add a
   `version_name` field instead of inventing `1.{code}.0`.
6. **Login throttle** (`auth.php` ~65–105): also throttle per email; use `flock` on the attempts
   file.
7. **Bearer token** (`auth.php` ~107–110, `admin-auth.js` ~127): remove the deterministic
   `X-Admin-Token` path; rely on the HttpOnly PHP session only.
8. `uploads/`: add `Options -Indexes` to its `.htaccess` and an empty `index.html`.
9. `firebase_proxy.php` ~23–30: allow-list the top-level nodes the dashboard really uses; ignore
   `?method=` / `X-HTTP-Method-Override` on GET.
10. `public_data.php` ~535–580: validate Firebase keys with `^[A-Za-z0-9_-]+$` before building paths.
11. Admin pages: `<meta name="robots" content="noindex">` in `includes/head.php`. Every
    `target="_blank"` link gets `rel="noopener"`.
12. Do not deploy `dev-router.php` (note it in the report; do not delete it, it is used locally).

## D. Correctness
1. Analytics (`index.php` ~19–62, `public_data.php` ~241–313): read-modify-write under `flock`;
   skip obvious bots; ignore a client-sent `is_unique`.
2. APK size: `release_manager.php` stores `file_size` (bytes) but the page reads `file_size_mb` →
   compute MB from bytes.
3. Download URL (`index.php` ~129–139): for a relative URL check `file_exists`; fall back to the
   newest APK in `uploads/`; hide the button if none.
4. `release_manager.php`: return a JSON error for an unknown action or method.
5. `index.php` ~1146–1148: `toLocaleString('en-US')` and `textContent`; ~1195 success message via
   `textContent`.
6. Server allow-list for `payment_method` and `plan` in the subscription request.
7. **Personal data in source:** `index.php` ~879–886 has a fallback with a full name, bank account
   number and phone shown when `payment_methods.json` is empty. Remove the fallback (show nothing).
8. One source for the default hour price (index.php 18000 vs public_data 7500): read it from
   settings only.

## E. Page quality
1. `<head>`: canonical, `og:image` / `og:type` / `og:url` / `og:locale=ar_AR`, `twitter:card`; og
   title/description from the same settings as `<title>`; a PNG favicon and apple-touch-icon.
2. Images: `loading="lazy"` and `width`/`height` on all below-the-fold images; WebP copies of the
   four large images (news_crops, hero_phone_mockup, news_water, feat_shield) with JPG fallback via
   `<picture>`. `Cache-Control: no-store` only on API responses, not on the page.
3. Form: `autocomplete="name"` / `"tel"`, `inputmode="numeric"` for phone and receipt; Esc closes
   the modal and focus returns to the button.

## F. Admin UX
1. Confirmation with the name shown before deleting an admin account; block removing or
   deactivating the last SUPER_ADMIN.
2. `save_payment_methods`: confirm before overwriting.
3. Force-update warning (B2 above) also in the confirm text of cancel/delete broadcast: "سيُلغى
   التحديث الإجباري أيضاً".
4. Remove emojis from admin buttons and messages.

## Allowed files
`web_dashboard/index.php`, `web_dashboard/admin.php`, `web_dashboard/api/public_data.php`,
`web_dashboard/api/release_manager.php`, `web_dashboard/assets/*`, `web_dashboard/api/auth.php` (C2, C6, C7 only),
`web_dashboard/api/firebase_proxy.php`, `web_dashboard/includes/head.php`, `web_dashboard/uploads/.htaccess`,
`web_dashboard/uploads/index.html` (new),
`web_dashboard/data/portal_settings.json` (new keys only, empty values).

## Done when
`php -l` passes on every changed PHP file; the page opens locally (`php -S localhost:8000
dev-router.php`) with the stats and testimonials hidden, and they appear after you fill them in
admin. List the changes in the report.

---
## Review notes (written by Claude)
_(empty)_
