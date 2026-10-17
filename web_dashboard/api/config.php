<?php
/**
 * Mosarib Web Dashboard & Management Portal
 * Configuration & Firebase Endpoints
 * Developed for Eng. Omar AL-Homi (TubbaSoft 2026)
 */

// Set error handling
error_reporting(E_ALL);
ini_set('display_errors', 0);

// Default Configuration
define('FIREBASE_PROJECT_ID', 'mosarib');
define('FIREBASE_RTDB_URL', 'https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app');
define('APP_PACKAGE_NAME', 'omarAlhomi.mosarib.com');
define('DEVELOPER_NAME', 'المهندس عمر الحومي');
define('SECRET_ACTIVATION_SALT', 'mosarib_secure_license_secret_key_alhomi_2026_water_app');
define('DEVICE_CODE_SALT', 'msrb_device_token_salt_v1');

// Upload settings
define('UPLOAD_DIR', __DIR__ . '/../uploads/');
define('MAX_UPLOAD_SIZE', 150 * 1024 * 1024); // 150MB for APKs

// Helper for JSON response
function jsonResponse($success, $message, $data = null, $statusCode = 200) {
    if (!headers_sent()) {
        header('Content-Type: application/json; charset=UTF-8');
    }
    http_response_code($statusCode);
    echo json_encode([
        'success' => $success,
        'message' => $message,
        'data' => $data,
        'timestamp' => time()
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// Helper to make HTTPS requests to Firebase Realtime Database
function firebaseRequest($path, $method = 'GET', $data = null, $authSecret = '') {
    $url = rtrim(FIREBASE_RTDB_URL, '/') . '/' . ltrim($path, '/');
    if (!empty($authSecret)) {
        $sep = strpos($url, '?') !== false ? '&' : '?';
        $url .= $sep . 'auth=' . urlencode($authSecret);
    }

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); // For shared hosting compatibility

    $headers = ['Accept: application/json'];
    if ($data !== null) {
        $jsonData = is_string($data) ? $data : json_encode($data);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
        $headers[] = 'Content-Type: application/json; charset=UTF-8';
    }
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        return ['error' => true, 'message' => $error, 'code' => 500];
    }

    return [
        'error' => false,
        'code' => $httpCode,
        'data' => json_decode($response, true),
        'raw' => $response
    ];
}
