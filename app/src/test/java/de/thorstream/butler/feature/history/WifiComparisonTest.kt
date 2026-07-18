package de.thorstream.butler.feature.history

import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiComparisonTest {
    @Test
    fun `comparison groups named wifi measurements and ranks the most stable network`() {
        val result = buildWifiComparison(
            listOf(
                measurement(5, "ThorNet", 18.0, 4.0, 0.0, 78, 480),
                measurement(4, "ThorNet", 22.0, 6.0, 0.0, 72, 360),
                measurement(3, "Guest", 80.0, 30.0, 2.0, 35, 40),
                measurement(2, null, 25.0, 5.0, 0.0, 60, 200),
                measurement(1, "Cable", 5.0, 1.0, 0.0, 100, 1_000, ConnectionType.ETHERNET),
            ),
        )

        assertEquals(4, result.wifiMeasurementCount)
        assertEquals(1, result.measurementsWithoutSsid)
        assertEquals(listOf("ThorNet", "Guest"), result.networks.map { it.ssid })
        assertEquals(100, result.networks[0].stabilityScore)
        assertTrue(result.networks[0].isBestMeasured)
        assertEquals(WifiComparisonConfidence.MEDIUM, result.networks[0].confidence)
        assertEquals(44, result.networks[1].stabilityScore)
        assertFalse(result.networks[1].isBestMeasured)
    }

    @Test
    fun `unknown and blank wifi names stay private and are counted as unavailable`() {
        val result = buildWifiComparison(
            listOf(
                measurement(3, "<unknown ssid>", 20.0, 5.0, 0.0, 70, 200),
                measurement(2, "   ", 20.0, 5.0, 0.0, 70, 200),
                measurement(1, null, 20.0, 5.0, 0.0, 70, 200),
            ),
        )

        assertTrue(result.networks.isEmpty())
        assertEquals(3, result.measurementsWithoutSsid)
    }

    @Test
    fun `network without measurable metrics remains visible without invented score`() {
        val result = buildWifiComparison(
            listOf(
                NetworkMeasurement(
                    id = 1,
                    timestamp = 100,
                    snapshot = NetworkSnapshot(connectionType = ConnectionType.WIFI, ssid = "Visible"),
                    assessment = QualityAssessment(NetworkQuality.NOT_MEASURABLE, "test"),
                ),
            ),
        )

        assertEquals(1, result.networks.size)
        assertNull(result.networks.single().stabilityScore)
        assertFalse(result.networks.single().isBestMeasured)
    }

    @Test
    fun `radio data alone does not create a streaming stability score`() {
        val result = buildWifiComparison(
            listOf(
                NetworkMeasurement(
                    id = 1,
                    timestamp = 100,
                    snapshot = NetworkSnapshot(
                        connectionType = ConnectionType.WIFI,
                        ssid = "RadioOnly",
                        signalStrengthPercent = 90,
                        linkSpeedMbps = 900,
                    ),
                    assessment = QualityAssessment(NetworkQuality.NOT_MEASURABLE, "test"),
                ),
            ),
        )

        assertNull(result.networks.single().stabilityScore)
        assertEquals(WifiComparisonConfidence.LOW, result.networks.single().confidence)
    }

    private fun measurement(
        id: Long,
        ssid: String?,
        latency: Double,
        jitter: Double,
        loss: Double,
        signal: Int,
        linkSpeed: Int,
        connectionType: ConnectionType = ConnectionType.WIFI,
    ) = NetworkMeasurement(
        id = id,
        timestamp = id * 100,
        snapshot = NetworkSnapshot(
            connectionType = connectionType,
            ssid = ssid,
            latencyMs = latency,
            jitterMs = jitter,
            packetLossPercent = loss,
            signalStrengthPercent = signal,
            linkSpeedMbps = linkSpeed,
        ),
        assessment = QualityAssessment(NetworkQuality.USABLE, "test"),
    )
}
