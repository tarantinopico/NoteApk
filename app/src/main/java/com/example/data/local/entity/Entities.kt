package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index("path", unique = true), Index("parentId")]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val path: String,
    val name: String,
    val parentId: String?
)

@Entity(
    tableName = "notes",
    indices = [Index("filePath", unique = true), Index("folderId")],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val filePath: String,
    val title: String,
    val contentSnippet: String,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val fileHash: String
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long
)

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)

@Entity(
    tableName = "bookmarks",
    indices = [Index("noteId")],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val domain: String,
    val noteId: String?,
    val createdAt: Long
)

@Entity(
    tableName = "bookmark_tag_cross_ref",
    primaryKeys = ["bookmarkId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(entity = BookmarkEntity::class, parentColumns = ["id"], childColumns = ["bookmarkId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class BookmarkTagCrossRef(
    val bookmarkId: String,
    val tagId: String
)

@Entity(
    tableName = "calendar_events",
    indices = [Index("noteId")],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val startAt: Long,
    val endAt: Long,
    val isTask: Boolean,
    val isDone: Boolean,
    val reminderAt: Long?,
    val noteId: String?
)
