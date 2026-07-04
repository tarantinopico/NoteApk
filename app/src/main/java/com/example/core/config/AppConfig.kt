package com.example.core.config

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class FontStyle { SANS, MONOSPACE }

@Serializable
enum class SortOrder { NAME_ASC, NAME_DESC, DATE_MODIFIED_DESC, DATE_CREATED_DESC }

@Serializable
enum class NoteMode { VIEW, EDIT }

@Serializable
enum class DayOfWeek { MONDAY, SUNDAY }

@Serializable
enum class CalendarViewModeConfig { MONTH, WEEK, AGENDA }

@Serializable
data class AppConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = false,
    val accentColor: Long = 0xFF2196F3, // Blue
    val isAnimationEnabled: Boolean = true,
    val animationSpeedMultiplier: Float = 1.0f,
    val fontStyle: FontStyle = FontStyle.SANS,
    val baseSpacingDp: Float = 8f,
    val baseRadiusDp: Float = 12f,
    val iconSizeDp: Float = 24f,
    val textScale: Float = 1.0f,
    val compactLayout: Boolean = false,
    val vaultUri: String? = null,
    val notesSortOrder: SortOrder = SortOrder.DATE_MODIFIED_DESC,
    val defaultNoteMode: NoteMode = NoteMode.VIEW,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val autosaveEnabled: Boolean = true,
    val autosaveIntervalMs: Long = 5000,
    val defaultFolderId: String? = null,
    val defaultTags: List<String> = emptyList(),
    val defaultCalendarView: CalendarViewModeConfig = CalendarViewModeConfig.MONTH,
    val dateFormat: String = "dd.MM.yyyy",
    val timeFormat: String = "HH:mm",
    val defaultReminderOffsetMs: Long = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val startScreen: String = "notes",
    val bottomNavOrder: List<String> = listOf("notes", "links", "calendar", "settings"),
    val isOnboardingCompleted: Boolean = false
)
