package de.thorstream.butler.core.common

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCalculationsTest {

    @Test
    fun `plausible sessions map to whole minutes`() {
        assertEquals(1L, SessionCalculations.sessionDurationMinutes(0, TimeUnit.MINUTES.toMillis(1)))
        assertEquals(47L, SessionCalculations.sessionDurationMinutes(0, TimeUnit.MINUTES.toMillis(47) + 30_000))
        assertEquals(720L, SessionCalculations.sessionDurationMinutes(0, TimeUnit.HOURS.toMillis(12)))
    }

    @Test
    fun `accidental launches shorter than one minute are discarded`() {
        assertNull(SessionCalculations.sessionDurationMinutes(0, 59_999))
        assertNull(SessionCalculations.sessionDurationMinutes(1_000, 1_000))
    }

    @Test
    fun `stale markers and clock jumps are discarded`() {
        assertNull(SessionCalculations.sessionDurationMinutes(0, TimeUnit.HOURS.toMillis(12) + 1))
        assertNull(SessionCalculations.sessionDurationMinutes(10_000, 0))
    }
}
