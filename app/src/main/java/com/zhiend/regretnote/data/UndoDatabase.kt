package com.zhiend.regretnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Entry::class], version = 2, exportSchema = false)
abstract class UndoDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var instance: UndoDatabase? = null

        /**
         * v1 → v2: the `(date, createdAt)` index that backs the timeline's sort.
         *
         * Written as a real migration rather than a destructive rebuild: the
         * table is a journal, and a journal that empties itself on an app update
         * is the one bug a user would never forgive. For the same reason there is
         * deliberately no `fallbackToDestructiveMigration()` here — a missing
         * migration should fail loudly during development, not quietly delete
         * years of someone's notes.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entries_date_createdAt` " +
                        "ON `entries` (`date`, `createdAt`)",
                )
            }
        }

        fun get(context: Context): UndoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UndoDatabase::class.java,
                    "undo.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
