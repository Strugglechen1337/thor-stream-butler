package de.thorstream.butler.core.common

import java.util.concurrent.TimeUnit

object SessionCalculations {
    /** Sessions shorter than this are treated as accidental launches. */
    val MIN_SESSION_MILLIS: Long = TimeUnit.MINUTES.toMillis(1)

    /** Sessions longer than this are treated as a stale marker (reboot, crash, clock change). */
    val MAX_SESSION_MILLIS: Long = TimeUnit.HOURS.toMillis(12)

    /**
     * Duration in whole minutes between start and end, or null when the value
     * is implausible and must not be recorded.
     */
    fun sessionDurationMinutes(startedAt: Long, endedAt: Long): Long? {
        val elapsed = endedAt - startedAt
        if (elapsed < MIN_SESSION_MILLIS || elapsed > MAX_SESSION_MILLIS) return null
        return TimeUnit.MILLISECONDS.toMinutes(elapsed)
    }
}
