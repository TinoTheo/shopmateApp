package com.shopmate.data.auth

import com.shopmate.data.remote.SessionStore
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthManager @Inject constructor(
    private val auth: Auth,
    private val sessionStore: SessionStore,
) : AuthManager {

    override val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    override val currentUserEmail: String?
        get() = auth.currentUserOrNull()?.email

    override val isSignedIn: Flow<Boolean> =
        auth.sessionStatus.map { it is SessionStatus.Authenticated }

    override suspend fun signUpEmail(email: String, password: String, fullName: String): AuthResult =
        runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject { put("full_name", fullName) }
            }
        }.toAuthResult()

    override suspend fun signInEmail(email: String, password: String): AuthResult =
        runCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }.toAuthResult()

    // Sends the SMS — fails gracefully if no provider is configured in the dashboard.
    override suspend fun requestOtp(phone: String): AuthResult =
        runCatching {
            auth.signInWith(OTP) {
                this.phone = phone
                createUser = true
            }
        }.fold(
            onSuccess = { AuthResult.PendingOtp(phone) },
            onFailure = { AuthResult.Error(it.userMessage()) },
        )

    override suspend fun verifyOtp(phone: String, code: String): AuthResult =
        runCatching {
            auth.verifyPhoneOtp(type = OtpType.Phone.SMS, phone = phone, token = code)
        }.toAuthResult()

    override suspend fun signOut() {
        runCatching { auth.signOut() }
    }

    override suspend fun hasUsableSession(): Boolean {
        auth.awaitInitialization()
        return auth.currentSessionOrNull() != null && !sessionStore.rejected.first()
    }

    override val needsReauth: Flow<Boolean> =
        combine(auth.sessionStatus, sessionStore.rejected) { status, rejected ->
            status !is SessionStatus.Authenticated || rejected
        }

    override suspend fun markSessionRejected() = sessionStore.markRejected()

    private fun Result<*>.toAuthResult(): AuthResult = fold(
        onSuccess = { AuthResult.Success },
        onFailure = { AuthResult.Error(it.userMessage()) },
    )

    private fun Throwable.userMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}
