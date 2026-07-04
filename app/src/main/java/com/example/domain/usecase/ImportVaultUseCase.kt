package com.example.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.domain.model.Note
import com.example.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ImportVaultUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open input stream"))
            
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.startsWith("notes/") && entry.name.endsWith(".md")) {
                        val content = zis.readBytes().decodeToString()
                        val parts = entry.name.substringAfter("notes/").substringBefore(".md").split("_")
                        val title = if (parts.size > 1) parts.dropLast(1).joinToString("_") else "Imported Note"
                        
                        val note = Note(
                            id = UUID.randomUUID().toString(),
                            filePath = "notes/${title}_${System.currentTimeMillis()}.md",
                            title = title,
                            contentSnippet = content.take(100),
                            folderId = null,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            tags = emptyList()
                        )
                        noteRepository.saveNote(note, content)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
