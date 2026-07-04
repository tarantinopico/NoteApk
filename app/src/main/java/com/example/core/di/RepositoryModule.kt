package com.example.core.di

import com.example.core.config.ConfigRepository
import com.example.data.local.dao.*
import com.example.data.repository.FolderRepositoryImpl
import com.example.data.repository.NoteRepositoryImpl
import com.example.data.repository.TagRepositoryImpl
import com.example.data.vault.VaultManager
import com.example.domain.repository.*
import com.example.domain.model.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        vaultManager: VaultManager,
        configRepository: ConfigRepository
    ): NoteRepository {
        return NoteRepositoryImpl(noteDao, vaultManager, configRepository)
    }

    @Provides
    @Singleton
    fun provideFolderRepository(folderDao: FolderDao): FolderRepository {
        return FolderRepositoryImpl(folderDao)
    }

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao): TagRepository {
        return TagRepositoryImpl(tagDao)
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(
        bookmarkDao: BookmarkDao,
        linkPreviewService: com.example.data.service.LinkPreviewService
    ): BookmarkRepository {
        return com.example.data.repository.BookmarkRepositoryImpl(bookmarkDao, linkPreviewService)
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(calendarEventDao: CalendarEventDao): CalendarRepository {
        return com.example.data.repository.CalendarRepositoryImpl(calendarEventDao)
    }
}
