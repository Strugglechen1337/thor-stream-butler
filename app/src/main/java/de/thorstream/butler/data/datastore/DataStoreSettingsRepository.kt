package de.thorstream.butler.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.ThemePreference
import de.thorstream.butler.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {
    private object Keys {
        val preLaunch = booleanPreferencesKey("pre_launch_check")
        val autoGreen = booleanPreferencesKey("auto_green")
        val warnYellow = booleanPreferencesKey("warn_yellow")
        val confirmRed = booleanPreferencesKey("confirm_red")
        val target = stringPreferencesKey("test_target")
        val pingCount = intPreferencesKey("ping_count")
        val duration = intPreferencesKey("test_duration")
        val download = booleanPreferencesKey("download_test")
        val theme = stringPreferencesKey("theme")
        val focusAnimations = booleanPreferencesKey("focus_animations")
        val logging = booleanPreferencesKey("diagnostic_logging")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { preferences ->
            AppSettings(
                preLaunchCheckEnabled = preferences[Keys.preLaunch] ?: true,
                autoLaunchOnGreen = preferences[Keys.autoGreen] ?: true,
                warnOnYellow = preferences[Keys.warnYellow] ?: true,
                confirmOnRed = preferences[Keys.confirmRed] ?: true,
                defaultTestTarget = normalizeTarget(preferences[Keys.target]),
                pingCount = (preferences[Keys.pingCount] ?: DEFAULT_PING_COUNT).coerceIn(MIN_PING_COUNT, MAX_PING_COUNT),
                testDurationSeconds = (preferences[Keys.duration] ?: DEFAULT_TEST_DURATION_SECONDS)
                    .coerceIn(MIN_TEST_DURATION_SECONDS, MAX_TEST_DURATION_SECONDS),
                downloadTestEnabled = preferences[Keys.download] ?: false,
                theme = preferences[Keys.theme]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.DARK,
                focusAnimationsEnabled = preferences[Keys.focusAnimations] ?: true,
                diagnosticLoggingEnabled = preferences[Keys.logging] ?: false,
            )
        }

    override suspend fun update(settings: AppSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.preLaunch] = settings.preLaunchCheckEnabled
            preferences[Keys.autoGreen] = settings.autoLaunchOnGreen
            preferences[Keys.warnYellow] = settings.warnOnYellow
            preferences[Keys.confirmRed] = settings.confirmOnRed
            preferences[Keys.target] = normalizeTarget(settings.defaultTestTarget)
            preferences[Keys.pingCount] = settings.pingCount.coerceIn(MIN_PING_COUNT, MAX_PING_COUNT)
            preferences[Keys.duration] = settings.testDurationSeconds.coerceIn(MIN_TEST_DURATION_SECONDS, MAX_TEST_DURATION_SECONDS)
            preferences[Keys.download] = settings.downloadTestEnabled
            preferences[Keys.theme] = settings.theme.name
            preferences[Keys.focusAnimations] = settings.focusAnimationsEnabled
            preferences[Keys.logging] = settings.diagnosticLoggingEnabled
        }
    }

    private fun normalizeTarget(value: String?): String = value
        ?.let(NetworkValidators::normalizeHost)
        ?.takeIf(NetworkValidators::isValidHostnameOrIp)
        ?: DEFAULT_TEST_TARGET

    private companion object {
        const val DEFAULT_TEST_TARGET = "1.1.1.1"
        const val DEFAULT_PING_COUNT = 5
        const val MIN_PING_COUNT = 1
        const val MAX_PING_COUNT = 20
        const val DEFAULT_TEST_DURATION_SECONDS = 5
        const val MIN_TEST_DURATION_SECONDS = 1
        const val MAX_TEST_DURATION_SECONDS = 15
    }
}
