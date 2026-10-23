package com.shopmate.ui.screen.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.domain.repository.TillStaff
import com.shopmate.domain.util.PinHasher
import com.shopmate.ui.components.BrandBand
import com.shopmate.ui.components.OnboardingBandHeight
import com.shopmate.ui.components.ShopMateTextButton
import com.shopmate.ui.screen.pos.NumberPad
import com.shopmate.ui.screen.settings.labelRes
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.ReceiptBeige
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen

/**
 * The till's lock (CLAUDE.md § Staff): who's selling, then their PIN on the same number pad the
 * POS uses. Same Trade Green band and white sheet as sign-in — it is the till's front door.
 */
@Composable
fun PinLockScreen(viewModel: PinLockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    BrandBand(bandHeight = OnboardingBandHeight) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val person = state.selected
            if (person == null) {
                WhoIsSelling(staff = state.staff, onSelect = viewModel::select)
            } else {
                EnterPin(
                    person = person,
                    state = state,
                    canGoBack = state.staff.size > 1,
                    onDigit = viewModel::digit,
                    onBackspace = viewModel::backspace,
                    onBack = viewModel::back,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhoIsSelling(staff: List<TillStaff>, onSelect: (TillStaff) -> Unit) {
    Text(
        text = stringResource(R.string.lock_who),
        style = MaterialTheme.typography.displaySmall,
        color = Ink,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        staff.forEach { person ->
            Surface(
                onClick = { onSelect(person) },
                shape = RoundedCornerShape(14.dp),
                color = ShopWhite,
                border = BorderStroke(1.dp, ReceiptBeige),
                modifier = Modifier.widthIn(min = 140.dp),
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(person.name, style = MaterialTheme.typography.titleMedium, color = ShopBlack)
                    Text(
                        stringResource(person.role.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = ShopBlackSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterPin(
    person: TillStaff,
    state: PinLockUiState,
    canGoBack: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.signin_title),
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(text = person.name, style = MaterialTheme.typography.titleMedium, color = InkSecondary)
        Spacer(Modifier.height(20.dp))

        // Four dots, filled as digits arrive — the PIN itself is never shown.
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(PinHasher.PIN_LENGTH) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .background(if (i < state.entered.length) TradeGreen else ReceiptBeige, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                state.waitSeconds > 0 -> stringResource(R.string.lock_wait, state.waitSeconds)
                state.wrong -> stringResource(R.string.signin_wrong)
                else -> stringResource(R.string.signin_helper)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.wrong || state.waitSeconds > 0) ShopBlack else ShopBlackSecondary,
        )
        Spacer(Modifier.height(16.dp))
        NumberPad(onDigit = onDigit, onBackspace = onBackspace)
        if (canGoBack) {
            ShopMateTextButton(
                text = stringResource(R.string.lock_not_me),
                onClick = onBack,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
