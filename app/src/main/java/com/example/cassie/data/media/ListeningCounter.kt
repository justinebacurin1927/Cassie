package com.example.cassie.data.media

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

@Stable
data class PlayCount(
    val songId: Long,
    val count: Int = 0,
)

class ListeningCounter(private val persistenceManager: PersistenceManager? = null) {
    private val _counts = MutableStateFlow(
        loadCounts()
    )
    val counts: StateFlow<Map<Long, PlayCount>> = _counts.asStateFlow()

    fun recordPlay(songId: Long) {
        _counts.update { current ->
            val existing = current[songId]
            val updated = current + (songId to PlayCount(
                songId = songId,
                count = (existing?.count ?: 0) + 1
            ))
            saveCounts(updated)
            updated
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

    // ── persistence ─────────────────────────────────────────────────

    private fun loadCounts(): Map<Long, PlayCount> {
        val raw = persistenceManager?.getString("play_counts") ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<Long, PlayCount>()
            obj.keys().forEach { key ->
                val count = obj.optInt(key, 0)
                map[key.toLong()] = PlayCount(songId = key.toLong(), count = count)
            }
            map
        } catch (_: Exception) { emptyMap() }
    }

    private fun saveCounts(counts: Map<Long, PlayCount>) {
        val obj = JSONObject()
        counts.forEach { (id, pc) -> obj.put(id.toString(), pc.count) }
        persistenceManager?.putString("play_counts", obj.toString())
    }
}
