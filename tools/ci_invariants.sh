#!/usr/bin/env bash
# حواجز بناء ثابتة تمنع انزلاق قرارات موثّقة (الخطة v4 §14).
# تُنفَّذ في CI وقبل الإصدار يدويًا: ./tools/ci_invariants.sh
set -euo pipefail

cd "$(dirname "$0")/.."

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

pass() {
  echo "OK: $1"
}

# 1) لا مسار ترحيل مدمّر في كود التطبيق (التعليقات مستثناة من الفحص).
destructive_hits=$(grep -rn "fallbackToDestructiveMigration" app/src/main 2>/dev/null \
  | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' || true)
if [ -n "$destructive_hits" ]; then
  echo "$destructive_hits" >&2
  fail "يوجد fallbackToDestructiveMigration في كود app/src/main — ممنوع، يفقد بيانات المستخدم."
fi
pass "لا يوجد ترحيل مدمّر في كود app/src/main"

# 2) مخطط Room مُصدَّر (شرط اختبار الترحيلات).
grep -q "exportSchema = true" app/src/main/java/com/example/core/database/AppDatabase.kt \
  || fail "exportSchema ليس true في AppDatabase.kt — لا يمكن اختبار الترحيلات."
grep -q 'arg("room.schemaLocation"' app/build.gradle.kts \
  || fail "room.schemaLocation غير مضبوط في app/build.gradle.kts."
pass "تصدير مخطط Room مفعّل"

# 3) نسخة الجمهور لا تُوقّع بمفتاح التصحيح أبدًا.
grep -q "RELEASE-SIGNING-GUARD" app/build.gradle.kts \
  || fail "حاجز التوقيع RELEASE-SIGNING-GUARD مفقود من app/build.gradle.kts."
grep -q 'signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null' app/build.gradle.kts \
  || fail "نسخة release قد تعود إلى مفتاح التصحيح. يجب أن تكون signingConfig مساوية لمفتاح الإصدار أو null."
pass "لا توقيع بمفتاح التصحيح في نسخة release"

# 4) الهوية الثابتة: معرّف الحزمة واسم قاعدة البيانات (قرارات موثّقة).
#    قرار المالك 2026-10-03: الاسم «بيننا»، والمعرّف com.baynana.app، ولم يُثبَّت التطبيق
#    لأي مستخدم من قبل. بعد أول توزيع حقيقي لا يُغيَّر هذا السطر إطلاقًا.
grep -q 'applicationId = "com.baynana.app"' app/build.gradle.kts \
  || fail "applicationId تغيّر — كسر مسار التحديث. راجع قرار الهوية قبل التعديل."
grep -q 'DATABASE_NAME = "water_distributor_db"' app/src/main/java/com/example/core/database/AppDatabase.kt \
  || fail "اسم قاعدة البيانات تغيّر — يفقد المستخدمون دفاترهم."
pass "معرّف الحزمة واسم قاعدة البيانات ثابتان"

# 4أ) إعدادات Firebase تطابق معرّف الحزمة، وإلا يفشل البناء أو يعمل SDK على تطبيق غير مسجّل.
if [ -f app/google-services.json ]; then
  config_package=$(grep -o '"package_name"[[:space:]]*:[[:space:]]*"[^"]*"' app/google-services.json \
    | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
  gradle_package=$(grep -o 'applicationId = "[^"]*"' app/build.gradle.kts | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
  if [ "$config_package" != "$gradle_package" ]; then
    fail "google-services.json يشير إلى الحزمة '$config_package' بينما applicationId هو '$gradle_package'. راجع docs/FIREBASE_SETUP_AR.md."
  fi
  pass "google-services.json يطابق معرّف الحزمة"
fi

# 4ب) الاسم الظاهر ثابت: «بيننا» وتحته «مستودع حساباتك ومعاملاتك».
grep -q '<string name="app_name">بيننا</string>' app/src/main/res/values/strings.xml \
  || fail "الاسم الظاهر ليس «بيننا» في strings.xml."
grep -q '<string name="app_subtitle">مستودع حساباتك ومعاملاتك</string>' app/src/main/res/values/strings.xml \
  || fail "العنوان تحت الاسم ليس «مستودع حساباتك ومعاملاتك» في strings.xml."
pass "الاسم الظاهر والعنوان معتمدان كما قرر المالك"

# 5) لا مفاتيح أو أسرار داخل المستودع.
if git ls-files | grep -E "(^|/)(keystore\.properties|debug\.keystore|.*\.jks|.*\.keystore|local\.properties)$" >/dev/null 2>&1; then
  fail "ملف مفاتيح/إعدادات محلي متتبع في المستودع."
fi
pass "لا ملفات مفاتيح متتبعة"

echo "كل الحواجز سليمة."
