package com.baynana.core.license

import android.content.Context
import com.baynana.domain.license.Base64Codec
import com.baynana.domain.license.EcdsaSignatureFormat
import com.baynana.domain.license.LicenseSignatureVerifier
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * التحقق من التصاريح بالمفتاح العام (ح١٣).
 *
 * المفتاح العام ليس سرًّا: وجوده في التطبيق هو المقصود. أما مفتاح التوقيع الخاص فلا يدخل التطبيق
 * ولا المستودع أبدًا — يبقى عند المالك (أو في مدير أسرار)، ويُستعمل في لوحة الأدمن عند إصدار
 * تصريح، فتصير المفاتيح **غير قابلة للتوليد** من داخل التطبيق حتى لو قرأ أحد الكود كله.
 *
 * المفتاح العام يُقرأ من ملفّ الأصول `license_public_key.txt` (‏X.509/SPKI بصيغة PEM أو base64
 * مجرّدة)، ويُسلَّم على البناء. الملف الموجود في المستودع **قالب بلا مفتاح**، فإن لم يستبدله المالك
 * بمفتاحه بقيت التصاريح الموقّعة مرفوضة برسالة صريحة، وهذا أصدق من قبول كل شيء بلا تحقق.
 */
object LicenseSignatures {

    const val PUBLIC_KEY_ASSET = "license_public_key.txt"

    /** يقرأ المفتاح العام من الأصول، أو `null` إن لم يوجد مفتاح بعد. */
    fun publicKeyBase64(context: Context): String? {
        val raw = runCatching {
            context.assets.open(PUBLIC_KEY_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return null
        return extractKeyMaterial(raw)
    }

    /** ينقّي نصّ المفتاح: يحذف أسطر PEM والتعليقات (`#`) وأي فراغ، ويُبقي على base64 وحده. */
    fun extractKeyMaterial(raw: String): String? {
        val material = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("#") }
            .filterNot { it.startsWith("-----") }
            .joinToString("")
        if (material.length < 40) return null
        if (Base64Codec.decode(material) == null) return null
        return material
    }

    fun verifier(context: Context): LicenseSignatureVerifier? {
        val material = publicKeyBase64(context) ?: return null
        return EcdsaSignatureVerifier(material)
    }
}

/**
 * يحقق توقيع ECDSA P-256 بتوقيع DER، ويقبل كذلك توقيع WebCrypto المستطيل (P1363) فيحوّله أولًا.
 * لهذا التحويل وجود: لوحة الأدمن توقّع في المتصفح، و`Signature` في جافا لا يعرف تلك الصيغة.
 */
class EcdsaSignatureVerifier(publicKeyBase64: String) : LicenseSignatureVerifier {

    private val publicKey: PublicKey? = runCatching {
        val encoded = Base64Codec.decode(publicKeyBase64) ?: return@runCatching null
        val spec = X509EncodedKeySpec(encoded)
        KeyFactory.getInstance("EC").generatePublic(spec)
    }.getOrNull()

    /** هل المفتاح العام صالح أصلًا؟ (نسخة بلا مفتاح صالح لا تعطي «توقيع مزوّر» بل «لا مفتاح».) */
    val isUsable: Boolean get() = publicKey != null

    override fun verify(payloadBytes: ByteArray, signatureBytes: ByteArray): Boolean {
        val key = publicKey ?: return false
        val der = if (EcdsaSignatureFormat.isRawP1363(signatureBytes)) {
            runCatching { EcdsaSignatureFormat.rawToDer(signatureBytes) }.getOrNull() ?: return false
        } else {
            signatureBytes
        }
        return runCatching {
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(key)
            verifier.update(payloadBytes)
            verifier.verify(der)
        }.getOrDefault(false)
    }
}
