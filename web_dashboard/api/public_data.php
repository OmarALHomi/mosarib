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

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// Handle GET request (Publicly accessible with lightweight 5-minute cache to protect Firebase Read limits)
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
        $fbPay = firebaseRequest('payment_methods.json');
        if (!empty($fbPay['data']) && is_array($fbPay['data'])) {
            $paymentMethods = $fbPay['data'];
            @file_put_contents($paymentMethodsFile, json_encode($paymentMethods, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        }

        // Fetch live users and devices from Firebase only once every 3 minutes
        $usersRes = firebaseRequest('all_users.json');
        $allUsers = $usersRes['data'] ?: [];
        $activeOperatorsCount = 0;
        $totalRegisteredCount = 0;
        $oneDayAgo = (time() - 86400) * 1000;
        $collectedRates = [];

        if (is_array($allUsers)) {
            $totalRegisteredCount = count($allUsers);
            foreach ($allUsers as $u) {
                $isOnline = (!empty($u['lastSeen']) && $u['lastSeen'] >= $oneDayAgo) ||
                            (!empty($u['lastActiveAt']) && $u['lastActiveAt'] >= $oneDayAgo);
                if ($isOnline && empty($u['isBlocked'])) {
                    $activeOperatorsCount++;
                }

                // Extract real hourly price submitted ONLY by genuine activated subscribers with the full version
                $isActivatedSubscriber = !empty($u['isActivated']) && empty($u['isBlocked']);
                if ($isActivatedSubscriber) {
                    $rate = null;
                    if (isset($u['pricePerHour']) && is_numeric($u['pricePerHour']) && $u['pricePerHour'] > 0) {
                        $rate = floatval($u['pricePerHour']);
                    } elseif (isset($u['hourlyRate']) && is_numeric($u['hourlyRate']) && $u['hourlyRate'] > 0) {
                        $rate = floatval($u['hourlyRate']);
                    }

                    if ($rate !== null && $rate >= 500 && $rate <= 50000) {
                        $collectedRates[] = $rate;
                    }
                }
            }
        }

        $hasRealRates = count($collectedRates) > 0;
        $calculatedAvg = $hasRealRates ? round(array_sum($collectedRates) / count($collectedRates)) : floatval($settings['avg_hourly_price'] ?? 0);
        $calculatedMin = $hasRealRates ? round(min($collectedRates)) : floatval($settings['min_hourly_price'] ?? 0);
        $calculatedMax = $hasRealRates ? round(max($collectedRates)) : floatval($settings['max_hourly_price'] ?? 0);
        $samplesCount = count($collectedRates);

        // Real counts directly from database without any inflated numbers
        // $activeOperatorsCount and $totalRegisteredCount reflect actual database state

        // Save cache to disk
        file_put_contents($cacheFile, json_encode([
            'active_operators_count' => $activeOperatorsCount,
            'total_registered_count' => $totalRegisteredCount,
            'avg' => $calculatedAvg,
            'min' => $calculatedMin,
            'max' => $calculatedMax,
            'has_real_rates' => $hasRealRates,
            'samples_count' => $samplesCount,
            'cached_at' => time()
        ]));
    }

    // Latest download release info
    $releasesFile = __DIR__ . '/../uploads/releases_index.json';
    $releases = file_exists($releasesFile) ? json_decode(file_get_contents($releasesFile), true) : [];
    $latestRelease = !empty($releases) ? $releases[0] : null;

    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $serverUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http') . '://' . $host;
    $defaultDownloadUrl = 'api/download.php';

    // Cloud broadcast check
    $fbBroadcast = firebaseRequest('app_broadcast.json')['data'] ?? null;
    $finalDownloadUrl = !empty($latestRelease['download_url']) ? $latestRelease['download_url'] : ($fbBroadcast['actionUrl'] ?? $defaultDownloadUrl);

    $apkLocalFile = __DIR__ . '/../uploads/Mosarib_Latest.apk';
    $actualApkSizeMb = file_exists($apkLocalFile) ? round(filesize($apkLocalFile) / (1024 * 1024), 2) : 2.78;

    // Portal & Downloads Analytics
    $analyticsFile = __DIR__ . '/../data/portal_analytics.json';
    $analytics = file_exists($analyticsFile) ? json_decode(file_get_contents($analyticsFile), true) : [];

    $portalSettings = array_merge([
        'active_region_name' => 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران، ذمار)',
        'avg_hourly_price' => floatval($calculatedAvg ?: 7500),
        'min_hourly_price' => floatval($calculatedMin ?: 5500),
        'max_hourly_price' => floatval($calculatedMax ?: 9500),
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
        'show_plans_section' => true,
        'show_developer_section' => true,
        'show_download_cta' => true
    ], $settings);

    // If manual override was provided in settings, use it, otherwise fallback to calculated
    if (isset($settings['avg_hourly_price']) && floatval($settings['avg_hourly_price']) > 0) {
        $portalSettings['avg_hourly_price'] = floatval($settings['avg_hourly_price']);
    } else {
        $portalSettings['avg_hourly_price'] = floatval($calculatedAvg ?: 7500);
    }
    if (isset($settings['min_hourly_price']) && floatval($settings['min_hourly_price']) > 0) {
        $portalSettings['min_hourly_price'] = floatval($settings['min_hourly_price']);
    }
    if (isset($settings['max_hourly_price']) && floatval($settings['max_hourly_price']) > 0) {
        $portalSettings['max_hourly_price'] = floatval($settings['max_hourly_price']);
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

// Handle POST request (Requires Admin Authentication to update settings or publish news)
if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $action = $input['action'] ?? ($_GET['action'] ?? '');

    // 0. Public Download Tracking (No Admin Auth Required)
    if ($action === 'track_download') {
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
        jsonResponse(true, 'تم تسجيل تنزيل التطبيق بنجاح', ['total_downloads' => $analytics['total_downloads']]);
    }

    // 0.1 Public Direct Subscription Request from Website (No Admin Auth Required)
    if ($action === 'submit_subscription_request') {
        $name = trim($input['subscriber_name'] ?? ($input['subscriberName'] ?? ''));
        $phone = trim($input['phone'] ?? '');
        $region = trim($input['region'] ?? '');
        $plan = strtoupper(trim($input['plan'] ?? 'YEARLY'));
        if (!in_array($plan, ['YEARLY', 'MONTHLY'])) $plan = 'YEARLY';
        $receipt = trim($input['receipt_number'] ?? ($input['receiptNumber'] ?? ''));
        $paymentMethod = trim($input['payment_method'] ?? ($input['paymentMethod'] ?? ''));
        $deviceCode = trim($input['device_code'] ?? ($input['deviceCode'] ?? ''));

        if (empty($name)) {
            jsonResponse(false, 'يرجى كتابة الاسم الكامل للمشترك');
        }
        if (empty($phone)) {
            jsonResponse(false, 'يرجى إدخال رقم الهاتف للتواصل');
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

        // 2. Local File Backup
        $webRequestsFile = __DIR__ . '/../data/web_subscription_requests.json';
        $existingReqs = file_exists($webRequestsFile) ? (json_decode(file_get_contents($webRequestsFile), true) ?: []) : [];
        array_unshift($existingReqs, $requestData);
        if (count($existingReqs) > 500) $existingReqs = array_slice($existingReqs, 0, 500);
        @file_put_contents($webRequestsFile, json_encode($existingReqs, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));

        jsonResponse(true, 'تم إرسال طلب تفعيل الاشتراك بنجاح! سيتم مراجعة إيداعك وتفعيل حسابك والتواصل معك فوراً.', [
            'request_id' => $reqId,
            'plan' => $plan
        ]);
    }

    require_once __DIR__ . '/auth.php';
    requireAuth();

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

    // 1. Update Water & Market Settings
    if ($action === 'update_settings') {
        $existingSettings = file_exists($settingsFile) ? (json_decode(file_get_contents($settingsFile), true) ?: []) : [];

        $settings = array_merge($existingSettings, [
            'active_region_name' => trim($input['active_region_name'] ?? ($existingSettings['active_region_name'] ?? 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران، ذمار)')),
            'avg_hourly_price' => floatval($input['avg_hourly_price'] ?? ($existingSettings['avg_hourly_price'] ?? 7500)),
            'min_hourly_price' => floatval($input['min_hourly_price'] ?? ($existingSettings['min_hourly_price'] ?? 5500)),
            'max_hourly_price' => floatval($input['max_hourly_price'] ?? ($existingSettings['max_hourly_price'] ?? 9500)),
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
            'show_plans_section' => isset($input['show_plans_section']) ? filter_var($input['show_plans_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_plans_section'] ?? true),
            'show_developer_section' => isset($input['show_developer_section']) ? filter_var($input['show_developer_section'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_developer_section'] ?? true),
            'show_download_cta' => isset($input['show_download_cta']) ? filter_var($input['show_download_cta'], FILTER_VALIDATE_BOOLEAN) : ($existingSettings['show_download_cta'] ?? true)
        ]);

        file_put_contents($settingsFile, json_encode($settings, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        // Clear cache so admin updates show instantly
        $cacheFile = __DIR__ . '/../data/market_cache.json';
        if (file_exists($cacheFile)) @unlink($cacheFile);

        jsonResponse(true, 'تم حفظ وتحديث إعدادات ومحتوى الموقع العام بنجاح!', $settings);
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

    // 4. Save Subscription Pricing & Discounts (Monthly & Yearly)
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
        // Sync to Firebase Realtime Database
        firebaseRequest('subscription_pricing.json', 'PUT', $pricingData);

        jsonResponse(true, 'تم حفظ وتحديث أسعار وخصومات الاشتراكات في فايربيس والموقع والتطبيق بنجاح!', $pricingData);
    }

    // 5. Delete Subscription Request
    if ($action === 'delete_subscription_request') {
        $reqId = $input['request_id'] ?? '';
        if ($reqId) {
            firebaseRequest("subscription_requests/{$reqId}.json", 'DELETE');
            $webRequestsFile = __DIR__ . '/../data/web_subscription_requests.json';
            if (file_exists($webRequestsFile)) {
                $reqs = json_decode(file_get_contents($webRequestsFile), true) ?: [];
                $reqs = array_values(array_filter($reqs, function($r) use ($reqId) {
                    return ($r['id'] ?? '') !== $reqId;
                }));
                file_put_contents($webRequestsFile, json_encode($reqs, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
            }
        }
        jsonResponse(true, 'تم حذف الطلب بنجاح');
    }

    // 6. Clear Completed Requests
    if ($action === 'clear_completed_requests') {
        $webRequestsFile = __DIR__ . '/../data/web_subscription_requests.json';
        if (file_exists($webRequestsFile)) {
            $reqs = json_decode(file_get_contents($webRequestsFile), true) ?: [];
            $reqs = array_values(array_filter($reqs, function($r) {
                return ($r['status'] ?? '') !== 'COMPLETED';
            }));
            file_put_contents($webRequestsFile, json_encode($reqs, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
        }
        jsonResponse(true, 'تم حذف كافة الطلبات المنفذة بنجاح');
    }
}
