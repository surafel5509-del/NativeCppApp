package com.androidforge.studio.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects", indices = [Index("name")])
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String,
    val templateId: String,
    val rootPath: String,
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "editor_tabs",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class EditorTabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val filePath: String,
    val title: String,
    val language: String,
    val isDirty: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "build_sessions",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId"), Index("startedAt")],
)
data class BuildSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val target: String,          // APK | AAB
    val status: String,          // IDLE | RUNNING | SUCCESS | FAILED | CANCELLED
    val stage: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val artifactPath: String?,
    val log: String,
    val diagnostics: String,
)

@Entity(
    tableName = "chat_messages",
    indices = [Index("projectId"), Index("createdAt")],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long?,        // null = global conversation
    val role: String,
    val content: String,
    val createdAt: Long,
)

@Entity(
    tableName = "plugins",
    indices = [Index("pluginId", unique = true)],
)
data class PluginEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pluginId: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val type: String,
    val enabled: Boolean,
    val entry: String,
    val sourcePath: String,
)

@Entity(
    tableName = "breakpoints",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId"), Index("projectId", "filePath", "line", unique = true)],
)
data class BreakpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val filePath: String,
    val line: Int,
    val enabled: Boolean,
    val condition: String,
)
