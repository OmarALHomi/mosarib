# نظام العمل بين Claude و Antigravity

| الدور | من | ماذا يفعل |
|---|---|---|
| مخطِّط ومشرف | Claude Code | يكتب المهام في `.ai/tasks/`، يراجع الكود، يشغّل البناء والاختبارات، يقبل أو يطلب تعديلات |
| منفِّذ | Antigravity | ينفّذ المهمة، يبني ويختبر، يكتب تقريره في `.ai/reports/` |
| صاحب القرار | أنت | تبدأ كل دورة، وتقرر الـ commit والنشر |

## الدورة الواحدة
1. **أنت ← Claude:** "خطط لـ ..." ← يكتب Claude ملف مهمة `.ai/tasks/NNN-name.md` بحالة `ready`.
2. **أنت ← Antigravity:** "نفّذ المهمة التالية من .ai/tasks حسب AGENTS.md".
3. **Antigravity:** ينفّذ، يبني، يختبر، يكتب `.ai/reports/NNN.md`، ويغيّر حالة المهمة إلى `done`.
4. **أنت ← Claude:** "راجع المهمة NNN".
5. **Claude:** يقرأ التقرير والـ diff ويشغّل البناء والاختبارات، ثم إما:
   - يغيّر الحالة إلى `accepted` ويقترح الـ commit، أو
   - يغيّرها إلى `changes-requested` ويكتب الملاحظات أسفل ملف المهمة، فتعود للخطوة 2.

## حالات المهمة (السطر `status:` في أعلى الملف)
`ready` ← `in-progress` ← `done` ← `accepted` أو `changes-requested`

## Rules for the implementing agent (English, binding)
- Work on exactly one task at a time; set `status: in-progress` when you start.
- Touch only the files listed under "Allowed files". If you believe another file must change,
  stop and explain it in the report instead of changing it.
- Do not change behaviour that the task does not ask for. No drive-by refactors.
- Keep the security model intact: never write license, device, block or config data from the
  app to Firebase; never add secrets to the app or to the web dashboard JavaScript.
- Database schema changes need a Room migration (bump the version in `AppDatabase.kt`).
  Never use destructive migrations.
- Match the existing code style; remove unused imports in files you produce.
- Build and test (commands in `AGENTS.md`) before writing the report. A task whose build
  fails is not done.
- Never commit, push, branch, or run `git checkout/reset/stash` unless the task says so.
- If something in the task is ambiguous, choose the safest option and say so in the report.
