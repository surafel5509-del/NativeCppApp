package com.androidforge.studio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.androidforge.studio.data.local.entity.BreakpointEntity
import com.androidforge.studio.data.local.entity.BuildSessionEntity
import com.androidforge.studio.data.local.entity.ChatMessageEntity
import com.androidforge.studio.data.local.entity.EditorTabEntity
import com.androidforge.studio.data.local.entity.PluginEntity
import com.androidforge.studio.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProjectEntity): Long

    @Query("UPDATE projects SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("UPDATE projects SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long)

    @Query("UPDATE projects SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface EditorTabDao {
    @Query("SELECT * FROM editor_tabs WHERE projectId = :projectId ORDER BY sortOrder")
    fun observeTabs(projectId: Long): Flow<List<EditorTabEntity>>

    @Query("SELECT * FROM editor_tabs WHERE projectId = :projectId ORDER BY sortOrder")
    suspend fun tabs(projectId: Long): List<EditorTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tab: EditorTabEntity): Long

    @Query("DELETE FROM editor_tabs WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun close(projectId: Long, filePath: String)

    @Query("DELETE FROM editor_tabs WHERE projectId = :projectId")
    suspend fun closeAll(projectId: Long)

    @Query("UPDATE editor_tabs SET isDirty = :dirty WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun setDirty(projectId: Long, filePath: String, dirty: Boolean)
}

@Dao
interface BuildSessionDao {
    @Query("SELECT * FROM build_sessions WHERE projectId = :projectId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun recent(projectId: Long, limit: Int): List<BuildSessionEntity>

    @Query("SELECT * FROM build_sessions WHERE projectId = :projectId ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(projectId: Long, limit: Int): Flow<List<BuildSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: BuildSessionEntity): Long

    @Query("DELETE FROM build_sessions WHERE projectId = :projectId")
    suspend fun clear(projectId: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE projectId IS :projectId ORDER BY createdAt")
    fun observe(projectId: Long?): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE projectId IS :projectId")
    suspend fun clear(projectId: Long?)
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins ORDER BY name")
    fun observeAll(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins")
    suspend fun all(): List<PluginEntity>

    @Upsert
    suspend fun upsert(entity: PluginEntity)

    @Query("UPDATE plugins SET enabled = :enabled WHERE pluginId = :pluginId")
    suspend fun setEnabled(pluginId: String, enabled: Boolean)

    @Query("DELETE FROM plugins WHERE pluginId = :pluginId")
    suspend fun delete(pluginId: String)
}

@Dao
interface BreakpointDao {
    @Query("SELECT * FROM breakpoints WHERE projectId = :projectId ORDER BY filePath, line")
    fun observeForProject(projectId: Long): Flow<List<BreakpointEntity>>

    @Query("SELECT * FROM breakpoints WHERE projectId = :projectId AND filePath = :filePath AND line = :line LIMIT 1")
    suspend fun get(projectId: Long, filePath: String, line: Int): BreakpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BreakpointEntity)

    @Query("DELETE FROM breakpoints WHERE projectId = :projectId AND filePath = :filePath AND line = :line")
    suspend fun delete(projectId: Long, filePath: String, line: Int)

    @Query("DELETE FROM breakpoints WHERE projectId = :projectId")
    suspend fun clearForProject(projectId: Long)
}
