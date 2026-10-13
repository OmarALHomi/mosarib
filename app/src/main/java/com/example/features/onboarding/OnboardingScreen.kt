package com.example.features.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import com.example.core.license.FirebaseLicenseService
import com.example.core.license.LicenseManager
import com.example.core.license.UserDeviceTracker
import com.example.core.util.GoogleDriveBackupHelper
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val pills: List<String>
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
                                Toast.makeText(context, "أهلاً بك مجدداً ${checkRes.subscriberName}! تم استعادة حسابك بنجاح.", Toast.LENGTH_LONG).show()
                                completeOnboarding()
                            }
                            is FirebaseLicenseService.ExistingUserResult.NotFound -> {
                                // CRITICAL ANTI-TAMPERING: Sign out Google, do NOT save email to Firebase
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
                    // Registration Flow
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

    val pages = remember {
        listOf(
            OnboardingPage(
                title = "إدارة دورات السقي والري بدقة",
                subtitle = "سجّل جلسات السقي للمزارعين بالدقائق والساعات، مع احتساب المبالغ تلقائياً وإمكانية إيقاف واستئناف العداد بمرونة.",
                icon = Icons.Default.WaterDrop,
                gradientColors = listOf(PrimaryTeal, Color(0xFF0284C7)),
                pills = listOf("عداد دقيق", "حساب تلقائي", "إيقاف واستئناف")
            ),
            OnboardingPage(
                title = "دفتر حسابات وسندات مالية متكامل",
                subtitle = "تحصيل فوري لمستحقات الري بسندات قبض، وقيد النفقات التشغيلية وشراء ساعات الماء من الآبار المجاورة.",
                icon = Icons.Default.Payments,
                gradientColors = listOf(AccentEmerald, Color(0xFF059669)),
                pills = listOf("سندات قبض", "سندات صرف", "حسابات الآبار")
            ),
            OnboardingPage(
                title = "تقارير احترافية ومشاركة واتساب",
                subtitle = "استخرج كشوفات حساب تفصيلية وإجمالية بصيغة PDF أنيقة قابلة للطباعة والإرسال الفوري للمزارعين عبر الواتساب.",
                icon = Icons.Default.Assessment,
                gradientColors = listOf(AccentGold, Color(0xFFD97706)),
                pills = listOf("كشوفات PDF", "مشاركة واتساب", "إحصائيات فورية")
            ),
            OnboardingPage(
                title = "نسخ احتياطي سحابي مع Google Drive",
                subtitle = "بياناتك بأمان دائم! اربط حساب Google لحفظ واستعادة نسخك الاحتياطية سحابياً في أي وقت ومن أي هاتف.",
                icon = Icons.Default.CloudDone,
                gradientColors = listOf(Color(0xFF16A34A), Color(0xFF0D9488)),
                pills = listOf("سحابة آمنة", "حفظ تلقائي", "استعادة سريعة")
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0) { pages.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Logo text
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
                        text = "المُسَرِّبْ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal
                        )
                    )
                }

                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = { completeOnboarding() },
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

            // Pager content
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
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Icon Box with glowing shadow
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .shadow(24.dp, CircleShape, spotColor = page.gradientColors.first().copy(alpha = 0.45f))
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
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Title
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subtitle
                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            fontSize = 14.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Feature Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
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

                    // Special for Google Drive slide (Page 3)
                    if (pageIdx == 3) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (signedInGoogleAccount != null) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم ربط الحساب: ${signedInGoogleAccount?.email}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534)
                                            )
                                        )
                                    }
                                } else if (isCheckingGoogleLogin) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryTeal)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري التحقق من وجود الحساب...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Path 1: Sign in with existing Google Account
                                        Button(
                                            onClick = {
                                                isLoginFlow = true
                                                val client = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                                                googleSignInLauncher.launch(client.signInIntent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تسجيل الدخول بحساب Google (للمسجلين مسبقاً)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        // Path 2: New Registration
                                        OutlinedButton(
                                            onClick = {
                                                isLoginFlow = false
                                                showGoogleBackupDialog = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentEmerald)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تسجيل جديد (بياناتك وحساب Google)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { idx ->
                        val isSelected = pagerState.currentPage == idx
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button
                val isLastPage = pagerState.currentPage == pages.size - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            completeOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) AccentEmerald else PrimaryTeal
                    )
                ) {
                    Text(
                        text = if (isLastPage) "ابدأ استخدام المُسَرِّب ✨" else "التالي",
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

        if (showGoogleBackupDialog) {
            val initialName = com.example.core.license.UserDeviceTracker.getRegisteredName(context).takeIf { it.isNotBlank() }
                ?: com.example.core.license.LicenseManager.getActivationRecord(context)?.subscriberName
                ?: ""

            val initialPhone = com.example.core.license.UserDeviceTracker.getRegisteredPhone(context).takeIf { it.isNotBlank() }
                ?: ""

            com.example.core.ui.GoogleBackupAccountDialog(
                initialName = initialName,
                initialPhone = initialPhone,
                currentGoogleAccount = signedInGoogleAccount,
                onDismiss = { showGoogleBackupDialog = false },
                onProceed = { name, phone, _ ->
                    showGoogleBackupDialog = false
                    com.example.core.license.UserDeviceTracker.saveUserRegistrationInfo(context, name, phone)
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
                        Text("تسجيل حساب جديد بالبيانات", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNotRegisteredDialog = false
                            completeOnboarding()
                        }
                    ) {
                        Text("المتابعة بالنسخة المجانية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
