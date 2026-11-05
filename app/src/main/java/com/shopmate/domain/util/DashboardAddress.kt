package com.shopmate.domain.util

import java.net.URI

/**
 * The guard on a dashboard address ShopMate sets remotely (§ Remote settings). Owners type their
 * email and password on whatever page the app opens, so a remote value is accepted only when it
 * is `https`, has no login part, port, query or fragment, and sits on a domain the APK already
 * trusts ([trustedDomains], each covering its subdomains). Anything else is ignored and the app
 * keeps the address it had — a typo or a tampered row can't send an owner to another site.
 */
class DashboardAddress(trustedDomains: String) {

    private val domains = trustedDomains.split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }

    /** [raw] as a clean address without a trailing slash, or null when it isn't trusted. */
    fun accept(raw: String?): String? {
        val text = raw?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: return null
        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.rawUserInfo != null || uri.port != -1) return null
        if (uri.rawQuery != null || uri.rawFragment != null) return null
        val host = uri.host?.lowercase() ?: return null
        if (domains.none { host == it || host.endsWith(".$it") }) return null
        return "https://$host${uri.rawPath.orEmpty()}"
    }
}
