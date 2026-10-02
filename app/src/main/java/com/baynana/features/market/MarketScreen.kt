package com.baynana.features.market

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.baynana.core.ui.LuxuryToastNotification
import com.baynana.ui.theme.AccentAmber
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.PrimaryTealDark
import com.baynana.ui.theme.StatusDebt

val FILTER_CATEGORIES = listOf("الكل", "قات", "عنب", "رمان", "بن", "حبوب", "خضار", "فواكه", "أخرى")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val listings by viewModel.filteredListings.collectAsStateWithLifecycle()
    val selectedCrop by viewModel.selectedCrop.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    var showAddListingDialog by remember { mutableStateOf(false) }
    var showRequestDallalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سوق وبورصة المحاصيل",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${listings.size} عرض",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ابحث بالعزلة، القرية، المحصول أو الدلال...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Crop Filter Chips Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FILTER_CATEGORIES.forEach { crop ->
                        val isSelected = selectedCrop == crop
                        val chipColor = when (crop) {
                            "قات" -> Color(0xFF2E7D32)
                            "عنب" -> Color(0xFF6A1B9A)
                            "رمان" -> Color(0xFFC2185B)
                            "بن" -> Color(0xFF5D4037)
                            "حبوب" -> Color(0xFFE65100)
                            else -> PrimaryTeal
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) chipColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.setCropFilter(crop) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = crop,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Two Quick Action Banners
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dallal Action
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAddListingDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "نشر عرض محصول 🤝",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                )
                                Text(
                                    text = "للدلالين والمزارعين",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    // Farmer Marketing Request Action
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRequestDallalDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "طلب دلال لثمرتي 🌾",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentAmber)
                                )
                                Text(
                                    text = "تسويق مزرعة جاهزة",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }

                // Listings Stream
                if (listings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedCrop != "الكل") "لا توجد عروض تطابق هذا البحث في البورصة"
                                else "لا توجد عروض محاصيل منشورة حالياً في سوق العزلة",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "كن أول من ينشر عرضاً أو اطلب دلالاً لتسويق محصولك",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(listings, key = { it.id }) { item ->
                            ListingCard(
                                listing = item,
                                onContactWhatsApp = { viewModel.contactDallalViaWhatsApp(context, item) },
                                onCall = {
                                    val phone = item.dallalPhone.ifBlank { "967773712030" }
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                },
                                onMarkSold = { viewModel.updateStatus(item.id, "SOLD") },
                                onDelete = { viewModel.deleteListing(item.id) }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    color = PrimaryTeal
                )
            }

            LuxuryToastNotification(
                toast = toast,
                onDismiss = { viewModel.dismissToast() }
            )
        }
    }

    if (showAddListingDialog) {
        AddListingDialog(
            onDismiss = { showAddListingDialog = false },
            onSubmit = { title, crop, desc, dist, vill, price, unit, dName, dPhone, fName, fPhone, hidePhone ->
                viewModel.createListing(
                    title = title,
                    cropType = crop,
                    description = desc,
                    district = dist,
                    village = vill,
                    priceEstimate = price,
                    priceUnit = unit,
                    dallalName = dName,
                    dallalPhone = dPhone,
                    farmerName = fName,
                    farmerPhone = fPhone,
                    hideFarmerPhone = hidePhone,
                    onComplete = { success ->
                        if (success) showAddListingDialog = false
                    }
                )
            }
        )
    }

    if (showRequestDallalDialog) {
        RequestDallalDialog(
            onDismiss = { showRequestDallalDialog = false },
            onSubmit = { fName, crop, vill, dPhone, notes ->
                viewModel.sendMarketingRequestWhatsApp(
                    context = context,
                    farmerName = fName,
                    cropType = crop,
                    village = vill,
                    dallalPhone = dPhone,
                    notes = notes
                )
                showRequestDallalDialog = false
            }
        )
    }
}

@Composable
fun ListingCard(
    listing: CropListing,
    onContactWhatsApp: () -> Unit,
    onCall: () -> Unit,
    onMarkSold: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row: Crop Badge + Status + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Crop Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryTeal.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = listing.cropType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        )
                    }

                    // Status Badge
                    val (statusText, statusBg, statusFg) = when (listing.status) {
                        "SOLD" -> Triple("تم البيع والصلح ✅", AccentEmerald.copy(alpha = 0.15f), AccentEmerald)
                        "IN_NEGOTIATION" -> Triple("جاري التفاوض ⏳", AccentAmber.copy(alpha = 0.15f), AccentAmber)
                        else -> Triple("متاح للبيع 🟢", PrimaryTeal.copy(alpha = 0.15f), PrimaryTeal)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusFg)
                        )
                    }
                }

                // Price Tag
                Text(
                    text = listing.getDisplayPrice(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (listing.isSold) MaterialTheme.colorScheme.onSurfaceVariant else AccentGold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Location
            if (listing.district.isNotBlank() || listing.village.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${listing.district} • ${listing.village}".trim(' ', '•'),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // Description
            if (listing.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Broker & Farmer Privacy Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤝 الدلال: ${listing.dallalName.ifBlank { "دلال معتمد" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (listing.farmerName.isNotBlank()) {
                            Text(
                                text = "المزارع: ${listing.farmerName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    if (listing.hideFarmerPhone) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "رقم المزارع محمي برعاية الدلال لحفظ السعاية 🛡️",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentGold,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    } else if (listing.farmerPhone.isNotBlank()) {
                        Text(
                            text = "هاتف المزارع: ${listing.farmerPhone}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Inquiry Button
                Button(
                    onClick = onContactWhatsApp,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text(
                        text = "💬 تفاوض عبر واتساب",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }

                // Call Broker Button
                if (listing.dallalPhone.isNotBlank()) {
                    Button(
                        onClick = onCall,
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "اتصال", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // If not sold, allow marking as sold
                if (!listing.isSold) {
                    IconButton(
                        onClick = onMarkSold,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "تم البيع",
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
