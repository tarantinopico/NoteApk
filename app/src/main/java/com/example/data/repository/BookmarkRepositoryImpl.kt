package com.example.data.repository

import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BookmarkTagCrossRef
import com.example.domain.model.Bookmark
import com.example.domain.model.Tag
import com.example.domain.repository.BookmarkRepository
import com.example.data.service.LinkPreviewService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val linkPreviewService: LinkPreviewService
) : BookmarkRepository {

    override fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { list ->
            list.map { withTags ->
                Bookmark(
                    id = withTags.bookmark.id,
                    url = withTags.bookmark.url,
                    title = withTags.bookmark.title,
                    description = withTags.bookmark.description,
                    imageUrl = withTags.bookmark.imageUrl,
                    domain = withTags.bookmark.domain,
                    noteId = withTags.bookmark.noteId,
                    tags = withTags.tags.map { Tag(it.id, it.name, it.color) },
                    createdAt = withTags.bookmark.createdAt
                )
            }
        }
    }

    override suspend fun addBookmark(url: String, noteId: String?, tags: List<Tag>): Bookmark {
        val preview = linkPreviewService.fetchPreview(url)
        val domain = preview?.domain ?: linkPreviewService.extractDomain(url)
        val title = preview?.title ?: domain

        val bookmarkEntity = BookmarkEntity(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            description = preview?.description,
            imageUrl = preview?.imageUrl,
            domain = domain,
            noteId = noteId,
            createdAt = System.currentTimeMillis()
        )

        bookmarkDao.insertBookmark(bookmarkEntity)
        tags.forEach { tag ->
            bookmarkDao.insertBookmarkTagCrossRef(BookmarkTagCrossRef(bookmarkEntity.id, tag.id))
        }

        return Bookmark(
            id = bookmarkEntity.id,
            url = bookmarkEntity.url,
            title = bookmarkEntity.title,
            description = bookmarkEntity.description,
            imageUrl = bookmarkEntity.imageUrl,
            domain = bookmarkEntity.domain,
            noteId = bookmarkEntity.noteId,
            tags = tags,
            createdAt = bookmarkEntity.createdAt
        )
    }

    override suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                id = bookmark.id,
                url = bookmark.url,
                title = bookmark.title,
                description = bookmark.description,
                imageUrl = bookmark.imageUrl,
                domain = bookmark.domain,
                noteId = bookmark.noteId,
                createdAt = bookmark.createdAt
            )
        )
        
        bookmarkDao.deleteTagsForBookmark(bookmark.id)
        bookmark.tags.forEach { tag ->
            bookmarkDao.insertBookmarkTagCrossRef(BookmarkTagCrossRef(bookmark.id, tag.id))
        }
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(
            BookmarkEntity(
                id = bookmark.id,
                url = bookmark.url,
                title = bookmark.title,
                description = bookmark.description,
                imageUrl = bookmark.imageUrl,
                domain = bookmark.domain,
                noteId = bookmark.noteId,
                createdAt = bookmark.createdAt
            )
        )
    }

    override suspend fun refreshPreview(bookmark: Bookmark) {
        val preview = linkPreviewService.fetchPreview(bookmark.url)
        if (preview != null) {
            val updated = bookmark.copy(
                title = preview.title,
                description = preview.description,
                imageUrl = preview.imageUrl,
                domain = preview.domain
            )
            updateBookmark(updated)
        }
    }
}
