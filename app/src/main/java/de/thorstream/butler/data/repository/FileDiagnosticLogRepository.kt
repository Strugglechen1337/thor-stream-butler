package de.thorstream.butler.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.DiagnosticLogEntry
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Stores only timestamps and predefined event names. Network identifiers never enter this file. */
@Singleton
class FileDiagnosticLogRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : DiagnosticLogRepository {
    private val mutex = Mutex()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    override suspend fun log(event: DiagnosticEvent) {
        if (!settingsRepository.settings.first().diagnosticLoggingEnabled) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val lines = file.takeIf(File::exists)?.readLines().orEmpty().takeLast(MAX_ENTRIES - 1)
                    file.bufferedWriter().use { writer ->
                        lines.forEach { writer.appendLine(it) }
                        writer.appendLine("${System.currentTimeMillis()}|${event.name}")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Logging must never interrupt diagnostics or launching an app.
                }
            }
        }
    }

    override suspend fun read(): List<DiagnosticLogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                file.takeIf(File::exists)?.readLines().orEmpty().mapNotNull { line ->
                    val parts = line.split('|', limit = 2)
                    val timestamp = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                    val event = parts.getOrNull(1)?.let { runCatching { DiagnosticEvent.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
                    DiagnosticLogEntry(timestamp, event)
                }.sortedByDescending { it.timestamp }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (file.exists() && !file.delete()) throw IOException("Unable to delete diagnostic log")
        }
    }

    private companion object {
        const val FILE_NAME = "diagnostic-events.log"
        const val MAX_ENTRIES = 200
    }
}
