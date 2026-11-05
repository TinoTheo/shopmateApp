package com.shopmate.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardAddressTest {

    private val guard = DashboardAddress("shopmate-remote.vercel.app, shopmate.africa")

    @Test
    fun `accepts a trusted domain and its subdomains`() {
        assertEquals("https://shopmate-remote.vercel.app", guard.accept("https://shopmate-remote.vercel.app/"))
        assertEquals("https://app.shopmate.africa", guard.accept(" https://APP.shopmate.africa "))
        assertEquals("https://shopmate.africa/owners", guard.accept("https://shopmate.africa/owners/"))
    }

    @Test
    fun `refuses look-alikes and other sites`() {
        assertNull(guard.accept("https://shopmate.africa.evil.com"))
        assertNull(guard.accept("https://evilshopmate.africa"))
        assertNull(guard.accept("https://shopmate-remote-login.vercel.app"))
        assertNull(guard.accept("https://example.com"))
    }

    @Test
    fun `refuses anything but a plain https address`() {
        assertNull(guard.accept("http://shopmate.africa"))
        assertNull(guard.accept("https://owner@shopmate.africa"))
        assertNull(guard.accept("https://shopmate.africa:8443"))
        assertNull(guard.accept("https://shopmate.africa/?next=https://example.com"))
        assertNull(guard.accept("https://shopmate.africa/#x"))
        assertNull(guard.accept("javascript:alert(1)"))
        assertNull(guard.accept("not an address"))
        assertNull(guard.accept(""))
        assertNull(guard.accept(null))
    }

    @Test
    fun `trusts nothing when no domains are configured`() {
        assertNull(DashboardAddress("").accept("https://shopmate-remote.vercel.app"))
    }
}
