package com.shopmate.ui.screen.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.shopmate.BuildConfig
import com.shopmate.data.remote.AuthTokenProvider
import com.shopmate.domain.model.SyncEvent
import com.shopmate.domain.repository.SyncRepository
import com.shopmate.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SyncUiState(
    val pendingCount: Int = 0,
    val lastSyncedAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val recentEvents: List<SyncEventDisplay> = emptyList(),
    val hasAuthToken: Boolean = false,
    // Sync target is the Supabase project, not the legacy dashboard API.
    val baseUrl: String = BuildConfig.SUPABASE_URL,
    val error: String? = null,
)

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR }

data class SyncEventDisplay(
    val id: String,
    val type: String,
    val status: EventStatus,
    val createdAt: Instant,
    val errorMessage: String?,
)

enum class EventStatus { PENDING, SYNCED, FAILED }

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
    private val authTokenProvider: AuthTokenProvider,
    workManager: WorkManager,
) : ViewModel() {

    private val hasToken = MutableStateFlow(false)

    private val manualWorkState = workManager
        .getWorkInfosForUniqueWorkFlow(SyncScheduler.MANUAL_WORK)

    val uiState: StateFlow<SyncUiState> = combine(
        syncRepository.getPendingCount(),
        syncRepository.getRecentEvents(50),
        manualWorkState,
        hasToken,
    ) { pending, events, workInfos, token ->
        val displays = events.map { it.toDisplay() }
        SyncUiState(
            pendingCount = pending,
            lastSyncedAt = displays.filter { it.status == EventStatus.SYNCED }
                .maxByOrNull { it.createdAt }?.createdAt,
            syncStatus = workInfos.toSyncStatus(),
            recentEvents = displays,
            hasAuthToken = token,
            error = workInfos.firstOrNull { it.state == WorkInfo.State.FAILED }
                ?.let { "Sync failed" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    init {
        refreshToken()
    }

    fun syncNow() {
        syncScheduler.syncNow()
    }

    fun refreshToken() {
        viewModelScope.launch { hasToken.value = authTokenProvider.hasToken() }
    }

    private fun SyncEvent.toDisplay() = SyncEventDisplay(
        id = id,
        type = eventType.name,
        status = when {
            synced -> EventStatus.SYNCED
            failed -> EventStatus.FAILED
            else -> EventStatus.PENDING
        },
        createdAt = createdAt,
        errorMessage = errorMessage,
    )

    private fun List<WorkInfo>.toSyncStatus(): SyncStatus = when (firstOrNull()?.state) {
        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> SyncStatus.SYNCING
        WorkInfo.State.SUCCEEDED -> SyncStatus.SUCCESS
        WorkInfo.State.FAILED -> SyncStatus.ERROR
        else -> SyncStatus.IDLE
    }
}
