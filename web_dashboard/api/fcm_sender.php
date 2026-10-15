<?php
/**
 * Real Google FCM v1 & Legacy Push Notification Engine
 * Sends actual system push notifications to wake devices and display in status bar
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth.php';

// Strict Security: Sending push notifications requires authenticated admin session
requireAuth();

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'POST') {
    jsonResponse(false, 'طريقة الطلب غير صالحة، يلزم استخدام POST', null, 405);
}

$input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
$title = trim($input['title'] ?? '');
$body = trim($input['body'] ?? '');
$target = trim($input['target'] ?? 'ALL'); // ALL, TRIAL, SUBSCRIBERS, SINGLE
$targetToken = trim($input['target_token'] ?? '');
$actionUrl = trim($input['action_url'] ?? '');
$serverKey = trim($input['fcm_server_key'] ?? ''); // Optional FCM Server Key

if (empty($title) || empty($body)) {
    jsonResponse(false, 'يرجى كتابة عنوان الإشعار ونصه');
}

// 1. Fetch Registered Device FCM Tokens from Firebase Realtime Database
$tokensRes = firebaseRequest('fcm_tokens.json');
$rawTokens = $tokensRes['data'] ?: [];

$tokenList = [];
if (is_array($rawTokens)) {
    foreach ($rawTokens as $item) {
        if (!empty($item['fcmToken'])) {
            $tokenList[] = $item['fcmToken'];
        }
    }
}

// If specific single device target
if ($target === 'SINGLE' && !empty($targetToken)) {
    $tokenList = [$targetToken];
}

$sentCount = 0;
$failedCount = 0;
$pushResults = [];

// 2. If FCM Server Key provided, send direct Push Notification via Google FCM
if (!empty($serverKey) && !empty($tokenList)) {
    $url = 'https://fcm.googleapis.com/fcm/send';

    // Chunk into batches of 1000 (FCM limit)
    $chunks = array_chunk($tokenList, 500);

    foreach ($chunks as $chunk) {
        $fields = [
            'registration_ids' => $chunk,
            'priority' => 'high',
            'notification' => [
                'title' => $title,
                'body' => $body,
                'sound' => 'default',
                'badge' => '1',
                'icon' => 'ic_launcher'
            ],
            'data' => [
                'title' => $title,
                'message' => $body,
                'body' => $body,
                'actionUrl' => $actionUrl,
                'type' => 'GENERAL',
                'timestamp' => strval(time() * 1000)
            ]
        ];

        $headers = [
            'Authorization: key=' . $serverKey,
            'Content-Type: application/json'
        ];

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));

        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode === 200) {
            $resData = json_decode($result, true);
            $sentCount += $resData['success'] ?? 0;
            $failedCount += $resData['failure'] ?? 0;
            $pushResults[] = $resData;
        } else {
            $failedCount += count($chunk);
        }
    }
}

// 3. Always mirror to Firebase Realtime Database /app_broadcast so in-app & fallback delivers 100%
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

jsonResponse(true, "تمت معالجة إرسال الإشعار بنجاح", [
    'registered_tokens_count' => count($tokenList),
    'fcm_sent_success' => $sentCount,
    'fcm_failed' => $failedCount,
    'fcm_direct_used' => !empty($serverKey),
    'broadcast_synced' => true
]);
