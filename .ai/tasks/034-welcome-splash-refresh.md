status: ready
<!-- Owner request 2026-11-04: refresh the welcome (onboarding) and splash screens for 1.4.0. Keep the
Google sign-in / account-check logic exactly as it is; texts, order and styling only. -->
# Task 034: Welcome and splash screens for 1.4.0

## A. Welcome (`features/onboarding/OnboardingScreen.kt`)
1. **Pages (5)**, each with one icon, a title of 3–6 words, one subtitle of up to 2 lines, and 3 pills:
   1. "سقي محسوب بالدقيقة": عداد دقيق · سعر الساعة · من/إلى.
   2. "سندات وحسابات واضحة": قبض · صرف · خصم.
   3. **New:** "قل العملية بصوتك": example under the title, in a card:
      «مع احمد علي ساعتين من 10 الف ومسلم 5 الف». Pills: إدخال صوتي · قراءة بالصوت · مجاني.
   4. "كشوف PDF وواتساب": كشف حساب · سند · تقارير.
   5. "بياناتك محفوظة": بدون إنترنت · نسخ على الهاتف · Google Drive (Latin wrapped in
      `⁨…⁩`).
2. **Texts:**
   - no exclamation marks;
   - no emojis (the last button "ابدأ استخدام المُسَرِّب ✨" → "ابدأ الآن");
   - Western digits.
3. **Look:**
   - same tokens as the app (`MaterialTheme.mosarib`);
   - page dots, "تخطي" and "التالي" placed for RTL;
   - nothing cut off at 360 dp width and with large font (1.3x);
   - dark theme readable.
4. Keep the sign-in, "تسجيل حساب جديد", "المتابعة مجاناً" and the no-account dialog flows unchanged.
   Only text tone: short and clear, no "!".

## B. Splash (`features/splash/SplashScreen.kt`)
1. Logo, "المُسَرِّب" and the line "إدارة توزيع مياه الآبار والري".
2. Add the version at the bottom in small muted text: "الإصدار {BuildConfig.VERSION_NAME}".
3. Remove "جاري التحميل..." (keep a thin progress indicator if loading takes time).
4. The splash must not add delay: it shows only while the app is really starting. Note in the report
   how long it shows on the phone.
5. Dark theme and light theme.

## Allowed files
`features/onboarding/OnboardingScreen.kt`, `features/splash/SplashScreen.kt`, drawable resources if a
new icon is needed.

## Done when
- Build and tests pass.
- The report has screenshots of every welcome page and the splash, light and dark, at 360 dp.
  - To see the welcome **without wiping the owner's data**, use a debug-only preview (`@Preview` or a
    debug menu entry). Never clear app data on the phone.
