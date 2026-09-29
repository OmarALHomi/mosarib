package com.example.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.SecondaryAquaDark

// ──────────────────────────────────────────────────────────────────────────────
// Data model for each nav item
// ──────────────────────────────────────────────────────────────────────────────
data class NavItem(
    val icon: ImageVector,
    val label: String,
    val testTag: String
)

// Ordered RTL: right → left  (index 0 = rightmost)
val mosaribNavItems = listOf(
    NavItem(Icons.Default.WaterDrop,                   "السقي",        "tab_sessions"),
    NavItem(Icons.Default.People,                      "العملاء",      "tab_customers"),
    NavItem(Icons.Default.Home,                        "الرئيسية",    "tab_home"),
    NavItem(Icons.AutoMirrored.Filled.ReceiptLong,     "العمليات",    "tab_vouchers"),
    NavItem(Icons.Default.Settings,                    "الإعدادات",   "tab_settings"),
)

// ──────────────────────────────────────────────────────────────────────────────
// Main composable
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MosaribNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Bar surface color
    val barColor = if (isDark) Color(0xFF122B2F) else Color(0xFFFFFFFF)
    val barShadowColor = if (isDark) Color(0xFF000000) else Color(0xFF007A87)

    // Home bubble gradient
    val homeBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF0097A7), PrimaryTealDark),
        radius = 120f
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── The floating bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = barShadowColor.copy(alpha = 0.25f),
                    spotColor = barShadowColor.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(barColor)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mosaribNavItems.forEachIndexed { index, item ->
                    if (index == 2) {
                        // Spacer placeholder for center home bubble
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        RegularNavItem(
                            item = item,
                            isSelected = selectedIndex == index,
                            isDark = isDark,
                            onClick = { onItemSelected(index) }
                        )
                    }
                }
            }
        }

        // ── Center elevated home bubble (overlaps bar top) ──
        val homeSelected = selectedIndex == 2
        val homeScale by animateFloatAsState(
            targetValue = if (homeSelected) 1.0f else 0.92f,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "home_scale"
        )

        Box(
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-22).dp)     // float above the bar top edge
                .graphicsLayer { scaleX = homeScale; scaleY = homeScale }
                .shadow(
                    elevation = if (homeSelected) 18.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFF0097A7).copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(homeBrush)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onItemSelected(2) }
                .testTag("tab_home"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "الرئيسية",
                    tint = Color.White,
                    modifier = Modifier.size(if (homeSelected) 26.dp else 22.dp)
                )
                if (homeSelected) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Regular (non-home) nav item
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun RegularNavItem(
    item: NavItem,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeColor = if (isDark) Color(0xFF26C6DA) else PrimaryTeal
    val inactiveColor = if (isDark) Color(0xFF607D8B) else Color(0xFF90A4AE)

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "icon_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(item.testTag)
    ) {
        // Active pill indicator above icon
        Box(
            modifier = Modifier
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) activeColor else Color.Transparent
                )
                .then(if (isSelected) Modifier.size(width = 24.dp, height = 3.dp) else Modifier.size(0.dp))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Icon with scale animation
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Label
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else inactiveColor.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}
