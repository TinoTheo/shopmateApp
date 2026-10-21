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
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.GoldMoment
import com.shopmate.ui.navigation.AppBarViewModel
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.Spacing
import java.time.LocalTime

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    appBarViewModel: AppBarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val storeName by appBarViewModel.storeName.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    ReportsContent(
        state = state,
        storeName = storeName,
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
    storeName: String,
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

            // A greeting before the numbers — the shop is a person's, not a dashboard's.
            if (state.selectedPeriod == ReportPeriod.TODAY) {
                GreetingRow(storeName = storeName)
            }

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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        RevenueHeroCard(
            summary = summary,
            comparisonLabel = state.comparisonLabel,
            isToday = state.selectedPeriod == ReportPeriod.TODAY,
        )

        // The one gold moment on the screen: whatever is selling fastest right now.
        summary.topProducts.firstOrNull()?.let { top ->
            GoldMoment(
                icon = Icons.Outlined.LocalFireDepartment,
                title = stringResource(R.string.report_top_seller_title, top.productName),
                subtitle = stringResource(R.string.report_top_seller_subtitle, top.quantitySold),
            )
        }

        RevenueChart(chart = state.chart)

        if (summary.paymentBreakdown.isNotEmpty()) {
            PaymentBreakdownBar(breakdown = summary.paymentBreakdown)
        }

        TopProductsList(products = summary.topProducts, periodLabel = summary.periodLabel)
    }
}

/** Time-aware greeting. Uses the store name when the merchant has set one. */
@Composable
private fun GreetingRow(storeName: String) {
    val hour = remember { LocalTime.now().hour }
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greeting_morning
            hour < 17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    Column {
        Text(
            text = if (storeName.isBlank()) {
                stringResource(R.string.greeting_plain, greeting)
            } else {
                stringResource(R.string.greeting_named, greeting, storeName)
            },
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
        )
        Text(
            text = stringResource(R.string.greeting_subtitle),
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
