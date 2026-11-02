package com.example.features.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
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
    var customerToDeletePermanently by remember { mutableStateOf<Customer?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }

    val getCountsForCustomer: suspend (Long) -> CustomerDeleteCounts? =
        onGetCustomerDeleteCounts ?: { id -> viewModel.getCustomerDeleteCounts(id) }
    val getTotalCounts: suspend () -> CustomerDeleteCounts? =
        onGetTotalDeleteCounts ?: { viewModel.getRecycleBinTotalCounts() }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "سلة المحذوفات",
        icon = Icons.Default.DeleteSweep,
        tone = DialogTone.Neutral,
        confirmText = "إغلاق",
        onConfirm = onDismiss,
        dismissText = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الحسابات المحذوفة (${archivedCustomers.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.mosarib.textMuted
                )
            )

            if (archivedCustomers.isNotEmpty()) {
                TextButton(
                    onClick = { showEmptyBinConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.mosarib.debt)
                ) {
                    Text(
                        text = "تفريغ الكل",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (archivedCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                items(archivedCustomers, key = { it.id }) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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

                                IconButton(
                                    onClick = { customerToDeletePermanently = customer },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "حذف نهائي",
                                        tint = MaterialTheme.mosarib.debt,
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

    // تأكيد الحذف النهائي لحساب واحد
    customerToDeletePermanently?.let { cust ->
        var deleteCounts by remember(cust.id) { mutableStateOf<CustomerDeleteCounts?>(null) }
        var isLoadingCounts by remember(cust.id) { mutableStateOf(true) }

        LaunchedEffect(cust.id) {
            isLoadingCounts = true
            deleteCounts = getCountsForCustomer(cust.id)
            isLoadingCounts = false
        }

        val message = if (isLoadingCounts) {
            "جاري فحص السجلات المرتبطة بالحساب..."
        } else {
            val counts = deleteCounts
            if (counts == null) {
                "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع."
            } else {
                "سيتم حذف الحساب \"${cust.name}\" نهائياً مع ${counts.sessions} جلسة و ${counts.vouchers} سندات و ${counts.purchases} مشتريات. لا يمكن التراجع."
            }
        }

        MosaribDialog(
            onDismissRequest = { customerToDeletePermanently = null },
            title = "حذف نهائي لا رجعة فيه",
            icon = Icons.Default.DeleteForever,
            tone = DialogTone.Danger,
            message = message,
            confirmText = "نعم، احذف نهائياً",
            onConfirm = {
                if (!isLoadingCounts) {
                    onPermanentlyDelete(cust)
                    customerToDeletePermanently = null
                }
            },
            dismissText = "إلغاء",
            onDismiss = { customerToDeletePermanently = null }
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

        val message = if (isLoadingTotalCounts) {
            "جاري فحص السجلات المرتبطة بسلة المحذوفات..."
        } else {
            val counts = totalCounts
            if (counts == null) {
                "تعذر حساب عدد السجلات المرتبطة. لا يمكن التراجع."
            } else {
                "سيتم حذف جميع الحسابات المحذوفة (${archivedCustomers.size}) نهائياً مع ${counts.sessions} جلسة و ${counts.vouchers} سندات و ${counts.purchases} مشتريات. لا يمكن التراجع."
            }
        }

        MosaribDialog(
            onDismissRequest = { showEmptyBinConfirm = false },
            title = "تفريغ سلة المحذوفات",
            icon = Icons.Default.DeleteSweep,
            tone = DialogTone.Danger,
            message = message,
            confirmText = "تفريغ السلة نهائياً",
            onConfirm = {
                if (!isLoadingTotalCounts) {
                    onEmptyBin()
                    showEmptyBinConfirm = false
                }
            },
            dismissText = "إلغاء",
            onDismiss = { showEmptyBinConfirm = false }
        )
    }
}
