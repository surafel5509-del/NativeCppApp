package com.androidforge.studio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidforge.studio.data.local.dao.BreakpointDao
import com.androidforge.studio.data.local.dao.BuildSessionDao
import com.androidforge.studio.data.local.dao.ChatMessageDao
import com.androidforge.studio.data.local.dao.EditorTabDao
import com.androidforge.studio.data.local.dao.PluginDao
import com.androidforge.studio.data.local.dao.ProjectDao
import com.androidforge.studio.data.local.entity.BreakpointEntity
import com.androidforge.studio.data.local.entity.BuildSessionEntity
import com.androidforge.studio.data.local.entity.ChatMessageEntity
import com.androidforge.studio.data.local.entity.EditorTabEntity
import com.androidforge.studio.data.local.entity.PluginEntity
import com.androidforge.studio.data.local.entity.ProjectEntity

@Database(
    entities = [
        ProjectEntity::class,
        EditorTabEntity::class,
        BuildSessionEntity::class,
        ChatMessageEntity::class,
        PluginEntity::class,
        BreakpointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun editorTabDao(): EditorTabDao
    abstract fun buildSessionDao(): BuildSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun pluginDao(): PluginDao
    abstract fun breakpointDao(): BreakpointDao

    companion object {
        const val NAME = "androidforge.db"
    }
}
