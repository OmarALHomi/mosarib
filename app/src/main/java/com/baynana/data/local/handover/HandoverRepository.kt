package com.baynana.data.local.handover

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.sync.RoomSyncStore
import com.baynana.domain.handover.BundleParse
import com.baynana.domain.handover.HandoverBundle
import com.baynana.domain.handover.HandoverBundleCodec
import com.baynana.domain.handover.HandoverItem
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.sync.ApplyOutcome
import com.baynana.domain.sync.RemoteChange
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.util.UUID

/**
 * التسليم بلا خادم (ح١٩): تجهيز حزمة من صندوق الصادر، واستيراد حزمة وصلت، وسجلّ ما جرى.
 *
 * **قواعد التصميم:**
 * - **لا يحتاج إنترنت ولا حسابًا ولا جهازًا ثانيًا في نفس اللحظة**: الحزمة نصّ يُشارَك عبر واتساب
 *   أو أي تطبيق، وتُقرأ لاحقًا على الجهاز الآخر.
 * - **الاستيراد لا يمنح صلاحية**: كل عنصر يمرّ من نفس مسار التغيير القادم من الشبكة
 *   ([RoomSyncStore.apply])، فيخضع لحجر القبر ومنع الازدواج وقواعد القيد والإقرار نفسها.
 * - **لا تُنشأ غرفة تلقائيًا**: دعوة الغرفة تُخزَّن «بانتظار قرار»، ولا تُفتح إلا بقبول بشري — كود
 *   الربط وحده لا يكشف كشفًا ولا دينًا.
 * - **الحزمة نفسها لا تُستورد مرتين**: فهرس فريد على (bundleId, direction) و`IGNORE` هي الحاجز،
 *   ونتيجة المحاولة الثانية تُقال للمستخدم صراحةً بدل تطبيق صامت.
 * - **الإرسال لا يُعتبر تسليمًا**: عناصر الصادر تبقى غير مُرسلة حتى يعود الإقرار؛ ولهذا لا نعلّم
 *   شيئًا «SENT» عند تجهيز الحزمة. الشاشة تقول «بانتظار إقرار الطرف» لا «تم».
 * - **لا يضيع شيء لأن الدعوة تأخّرت**: عنصر وصل قبل قبول غرفته يُحفظ في `pending_items` ويُطبَّق
 *   تلقائيًا لحظة القبول. فملفّ واحد يكفي للرحلة كاملة، وترتيب واتساب لا يقرّر مصير قيد.
 */
class HandoverRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    /**
     * وسم الجهاز في ترويسة الحزمة. يُمرَّر من الواجهة من `LicenseManager.getDeviceCode`، ولا يُستعمل
     * لأي صلاحية: هو إعلان يساعد الطرفين على تمييز الأجهزة في السجلّ فقط.
     */
    private val deviceCodeLabel: () -> String = { "" }
) {

    private val dao get() = db.handoverDao()
    private val ledger get() = db.ledgerDao()

    /** حزمة جاهزة للمشاركة، ومعها نصّها ورمزها المضغوط إن دخل في حدّ المحادثة. */
    data class Outgoing(
        val bundle: HandoverBundle,
        val text: String,
        val compactCode: String?,
        val fileName: String,
        val isInvitation: Boolean
    ) {
        val itemCount: Int get() = bundle.items.size

        val summaryArabic: String
            get() = if (isInvitation) {
                "دعوة غرفة: أرسلها للطرف ليقبل الربط. لا تُفتح غرفة ولا يُرى قيد قبل قبوله."
            } else {
                "حزمة فيها ${bundle.items.size} حركة من ${bundle.rooms.size} غرفة، بانتظار وصولها وإقرار الطرف."
            }
    }

    /** معاينة قبل الاستيراد: ما في الملف بالضبط، وبأي حال سيُستورد. */
    sealed interface Preview {
        data class Refused(val messageArabic: String) : Preview

        data class Ready(
            val bundle: HandoverBundle,
            val entries: Int,
            val acks: Int,
            val roomItems: Int,
            val unknown: Int,
            val alreadyImported: Boolean,
            val newInvitations: Int,
            val roomsExisting: Int,
            /** عناصر سليمة لكن غرفتها غير موجودة عندك بعد: تنتظر قبول الدعوة ولا تُرمى. */
            val waitingForRoom: Int = 0
        ) : Preview {
            val summaryArabic: String
                get() = buildString {
                    if (alreadyImported) append("هذه الحزمة سبق أن استُوردت على هذا الجهاز. ")
                    append("قيود: $entries")
                    if (acks > 0) append(" • إقرارات: $acks")
                    if (roomItems > 0) append(" • غرف: $roomItems")
                    if (unknown > 0) append(" • غير مفهوم: $unknown")
                    if (newInvitations > 0) append(" • دعوات جديدة: $newInvitations")
                    if (waitingForRoom > 0) append(" • بانتظار قبول غرفة: $waitingForRoom")
                    if (newInvitations > 0 && waitingForRoom > 0) {
                        append(" — اقبل الدعوة فيُطبَّق الباقي تلقائيًا")
                    }
                }
        }
    }

    /** نتيجة استيراد فعلي. لا تُجمَّل: كل رقم يعني ما يقول. */
    data class ImportReport(
        val bundleId: String,
        val alreadyImported: Boolean,
        val applied: Int,
        val duplicates: Int,
        val rejected: Int,
        val invited: Int,
        val ignoredTrailingText: Boolean,
        val notes: List<String>,
        /** عناصر محفوظة بانتظار قبول دعوة غرفتها (تُطبَّق لحظة القبول). */
        val deferred: Int = 0
    ) {
        val summaryArabic: String
            get() = when {
                alreadyImported -> "هذه الحزمة وصلت سابقًا: لم يُطبَّق منها شيء ثانٍ (منع الازدواج)."
                applied == 0 && duplicates == 0 && rejected == 0 && invited == 0 ->
                    "لم يدخل جديد: كل ما في الحزمة موجود عندك."
                else -> buildString {
                    append("دخل: $applied")
                    if (duplicates > 0) append(" • مكرر: $duplicates")
                    if (rejected > 0) append(" • مرفوض: $rejected")
                    if (invited > 0) append(" • دعوات بانتظار قبولك: $invited")
                    if (deferred > 0) append(" • محفوظ حتى تقبل الغرفة: $deferred")
                }
            }
    }

    // ------------------------------------------------------------------ تجهيز

    /**
     * تجهيز حزمة من كل ما لم يُقرّ بعد في صندوق الصادر (منتظر، فاشل، وميت أيضًا: التسليم اليدوي
     * طريق مشروع لما استعصى على الآلة). و[roomId] يحدّ الحزمة بغرفة واحدة، فالحزمة الصغيرة
     * تُقرأ وتُراجع بالعين.
     */
    suspend fun prepareOutgoing(roomId: String? = null, limit: Int = 120): Outgoing {
        val now = clock()
        val rows = ledger.pendingForHandover(limit)
        // لقطة الغرفة تُبنى أولًا وتُوضع في رأس الحزمة: الطرف الآخر يحصل على الغرفة **قبل** قيودها،
        // وإلا وصل القيد إلى غرفة لا يعرفها فيُحفظ مؤجّلًا حتى يقبل الدعوة.
        val roomItems = mutableListOf<HandoverItem>()
        val items = mutableListOf<HandoverItem>()
        val rooms = linkedSetOf<String>()

        rows.forEach { row ->
            val payloadRoom = roomIdOfPayload(row.payload)
            if (roomId != null && payloadRoom != roomId) return@forEach
            if (payloadRoom != null) rooms += payloadRoom
            items += HandoverItem(
                operationId = row.operationId,
                entityType = row.entityType,
                entityId = row.entityId,
                action = row.action,
                payload = row.payload,
                createdAt = row.createdAt
            )
        }

        // لقطة حالة كل غرفة معنيّة: بها يعرف الجهاز الآخر أن طرفه قبل الربط (PENDING → ACTIVE)،
        // أو أن الغرفة أُغلقت بالتراضي (→ CLOSED). تُبنى بنفس معرّفات الأعضاء، فلا يضيع نسب قيد.
        val roomIds = if (roomId != null) listOf(roomId) else rooms.toList()
        roomIds.forEach { id ->
            val snapshot = roomSnapshotItem(id, now) ?: return@forEach
            if (items.none { it.operationId == snapshot.operationId }) roomItems += snapshot
        }

        val bundle = buildBundle(roomItems + items, now)
        val outgoing = outgoingOf(bundle)
        logOutgoing(bundle, outgoing.text, now)
        return outgoing
    }

    /** دعوة صريحة لغرفة قائمة على هذا الجهاز (تُنشأ الدعوة ولا تفتح غرفة عند الطرف حتى يقبل). */
    suspend fun prepareInvitation(roomId: String, note: String = ""): Outgoing {
        val now = clock()
        val room = ledger.getRoom(roomId) ?: error("غرفة غير موجودة: $roomId")
        val members = ledger.getMembers(roomId)
        val me = members.firstOrNull { it.isMe }
            ?: error("لا عضوية لهذا الجهاز في الغرفة: لا تُعرَف هوية المُرسل")
        val partners = members.filter { !it.isMe }
        require(partners.size == 1) {
            "الدعوة تحتاج طرفًا واحدًا مقابلًا؛ في هذه الغرفة ${partners.size} أطراف — استخدم دعوة فردية لكل طرف."
        }
        val partner = partners.first()
        val payload = HandoverPayloads.roomInvitation(
            room = room,
            members = members,
            inviterMemberId = me.memberId,
            partnerMemberId = partner.memberId,
            note = note
        )
        val item = HandoverItem(
            operationId = "room-invite:${room.id}:${partner.memberId}:${now}",
            entityType = ENTITY_ROOM,
            entityId = room.id,
            action = ACTION_UPSERT,
            payload = payload,
            createdAt = now
        )
        val bundle = buildBundle(listOf(item), now)
        val outgoing = outgoingOf(bundle, isInvitation = true)
        logOutgoing(bundle, outgoing.text, now)
        return outgoing
    }

    // ------------------------------------------------------------------ استيراد

    suspend fun preview(text: String): Preview {
        val parsed = HandoverBundleCodec.parseAny(text)
        val bundle = when (parsed) {
            is BundleParse.Refused -> return Preview.Refused(parsed.reason.messageArabic)
            is BundleParse.Parsed -> parsed.bundle
        }
        var entries = 0
        var acks = 0
        var rooms = 0
        var unknown = 0
        var newInvitations = 0
        var roomsExisting = 0
        var waitingForRoom = 0
        bundle.items.forEach { item ->
            when (item.entityType) {
                ENTITY_ENTRY, ENTITY_ACK -> {
                    val roomId = roomIdOfPayload(item.payload)
                    if (roomId != null && ledger.getRoom(roomId) == null) waitingForRoom++ else {
                        if (item.entityType == ENTITY_ENTRY) entries++ else acks++
                    }
                }
                ENTITY_ROOM -> {
                    rooms++
                    val roomId = roomIdOfRoomPayload(item.payload)
                    if (roomId == null) {
                        unknown++
                    } else if (ledger.getRoom(roomId) == null) {
                        newInvitations++
                    } else {
                        roomsExisting++
                    }
                }
                else -> unknown++
            }
        }
        return Preview.Ready(
            bundle = bundle,
            entries = entries,
            acks = acks,
            roomItems = rooms,
            unknown = unknown,
            alreadyImported = dao.countBundle(bundle.bundleId, HandoverDirection.IN) > 0,
            newInvitations = newInvitations,
            roomsExisting = roomsExisting,
            waitingForRoom = waitingForRoom
        )
    }

    /**
     * استيراد حزمة. كل التطبيق في معاملة واحدة: إما يدخل ما دخل ويُكتب سجلّه، أو لا يتغيّر شيء
     * (ولا توجد حالة نصفية عند انقطاع التيار في منتصف الملف).
     */
    suspend fun importBundle(text: String, now: Long = clock()): ImportReport = db.withTransaction {
        val parsed = HandoverBundleCodec.parseAny(text)
        val bundle = when (parsed) {
            is BundleParse.Refused ->
                return@withTransaction ImportReport(
                    bundleId = "",
                    alreadyImported = false,
                    applied = 0,
                    duplicates = 0,
                    rejected = 0,
                    invited = 0,
                    ignoredTrailingText = false,
                    notes = listOf(parsed.reason.messageArabic)
                )
            is BundleParse.Parsed -> parsed.bundle
        }

        val alreadyImported = dao.countBundle(bundle.bundleId, HandoverDirection.IN) > 0
        if (alreadyImported) {
            return@withTransaction ImportReport(
                bundleId = bundle.bundleId,
                alreadyImported = true,
                applied = 0,
                duplicates = bundle.items.size,
                rejected = 0,
                invited = 0,
                ignoredTrailingText = bundle.hasTrailingText,
                notes = listOf("منع الازدواج: الحزمة نفسها لا تُستورد مرتين.")
            )
        }

        val applier = RoomSyncStore(db, cursorKey = "handover")
        var applied = 0
        var duplicates = 0
        var rejected = 0
        var invited = 0
        var deferred = 0
        val notes = mutableListOf<String>()

        // الترتيب مقصود: لقطات الغرف أولًا، لأن القيد بلا غرفة لا يُطبَّق — وبها **تُنشأ الدعوة**
        // قبل أن يُسأل عن قيودها، فيُحفظ ما لا يمكن تطبيقه بدل أن يُرفض.
        val ordered = bundle.items.sortedBy { if (it.entityType == ENTITY_ROOM) 0 else 1 }

        ordered.forEach { item ->
            val roomId = if (item.entityType == ENTITY_ROOM) {
                roomIdOfRoomPayload(item.payload)
            } else {
                roomIdOfPayload(item.payload)
            }

            // غرفة غير موجودة عندك: العنصر يُحفظ «مؤجّلًا» ولا يُرفض ولا يُرمى. يُطبَّق تلقائيًا
            // في اللحظة التي تقبل فيها الدعوة (acceptInvitation)، فلا يضيع قيد بسبب ترتيب رسالة.
            if (roomId != null && item.entityType != ENTITY_ROOM && ledger.getRoom(roomId) == null) {
                val inserted = dao.insertPendingItemIfNew(
                    PendingItemRow(
                        operationId = item.operationId,
                        roomId = roomId,
                        entityType = item.entityType,
                        entityId = item.entityId,
                        action = item.action,
                        payload = item.payload,
                        createdAt = item.createdAt,
                        receivedAt = now,
                        bundleId = bundle.bundleId
                    )
                )
                if (inserted == -1L) duplicates++ else deferred++
                return@forEach
            }

            val change = RemoteChange(
                kind = if (item.action == ACTION_VOID) RemoteChange.VOID else RemoteChange.UPSERT,
                entityType = item.entityType,
                entityId = item.entityId,
                operationId = item.operationId,
                payload = item.payload,
                // زمن الترتيب محلي: ما وصل وصل الآن، وزمن الخادم غير متاح بلا خادم.
                serverTime = now
            )
            when (applier.apply(change)) {
                ApplyOutcome.APPLIED -> {
                    applied++
                    if (item.entityType == ENTITY_ROOM && ledger.getRoom(item.entityId) == null) invited++
                }
                ApplyOutcome.DUPLICATE -> duplicates++
                else -> {
                    rejected++
                    if (notes.size < 5) notes += rejectionNote(item)
                }
            }
        }

        dao.insertLogIfNew(
            HandoverLogRow(
                id = "handover-${UUID.randomUUID()}",
                bundleId = bundle.bundleId,
                direction = HandoverDirection.IN,
                rooms = bundle.rooms.joinToString(","),
                peerDevice = bundle.deviceCode,
                createdAt = now,
                items = bundle.items.size,
                applied = applied,
                duplicates = duplicates,
                rejected = rejected,
                invited = invited,
                note = notes.joinToString(" | "),
                digest = HandoverBundleCodec.digestOf(text)
            )
        )

        ImportReport(
            bundleId = bundle.bundleId,
            alreadyImported = false,
            applied = applied,
            duplicates = duplicates,
            rejected = rejected,
            invited = invited,
            ignoredTrailingText = bundle.hasTrailingText,
            notes = notes,
            deferred = deferred
        )
    }

    // ------------------------------------------------------------ قبول الدعوة

    /** نتيجة قبول دعوة: الغرفة، وما دخل فورًا من عناصر كانت تنتظرها. */
    data class AcceptResult(
        val roomId: String,
        val applied: Int,
        val duplicates: Int,
        val rejected: Int
    ) {
        val summaryArabic: String
            get() = if (applied == 0 && duplicates == 0) {
                "فُتحت الغرفة، ولا حركات كانت تنتظرها."
            } else {
                buildString {
                    append("فُتحت الغرفة، ودخل معها: $applied حركة")
                    if (duplicates > 0) append(" • مكرر: $duplicates")
                    if (rejected > 0) append(" • مرفوض: $rejected")
                }
            }
    }

    /**
     * قبول دعوة غرفة: هنا فقط تُنشأ الغرفة على هذا الجهاز، بمعرّفات الأعضاء نفسها التي أرسلها
     * المُرسل — وهذا شرط أن يُنسب كل قيد إلى صاحبه ولا يصير «مجهولًا». وفي اللحظة نفسها تُطبَّق
     * كل العناصر التي كانت تنتظر هذه الغرفة، فيسير التسليم كاملًا بملفّ واحد.
     */
    suspend fun acceptInvitation(inviteId: String, now: Long = clock()): AcceptResult = db.withTransaction {
        val invite = dao.invite(inviteId) ?: error("دعوة غير موجودة")
        require(invite.status == InviteStatus.PENDING) { "هذه الدعوة حُسمت سابقًا" }
        val decoded = HandoverPayloads.decodeRoomInvitation(invite.payload)
        if (ledger.getRoom(decoded.room.id) == null) {
            ledger.upsertRoom(decoded.room.copy(status = RoomStatus.ACTIVE, updatedAt = now))
            decoded.members.forEach { member ->
                ledger.upsertMember(member.copy(isMe = member.memberId == invite.partnerMemberId))
            }
        }
        dao.setInviteStatus(inviteId, InviteStatus.ACCEPTED)

        val applier = RoomSyncStore(db, cursorKey = "handover")
        var applied = 0
        var duplicates = 0
        var rejected = 0
        dao.pendingItemsForRoom(decoded.room.id).forEach { row ->
            val change = RemoteChange(
                kind = if (row.action == ACTION_VOID) RemoteChange.VOID else RemoteChange.UPSERT,
                entityType = row.entityType,
                entityId = row.entityId,
                operationId = row.operationId,
                payload = row.payload,
                serverTime = now
            )
            when (applier.apply(change)) {
                ApplyOutcome.APPLIED -> {
                    applied++
                    dao.deletePendingItem(row.operationId)
                }
                ApplyOutcome.DUPLICATE -> {
                    duplicates++
                    dao.deletePendingItem(row.operationId)
                }
                // ما زال غير قابل للتطبيق (مثلًا إقرار سبق قيده في ترتيب لم يصل): يبقى محفوظًا
                // ولا يُحذف، فلا يضيع بإعادة قبول أو بوصول حزمة تالية.
                else -> rejected++
            }
        }
        AcceptResult(roomId = decoded.room.id, applied = applied, duplicates = duplicates, rejected = rejected)
    }

    suspend fun ignoreInvitation(inviteId: String) {
        dao.setInviteStatus(inviteId, InviteStatus.IGNORED)
    }

    // ------------------------------------------------------------------ قراءة

    fun observeLog(limit: Int = 40): Flow<List<HandoverLogRow>> = dao.observeLog(limit)

    fun observeInvites(): Flow<List<PendingInviteRow>> = dao.observeInvites()

    /** عدد ما لم يُقَرّ بعد (يظهر في الحزمة القادمة) — يُقرأ كتدفّق فتُحدَّث الشاشة وحدها. */
    fun observeWaitingCount(): Flow<Int> = dao.observePendingItemCount()

    suspend fun pendingCount(): Int = ledger.pendingHandoverCount()

    suspend fun openInvitations(): Int = dao.countInvites(InviteStatus.PENDING)

    // ------------------------------------------------------------------ داخلي

    private suspend fun buildBundle(items: List<HandoverItem>, now: Long): HandoverBundle {
        val rooms = items.mapNotNull { item ->
            if (item.entityType == ENTITY_ROOM) roomIdOfRoomPayload(item.payload) else roomIdOfPayload(item.payload)
        }.distinct()
        return HandoverBundle(
            bundleId = "bnn-${UUID.randomUUID()}",
            createdAt = now,
            deviceCode = deviceCodeLabel(),
            dbVersion = com.baynana.core.database.DATABASE_VERSION,
            rooms = rooms,
            items = items
        )
    }

    private fun outgoingOf(bundle: HandoverBundle, isInvitation: Boolean = false): Outgoing {
        val text = HandoverBundleCodec.encode(bundle)
        return Outgoing(
            bundle = bundle,
            text = text,
            compactCode = HandoverBundleCodec.compactCode(bundle),
            fileName = "${bundle.bundleId}.bnn.txt",
            isInvitation = isInvitation
        )
    }

    private suspend fun logOutgoing(bundle: HandoverBundle, text: String, now: Long) {
        dao.insertLogIfNew(
            HandoverLogRow(
                id = "handover-${UUID.randomUUID()}",
                bundleId = bundle.bundleId,
                direction = HandoverDirection.OUT,
                rooms = bundle.rooms.joinToString(","),
                peerDevice = "",
                createdAt = now,
                items = bundle.items.size,
                applied = 0,
                duplicates = 0,
                rejected = 0,
                invited = 0,
                note = "",
                digest = HandoverBundleCodec.digestOf(text)
            )
        )
    }

    private suspend fun roomSnapshotItem(roomId: String, now: Long): HandoverItem? {
        val room = ledger.getRoom(roomId) ?: return null
        val members = ledger.getMembers(roomId)
        if (members.isEmpty()) return null
        val me = members.firstOrNull { it.isMe } ?: members.first()
        // في الغرفة الثنائية يُسمّى الطرف الآخر صراحةً، فيعرف مستقبل اللقطة أي عضو هو.
        val partnerMemberId = members.filter { !it.isMe }.singleOrNull()?.memberId ?: ""
        val payload = HandoverPayloads.roomInvitation(
            room = room,
            members = members,
            inviterMemberId = me.memberId,
            partnerMemberId = partnerMemberId
        )
        return HandoverItem(
            operationId = "room-snapshot:${room.id}:${room.status}:${room.updatedAt}",
            entityType = ENTITY_ROOM,
            entityId = room.id,
            action = ACTION_UPSERT,
            payload = payload,
            createdAt = now
        )
    }

    private fun rejectionNote(item: HandoverItem): String = when (item.entityType) {
        ENTITY_ENTRY -> "قيد لم يُطبَّق: غرفته غير موجودة عندك، أو مبلغه/نوعه لا يوافق القواعد"
        ENTITY_ACK -> "إقرار لم يُطبَّق: قيده غير موجود عندك، أو القرار لا يوافق موضعه في القيد"
        ENTITY_ROOM -> "دعوة غرفة لم تُقرأ: تحتاج عضوًا معلومًا للطرف المدعو"
        else -> "نوع غير معروف: ${item.entityType}"
    }

    companion object {
        const val ENTITY_ENTRY = "entry"
        const val ENTITY_ACK = "ack"
        const val ENTITY_ROOM = "room"
        const val ACTION_UPSERT = "UPSERT"
        const val ACTION_VOID = "VOID"

        /** غرفة الحمولة: `roomId` أول حقل في كل حمولة، ويُقرأ بحذر (نصّ لا JSON كامل). */
        fun roomIdOfPayload(payload: String): String? = try {
            val root = JSONObject(payload)
            when {
                root.has("roomId") -> root.getString("roomId")
                root.has("entry") -> root.getJSONObject("entry").optString("roomId").ifBlank { null }
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        fun roomIdOfRoomPayload(payload: String): String? = try {
            JSONObject(payload).getJSONObject("room").getString("id")
        } catch (_: Exception) {
            null
        }
    }
}
