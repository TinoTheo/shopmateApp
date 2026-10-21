package com.shopmate.data.auth

enum class AuthMode { EMAIL_PASSWORD, PHONE_OTP }

object AuthConfig {
    // FLIP THIS to PHONE_OTP once an SMS provider is configured in the Supabase
    // dashboard (Auth → Providers → Phone). Nothing else needs to change.
    val ACTIVE_MODE: AuthMode = AuthMode.EMAIL_PASSWORD
}
