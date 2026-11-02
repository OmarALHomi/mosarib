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
import com.example.ui.theme.mosarib


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
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
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

                                )
                            )
                            Text(
                                text = "الحسابات المحذوفة (${archivedCustomers.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.mosarib.textMuted

                                )
                            )
                        }
                    }

                    if (archivedCustomers.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyBinConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("تفريغ الكل", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                                    tint = MaterialTheme.mosarib.textMuted.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "سلة المحذوفات فارغة",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.mosarib.textMuted
                                    )
                                )
                                Text(
                                    text = "لا توجد أي حسابات محذوفة حالياً",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.mosarib.textMuted
    
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
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                                    tint = if (customer.isWellOwner) MaterialTheme.colorScheme.primary else MaterialTheme.mosarib.success,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = customer.name,
                                                    fontWeight = FontWeight.Bold,

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
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.mosarib.textMuted
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
                                                    containerColor = MaterialTheme.mosarib.successContainer,
                                                    contentColor = MaterialTheme.mosarib.success
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("استعادة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            }

                                            // زر حذف نهائي
                                            IconButton(
                                                onClick = { customerToDeletePermanently = customer },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteForever,
                                                    contentDescription = "حذف نهائي",
                                                    tint = MaterialTheme.colorScheme.error,
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
            title = { Text("حذف نهائي لا رجعة فيه", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                if (isLoadingCounts) {
                    Text("جاري فحص السجلات المرتبطة بالحساب...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val counts = deleteCounts
                    if (counts == null) {
                        Text(
                            text = "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "سيتم حذف الحساب \"${cust.name}\" نهائياً مع ${counts.sessions} جلسة و ${counts.vouchers} سندات و ${counts.purchases} مشتريات. لا يمكن التراجع.",
                            style = MaterialTheme.typography.bodyMedium
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
            title = { Text("تفريغ سلة المحذوفات", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                if (isLoadingTotalCounts) {
                    Text("جاري فحص السجلات المرتبطة بسلة المحذوفات...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val counts = totalCounts
                    if (counts == null) {
                        Text(
                            text = "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "سيتم حذف جميع الحسابات المحذوفة (${archivedCustomers.size}) نهائياً مع ${counts.sessions} جلسة و ${counts.vouchers} سندات و ${counts.purchases} مشتريات. لا يمكن التراجع.",
                            style = MaterialTheme.typography.bodyMedium
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

