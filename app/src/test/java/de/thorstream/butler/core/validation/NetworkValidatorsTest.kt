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
    fun `MAC formats are normalized`() {
        assertTrue(NetworkValidators.isValidMac("01-23-45-67-89-ab"))
        assertEquals("01:23:45:67:89:AB", NetworkValidators.normalizeMac("0123.4567.89ab"))
        assertFalse(NetworkValidators.isValidMac("01:23:45"))
    }
}

