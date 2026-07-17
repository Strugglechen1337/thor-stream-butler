package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import javax.inject.Inject
import javax.inject.Singleton

data class QualityThresholds(
    val excellentLatencyMs: Double = 30.0,
    val criticalLatencyMs: Double = 60.0,
    val goodJitterMs: Double = 10.0,
    val criticalJitterMs: Double = 20.0,
    val problematicPacketLossPercent: Double = 1.0,
    val weakWifiSignalPercent: Int = 35,
    val fairWifiSignalPercent: Int = 55,
)

@Singleton
class QualityEvaluator @Inject constructor() {
    fun evaluate(snapshot: NetworkSnapshot, thresholds: QualityThresholds = QualityThresholds()): QualityAssessment {
        if (snapshot.connectionType == ConnectionType.NONE) {
            return QualityAssessment(
                quality = NetworkQuality.PROBLEMATIC,
                summary = "Keine aktive Netzwerkverbindung. Streaming ist derzeit nicht möglich.",
                problems = listOf("Keine aktive Verbindung"),
                recommendations = listOf("Verbinde das Gerät per Ethernet oder mit einem stabilen 5-/6-GHz-WLAN."),
            )
        }

        val critical = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (snapshot.internetValidated == false && snapshot.host == null) critical += "Kein bestätigter Internetzugang"
        if (snapshot.hostReachable == false) critical += "Streaming-Host nicht erreichbar"

        snapshot.latencyMs?.let {
            when {
                it > thresholds.criticalLatencyMs -> critical += "Hohe Latenz (${it.rounded()} ms)"
                it > thresholds.excellentLatencyMs -> warnings += "Erhöhte Latenz (${it.rounded()} ms)"
            }
        }
        snapshot.jitterMs?.let {
            when {
                it > thresholds.criticalJitterMs -> critical += "Starker Jitter (${it.rounded()} ms)"
                it > thresholds.goodJitterMs -> warnings += "Erhöhter Jitter (${it.rounded()} ms)"
            }
        }
        snapshot.packetLossPercent?.let {
            when {
                it > thresholds.problematicPacketLossPercent -> critical += "Paketverlust (${it.rounded()} %)"
                it > 0.0 -> warnings += "Leichter Paketverlust (${it.rounded()} %)"
            }
        }

        if (snapshot.connectionType == ConnectionType.CELLULAR) {
            warnings += "Mobilfunkverbindung"
            recommendations += "Nutze für gleichmäßiges Streaming nach Möglichkeit Ethernet oder WLAN."
        }
        if (snapshot.connectionType == ConnectionType.WIFI) {
            snapshot.wifiFrequencyMhz?.let { frequency ->
                if (frequency in 2_400..2_500) {
                    warnings += "2,4-GHz-WLAN"
                    recommendations += "Wechsle nach Möglichkeit auf 5- oder 6-GHz-WLAN."
                }
            }
            snapshot.signalStrengthPercent?.let { signal ->
                when {
                    signal < thresholds.weakWifiSignalPercent -> critical += "Sehr schwaches WLAN-Signal ($signal %)"
                    signal < thresholds.fairWifiSignalPercent -> warnings += "Schwaches WLAN-Signal ($signal %)"
                }
            }
        }

        if (snapshot.jitterMs != null && snapshot.jitterMs > thresholds.goodJitterMs) {
            recommendations += "Reduziere bei Bildaussetzern die Streaming-Bitrate."
        }
        if (snapshot.packetLossPercent != null && snapshot.packetLossPercent > 0.0) {
            recommendations += "Verringere Funkstörungen oder verwende eine kabelgebundene Verbindung."
        }
        if (snapshot.hostReachable == false) {
            recommendations += "Prüfe Host-Adresse, Port, Firewall und ob der Streaming-Host eingeschaltet ist."
        }

        val hasCoreMeasurement = snapshot.latencyMs != null || snapshot.packetLossPercent != null || snapshot.hostReachable != null
        val quality = when {
            critical.isNotEmpty() -> NetworkQuality.PROBLEMATIC
            !hasCoreMeasurement -> NetworkQuality.NOT_MEASURABLE
            warnings.isNotEmpty() -> NetworkQuality.USABLE
            else -> NetworkQuality.OPTIMAL
        }
        val problems = critical + warnings
        val summary = when (quality) {
            NetworkQuality.OPTIMAL -> "Die Verbindung ist für Gaming-Streaming sehr gut geeignet."
            NetworkQuality.USABLE -> "Die Verbindung ist grundsätzlich geeignet. ${warnings.firstOrNull().orEmpty()} kann die Qualität beeinträchtigen."
            NetworkQuality.PROBLEMATIC -> "Die Verbindung ist aktuell problematisch: ${critical.firstOrNull().orEmpty()}."
            NetworkQuality.NOT_MEASURABLE -> "Die Kernwerte konnten nicht zuverlässig gemessen werden. Verfügbare Basisdaten werden trotzdem angezeigt."
        }
        return QualityAssessment(quality, summary, problems.distinct(), recommendations.distinct())
    }
}

private fun Double.rounded(): String = String.format(java.util.Locale.GERMANY, "%.1f", this)

