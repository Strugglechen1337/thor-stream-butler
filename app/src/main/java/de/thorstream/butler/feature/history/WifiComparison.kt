package de.thorstream.butler.feature.history

import de.thorstream.butler.core.network.QualityThresholds
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkMeasurement
import kotlin.math.roundToInt

enum class WifiComparisonConfidence { LOW, MEDIUM, HIGH }

data class WifiNetworkComparison(
    val ssid: String,
    val measurementCount: Int,
    val averageLatencyMs: Double?,
    val averageJitterMs: Double?,
    val averagePacketLossPercent: Double?,
    val averageSignalPercent: Double?,
    val averageLinkSpeedMbps: Double?,
    val stabilityScore: Int?,
    val confidence: WifiComparisonConfidence,
    val lastMeasuredAt: Long,
    val isBestMeasured: Boolean = false,
)

data class WifiComparisonSummary(
    val networks: List<WifiNetworkComparison> = emptyList(),
    val wifiMeasurementCount: Int = 0,
    val measurementsWithoutSsid: Int = 0,
)

/**
 * Compares only Wi-Fi measurements that already exist in local history. This
 * deliberately does not scan nearby networks and never probes another host.
 * SSIDs are case-sensitive and remain on the device.
 */
internal fun buildWifiComparison(
    measurements: List<NetworkMeasurement>,
    thresholds: QualityThresholds = QualityThresholds(),
): WifiComparisonSummary {
    val wifiMeasurements = measurements.filter { it.snapshot.connectionType == ConnectionType.WIFI }
    val namedMeasurements = wifiMeasurements.mapNotNull { measurement ->
        measurement.snapshot.ssid
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("<unknown ssid>", ignoreCase = true) }
            ?.let { it to measurement }
    }

    val ranked = namedMeasurements
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .map { (ssid, samples) -> samples.toWifiComparison(ssid, thresholds) }
        .sortedWith(
            compareByDescending<WifiNetworkComparison> { it.stabilityScore != null }
                .thenByDescending { it.stabilityScore ?: -1 }
                .thenByDescending { it.measurementCount }
                .thenBy { it.averageLatencyMs ?: Double.MAX_VALUE }
                .thenBy { it.ssid.lowercase() },
        )

    val comparableNetworks = ranked.count { it.stabilityScore != null }
    val bestSsid = ranked.firstOrNull { it.stabilityScore != null }
        ?.ssid
        ?.takeIf { comparableNetworks >= 2 }

    return WifiComparisonSummary(
        networks = ranked.map { it.copy(isBestMeasured = it.ssid == bestSsid) },
        wifiMeasurementCount = wifiMeasurements.size,
        measurementsWithoutSsid = wifiMeasurements.size - namedMeasurements.size,
    )
}

private fun List<NetworkMeasurement>.toWifiComparison(
    ssid: String,
    thresholds: QualityThresholds,
): WifiNetworkComparison {
    val latency = mapNotNull { it.snapshot.latencyMs }.averageOrNull()
    val jitter = mapNotNull { it.snapshot.jitterMs }.averageOrNull()
    val loss = mapNotNull { it.snapshot.packetLossPercent }.averageOrNull()
    val signal = mapNotNull { it.snapshot.signalStrengthPercent?.toDouble() }.averageOrNull()
    val linkSpeed = mapNotNull { it.snapshot.linkSpeedMbps?.toDouble() }.averageOrNull()
    val comparableMeasurementCount = count {
        it.snapshot.latencyMs != null ||
            it.snapshot.jitterMs != null ||
            it.snapshot.packetLossPercent != null
    }

    val scoreParts = listOfNotNull(
        latency?.let {
            WeightedScore(
                lowerIsBetter(it, thresholds.excellentLatencyMs, thresholds.criticalLatencyMs * 2.0),
                35,
            )
        },
        jitter?.let {
            WeightedScore(
                lowerIsBetter(it, thresholds.goodJitterMs, thresholds.criticalJitterMs * 2.0),
                25,
            )
        },
        loss?.let {
            WeightedScore(
                lowerIsBetter(it, 0.0, thresholds.problematicPacketLossPercent * 5.0),
                25,
            )
        },
        signal?.let {
            WeightedScore(
                higherIsBetter(it, (thresholds.weakWifiSignalPercent - 15).toDouble(), thresholds.fairWifiSignalPercent.toDouble()),
                10,
            )
        },
        linkSpeed?.let {
            WeightedScore(
                higherIsBetter(it, thresholds.criticalWifiLinkSpeedMbps.toDouble(), thresholds.recommendedWifiLinkSpeedMbps.toDouble()),
                5,
            )
        },
    )
    val score = scoreParts.takeIf { comparableMeasurementCount > 0 && it.isNotEmpty() }?.let { parts ->
        (parts.sumOf { it.value * it.weight } / parts.sumOf { it.weight }).roundToInt().coerceIn(0, 100)
    }

    return WifiNetworkComparison(
        ssid = ssid,
        measurementCount = size,
        averageLatencyMs = latency,
        averageJitterMs = jitter,
        averagePacketLossPercent = loss,
        averageSignalPercent = signal,
        averageLinkSpeedMbps = linkSpeed,
        stabilityScore = score,
        confidence = when (comparableMeasurementCount) {
            in 0..1 -> WifiComparisonConfidence.LOW
            in 2..4 -> WifiComparisonConfidence.MEDIUM
            else -> WifiComparisonConfidence.HIGH
        },
        lastMeasuredAt = maxOf { it.timestamp },
    )
}

private data class WeightedScore(val value: Double, val weight: Int)

private fun lowerIsBetter(value: Double, ideal: Double, poor: Double): Double = when {
    value <= ideal -> 100.0
    value >= poor -> 0.0
    else -> (poor - value) / (poor - ideal) * 100.0
}

private fun higherIsBetter(value: Double, poor: Double, ideal: Double): Double = when {
    value <= poor -> 0.0
    value >= ideal -> 100.0
    else -> (value - poor) / (ideal - poor) * 100.0
}

private fun List<Double>.averageOrNull(): Double? = takeIf { it.isNotEmpty() }?.average()
