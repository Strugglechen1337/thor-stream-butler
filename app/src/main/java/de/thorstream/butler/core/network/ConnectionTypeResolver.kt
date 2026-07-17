package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.ConnectionType

/**
 * Resolves Android transport flags into the single connection type shown by
 * diagnostics. VPN deliberately wins over its underlying transport: users
 * need to know that their streaming traffic is routed through a tunnel even
 * when Android also reports Wi-Fi, Ethernet, or cellular connectivity.
 */
object ConnectionTypeResolver {
    fun resolve(
        hasVpn: Boolean,
        hasEthernet: Boolean,
        hasWifi: Boolean,
        hasCellular: Boolean,
    ): ConnectionType = when {
        hasVpn -> ConnectionType.VPN
        hasEthernet -> ConnectionType.ETHERNET
        hasWifi -> ConnectionType.WIFI
        hasCellular -> ConnectionType.CELLULAR
        else -> ConnectionType.OTHER
    }
}
