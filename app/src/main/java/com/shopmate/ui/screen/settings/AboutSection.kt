package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.BuildConfig
import com.shopmate.R
import com.shopmate.domain.repository.AppConfig
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary

@Composable
fun AboutSection(appVersion: String, remote: AppConfig? = null) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_about))
        // From ShopMate (§ Remote settings): notices only, nothing here stops the till.
        remote?.notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = ShopBlackSecondary,
                modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp),
            )
        }
        if (remote?.updateAvailable == true) {
            Text(
                stringResource(R.string.settings_update_available),
                style = MaterialTheme.typography.bodyMedium,
                color = ShopBlackSecondary,
                modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp),
            )
        }
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
        remote?.supportContact?.let {
            Text(
                stringResource(R.string.settings_need_help, it),
                style = MaterialTheme.typography.bodySmall,
                color = ShopBlackSecondary,
                modifier = androidx.compose.ui.Modifier.padding(top = 2.dp),
            )
        }
        // Where uploads go. A system detail for the owner or support — it lives here, not on Sync.
        Text(
            stringResource(R.string.sync_connected_to, BuildConfig.SUPABASE_URL),
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
