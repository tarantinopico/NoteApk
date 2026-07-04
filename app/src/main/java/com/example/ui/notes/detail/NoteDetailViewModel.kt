package com.example.ui.notes.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.ConfigRepository
import com.example.core.config.NoteMode
import com.example.domain.model.Note
import com.example.domain.model.Tag
import com.example.domain.model.CalendarItem
import com.example.domain.repository.CalendarRepository
import com.example.domain.usecase.CreateNoteUseCase
import com.example.domain.usecase.DeleteNoteUseCase
import com.example.domain.usecase.GetNoteContentUseCase
import com.example.domain.usecase.GetNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NoteDetailUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val noteId: String = "",
    val title: String = "",
    val content: String = "",
    val originalContent: String = "",
    val tags: List<Tag> = emptyList(),
    val mode: NoteMode = NoteMode.VIEW,
    val isDirty: Boolean = false,
    val error: String? = null,
    val note: Note? = null,
    val linkedCalendarItems: List<CalendarItem> = emptyList()
)

sealed class NoteDetailEvent {
    data class OnTitleChange(val title: String) : NoteDetailEvent()
    data class OnContentChange(val content: String) : NoteDetailEvent()
    object OnToggleMode : NoteDetailEvent()
    object OnSave : NoteDetailEvent()
    object OnDelete : NoteDetailEvent()
    data class OnTagAdd(val tag: Tag) : NoteDetailEvent()
    data class OnTagRemove(val tag: Tag) : NoteDetailEvent()
    object ClearError : NoteDetailEvent()
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNoteUseCase: GetNoteUseCase,
    private val getNoteContentUseCase: GetNoteContentUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val configRepository: ConfigRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get<String>("noteId")
    
    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var getCalendarItemsJob: Job? = null

    init {
        loadNote()
        loadLinkedCalendarItems()
    }

    private fun loadLinkedCalendarItems() {
        if (noteId != null && noteId != "new") {
            getCalendarItemsJob?.cancel()
            getCalendarItemsJob = viewModelScope.launch {
                calendarRepository.getAllEvents().collect { events ->
                    val linkedItems = events.filter { it.noteId == noteId }
                    _uiState.update { it.copy(linkedCalendarItems = linkedItems) }
                }
            }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            val config = configRepository.appConfig.first()
            _uiState.update { it.copy(mode = config.defaultNoteMode, isLoading = true) }

            if (noteId != null && noteId != "new") {
                try {
                    val note = getNoteUseCase(noteId)
                    if (note != null) {
                        val content = getNoteContentUseCase(noteId) ?: ""
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                noteId = note.id,
                                title = note.title,
                                content = content,
                                originalContent = content,
                                tags = note.tags,
                                note = note
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Note not found") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        noteId = UUID.randomUUID().toString(),
                        mode = NoteMode.EDIT
                    )
                }
            }
        }
    }

    fun onEvent(event: NoteDetailEvent) {
        when (event) {
            is NoteDetailEvent.OnTitleChange -> {
                _uiState.update { it.copy(title = event.title, isDirty = true) }
                scheduleAutoSave()
            }
            is NoteDetailEvent.OnContentChange -> {
                _uiState.update { it.copy(content = event.content, isDirty = true) }
                scheduleAutoSave()
            }
            NoteDetailEvent.OnToggleMode -> {
                _uiState.update { 
                    it.copy(mode = if (it.mode == NoteMode.VIEW) NoteMode.EDIT else NoteMode.VIEW) 
                }
            }
            NoteDetailEvent.OnSave -> {
                saveNote()
            }
            NoteDetailEvent.OnDelete -> {
                viewModelScope.launch {
                    val note = _uiState.value.note
                    if (note != null) {
                        deleteNoteUseCase(note)
                    }
                }
            }
            is NoteDetailEvent.OnTagAdd -> {
                val tags = _uiState.value.tags.toMutableList()
                if (!tags.contains(event.tag)) {
                    tags.add(event.tag)
                    _uiState.update { it.copy(tags = tags, isDirty = true) }
                    scheduleAutoSave()
                }
            }
            is NoteDetailEvent.OnTagRemove -> {
                val tags = _uiState.value.tags.toMutableList()
                tags.remove(event.tag)
                _uiState.update { it.copy(tags = tags, isDirty = true) }
                scheduleAutoSave()
            }
            NoteDetailEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000)
            saveNote()
        }
    }

    private fun saveNote() {
        val state = _uiState.value
        if (!state.isDirty || state.title.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val currentNote = state.note
                val noteToSave = currentNote?.copy(
                    title = state.title,
                    tags = state.tags
                ) ?: Note(
                    id = state.noteId,
                    filePath = "",
                    title = state.title,
                    contentSnippet = state.content.take(100),
                    folderId = null,
                    tags = state.tags,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                createNoteUseCase(noteToSave, state.content)
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        isDirty = false, 
                        originalContent = state.content,
                        note = noteToSave
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }
}
