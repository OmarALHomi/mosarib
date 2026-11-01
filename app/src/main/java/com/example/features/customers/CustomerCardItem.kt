package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

@Composable
fun CustomerCardItem(
    item: CustomerWithBalance,
    currencySymbol: String,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPdfStatementClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val customer = item.customer
    val isDark = isAppDark()
    var menuExpanded by remember(customer.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. الصف الأول: أيقونة (صاحب بئر أو عميل) بجانبها اسم العميل مباشرة + أزرار الإجراءات مثل ما هي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // أيقونة صاحب بئر أو مزارع بجانب الاسم مباشرة
                    CustomerAvatar(isWellOwner = customer.isWellOwner, isDark = isDark)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                CustomerCardActions(
                    customer = customer,
                    menuExpanded = menuExpanded,
                    onMenuExpandedChange = { menuExpanded = it },
                    onClick = onClick,
                    onCallClick = onCallClick,
                    onWhatsAppClick = onWhatsAppClick,
                    onPdfStatementClick = onPdfStatementClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. الصف الثاني: الحركات والرقم
            CustomerCardInfoRow(item = item, isDark = isDark)

            Spacer(modifier = Modifier.height(7.dp))

            // 3. الصف الثالث: المبلغ باللون الأحمر أو الأخضر أو مسدد (يتوسط البطاقة أفقياً) مع التفقيط باللغة العربية
            CustomerCardBalance(item = item, currencySymbol = currencySymbol, isDark = isDark)
        }
    }
}

@Composable
private fun CustomerAvatar(isWellOwner: Boolean, isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (isDark) (if (isWellOwner) Color(0xFF0288D1).copy(alpha = 0.2f) else Color(0xFF2E7D32).copy(alpha = 0.25f))
                else if (isWellOwner) Color(0xFFE1F5FE)
                else Color(0xFFE8F5E9)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Park,
            contentDescription = if (isWellOwner) "صاحب بئر" else "مزارع",
            tint = if (isWellOwner) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0288D1)) else (if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)),
            modifier = Modifier.size(19.dp)
        )
    }
}

/**
 * الأزرار المتجاوبة: في الشاشات العادية تظهر الأزرار، وفي الشاشات المضغوطة يقتصر الكرت على الأهم أو الثلاث نقاط
 */
@Composable
private fun CustomerCardActions(
    customer: Customer,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPdfStatementClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isCompactCard = screenWidth < 410
    val isUltraCompact = screenWidth < 330

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (!isUltraCompact && customer.phone.isNotEmpty()) {
            CustomerCardActionButton(
                icon = Icons.Default.Call,
                contentDescription = "اتصال",
                tint = PrimaryTeal,
                background = PrimaryTeal.copy(alpha = 0.12f),
                onClick = onCallClick
            )

            if (!isCompactCard) {
                CustomerCardActionButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال كشف",
                    tint = Color(0xFF2E7D32),
                    background = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    onClick = onWhatsAppClick
                )
            }
        }

        if (!isCompactCard) {
            CustomerCardActionButton(
                icon = Icons.Default.PictureAsPdf,
                contentDescription = "كشف حساب PDF",
                tint = AccentGold,
                background = AccentGold.copy(alpha = 0.14f),
                onClick = onPdfStatementClick
            )
        }

        Box {
            CustomerCardActionButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "خيارات",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                onClick = { onMenuExpandedChange(true) }
            )

            CustomerCardMenu(
                customer = customer,
                expanded = menuExpanded,
                onDismiss = { onMenuExpandedChange(false) },
                onClick = onClick,
                onCallClick = onCallClick,
                onWhatsAppClick = onWhatsAppClick,
                onPdfStatementClick = onPdfStatementClick,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun CustomerCardActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            // 40dp keeps the name readable on 360dp phones; Compose extends the hit area to 48dp
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun CustomerCardMenu(
    customer: Customer,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPdfStatementClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("عرض كشف الحساب", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
            onClick = {
                onDismiss()
                onClick()
            }
        )
        if (customer.phone.isNotEmpty()) {
            DropdownMenuItem(
                text = { Text("اتصال هاتفي (${customer.phone})") },
                leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryTeal) },
                onClick = {
                    onDismiss()
                    onCallClick()
                }
            )
            DropdownMenuItem(
                text = { Text("إرسال كشف عبر واتساب") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF2E7D32)) },
                onClick = {
                    onDismiss()
                    onWhatsAppClick()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("تصدير كشف حساب PDF") },
            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentGold) },
            onClick = {
                onDismiss()
                onPdfStatementClick()
            }
        )
        DropdownMenuItem(
            text = { Text(if (customer.isWellOwner) "تعديل حساب البئر" else "تعديل العميل") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onEditClick()
            }
        )
        DropdownMenuItem(
            text = { Text("أرشفة الحساب", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
            onClick = {
                onDismiss()
                onDeleteClick()
            }
        )
    }
}

@Composable
private fun CustomerCardInfoRow(item: CustomerWithBalance, isDark: Boolean) {
    val customer = item.customer

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // شارة عدد العمليات
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE3F2FD))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${item.totalOperationsCount} حركة",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (customer.customGroup.isNotBlank()) {
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentGold.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = customer.customGroup,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        val subInfo = buildList {
            if (customer.phone.isNotBlank()) add(customer.phone)
            if (customer.farmName.isNotBlank()) add(customer.farmName)
        }.joinToString(" • ")

        if (subInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = subInfo,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerCardBalance(item: CustomerWithBalance, currencySymbol: String, isDark: Boolean) {
    val (statusText, statusAmount, statusColor) = if (item.customer.isWellOwner) {
        val net = item.payableBalance - item.receivableBalance
        when {
            net > 0.001 -> Triple("له", net, Color(0xFF2E7D32))
            net < -0.001 -> Triple("عليه", -net, Color(0xFFE53935))
            else -> Triple("مسدد", 0.0, AccentEmerald)
        }
    } else {
        when {
            item.receivableBalance > 0.001 -> Triple("عليه", item.receivableBalance, Color(0xFFE53935))
            item.receivableBalance < -0.001 -> Triple("له", -item.receivableBalance, Color(0xFF2E7D32))
            else -> Triple("مسدد", 0.0, AccentEmerald)
        }
    }

    if (statusText == "مسدد") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF132A1C) else Color(0xFFE8F5E9))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "مسدد",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentEmerald,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$statusText: ${Formatters.formatCurrency(statusAmount, currencySymbol)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = statusColor,
                    fontSize = 16.5.sp
                ),
                textAlign = TextAlign.Center
            )
            val tafqeetWords = Formatters.amountToArabicWords(statusAmount, currencySymbol)
            if (tafqeetWords.isNotBlank()) {
                Text(
                    text = tafqeetWords,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = statusColor.copy(alpha = 0.90f)
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
