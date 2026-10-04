package com.baynana.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.migration.LegacyMigrationOutcome
import com.baynana.data.local.migration.LegacyMigrationRepository
import com.baynana.domain.ledger.RoomFeed
import com.baynana.domain.migration.LegacyMigrationPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * حالة الرئيسية: خلاصة الغرف + ما ينتظر إقراري + حالة الترحيل من الإرث.
 *
 * الشاشة **لا تحسب شيئًا**: كل رقم يأتي جاهزًا من `RoomFeed` (ومصدره `LedgerSnapshot`).
 */
class BaynanaHomeViewModel(application: Application) : AndroidViewModel(application) {

    data class MigrationState(
        val available: Boolean = false,
        val plan: LegacyMigrationPlan? = null,
        val outcome: LegacyMigrationOutcome? = null,
        val error: String? = null,
        val running: Boolean = false
    )

    data class UiState(
        val loading: Boolean = true,
        val rooms: List<RoomFeed> = emptyList(),
        val migration: MigrationState = MigrationState(),
        /** نصّ حالة المزامنة كما يُعرض للمستخدم بصدق (لا يوجد اتصال بعد). */
        val syncLine: String = "كل ما تكتبه محفوظ في دفترك على هذا الجهاز"
    ) {
        val awaitingMe: Int get() = rooms.sumOf { it.awaitingMyAcknowledgement }
        val openRooms: Int get() = rooms.size
        val hasAnyDebt: Boolean get() = rooms.any { it.remainingMinor > 0L }
    }

    private val database: AppDatabase = AppDatabase.getDatabase(application)
    private val repository = LedgerHomeRepository(database)
    private val migration = LegacyMigrationRepository(database)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val feeds = withContext(Dispatchers.IO) { repository.roomFeeds() }
            _state.value = _state.value.copy(loading = false, rooms = feeds)
            checkLegacy()
        }
    }

    /** هل يوجد إرث لم يُرحَّل؟ يُعرض للمالك قرار صريح لا ترحيل صامت. */
    private fun checkLegacy() {
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) {
                runCatching { migration.buildPlan() }.getOrNull()
            }
            if (plan == null) {
                _state.value = _state.value.copy(migration = MigrationState(available = false))
                return@launch
            }
            val alreadyMigrated = withContext(Dispatchers.IO) {
                database.ledgerDao().getSyncState(LegacyMigrationRepository.MARKER_KEY) != null
            }
            _state.value = _state.value.copy(
                migration = MigrationState(
                    available = plan.entries.isNotEmpty() && !alreadyMigrated,
                    plan = plan
                )
            )
        }
    }

    fun runMigration() {
        val plan = _state.value.migration.plan ?: return
        if (_state.value.migration.running) return
        _state.value = _state.value.copy(migration = _state.value.migration.copy(running = true, error = null))
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { migration.apply(plan) }
            }
            result.fold(
                onSuccess = { outcome ->
                    _state.value = _state.value.copy(
                        migration = MigrationState(available = false, plan = plan, outcome = outcome)
                    )
                    refresh()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        migration = _state.value.migration.copy(
                            running = false,
                            error = error.message ?: "تعذّر الترحيل"
                        )
                    )
                }
            )
        }
    }

    fun feedOf(roomId: String): RoomFeed? = _state.value.rooms.firstOrNull { it.roomId == roomId }
}
