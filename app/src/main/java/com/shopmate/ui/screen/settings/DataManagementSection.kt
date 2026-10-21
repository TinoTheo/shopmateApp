package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.StockRed

@Composable
fun DataManagementSection(
    onRetryFailed: () -> Unit,
    onReset: () -> Unit,
) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_data))

        SettingsRow(
            title = stringResource(R.string.settings_export),
            trailing = { ComingSoonTag() },
        )
        SettingsRow(
            title = stringResource(R.string.settings_retry_failed),
            description = stringResource(R.string.settings_retry_failed_desc),
            onClick = onRetryFailed,
        )
        SettingsRow(
            title = stringResource(R.string.settings_reset),
            titleColor = StockRed,
            onClick = onReset,
        )
    }
}

@Composable
private fun ComingSoonTag() {
    Surface(color = ReceiptBeige, shape = RoundedCornerShape(6.dp)) {
        Text(
            stringResource(R.string.settings_coming_soon),
            style = MaterialTheme.typography.labelMedium,
            color = ShopBlackSecondary,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
