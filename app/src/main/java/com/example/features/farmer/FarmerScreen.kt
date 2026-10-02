package com.example.features.farmer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.StatusDebt
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerScreen(
    viewModel: FarmerViewModel,
    onOpenLedger: (LinkedMusrib) -> Unit = {}
) {
    val linkedMusribs by viewModel.linkedMusribs.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showLinkDialog by remember { mutableStateOf(false) }
    var musribToUnlink by remember { mutableStateOf<LinkedMusrib?>(null) }
    var enteredCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "دفاتر الري ومسربي الماء 🌾",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTealDark,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    enteredCode = ""
                    showLinkDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ربط مع مسرب جديد", fontWeight = FontWeight.Bold) },
                containerColor = PrimaryTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (linkedMusribs.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "لم تقم بربط أي مسرب بعد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "اطلب كود الربط المكون من 5 خانات من صاحب البئر أو المسرب لمتابعة سقياتك ورصيدك مباشرة من جوالك وبشكل مجاني تماماً.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showLinkDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إدخال كود الربط الآن", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary of all linked musribs
                    item {
                        val totalDebt = linkedMusribs.sumOf { it.currentBalance.coerceAtLeast(0.0) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("عدد المسربين المربوطين", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${linkedMusribs.size} مسربين", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("إجمالي المتبقي عليك", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${Formatters.formatNumber(totalDebt)} ر.ي",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (totalDebt > 0) StatusDebt else AccentEmerald)
                                    )
                                }
                            }
                        }
                    }

                    items(linkedMusribs, key = { it.linkCode }) { musrib ->
                        LinkedMusribCard(
                            musrib = musrib,
                            onClick = { onOpenLedger(musrib) },
                            onCall = { FileSharingHelper.makePhoneCall(context, musrib.musribPhone) },
                            onDelete = { musribToUnlink = musrib }
                        )
                    }
                }
            }

            // Toast notification
            LuxuryToastNotification(toast = toast, onDismiss = { viewModel.dismissToast() })
        }
    }

    // Link Musrib Dialog
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showLinkDialog = false },
            title = { Text("ربط مع مسرب جديد 💧", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "أدخل كود الربط المكون من 5 خانات المعطى لك من المسرب (مثال: 7K9P2):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = { enteredCode = it.uppercase() },
                        placeholder = { Text("7K9P2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.linkMusrib(enteredCode) { success ->
                            if (success) showLinkDialog = false
                        }
                    },
                    enabled = enteredCode.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("تحقق وربط")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }, enabled = !isLoading) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Unlink Confirmation Dialog
    musribToUnlink?.let { musrib ->
        AlertDialog(
            onDismissRequest = { musribToUnlink = null },
            title = { Text("إلغاء ربط المسرب", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من إلغاء ربط كشف الحساب مع «${musrib.musribName}»؟ يمكنك إعادة الربط في أي وقت بالكود.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlinkMusrib(musrib)
                        musribToUnlink = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDebt)
                ) {
                    Text("نعم، إلغاء الربط")
                }
            },
            dismissButton = {
                TextButton(onClick = { musribToUnlink = null }) {
                    Text("تراجع")
                }
            }
        )
    }
}

@Composable
private fun LinkedMusribCard(
    musrib: LinkedMusrib,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    val balance = musrib.currentBalance
    val isDebtor = balance > 0
    val isCreditor = balance < 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = musrib.musribName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        if (musrib.farmName.isNotBlank()) {
                            Text(text = "مزرعة: ${musrib.farmName}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                Row {
                    if (musrib.musribPhone.isNotBlank()) {
                        IconButton(onClick = onCall) {
                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = PrimaryTeal)
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isDebtor -> StatusDebt.copy(alpha = 0.10f)
                            isCreditor -> AccentEmerald.copy(alpha = 0.10f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isDebtor -> "عليك للمسرب:"
                        isCreditor -> "لك عند المسرب:"
                        else -> "الرصيد متزن:"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDebtor -> StatusDebt
                            isCreditor -> AccentEmerald
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )

                Text(
                    text = "${Formatters.formatNumber(abs(balance))} ر.ي",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isDebtor -> StatusDebt
                            isCreditor -> AccentEmerald
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }
    }
}
