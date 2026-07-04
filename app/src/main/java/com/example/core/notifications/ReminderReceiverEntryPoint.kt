package com.example.core.notifications

import com.example.domain.repository.CalendarRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderReceiverEntryPoint {
    fun calendarRepository(): CalendarRepository
    fun reminderManager(): ReminderManager
}
