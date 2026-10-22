package com.shopmate.ui.screen.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.shopmate.BuildConfig
import com.shopmate.data.auth.AuthManager
import com.shopmate.domain.model.SyncEvent
import com.shopmate.domain.repository.SyncRepository
import com.shopmate.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SyncUiState(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
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
    authManager: AuthManager,
    workManager: WorkManager,
) : ViewModel() {

    private val manualWorkState = workManager
        .getWorkInfosForUniqueWorkFlow(SyncScheduler.MANUAL_WORK)

    private val counts =
        combine(syncRepository.getPendingCount(), syncRepository.getFailedCount()) { pending, failed ->
            pending to failed
        }

    val uiState: StateFlow<SyncUiState> = combine(
        counts,
        syncRepository.getRecentEvents(50),
        manualWorkState,
        authManager.needsReauth,
    ) { (pending, failed), events, workInfos, needsReauth ->
        val displays = events.map { it.toDisplay() }
        SyncUiState(
            pendingCount = pending,
            failedCount = failed,
            lastSyncedAt = displays.filter { it.status == EventStatus.SYNCED }
                .maxByOrNull { it.createdAt }?.createdAt,
            syncStatus = workInfos.toSyncStatus(),
            recentEvents = displays,
            hasAuthToken = !needsReauth,
            error = workInfos.firstOrNull { it.state == WorkInfo.State.FAILED }
                ?.let { "Sync failed" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    fun syncNow() {
        syncScheduler.syncNow()
    }

    /** Re-queues every refused upload and sends straight away. */
    fun sendFailedAgain() {
        viewModelScope.launch {
            syncRepository.retryFailed()
            syncScheduler.syncNow()
        }
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
