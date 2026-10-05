package com.baynana.domain.sync

/**
 * متجهات ذهبية لعقد المزامنة `v1` (ح٢٢) — **مولَّدة آليًّا من الخادم المرجعي، لا تُحرَّر بيد**.
 *
 * المصدر: `node tools/sync_contract_test.mjs --write`، والتحقق منها في CI بالأمر نفسه بلا
 * `--write`. فالمتجهات ليست وصفًا للعقد بل **ردّ الخادم الحقيقي**، واختبار Kotlin يفكّها بالدوال
 * التي يستعملها التطبيق في الإنتاج.
 */
object SyncWireVectors {

    /** الطلب كما يبنيه التطبيق (نفس الحقول ونفس الترتيب). */
    const val PUSH_REQUEST = "{\"deviceId\":\"device-a\",\"items\":[{\"operationId\":\"op-entry-1\",\"entityType\":\"entry\",\"entityId\":\"entry-1\",\"action\":\"UPSERT\",\"payload\":\"{\\\"id\\\":\\\"entry-1\\\",\\\"roomId\\\":\\\"room-water-1\\\",\\\"operationId\\\":\\\"op-entry-1\\\",\\\"type\\\":\\\"WATER_SESSION\\\",\\\"owedByMemberId\\\":\\\"counterpart-room-water-1\\\",\\\"owedToMemberId\\\":\\\"me\\\",\\\"amountMinor\\\":\\\"1500000\\\",\\\"currency\\\":\\\"YER_NEW\\\",\\\"occurredAt\\\":1767225596000}\",\"createdAt\":1767225596000}]}"

    /** ردّ الخادم على دفعة فيها: مقبول، ثم نفس العنصر (مكرر)، ثم عنصر بمبلغ رقمي. */
    const val PUSH_RESPONSE = "{\"outcomes\":[{\"operationId\":\"op-entry-1\",\"status\":\"ACCEPTED\",\"retryable\":false,\"reason\":\"\"},{\"operationId\":\"op-entry-1\",\"status\":\"ACCEPTED\",\"retryable\":false,\"reason\":\"مقبول سابقًا: إعادة الإرسال لا تُنشئ أثرًا ثانيًا\"},{\"operationId\":\"op-bad-money\",\"status\":\"REJECTED\",\"retryable\":false,\"reason\":\"المبلغ «amountMinor» يجب أن يكون نصًّا بالوحدة الصغرى (ADR-04)، لا رقمًا\"}]}"

    /** صفحة سحب محدودة (٢ من ٣): تُثبت أن "hasMore" صريح وأن المؤشر يعمل. */
    const val PULL_PAGE_LIMITED = "{\"changes\":[{\"kind\":\"UPSERT\",\"entityType\":\"entry\",\"entityId\":\"entry-1\",\"operationId\":\"op-entry-1\",\"payload\":\"{\\\"id\\\":\\\"entry-1\\\",\\\"roomId\\\":\\\"room-water-1\\\",\\\"operationId\\\":\\\"op-entry-1\\\",\\\"type\\\":\\\"WATER_SESSION\\\",\\\"owedByMemberId\\\":\\\"counterpart-room-water-1\\\",\\\"owedToMemberId\\\":\\\"me\\\",\\\"amountMinor\\\":\\\"1500000\\\",\\\"currency\\\":\\\"YER_NEW\\\",\\\"occurredAt\\\":1767225596000}\",\"serverTime\":1767225600000},{\"kind\":\"UPSERT\",\"entityType\":\"entry\",\"entityId\":\"entry-2\",\"operationId\":\"op-entry-2\",\"payload\":\"{\\\"id\\\":\\\"entry-2\\\",\\\"amountMinor\\\":\\\"250000\\\"}\",\"serverTime\":1767225600000}],\"nextCursor\":\"c:Mg\",\"hasMore\":true}"

    /** صفحة سحب بعد المؤشر: ما تبقّى. */
    const val PULL_PAGE_AFTER_CURSOR = "{\"changes\":[{\"kind\":\"UPSERT\",\"entityType\":\"entry\",\"entityId\":\"entry-3\",\"operationId\":\"op-entry-3\",\"payload\":\"{\\\"id\\\":\\\"entry-3\\\",\\\"amountMinor\\\":\\\"300000\\\"}\",\"serverTime\":1767225600000}],\"nextCursor\":\"c:Mw\",\"hasMore\":false}"

    /** صفحة فارغة: الخادم لا يعرف المؤشر (مؤشر قديم أو من خادم آخر). */
    const val PULL_EMPTY = "{\"changes\":[],\"nextCursor\":\"c:MA\",\"hasMore\":false}"

    /** ردّ مشوّه: التطبيق لا يُعلن نجاحًا ولا يُفسد شيئًا — كل العناصر تُعاد لاحقًا. */
    const val PUSH_RESPONSE_GARBAGE = "<html>502 Bad Gateway</html>"

    /** ردّ ناقص النتائج: عنصر واحد بينما الطلب عنصران ⇒ الثاني لا يُعلن نجاحه. */
    const val PUSH_RESPONSE_SHORT = "{\"outcomes\":[{\"operationId\":\"op-entry-1\",\"status\":\"ACCEPTED\",\"retryable\":false,\"reason\":\"\"}]}"
}
