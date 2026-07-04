package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun getNoteContent(id: String): String?
    suspend fun saveNote(note: Note, content: String)
    suspend fun updateNoteFolder(noteId: String, folderId: String?)
    suspend fun deleteNote(note: Note)
    suspend fun syncVault()
}

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun createFolder(name: String, parentId: String?): Folder?
    suspend fun deleteFolder(folder: Folder)
}

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun createTag(name: String, color: Long): Tag
}

interface BookmarkRepository {
    fun getAllBookmarks(): Flow<List<Bookmark>>
    suspend fun addBookmark(url: String, noteId: String?, tags: List<Tag> = emptyList()): Bookmark
    suspend fun updateBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark)
    suspend fun refreshPreview(bookmark: Bookmark)
}

interface CalendarRepository {
    fun getAllEvents(): Flow<List<CalendarItem>>
    fun getEventsInRange(start: Long, end: Long): Flow<List<CalendarItem>>
    suspend fun getEventById(id: String): CalendarItem?
    suspend fun addEvent(event: CalendarItem)
    suspend fun deleteEvent(event: CalendarItem)
}
