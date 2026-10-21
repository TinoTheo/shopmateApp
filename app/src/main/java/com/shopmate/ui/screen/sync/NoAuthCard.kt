package com.shopmate.ui.screen.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.components.ShopMateCard
import com.shopmate.ui.components.ShopMateSecondaryButton
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary

/**
 * Shown when there's no usable Supabase access token. The app is gated behind auth, so reaching
 * this means the session expired or failed to refresh: sync stays dormant (sales keep queuing
 * locally) until the cashier signs in again from Settings.
 */
@Composable
fun NoAuthCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShopMateCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = ShopBlackTertiary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.sync_connect_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = ShopBlack,
                )
                Text(
                    stringResource(R.string.sync_connect_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShopBlackSecondary,
                )
            }
        }
        ShopMateSecondaryButton(
            text = stringResource(R.string.sync_open_settings),
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
