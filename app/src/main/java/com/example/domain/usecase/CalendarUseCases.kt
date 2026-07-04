package com.example.domain.usecase

import com.example.domain.model.CalendarItem
import com.example.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import com.example.core.notifications.ReminderManager
import javax.inject.Inject

class GetCalendarItemsUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {
    operator fun invoke(start: Long, end: Long): Flow<List<CalendarItem>> {
        return calendarRepository.getEventsInRange(start, end)
    }
}

class CreateEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val reminderManager: ReminderManager
) {
    suspend operator fun invoke(event: CalendarItem) {
        calendarRepository.addEvent(event)
        if (event.reminderAt != null) {
            reminderManager.scheduleReminder(event)
        }
    }
}

class UpdateEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val reminderManager: ReminderManager
) {
    suspend operator fun invoke(event: CalendarItem) {
        calendarRepository.addEvent(event)
        if (event.reminderAt != null) {
            reminderManager.scheduleReminder(event)
        } else {
            reminderManager.cancelReminder(event.id)
        }
    }
}

class DeleteEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val reminderManager: ReminderManager
) {
    suspend operator fun invoke(event: CalendarItem) {
        calendarRepository.deleteEvent(event)
        reminderManager.cancelReminder(event.id)
    }
}

class ToggleTaskDoneUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(event: CalendarItem) {
        if (event.isTask) {
            calendarRepository.addEvent(event.copy(isDone = !event.isDone))
        }
    }
}

class ScheduleReminderUseCase @Inject constructor(
    private val reminderManager: ReminderManager
) {
    operator fun invoke(event: CalendarItem) {
        reminderManager.scheduleReminder(event)
    }
}

class CancelReminderUseCase @Inject constructor(
    private val reminderManager: ReminderManager
) {
    operator fun invoke(eventId: String) {
        reminderManager.cancelReminder(eventId)
    }
}
