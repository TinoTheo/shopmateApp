package com.shopmate.ui.screen.device

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.domain.repository.StoreToday
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.ui.components.BrandBand
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.FormMaxWidth
import com.shopmate.ui.components.OnboardingBandHeight
import com.shopmate.ui.components.ShopMatePrimaryButton
import com.shopmate.ui.components.ShopMateSecondaryButton
import com.shopmate.ui.components.ShopMateTextButton
import com.shopmate.ui.components.ShopMateTextField
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.NumberStyle
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.CurrencyFormatter
import com.shopmate.ui.util.DateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.font.FontWeight

/**
 * Asked once per store on each device (CLAUDE.md § Devices): sell here, or manage remotely.
 * On the sign-in band, because it's part of setting the device up.
 */
@Composable
fun DeviceRoleScreen(storeName: String, viewModel: DeviceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var sellHere by rememberSaveable(state.hasSold) { mutableStateOf<Boolean?>(if (state.hasSold) true else null) }
    var name by rememberSaveable(state.suggestedName) { mutableStateOf(state.suggestedName) }

    BrandBand(bandHeight = OnboardingBandHeight) {
        ConstrainedContent(maxWidth = FormMaxWidth) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.device_question, storeName),
                    style = MaterialTheme.typography.displaySmall,
                    color = Ink,
                )
                Text(
                    text = stringResource(R.string.device_question_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                Spacer(Modifier.height(8.dp))
                RoleOption(
                    title = stringResource(R.string.device_till_title),
                    detail = stringResource(R.string.device_till_detail),
                    selected = sellHere == true,
                    onClick = { sellHere = true },
                )
                RoleOption(
                    title = stringResource(R.string.device_remote_title),
                    detail = stringResource(R.string.device_remote_detail),
                    selected = sellHere == false,
                    onClick = { sellHere = false },
                )
                if (sellHere == true) {
                    ShopMateTextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        label = stringResource(R.string.device_till_name),
                    )
                }
                state.refusal?.let { RefusalText(it) }
                Spacer(Modifier.height(8.dp))
                ShopMatePrimaryButton(
                    text = stringResource(R.string.device_continue),
                    onClick = { if (sellHere == true) viewModel.becomeTill(name) else viewModel.becomeRemote() },
                    enabled = sellHere != null && (sellHere == false || name.isNotBlank()),
                    isLoading = state.busy,
                )
                ShopMateTextButton(text = stringResource(R.string.settings_sign_out), onClick = viewModel::signOut)
            }
        }
    }
}

@Composable
private fun RoleOption(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = ShopWhite,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TradeGreen else ReceiptBeige),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ShopBlack)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = ShopBlackSecondary)
        }
    }
}

@Composable
private fun RefusalText(refusal: TenancyRefusal) {
    Text(
        text = stringResource(
            when (refusal) {
                TenancyRefusal.TILL_LIMIT -> R.string.device_refused_till_limit
                TenancyRefusal.NOT_SENT -> R.string.device_refused_not_sent
                TenancyRefusal.OFFLINE -> R.string.team_refused_offline
                else -> R.string.team_refused_other
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = ShopBlack,
    )
}

/**
 * A remote device's home (§ Devices): today across the store and each till's last contact.
 * Anything detailed is the web dashboard's — this screen sends the owner there.
 */
@Composable
fun RemoteHomeScreen(storeName: String, viewModel: DeviceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.refreshToday() }

    fun openWeb(path: String) {
        val url = state.webDashboardUrl.trimEnd('/') + path
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    BrandBand(bandHeight = OnboardingBandHeight) {
        ConstrainedContent(maxWidth = FormMaxWidth) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(storeName, style = MaterialTheme.typography.displaySmall, color = Ink)
                Text(
                    stringResource(R.string.remote_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                Spacer(Modifier.height(8.dp))
                TodayBlock(today = state.today, refusal = state.todayRefusal)
                Spacer(Modifier.height(8.dp))
                if (state.webDashboardUrl.isBlank()) {
                    Text(
                        stringResource(R.string.remote_web_not_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShopBlackSecondary,
                    )
                } else {
                    // Reports on a remote device is the web report.
                    ShopMatePrimaryButton(text = stringResource(R.string.remote_open_reports), onClick = { openWeb("/reports") })
                    ShopMateSecondaryButton(text = stringResource(R.string.remote_open_dashboard), onClick = { openWeb("/") })
                }
                ShopMateTextButton(text = stringResource(R.string.remote_refresh), onClick = viewModel::refreshToday, enabled = !state.busy)
                ShopMateTextButton(text = stringResource(R.string.settings_sign_out), onClick = viewModel::signOut)
            }
        }
    }
}

@Composable
private fun TodayBlock(today: StoreToday?, refusal: TenancyRefusal?) {
    if (today == null) {
        Text(
            stringResource(if (refusal == TenancyRefusal.OFFLINE) R.string.remote_offline else R.string.remote_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = ShopBlackSecondary,
        )
        return
    }
    Text(stringResource(R.string.remote_today), style = MaterialTheme.typography.labelLarge, color = ShopBlackSecondary)
    Text(CurrencyFormatter.format(today.total), style = NumberStyle(40.sp, FontWeight.SemiBold), color = ShopBlack)
    Text(
        pluralStringResource(R.plurals.remote_sales, today.saleCount, today.saleCount),
        style = NumberStyle(14.sp),
        color = ShopBlackSecondary,
    )
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.remote_tills), style = MaterialTheme.typography.labelLarge, color = ShopBlackSecondary)
    if (today.tills.isEmpty()) {
        Text(stringResource(R.string.remote_no_tills), style = MaterialTheme.typography.bodyMedium, color = ShopBlackSecondary)
    }
    today.tills.forEach { till ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(till.name, style = MaterialTheme.typography.bodyLarge, color = ShopBlack)
                Text(
                    stringResource(R.string.remote_last_contact, till.code, till.lastSeenAt.whenSaid()),
                    style = NumberStyle(12.sp),
                    color = ShopBlackTertiary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.format(till.totalToday), style = NumberStyle(15.sp, FontWeight.SemiBold), color = ShopBlack)
                Text(
                    pluralStringResource(R.plurals.remote_sales, till.salesToday, till.salesToday),
                    style = NumberStyle(12.sp),
                    color = ShopBlackTertiary,
                )
            }
        }
    }
}

/** A till the owner removed (§ Devices). It stopped syncing; what it holds stays here. */
@Composable
fun TillRemovedScreen(storeName: String, viewModel: DeviceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    BrandBand(bandHeight = OnboardingBandHeight) {
        ConstrainedContent(maxWidth = FormMaxWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.till_removed_title, storeName), style = MaterialTheme.typography.displaySmall, color = Ink)
                Text(stringResource(R.string.till_removed_detail), style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                state.refusal?.let { RefusalText(it) }
                ShopMatePrimaryButton(
                    text = stringResource(R.string.till_removed_again),
                    onClick = { viewModel.becomeTill(state.suggestedName) },
                    isLoading = state.busy,
                )
                ShopMateTextButton(text = stringResource(R.string.settings_sign_out), onClick = viewModel::signOut)
            }
        }
    }
}

/** "10:42" today, "25 Oct, 10:42" before. */
private fun Instant.whenSaid(): String =
    if (atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()) DateFormatter.formatClock(this)
    else DateFormatter.formatDayClock(this)
