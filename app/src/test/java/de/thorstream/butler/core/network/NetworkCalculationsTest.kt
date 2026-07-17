package de.thorstream.butler.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkCalculationsTest {
    @Test
    fun `jitter is mean absolute difference between consecutive samples`() {
        assertEquals(10.0, NetworkCalculations.jitterMs(listOf(20.0, 30.0, 20.0))!!, 0.001)
    }

    @Test
    fun `jitter is unavailable for fewer than two samples`() {
        assertNull(NetworkCalculations.jitterMs(listOf(20.0)))
    }

    @Test
    fun `packet loss uses sent and received packet counts`() {
        assertEquals(20.0, NetworkCalculations.packetLossPercent(sentCount = 5, receivedCount = 4)!!, 0.001)
        assertEquals(0.0, NetworkCalculations.packetLossPercent(sentCount = 5, receivedCount = 5)!!, 0.001)
    }

    @Test
    fun `packet loss is unavailable without sent packets`() {
        assertNull(NetworkCalculations.packetLossPercent(0, 0))
    }
}
