package com.example.domain.model

data class Tag(
    val id: String,
    val name: String,
    val color: Long
)

data class Folder(
    val id: String,
    val path: String,
    val name: String,
    val parentId: String?
)

data class Note(
    val id: String,
    val filePath: String,
    val title: String,
    val contentSnippet: String,
    val folderId: String?,
    val tags: List<Tag>,
    val createdAt: Long,
    val updatedAt: Long
)

data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val domain: String,
    val noteId: String?,
    val tags: List<Tag>,
    val createdAt: Long
)

data class CalendarItem(
    val id: String,
    val title: String,
    val description: String?,
    val startAt: Long,
    val endAt: Long,
    val isTask: Boolean,
    val isDone: Boolean,
    val reminderAt: Long?,
    val noteId: String?
)
