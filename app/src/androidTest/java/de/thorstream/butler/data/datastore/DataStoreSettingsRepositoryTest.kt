package de.thorstream.butler.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.thorstream.butler.domain.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {
    @Test
    fun updateNormalizesTargetsAndBoundsDiagnosticSettings() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = DataStoreSettingsRepository(context)
        try {
            repository.update(
                AppSettings(
                    defaultTestTarget = "[::1]",
                    pingCount = 0,
                    testDurationSeconds = 0,
                ),
            )

            val minimums = repository.settings.first()
            assertEquals("::1", minimums.defaultTestTarget)
            assertEquals(1, minimums.pingCount)
            assertEquals(1, minimums.testDurationSeconds)

            repository.update(
                AppSettings(
                    defaultTestTarget = "not a valid host!",
                    pingCount = 99,
                    testDurationSeconds = 99,
                ),
            )

            val maximums = repository.settings.first()
            assertEquals("1.1.1.1", maximums.defaultTestTarget)
            assertEquals(20, maximums.pingCount)
            assertEquals(15, maximums.testDurationSeconds)
        } finally {
            repository.update(AppSettings())
        }
    }
}
