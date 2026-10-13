package com.example.core.updates

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.core.license.LicenseManager
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

@Composable
fun ForceUpdateScreen(
    broadcast: AppBroadcast
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF091217))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13222B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AccentGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = broadcast.title.ifBlank { "تحديث إجباري جديد متاح 🚀" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "تم إيقاف هذه النسخة القديمة من التطبيق (v${BuildConfig.VERSION_NAME}) لضمان أمان ودقة الحسابات، يرجى التحديث إلى أحدث إصدار للاستمرار.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF90A4AE),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (broadcast.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0B171D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = broadcast.message,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE0F2F1),
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Right
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Download Button
                val actionUrl = broadcast.actionUrl.ifBlank {
                    LicenseManager.getWhatsAppActivationUrl(LicenseManager.getDeviceCode(context))
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = broadcast.actionText.ifBlank { "تحميل التحديث الجديد الآن 📥" },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { (context as? Activity)?.finishAffinity() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق التطبيق", color = Color(0xFF78909C))
                }
            }
        }
    }
}
