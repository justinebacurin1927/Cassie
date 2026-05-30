package com.example.cassie.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cassie.data.media.TimedLyricLine
import com.example.cassie.ui.theme.CassieColors
import kotlinx.coroutines.launch

/**
 * Spotify-style karaoke lyrics with:
 * - Current line highlighted and bright
 * - Past lines dimmed
 * - Future lines dimmer
 * - Pulsing indicator dot on current line
 * - Glow effect on current line using accentColor
 * - Auto-scroll to keep current line centered
 */
@Composable
fun SyncedLyricsDisplay(
    timedLines: List<TimedLyricLine>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    accentColor: Color = CassieColors.PurpleAccent,
    modifier: Modifier = Modifier,
) {
    if (timedLines.isEmpty()) return

    // Find current line index based on playback position
    val currentIndex = remember(currentPositionMs, timedLines) {
        var idx = 0
        for (i in timedLines.indices) {
            val ts = timedLines[i].timestampMs
            if (ts >= 0 && ts <= currentPositionMs) {
                idx = i
            } else if (ts > currentPositionMs) {
                break
            }
        }
        idx
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to current line
    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) {
            val target = (currentIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    // Pulsing indicator animation
    val infinite = rememberInfiniteTransition(label = "lyricPulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(timedLines, key = { idx, _ -> idx }) { idx, line ->
            val isCurrent = idx == currentIndex
            val isPast = idx < currentIndex

            val textAlpha = when {
                isCurrent -> 1f
                isPast -> 0.4f
                else -> 0.55f
            }
            val textSize = if (isCurrent) 16.sp else 15.sp
            val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCurrent) {
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(vertical = 2.dp)
                        } else Modifier
                    )
                    .padding(horizontal = 8.dp),
            ) {
                // ── Indicator column ──
                Box(Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                    if (isCurrent) {
                        // Glowing pulsing dot indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = pulseAlpha))
                        )
                    }
                }

                // ── Lyric text ──
                Text(
                    text = line.text.ifEmpty { "…" },
                    color = CassieColors.TextPrimary.copy(alpha = textAlpha),
                    fontSize = textSize,
                    fontWeight = fontWeight,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
