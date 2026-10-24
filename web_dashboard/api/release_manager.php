<?php
/**
 * Release & APK Upload Manager with Direct Download Link Generator
 * Supports uploading APK files, tracking versions, and setting Force Update
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth.php';

// Strict Security: All release operations require authenticated admin session
requireAuth();

$method = $_SERVER['REQUEST_METHOD'];
$releasesFile = __DIR__ . '/../uploads/releases_index.json';

if (!file_exists(UPLOAD_DIR)) {
    mkdir(UPLOAD_DIR, 0755, true);
}

// 1. GET: Fetch list of releases and current active version
if ($method === 'GET') {
    $releases = file_exists($releasesFile) ? json_decode(file_get_contents($releasesFile), true) : [];
    if (!is_array($releases)) $releases = [];

    // Also get active broadcast / force-update state from Firebase
    $fbBroadcast = firebaseRequest('app_broadcast.json')['data'] ?? null;

    jsonResponse(true, 'تم جلب سجل الإصدارات بنجاح', [
        'releases' => $releases,
        'cloud_broadcast' => $fbBroadcast,
        'base_download_url' => (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http') . '://' . $_SERVER['HTTP_HOST'] . dirname(dirname($_SERVER['PHP_SELF'])) . '/uploads/'
    ]);
}

// 2. POST: Upload new APK release or update cloud force-update
if ($method === 'POST') {
    $action = $_POST['action'] ?? 'upload';

    if ($action === 'upload') {
        if (!isset($_FILES['apk_file']) || $_FILES['apk_file']['error'] !== UPLOAD_ERR_OK) {
            jsonResponse(false, 'لم يتم استلام ملف APK صالح أو حدث خطأ أثناء الرفع');
        }

        $file = $_FILES['apk_file'];
        $versionName = trim($_POST['version_name'] ?? '1.0.0');
        $versionCode = intval($_POST['version_code'] ?? 1);
        $releaseNotes = trim($_POST['release_notes'] ?? '');
        $isForceUpdate = isset($_POST['is_force_update']) && ($_POST['is_force_update'] === '1' || $_POST['is_force_update'] === 'true');

        $ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
        if ($ext !== 'apk') {
            jsonResponse(false, 'الملف المرفوع يجب أن يكون بصيغة APK حصراً (.apk)');
        }

        // Generate clean file name
        $fileName = 'Mosarib_v' . preg_replace('/[^0-9.]/', '', $versionName) . '_code' . $versionCode . '_' . time() . '.apk';
        $destPath = UPLOAD_DIR . $fileName;

        if (!move_uploaded_file($file['tmp_name'], $destPath)) {
            jsonResponse(false, 'فشل نقل وتخزين ملف APK في مجلد التنزيلات');
        }

        // Keep Mosarib_Latest.apk always pointing to the latest uploaded version
        @copy($destPath, UPLOAD_DIR . 'Mosarib_Latest.apk');

        $serverUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http') . '://' . $_SERVER['HTTP_HOST'];
        $downloadUrl = $serverUrl . dirname(dirname($_SERVER['PHP_SELF'])) . '/uploads/' . $fileName;

        $releases = file_exists($releasesFile) ? json_decode(file_get_contents($releasesFile), true) : [];
        if (!is_array($releases)) $releases = [];

        $newRelease = [
            'id' => 'REL_' . time(),
            'version_name' => $versionName,
            'version_code' => $versionCode,
            'release_notes' => $releaseNotes,
            'file_name' => $fileName,
            'file_size' => filesize($destPath),
            'download_url' => $downloadUrl,
            'is_force_update' => $isForceUpdate,
            'uploaded_at' => time() * 1000
        ];

        array_unshift($releases, $newRelease);
        file_put_contents($releasesFile, json_encode($releases, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

        // Sync with Firebase app_broadcast if force update selected
        if ($isForceUpdate) {
            $bcPayload = [
                'id' => 'UPDATE_' . time(),
                'title' => 'تحديث إجباري جديد متاح 🚀',
                'message' => $releaseNotes ?: 'تم إصدار تحديث جديد وهام لإصلاح الأخطاء وتحديث الأنظمة.',
                'actionUrl' => $downloadUrl,
                'actionText' => 'تحميل التحديث الآن (v' . $versionName . ')',
                'minRequiredVersionCode' => $versionCode,
                'isForceUpdate' => true,
                'publishedAt' => round(microtime(true) * 1000)
            ];
            firebaseRequest('app_broadcast.json', 'PUT', $bcPayload);
        }

        jsonResponse(true, 'تم رفع وتثبيت الإصدار الجديد بنجاح وإنشاء رابط التحميل المباشر', [
            'release' => $newRelease,
            'download_url' => $downloadUrl
        ]);
    }

    if ($action === 'set_cloud_link') {
        $customUrl = trim($_POST['custom_url'] ?? '');
        $versionCode = intval($_POST['version_code'] ?? 1);
        $notes = trim($_POST['release_notes'] ?? '');
        $isForce = isset($_POST['is_force_update']) && $_POST['is_force_update'] === '1';

        if (empty($customUrl)) {
            jsonResponse(false, 'يرجى كتابة رابط التحميل المباشر');
        }

        $bcPayload = [
            'id' => 'UPDATE_' . time(),
            'title' => $isForce ? 'تحديث إجباري جديد متاح 🚀' : 'تحديث متوفر لتطبيق مُسَرِّب',
            'message' => $notes ?: 'يتوفر إصدار جديد للتطبيق لتحسين الأداء وتجربة الاستخدام.',
            'actionUrl' => $customUrl,
            'actionText' => 'تحميل التحديث الجديد',
            'minRequiredVersionCode' => $versionCode,
            'isForceUpdate' => $isForce,
            'publishedAt' => round(microtime(true) * 1000)
        ];
        firebaseRequest('app_broadcast.json', 'PUT', $bcPayload);

        jsonResponse(true, 'تم ربط وبث رابط التحميل الخارجي لجميع المستخدمين بنجاح', $bcPayload);
    }
}
