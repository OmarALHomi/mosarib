package com.example.features.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.isAppDark
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryTeal

/**
 * نافذة سلة المحذوفات لحماية الحسابات واستعادتها أو مسحها نهائياً.
 */
@Composable
fun RecycleBinDialog(
    archivedCustomers: List<Customer>,
    onRestore: (Customer) -> Unit,
    onPermanentlyDelete: (Customer) -> Unit,
    onEmptyBin: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: CustomersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onGetCustomerDeleteCounts: (suspend (Long) -> CustomerDeleteCounts?)? = null,
    onGetTotalDeleteCounts: (suspend () -> CustomerDeleteCounts?)? = null
) {
    val isDark = isAppDark()
    var customerToDeletePermanently by remember { mutableStateOf<Customer?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }

    val getCountsForCustomer: suspend (Long) -> CustomerDeleteCounts? =
        onGetCustomerDeleteCounts ?: { id -> viewModel.getCustomerDeleteCounts(id) }
    val getTotalCounts: suspend () -> CustomerDeleteCounts? =
        onGetTotalDeleteCounts ?: { viewModel.getRecycleBinTotalCounts() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "سلة المحذوفات",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "الحسابات المحذوفة (${archivedCustomers.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (archivedCustomers.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyBinConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("تفريغ الكل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    if (archivedCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "سلة المحذوفات فارغة",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                )
                                Text(
                                    text = "لا توجد أي حسابات محذوفة حالياً",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(archivedCustomers, key = { it.id }) { customer ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1B2830) else Color(0xFFF8FAFC)
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (customer.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Park,
                                                    contentDescription = null,
                                                    tint = if (customer.isWellOwner) PrimaryTeal else Color(0xFF2E7D32),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = customer.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            val sub = listOfNotNull(
                                                customer.phone.takeIf { it.isNotBlank() },
                                                customer.farmName.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ")

                                            if (sub.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = sub,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // زر استعادة
                                            Button(
                                                onClick = { onRestore(customer) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentEmerald.copy(alpha = 0.15f),
                                                    contentColor = AccentEmerald
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("استعادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // زر حذف نهائي
                                            IconButton(
                                                onClick = { customerToDeletePermanently = customer },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteForever,
                                                    contentDescription = "حذف نهائي",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // تأكيد الحذف النهائي لحساب واحد
    customerToDeletePermanently?.let { cust ->
        var deleteCounts by remember(cust.id) { mutableStateOf<CustomerDeleteCounts?>(null) }
        var isLoadingCounts by remember(cust.id) { mutableStateOf(true) }

        LaunchedEffect(cust.id) {
            isLoadingCounts = true
            deleteCounts = getCountsForCustomer(cust.id)
            isLoadingCounts = false
        }

        AlertDialog(
            onDismissRequest = { customerToDeletePermanently = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text("حذف نهائي لا رجعة فيه", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                if (isLoadingCounts) {
                    Text("جاري فحص السجلات المرتبطة بالحساب...", fontSize = 13.sp)
                } else {
                    val counts = deleteCounts
                    if (counts == null) {
                        Text(
                            text = "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع.",
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "سيتم حذف الحساب \"${cust.name}\" نهائياً مع ${counts.sessions} جلسة و ${counts.vouchers} سندات و ${counts.purchases} مشتريات. لا يمكن التراجع.",
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentlyDelete(cust)
                        customerToDeletePermanently = null
                    },
                    enabled = !isLoadingCounts,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، احذف نهائياً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDeletePermanently = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // تأكيد تفريغ سلة المحذوفات بالكامل
    if (showEmptyBinConfirm) {
        var totalCounts by remember { mutableStateOf<CustomerDeleteCounts?>(null) }
        var isLoadingTotalCounts by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            isLoadingTotalCounts = true
            totalCounts = getTotalCounts()
            isLoadingTotalCounts = false
        }

        AlertDialog(
            onDismissRequest = { showEmptyBinConfirm = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("تفريغ سلة المحذوفات", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                if (isLoadingTotalCounts) {
                    Text("جاري فحص السجلات المرتبطة بسلة المحذوفات...", fontSize = 13.sp)
                } else {
                    val counts = totalCounts
                    if (counts == null) {
                        Text(
                            text = "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع.",
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "سيتم حذف جميع الحسابات المحذوفة (${archivedCustomers.size}) نهائياً مع ${counts.sessions} جلسة و ${counts.vouchers} سندات و ${counts.purchases} مشتريات. لا يمكن التراجع.",
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyBin()
                        showEmptyBinConfirm = false
                    },
                    enabled = !isLoadingTotalCounts,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("تفريغ السلة نهائياً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

