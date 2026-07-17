package de.thorstream.butler.feature.history

import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTrendTest {
    @Test
    fun `trend compares only matching connection context`() {
        val measurements = listOf(
            measurement(id = 3, latency = 20.0, type = ConnectionType.WIFI, ssid = "Gaming"),
            measurement(id = 2, latency = 80.0, type = ConnectionType.ETHERNET),
            measurement(id = 1, latency = 40.0, type = ConnectionType.WIFI, ssid = "Gaming"),
        )

        val result = buildHistoryItems(measurements)

        assertEquals(-1, result.first().trend.direction)
        assertEquals(-20.0, result.first().trend.differenceMs!!, 0.01)
        assertTrue(result[1].trend.isFirstComparable)
    }

    @Test
    fun `different wifi names are not compared`() {
        val result = buildHistoryItems(
            listOf(
                measurement(id = 2, latency = 20.0, type = ConnectionType.WIFI, ssid = "Gaming"),
                measurement(id = 1, latency = 40.0, type = ConnectionType.WIFI, ssid = "Guest"),
            ),
        )

        assertTrue(result.first().trend.isFirstComparable)
    }

    private fun measurement(id: Long, latency: Double, type: ConnectionType, ssid: String? = null) = NetworkMeasurement(
        id = id,
        timestamp = id,
        snapshot = NetworkSnapshot(connectionType = type, ssid = ssid, latencyMs = latency),
        assessment = QualityAssessment(NetworkQuality.USABLE, "test"),
    )
}
