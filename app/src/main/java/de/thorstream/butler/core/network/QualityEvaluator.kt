package de.thorstream.butler.core.network

import de.thorstream.butler.R
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import java.util.Locale
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
class QualityEvaluator @Inject constructor(private val strings: StringProvider) {
    fun evaluate(snapshot: NetworkSnapshot, thresholds: QualityThresholds = QualityThresholds()): QualityAssessment {
        if (snapshot.connectionType == ConnectionType.NONE) {
            return QualityAssessment(
                quality = NetworkQuality.PROBLEMATIC,
                summary = strings.get(R.string.eval_summary_no_connection),
                problems = listOf(strings.get(R.string.eval_problem_no_connection)),
                recommendations = listOf(strings.get(R.string.eval_reco_wired)),
            )
        }

        val critical = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (snapshot.internetValidated == false && snapshot.host == null) critical += strings.get(R.string.eval_problem_no_internet)
        if (snapshot.hostReachable == false) critical += strings.get(R.string.eval_problem_host_unreachable)

        snapshot.latencyMs?.let {
            when {
                it > thresholds.criticalLatencyMs -> critical += strings.get(R.string.eval_problem_high_latency, it.rounded())
                it > thresholds.excellentLatencyMs -> warnings += strings.get(R.string.eval_problem_elevated_latency, it.rounded())
            }
        }
        snapshot.jitterMs?.let {
            when {
                it > thresholds.criticalJitterMs -> critical += strings.get(R.string.eval_problem_high_jitter, it.rounded())
                it > thresholds.goodJitterMs -> warnings += strings.get(R.string.eval_problem_elevated_jitter, it.rounded())
            }
        }
        snapshot.packetLossPercent?.let {
            when {
                it > thresholds.problematicPacketLossPercent -> critical += strings.get(R.string.eval_problem_packet_loss, it.rounded())
                it > 0.0 -> warnings += strings.get(R.string.eval_problem_light_packet_loss, it.rounded())
            }
        }

        if (snapshot.connectionType == ConnectionType.CELLULAR) {
            warnings += strings.get(R.string.eval_problem_cellular)
            recommendations += strings.get(R.string.eval_reco_avoid_cellular)
        }
        if (snapshot.connectionType == ConnectionType.VPN) {
            warnings += strings.get(R.string.eval_problem_vpn)
            recommendations += strings.get(R.string.eval_reco_vpn)
        }
        if (snapshot.connectionType == ConnectionType.WIFI) {
            snapshot.wifiFrequencyMhz?.let { frequency ->
                if (frequency in 2_400..2_500) {
                    warnings += strings.get(R.string.eval_problem_wifi_24)
                    recommendations += strings.get(R.string.eval_reco_switch_band)
                }
            }
            snapshot.signalStrengthPercent?.let { signal ->
                when {
                    signal < thresholds.weakWifiSignalPercent -> critical += strings.get(R.string.eval_problem_weak_signal, signal)
                    signal < thresholds.fairWifiSignalPercent -> warnings += strings.get(R.string.eval_problem_fair_signal, signal)
                }
            }
        }

        if (snapshot.jitterMs != null && snapshot.jitterMs > thresholds.goodJitterMs) {
            recommendations += strings.get(R.string.eval_reco_lower_bitrate)
        }
        if (snapshot.packetLossPercent != null && snapshot.packetLossPercent > 0.0) {
            recommendations += strings.get(R.string.eval_reco_reduce_interference)
        }
        if (snapshot.hostReachable == false) {
            recommendations += strings.get(R.string.eval_reco_check_host)
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
            NetworkQuality.OPTIMAL -> strings.get(R.string.eval_summary_optimal)
            NetworkQuality.USABLE -> strings.get(R.string.eval_summary_usable, warnings.firstOrNull().orEmpty())
            NetworkQuality.PROBLEMATIC -> strings.get(R.string.eval_summary_problematic, critical.firstOrNull().orEmpty())
            NetworkQuality.NOT_MEASURABLE -> strings.get(R.string.eval_summary_not_measurable)
        }
        return QualityAssessment(quality, summary, problems.distinct(), recommendations.distinct())
    }
}

private fun Double.rounded(): String = String.format(Locale.getDefault(), "%.1f", this)
