package com.shopmate.ui.screen.auth

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.data.auth.AuthConfig
import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.AuthMode
import com.shopmate.data.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which phone-OTP sub-step the UI is on (only used when [AuthConfig.ACTIVE_MODE] is PHONE_OTP). */
enum class OtpStep { ENTER_PHONE, ENTER_CODE }

data class AuthUiState(
    val mode: AuthMode = AuthConfig.ACTIVE_MODE,
    val isSignUp: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    /** The address a confirmation email went to, while the account waits to be confirmed. */
    val awaitingConfirmation: String? = null,
    /** "Send the email again" works once this uptime (ms) has passed: Supabase limits sends. */
    val resendAllowedAt: Long = 0L,
    // Phone OTP sub-state
    val otpStep: OtpStep = OtpStep.ENTER_PHONE,
    val pendingPhone: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() = _uiState.update {
        it.copy(isSignUp = !it.isSignUp, error = null, info = null, awaitingConfirmation = null)
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, info = null) }

    // === Email path (active) ===

    fun signIn(email: String, password: String) {
        val emailError = validateEmail(email)
        val pwError = if (password.length < 6) PASSWORD_SHORT else null
        if (emailError != null || pwError != null) {
            _uiState.update { it.copy(error = emailError ?: pwError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, info = null) }
            when (val result = authManager.signInEmail(email.trim(), password)) {
                is AuthResult.Error -> _uiState.update {
                    // Supabase refuses an unconfirmed account by name: offer the email again
                    // instead of showing its words.
                    if (result.message.contains("not confirmed", ignoreCase = true)) {
                        it.copy(isSubmitting = false, error = NOT_CONFIRMED, awaitingConfirmation = email.trim())
                    } else {
                        it.copy(isSubmitting = false, error = friendly(result.message))
                    }
                }
                else -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    /** Sends the confirmation email again, at most once a minute. */
    fun resendConfirmation() {
        val email = _uiState.value.awaitingConfirmation ?: return
        if (SystemClock.uptimeMillis() < _uiState.value.resendAllowedAt) {
            _uiState.update { it.copy(error = null, info = SENT_RECENTLY) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, info = null) }
            when (val result = authManager.resendConfirmation(email)) {
                AuthResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, info = SENT_AGAIN, resendAllowedAt = SystemClock.uptimeMillis() + RESEND_GAP_MS)
                }
                is AuthResult.Error -> _uiState.update { it.copy(isSubmitting = false, error = friendly(result.message)) }
                is AuthResult.PendingOtp -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        val nameError = if (fullName.isBlank()) NAME_REQUIRED else null
        val emailError = validateEmail(email)
        val pwError = if (password.length < 6) PASSWORD_SHORT else null
        val firstError = nameError ?: emailError ?: pwError
        if (firstError != null) {
            _uiState.update { it.copy(error = firstError) }
            return
        }
        submit(
            action = { authManager.signUpEmail(email.trim(), password, fullName.trim()) },
            // If email confirmation is on, there's no session yet — tell the user to confirm.
            onSuccessNoSession = {
                _uiState.update {
                    it.copy(
                        info = CHECK_EMAIL,
                        isSignUp = false,
                        awaitingConfirmation = email.trim(),
                        resendAllowedAt = SystemClock.uptimeMillis() + RESEND_GAP_MS,
                    )
                }
            },
        )
    }

    // === Phone OTP path (built, gated) ===

    fun requestOtp(phone: String) {
        if (phone.isBlank()) {
            _uiState.update { it.copy(error = PHONE_REQUIRED) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, info = null) }
            when (val result = authManager.requestOtp(phone.trim())) {
                is AuthResult.PendingOtp -> _uiState.update {
                    it.copy(isSubmitting = false, otpStep = OtpStep.ENTER_CODE, pendingPhone = result.phone)
                }
                is AuthResult.Error -> _uiState.update { it.copy(isSubmitting = false, error = result.message) }
                AuthResult.Success -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun verifyOtp(code: String) {
        val phone = _uiState.value.pendingPhone
        if (phone == null) {
            _uiState.update { it.copy(otpStep = OtpStep.ENTER_PHONE) }
            return
        }
        if (code.isBlank()) {
            _uiState.update { it.copy(error = CODE_REQUIRED) }
            return
        }
        submit(action = { authManager.verifyOtp(phone, code.trim()) })
    }

    fun backToPhoneEntry() = _uiState.update {
        it.copy(otpStep = OtpStep.ENTER_PHONE, error = null, info = null)
    }

    /**
     * Runs an auth action. On [AuthResult.Success] the session flips and the root gate navigates
     * away automatically — no explicit navigation here. [onSuccessNoSession] covers sign-up when
     * email confirmation is enabled (success returned, but no active session yet).
     */
    private fun submit(
        action: suspend () -> AuthResult,
        onSuccessNoSession: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, info = null) }
            when (val result = action()) {
                is AuthResult.Success -> {
                    if (authManager.currentUserId == null && onSuccessNoSession != null) {
                        onSuccessNoSession()
                    }
                    _uiState.update { it.copy(isSubmitting = false) }
                }
                is AuthResult.Error -> _uiState.update { it.copy(isSubmitting = false, error = friendly(result.message)) }
                is AuthResult.PendingOtp -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    /** Supabase's send limit, in words; anything else passes through. */
    private fun friendly(message: String): String =
        if (message.contains("rate limit", ignoreCase = true)) EMAIL_RATE_LIMIT else message

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> EMAIL_REQUIRED
        !EMAIL_REGEX.matches(email.trim()) -> EMAIL_INVALID
        else -> null
    }

    // Marker constants resolved to string resources in the screen; kept here to keep the
    // ViewModel free of Android Context. The screen maps these to stringResource ids.
    companion object {
        const val EMAIL_REQUIRED = "email_required"
        const val EMAIL_INVALID = "email_invalid"
        const val PASSWORD_SHORT = "password_short"
        const val NAME_REQUIRED = "name_required"
        const val PHONE_REQUIRED = "phone_required"
        const val CODE_REQUIRED = "code_required"
        const val CHECK_EMAIL = "check_email"
        const val SENT_AGAIN = "sent_again"
        const val SENT_RECENTLY = "sent_recently"
        const val NOT_CONFIRMED = "not_confirmed"
        const val EMAIL_RATE_LIMIT = "email_rate_limit"
        private const val RESEND_GAP_MS = 60_000L
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
