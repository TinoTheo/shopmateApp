package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopmate.R
import com.shopmate.ui.components.ShopMateTextField
import com.shopmate.ui.theme.ShopBlackSecondary

@Composable
fun StoreSection(
    storeName: String,
    currencySymbol: String,
    onStoreNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
) {
    // Local edit buffer; commit to DataStore on each change (cheap — single key write).
    var name by remember(storeName) { mutableStateOf(storeName) }

    Column {
        SectionHeader(stringResource(R.string.settings_section_store))

        ShopMateTextField(
            value = name,
            onValueChange = {
                name = it
                onStoreNameChange(it)
            },
            label = stringResource(R.string.settings_store_name),
        )

        Text(
            stringResource(R.string.settings_currency),
            style = MaterialTheme.typography.bodyMedium,
            color = ShopBlackSecondary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        CurrencyPicker(selected = currencySymbol, onSelect = onCurrencyChange)
    }
}
