package com.example.features.settings

import androidx.compose.material.icons.filled.RestartAlt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

// Help tour, feedback and about cards shown after the license section
internal fun LazyListScope.supportSectionItems(
    onNavigateToHelp: () -> Unit,
    onResetGuides: () -> Unit,
    onOpenFeedback: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    // Section: مركز المساعدة
    item {
        SettingsNavigationCard(
            onClick = onNavigateToHelp,
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            iconTint = AccentGold,
            iconBackground = AccentGold.copy(alpha = 0.15f),
            title = "مركز المساعدة والتعليمات",
            subtitle = "إجابات سريعة، نصائح، وشروحات تفاعلية"
        )
    }

    // Section: إعادة عرض التعليمات
    item {
        SettingsNavigationCard(
            onClick = onResetGuides,
            icon = Icons.Default.RestartAlt,
            iconTint = PrimaryTeal,
            iconBackground = PrimaryTeal.copy(alpha = 0.15f),
            title = "إعادة عرض التعليمات",
            subtitle = "إعادة تفعيل الجولات التوضيحية والإرشادات من البداية"
        )
    }

    // Section: آراء وملاحظات المستخدمين
    item {
        SettingsNavigationCard(
            onClick = onOpenFeedback,
            icon = Icons.Default.Star,
            iconTint = SecondaryAqua,
            iconBackground = SecondaryAqua.copy(alpha = 0.15f),
            title = "شاركنا رأيك ومقترحاتك 💬",
            subtitle = "إرسال اقتراح، إبلاغ عن مشكلة، أو تقييم التجربة مباشرة للمطور"
        )
    }

    // Section: عن التطبيق والمطور
    item {
        SettingsNavigationCard(
            onClick = onNavigateToAbout,
            icon = Icons.Default.Info,
            iconTint = PrimaryTeal,
            iconBackground = PrimaryTeal.copy(alpha = 0.12f),
            title = "عن التطبيق والمطور",
            subtitle = "م. عمر عبد الله علي الحومي • v1.0.0"
        )
    }
}

// Clickable settings card that navigates to another screen or opens a dialog
@Composable
private fun SettingsNavigationCard(
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "عرض",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
