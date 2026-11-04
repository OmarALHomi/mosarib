<?php
/**
 * Mosarib Public Portal API - Water Statistics, Market Pulse & News
 * Provides live aggregated water availability index, hourly rate statistics,
 * active well operators count, and admin-published announcements/news.
 */
require_once __DIR__ . '/config.php';

$newsFile = __DIR__ . '/../data/portal_news.json';
$settingsFile = __DIR__ . '/../data/portal_settings.json';
$paymentMethodsFile = __DIR__ . '/../data/payment_methods.json';
$pricingFile = __DIR__ . '/../data/subscription_pricing.json';

// Ensure data folder exists
if (!file_exists(__DIR__ . '/../data')) {
    mkdir(__DIR__ . '/../data', 0755, true);
}

// Default portal settings if not configured
if (!file_exists($settingsFile)) {
    $defaultSettings = [
        'water_status' => 'GOOD',
        'water_status_text' => 'مستقر وجيد في معظم المناطق الزراعية',
        'water_index_percent' => 84,
        'avg_hourly_price' => 18000,
        'min_hourly_price' => 1500,
        'max_hourly_price' => 25000,
        'currency_symbol' => 'ريال',
        'active_regions_count' => 18,
        'total_pumped_hours' => 14200,
        'headline_alert' => 'موسم ري متصاعد ومؤشرات إيجابية في توفر مياه الآبار الجوفية.',
        'stats_wells' => '',
        'stats_vouchers' => '',
        'testimonials' => []
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

// Helper: Public rate limiting with flock (5 per hour per IP)
function checkPublicRateLimit($key, $limit = 5, $windowSeconds = 3600) {
    $throttleFile = __DIR__ . '/../data/public_throttles.json';
    $fp = @fopen($throttleFile, 'c+');
    if (!$fp) return true;
    if (!flock($fp, LOCK_EX)) {
        fclose($fp);
        return true;
    }
    $content = stream_get_contents($fp);
    $data = json_decode($content, true) ?: [];
    $now = time();

    // Clean expired entries
    foreach ($data as $k => $item) {
        if (($now - ($item['start_time'] ?? 0)) > $windowSeconds) {
            unset($data[$k]);
        }
    }

    if (!isset($data[$key])) {
        $data[$key] = ['count' => 1, 'start_time' => $now];
    } else {
        if ($data[$key]['count'] >= $limit) {
            flock($fp, LOCK_UN);
            fclose($fp);
            jsonResponse(false, 'تم تجاوز الحد المسموح به من الطلبات مؤقتاً، يرجى المحاولة بعد ساعة.', null, 429);
        }
        $data[$key]['count']++;
    }

    ftruncate($fp, 0);
    rewind($fp);
    fwrite($fp, json_encode($data, JSON_UNESCAPED_UNICODE));
    fflush($fp);
    flock($fp, LOCK_UN);
    fclose($fp);
    return true;
}

// Helper: Skip obvious bots
function isBotRequest() {
    $ua = strtolower($_SERVER['HTTP_USER_AGENT'] ?? '');
    if (empty($ua)) return true;
    return (bool)preg_match('/bot|crawl|spider|slurp|facebookexternalhit|bingpreview/i', $ua);
}

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// Handle GET request (Publicly accessible with lightweight 3-minute cache)
if ($method === 'GET') {
    $settings = file_exists($settingsFile) ? (json_decode(file_get_contents($settingsFile), true) ?: []) : [];
    $newsList = file_exists($newsFile) ? (json_decode(file_get_contents($newsFile), true) ?: []) : [];
    $pricing = file_exists($pricingFile) ? (json_decode(file_get_contents($pricingFile), true) ?: []) : [];
    $paymentMethods = file_exists($paymentMethodsFile) ? (json_decode(file_get_contents($paymentMethodsFile), true) ?: []) : [];

    // Cache file for Firebase aggregated rates & counts
    $cacheFile = __DIR__ . '/../data/market_cache.json';
    $cacheLifetime = 180; // 3 minutes cache
    $cachedData = (file_exists($cacheFile) && (time() - filemtime($cacheFile) < $cacheLifetime)) 
        ? json_decode(file_get_contents($cacheFile), true) 
        : null;

    if ($cachedData && is_array($cachedData)) {
        $activeOperatorsCount = $cachedData['active_operators_count'];
        $totalRegisteredCount = $cachedData['total_registered_count'];
        $calculatedAvg = $cachedData['avg'];
        $calculatedMin = $cachedData['min'];
        $calculatedMax = $cachedData['max'];
        $hasRealRates = $cachedData['has_real_rates'];
        $samplesCount = $cachedData['samples_count'];
    } else {
        // Sync Firebase portal_settings & pricing with local storage
        $fbSettings = firebaseRequest('portal_settings.json');
        if (!empty($fbSettings['data']) && is_array($fbSettings['data'])) {
            $settings = array_merge($settings, $fbSettings['data']);
            @file_put_contents($settingsFile, json_encode($settings, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        }

        $fbPricing = firebaseRequest('subscription_pricing.json');
        if (!empty($fbPricing['data']) && is_array($fbPricing['data'])) {
            $pricing = array_merge($pricing, $fbPricing['data']);
            @file_put_contents($pricingFile, json_encode($pricing, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        }

        $fbPayments = firebaseRequest('payment_methods.json');
        if (!empty($fbPayments['data']) && is_array($fbPayments['data'])) {
            $paymentMethods = $fbPayments['data'];
            @file_put_contents($paymentMethodsFile, json_encode($paymentMethods, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        }

        // Fetch users from Firebase for operator count and statistics
        $usersResponse = firebaseRequest('all_users.json');
        $usersData = $usersResponse['data'] ?? [];

        $activeOperatorsCount = 0;
        $totalRegisteredCount = 0;
        $hourlyPrices = [];

        if (is_array($usersData)) {
            $totalRegisteredCount = count($usersData);
            $thirtyDaysAgo = (time() - (30 * 86400)) * 1000;

            foreach ($usersData as $user) {
                if (empty($user) || !is_array($user)) continue;

                $lastSeen = $user['lastActiveAt'] ?? ($user['lastSeen'] ?? ($user['createdAt'] ?? 0));
                if ($lastSeen > $thirtyDaysAgo) {
                    $activeOperatorsCount++;
                }

                $price = $user['pricePerHour'] ?? ($user['hourlyRate'] ?? 0);
                if (is_numeric($price) && $price >= 500 && $price <= 50000) {
                    $hourlyPrices[] = floatval($price);
                }
            }
        }

        $samplesCount = count($hourlyPrices);
        if ($samplesCount >= 3) {
            $calculatedMin = min($hourlyPrices);
            $calculatedMax = max($hourlyPrices);
            $calculatedAvg = round(array_sum($hourlyPrices) / $samplesCount);
            $hasRealRates = true;
        } else {
            $calculatedMin = $settings['min_hourly_price'] ?? 1500;
            $calculatedMax = $settings['max_hourly_price'] ?? 25000;
            $calculatedAvg = $settings['avg_hourly_price'] ?? 18000;
            $hasRealRates = false;
        }

        // Cache results
        $cachePayload = [
            'active_operators_count' => $activeOperatorsCount,
            'total_registered_count' => $totalRegisteredCount,
            'avg' => $calculatedAvg,
            'min' => $calculatedMin,
            'max' => $calculatedMax,
            'has_real_rates' => $hasRealRates,
            'samples_count' => $samplesCount,
            'cached_at' => time()
        ];
        @file_put_contents($cacheFile, json_encode($cachePayload));
    }

    // Load APK Release Information
    $releasesIndexFile = __DIR__ . '/../uploads/releases_index.json';
    $releases = file_exists($releasesIndexFile) ? json_decode(file_get_contents($releasesIndexFile), true) : [];
    $latestRelease = (is_array($releases) && !empty($releases)) ? $releases[0] : null;

    // Check Cloud Broadcast for live force-update or download url
    $fbBroadcast = firebaseRequest('app_broadcast.json')['data'] ?? null;
    $finalDownloadUrl = '';
    if (!empty($fbBroadcast['actionUrl'])) {
        $finalDownloadUrl = $fbBroadcast['actionUrl'];
    } elseif (!empty($latestRelease['download_url'])) {
        $finalDownloadUrl = $latestRelease['download_url'];
    } else {
        $apks = glob(__DIR__ . '/../uploads/*.apk');
        if (!empty($apks)) {
            usort($apks, function($a, $b) { return filemtime($b) - filemtime($a); });
            $serverUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http') . '://' . $_SERVER['HTTP_HOST'];
            $finalDownloadUrl = $serverUrl . dirname(dirname($_SERVER['PHP_SELF'])) . '/uploads/' . basename($apks[0]);
        }
    }

    $actualApkSizeMb = '2.88';
    if (!empty($latestRelease['file_size_mb'])) {
        $actualApkSizeMb = $latestRelease['file_size_mb'];
    } elseif (!empty($latestRelease['file_size'])) {
        $actualApkSizeMb = number_format($latestRelease['file_size'] / (1024 * 1024), 2, '.', '');
    } else {
        $latestApkFile = __DIR__ . '/../uploads/Mosarib_Latest.apk';
        if (file_exists($latestApkFile)) {
            $actualApkSizeMb = number_format(filesize($latestApkFile) / (1024 * 1024), 2, '.', '');
        }
    }

    $analyticsFile = __DIR__ . '/../data/portal_analytics.json';
    $analytics = file_exists($analyticsFile) ? json_decode(file_get_contents($analyticsFile), true) : [];

    $portalSettings = array_merge([
        'active_region_name' => 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران، ذمار)',
        'avg_hourly_price' => floatval($settings['avg_hourly_price'] ?? 18000),
        'min_hourly_price' => floatval($settings['min_hourly_price'] ?? 1500),
        'max_hourly_price' => floatval($settings['max_hourly_price'] ?? 25000),
        'currency_symbol' => 'ريال',
        'rates_note' => 'الأسعار تختلف بحسب عمق البئر، حجم المضخة، ونوع الطاقة المستخدمة (شمسية / ديزل).',
        'water_status' => 'GOOD',
        'water_status_text' => 'مستقر ومتاح في معظم المناطق الزراعية',
        'water_index_percent' => 85,
        'headline_alert' => 'منظومة مُسَرِّب الميدانية: تحديث لحظي لأسعار ساعات الري والتحميل المباشر للنسخة الأحدث.',
        'hero_badge' => 'المنظومة الرقمية المعتمدة لملاك الآبار والموزعين',
        'hero_title' => 'إدارة جلسات السقي وحسابات مياه الآبار بدقة واحترافية',
        'hero_subtitle' => 'نظام ميداني متكامل يعمل بدون إنترنت لموزعي المياه وملاك الآبار الارتوازية: حساب أوقات السقي بالدقائق، إصدار سندات قبض وصرف فورية، وتوليد تقارير مالية PDF ومشاركتها عبر واتساب.',
        'contact_whatsapp' => '967773712030',
        'contact_phone' => '773712030',
        'developer_name' => 'المهندس عمر الحومي',
        'developer_company' => 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)',
        'developer_email' => 'omar@tubbasoft.com',
        'developer_message' => 'تم ابتكار وتطوير منظومة وتطبيق مُسَرِّب لحوكمة مياه الآبار الزراعية وحفظ حقوق الموزعين والمزارعين بأعلى دقة تقنية ومالية.',
        'show_top_ticker' => true,
        'show_rates_section' => true,
        'show_features_section' => true,
        'show_how_section' => true,
        'show_testimonials_section' => true,
        'show_plans_section' => true,
        'show_news_section' => true,
        'show_faq_section' => true,
        'show_developer_section' => true,
        'show_download_cta' => true,
        'stats_wells' => '',
        'stats_vouchers' => '',
        'testimonials' => []
    ], $settings);

    if (isset($settings['avg_hourly_price']) && floatval($settings['avg_hourly_price']) > 0) {
        $portalSettings['avg_hourly_price'] = floatval($settings['avg_hourly_price']);
    }

    jsonResponse(true, 'بيانات المنصة العامة والتقرير المائي جاهز', [
        'portal_settings' => $portalSettings,
        'water_metrics' => [
            'status' => $portalSettings['water_status'],
            'status_text' => $portalSettings['water_status_text'],
            'index_percent' => intval($portalSettings['water_index_percent']),
            'avg_hourly_price' => $portalSettings['avg_hourly_price'],
            'min_hourly_price' => $portalSettings['min_hourly_price'],
            'max_hourly_price' => $portalSettings['max_hourly_price'],
            'currency_symbol' => $portalSettings['currency_symbol'],
            'active_region_name' => $portalSettings['active_region_name'],
            'rates_note' => $portalSettings['rates_note'],
            'active_regions' => intval($settings['active_regions_count'] ?? 18),
            'active_operators_now' => $activeOperatorsCount,
            'total_operators_registered' => $totalRegisteredCount,
            'headline_alert' => $portalSettings['headline_alert']
        ],
        'analytics' => [
            'total_visitors' => intval($analytics['total_visitors'] ?? 0),
            'today_visitors' => intval($analytics['today_visitors'] ?? 0),
            'unique_visitors' => intval($analytics['unique_visitors'] ?? 0),
            'total_downloads' => intval($analytics['total_downloads'] ?? 0),
            'today_downloads' => intval($analytics['today_downloads'] ?? 0),
            'last_visit_at' => intval($analytics['last_visit_at'] ?? 0),
            'last_download_at' => intval($analytics['last_download_at'] ?? 0),
            'daily_stats' => $analytics['daily_stats'] ?? []
        ],
        'developer_info' => [
            'name' => $portalSettings['developer_name'],
            'role' => 'المطور وصاحب مشروع مُسَرِّب (مؤسس TubbaSoft)',
            'company' => $portalSettings['developer_company'],
            'email' => $portalSettings['developer_email'],
            'phone' => $portalSettings['contact_phone'],
            'whatsapp' => $portalSettings['contact_whatsapp'],
            'website' => 'https://tubbasoft.com',
            'support_message' => $portalSettings['developer_message']
        ],
        'news' => $newsList,
        'payment_methods' => file_exists($paymentMethodsFile) ? (json_decode(file_get_contents($paymentMethodsFile), true) ?: []) : [],
        'subscription_pricing' => file_exists($pricingFile) ? (json_decode(file_get_contents($pricingFile), true) ?: [
            'monthly' => ['price' => '10', 'original_price' => '', 'discount_text' => '', 'currency' => '$'],
            'yearly' => ['price' => '80', 'original_price' => '120', 'discount_text' => 'وفر 33% (خصم 40$)', 'currency' => '$']
        ]) : [
            'monthly' => ['price' => '10', 'original_price' => '', 'discount_text' => '', 'currency' => '$'],
            'yearly' => ['price' => '80', 'original_price' => '120', 'discount_text' => 'وفر 33% (خصم 40$)', 'currency' => '$']
        ],
        'app_release' => [
            'version_name' => $latestRelease['version_name'] ?? '1.4.0',
            'version_code' => $latestRelease['version_code'] ?? 4,
            'file_size_mb' => $actualApkSizeMb,
            'download_url' => $finalDownloadUrl,
            'release_notes' => $latestRelease['release_notes'] ?? 'الإصدار الرسمي النهائي لدعم إدارة الآبار وتدقيق ساعات الري وسندات القبض.'
        ]
    ]);
}

// Handle POST request
if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $action = $input['action'] ?? ($_GET['action'] ?? '');

    // 0. Public Download Tracking (No Admin Auth Required, with flock and bot skip)
    if ($action === 'track_download') {
        if (isBotRequest()) {
            jsonResponse(true, 'تم تخطي روبوت البحث', ['total_downloads' => 0]);
        }
        $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
        checkPublicRateLimit('dl_' . $ip, 20, 3600);

        $analyticsFile = __DIR__ . '/../data/portal_analytics.json';
        $today = date('Y-m-d');
        $fp = @fopen($analyticsFile, 'c+');
        if ($fp && flock($fp, LOCK_EX)) {
            $existing = json_decode(stream_get_contents($fp), true) ?: [];
            $analytics = array_merge([
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
            ], $existing);

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

            ftruncate($fp, 0);
            rewind($fp);
            fwrite($fp, json_encode($analytics, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
            fflush($fp);
            flock($fp, LOCK_UN);
            fclose($fp);
            jsonResponse(true, 'تم تسجيل تنزيل التطبيق بنجاح', ['total_downloads' => $analytics['total_downloads']]);
        }
        jsonResponse(false, 'تعذر تحديث إحصائيات التنزيل', null, 500);
    }

    // 0.0 Public Visit Tracking (No Admin Auth Required, ignore client is_unique, flock, bot skip)
    if ($action === 'track_visit') {
        if (isBotRequest()) {
            jsonResponse(true, 'تم تخطي روبوت البحث', ['total_visitors' => 0]);
        }
        $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
        checkPublicRateLimit('vis_' . $ip, 30, 3600);

        $today = date('Y-m-d');
        $cookieName = 'msrb_v_visit_' . str_replace('-', '', $today);
        $isNewVisitorToday = empty($_COOKIE[$cookieName]);
        if ($isNewVisitorToday) {
            @setcookie($cookieName, '1', time() + 86400, '/');
        }

        $analyticsFile = __DIR__ . '/../data/portal_analytics.json';
        $fp = @fopen($analyticsFile, 'c+');
        if ($fp && flock($fp, LOCK_EX)) {
            $existing = json_decode(stream_get_contents($fp), true) ?: [];
            $analytics = array_merge([
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
            ], $existing);

            if (($analytics['last_visit_date'] ?? '') !== $today) {
                $analytics['last_visit_date'] = $today;
                $analytics['today_visitors'] = 0;
            }
            $analytics['total_visitors'] = ($analytics['total_visitors'] ?? 0) + 1;
            $analytics['today_visitors'] = ($analytics['today_visitors'] ?? 0) + 1;
            $analytics['last_visit_at'] = time();
            if ($isNewVisitorToday) {
                $analytics['unique_visitors'] = ($analytics['unique_visitors'] ?? 0) + 1;
            }
            if (!isset($analytics['daily_stats'][$today])) {
                $analytics['daily_stats'][$today] = ['visitors' => 0, 'downloads' => 0];
            }
            $analytics['daily_stats'][$today]['visitors']++;

            ftruncate($fp, 0);
            rewind($fp);
            fwrite($fp, json_encode($analytics, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
            fflush($fp);
            flock($fp, LOCK_UN);
            fclose($fp);
            jsonResponse(true, 'تم تسجيل الزيارة بنجاح', ['total_visitors' => $analytics['total_visitors']]);
        }
        jsonResponse(false, 'تعذر تحديث إحصائيات الزيارات', null, 500);
    }

    // 0.1 Public Direct Subscription Request (No Admin Auth Required, throttled 5/hr, honeypot, sanitized)
    if ($action === 'submit_subscription_request') {
        $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
        checkPublicRateLimit('sub_' . $ip, 5, 3600);

        // Honeypot check: reject spam bots filling hidden field
        if (!empty($input['website'])) {
            jsonResponse(false, 'تعذر معالجة الطلب');
        }

        $name = mb_substr(trim($input['subscriber_name'] ?? ($input['subscriberName'] ?? '')), 0, 80);
        $phone = trim($input['phone'] ?? '');
        $region = mb_substr(trim($input['region'] ?? ''), 0, 80);
        $plan = strtoupper(trim($input['plan'] ?? 'YEARLY'));
        if (!in_array($plan, ['YEARLY', 'MONTHLY'], true)) {
            $plan = 'YEARLY';
        }
        $receipt = mb_substr(trim($input['receipt_number'] ?? ($input['receiptNumber'] ?? '')), 0, 80);
        $paymentMethod = mb_substr(trim($input['payment_method'] ?? ($input['paymentMethod'] ?? '')), 0, 80);
        $deviceCode = mb_substr(trim($input['device_code'] ?? ($input['deviceCode'] ?? '')), 0, 80);

        if (empty($name)) {
            jsonResponse(false, 'يرجى كتابة الاسم الكامل للمشترك');
        }
        if (empty($phone) || !preg_match('/^[0-9+ ]{7,15}$/', $phone)) {
            jsonResponse(false, 'يرجى إدخال رقم هاتف صحيح للتواصل');
        }

        $timestamp = round(microtime(true) * 1000);
        $reqId = 'WEB_' . time() . '_' . substr(md5(uniqid(mt_rand(), true)), 0, 4);

        $requestData = [
            'id' => $reqId,
            'subscriberName' => $name,
            'phone' => $phone,
            'region' => $region ?: 'غير محدد',
            'plan' => $plan,
            'receiptNumber' => $receipt ?: 'قيد التحويل',
            'paymentMethod' => $paymentMethod ?: 'غير محدد',
            'deviceCode' => $deviceCode ?: 'طلب من الموقع',
            'deviceModel' => 'الموقع الرسمي (Web Direct)',
            'androidVersion' => 'Web Portal',
            'requestType' => 'WEB_DIRECT',
            'status' => 'PENDING',
            'timestamp' => $timestamp
        ];

        // 1. Push to Firebase RTDB
        firebaseRequest("subscription_requests/{$reqId}.json", 'PUT', $requestData);

        // 2. Local File Backup under flock; never evict older real requests
        $webRequestsFile = __DIR__ . '/../data/web_subscription_requests.json';
        $fp = @fopen($webRequestsFile, 'c+');
        if ($fp && flock($fp, LOCK_EX)) {
            $existingReqs = json_decode(stream_get_contents($fp), true) ?: [];
            $existingReqs[] = $requestData;
            ftruncate($fp, 0);
            rewind($fp);
            fwrite($fp, json_encode($existingReqs, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
            fflush($fp);
            flock($fp, LOCK_UN);
            fclose($fp);
        }

        jsonResponse(true, 'تم إرسال طلب تفعيل الاشتراك بنجاح! سيتم مراجعة إيداعك وتفعيل حسابك والتواصل معك فوراً.', [
            'request_id' => $reqId,
            'plan' => $plan
        ]);
    }

    // --- Strictly Protected Admin Actions Below ---
    require_once __DIR__ . '/auth.php';
    requireAuth();

    // CSRF verification on every admin write (C2)
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    $csrf = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? ($headers['X-CSRF-Token'] ?? ($headers['x-csrf-token'] ?? ($input['csrf_token'] ?? '')));
    if (!verifyCsrfToken($csrf)) {
        jsonResponse(false, 'رمز أمان الجلسة غير صالح (CSRF)', null, 403);
    }

    // 1. Save / Update Payment Accounts (Admin Only)
    if ($action === 'save_payment_methods') {
        $methods = $input['payment_methods'] ?? [];
        if (!is_array($methods)) $methods = [];

        // Sanitize
        $cleanMethods = [];
        foreach ($methods as $idx => $m) {
            $bankName = trim($m['bank_name'] ?? '');
            $accNum = trim($m['account_number'] ?? '');
            if (empty($bankName) && empty($accNum)) continue;

            $cleanMethods[] = [
                'id' => $m['id'] ?? ('PAY_' . ($idx + 1)),
                'bank_name' => $bankName,
                'account_number' => $accNum,
                'account_currency' => trim($m['account_currency'] ?? 'YER'),
                'account_holder' => trim($m['account_holder'] ?? 'عمر الحومي'),
                'logo_url' => trim($m['logo_url'] ?? ''),
                'notes' => trim($m['notes'] ?? ''),
                'is_active' => isset($m['is_active']) ? filter_var($m['is_active'], FILTER_VALIDATE_BOOLEAN) : true
            ];
        }

        // Save locally
        file_put_contents($paymentMethodsFile, json_encode($cleanMethods, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

        // Push to Firebase Realtime Database
        firebaseRequest('payment_methods.json', 'PUT', $cleanMethods);

        jsonResponse(true, 'تم حفظ وتحديث حسابات وطرق الدفع والتحويل البنكي بنجاح!', $cleanMethods);
    }

    // 2. Update Water & Market Settings (including stats_wells, stats_vouchers, testimonials)
    if ($action === 'update_settings') {
        $existingSettings = file_exists($settingsFile) ? (json_decode(file_get_contents($settingsFile), true) ?: []) : [];

        $settings = array_merge($existingSettings, [
            'active_region_name' => trim($input['active_region_name'] ?? ($existingSettings['active_region_name'] ?? 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران، ذمار)')),
            'avg_hourly_price' => floatval($input['avg_hourly_price'] ?? ($existingSettings['avg_hourly_price'] ?? 18000)),
            'min_hourly_price' => floatval($input['min_hourly_price'] ?? ($existingSettings['min_hourly_price'] ?? 1500)),
            'max_hourly_price' => floatval($input['max_hourly_price'] ?? ($existingSettings['max_hourly_price'] ?? 25000)),
            'currency_symbol' => trim($input['currency_symbol'] ?? ($existingSettings['currency_symbol'] ?? 'ريال')),
            'rates_note' => trim($input['rates_note'] ?? ($existingSettings['rates_note'] ?? 'الأسعار تختلف بحسب عمق البئر، حجم المضخة، ونوع الطاقة المستخدمة (شمسية / ديزل).')),
            
            'water_status' => $input['water_status'] ?? ($existingSettings['water_status'] ?? 'GOOD'),
            'water_status_text' => trim($input['water_status_text'] ?? ($existingSettings['water_status_text'] ?? 'مستقر ومتاح في معظم المناطق الزراعية')),
            'water_index_percent' => intval($input['water_index_percent'] ?? ($existingSettings['water_index_percent'] ?? 85)),
            'active_regions_count' => intval($input['active_regions_count'] ?? ($existingSettings['active_regions_count'] ?? 18)),
            'headline_alert' => trim($input['headline_alert'] ?? ($existingSettings['headline_alert'] ?? '')),

            'hero_badge' => trim($input['hero_badge'] ?? ($existingSettings['hero_badge'] ?? 'المنظومة الرقمية المعتمدة لملاك الآبار والموزعين')),
            'hero_title' => trim($input['hero_title'] ?? ($existingSettings['hero_title'] ?? 'إدارة جلسات السقي وحسابات مياه الآبار بدقة واحترافية')),
            'hero_subtitle' => trim($input['hero_subtitle'] ?? ($existingSettings['hero_subtitle'] ?? 'نظام ميداني متكامل يعمل بدون إنترنت لموزعي المياه وملاك الآبار الارتوازية: حساب أوقات السقي بالدقائق، إصدار سندات قبض وصرف فورية، وتوليد تقارير مالية PDF ومشاركتها عبر واتساب.')),
            
            'contact_whatsapp' => trim($input['contact_whatsapp'] ?? ($existingSettings['contact_whatsapp'] ?? '967773712030')),
            'contact_phone' => trim($input['contact_phone'] ?? ($existingSettings['contact_phone'] ?? '773712030')),
            'developer_name' => trim($input['developer_name'] ?? ($existingSettings['developer_name'] ?? 'المهندس عمر الحومي')),
            'developer_company' => trim($input['developer_company'] ?? ($existingSettings['developer_company'] ?? 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)')),
            'developer_email' => trim($input['developer_email'] ?? ($existingSettings['developer_email'] ?? 'omar@tubbasoft.com')),
            'developer_message' => trim($input['developer_message'] ?? ($existingSettings['developer_message'] ?? 'تم ابتكار وتطوير منظومة وتطبيق مُسَرِّب لحوكمة مياه الآبار الزراعية وحفظ حقوق الموزعين والمزارعين بأعلى دقة تقنية ومالية.')),

            'show_top_ticker' => isset($input['show_top_ticker']) ? filter_var($input['show_top_ticker'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_top_ticker'] ?? true),
            'show_rates_section' => isset($input['show_rates_section']) ? filter_var($input['show_rates_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_rates_section'] ?? true),
            'show_features_section' => isset($input['show_features_section']) ? filter_var($input['show_features_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_features_section'] ?? true),
            'show_how_section' => isset($input['show_how_section']) ? filter_var($input['show_how_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_how_section'] ?? true),
            'show_testimonials_section' => isset($input['show_testimonials_section']) ? filter_var($input['show_testimonials_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_testimonials_section'] ?? true),
            'show_plans_section' => isset($input['show_plans_section']) ? filter_var($input['show_plans_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_plans_section'] ?? true),
            'show_news_section' => isset($input['show_news_section']) ? filter_var($input['show_news_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_news_section'] ?? true),
            'show_faq_section' => isset($input['show_faq_section']) ? filter_var($input['show_faq_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_faq_section'] ?? true),
            'show_developer_section' => isset($input['show_developer_section']) ? filter_var($input['show_developer_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_developer_section'] ?? true),
            'show_download_cta' => isset($input['show_download_cta']) ? filter_var($input['show_download_cta'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_download_cta'] ?? true),

            'stats_wells' => trim($input['stats_wells'] ?? ($existingSettings['stats_wells'] ?? '')),
            'stats_vouchers' => trim($input['stats_vouchers'] ?? ($existingSettings['stats_vouchers'] ?? '')),
            'testimonials' => is_array($input['testimonials'] ?? null) ? $input['testimonials'] : ($existingSettings['testimonials'] ?? [])
        ]);

        file_put_contents($settingsFile, json_encode($settings, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        $cacheFile = __DIR__ . '/../data/market_cache.json';
        if (file_exists($cacheFile)) @unlink($cacheFile);

        jsonResponse(true, 'تم حفظ وتحديث إعدادات ومحتوى الموقع العام بنجاح!', $settings);
    }

    // 3. Publish New Article / News
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

    // 4. Delete Article
    if ($action === 'delete_news') {
        $id = $input['id'] ?? '';
        $newsList = json_decode(file_get_contents($newsFile), true) ?: [];
        $newsList = array_values(array_filter($newsList, function($item) use ($id) {
            return $item['id'] !== $id;
        }));
        file_put_contents($newsFile, json_encode($newsList, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        jsonResponse(true, 'تم حذف الخبر بنجاح');
    }

    // 5. Cancel / Delete Active Broadcast Ad & Force Update
    if ($action === 'cancel_broadcast' || $action === 'delete_broadcast') {
        firebaseRequest('app_broadcast.json', 'PUT', null);
        firebaseRequest('app_broadcast.json', 'DELETE');
        jsonResponse(true, 'تم حذف وإلغاء الإعلان والتحديث من السحابة بنجاح');
    }

    // 6. Save Subscription Pricing & Discounts
    if ($action === 'save_subscription_pricing') {
        $monthlyPrice = trim($input['monthly_price'] ?? '10');
        $monthlyOriginal = trim($input['monthly_original_price'] ?? '');
        $monthlyDiscount = trim($input['monthly_discount_text'] ?? '');
        $monthlyCurrency = trim($input['monthly_currency'] ?? '$');

        $yearlyPrice = trim($input['yearly_price'] ?? '80');
        $yearlyOriginal = trim($input['yearly_original_price'] ?? '120');
        $yearlyDiscount = trim($input['yearly_discount_text'] ?? 'وفر 33% (خصم 40$)');
        $yearlyCurrency = trim($input['yearly_currency'] ?? '$');

        $pricingData = [
            'monthly' => [
                'price' => $monthlyPrice,
                'original_price' => $monthlyOriginal,
                'discount_text' => $monthlyDiscount,
                'currency' => $monthlyCurrency
            ],
            'yearly' => [
                'price' => $yearlyPrice,
                'original_price' => $yearlyOriginal,
                'discount_text' => $yearlyDiscount,
                'currency' => $yearlyCurrency
            ],
            'updated_at' => round(microtime(true) * 1000)
        ];

        file_put_contents($pricingFile, json_encode($pricingData, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        firebaseRequest('subscription_pricing.json', 'PUT', $pricingData);

        jsonResponse(true, 'تم حفظ وتحديث أسعار وخصومات الاشتراكات في فايربيس والموقع والتطبيق بنجاح!', $pricingData);
    }

    // 7. Delete Subscription Request (with Firebase key validation C10)
    if ($action === 'delete_subscription_request') {
        $reqId = $input['request_id'] ?? '';
        $keys = $input['keys'] ?? [];
        if (!is_array($keys)) $keys = [];
        if (!empty($reqId)) $keys[] = $reqId;
        $keys = array_values(array_unique(array_filter($keys)));

        foreach ($keys as $k) {
            $cleanK = trim($k);
            if (!empty($cleanK) && preg_match('/^[A-Za-z0-9_-]+$/', $cleanK)) {
                firebaseRequest("subscription_requests/{$cleanK}.json", 'DELETE');
            }
        }

        $webRequestsFile = __DIR__ . '/../data/web_subscription_requests.json';
        if (file_exists($webRequestsFile)) {
            $fp = @fopen($webRequestsFile, 'c+');
            if ($fp && flock($fp, LOCK_EX)) {
                $reqs = json_decode(stream_get_contents($fp), true) ?: [];
                $reqs = array_values(array_filter($reqs, function($r) use ($keys) {
                    $rId = $r['id'] ?? ($r['requestId'] ?? '');
                    return !in_array($rId, $keys);
                }));
                ftruncate($fp, 0);
                rewind($fp);
                fwrite($fp, json_encode($reqs, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
                fflush($fp);
                flock($fp, LOCK_UN);
                fclose($fp);
            }
        }
        jsonResponse(true, 'تم حذف الطلب نهائياً من السحابة وقاعدة البيانات بنجاح');
    }

    // 8. Clear Completed Requests (with Firebase key validation C10)
    if ($action === 'clear_completed_requests') {
        $keys = $input['keys'] ?? [];
        if (is_array($keys)) {
            foreach ($keys as $k) {
                $cleanK = trim($k);
                if (!empty($cleanK) && preg_match('/^[A-Za-z0-9_-]+$/', $cleanK)) {
                    firebaseRequest("subscription_requests/{$cleanK}.json", 'DELETE');
                }
            }
        }
        $webRequestsFile = __DIR__ . '/../data/web_subscription_requests.json';
        if (file_exists($webRequestsFile)) {
            $fp = @fopen($webRequestsFile, 'c+');
            if ($fp && flock($fp, LOCK_EX)) {
                $reqs = json_decode(stream_get_contents($fp), true) ?: [];
                $reqs = array_values(array_filter($reqs, function($r) {
                    return ($r['status'] ?? '') !== 'COMPLETED';
                }));
                ftruncate($fp, 0);
                rewind($fp);
                fwrite($fp, json_encode($reqs, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
                fflush($fp);
                flock($fp, LOCK_UN);
                fclose($fp);
            }
        }
        jsonResponse(true, 'تم حذف كافة الطلبات المنفذة نهائياً من السحابة بنجاح');
    }

    jsonResponse(false, 'إجراء غير معروف', null, 400);
}
