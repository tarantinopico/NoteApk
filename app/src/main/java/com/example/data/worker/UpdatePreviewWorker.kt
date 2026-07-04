package com.example.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.domain.repository.BookmarkRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class UpdatePreviewWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun bookmarkRepository(): BookmarkRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
            val bookmarkRepository = entryPoint.bookmarkRepository()
            
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            
            // Find bookmarks that might need preview refresh (e.g., no title or description)
            val bookmarksToUpdate = bookmarks.filter { 
                it.title == it.domain || (it.description.isNullOrBlank() && it.imageUrl.isNullOrBlank())
            }
            
            bookmarksToUpdate.forEach { bookmark ->
                bookmarkRepository.refreshPreview(bookmark)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
