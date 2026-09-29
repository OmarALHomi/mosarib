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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.PrimaryTealLight
import com.example.ui.theme.SecondaryAqua

// ──────────────────────────────────────────────────────────────────────────────
// Water-drop / teardrop shape (point at top, rounded at bottom)
// ──────────────────────────────────────────────────────────────────────────────
object WaterDropShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            // Start at top center peak
            moveTo(w * 0.5f, 0f)
            // Curve right-side down to bottom center
            cubicTo(
                w * 0.92f, h * 0.28f,
                w * 1.0f,  h * 0.58f,
                w * 0.5f,  h
            )
            // Curve left-side back up to peak
            cubicTo(
                w * 0.0f,  h * 0.58f,
                w * 0.08f, h * 0.28f,
                w * 0.5f,  0f
            )
            close()
        }
        return Outline.Generic(path)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Nav item model
// ──────────────────────────────────────────────────────────────────────────────
data class NavItem(
    val icon: ImageVector,
    val label: String,
    val testTag: String
)

// RTL order: index 0 = rightmost
val mosaribNavItems = listOf(
    NavItem(Icons.Default.WaterDrop,              "سجلات السقي",  "tab_sessions"),
    NavItem(Icons.Default.People,                 "العملاء",      "tab_customers"),
    NavItem(Icons.Default.Home,                   "الرئيسية",    "tab_home"),
    NavItem(Icons.AutoMirrored.Filled.ReceiptLong,"سجل العمليات","tab_vouchers"),
    NavItem(Icons.Default.Settings,               "الإعدادات",   "tab_settings"),
)

// ──────────────────────────────────────────────────────────────────────────────
// Main MosaribNavBar
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MosaribNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Surface colors from our palette
    val barBg = if (isDark) DarkSurface else LightSurface
    val barShadowColor = PrimaryTeal

    // Home drop gradient: PrimaryTealLight → PrimaryTealDark (our brand)
    val homeGradient = Brush.verticalGradient(
        colors = listOf(PrimaryTealLight, PrimaryTealDark)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── Bar body ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = barShadowColor.copy(alpha = 0.18f),
                    spotColor = barShadowColor.copy(alpha = 0.22f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(barBg)
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mosaribNavItems.forEachIndexed { index, item ->
                    if (index == 2) {
                        // placeholder so center stays empty (home floats above)
                        Spacer(modifier = Modifier.size(width = 68.dp, height = 60.dp))
                    } else {
                        NavIconItem(
                            item = item,
                            isSelected = selectedIndex == index,
                            isDark = isDark,
                            onClick = { onItemSelected(index) }
                        )
                    }
                }
            }
        }

        // ── Home water-drop button (elevated above bar) ───────────────────────
        val homeSelected = selectedIndex == 2
        val homeScale by animateFloatAsState(
            targetValue = if (homeSelected) 1.0f else 0.9f,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "home_scale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = (-8).dp)           // float up above bar top edge
                .graphicsLayer { scaleX = homeScale; scaleY = homeScale }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onItemSelected(2) }
                .testTag("tab_home")
        ) {
            // Water-drop container
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 68.dp)
                    .shadow(
                        elevation = if (homeSelected) 14.dp else 6.dp,
                        shape = WaterDropShape,
                        spotColor = PrimaryTeal.copy(alpha = 0.5f),
                        ambientColor = PrimaryTeal.copy(alpha = 0.3f)
                    )
                    .clip(WaterDropShape)
                    .background(homeGradient),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "الرئيسية",
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (homeSelected) 26.dp else 22.dp)
                            .offset(y = 4.dp)   // push slightly toward wide base
                    )

                    if (homeSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .offset(y = 4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Label below the drop
            Text(
                text = "الرئيسية",
                fontSize = 10.sp,
                fontWeight = if (homeSelected) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (homeSelected) PrimaryTeal
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Individual non-home nav item: icon inside themed circle + label
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun NavIconItem(
    item: NavItem,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    // Active/inactive icon container colors from our palette
    val activeBg   = if (isDark) PrimaryTealDark.copy(alpha = 0.85f)
                     else PrimaryTeal
    val inactiveBg = if (isDark) DarkSurfaceVariant
                     else LightSurfaceVariant

    val activeIconTint   = Color.White
    val inactiveIconTint = if (isDark) PrimaryTealLight.copy(alpha = 0.55f)
                           else PrimaryTeal.copy(alpha = 0.45f)

    val activeLabel   = if (isDark) PrimaryTealLight else PrimaryTeal
    val inactiveLabel = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "icon_scale_${item.testTag}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp)
            .testTag(item.testTag)
    ) {
        // Circular icon badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(
                    elevation = if (isSelected) 8.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = PrimaryTeal.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(if (isSelected) activeBg else inactiveBg)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) activeIconTint else inactiveIconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isSelected) activeLabel else inactiveLabel,
            maxLines = 1
        )
    }
}
