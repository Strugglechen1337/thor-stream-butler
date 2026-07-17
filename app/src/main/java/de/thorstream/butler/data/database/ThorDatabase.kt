package de.thorstream.butler.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class StringListConverters {
    private val separator = "\u001F"

    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(separator)

    @TypeConverter
    fun toList(value: String): List<String> = if (value.isBlank()) emptyList() else value.split(separator)
}

@Database(
    entities = [StreamingEntryEntity::class, LocalHostEntity::class, NetworkMeasurementEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(StringListConverters::class)
abstract class ThorDatabase : RoomDatabase() {
    abstract fun streamingEntryDao(): StreamingEntryDao
    abstract fun localHostDao(): LocalHostDao
    abstract fun networkMeasurementDao(): NetworkMeasurementDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE streaming_entries ADD COLUMN hostId INTEGER")
                db.execSQL("ALTER TABLE streaming_entries ADD COLUMN profileResolution TEXT NOT NULL DEFAULT 'AUTO'")
                db.execSQL("ALTER TABLE streaming_entries ADD COLUMN profileFramesPerSecond INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE streaming_entries ADD COLUMN profileBitrateMbps INTEGER NOT NULL DEFAULT 20")
            }
        }
    }
}
