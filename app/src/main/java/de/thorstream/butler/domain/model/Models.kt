package de.thorstream.butler.domain.model

enum class StreamingType(val displayName: String) {
    GEFORCE_NOW("GeForce NOW"),
    XBOX_CLOUD("Xbox Cloud Gaming"),
    XBOX_REMOTE("Xbox Remote Play / XBPlay"),
    PLAYSTATION_REMOTE("PlayStation Remote Play / PXPlay"),
    MOONLIGHT("Moonlight"),
    STEAM_LINK("Steam Link"),
    SUNSHINE_HOST("Sunshine Host"),
    CUSTOM("Benutzerdefiniert"),
}

enum class ConnectionType(val displayName: String) {
    ETHERNET("Ethernet"),
    WIFI("WLAN"),
    CELLULAR("Mobilfunk"),
    VPN("VPN"),
    OTHER("Andere"),
    NONE("Keine Verbindung"),
}

enum class NetworkQuality(val displayName: String) {
    OPTIMAL("Optimal"),
    USABLE("Nutzbar"),
    PROBLEMATIC("Problematisch"),
    NOT_MEASURABLE("Nicht messbar"),
}

data class StreamingEntry(
    val id: Long = 0,
    val displayName: String,
    val packageName: String,
    val iconReference: String = "package://$packageName",
    val streamingType: StreamingType = StreamingType.CUSTOM,
    val customName: String? = null,
    val sortOrder: Int = 0,
    val lastUsedAt: Long? = null,
    val lastNetworkQuality: NetworkQuality? = null,
    val isDemo: Boolean = false,
)

data class InstalledApp(
    val label: String,
    val packageName: String,
    val iconReference: String = "package://$packageName",
)

data class LocalHost(
    val id: Long = 0,
    val name: String,
    val address: String,
    val macAddress: String? = null,
    val port: Int? = null,
    val streamingType: StreamingType = StreamingType.CUSTOM,
    val wakeOnLanEnabled: Boolean = false,
    val broadcastAddress: String = "255.255.255.255",
    val lastReachable: Boolean? = null,
    val lastSuccessfulTestAt: Long? = null,
)

data class NetworkSnapshot(
    val connectionType: ConnectionType,
    val localIpAddress: String? = null,
    val gateway: String? = null,
    val ssid: String? = null,
    val wifiFrequencyMhz: Int? = null,
    val linkSpeedMbps: Int? = null,
    val signalStrengthPercent: Int? = null,
    val internetValidated: Boolean? = null,
    val dnsReachable: Boolean? = null,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val packetLossPercent: Double? = null,
    val downloadMbps: Double? = null,
    val hostReachable: Boolean? = null,
    val host: String? = null,
)

data class QualityAssessment(
    val quality: NetworkQuality,
    val summary: String,
    val problems: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
)

data class NetworkMeasurement(
    val id: Long = 0,
    val timestamp: Long,
    val snapshot: NetworkSnapshot,
    val assessment: QualityAssessment,
)

data class AppSettings(
    val preLaunchCheckEnabled: Boolean = true,
    val autoLaunchOnGreen: Boolean = true,
    val warnOnYellow: Boolean = true,
    val confirmOnRed: Boolean = true,
    val defaultTestTarget: String = "1.1.1.1",
    val pingCount: Int = 5,
    val testDurationSeconds: Int = 5,
    val downloadTestEnabled: Boolean = false,
    val theme: ThemePreference = ThemePreference.DARK,
    val focusAnimationsEnabled: Boolean = true,
    val diagnosticLoggingEnabled: Boolean = false,
)

enum class ThemePreference { DARK, SYSTEM }

