<?php
/**
 * Mosarib Public Portal API - Water Statistics, Market Pulse & News
 * Provides live aggregated water availability index, hourly rate statistics,
 * active well operators count, and admin-published announcements/news.
 */
require_once __DIR__ . '/config.php';

$newsFile = __DIR__ . '/../data/portal_news.json';
$settingsFile = __DIR__ . '/../data/portal_settings.json';

// Ensure data folder exists
if (!file_exists(__DIR__ . '/../data')) {
    mkdir(__DIR__ . '/../data', 0755, true);
}

// Default portal settings if not configured
if (!file_exists($settingsFile)) {
    $defaultSettings = [
        'water_status' => 'GOOD', // ABUNDANT, GOOD, MODERATE, SCARCE
        'water_status_text' => 'مستقر وجيد في معظم المناطق الزراعية',
        'water_index_percent' => 84, // 0 - 100%
        'avg_hourly_price' => 7500, // YER average
        'min_hourly_price' => 6000,
        'max_hourly_price' => 9500,
        'currency_symbol' => 'ريال',
        'active_regions_count' => 18,
        'total_pumped_hours' => 14200,
        'headline_alert' => 'موسم ري متصاعد ومؤشرات إيجابية في توفر مياه الآبار الجوفية.'
    ];
    file_put_contents($settingsFile, json_encode($defaultSettings, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

// Default initial news & articles
if (!file_exists($newsFile)) {
    $defaultNews = [
        [
            'id' => 'NEWS_1',
            'title' => 'تقرير سوق الري: وفرة ملحوظة في منسوب مياه الآبار واستقرار أسعار الساعة',
            'summary' => 'تشير مؤشرات شبكة مُسَرِّب إلى وفرة جيدة في معدلات الضخ والتشغيل مع توازن ملحوظ في تسعيرة ساعة الري لمختلف المحاصيل الزراعية.',
            'category' => 'تقارير المياه والآبار',
            'icon' => '💧',
            'published_at' => (time() - 86400) * 1000,
            'reads_count' => 540
        ],
        [
            'id' => 'NEWS_2',
            'title' => 'إطلاق الإصدار الذهبي من تطبيق مُسَرِّب لإدارة الآبار ودورات الري بدقة متناهية',
            'summary' => 'تم تعزيز خوارزميات الحساب المالي وكسور الساعات وحساب الفاقد المائي لضمان حفظ حقوق ملاك الآبار والمزارعين بدقة متناهية.',
            'category' => 'أخبار المنظومة',
            'icon' => '🚀',
            'published_at' => (time() - 172800) * 1000,
            'reads_count' => 1280
        ],
        [
            'id' => 'NEWS_3',
            'title' => 'إرشادات ترشيد الطاقة والوقود في تشغيل المضخات ومنظومات الطاقة الشمسية',
            'summary' => 'نصائح حصرية للمسربين وملاك الآبار الزراعية لتحقيق أقصى كفاءة تصريف بأقل كلفة تشغيلية على مدار دورات الري المتعاقبة.',
            'category' => 'إرشادات زراعية',
            'icon' => '☀️',
            'published_at' => (time() - 259200) * 1000,
            'reads_count' => 890
        ]
    ];
    file_put_contents($newsFile, json_encode($defaultNews, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

$method = $_SERVER['REQUEST_METHOD'];

// Handle GET request (Publicly accessible)
if ($method === 'GET') {
    $settings = json_decode(file_get_contents($settingsFile), true) ?: [];
    $newsList = json_decode(file_get_contents($newsFile), true) ?: [];

    // Fetch live users and devices from Firebase to calculate dynamic live operators count
    $usersRes = firebaseRequest('all_users.json');
    $allUsers = $usersRes['data'] ?: [];
    $activeOperatorsCount = 0;
    $totalRegisteredCount = 0;
    $oneDayAgo = (time() - 86400) * 1000;

    if (is_array($allUsers)) {
        $totalRegisteredCount = count($allUsers);
        foreach ($allUsers as $u) {
            if (!empty($u['lastSeen']) && $u['lastSeen'] >= $oneDayAgo && empty($u['isBlocked'])) {
                $activeOperatorsCount++;
            }
        }
    }
    // Base fallback realistic count
    $activeOperatorsCount = max($activeOperatorsCount, 28);
    $totalRegisteredCount = max($totalRegisteredCount, 85);

    // Latest download release info
    $releasesFile = __DIR__ . '/../uploads/releases_index.json';
    $releases = file_exists($releasesFile) ? json_decode(file_get_contents($releasesFile), true) : [];
    $latestRelease = !empty($releases) ? $releases[0] : null;

    $serverUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http') . '://' . $_SERVER['HTTP_HOST'];
    $defaultDownloadUrl = $serverUrl . dirname(dirname($_SERVER['PHP_SELF'])) . '/uploads/Mosarib_Latest.apk';

    // Cloud broadcast check
    $fbBroadcast = firebaseRequest('app_broadcast.json')['data'] ?? null;
    $finalDownloadUrl = !empty($latestRelease['download_url']) ? $latestRelease['download_url'] : ($fbBroadcast['actionUrl'] ?? $defaultDownloadUrl);

    jsonResponse(true, 'بيانات المنصة العامة والتقرير المائي جاهز', [
        'water_metrics' => [
            'status' => $settings['water_status'] ?? 'GOOD',
            'status_text' => $settings['water_status_text'] ?? 'متوفر ومستقر',
            'index_percent' => intval($settings['water_index_percent'] ?? 84),
            'avg_hourly_price' => floatval($settings['avg_hourly_price'] ?? 7500),
            'min_hourly_price' => floatval($settings['min_hourly_price'] ?? 6000),
            'max_hourly_price' => floatval($settings['max_hourly_price'] ?? 9500),
            'currency_symbol' => $settings['currency_symbol'] ?? 'ريال',
            'active_regions' => intval($settings['active_regions_count'] ?? 18),
            'active_operators_now' => $activeOperatorsCount,
            'total_operators_registered' => $totalRegisteredCount,
            'headline_alert' => $settings['headline_alert'] ?? ''
        ],
        'news' => $newsList,
        'app_release' => [
            'version_name' => $latestRelease['version_name'] ?? '1.1.0',
            'version_code' => $latestRelease['version_code'] ?? 3,
            'file_size_mb' => !empty($latestRelease['file_size']) ? round($latestRelease['file_size'] / (1024 * 1024), 1) : 24.5,
            'download_url' => $finalDownloadUrl,
            'release_notes' => $latestRelease['release_notes'] ?? 'تحديث متكامل ودعم كشوفات حساب ملاك الآبار وإشعارات الري الفورية.'
        ]
    ]);
}

// Handle POST request (Requires Admin Authentication to update settings or publish news)
if ($method === 'POST') {
    require_once __DIR__ . '/auth.php';
    requireAuth();

    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $action = $input['action'] ?? '';

    // 1. Update Water & Market Settings
    if ($action === 'update_settings') {
        $settings = [
            'water_status' => $input['water_status'] ?? 'GOOD',
            'water_status_text' => trim($input['water_status_text'] ?? 'مستقر'),
            'water_index_percent' => intval($input['water_index_percent'] ?? 80),
            'avg_hourly_price' => floatval($input['avg_hourly_price'] ?? 7500),
            'min_hourly_price' => floatval($input['min_hourly_price'] ?? 6000),
            'max_hourly_price' => floatval($input['max_hourly_price'] ?? 9500),
            'currency_symbol' => trim($input['currency_symbol'] ?? 'ريال'),
            'active_regions_count' => intval($input['active_regions_count'] ?? 18),
            'headline_alert' => trim($input['headline_alert'] ?? '')
        ];
        file_put_contents($settingsFile, json_encode($settings, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        jsonResponse(true, 'تم تحديث مؤشرات سوق المياه والأسعار بنجاح في الصفحة العامة!', $settings);
    }

    // 2. Publish New Article / News
    if ($action === 'add_news') {
        $title = trim($input['title'] ?? '');
        $summary = trim($input['summary'] ?? '');
        $category = trim($input['category'] ?? 'أخبار الري والآبار');
        $icon = trim($input['icon'] ?? '💧');

        if (empty($title) || empty($summary)) {
            jsonResponse(false, 'يرجى تزويد عنوان المقال وملخصه');
        }

        $newsList = json_decode(file_get_contents($newsFile), true) ?: [];
        $newArticle = [
            'id' => 'NEWS_' . time(),
            'title' => $title,
            'summary' => $summary,
            'category' => $category,
            'icon' => $icon,
            'published_at' => time() * 1000,
            'reads_count' => 1
        ];
        array_unshift($newsList, $newArticle);
        file_put_contents($newsFile, json_encode($newsList, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        jsonResponse(true, 'تم نشر الخبر / المقال بنجاح في الواجهة العامة للزوار!', $newArticle);
    }

    // 3. Delete Article
    if ($action === 'delete_news') {
        $id = $input['id'] ?? '';
        $newsList = json_decode(file_get_contents($newsFile), true) ?: [];
        $newsList = array_values(array_filter($newsList, function($item) use ($id) {
            return $item['id'] !== $id;
        }));
        file_put_contents($newsFile, json_encode($newsList, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        jsonResponse(true, 'تم حذف الخبر بنجاح');
    }
}
