package de.thorstream.butler.core.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WakeOnLanPacketTest {
    @Test
    fun `magic packet contains sync stream and sixteen mac repetitions`() {
        val packet = WakeOnLanPacket.create("01:23:45:67:89:AB")
        val mac = byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte())
        assertEquals(102, packet.size)
        assertArrayEquals(ByteArray(6) { 0xFF.toByte() }, packet.copyOfRange(0, 6))
        repeat(16) { index -> assertArrayEquals(mac, packet.copyOfRange(6 + index * 6, 12 + index * 6)) }
    }

    @Test
    fun `invalid mac is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { WakeOnLanPacket.create("not-a-mac") }
    }
}

