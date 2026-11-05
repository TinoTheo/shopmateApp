package com.shopmate.data.auth

import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data object Success : AuthResult()
    data class PendingOtp(val phone: String) : AuthResult() // OTP sent, awaiting code
    data class Error(val message: String) : AuthResult()
}

/**
 * Single seam over the auth provider. The UI branches on [AuthConfig.ACTIVE_MODE]; both the
 * email path (active for the demo) and the phone-OTP path (gated until an SMS provider is
 * configured) funnel into the same signed-in state.
 */
interface AuthManager {
    val currentUserId: String?
    val currentUserEmail: String?
    val isSignedIn: Flow<Boolean>

    // Email path (active for demo)
    suspend fun signUpEmail(email: String, password: String, fullName: String): AuthResult
    suspend fun signInEmail(email: String, password: String): AuthResult

    /** Sends the sign-up confirmation email again, for an account not yet confirmed. */
    suspend fun resendConfirmation(email: String): AuthResult

    // Phone OTP path (built, gated until provider keys exist)
    suspend fun requestOtp(phone: String): AuthResult
    suspend fun verifyOtp(phone: String, code: String): AuthResult

    suspend fun signOut()

    /**
     * True when a session is loaded and the server has not rejected it. Waits for the session to
     * load from [com.shopmate.data.remote.SessionStore] first, so a worker in a cold process does
     * not mistake "not loaded yet" for "signed out".
     */
    suspend fun hasUsableSession(): Boolean

    /** True once sync has been refused with `401`; cleared by the next sign-in or refresh. */
    val needsReauth: Flow<Boolean>

    /** Called by sync on a `401`: stop draining and ask the cashier to sign in again. */
    suspend fun markSessionRejected()
}
