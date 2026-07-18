package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.model.StreamingResolution
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingRecommendationEngineTest {
    private val engine = StreamingRecommendationEngine()

    @Test
    fun `excellent wired measurement recommends 4k`() {
        val result = engine.recommend(
            snapshot = NetworkSnapshot(
                connectionType = ConnectionType.ETHERNET,
                latencyMs = 15.0,
                jitterMs = 2.0,
                packetLossPercent = 0.0,
                downloadMbps = 100.0,
            ),
            assessment = QualityAssessment(NetworkQuality.OPTIMAL, "optimal"),
        )

        assertEquals(StreamingResolution.UHD_4K, result.resolution)
        assertEquals(65, result.bitrateMbps)
    }

    @Test
    fun `packet loss keeps recommendation conservative`() {
        val result = engine.recommend(
            snapshot = NetworkSnapshot(
                connectionType = ConnectionType.WIFI,
                latencyMs = 25.0,
                jitterMs = 8.0,
                packetLossPercent = 2.0,
                downloadMbps = 120.0,
            ),
            assessment = QualityAssessment(NetworkQuality.PROBLEMATIC, "loss"),
        )

        assertEquals(StreamingResolution.HD_720P, result.resolution)
        assertEquals(30, result.framesPerSecond)
        assertEquals(8, result.bitrateMbps)
    }

    @Test
    fun `missing latency never invents a high quality profile`() {
        val result = engine.recommend(
            snapshot = NetworkSnapshot(ConnectionType.WIFI, downloadMbps = 100.0),
            assessment = QualityAssessment(NetworkQuality.NOT_MEASURABLE, "missing"),
        )

        assertEquals(RecommendationReason.MEASUREMENT_INCOMPLETE, result.reason)
        assertEquals(StreamingResolution.HD_720P, result.resolution)
    }
}
