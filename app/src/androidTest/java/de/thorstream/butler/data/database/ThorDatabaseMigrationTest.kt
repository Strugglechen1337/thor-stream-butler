package de.thorstream.butler.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThorDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ThorDatabase::class.java,
    )

    @Test
    fun migrateOneToTwoAddsHostAndProfileDefaults() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            insertLegacyEntry()
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, ThorDatabase.MIGRATION_1_2).use { database ->
            database.query(
                "SELECT hostId, profileResolution, profileFramesPerSecond, profileBitrateMbps FROM streaming_entries WHERE id = 1",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(null, cursor.getString(0))
                assertEquals("AUTO", cursor.getString(1))
                assertEquals(60, cursor.getInt(2))
                assertEquals(20, cursor.getInt(3))
            }
        }
    }

    @Test
    fun migrateTwoToThreeKeepsEthernetProfileEmpty() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO streaming_entries (
                    id, displayName, packageName, iconReference, streamingType, customName,
                    hostId, profileResolution, profileFramesPerSecond, profileBitrateMbps,
                    sortOrder, lastUsedAt, lastNetworkQuality, isDemo
                ) VALUES (
                    1, 'Moonlight', 'com.limelight', 'package://com.limelight', 'MOONLIGHT', NULL,
                    NULL, 'FULL_HD_1080P', 60, 40,
                    0, NULL, NULL, 0
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, ThorDatabase.MIGRATION_2_3).use { database ->
            database.query(
                "SELECT profileResolution, ethernetResolution, ethernetFramesPerSecond, ethernetBitrateMbps FROM streaming_entries WHERE id = 1",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("FULL_HD_1080P", cursor.getString(0))
                assertEquals(null, cursor.getString(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
            }
        }
    }

    private fun SupportSQLiteDatabase.insertLegacyEntry() {
        execSQL(
            """
            INSERT INTO streaming_entries (
                id, displayName, packageName, iconReference, streamingType, customName,
                sortOrder, lastUsedAt, lastNetworkQuality, isDemo
            ) VALUES (
                1, 'Moonlight', 'com.limelight', 'package://com.limelight', 'MOONLIGHT', NULL,
                0, NULL, NULL, 0
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
