package com.example.cassie.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cassie.ui.theme.CassieColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TextPrimary = CassieColors.TextPrimary
private val TextDim     = CassieColors.TextDim
private val PurpleAccent = CassieColors.PurpleAccent
private val CardGrey     = CassieColors.CardGrey

/**
 * Right-edge A–Z sidebar for fast song lookup.
 *
 * The indicator is a floating bubble with a triangular tail pointing
 * AT the letter column, anchored to the active letter's Y position.
 * It pops in (spring scale) on press and disappears the moment the
 * finger lifts, so the UI returns to "normal" cleanly.
 *
 * Performance:
 *  - pointer events on the column update the Y position via a
 *    FloatState (skips structural equality on every frame).
 *  - `scrollToItem` (instant) is used so each new finger position
 *    wins the scroll race; animateScrollToItem would lag because
 *    each new press fights the previous in-flight animation.
 *  - Only the indicator Composable recomposes during drag — the
 *    column's letter colors and the LazyColumn are untouched.
 *
 * @param listState          the LazyColumn that hosts the songs list
 * @param songsStartIndex    the LazyColumn item index of the first song
 * @param letterToSongIndex  map of letter → song-list index
 * @param modifier           should be `Modifier.fillMaxSize()` so the
 *                           indicator can position itself relative to
 *                           the full home area, not just the column
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
    // Float state — primitive, no structural equality overhead per frame.
    var indicatorY by remember { mutableFloatStateOf(0f) }
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

    fun jumpTo(letter: Char, touchY: Float) {
        val songIdx = letterToSongIndex[letter] ?: return
        active = letter
        indicatorY = touchY
        // Instant scroll: snappy on drag. animateScrollToItem would
        // lag because each new finger position fights the in-flight
        // animation.
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
                .padding(end = 6.dp)
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
                                if (ch in present) jumpTo(ch, change.position.y)
                            } else {
                                // Pointer lifted — clear the indicator
                                // so the UI returns to its steady state.
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

        // ── Floating bubble-with-tail indicator ─────────────────────
        // Anchored to the column's vertical center, but offset to
        // follow the active letter's Y position. Tail points right
        // (toward the column) so the user sees exactly which letter
        // they're pressing.
        active?.let { ch ->
            // Pop-in spring on appear.
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "indicatorPop",
            )
            val bubbleSize = 56.dp
            // Column is padded 6.dp from the right edge, plus its own
            // background padding 3.dp each side + ~14.dp text width.
            // Anchor the bubble 32.dp to the left of the column's
            // right edge so the tail can fit pointing right.
            val columnRightOffset = 6.dp + 3.dp + 14.dp
            val bubbleOffsetX = -(columnRightOffset + 32.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset {
                        // Anchor the bubble's vertical center on indicatorY
                        IntOffset(
                            x = bubbleOffsetX.roundToPx(),
                            y = (indicatorY - bubbleSize.toPx() / 2f).roundToInt(),
                        )
                    }
                    .size(bubbleSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGrey.copy(alpha = 0.96f))
                    .drawBehind {
                        // Triangular tail on the right edge of the
                        // bubble, pointing toward the column.
                        val tailW = 10.dp.toPx()
                        val tailH = 16.dp.toPx()
                        val cx = size.width - 1.dp.toPx()
                        val cy = size.height / 2f
                        val path = Path().apply {
                            moveTo(cx, cy - tailH / 2f)
                            lineTo(cx + tailW, cy)
                            lineTo(cx, cy + tailH / 2f)
                            close()
                        }
                        drawPath(path, color = CardGrey.copy(alpha = 0.96f))
                    },
            ) {
                Text(
                    text = ch.toString(),
                    color = PurpleAccent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
