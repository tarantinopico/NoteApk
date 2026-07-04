package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.CortexDatabase
import com.example.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCortexDatabase(@ApplicationContext context: Context): CortexDatabase {
        return Room.databaseBuilder(
            context,
            CortexDatabase::class.java,
            "cortex_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideNoteDao(database: CortexDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideFolderDao(database: CortexDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideTagDao(database: CortexDatabase): TagDao = database.tagDao()

    @Provides
    fun provideBookmarkDao(database: CortexDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideCalendarEventDao(database: CortexDatabase): CalendarEventDao = database.calendarEventDao()
}
