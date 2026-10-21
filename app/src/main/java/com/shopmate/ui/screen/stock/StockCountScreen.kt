package com.shopmate.ui.screen.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shopmate.R
import com.shopmate.ui.components.InsightRow
import com.shopmate.ui.components.QuantityControl
import com.shopmate.ui.components.SectionHeader
import com.shopmate.ui.components.Severity
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.NumberMedium
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.WarmSurface

/**
 * A stocktake session. Three summary cards, then a stepper per line.
 *
 * Committing is a **batch of signed deltas, never a set of writes** (architecture rule 5) — the
 * variance is what moves stock, so a line that matches records nothing at all.
 *
 * The verb carries through: `Commit count` becomes `Counted 12 items` (§ Copy and voice).
 */
@Composable
fun StockCountScreen(
    navController: NavController,
    viewModel: StockCountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }
    // The count is done; the screen has nothing left to say.
    LaunchedEffect(state.committed) {
        if (state.committed) navController.popBackStack()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryCard(
                    label = stringResource(R.string.count_summary_counted),
                    value = state.countedCount,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = stringResource(R.string.count_summary_mismatch),
                    value = state.varianceLines.size,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = stringResource(R.string.count_summary_short),
                    value = state.shortCount,
                    modifier = Modifier.weight(1f),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                item {
                    SectionHeader(
                        label = stringResource(R.string.count_section_lines),
                        count = state.lines.size,
                    )
                }
                items(state.lines, key = { it.product.id }) { line ->
                    InsightRow(
                        name = line.product.name,
                        meta = stringResource(
                            R.string.count_line_meta,
                            line.expected,
                            if (line.variance > 0) "+${line.variance}" else "${line.variance}",
                        ),
                        // Variance is a Warning: it will hurt soon, and it is not a failure.
                        severity = if (line.matches) Severity.Healthy else Severity.Warning,
                        trailing = {
                            QuantityControl(
                                quantity = line.counted,
                                onIncrement = {
                                    viewModel.setCount(line.product.id, line.counted + 1)
                                },
                                onDecrement = {
                                    viewModel.setCount(line.product.id, line.counted - 1)
                                },
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            // Commit count is a content-sheet commit: irreversible, so it gets Checkout Neon.
            Button(
                onClick = viewModel::commit,
                enabled = state.lines.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CheckoutNeon,
                    contentColor = ShopBlack,
                    disabledContainerColor = CheckoutNeon.copy(alpha = 0.38f),
                    disabledContentColor = ShopBlack,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.count_commit),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(WarmSurface, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = "$value", style = NumberMedium, color = ShopBlack)
        Text(text = label, fontSize = 11.sp, color = ShopBlackMid)
    }
}
