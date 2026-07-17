package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.model.StreamingResolution
import javax.inject.Inject

enum class RecommendationReason {
    MEASUREMENT_INCOMPLETE,
    CONNECTION_UNSTABLE,
    BALANCED,
    FAST_CONNECTION,
    PREMIUM_WIRED,
}

data class StreamingRecommendation(
    val resolution: StreamingResolution,
    val framesPerSecond: Int,
    val bitrateMbps: Int,
    val reason: RecommendationReason,
)

/**
 * Produces conservative client settings from values that matter for interactive
 * streaming. It deliberately does not treat download speed as the only signal.
 */
class StreamingRecommendationEngine @Inject constructor() {
    fun recommend(snapshot: NetworkSnapshot, assessment: QualityAssessment): StreamingRecommendation {
        val latency = snapshot.latencyMs
        val jitter = snapshot.jitterMs
        val loss = snapshot.packetLossPercent
        val download = snapshot.downloadMbps

        if (assessment.quality == NetworkQuality.NOT_MEASURABLE || latency == null || jitter == null || loss == null) {
            return StreamingRecommendation(StreamingResolution.HD_720P, 30, 8, RecommendationReason.MEASUREMENT_INCOMPLETE)
        }

        if (
            assessment.quality == NetworkQuality.PROBLEMATIC ||
            latency > 60.0 || jitter > 20.0 || loss > 1.0 || snapshot.hostReachable == false
        ) {
            return StreamingRecommendation(StreamingResolution.HD_720P, 30, 8, RecommendationReason.CONNECTION_UNSTABLE)
        }

        if (assessment.quality == NetworkQuality.USABLE || latency > 40.0 || jitter > 10.0 || loss > 0.0) {
            return StreamingRecommendation(StreamingResolution.HD_720P, 60, 12, RecommendationReason.BALANCED)
        }

        if (
            snapshot.connectionType == ConnectionType.ETHERNET &&
            latency <= 20.0 && jitter <= 5.0 && loss == 0.0 && download != null && download >= 80.0
        ) {
            return StreamingRecommendation(StreamingResolution.UHD_4K, 60, 65, RecommendationReason.PREMIUM_WIRED)
        }

        if (download != null && download >= 50.0) {
            return StreamingRecommendation(StreamingResolution.QHD_1440P, 60, 40, RecommendationReason.FAST_CONNECTION)
        }

        return StreamingRecommendation(StreamingResolution.FULL_HD_1080P, 60, 25, RecommendationReason.BALANCED)
    }
}
