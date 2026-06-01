package com.example.cassie.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.lazy.LazyListState
import com.example.cassie.ui.theme.CassieColors
import kotlinx.coroutines.launch

private val TextPrimary = CassieColors.TextPrimary
private val TextDim     = CassieColors.TextDim
private val PurpleAccent = CassieColors.PurpleAccent

/**
 * Right-edge A–Z sidebar for fast song lookup.
 *
 * - Tap or drag a letter → smooth-scrolls to the first song beginning
 *   with that letter.
 * - The active letter is highlighted in [PurpleAccent].
 * - A big centered letter bubble appears while the user is actively
 *   touching the sidebar so they can see which letter they're on.
 * - The steady-state highlight (when the user is just scrolling the
 *   list, not touching the sidebar) follows the LazyColumn scroll
 *   position automatically.
 *
 * @param listState   the LazyColumn that hosts the songs list
 * @param songsStartIndex  the LazyColumn item index of the first song
 * @param letterToSongIndex  map of letter → song-list index
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
        scope.launch {
            listState.animateScrollToItem(songsStartIndex + songIdx)
        }
    }

    Box(modifier = modifier) {
        // The letter column. One pointer handler on the whole column
        // samples Y on every press so dragging along the edge jumps
        // through letters smoothly.
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(vertical = 6.dp, horizontal = 3.dp)
                .pointerInput(letterToSongIndex, songsStartIndex) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (!change.pressed) continue
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val i = ((change.position.y / h) * letters.size)
                                .toInt()
                                .coerceIn(0, letters.lastIndex)
                            val ch = letters[i]
                            if (ch in present) jumpTo(ch)
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

        // Centered bubble — only while the user is actively picking.
        active?.let { ch ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
            ) {
                Text(
                    text = ch.toString(),
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
