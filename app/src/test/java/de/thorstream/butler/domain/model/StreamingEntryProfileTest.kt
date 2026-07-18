package de.thorstream.butler.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingEntryProfileTest {
    private val wifiProfile = StreamingProfile(StreamingResolution.FULL_HD_1080P, 60, 20)
    private val wiredProfile = StreamingProfile(StreamingResolution.UHD_4K, 120, 80)

    @Test
    fun `ethernet uses the dedicated override when configured`() {
        val entry = StreamingEntry(displayName = "Moonlight", packageName = "com.limelight", profile = wifiProfile, ethernetProfile = wiredProfile)
        assertEquals(wiredProfile, entry.profileFor(ConnectionType.ETHERNET))
    }

    @Test
    fun `ethernet falls back to the default profile without an override`() {
        val entry = StreamingEntry(displayName = "Moonlight", packageName = "com.limelight", profile = wifiProfile)
        assertEquals(wifiProfile, entry.profileFor(ConnectionType.ETHERNET))
    }

    @Test
    fun `all other transports always use the default profile`() {
        val entry = StreamingEntry(displayName = "Moonlight", packageName = "com.limelight", profile = wifiProfile, ethernetProfile = wiredProfile)
        for (type in listOf(ConnectionType.WIFI, ConnectionType.CELLULAR, ConnectionType.VPN, ConnectionType.OTHER, ConnectionType.NONE)) {
            assertEquals(wifiProfile, entry.profileFor(type))
        }
    }
}
