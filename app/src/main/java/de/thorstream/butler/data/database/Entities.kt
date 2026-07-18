package de.thorstream.butler.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaming_entries")
data class StreamingEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val packageName: String,
    val iconReference: String,
    val streamingType: String,
    val customName: String?,
    val hostId: Long?,
    val profileResolution: String,
    val profileFramesPerSecond: Int,
    val profileBitrateMbps: Int,
    val sortOrder: Int,
    val lastUsedAt: Long?,
    val lastNetworkQuality: String?,
    val isDemo: Boolean,
)

@Entity(tableName = "local_hosts")
data class LocalHostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val macAddress: String?,
    val port: Int?,
    val streamingType: String,
    val wakeOnLanEnabled: Boolean,
    val broadcastAddress: String,
    val lastReachable: Boolean?,
    val lastSuccessfulTestAt: Long?,
)

@Entity(tableName = "network_measurements")
data class NetworkMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val connectionType: String,
    val localIpAddress: String?,
    val gateway: String?,
    val ssid: String?,
    val wifiFrequencyMhz: Int?,
    val linkSpeedMbps: Int?,
    val signalStrengthPercent: Int?,
    val internetValidated: Boolean?,
    val dnsReachable: Boolean?,
    val latencyMs: Double?,
    val jitterMs: Double?,
    val packetLossPercent: Double?,
    val downloadMbps: Double?,
    val hostReachable: Boolean?,
    val host: String?,
    val quality: String,
    val summary: String,
    val problems: List<String>,
    val recommendations: List<String>,
)
