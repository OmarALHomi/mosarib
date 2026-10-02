package com.baynana.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.SecondaryAqua

/**
 * Screen displayed when the user enabled biometric lock in Settings.
 */
@Composable
fun BiometricLockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authenticationSignal by remember { mutableStateOf<CancellationSignal?>(null) }
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    val credentialLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlock()
        } else {
            errorMessage = "لم يتم تأكيد قفل الهاتف"
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) authenticationSignal?.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            authenticationSignal?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun triggerAuth() {
        authenticationSignal?.cancel()
        errorMessage = null
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) break
            ctx = ctx.baseContext
        }
        val activity = ctx as? Activity
        if (activity != null) {
            authenticationSignal = BiometricHelper.authenticate(
                activity = activity,
                onSuccess = onUnlock,
                onError = { err -> errorMessage = err }
            )
        } else {
            // Fail closed: missing Activity is not evidence of identity.
            errorMessage = "تعذر فتح نافذة التحقق. أغلق التطبيق وأعد فتحه"
        }
    }

    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF003844),
                        Color(0xFF0C1B20),
                        Color(0xFF070F14)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Fingerprint Icon Emblem
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(16.dp, CircleShape, spotColor = SecondaryAqua)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryTeal, AccentEmerald)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "بيننا",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "التطبيق مقفل لحماية بياناتك وسجلات السقي",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB0C9D4),
                    textAlign = TextAlign.Center
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { triggerAuth() },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فتح القفل بالبصمة",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            if (keyguard?.isDeviceSecure == true) {
                TextButton(onClick = {
                    authenticationSignal?.cancel()
                    @Suppress("DEPRECATION")
                    val intent = keyguard.createConfirmDeviceCredentialIntent(
                        "بيننا", "أكد قفل الهاتف للوصول إلى حساباتك"
                    )
                    if (intent != null) {
                        credentialLauncher.launch(intent)
                    } else {
                        errorMessage = "قفل الهاتف غير متاح؛ لم يتم فتح التطبيق"
                    }
                }) {
                    Text("استخدام قفل الهاتف", color = Color.White)
                }
            }
        }
    }
}

/**
 * Screen displayed if the app is transferred/cloned to an unauthorized device.
 */
@Composable
fun DeviceUnauthorizedScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NoAccounts,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "الجهاز غير مصرح به",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "هذا التطبيق مقيد بالعمل على الجهاز الأصلي المرخص له فقط لمنع تسريب الحسابات. إذا قمت بتغيير هاتفك، يرجى التواصل مع مسؤول النظام.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            )
        }
    }
}
