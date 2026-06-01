package com.example.cassie.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cassie.ui.theme.CassieColors
import kotlinx.coroutines.launch

private val TextPrimary = CassieColors.TextPrimary
private val TextDim     = CassieColors.TextDim
private val PurpleAccent = CassieColors.PurpleAccent
private val CardGrey     = CassieColors.CardGrey

/**
 * Right-edge A–Z sidebar for fast song lookup.
 *
 * Behavior:
 *  - Tap or drag a letter → instant-scrolls to the first song starting
 *    with that letter (no animation — feels snappy on drag).
 *  - A big centered letter bubble appears while the user is actively
 *    pressing the sidebar; disappears the moment they lift their finger.
 *  - The steady-state highlight (when the user is just scrolling the
 *    list, not touching the sidebar) follows the LazyColumn scroll
 *    position automatically.
 *  - Letters with no songs are dimmed to 35% alpha (still tappable, but
 *    no-op so the user can still see the index structure).
 *
 * @param listState          the LazyColumn that hosts the songs list
 * @param songsStartIndex    the LazyColumn item index of the first song
 * @param letterToSongIndex  map of letter → song-list index
 * @param modifier           should be `Modifier.fillMaxSize()` so the
 *                           centered bubble has full area to anchor in
 */
@Composable
fun AlphabetScrollIndex(
    listState: LazyListState,
    songsStartIndex: Int,
    letterToSongIndex: Map<Char, Int>,
    modifier: Modifier = Modifier,
) {
    val letters = remember { ('A'..'Z').toList() }
    val present = remember(letterToSongIndex) { letterToSongIndex.keys.toSet() }
    var active by remember { mutableStateOf<Char?>(null) }
    val scope = rememberCoroutineScope()

    // Steady-state highlight: follow the scroll position when the
    // user isn't actively touching the sidebar.
    val currentLetter by remember {
        derivedStateOf {
            val idx = listState.firstVisibleItemIndex - songsStartIndex
            if (idx < 0) null
            else letterToSongIndex.entries
                .filter { it.value <= idx }
                .maxByOrNull { it.value }?.key
        }
    }

    fun jumpTo(letter: Char) {
        val songIdx = letterToSongIndex[letter] ?: return
        active = letter
        // Instant scroll: snappy on drag. animateScrollToItem would lag
        // because each new finger position fights the in-flight animation.
        scope.launch {
            listState.scrollToItem(songsStartIndex + songIdx)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        // ── Letter column on the right edge ─────────────────────────
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(vertical = 6.dp, horizontal = 3.dp)
                .pointerInput(letterToSongIndex, songsStartIndex, present) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val i = ((change.position.y / h) * letters.size)
                                    .toInt()
                                    .coerceIn(0, letters.lastIndex)
                                val ch = letters[i]
                                if (ch in present) jumpTo(ch)
                            } else {
                                // Pointer lifted — clear the bubble so
                                // the UI goes "back to normal".
                                if (active != null) active = null
                            }
                        }
                    }
                }
        ) {
            letters.forEach { ch ->
                val hasSong = ch in present
                val isActive = ch == active || (active == null && ch == currentLetter)
                Text(
                    text = ch.toString(),
                    color = when {
                        !hasSong  -> TextDim.copy(alpha = 0.35f)
                        isActive  -> PurpleAccent
                        else      -> TextDim
                    },
                    fontSize = if (isActive) 12.sp else 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // ── Centered letter bubble — ONLY while actively picking ────
        // `active != null` means the user is currently pressing; the
        // moment they release, the pointer handler above clears it.
        active?.let { ch ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardGrey.copy(alpha = 0.92f))
            ) {
                Text(
                    text = ch.toString(),
                    color = TextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
