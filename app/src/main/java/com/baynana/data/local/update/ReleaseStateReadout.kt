package com.baynana.data.local.update

import android.content.Context
import com.baynana.core.database.AppDatabase

/**
 * قراءة **حالة الإصدار المعروفة** بلا شبكة وبلا فحص (ح٢٤) — تُستعمل في تقرير الصحّة.
 *
 * ولماذا في هذه الطبقة بالذات؟ لأن حاجز ١٩ يمنع بناء مستودع الإصدار خارج شاشة التحديث وطبقة
 * البيانات، ومقصده منع **فحص من الخلفية**. وهذه الدالة لا تفحص: تقرأ ما هو محفوظ أصلًا وتُحكِم
 * بالمنطق نفسه (`ReleaseRepository.cachedState` ⇒ `UpdateDecision`)، فلا يوجد مصدر ثانٍ للحقيقة
 * ولا منطق مكرَّر قد ينحرف عن الشاشة التي يراها المستخدم.
 */
suspend fun readReleaseState(
    context: Context,
    db: AppDatabase,
    versionCode: Int? = null,
    versionName: String? = null
): ReleaseRepository.CheckResult {
    val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    @Suppress("DEPRECATION")
    val code = versionCode ?: info?.versionCode ?: 0
    val name = versionName ?: info?.versionName ?: ""
    return ReleaseRepository(context, db, code, name).cachedState()
}
