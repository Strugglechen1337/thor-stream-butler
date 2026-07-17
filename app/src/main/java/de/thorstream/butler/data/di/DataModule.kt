package de.thorstream.butler.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.thorstream.butler.data.database.LocalHostDao
import de.thorstream.butler.data.database.NetworkMeasurementDao
import de.thorstream.butler.data.database.StreamingEntryDao
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.data.common.AndroidStringProvider
import de.thorstream.butler.data.database.ThorDatabase
import de.thorstream.butler.data.datastore.DataStoreSettingsRepository
import de.thorstream.butler.data.repository.AndroidInstalledAppsRepository
import de.thorstream.butler.data.repository.RoomLocalHostRepository
import de.thorstream.butler.data.repository.RoomNetworkHistoryRepository
import de.thorstream.butler.data.repository.RoomStreamingEntryRepository
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.repository.InstalledAppsRepository
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.repository.StreamingEntryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ThorDatabase =
        Room.databaseBuilder(context, ThorDatabase::class.java, "thor_stream_butler.db")
            .addMigrations(ThorDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideStreamingDao(database: ThorDatabase): StreamingEntryDao = database.streamingEntryDao()
    @Provides fun provideHostDao(database: ThorDatabase): LocalHostDao = database.localHostDao()
    @Provides fun provideHistoryDao(database: ThorDatabase): NetworkMeasurementDao = database.networkMeasurementDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindInstalledAppsRepository(implementation: AndroidInstalledAppsRepository): InstalledAppsRepository
    @Binds abstract fun bindStreamingRepository(implementation: RoomStreamingEntryRepository): StreamingEntryRepository
    @Binds abstract fun bindHostRepository(implementation: RoomLocalHostRepository): LocalHostRepository
    @Binds abstract fun bindHistoryRepository(implementation: RoomNetworkHistoryRepository): NetworkHistoryRepository
    @Binds abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun bindStringProvider(implementation: AndroidStringProvider): StringProvider
}
