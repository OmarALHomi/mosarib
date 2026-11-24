<?php
/**
 * User removal for the admin dashboard (task 036).
 * - soft_delete: moves all_users/{key} to deleted_users/{key} (restorable for 30 days) and removes
 *   the user's email_index entry and FCM token.
 * - restore: puts the record back.
 * - hard_delete: SUPER_ADMIN only, from the deleted list, after typing the user's name or email.
 * License records (licenses/, devices/) are never touched: a deleted user who opens the app again
 * registers as new, and an active license stays valid for its device.
 * Every delete / restore is written to data/admin_audit.json under flock.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth.php';

requireAuth();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(false, 'طريقة غير مدعومة', null, 405);
}

$input = json_decode(file_get_contents('php://input'), true) ?: [];
$headers = function_exists('getallheaders') ? getallheaders() : [];
$csrf = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? ($headers['X-CSRF-Token'] ?? ($headers['x-csrf-token'] ?? ($input['csrf_token'] ?? '')));
if (!verifyCsrfToken($csrf)) {
    jsonResponse(false, 'رمز أمان الجلسة غير صالح (CSRF)، أعد تحميل الصفحة', null, 403);
}

$admin = $_SESSION['admin_user'] ?? [];
$adminRole = $admin['role'] ?? '';
$adminEmail = $admin['email'] ?? 'unknown';
if ($adminRole !== 'ADMIN' && $adminRole !== 'SUPER_ADMIN') {
    jsonResponse(false, 'غير مصرح: إدارة المستخدمين مقتصرة على المدراء', null, 403);
}

define('DELETED_KEEP_MS', 2592000000);
$auditFile = __DIR__ . '/../data/admin_audit.json';

function cleanUserKey($key) {
    $key = strtoupper(str_replace('-', '', trim((string)$key)));
    return preg_match('/^[A-Z0-9]{4,40}$/', $key) ? $key : null;
}

function emailIndexKey($email) {
    return str_replace(['.', '@'], ['_', '_at_'], strtolower(trim((string)$email)));
}

function auditLog($file, array $entry) {
    $fp = @fopen($file, 'c+');
    if (!$fp) return;
    if (flock($fp, LOCK_EX)) {
        $log = json_decode(stream_get_contents($fp), true);
        if (!is_array($log)) $log = [];
        $log[] = $entry;
        if (count($log) > 5000) $log = array_slice($log, -5000);
        ftruncate($fp, 0);
        rewind($fp);
        fwrite($fp, json_encode($log, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        fflush($fp);
        flock($fp, LOCK_UN);
    }
    fclose($fp);
}

function nowMs() {
    return (int) round(microtime(true) * 1000);
}

/** Removes deleted_users records older than 30 days. Returns the remaining records. */
function purgeExpired($auditFile, $adminEmail) {
    $res = firebaseRequest('deleted_users.json');
    $all = ($res['error'] === false && is_array($res['data'])) ? $res['data'] : [];
    $now = nowMs();
    foreach ($all as $key => $rec) {
        $deletedAt = is_array($rec) ? intval($rec['deletedAt'] ?? 0) : 0;
        if ($deletedAt > 0 && $now - $deletedAt > DELETED_KEEP_MS) {
            firebaseRequest("deleted_users/$key.json", 'DELETE');
            auditLog($auditFile, ['action' => 'auto_purge', 'user' => $key, 'by' => 'SYSTEM', 'at' => $now]);
            unset($all[$key]);
        }
    }
    return $all;
}

$action = $input['action'] ?? '';

if ($action === 'list_deleted') {
    $records = purgeExpired($auditFile, $adminEmail);
    $list = [];
    foreach ($records as $key => $rec) {
        if (!is_array($rec)) continue;
        $u = is_array($rec['user'] ?? null) ? $rec['user'] : [];
        $list[] = [
            'key' => $key,
            'name' => $u['subscriberName'] ?? ($u['googleDisplayName'] ?? ''),
            'email' => $u['googleEmail'] ?? '',
            'phone' => $u['phone'] ?? '',
            'appVersion' => $u['appVersion'] ?? '',
            'deletedAt' => $rec['deletedAt'] ?? 0,
            'deletedBy' => $rec['deletedBy'] ?? '',
            'reason' => $rec['reason'] ?? '',
            'purgeAt' => intval($rec['deletedAt'] ?? 0) + DELETED_KEEP_MS
        ];
    }
    usort($list, fn($a, $b) => $b['deletedAt'] <=> $a['deletedAt']);
    jsonResponse(true, 'قائمة المحذوفين', ['users' => $list, 'can_hard_delete' => $adminRole === 'SUPER_ADMIN']);
}

if ($action === 'soft_delete') {
    $keys = is_array($input['keys'] ?? null) ? $input['keys'] : [];
    $reason = mb_substr(trim((string)($input['reason'] ?? '')), 0, 200);
    if (empty($keys) || count($keys) > 500) {
        jsonResponse(false, 'لم يتم تحديد مستخدمين للحذف');
    }
    $done = 0;
    $failed = [];
    foreach ($keys as $rawKey) {
        $key = cleanUserKey($rawKey);
        if ($key === null) { $failed[] = (string)$rawKey; continue; }
        $res = firebaseRequest("all_users/$key.json");
        $user = ($res['error'] === false && is_array($res['data'])) ? $res['data'] : null;
        if ($user === null) { $failed[] = $key; continue; }

        // Keep the email_index entry inside the record so a restore can put it back
        $emailEntry = null;
        $emailKey = !empty($user['googleEmail']) ? emailIndexKey($user['googleEmail']) : '';
        if ($emailKey !== '') {
            $er = firebaseRequest("email_index/$emailKey.json");
            if ($er['error'] === false && is_array($er['data']) && strtoupper($er['data']['deviceKey'] ?? '') === $key) {
                $emailEntry = $er['data'];
            }
        }
        $tr = firebaseRequest("fcm_tokens/$key.json");
        $tokenEntry = ($tr['error'] === false && is_array($tr['data'])) ? $tr['data'] : null;

        $record = [
            'user' => $user,
            'emailIndex' => $emailEntry,
            'emailKey' => $emailKey,
            'fcmToken' => $tokenEntry,
            'deletedAt' => nowMs(),
            'deletedBy' => $adminEmail,
            'reason' => $reason
        ];
        $put = firebaseRequest("deleted_users/$key.json", 'PUT', $record);
        if ($put['error'] !== false) { $failed[] = $key; continue; }

        firebaseRequest("all_users/$key.json", 'DELETE');
        if ($emailEntry !== null) firebaseRequest("email_index/$emailKey.json", 'DELETE');
        if ($tokenEntry !== null) firebaseRequest("fcm_tokens/$key.json", 'DELETE');

        auditLog($auditFile, ['action' => 'soft_delete', 'user' => $key, 'name' => $user['subscriberName'] ?? '', 'by' => $adminEmail, 'reason' => $reason, 'at' => nowMs()]);
        $done++;
    }
    jsonResponse($done > 0, $done > 0 ? "تم نقل {$done} مستخدم إلى المحذوفين (يمكن استرجاعهم خلال 30 يوماً)" : 'تعذر حذف المستخدمين المحددين', ['done' => $done, 'failed' => $failed]);
}

if ($action === 'restore') {
    $key = cleanUserKey($input['key'] ?? '');
    if ($key === null) jsonResponse(false, 'مستخدم غير صالح');
    $res = firebaseRequest("deleted_users/$key.json");
    $rec = ($res['error'] === false && is_array($res['data'])) ? $res['data'] : null;
    if ($rec === null || !is_array($rec['user'] ?? null)) jsonResponse(false, 'السجل غير موجود في المحذوفين');

    // If the device registered again meanwhile, keep its newer record
    $cur = firebaseRequest("all_users/$key.json");
    if (!($cur['error'] === false && is_array($cur['data']))) {
        $putUser = firebaseRequest("all_users/$key.json", 'PUT', $rec['user']);
        if ($putUser['error'] !== false || ($putUser['code'] ?? 500) >= 400) {
            jsonResponse(false, 'تعذر استرجاع بيانات المستخدم إلى السحابة', null, 502);
        }
    }
    if (!empty($rec['emailKey']) && is_array($rec['emailIndex'] ?? null)) {
        firebaseRequest("email_index/{$rec['emailKey']}.json", 'PUT', $rec['emailIndex']);
    }
    if (is_array($rec['fcmToken'] ?? null)) {
        firebaseRequest("fcm_tokens/$key.json", 'PUT', $rec['fcmToken']);
    }
    $delRes = firebaseRequest("deleted_users/$key.json", 'DELETE');
    if ($delRes['error'] !== false || ($delRes['code'] ?? 500) >= 400) {
        jsonResponse(false, 'تم استرجاع المستخدم لكن تعذر مسح السجل من قائمة المحذوفين', null, 502);
    }
    auditLog($auditFile, ['action' => 'restore', 'user' => $key, 'name' => $rec['user']['subscriberName'] ?? '', 'by' => $adminEmail, 'at' => nowMs()]);
    jsonResponse(true, 'تم استرجاع المستخدم');
}

if ($action === 'hard_delete') {
    if ($adminRole !== 'SUPER_ADMIN') {
        jsonResponse(false, 'الحذف النهائي مقتصر على المدير الأعلى (SUPER_ADMIN)', null, 403);
    }
    $key = cleanUserKey($input['key'] ?? '');
    $confirmText = trim((string)($input['confirm'] ?? ''));
    if ($key === null) jsonResponse(false, 'مستخدم غير صالح');
    $res = firebaseRequest("deleted_users/$key.json");
    $rec = ($res['error'] === false && is_array($res['data'])) ? $res['data'] : null;
    if ($rec === null) jsonResponse(false, 'الحذف النهائي يتم من قائمة المحذوفين فقط');
    $u = is_array($rec['user'] ?? null) ? $rec['user'] : [];
    $name = trim($u['subscriberName'] ?? '');
    $email = strtolower(trim($u['googleEmail'] ?? ''));
    $matches = $confirmText !== '' && (
        ($name !== '' && $confirmText === $name)
        || ($email !== '' && strtolower($confirmText) === $email)
        || cleanUserKey($confirmText) === $key
    );
    if (!$matches) {
        jsonResponse(false, 'اكتب اسم المستخدم أو بريده كما هو لتأكيد الحذف النهائي');
    }
    $hardDel = firebaseRequest("deleted_users/$key.json", 'DELETE');
    if ($hardDel['error'] !== false || ($hardDel['code'] ?? 500) >= 400) {
        jsonResponse(false, 'تعذر الحذف النهائي من فايربيس', null, 502);
    }
    auditLog($auditFile, ['action' => 'hard_delete', 'user' => $key, 'name' => $u['subscriberName'] ?? '', 'by' => $adminEmail, 'at' => nowMs()]);
    jsonResponse(true, 'تم الحذف النهائي. سجلات الترخيص لم تُمس.');
}

jsonResponse(false, 'إجراء غير معروف', null, 400);
