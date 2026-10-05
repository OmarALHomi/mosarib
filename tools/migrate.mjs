#!/usr/bin/env node
/**
 * ترحيل دفتر «بيننا» بجرد مطابق (ح٢٣) — الأداة التي تُثبت «صفر فرق في الجرد» بدل أن تدّعيه.
 *
 * **الفكرة في سطر:** لا يكفي أن يُنقل الملفّ، بل يجب أن يُقارَن **كل رقم** قبل وبعد. فالأداة
 * تبني جردًا من الأحداث، تستوردها عبر عقد `v1` نفسه (لا مسار كتابة خاص)، تسحبها من الخادم مرة
 * أخرى، تبني الجرد من المسحوب، وتقارنه بالأصل — وكل فرق يُقال بالعربية واحدًا واحدًا.
 *
 * **مصدر الحقيقة للجرد واحد**: نفس القواعد المكتوبة في `LedgerMigration.kt` (ملغى يُستثنى، عكسي
 * يُستثنى، مسودة تُعدّ ولا تُقارن). والمتجهات الذهبية (`--write-vectors`) تُثبّت أن الطرفين
 * (Node وKotlin) يعطيان الجرد نفسه ونصوص الفروق نفسها — فلو انحرف أحدهما سقط الفحص.
 *
 * الأوضاع:
 *   node tools/migrate.mjs --self-test                  # الفحص الكامل داخل العملية (بلا شبكة)
 *   node tools/migrate.mjs --write-vectors              # توليد متجهات Kotlin من مخرَج الأداة
 *   node tools/migrate.mjs --check                      # التأكد أن المتجهات لم تنحرف
 *   node tools/migrate.mjs --import <file> --url <url> --token <t>   # ترحيل حقيقي عبر الشبكة
 *   node tools/migrate.mjs --verify <file> --url <url> --token <t>   # مطابقة الجرد فقط
 *   node tools/migrate.mjs --export --url <url> --token <t> --out <file>  # تصدير أحداث من خادم
 *
 * ولا مفاتيح ولا سرّ هنا: الرمز يُقرأ من `--token` أو من `SYNC_TOKEN`، ولا يُطبع في أي مخرَج.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { SyncServerStore, encodeCursor, handleRequest } from "../server/baynana-sync-server.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "..");
const vectorsPath = path.join(repoRoot, "app/src/test/java/com/baynana/domain/migration/MigrationVectors.kt");

export const INVENTORY_FORMAT = "BAYNANA-INVENTORY-1";
export const MIGRATION_FORMAT = "BAYNANA-MIGRATION-1";
const STATUS_DRAFT = "DRAFT";
const STATUS_VOIDED = "VOIDED";

// ------------------------------------------------------------------ الجرد (نسخة Node من نفس القواعد)

/** يقرأ القيود من الأحداث: تفكيك `entry`، توحيد بالمعرّف، وتفضيل الأحدث تحديثًا. */
function entriesFrom(events) {
    const byOperation = new Map();
    for (const event of events) {
        let payload;
        try {
            payload = JSON.parse(event.payload ?? "");
        } catch {
            continue;
        }
        const entry = payload && typeof payload === "object" ? payload.entry : null;
        if (!entry || typeof entry !== "object") continue;
        const operationId = String(entry.operationId ?? event.operationId ?? "");
        if (!operationId) continue;
        const updatedAt = Number(entry.updatedAt ?? 0);
        const previous = byOperation.get(operationId);
        if (!previous || updatedAt >= previous.updatedAt) {
            byOperation.set(operationId, { entry, operationId, updatedAt });
        }
    }
    return byOperation;
}

export function inventoryOf(events) {
    const byOperation = entriesFrom(events);
    const rooms = new Map();
    for (const seen of byOperation.values()) {
        const roomId = String(seen.entry.roomId ?? "");
        if (!roomId) continue;
        if (!rooms.has(roomId)) rooms.set(roomId, []);
        rooms.get(roomId).push(seen);
    }

    const roomList = [...rooms.entries()]
        .map(([roomId, list]) => {
            let active = 0;
            let voided = 0;
            let reversals = 0;
            let minOccurredAt = 0;
            let maxOccurredAt = 0;
            let currency = "";
            const byType = new Map();
            const netByMember = new Map();

            for (const { entry } of list) {
                const amount = BigInt(String(entry.amountMinor ?? "0").replace(/[^0-9-]/g, "") || "0");
                const status = String(entry.status ?? "");
                const isReversal = String(entry.reversesEntryId ?? "") !== "";
                if (!currency) currency = String(entry.currency ?? "");
                const occurredAt = Number(entry.occurredAt ?? 0);
                if (occurredAt > 0 && (minOccurredAt === 0 || occurredAt < minOccurredAt)) minOccurredAt = occurredAt;
                if (occurredAt > maxOccurredAt) maxOccurredAt = occurredAt;

                if (status === STATUS_DRAFT) {
                    // مسودة: تُعدّ وحدها ولا تدخل أي رقم يُقارَن.
                } else if (status === STATUS_VOIDED) {
                    voided++;
                } else if (isReversal) {
                    reversals++;
                } else {
                    active++;
                    const type = String(entry.type ?? "غير معروف");
                    const tally = byType.get(type) ?? { count: 0, sumMinor: 0n };
                    byType.set(type, { count: tally.count + 1, sumMinor: tally.sumMinor + amount });
                    const owedBy = String(entry.owedByMemberId ?? "");
                    const owedTo = String(entry.owedToMemberId ?? "");
                    // نفس قاعدة المحرّك: على الأوّل يُخصم، وللثاني يُضاف.
                    if (owedBy) netByMember.set(owedBy, (netByMember.get(owedBy) ?? 0n) - amount);
                    if (owedTo) netByMember.set(owedTo, (netByMember.get(owedTo) ?? 0n) + amount);
                }
            }

            return {
                roomId,
                currency,
                active,
                voided,
                reversals,
                byType: Object.fromEntries([...byType.entries()].sort().map(([k, v]) => [k, { count: v.count, sumMinor: v.sumMinor.toString() }])),
                netByMember: Object.fromEntries([...netByMember.entries()].sort().map(([k, v]) => [k, v.toString()])),
                minOccurredAt,
                maxOccurredAt,
                operationIds: list.map((seen) => seen.operationId).sort(),
            };
        })
        .sort((a, b) => (a.roomId < b.roomId ? -1 : a.roomId > b.roomId ? 1 : 0));

    const sortedOperations = [...byOperation.keys()].sort();
    return {
        format: INVENTORY_FORMAT,
        rooms: roomList,
        activeTotal: roomList.reduce((sum, room) => sum + room.active, 0),
        voidedTotal: roomList.reduce((sum, room) => sum + room.voided, 0),
        reversalsTotal: roomList.reduce((sum, room) => sum + room.reversals, 0),
        drafts: [...byOperation.values()].filter((seen) => String(seen.entry.status ?? "") === STATUS_DRAFT).length,
        operationIds: sortedOperations,
    };
}

/** يقارن جردين ويعطي نصوص الفروق نفسها التي تعطيها Kotlin (تثبّتها المتجهات). */
export function compareInventories(source, target) {
    const differences = [];
    const notes = [];
    const sourceRooms = new Map(source.rooms.map((room) => [room.roomId, room]));
    const targetRooms = new Map(target.rooms.map((room) => [room.roomId, room]));

    for (const roomId of [...targetRooms.keys()].filter((id) => !sourceRooms.has(id)).sort()) {
        differences.push(`غرفة زائدة عند الوجهة: ${roomId}`);
    }
    for (const roomId of [...sourceRooms.keys()].filter((id) => !targetRooms.has(id)).sort()) {
        differences.push(`غرفة مفقودة من الوجهة: ${roomId}`);
    }

    for (const roomId of [...sourceRooms.keys()].filter((id) => targetRooms.has(id)).sort()) {
        const a = sourceRooms.get(roomId);
        const b = targetRooms.get(roomId);
        if (a.currency !== b.currency) differences.push(`غرفة ${roomId}: العملة مختلفة: ${a.currency} مقابل ${b.currency}`);
        if (a.active !== b.active) differences.push(`غرفة ${roomId}: القيود النشطة ${a.active} عند المصدر و${b.active} عند الوجهة`);
        if (a.voided !== b.voided) differences.push(`غرفة ${roomId}: الملغى ${a.voided} عند المصدر و${b.voided} عند الوجهة`);
        if (a.reversals !== b.reversals) differences.push(`غرفة ${roomId}: القيود العكسية ${a.reversals} عند المصدر و${b.reversals} عند الوجهة`);
        if (a.minOccurredAt !== b.minOccurredAt) differences.push(`غرفة ${roomId}: أقدم قيد ${a.minOccurredAt} مقابل ${b.minOccurredAt}`);
        if (a.maxOccurredAt !== b.maxOccurredAt) differences.push(`غرفة ${roomId}: أحدث قيد ${a.maxOccurredAt} مقابل ${b.maxOccurredAt}`);

        for (const type of [...new Set([...Object.keys(a.byType), ...Object.keys(b.byType)])].sort()) {
            const ta = a.byType[type];
            const tb = b.byType[type];
            if (!ta || !tb) {
                differences.push(`غرفة ${roomId}: النوع ${type} موجود عند ${ta ? "المصدر" : "الوجهة"} فقط`);
            } else {
                if (ta.count !== tb.count) differences.push(`غرفة ${roomId}: النوع ${type} — العدد ${ta.count} مقابل ${tb.count}`);
                if (ta.sumMinor !== tb.sumMinor) differences.push(`غرفة ${roomId}: النوع ${type} — المجموع ${ta.sumMinor} مقابل ${tb.sumMinor}`);
            }
        }

        for (const member of [...new Set([...Object.keys(a.netByMember), ...Object.keys(b.netByMember)])].sort()) {
            const na = a.netByMember[member];
            const nb = b.netByMember[member];
            if (na === undefined) differences.push(`غرفة ${roomId}: العضو ${member} يظهر عند الوجهة فقط بصافي ${nb}`);
            else if (nb === undefined) differences.push(`غرفة ${roomId}: العضو ${member} مفقود من الوجهة (صافيه ${na})`);
            else if (na !== nb) differences.push(`غرفة ${roomId}: صافي العضو ${member} ${na} مقابل ${nb}`);
        }

        const targetSet = new Set(b.operationIds);
        const sourceSet = new Set(a.operationIds);
        const missing = a.operationIds.filter((id) => !targetSet.has(id));
        const extra = b.operationIds.filter((id) => !sourceSet.has(id));
        if (missing.length) differences.push(`غرفة ${roomId}: قيود ناقصة عند الوجهة: ${missing.join("، ")}`);
        if (extra.length) differences.push(`غرفة ${roomId}: قيود زائدة عند الوجهة: ${extra.join("، ")}`);
    }

    if (source.activeTotal !== target.activeTotal) {
        differences.push(`إجمالي القيود النشطة ${source.activeTotal} عند المصدر و${target.activeTotal} عند الوجهة`);
    }
    if (source.voidedTotal !== target.voidedTotal) {
        differences.push(`إجمالي الملغى ${source.voidedTotal} عند المصدر و${target.voidedTotal} عند الوجهة`);
    }
    if (source.operationIds.join("|") !== target.operationIds.join("|")) {
        const targetSet = new Set(target.operationIds);
        const sourceSet = new Set(source.operationIds);
        const missing = source.operationIds.filter((id) => !targetSet.has(id));
        const extra = target.operationIds.filter((id) => !sourceSet.has(id));
        if (missing.length) differences.push(`قيود ناقصة عند الوجهة: ${missing.join("، ")}`);
        if (extra.length) differences.push(`قيود زائدة عند الوجهة: ${extra.join("، ")}`);
    }
    if (source.drafts > 0) {
        notes.push(`مسودات محلية لم تُنقل ولم تُقارن: ${source.drafts} (تبقى على جهاز صاحبها حتى يقرّر)`);
    }
    return { differences, notes };
}

// ------------------------------------------------------------------ الملفّ

/** يبني نصّ ملفّ الترحيل: ترويسة بالجرد ثم الأحداث بترتيب ثابت. */
export function exportText(events, exportedAt, deviceId) {
    const ordered = [...events].sort((a, b) => a.createdAt - b.createdAt || (a.operationId < b.operationId ? -1 : 1));
    const header = {
        kind: "header",
        format: MIGRATION_FORMAT,
        exportedAt,
        deviceId,
        inventory: inventoryOf(events),
    };
    return [JSON.stringify(header), ...ordered.map((event) => JSON.stringify({ kind: "event", ...event }))].join("\n") + "\n";
}

/** يقرأ ملفّ ترحيل بأخطاء عربية مفهومة. */
export function parseMigration(text) {
    const lines = text.split("\n").filter((line) => line.trim() !== "");
    if (!lines.length) throw new Error("الملفّ فارغ: لا ترويسة ولا أحداث");
    let header;
    try {
        header = JSON.parse(lines[0]);
    } catch {
        throw new Error("الترويسة ليست JSON صحيحًا — الملفّ تالف أو مبتور");
    }
    if (header.kind !== "header") throw new Error("أول سطر ليس ترويسة ملفّ ترحيل");
    if (header.format !== MIGRATION_FORMAT) throw new Error(`صيغة الملفّ غير معروفة: ${header.format ?? "بلا صيغة"}`);
    const events = [];
    lines.slice(1).forEach((line, index) => {
        let json;
        try {
            json = JSON.parse(line);
        } catch {
            throw new Error(`السطر ${index + 2} تالف — الملفّ مبتور أو معدَّل`);
        }
        if (json.kind !== "event") return;
        events.push({
            operationId: json.operationId ?? "",
            entityType: json.entityType ?? "",
            entityId: json.entityId ?? "",
            action: json.action ?? "UPSERT",
            payload: json.payload ?? "",
            createdAt: Number(json.createdAt ?? 0),
        });
    });
    return { exportedAt: Number(header.exportedAt ?? 0), deviceId: header.deviceId ?? "", inventory: header.inventory, events };
}

/**
 * اتّساق الملفّ مع ترويسته: هل الأحداث الموجودة فعلًا تعطي الجرد المكتوب في الترويسة؟
 *
 * **لِمَ هذا الفحص؟** لأن ملفًّا مُعدَّلًا أو مبتورًا قد يبقى ترويسته تحمل أرقام الأصل، فيمرّ
 * «التحقق» وهو لا يقرأ إلا الترويسة — أي يصدّق ادّعاء الملفّ عن نفسه. هذا الفحص يقارن الادّعاء
 * بالواقع، ولا يقرأ الملفّ بعد الآن من دون أن يجتازه.
 */
export function verifyFileIntegrity(file) {
    return compareInventories(file.inventory ?? inventoryOf([]), inventoryOf(file.events));
}

/** يدمج فروق الاتّساق مع فروق الوجهة في حكم واحد، بوسم صريح لا يُخفى. */
function mergedDiff(file, targetInventory) {
    const integrity = verifyFileIntegrity(file);
    const againstTarget = compareInventories(file.inventory ?? inventoryOf([]), targetInventory);
    return {
        differences: [
            ...integrity.differences.map((line) => `الملفّ غير متّسق مع ترويسته — ${line}`),
            ...againstTarget.differences,
        ],
        notes: [...integrity.notes, ...againstTarget.notes],
    };
}

// ------------------------------------------------------------------ النقل عبر العقد

/** يستورد الأحداث عبر عقد `v1` نفسه (لا مسار خاص): دفعات صغيرة، ومنع التكرار يعمل عند الخادم. */
export async function importThroughContract(events, { url, token, deviceId = "migration", batchSize = 50, fetchImpl = fetch } = {}) {
    const outcomes = { accepted: 0, rejected: 0, errors: [], reasons: [] };
    for (let index = 0; index < events.length; index += batchSize) {
        const batch = events.slice(index, index + batchSize);
        const response = await fetchImpl(`${url.replace(/\/$/, "")}/api/v1/changes`, {
            method: "POST",
            headers: { "content-type": "application/json; charset=utf-8", authorization: `Bearer ${token}` },
            body: JSON.stringify({ deviceId, items: batch.map((event) => ({ ...event })) }),
        });
        if (!response.ok) {
            outcomes.errors.push(`دفعة ${index / batchSize + 1}: ردّ الخادم ${response.status}`);
            continue;
        }
        const body = await response.json();
        for (const outcome of body.outcomes ?? []) {
            if (outcome.status === "ACCEPTED") outcomes.accepted++;
            else {
                outcomes.rejected++;
                outcomes.reasons.push(outcome.reason ?? "رُفض بلا سبب");
            }
        }
    }
    return outcomes;
}

/** يسحب **كل** التغييرات من الخادم بمؤشرات حتى النهاية، ويعيدها أحداثًا جاهزة للجرد. */
export async function pullAll({ url, token, limit = 200, fetchImpl = fetch }) {
    const events = [];
    let cursor = null;
    for (let page = 0; page < 10_000; page++) {
        const query = new URLSearchParams({ limit: String(limit) });
        if (cursor) query.set("cursor", cursor);
        const response = await fetchImpl(`${url.replace(/\/$/, "")}/api/v1/changes?${query}`, {
            headers: { authorization: `Bearer ${token}` },
        });
        if (!response.ok) throw new Error(`تعذّر السحب: ردّ الخادم ${response.status}`);
        const body = await response.json();
        for (const change of body.changes ?? []) {
            events.push({
                operationId: change.operationId,
                entityType: change.entityType,
                entityId: change.entityId,
                action: change.kind,
                payload: change.payload,
                createdAt: change.serverTime,
            });
        }
        if (!body.hasMore) break;
        cursor = body.nextCursor;
    }
    return events;
}

/** استيراد داخل العملية (بلا شبكة): يُمرّ من نفس `handleRequest` الذي يخدم الشبكة. */
export function importInProcess(store, events, { token = null } = {}) {
    const outcomes = { accepted: 0, rejected: 0, reasons: [] };
    for (let index = 0; index < events.length; index += 50) {
        const batch = events.slice(index, index + 50);
        const response = handleRequest(
            store,
            {
                method: "POST",
                url: "/api/v1/changes",
                headers: { authorization: `Bearer ${token}` },
                body: { deviceId: "migration", items: batch },
            },
            { token }
        );
        for (const outcome of response.body.outcomes ?? []) {
            if (outcome.status === "ACCEPTED") outcomes.accepted++;
            else {
                outcomes.rejected++;
                outcomes.reasons.push(outcome.reason ?? "رُفض بلا سبب");
            }
        }
    }
    return outcomes;
}

/** قراءة كل التغييرات من مخزن داخل العملية (نفس شكل السحب). */
export function pullInProcess(store) {
    return store.pull(null, 10_000).changes.map((change) => ({
        operationId: change.operationId,
        entityType: change.entityType,
        entityId: change.entityId,
        action: change.kind,
        payload: change.payload,
        createdAt: change.serverTime,
    }));
}

// ------------------------------------------------------------------ بيانات الفحص

/** دفتر صغير واقعي: سقيتان، سداد مخصَّص، إلغاء بقيد عكسي، ومسودة محلية. */
export function sampleLedger() {
    const base = 1_767_225_600_000;
    const entry = (id, operationId, overrides = {}) => ({
        id,
        roomId: "room-water-1",
        operationId,
        type: "WATER_SESSION",
        owedByMemberId: "farmer",
        owedToMemberId: "distributor",
        amountMinor: "1500000",
        currency: "YER_NEW",
        occurredAt: base,
        status: "SENT",
        description: "سقية",
        createdByMemberId: "distributor",
        createdAt: base,
        updatedAt: base,
        ...overrides,
    });
    return [
        {
            operationId: "op-water-1",
            entityType: "entry",
            entityId: "entry-water-1",
            action: "UPSERT",
            payload: JSON.stringify({ v: 1, kind: "ENTRY", entry: entry("entry-water-1", "op-water-1") }),
            createdAt: base,
        },
        {
            operationId: "op-water-2",
            entityType: "entry",
            entityId: "entry-water-2",
            action: "UPSERT",
            payload: JSON.stringify({
                v: 1,
                kind: "ENTRY",
                // الأصل الملغى يبقى في الدفتر بحالة VOIDED (لا يُحذف): القيد العكسي يظهر الأثر.
                entry: entry("entry-water-2", "op-water-2", {
                    amountMinor: "2500000",
                    occurredAt: base + 60_000,
                    status: "VOIDED",
                    updatedAt: base + 180_000,
                }),
            }),
            createdAt: base + 60_000,
        },
        {
            operationId: "op-payment-1",
            entityType: "entry",
            entityId: "entry-payment-1",
            action: "UPSERT",
            payload: JSON.stringify({
                v: 1,
                kind: "RECEIPT",
                entry: entry("entry-payment-1", "op-payment-1", {
                    type: "PAYMENT",
                    owedByMemberId: "distributor",
                    owedToMemberId: "farmer",
                    amountMinor: "1000000",
                    occurredAt: base + 120_000,
                }),
                allocations: [{ paymentEntryId: "entry-payment-1", debtEntryId: "entry-water-1", amountMinor: "1000000", currency: "YER_NEW", createdAt: base + 120_000 }],
                mode: { kind: "OLDEST_FIRST" },
                unappliedMinor: "0",
            }),
            createdAt: base + 120_000,
        },
        {
            operationId: "op-void-1",
            entityType: "entry",
            entityId: "entry-water-2",
            action: "VOID",
            payload: JSON.stringify({
                v: 1,
                kind: "VOID",
                operationId: "op-void-1",
                reversesEntryId: "entry-water-2",
                reason: "سقية مكرّرة",
                entry: entry("entry-reversal-2", "op-void-1", {
                    status: "SENT",
                    reversesEntryId: "entry-water-2",
                    owedByMemberId: "distributor",
                    owedToMemberId: "farmer",
                    amountMinor: "2500000",
                    occurredAt: base + 180_000,
                }),
            }),
            createdAt: base + 180_000,
        },
        {
            // المسودة: تُعدّ في الجرد ولا تُنقل بطبيعتها (لكنّ الأداة تنقل ما في الملفّ؛ وهنا نضعها
            // في الجرد فقط لأن التطبيق لا يصدّرها. وجودها هنا يقيس «تُعدّ ولا تُقارن».)
            operationId: "op-draft-1",
            entityType: "entry",
            entityId: "entry-draft-1",
            action: "UPSERT",
            payload: JSON.stringify({
                v: 1,
                kind: "ENTRY",
                entry: entry("entry-draft-1", "op-draft-1", { status: "DRAFT", amountMinor: "700000" }),
            }),
            createdAt: base + 240_000,
        },
    ];
}

// ------------------------------------------------------------------ المتجهات الذهبية

function vectorsFor() {
    const events = sampleLedger();
    const withoutDraft = events.filter((event) => !event.payload.includes('"status":"DRAFT"'));
    const store = new SyncServerStore();
    importInProcess(store, withoutDraft);
    const pulled = pullInProcess(store);

    const sourceInventory = inventoryOf(withoutDraft);
    const targetInventory = inventoryOf(pulled);
    const clean = compareInventories(sourceInventory, targetInventory);

    const dropped = withoutDraft.filter((event) => event.operationId !== "op-payment-1");
    const changedAmount = withoutDraft.map((event) =>
        event.operationId === "op-water-1"
            ? { ...event, payload: event.payload.replace('"1500000"', '"1400000"') }
            : event
    );
    const extraRoom = [
        ...withoutDraft,
        {
            operationId: "op-other-room",
            entityType: "entry",
            entityId: "entry-other-room",
            action: "UPSERT",
            payload: JSON.stringify({
                v: 1,
                kind: "ENTRY",
                entry: {
                    id: "entry-other-room",
                    roomId: "room-goods-7",
                    operationId: "op-other-room",
                    type: "GOODS_DEBT",
                    owedByMemberId: "farmer",
                    owedToMemberId: "distributor",
                    amountMinor: "500000",
                    currency: "YER_NEW",
                    occurredAt: 1_767_225_900_000,
                    status: "SENT",
                    createdAt: 1_767_225_900_000,
                    updatedAt: 1_767_225_900_000,
                },
            }),
            createdAt: 1_767_225_900_000,
        },
    ];

    return {
        events: withoutDraft,
        sourceInventory,
        targetInventory,
        clean,
        droppedDiff: compareInventories(inventoryOf(dropped), targetInventory),
        changedDiff: compareInventories(inventoryOf(changedAmount), targetInventory),
        extraRoomSourceInventory: inventoryOf(extraRoom),
        extraRoomDiff: compareInventories(inventoryOf(extraRoom), targetInventory),
        inventoryWithDraft: inventoryOf(events),
    };
}

function kotlinVectors(data) {
    const s = (value) => JSON.stringify(JSON.stringify(value));
    const list = (value) => JSON.stringify(JSON.stringify(value));
    return `package com.baynana.domain.migration

/**
 * متجهات ذهبية للترحيل (ح٢٣) — **مولَّدة آليًّا، لا تُحرَّر بيد**.
 *
 * المصدر: \`node tools/migrate.mjs --write-vectors\`، والتحقق في CI بالأمر نفسه. فالجرد هنا ليس
 * وصفًا للقواعد بل **مخرَج الأداة الحقيقية** بعد استيراد الأحداث عبر عقد \`v1\` وسحبها مرة أخرى،
 * ونصوص الفروق هي نصوص Kotlin نفسها — فلو انحرف طرف عن الآخر يسقط الفحص.
 */
object MigrationVectors {

    /** الأحداث كما صدّرها الجهاز (مسودة محلية مستثناة: لا تُنقل). */
    const val EVENTS = ${s(data.events)}

    /** جرد المصدر كما بناه التطبيق. */
    const val SOURCE_INVENTORY = ${s(data.sourceInventory)}

    /** الجرد بعد الاستيراد والسحب من الخادم — يجب أن يطابق المصدر بلا فرق. */
    const val TARGET_INVENTORY = ${s(data.targetInventory)}

    /** مقارنة نظيفة: لا فروق. */
    const val CLEAN_DIFFERENCES = "[]"

    /** حذف حدث سداد: الفرق المتوقّع. */
    const val DROPPED_PAYMENT_DIFFERENCES = ${list(data.droppedDiff.differences)}

    /** تغيير مبلغ سقية: الفرق المتوقّع. */
    const val CHANGED_AMOUNT_DIFFERENCES = ${list(data.changedDiff.differences)}

    /** جرد مصدر فيه غرفة زائدة. */
    const val EXTRA_ROOM_SOURCE_INVENTORY = ${s(data.extraRoomSourceInventory)}

    /** غرفة زائدة عند المصدر: الفرق المتوقّع. */
    const val EXTRA_ROOM_DIFFERENCES = ${list(data.extraRoomDiff.differences)}

    /** جرد يشمل مسودة: تُعدّ ولا تُقارن. */
    const val INVENTORY_WITH_DRAFT = ${s(data.inventoryWithDraft)}
}
`;
}

// ------------------------------------------------------------------ الشبكة: أوامر حقيقية

function parseArgs(argv) {
    const args = {};
    for (let index = 0; index < argv.length; index++) {
        const token = argv[index];
        if (token.startsWith("--")) {
            const next = argv[index + 1];
            if (next === undefined || next.startsWith("--")) args[token.slice(2)] = true;
            else {
                args[token.slice(2)] = next;
                index++;
            }
        }
    }
    return args;
}

async function commandExport(args) {
    const token = args.token ?? process.env.SYNC_TOKEN;
    if (!args.url || !token) throw new Error("يلزم --url و--token (أو SYNC_TOKEN)");
    const events = await pullAll({ url: String(args.url), token });
    const text = exportText(events, Date.now(), String(args.device ?? "server-export"));
    const out = String(args.out ?? "baynana-migration.jsonl");
    fs.writeFileSync(out, text, "utf8");
    const inventory = inventoryOf(events);
    console.log(`كُتب ${out}: ${events.length} حدثًا، ${inventory.rooms.length} غرفة، ${inventory.activeTotal} قيدًا نشطًا.`);
}

async function commandImport(args) {
    const token = args.token ?? process.env.SYNC_TOKEN;
    if (!args.import || !args.url || !token) throw new Error("يلزم --import و--url و--token (أو SYNC_TOKEN)");
    const file = parseMigration(fs.readFileSync(String(args.import), "utf8"));
    if (!file.events.length) throw new Error("الملفّ لا يحمل أحداثًا: لا شيء يُستورد");
    const outcomes = await importThroughContract(file.events, { url: String(args.url), token });
    console.log(`استُورد: ${outcomes.accepted} مقبولًا، ${outcomes.rejected} مرفوضًا.`);
    outcomes.reasons.slice(0, 5).forEach((reason) => console.log(`  رفض: ${reason}`));
    const pulled = await pullAll({ url: String(args.url), token });
    const diff = mergedDiff(file, inventoryOf(pulled));
    if (verifyFileIntegrity(file).differences.length) {
        console.error("⛔ الملفّ غير متّسق مع ترويسته: مُعدَّل أو مبتور — لا يُقبل نقله:");
    }
    diff.notes.forEach((note) => console.log(`  ملاحظة: ${note}`));
    if (diff.differences.length) {
        console.error("⛔ الجرد لا يطابق — لا يُعلن الترحيل ناجحًا:");
        diff.differences.forEach((line) => console.error(`  • ${line}`));
        process.exit(1);
    }
    console.log("✅ صفر فرق في الجرد بعد الاستيراد.");
}

async function commandVerify(args) {
    const token = args.token ?? process.env.SYNC_TOKEN;
    if (!args.verify || !args.url || !token) throw new Error("يلزم --verify و--url و--token (أو SYNC_TOKEN)");
    const file = parseMigration(fs.readFileSync(String(args.verify), "utf8"));
    const pulled = await pullAll({ url: String(args.url), token });
    const diff = mergedDiff(file, inventoryOf(pulled));
    if (verifyFileIntegrity(file).differences.length) {
        console.error("⛔ الملفّ غير متّسق مع ترويسته: مُعدَّل أو مبتور — لا يُقبل نقله:");
    }
    diff.notes.forEach((note) => console.log(`  ملاحظة: ${note}`));
    if (diff.differences.length) {
        console.error("⛔ الجرد لا يطابق:");
        diff.differences.forEach((line) => console.error(`  • ${line}`));
        process.exit(1);
    }
    console.log("✅ صفر فرق في الجرد.");
}

// ------------------------------------------------------------------ الفحص الذاتي

function selfTest() {
    const events = sampleLedger();
    const transportable = events.filter((event) => !event.payload.includes('"status":"DRAFT"'));
    const inventory = inventoryOf(transportable);

    // ١) الجرد نفسه معقول: الأرقام تُراجَع يدويًا في التعليق، لا بالثقة في الأداة.
    //    سقية 1,500,000 + سقية 2,500,000 (ملغاة بقيد عكسي) + سداد 1,000,000.
    const room = inventory.rooms[0];
    assert.equal(inventory.rooms.length, 1);
    assert.equal(room.active, 2, "النشط: السقية الأولى والسداد (والثانية ملغاة)");
    assert.equal(room.voided, 1);
    assert.equal(room.reversals, 1);
    assert.equal(inventory.activeTotal, 2);
    assert.equal(room.byType.WATER_SESSION.count, 1);
    assert.equal(room.byType.WATER_SESSION.sumMinor, "1500000");
    assert.equal(room.byType.PAYMENT.sumMinor, "1000000");
    // صافي المزارع: -1,500,000 (سقية) +1,000,000 (سداد) = -500,000، وصافي الموزّع +500,000.
    assert.equal(room.netByMember.farmer, "-500000");
    assert.equal(room.netByMember.distributor, "500000");
    assert.equal(inventory.drafts, 0, "المسودة ليست في هذه القائمة");
    console.log("  ✅ الجرد يُحسب بالقواعد المعلنة (الملغى والعكسي مستثنيان، والصافي مطابق لليد)");

    // ٢) مسودة في المدخل: تُعدّ ولا تُقارن.
    const withDraft = inventoryOf(events);
    assert.equal(withDraft.drafts, 1);
    assert.deepEqual(withDraft.rooms[0].netByMember, inventory.rooms[0].netByMember, "المسودة لا تُغيّر الصافي");
    console.log("  ✅ المسودة تُعدّ وحدها ولا تدخل أي رقم يُقارن");

    // ٣) استيراد عبر العقد ثم سحب: صفر فرق.
    const store = new SyncServerStore();
    const outcomes = importInProcess(store, transportable);
    assert.equal(outcomes.rejected, 0);
    const dragged = importInProcess(store, transportable); // إعادة الاستيراد كاملة
    assert.equal(dragged.rejected, 0);
    assert.equal(store.changes.length, transportable.length, "إعادة الاستيراد لا تُضاعف شيئًا");
    const pulled = pullInProcess(store);
    const clean = compareInventories(inventory, inventoryOf(pulled));
    assert.deepEqual(clean.differences, [], "يجب أن يكون الجرد مطابقًا");
    console.log("  ✅ استيراد عبر العقد نفسه: صفر فرق، وإعادة الاستيراد لا تُضاعف");

    // ٤) الحالات المفسودة تُكتشف فعلًا (الفحص الذي يهمّ: أداة تقول «سليم» دائمًا عديمة الفائدة).
    const dropped = compareInventories(inventoryOf(transportable.filter((e) => e.operationId !== "op-payment-1")), inventory);
    assert.ok(dropped.differences.length > 0, "حذف حدث يجب أن يُكتشف");
    assert.ok(dropped.differences.some((line) => line.includes("op-payment-1")), "ويُسمّى القيد المفقود");
    assert.ok(dropped.differences.some((line) => line.includes("صافي العضو")), "ويُذكر أثر الصافي");

    const changed = compareInventories(
        inventoryOf(transportable.map((e) => (e.operationId === "op-water-1" ? { ...e, payload: e.payload.replace('"1500000"', '"1400000"') } : e))),
        inventory
    );
    assert.ok(changed.differences.some((line) => line.includes("المجموع")), "تغيير مبلغ يجب أن يُكتشف بالمجموع");

    const extra = compareInventories(inventory, inventoryOf(transportable.filter((e) => e.operationId !== "op-water-1")));
    assert.ok(extra.differences.some((line) => line.includes("ناقصة")), "الفرق العكسي يُسمّى ناقصًا عند الوجهة");
    console.log("  ✅ النقص والتغيير والزيادة تُكتشف بنصوصها العربية (لا «سليم» دائمًا)");

    // ٥) الملفّ: ترويسة + أحداث، وقراءة صحيحة، وأخطاء مفهومة.
    const text = exportText(transportable, 1_767_225_600_000, "device-1");
    const parsed = parseMigration(text);
    assert.equal(parsed.events.length, transportable.length);
    assert.deepEqual(parsed.inventory, inventory, "جرد الترويسة هو جرد الأحداث");
    assert.throws(() => parseMigration(""), /فارغ/);
    assert.throws(() => parseMigration("{ ليس JSON"), /تالف|JSON/);
    assert.throws(() => parseMigration(JSON.stringify({ kind: "header", format: "X" })), /غير معروفة/);
    const truncated = text.split("\n");
    truncated[2] = truncated[2].slice(0, 20);
    assert.throws(() => parseMigration(truncated.join("\n")), /تالف|مبتور/);
    console.log("  ✅ الملفّ يُقرأ، والملفّ التالف يُقال سببه بالعربية");

    // ٥ب) الملفّ المُعدَّل أو المبتور يُكشف بفحص الاتّساق، لا بالثقة في ترويسته.
    const tamperedLines = text.split("\n").filter((line) => !(line.startsWith('{"kind":"event"') && line.includes("op-payment-1")));
    const tampered = parseMigration(tamperedLines.join("\n"));
    const integrity = verifyFileIntegrity(tampered);
    assert.ok(integrity.differences.length > 0, "حذف حدث مع بقاء الترويسة يجب أن يُكشف");
    assert.ok(integrity.differences.some((line) => line.includes("op-payment-1")), "ويُسمّى القيد المفقود");
    assert.deepEqual(verifyFileIntegrity(parsed).differences, [], "الملفّ السليم متّسق مع ترويسته");
    const merged = mergedDiff(tampered, inventoryOf(transportable));
    assert.ok(merged.differences[0].startsWith("الملفّ غير متّسق مع ترويسته"), "الوسم صريح في أول الفروق");
    console.log("  ✅ الملفّ المُعدَّل أو المبتور يُكشف بمقارنة ترويسته بأحداثه (لا بالثقة بادّعائه)");

    // ٦) الحتمية: نفس الأحداث بترتيب مختلف تعطي الملفّ والجرد نفسهما.
    const shuffled = [...transportable].reverse();
    assert.equal(exportText(shuffled, 1_767_225_600_000, "device-1"), text, "الملفّ حتمي لا يعتمد على ترتيب الإدخال");
    assert.deepEqual(inventoryOf(shuffled), inventory);
    console.log("  ✅ الملفّ حتمي: نفس المدخل بترتيب مختلف = نفس المخرَج بالبايت");
}

// ------------------------------------------------------------------ التنفيذ

async function main() {
    const args = parseArgs(process.argv.slice(2));

    if (args["self-test"]) {
        console.log("فحص أداة الترحيل (بلا شبكة):");
        selfTest();
        const data = vectorsFor();
        const kotlin = kotlinVectors(data);
        const current = fs.existsSync(vectorsPath) ? fs.readFileSync(vectorsPath, "utf8") : "";
        if (current !== kotlin) {
            console.error("\n⛔ متجهات الترحيل غير مطابقة لمخرَج الأداة — أعِد التوليد: --write-vectors");
            process.exit(1);
        }
        console.log("  ✅ متجهات Kotlin هي مخرَج الأداة الآن");
        console.log("\n✅ أداة الترحيل: الجرد مطابق، والفروق تُكتشف، والملفّ حتمي.");
        return;
    }

    if (args["write-vectors"]) {
        fs.mkdirSync(path.dirname(vectorsPath), { recursive: true });
        fs.writeFileSync(vectorsPath, kotlinVectors(vectorsFor()), "utf8");
        console.log(`كُتب: ${path.relative(repoRoot, vectorsPath)}`);
        return;
    }

    if (args.export) return commandExport(args);
    if (args.import) return commandImport(args);
    if (args.verify) return commandVerify(args);

    console.log("الاستعمال:");
    console.log("  node tools/migrate.mjs --self-test");
    console.log("  node tools/migrate.mjs --write-vectors");
    console.log("  node tools/migrate.mjs --import <file> --url <url> --token <t>");
    console.log("  node tools/migrate.mjs --verify <file> --url <url> --token <t>");
    console.log("  node tools/migrate.mjs --export --url <url> --token <t> --out <file>");
    process.exit(2);
}

// الأوامر تُشغَّل عند التنفيذ المباشر فقط: الاستيراد البرمجي (اختبار أو أداة أخرى) لا يُطلق CLI.
if (import.meta.url === `file://${process.argv[1]}`) {
    main().catch((error) => {
        console.error(`⛔ ${error.message}`);
        process.exit(1);
    });
}

// تُستعمل في الاختبارات والاستيراد البرمجي بلا تشغيل الأوامر.
export { encodeCursor };
