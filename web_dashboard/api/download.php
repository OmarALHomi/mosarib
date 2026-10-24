<?php
/**
 * Mosarib APK Direct Download Gateway
 * Dynamically resolves the latest APK release, logs real analytics,
 * and delivers the file cleanly and reliably on shared and cloud hosting.
 */
require_once __DIR__ . '/config.php';

$uploadsDir = __DIR__ . '/../uploads/';
$analyticsFile = __DIR__ . '/../data/portal_analytics.json';
$releasesFile = $uploadsDir . 'releases_index.json';

// 1. Resolve Target APK file
$targetFileName = null;
$targetFilePath = null;

// Strategy A: Check latest indexed release from release manager
if (file_exists($releasesFile)) {
    $releases = json_decode(@file_get_contents($releasesFile), true);
    if (is_array($releases) && !empty($releases)) {
        foreach ($releases as $rel) {
            if (!empty($rel['file_name'])) {
                $checkPath = $uploadsDir . $rel['file_name'];
                if (file_exists($checkPath) && is_file($checkPath) && filesize($checkPath) > 100000) {
                    $targetFileName = $rel['file_name'];
                    $targetFilePath = $checkPath;
                    break;
                }
            }
        }
    }
}

// Strategy B: Check Mosarib_Latest.apk
if (!$targetFilePath && file_exists($uploadsDir . 'Mosarib_Latest.apk') && filesize($uploadsDir . 'Mosarib_Latest.apk') > 100000) {
    $targetFileName = 'Mosarib_Latest.apk';
    $targetFilePath = $uploadsDir . 'Mosarib_Latest.apk';
}

// Strategy C: Scan directory for the newest .apk file
if (!$targetFilePath && is_dir($uploadsDir)) {
    $apkFiles = glob($uploadsDir . '*.apk');
    if (!empty($apkFiles)) {
        // Sort newest first
        usort($apkFiles, function($a, $b) {
            return filemtime($b) - filemtime($a);
        });
        $newest = $apkFiles[0];
        if (filesize($newest) > 100000) {
            $targetFileName = basename($newest);
            $targetFilePath = $newest;
        }
    }
}

// 2. Log Analytics if a file is found
if ($targetFilePath) {
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
        $existing = json_decode(@file_get_contents($analyticsFile), true);
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

    // Redirect directly to the static APK file
    // This allows Apache to stream the file natively with range support, high speed, and 0 memory overhead
    header('Location: ../uploads/' . rawurlencode($targetFileName), true, 302);
    exit;
}

// 3. Fallback: Friendly notification page if no APK exists yet
http_response_code(404);
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>تطبيق مُسَرِّب • جاري تجهيز النسخة</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@600;700;800&display=swap" rel="stylesheet">
  <style>
    body {
      font-family: 'Cairo', system-ui, sans-serif;
      background: #F8FAFC;
      color: #0F172A;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      margin: 0;
      padding: 20px;
      box-sizing: border-box;
      text-align: center;
    }
    .card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 16px;
      padding: 36px 28px;
      max-width: 460px;
      width: 100%;
      box-shadow: 0 10px 25px rgba(0,0,0,0.06);
    }
    .icon {
      font-size: 48px;
      margin-bottom: 16px;
    }
    h1 {
      font-size: 20px;
      font-weight: 800;
      margin-bottom: 10px;
    }
    p {
      color: #64748B;
      font-size: 14px;
      line-height: 1.6;
      margin-bottom: 24px;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 12px 22px;
      border-radius: 10px;
      font-weight: 700;
      font-size: 14px;
      text-decoration: none;
      transition: all 0.2s;
    }
    .btn-primary {
      background: #0D9488;
      color: #FFFFFF;
      margin-bottom: 10px;
      width: 100%;
      box-sizing: border-box;
    }
    .btn-outline {
      background: transparent;
      color: #334155;
      border: 1px solid #CBD5E1;
      width: 100%;
      box-sizing: border-box;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">⏳</div>
    <h1>جاري تجهيز النسخة الأحدث</h1>
    <p>يتم حالياً رفع وتجهيز النسخة الأحدث من تطبيق مُسَرِّب الميداني. يمكنك العودة للصفحة الرئيسية أو التواصل المباشر عبر واتساب لتزويدك برابط مباشر فوراً.</p>
    <a href="https://wa.me/967773712030?text=<?= rawurlencode('السلام عليكم، أود الحصول على رابط تحميل أحدث نسخة من تطبيق مسرب') ?>" class="btn btn-primary" target="_blank">
      <span>تواصل عبر واتساب 💬</span>
    </a>
    <a href="../index.php" class="btn btn-outline">
      <span>العودة للرئيسية</span>
    </a>
  </div>
</body>
</html>
