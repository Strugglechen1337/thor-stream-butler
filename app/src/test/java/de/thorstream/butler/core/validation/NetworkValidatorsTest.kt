package de.thorstream.butler.core.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkValidatorsTest {
    @Test
    fun `valid IPv4 addresses are accepted`() {
        assertTrue(NetworkValidators.isValidIpv4("192.168.1.20"))
        assertTrue(NetworkValidators.isValidIpv4("0.0.0.0"))
        assertTrue(NetworkValidators.isValidIpv4("255.255.255.255"))
    }

    @Test
    fun `invalid IPv4 addresses are rejected`() {
        assertFalse(NetworkValidators.isValidIpv4("192.168.1.256"))
        assertFalse(NetworkValidators.isValidIpv4("192.168.1"))
        assertFalse(NetworkValidators.isValidIpv4("192.168.01.1"))
    }

    @Test
    fun `IPv6 addresses including scoped and bracketed forms are accepted`() {
        assertTrue(NetworkValidators.isValidIpv6("2001:db8::1"))
        assertTrue(NetworkValidators.isValidIpv6("[fd00::4798]"))
        assertTrue(NetworkValidators.isValidIpv6("fe80::1%wlan0"))
        assertTrue(NetworkValidators.isValidHostnameOrIp("2001:db8::1"))
        assertEquals("fd00::4798", NetworkValidators.normalizeHost("[fd00::4798]"))
    }

    @Test
    fun `malformed IPv6 addresses and zones are rejected`() {
        assertFalse(NetworkValidators.isValidIpv6("2001:db8:::1"))
        assertFalse(NetworkValidators.isValidIpv6("fe80::1%bad zone"))
        assertFalse(NetworkValidators.isValidIpv6("example.com"))
        assertFalse(NetworkValidators.isValidHostnameOrIp("[2001:db8::1"))
        assertFalse(NetworkValidators.isValidHostnameOrIp("2001:db8::1]"))
    }

    @Test
    fun `MAC formats are normalized`() {
        assertTrue(NetworkValidators.isValidMac("01-23-45-67-89-ab"))
        assertEquals("01:23:45:67:89:AB", NetworkValidators.normalizeMac("0123.4567.89ab"))
        assertFalse(NetworkValidators.isValidMac("01:23:45"))
    }
}
