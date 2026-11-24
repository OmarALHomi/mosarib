<?php
/**
 * Mosarib Firebase Secure Admin Proxy
 * The only way the dashboard reaches Realtime Database: the browser never talks to Firebase
 * directly and never sees FIREBASE_AUTH_SECRET. The response body is Firebase's own JSON, so
 * callers can treat it exactly like a direct REST response.
 */

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth.php';

requireAuth();

if (FIREBASE_AUTH_SECRET === '') {
    jsonResponse(false, 'FIREBASE_AUTH_SECRET غير مضبوط في api/config.local.php على الخادم', null, 500);
}

$path = $_GET['path'] ?? '';
// Database paths only: letters, digits, _ - / and an optional .json suffix.
if ($path === '' || !preg_match('#^[A-Za-z0-9_\-/]+(\.json)?$#', $path) || strpos($path, '//') !== false) {
    jsonResponse(false, 'مسار غير صالح', null, 400);
}
if (substr($path, -5) !== '.json') {
    $path .= '.json';
}

// Allow-list the top-level nodes used by the dashboard
$allowedNodes = [
    'portal_settings', 'subscription_pricing', 'payment_methods', 'fcm_tokens',
    'app_broadcast', 'app_config', 'licenses', 'devices', 'device_overrides',
    'subscription_requests', 'all_users', 'blocked_emails', 'feedbacks'
];
$cleanPath = ltrim($path, '/');
$topNode = explode('/', $cleanPath)[0];
$topNode = preg_replace('/\.json$/', '', $topNode);
if (!in_array($topNode, $allowedNodes, true)) {
    jsonResponse(false, 'مسار غير مصرح به في قاعدة البيانات', null, 403);
}

$requestMethod = $_SERVER['REQUEST_METHOD'];
$method = $requestMethod;
// Strict security: ignore method overrides on GET requests
if ($requestMethod !== 'GET') {
    if (!empty($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'])) {
        $method = strtoupper($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE']);
    } elseif (!empty($_GET['method'])) {
        $method = strtoupper($_GET['method']);
    }
}
if (!in_array($method, ['GET', 'PUT', 'PATCH', 'POST', 'DELETE'], true)) {
    jsonResponse(false, 'طريقة غير مدعومة', null, 405);
}

// Writes ride on the session cookie: enforce CSRF + ADMIN/SUPER_ADMIN role (MODERATOR is read-only)
if ($method !== 'GET') {
    $currentRole = $_SESSION['admin_user']['role'] ?? '';
    if ($currentRole !== 'ADMIN' && $currentRole !== 'SUPER_ADMIN') {
        jsonResponse(false, 'غير مصرح: التعديل مقتصر على المدراء فقط (ADMIN / SUPER_ADMIN)', null, 403);
    }
    if ($method === 'DELETE' && strpos($cleanPath, '/') === false) {
        jsonResponse(false, 'ممنوع حذف العقد الرئيسية بالكامل', null, 403);
    }
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    $csrf = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? ($headers['X-CSRF-Token'] ?? ($headers['x-csrf-token'] ?? ''));
    if (!verifyCsrfToken($csrf)) {
        jsonResponse(false, 'رمز CSRF غير صالح، أعد تحميل الصفحة', null, 403);
    }
}

$inputData = null;
if (in_array($method, ['POST', 'PUT', 'PATCH'], true)) {
    $rawInput = file_get_contents('php://input');
    $inputData = json_decode($rawInput, true);
    if ($inputData === null && trim($rawInput) !== 'null') {
        jsonResponse(false, 'جسم الطلب ليس JSON صالحاً', null, 400);
    }
    // json_decode turns {} into [] and loses the distinction; send the original text instead.
    $inputData = $rawInput;
}

$result = firebaseRequest($path, $method, $inputData);

if (!empty($result['error'])) {
    jsonResponse(false, 'فشل الاتصال بفايربيس: ' . ($result['message'] ?? 'خطأ غير معروف'), null, 502);
}

http_response_code($result['code']);
header('Content-Type: application/json; charset=UTF-8');
echo $result['raw'];
