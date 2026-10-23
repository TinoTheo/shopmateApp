package com.shopmate.data.remote

import com.shopmate.domain.repository.TenancyRefusal
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException

/**
 * The tenancy functions raise named errors (`store_limit`, `staff_limit`, …; see the
 * 20261023090000 migration). PostgREST passes the name through as the error message.
 */
internal fun Throwable.tenancyRefusal(): TenancyRefusal {
    if (this is HttpRequestException) return TenancyRefusal.OFFLINE
    if (this !is RestException) return TenancyRefusal.OTHER
    val text = "$error $message"
    return when {
        "store_limit" in text -> TenancyRefusal.STORE_LIMIT
        "staff_limit" in text -> TenancyRefusal.STAFF_LIMIT
        "licence_expired" in text -> TenancyRefusal.LICENCE_EXPIRED
        "invite_not_found" in text -> TenancyRefusal.INVITE_NOT_FOUND
        "not_allowed" in text || statusCode == 403 || statusCode == 401 -> TenancyRefusal.NOT_ALLOWED
        else -> TenancyRefusal.OTHER
    }
}
