package com.example.ui.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Tag
import com.example.domain.usecase.GetTagsUseCase
import com.example.domain.repository.BookmarkRepository
import com.example.domain.model.Bookmark
import com.example.domain.usecase.AddBookmarkUseCase
import com.example.domain.usecase.DeleteBookmarkUseCase
import com.example.domain.usecase.GetBookmarksUseCase
import com.example.domain.usecase.RefreshPreviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LinksUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterDomain: String? = null,
    val filterTagId: String? = null,
    val isAddDialogVisible: Boolean = false,
    val urlInput: String = "",
    val isAdding: Boolean = false,
    val error: String? = null,
    val bookmarkToEditTags: Bookmark? = null
)

sealed class LinksEvent {
    data class OnSearchQueryChange(val query: String) : LinksEvent()
    data class OnFilterDomainChange(val domain: String?) : LinksEvent()
    data class OnFilterTagChange(val tagId: String?) : LinksEvent()
    object OnAddClick : LinksEvent()
    object OnAddCancel : LinksEvent()
    data class OnUrlInputChange(val url: String) : LinksEvent()
    object OnAddConfirm : LinksEvent()
    data class OnDelete(val bookmark: Bookmark) : LinksEvent()
    data class OnRefreshPreview(val bookmark: Bookmark) : LinksEvent()
    data class OnEditTagsClick(val bookmark: Bookmark) : LinksEvent()
    object OnEditTagsDismiss : LinksEvent()
    data class OnToggleTag(val tag: Tag) : LinksEvent()
    object ClearError : LinksEvent()
}

@HiltViewModel
class LinksViewModel @Inject constructor(
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val refreshPreviewUseCase: RefreshPreviewUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinksUiState())
    val uiState: StateFlow<LinksUiState> = _uiState.asStateFlow()

    private var getBookmarksJob: Job? = null
    private var getTagsJob: Job? = null

    init {
        loadTags()
        loadBookmarks()
    }

    private fun loadTags() {
        getTagsJob?.cancel()
        getTagsJob = getTagsUseCase().onEach { tags ->
            _uiState.update { it.copy(availableTags = tags) }
        }.launchIn(viewModelScope)
    }

    private fun loadBookmarks() {
        getBookmarksJob?.cancel()
        getBookmarksJob = getBookmarksUseCase(
            searchQuery = _uiState.value.searchQuery,
            filterDomain = _uiState.value.filterDomain,
            filterTagId = _uiState.value.filterTagId
        ).onEach { bookmarks ->
            _uiState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: LinksEvent) {
        when (event) {
            is LinksEvent.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                loadBookmarks()
            }
            is LinksEvent.OnFilterDomainChange -> {
                _uiState.update { it.copy(filterDomain = event.domain) }
                loadBookmarks()
            }
            is LinksEvent.OnFilterTagChange -> {
                _uiState.update { it.copy(filterTagId = event.tagId) }
                loadBookmarks()
            }
            LinksEvent.OnAddClick -> {
                _uiState.update { it.copy(isAddDialogVisible = true, urlInput = "") }
            }
            LinksEvent.OnAddCancel -> {
                _uiState.update { it.copy(isAddDialogVisible = false, urlInput = "") }
            }
            is LinksEvent.OnUrlInputChange -> {
                _uiState.update { it.copy(urlInput = event.url) }
            }
            LinksEvent.OnAddConfirm -> {
                val url = _uiState.value.urlInput
                if (url.isNotBlank()) {
                    _uiState.update { it.copy(isAdding = true) }
                    viewModelScope.launch {
                        try {
                            addBookmarkUseCase(url)
                            _uiState.update { it.copy(isAdding = false, isAddDialogVisible = false, urlInput = "") }
                        } catch (e: Exception) {
                            _uiState.update { it.copy(isAdding = false, error = "Chyba při přidávání odkazu") }
                        }
                    }
                }
            }
            is LinksEvent.OnDelete -> {
                viewModelScope.launch {
                    deleteBookmarkUseCase(event.bookmark)
                }
            }
            is LinksEvent.OnRefreshPreview -> {
                viewModelScope.launch {
                    refreshPreviewUseCase(event.bookmark)
                }
            }
            is LinksEvent.OnEditTagsClick -> {
                _uiState.update { it.copy(bookmarkToEditTags = event.bookmark) }
            }
            LinksEvent.OnEditTagsDismiss -> {
                _uiState.update { it.copy(bookmarkToEditTags = null) }
            }
            is LinksEvent.OnToggleTag -> {
                val currentBookmark = _uiState.value.bookmarkToEditTags ?: return
                viewModelScope.launch {
                    val hasTag = currentBookmark.tags.any { it.id == event.tag.id }
                    val newTags = if (hasTag) {
                        currentBookmark.tags.filter { it.id != event.tag.id }
                    } else {
                        currentBookmark.tags + event.tag
                    }
                    val updatedBookmark = currentBookmark.copy(tags = newTags)
                    bookmarkRepository.updateBookmark(updatedBookmark)
                    _uiState.update { it.copy(bookmarkToEditTags = updatedBookmark) }
                }
            }
            LinksEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }
}
