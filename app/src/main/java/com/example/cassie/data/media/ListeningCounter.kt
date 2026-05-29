package com.example.cassie.data.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlayCount(
    val songId: Long,
    val count: Int = 0,
)

class ListeningCounter {
    private val _counts = MutableStateFlow(
        emptyMap<Long, PlayCount>()
    )
    val counts: StateFlow<Map<Long, PlayCount>> = _counts.asStateFlow()

    fun recordPlay(songId: Long) {
        _counts.update { current ->
            val existing = current[songId]
            current + (songId to PlayCount(
                songId = songId,
                count = (existing?.count ?: 0) + 1
            ))
        }
    }

    fun getTop50(songs: List<Song>): List<Pair<Song, Int>> {
        val sorted = _counts.value.entries
            .sortedByDescending { it.value.count }
            .take(50)

        return sorted.mapNotNull { (id, playCount) ->
            val song = songs.find { it.id == id }
            song?.let { it to playCount.count }
        }
    }

    fun getRecentPlays(songs: List<Song>, limit: Int = 5): List<Song> {
        return _counts.value.entries
            .sortedByDescending { it.value.count }
            .take(limit)
            .mapNotNull { (id, _) -> songs.find { it.id == id } }
    }
}
