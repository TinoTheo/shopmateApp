package com.shopmate.data.auth

import com.shopmate.BuildConfig
import com.shopmate.data.remote.SessionStore
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class SupabaseAuthManager @Inject constructor(
    private val auth: Auth,
    private val sessionStore: SessionStore,
) : AuthManager {

    /** Session status once the stored session has been read; [SessionStatus.Initializing] says nothing yet. */
    private val knownStatus: Flow<SessionStatus> =
        auth.sessionStatus.filterNot { it is SessionStatus.Initializing }

    override val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    override val currentUserEmail: String?
        get() = auth.currentUserOrNull()?.email

    /**
     * Signed in means a session is held on this device — not that the server is reachable.
     * Offline, supabase-kt's token refresh fails and it reports [SessionStatus.RefreshFailure]
     * while it retries; that is the network being away, not the cashier being signed out, so it
     * must not send the till to the sign-in screen. Only [SessionStatus.NotAuthenticated] does.
     */
    override val isSignedIn: Flow<Boolean> =
        knownStatus.map { it.holdsSession() }.distinctUntilChanged()

    override suspend fun signUpEmail(email: String, password: String, fullName: String): AuthResult =
        runCatching {
            auth.signUpWith(Email, redirectUrl = confirmedPage()) {
                this.email = email
                this.password = password
                data = buildJsonObject { put("full_name", fullName) }
            }
        }.toAuthResult()

    override suspend fun resendConfirmation(email: String): AuthResult =
        runCatching { auth.resendEmail(OtpType.Email.SIGNUP, email) }.toAuthResult()

    /**
     * Where the confirmation link lands: the web dashboard's "You're confirmed" page, which tells
     * the owner to come back to the app. Without a dashboard address, Supabase's Site URL.
     */
    private fun confirmedPage(): String? =
        BuildConfig.WEB_DASHBOARD_URL.trimEnd('/').takeIf { it.isNotBlank() }?.let { "$it/confirmed/" }

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

    /**
     * An expired access token is refreshed here first, so a worker that runs before supabase-kt's
     * own refresh never uploads with it — the server would answer 401 and the till would be told
     * to sign in again over what was only a stale token. A refresh that fails (offline) means
     * "not now", never "rejected".
     */
    override suspend fun hasUsableSession(): Boolean {
        auth.awaitInitialization()
        val session = auth.currentSessionOrNull() ?: return false
        if (sessionStore.rejected.first()) return false
        if (session.expiresAt <= Clock.System.now() + EXPIRY_MARGIN) {
            runCatching { auth.refreshCurrentSession() }.onFailure { return false }
        }
        return true
    }

    /**
     * True only when the cashier has to sign in again: signed out, or the server refused the
     * session. A refresh failing offline is not that — and treating it as such made this flip on
     * every 10-second retry, restarting the sync trigger each time.
     */
    override val needsReauth: Flow<Boolean> =
        combine(knownStatus, sessionStore.rejected) { status, rejected ->
            !status.holdsSession() || rejected
        }.distinctUntilChanged()

    override suspend fun markSessionRejected() = sessionStore.markRejected()

    private fun SessionStatus.holdsSession(): Boolean =
        this is SessionStatus.Authenticated || this is SessionStatus.RefreshFailure

    private fun Result<*>.toAuthResult(): AuthResult = fold(
        onSuccess = { AuthResult.Success },
        onFailure = { AuthResult.Error(it.userMessage()) },
    )

    private fun Throwable.userMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."

    private companion object {
        /** Refresh a token this close to expiry before using it, so it can't lapse mid-upload. */
        val EXPIRY_MARGIN = 60.seconds
    }
}
