package com.example.domain.usecase

import com.example.core.config.SortOrder
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.domain.model.Tag
import com.example.domain.repository.FolderRepository
import com.example.domain.repository.NoteRepository
import com.example.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(
        folderId: String? = null,
        searchQuery: String = "",
        activeTagIds: Set<String> = emptySet(),
        sortOrder: SortOrder = SortOrder.DATE_MODIFIED_DESC
    ): Flow<List<Note>> {
        return noteRepository.getAllNotes().map { notes ->
            notes.filter { note ->
                val matchesFolder = note.folderId == folderId
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    note.title.contains(searchQuery, ignoreCase = true) ||
                            note.contentSnippet.contains(searchQuery, ignoreCase = true)
                }
                val matchesTags = if (activeTagIds.isEmpty()) true else {
                    note.tags.any { it.id in activeTagIds }
                }
                matchesFolder && matchesSearch && matchesTags
            }.sortedWith(
                when (sortOrder) {
                    SortOrder.NAME_ASC -> compareBy { it.title.lowercase() }
                    SortOrder.NAME_DESC -> compareByDescending { it.title.lowercase() }
                    SortOrder.DATE_MODIFIED_DESC -> compareByDescending { it.updatedAt }
                    SortOrder.DATE_CREATED_DESC -> compareByDescending { it.createdAt }
                }
            )
        }
    }
}

class GetNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(id: String): Note? {
        return noteRepository.getNoteById(id)
    }
}

class GetNoteContentUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(id: String): String? {
        return noteRepository.getNoteContent(id)
    }
}

class CreateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(note: Note, content: String) {
        noteRepository.saveNote(note, content)
    }
}

class DeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        noteRepository.deleteNote(note)
    }
}

class MoveNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(noteId: String, targetFolderId: String?) {
        noteRepository.updateNoteFolder(noteId, targetFolderId)
    }
}

class GetFoldersUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(): Flow<List<Folder>> = folderRepository.getAllFolders()
}

class CreateFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    suspend operator fun invoke(name: String, parentId: String? = null): Folder {
        return folderRepository.createFolder(name, parentId) ?: throw IllegalStateException("Failed to create folder")
    }
}

class DeleteFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository
) {
    suspend operator fun invoke(folder: Folder) {
        folderRepository.deleteFolder(folder)
    }
}

class GetTagsUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {
    operator fun invoke(): Flow<List<Tag>> = tagRepository.getAllTags()
}

class CreateTagUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(name: String, color: Long): Tag = tagRepository.createTag(name, color)
}
