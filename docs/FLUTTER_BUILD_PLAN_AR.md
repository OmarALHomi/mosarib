# خطة بناء «بيننا» بـFlutter — وثيقة تسليم لوكيل تنفيذ

> **اقرأ هذا أولًا (للوكيل):** هذه ليست اقتراحًا تقنية، بل **عقد تنفيذ**. كل قاعدة في §2 ليست ذوقًا بل
> نتيجة عطل حقيقي وقع في النسخة الأصلية (Kotlin/Android) ووُثِّق. لا تُغيّر قاعدة لأنها «صعبة»، ولا
> تعِد بميزة لم تُختبر. وإن انسدّ عليك أمر، **توقّف واكتب السؤال** بدل أن تخترع حلًّا يخالف عقدًا.
>
> **البوابة الكبرى:** لا يُقال «مكتمل» لأن الشيفرة تُترجم. يُقال ذلك حين يمرّ اختبار آلي يثبته.

**TL;DR (machine summary):** Build `baynana_flutter` — an Arabic-first, RTL, offline-first
ledger app for Yemeni water/credit groups. Pure-Dart domain layer (no I/O, no Flutter imports),
drift (SQLite) persistence, Riverpod + go_router UI. Money is **integer minor units** (`YEN_NEW`
= 3 decimals), never `double`. Golden vectors from the existing repo
(`tools/migrate.mjs --write-vectors`, `tools/sync_contract_test.mjs --write`) must be matched
byte-for-byte by Dart tests. No analytics, no silent update, no silent install, no destructive
migration. Ship order: domain engines → persistence → sync contract client → UI per design system →
parity gates.

---

## 0) كيف تُنفَّذ هذه الوثيقة (قواعد العمل للوكيل)

1. **مرحلة واحدة في كل مرة**، من §6 بالترتيب. لا تبدأ F(n+1) قبل أن يمرّ CI مرحلة F(n).
2. **اختبار لكل قاعدة**: أي عبارة في §2 تُترجم إلى اختبار Dart. لا قاعدة بلا اختبار.
3. **لا تعيد التصميم**: نظام التصميم (نيلة وذهب، ١٦sp، تباين ≥4.5:1، اتصال/اتجاه RTL) من
   `docs/DESIGN_OVERHAUL_PLAN_AR.md` — انسخه كما هو، ولا تخترع ألوانًا أو مصطلحات.
4. **اللغة**: كل نصّ يراه المستخدم **عربية فصحى مبسّطة** بلا مفردات قديمة (ممنوع: «مسرب»، «جربة»،
   «دورة ري»، «عميل» بمعنى الطرف في الدفتر — استعمل «طرف/غرفة/دين/قبض»).
5. **لا بيانات خارج الجهاز** إلا عبر قناة المزامنة التي يضبطها المالك، وبقراره.
6. **كل التزام (commit) يعمل**: `flutter analyze` نظيف، `flutter test` أخضر، والتنسيق
   `dart format` بلا تغيير.
7. **التوثيق جزء من المخرج**: كل مرحلة تُنتهي بتحديث `docs/STATUS_AR.md` بسطر واحد صادق.

---

## 1) الهدف من المنتج (سطر واحد لكل طرف)

- **المالك/المسرِّب (موزّع الماء):** دفتر سقيات وسدادات لا يضيع مع انقطاع الشبكة، ويُشارَك مع الفلاحين
  بجرد مطابق.
- **الفلاح:** يرى ما عليه ولمن، ويُقرّ الحركات أو يعترض عليها بسبب مكتوب.
- **الدلال/التاجر:** صلح وأقساط وسعاية، وسوق عروض **بلا كشف أرقام ديون ولا هواتف**.

**خارج النطاق (ممنوع بناؤه):** تسعير، إعلانات (ولو «مدفوعة»)، محفظة تحفظ رصيدًا، رسم على المدين،
بيع شارة الثقة، تتبّع تحليلات، أي ميزة «اجتماعية» تكشف مبالغ طرف لطرف آخر.

---

## 2) القواعد غير القابلة للتفاوض (كل واحدة = اختبار)

| # | القاعدة | الاختبار الذي يثبتها |
|---|---|---|
| C1 | **المال عدد صحيح بالوحدة الصغرى** (الفلس)، `YER_NEW` = ٣ خانات عشرية، ولا `double`/`float` في أي مسار نقد | اختبار يحلّل كل ملفّ نقد ويفشل لو ظهر `double`/`Float`/`NumberFormat`؛ وقراءة `MAX_MINOR` |
| C2 | **عرض صفري الكسور**: المبلغ يُعرض `#,##0` مع اسم العملة، والمصدر دالّة واحدة `MoneyFormat` | اختبار ذهبي بمبالغ حدّية + منع تنسيق أرقام يدوي في الواجهة |
| C3 | **حفظ محلي أولًا**: القيد يُكتب في قاعدة الجهاز قبل أي شبكة، ويظهر لصاحبه فورًا | اختبار: بلا شبكة ⇒ القيد موجود محليًّا وحالته «بانتظار الإرسال» |
| C4 | **إعادة الإرسال لا تُضعِف ولا تُضاعِف**: منع ازدواج بـ`operationId` (فريد في القاعدة والخادم) | اختبار: إرسال الدفعة مرّتين ⇒ قيد واحد |
| C5 | **الإقرار يغيّر الحالة فقط**: لا حذف ولا تجميد، والاعتراض/طلب التعديل يبقى مع سببه | اختبار: بعد الاعتراض يبقى القيد والمبلغ، وتتغيّر الحالة والسبب |
| C6 | **لا حذف قيد شارك فيه طرف ثانٍ**: الإلغاء بقيد عكسي، والتاريخ الأصلي لا يتغيّر | اختبار: حذف/تعديل مبلغ قيد مُقرّ ⇒ مرفوض، والإلغاء يُنشئ قيدًا عكسيًا |
| C7 | **القبض العام لا يُغلق دينًا معيّنًا صامتًا**: «الأقدم أولًا» داخل الغرفة/العملة بعد تأكيد ظاهر | اختبار تخصيص: ترتيب الأقدم، وإظهار المعاينة قبل الحفظ، والزائد رصيد دائن |
| C8 | **لا مقاصّة ولا تحويل بين عملتين** | اختبار: دين بالريال الجديد وقبض بالقديم ⇒ يُرفضان معًا لا يُحوَّلان |
| C9 | **لا ترحيل تخريبي**: الفشل أولى من المسح، ولا يُسمّى نقل ناقص «كاملًا» | اختبار: ملفّ ترحيل بحدث ناقص ⇒ يُرفض ويُسمّى المفقود؛ والقديم لا يُمَس |
| C10 | **عدم كشف**: لا هاتف فلاح في أي عرض سوق، ولا مبالغ ديون، ولا معرّف جهاز في تقرير الصحّة | اختبار نفي على نصوص الواجهة والمشاركة |
| C11 | **لا تحديث صامت**: الفحص زرّ يفتحه الإنسان، والتثبيت عبر شاشة النظام | حاجز يمنع أي خدمة فحص خلفية وأي واجهة تثبيت صامت |
| C12 | **لا تتبّع**: صفر مكتبات تحليلات/أعطال، والمقاييس تُحسب على الجهاز وتُشارَك بقرار المالك | حاجز على `pubspec.yaml` + حاجز يمنع شبكة في طبقة القياس |
| C13 | **بوابات المزامنة P0**: لا نشر ميزة مشتركة قبل إغلاق بوابات الأمان (deny-by-default) |
| C14 | **العنوان من أصل بنائي**: عنوان المزامنة/الإصدار يُقرأ من `assets/*.txt` ولا يُحرَّر من الشاشة | اختبار: قيمة غير `https` أو مضيف خاطئ ⇒ لا قناة |

---

## 3) المعمارية (طبقات وصلاحيات الاستيراد)

```
lib/
  domain/        # Dart نقيّ: بلا imports من flutter/drift/http. منطق الحساب والحالات.
  data/          # drift (SQLite) + مستودعات + محرّكات القراءة/الكتابة + صندوق الصادر.
  core/          # المال، التنسيق، الأمان، الشبكة، الإعدادات، القناة.
  features/      # الشاشات (Widgets) + متحكّمات Riverpod. لا حسابات هنا.
  ui/            # نظام التصميم: ألوان، Typography، مكوّنات مشتركة.
assets/          # sync_endpoint.txt، release_endpoint.txt، release_public_key.txt (نماذج)
test/            # اختبارات + متجهات ذهبية (JSON)
```

**قواعد الاستيراد (تُفرض بحاجز آلي):**
- `domain/**` لا يستورد `package:flutter` ولا `drift` ولا `http` ولا `dart:io`.
- `features/**` لا يستورد `drift` مباشرة (يمرّ من مستودعات `data` أو مزوّدات `core`).
- لا `double` في `domain/ledger/**`، `domain/settlement/**`، `domain/market/**`.

**لماذا طبقة نقيّة؟** لأن كل البوابات الذهبية في النسخة الأصلية تُختبر بلا أندرويد ولا قاعدة بيانات،
وهذا وحده ما جعل اكتشاف الأعطال ممكنًا (خطأ الجرد الناقص، والقيود الملغاة في التصدير، إلخ).

---

## 4) الحزم المقترحة (وقرار لكل واحدة)

| الحاجة | الحزمة | لماذا هي | بديل مرفوض ولماذا |
|---|---|---|---|
| قاعدة بيانات | `drift` + `sqlite3_flutter_libs` | أنواع آمنة، ترحيل مخطط مكتوب، واستعلامات مُختبرة | `sqflite` بلا طبقة أنواع: يسهّل أخطاء الأعمدة |
| إدارة الحالة | `flutter_riverpod` | اختبار سهل، فصل عن الواجهة | `setState` وحده: لا يكفي لشاشات تعتمد على قاعدة حيّة |
| التنقّل | `go_router` | مسارات صريحة + حالة الشلّ (٤ تبويبات + المزيد) | تنقّل يدوي بمدقّقات: يتفرّق مع ١٠ شاشات |
| نماذج غير قابلة للتغيير | `freezed` + `json_serializable` | `copyWith`/`==` مجّانًا ومنع تعديل بالخطأ | فئات يدوية: أخطاء صمتية في المقارنات |
| تشفير/توقيع | `cryptography` (ECDSA P-256) أو `pointycastle` | تحقّق توقيع ملفّ الإصدار بمفتاح المالك | `basic_utils`: سطح أوسع من الحاجة |
| تخزين أسرار | `flutter_secure_storage` | رمز الجهاز لا يُخزَّن نصًّا في prefs | `shared_preferences`: نصّ مكشوف |
| مستندات PDF | `pdf` + `bidi` + خط عربي مرفق | كشف عربي صحيح الاتجاه بلا خادم | `printing`: يضيف طبقة لا نحتاجها |
| أرقام/تواريخ | `intl` (بلا `NumberFormat` للمال) | تواريخ عربية وأرقام، والمال من `MoneyFormat` لدينا | `NumberFormat.currency`: يخالف C1/C2 |
| مهام خلفية | `workmanager` | «حاول إرسال ما في الصندوق» عند توفّر شبكة | مؤقّت داخلي: يُوقف مع النظام |
| مشاركة ملفّات | `share_plus` + `file_picker` | مشاركة ملفّ الترحيل/التسليم، واختيار ملفّ للاستيراد | `path_provider` وحده: لا يكفي للتبادل |
| حالة الشبكة | `connectivity_plus` | لإظهار «بلا شبكة» بصدق، لا لمنع المحاولة | الاعتماد على فشل الطلب وحده: تجربة أسوأ |
| حماية الشاشة | `screen_protector` (أو مكافئ) | إخفاء الكشف عند تصوير/خلفية في شاشات القفل | لا شيء: يكشف الدفتر في معاينة التطبيقات |
| بصمة/قفل | `local_auth` | «لا فتح بلا تحقّق» (ح١٨) | PIN داخلي وحده: يقبل التخمين |

> **قاعدة الحزم:** كل حزمة تُضاف تُسجَّل في `docs/DEPENDENCIES_AR.md` بسبب واحد وبديل مرفوض. والحزم
> التي تلمس الشبكة/التحليلات تُرفض آليًّا في الحاجز (C12).

---

## 5) مصدر الحقيقة المشترك مع النسخة الأصلية (أهم قسم للوكيل)

هذه أهم نقطة: **لا يُعاد اختراع القواعد**. في مستودع النسخة الأصلية `OmarALHomi/mosarib` توجد أدوات
تُولّد **متجهات ذهبية** محايدة اللغة. مطلوب منك:

1. انسخ من المستودع الأصلي إلى `tools/` (بلا تعديل منطق):
   - `tools/migrate.mjs` (جرد الترحيل، الفروق، الفحص الذاتي، `--write-vectors`).
   - `tools/sync_contract_test.mjs` (عقد المزامنة، `--write`).
   - `server/baynana-sync-server.mjs` (الخادم المرجعي للاختبار).
2. ولّد المتجهات:
   ```bash
   node tools/migrate.mjs --self-test          # يجب أن يطبع ✅
   node tools/migrate.mjs --write-vectors      # يكتب متجهات (Kotlin) — عدّلها لتكتب Dart/JSON
   node tools/sync_contract_test.mjs --write
   ```
3. **عدّل المُولِّد لا المتجهات**: أضف خِيار `--format=dart` (أو اكتب ملفّات `test/vectors/*.json`
   ثم تُقرأ في Dart). ممنوع تعديل ملفّ متجهات بيدك، وممنوع «تعديل التوقّع» ليمرّ الاختبار.
4. اكتب اختبارات Dart تقارن **حرفيًّا** (byte-for-byte):
   - جرد الدفتر: نفس الأعداد/المجاميع/صافي الأعضاء ونفس ترتيب الكتابة.
   - نصوص الفروق العربية: نفس الجمل بالحرف (مثال: `غرفة room-water-1: قيود ناقصة عند الوجهة: op-payment-1`).
   - فكّ عقد المزامنة: نفس ردود الخادم (٢٠٠/٤٠١/٤١٣/٤٢٢/٤٠٩) ونفس الرسائل.
   - ملفّ الترحيل: JSONL بترتيب حتمي (`createdAt` ثم `operationId`)، وترويسة تحمل الجرد.
5. **توافق الاستيراد مع الماضي**: يجب أن يستورد الحقل الجديد **ملفّ الترحيل**
   (`BAYNANA-MIGRATION-1`) و**النسخة الاحتياطية القديمة** (JSON إن وُجد في
   `app/src/main/java/com/baynana/core/util/BackupManager.kt` بالنسخة الأصلية). اختبار: ملفّ حقيقي من
   النسخة القديمة يُستورد ويطابق الجرد.

**أمر البوابة قبل كل دفع:**
```bash
node tools/migrate.mjs --self-test && node tools/sync_contract_test.mjs --check && bash tools/guards.sh && flutter test
```

---

## 6) المراحل (كل مرحلة = PR مستقلّ بمعايير قبول)

### F0 — الهيكل والانضباط (نصف يوم)
- `flutter create --org com.baynana --project-name baynana_flutter .` ثم إعداد: `analysis_options.yaml`
  (صرامة عالية: `strict-casts`, `strict-raw-types`, `prefer_final_locals`)، `dart format`،
  `.github/workflows/flutter-ci.yml` (analyze + format check + test + تشغيل أدوات Node).
- **معايير القبول:** CI أخضر على مشروع فارغ، وفي `tools/guards.sh` حواجز C1/C10/C11/C12 ولو كان
  الفحص فارغًا (الحواجز تُكتب أولًا، لا آخرًا).

### F1 — المال والدفتر (المحرّك النقيّ) — الأهم
- `domain/money/money_format.dart` (عرض صفري الكسور، `minorUnits` لكل عملة، `MAX_MINOR`).
- `domain/ledger/`: أنواع القيود (سقية/دين/سداد/تسوية/عكسية)، الحالات (مسودة، مُرسل، مُقرّ، معترض،
  طلب تعديل، ملغى)، محرّك المشتقات (الرصيد، صافي كل عضو، الرصيد الجاري للكشف).
- **معايير القبول:** متجهات المال والدفتر تمرّ حرفيًّا؛ ولا `double` في `domain/**` (حاجز C1)؛
  اختبارات: إلغاء بقيد عكسي، الإقرار لا يحذف، منع الازدواج بـ`operationId`.

### F2 — القاعدة والاستمرارية (drift)
- مخطط SQLite (غرف، أعضاء، قيود، إقرارات، إسقاطات تخصيص، صندوق صادر، حالة مزامنة، جداول الإرث).
- **مهم:** اسم القاعدة `water_distributor_db` **محفوظ**، ومسار الترقية `AutoMigration` مكتوب صراحة.
- ترحيلات المخطط مُختبرة بـ`drift` test (من كل نسخة سابقة إلى الأحدث).
- **معايير القبول:** كل استعلام مستعمل في `domain` له تغطية؛ واختبار انقطاع: كتابة بلا شبكة ثم إعادة
  تشغيل التطبيق ⇒ القيد موجود (C3).

### F3 — صندوق الصادر والمزامنة (عقد v1)
- `core/sync/sync_wire.dart` (فكّ/ترميز العقد)، `transport.dart` (واجهة)، `http_transport.dart`
  (`https` فقط؛ الأخطاء: 401/403/409/422 ⇒ رفض غير قابل لإعادة المحاولة برسالة عربية، 413/5xx ⇒
  خطأ قابل), `sync_engine.dart` (جولات الإرسال/السحب بالمؤشّر), `sync_worker.dart` (workmanager).
- **معايير القبول:** اختبار يفتح الخادم المرجعي (Node) داخل CI ويقارن الردود بالمتجهات؛ إعادة الإرسال
  لا تُضاعف (C4)؛ لا شبكة ⇒ حالة عربية مفهومة.
- **إضافة إلزامية:** شاشة «رمز الجهاز» (الشاشة التي بلاها لا تُضبط القناة على جهاز حقيقي) — إدخال
  رمز، اختبار اتصال، رسائل عربية، وتخزين آمن.

### F4 — نظام التصميم والشاشات الأساسية
- انسخ من `docs/DESIGN_OVERHAUL_PLAN_AR.md`: لوحة نيلة/ذهب، `BaynanaMark`، شرائح الحالة، بطاقات،
  وحالات خمس لكل شاشة (فارغ/تحميل/بلا شبكة/خطأ بسببه/TalkBack).
- الشاشات: الرئيسية، غرفي، كشف الطرف، إدخال قيد، الإقرار (٣ لمسات)، القبض والتخصيص، الحركات، المزيد.
- **معايير القبول:** اختبار تباين آلي لكل أزواج الألوان (≥4.5:1)؛ ٣٢٠dp و٢٠٠٪ تكبير بلا قصّ؛ RTL كامل.

### F5 — التقارير والـPDF
- `domain/statement/statement_document.dart` (مستند واحد)، `StatementSheet` ترسمه، `pdf_writer` يكتبه.
- **معايير القبول:** بوابة ذهبية بعشرة سيناريوهات: **نصّ الشاشة = نصّ PDF حرفيًّا**، وخط عربي مرفق.

### F6 — الصلح والسوق (محرّك ثم شاشة)
- `domain/settlement/deal_engine.dart`: مجموع الأقساط = المتبقي دائمًا، لا بيع مزدوج، إلغاء بخطة.
- `domain/market/market_engine.dart` + `listing_privacy.dart`: لا هاتف فلاح بنيويًّا، ولا مبالغ ديون.
- **معايير القبول:** اختبارات النفي (C10) تمرّ؛ و«السعاية» بيان مستقل عن دَين المحصول.

### F7 — التسليم بلا إنترنت + الترحيل بجرد مطابق
- ملفّ/رمز قابل للمشاركة بلا خادم، ولا فتح غرفة إلا بقبول صريح، وإعادة الإرسال لا تخصم مرّتين.
- الترحيل: تصدير JSONL، استيراد عبر عقد المزامنة نفسه، ثم **مقارنة جرد** وإظهار كل فرق بالعربية،
  وفحص اتّساق الملفّ مع ترويسته (ملفّ مُعدَّل يُرفض بوسم صريح).
- **معايير القبول:** «صفر فرق» أو قائمة فروق ورمز خروج فاشل؛ وترحيل دفتر قديم بلا تواريخ مختلقة.

### F8 — قناة التحديث والتوقيع
- قارئ ملفّ الإصدار الموقّع (ECDSA P-256)، `min-supported`، زرّ تحديث، تنزيل بتحقّق sha256، وفتح
  شاشة النظام للتثبيت. العنوان من `assets/release_endpoint.txt` (https فقط).
- **معايير القبول:** ملفّ غير موقّع أو موقّع بمفتاح آخر ⇒ مرفوض برسالة؛ لا فحص خلفي (C11).

### F9 — الإطلاق المراقب
- تقرير صحّة محليّ بستّ بوابات معلنة (G1..G6) وثلاث حالات (سليمة/ساقطة/لم تُقس)، بلا شبكة وبلا تتبّع.
- **معايير القبول:** بوابة «المجهول ليس سليمًا» مُختبرة؛ ولا مكتبة تتبّع في `pubspec.yaml`.

---

## 7) خريطة الترحيل: من Kotlin إلى Dart (أسماء الملفّات المقابلة)

| النسخة الأصلية (Kotlin) | المقابل في Flutter | ملاحظة |
|---|---|---|
| `domain/ledger/LedgerSnapshot.kt` | `domain/ledger/ledger_snapshot.dart` | نفس قواعد الاستثناء (ملغى/عكسي/مسودة) |
| `domain/ledger/LedgerRequests.kt` | `domain/ledger/ledger_requests.dart` | `ReceiptSpec` و`AllocationMode` نفس الأسماء |
| `data/local/ledger/LedgerDao.kt` | `data/local/ledger_dao.dart` | drift، مع `insertEntryIfNew` كمنع ازدواج |
| `data/local/sync/SyncEngine.kt` + `TransportPort.kt` | `core/sync/sync_engine.dart` + `transport.dart` | نفس ترتيب الجولات ونفس ٢٠٠ تكرار |
| `domain/migration/LedgerMigration.kt` | `domain/migration/ledger_migration.dart` | نفس نصّ الجرد ونفس نصوص الفروق العربية |
| `domain/observe/HealthReport.kt` | `domain/observe/health_report.dart` | نفس البوابات G1..G6 ونفس حدود `Limits` |
| `domain/update/*` | `domain/update/*` | نفس `UpdateDecision` و«الحد الأدنى المدعوم» |
| `domain/settlement/DealEngine.kt` | `domain/settlement/deal_engine.dart` | نفس شروط الرفض ونصوص البيان |
| `domain/market/*` | `domain/market/*` | نفس خصوصية العرض |
| `features/**` (Compose) | `features/**` (Widgets) | أعد الرسم بنفس المحتوى لا بنفس الشيفرة |

**قاعدة التسمية:** أبقِ أسماء المفاهيم كما هي (operationId، entryId، roomId، syncCursor) — لأن العقد
مع الخادم والأدوات يقرأ هذه الأسماء.

---

## 8) CI والحواجز (لا تكتمل مرحلة بلا حواجزها)

`.github/workflows/flutter-ci.yml`:
```yaml
jobs:
  verify:
    steps:
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with: { channel: stable }
      - run: dart format --output=none --set-exit-if-changed .
      - run: flutter analyze --fatal-infos
      - run: node tools/migrate.mjs --self-test
      - run: node tools/sync_contract_test.mjs --check
      - run: bash tools/guards.sh
      - run: flutter test --coverage
```

`tools/guards.sh` يجب أن يمنع (بأسماء ملفّات ومخرجات صريحة):
1. `double`/`Float`/`NumberFormat`/`DecimalFormat` في مسارات النقد وملفّات الواجهة.
2. مكتبات تتبّع/تحليلات في `pubspec.yaml`.
3. أي `http://` في الشيفرة أو الأصول (C14).
4. استيراد `flutter`/`drift` في `lib/domain/**`.
5. مفردات قديمة («مسرب»، «جربة»، «دورة ري») في أي نصّ ظاهر.
6. `PackageInstaller`/تثبيت صامت/فحص تحديث خارج شاشة التحديث (C11).
7. خدمات تحليل أعطال (`firebase_crashlytics`, `sentry_flutter`, …).
8. أصول مفاتيح/أسرار متتبَّعة (`*.jks`, `key.b64`, `*.pem` خاصة).

**قاعدة الحواجز:** الحاجز يُكتب **قبل** الشيفرة التي يحرسها، ويُذكر في رسالة الفشل سببُه لا وصفُه.

---

## 9) الأمان والخصوصية (ملخّص تنفيذي)

- **لا حساب ولا سيرفر للبدء:** التطبيق يعمل كاملًا محليًّا؛ والقناة اختيارية.
- **الرمز** يُخزَّن في تخزين آمن ولا يُطبع في سجلّات؛ و**لا واجهة تعرضه**.
- **العنوان** من الأصل البنائي، و`https` فقط (`usesCleartextTraffic` مكافئ: منع أي نصّ مكشوف).
- **القفل** (ح١٨): بصمة/رمز نظام، وإخفاء المحتوى عند الخلفية، ولا فتح بلا تحقّق.
- **النسخ الاحتياطي:** تصدير صريح فقط، ولا «نسخة كاملة» لنقل ناقص، ولا مطالبة بحذف الأصل.
- **التوقيع:** مفتاح الإصدار خارج المستودع، وملفّ الإصدار يُرفض إن لم يُطابق المفتاح المحفوظ.

---

## 10) قائمة الممنوعات (للوكيل — مخالفتها تُغلق المرحلة)

1. لا تعدّل ملفّات المتجهات الذهبية يدويًّا، ولا تُخفِّض اختبارًا ليَمرّ.
2. لا تُضِف `double` لأي مبلغ، ولا تحوّل مبلغًا عبر `toDouble()` ولو «مؤقّتًا».
3. لا تحديث صامت، ولا تنزيل خلفي، ولا تثبيت بلا شاشة النظام.
4. لا تتبّع، ولا تحليلات، ولا رفع أعطال تلقائي.
5. لا حذف قيد؛ الإلغاء بقيد عكسي. ولا تعديل تاريخ أصل.
6. لا «تمّ» في واجهة بلا دليل (رقم/جرد/حالة حقيقية).
7. لا أسرار ولا مفاتيح في المستودع.
8. لا تغيير في هوية الاسم أو `applicationId` بلا قرار مالك مكتوب.
9. لا تعطيل حاجز لأنه «يعطّل العمل» — أصلح السبب.
10. لا دمج مرحلة قبل مرور CI، ولا «سأصلح لاحقًا».

---

## 11) معايير القبول النهائية (Definition of Done)

- [ ] كل قواعد §2 لها اختبار يثبتها، وكلها خضراء في CI.
- [ ] متجهات المال/الدفتر/العقد/الترحيل تمرّ **حرفيًّا** مع النسخة الأصلية.
- [ ] استيراد ملفّ ترحيل حقيقي من النسخة القديمة يُنتج «صفر فرق في الجرد».
- [ ] الشاشات العشر بحالاتها الخمس، وRTL، و٣٢٠dp، و٢٠٠٪، وتقرير تباين آلي.
- [ ] اختبار انقطاع شبكة كامل: قيد يُكتب بلا شبكة، يظهر محليًّا، ثم يُرسل عند عودة الشبكة مرّة واحدة.
- [ ] بوابة تحديث: ملفّ موقّع بمفتاح المالك يُقبل، وغيره يُرفض، ولا فحص خلفي.
- [ ] لا مكتبة تتبّع، ولا سرّ في المستودع، ولا نصّ مكشوف.
- [ ] `docs/STATUS_AR.md` يحمل سطرًا صادقًا لكل مرحلة (ما نُفِّذ وما لم يُنفَّذ).

---

## 12) أوامر البدء (نسخ ولصق)

```bash
# ١) مشروع جديد في مستودع جديد: baynana_flutter
flutter create --org com.baynana --project-name baynana_flutter --platforms android .
git init && git add -A && git commit -m "F0: هيكل مشروع بيننا بـFlutter"

# ٢) الحزم
flutter pub add flutter_riverpod go_router drift sqlite3_flutter_libs dev:drift_dev dev:build_runner \
  freezed_annotation dev:freezed dev:json_serializable cryptography flutter_secure_storage \
  pdf bidi intl workmanager share_plus file_picker connectivity_plus local_auth screen_protector

# ٣) الأدوات: انسخ من المستودع الأصلي
mkdir -p tools server test/vectors
#   tools/migrate.mjs، tools/sync_contract_test.mjs، server/baynana-sync-server.mjs
node tools/migrate.mjs --self-test
node tools/sync_contract_test.mjs --check

# ٤) البوابة
dart format . && flutter analyze --fatal-infos && flutter test
```

---

## 13) خريطة الملفّات المقترحة (هدف نهاية F4)

```
lib/
  main.dart
  app.dart                      # MaterialApp + RTL + الثيم (فاتح/ليلي)
  core/money/money_format.dart
  core/sync/{sync_config,sync_wire,transport,http_transport,sync_engine,sync_worker}.dart
  core/update/{release_manifest,release_channel,release_verifier,release_repository}.dart
  core/security/{app_lock,secure_store,screen_guard}.dart
  data/local/{database,ledger_dao,sync_dao,migration_daos,adapters}.dart
  data/repositories/{ledger_repository,receipt_repository,sync_status_repository,
                     ledger_migration_repository,health_repository}.dart
  domain/ledger/{types,statuses,ledger_snapshot,ledger_requests,effective_entries}.dart
  domain/statement/statement_document.dart
  domain/migration/ledger_migration.dart
  domain/observe/health_report.dart
  domain/settlement/deal_engine.dart
  domain/market/{market_engine,listing_privacy}.dart
  features/{home,rooms,statements,entries,receipts,movements,deals,market,more,
            sync,migration,update,observe,lock}/…
  ui/components/{baynana_mark,status_chip,ledger_components,money_text,ack_bar,
                 allocation_preview,crisis_banner}.dart
assets/{sync_endpoint.txt,release_endpoint.txt,release_public_key.txt,fonts/}
test/{money,ledger,statement,migration,sync_contract,health,settlement,market,guards}/
docs/{STATUS_AR.md,DEPENDENCIES_AR.md,DESIGN_AR.md}
tools/{migrate.mjs,sync_contract_test.mjs,guards.sh}
```

---

## 14) تقدير الحجم وترتيب الأولوية

| المرحلة | الحجم التقريبي | الأولوية | ملاحظة |
|---|---|---|---|
| F0–F1 | ٣–٤ أيام | **P0** | المال والدفتر: أي خطأ هنا يُفسد الثقة كاملة |
| F2 | ٢–٣ أيام | **P0** | الاستمرارية ومنع الازدواج |
| F3 | ٣–٤ أيام | **P0** | العقد والمزامنة ورمز الجهاز |
| F4 | ٤–٦ أيام | **P0** | الشاشات الأساسية: بها يرى المستخدم المنتج |
| F5 | ٢–٣ أيام | P1 | التقارير |
| F6 | ٣–٤ أيام | P1 | الصلح والسوق (بعد إغلاق بوابات P0) |
| F7 | ٢–٣ أيام | P0 | التسليم والترحيل: بها ينتقل الناس فعلًا |
| F8 | ٢–٣ أيام | P0 | قناة التحديث (بلا توقيع لا توزيع) |
| F9 | ٢ أيام | P1 | الإطلاق المراقب |

**الإجمالي:** نحو ٤–٦ أسابيع لوكيل واحد منظّم، ويكون أول تسليم قابل للاستعمال الحقيقي عند نهاية F4+F7+F8.

---

## 15) أول ثلاث مهام اطلبها من الوكيل (نصّ جاهز)

1. «أنشئ المشروع والانضباط: F0 كما في §6 مع `tools/guards.sh` (الحواجز الثمانية) وCI أخضر على
   مشروع فارغ، وارفع PR.»
2. «نفّذ F1: المال والدفتر بلا `double`، مع اختبارات المتجهات الذهبية المنسوخة من المستودع الأصلي،
   وأثبت أن نصوص الفروق العربية مطابقة بالحرف.»
3. «نفّذ F2: قاعدة drift مع `insertEntryIfNew` ومنع الازدواج، واختبار انقطاع: كتابة بلا شبكة ثم
   إعادة تشغيل ⇒ القيد موجود.»

---

*هذه الوثيقة تُقرأ مع: `docs/DESIGN_OVERHAUL_PLAN_AR.md` (التصميم)، و`docs/BUILD_PLAN_V6_AR.md`
(ترتيب الحزم والبوابات)، و`docs/SYNC_SERVER_AR.md` (عقد الخادم)، و`docs/MIGRATION_AR.md` (الترحيل)،
و`docs/OBSERVE_AR.md` (الإطلاق المراقب).*
