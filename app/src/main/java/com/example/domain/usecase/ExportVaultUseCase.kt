package com.example.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ExportVaultUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open output stream"))
            
            ZipOutputStream(outputStream).use { zos ->
                val notes = noteRepository.getAllNotes().first()
                for (note in notes) {
                    val content = noteRepository.getNoteContent(note.id) ?: ""
                    val safeTitle = note.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val entry = ZipEntry("notes/${safeTitle}_${note.id}.md")
                    zos.putNextEntry(entry)
                    zos.write(content.toByteArray())
                    zos.closeEntry()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
