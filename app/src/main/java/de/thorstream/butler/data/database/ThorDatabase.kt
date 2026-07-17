package de.thorstream.butler.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class StringListConverters {
    private val separator = "\u001F"

    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(separator)

    @TypeConverter
    fun toList(value: String): List<String> = if (value.isBlank()) emptyList() else value.split(separator)
}

@Database(
    entities = [StreamingEntryEntity::class, LocalHostEntity::class, NetworkMeasurementEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(StringListConverters::class)
abstract class ThorDatabase : RoomDatabase() {
    abstract fun streamingEntryDao(): StreamingEntryDao
    abstract fun localHostDao(): LocalHostDao
    abstract fun networkMeasurementDao(): NetworkMeasurementDao
}

