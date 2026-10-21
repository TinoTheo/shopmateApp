package com.shopmate.ui.screen.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.EmptyState

/** Read-only list of recent sales; tapping a row opens its receipt in a bottom sheet. */
@Composable
fun SalesHistoryScreen(
    viewModel: SalesHistoryViewModel = hiltViewModel(),
) {
    val sales by viewModel.sales.collectAsState()
    val selectedSale by viewModel.selectedSale.collectAsState()

    if (sales.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = stringResource(R.string.sales_empty_title),
                subtitle = stringResource(R.string.sales_empty_subtitle),
            )
        }
    } else {
        ConstrainedContent {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sales, key = { it.id }) { sale ->
                    SaleRow(sale = sale, onClick = { viewModel.selectSale(sale.id) })
                }
            }
        }
    }

    selectedSale?.let { sale ->
        ReceiptDetailSheet(sale = sale, onDismiss = viewModel::closeReceipt)
    }
}
