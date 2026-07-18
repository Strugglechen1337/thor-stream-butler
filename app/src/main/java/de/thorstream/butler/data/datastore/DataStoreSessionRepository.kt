package de.thorstream.butler.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.core.common.SessionCalculations
import de.thorstream.butler.domain.model.StreamingSession
import de.thorstream.butler.domain.repository.StreamingSessionRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "streaming_sessions")

/**
 * Persists the active-session marker and the latest completed session in a
 * small dedicated DataStore. Survives process death so a session completes
 * even when Android reclaimed the app in between.
 */
@Singleton
class DataStoreSessionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : StreamingSessionRepository {
    private object Keys {
        val activeName = stringPreferencesKey("active_name")
        val activeStart = longPreferencesKey("active_start")
        val lastName = stringPreferencesKey("last_name")
        val lastStart = longPreferencesKey("last_start")
        val lastDuration = longPreferencesKey("last_duration_minutes")
    }

    override val lastSession: Flow<StreamingSession?> = context.sessionDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            val name = preferences[Keys.lastName] ?: return@map null
            val start = preferences[Keys.lastStart] ?: return@map null
            val duration = preferences[Keys.lastDuration] ?: return@map null
            StreamingSession(entryName = name, startedAt = start, durationMinutes = duration)
        }

    override suspend fun startSession(entryName: String, startedAt: Long) {
        context.sessionDataStore.edit { preferences ->
            preferences[Keys.activeName] = entryName
            preferences[Keys.activeStart] = startedAt
        }
    }

    override suspend fun completeActiveSession(endedAt: Long) {
        val preferences = context.sessionDataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        val name = preferences[Keys.activeName]
        val start = preferences[Keys.activeStart]
        if (name == null || start == null) return
        val duration = SessionCalculations.sessionDurationMinutes(start, endedAt)
        context.sessionDataStore.edit { mutable ->
            mutable.remove(Keys.activeName)
            mutable.remove(Keys.activeStart)
            if (duration != null) {
                mutable[Keys.lastName] = name
                mutable[Keys.lastStart] = start
                mutable[Keys.lastDuration] = duration
            }
        }
    }
}
