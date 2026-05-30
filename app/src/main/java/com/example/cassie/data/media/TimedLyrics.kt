package com.example.cassie.data.media

/**
 * A single line of timed lyrics parsed from LRC format.
 */
data class TimedLyricLine(
    val timestampMs: Long,
    val text: String,
)

/**
 * Parses LRC (LyRiCs) format into timed lines.
 * Supports: [mm:ss.xx] or [mm:ss] style timestamps.
 * Lines with multiple timestamps are expanded into separate entries.
 */
fun parseLrc(syncedLyrics: String): List<TimedLyricLine> {
    val result = mutableListOf<TimedLyricLine>()
    val linePattern = Regex("""\[(\d+):(\d+(?:\.\d+)?)\](.*)""")

    syncedLyrics.lines().forEach { line ->
        val matches = linePattern.findAll(line)
        var lyricText = ""
        val timestamps = mutableListOf<Long>()

        for (match in matches) {
            val minStr = match.groupValues[1]
            val secStr = match.groupValues[2]
            val minute = (minStr.toIntOrNull() ?: 0).toLong()
            val second = secStr.toFloatOrNull() ?: 0f
            val ms = (minute * 60L * 1000L + (second * 1000f).toLong()).coerceAtLeast(0L)
            timestamps.add(ms)
            // Extract text after the last timestamp bracket
            lyricText = match.groupValues[3].trim()
        }

        // If we found timestamps, add entries
        if (timestamps.isNotEmpty()) {
            timestamps.forEach { ts ->
                // Merge adjacent entries with same text (multiple timestamps per line in LRC)
                val existing = result.lastOrNull()
                if (existing != null && existing.text == lyricText) {
                    // Skip duplicate — already added with an earlier timestamp
                } else {
                    result.add(TimedLyricLine(ts, lyricText))
                }
            }
        } else if (line.isNotBlank()) {
            // Lines without timestamps — keep as plain text with a -1 timestamp
            // Only add if previous line doesn't already have this as text
            val prev = result.lastOrNull()
            if (prev == null || prev.text != line.trim()) {
                result.add(TimedLyricLine(-1, line.trim()))
            }
        }
    }

    // Sort by timestamp and remove duplicates
    return result
        .distinctBy { it.timestampMs to it.text }
        .sortedBy { it.timestampMs }
}
