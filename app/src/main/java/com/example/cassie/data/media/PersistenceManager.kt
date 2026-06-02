package com.example.cassie.data.media

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PersistenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cassie_data", Context.MODE_PRIVATE)

    // ── Favorites ──────────────────────────────────────────────────

    fun saveFavoriteIds(ids: Set<Long>) {
        prefs.edit().putStringSet("favorites", ids.map { it.toString() }.toSet()).apply()
    }

    fun loadFavoriteIds(): Set<Long> {
        return prefs.getStringSet("favorites", emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet() ?: emptySet()
    }

    // ── Playlists ──────────────────────────────────────────────────

    fun savePlaylists(playlists: List<Playlist>) {
        val json = JSONArray()
        for (p in playlists) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            val songIds = JSONArray()
            p.songIds.forEach { songIds.put(it) }
            obj.put("songIds", songIds)
            if (p.coverUri != null) obj.put("coverUri", p.coverUri)
            json.put(obj)
        }
        prefs.edit().putString("playlists", json.toString()).apply()
    }

    fun loadPlaylists(): List<Playlist> {
        val raw = prefs.getString("playlists", null) ?: return emptyList()
        return try {
            val json = JSONArray(raw)
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                val songIds = mutableListOf<Long>()
                val arr = obj.getJSONArray("songIds")
                (0 until arr.length()).forEach { songIds.add(arr.getLong(it)) }
                Playlist(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    songIds = songIds.toList(), // immutable copy
                    coverUri = obj.optString("coverUri", "").takeIf { it.isNotBlank() },
                )
            }
        } catch (e: Exception) {
            // Corrupt blob — move it aside (don't overwrite) so the
            // user can recover it from device storage and we have
            // something to attach a bug report to. Returning empty
            // here without preserving the raw would silently lose
            // every playlist on the next save.
            try {
                val aside = "playlists_corrupt_${System.currentTimeMillis()}"
                prefs.edit().putString(aside, raw).remove("playlists").apply()
            } catch (_: Exception) { /* best-effort */ }
            emptyList()
        }
    }

    fun saveNextId(nextId: Long) {
        prefs.edit().putLong("playlist_next_id", nextId).apply()
    }

    fun loadNextId(): Long {
        return prefs.getLong("playlist_next_id", 1L)
    }

    // ── Generic key-value for any additional data (e.g. play counts) ──

    fun getString(key: String, default: String? = null): String? {
        return prefs.getString(key, default)
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    /**
     * Synchronous variant of [putString] that BLOCKS the caller until
     * the write has been committed to disk. Use this for data that
     * must survive a force-kill (e.g. the per-minute Top 50 tracker,
     * which the user flagged as "critical" and "loses data when the
     * app is closed"). Async [putString] uses `apply()` which writes
     * to memory immediately but flushes to disk in the background —
     * if the OS kills the process before the flush, the write is
     * lost. [putStringCommit] uses `commit()` so the write is
     * guaranteed durable before this call returns.
     */
    fun putStringCommit(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }
}
