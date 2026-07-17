package de.thorstream.butler.core.validation

import java.net.Inet6Address
import java.net.InetAddress

object NetworkValidators {
    fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) &&
                !(part.length > 1 && part.startsWith('0')) && part.toIntOrNull() in 0..255
        }
    }

    fun isValidIpv6(value: String): Boolean {
        val normalized = normalizeHost(value)
        val pieces = normalized.split('%', limit = 2)
        val address = pieces.first()
        val zone = pieces.getOrNull(1)
        if (!address.contains(':') || zone?.let { it.isEmpty() || it.length > 32 || !it.all(::isZoneCharacter) } == true) return false
        return runCatching { InetAddress.getByName(address) is Inet6Address }.getOrDefault(false)
    }

    fun isValidHostnameOrIp(value: String): Boolean {
        val normalized = normalizeHost(value)
        if (isValidIpv4(normalized) || isValidIpv6(normalized)) return true
        if (normalized.length !in 1..253 || normalized.startsWith('.') || normalized.endsWith('.')) return false
        return normalized.split('.').all { label ->
            label.length in 1..63 && !label.startsWith('-') && !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    fun normalizeHost(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith('[') && trimmed.endsWith(']')) trimmed.substring(1, trimmed.lastIndex) else trimmed
    }

    fun normalizeMac(value: String): String? {
        val compact = value.replace(":", "").replace("-", "").replace(".", "")
        if (compact.length != 12 || compact.any { it.digitToIntOrNull(16) == null }) return null
        return compact.uppercase().chunked(2).joinToString(":")
    }

    fun isValidMac(value: String): Boolean = normalizeMac(value) != null

    private fun isZoneCharacter(value: Char): Boolean = value.isLetterOrDigit() || value == '_' || value == '-' || value == '.'
}
