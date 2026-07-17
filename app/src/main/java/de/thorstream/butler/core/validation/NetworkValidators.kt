package de.thorstream.butler.core.validation

object NetworkValidators {
    fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) &&
                !(part.length > 1 && part.startsWith('0')) && part.toIntOrNull() in 0..255
        }
    }

    fun isValidHostnameOrIpv4(value: String): Boolean {
        if (isValidIpv4(value)) return true
        if (value.length !in 1..253 || value.startsWith('.') || value.endsWith('.')) return false
        return value.split('.').all { label ->
            label.length in 1..63 && !label.startsWith('-') && !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    fun normalizeMac(value: String): String? {
        val compact = value.replace(":", "").replace("-", "").replace(".", "")
        if (compact.length != 12 || compact.any { it.digitToIntOrNull(16) == null }) return null
        return compact.uppercase().chunked(2).joinToString(":")
    }

    fun isValidMac(value: String): Boolean = normalizeMac(value) != null
}

