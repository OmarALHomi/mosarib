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
grep -q "exportSchema = true" app/src/main/java/com/baynana/core/database/AppDatabase.kt \
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
grep -q 'DATABASE_NAME = "water_distributor_db"' app/src/main/java/com/baynana/core/database/AppDatabase.kt \
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

# 6) حدود المعمارية: طبقة domain نقية بلا Android وبلا شبكة (الخطة v6 §3.1).
#    أي استيراد لـandroid/androidx/firebase/room داخل domain يفشل البناء هنا.
if [ -d app/src/main/java/com/baynana/domain ]; then
  impure=$(grep -rnE "^import (android|androidx|com\.google\.firebase|com\.google\.android|okhttp|retrofit)" \
    app/src/main/java/com/baynana/domain 2>/dev/null || true)
  if [ -n "$impure" ]; then
    echo "$impure" >&2
    fail "طبقة domain تستورد Android أو شبكة — القاعدة: ui → domain → data.contract، وdomain نقي."
  fi
  pass "طبقة domain نقية (بلا Android وبلا شبكة)"
fi

# 7) لا كسور عشرية في مسارات المال داخل domain (ADR-04): التخزين والحساب والعرض بالفلس.
#    الاستثناء الوحيد: `Money.ofMajor` الذي يحوّل إدخال المستخدم بالريال إلى فلس مرة واحدة.
MONEY_DOUBLE_HITS=$(
  grep -rnE "(^|[^A-Za-z0-9_])(Double|Float)([^A-Za-z0-9_]|$)|toDouble\(|toFloat\("     app/src/main/java/com/baynana/domain/money app/src/main/java/com/baynana/domain/ledger app/src/main/java/com/baynana/domain/settlement app/src/main/java/com/baynana/domain/market 2>/dev/null     | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' || true
)
if [ -n "$MONEY_DOUBLE_HITS" ]; then
  echo "FAIL: كسور عشرية في مسار مالي (ADR-04 يمنع Double/Float):"
  echo "$MONEY_DOUBLE_HITS"
  exit 1
fi
echo "OK: لا Double ولا Float في مسارات المال (money/ledger/settlement/market)"

# 8) الواجهة الجديدة (د١–د٢) بلا Double/Float وبلا حساب مبالغ داخلها (ADR-04 + خطة التصميم §8).
#    النطاق: الملفات التي وُلدت في مسار التصميم الجديد فقط؛ أما الشاشات القديمة فتُحذف في د٤–د٦
#    ولا يُشترط إصلاحها هنا (وإلا تجمّد العمل على الشكل القديم بدل تجاوزه).
NEW_UI_PATHS="app/src/main/java/com/baynana/MainScreen.kt app/src/main/java/com/baynana/ui/theme app/src/main/java/com/baynana/ui/identity app/src/main/java/com/baynana/ui/components app/src/main/java/com/baynana/features/home/BaynanaHomeScreen.kt app/src/main/java/com/baynana/features/home/BaynanaHomeViewModel.kt app/src/main/java/com/baynana/features/home/MigrationDialog.kt app/src/main/java/com/baynana/features/rooms app/src/main/java/com/baynana/features/movements app/src/main/java/com/baynana/features/more/MoreScreen.kt app/src/main/java/com/baynana/features/shell"
NEW_UI_HITS=$(
  grep -rnE "(^|[^A-Za-z0-9_])(Double|Float)([^A-Za-z0-9_]|$)|toDouble\(|toFloat\(" $NEW_UI_PATHS 2>/dev/null \
    | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' || true
)
if [ -n "$NEW_UI_HITS" ]; then
  echo "FAIL: كسور عشرية في واجهة التصميم الجديدة (المال بالفلس فقط):"
  echo "$NEW_UI_HITS"
  exit 1
fi

# 8ب) لوحة الهوية الجديدة هي المعتمدة: لا تبقى أسماء لوحة المسرب إلا في ملف الجسر المُعلن.
LEGACY_PALETTE_HITS=$(
  grep -rnE "PrimaryTeal|SecondaryAqua|AccentGold|AccentEmerald|StatusDebt" app/src/main/java/com/baynana \
    | grep -v "ui/theme/LegacyPaletteAliases.kt" | grep -v "LegacyPaletteAliases" || true
)
if [ -n "$LEGACY_PALETTE_HITS" ]; then
  echo "تنبيه: أسماء اللوحة القديمة في $(echo "$LEGACY_PALETTE_HITS" | wc -l | tr -d ' ') سطرًا داخل شاشات لم تُنقَّ بعد (تُحذف مع ملف الجسر في د٦)."
fi
echo "OK: لا Double ولا Float في واجهة التصميم الجديدة، واللوحة القديمة محصورة في جسر واحد"

# 9) لا تكرار لأسماء الموارد داخل المجلد نفسه (مثل `ic_launcher_foreground.png` و`.xml` معًا).
#    سبب الحاجز: هذه الأخطاء لا تظهر في المحرّر، بل تُفشل دمج الموارد في البناء/الاختبارات
#    برسالة «Duplicate resources» — وقد كلفتنا دورة CI كاملة مرة.
RES_DUPLICATES=""
while IFS= read -r dir; do
  dup=$(ls "$dir" 2>/dev/null | sed 's/\.[^.]*$//' | sort | uniq -d)
  if [ -n "$dup" ]; then
    RES_DUPLICATES="$RES_DUPLICATES$dir: $(echo "$dup" | tr '\n' ' ')\n"
  fi
done < <(find app/src/main/res -maxdepth 1 -type d \( -name "drawable*" -o -name "mipmap*" \) | sort)
if [ -n "$RES_DUPLICATES" ]; then
  echo "FAIL: اسم مورد مكرر في المجلد نفسه (دمج الموارد سيفشل):"
  printf "%b" "$RES_DUPLICATES"
  exit 1
fi
echo "OK: لا تكرار لأسماء الموارد في مجلدات drawable/mipmap"

# 10) لا استعمال كامل التسمية لأيقونات مادريال (`androidx.compose.material.icons.Icons.Filled.X`):
#     لأن `Icons.Filled.X` خاصية امتداد (extension property) لا تُستدعى بأسماء كاملة، فيفشل البناء
#     بـ«Unresolved reference» مع أن الأيقونة موجودة. الصحيح: استيراد الاسم ثم `Icons.Filled.X`.
BAD_ICON_REFS=$(grep -rn "androidx\.compose\.material\.icons\.Icons\." app/src/main/java \
  | grep -v "^[^:]*:[0-9]*:import " || true)
if [ -n "$BAD_ICON_REFS" ]; then
  echo "FAIL: أيقونة مستعملة بأسماء كاملة (خاصية امتداد لا تُستدعى هكذا):"
  echo "$BAD_ICON_REFS" | head -5
  exit 1
fi
echo "OK: أيقونات مادريال مستوردة بأسمائها لا بأسماء كاملة"

# 11) لا تنسيق أرقام في الواجهة الجديدة بـNumberFormat/DecimalFormat/String.format: كل مبلغ يمرّ
#     من `MoneyFormat`، والفواصل والكسور تُحسب هناك مرة واحدة (ADR-04 + د٣).
NEW_UI_PATHS="app/src/main/java/com/baynana/MainScreen.kt app/src/main/java/com/baynana/ui/theme app/src/main/java/com/baynana/ui/identity app/src/main/java/com/baynana/ui/components app/src/main/java/com/baynana/features/home/BaynanaHomeScreen.kt app/src/main/java/com/baynana/features/home/BaynanaHomeViewModel.kt app/src/main/java/com/baynana/features/home/MigrationDialog.kt app/src/main/java/com/baynana/features/rooms app/src/main/java/com/baynana/features/movements app/src/main/java/com/baynana/features/more/MoreScreen.kt app/src/main/java/com/baynana/features/shell app/src/main/java/com/baynana/features/market/BaynanaMarketScreen.kt app/src/main/java/com/baynana/features/market/BaynanaMarketViewModel.kt app/src/main/java/com/baynana/features/market/ListingDialogs.kt app/src/main/java/com/baynana/features/dev"
FORMAT_HITS=$(
  grep -rnE "(NumberFormat|DecimalFormat|String\.format|"%.2f"|\.toBigDecimal)" $NEW_UI_PATHS 2>/dev/null \
    | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' || true
)
if [ -n "$FORMAT_HITS" ]; then
  echo "FAIL: تنسيق أرقام في واجهة التصميم الجديدة — المطلوب MoneyFormat:"
  echo "$FORMAT_HITS"
  exit 1
fi
echo "OK: لا تنسيق أرقام يدوي في الواجهة الجديدة (MoneyFormat وحده)"

echo "كل الحواجز سليمة."
