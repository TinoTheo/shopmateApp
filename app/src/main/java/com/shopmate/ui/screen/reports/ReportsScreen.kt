package com.shopmate.ui.screen.reports

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.domain.model.PeriodSummary
import com.shopmate.domain.model.ReportPeriod
import com.shopmate.domain.model.ReportWindow
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.GoldMoment
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.Spacing

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    ReportsContent(
        state = state,
        onSelectPeriod = viewModel::selectPeriod,
        onCustomClick = { showDatePicker = true },
    )

    if (showDatePicker) {
        DateRangePickerDialog(
            onConfirm = { start, end ->
                showDatePicker = false
                viewModel.selectCustomRange(start, end)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun ReportsContent(
    state: ReportsUiState,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onCustomClick: () -> Unit,
) {
    ConstrainedContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Spacer(Modifier.height(Spacing.sm))

            // A greeting before the numbers, then the dates the numbers cover.
            state.window?.let { GreetingRow(window = it) }

            PeriodSelector(
                selected = state.selectedPeriod,
                onSelect = onSelectPeriod,
                onCustomClick = onCustomClick,
            )

            Crossfade(
                targetState = state.isLoading,
                animationSpec = tween(durationMillis = 300),
                label = "reports-loading",
            ) { loading ->
                if (loading) {
                    ReportsShimmer()
                } else if (state.error != null) {
                    ErrorState(message = state.error)
                } else if (state.summary != null) {
                    LoadedReports(state = state, summary = state.summary)
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun LoadedReports(state: ReportsUiState, summary: PeriodSummary) {
    val window = state.window ?: return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        RevenueHeroCard(summary = summary, window = window)

        // The one gold moment on the screen: whatever sold most in this period.
        summary.topProducts.firstOrNull()?.let { top ->
            GoldMoment(
                icon = Icons.Outlined.LocalFireDepartment,
                title = stringResource(R.string.reports_top_title, top.productName),
                subtitle = window.soldText(top.quantitySold),
            )
        }

        RevenueChart(chart = state.chart, emptyText = stringResource(R.string.reports_empty_chart))

        if (summary.paymentBreakdown.isNotEmpty()) {
            PaymentBreakdownBar(breakdown = summary.paymentBreakdown)
        }

        TopProductsList(products = summary.topProducts, emptyText = window.emptyText())
    }
}

/** Time-aware greeting, with no name: the store name is already in the header. */
@Composable
private fun GreetingRow(window: ReportWindow) {
    Column {
        Text(
            text = reportsGreeting(),
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
        )
        Text(
            text = window.rangeText(),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}
