package com.shopmate.ui.screen.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.local.TillCode
import com.shopmate.domain.model.EventType
import com.shopmate.domain.model.SyncEntry
import com.shopmate.domain.model.SyncFailureReason
import com.shopmate.domain.repository.SettingsRepository
import com.shopmate.domain.repository.SyncRepository
import com.shopmate.sync.NetworkMonitor
import com.shopmate.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/**
 * The one card at the top of Sync. Exactly one shows, in this priority (copy-pass handoff § 1):
 * signed out beats refused beats offline beats waiting beats all sent.
 */
sealed interface SyncCard {
    data class Paused(val waiting: Int) : SyncCard
    data class NotSent(val notSent: Int, val waiting: Int, val canSendNow: Boolean) : SyncCard
    data class Offline(val waiting: Int, val since: Instant?) : SyncCard
    data class Waiting(val waiting: Int, val sales: Int, val productLists: Int, val sending: Boolean) : SyncCard
    data class AllSent(val lastSentAt: Instant?) : SyncCard
}

enum class RowStatus { WAITING, SENDING, SENT, PAUSED, NOT_SENT }

/** One history row, ready to read. */
data class SyncRowUi(
    val id: String,
    val type: EventType,
    val at: Instant,
    val status: RowStatus,
    /** Sale rows only. */
    val amount: BigDecimal?,
    val receiptNumber: String?,
    /** Set when the sale was renumbered: the number on the customer's paper receipt. */
    val originalReceiptNumber: String?,
    val sentAt: Instant?,
    /** Why the store refused it — never shown as raw text outside Details. */
    val reason: SyncFailureReason?,
    /** The server's own words, for Details only. */
    val rawError: String?,
    /** Waiting, and the store didn't answer the last attempt — the worker is retrying. */
    val noAnswer: Boolean,
)

data class SyncUiState(
    val card: SyncCard = SyncCard.AllSent(null),
    val rows: List<SyncRowUi> = emptyList(),
    val storeName: String = "",
    val tillCode: String = "",
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
    private val authManager: AuthManager,
    networkMonitor: NetworkMonitor,
    settings: SettingsRepository,
    tillCode: TillCode,
    workManager: WorkManager,
) : ViewModel() {

    private val tillCodeValue = MutableStateFlow("")

    /** Rows the cashier just sent again; if one comes back refused, say so once. */
    private val retried = mutableSetOf<String>()
    private val refusedAgainChannel = Channel<Unit>(Channel.CONFLATED)
    val refusedAgain: Flow<Unit> = refusedAgainChannel.receiveAsFlow()

    init {
        viewModelScope.launch { tillCodeValue.value = tillCode.get() }
    }

    private val work = workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.MANUAL_WORK)

    private val connection = combine(
        networkMonitor.isOnline,
        networkMonitor.offlineSince,
        authManager.needsReauth,
    ) { online, since, needsReauth -> Connection(online, since, needsReauth) }

    private val queue = combine(
        syncRepository.getRecentEntries(HISTORY_LIMIT),
        syncRepository.getPendingEvents(),
        syncRepository.getFailedCount(),
        syncRepository.lastSentAt(),
    ) { entries, pending, failed, lastSent -> Queue(entries, pending.map { it.eventType }, failed, lastSent) }

    private val labels = combine(settings.storeName, tillCodeValue) { store, code -> store to code }

    val uiState: StateFlow<SyncUiState> = combine(connection, queue, work, labels) { c, q, workInfos, (store, code) ->
        val info = workInfos.firstOrNull()
        val sending = c.online && info?.state == WorkInfo.State.RUNNING
        // A retry after the store didn't answer (5xx, timeout): still waiting, not a failure.
        val noAnswer = c.online && (
            (info?.state == WorkInfo.State.ENQUEUED && info.runAttemptCount > 0) ||
                info?.state == WorkInfo.State.FAILED
            )
        val waiting = q.pendingTypes.size

        val card = when {
            c.needsReauth -> SyncCard.Paused(waiting)
            q.failed > 0 -> SyncCard.NotSent(q.failed, waiting, canSendNow = c.online)
            waiting > 0 && !c.online -> SyncCard.Offline(waiting, c.offlineSince)
            waiting > 0 -> SyncCard.Waiting(
                waiting = waiting,
                sales = q.pendingTypes.count { it == EventType.SALE },
                productLists = q.pendingTypes.count { it == EventType.SNAPSHOT },
                sending = sending,
            )
            else -> SyncCard.AllSent(q.lastSentAt)
        }

        val rows = q.entries.map { entry -> entry.toRow(c.needsReauth, sending, noAnswer) }
        checkRefusedAgain(rows)
        SyncUiState(card = card, rows = rows, storeName = store, tillCode = code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    /** Online only — the button is hidden offline, so it never spins waiting for a connection. */
    fun sendNow() = syncScheduler.syncNow()

    /** Sends one refused row again. If the store refuses it again, [refusedAgain] fires. */
    fun sendAgain(eventId: String) {
        viewModelScope.launch {
            syncRepository.retry(eventId)
            // Tracked only once the row is back in the queue, so its old "not sent" state can't
            // be mistaken for a fresh refusal.
            synchronized(retried) { retried += eventId }
            syncScheduler.syncNow()
        }
    }

    /** "Sign in" on the paused card: end the refused session; the app returns to sign-in. */
    fun signIn() {
        viewModelScope.launch { authManager.signOut() }
    }

    private fun checkRefusedAgain(rows: List<SyncRowUi>) {
        val refusedAgain = synchronized(retried) {
            val settled = rows.filter { it.id in retried && it.status in SETTLED }
            retried -= settled.map { it.id }.toSet()
            settled.any { it.status == RowStatus.NOT_SENT }
        }
        if (refusedAgain) refusedAgainChannel.trySend(Unit)
    }

    private fun SyncEntry.toRow(paused: Boolean, sending: Boolean, noAnswer: Boolean): SyncRowUi {
        val e = event
        val status = when {
            e.failed -> RowStatus.NOT_SENT
            e.synced -> RowStatus.SENT
            paused -> RowStatus.PAUSED
            sending -> RowStatus.SENDING
            else -> RowStatus.WAITING
        }
        return SyncRowUi(
            id = e.id,
            type = e.eventType,
            at = e.createdAt,
            status = status,
            amount = sale?.total,
            receiptNumber = sale?.receiptNumber,
            originalReceiptNumber = sale?.originalReceiptNumber,
            sentAt = sale?.sentAt,
            reason = failureReason,
            rawError = e.errorMessage,
            noAnswer = status == RowStatus.WAITING && noAnswer,
        )
    }

    private data class Connection(val online: Boolean, val offlineSince: Instant?, val needsReauth: Boolean)

    private data class Queue(
        val entries: List<SyncEntry>,
        val pendingTypes: List<EventType>,
        val failed: Int,
        val lastSentAt: Instant?,
    )

    private companion object {
        const val HISTORY_LIMIT = 50
        val SETTLED = setOf(RowStatus.SENT, RowStatus.NOT_SENT)
    }
}
