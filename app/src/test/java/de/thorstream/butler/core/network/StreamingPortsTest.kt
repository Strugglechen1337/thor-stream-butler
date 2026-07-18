package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.LocalHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingPortsTest {
    @Test
    fun `host without a port probes only the curated list`() {
        val probes = StreamingPorts.probesFor(LocalHost(id = 1, name = "PC", address = "pc.local", port = null))
        assertEquals(StreamingPorts.KNOWN, probes)
    }

    @Test
    fun `a custom port is probed first and marked as the configured port`() {
        val probes = StreamingPorts.probesFor(LocalHost(id = 1, name = "PC", address = "pc.local", port = 8000))
        assertEquals(8000, probes.first().port)
        assertEquals(null, probes.first().serviceName)
        assertEquals(StreamingPorts.KNOWN.size + 1, probes.size)
    }

    @Test
    fun `a configured port already in the curated list is not duplicated`() {
        val known = StreamingPorts.KNOWN.first().port
        val probes = StreamingPorts.probesFor(LocalHost(id = 1, name = "PC", address = "pc.local", port = known))
        assertEquals(StreamingPorts.KNOWN.size, probes.size)
        assertEquals(1, probes.count { it.port == known })
    }

    @Test
    fun `curated ports carry service labels and are unique`() {
        assertTrue(StreamingPorts.KNOWN.all { it.serviceName != null })
        assertEquals(StreamingPorts.KNOWN.size, StreamingPorts.KNOWN.map { it.port }.toSet().size)
    }
}
