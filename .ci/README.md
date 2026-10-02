# أدوات CI المؤقتة

## استخراج مخططات Room

مخططات Room تُصدَّر تلقائيًا إلى `app/schemas/` مع كل بناء (KSP + `room.schemaLocation`).
عند الحاجة إلى استخراج المخطط من CI (لأن تنزيل artifacts محجوب في بعض البيئات):

1. أنشئ ملفًا فارغًا `.ci/dump-schemas`.
2. ادفع، ثم اقرأ تعليقات (annotations) الدفعة عبر:
   `gh api repos/<owner>/<repo>/check-runs/<id>/annotations`
3. المخطط مضغوط بـ gzip ثم مرمّز base64 في تعليق باسم `SCHEMA|<path>|1/N`.
4. فكّ الترميز واحفظه في `app/schemas/<package>/<version>.json`، ثم احذف ملف العلامة.

القيود التي تحكم التصميم: تعليق GitHub يُقتطع عند 4096 حرفًا، وحد التعليقات عشرة لكل خطوة.
لذلك يُضغط الملف قبل ترميزه.
