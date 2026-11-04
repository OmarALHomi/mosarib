package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.core.ui.guide.guideTarget

data class MosaribNavItemData(
    val id: Int,
    val label: String,
    val testTag: String
)

val mosaribNavItems = listOf(
    MosaribNavItemData(0, "التقارير", "tab_reports"),
    MosaribNavItemData(1, "العملاء", "tab_customers"),
    MosaribNavItemData(2, "الرئيسية", "tab_home"),
    MosaribNavItemData(3, "العمليات", "tab_vouchers"),
    MosaribNavItemData(4, "الإعدادات", "tab_settings")
)

/**
 * شريط تنقل سفلي عائم وأنيق على شكل كبسولة (Floating Pill Navigation Bar).
 */
@Composable
fun MosaribNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surface
    }

    val elevation = if (isDark) 0.dp else 6.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            shadowElevation = elevation,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mosaribNavItems.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index

                    val targetIndicatorColor = if (isSelected) {
                        if (isDark) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    } else {
                        Color.Transparent
                    }

                    val indicatorColor by animateColorAsState(
                        targetValue = targetIndicatorColor,
                        label = "indicator_color_${item.id}"
                    )

                    val contentColor = if (isSelected) {
                        if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val (unselectedIcon, selectedIcon) = when (item.id) {
                        0 -> Icons.Outlined.BarChart to Icons.Rounded.BarChart
                        1 -> Icons.Outlined.Group to Icons.Rounded.Group
                        2 -> Icons.Outlined.Home to Icons.Rounded.Home
                        3 -> Icons.AutoMirrored.Outlined.ReceiptLong to Icons.AutoMirrored.Rounded.ReceiptLong
                        else -> Icons.Outlined.Settings to Icons.Rounded.Settings
                    }

                    val navGuideTarget = when (item.id) {
                        0 -> "nav_reports"
                        1 -> "nav_customers"
                        3 -> "nav_vouchers"
                        4 -> "nav_settings"
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .then(if (navGuideTarget != null) Modifier.guideTarget(navGuideTarget) else Modifier)
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                onClick = {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onItemSelected(index)
                                    }
                                }
                            )
                            .semantics {
                                contentDescription = item.label
                            }
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(indicatorColor)
                                .padding(horizontal = if (isSelected) 14.dp else 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = contentColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
