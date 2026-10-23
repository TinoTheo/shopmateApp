package com.shopmate.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.ui.components.BrandBand
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.OnboardingBandHeight
import com.shopmate.ui.components.FormMaxWidth
import com.shopmate.ui.components.ShopMatePrimaryButton
import com.shopmate.ui.components.ShopMateTextButton
import com.shopmate.ui.components.ShopMateTextField
import com.shopmate.ui.screen.settings.CurrencyPicker
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    var orgName by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("$") }
    // Staff join an existing shop; owners create one. One screen, two paths.
    var joining by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    BrandBand(bandHeight = OnboardingBandHeight) {
        ConstrainedContent(maxWidth = FormMaxWidth) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
            ) {
                Text(
                    text = stringResource(if (joining) R.string.onboard_join_title else R.string.onboard_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(if (joining) R.string.onboard_join_subtitle else R.string.onboard_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                Spacer(Modifier.height(28.dp))

                if (joining) {
                    ShopMateTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
                        label = stringResource(R.string.onboard_join_code),
                    )
                    Spacer(Modifier.height(28.dp))
                    ShopMatePrimaryButton(
                        text = stringResource(R.string.onboard_join),
                        onClick = { viewModel.joinShop(code) },
                        isLoading = state.isSubmitting,
                    )
                    ShopMateTextButton(
                        text = stringResource(R.string.onboard_create_instead),
                        onClick = { joining = false; viewModel.clearError() },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    state.error?.let { marker ->
                        Text(
                            text = localizedError(marker),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        )
                    }
                    return@Column
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShopMateTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = stringResource(R.string.onboarding_org_name),
                    )
                    ShopMateTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = stringResource(R.string.onboard_shop_name),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                CurrencyPicker(selected = currency, onSelect = { currency = it })

                Spacer(Modifier.height(28.dp))
                ShopMatePrimaryButton(
                    text = stringResource(R.string.onboard_create),
                    onClick = { viewModel.createShop(orgName, storeName, currency) },
                    isLoading = state.isSubmitting,
                )
                ShopMateTextButton(
                    text = stringResource(R.string.onboard_join_instead),
                    onClick = { joining = true; viewModel.clearError() },
                    modifier = Modifier.padding(top = 8.dp),
                )

                state.error?.let { marker ->
                    Text(
                        text = localizedError(marker),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun localizedError(marker: String): String = when (marker) {
    OnboardingViewModel.ORG_REQUIRED -> stringResource(R.string.onboarding_error_org_required)
    OnboardingViewModel.STORE_REQUIRED -> stringResource(R.string.onboarding_error_store_required)
    OnboardingViewModel.GENERIC -> stringResource(R.string.onboarding_error_generic)
    OnboardingViewModel.CODE_REQUIRED -> stringResource(R.string.onboard_join_error_required)
    OnboardingViewModel.CODE_NOT_FOUND -> stringResource(R.string.onboard_join_error_not_found)
    OnboardingViewModel.OFFLINE -> stringResource(R.string.onboard_error_offline)
    OnboardingViewModel.NOT_SENT -> stringResource(R.string.onboard_error_not_sent)
    else -> marker
}
