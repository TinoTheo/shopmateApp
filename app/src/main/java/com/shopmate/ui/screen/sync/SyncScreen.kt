package com.shopmate.ui.screen.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.ShopMatePrimaryButton
import com.shopmate.ui.theme.ShopBlackTertiary

@Composable
fun SyncScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    SyncContent(
        state = state,
        onSyncNow = viewModel::syncNow,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun SyncContent(
    state: SyncUiState,
    onSyncNow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Resolve composable-only resources before entering the LazyListScope builder.
    val historyTitle = stringResource(R.string.sync_history)
    ConstrainedContent {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "status") {
            SyncStatusCard(
                pendingCount = state.pendingCount,
                lastSyncedAt = state.lastSyncedAt,
                isError = state.syncStatus == SyncStatus.ERROR || state.error != null,
                errorMessage = state.error,
            )
        }

        item(key = "sync-button") {
            val syncing = state.syncStatus == SyncStatus.SYNCING
            ShopMatePrimaryButton(
                text = if (syncing) stringResource(R.string.sync_syncing) else stringResource(R.string.sync_now),
                onClick = onSyncNow,
                enabled = state.hasAuthToken && !syncing,
                isLoading = syncing,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!state.hasAuthToken) {
            item(key = "no-auth") { NoAuthCard(onOpenSettings = onOpenSettings) }
        }

        if (state.recentEvents.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.sync_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShopBlackTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        } else {
            syncEventHistory(
                title = historyTitle,
                events = state.recentEvents,
            )
        }

        item(key = "connection-info") {
            Text(
                text = stringResource(R.string.sync_connected_to, state.baseUrl),
                style = MaterialTheme.typography.labelMedium,
                color = ShopBlackTertiary,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
    }
}
