package com.example.cassie.data.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long> = emptyList(),
    val coverUri: String? = null, // custom cover image (internal storage path)
) {
    val songCount: Int get() = songIds.size
}

class PlaylistStore(persistenceManager: PersistenceManager? = null) {
    private val pm = persistenceManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _playlists = MutableStateFlow<List<Playlist>>(
        pm?.loadPlaylists() ?: emptyList()
    )
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private var nextId: Long = pm?.loadNextId() ?: 1L

    private fun persist() {
        val current = _playlists.value
        val id = nextId
        scope.launch {
            pm?.let {
                it.savePlaylists(current)
                it.saveNextId(id)
            }
        }
    }

    fun create(name: String): Playlist {
        val playlist = Playlist(id = nextId++, name = name)
        _playlists.update { it + playlist }
        persist()
        return playlist
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId && songId !in p.songIds) {
                    p.copy(songIds = p.songIds + songId)
                } else p
            }
        }
        persist()
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) {
                    p.copy(songIds = p.songIds.filter { it != songId })
                } else p
            }
        }
        persist()
    }

    fun delete(playlistId: Long) {
        _playlists.update { it.filter { p -> p.id != playlistId } }
        persist()
    }

    fun updateCoverUri(playlistId: Long, coverUri: String?) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) p.copy(coverUri = coverUri) else p
            }
        }
        persist()
    }

    fun rename(playlistId: Long, newName: String) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) p.copy(name = newName) else p
            }
        }
        persist()
    }

    fun getSongsForPlaylist(playlistId: Long, allSongs: List<Song>): List<Song> {
        val playlist = _playlists.value.find { it.id == playlistId } ?: return emptyList()
        return allSongs.filter { it.id in playlist.songIds }
    }
}
