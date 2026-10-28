package com.shopmate.ui.screen.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.shopmate.R
import com.shopmate.data.auth.AuthMode
import com.shopmate.ui.components.BrandBand
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.FormMaxWidth
import com.shopmate.ui.components.ShopMatePrimaryButton
import com.shopmate.ui.components.ShopMateTextButton
import com.shopmate.ui.components.ShopMateTextField
import com.shopmate.ui.components.SignInBandHeight
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.Ink
import com.shopmate.ui.theme.InkSecondary
import com.shopmate.ui.theme.Spacing
import com.shopmate.ui.theme.StockGreen
import com.shopmate.ui.theme.TradeGreen

@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    // The band carries the logo, so the old tile is gone; the status bar sits on green.
    BrandBand(bandHeight = SignInBandHeight) {
        ConstrainedContent(maxWidth = FormMaxWidth) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(if (state.isSignUp) R.string.auth_welcome else R.string.signin_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                // "{store} · Till {n}" needs a store, which isn't known before sign-in, so signing
                // in shows the title alone. Creating an account keeps its own line.
                if (state.isSignUp) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.auth_subtitle_sign_up),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(28.dp))

                when (state.mode) {
                    AuthMode.EMAIL_PASSWORD -> EmailForm(state, viewModel)
                    AuthMode.PHONE_OTP -> PhoneOtpForm(state, viewModel)
                }

                state.info?.let { marker ->
                    FeedbackText(localizedMessage(marker, state.awaitingConfirmation), color = StockGreen)
                }
                state.error?.let { marker ->
                    FeedbackText(localizedMessage(marker, state.awaitingConfirmation), color = ErrorRed)
                }
                if (state.awaitingConfirmation != null && state.mode == AuthMode.EMAIL_PASSWORD) {
                    ShopMateTextButton(
                        text = stringResource(R.string.auth_resend_email),
                        onClick = viewModel::resendConfirmation,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun EmailForm(state: AuthUiState, viewModel: AuthViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Fields and buttons size themselves; the column just centres them.
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isSignUp) {
            ShopMateTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.auth_full_name),
            )
        }
        ShopMateTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.auth_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        ShopMateTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.auth_password),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(Modifier.height(4.dp))
        ShopMatePrimaryButton(
            text = stringResource(if (state.isSignUp) R.string.auth_sign_up else R.string.auth_sign_in),
            onClick = {
                if (state.isSignUp) viewModel.signUp(email, password, name)
                else viewModel.signIn(email, password)
            },
            isLoading = state.isSubmitting,
        )
        ShopMateTextButton(
            text = stringResource(
                if (state.isSignUp) R.string.auth_toggle_to_sign_in else R.string.auth_toggle_to_sign_up,
            ),
            onClick = viewModel::toggleMode,
        )
    }
}

/** Phone OTP path — built and functional, shown only when [AuthMode.PHONE_OTP] is active. */
@Composable
private fun PhoneOtpForm(state: AuthUiState, viewModel: AuthViewModel) {
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable(state.otpStep) { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state.otpStep) {
            OtpStep.ENTER_PHONE -> {
                ShopMateTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.auth_phone),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Spacer(Modifier.height(4.dp))
                ShopMatePrimaryButton(
                    text = stringResource(R.string.auth_send_code),
                    onClick = { viewModel.requestOtp(phone) },
                    isLoading = state.isSubmitting,
                )
            }
            OtpStep.ENTER_CODE -> {
                Text(
                    text = stringResource(R.string.auth_code_sent_to, state.pendingPhone.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                ShopMateTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = stringResource(R.string.auth_code),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                Spacer(Modifier.height(4.dp))
                ShopMatePrimaryButton(
                    text = stringResource(R.string.auth_verify),
                    onClick = { viewModel.verifyOtp(code) },
                    isLoading = state.isSubmitting,
                )
                ShopMateTextButton(
                    text = stringResource(R.string.auth_resend_code),
                    onClick = viewModel::backToPhoneEntry,
                )
            }
        }
    }
}

@Composable
private fun FeedbackText(message: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
}

/** Maps ViewModel marker constants to localized strings; passes through raw provider messages. */
@Composable
private fun localizedMessage(marker: String, email: String? = null): String = when (marker) {
    AuthViewModel.EMAIL_REQUIRED -> stringResource(R.string.auth_error_email_required)
    AuthViewModel.EMAIL_INVALID -> stringResource(R.string.auth_error_email_invalid)
    AuthViewModel.PASSWORD_SHORT -> stringResource(R.string.auth_error_password_short)
    AuthViewModel.NAME_REQUIRED -> stringResource(R.string.auth_error_name_required)
    AuthViewModel.PHONE_REQUIRED -> stringResource(R.string.auth_error_phone_required)
    AuthViewModel.CODE_REQUIRED -> stringResource(R.string.auth_error_code_required)
    AuthViewModel.CHECK_EMAIL -> stringResource(R.string.auth_check_email_to, email.orEmpty())
    AuthViewModel.SENT_AGAIN -> stringResource(R.string.auth_sent_again, email.orEmpty())
    AuthViewModel.SENT_RECENTLY -> stringResource(R.string.auth_sent_recently)
    AuthViewModel.NOT_CONFIRMED -> stringResource(R.string.auth_error_not_confirmed, email.orEmpty())
    AuthViewModel.EMAIL_RATE_LIMIT -> stringResource(R.string.auth_error_email_rate_limit)
    else -> marker
}
