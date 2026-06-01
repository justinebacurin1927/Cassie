package com.example.cassie.ui.party

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cassie.party.SkipperEngine
import com.example.cassie.party.UserEvent
import com.example.cassie.party.UserPattern
import com.example.cassie.party.UserPatternType

// ── Palette (matches the rest of Cassie's dark UI) ─────────────────
private val CardGrey      = Color(0xFF1E1E1E)
private val InnerGrey     = Color(0xFF121212)
private val TextPrimary   = Color.White
private val TextDim       = Color.White.copy(alpha = 0.45f)
private val TextMuted     = Color.White.copy(alpha = 0.25f)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TealAccent    = Color(0xFF03DAC5)
private val WarnAccent    = Color(0xFFFFB74D)

/**
 * Debug-only view of what Skipper's pattern recognizer is seeing.
 *
 * This card is the v1 verification surface. When the recognizer is
 * working end-to-end we'll replace it with the final SkipperCard
 * (penguin illustration + a single generated line). For now it shows:
 *
 *  - Currently detected patterns with confidence bars
 *  - The most recent 8 raw events
 *  - A reset button so we can test pattern detection from a clean slate
 */
@Composable
fun SkipperDebugCard(modifier: Modifier = Modifier) {
    val patterns by SkipperEngine.currentPatterns.collectAsState()
    val events by SkipperEngine.recentEvents.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardGrey)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────
        Text(
            text = "SKIPPER · DEBUG",
            color = PurpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Pattern Recognition v1",
            color = TextDim,
            fontSize = 10.sp,
        )

        Spacer(Modifier.height(16.dp))

        // ── Detected patterns ────────────────────────────────────
        Text(
            text = "Detected patterns (${patterns.size})",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        if (patterns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InnerGrey)
                    .padding(12.dp),
            ) {
                Text(
                    text = "no patterns yet — play, skip, or loop some songs",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
        } else {
            patterns.forEach { PatternRow(it) }
        }

        Spacer(Modifier.height(20.dp))

        // ── Recent events ────────────────────────────────────────
        Text(
            text = "Recent events (${events.size})",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(InnerGrey)
                .padding(12.dp),
        ) {
            if (events.isEmpty()) {
                Text(
                    text = "no events yet",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            } else {
                Column {
                    events.take(8).forEach { EventRow(it) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Reset (test helper) ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WarnAccent.copy(alpha = 0.15f))
                .padding(12.dp),
        ) {
            Column {
                Text(
                    text = "reset all behavior stats",
                    color = WarnAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap the parent card area to wipe Skipper's memory and start fresh. Useful for testing.",
                    color = TextDim,
                    fontSize = 10.sp,
                )
            }
        }

        // Click handler is wired at the parent level (in the home
        // screen) so we don't accidentally nest clickable areas.
    }
}

@Composable
private fun PatternRow(pattern: UserPattern) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Confidence bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(InnerGrey),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pattern.confidence)
                    .background(TealAccent),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${(pattern.confidence * 100).toInt()}%",
            color = TextDim,
            fontSize = 10.sp,
            modifier = Modifier.width(36.dp),
        )
    }
    Spacer(Modifier.height(2.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pattern.type.label(),
            color = TealAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = pattern.evidence,
            color = TextDim,
            fontSize = 11.sp,
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EventRow(event: UserEvent) {
    val (tag, detail) = event.toDisplay()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = tag,
            color = PurpleAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = detail,
            color = TextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun UserPatternType.label(): String = when (this) {
    UserPatternType.SKIPPER -> "SKIPPER"
    UserPatternType.LOOPER -> "LOOPER"
    UserPatternType.REPEATER -> "REPEATER"
    UserPatternType.MARATHONER -> "MARATHONER"
    UserPatternType.PARTIER -> "PARTIER"
    UserPatternType.EXPLORER -> "EXPLORER"
    UserPatternType.NIGHT_OWL -> "NIGHT_OWL"
    UserPatternType.FAVORITE_HOARDER -> "FAV_HOARDER"
    UserPatternType.LYRICS_LOVER -> "LYRICS_LOVER"
}

private fun UserEvent.toDisplay(): Pair<String, String> = when (this) {
    is UserEvent.SongStarted ->
        "START" to "song=$songId src=${source.name}"
    is UserEvent.SongSkipped ->
        "SKIP" to "song=$songId at=${positionMs / 1000}s/${songDurationMs / 1000}s"
    is UserEvent.SongReplayed ->
        "REPLAY" to "song=$songId"
    is UserEvent.SongLooped ->
        "LOOP" to "song=$songId"
    is UserEvent.SongPaused ->
        "PAUSE" to "song=$songId @${positionMs / 1000}s"
    is UserEvent.SongResumed ->
        "RESUME" to "song=$songId"
    is UserEvent.SongSeeked ->
        "SEEK" to "song=$songId ${fromMs / 1000}->${toMs / 1000}s"
    is UserEvent.SongCompleted ->
        "DONE" to "song=$songId"
    is UserEvent.PartyModeToggled ->
        "PARTY" to (if (enabled) "ON" else "OFF")
    is UserEvent.RepeatModeChanged ->
        "REPEAT" to "mode=$mode"
    is UserEvent.ShuffleToggled ->
        "SHUFFLE" to (if (enabled) "ON" else "OFF")
    is UserEvent.SleepTimerSet ->
        "SLEEP" to "${minutes}m"
    is UserEvent.FavoriteToggled ->
        "FAV" to "song=$songId ${if (isFavorite) "+" else "-"}"
    is UserEvent.LyricsOpened ->
        "LYRICS" to "song=$songId"
    is UserEvent.AppForegrounded ->
        "FG" to ""
    is UserEvent.AppBackgrounded ->
        "BG" to ""
    is UserEvent.MinutesListenedTicked ->
        "MIN+" to "song=$songId"
}
