package com.example.domain.usecase

import com.example.domain.model.Bookmark
import com.example.domain.model.Tag
import com.example.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(searchQuery: String = "", filterTagId: String? = null, filterDomain: String? = null): Flow<List<Bookmark>> {
        return bookmarkRepository.getAllBookmarks().map { bookmarks ->
            var result = bookmarks
            
            if (filterTagId != null) {
                result = result.filter { it.tags.any { tag -> tag.id == filterTagId } }
            }
            
            if (filterDomain != null) {
                result = result.filter { it.domain == filterDomain }
            }

            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase()
                result = result.filter { 
                    it.title.lowercase().contains(query) || 
                    it.url.lowercase().contains(query) ||
                    (it.description?.lowercase()?.contains(query) ?: false)
                }
            }

            result.sortedByDescending { it.createdAt }
        }
    }
}

class AddBookmarkUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(url: String, noteId: String? = null, tags: List<Tag> = emptyList()): Bookmark {
        val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        return bookmarkRepository.addBookmark(validUrl, noteId, tags)
    }
}

class DeleteBookmarkUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.deleteBookmark(bookmark)
    }
}

class RefreshPreviewUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.refreshPreview(bookmark)
    }
}
