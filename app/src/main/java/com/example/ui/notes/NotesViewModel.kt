package com.example.ui.notes

import com.example.core.config.SortOrder
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.domain.model.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.ConfigRepository
import com.example.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NotesUiState(
    val isLoading: Boolean = true,
    val notes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val currentFolderId: String? = null,
    val activeTagIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_MODIFIED_DESC,
    val error: String? = null,
    val isVaultConfigured: Boolean = false,
    val currentFolderPath: List<Folder> = emptyList() // For Breadcrumbs
)

sealed class NotesEvent {
    data class OnNoteClick(val noteId: String) : NotesEvent()
    object OnCreateNoteClick : NotesEvent()
    data class OnDeleteNote(val note: Note) : NotesEvent()
    data class OnMoveNote(val note: Note, val targetFolderId: String?) : NotesEvent()
    data class OnFolderClick(val folderId: String?) : NotesEvent()
    data class OnCreateFolder(val name: String) : NotesEvent()
    data class OnDeleteFolder(val folder: Folder) : NotesEvent()
    data class OnTagFilterClick(val tagId: String) : NotesEvent()
    data class OnCreateTag(val name: String, val color: Long) : NotesEvent()
    data class OnSearchQueryChange(val query: String) : NotesEvent()
    data class OnSortOrderChange(val sortOrder: SortOrder) : NotesEvent()
    object OnUndoDelete : NotesEvent()
    object ClearError : NotesEvent()
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var recentlyDeletedNote: Note? = null
    private var notesFlowJob: Job? = null

    init {
        viewModelScope.launch {
            configRepository.appConfig.collect { config ->
                _uiState.update { 
                    it.copy(
                        isVaultConfigured = config.vaultUri != null,
                        sortOrder = config.notesSortOrder
                    )
                }
                observeNotes()
            }
        }
        
        viewModelScope.launch {
            getFoldersUseCase().collect { folders ->
                _uiState.update { state -> 
                    state.copy(folders = folders, currentFolderPath = buildPath(folders, state.currentFolderId)) 
                }
            }
        }
        
        viewModelScope.launch {
            getTagsUseCase().collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
    }

    private fun observeNotes() {
        notesFlowJob?.cancel()
        notesFlowJob = viewModelScope.launch {
            val state = _uiState.value
            getNotesUseCase(
                folderId = state.currentFolderId,
                searchQuery = state.searchQuery,
                activeTagIds = state.activeTagIds,
                sortOrder = state.sortOrder
            ).collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoading = false) }
            }
        }
    }

    private fun buildPath(folders: List<Folder>, currentId: String?): List<Folder> {
        if (currentId == null) return emptyList()
        val path = mutableListOf<Folder>()
        var current = folders.find { it.id == currentId }
        while (current != null) {
            path.add(0, current)
            current = folders.find { it.id == current.parentId }
        }
        return path
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(300) // Debounce
                    observeNotes()
                }
            }
            is NotesEvent.OnFolderClick -> {
                _uiState.update { state -> 
                    state.copy(
                        currentFolderId = event.folderId,
                        currentFolderPath = buildPath(state.folders, event.folderId)
                    ) 
                }
                observeNotes()
            }
            is NotesEvent.OnTagFilterClick -> {
                _uiState.update { state ->
                    val newTags = if (state.activeTagIds.contains(event.tagId)) {
                        state.activeTagIds - event.tagId
                    } else {
                        state.activeTagIds + event.tagId
                    }
                    state.copy(activeTagIds = newTags)
                }
                observeNotes()
            }
            is NotesEvent.OnSortOrderChange -> {
                viewModelScope.launch {
                    configRepository.updateConfig { it.copy(notesSortOrder = event.sortOrder) }
                }
            }
            is NotesEvent.OnCreateNoteClick -> {
                // Should navigate to note editor, not implemented here
            }
            is NotesEvent.OnNoteClick -> {
                // Should navigate to note editor, not implemented here
            }
            is NotesEvent.OnDeleteNote -> {
                recentlyDeletedNote = event.note
                viewModelScope.launch {
                    try {
                        deleteNoteUseCase(event.note)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            NotesEvent.OnUndoDelete -> {
                val note = recentlyDeletedNote ?: return
                viewModelScope.launch {
                    // Create note with empty content or minimal content - in a real app we'd save full content before delete
                    createNoteUseCase(note, note.contentSnippet) 
                    recentlyDeletedNote = null
                }
            }
            is NotesEvent.OnMoveNote -> {
                viewModelScope.launch {
                    try {
                        moveNoteUseCase(event.note.id, event.targetFolderId)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            is NotesEvent.OnCreateFolder -> {
                viewModelScope.launch {
                    try {
                        createFolderUseCase(event.name, _uiState.value.currentFolderId)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            is NotesEvent.OnDeleteFolder -> {
                viewModelScope.launch {
                    try {
                        deleteFolderUseCase(event.folder)
                        if (_uiState.value.currentFolderId == event.folder.id) {
                            onEvent(NotesEvent.OnFolderClick(event.folder.parentId))
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            is NotesEvent.OnCreateTag -> {
                viewModelScope.launch {
                    try {
                        createTagUseCase(event.name, event.color)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            NotesEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }
}
