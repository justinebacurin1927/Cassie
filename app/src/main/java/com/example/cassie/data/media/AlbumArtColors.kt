package com.example.cassie.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.LinkedHashMap

/**
 * Extracts dominant / vibrant colors from album art.
 * Uses AndroidX Palette library for smart swatch selection.
 */
@Stable
data class AlbumArtColors(
    val primary: Color = Color(0xFF1E1E1E),
    val secondary: Color = Color(0xFF282828),
    val accent: Color = Color(0xFFBB86FC),
    val isDark: Boolean = true,
)

object AlbumArtColorExtractor {

    // LRU cache to avoid re-extracting
    private val cache = object : LinkedHashMap<Long, AlbumArtColors>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, AlbumArtColors>?): Boolean =
            size > 128
    }

    suspend fun extract(context: Context, albumArtUri: String?, albumId: Long): AlbumArtColors = withContext(Dispatchers.Default) {
        if (albumArtUri == null) return@withContext AlbumArtColors()

        // Check cache
        cache[albumId]?.let { return@withContext it }

        try {
            val bitmap = loadBitmap(context, albumArtUri) ?: return@withContext AlbumArtColors()
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()

            val dominant = palette.dominantSwatch
            val vibrant = palette.vibrantSwatch
            val muted = palette.mutedSwatch
            val darkVibrant = palette.darkVibrantSwatch

            val primaryColor = vibrant?.rgb?.let { Color(it) }
                ?: dominant?.rgb?.let { Color(it) }
                ?: Color(0xFF1E1E1E)

            val secondaryColor = muted?.rgb?.let { Color(it) }
                ?: darkVibrant?.rgb?.let { Color(it) }
                ?: primaryColor.copy(alpha = 0.6f)

            val accentColor = when {
                vibrant != null -> Color(vibrant.rgb)
                dominant != null -> Color(dominant.rgb)
                else -> Color(0xFFBB86FC)
            }

            val luminance = 0.2126 * primaryColor.red + 0.7152 * primaryColor.green + 0.0722 * primaryColor.blue
            val isDark = luminance < 0.5

            val colors = AlbumArtColors(
                primary = primaryColor,
                secondary = secondaryColor,
                accent = accentColor,
                isDark = isDark,
            )

            cache[albumId] = colors
            colors
        } catch (_: Exception) {
            AlbumArtColors()
        }
    }

    private fun loadBitmap(context: Context, uri: String): Bitmap? {
        return try {
            val input = when {
                uri.startsWith("content://") ->
                    context.contentResolver.openInputStream(Uri.parse(uri))
                uri.startsWith("file://") ->
                    FileInputStream(File(uri.removePrefix("file://")))
                uri.startsWith("/") ->
                    FileInputStream(File(uri))
                else -> null
            } ?: return null
            input.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }

    fun clearCache() = cache.clear()
}
