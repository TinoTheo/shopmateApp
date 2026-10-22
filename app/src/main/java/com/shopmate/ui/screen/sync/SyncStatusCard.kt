package com.shopmate.ui.screen.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.theme.CheckoutNeonSubtle
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.RelativeTimeFormatter
import java.time.Instant

/**
 * Top-of-screen status summary with three states. "All synced" is one of only three places
 * Checkout Neon (as CheckoutNeonSubtle) appears in the app.
 */
@Composable
fun SyncStatusCard(
    pendingCount: Int,
    /** Uploads the server refused. Takes precedence: it is the one state that needs a person. */
    failedCount: Int,
    lastSyncedAt: Instant?,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val lastSyncedText = lastSyncedAt
        ?.let { stringResource(R.string.sync_last_synced, RelativeTimeFormatter.format(it)) }
        ?: stringResource(R.string.sync_never)

    data class State(val bg: Color, val icon: ImageVector, val iconTint: Color, val title: String, val titleColor: Color, val subtitle: String)

    val state = when {
        failedCount > 0 -> State(
            bg = Color(0xFFFFEBEE),
            icon = Icons.Outlined.CloudOff,
            iconTint = ErrorRed,
            title = if (failedCount == 1) stringResource(R.string.sync_failed_one)
            else stringResource(R.string.sync_failed_many, failedCount),
            titleColor = ErrorRed,
            subtitle = stringResource(R.string.sync_failed_hint),
        )
        isError -> State(
            bg = Color(0xFFFFEBEE),
            icon = Icons.Outlined.CloudOff,
            iconTint = ErrorRed,
            title = stringResource(R.string.sync_failed),
            titleColor = ErrorRed,
            subtitle = errorMessage ?: lastSyncedText,
        )
        pendingCount > 0 -> State(
            bg = ReceiptBeige,
            icon = Icons.Outlined.CloudUpload,
            iconTint = ShopBlackSecondary,
            title = if (pendingCount == 1) stringResource(R.string.sync_one_pending)
            else stringResource(R.string.sync_items_pending, pendingCount),
            titleColor = ShopBlack,
            subtitle = if (lastSyncedAt == null) stringResource(R.string.sync_waiting_connection) else lastSyncedText,
        )
        else -> State(
            bg = CheckoutNeonSubtle,
            icon = Icons.Outlined.CloudDone,
            iconTint = TradeGreen,
            title = stringResource(R.string.sync_all_synced),
            titleColor = ShopBlack,
            subtitle = lastSyncedText,
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = state.bg,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(state.icon, contentDescription = null, tint = state.iconTint, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium, color = state.titleColor)
                Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = ShopBlackSecondary)
            }
        }
    }
}
