package com.example.data.repository

import android.net.Uri
import com.example.core.config.ConfigRepository
import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NoteTagCrossRef
import com.example.data.local.entity.TagEntity
import com.example.data.vault.VaultManager
import com.example.data.vault.YamlFrontMatter
import com.example.domain.model.Note
import com.example.domain.model.Tag
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val vaultManager: VaultManager,
    private val configRepository: ConfigRepository
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { noteWithTags ->
                Note(
                    id = noteWithTags.note.id,
                    filePath = noteWithTags.note.filePath,
                    title = noteWithTags.note.title,
                    contentSnippet = noteWithTags.note.contentSnippet,
                    folderId = noteWithTags.note.folderId,
                    tags = noteWithTags.tags.map { Tag(it.id, it.name, it.color) },
                    createdAt = noteWithTags.note.createdAt,
                    updatedAt = noteWithTags.note.updatedAt
                )
            }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        val noteWithTags = noteDao.getNoteById(id) ?: return null
        return Note(
            id = noteWithTags.note.id,
            filePath = noteWithTags.note.filePath,
            title = noteWithTags.note.title,
            contentSnippet = noteWithTags.note.contentSnippet,
            folderId = noteWithTags.note.folderId,
            tags = noteWithTags.tags.map { Tag(it.id, it.name, it.color) },
            createdAt = noteWithTags.note.createdAt,
            updatedAt = noteWithTags.note.updatedAt
        )
    }

    override suspend fun getNoteContent(id: String): String? {
        val noteWithTags = noteDao.getNoteById(id) ?: return null
        val filePath = noteWithTags.note.filePath
        if (filePath.isNotEmpty()) {
            val fileContent = vaultManager.readFileContent(Uri.parse(filePath))
            if (fileContent != null) {
                return YamlFrontMatter.parse(fileContent).content
            }
        }
        return null
    }

    override suspend fun saveNote(note: Note, content: String) {
        val config = configRepository.appConfig.first()
        val vaultUriStr = config.vaultUri ?: return
        
        // Ensure file exists or create it
        val uri = if (note.filePath.isNotEmpty()) {
            Uri.parse(note.filePath)
        } else {
            val fileName = "${note.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.md"
            val newFile = vaultManager.createFile(vaultUriStr, fileName) ?: return
            newFile.uri
        }

        val metadata = mapOf(
            "id" to note.id,
            "title" to note.title,
            "created" to note.createdAt.toString(),
            "updated" to System.currentTimeMillis().toString(),
            "tags" to note.tags.joinToString(",") { it.name }
        )
        
        val fileContent = YamlFrontMatter.serialize(metadata, content)
        vaultManager.writeFileContent(uri, fileContent)

        val snippet = content.take(100)
        
        val entity = NoteEntity(
            id = note.id,
            filePath = uri.toString(),
            title = note.title,
            contentSnippet = snippet,
            folderId = note.folderId,
            createdAt = note.createdAt,
            updatedAt = System.currentTimeMillis(),
            fileHash = fileContent.hashCode().toString()
        )
        
        noteDao.insertNote(entity)
        noteDao.deleteTagsForNote(note.id)
        note.tags.forEach { tag ->
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(note.id, tag.id))
        }
    }

    override suspend fun updateNoteFolder(noteId: String, folderId: String?) {
        val noteEntity = noteDao.getNoteById(noteId)?.note ?: return
        noteDao.insertNote(noteEntity.copy(folderId = folderId, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteNote(note: Note) {
        val entity = noteDao.getNoteById(note.id)?.note ?: return
        noteDao.deleteNote(entity)
        
        if (note.filePath.isNotEmpty()) {
            vaultManager.deleteFile(Uri.parse(note.filePath))
        }
    }

    override suspend fun syncVault() {
        // Basic sync: list files in vault, parse them, update DB
        val config = configRepository.appConfig.first()
        val vaultUriStr = config.vaultUri ?: return
        val root = vaultManager.getRootDirectory(vaultUriStr) ?: return
        
        // For simplicity, just listing files in the root for now
        for (file in root.listFiles()) {
            if (file.isFile && file.name?.endsWith(".md") == true) {
                val content = vaultManager.readFileContent(file.uri) ?: continue
                val parsed = YamlFrontMatter.parse(content)
                val id = parsed.metadata["id"] ?: UUID.randomUUID().toString()
                val title = parsed.metadata["title"] ?: file.name?.removeSuffix(".md") ?: "Untitled"
                val created = parsed.metadata["created"]?.toLongOrNull() ?: System.currentTimeMillis()
                val updated = parsed.metadata["updated"]?.toLongOrNull() ?: System.currentTimeMillis()
                
                val entity = NoteEntity(
                    id = id,
                    filePath = file.uri.toString(),
                    title = title,
                    contentSnippet = parsed.content.take(100),
                    folderId = null,
                    createdAt = created,
                    updatedAt = updated,
                    fileHash = content.hashCode().toString()
                )
                noteDao.insertNote(entity)
                
                // Handle tags if needed
                val tagStr = parsed.metadata["tags"]
                if (tagStr != null) {
                    val tagNames = tagStr.split(",").map { it.trim() }
                    // In a real app we'd insert these tags into TagDao if they don't exist
                }
            }
        }
    }
}
