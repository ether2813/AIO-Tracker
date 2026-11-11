package com.aio.tracker

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aio.tracker.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks the exported Room schemas in `app/schemas` (see README > Changing the database schema).
 *
 * When the database version is bumped, add a test here that creates the old version with
 * [MigrationTestHelper.createDatabase], inserts rows, and then calls
 * [MigrationTestHelper.runMigrationsAndValidate] with the new migration.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun version1SchemaOpens() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            val tables = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT IN ('android_metadata', 'room_master_table', 'sqlite_sequence') ORDER BY name",
            ).use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList() }
            assertEquals(
                listOf(
                    "ai_summary", "exercise", "jumuah_override", "loan", "money_txn",
                    "note", "prayer_config", "prayer_log", "todo", "workout",
                ),
                tables,
            )
        }

        // Room validates the file against the current entities when it opens it. Manual migrations added to
        // AppDatabase.get() later must be added here too.
        val room = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()
        try {
            room.openHelper.writableDatabase
        } finally {
            room.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
