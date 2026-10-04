package com.baynana.features.statements

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.RoomFeed
import com.baynana.ui.components.CrisisBanner
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.StatementSheet
import com.baynana.ui.components.SyncBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * **كشف الطرف**: الجواب الكامل على «كم عليّ ولمن؟» — أسطر زمنية، ورصيد جارٍ بعد كل سطر، ورأس
 * بأرقام ثلاثة، وزرّ مشاركة يُخرج **نفس النصّ** الذي على الشاشة ([StatementExport]).
 *
 * الشاشة لا تحسب شيئًا: `StatementEngine` يبني [MemberStatement] من لقطة الغرفة، وهي تعرضه.
 * ولهذا يستحيل أن تختلف شاشة عن ورقة: النصّ يُبنى من نفس المصدر، وهذا شرط البوابة الذهبية (د٥).
 *
 * حالاتها الأربع صريحة: تحميل، بلا اتصال (والتصفح يعمل)، خطأ بسببه وزرّ إعادة، وفراغ بجملة
 * تقول ما العمل — بدل شاشة بيضاء.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    roomId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val home = remember { LedgerHomeRepository(database) }
    val ledger = remember { LedgerRepository(database) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    var feed by remember { mutableStateOf<RoomFeed?>(null) }
    var members by remember { mutableStateOf<List<RoomMember>>(emptyList()) }
    var selectedMemberId by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf<MemberStatement?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun load(memberId: String) {
        loading = true
        runCatching {
            withContext(Dispatchers.IO) {
                val currentFeed = home.feedOf(roomId)
                val roomMembers = database.ledgerDao().getMembers(roomId)
                val target = memberId.ifBlank {
                    roomMembers.firstOrNull { it.isMe }?.memberId
                        ?: currentFeed?.myMemberId
                        ?: LedgerHomeRepository.MY_MEMBER_ID
                }
                Triple(currentFeed, roomMembers, ledger.statementFor(roomId, target))
            }
        }.fold(
            onSuccess = { (currentFeed, roomMembers, built) ->
                feed = currentFeed
                members = roomMembers
                statement = built
                selectedMemberId = built.memberId
                error = null
            },
            onFailure = { error = it.message ?: "تعذّر قراءة الكشف" }
        )
        loading = false
    }

    LaunchedEffect(roomId) { load("") }

    val current = statement
    val roomLabel = feed?.title?.takeIf { it.isNotBlank() } ?: "غرفة"
    val selectedName = members.firstOrNull { it.memberId == selectedMemberId }
        ?.displayName?.takeIf { it.isNotBlank() }
        ?: if (selectedMemberId == feed?.myMemberId) "كشفي" else "كشف الطرف"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("كشف الحساب", fontWeight = FontWeight.Bold)
                        Text(
                            roomLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (loading) {
                item { SyncBadge("جاري تجهيز الكشف من دفترك…") }
                return@LazyColumn
            }

            error?.let { text ->
                item {
                    CrisisBanner(
                        title = "تعذّر فتح الكشف",
                        action = "$text — أعد المحاولة، وإن تكرّر أعد تشغيل التطبيق.",
                        isDanger = true,
                        onAction = { scope.launch { load(selectedMemberId) } }
                    )
                }
            }

            message?.let { text ->
                item { SyncBadge(text) }
            }

            if (current == null) {
                // شرط واحد صريح: الشرط المركّب يمنع Kotlin من استنتاج أن `current` غير فارغ بعده.
                if (error == null) {
                    item {
                        EmptyState(
                            title = "لا كشف بعد",
                            body = "لا سطور في هذه الغرفة حتى الآن. أول قيد تكتبه يظهر هنا برصيده الجاري."
                        )
                    }
                }
                return@LazyColumn
            }

            // كشفي أم كشف الطرف؟ الطرفان في غرفة واحدة، ولكل واحد كشفه من جهته.
            if (members.size > 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            val label = member.displayName.takeIf { it.isNotBlank() }
                                ?: if (member.isMe) "كشفي" else "الطرف الآخر"
                            FilterChip(
                                selected = member.memberId == selectedMemberId,
                                onClick = { scope.launch { load(member.memberId) } },
                                label = { Text(if (member.isMe) "كشفي" else label) }
                            )
                        }
                    }
                }
            }

            item {
                StatementSheet(
                    statement = current,
                    title = selectedName,
                    subtitle = "$roomLabel • من دفترك على هذا الجهاز",
                    onShare = {
                        val export = StatementExport.build(
                            statement = current,
                            roomTitle = roomLabel,
                            subject = selectedName,
                            dateText = { at -> dateFormat.format(Date(at)) }
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TITLE, export.fileName)
                            putExtra(Intent.EXTRA_TEXT, export.body)
                        }
                        message = "النصّ المُشارَك هو نفسه المعروض هنا، حرفًا بحرف."
                        context.startActivity(Intent.createChooser(intent, "إرسال الكشف"))
                    },
                    dateFormat = dateFormat
                )
            }

            if (current.lines.isNotEmpty()) {
                item {
                    TextButton(onClick = {
                        val export = StatementExport.build(
                            statement = current,
                            roomTitle = roomLabel,
                            subject = selectedName,
                            dateText = { at -> dateFormat.format(Date(at)) }
                        )
                        message = "اسم الملفّ عند المشاركة: ${export.fileName}"
                    }) { Text("ما اسم الملفّ الذي سيُرسل؟", style = MaterialTheme.typography.labelMedium) }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "الرصيد الجاري أسفل كل مبلغ هو ما عليك حتى ذلك السطر. والإقرار لا يغيّر الأرقام، " +
                        "والاعتراض لا يمحو سطرًا: كل تغيير يبقى ظاهرًا للطرفين.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
