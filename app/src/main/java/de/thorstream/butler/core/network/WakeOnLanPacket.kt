package de.thorstream.butler.core.network

import de.thorstream.butler.core.validation.NetworkValidators

object WakeOnLanPacket {
    fun create(macAddress: String): ByteArray {
        val normalized = requireNotNull(NetworkValidators.normalizeMac(macAddress)) { "Invalid MAC address" }
        val macBytes = normalized.split(':').map { it.toInt(16).toByte() }
        return ByteArray(6 + 16 * 6).also { packet ->
            repeat(6) { packet[it] = 0xFF.toByte() }
            repeat(16) { repetition ->
                macBytes.forEachIndexed { index, byte -> packet[6 + repetition * 6 + index] = byte }
            }
        }
    }
}

