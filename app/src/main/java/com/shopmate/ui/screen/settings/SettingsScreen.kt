package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.FormMaxWidth
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.ReceiptBeige

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    teamViewModel: TeamViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val team by teamViewModel.uiState.collectAsState()
    val ownerAtTill by viewModel.ownerAtTill.collectAsState()

    // A store just added appears in the store picker straight away.
    LaunchedEffect(team.notice) {
        if (team.notice is TeamNotice.StoreAdded) viewModel.refreshStores()
    }
    var showReset by remember { mutableStateOf(false) }
    var showSignOut by remember { mutableStateOf(false) }

    SettingsContent(
        state = state,
        onStoreNameChange = viewModel::updateStoreName,
        onCurrencyChange = viewModel::updateCurrencySymbol,
        onSignOut = { showSignOut = true },
        onSwitchStore = viewModel::requestSwitch,
        onRetryFailed = viewModel::retryFailedSyncs,
        onResetRequest = { showReset = true },
        ownerAtTill = ownerAtTill,
        team = {
            TeamSection(
                state = team,
                onSetOwnerPin = teamViewModel::setOwnerPin,
                onAddStaff = teamViewModel::addStaff,
                onUpdate = teamViewModel::update,
                onChangePin = teamViewModel::changePin,
                onRemove = teamViewModel::remove,
                onAddStore = teamViewModel::addStore,
                onRemoveTill = teamViewModel::removeTill,
            )
        },
    )

    if (showReset) {
        ResetConfirmationDialog(
            onDismiss = { showReset = false },
            onConfirmed = {
                viewModel.resetAllData()
                showReset = false
            },
        )
    }

    state.pendingSwitch?.let { store ->
        AlertDialog(
            onDismissRequest = viewModel::cancelSwitch,
            title = { Text(stringResource(R.string.settings_switch_title, store.name)) },
            text = { Text(stringResource(R.string.settings_switch_body, store.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSwitch) {
                    Text(stringResource(R.string.settings_switch_confirm, store.name))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelSwitch) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text(stringResource(R.string.settings_sign_out)) },
            text = { Text(stringResource(R.string.settings_sign_out_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showSignOut = false
                }) { Text(stringResource(R.string.settings_sign_out), color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOut = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onStoreNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSignOut: () -> Unit,
    onSwitchStore: (String) -> Unit,
    onRetryFailed: () -> Unit,
    onResetRequest: () -> Unit,
    ownerAtTill: Boolean,
    team: @Composable () -> Unit,
) {
    ConstrainedContent(maxWidth = FormMaxWidth) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            StoreSection(
                storeName = state.storeName,
                currencySymbol = state.currencySymbol,
                onStoreNameChange = onStoreNameChange,
                onCurrencyChange = onCurrencyChange,
            )
            HorizontalDivider(color = ReceiptBeige, modifier = Modifier.padding(top = 20.dp))
            CloudSyncSection(
                state = state,
                onSignOut = onSignOut,
                onSwitchStore = onSwitchStore,
                ownerAtTill = ownerAtTill,
            )
            team()
            HorizontalDivider(color = ReceiptBeige, modifier = Modifier.padding(top = 20.dp))
            DataManagementSection(
                onRetryFailed = onRetryFailed,
                onReset = onResetRequest,
                canReset = ownerAtTill,
            )
            HorizontalDivider(color = ReceiptBeige, modifier = Modifier.padding(top = 20.dp))
            AboutSection(appVersion = state.appVersion)
            Spacer(Modifier.height(32.dp))
        }
    }
}
