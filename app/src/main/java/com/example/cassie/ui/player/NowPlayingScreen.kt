package com.example.cassie.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.cassie.data.media.LyricsRepository
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palette ────────────────────────────────────────────────────────
private val PureBlack    = Color(0xFF000000)
private val DarkGrey     = Color(0xFF121212)
private val CardGrey     = Color(0xFF1E1E1E)
private val SurfaceGrey  = Color(0xFF282828)
private val PurpleAccent = Color(0xFFBB86FC)
private val TextPrimary  = Color.White
private val TextSecondary= Color.White.copy(alpha = 0.6f)
private val TextDim      = Color.White.copy(alpha = 0.35f)

@Composable
fun NowPlayingScreen(
    playbackManager: PlaybackManager,
    onClose: () -> Unit,
) {
    val state by playbackManager.playerState.collectAsState()

    // live position
    var position by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            position = playbackManager.getCurrentPosition()
        }
    }

    val duration = state.duration.coerceAtLeast(1L)
    val song = state.currentSong

    // ── lyrics state ────────────────────────────────────────────────
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var loadingLyrics by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ── queue dialog ────────────────────────────────────────────────
    var showQueue by remember { mutableStateOf(false) }
    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            containerColor = CardGrey,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Up Next", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                val q = state.queue
                if (q.isEmpty()) {
                    Text("Queue is empty", color = TextDim)
                } else {
                    Column {
                        q.forEachIndexed { idx, qSong ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${idx + 1}.",
                                    color = TextDim,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(24.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(qSong.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(qSong.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueue = false }) {
                    Text("Close", color = PurpleAccent)
                }
            }
        )
    }

    // ── sleep timer state ───────────────────────────────────────────
    var showSleepPicker by remember { mutableStateOf(false) }
    if (showSleepPicker) {
        AlertDialog(
            onDismissRequest = { showSleepPicker = false },
            containerColor = CardGrey,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Sleep Timer", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(
                            onClick = {
                                playbackManager.setSleepTimer(mins)
                                showSleepPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$mins minutes", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                    if (state.sleepTimerRemainingSec > 0) {
                        HorizontalDivider(color = SurfaceGrey, modifier = Modifier.padding(vertical = 8.dp))
                        TextButton(
                            onClick = {
                                playbackManager.cancelSleepTimer()
                                showSleepPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Timer", color = PurpleAccent, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepPicker = false }) {
                    Text("Close", color = PurpleAccent)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGrey)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // ── header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
                Text("NOW PLAYING", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Row {
                    // lyrics toggle
                    IconButton(onClick = {
                        showLyrics = !showLyrics
                        if (showLyrics && lyricsText == null && song != null && !loadingLyrics) {
                            loadingLyrics = true
                            scope.launch {
                                lyricsText = LyricsRepository.fetchLyrics(song.artist, song.title)?.plainLyrics
                                loadingLyrics = false
                            }
                        }
                    }) {
                        Icon(
                            if (showLyrics) Icons.Default.MusicNote else Icons.Default.Lyrics,
                            "Lyrics", tint = if (showLyrics) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // queue
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.Default.QueueMusic, "Queue", tint = TextDim, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── album art or lyrics ──
            if (showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardGrey)
                        .padding(16.dp)
                ) {
                    when {
                        loadingLyrics -> Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(24.dp))
                        }
                        lyricsText != null -> Box(Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState())) {
                            Text(
                                lyricsText!!, color = TextSecondary, fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                        else -> Text(
                            "No lyrics found", color = TextDim, fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                // album art
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .shadow(20.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardGrey),
                    contentAlignment = Alignment.Center
                ) {
                    if (song != null && song.albumArtUri != null) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PurpleAccent.copy(0.3f), PurpleAccent.copy(0.1f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = TextDim, modifier = Modifier.size(80.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── song info ──
            Text(
                song?.title ?: "No Track",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song?.artist ?: "Unknown Artist",
                fontSize = 15.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── seekbar ──
            val density = LocalDensity.current
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                playbackManager.seekTo((fraction * duration).toLong())
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                playbackManager.seekTo((fraction * duration).toLong())
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TextDim.copy(alpha = 0.3f))
                    ) {
                        val progress = (position.toFloat() / duration).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurpleAccent)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), color = TextDim, fontSize = 11.sp)
                    Text(formatTime(duration), color = TextDim, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── playback controls ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = Icons.Default.Shuffle,
                    active = state.shuffleMode,
                    onClick = { playbackManager.toggleShuffle() }
                )
                ControlCircle(icon = Icons.Default.SkipPrevious, size = 48.dp, onClick = { playbackManager.skipToPrevious() })
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { playbackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = PureBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }
                ControlCircle(icon = Icons.Default.SkipNext, size = 48.dp, onClick = { playbackManager.skipToNext() })
                ControlButton(
                    icon = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    active = state.repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = { playbackManager.cycleRepeat() }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── sleep timer ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGrey)
                    .clickable { showSleepPicker = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sleep Timer", color = TextSecondary, fontSize = 14.sp)
                if (state.sleepTimerRemainingSec > 0) {
                    val mins = state.sleepTimerRemainingSec / 60
                    val secs = state.sleepTimerRemainingSec % 60
                    Text(
                        "${mins}:%02d".format(secs),
                        color = PurpleAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Timer, null, tint = TextDim, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) PurpleAccent.copy(alpha = 0.25f) else TextDim.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) PurpleAccent else TextDim, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ControlCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(TextDim.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(size * 0.5f))
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
