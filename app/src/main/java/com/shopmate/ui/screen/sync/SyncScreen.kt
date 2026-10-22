package com.shopmate.ui.screen.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.util.DeviceLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * "Where is my data?" — measured in what's waiting to send. One status card, then the history.
 * On tablets the history sits beside a panel showing the selected refused row.
 */
@Composable
fun SyncScreen(
    deviceLayout: DeviceLayout,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val refusedAgainText = stringResource(R.string.sync_refused_again)
    val copiedText = stringResource(R.string.sync_details_copied)

    LaunchedEffect(Unit) {
        viewModel.refusedAgain.collect { snackbar.showSnackbar(refusedAgainText) }
    }

    val actions = SyncActions(
        sendNow = viewModel::sendNow,
        signIn = viewModel::signIn,
        sendAgain = viewModel::sendAgain,
        copyDetails = { text ->
            clipboard.setText(AnnotatedString(text))
            scope.launch { snackbar.showSnackbar(copiedText) }
        },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (deviceLayout) {
                DeviceLayout.COMPACT -> SyncPhone(state, actions)
                DeviceLayout.EXPANDED -> SyncTablet(state, actions)
            }
        }
    }
}

class SyncActions(
    val sendNow: () -> Unit,
    val signIn: () -> Unit,
    val sendAgain: (String) -> Unit,
    val copyDetails: (String) -> Unit,
)

@Composable
private fun SyncPhone(state: SyncUiState, actions: SyncActions) {
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "card") {
            SyncStatusCard(
                card = state.card,
                onSendNow = actions.sendNow,
                onSignIn = actions.signIn,
                onSeeWhy = { seeWhy(state, listState, scope) { expandedId = it } },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        historyRows(state, expandedId, { id -> expandedId = if (expandedId == id) null else id }, actions)
        item(key = "footer") { Footer(state.storeName) }
    }
}

@Composable
private fun SyncTablet(state: SyncUiState, actions: SyncActions) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Until the cashier picks one, the panel shows the first refused row, if there is one.
    val selected = state.rows.firstOrNull { it.id == selectedId }
        ?: state.rows.firstOrNull { it.status == RowStatus.NOT_SENT }

    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "card") {
                SyncStatusCard(
                    card = state.card,
                    onSendNow = actions.sendNow,
                    onSignIn = actions.signIn,
                    onSeeWhy = { seeWhy(state, listState, scope) { selectedId = it } },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            historyRows(
                state = state,
                expandedId = selected?.id,
                onToggle = { id -> selectedId = id },
                actions = actions,
                expandInPlace = false,
            )
            item(key = "footer") { Footer(state.storeName) }
        }
        if (selected?.status == RowStatus.NOT_SENT) {
            Column(
                Modifier
                    .width(440.dp)
                    .fillMaxHeight()
                    .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                    .background(ShopWhite, RoundedCornerShape(16.dp))
                    .border(1.dp, ReceiptBeige, RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                SyncEventRow(
                    row = selected,
                    expanded = false,
                    onToggle = {},
                    onSendAgain = {},
                    onCopyDetails = {},
                    tillCode = state.tillCode,
                    expandInPlace = false,
                )
                RefusedRowContent(
                    row = selected,
                    onSendAgain = { actions.sendAgain(selected.id) },
                    onCopyDetails = actions.copyDetails,
                    tillCode = state.tillCode,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historyRows(
    state: SyncUiState,
    expandedId: String?,
    onToggle: (String) -> Unit,
    actions: SyncActions,
    expandInPlace: Boolean = true,
) {
    if (state.rows.isEmpty()) {
        item(key = "empty") {
            Text(
                text = stringResource(R.string.sync_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                color = ShopBlackTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
        }
        return
    }
    item(key = "history-header") {
        Text(
            text = stringResource(R.string.sync_history),
            style = MaterialTheme.typography.titleSmall,
            color = ShopBlack,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
    items(state.rows, key = { it.id }) { row ->
        SyncEventRow(
            row = row,
            expanded = row.id == expandedId,
            onToggle = { onToggle(row.id) },
            onSendAgain = { actions.sendAgain(row.id) },
            onCopyDetails = actions.copyDetails,
            tillCode = state.tillCode,
            expandInPlace = expandInPlace,
            selected = !expandInPlace && row.id == expandedId,
        )
    }
}

/** "See why": open the first refused row and bring it into view. */
private fun seeWhy(state: SyncUiState, listState: LazyListState, scope: CoroutineScope, open: (String) -> Unit) {
    val index = state.rows.indexOfFirst { it.status == RowStatus.NOT_SENT }
    if (index < 0) return
    open(state.rows[index].id)
    // Card, then the history header, then the rows.
    scope.launch { listState.animateScrollToItem(index + 2) }
}

@Composable
private fun Footer(storeName: String) {
    Text(
        text = stringResource(R.string.sync_footer, storeName.ifBlank { stringResource(R.string.app_name) }),
        style = MaterialTheme.typography.labelMedium,
        color = ShopBlackTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
}
