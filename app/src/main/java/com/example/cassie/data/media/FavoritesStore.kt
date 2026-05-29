package com.example.cassie.data.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FavoritesStore(persistenceManager: PersistenceManager? = null) {
    private val _favoriteIds = MutableStateFlow<Set<Long>>(
        persistenceManager?.loadFavoriteIds() ?: emptySet()
    )
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()
    private val pm = persistenceManager

    fun toggle(songId: Long) {
        _favoriteIds.update { ids ->
            val updated = if (songId in ids) ids - songId else ids + songId
            pm?.saveFavoriteIds(updated)
            updated
        }
    }

    fun isFavorite(songId: Long): Boolean = songId in _favoriteIds.value

    fun getFavorites(allSongs: List<Song>): List<Song> =
        allSongs.filter { it.id in _favoriteIds.value }
}
