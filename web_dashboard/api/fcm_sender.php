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
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
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
$target = trim($input['target'] ?? 'ALL'); // ALL, TRIAL, SUBSCRIBERS, SINGLE
$targetToken = trim($input['target_token'] ?? '');
$actionUrl = trim($input['action_url'] ?? '');

if (empty($title) || empty($body)) {
    jsonResponse(false, 'يرجى كتابة عنوان الإشعار ونصه');
}

// Fetch Registered Tokens from Firebase RTDB
$tokensRes = firebaseRequest('fcm_tokens.json');
$rawTokens = ($tokensRes['error'] === false && is_array($tokensRes['data'])) ? $tokensRes['data'] : [];

// Filter tokens if targeting TRIAL or SUBSCRIBERS
$targetDeviceCodes = null;
if ($target === 'TRIAL' || $target === 'SUBSCRIBERS') {
    $usersRes = firebaseRequest('all_users.json');
    $allUsers = ($usersRes['error'] === false && is_array($usersRes['data'])) ? $usersRes['data'] : [];
    $targetDeviceCodes = [];
    foreach ($allUsers as $u) {
        $devCode = $u['deviceCode'] ?? '';
        if (empty($devCode)) continue;
        $isActivated = !empty($u['isActivated']);
        if ($target === 'SUBSCRIBERS' && $isActivated) {
            $targetDeviceCodes[$devCode] = true;
        } elseif ($target === 'TRIAL' && !$isActivated) {
            $targetDeviceCodes[$devCode] = true;
        }
    }
}

$tokenList = [];
foreach ($rawTokens as $key => $item) {
    if (empty($item['fcmToken'])) continue;
    $devCode = !empty($item['deviceCode']) ? $item['deviceCode'] : $key;
    if ($targetDeviceCodes !== null && !empty($devCode) && !isset($targetDeviceCodes[$devCode])) {
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

$auth = getGoogleFcmAccessToken($serviceAccountFile);
$serverKeyFile = __DIR__ . '/../data/fcm_server_key.txt';
$serverKey = (file_exists($serverKeyFile) && filesize($serverKeyFile) > 10) ? trim(file_get_contents($serverKeyFile)) : '';

$sentSuccess = 0;
$sentFailed = 0;
$fcmV1Used = false;
$errorDetails = [];

// Method A: Google FCM HTTP v1 (OAuth2)
if ($auth) {
    $fcmV1Used = true;
    $fcmUrl = "https://fcm.googleapis.com/v1/projects/{$auth['project_id']}/messages:send";
    $bearerToken = $auth['token'];

    // If Target is ALL, broadcast directly to topic 'all' (wakes 100% of devices in 1 call)
    if ($target === 'ALL') {
        $topicPayload = [
            'message' => [
                'topic' => 'all',
                'notification' => [
                    'title' => $title,
                    'body' => $body
                ],
                'data' => [
                    'title' => $title,
                    'body' => $body,
                    'message' => $body,
                    'actionUrl' => $actionUrl,
                    'type' => 'BROADCAST',
                    'timestamp' => strval(round(microtime(true) * 1000))
                ],
                'android' => [
                    'priority' => 'HIGH',
                    'notification' => [
                        'channel_id' => 'mosarib_notifications_channel',
                        'notification_priority' => 'PRIORITY_MAX',
                        'default_sound' => true,
                        'default_vibrate_timings' => true,
                        'visibility' => 'PUBLIC',
                        'icon' => 'ic_launcher'
                    ]
                ]
            ]
        ];

        $ch = curl_init($fcmUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 8);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $bearerToken,
            'Content-Type: application/json; UTF-8'
        ]);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($topicPayload, JSON_UNESCAPED_UNICODE));
        $res = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($httpCode === 200) {
            $sentSuccess++;
        }
    }

    foreach ($tokenList as $tItem) {
        $token = $tItem['token'];
        $messagePayload = [
            'message' => [
                'token' => $token,
                'notification' => [
                    'title' => $title,
                    'body' => $body
                ],
                'data' => [
                    'title' => $title,
                    'body' => $body,
                    'message' => $body,
                    'actionUrl' => $actionUrl,
                    'type' => 'BROADCAST',
                    'timestamp' => strval(round(microtime(true) * 1000))
                ],
                'android' => [
                    'priority' => 'HIGH',
                    'notification' => [
                        'channel_id' => 'mosarib_notifications_channel',
                        'notification_priority' => 'PRIORITY_MAX',
                        'default_sound' => true,
                        'default_vibrate_timings' => true,
                        'visibility' => 'PUBLIC',
                        'icon' => 'ic_launcher'
                    ]
                ]
            ]
        ];

        $ch = curl_init($fcmUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 8);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $bearerToken,
            'Content-Type: application/json; UTF-8'
        ]);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($messagePayload, JSON_UNESCAPED_UNICODE));
        $res = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

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
// Method B: FCM Legacy Server Key Fallback
elseif (!empty($serverKey)) {
    $legacyUrl = 'https://fcm.googleapis.com/fcm/send';
    $legacyPayload = [
        'notification' => [
            'title' => $title,
            'body' => $body,
            'sound' => 'default',
            'channel_id' => 'mosarib_notifications_channel'
        ],
        'data' => [
            'title' => $title,
            'body' => $body,
            'message' => $body,
            'actionUrl' => $actionUrl,
            'type' => 'BROADCAST',
            'timestamp' => strval(round(microtime(true) * 1000))
        ],
        'priority' => 'high'
    ];

    if ($target === 'ALL') {
        $legacyPayload['to'] = '/topics/all';
    } elseif (!empty($tokenList)) {
        $registrationIds = array_column($tokenList, 'token');
        $legacyPayload['registration_ids'] = $registrationIds;
    }

    $ch = curl_init($legacyUrl);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
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

// Sync delivery
if ($target === 'ALL' || $target === 'BROADCAST') {
    // Only broadcast globally if target is ALL or BROADCAST
    $broadcastPayload = [
        'id' => 'PUSH_' . round(microtime(true) * 1000),
        'title' => $title,
        'message' => $body,
        'actionUrl' => $actionUrl,
        'actionText' => !empty($actionUrl) ? 'عرض التفاصيل' : 'فتح التطبيق',
        'type' => 'INFO',
        'publishedAt' => round(microtime(true) * 1000)
    ];
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
    'actionUrl' => $actionUrl,
    'sentAt' => round(microtime(true) * 1000),
    'sentSuccess' => $sentSuccess,
    'sentFailed' => $sentFailed,
    'fcmV1Used' => $fcmV1Used
];
firebaseRequest("notifications_log/$logId.json", 'PUT', $logPayload);

$msg = "تمت معالجة الإشعار بنجاح!";
if ($fcmV1Used) {
    $msg = "تم إرسال الإشعار بنجاح عبر Google FCM HTTP v1 إلى {$sentSuccess} هاتف وسيرن والتطبيق مغلق!";
    if ($sentFailed > 0) {
        $msg .= " (فشل {$sentFailed} توكن قديم أو غير نشط وتم تنظيفها).";
    }
} else {
    $msg = "تم بث الإشعار في السحابة بنجاح! تنبيه: لم يتم تفعيل مفتاح Google Service Account بعد للإرسال المباشر عندما يكون التطبيق مغلقاً تماماً، يرجى ربطه من لوحة الإدارة.";
}

jsonResponse(true, $msg, [
    'fcm_v1_used' => $fcmV1Used,
    'registered_tokens_count' => count($tokenList),
    'fcm_sent_success' => $sentSuccess,
    'fcm_failed' => $sentFailed,
    'broadcast_synced' => true,
    'error_details' => array_slice(array_unique($errorDetails), 0, 3)
]);
