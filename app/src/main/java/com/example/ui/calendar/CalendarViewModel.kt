package com.example.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.ConfigRepository
import com.example.domain.model.CalendarItem
import com.example.domain.model.Note
import com.example.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject

enum class CalendarViewMode {
    MONTH, WEEK, AGENDA
}

data class CalendarUiState(
    val items: List<CalendarItem> = emptyList(),
    val availableNotes: List<Note> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val isLoading: Boolean = true,
    val isEditorVisible: Boolean = false,
    val itemToEdit: CalendarItem? = null,
    val firstDayOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY
)

sealed class CalendarEvent {
    data class OnDateSelected(val date: LocalDate) : CalendarEvent()
    data class OnViewModeChanged(val mode: CalendarViewMode) : CalendarEvent()
    data class OnToggleTaskDone(val item: CalendarItem) : CalendarEvent()
    data class OnEditItem(val item: CalendarItem?) : CalendarEvent() // null means create new
    data class OnSaveItem(val item: CalendarItem) : CalendarEvent()
    data class OnDeleteItem(val item: CalendarItem) : CalendarEvent()
    object OnEditorDismiss : CalendarEvent()
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarItemsUseCase: GetCalendarItemsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val toggleTaskDoneUseCase: ToggleTaskDoneUseCase,
    private val configRepository: ConfigRepository,
    private val getNotesUseCase: GetNotesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var getItemsJob: Job? = null
    private var getNotesJob: Job? = null

    init {
        viewModelScope.launch {
            configRepository.appConfig.collect { config ->
                val dayOfWeek = if (config.firstDayOfWeek == com.example.core.config.DayOfWeek.SUNDAY) java.time.DayOfWeek.SUNDAY else java.time.DayOfWeek.MONDAY
                _uiState.update { it.copy(firstDayOfWeek = dayOfWeek) }
                // Also default view mode could be handled here if added to config
            }
        }
        loadItemsForCurrentRange()
        loadNotes()
    }

    private fun loadNotes() {
        getNotesJob?.cancel()
        getNotesJob = getNotesUseCase().onEach { notes ->
            _uiState.update { it.copy(availableNotes = notes) }
        }.launchIn(viewModelScope)
    }

    private fun loadItemsForCurrentRange() {
        getItemsJob?.cancel()
        val date = _uiState.value.selectedDate
        
        // Load for a wider range to allow scrolling
        val startOfMonth = date.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth()).plusMonths(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        getItemsJob = getCalendarItemsUseCase(startOfMonth, endOfMonth).onEach { items ->
            _uiState.update { it.copy(items = items, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.OnDateSelected -> {
                _uiState.update { it.copy(selectedDate = event.date) }
                loadItemsForCurrentRange()
            }
            is CalendarEvent.OnViewModeChanged -> {
                _uiState.update { it.copy(viewMode = event.mode) }
            }
            is CalendarEvent.OnToggleTaskDone -> {
                viewModelScope.launch {
                    toggleTaskDoneUseCase(event.item)
                }
            }
            is CalendarEvent.OnEditItem -> {
                _uiState.update { it.copy(isEditorVisible = true, itemToEdit = event.item) }
            }
            is CalendarEvent.OnSaveItem -> {
                viewModelScope.launch {
                    if (_uiState.value.itemToEdit != null) {
                        updateEventUseCase(event.item)
                    } else {
                        createEventUseCase(event.item)
                    }
                    _uiState.update { it.copy(isEditorVisible = false, itemToEdit = null) }
                }
            }
            is CalendarEvent.OnDeleteItem -> {
                viewModelScope.launch {
                    deleteEventUseCase(event.item)
                    _uiState.update { it.copy(isEditorVisible = false, itemToEdit = null) }
                }
            }
            CalendarEvent.OnEditorDismiss -> {
                _uiState.update { it.copy(isEditorVisible = false, itemToEdit = null) }
            }
        }
    }
}
