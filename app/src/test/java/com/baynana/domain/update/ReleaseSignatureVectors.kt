package com.baynana.domain.update

/**
 * متجهات ذهبية لقناة الإصدار (ح٢٠) — **مولَّدة آليًّا، لا تُحرَّر بيد**.
 *
 * المصدر: `node tools/release_manifest.mjs --write` (والتحقق منها: `--check` في CI).
 * والمفتاح هنا **مفتاح اختبار معلن** وظيفته توليد متجهات ثابتة؛ لا يوقّع أي ملفّ إصدار حقيقي.
 * والغاية: أن يُوقَّع الرمز بأداة المالك (Node) ويُتحقق منه في التطبيق (Kotlin) على البايتات
 * نفسها — فالرحلة كاملة مفحوصة، لا «يعمل عندي».
 */
object ReleaseSignatureVectors {

    const val TEST_PUBLIC_KEY_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWXCWecdzuBjRIoC5cPppDuhDBJNXnrx9NcSbxzUG0A/+Db/jlKK2H+mXyMEwyQdp+Ewn/lVcHW5Vct+hYZUWog=="

    /** إصدار اختياري: أحدث من ٣، ومدعوم للجميع. */
    const val OPTIONAL_TOKEN =
        "BNR1.MXw0fDEuMnwxfGh0dHBzOi8vcmVsZWFzZXMuYmF5bmFuYS5leGFtcGxlL2JheW5hbmEtMS4yLmFwa3wwZjhjMWQyZTNiNGE1OTY4Nzc2NjU1NDQzMzIyMTEwMGFhYmJjY2RkZWVmZjAwMTEyMjMzNDQ1NTY2Nzc4ODk5fDE3NjcyMjU2MDAwMDB82KXYtdmE2KfYrSDYrdiz2KfYqCDCq9in2YTYqNin2YLZisK7INi52YbYryDYp9mE2LPYr9in2K8g2KfZhNis2LLYptmK2Iwg2YjYqtit2LPZitmGINiz2LHYudipINin2YTZg9i02YjZgQ.MEUCIGyddsZlsR0Qptqt7wp69zpZ7XImzC47aIQTR8l9K2DJAiEAvyHYfyAvkbHjkPT06xbd1RhOFZwgT62HSYiQy-fh3A0"

    const val OPTIONAL_PAYLOAD =
        "1|4|1.2|1|https://releases.baynana.example/baynana-1.2.apk|0f8c1d2e3b4a59687766554433221100aabbccddeeff00112233445566778899|1767225600000|إصلاح حساب «الباقي» عند السداد الجزئي، وتحسين سرعة الكشوف"

    /** إصدار إجباري: من هو على ٤ أو أقل يلزمه التحديث. */
    const val REQUIRED_TOKEN =
        "BNR1.MXw3fDIuMC4xfDV8aHR0cHM6Ly9yZWxlYXNlcy5iYXluYW5hLmV4YW1wbGUvYmF5bmFuYS0xLjIuYXBrfDBmOGMxZDJlM2I0YTU5Njg3NzY2NTU0NDMzMjIxMTAwYWFiYmNjZGRlZWZmMDAxMTIyMzM0NDU1NjY3Nzg4OTl8MTc2NzIyNTYwMDAwMHzYqti62YrZitixINmB2Yog2LXZiti62Kkg2KfZhNmF2LLYp9mF2YbYqTog2KfZhNmG2LPYriDYp9mE2KPZgtiv2YUg2YXZhiDZpSDYqtiq2YjZgtmBINi52YYg2KfZhNmF2LTYp9ix2YPYqSDYrdiq2Ykg2KfZhNiq2K3Yr9mK2Ks.MEYCIQCblOVTKEJZ7JYIlDdZpefhAlYCipcvCsmnvYIXPT41ugIhAOfWuLJgchMkf-G0RNMXSAr5Y0Lm8j_KFvV4Gxt_IBSM"

    /** نفس الحمولة بحرف مقلوب في ترميزها: التوقيع يجب أن يسقط. */
    const val TAMPERED_PAYLOAD_TOKEN =
        "BNR1.AXw0fDEuMnwxfGh0dHBzOi8vcmVsZWFzZXMuYmF5bmFuYS5leGFtcGxlL2JheW5hbmEtMS4yLmFwa3wwZjhjMWQyZTNiNGE1OTY4Nzc2NjU1NDQzMzIyMTEwMGFhYmJjY2RkZWVmZjAwMTEyMjMzNDQ1NTY2Nzc4ODk5fDE3NjcyMjU2MDAwMDB82KXYtdmE2KfYrSDYrdiz2KfYqCDCq9in2YTYqNin2YLZisK7INi52YbYryDYp9mE2LPYr9in2K8g2KfZhNis2LLYptmK2Iwg2YjYqtit2LPZitmGINiz2LHYudipINin2YTZg9i02YjZgQ.MEUCIGyddsZlsR0Qptqt7wp69zpZ7XImzC47aIQTR8l9K2DJAiEAvyHYfyAvkbHjkPT06xbd1RhOFZwgT62HSYiQy-fh3A0"

    /** الحمولة الصحيحة بتوقيع مقلوب: يجب أن يسقط. */
    const val CORRUPT_SIGNATURE_TOKEN =
        "BNR1.MXw0fDEuMnwxfGh0dHBzOi8vcmVsZWFzZXMuYmF5bmFuYS5leGFtcGxlL2JheW5hbmEtMS4yLmFwa3wwZjhjMWQyZTNiNGE1OTY4Nzc2NjU1NDQzMzIyMTEwMGFhYmJjY2RkZWVmZjAwMTEyMjMzNDQ1NTY2Nzc4ODk5fDE3NjcyMjU2MDAwMDB82KXYtdmE2KfYrSDYrdiz2KfYqCDCq9in2YTYqNin2YLZisK7INi52YbYryDYp9mE2LPYr9in2K8g2KfZhNis2LLYptmK2Iwg2YjYqtit2LPZitmGINiz2LHYudipINin2YTZg9i02YjZgQ.AEUCIGyddsZlsR0Qptqt7wp69zpZ7XImzC47aIQTR8l9K2DJAiEAvyHYfyAvkbHjkPT06xbd1RhOFZwgT62HSYiQy-fh3A0"

    /** ملفّ بلا علامة «بيننا»: لا يُقبل ولو كان JSON سليمًا. */
    const val PLAIN_JSON =
        "{\"versionCode\":99,\"versionName\":\"9.9\",\"apkUrl\":\"https://attacker.example/app.apk\"}"
}
