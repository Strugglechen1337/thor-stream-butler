package de.thorstream.butler.core.network

object NetworkCalculations {
    fun jitterMs(latenciesMs: List<Double>): Double? {
        if (latenciesMs.size < 2) return null
        return latenciesMs.zipWithNext { previous, current -> kotlin.math.abs(current - previous) }.average()
    }

    fun packetLossPercent(sentCount: Int, receivedCount: Int): Double? {
        if (sentCount <= 0) return null
        val lost = (sentCount - receivedCount).coerceIn(0, sentCount)
        return lost.toDouble() / sentCount.toDouble() * 100.0
    }
}

