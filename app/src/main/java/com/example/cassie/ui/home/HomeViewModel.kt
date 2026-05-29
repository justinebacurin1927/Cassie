package com.example.cassie.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cassie.data.media.MediaStoreScanner
import com.example.cassie.data.media.Song
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val scanner = MediaStoreScanner(app)
    private val _ui = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            val songs = scanner.scan()
            _ui.update { it.copy(songs = songs, isLoading = false) }
        }
    }

    fun refreshLibrary() = loadSongs()
}
