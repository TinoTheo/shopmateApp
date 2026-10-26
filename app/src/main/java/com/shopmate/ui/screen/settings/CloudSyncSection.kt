package com.shopmate.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.components.ShopMateTextButton
import com.shopmate.ui.theme.CheckoutNeonSubtle
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.domain.repository.StoreSwitch
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.theme.TradeGreen

/**
 * Cloud account + active store. Replaces the old hand-typed token / base-URL entry (that code
 * still exists in SettingsRepository, just no longer surfaced) now that auth and sync go through
 * the Supabase session.
 */
@Composable
fun CloudSyncSection(
    state: SettingsUiState,
    onSignOut: () -> Unit,
    onSwitchStore: (String) -> Unit,
    ownerAtTill: Boolean = true,
) {
    Column {
        SectionHeader(stringResource(R.string.settings_section_account))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(
                Modifier
                    .size(8.dp)
                    .background(if (state.userEmail != null) StockGreen else ShopBlackTertiary, CircleShape),
            )
            Text(
                text = state.userEmail?.let { stringResource(R.string.settings_signed_in_as, it) }
                    ?: stringResource(R.string.settings_not_connected),
                style = MaterialTheme.typography.bodyLarge,
                color = ShopBlack,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        }
        // Signing the till out is the owner's: a manager at the till can't (§ Staff).
        if (ownerAtTill) {
            ShopMateTextButton(
                text = stringResource(R.string.settings_sign_out),
                onClick = onSignOut,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        SectionHeader(stringResource(R.string.settings_active_store))
        StoreRow(state = state, onSwitchStore = onSwitchStore)
        SwitchOutcomeText(state)
    }
}

/** Why the till didn't move, or that it did — in the place the owner just tapped. */
@Composable
private fun SwitchOutcomeText(state: SettingsUiState) {
    val text = when (val outcome = state.switchOutcome) {
        null -> null
        StoreSwitch.Switched -> state.activeStoreName?.let { stringResource(R.string.settings_switch_done, it) }
        StoreSwitch.Offline -> stringResource(R.string.settings_switch_offline)
        is StoreSwitch.NotSentYet ->
            pluralStringResource(R.plurals.settings_switch_not_sent, outcome.count, outcome.count)
    } ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = ShopBlackSecondary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StoreRow(state: SettingsUiState, onSwitchStore: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val clickable = state.hasMultipleStores

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (clickable) Modifier.clickable { expanded = true } else Modifier)
            .background(CheckoutNeonSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Outlined.Store, contentDescription = null, tint = TradeGreen)
        Text(
            text = state.activeStoreName ?: stringResource(R.string.settings_no_store),
            style = MaterialTheme.typography.bodyLarge,
            color = ShopBlack,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        if (clickable) {
            Text(
                text = stringResource(R.string.settings_switch_store),
                style = MaterialTheme.typography.labelLarge,
                color = TradeGreen,
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.stores.forEach { store ->
                DropdownMenuItem(
                    text = { Text(store.name) },
                    onClick = {
                        expanded = false
                        if (store.id != state.activeStoreId) onSwitchStore(store.id)
                    },
                    trailingIcon = {
                        if (store.id == state.activeStoreId) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = TradeGreen)
                        }
                    },
                )
            }
        }
    }
}
