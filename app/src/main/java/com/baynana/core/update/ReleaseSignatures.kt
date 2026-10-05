package com.baynana.core.update

import android.content.Context
import com.baynana.core.license.EcdsaSignatureVerifier
import com.baynana.core.license.LicenseSignatures

/**
 * التحقق من ملفّ الإصدار بالمفتاح العام (ح٢٠).
 *
 * **مفتاح التحديث غير مفتاح التصاريح، عمدًا:** مفتاح التصاريح يُستعمل في لوحة المتصفح عند إصدار
 * رمز لمستخدم، فيقترب من يد كل من يستعمل اللوحة. أما مفتاح التحديث فهو الذي يحمي الأجهزة كلها
 * من «تحديث» مزيّف، فلا يُسلَّم إلا لمن يبني الإصدار. فصلُ المفتاحين يجعل تسرّب أحدهما لا يفتح
 * الباب الآخر.
 *
 * المفتاح العام يُقرأ من الأصل `release_public_key.txt` (X.509/SPKI بصيغة PEM أو base64)، والملفّ
 * في المستودع **قالب بلا مفتاح**: فإن لم يستبدله المالك بمفتاحه بقيت ملفّات الإصدار مرفوضة برسالة
 * صريحة — وهذا أصدق من قبول ملفّ بلا تحقق ثم الظنّ أن القناة محميّة.
 */
object ReleaseSignatures {

    const val PUBLIC_KEY_ASSET = "release_public_key.txt"

    /** أدوات قراءة المفتاح العام نفسها المستعملة في التصاريح: تنظيف واحد لا اثنان. */
    fun publicKeyBase64(context: Context): String? = readAsset(context, PUBLIC_KEY_ASSET)
        ?.let { LicenseSignatures.extractKeyMaterial(it) }

    fun readAsset(context: Context, name: String): String? = runCatching {
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    /** الفاحص، أو `null` إن لم يُسلَّم مفتاح بعد (فنقول «لا مفتاح» لا «توقيع مزوّر»). */
    fun verifier(context: Context): EcdsaSignatureVerifier? {
        val material = publicKeyBase64(context) ?: return null
        return EcdsaSignatureVerifier(material).takeIf { it.isUsable }
    }

    /**
     * فاحص جاهز لتمريره إلى `ReleaseManifest.read`: يعيد `null` إن لم يكن هناك مفتاح، فلا
     * يظنّ المستدعي أن الرفض بسبب توقيع مزيّف.
     */
    fun check(context: Context): ((ByteArray, ByteArray) -> Boolean)? =
        verifier(context)?.let { verifier -> { payload, signature -> verifier.verify(payload, signature) } }
}
