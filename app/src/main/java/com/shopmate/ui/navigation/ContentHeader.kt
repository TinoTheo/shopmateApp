package com.shopmate.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.TradeGreen
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Slim white header inside the content sheet — replaces the old Trade Green TopAppBar.
 * Main destinations show the store name; sub-screens get a back arrow and their own title.
 * Tablets additionally get a centered live clock (cashiers need the time for receipts and
 * shift tracking — deliberate POS decision, not decoration) and a sync-status dot.
 */
@Composable
fun ContentHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showClock: Boolean = false,
    pendingSyncCount: Int? = null,
    failedSyncCount: Int = 0,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (showBack) 6.dp else 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = ShopBlack,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = ShopBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            if (pendingSyncCount != null) {
                SyncStatusIndicator(pendingCount = pendingSyncCount, failedCount = failedSyncCount)
                Spacer(Modifier.width(6.dp))
            }
            actions()
        }

        if (showClock) {
            HeaderClock(modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** Live time + date, ticking once a minute; absolutely centered so it never shifts. */
@Composable
private fun HeaderClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Wake on the next minute boundary so the display never lags.
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = now.format(timeFormatter),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ShopBlackSecondary,
        )
        Text(
            text = " · ",
            fontSize = 12.sp,
            color = ShopBlackTertiary,
        )
        Text(
            text = now.format(dateFormatter),
            fontSize = 12.sp,
            color = ShopBlackTertiary,
        )
    }
}

/**
 * 6dp status dot + label, in order of what the cashier most needs to know:
 * - **N not sent** — the server refused uploads. A failure, so [ErrorRed] (§ Severity). Shown
 *   even while other uploads are waiting: a refused sale must never hide behind a queue.
 * - **N waiting** — held on this device until the next upload. Calm, tertiary.
 * - **Up to date** — nothing waiting and nothing refused. Trade Green dot.
 */
@Composable
private fun SyncStatusIndicator(pendingCount: Int, failedCount: Int, modifier: Modifier = Modifier) {
    val (dot, label, labelColor) = when {
        failedCount > 0 -> Triple(ErrorRed, stringResource(R.string.sync_header_failed, failedCount), ErrorRed)
        pendingCount > 0 -> Triple(ShopBlackTertiary, stringResource(R.string.sync_pending, pendingCount), ShopBlackTertiary)
        else -> Triple(TradeGreen, stringResource(R.string.sales_synced), ShopBlackTertiary)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = labelColor,
        )
    }
}
