package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary

@Composable
fun AboutSection(appVersion: String) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_about))
        Text(
            stringResource(R.string.settings_app_name),
            style = MaterialTheme.typography.bodyMedium,
            color = ShopBlackSecondary,
        )
        Text(
            stringResource(R.string.settings_version, appVersion),
            style = MaterialTheme.typography.bodySmall,
            color = ShopBlackTertiary,
            modifier = androidx.compose.ui.Modifier.padding(top = 2.dp),
        )
        Text(
            stringResource(R.string.settings_made_in),
            style = MaterialTheme.typography.bodySmall,
            color = ShopBlackTertiary,
            modifier = androidx.compose.ui.Modifier.padding(top = 8.dp),
        )
    }
}
