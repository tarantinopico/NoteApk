package com.example.data.vault

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Singleton
class VaultManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getRootDirectory(uriString: String): DocumentFile? {
        return try {
            val uri = Uri.parse(uriString)
            DocumentFile.fromTreeUri(context, uri)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createFolder(parentUri: String, folderName: String): DocumentFile? = withContext(Dispatchers.IO) {
        val parent = getRootDirectory(parentUri) ?: return@withContext null
        parent.createDirectory(folderName)
    }

    suspend fun createFile(parentUri: String, fileName: String, mimeType: String = "text/markdown"): DocumentFile? = withContext(Dispatchers.IO) {
        val parent = getRootDirectory(parentUri) ?: return@withContext null
        parent.createFile(mimeType, fileName)
    }

    suspend fun readFileContent(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun writeFileContent(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(content)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = DocumentFile.fromSingleUri(context, uri)
            file?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(uri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = DocumentFile.fromSingleUri(context, uri)
            file?.renameTo(newName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listFiles(parentUri: String): List<DocumentFile> = withContext(Dispatchers.IO) {
        try {
            val parent = getRootDirectory(parentUri) ?: return@withContext emptyList()
            parent.listFiles().toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
