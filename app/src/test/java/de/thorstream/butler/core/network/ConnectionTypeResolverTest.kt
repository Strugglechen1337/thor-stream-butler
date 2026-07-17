package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionTypeResolverTest {
    @Test
    fun `vpn takes precedence over every underlying transport`() {
        val result = ConnectionTypeResolver.resolve(
            hasVpn = true,
            hasEthernet = true,
            hasWifi = true,
            hasCellular = true,
        )

        assertEquals(ConnectionType.VPN, result)
    }

    @Test
    fun `wired and wifi transports retain their preferred order without vpn`() {
        val ethernet = ConnectionTypeResolver.resolve(
            hasVpn = false,
            hasEthernet = true,
            hasWifi = true,
            hasCellular = true,
        )
        val wifi = ConnectionTypeResolver.resolve(
            hasVpn = false,
            hasEthernet = false,
            hasWifi = true,
            hasCellular = true,
        )

        assertEquals(ConnectionType.ETHERNET, ethernet)
        assertEquals(ConnectionType.WIFI, wifi)
    }

    @Test
    fun `unknown transport is reported as other`() {
        val result = ConnectionTypeResolver.resolve(
            hasVpn = false,
            hasEthernet = false,
            hasWifi = false,
            hasCellular = false,
        )

        assertEquals(ConnectionType.OTHER, result)
    }
}
