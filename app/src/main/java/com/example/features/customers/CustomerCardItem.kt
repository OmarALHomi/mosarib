package com.example.features.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.mosarib

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

    val avatarColor = AvatarPalette.forId(customer.id, isDark)
    val initials = remember(customer.name) {
        val words = customer.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            words.isEmpty() -> ""
            words.size == 1 -> words[0].take(1)
            else -> "${words[0].take(1)}${words[1].take(1)}"
        }
    }

    val (statusText, statusAmount, statusColor) = if (customer.isWellOwner) {
        val net = item.payableBalance - item.receivableBalance
        when {
            net > 0.001 -> Triple("له", net, MaterialTheme.mosarib.credit)
            net < -0.001 -> Triple("عليه", -net, MaterialTheme.mosarib.debt)
            else -> Triple("مسدد", 0.0, MaterialTheme.mosarib.settled)
        }
    } else {
        when {
            item.receivableBalance > 0.001 -> Triple("عليه", item.receivableBalance, MaterialTheme.mosarib.debt)
            item.receivableBalance < -0.001 -> Triple("له", -item.receivableBalance, MaterialTheme.mosarib.credit)
            else -> Triple("مسدد", 0.0, MaterialTheme.mosarib.settled)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .shadow(1.dp, MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Leading 44dp avatar circle with initials (+ well owner badge)
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(avatarColor.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = avatarColor.onBackground
                    )
                }

                if (customer.isWellOwner) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.mosarib.info),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "صاحب بئر",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Middle Column: Name and Metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val metaText = remember(customer.customGroup, item.totalOperationsCount, customer.phone) {
                    buildList {
                        if (customer.customGroup.isNotBlank()) add(customer.customGroup)
                        add("${item.totalOperationsCount} حركة")
                        if (customer.phone.isNotBlank()) add(customer.phone)
                    }.joinToString(" • ")
                }

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.mosarib.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Trailing Column: Financial Status
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (statusText == "مسدد") "0" else Formatters.formatCurrency(statusAmount, currencySymbol),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    maxLines = 1,
                    softWrap = false
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 4. Actions: Call button (if phone present) + Kebab Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (customer.phone.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = onCallClick
                            )
                            .semantics {
                                contentDescription = "اتصال"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = { menuExpanded = true }
                            )
                            .semantics {
                                contentDescription = "خيارات"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("إرسال كشف عبر واتساب") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.mosarib.success) },
                            onClick = {
                                menuExpanded = false
                                onWhatsAppClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("كشف حساب PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentGold) },
                            onClick = {
                                menuExpanded = false
                                onPdfStatementClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("عرض كشف الحساب", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        if (customer.phone.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("اتصال هاتفي (${customer.phone})") },
                                leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryTeal) },
                                onClick = {
                                    menuExpanded = false
                                    onCallClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (customer.isWellOwner) "تعديل حساب البئر" else "تعديل العميل") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("أرشفة الحساب", color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.mosarib.debt) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
