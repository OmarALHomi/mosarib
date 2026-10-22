<?php
/**
 * Mosarib Firebase Secure Admin Proxy
 * Allows local server (Laragon/XAMPP/Hosting) to query Firebase without CORS or SSL issues
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

require_once __DIR__ . '/config.php';

// Only authenticated admins can use this proxy
if (empty($_SESSION['admin_logged_in'])) {
    jsonResponse(false, 'غير مصرح: يجب تسجيل الدخول للوحة الإدارة أولاً', null, 401);
}

$path = $_GET['path'] ?? '';
if (empty($path)) {
    jsonResponse(false, 'المسار path مطلوب', null, 400);
}

// Sanitization: prevent directory traversal
$path = preg_replace('/(\.\.\/|\.\.\\\\)/', '', $path);
$method = $_SERVER['REQUEST_METHOD'];
if (!empty($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'])) {
    $method = strtoupper($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE']);
} elseif (!empty($_GET['method'])) {
    $method = strtoupper($_GET['method']);
}

$secret = $_GET['auth'] ?? ($_SESSION['mosarib_admin_secret_key'] ?? (defined('FIREBASE_AUTH_SECRET') ? FIREBASE_AUTH_SECRET : ''));

$inputData = null;
if (in_array($method, ['POST', 'PUT', 'PATCH'])) {
    $rawInput = file_get_contents('php://input');
    $inputData = json_decode($rawInput, true);
    if ($inputData === null && !empty($rawInput)) {
        $inputData = $rawInput;
    }
}

$result = firebaseRequest($path, $method, $inputData, $secret);

if (!empty($result['error'])) {
    jsonResponse(false, 'فشل الاتصال بفايربيس: ' . ($result['message'] ?? 'خطأ غير معروف'), null, 500);
}

if ($result['code'] >= 400) {
    $errMsg = 'خطأ من فايربيس (' . $result['code'] . ')';
    if ($result['code'] === 401) {
        $errMsg = 'إذن الوصول مرفوض (Permission denied): يرجى ضبط قواعد فايربيس أو توفير مفتاح الأمان Database Secret';
    }
    jsonResponse(false, $errMsg, $result['data'], $result['code']);
}

jsonResponse(true, 'تم جلب البيانات بنجاح', $result['data']);
