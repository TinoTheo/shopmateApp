package com.shopmate.ui.screen.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.util.RelativeTimeFormatter

/** One history entry. Failed rows expand on tap to reveal the server error message. */
@Composable
fun SyncEventRow(
    event: SyncEventDisplay,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val isFailed = event.status == EventStatus.FAILED

    val typeLabel = if (event.type == "SNAPSHOT") {
        stringResource(R.string.sync_type_snapshot)
    } else {
        stringResource(R.string.sync_type_sale)
    }
    val typeIcon = if (event.type == "SNAPSHOT") Icons.Outlined.CameraAlt else Icons.AutoMirrored.Outlined.ReceiptLong

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isFailed) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(typeIcon, contentDescription = null, tint = ShopBlackTertiary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(typeLabel, style = MaterialTheme.typography.bodyMedium, color = ShopBlack)
                Text(
                    RelativeTimeFormatter.format(event.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = ShopBlackTertiary,
                )
            }
            StatusIcon(event.status)
        }
        AnimatedVisibility(visible = isFailed && expanded) {
            Text(
                text = event.errorMessage ?: stringResource(R.string.sync_failed),
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatusIcon(status: EventStatus) {
    val (icon, tint) = when (status) {
        EventStatus.SYNCED -> Icons.Outlined.CheckCircle to StockGreen
        EventStatus.PENDING -> Icons.Outlined.Schedule to ShopBlackTertiary
        EventStatus.FAILED -> Icons.Outlined.Close to ErrorRed
    }
    Icon(icon, contentDescription = status.name, tint = tint, modifier = Modifier.size(18.dp))
}
