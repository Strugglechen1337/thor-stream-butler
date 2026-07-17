package de.thorstream.butler.core.network

import de.thorstream.butler.R
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.fakes.FakeStringProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityEvaluatorTest {
    private val evaluator = QualityEvaluator(FakeStringProvider())

    @Test
    fun `good ethernet measurement is optimal`() {
        val result = evaluator.evaluate(
            NetworkSnapshot(
                connectionType = ConnectionType.ETHERNET,
                internetValidated = true,
                latencyMs = 18.0,
                jitterMs = 2.0,
                packetLossPercent = 0.0,
            ),
        )
        assertEquals(NetworkQuality.OPTIMAL, result.quality)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `moderate latency produces usable result`() {
        val result = evaluator.evaluate(
            NetworkSnapshot(ConnectionType.WIFI, wifiFrequencyMhz = 5_200, signalStrengthPercent = 80, latencyMs = 45.0, jitterMs = 5.0, packetLossPercent = 0.0),
        )
        assertEquals(NetworkQuality.USABLE, result.quality)
        assertTrue(result.problems.any { it.startsWith("res:${R.string.eval_problem_elevated_latency}") })
    }

    @Test
    fun `packet loss over one percent is problematic`() {
        val result = evaluator.evaluate(
            NetworkSnapshot(ConnectionType.WIFI, latencyMs = 25.0, jitterMs = 5.0, packetLossPercent = 2.0),
        )
        assertEquals(NetworkQuality.PROBLEMATIC, result.quality)
    }

    @Test
    fun `available transport without core measurements is gray`() {
        val result = evaluator.evaluate(NetworkSnapshot(ConnectionType.ETHERNET, internetValidated = true))
        assertEquals(NetworkQuality.NOT_MEASURABLE, result.quality)
    }

    @Test
    fun `unreachable local host is problematic even with good latency`() {
        val result = evaluator.evaluate(
            NetworkSnapshot(ConnectionType.ETHERNET, latencyMs = 5.0, jitterMs = 1.0, packetLossPercent = 0.0, host = "192.168.1.2", hostReachable = false),
        )
        assertEquals(NetworkQuality.PROBLEMATIC, result.quality)
        assertTrue(result.recommendations.any { it.startsWith("res:${R.string.eval_reco_check_host}") })
    }

    @Test
    fun `vpn with otherwise good measurements is usable and explained`() {
        val result = evaluator.evaluate(
            NetworkSnapshot(
                connectionType = ConnectionType.VPN,
                internetValidated = true,
                latencyMs = 18.0,
                jitterMs = 2.0,
                packetLossPercent = 0.0,
            ),
        )

        assertEquals(NetworkQuality.USABLE, result.quality)
        assertTrue(result.problems.any { it.startsWith("res:${R.string.eval_problem_vpn}") })
        assertTrue(result.recommendations.any { it.startsWith("res:${R.string.eval_reco_vpn}") })
    }
}

