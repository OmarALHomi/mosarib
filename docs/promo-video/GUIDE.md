# دليل عمل الفيديو الترويجي لتطبيق المُسَرِّب

**المدة:** 30 ثانية. **المقاس:** طولي 9:16 (1080×1920)، مناسب لحالات واتساب وتيك توك وريلز.

---

## 1. تجهيز الشاشات (قبل أي شيء)
**لا تستخدم بيانات عملائك الحقيقيين.** أدخل بيانات تجريبية أولاً:

**أ. العملاء** (أضفهم من «عميل جديد»):

| الاسم | النوع |
|---|---|
| أحمد علي | مزارع |
| سالم ناصر | مزارع |
| محمد صالح | مزارع |
| حارث سعيد | مزارع |
| يوسف قاسم | صاحب بئر |

**ب. العمليات** (أدخلها بالصوت، فهذا يختبر الميزة أيضاً):
1. «مع أحمد علي ساعتين ونص من 10 الف ومسلم 5 الف»
2. «مع سالم ناصر ثلاث ساعات من 10 الف»
3. «20 الف من حارث سعيد»
4. «15 الف ديزل»

**ج. الصور الست المطلوبة.** Claude يلتقطها من الجوال وأنت تفتح كل شاشة، وتُحفظ في `docs/promo-video/screens/`:

| الملف | الشاشة | المشهد |
|---|---|---|
| `01_home.png` | الرئيسية | 1 و6 |
| `02_voice_examples.png` | الإدخال الصوتي قبل التسجيل (الأمثلة) | 2 |
| `04b_session_filled.png` | نافذة السقي معبّأة من الصوت (أعد التقاطها بعد إصلاح المهمة 037) | 3 |
| `05a_customers.png` | قائمة العملاء والأرصدة | 3 |
| `06b_statement_pdf.png` | الكشف PDF نفسه (أعد التقاطه بعد إصلاح 037) | 4 |
| `07b_drive.png` | قسم Google Drive (موّه البريد) | 5 |
| `03_voice_understood.png` | بطاقة «فهمت: سقي · أحمد علي …» بعد التسجيل | 2 |
| `04_session_form.png` | نافذة السقي معبّأة | 3 |
| `05_customer_card.png` | تفاصيل أحمد علي (الرصيد والعمليات) | 3 |
| `06_statement.png` | كشف حساب PDF أو شاشة المشاركة | 4 |
| `07_backup.png` | صفحة النسخ الاحتياطي و«آخر نسخة إلى Drive» | 5 |

**نصيحة:** استخدم الوضع الداكن، فألوانه أجمل في الفيديو. واشحن الجوال حتى تظهر البطارية ممتلئة.

---

## 2. الأدوات (اختر واحدة)

| الأداة | الأفضل في | تقبل صورة؟ | مجانية؟ |
|---|---|---|---|
| **Google Veo** (عبر Gemini أو Flow) | مشاهد واقعية بالناس والحقول | نعم | تجربة محدودة |
| **Kling AI** | تحريك صورة الشاشة بنعومة | نعم | رصيد يومي مجاني |
| **Runway Gen-3/4** | تحكم دقيق بالحركة | نعم | رصيد تجريبي |
| **CapCut** (للجمع) | جمع المشاهد والنصوص العربية والموسيقى | — | مجاني |

**الطريقة المقترحة:**
- مشاهد الناس والحقول (1، 2أ، 4، 5): **تُولَّد بالذكاء الاصطناعي** من البرومبت فقط.
- مشاهد الشاشات (2ب، 3): **تُحرَّك من صورك الحقيقية**، فهي أصدق وأوضح.
- **النصوص العربية:** تُكتب في CapCut وليس في البرومبت، لأن أدوات التوليد تكتب العربي خطأ غالباً.

---

## 3. المشاهد بالتفصيل

> البرومبتات بالإنجليزية لأن الأدوات تفهمها أدق. النص العربي (الكابشن) تضيفه في CapCut.

### المشهد 1 (0–5 ث): الافتتاح
**الهدف:** الإحساس بالمكان والقيمة.
**يولَّد بالبرومبت (بدون صورة):**
```
Cinematic aerial drone shot at golden hour over green terraced farms in a Yemeni mountain valley.
A small stone well with a diesel water pump, clear water gushing into narrow irrigation channels
between the crops. Slow forward camera movement, warm sunlight, light haze, realistic, 4K,
vertical 9:16, no text.
```
**الكابشن في CapCut:** «كل قطرة محسوبة… وكل حقٍّ محفوظ»

### المشهد 2أ (5–8 ث): صاحب البئر يتكلم
**يولَّد بالبرومبت:**
```
Medium close-up of a Yemeni well owner in his 40s wearing a traditional white thobe, a jambiya
belt and a light shawl, standing beside a water pump in a green field. He holds an Android phone
near his mouth, pressing a button with his thumb, speaking naturally and confidently. Golden
hour light, shallow depth of field, realistic, vertical 9:16, no text on screen.
```

### المشهد 2ب (8–11 ث): الكلام يتحول لعملية
**الصور:** `02_voice_examples.png`، ثم `03_voice_understood.png`.
**في Kling أو Runway (Image to Video) لكل صورة:**
```
Smooth slow zoom-in on this phone app screen, subtle light glow, the interface stays sharp and
readable, no distortion of text, no new elements, 3 seconds.
```
**الكابشن:** «قل العملية بصوتك… والتطبيق يكتب ويحسب»
**صوت المشهد:** سجّل صوتك وأنت تقول الجملة: «مع أحمد علي ساعتين ونص من عشرة ألف ومسلم خمسة ألف».

### المشهد 3 (11–17 ث): قوة التطبيق
**الصور:** `04_session_form.png`، ثم `05_customer_card.png`.
**البرومبت لكل صورة:**
```
Gentle vertical pan across this app screen from top to bottom, clean modern UI, crisp text,
soft depth shadow around the phone frame, 3 seconds, no distortion.
```
**الكابشن:** «سقي بالدقيقة · سندات قبض وصرف · أرصدة واضحة»

### المشهد 4 (17–22 ث): الكشف عبر واتساب
**يولَّد بالبرومبت:**
```
Two Yemeni men in a green farm at sunset: the well owner shows his phone screen to a farmer in
traditional clothing; the farmer receives a message on his own phone, reads it, smiles, nods and
they shake hands warmly. Realistic, friendly mood, shallow depth of field, vertical 9:16, no text.
```
ثم ضع لقطة سريعة من `06_statement.png` (ثانية واحدة).
**الكابشن:** «كشف حساب واضح عبر واتساب… بلا خلاف»

### المشهد 5 (22–26 ث): بدون إنترنت
**يولَّد بالبرومبت:**
```
Close-up of an Android phone resting on a stone wall next to a flowing irrigation channel in a
remote valley, the status bar shows no signal. The screen glows softly. Camera slowly pulls back
to reveal the vast green valley. Realistic, golden light, vertical 9:16, no readable text.
```
ثم لقطة من `07_backup.png`.
**الكابشن:** «يعمل بدون إنترنت · نسخ احتياطي تلقائي على Google Drive»

### المشهد 6 (26–30 ث): الختام
**في CapCut مباشرة (لا يحتاج توليد):**
- **الخلفية:** لون #0f5f58 أو `01_home.png` مموّهة.
- **في المنتصف:** شعار التطبيق (قطرة بيضاء على مربع فيروزي)، ثم «المُسَرِّب»، ثم «إدارة توزيع مياه الآبار والري».
- **في الأسفل:** «حمّله مجاناً: mosarib.tubbasoft.com».

---

## 4. التجميع في CapCut (خطوة بخطوة)
1. **مشروع جديد:** المقاس 9:16.
2. **ترتيب اللقطات:** رتّب المشاهد 1 إلى 6 حسب التوقيتات أعلاه.
3. **الانتقالات:** «Dissolve» قصير (0.3 ث) بين المشاهد، بدون انتقالات مبالغ فيها.
4. **النصوص:**
   - الخط Cairo Bold أو Tajawal Bold، والحجم كبير وواضح.
   - اللون أبيض مع ظل خفيف، والكلمة المهمة بالذهبي #f5c542.
   - المكان في الثلث السفلي، ولا تغطِّ وجه الشخص أو الشاشة.
5. **الموسيقى:**
   - من مكتبة CapCut ابحث عن «oud» أو «arabic acoustic»، هادئة مع إيقاع خفيف.
   - صوت الموسيقى 30% عند التعليق الصوتي.
6. **التعليق الصوتي (اختياري):** سجّله بصوتك أو بصوت شخص بلهجة يمنية:
   > «المُسَرِّب… كل قطرة محسوبة. قل العملية بصوتك، والتطبيق يكتب ويحسب.
   > سقي بالدقيقة، سندات، وكشوف واتساب واضحة. يشتغل بدون إنترنت، وبياناتك محفوظة.
   > حمّله مجاناً من موقع المُسَرِّب.»
7. **الترجمة:** أضف ترجمة تلقائية للتعليق، وراجع الكلمات العربية.
8. **التصدير:** 1080p، 30fps.

---

## 5. نسخ إضافية من نفس المواد
- **15 ثانية** لحالات واتساب: المشاهد 2، ثم 3، ثم 6.
- **صورة ثابتة** للمنشورات: استخدم `web_dashboard/assets/images/og_share.png`.
- **فيديو أفقي 16:9** لليوتيوب: نفس المشاهد بإعادة القص.

## 6. قبل النشر (تحقق)
- [ ] لا يظهر أي اسم أو مبلغ لعميل حقيقي.
- [ ] الأرقام في الشاشات إنجليزية (0-9) والتاريخ صحيح.
- [ ] رابط الموقع مكتوب صحيحاً: mosarib.tubbasoft.com
- [ ] لا يوجد نص عربي مشوّه ولّده الذكاء الاصطناعي داخل اللقطات.
- [ ] الموسيقى من مكتبة مرخّصة (مكتبة CapCut).
