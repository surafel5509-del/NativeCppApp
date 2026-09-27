package com.androidforge.studio.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Future migrations live here. v1 is the initial schema created by Room.
 * `fallbackToDestructiveMigration(dropAllTables = true)` covers dev builds.
 */
object Migrations {
    // Example for v1 -> v2:
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         db.execSQL("ALTER TABLE projects ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
    //     }
    // }
}
