<?php
/**
 * Direct APK Download Gateway with automatic metric tracking
 * Records real downloads in data/portal_analytics.json before serving Mosarib_Latest.apk
 */
require_once __DIR__ . '/config.php';

$analyticsFile = __DIR__ . '/../data/portal_analytics.json';
$today = date('Y-m-d');

$analytics = [
    'total_visitors' => 0,
    'today_visitors' => 0,
    'unique_visitors' => 0,
    'last_visit_date' => $today,
    'last_visit_at' => 0,
    'total_downloads' => 0,
    'today_downloads' => 0,
    'last_download_date' => $today,
    'last_download_at' => 0,
    'daily_stats' => []
];

if (file_exists($analyticsFile)) {
    $existing = json_decode(file_get_contents($analyticsFile), true);
    if (is_array($existing)) {
        $analytics = array_merge($analytics, $existing);
    }
}

if (($analytics['last_download_date'] ?? '') !== $today) {
    $analytics['last_download_date'] = $today;
    $analytics['today_downloads'] = 0;
}

$analytics['total_downloads']++;
$analytics['today_downloads']++;
$analytics['last_download_at'] = time();

if (!isset($analytics['daily_stats'][$today])) {
    $analytics['daily_stats'][$today] = ['visitors' => 0, 'downloads' => 0];
}
$analytics['daily_stats'][$today]['downloads']++;

@file_put_contents($analyticsFile, json_encode($analytics, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

// Determine APK file to deliver
$apkPath = __DIR__ . '/../uploads/Mosarib_Latest.apk';
if (file_exists($apkPath)) {
    header('Content-Description: File Transfer');
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Disposition: attachment; filename="Mosarib_Latest.apk"');
    header('Content-Length: ' . filesize($apkPath));
    header('Cache-Control: no-cache, must-revalidate');
    header('Pragma: no-cache');
    header('Expires: 0');
    readfile($apkPath);
    exit;
} else {
    // Fallback to relative redirect
    header('Location: ../uploads/Mosarib_Latest.apk');
    exit;
}
