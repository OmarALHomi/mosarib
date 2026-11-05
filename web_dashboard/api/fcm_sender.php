<?php
/**
 * Real Google FCM HTTP v1 Push Notification Engine (2026 Enterprise Edition)
 * Sends real push notifications to wake devices and display in status bar
 * even when the application is completely closed or killed.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth.php';

// Strict Security: Sending push notifications requires authenticated admin session
requireAuth();

$serviceAccountFile = __DIR__ . '/../data/service_account.json';
$accessTokenCacheFile = __DIR__ . '/../data/fcm_access_token.json';

// Helper: URL-safe Base64 encode
function base64UrlEncode($data) {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

// Helper: Generates Google OAuth2 Access Token from Service Account JSON using pure PHP openssl
function getGoogleFcmAccessToken($saFile) {
    global $accessTokenCacheFile;
    if (!file_exists($saFile)) return null;

    $sa = json_decode(file_get_contents($saFile), true);
    if (!is_array($sa) || empty($sa['client_email']) || empty($sa['private_key'])) {
        return null;
    }

    // Check cached token (valid for 50 minutes)
    if (file_exists($accessTokenCacheFile)) {
        $cached = json_decode(file_get_contents($accessTokenCacheFile), true);
        if (!empty($cached['token']) && !empty($cached['expires_at']) && $cached['expires_at'] > time() + 120) {
            return [
                'token' => $cached['token'],
                'project_id' => $sa['project_id'] ?? FIREBASE_PROJECT_ID
            ];
        }
    }

    $header = base64UrlEncode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $now = time();
    $claims = [
        'iss' => $sa['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600
    ];
    $payload = base64UrlEncode(json_encode($claims));
    $toSign = $header . '.' . $payload;

    $signature = '';
    $privateKey = openssl_pkey_get_private($sa['private_key']);
    if (!$privateKey) return null;
    if (!openssl_sign($toSign, $signature, $privateKey, OPENSSL_ALGO_SHA256)) return null;

    $jwt = $toSign . '.' . base64UrlEncode($signature);

    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ]));
    $res = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode === 200) {
        $data = json_decode($res, true);
        if (!empty($data['access_token'])) {
            @file_put_contents($accessTokenCacheFile, json_encode([
                'token' => $data['access_token'],
                'expires_at' => $now + ($data['expires_in'] ?? 3600)
            ]));
            return [
                'token' => $data['access_token'],
                'project_id' => $sa['project_id'] ?? FIREBASE_PROJECT_ID
            ];
        }
    }

    return null;
}

$method = $_SERVER['REQUEST_METHOD'];

// Handle GET: Check FCM status
if ($method === 'GET') {
    $hasSa = file_exists($serviceAccountFile);
    $serverKeyFile = __DIR__ . '/../data/fcm_server_key.txt';
    $hasServerKey = file_exists($serverKeyFile) && filesize($serverKeyFile) > 10;
    $saDetails = null;
    $isTokenValid = false;
    if ($hasSa) {
        $saRaw = json_decode(file_get_contents($serviceAccountFile), true);
        if (is_array($saRaw) && !empty($saRaw['client_email'])) {
            $saDetails = [
                'project_id' => $saRaw['project_id'] ?? FIREBASE_PROJECT_ID,
                'client_email' => $saRaw['client_email'],
                'private_key_id' => substr($saRaw['private_key_id'] ?? '******', 0, 8) . '...'
            ];
            $auth = getGoogleFcmAccessToken($serviceAccountFile);
            $isTokenValid = ($auth !== null);
        }
    }

    // Get count of registered tokens
    $tokensRes = firebaseRequest('fcm_tokens.json');
    $rawTokens = ($tokensRes['error'] === false && is_array($tokensRes['data'])) ? $tokensRes['data'] : [];
    $tokensCount = count($rawTokens);

    jsonResponse(true, 'حالة محرك الإشعارات', [
        'is_fcm_v1_configured' => ($hasSa && $isTokenValid) || $hasServerKey,
        'has_server_key' => $hasServerKey,
        'service_account' => $saDetails,
        'registered_tokens_count' => $tokensCount,
        'firebase_project' => FIREBASE_PROJECT_ID
    ]);
}

// Handle POST
$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true) ?: $_POST;
$action = $input['action'] ?? $_GET['action'] ?? 'send';

// Every admin write carries the session CSRF token (header from adminApiFetch, or the body field)
$csrfHeaders = function_exists('getallheaders') ? getallheaders() : [];
$csrfToken = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? ($csrfHeaders['X-CSRF-Token'] ?? ($csrfHeaders['x-csrf-token'] ?? ($input['csrf_token'] ?? '')));
if (!verifyCsrfToken($csrfToken)) {
    jsonResponse(false, 'رمز أمان الجلسة غير صالح (CSRF)، أعد تحميل الصفحة', null, 403);
}

// 1.5 Action: Upload a picture for a notification or announcement (task 036)
if ($action === 'upload_image') {
    if (!isset($_FILES['image_file']) || $_FILES['image_file']['error'] !== UPLOAD_ERR_OK) {
        jsonResponse(false, 'لم يتم استلام صورة صالحة');
    }
    $img = $_FILES['image_file'];
    if ($img['size'] > 1024 * 1024 || filesize($img['tmp_name']) > 1024 * 1024) {
        jsonResponse(false, 'حجم الصورة يجب ألا يتجاوز 1 ميجابايت');
    }
    $fh = @fopen($img['tmp_name'], 'rb');
    $head = $fh ? fread($fh, 12) : '';
    if ($fh) fclose($fh);
    $ext = null;
    if (substr($head, 0, 3) === "\xFF\xD8\xFF") $ext = 'jpg';
    elseif (substr($head, 0, 8) === "\x89PNG\r\n\x1A\n") $ext = 'png';
    elseif (substr($head, 0, 4) === 'RIFF' && substr($head, 8, 4) === 'WEBP') $ext = 'webp';
    if ($ext === null) {
        jsonResponse(false, 'الصيغ المسموحة: JPG أو PNG أو WEBP فقط');
    }
    $dir = UPLOAD_DIR . 'notices/';
    if (!is_dir($dir)) @mkdir($dir, 0755, true);
    $name = 'notice_' . bin2hex(random_bytes(12)) . '.' . $ext;
    if (!move_uploaded_file($img['tmp_name'], $dir . $name)) {
        jsonResponse(false, 'تعذر حفظ الصورة على الخادم');
    }
    $base = 'https://' . $_SERVER['HTTP_HOST'] . rtrim(str_replace('\\', '/', dirname(dirname($_SERVER['PHP_SELF']))), '/');
    jsonResponse(true, 'تم رفع الصورة', ['url' => $base . '/uploads/notices/' . $name]);
}

// 1. Action: Save Service Account JSON
if ($action === 'save_service_account') {
    $saContent = trim($input['service_account_json'] ?? '');
    $serverKeyFile = __DIR__ . '/../data/fcm_server_key.txt';

    // Check if uploaded via multipart file
    if (isset($_FILES['service_account_file']) && $_FILES['service_account_file']['error'] === UPLOAD_ERR_OK) {
        $saContent = file_get_contents($_FILES['service_account_file']['tmp_name']);
    }

    if (empty($saContent)) {
        jsonResponse(false, 'يرجى تقديم محتوى ملف Google Service Account JSON أو مفتاح Firebase Server Key');
    }

    // Check if user provided FCM Server Key (Legacy single string)
    if (!str_starts_with($saContent, '{') && strlen($saContent) > 20) {
        @file_put_contents($serverKeyFile, $saContent);
        jsonResponse(true, 'تم حفظ وتفعيل مفتاح Firebase Cloud Messaging Server Key بنجاح! محرك الإشعارات جاهز للإرسال إلى الهواتف المغلقة ✅', [
            'type' => 'SERVER_KEY'
        ]);
    }

    $saData = json_decode($saContent, true);
    if (!is_array($saData) || empty($saData['client_email']) || empty($saData['private_key'])) {
        jsonResponse(false, 'ملف Service Account غير صالح: يجب أن يحتوي على كود JSON الصحيح أو مفتاح السيرفر');
    }

    // Test private key validity
    $testKey = openssl_pkey_get_private($saData['private_key']);
    if (!$testKey) {
        jsonResponse(false, 'المفتاح الخاص (private_key) غير صالح أو تالف');
    }

    // Save to data/service_account.json
    if (@file_put_contents($serviceAccountFile, json_encode($saData, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES)) === false) {
        jsonResponse(false, 'تعذر حفظ ملف Service Account على السيرفر، تحقق من أذونات مجلد data');
    }

    // Invalidate cached token
    if (file_exists($accessTokenCacheFile)) {
        @unlink($accessTokenCacheFile);
    }

    // Test generating access token
    $testAuth = getGoogleFcmAccessToken($serviceAccountFile);
    if (!$testAuth) {
        jsonResponse(false, 'تم حفظ الملف، ولكن تعذر التحقق منه وتوليد رمز الوصول من Google OAuth2. تأكد من صحة بيانات الحساب');
    }

    jsonResponse(true, 'تم حفظ مفتاح Google Service Account بنجاح! محرك FCM HTTP v1 نشط الآن ويرن في الأجهزة المغلقة.', [
        'project_id' => $saData['project_id'] ?? FIREBASE_PROJECT_ID,
        'client_email' => $saData['client_email']
    ]);
}

// 2. Action: Test FCM Connection
if ($action === 'test_connection') {
    $serverKeyFile = __DIR__ . '/../data/fcm_server_key.txt';
    $hasServerKey = file_exists($serverKeyFile) && filesize($serverKeyFile) > 10;
    $auth = getGoogleFcmAccessToken($serviceAccountFile);
    if (!$auth && !$hasServerKey) {
        jsonResponse(false, 'لم يتم تهيئة مفتاح Service Account أو Server Key بعد. يرجى رفع أو إدخال المفتاح أولاً');
    }
    jsonResponse(true, 'الاتصال بـ Google FCM سليم وجاهز للإرسال السحابي الفوري للأجهزة المغلقة ✅', [
        'mode' => $auth ? 'FCM_HTTP_V1' : 'FCM_SERVER_KEY',
        'project_id' => $auth['project_id'] ?? FIREBASE_PROJECT_ID
    ]);
}

// 2.5 Action: Cancel / Delete Active Broadcast Ad
if ($action === 'cancel_broadcast' || $action === 'delete_broadcast') {
    firebaseRequest('app_broadcast.json', 'PUT', null);
    firebaseRequest('app_broadcast.json', 'DELETE');
    jsonResponse(true, 'تم حذف وإلغاء الإعلان والتحديث من السحابة بنجاح');
}

// 3. Action: Send Notification to Devices
$title = trim($input['title'] ?? '');
$body = trim($input['body'] ?? '');
$target = trim($input['target'] ?? 'ALL'); // ALL, TRIAL, SUBSCRIBERS, VERSION, DEVICE, SINGLE
$targetToken = trim($input['target_token'] ?? '');
$actionUrl = trim($input['action_url'] ?? '');
$imageUrl = trim($input['image_url'] ?? '');
$openInApp = !empty($input['open_in_app']);
$dryRun = !empty($input['dry_run']);

if (!$dryRun && (empty($title) || empty($body))) {
    jsonResponse(false, 'يرجى كتابة عنوان الإشعار ونصه');
}
// Links: https:// opens the browser, mosarib:// opens a screen in the app
if ($actionUrl !== '' && !preg_match('#^(https://|mosarib://)#i', $actionUrl)) {
    jsonResponse(false, 'الرابط يجب أن يبدأ بـ https:// أو mosarib://');
}
if ($imageUrl !== '' && stripos($imageUrl, 'https://') !== 0) {
    jsonResponse(false, 'رابط الصورة يجب أن يبدأ بـ https://');
}

// Version filter (task 036): EQ = exact build, LT = older than, RANGE = from..to
$versionMode = strtoupper(trim($input['version_mode'] ?? ''));
$versionFrom = intval($input['version_from'] ?? 0);
$versionTo = intval($input['version_to'] ?? 0);
$versionRange = null; // [min, max] build numbers, 0 = open
if ($target === 'VERSION') {
    if ($versionMode === 'EQ' && $versionFrom > 0) $versionRange = [$versionFrom, $versionFrom];
    elseif ($versionMode === 'LT' && $versionFrom > 1) $versionRange = [0, $versionFrom - 1];
    elseif ($versionMode === 'RANGE' && $versionFrom > 0 && $versionTo >= $versionFrom) $versionRange = [$versionFrom, $versionTo];
    else jsonResponse(false, 'حدد رقم الإصدار (رقم البناء) بشكل صحيح');
}

function versionInRange($code, $range) {
    if ($range === null) return true;
    $code = intval($code);
    if ($code <= 0) return false; // unknown build: not targeted
    if ($range[0] > 0 && $code < $range[0]) return false;
    if ($range[1] > 0 && $code > $range[1]) return false;
    return true;
}

// Fetch Registered Tokens from Firebase RTDB
$tokensRes = firebaseRequest('fcm_tokens.json');
$rawTokens = ($tokensRes['error'] === false && is_array($tokensRes['data'])) ? $tokensRes['data'] : [];

// Audience filter by plan or by app build (the build number comes from all_users/{key}/versionCode)
$targetDeviceKeys = null;
if ($target === 'TRIAL' || $target === 'SUBSCRIBERS' || $target === 'VERSION') {
    $usersRes = firebaseRequest('all_users.json');
    $allUsers = ($usersRes['error'] === false && is_array($usersRes['data'])) ? $usersRes['data'] : [];
    $targetDeviceKeys = [];
    foreach ($allUsers as $key => $u) {
        if (!is_array($u)) continue;
        $devKey = strtoupper(str_replace('-', '', $u['cleanDeviceKey'] ?? ($u['deviceCode'] ?? $key)));
        if ($devKey === '') continue;
        $isActivated = !empty($u['isActivated']);
        $ok = ($target === 'SUBSCRIBERS' && $isActivated)
            || ($target === 'TRIAL' && !$isActivated)
            || ($target === 'VERSION' && versionInRange($u['versionCode'] ?? 0, $versionRange));
        if ($ok) $targetDeviceKeys[$devKey] = true;
    }
}

$tokenList = [];
foreach ($rawTokens as $key => $item) {
    if (empty($item['fcmToken'])) continue;
    $devCode = !empty($item['deviceCode']) ? $item['deviceCode'] : $key;
    $devKey = strtoupper(str_replace('-', '', $devCode));
    if ($targetDeviceKeys !== null && !isset($targetDeviceKeys[$devKey])) {
        continue;
    }
    $tokenList[] = [
        'token' => $item['fcmToken'],
        'deviceCode' => $devCode,
        'key' => $key
    ];
}

if ($target === 'SINGLE' && !empty($targetToken)) {
    $tokenList = [['token' => $targetToken, 'deviceCode' => 'SINGLE']];
}

$targetDeviceCode = trim($input['target_device_code'] ?? '');
$targetDeviceCodeClean = !empty($targetDeviceCode) ? strtoupper(str_replace('-', '', $targetDeviceCode)) : '';
if ($target === 'DEVICE' && !empty($targetDeviceCode)) {
    $filtered = [];
    foreach ($tokenList as $t) {
        $devClean = strtoupper(str_replace('-', '', $t['deviceCode'] ?? ''));
        $keyClean = strtoupper(str_replace('-', '', $t['key'] ?? ''));
        if ($devClean === $targetDeviceCodeClean || $keyClean === $targetDeviceCodeClean || ($t['deviceCode'] ?? '') === $targetDeviceCode) {
            $filtered[] = $t;
        }
    }
    $tokenList = $filtered;
    if (empty($tokenList) && !empty($targetToken)) {
        $tokenList = [['token' => $targetToken, 'deviceCode' => $targetDeviceCode]];
    }
}

// "سيصل إلى n مستخدم" preview before sending
if ($dryRun) {
    $audience = ($target === 'ALL') ? count($rawTokens) : count($tokenList);
    jsonResponse(true, 'عدد الأجهزة المستهدفة', ['audience' => $audience]);
}

// Data-only message: the app always builds the notification itself, so a tap opens the link
// (browser for https, a screen for mosarib://) and the picture is shown. A "notification" block
// lets Android show it alone while the app is closed, and then a tap only opens the app.
$dataPayload = [
    'title' => $title,
    'body' => $body,
    'message' => $body,
    'actionUrl' => $actionUrl,
    'imageUrl' => $imageUrl,
    'openInApp' => $openInApp ? 'true' : 'false',
    'type' => 'BROADCAST',
    'timestamp' => strval(round(microtime(true) * 1000))
];

$auth = getGoogleFcmAccessToken($serviceAccountFile);
$serverKeyFile = __DIR__ . '/../data/fcm_server_key.txt';
$serverKey = (file_exists($serverKeyFile) && filesize($serverKeyFile) > 10) ? trim(file_get_contents($serverKeyFile)) : '';

$sentSuccess = 0;
$sentFailed = 0;
$fcmV1Used = false;
$errorDetails = [];

function fcmV1Post($fcmUrl, $bearerToken, $message) {
    $ch = curl_init($fcmUrl);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 8);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $bearerToken,
        'Content-Type: application/json; UTF-8'
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(['message' => $message], JSON_UNESCAPED_UNICODE));
    $res = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return [$httpCode, $res];
}

// Method A: Google FCM HTTP v1 (OAuth2)
if ($auth) {
    $fcmV1Used = true;
    $fcmUrl = "https://fcm.googleapis.com/v1/projects/{$auth['project_id']}/messages:send";
    $bearerToken = $auth['token'];

    // ALL goes to the 'all' topic in one call; per-token sending is only its fallback,
    // so no phone gets the same notification twice.
    $topicSent = false;
    if ($target === 'ALL') {
        [$httpCode, $res] = fcmV1Post($fcmUrl, $bearerToken, [
            'topic' => 'all',
            'data' => $dataPayload,
            'android' => ['priority' => 'HIGH']
        ]);
        if ($httpCode === 200) {
            $sentSuccess++;
            $topicSent = true;
        }
    }

    if (!$topicSent) {
        foreach ($tokenList as $tItem) {
            [$httpCode, $res] = fcmV1Post($fcmUrl, $bearerToken, [
                'token' => $tItem['token'],
                'data' => $dataPayload,
                'android' => ['priority' => 'HIGH']
            ]);
            if ($httpCode === 200) {
                $sentSuccess++;
            } else {
                $sentFailed++;
                $resJson = json_decode($res, true);
                $errCode = $resJson['error']['details'][0]['errorCode'] ?? $resJson['error']['message'] ?? "HTTP $httpCode";
                $errorDetails[] = $errCode;
                // If token is unregistered or invalid, clean it up from RTDB
                if (in_array($errCode, ['UNREGISTERED', 'INVALID_ARGUMENT']) && !empty($tItem['deviceCode'])) {
                    $cleanKey = str_replace('-', '', trim($tItem['deviceCode']));
                    firebaseRequest("fcm_tokens/$cleanKey.json", 'DELETE');
                }
            }
        }
    }
}
// Method B: FCM Legacy Server Key Fallback
elseif (!empty($serverKey)) {
    $legacyPayload = [
        'data' => $dataPayload,
        'priority' => 'high'
    ];
    if ($target === 'ALL') {
        $legacyPayload['to'] = '/topics/all';
    } elseif (!empty($tokenList)) {
        $legacyPayload['registration_ids'] = array_column($tokenList, 'token');
    }

    $ch = curl_init('https://fcm.googleapis.com/fcm/send');
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: key=' . $serverKey,
        'Content-Type: application/json; UTF-8'
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($legacyPayload, JSON_UNESCAPED_UNICODE));
    $res = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($httpCode === 200) {
        $sentSuccess++;
    }
}

// Sync delivery: the same message as an in-app announcement for ALL / BROADCAST / VERSION
if ($target === 'ALL' || $target === 'BROADCAST' || $target === 'VERSION') {
    $broadcastPayload = [
        'id' => 'PUSH_' . round(microtime(true) * 1000),
        'title' => $title,
        'message' => $body,
        'actionUrl' => $actionUrl,
        'actionText' => !empty($actionUrl) ? 'عرض التفاصيل' : 'فتح التطبيق',
        'type' => 'INFO',
        'publishedAt' => round(microtime(true) * 1000)
    ];
    if ($imageUrl !== '') $broadcastPayload['imageUrl'] = $imageUrl;
    if ($versionRange !== null) {
        if ($versionRange[0] > 0) $broadcastPayload['targetMinVersionCode'] = $versionRange[0];
        if ($versionRange[1] > 0) $broadcastPayload['targetMaxVersionCode'] = $versionRange[1];
    }
    firebaseRequest('app_broadcast.json', 'PUT', $broadcastPayload);
} elseif ($target === 'DEVICE' && !empty($targetDeviceCodeClean)) {
    // Targeted directly to this device: store in device_overrides & all_users so it shows in app
    $devMsgId = 'push_' . round(microtime(true) * 1000);
    $devMsgPayload = [
        'title' => $title,
        'text' => $body,
        'type' => 'important',
        'displays' => '1',
        'active' => true,
        'createdAt' => round(microtime(true) * 1000),
        'actionUrl' => $actionUrl
    ];
    firebaseRequest("device_overrides/{$targetDeviceCodeClean}/messages/{$devMsgId}.json", 'PUT', $devMsgPayload);
    firebaseRequest("device_overrides/{$targetDeviceCodeClean}/specialWarningMessage.json", 'PUT', $body);
    firebaseRequest("all_users/{$targetDeviceCodeClean}/specialWarningMessage.json", 'PUT', $body);
}

// Log to notifications history
$logId = 'LOG_' . round(microtime(true) * 1000);
$logPayload = [
    'id' => $logId,
    'title' => $title,
    'body' => $body,
    'target' => $target,
    'versionRange' => $versionRange,
    'actionUrl' => $actionUrl,
    'imageUrl' => $imageUrl,
    'sentAt' => round(microtime(true) * 1000),
    'sentSuccess' => $sentSuccess,
    'sentFailed' => $sentFailed,
    'fcmV1Used' => $fcmV1Used
];
firebaseRequest("notifications_log/$logId.json", 'PUT', $logPayload);

$audienceCount = ($target === 'ALL') ? count($rawTokens) : count($tokenList);
$msg = "تمت معالجة الإشعار بنجاح!";
if ($fcmV1Used) {
    $msg = "تم إرسال الإشعار بنجاح عبر Google FCM إلى {$audienceCount} جهاز وسيرن والتطبيق مغلق!";
    if ($sentFailed > 0) {
        $msg .= " (فشل {$sentFailed} توكن قديم أو غير نشط وتم تنظيفها).";
    }
} else {
    $msg = "تم بث الإشعار في السحابة بنجاح! تنبيه: لم يتم تفعيل مفتاح Google Service Account بعد للإرسال المباشر عندما يكون التطبيق مغلقاً تماماً، يرجى ربطه من لوحة الإدارة.";
}

jsonResponse(true, $msg, [
    'fcm_v1_used' => $fcmV1Used,
    'registered_tokens_count' => $audienceCount,
    'fcm_sent_success' => $sentSuccess,
    'fcm_failed' => $sentFailed,
    'broadcast_synced' => true,
    'error_details' => array_slice(array_unique($errorDetails), 0, 3)
]);
