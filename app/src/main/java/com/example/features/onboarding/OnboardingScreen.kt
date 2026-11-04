package com.example.features.onboarding

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.license.FirebaseLicenseService
import com.example.core.license.LicenseManager
import com.example.core.license.UserDeviceTracker
import com.example.core.ui.GoogleBackupAccountDialog
import com.example.core.util.GoogleDriveBackupHelper
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.MosaribTheme
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val pills: List<String>,
    val voiceExample: String? = null
)

val DefaultOnboardingPages = listOf(
    OnboardingPageData(
        title = "سقي محسوب بالدقيقة",
        subtitle = "تسجيل دقيق لدورات السقي واحتساب التكلفة تلقائياً لكل مزارع.",
        icon = Icons.Default.WaterDrop,
        gradientColors = listOf(PrimaryTeal, SecondaryAqua),
        pills = listOf("عداد دقيق", "سعر الساعة", "من/إلى")
    ),
    OnboardingPageData(
        title = "سندات وحسابات واضحة",
        subtitle = "تحصيل فوري بمستندات مالية موثقة وقيد النفقات ومتابعة الديون.",
        icon = Icons.Default.Payments,
        gradientColors = listOf(AccentEmerald, Color(0xFF059669)),
        pills = listOf("قبض", "صرف", "خصم")
    ),
    OnboardingPageData(
        title = "قل العملية بصوتك",
        subtitle = "تحدث بطبيعتك دون ترتيب محدد وسيتولى التطبيق ملء العملية بالكامل.",
        icon = Icons.Default.Mic,
        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
        pills = listOf("إدخال صوتي", "قراءة بالصوت", "مجاني"),
        voiceExample = "«مع احمد علي ساعتين من 10 الف ومسلم 5 الف»"
    ),
    OnboardingPageData(
        title = "كشوف \u2068PDF\u2069 وواتساب",
        subtitle = "استخرج كشوفات حساب تفصيلية وإجمالية بصيغة \u2068PDF\u2069 جاهزة للطباعة والمشاركة.",
        icon = Icons.Default.Assessment,
        gradientColors = listOf(AccentGold, Color(0xFFD97706)),
        pills = listOf("كشف حساب", "سند", "تقارير")
    ),
    OnboardingPageData(
        title = "بياناتك وحساباتك محفوظة",
        subtitle = "يعمل في أعمق الحقول بدون شبكة مع نسخ احتياطي محلي وسحابي.",
        icon = Icons.Default.CloudDone,
        gradientColors = listOf(Color(0xFF16A34A), Color(0xFF0D9488)),
        pills = listOf("بدون إنترنت", "نسخ على الهاتف", "\u2068Google Drive\u2069")
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var signedInGoogleAccount by remember {
        mutableStateOf(GoogleDriveBackupHelper.getSignedInAccount(context))
    }

    var showGoogleBackupDialog by remember { mutableStateOf(false) }
    var isLoginFlow by remember { mutableStateOf(false) }
    var isCheckingGoogleLogin by remember { mutableStateOf(false) }
    var showNotRegisteredDialog by remember { mutableStateOf(false) }
    var unregisteredEmailAttempt by remember { mutableStateOf("") }

    fun completeOnboarding() {
        LicenseManager.setOnboardingCompleted(context, true)
        onFinish()
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                if (isLoginFlow) {
                    isCheckingGoogleLogin = true
                    val email = account.email ?: ""
                    scope.launch {
                        val checkRes = FirebaseLicenseService.findExistingUserByGoogleEmail(email)
                        isCheckingGoogleLogin = false
                        when (checkRes) {
                            is FirebaseLicenseService.ExistingUserResult.Found -> {
                                UserDeviceTracker.saveUserRegistrationInfo(context, checkRes.subscriberName, checkRes.phone)
                                UserDeviceTracker.setGoogleAccountAuthenticatedForApp(context, true)
                                signedInGoogleAccount = account
                                Toast.makeText(context, "أهلاً بك مجدداً ${checkRes.subscriberName}. تم استعادة حسابك بنجاح.", Toast.LENGTH_LONG).show()
                                completeOnboarding()
                            }
                            is FirebaseLicenseService.ExistingUserResult.NotFound -> {
                                val client = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                                client.signOut()
                                UserDeviceTracker.setGoogleAccountAuthenticatedForApp(context, false)
                                signedInGoogleAccount = null
                                unregisteredEmailAttempt = email
                                showNotRegisteredDialog = true
                            }
                            is FirebaseLicenseService.ExistingUserResult.Error -> {
                                Toast.makeText(context, checkRes.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    signedInGoogleAccount = account
                    UserDeviceTracker.setGoogleAccountAuthenticatedForApp(context, true)
                    scope.launch {
                        UserDeviceTracker.syncPresence(context, 0)
                    }
                }
            }
        } catch (_: Exception) {
            isCheckingGoogleLogin = false
        }
    }

    val pagerState = rememberPagerState(initialPage = 0) { DefaultOnboardingPages.size }

    OnboardingContent(
        pagerState = pagerState,
        pages = DefaultOnboardingPages,
        signedInGoogleAccount = signedInGoogleAccount,
        isCheckingGoogleLogin = isCheckingGoogleLogin,
        onSkip = { completeOnboarding() },
        onNext = {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        },
        onStart = { completeOnboarding() },
        onGoogleSignIn = {
            isLoginFlow = true
            val client = GoogleDriveBackupHelper.getGoogleSignInClient(context)
            googleSignInLauncher.launch(client.signInIntent)
        },
        onNewRegistration = {
            isLoginFlow = false
            showGoogleBackupDialog = true
        }
    )

    if (showGoogleBackupDialog) {
        val initialName = UserDeviceTracker.getRegisteredName(context).takeIf { it.isNotBlank() }
            ?: LicenseManager.getActivationRecord(context)?.subscriberName
            ?: ""

        val initialPhone = UserDeviceTracker.getRegisteredPhone(context).takeIf { it.isNotBlank() }
            ?: ""

        GoogleBackupAccountDialog(
            initialName = initialName,
            initialPhone = initialPhone,
            currentGoogleAccount = signedInGoogleAccount,
            onDismiss = { showGoogleBackupDialog = false },
            onProceed = { name, phone, _ ->
                showGoogleBackupDialog = false
                UserDeviceTracker.saveUserRegistrationInfo(context, name, phone)
                val client = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                googleSignInLauncher.launch(client.signInIntent)
            }
        )
    }

    if (showNotRegisteredDialog) {
        AlertDialog(
            onDismissRequest = { showNotRegisteredDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "لا يوجد حساب مسجل مسبقاً",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "لا يوجد حساب أو اشتراك مرتبط بهذا البريد الإلكتروني:\n($unregisteredEmailAttempt)\n\nيمكنك تسجيل حساب جديد ببياناتك الآن، أو المتابعة باستخدام النسخة المجانية.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotRegisteredDialog = false
                        isLoginFlow = false
                        showGoogleBackupDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تسجيل حساب جديد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotRegisteredDialog = false
                        completeOnboarding()
                    }
                ) {
                    Text("المتابعة مجاناً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingContent(
    pagerState: PagerState,
    pages: List<OnboardingPageData>,
    signedInGoogleAccount: GoogleSignInAccount? = null,
    isCheckingGoogleLogin: Boolean = false,
    onSkip: () -> Unit = {},
    onNext: () -> Unit = {},
    onStart: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onNewRegistration: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Brand Logo & Skip button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المُسَرِّب",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryTeal
                            )
                        )
                    }

                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(
                            onClick = onSkip,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "تخطي",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIdx ->
                    val page = pages[pageIdx]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Feature Icon with glowing circular frame
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .shadow(20.dp, CircleShape, spotColor = page.gradientColors.first().copy(alpha = 0.45f))
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(page.gradientColors)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title (3–6 words, no exclamation mark)
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optional Voice Example Card (Page 3)
                        if (page.voiceExample != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = page.voiceExample,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Subtitle (up to 2 lines, no exclamation mark)
                        Text(
                            text = page.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Feature Pills using FlowRow to adapt cleanly to small screens & large fonts
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            page.pills.forEach { pill ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(page.gradientColors.first().copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = pill,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = page.gradientColors.first(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        // Google Drive Connection Card on Page 5
                        if (pageIdx == 4) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (signedInGoogleAccount != null) {
                                        AccentEmerald.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (signedInGoogleAccount != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = AccentEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تم ربط الحساب: ${signedInGoogleAccount.email}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                                                )
                                            )
                                        }
                                    } else if (isCheckingGoogleLogin) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = PrimaryTeal
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "جاري التحقق من وجود الحساب...",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Button(
                                                onClick = onGoogleSignIn,
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "دخول بحساب Google",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = onNewRegistration,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = AccentEmerald
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "تسجيل حساب جديد",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = AccentEmerald
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Navigation: Indicator Dots and Next/Start Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pages.size) { idx ->
                            val isSelected = pagerState.currentPage == idx
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 26.dp else 8.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(8.dp)
                                    .width(dotWidth)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Button (التالي / ابدأ الآن)
                    val isLastPage = pagerState.currentPage == pages.size - 1
                    Button(
                        onClick = {
                            if (isLastPage) onStart() else onNext()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastPage) AccentEmerald else PrimaryTeal
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "ابدأ الآن" else "التالي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Previews (360dp width, Light and Dark themes for each page) ───

@Preview(name = "Page 1 - Light 360dp", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun OnboardingPage1LightPreview() {
    MosaribTheme(darkTheme = false) {
        val pagerState = rememberPagerState(initialPage = 0) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 1 - Dark 360dp", widthDp = 360, heightDp = 640, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingPage1DarkPreview() {
    MosaribTheme(darkTheme = true) {
        val pagerState = rememberPagerState(initialPage = 0) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 2 - Light 360dp", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun OnboardingPage2LightPreview() {
    MosaribTheme(darkTheme = false) {
        val pagerState = rememberPagerState(initialPage = 1) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 2 - Dark 360dp", widthDp = 360, heightDp = 640, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingPage2DarkPreview() {
    MosaribTheme(darkTheme = true) {
        val pagerState = rememberPagerState(initialPage = 1) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 3 - Light 360dp", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun OnboardingPage3LightPreview() {
    MosaribTheme(darkTheme = false) {
        val pagerState = rememberPagerState(initialPage = 2) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 3 - Dark 360dp", widthDp = 360, heightDp = 640, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingPage3DarkPreview() {
    MosaribTheme(darkTheme = true) {
        val pagerState = rememberPagerState(initialPage = 2) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 4 - Light 360dp", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun OnboardingPage4LightPreview() {
    MosaribTheme(darkTheme = false) {
        val pagerState = rememberPagerState(initialPage = 3) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 4 - Dark 360dp", widthDp = 360, heightDp = 640, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingPage4DarkPreview() {
    MosaribTheme(darkTheme = true) {
        val pagerState = rememberPagerState(initialPage = 3) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 5 - Light 360dp", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun OnboardingPage5LightPreview() {
    MosaribTheme(darkTheme = false) {
        val pagerState = rememberPagerState(initialPage = 4) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}

@Preview(name = "Page 5 - Dark 360dp", widthDp = 360, heightDp = 640, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingPage5DarkPreview() {
    MosaribTheme(darkTheme = true) {
        val pagerState = rememberPagerState(initialPage = 4) { DefaultOnboardingPages.size }
        OnboardingContent(pagerState = pagerState, pages = DefaultOnboardingPages)
    }
}
