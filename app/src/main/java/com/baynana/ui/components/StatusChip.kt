package com.baynana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.settlement.DealStatus
import com.baynana.domain.settlement.InstallmentStatus
import com.baynana.domain.market.ListingStatus
import com.baynana.ui.theme.BaynanaStatus

/**
 * شارة حالة: **لون + رمز + كلمة** معًا. لا نعتمد على اللون وحده لأن ثلث الرجال لا يفرّقون بعض
 * الألوان، ولا على الكلمة وحدها لأن القراءة في الشمس تحتاج تمييزًا بصريًا.
 */
@Composable
fun StatusChip(
    text: String,
    container: Color,
    onContainer: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .semantics { contentDescription = text }
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = onContainer
        )
    }
}

/** شارة حالة غرفة — تُستخدم في القوائم بلا تكرار لوسائط اللون والرمز. */
@Composable
fun RoomStatusChip(status: String, modifier: Modifier = Modifier) {
    val look = StatusLook.room(status)
    StatusChip(look.label, look.container, look.onContainer, look.icon, modifier)
}

/** شارة حالة قيد. */
@Composable
fun EntryStatusChip(status: String, modifier: Modifier = Modifier) {
    val look = StatusLook.entry(status)
    StatusChip(look.label, look.container, look.onContainer, look.icon, modifier)
}

/** لون حالة القيد ورمزها وكلمتها من مصدر واحد؛ أي حالة جديدة تُضاف هنا مرة واحدة. */
object StatusLook {

    data class Look(val label: String, val icon: ImageVector, val container: Color, val onContainer: Color)

    @Composable
    fun entry(status: String): Look {
        val colors = BaynanaStatus.colors
        return when (status) {
            EntryStatus.DRAFT -> Look(
                "مسودة لم تُرسل", Icons.Filled.EditNote,
                colors.infoContainer, colors.onInfoContainer
            )
            EntryStatus.SENT -> Look(
                "بانتظار الإقرار", Icons.Filled.HourglassTop,
                colors.waitingContainer, colors.onWaitingContainer
            )
            EntryStatus.ACKNOWLEDGED -> Look(
                "مُقرّ", Icons.Filled.CheckCircle,
                colors.acknowledgedContainer, colors.onAcknowledgedContainer
            )
            EntryStatus.DISPUTED -> Look(
                "معترَض عليه", Icons.Filled.ReportProblem,
                colors.dangerContainer, colors.onDangerContainer
            )
            EntryStatus.CHANGE_REQUESTED -> Look(
                "طُلب تعديله", Icons.Filled.Edit,
                colors.waitingContainer, colors.onWaitingContainer
            )
            EntryStatus.VOIDED -> Look(
                "ملغى بقيد عكسي", Icons.Filled.RemoveCircle,
                colors.dangerContainer, colors.onDangerContainer
            )
            else -> Look(
                "غير معروف", Icons.Filled.HelpOutline,
                colors.infoContainer, colors.onInfoContainer
            )
        }
    }

    @Composable
    fun room(status: String): Look {
        val colors = BaynanaStatus.colors
        return when (status) {
            RoomStatus.PENDING -> Look(
                "بانتظار قبول الربط", Icons.Filled.LinkOff,
                colors.waitingContainer, colors.onWaitingContainer
            )
            RoomStatus.ACTIVE -> Look(
                "قائمة", Icons.Filled.Link,
                colors.acknowledgedContainer, colors.onAcknowledgedContainer
            )
            RoomStatus.CLOSED -> Look(
                "مغلقة بتراضي الطرفين", Icons.Filled.Lock,
                colors.infoContainer, colors.onInfoContainer
            )
            else -> Look(
                "مرفوضة", Icons.Filled.Block,
                colors.dangerContainer, colors.onDangerContainer
            )
        }
    }

    @Composable
    fun deal(status: String): Look {
        val colors = BaynanaStatus.colors
        return when (status) {
            DealStatus.DRAFT -> Look("مسودة", Icons.Filled.EditNote, colors.infoContainer, colors.onInfoContainer)
            DealStatus.PENDING -> Look("بانتظار الإقرار", Icons.Filled.HourglassTop, colors.waitingContainer, colors.onWaitingContainer)
            DealStatus.ACTIVE -> Look("قائم", Icons.Filled.Handshake, colors.acknowledgedContainer, colors.onAcknowledgedContainer)
            DealStatus.COMPLETED -> Look("مكتمل", Icons.Filled.CheckCircle, colors.acknowledgedContainer, colors.onAcknowledgedContainer)
            else -> Look("مفسوخ", Icons.Filled.Cancel, colors.dangerContainer, colors.onDangerContainer)
        }
    }

    @Composable
    fun installment(status: String): Look {
        val colors = BaynanaStatus.colors
        return when (status) {
            InstallmentStatus.SCHEDULED -> Look("مستحق", Icons.Filled.Event, colors.waitingContainer, colors.onWaitingContainer)
            InstallmentStatus.PARTIAL -> Look("مدفوع جزئيًا", Icons.Filled.Timelapse, colors.waitingContainer, colors.onWaitingContainer)
            InstallmentStatus.PAID -> Look("مدفوع", Icons.Filled.CheckCircle, colors.acknowledgedContainer, colors.onAcknowledgedContainer)
            else -> Look("ملغى", Icons.Filled.Cancel, colors.dangerContainer, colors.onDangerContainer)
        }
    }

    @Composable
    fun listing(status: String): Look {
        val colors = BaynanaStatus.colors
        return when (status) {
            ListingStatus.PUBLISHED -> Look("معروض", Icons.Filled.Storefront, colors.acknowledgedContainer, colors.onAcknowledgedContainer)
            ListingStatus.RESERVED -> Look("محجوز لصلح", Icons.Filled.BookmarkAdded, colors.waitingContainer, colors.onWaitingContainer)
            ListingStatus.SOLD -> Look("تم البيع", Icons.Filled.Sell, colors.infoContainer, colors.onInfoContainer)
            ListingStatus.PENDING_REVIEW -> Look("بانتظار المصادقة", Icons.Filled.RateReview, colors.waitingContainer, colors.onWaitingContainer)
            ListingStatus.REJECTED -> Look("مرفوض", Icons.Filled.Block, colors.dangerContainer, colors.onDangerContainer)
            ListingStatus.WITHDRAWN -> Look("مسحوب", Icons.Filled.Undo, colors.infoContainer, colors.onInfoContainer)
            else -> Look("مسودة", Icons.Filled.EditNote, colors.infoContainer, colors.onInfoContainer)
        }
    }

    /** شارة صغيرة جاهزة من حالة قيد. */
    @Composable
    fun EntryChip(status: String, modifier: Modifier = Modifier) {
        val look = entry(status)
        StatusChip(look.label, look.container, look.onContainer, look.icon, modifier)
    }

    @Composable
    fun RoomChip(status: String, modifier: Modifier = Modifier) {
        val look = room(status)
        StatusChip(look.label, look.container, look.onContainer, look.icon, modifier)
    }

    @Composable
    fun DealChip(status: String, modifier: Modifier = Modifier) {
        val look = deal(status)
        StatusChip(look.label, look.container, look.onContainer, look.icon, modifier)
    }

    @Composable
    fun ListingChip(status: String, modifier: Modifier = Modifier) {
        val look = listing(status)
        StatusChip(look.label, look.container, look.onContainer, look.icon, modifier)
    }
}

/** صندوق معلومة صغير (بلا أيقونة): يُستعمل داخل البطاقات. */
@Composable
fun InfoPill(text: String, container: Color, onContainer: Color) {
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = onContainer)
    }
}
