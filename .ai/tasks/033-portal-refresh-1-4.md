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
3. Change nothing in `api/auth.php` or the Firebase rules.

## Allowed files
`web_dashboard/index.php`, `web_dashboard/admin.php`, `web_dashboard/api/public_data.php`,
`web_dashboard/api/release_manager.php`, `web_dashboard/assets/*` (only if a style is needed),
`web_dashboard/data/portal_settings.json` (new keys only, empty values).

## Done when
`php -l` passes on every changed PHP file; the page opens locally (`php -S localhost:8000
dev-router.php`) with the stats and testimonials hidden, and they appear after you fill them in
admin. List the changes in the report.

---
## Review notes (written by Claude)
_(empty)_
