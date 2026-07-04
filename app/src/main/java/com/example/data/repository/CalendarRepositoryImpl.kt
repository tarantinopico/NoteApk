package com.example.data.repository

import com.example.data.local.dao.CalendarEventDao
import com.example.data.local.entity.CalendarEventEntity
import com.example.domain.model.CalendarItem
import com.example.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    private val calendarEventDao: CalendarEventDao
) : CalendarRepository {

    override fun getAllEvents(): Flow<List<CalendarItem>> {
        return calendarEventDao.getAllEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getEventsInRange(start: Long, end: Long): Flow<List<CalendarItem>> {
        return calendarEventDao.getEventsInRange(start, end).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getEventById(id: String): CalendarItem? {
        return calendarEventDao.getEventById(id)?.toDomainModel()
    }

    override suspend fun addEvent(event: CalendarItem) {
        calendarEventDao.insertEvent(event.toEntity())
    }

    override suspend fun deleteEvent(event: CalendarItem) {
        calendarEventDao.deleteEvent(event.toEntity())
    }

    private fun CalendarEventEntity.toDomainModel(): CalendarItem {
        return CalendarItem(
            id = id,
            title = title,
            description = description,
            startAt = startAt,
            endAt = endAt,
            isTask = isTask,
            isDone = isDone,
            reminderAt = reminderAt,
            noteId = noteId
        )
    }

    private fun CalendarItem.toEntity(): CalendarEventEntity {
        return CalendarEventEntity(
            id = id,
            title = title,
            description = description,
            startAt = startAt,
            endAt = endAt,
            isTask = isTask,
            isDone = isDone,
            reminderAt = reminderAt,
            noteId = noteId
        )
    }
}
