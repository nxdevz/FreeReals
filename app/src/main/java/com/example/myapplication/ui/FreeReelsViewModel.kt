package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ContentTab
import com.example.myapplication.data.FreeReelsRepository
import com.example.myapplication.model.DramaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScreenTab(val label: String) {
    FOR_YOU("Untuk Kamu"),
    HOMEPAGE("Beranda"),
    ANIME("Anime"),
    SEARCH("Cari"),
}

data class FeedState(
    val items: List<DramaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLoaded: Boolean = false,
)

data class AppUiState(
    val activeTab: ScreenTab = ScreenTab.FOR_YOU,
    val feeds: Map<ScreenTab, FeedState> = mapOf(
        ScreenTab.FOR_YOU to FeedState(),
        ScreenTab.HOMEPAGE to FeedState(),
        ScreenTab.ANIME to FeedState(),
        ScreenTab.SEARCH to FeedState(),
    ),
    val searchQuery: String = "",
    val selectedItem: DramaItem? = null,
)

class FreeReelsViewModel(private val repository: FreeReelsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadTab(ScreenTab.FOR_YOU)
    }

    fun onTabSelected(tab: ScreenTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab != ScreenTab.SEARCH) {
            val feed = _uiState.value.feeds.getValue(tab)
            if (!feed.hasLoaded && !feed.isLoading) {
                loadTab(tab)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) {
            setFeed(ScreenTab.SEARCH) { FeedState() }
            return
        }

        setFeed(ScreenTab.SEARCH) { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            runCatching { repository.search(query) }
                .onSuccess { list ->
                    setFeed(ScreenTab.SEARCH) {
                        FeedState(items = list, isLoading = false, error = null, hasLoaded = true)
                    }
                }
                .onFailure { throwable ->
                    setFeed(ScreenTab.SEARCH) {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Pencarian gagal.",
                            hasLoaded = true,
                        )
                    }
                }
        }
    }

    fun openDetail(item: DramaItem) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedItem = null) }
    }

    private fun loadTab(tab: ScreenTab) {
        val contentTab = when (tab) {
            ScreenTab.FOR_YOU -> ContentTab.FOR_YOU
            ScreenTab.HOMEPAGE -> ContentTab.HOMEPAGE
            ScreenTab.ANIME -> ContentTab.ANIME
            ScreenTab.SEARCH -> return
        }

        setFeed(tab) { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            runCatching { repository.fetchByTab(contentTab) }
                .onSuccess { list ->
                    setFeed(tab) {
                        FeedState(items = list, isLoading = false, error = null, hasLoaded = true)
                    }
                }
                .onFailure { throwable ->
                    setFeed(tab) {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Gagal mengambil data.",
                            hasLoaded = true,
                        )
                    }
                }
        }
    }

    private fun setFeed(tab: ScreenTab, transform: (FeedState) -> FeedState) {
        _uiState.update { state ->
            state.copy(feeds = state.feeds.toMutableMap().apply { put(tab, transform(getValue(tab))) })
        }
    }
}

class FreeReelsViewModelFactory(private val repository: FreeReelsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FreeReelsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FreeReelsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}