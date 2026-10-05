#!/usr/bin/env node
/**
 * قناة الإصدار (ح٢٠) — المنطق المشترك: بناء الحمولة، والتوقيع، والتحقق، والقراءة.
 *
 * لِمَ هذا الملفّ بالذات؟ لأن أخطر عيب في قناة التحديث لا يظهر في اختبار محلي: أن يكون الملفّ
 * **صحيحًا عند المالك ومرفوضًا في التطبيق** (أو العكس) بسبب فرق في ترتيب الحقول أو الترميز.
 * فيُكتب المنطق مرة واحدة نصًّا مقروءًا، تُستدعيه أداة التوقيع، ويُولَّد به «متجه ذهبي» تُثبّته
 * اختبارات Kotlin. وبهذا يُختبر الطريق كله: توقيع Node ← تحقق Kotlin.
 *
 * **مفتاح التوقيع هنا مفتاح اختبار معلن**، مهمّته الوحيدة توليد متجهات ثابتة يُعاد إنتاجها
 * بلا مفاتيح ولا أسرار. أما مفتاح المالك الحقيقي فلا يدخل المستودع أبدًا، ويُقرأ من ملفّ خارجي
 * في `tools/release_sign.mjs`.
 */
import crypto from "node:crypto";

export const RELEASE_PREFIX = "BNR1";
export const PAYLOAD_VERSION = "1";
export const FIELD_SEPARATOR = "|";

/** مفتاح اختبار: يوقّع متجهات الاختبار وحدها. لا يوقّع أي ملفّ إصدار حقيقي. */
export const VECTOR_TEST_PRIVATE_KEY_BASE64 =
  "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgczemMemgf71E7/k/8rbCjESAzSIWc7QH69mFxHi3y9ShRANCAARZcJZ5x3O4GNEigLlw+mkO6EMEk1eevH01xJvHNQbQD/4Nv+OUorYf6ZfIwTDJB2n4TCf+VVwdblVy36FhlRai";

export const VECTOR_TEST_PUBLIC_KEY_BASE64 =
  "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWXCWecdzuBjRIoC5cPppDuhDBJNXnrx9NcSbxzUG0A/+Db/jlKK2H+mXyMEwyQdp+Ewn/lVcHW5Vct+hYZUWog==";

export function base64UrlFromBytes(bytes) {
  return Buffer.from(bytes).toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function bytesFromBase64Url(text) {
  const normalized = String(text).replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  return Buffer.from(padded, "base64");
}

/** الحمولة بالنصّ نفسه الذي يقرأه التطبيق، بنفس ترتيب الحقول. */
export function buildPayload(release) {
  return [
    PAYLOAD_VERSION,
    String(release.versionCode),
    String(release.versionName),
    String(release.minSupportedVersionCode),
    String(release.apkUrl),
    String(release.apkSha256).toLowerCase(),
    String(release.publishedAt),
    String(release.messageArabic),
  ].join(FIELD_SEPARATOR);
}

/** توقيع DER كما يتوقّعه `Signature.getInstance("SHA256withECDSA")` في التطبيق. */
export function signPayload(payload, privateKeyBase64 = VECTOR_TEST_PRIVATE_KEY_BASE64) {
  const key = crypto.createPrivateKey({
    key: Buffer.from(privateKeyBase64, "base64"),
    format: "der",
    type: "pkcs8",
  });
  const signature = crypto.sign("sha256", Buffer.from(payload, "utf8"), { key, dsaEncoding: "der" });
  return base64UrlFromBytes(signature);
}

export function verifyPayload(payload, signatureBase64Url, publicKeyBase64 = VECTOR_TEST_PUBLIC_KEY_BASE64) {
  const key = crypto.createPublicKey({
    key: Buffer.from(publicKeyBase64, "base64"),
    format: "der",
    type: "spki",
  });
  return crypto.verify(
    "sha256",
    Buffer.from(payload, "utf8"),
    { key, dsaEncoding: "der" },
    bytesFromBase64Url(signatureBase64Url)
  );
}

/** يبني الرمز الكامل: `BNR1.<الحمولة>.<التوقيع>`. */
export function buildToken(release, privateKeyBase64 = VECTOR_TEST_PRIVATE_KEY_BASE64) {
  const payload = buildPayload(release);
  const signature = signPayload(payload, privateKeyBase64);
  return { payload, signature, token: `${RELEASE_PREFIX}.${base64UrlFromBytes(Buffer.from(payload, "utf8"))}.${signature}` };
}

/** إصدار مثال يُستعمل في المتجهات: كل حقول الحقول مضبوطة لتغطية الحالات. */
export const EXAMPLE_RELEASE = {
  versionCode: 4,
  versionName: "1.2",
  minSupportedVersionCode: 1,
  apkUrl: "https://releases.baynana.example/baynana-1.2.apk",
  apkSha256: "0f8c1d2e3b4a59687766554433221100aabbccddeeff00112233445566778899",
  publishedAt: 1_767_225_600_000,
  messageArabic: "إصلاح حساب «الباقي» عند السداد الجزئي، وتحسين سرعة الكشوف",
};

/** نسخة إجبارية: من هو على ١ أو ٢ يلزمه التحديث، ومن هو على ٣ يبقى مخيَّرًا. */
export const REQUIRED_RELEASE = {
  ...EXAMPLE_RELEASE,
  versionCode: 7,
  versionName: "2.0.1",
  minSupportedVersionCode: 5,
  messageArabic: "تغيير في صيغة المزامنة: النسخ الأقدم من ٥ تتوقف عن المشاركة حتى التحديث",
};
